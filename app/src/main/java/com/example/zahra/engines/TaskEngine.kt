package com.example.zahra.engines

import com.example.zahra.core.ZahraNormalizer
import com.example.zahra.entities.EntityExtractor
import com.example.zahra.model.ZahraTask

class TaskEngine {
  private val taskList = mutableListOf<ZahraTask>()

  private val CATEGORY_PROFILES = mapOf(
    "عمل" to listOf("اجتماع", "تقرير", "مشروع", "مستند", "عميل", "إيميل", "كود", "برمجة", "تسليم", "work", "meeting"),
    "شخصي" to listOf("بيت", "عائلة", "شراء", "سوق", "أغراض", "زيارة", "صديق", "home", "family", "shopping"),
    "عاجل" to listOf("ضروري", "هام", "اليوم", "الآن", "فوري", "عاجل", "طارئ", "urgent", "asap"),
    "مالي" to listOf("فاتورة", "بنك", "حساب", "راتب", "دفع", "قسط", "سداد", "فلوس", "bank", "invoice"),
    "صحي" to listOf("طبيب", "دواء", "موعد", "رياضة", "جيم", "فحص", "صحة", "عيادة", "doctor", "medicine")
  )

  fun createTask(input: String): ZahraTask {
    val dateInfo = EntityExtractor.parseRelativeDate(input)
    val category = detectCategory(input)
    val priority = calculatePriority(input, category, dateInfo != null)

    var cleanTitle = input.replace(Regex("^(?:أضف\\s+مهمة|انشئ\\s+مهمة|سجل\\s+مهمة|مهمة\\s+جديدة|تاسك:?)\\s*", RegexOption.IGNORE_CASE), "").trim()
    if (cleanTitle.isBlank()) cleanTitle = input

    val task = ZahraTask(
      title = cleanTitle,
      category = category,
      priorityScore = priority,
      dueDate = dateInfo?.formattedDate
    )
    taskList.add(0, task)
    return task
  }

  fun getTasks(): List<ZahraTask> = taskList.toList()

  fun toggleTask(id: String): Boolean {
    val idx = taskList.indexOfFirst { it.id == id }
    if (idx != -1) {
      val t = taskList[idx]
      taskList[idx] = t.copy(completed = !t.completed)
      return true
    }
    return false
  }

  fun deleteTask(id: String): Boolean {
    return taskList.removeAll { it.id == id }
  }

  private fun detectCategory(text: String): String {
    val words = ZahraNormalizer.extractWords(text)
    var bestCat = "عام"
    var maxMatch = 0

    for ((cat, keywords) in CATEGORY_PROFILES) {
      val matches = words.count { w -> keywords.any { it.contains(w) || w.contains(it) } }
      if (matches > maxMatch) {
        maxMatch = matches
        bestCat = cat
      }
    }
    return bestCat
  }

  private fun calculatePriority(text: String, category: String, hasDueDate: Boolean): Int {
    var p = 2
    if (category == "عاجل" || text.contains("عاجل") || text.contains("ضروري") || text.contains("فوري")) {
      p += 2
    }
    if (hasDueDate) p += 1
    return p.coerceIn(1, 5)
  }
}
