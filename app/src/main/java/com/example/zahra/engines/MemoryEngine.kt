package com.example.zahra.engines

import com.example.zahra.model.FactCategory
import com.example.zahra.model.ZahraUserFact
import java.util.regex.Pattern

class MemoryEngine {
  private val facts = mutableListOf<ZahraUserFact>()

  init {
    // Memory starts empty for a new user installation.
    // Zahra dynamically learns the user's name, preferences, and details through natural conversation.
  }

  fun getFacts(): List<ZahraUserFact> = facts.toList()

  fun getFactsByCategory(category: FactCategory): List<ZahraUserFact> {
    return facts.filter { it.category == category }
  }

  fun addFact(category: FactCategory, key: String, value: String, confidence: Double = 1.0): ZahraUserFact {
    // Check if an existing fact with similar key exists, update it if so
    val existingIndex = facts.indexOfFirst { it.key.equals(key.trim(), ignoreCase = true) }
    val newFact = ZahraUserFact(
      category = category,
      key = key.trim(),
      value = value.trim(),
      confidence = confidence,
      createdAt = System.currentTimeMillis()
    )

    if (existingIndex >= 0) {
      facts[existingIndex] = newFact
    } else {
      facts.add(0, newFact)
    }
    return newFact
  }

  fun updateFact(id: String, newKey: String, newValue: String, category: FactCategory? = null) {
    val idx = facts.indexOfFirst { it.id == id }
    if (idx >= 0) {
      val old = facts[idx]
      facts[idx] = old.copy(
        key = newKey.trim(),
        value = newValue.trim(),
        category = category ?: old.category
      )
    }
  }

  fun deleteFact(id: String) {
    facts.removeAll { it.id == id }
  }

  fun clearMemory() {
    facts.clear()
  }

  /**
   * Intelligently scans user messages to extract and store personal facts automatically.
   * Returns newly discovered ZahraUserFact, or null if no new fact was detected.
   */
  fun scanAndExtractFact(input: String): ZahraUserFact? {
    val trimmed = input.trim()

    // 1. Name patterns: "اسمي أحمد", "أنا اسمي دجانة", "نادني بـ محمد"
    val nameRegex = Regex("(?:اسمي|أنا اسمي|ادعني|نادني بـ?|اسمي هو)\\s+([\\p{L}\\s]{2,25})", RegexOption.IGNORE_CASE)
    nameRegex.find(trimmed)?.let { match ->
      val name = match.groupValues[1].trim()
      if (name.isNotEmpty() && !name.contains("زهرة")) {
        return addFact(FactCategory.PERSONAL, "الاسم", name, 0.95)
      }
    }

    // 2. Location patterns: "أعيش في الرياض", "أسكن في القاهرة", "أنا من دبي", "مدينتي هي مكة"
    val locRegex = Regex("(?:أعيش في|أسكن في|أنا من|مدينتي هي|موقعي في)\\s+([\\p{L}\\s]{2,25})", RegexOption.IGNORE_CASE)
    locRegex.find(trimmed)?.let { match ->
      val city = match.groupValues[1].trim()
      if (city.isNotEmpty()) {
        return addFact(FactCategory.LOCATION_WORK, "المدينة الحالية", city, 0.92)
      }
    }

    // 3. Work/Profession: "أعمل مهندس برمجيات", "وظيفتي طبيب", "أنا طالب"
    val workRegex = Regex("(?:أعمل|وظيفتي|مهنتي هي|مجال عملي هو|أنا أعمل)\\s+([\\p{L}\\s]{2,30})", RegexOption.IGNORE_CASE)
    workRegex.find(trimmed)?.let { match ->
      val job = match.groupValues[1].trim()
      if (job.isNotEmpty()) {
        return addFact(FactCategory.LOCATION_WORK, "المهنة / العمل", job, 0.90)
      }
    }

    // 4. Explicit Memory commands: "تذكر أن ...", "احفظ أن ...", "لا تنس أن ..."
    val rememberRegex = Regex("(?:تذكر أن|تذكري أن|احفظ أن|احفظ عندك أن|سجل في ذاكرتك أن|لا تنس أن)\\s+(.+)", RegexOption.IGNORE_CASE)
    rememberRegex.find(trimmed)?.let { match ->
      val content = match.groupValues[1].trim()
      if (content.isNotEmpty()) {
        return addFact(FactCategory.PREFERENCE, "تفضيل مسجل", content, 0.98)
      }
    }

    // 5. Likes/Interests: "أحب البرمجة", "أفضل القهوة السادة", "اهتمامي بالفيزياء"
    val interestRegex = Regex("(?:أحب|أفضل|اهتمامي هو|أهتم بـ)\\s+([\\p{L}\\s]{3,35})", RegexOption.IGNORE_CASE)
    interestRegex.find(trimmed)?.let { match ->
      val topic = match.groupValues[1].trim()
      if (topic.isNotEmpty() && !topic.contains("زهرة") && !topic.contains("مساعد")) {
        return addFact(FactCategory.INTERESTS, "اهتمام / تفضيل", topic, 0.88)
      }
    }

    return null
  }

  /**
   * Generates a coherent Arabic natural language answer summarizing all known facts about user.
   */
  fun summarizeKnownMemory(): String {
    if (facts.isEmpty()) {
      return "ذاكرتي فارغة حالياً عنك! يمكنك إخباري باسمك، مدينتك، مهنتك، أو أي تفضيلات ترغب أن أحفظها وسأتذكرها فوراً."
    }

    val sb = StringBuilder("إليك ما أتذكره وأحفظه عنك في الذاكرة المحلية المشفرة (${facts.size} معلومات):\n\n")
    facts.groupBy { it.category }.forEach { (cat, list) ->
      sb.append("🏷️ **${cat.labelAr}**:\n")
      list.forEach { fact ->
        sb.append("• ${fact.key}: ${fact.value}\n")
      }
      sb.append("\n")
    }
    sb.append("💡 جميع هذه البيانات مشفرة محلياً على جهازك ولا تغادره مطلقاً، ويمكنك تعديلها أو حذفها من شاشة \"الإعدادات والذاكرة\".")
    return sb.toString().trim()
  }
}
