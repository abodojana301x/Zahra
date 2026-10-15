package com.example.zahra.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GptAccentBlue
import com.example.zahra.model.FactCategory
import com.example.zahra.model.ZahraUserFact
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
  userProfileName: String,
  accentColorName: String,
  userFacts: List<ZahraUserFact>,
  onUpdateProfileName: (String) -> Unit,
  onAddFact: (FactCategory, String, String) -> Unit,
  onUpdateFact: (String, String, String, FactCategory?) -> Unit,
  onDeleteFact: (String) -> Unit,
  onClearMemory: () -> Unit,
  onSelectAccentColor: (String) -> Unit,
  onBackToChat: () -> Unit,
  isGlassmorphismEnabled: Boolean = false,
  onToggleGlassmorphism: () -> Unit = {},
  onNavigateToSchedule: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  BackHandler { onBackToChat() }

  var showMemorySheet by remember { mutableStateOf(false) }
  var showColorPicker by remember { mutableStateOf(false) }
  var showEditNameDialog by remember { mutableStateOf(false) }
  var editNameValue by remember { mutableStateOf(userProfileName) }

  // Memory Dialog states
  var showAddFactDialog by remember { mutableStateOf(false) }
  var newFactCategory by remember { mutableStateOf(FactCategory.PERSONAL) }
  var newFactKey by remember { mutableStateOf("") }
  var newFactValue by remember { mutableStateOf("") }

  Scaffold(
    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
    contentWindowInsets = WindowInsets.safeDrawing,
    topBar = {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp, vertical = 8.dp)
      ) {
        // Back Button (In RTL points right)
        IconButton(
          onClick = onBackToChat,
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .testTag("settings_back_button")
        ) {
          Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "الرجوع للدردشة",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }

        // Info Button on the other side
        IconButton(
          onClick = {
            Toast.makeText(context, "نظام زهرة يعمل محلياً 100% دون خوادم أو اشتراكات خارجية", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surface)
            .testTag("settings_info_button")
        ) {
          Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = "معلومات",
            tint = MaterialTheme.colorScheme.onSurface
          )
        }
      }
    }
  ) { innerPadding ->
    LazyColumn(
      modifier = modifier
        .fillMaxSize()
        .padding(innerPadding)
        .padding(horizontal = 16.dp),
      contentPadding = PaddingValues(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
      // Profile Picture & Name Header
      item {
        Column(
          modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
          horizontalAlignment = Alignment.CenterHorizontally
        ) {
          Box(
            contentAlignment = Alignment.BottomStart,
            modifier = Modifier.clickable {
              editNameValue = userProfileName
              showEditNameDialog = true
            }
          ) {
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFF1E1E24))
            ) {
              Text(
                text = userProfileName.take(1).ifBlank { "م" },
                color = Color.White,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold
              )
            }

            // Edit Pencil Badge
            Box(
              contentAlignment = Alignment.Center,
              modifier = Modifier
                .offset(x = (-4).dp, y = 4.dp)
                .size(28.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
            ) {
              Icon(
                imageVector = Icons.Outlined.Edit,
                contentDescription = "تعديل الاسم",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(15.dp)
              )
            }
          }

          Spacer(modifier = Modifier.height(12.dp))

          Text(
            text = userProfileName,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clickable {
              editNameValue = userProfileName
              showEditNameDialog = true
            }
          )
        }
      }

      // Group 1: الملف الشخصي ونوع النظام (أوفلاين 100% بدون اشتراكات)
      item {
        SettingsGroupCard {
          SettingsRowItem(
            title = "الملف الشخصي",
            icon = Icons.Outlined.Person,
            trailingText = userProfileName,
            onClick = {
              editNameValue = userProfileName
              showEditNameDialog = true
            }
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRowItem(
            title = "نوع النظام",
            icon = Icons.Outlined.Devices,
            trailingText = "محلي 100% (أوفلاين)",
            onClick = {
              Toast.makeText(context, "البرنامج يعمل محلياً بالكامل دون أي اتصال بالإنترنت أو حسابات سحابية", Toast.LENGTH_SHORT).show()
            }
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRowItem(
            title = "مساحة العمل",
            icon = Icons.Outlined.WorkOutline,
            trailingText = "شخصي مستقل",
            onClick = {
              Toast.makeText(context, "مساحة عمل خاصة ومشفرة محلياً على هاتفك", Toast.LENGTH_SHORT).show()
            }
          )
        }
      }

      // Group 2: التخصيص، الذاكرة، المكونات، لون التمييز (Screenshot 4)
      item {
        SettingsGroupCard {
          SettingsRowItem(
            title = "التخصيص",
            icon = Icons.Outlined.SentimentSatisfied,
            onClick = {
              Toast.makeText(context, "تخصيص المساعد الشخصي مفعل", Toast.LENGTH_SHORT).show()
            }
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRowItem(
            title = "الذاكرة",
            icon = Icons.Outlined.MenuBook,
            trailingText = "${userFacts.size} حقيقة محفوظة",
            onClick = { showMemorySheet = true }
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRowItem(
            title = "المكونات الإضافية",
            icon = Icons.Outlined.Extension,
            trailingText = "5 محركات نشطة",
            onClick = {
              Toast.makeText(context, "المحركات الذكية: الصلاة، المهام، الملاحظات، الحساب، الترجمة", Toast.LENGTH_LONG).show()
            }
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRowItem(
            title = "الجدولة والتنبيهات للوكيل",
            icon = Icons.Outlined.Alarm,
            trailingText = "إدارة المواعيد",
            onClick = onNavigateToSchedule
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRowItem(
            title = "نمط الزجاج الشفاف (Glassmorphism)",
            icon = Icons.Outlined.BlurOn,
            trailingContent = {
              Switch(
                checked = isGlassmorphismEnabled,
                onCheckedChange = { onToggleGlassmorphism() },
                modifier = Modifier.height(26.dp)
              )
            },
            onClick = onToggleGlassmorphism
          )
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRowItem(
            title = "لون التمييز",
            icon = Icons.Outlined.Palette,
            trailingContent = {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                  imageVector = Icons.Default.KeyboardArrowDown,
                  contentDescription = null,
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                  text = accentColorName,
                  fontSize = 14.sp,
                  color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.width(6.dp))
                Box(
                  modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(GptAccentBlue)
                )
              }
            },
            onClick = { showColorPicker = true }
          )
        }
      }

      // Group 3: إعدادات النظام المتقدمة والأمان المحلي
      item {
        SettingsGroupCard {
          SettingsRowItem(title = "عام", icon = Icons.Outlined.Settings) {
            Toast.makeText(context, "الإعدادات العامة", Toast.LENGTH_SHORT).show()
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRowItem(title = "الاستخدام والحدود", icon = Icons.Outlined.InsertChartOutlined) {
            Toast.makeText(context, "استخدام غير محدود ومحلي 100% دون خوادم", Toast.LENGTH_SHORT).show()
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRowItem(title = "الإشعارات", icon = Icons.Outlined.Notifications) {
            Toast.makeText(context, "إعدادات الإشعارات والتنبيهات المحلية", Toast.LENGTH_SHORT).show()
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRowItem(title = "الصوت", icon = Icons.Outlined.GraphicEq) {
            Toast.makeText(context, "الصوت: محلي بالعربية الفصحى", Toast.LENGTH_SHORT).show()
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRowItem(title = "السلامة والموثوقية", icon = Icons.Outlined.Security) {
            Toast.makeText(context, "حماية كاملة حتمية دون جمع أي بيانات", Toast.LENGTH_SHORT).show()
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRowItem(title = "الأمان والخصوصية", icon = Icons.Outlined.Lock) {
            Toast.makeText(context, "مشفر محلياً 100% — لا اتصال بالإنترنت", Toast.LENGTH_SHORT).show()
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRowItem(title = "التخزين المحلي", icon = Icons.Outlined.Storage) {
            Toast.makeText(context, "جميع البيانات محفوظة ومشفرة على جهازك فقط", Toast.LENGTH_SHORT).show()
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRowItem(title = "مركز الخصوصية", icon = Icons.Outlined.Policy) {
            Toast.makeText(context, "خصوصية مطلقة: صفر بايت تخرج من هاتفك", Toast.LENGTH_SHORT).show()
          }
          HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
          SettingsRowItem(title = "السجل والبيانات", icon = Icons.Outlined.Archive) {
            Toast.makeText(context, "البيانات محفوظة محلياً بالكامل", Toast.LENGTH_SHORT).show()
          }
        }
      }
    }
  }

  // Edit Name Dialog
  if (showEditNameDialog) {
    AlertDialog(
      onDismissRequest = { showEditNameDialog = false },
      title = { Text("تعديل الاسم في التطبيق", fontWeight = FontWeight.Bold) },
      text = {
        OutlinedTextField(
          value = editNameValue,
          onValueChange = { editNameValue = it },
          label = { Text("اكتب اسمك المفضل") },
          singleLine = true,
          modifier = Modifier.fillMaxWidth()
        )
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (editNameValue.isNotBlank()) {
              onUpdateProfileName(editNameValue.trim())
              showEditNameDialog = false
            }
          }
        ) {
          Text("حفظ")
        }
      },
      dismissButton = {
        TextButton(onClick = { showEditNameDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }

  // Accent Color Picker Dialog
  if (showColorPicker) {
    AlertDialog(
      onDismissRequest = { showColorPicker = false },
      title = { Text("اختر لون التمييز", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          listOf(
            "أزرق" to Color(0xFF007AFF),
            "أخضر زمردي" to Color(0xFF10A37F),
            "بنفسجي" to Color(0xFF8B5CF6),
            "برتقالي" to Color(0xFFF59E0B)
          ).forEach { (name, color) ->
            Row(
              verticalAlignment = Alignment.CenterVertically,
              modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .clickable {
                  onSelectAccentColor(name)
                  showColorPicker = false
                }
                .padding(12.dp)
            ) {
              Box(
                modifier = Modifier
                  .size(18.dp)
                  .clip(CircleShape)
                  .background(color)
              )
              Spacer(modifier = Modifier.width(12.dp))
              Text(text = name, fontSize = 15.sp, modifier = Modifier.weight(1f))
              if (accentColorName == name) {
                Icon(
                  imageVector = Icons.Default.Check,
                  contentDescription = null,
                  tint = GptAccentBlue,
                  modifier = Modifier.size(18.dp)
                )
              }
            }
          }
        }
      },
      confirmButton = {
        TextButton(onClick = { showColorPicker = false }) {
          Text("إغلاق")
        }
      }
    )
  }

  // Memory Manager Modal Sheet
  if (showMemorySheet) {
    ModalBottomSheet(
      onDismissRequest = { showMemorySheet = false },
      shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
      containerColor = MaterialTheme.colorScheme.surface
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .padding(20.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Text(
            text = "ذاكرة زهرة عنك",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
          )
          Button(
            onClick = { showAddFactDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = GptAccentBlue),
            shape = CircleShape,
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp)
          ) {
            Text("+ إضافة حقيقة", fontSize = 12.sp, color = Color.White)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (userFacts.isEmpty()) {
          Text(
            text = "لم يتم حفظ أي حقائق بعد. ستحفظ زهرة تفضيلاتك تلقائياً أثناء الحديث.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.padding(vertical = 24.dp)
          )
        } else {
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .heightIn(max = 350.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            items(userFacts, key = { it.id }) { fact ->
              Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(12.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .padding(12.dp)
              ) {
                Column(modifier = Modifier.weight(1f)) {
                  Text(
                    text = fact.key,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                  )
                  Text(
                    text = fact.value,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                  )
                }
                IconButton(onClick = { onDeleteFact(fact.id) }) {
                  Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "حذف الحقيقة",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(18.dp)
                  )
                }
              }
            }
          }
        }
      }
    }
  }

  // Add Fact Dialog
  if (showAddFactDialog) {
    AlertDialog(
      onDismissRequest = { showAddFactDialog = false },
      title = { Text("إضافة معلومة جديدة للذاكرة") },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = newFactKey,
            onValueChange = { newFactKey = it },
            label = { Text("المجال / المفتاح (مثل: الاسم أو المدينة)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newFactValue,
            onValueChange = { newFactValue = it },
            label = { Text("القيمة (مثل: أحمد أو الرياض أو القراءة)") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        TextButton(
          onClick = {
            if (newFactKey.isNotBlank() && newFactValue.isNotBlank()) {
              onAddFact(newFactCategory, newFactKey.trim(), newFactValue.trim())
              newFactKey = ""
              newFactValue = ""
              showAddFactDialog = false
            }
          }
        ) {
          Text("حفظ")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddFactDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }
}

@Composable
private fun SettingsGroupCard(
  content: @Composable ColumnScope.() -> Unit
) {
  Surface(
    shape = RoundedCornerShape(18.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 1.dp,
    modifier = Modifier
      .fillMaxWidth()
      .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
  ) {
    Column(
      modifier = Modifier.fillMaxWidth(),
      content = content
    )
  }
}

@Composable
private fun SettingsRowItem(
  title: String,
  icon: ImageVector,
  iconTint: Color? = null,
  titleColor: Color? = null,
  isBoldTitle: Boolean = false,
  trailingText: String? = null,
  trailingContent: (@Composable () -> Unit)? = null,
  onClick: () -> Unit
) {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    modifier = Modifier
      .fillMaxWidth()
      .clickable(onClick = onClick)
      .padding(horizontal = 16.dp, vertical = 14.dp)
  ) {
    // In native Arabic RTL: Trailing content/text is on the left
    if (trailingContent != null) {
      trailingContent()
    } else if (trailingText != null) {
      Text(
        text = trailingText,
        fontSize = 13.sp,
        color = MaterialTheme.colorScheme.onSurfaceVariant
      )
    }

    Spacer(modifier = Modifier.weight(1f))

    // Title in the middle
    Text(
      text = title,
      fontSize = 15.sp,
      fontWeight = if (isBoldTitle) FontWeight.Bold else FontWeight.Medium,
      color = titleColor ?: MaterialTheme.colorScheme.onSurface
    )

    Spacer(modifier = Modifier.width(14.dp))

    // Icon on the right in RTL
    Icon(
      imageVector = icon,
      contentDescription = title,
      tint = iconTint ?: MaterialTheme.colorScheme.onSurface,
      modifier = Modifier.size(20.dp)
    )
  }
}
