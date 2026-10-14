package com.example.zahra.intent

data class RegexMatch(
  val intent: String,
  val confidence: Double,
  val extractedCommand: String? = null
)

object RegexMatcher {
  private data class PatternRule(
    val intent: String,
    val regex: Regex,
    val confidence: Double,
    val extractGroup: Int? = null
  )

  private val RULES = listOf(
    // 1. Math calculation
    PatternRule("calculate", Regex("^(?:احسب|كم\\s+يساوي|ناتج|calc(?:ulate)?)\\s+([0-9+\\-*/^().\\s%]+)", RegexOption.IGNORE_CASE), 0.98, 1),
    // 2. Unit conversion
    PatternRule("convert_unit", Regex("^(?:حول|تحويل|convert)\\s+(.+?)\\s+(?:الى|إلى|to)\\s+(.+)", RegexOption.IGNORE_CASE), 0.95),
    // 3. Translation
    PatternRule("translate", Regex("^(?:ترجم(?:لي)?|ما\\s+ترجمة|translate)\\s+(.+?)(?:\\s+(?:الى|إلى|to)\\s+(.+))?$", RegexOption.IGNORE_CASE), 0.95, 1),
    // 4. Summarization
    PatternRule("summarize", Regex("^(?:لخص(?:لي)?|تلخيص|أعطني\\s+ملخص|summarize)\\s+(.+)", RegexOption.DOT_MATCHES_ALL), 0.95, 1),
    // 5. Note creation
    PatternRule("remember_note", Regex("^(?:سجل\\s+ملاحظة|اكتب\\s+ملاحظة|احفظ\\s+ملاحظة|تذكر\\s+ان|تذكر\\s+أن|note:?)\\s*(.+)", RegexOption.DOT_MATCHES_ALL), 0.96, 1),
    // 6. Task creation
    PatternRule("create_task", Regex("^(?:اضف\\s+مهمة|أضف\\s+مهمة|انشئ\\s+مهمة|سجل\\s+مهمة|مهمة\\s+جديدة|task:?)\\s*(.+)", RegexOption.DOT_MATCHES_ALL), 0.96, 1),
    // 7. Prayer times
    PatternRule("ask_prayer_time", Regex("^(?:متى\\s+صلاة|مواقيت\\s+الصلاة|موعد\\s+اذان|موعد\\s+أذان|كم\\s+بقي\\s+على\\s+صلاة|prayer\\s+times?)", RegexOption.IGNORE_CASE), 0.96),
    // 8. Qibla
    PatternRule("ask_religion", Regex("^(?:اين\\s+القبلة|أين\\s+القبلة|اتجاه\\s+القبلة|qibla)", RegexOption.IGNORE_CASE), 0.95),
    // 9. Quran
    PatternRule("ask_quran", Regex("^(?:سورة|اية|آية|تفسير\\s+اية|تفسير\\s+سورة|قران|قرآن)\\s+(.+)", RegexOption.IGNORE_CASE), 0.93, 1),
    // 10. Hadith
    PatternRule("ask_hadith", Regex("^(?:حديث|هل\\s+هذا\\s+حديث|تخريج\\s+حديث|قال\\s+رسول\\s+الله)\\s*(.*)", RegexOption.IGNORE_CASE), 0.92),
    // 11. Greetings
    PatternRule("greet", Regex("^(?:السلام\\s+عليكم|مرحبا|أهلا|اهلا|صباح\\s+الخير|مساء\\s+الخير|hi|hello|bonjour)\\b", RegexOption.IGNORE_CASE), 0.95),
    // 12. Farewells
    PatternRule("farewell", Regex("^(?:مع\\s+السلامة|إلى\\s+اللقاء|الى\\s+اللقاء|وداعا|باي|bye|goodbye|au\\s+revoir)\\b", RegexOption.IGNORE_CASE), 0.95),
    // 13. Help
    PatternRule("help", Regex("^(?:ساعدني|مساعدة|ماذا\\s+تستطيعين|كيف\\s+استخدمك|دليل|help)\\b", RegexOption.IGNORE_CASE), 0.95),
    // 14. Definitions / General
    PatternRule("define_word", Regex("^(?:ما\\s+هو|ما\\s+هي|ما\\s+معنى|عرف(?:لي)?|what\\s+is)\\s+(.+)", RegexOption.IGNORE_CASE), 0.92, 1)
  )

  fun match(normalizedText: String): RegexMatch? {
    val clean = normalizedText.trim()
    for (rule in RULES) {
      val m = rule.regex.find(clean)
      if (m != null) {
        val extracted = if (rule.extractGroup != null && m.groupValues.size > rule.extractGroup) {
          m.groupValues[rule.extractGroup].trim()
        } else null
        return RegexMatch(rule.intent, rule.confidence, extracted)
      }
    }
    return null
  }
}
