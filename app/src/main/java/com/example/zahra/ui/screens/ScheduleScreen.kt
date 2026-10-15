package com.example.zahra.ui.screens

import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.GptAccentBlue
import com.example.zahra.model.ZahraSchedule

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleScreen(
  schedules: List<ZahraSchedule>,
  onToggleSchedule: (String) -> Unit,
  onDeleteSchedule: (String) -> Unit,
  onAddSchedule: (String, String, Int, Int, Boolean) -> Unit,
  onTestNotification: (ZahraSchedule) -> Unit,
  onBackToChat: () -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  BackHandler { onBackToChat() }

  var showAddDialog by remember { mutableStateOf(false) }
  var newTitle by remember { mutableStateOf("") }
  var newMessage by remember { mutableStateOf("") }
  var newHour by remember { mutableIntStateOf(8) }
  var newMinute by remember { mutableIntStateOf(0) }
  var newIsDaily by remember { mutableStateOf(true) }

  Scaffold(
    topBar = {
      TopAppBar(
        title = {
          Text(
            text = "الجدولة والتنبيهات المجدولة",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
          )
        },
        navigationIcon = {
          IconButton(
            onClick = onBackToChat,
            modifier = Modifier.testTag("schedule_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "رجوع"
            )
          }
        },
        actions = {
          IconButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
              .clip(CircleShape)
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .testTag("schedule_add_button")
          ) {
            Icon(
              imageVector = Icons.Default.Add,
              contentDescription = "إضافة موعد",
              tint = MaterialTheme.colorScheme.onSurface
            )
          }
        },
        colors = TopAppBarDefaults.topAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        )
      )
    },
    floatingActionButton = {
      FloatingActionButton(
        onClick = { showAddDialog = true },
        containerColor = Color(0xFFF59E0B),
        contentColor = Color.Black,
        shape = CircleShape,
        modifier = Modifier.testTag("schedule_fab_add")
      ) {
        Icon(Icons.Default.Add, contentDescription = "إضافة تنبيه جديد")
      }
    },
    containerColor = MaterialTheme.colorScheme.background,
    modifier = modifier.fillMaxSize()
  ) { paddingValues ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(paddingValues)
        .padding(horizontal = 16.dp)
    ) {
      // Summary Agent Header Card
      Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        modifier = Modifier
          .fillMaxWidth()
          .padding(vertical = 8.dp)
          .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(14.dp)
        ) {
          Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
              .size(46.dp)
              .clip(CircleShape)
              .background(Color(0xFFF59E0B).copy(alpha = 0.15f))
          ) {
            Icon(
              imageVector = Icons.Outlined.Alarm,
              contentDescription = null,
              tint = Color(0xFFF59E0B),
              modifier = Modifier.size(24.dp)
            )
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "وكيل الجدولة والتنبيهات المستقل",
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = MaterialTheme.colorScheme.onSurface
            )
            Text(
              text = "إشعارات نظام أندرويد الحقيقية تعمل محلياً وبدقة في الموعد.",
              fontSize = 12.sp,
              color = MaterialTheme.colorScheme.onSurfaceVariant
            )
          }
        }
      }

      if (schedules.isEmpty()) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
              imageVector = Icons.Outlined.Alarm,
              contentDescription = null,
              tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
              modifier = Modifier.size(56.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
              text = "لا توجد مواعيد أو تنبيهات مجدولة بعد",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium
            )
            Spacer(modifier = Modifier.height(8.dp))
            TextButton(
              onClick = {
                onAddSchedule("أذكار الصباح وتحفيز اليوم", "صباح الخير! ابدأ يومك بإيجابية وتوكل على الله.", 8, 0, true)
              }
            ) {
              Text("إضافة موعد مقترح تلقائياً (08:00 ص)")
            }
          }
        }
      } else {
        LazyColumn(
          verticalArrangement = Arrangement.spacedBy(10.dp),
          contentPadding = PaddingValues(vertical = 12.dp),
          modifier = Modifier.fillMaxSize()
        ) {
          items(schedules, key = { it.id }) { item ->
            ScheduleListItem(
              schedule = item,
              onToggle = { onToggleSchedule(item.id) },
              onDelete = { onDeleteSchedule(item.id) },
              onTest = { onTestNotification(item) }
            )
          }
        }
      }
    }
  }

  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = { Text("إضافة موعد وتنبيه جديد", fontWeight = FontWeight.Bold) },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
          OutlinedTextField(
            value = newTitle,
            onValueChange = { newTitle = it },
            label = { Text("عنوان التنبيه أو المهمة") },
            placeholder = { Text("مثال: ورد القرآن، موعد الطبيب، مراجعة المشروع") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          OutlinedTextField(
            value = newMessage,
            onValueChange = { newMessage = it },
            label = { Text("رسالة التنبيه المنبثقة") },
            placeholder = { Text("مثال: حان وقت إنجاز المهمة المطلوبة!") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
          )

          Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            OutlinedTextField(
              value = newHour.toString(),
              onValueChange = { newHour = it.toIntOrNull()?.coerceIn(0, 23) ?: 8 },
              label = { Text("الساعة (0-23)") },
              modifier = Modifier.weight(1f)
            )
            OutlinedTextField(
              value = newMinute.toString(),
              onValueChange = { newMinute = it.toIntOrNull()?.coerceIn(0, 59) ?: 0 },
              label = { Text("الدقيقة (0-59)") },
              modifier = Modifier.weight(1f)
            )
          }

          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .fillMaxWidth()
              .clickable { newIsDaily = !newIsDaily }
          ) {
            Checkbox(checked = newIsDaily, onCheckedChange = { newIsDaily = it })
            Spacer(modifier = Modifier.width(6.dp))
            Text("تكرار يومي في نفس الموعد", fontSize = 13.sp)
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newTitle.isNotBlank()) {
              onAddSchedule(
                newTitle.trim(),
                newMessage.ifBlank { "تنبيه مجدول من وكيل زهرة 3: $newTitle" },
                newHour,
                newMinute,
                newIsDaily
              )
              showAddDialog = false
              newTitle = ""
              newMessage = ""
              Toast.makeText(context, "تمت جدولة التنبيه بنجاح!", Toast.LENGTH_SHORT).show()
            }
          }
        ) {
          Text("حفظ وجدولة")
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text("إلغاء")
        }
      }
    )
  }
}

@Composable
private fun ScheduleListItem(
  schedule: ZahraSchedule,
  onToggle: () -> Unit,
  onDelete: () -> Unit,
  onTest: () -> Unit
) {
  Surface(
    shape = RoundedCornerShape(14.dp),
    color = MaterialTheme.colorScheme.surface,
    tonalElevation = 2.dp,
    modifier = Modifier
      .fillMaxWidth()
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(14.dp))
      .testTag("schedule_item_${schedule.id}")
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Surface(
            color = Color(0xFFF59E0B).copy(alpha = 0.15f),
            shape = RoundedCornerShape(8.dp)
          ) {
            Text(
              text = schedule.timeDisplay,
              color = Color(0xFFF59E0B),
              fontSize = 14.sp,
              fontWeight = FontWeight.Bold,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
            )
          }

          Spacer(modifier = Modifier.width(8.dp))

          Surface(
            color = MaterialTheme.colorScheme.surfaceVariant,
            shape = CircleShape
          ) {
            Text(
              text = if (schedule.isDaily) "يومي" else "مرة واحدة",
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp,
              modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
            )
          }
        }

        Switch(
          checked = schedule.isEnabled,
          onCheckedChange = { onToggle() },
          modifier = Modifier.height(26.dp)
        )
      }

      Spacer(modifier = Modifier.height(8.dp))

      Text(
        text = schedule.title,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface
      )

      if (schedule.message.isNotBlank()) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
          text = schedule.message,
          fontSize = 12.sp,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          lineHeight = 16.sp
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier.fillMaxWidth()
      ) {
        FilledTonalButton(
          onClick = onTest,
          shape = RoundedCornerShape(8.dp),
          contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
        ) {
          Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(15.dp))
          Spacer(modifier = Modifier.width(4.dp))
          Text("تجربة الإشعار الحقيقي", fontSize = 11.sp)
        }

        IconButton(
          onClick = onDelete,
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "حذف",
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(18.dp)
          )
        }
      }
    }
  }
}
