package com.example.zahra.ui.screens

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.zahra.model.Sender
import com.example.zahra.model.ZahraMessage
import com.example.zahra.ui.ZahraUiState
import com.example.zahra.ui.components.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
  state: ZahraUiState,
  onSendMessage: (String) -> Unit,
  onToggleTask: (String) -> Unit,
  onNavigateToMemory: () -> Unit,
  onOpenDrawer: () -> Unit,
  onNewChat: () -> Unit,
  onToggleVoiceMode: (Boolean) -> Unit,
  isDeepThinkingEnabled: Boolean,
  onToggleDeepThinking: () -> Unit,
  modifier: Modifier = Modifier
) {
  var inputText by remember { mutableStateOf("") }
  var showToolsSheet by remember { mutableStateOf(false) }
  val listState = rememberLazyListState()
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  // Auto-scroll when new messages arrive or when streaming
  LaunchedEffect(state.messages.size, state.isStreaming) {
    if (state.messages.isNotEmpty()) {
      listState.animateScrollToItem(state.messages.size - 1)
    }
  }

  Box(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.background)
  ) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .windowInsetsPadding(WindowInsets.statusBars)
    ) {
      // Top App Bar
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        // Left: Circular Button (New Chat / Reset)
        IconButton(
          onClick = onNewChat,
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag("chat_new_chat_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.Edit,
            contentDescription = "محادثة جديدة",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(20.dp)
          )
        }

        // Center: App Title & Offline Badge (No subscriptions)
        // Center: App Title & Offline Badge (Zahra 3)
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.Center,
          modifier = Modifier.testTag("chat_top_bar_title")
        ) {
          Text(
            text = "زهرة 3",
            fontSize = 19.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
          )
          Spacer(modifier = Modifier.width(8.dp))
          Surface(
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.height(24.dp)
          ) {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier.padding(horizontal = 8.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(6.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF10A37F))
              )
              Spacer(modifier = Modifier.width(5.dp))
              Text(
                text = "وكيل محلي",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
              )
            }
          }
        }

        // Right: Circular Button (Drawer Toggle with 2 horizontal lines)
        IconButton(
          onClick = onOpenDrawer,
          modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .testTag("chat_drawer_toggle_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.Menu,
            contentDescription = "القائمة الجانبية",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(22.dp)
          )
        }
      }

      // Middle Content: Empty State (Screenshot 1) OR Messages List
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) {
        if (state.messages.isEmpty()) {
          // Empty State matching Screenshot 1
          Column(
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.End
          ) {
            // Suggestion 1: ابدأ دردشة صوتية
            ChatSuggestionRow(
              title = "ابدأ دردشة صوتية",
              icon = Icons.Outlined.Headphones,
              tag = "suggestion_voice",
              onClick = { onToggleVoiceMode(true) }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Suggestion 2: إنشاء صورة أو ملصق
            ChatSuggestionRow(
              title = "إنشاء صورة أو ملصق",
              icon = Icons.Outlined.Image,
              tag = "suggestion_image",
              onClick = {
                onSendMessage("أنشئ لي صورة إجرائية تعبيرية جميلة")
              }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Suggestion 3: جدولة موعد للوكيل
            ChatSuggestionRow(
              title = "جدولة موعد للوكيل",
              icon = Icons.Outlined.Alarm,
              tag = "suggestion_schedule",
              onClick = {
                onSendMessage("فكرني كل يوم الساعة 8 صباحاً بالمهام والأذكار")
              }
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Suggestion 4: الكتابة أو التحرير
            ChatSuggestionRow(
              title = "الكتابة أو التحرير",
              icon = Icons.Outlined.Edit,
              tag = "suggestion_writing",
              onClick = {
                onSendMessage("ساعدني في صياغة نص احترافي باللغة العربية الفصحى")
              }
            )

            Spacer(modifier = Modifier.height(16.dp))
          }
        } else {
          // Messages LazyColumn
          LazyColumn(
            state = listState,
            modifier = Modifier
              .fillMaxSize()
              .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
          ) {
            items(state.messages) { message ->
              if (message.sender == Sender.USER) {
                // User Message: Clean bubble aligned to end
                Row(
                  modifier = Modifier.fillMaxWidth(),
                  horizontalArrangement = Arrangement.Start
                ) {
                  Box(
                    modifier = Modifier
                      .widthIn(max = 300.dp)
                      .clip(RoundedCornerShape(20.dp))
                      .background(MaterialTheme.colorScheme.surfaceVariant)
                      .padding(horizontal = 16.dp, vertical = 12.dp)
                  ) {
                    Text(
                      text = message.text,
                      color = MaterialTheme.colorScheme.onSurface,
                      fontSize = 15.sp,
                      lineHeight = 22.sp
                    )
                  }
                }
              } else {
                // Assistant Message: Clean text with copy & tools
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                ) {
                  // Chain of Thought if Deep Thinking was active
                  if (message.deepThinking != null) {
                    DeepThinkingCard(payload = message.deepThinking)
                    Spacer(modifier = Modifier.height(10.dp))
                  }

                  Text(
                    text = message.text,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    lineHeight = 23.sp
                  )

                  // Tool payload if available (Religion, Tasks, Notes, Math, Translate, Image, Schedule)
                  if (message.toolPayload != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    InteractiveToolCard(
                      payload = message.toolPayload,
                      onToggleTask = onToggleTask,
                      onQuickQuery = { onSendMessage(it) },
                      onNavigateToMemory = onNavigateToMemory
                    )
                  }

                  Spacer(modifier = Modifier.height(8.dp))

                  // Actions row: Copy & Source
                  Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Start
                  ) {
                    IconButton(
                      onClick = {
                        clipboardManager.setText(AnnotatedString(message.text))
                        Toast.makeText(context, "تم نسخ الرد", Toast.LENGTH_SHORT).show()
                      },
                      modifier = Modifier.size(32.dp)
                    ) {
                      Icon(
                        imageVector = Icons.Outlined.ContentCopy,
                        contentDescription = "نسخ الرد",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                      )
                    }

                    if (!message.source.isNullOrBlank()) {
                      Text(
                        text = "• ${message.source}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 11.sp
                      )
                    }
                  }
                }
              }
            }

            // Live streaming simulation bubble
            if (state.isStreaming && state.streamingText.isNotBlank()) {
              item {
                Column(
                  modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                ) {
                  Text(
                    text = state.streamingText,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 15.sp,
                    lineHeight = 23.sp
                  )
                }
              }
            }
          }
        }
      }

      // Deep thinking badge if enabled
      AnimatedVisibility(visible = isDeepThinkingEnabled) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 2.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Psychology,
            contentDescription = null,
            tint = GptAccentBlue,
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = "وضع التفكير بعمق أكبر مفعل",
            color = GptAccentBlue,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }

      // Bottom Input Capsule (matching Screenshot 1)
      Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(horizontal = 16.dp, vertical = 8.dp)
          .heightIn(min = 54.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          // Right in RTL: Plus (+) Button
          IconButton(
            onClick = { showToolsSheet = true },
            modifier = Modifier
              .size(38.dp)
              .testTag("chat_tools_plus_button")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "إضافة أدوات ومرفقات",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(24.dp)
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          // Center: Text Input Field
          Box(
            modifier = Modifier.weight(1f),
            contentAlignment = Alignment.CenterEnd
          ) {
            if (inputText.isEmpty()) {
              Text(
                text = "اسأل زهراء",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 15.sp,
                modifier = Modifier.fillMaxWidth()
              )
            }
            BasicTextField(
              value = inputText,
              onValueChange = { inputText = it },
              textStyle = TextStyle(
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 15.sp
              ),
              cursorBrush = SolidColor(GptAccentBlue),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("chat_input_text_field")
            )
          }

          // Quick Deep Thinking toggle button
          IconButton(
            onClick = onToggleDeepThinking,
            modifier = Modifier
              .size(38.dp)
              .testTag("chat_quick_deep_thinking_button")
          ) {
            Icon(
              imageVector = if (isDeepThinkingEnabled) Icons.Filled.Psychology else Icons.Outlined.Psychology,
              contentDescription = "التفكير بعمق",
              tint = if (isDeepThinkingEnabled) Color(0xFF8B5CF6) else MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(23.dp)
            )
          }

          Spacer(modifier = Modifier.width(2.dp))

          // Left in RTL: Mic button & Blue Voice/Send circle button
          IconButton(
            onClick = { onToggleVoiceMode(true) },
            modifier = Modifier
              .size(38.dp)
              .testTag("chat_mic_button")
          ) {
            Icon(
              imageVector = Icons.Outlined.Mic,
              contentDescription = "تحدث صوتياً",
              tint = MaterialTheme.colorScheme.onSurface,
              modifier = Modifier.size(22.dp)
            )
          }

          Spacer(modifier = Modifier.width(4.dp))

          // Blue Voice/Send Button
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(GptAccentBlue)
              .clickable {
                if (inputText.isNotBlank()) {
                  onSendMessage(inputText)
                  inputText = ""
                } else {
                  onToggleVoiceMode(true)
                }
              }
              .testTag("chat_voice_or_send_button")
          ) {
            if (inputText.isNotBlank()) {
              Icon(
                imageVector = Icons.AutoMirrored.Filled.Send,
                contentDescription = "إرسال",
                tint = Color.White,
                modifier = Modifier.size(18.dp)
              )
            } else {
              Icon(
                imageVector = Icons.Default.GraphicEq,
                contentDescription = "الوضع الصوتي التفاعلي",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }
      }
    }
  }

  // Tools Bottom Sheet (+)
  if (showToolsSheet) {
    ToolsBottomSheet(
      isDeepThinkingEnabled = isDeepThinkingEnabled,
      onToggleDeepThinking = onToggleDeepThinking,
      onSelectAction = { query ->
        onSendMessage(query)
      },
      onDismiss = { showToolsSheet = false }
    )
  }
}

@Composable
private fun ChatSuggestionRow(
  title: String,
  icon: ImageVector,
  tag: String,
  onClick: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .clip(RoundedCornerShape(12.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 6.dp)
      .testTag(tag)
  ) {
    Text(
      text = title,
      fontSize = 15.sp,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.width(12.dp))

    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.size(22.dp)
    )
  }
}
