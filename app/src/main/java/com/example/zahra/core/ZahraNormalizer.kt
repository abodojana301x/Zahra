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

  fun francoToArabic(input: String): String {
    var s = input.lowercase()
    val replacements = listOf(
      "shokran" to "شكرا",
      "ezayak" to "ازيك",
      "ezayek" to "ازيك",
      "3amel eh" to "عامل ايه",
      "3amla eh" to "عاملة ايه",
      "fakkarni" to "فكرني",
      "jadwel" to "جدول",
      "saba7 el kher" to "صباح الخير",
      "saba7 el noor" to "صباح النور",
      "mesa2 el kher" to "مساء الخير",
      "inshallah" to "ان شاء الله",
      "elhamdolilah" to "الحمد لله",
      "tamam" to "تمام",
      "habibi" to "حبيبي",
      "keda" to "كده",
      "mesh" to "مش",
      "3ala" to "على",
      "fe" to "في",
      "kol" to "كل",
      "koll" to "كل",
      "yomy" to "يومي",
      "yoman" to "يوميا",
      "sa3a" to "ساعة",
      "soora" to "صورة",
      "esm" to "اسم",
      "esmy" to "اسمي",
      "ana" to "انا",
      "enta" to "انت"
    )
    for ((from, to) in replacements) {
      s = s.replace(Regex("\\b$from\\b"), to)
    }

    // Single character phonetic mappings
    s = s.replace("3", "ع")
      .replace("7", "ح")
      .replace("2", "ء")
      .replace("5", "خ")
      .replace("8", "غ")
      .replace("9", "ق")
      .replace("sh", "ش")
      .replace("th", "ث")
      .replace("kh", "خ")
      .replace("gh", "غ")

    return s
  }

  fun normalize(input: String?): String {
    if (input.isNullOrBlank()) return ""
    var text = input.trim()

    // If input is likely Franco-Arabic, convert to Arabic
    if (Regex("""[235789]""").containsMatchIn(text) || Regex("""\b(shokran|ezayak|3amel|fakkarni)\b""", RegexOption.IGNORE_CASE).containsMatchIn(text)) {
      text = francoToArabic(text)
    }

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

    // 6. Normalize common colloquial words to canonical base
    val colloquialMap = listOf(
      "عاوز" to "عايز",
      "محتاج" to "عايز",
      "اريد" to "عايز",
      "بدي" to "عايز",
      "دلوقتي" to "الان",
      "الحين" to "الان",
      "هسه" to "الان",
      "وبكره" to "وغدا",
      "بكره" to "غدا",
      "وامبارح" to "وامس",
      "امبارح" to "امس"
    )
    for ((from, to) in colloquialMap) {
      text = text.replace(from, to)
    }

    // 7. Normalize multiple whitespaces
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
