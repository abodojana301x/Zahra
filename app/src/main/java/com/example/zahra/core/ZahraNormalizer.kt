package com.example.zahra.core

object ZahraNormalizer {
  private val TASHKEEL_REGEX = Regex("[\\u064B-\\u065F\\u0670\\u0640]")
  private val ARABIC_INDIC_DIGITS = mapOf(
    '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
    '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
    '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
    '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9'
  )

  val ARABIC_STOPWORDS = setOf(
    "في", "من", "على", "إلى", "عن", "مع", "هذا", "هذه", "ذلك", "تلك", "التي", "الذي",
    "هو", "هي", "هم", "هن", "نحن", "أنا", "أنت", "كان", "كانت", "يكون", "أن", "إن",
    "ما", "لا", "لم", "لن", "هل", "قد", "ثم", "أو", "بل", "لكن", "كل", "بعض", "يا"
  )

  fun normalize(input: String?): String {
    if (input.isNullOrBlank()) return ""
    var text = input.trim()

    // 1. Strip tashkeel and tatweel
    text = TASHKEEL_REGEX.replace(text, "")

    // 2. Unify Alef: أ, إ, آ, ٱ -> ا
    text = text.replace(Regex("[أإآٱ]"), "ا")

    // 3. Unify Yaa: ى, ئ -> ي
    text = text.replace(Regex("[ىئ]"), "ي")

    // 4. Unify Taa Marbuta: ة -> ه
    text = text.replace('ة', 'ه')

    // 5. Unify Arabic-Indic digits
    val sb = StringBuilder(text.length)
    for (ch in text) {
      sb.append(ARABIC_INDIC_DIGITS[ch] ?: ch)
    }
    text = sb.toString()

    // 6. Normalize multiple whitespaces
    return text.replace(Regex("\\s+"), " ")
  }

  fun extractWords(text: String): List<String> {
    val norm = normalize(text)
    val wordRegex = Regex("[\\p{L}\\p{N}]+")
    return wordRegex.findAll(norm)
      .map { it.value.lowercase() }
      .filter { it.length > 1 && !ARABIC_STOPWORDS.contains(it) }
      .toList()
  }
}
