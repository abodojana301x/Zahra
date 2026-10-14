package com.example.zahra.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.theme.*
import com.example.zahra.ui.screens.*

@Composable
fun ZahraApp(
  viewModel: ZahraViewModel = viewModel()
) {
  val uiState by viewModel.uiState.collectAsState()

  // Provide native Arabic RTL support
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
    Scaffold(
      containerColor = ZahraBackground,
      contentWindowInsets = WindowInsets.safeDrawing,
      bottomBar = {
        ZahraBottomNavBar(
          currentTab = uiState.currentTab,
          onSelectTab = { viewModel.switchTab(it) }
        )
      }
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
      ) {
        when (uiState.currentTab) {
          AppTab.CHAT -> ChatScreen(
            state = uiState,
            onSendMessage = { viewModel.sendMessage(it) }
          )
          AppTab.TASKS -> TasksScreen(
            tasks = uiState.tasks,
            onToggleTask = { viewModel.toggleTask(it) },
            onDeleteTask = { viewModel.deleteTask(it) },
            onAddTask = { viewModel.createTaskDirect(it) }
          )
          AppTab.NOTES -> NotesScreen(
            notes = uiState.notes,
            onAddNote = { content, title -> viewModel.createNoteDirect(content, title) },
            onDeleteNote = { viewModel.deleteNote(it) }
          )
          AppTab.TRANSLATE -> TranslateScreen(
            onTranslate = { text, target -> viewModel.translateDirect(text, target) }
          )
          AppTab.RELIGION -> ReligionScreen(
            onQueryReligion = { viewModel.queryReligionDirect(it) }
          )
          AppTab.ARCHIVE -> ArchiveScreen(
            messages = uiState.messages
          )
          AppTab.SETTINGS -> SettingsScreen()
        }
      }
    }
  }
}

@Composable
fun ZahraBottomNavBar(
  currentTab: AppTab,
  onSelectTab: (AppTab) -> Unit
) {
  val tabs = listOf(
    TabItem(AppTab.CHAT, Icons.Default.ChatBubble, "محادثة"),
    TabItem(AppTab.TASKS, Icons.Default.Checklist, "مهام"),
    TabItem(AppTab.NOTES, Icons.Default.EditNote, "ملاحظات"),
    TabItem(AppTab.TRANSLATE, Icons.Default.Translate, "ترجمة"),
    TabItem(AppTab.RELIGION, Icons.Default.Mosque, "الشريعة"),
    TabItem(AppTab.ARCHIVE, Icons.Default.History, "أرشيف"),
    TabItem(AppTab.SETTINGS, Icons.Default.Settings, "إعدادات")
  )

  Surface(
    color = ZahraSurface.copy(alpha = 0.95f),
    modifier = Modifier
      .fillMaxWidth()
      .windowInsetsPadding(WindowInsets.navigationBars)
      .border(
        width = 0.5.dp,
        color = ZahraGlassBorder
      )
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 4.dp, vertical = 6.dp),
      horizontalArrangement = Arrangement.SpaceAround,
      verticalAlignment = Alignment.CenterVertically
    ) {
      tabs.forEach { item ->
        val isSelected = currentTab == item.tab
        Column(
          horizontalAlignment = Alignment.CenterHorizontally,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onSelectTab(item.tab) }
            .padding(horizontal = 6.dp, vertical = 4.dp)
        ) {
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(10.dp))
              .background(if (isSelected) ZahraCyan.copy(alpha = 0.2f) else Color.Transparent)
              .padding(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Icon(
              imageVector = item.icon,
              contentDescription = item.label,
              tint = if (isSelected) ZahraCyan else ZahraTextTertiary,
              modifier = Modifier.size(20.dp)
            )
          }
          Spacer(modifier = Modifier.height(2.dp))
          Text(
            text = item.label,
            color = if (isSelected) ZahraCyan else ZahraTextTertiary,
            fontSize = 9.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
          )
        }
      }
    }
  }
}

private data class TabItem(
  val tab: AppTab,
  val icon: ImageVector,
  val label: String
)
