package com.example.zahra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.zahra.model.Sender
import com.example.zahra.model.ZahraMessage
import com.example.zahra.ui.ZahraUiState
import com.example.zahra.ui.components.ConfidenceBadge
import com.example.zahra.ui.components.QuickReplyChip
import com.example.zahra.ui.components.SourceCitationCard
import kotlinx.coroutines.launch

@Composable
fun ChatScreen(
  state: ZahraUiState,
  onSendMessage: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var inputText by remember { mutableStateOf("") }
  val listState = rememberLazyListState()
  val scope = rememberCoroutineScope()

  LaunchedEffect(state.messages.size, state.isStreaming) {
    if (state.messages.isNotEmpty()) {
      listState.animateScrollToItem(state.messages.size)
    }
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ZahraBackground)
  ) {
    // Top Offline Status Banner
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .background(Color(0xFF0F172A).copy(alpha = 0.8f))
        .border(width = 0.5.dp, color = ZahraGlassBorder)
        .padding(horizontal = 16.dp, vertical = 6.dp)
    ) {
      Icon(
        imageVector = Icons.Default.Shield,
        contentDescription = "Zero Network Shield",
        tint = ZahraEmerald,
        modifier = Modifier.size(14.dp)
      )
      Spacer(modifier = Modifier.width(6.dp))
      Text(
        text = "100% بدون إنترنت • صفر تتبع • ذكاء حتمي",
        color = ZahraEmerald,
        fontSize = 11.sp,
        fontWeight = FontWeight.SemiBold
      )
      Spacer(modifier = Modifier.weight(1f))
      Text(
        text = "Zahra 2.0",
        color = ZahraTextSecondary,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold
      )
    }

    // Messages List
    LazyColumn(
      state = listState,
      modifier = Modifier
        .weight(1f)
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      items(state.messages, key = { it.id }) { msg ->
        MessageBubble(msg = msg)
      }

      // Streaming bubble simulation
      if (state.isStreaming && state.streamingText.isNotEmpty()) {
        item {
          Box(
            modifier = Modifier
              .fillMaxWidth()
              .padding(end = 48.dp)
              .clip(RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp))
              .background(ZahraSurfaceGlass)
              .border(1.dp, ZahraGlassBorder, RoundedCornerShape(18.dp))
              .padding(14.dp)
          ) {
            Text(
              text = state.streamingText,
              color = ZahraTextPrimary,
              fontSize = 14.sp,
              lineHeight = 22.sp
            )
          }
        }
      }
    }

    // Quick Action Suggestions Row
    if (state.activeSuggestions.isNotEmpty() && !state.isStreaming) {
      LazyRow(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(state.activeSuggestions) { suggestion ->
          QuickReplyChip(
            text = suggestion,
            onClick = {
              onSendMessage(suggestion)
            }
          )
        }
      }
    }

    // Input Bar
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp, vertical = 8.dp)
        .clip(RoundedCornerShape(24.dp))
        .background(ZahraSurfaceGlass)
        .border(1.dp, ZahraGlassBorder, RoundedCornerShape(24.dp))
        .padding(horizontal = 12.dp, vertical = 4.dp)
    ) {
      TextField(
        value = inputText,
        onValueChange = { inputText = it },
        placeholder = {
          Text(
            text = "اكتب لزهرة (حساب، مواقيت، ترجمة، مهام)...",
            color = ZahraTextTertiary,
            fontSize = 13.sp
          )
        },
        colors = TextFieldDefaults.colors(
          focusedContainerColor = Color.Transparent,
          unfocusedContainerColor = Color.Transparent,
          disabledContainerColor = Color.Transparent,
          focusedIndicatorColor = Color.Transparent,
          unfocusedIndicatorColor = Color.Transparent,
          focusedTextColor = ZahraTextPrimary,
          unfocusedTextColor = ZahraTextPrimary,
          cursorColor = ZahraCyan
        ),
        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
        keyboardActions = KeyboardActions(onSend = {
          if (inputText.isNotBlank()) {
            val q = inputText
            inputText = ""
            onSendMessage(q)
          }
        }),
        modifier = Modifier
          .weight(1f)
          .testTag("chat_input_field")
      )

      IconButton(
        onClick = {
          if (inputText.isNotBlank()) {
            val q = inputText
            inputText = ""
            onSendMessage(q)
          }
        },
        enabled = inputText.isNotBlank() && !state.isStreaming,
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(
            if (inputText.isNotBlank()) Brush.linearGradient(listOf(ZahraViolet, ZahraCyan))
            else Brush.linearGradient(listOf(Color(0xFF2A2A3A), Color(0xFF2A2A3A)))
          )
          .testTag("chat_send_button")
      ) {
        Icon(
          imageVector = Icons.AutoMirrored.Filled.Send,
          contentDescription = "Send Message",
          tint = if (inputText.isNotBlank()) Color.White else ZahraTextTertiary,
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}

@Composable
fun MessageBubble(msg: ZahraMessage) {
  val isUser = msg.sender == Sender.USER

  Column(
    horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    modifier = Modifier.fillMaxWidth()
  ) {
    Box(
      modifier = Modifier
        .widthIn(max = 320.dp)
        .clip(
          if (isUser) RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
          else RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
        )
        .background(
          if (isUser) Brush.linearGradient(listOf(ZahraViolet, Color(0xFF4C1D95)))
          else Brush.linearGradient(listOf(Color(0xFF161626), Color(0xFF1E1E30)))
        )
        .border(
          width = 1.dp,
          color = if (isUser) ZahraViolet.copy(alpha = 0.5f) else ZahraGlassBorder,
          shape = if (isUser) RoundedCornerShape(topStart = 18.dp, topEnd = 4.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
          else RoundedCornerShape(topStart = 4.dp, topEnd = 18.dp, bottomStart = 18.dp, bottomEnd = 18.dp)
        )
        .padding(14.dp)
    ) {
      Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        if (!isUser && msg.intent != null) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "زهرة • ${msg.intent}",
              color = ZahraCyan,
              fontSize = 11.sp,
              fontWeight = FontWeight.Bold
            )
            ConfidenceBadge(tier = msg.confidenceTier, confidence = msg.confidence)
          }
        }

        Text(
          text = msg.text,
          color = ZahraTextPrimary,
          fontSize = 14.sp,
          lineHeight = 22.sp
        )

        if (!isUser && !msg.source.isNullOrBlank()) {
          Spacer(modifier = Modifier.height(4.dp))
          SourceCitationCard(sourceText = msg.source)
        }
      }
    }
  }
}
