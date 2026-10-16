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

enum class ZahraScreen {
  CHAT,
  SCHEDULE,
  SETTINGS,
  TASKS,
  NOTES,
  ARCHIVE,
  RELIGION,
  TRANSLATE
}

data class ZahraUiState(
  val currentScreen: ZahraScreen = ZahraScreen.CHAT,
  val activeSessionId: String = "",
  val sessions: List<ZahraSession> = emptyList(),
  val pinnedSessionIds: Set<String> = emptySet(),
  val searchDrawerQuery: String = "",
  val tasks: List<ZahraTask> = emptyList(),
  val notes: List<ZahraNote> = emptyList(),
  val schedules: List<ZahraSchedule> = emptyList(),
  val userFacts: List<ZahraUserFact> = emptyList(),
  val isStreaming: Boolean = false,
  val streamingText: String = "",
  val isDeepThinkingEnabled: Boolean = false,
  val isGlassmorphismEnabled: Boolean = false,
  val isVoiceModeActive: Boolean = false,
  val showMemoryDetailModal: Boolean = false,
  val showToolsBottomSheet: Boolean = false,
  val userProfileName: String = "المستخدم",
  val accentColorName: String = "أزرق",
  val activeSuggestions: List<String> = listOf(
    "ابدأ دردشة صوتية",
    "إنشاء صورة أو ملصق",
    "جدولة تذكير يومي للوكيل",
    "الكتابة أو التحرير"
  ),
  val isOfflineVerified: Boolean = true
) {
  val currentSession: ZahraSession?
    get() = sessions.find { it.id == activeSessionId } ?: sessions.firstOrNull()

  val messages: List<ZahraMessage>
    get() = currentSession?.messages ?: emptyList()
}

class ZahraViewModel(application: Application) : AndroidViewModel(application) {
  val router = ZahraRouter(application)

  private val _uiState = MutableStateFlow(ZahraUiState())
  val uiState: StateFlow<ZahraUiState> = _uiState.asStateFlow()

  init {
    // Initial clean empty session for a fresh offline installation
    val initialSession = ZahraSession(
      id = "session_active_main",
      title = "محادثة جديدة",
      messages = emptyList()
    )

    val storedFacts = router.memoryEngine.getFacts()
    val knownName = storedFacts.find { it.key.contains("الاسم") }?.value

    _uiState.value = _uiState.value.copy(
      activeSessionId = initialSession.id,
      sessions = listOf(initialSession),
      pinnedSessionIds = emptySet(),
      userProfileName = knownName ?: "المستخدم",
      tasks = router.taskEngine.getTasks(),
      notes = router.noteEngine.getNotes(),
      schedules = router.scheduleEngine.getSchedules(),
      userFacts = storedFacts
    )
  }

  fun navigateToScreen(screen: ZahraScreen) {
    _uiState.value = _uiState.value.copy(currentScreen = screen)
  }

  fun setSearchDrawerQuery(query: String) {
    _uiState.value = _uiState.value.copy(searchDrawerQuery = query)
  }

  fun togglePinSession(sessionId: String) {
    val currentPinned = _uiState.value.pinnedSessionIds.toMutableSet()
    if (currentPinned.contains(sessionId)) {
      currentPinned.remove(sessionId)
    } else {
      currentPinned.add(sessionId)
    }
    _uiState.value = _uiState.value.copy(pinnedSessionIds = currentPinned)
  }

  fun toggleDeepThinking() {
    _uiState.value = _uiState.value.copy(
      isDeepThinkingEnabled = !_uiState.value.isDeepThinkingEnabled
    )
  }

  fun setVoiceModeActive(active: Boolean) {
    _uiState.value = _uiState.value.copy(isVoiceModeActive = active)
  }

  fun updateProfileName(newName: String) {
    val trimmed = newName.trim()
    if (trimmed.isNotBlank()) {
      _uiState.value = _uiState.value.copy(userProfileName = trimmed)
      router.memoryEngine.addFact(FactCategory.PERSONAL, "الاسم", trimmed, 1.0)
      _uiState.value = _uiState.value.copy(userFacts = router.memoryEngine.getFacts())
    }
  }

  fun setShowMemoryDetailModal(show: Boolean) {
    _uiState.value = _uiState.value.copy(showMemoryDetailModal = show)
  }

  fun setShowToolsBottomSheet(show: Boolean) {
    _uiState.value = _uiState.value.copy(showToolsBottomSheet = show)
  }

  fun setAccentColor(colorName: String) {
    _uiState.value = _uiState.value.copy(accentColorName = colorName)
  }

  fun startNewSession() {
    val newSession = ZahraSession(
      title = "محادثة جديدة",
      messages = emptyList()
    )
    val updatedSessions = listOf(newSession) + _uiState.value.sessions
    _uiState.value = _uiState.value.copy(
      activeSessionId = newSession.id,
      sessions = updatedSessions,
      currentScreen = ZahraScreen.CHAT,
      searchDrawerQuery = ""
    )
  }

  fun switchSession(sessionId: String) {
    if (_uiState.value.sessions.any { it.id == sessionId }) {
      _uiState.value = _uiState.value.copy(
        activeSessionId = sessionId,
        currentScreen = ZahraScreen.CHAT
      )
    }
  }

  fun deleteSession(sessionId: String) {
    val filtered = _uiState.value.sessions.filter { it.id != sessionId }
    if (filtered.isEmpty()) {
      startNewSession()
    } else {
      val nextActiveId = if (_uiState.value.activeSessionId == sessionId) {
        filtered.first().id
      } else {
        _uiState.value.activeSessionId
      }
      _uiState.value = _uiState.value.copy(
        sessions = filtered,
        activeSessionId = nextActiveId
      )
    }
  }

  fun renameSession(sessionId: String, newTitle: String) {
    if (newTitle.isBlank()) return
    val updated = _uiState.value.sessions.map { session ->
      if (session.id == sessionId) session.copy(title = newTitle.trim()) else session
    }
    _uiState.value = _uiState.value.copy(sessions = updated)
  }

  fun sendMessage(query: String) {
    if (query.isBlank() || _uiState.value.isStreaming) return

    val userMsg = ZahraMessage(
      sender = Sender.USER,
      text = query.trim()
    )

    val currentSession = _uiState.value.currentSession ?: return
    val currentMsgs = currentSession.messages.toMutableList()
    currentMsgs.add(userMsg)

    // Automatically name the session if it's the first message
    val sessionTitle = if (currentSession.title == "محادثة جديدة" || currentSession.title.startsWith("محادثة جديدة")) {
      query.trim().take(28)
    } else {
      currentSession.title
    }

    val updatedSessions = _uiState.value.sessions.map { s ->
      if (s.id == currentSession.id) {
        s.copy(
          title = sessionTitle,
          messages = currentMsgs,
          updatedAt = System.currentTimeMillis()
        )
      } else s
    }

    _uiState.value = _uiState.value.copy(
      sessions = updatedSessions,
      isStreaming = true,
      streamingText = ""
    )

    viewModelScope.launch {
      val isDeep = _uiState.value.isDeepThinkingEnabled
      val response = router.process(query.trim(), isDeep)

      // Fast, smooth streaming simulation
      val fullText = response.text
      val chunkSize = (fullText.length / 8).coerceAtLeast(3)
      var currentLen = 0

      while (currentLen < fullText.length) {
        currentLen = (currentLen + chunkSize).coerceAtMost(fullText.length)
        _uiState.value = _uiState.value.copy(
          streamingText = fullText.substring(0, currentLen)
        )
        delay(20)
      }

      val zahraMsg = ZahraMessage(
        sender = Sender.ZAHRA,
        text = response.text,
        intent = response.intent,
        confidence = response.confidence,
        confidenceTier = response.confidenceTier,
        outcomeState = response.outcomeState,
        source = response.source,
        followups = response.followups,
        toolPayload = response.toolPayload,
        deepThinking = response.deepThinking
      )

      val finishedMsgs = currentMsgs.toMutableList()
      finishedMsgs.add(zahraMsg)

      val finalSessions = _uiState.value.sessions.map { s ->
        if (s.id == currentSession.id) {
          s.copy(
            messages = finishedMsgs,
            updatedAt = System.currentTimeMillis()
          )
        } else s
      }

      val finalFacts = router.memoryEngine.getFacts()
      val discoveredName = finalFacts.find { it.key.contains("الاسم") }?.value
      val effectiveProfileName = if (!discoveredName.isNullOrBlank()) discoveredName else _uiState.value.userProfileName

      _uiState.value = _uiState.value.copy(
        sessions = finalSessions,
        isStreaming = false,
        streamingText = "",
        userProfileName = effectiveProfileName,
        tasks = router.taskEngine.getTasks(),
        notes = router.noteEngine.getNotes(),
        schedules = router.scheduleEngine.getSchedules(),
        userFacts = finalFacts,
        activeSuggestions = if (response.followups.isNotEmpty()) response.followups else _uiState.value.activeSuggestions
      )
    }
  }

  fun toggleGlassmorphism() {
    _uiState.value = _uiState.value.copy(
      isGlassmorphismEnabled = !_uiState.value.isGlassmorphismEnabled
    )
  }

  // Schedule Management
  fun toggleSchedule(id: String) {
    router.scheduleEngine.toggleSchedule(id)
    _uiState.value = _uiState.value.copy(
      schedules = router.scheduleEngine.getSchedules()
    )
  }

  fun deleteSchedule(id: String) {
    router.scheduleEngine.deleteSchedule(id)
    _uiState.value = _uiState.value.copy(
      schedules = router.scheduleEngine.getSchedules()
    )
  }

  fun addSchedule(title: String, message: String, hour: Int, minute: Int, isDaily: Boolean) {
    val displayPeriod = if (hour < 12) "ص" else "م"
    val displayHour = if (hour % 12 == 0) 12 else hour % 12
    val timeDisplay = String.format(java.util.Locale.getDefault(), "%02d:%02d %s", displayHour, minute, displayPeriod)

    val sched = ZahraSchedule(
      title = title,
      message = message,
      timeDisplay = timeDisplay,
      hour = hour,
      minute = minute,
      isDaily = isDaily,
      isEnabled = true,
      actionType = if (isDaily) "DAILY_TASK" else "REMINDER"
    )
    router.scheduleEngine.addSchedule(sched)
    _uiState.value = _uiState.value.copy(
      schedules = router.scheduleEngine.getSchedules()
    )
  }

  fun testFireScheduleNotification(schedule: ZahraSchedule) {
    router.scheduleEngine.testFireNotification(schedule)
  }

  fun toggleTask(id: String) {
    router.taskEngine.toggleTask(id)
    val updatedTasks = router.taskEngine.getTasks()

    val updatedSessions = _uiState.value.sessions.map { s ->
      val updatedMsgs = s.messages.map { msg ->
        val payload = msg.toolPayload
        if (payload is ZahraToolPayload.TaskPayload && payload.task.id == id) {
          msg.copy(toolPayload = ZahraToolPayload.TaskPayload(payload.task.copy(completed = !payload.task.completed)))
        } else msg
      }
      s.copy(messages = updatedMsgs)
    }

    _uiState.value = _uiState.value.copy(
      tasks = updatedTasks,
      sessions = updatedSessions
    )
  }

  fun addTask(title: String) {
    if (title.isBlank()) return
    router.taskEngine.createTask(title)
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

  fun addNote(title: String, content: String) {
    if (title.isBlank()) return
    router.noteEngine.createNote("$title $content")
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

  // Memory management methods
  fun addFact(category: FactCategory, key: String, value: String) {
    if (key.isBlank() || value.isBlank()) return
    router.memoryEngine.addFact(category, key, value)
    _uiState.value = _uiState.value.copy(
      userFacts = router.memoryEngine.getFacts()
    )
  }

  fun updateFact(id: String, key: String, value: String, category: FactCategory? = null) {
    router.memoryEngine.updateFact(id, key, value, category)
    _uiState.value = _uiState.value.copy(
      userFacts = router.memoryEngine.getFacts()
    )
  }

  fun deleteFact(id: String) {
    router.memoryEngine.deleteFact(id)
    _uiState.value = _uiState.value.copy(
      userFacts = router.memoryEngine.getFacts()
    )
  }

  fun clearMemory() {
    router.memoryEngine.clearMemory()
    _uiState.value = _uiState.value.copy(
      userFacts = emptyList()
    )
  }
}
