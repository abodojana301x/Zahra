package com.example.zahra.understanding

import com.example.zahra.core.ZahraNormalizer
import java.util.Locale

/**
 * Result of dialect adaptation and conversational extraction.
 */
data class AdaptedQuery(
  val rawInput: String,
  val coreQuery: String,
  val detectedDialect: String, // "egyptian_colloquial", "msa", "english", "mixed"
  val strippedPrefixes: List<String> = emptyList(),
  val inferredIntentOverride: String? = null
)

/**
 * Egyptian Arabic and Modern Standard Arabic Dialect Adapter.
 * Normalizes common conversational colloquialisms, politeness markers,
 * questions, and handles English/mixed commands without destroying semantic meaning.
 */
object DialectAndColloquialAdapter {

  private val CONVERSATIONAL_PREFIXES = listOf(
    "بقولك ايه", "بقولك إيه", "معلش", "لو سمحت", "من فضلك",
    "يا ريت", "ياريت", "ممكن بعد اذنك", "ممكن بعد إذنك", "ممكن",
    "طب تمام", "طب", "طيب", "طب معلش", "بالله عليك", "عايزك",
    "عاوزك", "نفسي", "ودى", "ودي", "حبذا لو"
  )

  private val INTENT_TRIGGER_PREFIXES = listOf(
    Regex("^(?:عايز\\s+اعرف|عاوز\\s+اعرف|حابب\\s+اعرف|ودّي\\s+اعرف|نفسي\\s+اعرف|قولي\\s+كده|قولي)\\s+", RegexOption.IGNORE_CASE) to "query_knowledge",
    Regex("^(?:هو\\s+يعني\\s+ايه|هو\\s+ايه|معنى\\s+ايه)\\s+", RegexOption.IGNORE_CASE) to "define_word",
    Regex("^(?:احسبلي|احسب\\s+لي|معلش\\s+احسب|ممكن\\s+تحسب)\\s+", RegexOption.IGNORE_CASE) to "calculate",
    Regex("^(?:فكرني\\s+كده|ذكرني\\s+بالله\\s+عليك|ممكن\\s+تفكرني)\\s+", RegexOption.IGNORE_CASE) to "create_schedule",
    Regex("^(?:ترجملي|ترجم\\s+لي|ممكن\\s+ترجمة)\\s+", RegexOption.IGNORE_CASE) to "translate",
    Regex("^(?:لخصلي|لخص\\s+لي|ممكن\\s+تلخيص)\\s+", RegexOption.IGNORE_CASE) to "summarize",
    Regex("^(?:سجلي\\s+مهمة|ضيفلي\\s+مهمة|أضفلي\\s+مهمة)\\s+", RegexOption.IGNORE_CASE) to "create_task"
  )

  // English command mapping
  private val ENGLISH_COMMAND_REGEXES = listOf(
    Regex("""^(?:calc|calculate)\s+([0-9+\-*/^().\s%]+)$""", RegexOption.IGNORE_CASE) to "calculate",
    Regex("""^(?:convert)\s+(.+?)\s+(?:to|into)\s+(.+)$""", RegexOption.IGNORE_CASE) to "convert_unit",
    Regex("""^(?:translate)\s+(.+?)(?:\s+(?:to|into)\s+(.+))?$""", RegexOption.IGNORE_CASE) to "translate",
    Regex("""^(?:summarize)\s+(.+)$""", RegexOption.IGNORE_CASE) to "summarize",
    Regex("""^(?:add\s+task|new\s+task|todo:?)\s*(.+)$""", RegexOption.IGNORE_CASE) to "create_task",
    Regex("""^(?:remind\s+me|schedule|set\s+alarm)\s*(.+)$""", RegexOption.IGNORE_CASE) to "create_schedule",
    Regex("""^(?:what\s+is|who\s+is|define)\s+(.+)$""", RegexOption.IGNORE_CASE) to "query_knowledge",
    Regex("""^(?:tasks|list\s+tasks|my\s+tasks)$""", RegexOption.IGNORE_CASE) to "list_tasks",
    Regex("""^(?:notes|list\s+notes|my\s+notes)$""", RegexOption.IGNORE_CASE) to "list_notes",
    Regex("""^(?:schedules|alarms|list\s+schedules)$""", RegexOption.IGNORE_CASE) to "list_schedules"
  )

  fun adapt(rawInput: String): AdaptedQuery {
    val clean = rawInput.trim()
    if (clean.isBlank()) {
      return AdaptedQuery(rawInput, "", "msa")
    }

    // Check for pure or mixed English commands
    val hasEnglish = clean.any { it in 'a'..'z' || it in 'A'..'Z' }
    val hasArabic = clean.any { it in '\u0600'..'\u06FF' }

    if (hasEnglish && !hasArabic) {
      for ((pattern, intent) in ENGLISH_COMMAND_REGEXES) {
        val m = pattern.find(clean)
        if (m != null) {
          val core = if (intent == "convert_unit") clean else if (m.groupValues.size > 1) m.groupValues[1].trim() else clean
          return AdaptedQuery(
            rawInput = clean,
            coreQuery = core,
            detectedDialect = "english",
            inferredIntentOverride = intent
          )
        }
      }
      return AdaptedQuery(clean, clean, "english")
    }

    // Arabic / Egyptian processing
    var text = clean
    val stripped = mutableListOf<String>()
    var intentOverride: String? = null

    // 1. Strip conversational filler prefixes
    var changed = true
    while (changed) {
      changed = false
      for (prefix in CONVERSATIONAL_PREFIXES) {
        val prefixRegex = Regex("^" + Regex.escape(prefix) + "\\s*[,،]?\\s*", RegexOption.IGNORE_CASE)
        val m = prefixRegex.find(text)
        if (m != null) {
          stripped.add(prefix)
          text = text.substring(m.range.last + 1).trim()
          changed = true
          break
        }
      }
    }

    // 2. Check for intent trigger prefixes
    for ((regex, intent) in INTENT_TRIGGER_PREFIXES) {
      val m = regex.find(text)
      if (m != null) {
        intentOverride = intent
        text = text.substring(m.range.last + 1).trim()
        break
      }
    }

    // 3. Dialect detection
    val isEgyptian = stripped.any { it in listOf("بقولك ايه", "معلش", "طب", "عايزك", "عاوزك", "قولي كده") } ||
      text.contains("عايز") || text.contains("عاوز") || text.contains("كده") || text.contains("مش") || text.contains("ازيك")

    val dialect = when {
      hasEnglish && hasArabic -> "mixed"
      isEgyptian -> "egyptian_colloquial"
      else -> "msa"
    }

    return AdaptedQuery(
      rawInput = clean,
      coreQuery = text.ifBlank { clean },
      detectedDialect = dialect,
      strippedPrefixes = stripped,
      inferredIntentOverride = intentOverride
    )
  }
}
