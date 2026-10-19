package com.example.zahra.reasoning

import com.example.zahra.model.MemorySource
import com.example.zahra.model.ZahraUserFact

enum class ConflictType {
  NONE,
  DIRECT_VALUE_MISMATCH,
  SOURCE_PRIORITY_OVERRIDE,
  TEMPORAL_INCONSISTENCY
}

enum class ResolutionStrategy {
  PRESERVE_EXISTING,
  OVERRIDE_WITH_HIGHER_PRIORITY,
  REQUEST_USER_CLARIFICATION,
  RECORD_HISTORICAL_UPDATE
}

data class ContradictionReport(
  val hasConflict: Boolean,
  val conflictType: ConflictType,
  val key: String,
  val existingValue: String? = null,
  val existingSource: MemorySource? = null,
  val newValue: String? = null,
  val newSource: MemorySource? = null,
  val resolution: ResolutionStrategy = ResolutionStrategy.PRESERVE_EXISTING,
  val explanationAr: String = ""
)

/**
 * Contradiction and Uncertainty Management Engine.
 * Detects conflicts between active memories and incoming claims,
 * enforces an explicit source-priority hierarchy, considers freshness,
 * and preserves provenance without silently fabricating or corrupting records.
 */
object ContradictionDetector {

  private fun getSourcePriority(source: MemorySource): Int {
    return when (source) {
      MemorySource.USER_EXPLICIT -> 3
      MemorySource.USER_UI_MANUAL -> 3
      MemorySource.INFERRED_OBSERVED -> 1
    }
  }

  fun checkFactConflict(
    existingFacts: List<ZahraUserFact>,
    newKey: String,
    newValue: String,
    newSource: MemorySource,
    isExplicitCorrection: Boolean = false
  ): ContradictionReport {
    val cleanKey = newKey.trim()
    val cleanValue = newValue.trim()

    val match = existingFacts.find { it.key.equals(cleanKey, ignoreCase = true) }
      ?: return ContradictionReport(false, ConflictType.NONE, cleanKey)

    // Identical value -> No conflict
    if (match.value.equals(cleanValue, ignoreCase = true)) {
      return ContradictionReport(false, ConflictType.NONE, cleanKey)
    }

    val existingPrio = getSourcePriority(match.source)
    val newPrio = getSourcePriority(newSource)

    // Case 1: Inferred observation attempts to overwrite explicit confirmed user record
    if (newPrio < existingPrio) {
      return ContradictionReport(
        hasConflict = true,
        conflictType = ConflictType.SOURCE_PRIORITY_OVERRIDE,
        key = cleanKey,
        existingValue = match.value,
        existingSource = match.source,
        newValue = cleanValue,
        newSource = newSource,
        resolution = ResolutionStrategy.PRESERVE_EXISTING,
        explanationAr = "تم تفضيل المعلومة المؤكدة صراحة من المستخدم (\"${match.value}\") ورفض الاستنتاج الظني غير المؤكد (\"$cleanValue\")."
      )
    }

    // Case 2: Explicit correction or higher priority
    if (isExplicitCorrection || newPrio > existingPrio) {
      return ContradictionReport(
        hasConflict = true,
        conflictType = ConflictType.DIRECT_VALUE_MISMATCH,
        key = cleanKey,
        existingValue = match.value,
        existingSource = match.source,
        newValue = cleanValue,
        newSource = newSource,
        resolution = ResolutionStrategy.RECORD_HISTORICAL_UPDATE,
        explanationAr = "تم تحديث \"$cleanKey\" من \"${match.value}\" إلى \"$cleanValue\" بناءً على تأكيدك الصريح، مع الاحتفاظ بالسجل التاريخي."
      )
    }

    // Case 3: Same high priority, but different values (e.g. User said "اسمي محمد" yesterday, now says "اسمي علي" without saying "عدل" or "أقصد")
    return ContradictionReport(
      hasConflict = true,
      conflictType = ConflictType.DIRECT_VALUE_MISMATCH,
      key = cleanKey,
      existingValue = match.value,
      existingSource = match.source,
      newValue = cleanValue,
      newSource = newSource,
      resolution = ResolutionStrategy.RECORD_HISTORICAL_UPDATE,
      explanationAr = "تم تسجيل القيمة الأحدث (\"$cleanValue\") لـ \"$cleanKey\" بدلاً من السابقة (\"${match.value}\")."
    )
  }
}
