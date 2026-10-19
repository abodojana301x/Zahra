package com.example.zahra.understanding

import com.example.zahra.core.ZahraNormalizer
import com.example.zahra.entities.EntityExtractor
import com.example.zahra.model.ClarificationOption
import com.example.zahra.model.ClarificationPayload

/**
 * Result of ambiguity analysis.
 */
sealed interface AmbiguityCheckResult {
  object Clear : AmbiguityCheckResult
  data class Ambiguous(val payload: ClarificationPayload) : AmbiguityCheckResult
}

/**
 * Ambiguity and Clarification Detector.
 * When multiple interpretations are plausible or required parameters are missing,
 * produces a concise clarification question with 2-3 button options instead of guessing.
 */
object AmbiguityAndClarificationDetector {

  fun check(rawInput: String, normalizedInput: String, candidateIntent: String): AmbiguityCheckResult {
    val clean = rawInput.trim()
    val norm = normalizedInput.trim()

    // 1. Incomplete unit conversion:
    // e.g. "حول 10 كم" (missing destination unit) or "حول 50" (missing both units)
    if (candidateIntent == "convert_unit" || norm.startsWith("حول ") || norm.startsWith("تحويل ")) {
      val parsed = EntityExtractor.parseUnitRequest(norm)
      if (parsed == null || parsed.toUnit.isBlank()) {
        val numMatch = Regex("""\b(\d+(?:\.\d+)?)\b""").find(norm)
        val valueStr = numMatch?.groupValues?.get(1) ?: "10"
        val fromUnit = when {
          norm.contains("كم") || norm.contains("كيلومتر") -> "كم"
          norm.contains("ميل") -> "ميل"
          norm.contains("كيلو") || norm.contains("كجم") -> "كيلو"
          norm.contains("باوند") || norm.contains("رطل") -> "باوند"
          norm.contains("متر") -> "متر"
          else -> null
        }

        if (fromUnit == "كم") {
          return AmbiguityCheckResult.Ambiguous(
            ClarificationPayload(
              question = "إلى أي وحدة تود تحويل $valueStr كم؟",
              options = listOf(
                ClarificationOption("to_miles", "إلى ميل", "حول $valueStr كم إلى ميل"),
                ClarificationOption("to_meters", "إلى أمتار", "حول $valueStr كم إلى متر"),
                ClarificationOption("cancel", "إلغاء", "إلغاء")
              ),
              originalQuery = clean,
              reason = "الوحدة المستهدفة غير محددة"
            )
          )
        } else if (fromUnit == "كيلو") {
          return AmbiguityCheckResult.Ambiguous(
            ClarificationPayload(
              question = "إلى أي وحدة تود تحويل $valueStr كجم؟",
              options = listOf(
                ClarificationOption("to_lbs", "إلى باوند (رطل)", "حول $valueStr كجم إلى باوند"),
                ClarificationOption("to_grams", "إلى جرامات", "حول $valueStr كجم إلى جرام"),
                ClarificationOption("cancel", "إلغاء", "إلغاء")
              ),
              originalQuery = clean,
              reason = "الوحدة المستهدفة غير محددة"
            )
          )
        } else if (fromUnit == null) {
          return AmbiguityCheckResult.Ambiguous(
            ClarificationPayload(
              question = "ما هي الوحدات التي تود التحويل بينها؟",
              options = listOf(
                ClarificationOption("km_to_mi", "$valueStr كم إلى ميل", "حول $valueStr كم إلى ميل"),
                ClarificationOption("kg_to_lb", "$valueStr كجم إلى باوند", "حول $valueStr كجم إلى باوند"),
                ClarificationOption("c_to_f", "$valueStr مئوية إلى فهرنهايت", "حول $valueStr مئوي إلى فهرنهايت")
              ),
              originalQuery = clean,
              reason = "الوحدات غير محددة في طلب التحويل"
            )
          )
        }
      }
    }

    // 2. Incomplete scheduling request (action present, but time/delay omitted):
    // e.g. "فكرني بكرة بموعد الطبيب", "ذكرني بالاجتماع", "نبهني بالصلاة"
    val isScheduleCommand = norm.contains("فكرني") || norm.contains("ذكرني") || norm.contains("جدول لي") || norm.contains("نبهني")
    if (candidateIntent == "create_schedule" || isScheduleCommand) {
      val parsedTime = EntityExtractor.parseTimeOrDelay(norm)
      if (parsedTime == null) {
        val topic = clean.replace(Regex("""(?:فكرني|ذكرني|جدول\s*لي|نبهني)\s*(?:بـ|ب)?\s*"""), "").trim()
        val displayTopic = if (topic.isNotBlank()) "بـ \"$topic\"" else "بهذا الأمر"
        return AmbiguityCheckResult.Ambiguous(
          ClarificationPayload(
            question = "يرجى تحديد وقت التنبيه أو وقت واضح للتذكير: في أي وقت تود أن أذكرك $displayTopic؟",
            options = listOf(
              ClarificationOption("time_morning", "الساعة 9:00 صباحاً", "فكرني $displayTopic الساعة 9:00 صباحاً"),
              ClarificationOption("time_evening", "الساعة 5:00 مساءً", "فكرني $displayTopic الساعة 5:00 مساءً"),
              ClarificationOption("time_1hr", "بعد ساعة من الآن", "فكرني $displayTopic بعد ساعة")
            ),
            originalQuery = clean,
            reason = "وقت التذكير غير محدد"
          )
        )
      }
    }

    // 3. Ambiguous action between Task and Note:
    // e.g. "سجل فكرة التطبيق" or "احفظ موضوع التقرير"
    val isAmbiguousRecord = (norm.startsWith("سجل ") || norm.startsWith("احفظ ")) &&
      !norm.contains("مهمة") && !norm.contains("ملاحظة") && !norm.contains("تذكر") && !norm.contains("جدول")
    if (isAmbiguousRecord) {
      val item = clean.replace(Regex("""^(?:سجل|احفظ)\s*"""), "").trim()
      return AmbiguityCheckResult.Ambiguous(
        ClarificationPayload(
          question = "هل ترغب في حفظ \"$item\" كمهمة تنفيذية أم كملاحظة دائمة؟",
          options = listOf(
            ClarificationOption("as_task", "إضافة كمهمة ✅", "أضف مهمة $item"),
            ClarificationOption("as_note", "حفظ كملاحظة 📝", "سجل ملاحظة $item"),
            ClarificationOption("cancel", "إلغاء", "إلغاء")
          ),
          originalQuery = clean,
          reason = "التمييز بين المهمة والملاحظة"
        )
      )
    }

    // 4. Incomplete math expression:
    if (candidateIntent == "calculate" || norm.startsWith("احسب ")) {
      val expr = clean.replace(Regex("""^(?:احسب|كم\s+يساوي|calc)\s*""", RegexOption.IGNORE_CASE), "").trim()
      if (expr.isBlank() || expr.matches(Regex("""^[+\-*/^().\s%]+$""")) || expr.endsWith("+") || expr.endsWith("-") || expr.endsWith("*") || expr.endsWith("/")) {
        return AmbiguityCheckResult.Ambiguous(
          ClarificationPayload(
            question = "المعادلة الرياضية غير مكتملة. يرجى تزويدي بالطرف الآخر للحساب:",
            options = listOf(
              ClarificationOption("math_eg1", "مثال: 50 * 4", "احسب 50 * 4"),
              ClarificationOption("math_eg2", "مثال: (100 + 20) / 4", "احسب (100 + 20) / 4"),
              ClarificationOption("cancel", "إلغاء", "إلغاء")
            ),
            originalQuery = clean,
            reason = "المعامل الرياضي غير مكتمل"
          )
        )
      }
    }

    return AmbiguityCheckResult.Clear
  }
}
