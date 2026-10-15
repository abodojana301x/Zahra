package com.example.zahra.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GptAccentBlue

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsBottomSheet(
  isDeepThinkingEnabled: Boolean,
  onToggleDeepThinking: () -> Unit,
  onSelectAction: (String) -> Unit,
  onDismiss: () -> Unit
) {
  var showPluginsSubmenu by remember { mutableStateOf(false) }

  ModalBottomSheet(
    onDismissRequest = onDismiss,
    containerColor = MaterialTheme.colorScheme.surface,
    shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    tonalElevation = 4.dp,
    dragHandle = {
      Box(
        modifier = Modifier
          .padding(top = 10.dp, bottom = 8.dp)
          .size(width = 36.dp, height = 4.dp)
          .clip(CircleShape)
          .background(MaterialTheme.colorScheme.outlineVariant)
      )
    }
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 20.dp, vertical = 8.dp)
        .padding(bottom = 24.dp)
    ) {
      if (!showPluginsSubmenu) {
        // Screenshot 3 Exact Items
        ToolSheetItem(
          title = "الجدولة والتنبيهات للوكيل",
          icon = Icons.Outlined.Alarm,
          tag = "tool_schedule",
          onClick = {
            onSelectAction("فكرني كل يوم الساعة 8 صباحاً بالمهام والأذكار")
            onDismiss()
          }
        )

        ToolSheetItem(
          title = "إنشاء صورة أو ملصق إجرائي",
          icon = Icons.Outlined.Palette,
          tag = "tool_generate_image",
          onClick = {
            onSelectAction("أنشئ لي صورة إجرائية تعبيرية جميلة")
            onDismiss()
          }
        )

        ToolSheetItem(
          title = "الكاميرا",
          icon = Icons.Outlined.PhotoCamera,
          tag = "tool_camera",
          onClick = {
            onSelectAction("تم مسح مستند بالكاميرا: استخرج النقاط الرئيسية")
            onDismiss()
          }
        )

        ToolSheetItem(
          title = "الصور والخلفيات",
          icon = Icons.Outlined.Image,
          tag = "tool_photos",
          onClick = {
            onSelectAction("صمم لي بطاقة اقتباس رقمية فاخرة")
            onDismiss()
          }
        )

        ToolSheetItem(
          title = "الملفات والتلخيص",
          icon = Icons.Outlined.AttachFile,
          tag = "tool_files",
          onClick = {
            onSelectAction("لخص لي هذا المستند بدقة")
            onDismiss()
          }
        )

        ToolSheetItem(
          title = "المكونات الإضافية",
          icon = Icons.Outlined.Extension,
          tag = "tool_plugins",
          onClick = {
            showPluginsSubmenu = true
          }
        )

        ToolSheetItem(
          title = "فكّر بعمق أكبر (Chain of Thought)",
          icon = Icons.Outlined.Psychology,
          tag = "tool_deep_thinking",
          trailingBadge = if (isDeepThinkingEnabled) "مفعل" else "معطل",
          onClick = {
            onToggleDeepThinking()
          }
        )
      } else {
        // Submenu for Zahra's smart engines
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp)
        ) {
          Text(
            text = "المكونات الإضافية لزهراء",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
          )
          TextButton(onClick = { showPluginsSubmenu = false }) {
            Text("رجوع", color = GptAccentBlue, fontWeight = FontWeight.Bold)
          }
        }

        ToolSheetItem(
          title = "الشريعة ومواقيت الصلاة",
          icon = Icons.Outlined.Mosque,
          tag = "plugin_religion",
          onClick = {
            onSelectAction("ما هي مواقيت الصلاة لليوم؟")
            onDismiss()
          }
        )

        ToolSheetItem(
          title = "إدارة المهام والتسكات",
          icon = Icons.Outlined.CheckCircleOutline,
          tag = "plugin_tasks",
          onClick = {
            onSelectAction("أضف مهمة تجهيز العرض التقديمي غداً")
            onDismiss()
          }
        )

        ToolSheetItem(
          title = "الملاحظات السريعة والوسوم",
          icon = Icons.Outlined.EditNote,
          tag = "plugin_notes",
          onClick = {
            onSelectAction("احفظ ملاحظة: اجتماع الفريق الساعة الرابعة عصراً")
            onDismiss()
          }
        )

        ToolSheetItem(
          title = "الحسابات الرياضية والوحدات",
          icon = Icons.Outlined.Calculate,
          tag = "plugin_math",
          onClick = {
            onSelectAction("احسب (150 * 12) + 400")
            onDismiss()
          }
        )

        ToolSheetItem(
          title = "الترجمة الفورية الدقيقة",
          icon = Icons.Outlined.Translate,
          tag = "plugin_translate",
          onClick = {
            onSelectAction("ترجم مرحبا بكم في عالم الذكاء الاصطناعي إلى الإنجليزية")
            onDismiss()
          }
        )
      }
    }
  }
}

@Composable
private fun ToolSheetItem(
  title: String,
  icon: ImageVector,
  tag: String,
  trailingBadge: String? = null,
  onClick: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .clickable(onClick = onClick)
      .padding(vertical = 12.dp, horizontal = 8.dp)
      .testTag(tag)
  ) {
    // In native Arabic RTL: Icon is on the right side of the row
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .size(44.dp)
        .clip(CircleShape)
        .background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
      Icon(
        imageVector = icon,
        contentDescription = title,
        tint = MaterialTheme.colorScheme.onSurface,
        modifier = Modifier.size(22.dp)
      )
    }

    Spacer(modifier = Modifier.width(16.dp))

    Text(
      text = title,
      fontSize = 16.sp,
      fontWeight = FontWeight.Medium,
      color = MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.weight(1f)
    )

    if (trailingBadge != null) {
      Box(
        modifier = Modifier
          .clip(RoundedCornerShape(12.dp))
          .background(GptAccentBlue.copy(alpha = 0.15f))
          .padding(horizontal = 10.dp, vertical = 4.dp)
      ) {
        Text(
          text = trailingBadge,
          color = GptAccentBlue,
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }
  }
}
