package com.example.zahra.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.ZahraTheme
import com.example.zahra.ui.components.VoiceModeDialog
import com.example.zahra.ui.components.ZahraDrawerContent
import com.example.zahra.ui.screens.*
import kotlinx.coroutines.launch

@Composable
fun ZahraApp(
  viewModel: ZahraViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsState()
  val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
  val scope = rememberCoroutineScope()

  // Native Arabic RTL Layout Direction
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    ZahraTheme {
      // Global BackHandler: Close drawer if open, else navigate back to chat
      BackHandler(enabled = drawerState.isOpen || uiState.currentScreen != ZahraScreen.CHAT) {
        if (drawerState.isOpen) {
          scope.launch { drawerState.close() }
        } else if (uiState.currentScreen != ZahraScreen.CHAT) {
          viewModel.navigateToScreen(ZahraScreen.CHAT)
        }
      }

      ModalNavigationDrawer(
        drawerState = drawerState,
        gesturesEnabled = uiState.currentScreen == ZahraScreen.CHAT,
        drawerContent = {
          ZahraDrawerContent(
            sessions = uiState.sessions,
            activeSessionId = uiState.activeSessionId,
            pinnedSessionIds = uiState.pinnedSessionIds,
            searchQuery = uiState.searchDrawerQuery,
            userProfileName = uiState.userProfileName,
            onSearchQueryChange = { viewModel.setSearchDrawerQuery(it) },
            onSelectSession = { viewModel.switchSession(it) },
            onNewChat = { viewModel.startNewSession() },
            onOpenSettings = { viewModel.navigateToScreen(ZahraScreen.SETTINGS) },
            onCloseDrawer = { scope.launch { drawerState.close() } },
            onNavigateToScreen = { screen -> viewModel.navigateToScreen(screen) }
          )
        }
      ) {
        Surface(
          modifier = Modifier.fillMaxSize(),
          color = MaterialTheme.colorScheme.background
        ) {
          when (uiState.currentScreen) {
            ZahraScreen.CHAT -> {
              ChatScreen(
                state = uiState,
                onSendMessage = { viewModel.sendMessage(it) },
                onToggleTask = { viewModel.toggleTask(it) },
                onNavigateToMemory = { viewModel.navigateToScreen(ZahraScreen.SETTINGS) },
                onOpenDrawer = { scope.launch { drawerState.open() } },
                onNewChat = { viewModel.startNewSession() },
                onToggleVoiceMode = { viewModel.setVoiceModeActive(it) },
                isDeepThinkingEnabled = uiState.isDeepThinkingEnabled,
                onToggleDeepThinking = { viewModel.toggleDeepThinking() }
              )
            }

            ZahraScreen.SCHEDULE -> {
              ScheduleScreen(
                schedules = uiState.schedules,
                onToggleSchedule = { viewModel.toggleSchedule(it) },
                onDeleteSchedule = { viewModel.deleteSchedule(it) },
                onAddSchedule = { title, msg, h, min, isDaily ->
                  viewModel.addSchedule(title, msg, h, min, isDaily)
                },
                onTestNotification = { viewModel.testFireScheduleNotification(it) },
                onBackToChat = { viewModel.navigateToScreen(ZahraScreen.CHAT) }
              )
            }

            ZahraScreen.SETTINGS -> {
              SettingsScreen(
                userProfileName = uiState.userProfileName,
                accentColorName = uiState.accentColorName,
                userFacts = uiState.userFacts,
                onUpdateProfileName = { viewModel.updateProfileName(it) },
                onAddFact = { cat, k, v -> viewModel.addFact(cat, k, v) },
                onUpdateFact = { id, k, v, c -> viewModel.updateFact(id, k, v, c) },
                onDeleteFact = { viewModel.deleteFact(it) },
                onClearMemory = { viewModel.clearMemory() },
                onSelectAccentColor = { viewModel.setAccentColor(it) },
                onBackToChat = { viewModel.navigateToScreen(ZahraScreen.CHAT) },
                isGlassmorphismEnabled = uiState.isGlassmorphismEnabled,
                onToggleGlassmorphism = { viewModel.toggleGlassmorphism() },
                onNavigateToSchedule = { viewModel.navigateToScreen(ZahraScreen.SCHEDULE) }
              )
            }

            ZahraScreen.TASKS -> {
              TasksScreen(
                tasks = uiState.tasks,
                onToggleTask = { viewModel.toggleTask(it) },
                onDeleteTask = { viewModel.deleteTask(it) },
                onAddTask = { viewModel.addTask(it) }
              )
            }

            ZahraScreen.NOTES -> {
              NotesScreen(
                notes = uiState.notes,
                onAddNote = { title, content -> viewModel.addNote(title, content ?: "") },
                onDeleteNote = { viewModel.deleteNote(it) }
              )
            }

            ZahraScreen.RELIGION -> {
              ReligionScreen(
                onQueryReligion = { query -> viewModel.router.religionEngine.query(query).text }
              )
            }

            ZahraScreen.TRANSLATE -> {
              TranslateScreen(
                onTranslate = { text, target ->
                  viewModel.router.translationEngine.translate(text, target).translatedText
                }
              )
            }

            ZahraScreen.ARCHIVE -> {
              ArchiveScreen(
                sessions = uiState.sessions,
                activeSessionId = uiState.activeSessionId,
                onSelectSession = { viewModel.switchSession(it) },
                onNewSession = { viewModel.startNewSession() },
                onDeleteSession = { viewModel.deleteSession(it) },
                onRenameSession = { id, title -> viewModel.renameSession(id, title) }
              )
            }
          }
        }
      }

      // Voice Mode Interactive Overlay Dialog
      if (uiState.isVoiceModeActive) {
        VoiceModeDialog(
          onDismiss = { viewModel.setVoiceModeActive(false) },
          onVoiceInputCompleted = { speechText ->
            viewModel.sendMessage(speechText)
          }
        )
      }
    }
  }
}
