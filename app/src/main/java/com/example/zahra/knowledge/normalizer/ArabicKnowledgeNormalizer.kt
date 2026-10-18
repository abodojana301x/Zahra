package com.example.zahra.knowledge.normalizer

/**
 * Non-destructive Arabic and English Normalizer for Offline Knowledge Retrieval.
 * 
 * Guiding Principles:
 * 1. The original source text is ALWAYS preserved in storage and presentation.
 * 2. Normalization creates indexed representations and query representations.
 * 3. Bounded morphological rules: Never aggressively collapse roots or religious terms.
 */
object ArabicKnowledgeNormalizer {

  private val TASHKEEL_AND_TATWEEL_REGEX = Regex("[\\u064B-\\u065F\\u0670\\u0640]")
  private val PUNCTUATION_REGEX = Regex("[!\"#$%&'()*+,\\-./:;<=>?@\\[\\]^_`{|}~،؟؛«»“”‘’…–—]")
  private val MULTI_SPACE_REGEX = Regex("\\s+")

  private val ARABIC_INDIC_DIGITS = mapOf(
    '٠' to '0', '١' to '1', '٢' to '2', '٣' to '3', '٤' to '4',
    '٥' to '5', '٦' to '6', '٧' to '7', '٨' to '8', '٩' to '9',
    '۰' to '0', '۱' to '1', '۲' to '2', '۳' to '3', '۴' to '4',
    '۵' to '5', '۶' to '6', '۷' to '7', '۸' to '8', '۹' to '9'
  )

  // Words where "ال" is integral to the root or name and must NOT be stripped as a definite article prefix
  private val NON_STRIPPABLE_AL_WORDS = setOf(
    "الله", "الذي", "التي", "الذين", "اللائي", "اللاتي", "الان", "الماس", "الم", "الف", "الوان", "اليوم"
  )

  // Words where "و" is integral to the root and must NOT be stripped as a conjunction prefix
  private val NON_STRIPPABLE_WAW_ROOTS = setOf(
    "واحد", "وقت", "وزن", "وجوب", "وضوء", "ورد", "وقوف", "وصول", "وحي", "ورع", "وهب", "ولد", "وفاة"
  )

  // Minimal stop words that can be ignored during broad keyword intersection (NOT ignored in phrase matches)
  val KNOWLEDGE_STOP_WORDS = setOf(
    "في", "من", "على", "إلى", "عن", "مع", "هذا", "هذه", "ذلك", "تلك", "التي", "الذي",
    "هو", "هي", "هم", "أنا", "نحن", "أنت", "كان", "كانت", "أن", "إن", "ما", "هل", "قد",
    "ثم", "أو", "بل", "لكن", "كل", "بعض", "يا", "ماذا", "أين", "كيف", "كم", "متى", "لماذا",
    "حديث", "احاديث", "أحاديث", "النبي", "رسول", "آية", "اية", "سورة", "سوره", "قرآن", "قران",
    "اريد", "أريد", "اعطني", "أعطني", "اذكر", "أذكر", "لي", "معنى", "تعريف"
  )

  /**
   * Normalizes a string for search matching.
   * Does NOT alter the original text stored in the KnowledgeRecord.
   */
  fun normalizeForSearch(text: String?): String {
    if (text.isNullOrBlank()) return ""

    var s = text.trim()

    // 1. Strip Tashkeel (harakat) and Tatweel (kashida)
    s = TASHKEEL_AND_TATWEEL_REGEX.replace(s, "")

    // 2. Unify Alef forms: أ, إ, آ, ٱ -> ا
    s = s.replace(Regex("[أإآٱ]"), "ا")

    // 3. Unify Yaa / Alef Maqsura: ى, ئ -> ي
    s = s.replace(Regex("[ىئ]"), "ي")

    // 4. Unify Taa Marbuta: ة -> ه
    s = s.replace('ة', 'ه')

    // 5. Unify Hamza on Waw: ؤ -> و
    s = s.replace('ؤ', 'و')

    // 6. Map Arabic-Indic and Eastern digits to ASCII digits
    val sb = StringBuilder(s.length)
    for (ch in s) {
      sb.append(ARABIC_INDIC_DIGITS[ch] ?: ch)
    }
    s = sb.toString()

    // 7. Strip punctuation to avoid boundary mismatches
    s = PUNCTUATION_REGEX.replace(s, " ")

    // 8. Lowercase Latin characters
    s = s.lowercase()

    // 9. Normalize multiple whitespace sequences
    return MULTI_SPACE_REGEX.replace(s, " ").trim()
  }

  /**
   * Extracts search tokens from normalized text.
   */
  fun extractTokens(text: String, filterStopWords: Boolean = true): List<String> {
    val norm = normalizeForSearch(text)
    if (norm.isBlank()) return emptyList()

    val tokens = norm.split(' ')
      .map { it.trim() }
      .filter { it.isNotBlank() && it.length >= 2 }

    if (!filterStopWords) return tokens

    val filtered = tokens.filterNot { KNOWLEDGE_STOP_WORDS.contains(it) }
    // If all tokens are stop words (e.g. query is "ما هو"), return unfiltered so query doesn't become empty
    return if (filtered.isEmpty()) tokens else filtered
  }

  /**
   * Conservative bounded stemmer that extracts candidate root variations.
   * Ensures root integrity for religious terminology, physical units, and names.
   */
  fun getBoundedStems(token: String): List<String> {
    val norm = normalizeForSearch(token)
    if (norm.length < 4) return listOf(norm)

    val candidates = mutableListOf(norm)

    // Bounded Definite Article ("ال") removal
    if (norm.startsWith("ال") && norm.length >= 5 && !NON_STRIPPABLE_AL_WORDS.contains(norm)) {
      val stripped = norm.substring(2)
      if (stripped.length >= 3) {
        candidates.add(stripped)
      }
    }

    // Bounded Conjunction ("و") removal
    if (norm.startsWith("و") && norm.length >= 5 && !NON_STRIPPABLE_WAW_ROOTS.contains(norm)) {
      val stripped = norm.substring(1)
      if (stripped.length >= 3) {
        candidates.add(stripped)
        // If it was "والكتاب" -> stripped is "الكتاب" -> also add "كتاب"
        if (stripped.startsWith("ال") && stripped.length >= 5 && !NON_STRIPPABLE_AL_WORDS.contains(stripped)) {
          candidates.add(stripped.substring(2))
        }
      }
    }

    return candidates.distinct()
  }
}
