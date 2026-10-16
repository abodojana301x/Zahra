package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.zahra.engines.ZahraRouter
import com.example.zahra.model.OutcomeState
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WorkstreamDConfidenceAndSafetyTest {

  private lateinit var context: Context
  private lateinit var router: ZahraRouter

  @Before
  fun setUp() {
    context = ApplicationProvider.getApplicationContext()
    router = ZahraRouter(context)
  }

  // --- D1 & D3: All 4 Explicit Outcome States Verifiably Represented ---

  @Test
  fun testOutcomeState_SUPPORTED_onValidArithmetic() {
    val res = router.process("احسب 50 * 4 + 20")
    assertEquals(OutcomeState.SUPPORTED, res.outcomeState)
    assertTrue(res.text.contains("220"))
  }

  @Test
  fun testOutcomeState_AMBIGUOUS_whenRequiredTimeIsMissingInSchedule() {
    val initialScheduleCount = router.scheduleEngine.getSchedules().size

    // High heuristic intent match ("فكرني") but missing time!
    val res = router.process("فكرني بمراجعة بحث التخرج وكتابة الملخص")

    assertEquals("Missing time must produce AMBIGUOUS", OutcomeState.AMBIGUOUS, res.outcomeState)
    assertNull("No schedule must be created", res.createdSchedule)
    assertEquals(initialScheduleCount, router.scheduleEngine.getSchedules().size)
    assertTrue("Must ask user for time", res.text.contains("يرجى تحديد وقت"))
  }

  @Test
  fun testOutcomeState_UNSUPPORTED_whenInformationIsAbsent() {
    val res = router.process("ما هو حديث النبي عن قيادة الطائرات الفضائية؟")
    assertEquals(OutcomeState.UNSUPPORTED, res.outcomeState)
    assertTrue("Must state limitation or absence", res.text.contains("غير مسجل") || res.text.contains("أمتنع") || res.text.contains("يمتنع") || res.text.contains("مش متأكد"))
  }

  @Test
  fun testOutcomeState_EXECUTION_ERROR_onDivisionByZero() {
    val res = router.process("احسب 100 / (5 - 5)")
    assertEquals(OutcomeState.EXECUTION_ERROR, res.outcomeState)
    assertTrue(res.text.contains("صفر") || res.text.contains("غير معرفة"))
  }

  // --- D4: Confidence Cannot Authorize Execution By Itself ---

  @Test
  fun testConfidenceCannotAuthorizeExecution_withoutRequiredFields() {
    // Even if regex matches "create_schedule" with 0.98 confidence,
    // if time field is absent, it must NOT execute.
    val res = router.process("فكرني غداً")
    assertEquals(OutcomeState.AMBIGUOUS, res.outcomeState)
    assertNull(res.createdSchedule)
  }

  @Test
  fun testUnsupportedUnitConversion_neverFabricatesNumbers() {
    val res = router.process("حول 50 كجم إلى كيلوفولت")
    assertEquals(OutcomeState.UNSUPPORTED, res.outcomeState)
    assertTrue(res.text.contains("غير مدعوم"))
  }

  // --- D5: Result Verification (No False Success Confirmations) ---

  @Test
  fun testMathematicalFailure_neverReturnsFalseSuccess() {
    val res = router.process("احسب 25 / 0")
    assertFalse(res.text.contains("الناتج: 25 / 0 = 0"))
    assertEquals(OutcomeState.EXECUTION_ERROR, res.outcomeState)
  }
}
