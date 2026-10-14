package com.example.zahra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.zahra.model.Sender
import com.example.zahra.model.ZahraMessage
import com.example.zahra.ui.components.ConfidenceBadge
import com.example.zahra.ui.components.GlassCard
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ArchiveScreen(
  messages: List<ZahraMessage>,
  modifier: Modifier = Modifier
) {
  val assistantMessages = messages.filter { it.sender == Sender.ZAHRA }
  val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ZahraBackground)
      .padding(16.dp)
  ) {
    // Header
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.History, contentDescription = "Memory Archive", tint = ZahraCyan, modifier = Modifier.size(24.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(text = "سجل الذاكرة والعمليات", color = ZahraTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = "مخزن الحوارات الحلقي (Ring Buffer) وسجل الثقة والمصادر", color = ZahraTextSecondary, fontSize = 12.sp)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Stats Overview
    Row(
      horizontalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      GlassCard(modifier = Modifier.weight(1f)) {
        Text(text = "العمليات المسجلة", color = ZahraTextSecondary, fontSize = 11.sp)
        Text(text = "${messages.size}", color = ZahraCyan, fontSize = 20.sp, fontWeight = FontWeight.Bold)
      }
      GlassCard(modifier = Modifier.weight(1f)) {
        Text(text = "المصادر الموثقة", color = ZahraTextSecondary, fontSize = 11.sp)
        Text(text = "${assistantMessages.count { !it.source.isNullOrBlank() }}", color = ZahraEmerald, fontSize = 20.sp, fontWeight = FontWeight.Bold)
      }
      GlassCard(modifier = Modifier.weight(1f)) {
        Text(text = "حجم الذاكرة", color = ZahraTextSecondary, fontSize = 11.sp)
        Text(text = "< 2 MB", color = ZahraPink, fontSize = 20.sp, fontWeight = FontWeight.Bold)
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Timeline List
    LazyColumn(
      verticalArrangement = Arrangement.spacedBy(10.dp),
      modifier = Modifier.weight(1f)
    ) {
      items(assistantMessages.reversed()) { msg ->
        GlassCard(modifier = Modifier.fillMaxWidth()) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.fillMaxWidth()
          ) {
            Text(
              text = "قصد: ${msg.intent ?: "عام"}",
              color = ZahraCyan,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
            ConfidenceBadge(tier = msg.confidenceTier, confidence = msg.confidence)
          }

          Spacer(modifier = Modifier.height(6.dp))

          Text(
            text = msg.text,
            color = ZahraTextPrimary,
            fontSize = 13.sp,
            maxLines = 3
          )

          if (!msg.source.isNullOrBlank()) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "المصدر: ${msg.source}",
              color = ZahraTextTertiary,
              fontSize = 10.sp
            )
          }

          Spacer(modifier = Modifier.height(4.dp))
          Text(
            text = "الوقت: ${sdf.format(Date(msg.timestamp))}",
            color = ZahraTextTertiary,
            fontSize = 10.sp
          )
        }
      }
    }
  }
}
