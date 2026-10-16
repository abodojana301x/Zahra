package com.example.zahra.engines

import android.content.Context
import android.content.SharedPreferences
import com.example.zahra.core.ZahraNormalizer
import com.example.zahra.model.ZahraNote
import org.json.JSONArray
import org.json.JSONObject

class NoteEngine(context: Context? = null) {
  private val prefs: SharedPreferences? = context?.getSharedPreferences("zahra_notes_prefs", Context.MODE_PRIVATE)
  private val notesList = mutableListOf<ZahraNote>()

  init {
    loadNotes()
  }

  fun createNote(rawContent: String, title: String? = null): ZahraNote {
    val clean = rawContent.replace(Regex("^(?:سجل\\s+ملاحظة|اكتب\\s+ملاحظة|احفظ\\s+ملاحظة|تذكر\\s+أن|note:?)\\s*", RegexOption.IGNORE_CASE), "").trim()
    val noteTitle = title ?: (clean.split("\n").firstOrNull()?.take(35) ?: "ملاحظة جديدة")
    val tags = extractRakeTags(clean)

    val note = ZahraNote(
      title = noteTitle,
      content = clean,
      tags = tags
    )
    notesList.add(0, note)
    saveNotes()
    return note
  }

  fun getNotes(query: String? = null): List<ZahraNote> {
    if (query.isNullOrBlank()) return notesList.toList()
    val q = query.lowercase()
    return notesList.filter {
      it.title.lowercase().contains(q) ||
        it.content.lowercase().contains(q) ||
        it.tags.any { t -> t.contains(q) }
    }
  }

  fun deleteNote(id: String): Boolean {
    val ok = notesList.removeAll { it.id == id }
    if (ok) saveNotes()
    return ok
  }

  private fun saveNotes() {
    prefs?.let { p ->
      val arr = JSONArray()
      for (n in notesList) {
        val obj = JSONObject().apply {
          put("id", n.id)
          put("title", n.title)
          put("content", n.content)
          val tagsArr = JSONArray()
          n.tags.forEach { tagsArr.put(it) }
          put("tags", tagsArr)
          put("createdAt", n.createdAt)
        }
        arr.put(obj)
      }
      p.edit().putString("notes_data", arr.toString()).commit()
    }
  }

  private fun loadNotes() {
    notesList.clear()
    val raw = prefs?.getString("notes_data", null) ?: return
    try {
      val arr = JSONArray(raw)
      for (i in 0 until arr.length()) {
        val obj = arr.getJSONObject(i)
        val tagsArr = obj.optJSONArray("tags")
        val tags = mutableListOf<String>()
        if (tagsArr != null) {
          for (j in 0 until tagsArr.length()) {
            tags.add(tagsArr.getString(j))
          }
        }
        notesList.add(
          ZahraNote(
            id = obj.getString("id"),
            title = obj.getString("title"),
            content = obj.getString("content"),
            tags = tags,
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
          )
        )
      }
    } catch (_: Exception) {}
  }

  private fun extractRakeTags(text: String, maxTags: Int = 4): List<String> {
    val words = ZahraNormalizer.extractWords(text)
    if (words.isEmpty()) return listOf("عام")

    val freq = mutableMapOf<String, Int>()
    val degree = mutableMapOf<String, Int>()

    for (w in words) {
      freq[w] = (freq[w] ?: 0) + 1
      degree[w] = (degree[w] ?: 0) + words.size
    }

    return freq.map { (w, f) ->
      val deg = degree[w] ?: f
      w to (deg.toDouble() / f)
    }
      .sortedByDescending { it.second }
      .take(maxTags)
      .map { it.first }
  }
}
