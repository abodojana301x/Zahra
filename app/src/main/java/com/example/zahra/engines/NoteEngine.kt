package com.example.zahra.engines

import com.example.zahra.core.ZahraNormalizer
import com.example.zahra.model.ZahraNote

class NoteEngine {
  private val notesList = mutableListOf<ZahraNote>()

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
    return notesList.removeAll { it.id == id }
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
