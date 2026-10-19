package com.example.zahra.entities

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

data class ParsedDate(
  val formattedDate: String,
  val timestamp: Long,
  val rawExpression: String,
  val daysOffset: Int = 0
)

data class ParsedTime(
  val hour: Int,
  val minute: Int,
  val timeDisplay: String,
  val isRelativeDelay: Boolean = false,
  val delayMinutes: Int = 0,
  val hasTimeSpecified: Boolean = true,
  val isPm: Boolean = false,
  val isAm: Boolean = false,
  val rawExpression: String = "",
  val targetTimestamp: Long = 0L
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

  /**
   * Parse relative dates in Arabic against an optional controllable reference calendar.
   * Multi-word phrases like "بعد بكرة" and "بعد غد" are tested BEFORE single-word "بكرة" / "غد"
   * to strictly prevent prefix collision defects.
   */
  fun parseRelativeDate(text: String, refCalendar: Calendar? = null): ParsedDate? {
    val clean = text.lowercase(Locale.ROOT)
    val cal = (refCalendar?.clone() as? Calendar) ?: Calendar.getInstance()
    val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    // 1. Check multi-word offsets FIRST to avoid prefix-matching collisions
    if (clean.contains("بعد بكرة") || clean.contains("بعد بكرة") || clean.contains("بعد غد") || clean.contains("بعد غداً")) {
      cal.add(Calendar.DAY_OF_YEAR, 2)
      return ParsedDate(sdf.format(cal.time), cal.timeInMillis, "بعد غد", daysOffset = 2)
    }

    if (clean.contains("بعد أسبوع") || clean.contains("بعد اسبوع")) {
      cal.add(Calendar.DAY_OF_YEAR, 7)
      return ParsedDate(sdf.format(cal.time), cal.timeInMillis, "بعد أسبوع", daysOffset = 7)
    }

    if (clean.contains("امس") || clean.contains("أمس") || clean.contains("امبارح") || clean.contains("البارحة")) {
      cal.add(Calendar.DAY_OF_YEAR, -1)
      return ParsedDate(sdf.format(cal.time), cal.timeInMillis, "أمس", daysOffset = -1)
    }

    // 2. Single-day future relative words
    if (clean.contains("بكرة") || clean.contains("غدا") || clean.contains("غداً") || clean.contains("للغد") || clean.contains("غد")) {
      cal.add(Calendar.DAY_OF_YEAR, 1)
      return ParsedDate(sdf.format(cal.time), cal.timeInMillis, "غداً", daysOffset = 1)
    }

    // 3. Current day
    if (clean.contains("اليوم") || clean.contains("النهارده") || clean.contains("النهاردة")) {
      return ParsedDate(sdf.format(cal.time), cal.timeInMillis, "اليوم", daysOffset = 0)
    }

    // 4. Specific weekday names
    for ((dayName, targetDay) in DAYS_MAP) {
      if (clean.contains(dayName)) {
        val currentDay = cal.get(Calendar.DAY_OF_WEEK)
        var diff = targetDay - currentDay
        if (diff <= 0) diff += 7
        cal.add(Calendar.DAY_OF_YEAR, diff)
        return ParsedDate(sdf.format(cal.time), cal.timeInMillis, dayName, daysOffset = diff)
      }
    }

    return null
  }

  /**
   * Parse relative time delays (e.g. "بعد نصف ساعة", "بعد 10 دقائق", "الساعة 5 مساءً")
   * with controllable reference time.
   */
  fun parseTimeOrDelay(text: String, refCalendar: Calendar? = null): ParsedTime? {
    val clean = text.lowercase(Locale.ROOT)
    val cal = (refCalendar?.clone() as? Calendar) ?: Calendar.getInstance()

    // 1. Relative delays in minutes/hours
    if (clean.contains("بعد ساعتين")) {
      cal.add(Calendar.HOUR_OF_DAY, 2)
      return makeParsedTime(cal, isDelay = true, delayMins = 120, raw = "بعد ساعتين")
    }

    if (clean.contains("بعد ساعة") || clean.contains("بعد ساعه")) {
      cal.add(Calendar.HOUR_OF_DAY, 1)
      return makeParsedTime(cal, isDelay = true, delayMins = 60, raw = "بعد ساعة")
    }

    if (clean.contains("بعد نص ساعة") || clean.contains("بعد نصف ساعة") || clean.contains("بعد نصف ساعه")) {
      cal.add(Calendar.MINUTE, 30)
      return makeParsedTime(cal, isDelay = true, delayMins = 30, raw = "بعد نصف ساعة")
    }

    if (clean.contains("بعد ربع ساعة") || clean.contains("بعد ربع ساعه")) {
      cal.add(Calendar.MINUTE, 15)
      return makeParsedTime(cal, isDelay = true, delayMins = 15, raw = "بعد ربع ساعة")
    }

    val delayMatch = Regex("""بعد\s*(\d{1,3})\s*(دقيقة|دقائق|دقا')""").find(clean)
    if (delayMatch != null) {
      val mins = delayMatch.groupValues[1].toIntOrNull() ?: 10
      cal.add(Calendar.MINUTE, mins)
      return makeParsedTime(cal, isDelay = true, delayMins = mins, raw = "بعد $mins دقيقة")
    }

    val delayHoursMatch = Regex("""بعد\s*(\d{1,2})\s*(ساعة|ساعات)""").find(clean)
    if (delayHoursMatch != null) {
      val hrs = delayHoursMatch.groupValues[1].toIntOrNull() ?: 1
      cal.add(Calendar.HOUR_OF_DAY, hrs)
      return makeParsedTime(cal, isDelay = true, delayMins = hrs * 60, raw = "بعد $hrs ساعة")
    }

    // 2. Explicit clock time:
    // Requires either:
    // - Preceded by "الساعة" (e.g. "الساعة 5", "الساعة 5:30")
    // - Or formatted with a colon (e.g. "5:30", "14:00")
    // - Or immediately followed by AM/PM marker (e.g. "5 مساءً", "8 ص")
    val isPm = clean.contains("مساء") || clean.contains("عصر") || clean.contains("مغرب") ||
               clean.contains("عشاء") || clean.contains("ليل") || clean.contains("pm") || clean.contains(" م")
    val isAm = clean.contains("صباح") || clean.contains("فجر") || clean.contains("am") || clean.contains(" ص")

    val explicitTimeRegex = Regex("""(?:الساعة\s*)(\d{1,2})(?::(\d{1,2}))?|(\d{1,2}):(\d{2})|(\d{1,2})\s*(?:مساء|صباح|عصر|فجر|مغرب|عشاء|ليل|pm|am|ص|م)\b""")
    val match = explicitTimeRegex.find(clean)
    var foundExplicitTime = false
    var rawHour = 0
    var rawMinute = 0

    if (match != null) {
      val hStr = match.groupValues[1].ifEmpty { match.groupValues[3].ifEmpty { match.groupValues[5] } }
      val mStr = match.groupValues[2].ifEmpty { match.groupValues[4] }
      val parsedH = hStr.toIntOrNull()
      if (parsedH != null && parsedH in 0..24) {
        foundExplicitTime = true
        rawHour = parsedH
        rawMinute = mStr.toIntOrNull() ?: 0
      }
    }

    if (!foundExplicitTime) {
      // Check textual hours: "الخامسة", "السادسة", "العاشرة", "ثمانية", etc.
      val textualHour = when {
        clean.contains("الساعة الواحدة") || clean.contains("الساعة 1") -> 1
        clean.contains("الساعة الثانية") || clean.contains("الساعة 2") -> 2
        clean.contains("الساعة الثالثة") || clean.contains("الساعة 3") -> 3
        clean.contains("الساعة الرابعة") || clean.contains("الساعة 4") -> 4
        clean.contains("الساعة الخامسة") || clean.contains("الساعة 5") -> 5
        clean.contains("الساعة السادسة") || clean.contains("الساعة 6") -> 6
        clean.contains("الساعة السابعة") || clean.contains("الساعة 7") -> 7
        clean.contains("الساعة الثامنة") || clean.contains("الساعة 8") -> 8
        clean.contains("الساعة التاسعة") || clean.contains("الساعة 9") -> 9
        clean.contains("الساعة العاشرة") || clean.contains("الساعة 10") -> 10
        clean.contains("الساعة الحادية عشرة") || clean.contains("الساعة 11") -> 11
        clean.contains("الساعة الثانية عشرة") || clean.contains("الساعة 12") -> 12
        clean.contains("تمانية") || clean.contains("ثماني") -> 8
        clean.contains("تسعة") || clean.contains("تسع") -> 9
        clean.contains("عشرة") || clean.contains("عشر") -> 10
        clean.contains("سبعة") || clean.contains("سبع") -> 7
        clean.contains("ستة") || clean.contains("ست") -> 6
        clean.contains("خمسة") || clean.contains("خمس") -> 5
        clean.contains("اربعة") || clean.contains("أربعة") || clean.contains("اربع") -> 4
        else -> null
      }
      if (textualHour != null && clean.contains("الساعة")) {
        foundExplicitTime = true
        rawHour = textualHour
        rawMinute = 0
      }
    }

    if (foundExplicitTime) {
      if (clean.contains("نص") || clean.contains("نصف") || clean.contains("30")) {
        rawMinute = 30
      } else if (clean.contains("ربع") || clean.contains("15")) {
        rawMinute = 15
      } else if (clean.contains("ثلث") || clean.contains("20")) {
        rawMinute = 20
      }

      val resolvedHour = if (isPm && rawHour < 12) {
        rawHour + 12
      } else if (isAm && rawHour == 12) {
        0
      } else if (!isPm && !isAm && rawHour in 1..6) {
        // If 1..6 with no AM/PM, standard assumption in Arabic daily reminders is afternoon/PM unless morning specified
        rawHour + 12
      } else {
        rawHour
      }

      cal.set(Calendar.HOUR_OF_DAY, resolvedHour)
      cal.set(Calendar.MINUTE, rawMinute)
      cal.set(Calendar.SECOND, 0)
      cal.set(Calendar.MILLISECOND, 0)

      val displayPeriod = if (resolvedHour < 12) "ص" else "م"
      val displayHour = if (resolvedHour % 12 == 0) 12 else resolvedHour % 12
      val displayStr = String.format(Locale.US, "%02d:%02d %s", displayHour, rawMinute, displayPeriod)

      return ParsedTime(
        hour = resolvedHour,
        minute = rawMinute,
        timeDisplay = displayStr,
        isRelativeDelay = false,
        hasTimeSpecified = true,
        isPm = isPm,
        isAm = isAm,
        rawExpression = match?.value ?: "الساعة $rawHour",
        targetTimestamp = cal.timeInMillis
      )
    }

    return null
  }

  private fun makeParsedTime(cal: Calendar, isDelay: Boolean, delayMins: Int, raw: String): ParsedTime {
    val h = cal.get(Calendar.HOUR_OF_DAY)
    val m = cal.get(Calendar.MINUTE)
    val period = if (h < 12) "ص" else "م"
    val h12 = if (h % 12 == 0) 12 else h % 12
    val displayStr = String.format(Locale.US, "%02d:%02d %s", h12, m, period)

    return ParsedTime(
      hour = h,
      minute = m,
      timeDisplay = displayStr,
      isRelativeDelay = isDelay,
      delayMinutes = delayMins,
      hasTimeSpecified = true,
      rawExpression = raw,
      targetTimestamp = cal.timeInMillis
    )
  }

  fun parseUnitRequest(text: String): ParsedUnitConversion? {
    val regex = Regex("(?:حول|تحويل|convert)?\\s*(\\d+(?:\\.\\d+)?)\\s*([a-zA-Z\\u0621-\\u064A]+)(?:\\s*(?:إلى|الى|الي|to)\\s*([a-zA-Z\\u0621-\\u064A]+))?", RegexOption.IGNORE_CASE)
    val match = regex.find(text) ?: return null

    val value = match.groupValues[1].toDoubleOrNull() ?: return null
    val from = match.groupValues[2].lowercase(Locale.ROOT)
    val to = match.groupValues.getOrNull(3)?.lowercase(Locale.ROOT) ?: ""

    val normalizedFrom = normalizeUnit(from)
    val normalizedTo = normalizeUnit(to)

    return ParsedUnitConversion(value, normalizedFrom, normalizedTo)
  }

  private fun normalizeUnit(raw: String): String {
    return when (raw.lowercase(Locale.ROOT)) {
      "كيلو", "كيلوغرام", "كجم", "kg", "kilogram", "kilograms" -> "kg"
      "باوند", "رطل", "lb", "lbs", "pound", "pounds" -> "lb"
      "كيلومتر", "كم", "km", "kilometer", "kilometers" -> "km"
      "ميل", "mi", "mile", "miles" -> "mi"
      "متر", "m", "meter", "meters" -> "m"
      "قدم", "ft", "feet", "foot" -> "ft"
      "مئوية", "سيليزيوس", "c", "celsius" -> "C"
      "فهرنهايت", "f", "fahrenheit" -> "F"
      "لتر", "l", "liter", "liters" -> "L"
      "جالون", "gal", "gallon", "gallons" -> "gal"
      "دولار", "usd", "dollar", "dollars" -> "USD"
      "ريال", "sar", "riyal", "riyals" -> "SAR"
      "جنيه", "egp", "pound_eg" -> "EGP"
      "درهم", "aed", "dirham" -> "AED"
      "دينار", "kwd", "dinar" -> "KWD"
      "يورو", "eur", "euro", "euros" -> "EUR"
      else -> raw
    }
  }
}
