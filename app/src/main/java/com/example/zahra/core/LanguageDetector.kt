package com.example.zahra.core

enum class DetectedLang {
  AR,
  EN,
  FR,
  FRANCO,
  MIXED,
  UNKNOWN
}

data class LangResult(
  val language: DetectedLang,
  val script: String,
  val confidence: Double
)

object LanguageDetector {
  private val ARABIC_REGEX = Regex("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]")
  private val LATIN_REGEX = Regex("[a-zA-Z\\u00C0-\\u00FF]")
  private val FRENCH_ACCENTS = Regex("[éèêëàâçîïôûùüœæ]", RegexOption.IGNORE_CASE)
  private val FRENCH_STOPWORDS = Regex("\\b(le|la|les|un|une|des|du|de|et|est|en|que|qui|dans)\\b", RegexOption.IGNORE_CASE)

  fun detect(text: String?): LangResult {
    if (text.isNullOrBlank()) {
      return LangResult(DetectedLang.UNKNOWN, "None", 0.0)
    }

    val arCount = ARABIC_REGEX.findAll(text).count()
    val latinCount = LATIN_REGEX.findAll(text).count()
    val total = arCount + latinCount

    if (total == 0) {
      return LangResult(DetectedLang.UNKNOWN, "NumericOrSymbol", 0.5)
    }

    val arRatio = arCount.toDouble() / total
    val latinRatio = latinCount.toDouble() / total

    if (arRatio > 0.75) {
      return LangResult(DetectedLang.AR, "Arabic", (0.7 + arRatio * 0.3).coerceAtMost(1.0))
    }

    if (latinRatio > 0.75) {
      val francoKeywords = listOf("3amel", "ezayak", "shokran", "habibi", "enta", "ana", "saba7", "tamam", "keda", "khalas", "inshallah", "fakkarni", "jadwel", "mesh", "leh", "koll")
      val hasFrancoDigits = Regex("""[a-zA-Z]+[235789]|[235789]+[a-zA-Z]+""").containsMatchIn(text)
      val hasFrancoWords = francoKeywords.any { text.lowercase().contains(it) }

      if (hasFrancoDigits || hasFrancoWords) {
        return LangResult(DetectedLang.FRANCO, "Franco-Arabic", 0.92)
      }

      val isFrench = FRENCH_ACCENTS.containsMatchIn(text) || FRENCH_STOPWORDS.containsMatchIn(text)
      return LangResult(
        if (isFrench) DetectedLang.FR else DetectedLang.EN,
        "Latin",
        (0.7 + latinRatio * 0.3).coerceAtMost(1.0)
      )
    }

    return LangResult(DetectedLang.MIXED, "Mixed", 0.6)
  }
}
