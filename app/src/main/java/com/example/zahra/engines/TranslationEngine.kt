package com.example.zahra.engines

import android.content.Context
import org.json.JSONObject

data class TransResult(
  val translatedText: String,
  val source: String,
  val confidence: Double
)

class TranslationEngine(context: Context) {
  private val dictionary = mutableListOf<Triple<String, String, String>>() // AR, EN, FR
  private val phrases = mutableListOf<Triple<String, String, String>>()

  init {
    try {
      val jsonStr = context.assets.open("translation_kb.json").bufferedReader().use { it.readText() }
      val root = JSONObject(jsonStr)

      val dictArray = root.optJSONArray("dictionary")
      if (dictArray != null) {
        for (i in 0 until dictArray.length()) {
          val obj = dictArray.getJSONObject(i)
          dictionary.add(
            Triple(
              obj.optString("ar"),
              obj.optString("en"),
              obj.optString("fr")
            )
          )
        }
      }

      val phraseArray = root.optJSONArray("phrases")
      if (phraseArray != null) {
        for (i in 0 until phraseArray.length()) {
          val obj = phraseArray.getJSONObject(i)
          phrases.add(
            Triple(
              obj.optString("ar"),
              obj.optString("en"),
              obj.optString("fr")
            )
          )
        }
      }
    } catch (e: Exception) {
      e.printStackTrace()
    }
  }

  fun translate(text: String, targetLang: String = "en"): TransResult {
    val clean = text.trim()
    if (clean.isBlank()) {
      return TransResult("", "معجم زهرة المدمج", 0.0)
    }

    // 1. Exact phrase lookup
    for (phrase in phrases) {
      if (phrase.first.contains(clean) || phrase.second.equals(clean, ignoreCase = true) || phrase.third.equals(clean, ignoreCase = true)) {
        val out = when (targetLang.lowercase()) {
          "ar" -> phrase.first
          "fr" -> phrase.third
          else -> phrase.second
        }
        return TransResult(out, "جدول العبارات الاصطلاحية المدمج (Phrase Table)", 0.98)
      }
    }

    // 2. Word-by-word with light prefix stripping
    val words = clean.split(Regex("\\s+"))
    val translatedWords = mutableListOf<String>()
    var matchedCount = 0

    for (w in words) {
      val direct = lookupWord(w, targetLang)
      if (direct != null) {
        translatedWords.add(direct)
        matchedCount++
      } else {
        translatedWords.add("[$w]")
      }
    }

    val conf = if (words.isNotEmpty()) (matchedCount.toDouble() / words.size) * 0.85 else 0.2
    return TransResult(
      translatedWords.joinToString(" "),
      "معجم زهرة الثلاثي المدمج (عربي - إنجليزي - فرنسي)",
      (conf * 100).toInt() / 100.0
    )
  }

  private fun lookupWord(word: String, targetLang: String): String? {
    val wLower = word.lowercase()
    for (entry in dictionary) {
      if (entry.first == word || entry.second.lowercase() == wLower || entry.third.lowercase() == wLower) {
        return when (targetLang.lowercase()) {
          "ar" -> entry.first
          "fr" -> entry.third
          else -> entry.second
        }
      }
    }

    // Strip Arabic prefixes: ال، و، ف، ب
    if (word.startsWith("ال") || word.startsWith("وال") || word.startsWith("بال")) {
      val stripped = word.replace(Regex("^(?:وال|بال|فال|ال)"), "")
      for (entry in dictionary) {
        if (entry.first == stripped) {
          val base = when (targetLang.lowercase()) {
            "ar" -> entry.first
            "fr" -> entry.third
            else -> entry.second
          }
          return if (targetLang == "en") "the $base" else base
        }
      }
    }

    return null
  }
}
