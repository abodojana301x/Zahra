package com.example.zahra.understanding

import com.example.zahra.core.ZahraNormalizer
import com.example.zahra.model.FactCategory
import com.example.zahra.model.ZahraResponse

data class DialogTurn(
  val userQuery: String,
  val response: ZahraResponse,
  val timestamp: Long = System.currentTimeMillis()
)

sealed interface CorrectionResolution {
  data class UpdateFact(val category: FactCategory, val key: String, val newValue: String) : CorrectionResolution
  data class RefinedQuery(val reformulatedQuery: String, val reason: String) : CorrectionResolution
  data class UndoAction(val actionDescription: String) : CorrectionResolution
  object None : CorrectionResolution
}

/**
 * Multi-turn Dialog Context and User Correction Tracker.
 * Recognizes corrections ("لا مش كده", "اسمي أحمد مش محمد", "تراجع")
 * and resolves follow-up references using recent session context.
 */
class ContextAndCorrectionTracker {

  private val recentTurns = mutableListOf<DialogTurn>()
  private val maxTurns = 5

  @Synchronized
  fun recordTurn(query: String, response: ZahraResponse) {
    recentTurns.add(DialogTurn(query, response))
    if (recentTurns.size > maxTurns) {
      recentTurns.removeAt(0)
    }
  }

  @Synchronized
  fun getLastTurn(): DialogTurn? = recentTurns.lastOrNull()

  @Synchronized
  fun clear() {
    recentTurns.clear()
  }

  /**
   * Analyzes an incoming query against the immediate previous turn to detect corrections or follow-up references.
   */
  fun detectCorrectionOrFollowup(rawInput: String): CorrectionResolution {
    val clean = rawInput.trim()
    val norm = ZahraNormalizer.normalize(clean)
    val last = getLastTurn()

    // 1. Explicit undo / cancel:
    if (norm == "تراجع" || norm == "الغي ده" || norm == "تراجع عن ذلك" || norm == "الغ الاخير") {
      val desc = last?.response?.text?.take(40) ?: "الإجراء الأخير"
      return CorrectionResolution.UndoAction(desc)
    }

    // 2. Fact correction pattern: "لا، أنا اسمي [X] مش [Y]" or "اسمي مش [Y]، اسمي [X]"
    val nameCorrectionMatch = Regex("""(?:اسمي\s+مش|لست|مش|لا\s+انا\s+اسمي|لا\s+اسمي)\s+([^\s،,]+)\s*(?:بل|مش|ولكن|انما|,|،)?\s*(?:اسمي)?\s*([^\s،,]+)?""").find(norm)
    if (nameCorrectionMatch != null) {
      val g1 = nameCorrectionMatch.groupValues.getOrNull(1) ?: ""
      val g2 = nameCorrectionMatch.groupValues.getOrNull(2) ?: ""
      // Determine which one is the new name
      val newName = if (g2.isNotBlank() && g2 != "مش") g2 else g1
      if (newName.isNotBlank() && newName.length >= 2) {
        return CorrectionResolution.UpdateFact(FactCategory.PERSONAL, "الاسم", newName)
      }
    }

    // Direct name update: "اسمي الجديد خالد" or "عدل اسمي الى خالد"
    val directNameUpdate = Regex("""(?:عدل\s+اسمي\s+(?:الى|إلى|الي)|اسمي\s+الجديد)\s+(.+)""").find(norm)
    if (directNameUpdate != null) {
      val newName = directNameUpdate.groupValues[1].trim()
      if (newName.isNotBlank()) {
        return CorrectionResolution.UpdateFact(FactCategory.PERSONAL, "الاسم", newName)
      }
    }

    // 3. User negation / refinement of previous turn:
    // e.g. "لا مش كده، قصدي..." or "قصدي احسب 100 * 2"
    if (norm.startsWith("قصدي ") || norm.startsWith("لا قصدي ") || norm.startsWith("لا، قصدي ") ||
        norm.startsWith("اقصد ") || norm.startsWith("لا اقصد ")) {
      val refined = clean.replace(Regex("""^(?:لا\s*[,،]?\s*)?(?:قصدي|اقصد)\s*"""), "").trim()
      if (refined.isNotBlank()) {
        return CorrectionResolution.RefinedQuery(refined, "تصحيح لمقصد المستخدم بناءً على اعتراضه")
      }
    }

    // 4. Follow-up query references:
    // "وماذا عن [س]؟" or "طب و [س]؟"
    if ((norm.startsWith("وماذا عن ") || norm.startsWith("طب و ") || norm.startsWith("ماذا عن ")) && last != null) {
      val subject = clean.replace(Regex("""^(?:وماذا\s+عن|طب\s+و|ماذا\s+عن)\s*"""), "").trim()
      if (subject.isNotBlank()) {
        val lastIntent = last.response.intent
        val reformulated = when (lastIntent) {
          "query_knowledge", "define_word" -> "معلومات عن $subject"
          "ask_religion" -> "حكم $subject"
          "translate" -> "ترجم $subject"
          else -> subject
        }
        return CorrectionResolution.RefinedQuery(reformulated, "متابعة لنفس سياق السؤال السابق ($lastIntent)")
      }
    }

    // 5. Follow-up summarization: "لخصه" / "لخص الرد"
    if ((norm == "لخصه" || norm == "لخص هذا" || norm == "لخص الرد") && last != null) {
      val textToSummarize = last.response.text
      if (textToSummarize.length >= 20) {
        return CorrectionResolution.RefinedQuery("لخص $textToSummarize", "طلب تلخيص لمخرجات الرد السابق")
      }
    }

    return CorrectionResolution.None
  }
}
