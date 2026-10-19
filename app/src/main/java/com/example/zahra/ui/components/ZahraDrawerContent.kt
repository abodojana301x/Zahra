package com.example.zahra.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GptAccentBlue
import com.example.zahra.model.ZahraSession

@Composable
fun ZahraDrawerContent(
  sessions: List<ZahraSession>,
  activeSessionId: String,
  pinnedSessionIds: Set<String>,
  searchQuery: String,
  userProfileName: String,
  onSearchQueryChange: (String) -> Unit,
  onSelectSession: (String) -> Unit,
  onNewChat: () -> Unit,
  onOpenSettings: () -> Unit,
  onCloseDrawer: () -> Unit,
  onNavigateToScreen: (com.example.zahra.ui.ZahraScreen) -> Unit = {},
  modifier: Modifier = Modifier
) {
  var isSearchExpanded by remember { mutableStateOf(false) }

  val filteredSessions = remember(sessions, searchQuery) {
    if (searchQuery.isBlank()) sessions
    else sessions.filter { it.title.contains(searchQuery.trim(), ignoreCase = true) }
  }

  val pinnedSessions = filteredSessions.filter { pinnedSessionIds.contains(it.id) }
  val recentSessions = filteredSessions.filter { !pinnedSessionIds.contains(it.id) }

  ModalDrawerSheet(
    drawerContainerColor = MaterialTheme.colorScheme.surface,
    drawerTonalElevation = 2.dp,
    modifier = modifier
      .width(310.dp)
      .fillMaxHeight()
      .windowInsetsPadding(WindowInsets.statusBars)
      .testTag("zahra_drawer_content")
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
      // Top Drawer Header
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .padding(bottom = 8.dp)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Image(
            painter = painterResource(id = R.drawable.ic_zahra_logo),
            contentDescription = "شعار زهرة",
            modifier = Modifier
              .size(34.dp)
              .clip(CircleShape)
          )
          Spacer(modifier = Modifier.width(10.dp))
          Text(
            text = "زهرة 3",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 2.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Surface(
            shape = CircleShape,
            color = Color(0xFF10A37F).copy(alpha = 0.15f)
          ) {
            Text(
              text = "وكيل ذكي",
              color = Color(0xFF10A37F),
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
            )
          }
        }

        IconButton(
          onClick = { isSearchExpanded = !isSearchExpanded },
          modifier = Modifier
            .size(38.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag("drawer_search_toggle")
        ) {
          Icon(
            imageVector = Icons.Outlined.Search,
            contentDescription = "بحث",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
          )
        }
      }

      // Search input field when expanded
      AnimatedVisibility(visible = isSearchExpanded) {
        OutlinedTextField(
          value = searchQuery,
          onValueChange = onSearchQueryChange,
          placeholder = { Text("بحث في المحادثات...", fontSize = 13.sp) },
          singleLine = true,
          shape = RoundedCornerShape(14.dp),
          colors = OutlinedTextFieldDefaults.colors(
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant
          ),
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
            .testTag("drawer_search_input")
        )
      }

      // Scrollable content: Zahra offline smart features, Pinned section, Recent chats
      LazyColumn(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(2.dp)
      ) {
        // Zahra Core Agent Capabilities
        item {
          DrawerCategoryItem("الجدولة والتنبيهات (الوكيل)", Icons.Outlined.Alarm) {
            onNavigateToScreen(com.example.zahra.ui.ZahraScreen.SCHEDULE)
            onCloseDrawer()
          }
        }
        item {
          DrawerCategoryItem("المهام الذكية", Icons.Outlined.CheckCircleOutline) {
            onNavigateToScreen(com.example.zahra.ui.ZahraScreen.TASKS)
            onCloseDrawer()
          }
        }
        item {
          DrawerCategoryItem("الملاحظات المشفرة", Icons.Outlined.EditNote) {
            onNavigateToScreen(com.example.zahra.ui.ZahraScreen.NOTES)
            onCloseDrawer()
          }
        }
        item {
          DrawerCategoryItem("الشريعة ومواقيت الصلاة", Icons.Outlined.Mosque) {
            onNavigateToScreen(com.example.zahra.ui.ZahraScreen.RELIGION)
            onCloseDrawer()
          }
        }
        item {
          DrawerCategoryItem("الترجمة المعتمدة", Icons.Outlined.Translate) {
            onNavigateToScreen(com.example.zahra.ui.ZahraScreen.TRANSLATE)
            onCloseDrawer()
          }
        }
        item {
          DrawerCategoryItem("الإعدادات والذاكرة", Icons.Outlined.Tune) {
            onNavigateToScreen(com.example.zahra.ui.ZahraScreen.SETTINGS)
            onCloseDrawer()
          }
        }

        // Section: مثبت (Pinned)
        if (pinnedSessions.isNotEmpty()) {
          item {
            Text(
              text = "مثبت",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 16.dp, bottom = 6.dp, start = 8.dp)
            )
          }

          items(pinnedSessions, key = { "pinned_${it.id}" }) { session ->
            DrawerSessionItem(
              title = session.title,
              isSelected = session.id == activeSessionId,
              icon = Icons.Outlined.ChatBubbleOutline,
              onClick = {
                onSelectSession(session.id)
                onCloseDrawer()
              }
            )
          }
        }

        // Section: المحادثات الأخيرة (Recent Chats)
        if (recentSessions.isNotEmpty()) {
          item {
            Text(
              text = "المحادثات الأخيرة",
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.padding(top = 16.dp, bottom = 6.dp, start = 8.dp)
            )
          }

          items(recentSessions, key = { it.id }) { session ->
            DrawerSessionItem(
              title = session.title.ifBlank { "محادثة جديدة" },
              isSelected = session.id == activeSessionId,
              icon = null,
              onClick = {
                onSelectSession(session.id)
                onCloseDrawer()
              }
            )
          }
        }
      }

      HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant,
        modifier = Modifier.padding(vertical = 10.dp)
      )

      // Bottom User Bar & New Chat Button
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(bottom = 6.dp)
      ) {
        // User Profile Row (Avatar + Name) -> opens Settings
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable {
              onOpenSettings()
              onCloseDrawer()
            }
            .padding(6.dp)
            .testTag("drawer_user_profile_button")
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(38.dp)
              .clip(CircleShape)
              .background(Color(0xFF1E1E24))
          ) {
            Text(
              text = userProfileName.take(1).ifBlank { "م" },
              color = Color.White,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Text(
            text = userProfileName,
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onSurface
          )
        }

        // Blue Pill "دردشة جديدة" (New Chat) Button
        Button(
          onClick = {
            onNewChat()
            onCloseDrawer()
          },
          colors = ButtonDefaults.buttonColors(containerColor = GptAccentBlue),
          shape = CircleShape,
          contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
          modifier = Modifier.testTag("drawer_new_chat_button")
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Edit,
              contentDescription = null,
              tint = Color.White,
              modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text("محادثة جديدة", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          }
        }
      }
    }
  }
}

@Composable
private fun DrawerCategoryItem(
  title: String,
  icon: ImageVector,
  onClick: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 10.dp)
  ) {
    Text(
      text = title,
      fontSize = 15.sp,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f)
    )

    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.size(20.dp)
    )
  }
}

@Composable
private fun DrawerSessionItem(
  title: String,
  isSelected: Boolean,
  icon: ImageVector?,
  onClick: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(
        if (isSelected) MaterialTheme.colorScheme.surfaceVariant
        else Color.Transparent
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 8.dp, vertical = 9.dp)
  ) {
    Text(
      text = title,
      fontSize = 14.sp,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
      color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
      modifier = Modifier.weight(1f)
    )

    if (icon != null) {
      Spacer(modifier = Modifier.width(8.dp))
      Icon(
        imageVector = icon,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.size(16.dp)
      )
    }
  }
}
