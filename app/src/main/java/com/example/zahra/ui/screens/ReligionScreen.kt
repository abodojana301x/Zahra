package com.example.zahra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CompassCalibration
import androidx.compose.material.icons.filled.Mosque
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.zahra.ui.components.GlassCard

@Composable
fun ReligionScreen(
  onQueryReligion: (String) -> String,
  modifier: Modifier = Modifier
) {
  var selectedTab by remember { mutableStateOf(0) } // 0: مواقيت وقبلة, 1: قرآن وحديث, 2: فقه
  val tabs = listOf("المواقيت والقبلة", "القرآن والحديث", "أحكام الفقه")

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ZahraBackground)
      .padding(16.dp)
  ) {
    // Header
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(Icons.Default.Mosque, contentDescription = "Islamic Knowledge", tint = ZahraEmerald, modifier = Modifier.size(24.dp))
      Spacer(modifier = Modifier.width(8.dp))
      Column {
        Text(text = "الشريعة والمواقيت", color = ZahraTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
        Text(text = "أدلة موثقة من القرآن وصحيح السنة وحسابات فلكية دقيقة", color = ZahraTextSecondary, fontSize = 12.sp)
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Tab pills
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      tabs.forEachIndexed { index, title ->
        val isSelected = selectedTab == index
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) ZahraEmerald else ZahraSurfaceGlass)
            .clickable { selectedTab = index }
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = title,
            color = if (isSelected) Color.Black else ZahraTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(16.dp))

    when (selectedTab) {
      0 -> PrayerAndQiblaSection(onQueryReligion = onQueryReligion)
      1 -> QuranAndHadithSection(onQueryReligion = onQueryReligion)
      2 -> FiqhSection(onQueryReligion = onQueryReligion)
    }
  }
}

@Composable
fun PrayerAndQiblaSection(onQueryReligion: (String) -> String) {
  val prayerResult = remember { onQueryReligion("مواقيت الصلاة") }
  val qiblaResult = remember { onQueryReligion("اتجاه القبلة") }

  LazyColumn(verticalArrangement = Arrangement.spacedBy(14.dp)) {
    item {
      // Qibla Card
      GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = ZahraCyan.copy(alpha = 0.4f)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(ZahraCyan.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.CompassCalibration, contentDescription = "Qibla", tint = ZahraCyan)
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(text = "اتجاه القبلة المشرفة", color = ZahraCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = "صيغة الدائرة العظمى (Great-Circle)", color = ZahraTextSecondary, fontSize = 11.sp)
          }
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(text = qiblaResult, color = ZahraTextPrimary, fontSize = 13.sp, lineHeight = 20.sp)
      }
    }

    item {
      // Prayer Times Card
      GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = ZahraEmerald.copy(alpha = 0.4f)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(44.dp)
              .clip(CircleShape)
              .background(ZahraEmerald.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.Schedule, contentDescription = "Prayers", tint = ZahraEmerald)
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(text = "مواقيت الصلوات الخمس", color = ZahraEmerald, fontSize = 15.sp, fontWeight = FontWeight.Bold)
            Text(text = "محسوبة فلكياً وفق ميل الشمس اليومي", color = ZahraTextSecondary, fontSize = 11.sp)
          }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(text = prayerResult, color = ZahraTextPrimary, fontSize = 13.sp, lineHeight = 22.sp)
      }
    }
  }
}

@Composable
fun QuranAndHadithSection(onQueryReligion: (String) -> String) {
  var selectedItem by remember { mutableStateOf("آية الكرسي") }
  val sampleQueries = listOf("آية الكرسي", "سورة الفاتحة", "سورة الإخلاص", "حديث النية", "حديث حسن الخلق", "حديث التيسير")
  val currentAnswer = remember(selectedItem) { onQueryReligion(selectedItem) }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    // Quick select chips
    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      sampleQueries.take(3).forEach { q ->
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selectedItem == q) ZahraCyan else ZahraSurfaceGlass)
            .clickable { selectedItem = q }
            .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Text(text = q, color = if (selectedItem == q) Color.Black else ZahraTextSecondary, fontSize = 11.sp)
        }
      }
    }
    Row(
      horizontalArrangement = Arrangement.spacedBy(6.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      sampleQueries.drop(3).forEach { q ->
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selectedItem == q) ZahraCyan else ZahraSurfaceGlass)
            .clickable { selectedItem = q }
            .padding(horizontal = 8.dp, vertical = 6.dp)
        ) {
          Text(text = q, color = if (selectedItem == q) Color.Black else ZahraTextSecondary, fontSize = 11.sp)
        }
      }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
      Text(text = currentAnswer, color = ZahraTextPrimary, fontSize = 13.sp, lineHeight = 22.sp)
    }
  }
}

@Composable
fun FiqhSection(onQueryReligion: (String) -> String) {
  var selectedFiqh by remember { mutableStateOf("أركان الوضوء") }
  val questions = listOf("أركان الوضوء", "نواقض الوضوء", "سجود السهو")
  val answer = remember(selectedFiqh) { onQueryReligion(selectedFiqh) }

  Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      questions.forEach { q ->
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (selectedFiqh == q) ZahraEmerald else ZahraSurfaceGlass)
            .clickable { selectedFiqh = q }
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text(text = q, color = if (selectedFiqh == q) Color.Black else ZahraTextSecondary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
      }
    }

    GlassCard(modifier = Modifier.fillMaxWidth()) {
      Text(text = answer, color = ZahraTextPrimary, fontSize = 13.sp, lineHeight = 22.sp)
    }
  }
}
