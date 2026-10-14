package com.example.zahra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.zahra.model.ZahraTask
import com.example.zahra.ui.components.GlassCard

@Composable
fun TasksScreen(
  tasks: List<ZahraTask>,
  onToggleTask: (String) -> Unit,
  onDeleteTask: (String) -> Unit,
  onAddTask: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var newTaskText by remember { mutableStateOf("") }
  var selectedFilter by remember { mutableStateOf("الكل") }

  val filters = listOf("الكل", "عمل", "عاجل", "شخصي", "مالي", "صحي")
  val filteredTasks = tasks.filter {
    if (selectedFilter == "الكل") true else it.category == selectedFilter
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .background(ZahraBackground)
      .padding(16.dp)
  ) {
    // Header
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column {
        Text(
          text = "إدارة المهام الذكية",
          color = ZahraTextPrimary,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "تصنيف تلقائي بنموذج TF-IDF وحساب أولويات حتمي",
          color = ZahraTextSecondary,
          fontSize = 12.sp
        )
      }
      Badge(containerColor = ZahraViolet) {
        Text(
          text = "${tasks.count { !it.completed }} متبقية",
          color = Color.White,
          modifier = Modifier.padding(4.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Add Task Field
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(ZahraSurfaceGlass)
        .border(1.dp, ZahraGlassBorder, RoundedCornerShape(16.dp))
        .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
      TextField(
        value = newTaskText,
        onValueChange = { newTaskText = it },
        placeholder = {
          Text(
            text = "اكتب مهمة (مثال: موعد الطبيب الأحد الجاي عاجل)...",
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
          unfocusedTextColor = ZahraTextPrimary
        ),
        modifier = Modifier
          .weight(1f)
          .testTag("add_task_input")
      )
      IconButton(
        onClick = {
          if (newTaskText.isNotBlank()) {
            onAddTask(newTaskText)
            newTaskText = ""
          }
        },
        modifier = Modifier
          .size(36.dp)
          .clip(CircleShape)
          .background(ZahraCyan)
          .testTag("add_task_button")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Task", tint = Color.Black)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Filter Chips
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      filters.forEach { f ->
        val isSelected = selectedFilter == f
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(if (isSelected) ZahraCyan else ZahraSurfaceGlass)
            .border(1.dp, if (isSelected) ZahraCyan else ZahraGlassBorder, RoundedCornerShape(12.dp))
            .clickable { selectedFilter = f }
            .padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
          Text(
            text = f,
            color = if (isSelected) Color.Black else ZahraTextSecondary,
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Task List
    if (filteredTasks.isEmpty()) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "لا توجد مهام مسجلة في هذا القسم.",
          color = ZahraTextTertiary,
          fontSize = 14.sp
        )
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.weight(1f)
      ) {
        items(filteredTasks, key = { it.id }) { task ->
          TaskCard(
            task = task,
            onToggle = { onToggleTask(task.id) },
            onDelete = { onDeleteTask(task.id) }
          )
        }
      }
    }
  }
}

@Composable
fun TaskCard(
  task: ZahraTask,
  onToggle: () -> Unit,
  onDelete: () -> Unit
) {
  GlassCard(
    modifier = Modifier.fillMaxWidth()
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      IconButton(onClick = onToggle) {
        Icon(
          imageVector = if (task.completed) Icons.Default.CheckCircle else Icons.Outlined.Circle,
          contentDescription = "Toggle Complete",
          tint = if (task.completed) ZahraEmerald else ZahraTextTertiary
        )
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = task.title,
          color = if (task.completed) ZahraTextTertiary else ZahraTextPrimary,
          fontSize = 14.sp,
          fontWeight = FontWeight.Medium,
          textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
        )

        Spacer(modifier = Modifier.height(4.dp))

        Row(
          horizontalArrangement = Arrangement.spacedBy(8.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          // Category pill
          Text(
            text = task.category,
            color = ZahraCyan,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(ZahraCyan.copy(alpha = 0.15f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          )

          // Priority pill
          val priorityColor = when {
            task.priorityScore >= 4 -> ZahraPink
            task.priorityScore >= 3 -> ZahraAmber
            else -> ZahraTextSecondary
          }
          Text(
            text = "أولوية: ${task.priorityScore}/5",
            color = priorityColor,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(priorityColor.copy(alpha = 0.15f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          )

          // Due date
          if (task.dueDate != null) {
            Text(
              text = "📅 ${task.dueDate}",
              color = ZahraTextSecondary,
              fontSize = 10.sp
            )
          }
        }
      }

      IconButton(onClick = onDelete) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete Task",
          tint = ZahraTextTertiary.copy(alpha = 0.6f),
          modifier = Modifier.size(18.dp)
        )
      }
    }
  }
}
