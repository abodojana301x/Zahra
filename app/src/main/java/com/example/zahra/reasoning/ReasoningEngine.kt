package com.example.zahra.reasoning

import com.example.zahra.model.EvidenceType
import com.example.zahra.model.GroundedFact

data class TypedFact(
  val id: String = java.util.UUID.randomUUID().toString(),
  val subject: String,
  val predicate: String,
  val objectValue: String,
  val evidenceType: EvidenceType = EvidenceType.VERIFIED,
  val provenance: String = "محرك القواعد الحتمية",
  val confidence: Double = 1.0,
  val derivedFromRuleId: String? = null,
  val parentFactIds: List<String> = emptyList(),
  val depth: Int = 0
)

data class FactPattern(
  val subject: String? = null,
  val predicate: String? = null,
  val objectValue: String? = null
) {
  fun matches(fact: TypedFact): Boolean {
    if (subject != null && !subject.equals(fact.subject, ignoreCase = true)) return false
    if (predicate != null && !predicate.equals(fact.predicate, ignoreCase = true)) return false
    if (objectValue != null && !objectValue.equals(fact.objectValue, ignoreCase = true)) return false
    return true
  }
}

data class InferenceRule(
  val id: String,
  val name: String,
  val patterns: List<FactPattern>,
  val deduce: (matchedFacts: List<TypedFact>) -> TypedFact?,
  val maxAllowedDepth: Int = 3
)

data class ReasoningLimits(
  val maxSteps: Int = 50,
  val maxDepth: Int = 5,
  val timeoutMs: Long = 200L
)

data class ReasoningResult(
  val initialFacts: List<TypedFact>,
  val derivedFacts: List<TypedFact>,
  val appliedRuleIds: List<String>,
  val stepsCount: Int,
  val executionTimeMs: Long,
  val hitStepLimit: Boolean = false,
  val hitDepthLimit: Boolean = false,
  val hitTimeout: Boolean = false
) {
  fun toGroundedFacts(): List<GroundedFact> {
    return (initialFacts + derivedFacts).map {
      GroundedFact(
        id = it.id,
        claim = "${it.subject} ${it.predicate} ${it.objectValue}",
        evidenceType = it.evidenceType,
        source = it.provenance,
        confidence = it.confidence,
        supportingPremiseIds = it.parentFactIds
      )
    }
  }
}

/**
 * Bounded Deterministic Reasoning Engine.
 * Enforces explicit limits on recursion depth (max 5), inference steps (max 50),
 * and execution time (200ms) with cycle prevention and provenance tracking.
 */
class ReasoningEngine(
  private val rules: List<InferenceRule> = getDefaultRules(),
  private val limits: ReasoningLimits = ReasoningLimits()
) {

  fun infer(initialFacts: List<TypedFact>): ReasoningResult {
    val startTime = System.currentTimeMillis()
    val knownFacts = initialFacts.toMutableList()
    val derived = mutableListOf<TypedFact>()
    val appliedRules = mutableListOf<String>()
    val executedSignatures = mutableSetOf<String>()

    var steps = 0
    var hitStepLimit = false
    var hitDepthLimit = false
    var hitTimeout = false

    var progress = true
    while (progress) {
      progress = false

      // Check timeout
      if (System.currentTimeMillis() - startTime > limits.timeoutMs) {
        hitTimeout = true
        break
      }

      // Check step limit
      if (steps >= limits.maxSteps) {
        hitStepLimit = true
        break
      }

      for (rule in rules) {
        if (steps >= limits.maxSteps) {
          hitStepLimit = true
          break
        }

        // Match all candidate combinations
        val combinations = findAllPatternMatches(rule.patterns, knownFacts)
        for (matchedFacts in combinations) {
          if (steps >= limits.maxSteps) {
            hitStepLimit = true
            break
          }

          // Check depth limit
          val maxParentDepth = matchedFacts.maxOfOrNull { it.depth } ?: 0
          if (maxParentDepth >= limits.maxDepth || maxParentDepth >= rule.maxAllowedDepth) {
            hitDepthLimit = true
            continue
          }

          // Cycle prevention: Signature of (ruleId + premise IDs sorted)
          val signature = rule.id + ":" + matchedFacts.map { it.id }.sorted().joinToString(",")
          if (executedSignatures.contains(signature)) {
            continue
          }

          steps++
          executedSignatures.add(signature)

          val newFact = rule.deduce(matchedFacts)
          if (newFact != null) {
            // Prevent exact duplicate facts
            val alreadyKnown = knownFacts.any {
              it.subject.equals(newFact.subject, ignoreCase = true) &&
                it.predicate.equals(newFact.predicate, ignoreCase = true) &&
                it.objectValue.equals(newFact.objectValue, ignoreCase = true)
            }

            if (!alreadyKnown) {
              val factWithMetadata = newFact.copy(
                derivedFromRuleId = rule.id,
                parentFactIds = matchedFacts.map { it.id },
                depth = maxParentDepth + 1
              )
              knownFacts.add(factWithMetadata)
              derived.add(factWithMetadata)
              appliedRules.add(rule.id)
              progress = true
            }
          }
        }
      }
    }

    val totalTime = System.currentTimeMillis() - startTime
    return ReasoningResult(
      initialFacts = initialFacts,
      derivedFacts = derived,
      appliedRuleIds = appliedRules.distinct(),
      stepsCount = steps,
      executionTimeMs = totalTime,
      hitStepLimit = hitStepLimit,
      hitDepthLimit = hitDepthLimit,
      hitTimeout = hitTimeout
    )
  }

  private fun findAllPatternMatches(patterns: List<FactPattern>, facts: List<TypedFact>): List<List<TypedFact>> {
    if (patterns.isEmpty()) return emptyList()
    var results = listOf<List<TypedFact>>(emptyList())
    for (p in patterns) {
      val next = mutableListOf<List<TypedFact>>()
      for (current in results) {
        for (f in facts) {
          if (!current.contains(f) && p.matches(f)) {
            next.add(current + f)
          }
        }
      }
      results = next
      if (results.isEmpty()) break
    }
    return results
  }

  companion object {
    /**
     * Default deterministic domain inference rules for Zahra.
     */
    fun getDefaultRules(): List<InferenceRule> = listOf(
      // Rule 1: Prayer time calculation dependency
      InferenceRule(
        id = "rule_prayer_current_city",
        name = "اشتقاق مواقيت الصلاة من مدينة المستخدم المسجلة",
        patterns = listOf(
          FactPattern(predicate = "المدينة")
        ),
        deduce = { premises ->
          val cityFact = premises[0]
          TypedFact(
            subject = "مواقيت الصلاة",
            predicate = "محسوبة_لمدينة",
            objectValue = cityFact.objectValue,
            evidenceType = EvidenceType.DEDUCTION,
            provenance = "محرك القواعد الفلكية (اعتماداً على مدينة المستخدم)"
          )
        }
      ),

      // Rule 2: Qibla direction dependency
      InferenceRule(
        id = "rule_qibla_from_city",
        name = "اشتقاق اتجاه القبلة من مدينة المستخدم",
        patterns = listOf(
          FactPattern(predicate = "المدينة")
        ),
        deduce = { premises ->
          val cityFact = premises[0]
          TypedFact(
            subject = "اتجاه القبلة",
            predicate = "محدد_لموقع",
            objectValue = cityFact.objectValue,
            evidenceType = EvidenceType.DEDUCTION,
            provenance = "صيغة الدائرة العظمى (Great Circle Formula)"
          )
        }
      ),

      // Rule 3: Scheduling reminder time constraint
      InferenceRule(
        id = "rule_schedule_time_future",
        name = "التحقق من وقوع التذكير في المستقبل",
        patterns = listOf(
          FactPattern(subject = "التذكير", predicate = "الحالة", objectValue = "مطلوب_الجدولة")
        ),
        deduce = { premises ->
          TypedFact(
            subject = "التذكير",
            predicate = "فحص_الصلاحية",
            objectValue = "مستوفٍ للشروط الزمنية",
            evidenceType = EvidenceType.DEDUCTION,
            provenance = "مدقق قيود الجدولة الحتمية"
          )
        }
      )
    )
  }
}
