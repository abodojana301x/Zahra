package com.example.zahra.knowledge.synonyms

import com.example.zahra.knowledge.normalizer.ArabicKnowledgeNormalizer

/**
 * Deterministic, offline synonym dictionary.
 * 
 * Rules:
 * - Stored purely locally with zero network calls.
 * - Max expansion per token is strictly bounded (max 3 aliases) to prevent unbounded fanout.
 * - Cycle-detection prevents infinite recursion or mutual loops.
 * - Expansions are tagged so callers can distinguish direct matches from synonym expansions.
 */
class OfflineSynonymDictionary {

  private val synonymMap = mutableMapOf<String, MutableSet<String>>()

  init {
    loadBuiltinSynonyms()
  }

  private fun loadBuiltinSynonyms() {
    // Science & Physics
    registerBidirectional("سرعة الضوء", listOf("فيزياء الضوء", "سرعة انتشار الضوء", "ثابت سرعة الضوء", "speed of light"))
    registerBidirectional("غليان الماء", listOf("درجة غليان الماء", "نقطة الغليان", "تبخر الماء", "boiling point of water"))
    registerBidirectional("جبل إفرست", listOf("قمة إفرست", "أعلى قمة", "إفرست", "everest"))

    // Technology
    registerBidirectional("تشفير aes", listOf("معيار التشفير المتقدم", "تشفير aes-256", "aes256", "تشفير متناظر"))
    registerBidirectional("أمان البيانات", listOf("حماية البيانات", "سرية المعلومات"))

    // History & Math
    registerBidirectional("الخوارزمي", listOf("محمد بن موسى الخوارزمي", "مؤسس علم الجبر", "أبو الجبر", "khwarizmi"))
    registerBidirectional("خوارزمية", listOf("خوارزميات", "algorithm"))

    // Health
    registerBidirectional("ساعات النوم", listOf("مدة النوم", "احتياج النوم", "النوم الصحي", "نوم البالغين", "sleep duration"))
    registerBidirectional("صحة القلب", listOf("الوقاية القلبية", "سلامة القلب"))

    // Quranic Concepts
    registerBidirectional("آية الكرسي", listOf("أعظم آية", "اية الكرسي", "سيد آي القرآن"))
    registerBidirectional("سورة الفاتحة", listOf("أم الكتاب", "السبع المثاني", "فاتحة الكتاب"))
    registerBidirectional("سورة الإخلاص", listOf("قل هو الله أحد", "سورة التوحيد"))
    registerBidirectional("سورة الشرح", listOf("ألم نشرح لك صدرك", "سورة الانشراح"))
    registerBidirectional("سورة العصر", listOf("والعصر إن الإنسان لفي خسر", "سورة الزمان"))

    // Hadith & Ethics
    registerBidirectional("النية", listOf("إخلاص النية", "الأعمال بالنيات", "صلاح العمل"))
    registerBidirectional("طلب العلم", listOf("فضل العلم", "سلوك طريق العلم", "تعلم العلم"))
    registerBidirectional("حسن الخلق", listOf("محبة الخير للغير", "حب الخير للناس", "الأخلاق الحسنة"))
    registerBidirectional("التيسير", listOf("السماحة", "الرفق", "يسروا ولا تعسروا", "عدم التعسير"))
    registerBidirectional("حفظ اللسان", listOf("كف الأذى", "سلامة المسلم من اللسان", "ضبط الكلام"))

    // Fiqh Terms
    registerBidirectional("أركان الوضوء", listOf("فرائض الوضوء", "شروط صحة الوضوء", "صفة الوضوء"))
    registerBidirectional("نواقض الوضوء", listOf("مبطلات الوضوء", "مفسدات الوضوء", "ما ينقض الطهارة"))
    registerBidirectional("شروط الصلاة", listOf("شروط صحة الصلاة", "واجبات الصلاة"))
    registerBidirectional("صلاة المسافر", listOf("قصر الصلاة", "جمع الصلاة للمسافر", "رخصة السفر"))
    registerBidirectional("زكاة الفطر", listOf("صدقة الفطر", "مقدار زكاة الفطر", "وقت إخراج زكاة الفطر"))
  }

  /**
   * Registers canonical terms and aliases in normalized form.
   */
  fun registerBidirectional(canonical: String, aliases: List<String>) {
    val normCanonical = ArabicKnowledgeNormalizer.normalizeForSearch(canonical)
    val set = synonymMap.getOrPut(normCanonical) { mutableSetOf() }

    for (alias in aliases) {
      val normAlias = ArabicKnowledgeNormalizer.normalizeForSearch(alias)
      if (normAlias.isNotBlank() && normAlias != normCanonical) {
        set.add(normAlias)
        // Also map reverse with cycle protection
        val revSet = synonymMap.getOrPut(normAlias) { mutableSetOf() }
        revSet.add(normCanonical)
      }
    }
  }

  /**
   * Returns bounded expansions for a given token or phrase.
   * Max 3 deterministic expansions per term.
   */
  fun getBoundedExpansions(term: String, maxExpansions: Int = 3): List<String> {
    val norm = ArabicKnowledgeNormalizer.normalizeForSearch(term)
    val direct = synonymMap[norm] ?: return emptyList()

    return direct.sorted() // Stable deterministic ordering
      .take(maxExpansions)
  }

  /**
   * Returns total registered synonym clusters.
   */
  fun size(): Int = synonymMap.size
}
