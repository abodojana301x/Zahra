package com.example.zahra.reasoning

import com.example.zahra.model.*

/**
 * Epistemic Grounding Pipeline.
 * Segregates raw request, understanding, stored state, applied rules,
 * derived results, uncertainty, and final user-facing response.
 */
object EpistemicGroundingPipeline {

  fun buildTrace(
    rawRequest: String,
    intent: String,
    entities: Map<String, String>,
    appliedRules: List<String>,
    facts: List<GroundedFact>,
    uncertainty: List<String>,
    outcomeState: OutcomeState,
    sourceDesc: String?
  ): EvidenceGroundingTrace {
    val verifiedCount = facts.count { it.evidenceType == EvidenceType.VERIFIED || it.evidenceType == EvidenceType.LOCAL_CURATED }
    val userCount = facts.count { it.evidenceType == EvidenceType.USER_SUPPLIED }
    val deductionCount = facts.count { it.evidenceType == EvidenceType.DEDUCTION }
    val unknownCount = facts.count { it.evidenceType == EvidenceType.UNKNOWN }

    val summary = buildString {
      append("التأصيل المعرفي: ")
      when (outcomeState) {
        OutcomeState.SUPPORTED -> {
          append("مخرجات مؤصلة بالكامل بدون تخمين احتمالي. ")
          if (verifiedCount > 0) append("$verifiedCount حقيقة محققة محلياً، ")
          if (userCount > 0) append("$userCount معطيات من المستخدم، ")
          if (deductionCount > 0) append("$deductionCount استنتاج حتمي.")
        }
        OutcomeState.AMBIGUOUS -> {
          append("طلب ملتبس أو معطيات ناقصة بحاجة إلى توضيح المستخدم.")
        }
        OutcomeState.UNSUPPORTED -> {
          append("امتناع صادق لعدم توفر دليل موثق في قاعدة البيانات المحلية (Truth Before Intelligence).")
        }
        OutcomeState.EXECUTION_ERROR -> {
          append("فشل في استيفاء شروط أو تنفيذ الإجراء المطلوب.")
        }
      }
    }

    return EvidenceGroundingTrace(
      originalRequest = rawRequest,
      detectedIntent = intent,
      extractedEntities = entities,
      appliedRuleIds = appliedRules,
      groundedFacts = facts,
      uncertaintyNotes = uncertainty,
      transparencySummary = summary.trimEnd(',', ' ')
    )
  }
}
