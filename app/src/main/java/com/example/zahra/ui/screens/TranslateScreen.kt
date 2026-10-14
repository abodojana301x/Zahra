package com.example.zahra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.zahra.ui.components.GlassCard

@Composable
fun TranslateScreen(
  onTranslate: (String, String) -> String,
  modifier: Modifier = Modifier
) {
  var inputText by remember { mutableStateOf("سلام وعلم ومعرفة") }
  var targetLang by remember { mutableStateOf("en") } // en, fr, ar
  var translatedResult by remember { mutableStateOf("") }

  LaunchedEffect(inputText, targetLang) {
    if (inputText.isNotBlank()) {
      translatedResult = onTranslate(inputText, targetLang)
    }
  }

  val samplePhrases = listOf(
    "كيف حالك", "تشرفت بمعرفتك", "أحتاج إلى مساعدة", "كم الساعة الآن", "صباح الخير"
  )

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ZahraBackground)
      .padding(16.dp)
  ) {
    // Header
    Column {
      Text(
        text = "المترجم اللغوي المدمج",
        color = ZahraTextPrimary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold
      )
      Text(
        text = "معاجم ثلاثية وقواعد صرفية مدمجة بالكامل بدون اتصال بالشبكة",
        color = ZahraTextSecondary,
        fontSize = 12.sp
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Language Selector Bar
    GlassCard(modifier = Modifier.fillMaxWidth()) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Text(
          text = if (targetLang == "ar") "الإنجليزية / الفرنسية" else "العربية",
          color = ZahraCyan,
          fontWeight = FontWeight.Bold,
          fontSize = 14.sp
        )

        IconButton(onClick = {
          targetLang = if (targetLang == "ar") "en" else "ar"
        }) {
          Icon(Icons.Default.SwapHoriz, contentDescription = "Swap Languages", tint = ZahraPink)
        }

        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          val langs = listOf("en" to "EN", "fr" to "FR", "ar" to "AR")
          langs.forEach { (code, label) ->
            val isSelected = targetLang == code
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSelected) ZahraCyan else ZahraSurfaceGlass)
                .clickable { targetLang = code }
                .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
              Text(
                text = label,
                color = if (isSelected) Color.Black else ZahraTextSecondary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
            }
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Source Input Area
    OutlinedTextField(
      value = inputText,
      onValueChange = { inputText = it },
      label = { Text("النص المراد ترجمته") },
      minLines = 3,
      colors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = ZahraTextPrimary,
        unfocusedTextColor = ZahraTextPrimary,
        focusedBorderColor = ZahraCyan,
        unfocusedBorderColor = ZahraGlassBorder
      ),
      modifier = Modifier
        .fillMaxWidth()
        .testTag("translate_input_field")
    )

    Spacer(modifier = Modifier.height(12.dp))

    // Translated Output Area
    GlassCard(
      modifier = Modifier.fillMaxWidth(),
      borderColor = ZahraCyan.copy(alpha = 0.5f)
    ) {
      Text(
        text = "الترجمة الفورية (Offline):",
        color = ZahraCyan,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold
      )
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = translatedResult.ifBlank { "اكتب نصاً أعلاه لترجمته فورياً..." },
        color = ZahraTextPrimary,
        fontSize = 16.sp,
        fontWeight = FontWeight.Medium,
        lineHeight = 24.sp
      )
      Spacer(modifier = Modifier.height(8.dp))
      Text(
        text = "📖 المصدر: معجم زهرة الثلاثي المدمج (عربي - إنجليزي - فرنسي)",
        color = ZahraTextTertiary,
        fontSize = 10.sp
      )
    }

    Spacer(modifier = Modifier.height(16.dp))

    // Common Phrases quick tap
    Text(
      text = "عبارات شائعة سريعة:",
      color = ZahraTextSecondary,
      fontSize = 12.sp,
      fontWeight = FontWeight.SemiBold
    )
    Spacer(modifier = Modifier.height(8.dp))
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      items(samplePhrases) { phrase ->
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(ZahraSurfaceGlass)
            .border(1.dp, ZahraGlassBorder, RoundedCornerShape(12.dp))
            .clickable { inputText = phrase }
            .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
          Text(text = phrase, color = ZahraTextPrimary, fontSize = 12.sp)
        }
      }
    }
  }
}
