package com.example.zahra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
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
fun SettingsScreen(
  modifier: Modifier = Modifier
) {
  LazyColumn(
    modifier = modifier
      .fillMaxSize()
      .background(ZahraBackground)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    item {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Settings, contentDescription = "Settings", tint = ZahraViolet, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Column {
          Text(text = "الإعدادات والشفافية", color = ZahraTextPrimary, fontSize = 20.sp, fontWeight = FontWeight.Bold)
          Text(text = "مبادئ الأمان، القيود الصادقة، وسرية البيانات", color = ZahraTextSecondary, fontSize = 12.sp)
        }
      }
    }

    // Airplane Mode & Zero Tracking Badge Card
    item {
      GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = ZahraEmerald.copy(alpha = 0.5f)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Box(
            modifier = Modifier
              .size(40.dp)
              .clip(CircleShape)
              .background(ZahraEmerald.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.AirplanemodeActive, contentDescription = "Offline Mode", tint = ZahraEmerald)
          }
          Spacer(modifier = Modifier.width(12.dp))
          Column {
            Text(text = "وضع الطيران دائم • 100% Offline", color = ZahraEmerald, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            Text(text = "لا توجد أي مكالمات شبكة أو مستشعرات تتبع أو إرسال بيانات", color = ZahraTextSecondary, fontSize = 11.sp)
          }
        }
      }
    }

    // Section 11: Honest Limitations (MANDATORY IN ABOUT/SETTINGS)
    item {
      GlassCard(
        modifier = Modifier.fillMaxWidth(),
        borderColor = ZahraPink.copy(alpha = 0.3f)
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Info, contentDescription = "Honest Limitations", tint = ZahraPink, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(
            text = "القيود الصادقة (Honest Limitations)",
            color = ZahraPink,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold
          )
        }

        Spacer(modifier = Modifier.height(10.dp))

        val limitations = listOf(
          "• هذا نظام خبير حتمي قائم على القواعد، وليس نموذجاً لغوياً توليدياً (No-LLM).",
          "• جودة الترجمة ممتازة للكلمات والعبارات الشائعة، ومحدودة للنصوص الأدبية الطويلة المعقدة.",
          "• إجابات الشريعة والدين مستقاة حصراً من مصادر معتمدة مدمجة — لا تخمين أو اجتهاد آلي أبداً.",
          "• التلخيص استخلاصي بحت (TextRank) يختار أهم الجمل الموجودة، ولا يؤلف أو يولد جملاً جديدة.",
          "• الحساب الرياضي دقيق وحتمي، على عكس النماذج الاحتمالية.",
          "• عبارة \"مش متأكد من الإجابة\" إجابة صحيحة ومتوقعة ومقصودة ومصممة لمنع أي هلوسة."
        )

        limitations.forEach { line ->
          Text(
            text = line,
            color = ZahraTextPrimary,
            fontSize = 12.sp,
            lineHeight = 20.sp
          )
          Spacer(modifier = Modifier.height(4.dp))
        }
      }
    }

    // Security & Cryptography Specifications
    item {
      GlassCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Icon(Icons.Default.Lock, contentDescription = "Security", tint = ZahraCyan, modifier = Modifier.size(20.dp))
          Spacer(modifier = Modifier.width(8.dp))
          Text(text = "الأمان والتشفير (Zero-Knowledge)", color = ZahraCyan, fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(text = "• معيار التشفير: AES-256-GCM متوافق مع معايير NIST FIPS 197.", color = ZahraTextSecondary, fontSize = 12.sp)
        Text(text = "• حفظ المفاتيح: داخل Android Hardware Keystore غير القابلة للاستخراج.", color = ZahraTextSecondary, fontSize = 12.sp)
        Text(text = "• التزامن السحابي: معطل بالكامل (Zero-Network). بياناتك لا تغادر هاتفك نهائياً.", color = ZahraTextSecondary, fontSize = 12.sp)
      }
    }

    // Performance Budget Benchmarks
    item {
      GlassCard(modifier = Modifier.fillMaxWidth()) {
        Text(text = "ميزانية الأداء (Performance Budget)", color = ZahraViolet, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(8.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(text = "زمن الاستجابة (P50)", color = ZahraTextSecondary, fontSize = 12.sp)
          Text(text = "< 200 ms", color = ZahraEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(text = "استهلاك الذاكرة (RAM)", color = ZahraTextSecondary, fontSize = 12.sp)
          Text(text = "< 120 MB", color = ZahraEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(text = "حجم التطبيق الإجمالي", color = ZahraTextSecondary, fontSize = 12.sp)
          Text(text = "< 50 MB", color = ZahraEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text(text = "تتبع البيانات الخارجية", color = ZahraTextSecondary, fontSize = 12.sp)
          Text(text = "0 Bytes", color = ZahraEmerald, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }
      }
    }

    // App Identity & Version
    item {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Text(text = "زهرة • Zahra 2.0 Professional", color = ZahraTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
          Text(text = "ذكاء حقيقي. بدون إنترنت. بدون نموذج. بدون تتبع.", color = ZahraTextTertiary, fontSize = 11.sp)
        }
      }
    }
  }
}
