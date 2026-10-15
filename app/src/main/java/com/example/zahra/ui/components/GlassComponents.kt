package com.example.zahra.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GptAccentBlue
import com.example.zahra.model.ConfidenceTier

@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  borderColor: Color = MaterialTheme.colorScheme.outlineVariant,
  backgroundColor: Color = MaterialTheme.colorScheme.surface,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(18.dp))
      .background(backgroundColor)
      .border(
        width = 0.5.dp,
        color = borderColor,
        shape = RoundedCornerShape(18.dp)
      )
      .padding(16.dp),
    content = content
  )
}

@Composable
fun ConfidenceBadge(
  tier: ConfidenceTier,
  confidence: Double,
  modifier: Modifier = Modifier
) {
  val (bgColor, textColor, label) = when (tier) {
    ConfidenceTier.DIRECT -> Triple(
      Color(0xFF10A37F).copy(alpha = 0.12f),
      Color(0xFF10A37F),
      "ثقة عالية (${(confidence * 100).toInt()}%)"
    )
    ConfidenceTier.QUALIFIED -> Triple(
      GptAccentBlue.copy(alpha = 0.12f),
      GptAccentBlue,
      "مؤكد (${(confidence * 100).toInt()}%)"
    )
    ConfidenceTier.UNCERTAIN -> Triple(
      Color(0xFFF59E0B).copy(alpha = 0.12f),
      Color(0xFFF59E0B),
      "تقريبي (${(confidence * 100).toInt()}%)"
    )
  }

  Box(
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(bgColor)
      .padding(horizontal = 8.dp, vertical = 3.dp)
  ) {
    Text(
      text = label,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.SemiBold
    )
  }
}

@Composable
fun QuickReplyChip(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier
) {
  Box(
    modifier = modifier
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant)
      .border(
        width = 0.5.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        shape = RoundedCornerShape(16.dp)
      )
      .clickable(onClick = onClick)
      .padding(horizontal = 14.dp, vertical = 8.dp)
  ) {
    Text(
      text = text,
      color = MaterialTheme.colorScheme.onSurface,
      fontSize = 13.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun SourceCitationCard(
  source: String,
  isVerified: Boolean = true,
  modifier: Modifier = Modifier
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(10.dp))
      .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
      .padding(horizontal = 12.dp, vertical = 6.dp)
  ) {
    Box(
      modifier = Modifier
        .size(6.dp)
        .clip(CircleShape)
        .background(if (isVerified) Color(0xFF10A37F) else Color(0xFFF59E0B))
    )
    Spacer(modifier = Modifier.width(8.dp))
    Text(
      text = "المصدر: $source",
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 11.sp,
      fontWeight = FontWeight.Normal
    )
  }
}
