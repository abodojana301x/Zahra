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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.zahra.model.ConfidenceTier

@Composable
fun GlassCard(
  modifier: Modifier = Modifier,
  borderColor: Color = ZahraGlassBorder,
  backgroundColor: Color = ZahraSurfaceGlass,
  content: @Composable ColumnScope.() -> Unit
) {
  Column(
    modifier = modifier
      .clip(RoundedCornerShape(20.dp))
      .background(backgroundColor)
      .border(
        width = 1.dp,
        brush = Brush.linearGradient(
          listOf(
            borderColor.copy(alpha = 0.35f),
            ZahraViolet.copy(alpha = 0.15f),
            borderColor.copy(alpha = 0.10f)
          )
        ),
        shape = RoundedCornerShape(20.dp)
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
      ZahraEmerald.copy(alpha = 0.2f),
      ZahraEmerald,
      "ثقة عالية (${(confidence * 100).toInt()}%)"
    )
    ConfidenceTier.QUALIFIED -> Triple(
      ZahraAmber.copy(alpha = 0.2f),
      ZahraAmber,
      "ثقة متوسطة (${(confidence * 100).toInt()}%)"
    )
    ConfidenceTier.UNCERTAIN -> Triple(
      ZahraPink.copy(alpha = 0.2f),
      ZahraPink,
      "غير مؤكد (امتناع حتمي)"
    )
  }

  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .clip(RoundedCornerShape(8.dp))
      .background(bgColor)
      .padding(horizontal = 8.dp, vertical = 4.dp)
  ) {
    Box(
      modifier = Modifier
        .size(6.dp)
        .clip(CircleShape)
        .background(textColor)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = label,
      color = textColor,
      fontSize = 11.sp,
      fontWeight = FontWeight.Medium
    )
  }
}

@Composable
fun SourceCitationCard(
  sourceText: String,
  modifier: Modifier = Modifier
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(12.dp))
      .background(Color(0xFF0F172A).copy(alpha = 0.6f))
      .border(1.dp, ZahraCyan.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Text(
      text = "📖 المصدر المعتمد:",
      color = ZahraCyan,
      fontSize = 11.sp,
      fontWeight = FontWeight.Bold
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(
      text = sourceText.removePrefix("[المصدر: ").removeSuffix("]"),
      color = ZahraTextSecondary,
      fontSize = 11.sp,
      maxLines = 2
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
      .background(ZahraViolet.copy(alpha = 0.25f))
      .border(1.dp, ZahraViolet.copy(alpha = 0.5f), RoundedCornerShape(16.dp))
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 8.dp)
  ) {
    Text(
      text = text,
      color = ZahraTextPrimary,
      fontSize = 12.sp,
      fontWeight = FontWeight.Medium
    )
  }
}
