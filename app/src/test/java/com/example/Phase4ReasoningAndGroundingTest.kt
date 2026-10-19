package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.zahra.engines.ZahraRouter
import com.example.zahra.model.*
import com.example.zahra.reasoning.*
import com.example.zahra.security.ZahraCrypto
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class Phase4ReasoningAndGroundingTest {

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
  // 1. BOUNDED DETERMINISTIC REASONING ENGINE
  // =========================================================================

  @Test
  fun testReasoningEngine_enforcesStepLimit() {
    // Construct a recursive rule that generates new facts indefinitely
    var counter = 0
    val infiniteRule = InferenceRule(
      id = "infinite_rule",
      name = "قاعدة لا نهائية تجريبية",
      patterns = listOf(FactPattern(predicate = "عداد")),
      deduce = { premises ->
        counter++
        TypedFact(
          subject = "عنصر_$counter",
          predicate = "عداد",
          objectValue = "$counter"
        )
      },
      maxAllowedDepth = 100
    )

    val engine = ReasoningEngine(
      rules = listOf(infiniteRule),
      limits = ReasoningLimits(maxSteps = 10, maxDepth = 20, timeoutMs = 500)
    )

    val initial = listOf(TypedFact(subject = "عنصر_0", predicate = "عداد", objectValue = "0"))
    val result = engine.infer(initial)

    assertTrue("Engine must hit step limit and terminate gracefully", result.hitStepLimit)
    assertEquals(10, result.stepsCount)
  }

  @Test
  fun testReasoningEngine_enforcesDepthLimit() {
    val chainRule = InferenceRule(
      id = "chain_rule",
      name = "سلسلة اشتقاق متتالية",
      patterns = listOf(FactPattern(predicate = "عمق")),
      deduce = { premises ->
        val p = premises[0]
        val currentDepth = p.objectValue.toIntOrNull() ?: 0
        TypedFact(
          subject = "مستوى_${currentDepth + 1}",
          predicate = "عمق",
          objectValue = "${currentDepth + 1}"
        )
      },
      maxAllowedDepth = 3
    )

    val engine = ReasoningEngine(
      rules = listOf(chainRule),
      limits = ReasoningLimits(maxSteps = 50, maxDepth = 3, timeoutMs = 500)
    )

    val initial = listOf(TypedFact(subject = "مستوى_0", predicate = "عمق", objectValue = "0", depth = 0))
    val result = engine.infer(initial)

    assertTrue("Engine must hit depth limit", result.hitDepthLimit)
    assertTrue("Max depth cannot exceed 3", result.derivedFacts.all { it.depth <= 3 })
  }

  @Test
  fun testReasoningEngine_tracksProvenance() {
    val rules = ReasoningEngine.getDefaultRules()
    val engine = ReasoningEngine(rules = rules)

    val userCityFact = TypedFact(
      id = "fact_user_city_01",
      subject = "المستخدم",
      predicate = "المدينة",
      objectValue = "الرياض",
      evidenceType = EvidenceType.USER_SUPPLIED,
      provenance = "ذاكرة المستخدم"
    )

    val result = engine.infer(listOf(userCityFact))
    val qiblaFact = result.derivedFacts.find { it.subject == "اتجاه القبلة" }
    assertNotNull("Qibla fact should be deduced from user city", qiblaFact)
    assertTrue("Derived fact must link to parent fact ID", qiblaFact!!.parentFactIds.contains("fact_user_city_01"))
    assertEquals("rule_qibla_from_city", qiblaFact.derivedFromRuleId)
  }

  // =========================================================================
  // 2. CONTRADICTION & UNCERTAINTY MANAGEMENT
  // =========================================================================

  @Test
  fun testContradictionDetector_explicitWinsOverInferred() {
    val existing = listOf(
      ZahraUserFact(
        key = "المدينة",
        value = "القاهرة",
        category = FactCategory.LOCATION_WORK,
        source = MemorySource.USER_EXPLICIT
      )
    )

    val report = ContradictionDetector.checkFactConflict(
      existingFacts = existing,
      newKey = "المدينة",
      newValue = "الإسكندرية",
      newSource = MemorySource.INFERRED_OBSERVED
    )

    assertTrue("Must detect conflict", report.hasConflict)
    assertEquals(ConflictType.SOURCE_PRIORITY_OVERRIDE, report.conflictType)
    assertEquals(ResolutionStrategy.PRESERVE_EXISTING, report.resolution)
    assertTrue("Must state that confirmed fact was preserved", report.explanationAr.contains("المعلومة المؤكدة"))
  }

  @Test
  fun testContradictionDetector_explicitCorrectionPermitted() {
    val existing = listOf(
      ZahraUserFact(
        key = "الاسم",
        value = "محمد",
        category = FactCategory.PERSONAL,
        source = MemorySource.USER_EXPLICIT
      )
    )

    val report = ContradictionDetector.checkFactConflict(
      existingFacts = existing,
      newKey = "الاسم",
      newValue = "أحمد",
      newSource = MemorySource.USER_EXPLICIT,
      isExplicitCorrection = true
    )

    assertTrue(report.hasConflict)
    assertEquals(ResolutionStrategy.RECORD_HISTORICAL_UPDATE, report.resolution)
    assertTrue("Must explain update", report.explanationAr.contains("تحديث"))
  }

  // =========================================================================
  // 3. EVIDENCE GROUNDING PIPELINE
  // =========================================================================

  @Test
  fun testEpistemicGroundingPipeline_buildsFullTrace() {
    val facts = listOf(
      GroundedFact(
        claim = "سرعة الضوء = 299,792,458 م/ث",
        evidenceType = EvidenceType.LOCAL_CURATED,
        source = "BIPM SI",
        confidence = 0.99
      ),
      GroundedFact(
        claim = "اسم المستخدم = خالد",
        evidenceType = EvidenceType.USER_SUPPLIED,
        source = "تأكيد المستخدم",
        confidence = 1.0
      )
    )

    val trace = EpistemicGroundingPipeline.buildTrace(
      rawRequest = "ما هي سرعة الضوء يا زهرة؟",
      intent = "query_knowledge",
      entities = mapOf("topic" to "سرعة الضوء"),
      appliedRules = listOf("rule_knowledge_lookup"),
      facts = facts,
      uncertainty = emptyList(),
      outcomeState = OutcomeState.SUPPORTED,
      sourceDesc = "قاعدة البيانات المعرفية"
    )

    assertNotNull(trace)
    assertEquals("query_knowledge", trace.detectedIntent)
    assertEquals(2, trace.groundedFacts.size)
    assertTrue(trace.transparencySummary.contains("مؤصلة بالكامل"))
  }

  @Test
  fun testRouterResponsesIncludeEvidenceTrace() {
    val resp = router.process("احسب 120 + 80")
    assertNotNull("Evidence trace must be attached to response", resp.evidenceTrace)
    assertEquals(OutcomeState.SUPPORTED, resp.outcomeState)
    assertTrue("Grounded facts must contain arithmetic deduction", resp.evidenceTrace!!.groundedFacts.any { it.evidenceType == EvidenceType.DEDUCTION })
  }
}
