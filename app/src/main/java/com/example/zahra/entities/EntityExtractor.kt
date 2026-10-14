package com.example.zahra.entities

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class ParsedDate(
  val formattedDate: String,
  val timestamp: Long,
  val rawExpression: String
)

data class ParsedUnitConversion(
  val value: Double,
  val fromUnit: String,
  val toUnit: String
)

object EntityExtractor {
  private val DAYS_MAP = mapOf(
    "الاحد" to Calendar.SUNDAY, "الأحد" to Calendar.SUNDAY,
    "الاثنين" to Calendar.MONDAY, "الإثنين" to Calendar.MONDAY,
    "الثلاثاء" to Calendar.TUESDAY,
    "الاربعاء" to Calendar.WEDNESDAY, "الأربعاء" to Calendar.WEDNESDAY,
    "الخميس" to Calendar.THURSDAY,
    "الجمعة" to Calendar.FRIDAY,
    "السبت" to Calendar.SATURDAY
  )

  fun parseRelativeDate(text: String): ParsedDate? {
    val clean = text.lowercase()
    val cal = Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

    if (clean.contains("اليوم")) {
      return ParsedDate(sdf.format(cal.time), cal.timeInMillis, "اليوم")
    }

    if (clean.contains("بكرة") || clean.contains("غدا") || clean.contains("غداً") || clean.contains("للغد") || clean.contains("غد")) {
      cal.add(Calendar.DAY_OF_YEAR, 1)
      return ParsedDate(sdf.format(cal.time), cal.timeInMillis, "غداً")
    }

    if (clean.contains("بعد بكرة") || clean.contains("بعد غد")) {
      cal.add(Calendar.DAY_OF_YEAR, 2)
      return ParsedDate(sdf.format(cal.time), cal.timeInMillis, "بعد غد")
    }

    if (clean.contains("بعد أسبوع") || clean.contains("بعد اسبوع")) {
      cal.add(Calendar.DAY_OF_YEAR, 7)
      return ParsedDate(sdf.format(cal.time), cal.timeInMillis, "بعد أسبوع")
    }

    for ((dayName, targetDay) in DAYS_MAP) {
      if (clean.contains(dayName)) {
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        var diff = targetDay - currentDay
        if (diff <= 0) diff += 7
        cal.add(Calendar.DAY_OF_YEAR, diff)
        return ParsedDate(sdf.format(cal.time), cal.timeInMillis, dayName)
      }
    }

    return null
  }

  fun parseUnitRequest(text: String): ParsedUnitConversion? {
    val regex = Regex("(?:حول|تحويل|convert)?\\s*(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z\\u0621-\\u064A]+)(?:\\s*(?:إلى|الى|to)\\s*([a-zA-Z\\u0621-\\u064A]+))?", RegexOption.IGNORE_CASE)
    val match = regex.find(text) ?: return null

    val value = match.groupValues[1].toDoubleOrNull() ?: return null
    val from = match.groupValues[2].lowercase()
    val to = match.groupValues.getOrNull(3)?.lowercase() ?: ""

    val normalizedFrom = normalizeUnit(from)
    val normalizedTo = normalizeUnit(to)

    return ParsedUnitConversion(value, normalizedFrom, normalizedTo)
  }

  private fun normalizeUnit(raw: String): String {
    return when (raw) {
      "كيلو", "كيلوغرام", "كجم", "kg" -> "kg"
      "باوند", "رطل", "lb" -> "lb"
      "كيلومتر", "كم", "km" -> "km"
      "ميل", "mi" -> "mi"
      "متر", "m" -> "m"
      "قدم", "ft" -> "ft"
      "مئوية", "سيليزيوس", "c" -> "C"
      "فهرنهايت", "f" -> "F"
      "لتر", "l" -> "L"
      "جالون", "gal" -> "gal"
      "دولار", "usd" -> "USD"
      "ريال", "sar" -> "SAR"
      "جنيه", "egp" -> "EGP"
      "درهم", "aed" -> "AED"
      "دينار", "kwd" -> "KWD"
      "يورو", "eur" -> "EUR"
      else -> raw
    }
  }
}
