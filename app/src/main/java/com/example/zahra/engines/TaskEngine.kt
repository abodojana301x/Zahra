package com.example.zahra.engines

import android.content.Context
import android.content.SharedPreferences
import com.example.zahra.core.ZahraNormalizer
import com.example.zahra.entities.EntityExtractor
import com.example.zahra.model.ZahraTask
import org.json.JSONArray
import org.json.JSONObject

class TaskEngine(context: Context? = null) {
  private val prefs: SharedPreferences? = context?.getSharedPreferences("zahra_tasks_prefs", Context.MODE_PRIVATE)
  private val taskList = mutableListOf<ZahraTask>()

  private val CATEGORY_PROFILES = mapOf(
    "عمل" to listOf("اجتماع", "تقرير", "مشروع", "مستند", "عميل", "إيميل", "كود", "برمجة", "تسليم", "work", "meeting"),
    "شخصي" to listOf("بيت", "عائلة", "شراء", "سوق", "أغراض", "زيارة", "صديق", "home", "family", "shopping"),
    "عاجل" to listOf("ضروري", "هام", "اليوم", "الآن", "فوري", "عاجل", "طارئ", "urgent", "asap"),
    "مالي" to listOf("فاتورة", "بنك", "حساب", "راتب", "دفع", "قسط", "سداد", "فلوس", "bank", "invoice"),
    "صحي" to listOf("طبيب", "دواء", "موعد", "رياضة", "جيم", "فحص", "صحة", "عيادة", "doctor", "medicine")
  )

  init {
    loadTasks()
  }

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
    saveTasks()
    return task
  }

  fun getTasks(): List<ZahraTask> = taskList.toList()

  fun toggleTask(id: String): Boolean {
    val idx = taskList.indexOfFirst { it.id == id }
    if (idx != -1) {
      val t = taskList[idx]
      taskList[idx] = t.copy(completed = !t.completed)
      saveTasks()
      return true
    }
    return false
  }

  fun deleteTask(id: String): Boolean {
    val ok = taskList.removeAll { it.id == id }
    if (ok) saveTasks()
    return ok
  }

  private fun saveTasks() {
    prefs?.let { p ->
      val arr = JSONArray()
      for (t in taskList) {
        val obj = JSONObject().apply {
          put("id", t.id)
          put("title", t.title)
          put("category", t.category)
          put("priorityScore", t.priorityScore)
          t.dueDate?.let { put("dueDate", it) }
          put("completed", t.completed)
          put("createdAt", t.createdAt)
        }
        arr.put(obj)
      }
      p.edit().putString("tasks_data", arr.toString()).commit()
    }
  }

  private fun loadTasks() {
    taskList.clear()
    val raw = prefs?.getString("tasks_data", null) ?: return
    try {
      val arr = JSONArray(raw)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        taskList.add(
          ZahraTask(
            id = obj.getString("id"),
            title = obj.getString("title"),
            category = obj.getString("category"),
            priorityScore = obj.getInt("priorityScore"),
            dueDate = if (obj.has("dueDate")) obj.getString("dueDate") else null,
            completed = obj.optBoolean("completed", false),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
          )
        )
      }
    } catch (_: Exception) {}
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
