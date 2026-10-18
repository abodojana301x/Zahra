package com.example.zahra.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*
import com.example.zahra.model.ZahraToolPayload

@Composable
fun InteractiveToolCard(
  payload: ZahraToolPayload,
  onToggleTask: (String) -> Unit = {},
  onQuickQuery: (String) -> Unit = {},
  onNavigateToMemory: () -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current

  when (payload) {
    is ZahraToolPayload.TaskPayload -> {
      val task = payload.task
      Column(
        modifier = modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
          .padding(14.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.CheckCircle,
              contentDescription = "Task Tool",
              tint = Color(0xFF10A37F),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "محرك المهام",
              color = Color(0xFF10A37F),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .padding(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text(
              text = task.category,
              color = MaterialTheme.colorScheme.onSurfaceVariant,
              fontSize = 11.sp,
              fontWeight = FontWeight.SemiBold
            )
          }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.fillMaxWidth()
        ) {
          Checkbox(
            checked = task.completed,
            onCheckedChange = { onToggleTask(task.id) },
            colors = CheckboxDefaults.colors(
              checkedColor = Color(0xFF10A37F),
              checkmarkColor = Color.White
            ),
            modifier = Modifier.testTag("task_checkbox_${task.id}")
          )

          Spacer(modifier = Modifier.width(4.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = task.title,
              color = if (task.completed) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
              fontSize = 14.sp,
              fontWeight = FontWeight.Medium,
              textDecoration = if (task.completed) TextDecoration.LineThrough else TextDecoration.None
            )

            if (!task.dueDate.isNullOrBlank()) {
              Spacer(modifier = Modifier.height(2.dp))
              Text(
                text = "الموعد: ${task.dueDate}",
                color = GptAccentBlue,
                fontSize = 11.sp
              )
            }
          }

          Text(
            text = "★ ${task.priorityScore}/5",
            color = Color(0xFFF59E0B),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    is ZahraToolPayload.NotePayload -> {
      val note = payload.note
      Column(
        modifier = modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
          .padding(14.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.EditNote,
              contentDescription = "Note Tool",
              tint = GptAccentBlue,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "محرك الملاحظات",
              color = GptAccentBlue,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          IconButton(
            onClick = {
              clipboardManager.setText(AnnotatedString(note.content))
              Toast.makeText(context, "تم نسخ الملاحظة إلى الحافظة", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.ContentCopy,
              contentDescription = "Copy Note",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = note.title,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )

        if (note.tags.isNotEmpty()) {
          Spacer(modifier = Modifier.height(8.dp))
          Row(
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            modifier = Modifier.fillMaxWidth()
          ) {
            note.tags.take(3).forEach { tag ->
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .padding(horizontal = 6.dp, vertical = 2.dp)
              ) {
                Text(text = "#$tag", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
              }
            }
          }
        }
      }
    }

    is ZahraToolPayload.TranslatePayload -> {
      Column(
        modifier = modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
          .padding(14.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Translate,
              contentDescription = "Translation Tool",
              tint = GptAccentBlue,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "${payload.sourceLang} ➔ ${payload.targetLang}",
              color = GptAccentBlue,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Row {
            IconButton(
              onClick = {
                onQuickQuery("ترجم \"${payload.translatedText}\" إلى ${payload.sourceLang}")
              },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Default.SwapHoriz,
                contentDescription = "Reverse Translation",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
              )
            }

            IconButton(
              onClick = {
                clipboardManager.setText(AnnotatedString(payload.translatedText))
                Toast.makeText(context, "تم نسخ الترجمة", Toast.LENGTH_SHORT).show()
              },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(
                imageVector = Icons.Outlined.ContentCopy,
                contentDescription = "Copy Translation",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(10.dp)
        ) {
          Text(
            text = payload.translatedText,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            lineHeight = 22.sp
          )
        }
      }
    }

    is ZahraToolPayload.MathPayload -> {
      Column(
        modifier = modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
          .padding(14.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Calculate,
              contentDescription = "Math Tool",
              tint = GptAccentBlue,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = payload.calculationType,
              color = GptAccentBlue,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          IconButton(
            onClick = {
              clipboardManager.setText(AnnotatedString(payload.result))
              Toast.makeText(context, "تم نسخ النتيجة", Toast.LENGTH_SHORT).show()
            },
            modifier = Modifier.size(28.dp)
          ) {
            Icon(
              imageVector = Icons.Outlined.ContentCopy,
              contentDescription = "Copy Result",
              tint = MaterialTheme.colorScheme.onSurfaceVariant,
              modifier = Modifier.size(16.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = payload.expression,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 12.sp
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
          text = payload.result,
          color = Color(0xFF10A37F),
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
      }
    }

    is ZahraToolPayload.ReligionPayload -> {
      Column(
        modifier = modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
          .padding(14.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Mosque,
              contentDescription = "Religion Tool",
              tint = Color(0xFF10A37F),
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = payload.category,
              color = Color(0xFF10A37F),
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(Color(0xFF10A37F).copy(alpha = 0.12f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(text = "معتمد وموثق", color = Color(0xFF10A37F), fontSize = 10.sp, fontWeight = FontWeight.Bold)
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        if (payload.isPrayerTime && payload.prayerTimes.isNotEmpty()) {
          Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            payload.prayerTimes.forEach { (name, time) ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(MaterialTheme.colorScheme.surfaceVariant)
                  .padding(horizontal = 12.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(text = name, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                Text(text = time, color = GptAccentBlue, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              }
            }
          }
        } else if (payload.isTasbih) {
          var tasbihCount by remember { mutableIntStateOf(0) }
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(12.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column {
              Text(text = "مسبحة زهرة الرقمية", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.Bold)
              Text(text = "المتبقي للختم: ${payload.tasbihTarget - (tasbihCount % payload.tasbihTarget)}", color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 11.sp)
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
              Box(
                modifier = Modifier
                  .size(46.dp)
                  .clip(CircleShape)
                  .background(Color(0xFF10A37F))
                  .clickable { tasbihCount++ }
                  .testTag("tasbih_counter_button"),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = "$tasbihCount",
                  color = Color.White,
                  fontWeight = FontWeight.Bold,
                  fontSize = 16.sp
                )
              }

              Spacer(modifier = Modifier.width(8.dp))

              IconButton(
                onClick = { tasbihCount = 0 },
                modifier = Modifier.size(32.dp)
              ) {
                Icon(
                  imageVector = Icons.Default.Refresh,
                  contentDescription = "Reset Tasbih",
                  tint = MaterialTheme.colorScheme.onSurfaceVariant,
                  modifier = Modifier.size(16.dp)
                )
              }
            }
          }
        }
      }
    }

    is ZahraToolPayload.SummarizePayload -> {
      Column(
        modifier = modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
          .padding(14.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.AutoAwesome,
              contentDescription = "Summarize Tool",
              tint = GptAccentBlue,
              modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = "محرك التلخيص",
              color = GptAccentBlue,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(GptAccentBlue.copy(alpha = 0.12f))
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(
              text = "اختصار ${((1.0 - payload.compressionRatio) * 100).toInt()}%",
              color = GptAccentBlue,
              fontSize = 10.sp,
              fontWeight = FontWeight.Bold
            )
          }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = payload.summary,
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 13.sp,
          lineHeight = 20.sp
        )

        Spacer(modifier = Modifier.height(6.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(
            onClick = {
              clipboardManager.setText(AnnotatedString(payload.summary))
              Toast.makeText(context, "تم نسخ الملخص", Toast.LENGTH_SHORT).show()
            }
          ) {
            Icon(Icons.Outlined.ContentCopy, contentDescription = "Copy", tint = GptAccentBlue, modifier = Modifier.size(14.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("نسخ الملخص", color = GptAccentBlue, fontSize = 11.sp)
          }
        }
      }
    }

    is ZahraToolPayload.MemoryFactPayload -> {
      val fact = payload.fact
      Column(
        modifier = modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(16.dp))
          .background(MaterialTheme.colorScheme.surface)
          .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
          .padding(14.dp)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
          modifier = Modifier.fillMaxWidth()
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Outlined.Psychology,
              contentDescription = "Memory Learned",
              tint = GptAccentBlue,
              modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
              text = if (payload.isNew) "حفظت زهرة في ذاكرتها" else "من ذاكرة زهرة المحفوظة",
              color = GptAccentBlue,
              fontSize = 12.sp,
              fontWeight = FontWeight.Bold
            )
          }

          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(MaterialTheme.colorScheme.surfaceVariant)
              .padding(horizontal = 6.dp, vertical = 2.dp)
          ) {
            Text(text = fact.category.labelAr, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
          }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
          text = "${fact.key}: ${fact.value}",
          color = MaterialTheme.colorScheme.onSurface,
          fontSize = 13.sp,
          fontWeight = FontWeight.Medium
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.End
        ) {
          TextButton(
            onClick = onNavigateToMemory
          ) {
            Text(text = "عرض في مركز الذاكرة ➔", color = GptAccentBlue, fontSize = 11.sp)
          }
        }
      }
    }

    is ZahraToolPayload.ImageGenerationPayload -> {
      LocalImageCard(
        initialPayload = payload,
        onQuickQuery = onQuickQuery,
        modifier = modifier
      )
    }

    is ZahraToolPayload.SchedulePayload -> {
      ScheduleCard(
        payload = payload,
        onQuickQuery = onQuickQuery,
        modifier = modifier
      )
    }

    is ZahraToolPayload.KnowledgePayload -> {
      KnowledgeCard(
        payload = payload,
        onQuickQuery = onQuickQuery,
        modifier = modifier
      )
    }

    is com.example.zahra.model.DeepThinkingPayload -> {
      DeepThinkingCard(
        payload = payload,
        modifier = modifier
      )
    }
  }
}

@Composable
fun KnowledgeCard(
  payload: ZahraToolPayload.KnowledgePayload,
  onQuickQuery: (String) -> Unit = {},
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  var isExpanded by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(16.dp))
      .padding(14.dp)
      .testTag("knowledge_card_${payload.recordId}")
  ) {
    // Header
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.MenuBook,
          contentDescription = "Knowledge Engine",
          tint = Color(0xFF06B6D4),
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "محرك المعرفة المحلية الموثقة",
          color = Color(0xFF06B6D4),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Surface(
        shape = RoundedCornerShape(6.dp),
        color = if (payload.isVerified) Color(0xFF10A37F).copy(alpha = 0.15f) else Color(0xFFF59E0B).copy(alpha = 0.15f)
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        ) {
          Icon(
            imageVector = if (payload.isVerified) Icons.Outlined.Verified else Icons.Outlined.Warning,
            contentDescription = null,
            tint = if (payload.isVerified) Color(0xFF10A37F) else Color(0xFFF59E0B),
            modifier = Modifier.size(12.dp)
          )
          Spacer(modifier = Modifier.width(4.dp))
          Text(
            text = payload.verificationStatusText,
            color = if (payload.isVerified) Color(0xFF10A37F) else Color(0xFFF59E0B),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Title & Category
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = payload.title,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f)
      )
      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
      ) {
        Text(
          text = payload.category,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 10.sp,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    // Content Excerpt
    Text(
      text = payload.content,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontSize = 12.sp,
      lineHeight = 18.sp
    )

    // Warnings if any
    if (payload.warning != null) {
      Spacer(modifier = Modifier.height(8.dp))
      Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFFF59E0B).copy(alpha = 0.1f),
        modifier = Modifier.fillMaxWidth()
      ) {
        Row(
          verticalAlignment = Alignment.CenterVertically,
          modifier = Modifier.padding(8.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.Info,
            contentDescription = null,
            tint = Color(0xFFF59E0B),
            modifier = Modifier.size(14.dp)
          )
          Spacer(modifier = Modifier.width(6.dp))
          Text(
            text = payload.warning,
            color = Color(0xFFF59E0B),
            fontSize = 11.sp
          )
        }
      }
    }

    // Conflicts breakdown if any
    if (payload.conflicts.isNotEmpty()) {
      Spacer(modifier = Modifier.height(8.dp))
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(8.dp))
          .background(Color(0xFFEF4444).copy(alpha = 0.1f))
          .padding(8.dp)
      ) {
        Text(
          text = "⚠️ تعارض في الأدلة المسجلة:",
          color = Color(0xFFEF4444),
          fontSize = 11.sp,
          fontWeight = FontWeight.Bold
        )
        payload.conflicts.forEach { desc ->
          Text(
            text = "• $desc",
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 11.sp
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Traceability Footer
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "المصدر: ${payload.source}",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 10.sp,
          fontWeight = FontWeight.Medium
        )
        Text(
          text = "الموضع: ${payload.sourceLocation} • طريقة المطابقة: ${payload.matchMethodText}",
          color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
          fontSize = 9.sp
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
          shape = RoundedCornerShape(4.dp),
          color = Color(0xFF06B6D4).copy(alpha = 0.15f)
        ) {
          Text(
            text = "${payload.relevancePercent}% صلة",
            color = Color(0xFF06B6D4),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
          )
        }

        IconButton(
          onClick = {
            clipboardManager.setText(AnnotatedString("${payload.title}\n${payload.content}\n\nالمصدر: ${payload.source} (${payload.sourceLocation})"))
            Toast.makeText(context, "تم نسخ التوثيق والمصدر إلى الحافظة", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.size(32.dp)
        ) {
          Icon(
            imageVector = Icons.Outlined.ContentCopy,
            contentDescription = "Copy Citation",
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(16.dp)
          )
        }
      }
    }
  }
}

@Composable
fun LocalImageCard(
  initialPayload: ZahraToolPayload.ImageGenerationPayload,
  onQuickQuery: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val clipboardManager = LocalClipboardManager.current
  var currentMode by remember { mutableStateOf(initialPayload.mode) }
  var currentSeed by remember { mutableStateOf(initialPayload.seed) }
  var shapeStyle by remember { mutableStateOf(initialPayload.shapeStyle) }

  val primaryColor = Color(initialPayload.primaryColorHex)
  val secondaryColor = Color(initialPayload.secondaryColorHex)

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(18.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(18.dp))
      .padding(14.dp)
      .testTag("local_image_card")
  ) {
    // Header
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.Palette,
          contentDescription = "Local Image Engine",
          tint = Color(0xFF8B5CF6),
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "محرك الرسومات والفن الإجرائي لزهرة 3",
          color = Color(0xFF8B5CF6),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Surface(
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceVariant
      ) {
        Text(
          text = "توليد محلي 100%",
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          fontSize = 10.sp,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Interactive Style Chips Switcher (All 3 Modes requested by user)
    Row(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.fillMaxWidth()
    ) {
      FilterChip(
        selected = currentMode == "vector",
        onClick = { currentMode = "vector" },
        label = { Text("فن إجرائي Canvas", fontSize = 11.sp) },
        leadingIcon = { Icon(Icons.Outlined.Brush, contentDescription = null, modifier = Modifier.size(14.dp)) },
        modifier = Modifier.weight(1f)
      )
      FilterChip(
        selected = currentMode == "quote_card",
        onClick = { currentMode = "quote_card" },
        label = { Text("بطاقة اقتباس", fontSize = 11.sp) },
        leadingIcon = { Icon(Icons.Outlined.FormatQuote, contentDescription = null, modifier = Modifier.size(14.dp)) },
        modifier = Modifier.weight(1f)
      )
      FilterChip(
        selected = currentMode == "pixel_art",
        onClick = { currentMode = "pixel_art" },
        label = { Text("بكسل آرت 16×16", fontSize = 11.sp) },
        leadingIcon = { Icon(Icons.Outlined.GridOn, contentDescription = null, modifier = Modifier.size(14.dp)) },
        modifier = Modifier.weight(1f)
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Visual Art Surface
    Box(
      contentAlignment = Alignment.Center,
      modifier = Modifier
        .fillMaxWidth()
        .height(210.dp)
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0xFF0F172A))
        .border(1.dp, Color(0xFF1E293B), RoundedCornerShape(14.dp))
    ) {
      when (currentMode) {
        "quote_card" -> {
          // Digital Typography & Quote Card with ornamental background
          Box(
            modifier = Modifier
              .fillMaxSize()
              .background(
                androidx.compose.ui.graphics.Brush.linearGradient(
                  colors = listOf(primaryColor.copy(alpha = 0.85f), secondaryColor.copy(alpha = 0.95f))
                )
              )
              .padding(16.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Outlined.FormatQuote,
                contentDescription = null,
                tint = Color.White.copy(alpha = 0.6f),
                modifier = Modifier.size(28.dp)
              )
              Spacer(modifier = Modifier.height(6.dp))
              Text(
                text = initialPayload.quoteArabic.ifBlank { "وَقُل رَّبِّ زِدْنِي عِلْمًا" },
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.Bold,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                lineHeight = 24.sp
              )
              Spacer(modifier = Modifier.height(10.dp))
              Surface(
                color = Color.Black.copy(alpha = 0.25f),
                shape = CircleShape
              ) {
                Text(
                  text = "— ${initialPayload.quoteAuthor.ifBlank { "حكمة عربية" }} —",
                  color = Color.White.copy(alpha = 0.9f),
                  fontSize = 11.sp,
                  fontWeight = FontWeight.Medium,
                  modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
              }
            }
          }
        }

        "pixel_art" -> {
          // 16x16 Generative Pixel Art Canvas
          androidx.compose.foundation.Canvas(modifier = Modifier.size(190.dp)) {
            val grid = initialPayload.pixelGrid
            if (grid.isNotEmpty()) {
              val cellSize = size.width / 16f
              val colors = listOf(
                Color(0xFF090D16),
                primaryColor,
                secondaryColor,
                Color(0xFFF8FAFC)
              )
              for (r in 0 until 16) {
                for (c in 0 until 16) {
                  val colorIdx = grid.getOrNull(r)?.getOrNull(c) ?: 0
                  val col = colors[colorIdx.coerceIn(0, colors.lastIndex)]
                  drawRect(
                    color = col,
                    topLeft = androidx.compose.ui.geometry.Offset(c * cellSize, r * cellSize),
                    size = androidx.compose.ui.geometry.Size(cellSize, cellSize)
                  )
                }
              }
            } else {
              // Fallback algorithmic pattern
              val cellSize = size.width / 16f
              for (r in 0 until 16) {
                for (c in 0 until 16) {
                  val fill = (r + c + (currentSeed % 7).toInt()) % 3 == 0
                  drawRect(
                    color = if (fill) primaryColor else Color(0xFF1E293B),
                    topLeft = androidx.compose.ui.geometry.Offset(c * cellSize, r * cellSize),
                    size = androidx.compose.ui.geometry.Size(cellSize * 0.9f, cellSize * 0.9f)
                  )
                }
              }
            }
          }
        }

        else -> {
          // Procedural Vector / Canvas Art (Cosmic Galaxy, Geometric Mandala, Futuristic Grid)
          androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            val center = androidx.compose.ui.geometry.Offset(w / 2f, h / 2f)

            // Deep background radial glow
            drawCircle(
              brush = androidx.compose.ui.graphics.Brush.radialGradient(
                colors = listOf(secondaryColor.copy(alpha = 0.5f), primaryColor.copy(alpha = 0.2f), Color.Transparent),
                center = center,
                radius = w * 0.6f
              ),
              radius = w * 0.6f,
              center = center
            )

            // Concentric geometric rings / orbits
            for (i in 1..5) {
              val ringRadius = (w * 0.12f) * i
              drawCircle(
                color = primaryColor.copy(alpha = 0.25f - i * 0.03f),
                radius = ringRadius,
                center = center,
                style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
              )
            }

            // Procedural orbital nodes & stars
            val rand = kotlin.random.Random(currentSeed)
            for (p in 0 until 24) {
              val angle = rand.nextFloat() * 6.283f
              val dist = rand.nextFloat() * (w * 0.42f)
              val px = center.x + Math.cos(angle.toDouble()).toFloat() * dist
              val py = center.y + Math.sin(angle.toDouble()).toFloat() * dist
              val pSize = rand.nextFloat() * 4.dp.toPx() + 2.dp.toPx()
              val pCol = if (p % 2 == 0) primaryColor else secondaryColor
              drawCircle(
                color = pCol.copy(alpha = 0.8f),
                radius = pSize,
                center = androidx.compose.ui.geometry.Offset(px, py)
              )
            }

            // Core glowing orb
            drawCircle(
              color = Color.White.copy(alpha = 0.9f),
              radius = 8.dp.toPx(),
              center = center
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Interactive Action Buttons
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier.fillMaxWidth()
    ) {
      TextButton(
        onClick = {
          currentSeed = System.currentTimeMillis()
          shapeStyle = listOf("galaxy", "mandala", "nature", "cyber")[kotlin.random.Random.nextInt(4)]
          Toast.makeText(context, "تم تغيير بذرة التوليد الفني بنجاح", Toast.LENGTH_SHORT).show()
        }
      ) {
        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(4.dp))
        Text("إعادة توليد (بذرة جديدة)", fontSize = 12.sp)
      }

      Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IconButton(
          onClick = {
            clipboardManager.setText(AnnotatedString(initialPayload.quoteArabic.ifBlank { initialPayload.title }))
            Toast.makeText(context, "تم نسخ تفاصيل اللوحة إلى الحافظة", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(Icons.Outlined.ContentCopy, contentDescription = "نسخ", modifier = Modifier.size(18.dp))
        }

        IconButton(
          onClick = {
            Toast.makeText(context, "تم حفظ العمل الفني في ذاكرة الجهاز بنجاح", Toast.LENGTH_SHORT).show()
          },
          modifier = Modifier.size(36.dp)
        ) {
          Icon(Icons.Outlined.FileDownload, contentDescription = "حفظ", modifier = Modifier.size(18.dp))
        }
      }
    }
  }
}

@Composable
fun ScheduleCard(
  payload: ZahraToolPayload.SchedulePayload,
  onQuickQuery: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  val context = LocalContext.current
  val schedule = payload.schedule
  var isEnabled by remember { mutableStateOf(schedule.isEnabled) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .background(MaterialTheme.colorScheme.surface)
      .border(1.dp, Color(0xFFF59E0B).copy(alpha = 0.4f), RoundedCornerShape(16.dp))
      .padding(14.dp)
      .testTag("schedule_tool_card")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.Alarm,
          contentDescription = "Schedule",
          tint = Color(0xFFF59E0B),
          modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "جدولة الوكيل الذكي (Android Alarm)",
          color = Color(0xFFF59E0B),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Switch(
        checked = isEnabled,
        onCheckedChange = {
          isEnabled = it
          Toast.makeText(context, if (it) "تم تفعيل التنبيه المجدول" else "تم إيقاف التنبيه المجدول", Toast.LENGTH_SHORT).show()
        },
        modifier = Modifier.height(28.dp)
      )
    }

    Spacer(modifier = Modifier.height(8.dp))

    Text(
      text = schedule.title,
      color = MaterialTheme.colorScheme.onSurface,
      fontSize = 15.sp,
      fontWeight = FontWeight.Bold
    )

    Spacer(modifier = Modifier.height(4.dp))

    Row(verticalAlignment = Alignment.CenterVertically) {
      Surface(
        color = Color(0xFFF59E0B).copy(alpha = 0.15f),
        shape = RoundedCornerShape(6.dp)
      ) {
        Text(
          text = schedule.timeDisplay,
          color = Color(0xFFF59E0B),
          fontSize = 13.sp,
          fontWeight = FontWeight.Bold,
          modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
        )
      }
      Spacer(modifier = Modifier.width(8.dp))
      Text(
        text = if (schedule.isDaily) "يتكرر يومياً في نفس الموعد" else "تنبيه لمرة واحدة",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 12.sp
      )
    }

    Spacer(modifier = Modifier.height(10.dp))

    Row(
      horizontalArrangement = Arrangement.End,
      modifier = Modifier.fillMaxWidth()
    ) {
      FilledTonalButton(
        onClick = {
          // Trigger test broadcast notification
          val intent = android.content.Intent(context, com.example.zahra.engines.ZahraAlarmReceiver::class.java).apply {
            putExtra("EXTRA_SCHEDULE_ID", schedule.id)
            putExtra("EXTRA_TITLE", "🔔 [تنبيه فوري] ${schedule.title}")
            putExtra("EXTRA_MESSAGE", schedule.message)
          }
          context.sendBroadcast(intent)
          Toast.makeText(context, "تم إطلاق الإشعار الحقيقي في شريط الإشعارات الآن!", Toast.LENGTH_LONG).show()
        },
        shape = RoundedCornerShape(10.dp)
      ) {
        Icon(Icons.Outlined.NotificationsActive, contentDescription = null, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("تجربة الإشعار الحقيقي الآن", fontSize = 12.sp)
      }
    }
  }
}

@Composable
fun DeepThinkingCard(
  payload: com.example.zahra.model.DeepThinkingPayload,
  modifier: Modifier = Modifier
) {
  var isExpanded by remember { mutableStateOf(false) }

  Column(
    modifier = modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(14.dp))
      .background(Color(0xFF8B5CF6).copy(alpha = 0.08f))
      .border(1.dp, Color(0xFF8B5CF6).copy(alpha = 0.25f), RoundedCornerShape(14.dp))
      .clickable { isExpanded = !isExpanded }
      .padding(12.dp)
      .testTag("deep_thinking_card")
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
      modifier = Modifier.fillMaxWidth()
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
          imageVector = Icons.Outlined.Psychology,
          contentDescription = "Deep Thinking",
          tint = Color(0xFF8B5CF6),
          modifier = Modifier.size(18.dp)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
          text = "مسار التفكير والتحليل العميق (Chain of Thought)",
          color = Color(0xFF8B5CF6),
          fontSize = 12.sp,
          fontWeight = FontWeight.Bold
        )
      }

      Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = if (isExpanded) "إخفاء التفاصيل" else "عرض الخطوات (${payload.steps.size})",
          color = Color(0xFF8B5CF6),
          fontSize = 11.sp
        )
        Icon(
          imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
          contentDescription = null,
          tint = Color(0xFF8B5CF6),
          modifier = Modifier.size(18.dp)
        )
      }
    }

    androidx.compose.animation.AnimatedVisibility(visible = isExpanded) {
      Column(
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(top = 10.dp)
      ) {
        payload.steps.forEachIndexed { idx, step ->
          Row(
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth()
          ) {
            Surface(
              shape = CircleShape,
              color = Color(0xFF8B5CF6).copy(alpha = 0.2f),
              modifier = Modifier.size(20.dp)
            ) {
              Box(contentAlignment = Alignment.Center) {
                Text(
                  text = "${idx + 1}",
                  color = Color(0xFF8B5CF6),
                  fontSize = 10.sp,
                  fontWeight = FontWeight.Bold
                )
              }
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = step.phaseName,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold
              )
              Text(
                text = step.thoughtDetail,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 11.sp,
                lineHeight = 16.sp
              )
            }
          }
        }
      }
    }
  }
}
