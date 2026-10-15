package com.example.zahra.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.zahra.model.ZahraSession
import com.example.zahra.ui.components.GlassCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArchiveScreen(
  sessions: List<ZahraSession>,
  activeSessionId: String,
  onSelectSession: (String) -> Unit,
  onNewSession: () -> Unit,
  onDeleteSession: (String) -> Unit,
  onRenameSession: (String, String) -> Unit,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var renameSessionTarget by remember { mutableStateOf<ZahraSession?>(null) }
  var newTitleText by remember { mutableStateOf("") }

  val sdf = remember { SimpleDateFormat("yyyy/MM/dd • hh:mm a", Locale.getDefault()) }

  val filteredSessions = remember(sessions, searchQuery) {
    if (searchQuery.isBlank()) sessions
    else {
      sessions.filter { session ->
        session.title.contains(searchQuery, ignoreCase = true) ||
          session.messages.any { it.text.contains(searchQuery, ignoreCase = true) }
      }
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ZahraBackground)
      .padding(16.dp)
  ) {
    // Header & New Chat Action
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Default.History,
          contentDescription = "Chat History",
          tint = ZahraCyan,
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(
            text = "سجل المحادثات",
            color = ZahraTextPrimary,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
          )
          Text(
            text = "${sessions.size} محادثة محفوظة محلياً ومشفرة",
            color = ZahraTextSecondary,
            fontSize = 12.sp
          )
        }
      }

      Button(
        onClick = onNewSession,
        colors = ButtonDefaults.buttonColors(containerColor = ZahraViolet),
        shape = RoundedCornerShape(12.dp),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.testTag("new_chat_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = "New Chat", modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("محادثة جديدة", fontSize = 12.sp, fontWeight = FontWeight.Bold)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Search bar
    OutlinedTextField(
      value = searchQuery,
      onValueChange = { searchQuery = it },
      placeholder = { Text("ابحث في عناوين ونصوص المحادثات...", color = ZahraTextTertiary, fontSize = 12.sp) },
      leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = ZahraTextSecondary, modifier = Modifier.size(18.dp)) },
      trailingIcon = {
        if (searchQuery.isNotEmpty()) {
          IconButton(onClick = { searchQuery = "" }) {
            Icon(Icons.Default.Close, contentDescription = "Clear", tint = ZahraTextSecondary, modifier = Modifier.size(16.dp))
          }
        }
      },
      singleLine = true,
      colors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = ZahraCyan,
        unfocusedBorderColor = ZahraGlassBorder,
        focusedContainerColor = ZahraSurfaceGlass,
        unfocusedContainerColor = ZahraSurfaceGlass,
        focusedTextColor = ZahraTextPrimary,
        unfocusedTextColor = ZahraTextPrimary
      ),
      shape = RoundedCornerShape(14.dp),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("search_sessions_input")
    )

    Spacer(modifier = Modifier.height(14.dp))

    // Sessions List
    if (filteredSessions.isEmpty()) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.Forum, contentDescription = null, tint = ZahraTextTertiary, modifier = Modifier.size(48.dp))
          Spacer(modifier = Modifier.height(12.dp))
          Text(text = "لا توجد محادثات مطابقة للبحث", color = ZahraTextSecondary, fontSize = 14.sp)
        }
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f)
      ) {
        items(filteredSessions, key = { it.id }) { session ->
          val isActive = session.id == activeSessionId
          val lastMsg = session.messages.lastOrNull()?.text ?: "لا توجد رسائل"

          GlassCard(
            modifier = Modifier
              .fillMaxWidth()
              .clickable { onSelectSession(session.id) }
              .testTag("session_card_${session.id}"),
            borderColor = if (isActive) ZahraCyan.copy(alpha = 0.6f) else ZahraGlassBorder
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.SpaceBetween,
              modifier = Modifier.fillMaxWidth()
            ) {
              Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                Box(
                  modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (isActive) ZahraCyan else ZahraTextTertiary.copy(alpha = 0.4f))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = session.title,
                  color = if (isActive) ZahraCyan else ZahraTextPrimary,
                  fontSize = 14.sp,
                  fontWeight = FontWeight.Bold,
                  maxLines = 1
                )
              }

              Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                  Box(
                    modifier = Modifier
                      .clip(RoundedCornerShape(6.dp))
                      .background(ZahraCyan.copy(alpha = 0.2f))
                      .padding(horizontal = 6.dp, vertical = 2.dp)
                  ) {
                    Text("نشطة الآن", color = ZahraCyan, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                  }
                  Spacer(modifier = Modifier.width(6.dp))
                }

                IconButton(
                  onClick = {
                    renameSessionTarget = session
                    newTitleText = session.title
                  },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(Icons.Default.Edit, contentDescription = "Rename", tint = ZahraTextTertiary, modifier = Modifier.size(14.dp))
                }

                IconButton(
                  onClick = { onDeleteSession(session.id) },
                  modifier = Modifier.size(28.dp)
                ) {
                  Icon(Icons.Default.DeleteOutline, contentDescription = "Delete", tint = ZahraPink.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
              }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
              text = lastMsg,
              color = ZahraTextSecondary,
              fontSize = 12.sp,
              maxLines = 2,
              lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Text(
                text = "${session.messages.size} رسالة",
                color = ZahraTextTertiary,
                fontSize = 11.sp
              )
              Text(
                text = sdf.format(Date(session.updatedAt)),
                color = ZahraTextTertiary,
                fontSize = 10.sp
              )
            }
          }
        }
      }
    }
  }

  // Rename Session Dialog
  if (renameSessionTarget != null) {
    AlertDialog(
      onDismissRequest = { renameSessionTarget = null },
      title = { Text("تعديل عنوان المحادثة", color = ZahraTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold) },
      text = {
        OutlinedTextField(
          value = newTitleText,
          onValueChange = { newTitleText = it },
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = ZahraCyan,
            focusedTextColor = ZahraTextPrimary,
            unfocusedTextColor = ZahraTextPrimary
          ),
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            renameSessionTarget?.let { onRenameSession(it.id, newTitleText) }
            renameSessionTarget = null
          }
        ) {
          Text("حفظ", color = ZahraCyan, fontWeight = FontWeight.Bold)
        }
      },
      dismissButton = {
        TextButton(onClick = { renameSessionTarget = null }) {
          Text("إلغاء", color = ZahraTextSecondary)
        }
      },
      containerColor = Color(0xFF16162A),
      shape = RoundedCornerShape(16.dp)
    )
  }
}
