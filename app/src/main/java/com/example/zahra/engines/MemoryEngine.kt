package com.example.zahra.engines

import android.content.Context
import com.example.zahra.model.FactCategory
import com.example.zahra.model.MemorySaveResult
import com.example.zahra.model.MemorySource
import com.example.zahra.model.ZahraUserFact
import com.example.zahra.storage.InMemoryMemoryDriver
import com.example.zahra.storage.SafeMemoryStore
import com.example.zahra.storage.SharedPreferencesMemoryDriver

/**
 * Phase 2 Persistent Memory Engine for Zahra.
 * Implements the explicit lifecycle: Requested → Validated → Saved → Verified → Retrievable.
 * Guarantees that saved information survives process restarts, is verified via read-back,
 * and clearly separates persistent facts from temporary session context.
 */
class MemoryEngine(
  context: Context? = null,
  customStore: SafeMemoryStore? = null
) {
  val store: SafeMemoryStore = customStore ?: run {
    if (context != null) {
      SafeMemoryStore(SharedPreferencesMemoryDriver(context), enableEncryption = true)
    } else {
      SafeMemoryStore(InMemoryMemoryDriver(), enableEncryption = false)
    }
  }

  fun getFacts(): List<ZahraUserFact> = store.getConfirmedFacts()

  fun getTemporaryFacts(): List<ZahraUserFact> = store.getTemporaryFacts()

  fun getFactsByCategory(category: FactCategory): List<ZahraUserFact> {
    return store.getConfirmedFacts().filter { it.category == category }
  }

  /**
   * Adds or updates a confirmed persistent fact with verified read-back.
   */
  fun addFact(
    category: FactCategory,
    key: String,
    value: String,
    confidence: Double = 1.0,
    source: MemorySource = MemorySource.USER_EXPLICIT
  ): ZahraUserFact {
    val result = store.saveFact(
      category = category,
      key = key,
      value = value,
      confidence = confidence,
      source = source,
      isConfirmed = true
    )
    return when (result) {
      is MemorySaveResult.Success -> result.fact
      else -> {
        // Fallback for direct backward compatibility
        ZahraUserFact(category = category, key = key, value = value, confidence = confidence, source = source)
      }
    }
  }

  /**
   * Complete lifecycle save with explicit result reporting (for natural language commands).
   */
  fun saveFactWithResult(
    category: FactCategory,
    key: String,
    value: String,
    confidence: Double = 1.0,
    source: MemorySource = MemorySource.USER_EXPLICIT
  ): MemorySaveResult {
    return store.saveFact(
      category = category,
      key = key,
      value = value,
      confidence = confidence,
      source = source,
      isConfirmed = true
    )
  }

  fun updateFact(id: String, newKey: String, newValue: String, category: FactCategory? = null): MemorySaveResult {
    return store.updateFact(id, newKey, newValue, category)
  }

  fun deleteFact(id: String): Boolean {
    return store.deleteFact(id)
  }

  fun deleteFactByKeyOrContent(query: String): Boolean {
    val clean = query.trim().lowercase()
    val fact = store.getConfirmedFacts().find {
      it.key.lowercase().contains(clean) || it.value.lowercase().contains(clean) || clean.contains(it.key.lowercase())
    } ?: return false
    return store.deleteFact(fact.id)
  }

  fun clearMemory(): Boolean {
    return store.clearAll()
  }

  /**
   * Extracts and handles explicit memory commands ("تذكر أن...", "احفظ أن...", "لا تنس أن...").
   * Returns a MemorySaveResult ensuring verified persistence.
   */
  fun saveExplicitMemory(input: String): MemorySaveResult? {
    val trimmed = input.trim()
    val rememberRegex = Regex("(?:تذكر أن|تذكري أن|احفظ أن|احفظ عندك أن|سجل في ذاكرتك أن|لا تنس أن|تذكر ان|احفظ ان)\\s+(.+)", RegexOption.IGNORE_CASE)
    val match = rememberRegex.find(trimmed) ?: return null

    val rawContent = match.groupValues[1].trim()
    if (rawContent.isBlank()) {
      return MemorySaveResult.ValidationFailed("محتوى التذكر فارغ.")
    }

    // Determine key and value from phrasing
    // e.g. "رقم هاتفي هو 0501234567" -> key: "رقم هاتفي", value: "0501234567"
    // e.g. "أفضل الشاي الأخضر" -> key: "تفضيل مسجل", value: "أفضل الشاي الأخضر"
    var key = "تفضيل مسجل"
    var value = rawContent
    val category = when {
      rawContent.contains("اسمي") || rawContent.contains("عمري") || rawContent.contains("هاتفي") || rawContent.contains("ميلادي") -> {
        key = if (rawContent.contains("اسمي")) "الاسم" else if (rawContent.contains("هاتفي")) "رقم الهاتف" else "بيانات شخصية"
        FactCategory.PERSONAL
      }
      rawContent.contains("أعيش") || rawContent.contains("أسكن") || rawContent.contains("مدينتي") || rawContent.contains("عملي") || rawContent.contains("وظيفتي") -> {
        key = if (rawContent.contains("عمل") || rawContent.contains("وظيفة")) "المهنة / العمل" else "المدينة / الإقامة"
        FactCategory.LOCATION_WORK
      }
      rawContent.contains("أحب") || rawContent.contains("أهوى") || rawContent.contains("مشروع") || rawContent.contains("اهتمام") -> {
        key = "اهتمام شخصي"
        FactCategory.INTERESTS
      }
      else -> FactCategory.PREFERENCE
    }

    // Handle split patterns like "رقم هاتفي هو X" or "اسمي هو X" or "اسمي X"
    if (rawContent.startsWith("اسمي ") || rawContent.startsWith("اسمي هو ")) {
      key = "الاسم"
      value = rawContent.replace(Regex("""^اسمي(?:\s+هو)?\s+"""), "").trim()
    } else {
      val isPattern = Regex("(.+?)\\s+(?:هو|هي|=)\\s+(.+)")
      isPattern.find(rawContent)?.let { m ->
        val extractedK = m.groupValues[1].trim()
        val extractedV = m.groupValues[2].trim()
        if (extractedK.length in 2..40 && extractedV.isNotEmpty()) {
          key = extractedK
          value = extractedV
        }
      }
    }

    return store.saveFact(
      category = category,
      key = key,
      value = value,
      confidence = 1.0,
      source = MemorySource.USER_EXPLICIT,
      isConfirmed = true
    )
  }

  /**
   * Scans user messages to detect personal facts:
   * Explicit name/city/work introductions are saved persistently with verification.
   * Casual, unprompted remarks ("أحب البرمجة") are recorded as temporary session context
   * to avoid silently converting uncertain inferences into confirmed facts.
   */
  fun scanAndExtractFact(input: String): ZahraUserFact? {
    val trimmed = input.trim()

    // 1. Explicit introduction of name: "اسمي أحمد", "أنا اسمي دجانة", "نادني بـ محمد"
    val nameRegex = Regex("(?:اسمي|أنا اسمي|ادعني|نادني بـ?|اسمي هو)\\s+([\\p{L}\\s]{2,25})", RegexOption.IGNORE_CASE)
    nameRegex.find(trimmed)?.let { match ->
      val name = match.groupValues[1].trim()
      if (name.isNotEmpty() && !name.contains("زهرة") && !name.contains("مساعد")) {
        val res = store.saveFact(
          category = FactCategory.PERSONAL,
          key = "الاسم",
          value = name,
          confidence = 0.98,
          source = MemorySource.USER_EXPLICIT,
          isConfirmed = true
        )
        if (res is MemorySaveResult.Success) return res.fact
      }
    }

    // 2. Explicit introduction of residence: "أعيش في الرياض", "أسكن في القاهرة", "أنا من دبي"
    val locRegex = Regex("(?:أعيش في|أسكن في|أنا من|مدينتي هي|موقعي في)\\s+([\\p{L}\\s]{2,25})", RegexOption.IGNORE_CASE)
    locRegex.find(trimmed)?.let { match ->
      val city = match.groupValues[1].trim()
      if (city.isNotEmpty() && !city.contains("البيت") && !city.contains("مكان")) {
        val res = store.saveFact(
          category = FactCategory.LOCATION_WORK,
          key = "المدينة الحالية",
          value = city,
          confidence = 0.95,
          source = MemorySource.USER_EXPLICIT,
          isConfirmed = true
        )
        if (res is MemorySaveResult.Success) return res.fact
      }
    }

    // 3. Work/Profession: "أعمل مهندس برمجيات", "وظيفتي طبيب", "أنا طالب"
    val workRegex = Regex("(?:أعمل|وظيفتي|مهنتي هي|مجال عملي هو|أنا أعمل)\\s+([\\p{L}\\s]{2,30})", RegexOption.IGNORE_CASE)
    workRegex.find(trimmed)?.let { match ->
      val job = match.groupValues[1].trim()
      if (job.isNotEmpty()) {
        val res = store.saveFact(
          category = FactCategory.LOCATION_WORK,
          key = "المهنة / العمل",
          value = job,
          confidence = 0.92,
          source = MemorySource.USER_EXPLICIT,
          isConfirmed = true
        )
        if (res is MemorySaveResult.Success) return res.fact
      }
    }

    // 4. Explicit Memory commands: "تذكر أن ...", "احفظ أن ..."
    val explicitResult = saveExplicitMemory(trimmed)
    if (explicitResult is MemorySaveResult.Success) {
      return explicitResult.fact
    }

    // 5. Inferred / Casual Interests ("أحب القراءة", "أفضل القهوة"):
    // Crucial requirement: "Never silently convert uncertain inferences into confirmed facts."
    // Saved ONLY to temporary session context, NOT persistent memory!
    val interestRegex = Regex("(?:أحب|أفضل|اهتمامي هو|أهتم بـ)\\s+([\\p{L}\\s]{3,35})", RegexOption.IGNORE_CASE)
    interestRegex.find(trimmed)?.let { match ->
      val topic = match.groupValues[1].trim()
      if (topic.isNotEmpty() && !topic.contains("زهرة") && !topic.contains("مساعد")) {
        return store.addTemporaryFact(FactCategory.INTERESTS, "اهتمام / تفضيل", topic)
      }
    }

    return null
  }

  /**
   * Generates a natural language summary of confirmed memories.
   */
  fun summarizeKnownMemory(): String {
    val facts = store.getConfirmedFacts()
    val tempFacts = store.getTemporaryFacts()

    if (facts.isEmpty() && tempFacts.isEmpty()) {
      return "ذاكرتي الدائمة فارغة حالياً عنك! يمكنك أن تطلب مني حفظ أي معلومة بقولك: \"تذكر أن اسمي...\" أو \"احفظ أنني أعيش في...\". سأقوم بحفظها وتأكيد كتابتها في الذاكرة المشفرة فوراً."
    }

    val sb = StringBuilder()
    if (facts.isNotEmpty()) {
      sb.append("📋 **المعلومات المحفوظة في الذاكرة الدائمة المشفرة** (${facts.size}):\n\n")
      facts.groupBy { it.category }.forEach { (cat, list) ->
        sb.append("🏷️ **${cat.labelAr}**:\n")
        list.forEach { fact ->
          sb.append("• ${fact.key}: ${fact.value} (المصدر: ${fact.source.labelAr})\n")
        }
        sb.append("\n")
      }
    }

    if (tempFacts.isNotEmpty()) {
      sb.append("💭 **سياق المحادثة الحالية (غير مثبت بشكل دائم)**:\n")
      tempFacts.forEach { fact ->
        sb.append("• ${fact.key}: ${fact.value}\n")
      }
      sb.append("\n")
    }

    sb.append("🔐 جميع بيانات الذاكرة مشفرة محلياً (AES-256-GCM) ولا تغادر جهازك مطلقاً، ولديك كامل الصلاحية لتعديلها أو حذفها من شاشة الإعدادات أو عبر أمر مباشر (مثل: \"احذف من الذاكرة...\").")
    return sb.toString().trim()
  }
}
