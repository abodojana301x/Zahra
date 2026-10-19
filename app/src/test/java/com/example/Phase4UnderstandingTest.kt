package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.zahra.engines.ZahraRouter
import com.example.zahra.model.ConfidenceTier
import com.example.zahra.model.OutcomeState
import com.example.zahra.security.ZahraCrypto
import com.example.zahra.understanding.AmbiguityAndClarificationDetector
import com.example.zahra.understanding.AmbiguityCheckResult
import com.example.zahra.understanding.DialectAndColloquialAdapter
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Phase4UnderstandingTest {

  private lateinit var context: Context
  private lateinit var router: ZahraRouter

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    ZahraCrypto.setTestKeyForTestingOnly(ZahraCrypto.generateTestKey())
    router = ZahraRouter(context)
  }

  @After
  fun tearDown() {
    ZahraCrypto.setTestKeyForTestingOnly(null)
  }

  // =========================================================================
  // 1. EGYPTIAN ARABIC & COLLOQUIAL UNDERSTANDING
  // =========================================================================

  @Test
  fun testEgyptianColloquialPrefixStripping_extractsCoreQuery() {
    val adapted1 = DialectAndColloquialAdapter.adapt("معلش احسبلي 25 * 4")
    assertEquals("egyptian_colloquial", adapted1.detectedDialect)
    assertTrue("Stripped prefix must contain معلش", adapted1.strippedPrefixes.contains("معلش"))

    val adapted2 = DialectAndColloquialAdapter.adapt("بقولك ايه، ممكن تقولي ما هي سرعة الضوء؟")
    assertTrue(adapted2.strippedPrefixes.any { it.contains("بقولك") || it.contains("ممكن") })
  }

  @Test
  fun testEgyptianIntentOverrides() {
    val adapted = DialectAndColloquialAdapter.adapt("عايز اعرف سرعة الضوء")
    assertEquals("query_knowledge", adapted.inferredIntentOverride)

    val adaptedCalc = DialectAndColloquialAdapter.adapt("معلش احسبلي 100 + 50")
    assertEquals("calculate", adaptedCalc.inferredIntentOverride)
  }

  @Test
  fun testColloquialMathCalculationInRouter() {
    val resp = router.process("معلش احسبلي 25 * 4")
    assertEquals("calculate", resp.intent)
    assertEquals(OutcomeState.SUPPORTED, resp.outcomeState)
    assertTrue("Result must contain 100", resp.text.contains("100"))
  }

  @Test
  fun testColloquialKnowledgeQueryInRouter() {
    val resp = router.process("عايز اعرف ما هي سرعة الضوء")
    assertTrue("Intent should be knowledge or science", resp.intent == "query_knowledge" || resp.intent == "define_word")
    assertEquals(OutcomeState.SUPPORTED, resp.outcomeState)
    assertTrue("Must mention speed of light in vacuum", resp.text.contains("299,792,458") || resp.text.contains("سرعة الضوء"))
  }

  // =========================================================================
  // 2. ENGLISH AND MIXED COMMAND UNDERSTANDING
  // =========================================================================

  @Test
  fun testEnglishMathCommand() {
    val resp = router.process("calc 50 * 4")
    assertEquals("calculate", resp.intent)
    assertEquals(OutcomeState.SUPPORTED, resp.outcomeState)
    assertTrue("Must evaluate 50 * 4 = 200", resp.text.contains("200"))
  }

  @Test
  fun testEnglishUnitConversion() {
    val resp = router.process("convert 10 km to miles")
    assertEquals("convert_unit", resp.intent)
    assertEquals(OutcomeState.SUPPORTED, resp.outcomeState)
    assertTrue("Must convert km to miles", resp.text.contains("ميل") || resp.text.contains("6.21"))
  }

  @Test
  fun testEnglishKnowledgeQuery() {
    val resp = router.process("what is speed of light")
    assertTrue(resp.intent == "query_knowledge" || resp.intent == "define_word")
    assertEquals(OutcomeState.SUPPORTED, resp.outcomeState)
    assertTrue("Must cite speed of light", resp.text.contains("299,792,458") || resp.text.contains("سرعة الضوء"))
  }

  // =========================================================================
  // 3. AMBIGUITY & CLARIFICATION DETECTION
  // =========================================================================

  @Test
  fun testAmbiguousUnitConversion_triggersClarification() {
    val check = AmbiguityAndClarificationDetector.check("حول 10 كم", "حول 10 كم", "convert_unit")
    assertTrue("Must be identified as ambiguous", check is AmbiguityCheckResult.Ambiguous)
    val ambig = (check as AmbiguityCheckResult.Ambiguous).payload
    assertTrue("Options must offer miles or meters", ambig.options.any { it.associatedQuery.contains("ميل") })
  }

  @Test
  fun testAmbiguousScheduling_triggersClarification() {
    val check = AmbiguityAndClarificationDetector.check("فكرني بموعد الطبيب", "فكرني بموعد الطبيب", "create_schedule")
    assertTrue("Scheduling without time must be ambiguous", check is AmbiguityCheckResult.Ambiguous)
    val ambig = (check as AmbiguityCheckResult.Ambiguous).payload
    assertEquals("وقت التذكير غير محدد", ambig.reason)
    assertTrue("Must suggest morning or evening times", ambig.options.isNotEmpty())
  }

  @Test
  fun testAmbiguousRouterResponse_returnsClarificationCard() {
    val resp = router.process("حول 20 كم")
    assertEquals(OutcomeState.AMBIGUOUS, resp.outcomeState)
    assertNotNull("Clarification payload must be set", resp.clarification)
    assertTrue("Must include button options for user", resp.clarification!!.options.isNotEmpty())
  }

  // =========================================================================
  // 4. MULTI-TURN CONTEXT & USER CORRECTIONS
  // =========================================================================

  @Test
  fun testCorrectionOfName_updatesMemoryAndExplainsConflict() {
    // 1. Initial greeting / statement
    router.process("تذكر أن اسمي محمد")
    val initialFact = router.memoryEngine.getFacts().find { it.key == "الاسم" }
    assertEquals("محمد", initialFact?.value)

    // 2. User correction: "اسمي مش محمد، اسمي علي"
    val correctionResp = router.process("اسمي مش محمد، اسمي علي")
    assertEquals("correct_fact", correctionResp.intent)
    assertEquals(OutcomeState.SUPPORTED, correctionResp.outcomeState)
    assertTrue("Must confirm update", correctionResp.text.contains("علي"))

    val updatedFact = router.memoryEngine.getFacts().find { it.key == "الاسم" }
    assertEquals("علي", updatedFact?.value)
  }

  @Test
  fun testFollowupReference_carriesOverTopic() {
    // 1. First turn: speed of light
    val resp1 = router.process("ما هي سرعة الضوء؟")
    assertEquals(OutcomeState.SUPPORTED, resp1.outcomeState)

    // 2. Follow-up: "وماذا عن غليان الماء؟"
    val resp2 = router.process("وماذا عن غليان الماء؟")
    assertEquals(OutcomeState.SUPPORTED, resp2.outcomeState)
    assertTrue("Must answer boiling point of water", resp2.text.contains("100") || resp2.text.contains("غليان الماء"))
  }

  @Test
  fun testUndoAction_triggersSafeRollback() {
    router.process("أضف مهمة شراء حاسوب")
    val undoResp = router.process("تراجع")
    assertEquals("undo_action", undoResp.intent)
    assertEquals(OutcomeState.SUPPORTED, undoResp.outcomeState)
    assertTrue("Must confirm undo", undoResp.text.contains("تم التراجع"))
  }
}
