package com.example.zahra.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.zahra.engines.ZahraRouter
import com.example.zahra.model.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class AppTab(val titleAr: String, val titleEn: String) {
  CHAT("محادثة", "Chat"),
  TASKS("المهام", "Tasks"),
  NOTES("الملاحظات", "Notes"),
  TRANSLATE("الترجمة", "Translate"),
  RELIGION("الشريعة", "Religion"),
  ARCHIVE("الأرشيف", "Archive"),
  SETTINGS("الإعدادات", "Settings")
}

data class ZahraUiState(
  val currentTab: AppTab = AppTab.CHAT,
  val messages: List<ZahraMessage> = emptyList(),
  val tasks: List<ZahraTask> = emptyList(),
  val notes: List<ZahraNote> = emptyList(),
  val isStreaming: Boolean = false,
  val streamingText: String = "",
  val activeSuggestions: List<String> = listOf(
    "احسب 250 * 4",
    "حول 5 كيلو إلى باوند",
    "متى صلاة المغرب",
    "ترجم سلام إلى الإنجليزية",
    "أضف مهمة تسليم التقرير غداً",
    "لخص هذا النص"
  ),
  val isOfflineVerified: Boolean = true
)

class ZahraViewModel(application: Application) : AndroidViewModel(application) {
  private val router = ZahraRouter(application)

  private val _uiState = MutableStateFlow(ZahraUiState())
  val uiState: StateFlow<ZahraUiState> = _uiState.asStateFlow()

  init {
    // Initial greeting from Zahra
    val initialMsg = ZahraMessage(
      sender = Sender.ZAHRA,
      text = "السلام عليكم ورحمة الله وبركاته! أنا \"زهرة\" (Zahra 2.0) — مساعدك الذكي المستقل الذي يعمل بنسبة 100% دون إنترنت، وبلا أي نماذج توليدية أو تتبع خارجي.\n\nتعتمد قدراتي على الخوارزميات الحتمية والتحليل الإحصائي وقواعد البيانات المدمجة الموثقة فقط. كيف يمكنني خدمتك اليوم؟",
      intent = "greet",
      confidence = 1.0,
      confidenceTier = ConfidenceTier.DIRECT,
      source = "نظام زهرة الترحيبي المستقل"
    )

    // Pre-populate sample tasks and notes for immediate utility
    router.taskEngine.createTask("أضف مهمة مراجعة ميزانية المشروع غداً")
    router.taskEngine.createTask("أضف مهمة تسليم التقرير السنوي عاجل")
    router.noteEngine.createNote("ملاحظة حول أمان البيانات: التشفير المتناظر AES-256-GCM يوفر سرية كاملة محلياً دون تسريب مفاتيح.")

    _uiState.value = _uiState.value.copy(
      messages = listOf(initialMsg),
      tasks = router.taskEngine.getTasks(),
      notes = router.noteEngine.getNotes()
    )
  }

  fun switchTab(tab: AppTab) {
    _uiState.value = _uiState.value.copy(
      currentTab = tab,
      tasks = router.taskEngine.getTasks(),
      notes = router.noteEngine.getNotes()
    )
  }

  fun sendMessage(query: String) {
    if (query.isBlank() || _uiState.value.isStreaming) return

    val userMsg = ZahraMessage(
      sender = Sender.USER,
      text = query.trim()
    )

    // Append user message & maintain short-term 10-turn memory ring buffer
    val currentMessages = _uiState.value.messages.toMutableList()
    currentMessages.add(userMsg)
    if (currentMessages.size > 20) { // 10 user + 10 assistant turns
      currentMessages.removeAt(0)
    }

    _uiState.value = _uiState.value.copy(
      messages = currentMessages,
      isStreaming = true,
      streamingText = ""
    )

    viewModelScope.launch {
      // Process offline through Zahra 6-layer engine
      val response = router.process(query)

      // Fast streaming effect simulation (<200ms latency)
      val fullText = response.text
      val chunkSize = (fullText.length / 8).coerceAtLeast(3)
      var currentLen = 0

      while (currentLen < fullText.length) {
        currentLen = (currentLen + chunkSize).coerceAtMost(fullText.length)
        _uiState.value = _uiState.value.copy(
          streamingText = fullText.substring(0, currentLen)
        )
        delay(25)
      }

      val zahraMsg = ZahraMessage(
        sender = Sender.ZAHRA,
        text = response.text,
        intent = response.intent,
        confidence = response.confidence,
        confidenceTier = response.confidenceTier,
        source = response.source,
        followups = response.followups
      )

      val updatedMessages = _uiState.value.messages.toMutableList()
      updatedMessages.add(zahraMsg)
      if (updatedMessages.size > 20) {
        updatedMessages.removeAt(0)
      }

      _uiState.value = _uiState.value.copy(
        messages = updatedMessages,
        isStreaming = false,
        streamingText = "",
        tasks = router.taskEngine.getTasks(),
        notes = router.noteEngine.getNotes(),
        activeSuggestions = if (response.followups.isNotEmpty()) response.followups else _uiState.value.activeSuggestions
      )
    }
  }

  fun createTaskDirect(text: String) {
    if (text.isBlank()) return
    router.taskEngine.createTask(text)
    _uiState.value = _uiState.value.copy(
      tasks = router.taskEngine.getTasks()
    )
  }

  fun toggleTask(id: String) {
    router.taskEngine.toggleTask(id)
    _uiState.value = _uiState.value.copy(
      tasks = router.taskEngine.getTasks()
    )
  }

  fun deleteTask(id: String) {
    router.taskEngine.deleteTask(id)
    _uiState.value = _uiState.value.copy(
      tasks = router.taskEngine.getTasks()
    )
  }

  fun createNoteDirect(content: String, title: String? = null) {
    if (content.isBlank()) return
    router.noteEngine.createNote(content, title)
    _uiState.value = _uiState.value.copy(
      notes = router.noteEngine.getNotes()
    )
  }

  fun deleteNote(id: String) {
    router.noteEngine.deleteNote(id)
    _uiState.value = _uiState.value.copy(
      notes = router.noteEngine.getNotes()
    )
  }

  fun translateDirect(text: String, targetLang: String): String {
    return router.translationEngine.translate(text, targetLang).translatedText
  }

  fun queryReligionDirect(q: String): String {
    return router.religionEngine.query(q).text
  }

  fun getQiblaAzimuth(): Double = 247.3 // Riyadh reference to Mecca
}
