package com.example.zahra.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
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
import com.example.zahra.model.ZahraNote
import com.example.zahra.ui.components.GlassCard

@Composable
fun NotesScreen(
  notes: List<ZahraNote>,
  onAddNote: (String, String?) -> Unit,
  onDeleteNote: (String) -> Unit,
  modifier: Modifier = Modifier
) {
  var searchQuery by remember { mutableStateOf("") }
  var showAddDialog by remember { mutableStateOf(false) }
  var newNoteTitle by remember { mutableStateOf("") }
  var newNoteContent by remember { mutableStateOf("") }

  val filteredNotes = notes.filter { n ->
    if (searchQuery.isBlank()) true
    else n.title.contains(searchQuery, ignoreCase = true) ||
      n.content.contains(searchQuery, ignoreCase = true) ||
      n.tags.any { it.contains(searchQuery, ignoreCase = true) }
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
          text = "ملاحظاتي المشفرة",
          color = ZahraTextPrimary,
          fontSize = 20.sp,
          fontWeight = FontWeight.Bold
        )
        Text(
          text = "استخراج وسوم تلقائي بخوارزمية RAKE وبحث نصي فوري",
          color = ZahraTextSecondary,
          fontSize = 12.sp
        )
      }
      IconButton(
        onClick = { showAddDialog = true },
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape)
          .background(ZahraViolet)
          .testTag("add_note_fab")
      ) {
        Icon(Icons.Default.Add, contentDescription = "Add Note", tint = Color.White)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Search bar
    Row(
      verticalAlignment = Alignment.CenterVertically,
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(16.dp))
        .background(ZahraSurfaceGlass)
        .border(1.dp, ZahraGlassBorder, RoundedCornerShape(16.dp))
        .padding(horizontal = 12.dp, vertical = 2.dp)
    ) {
      Icon(Icons.Default.Search, contentDescription = "Search", tint = ZahraTextSecondary)
      TextField(
        value = searchQuery,
        onValueChange = { searchQuery = it },
        placeholder = {
          Text(text = "ابحث في الملاحظات والوسوم...", color = ZahraTextTertiary, fontSize = 13.sp)
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
          .testTag("search_notes_input")
      )
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Notes List
    if (filteredNotes.isEmpty()) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "لا توجد ملاحظات مطابقة.",
          color = ZahraTextTertiary,
          fontSize = 14.sp
        )
      }
    } else {
      LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.weight(1f)
      ) {
        items(filteredNotes, key = { it.id }) { note ->
          NoteCard(
            note = note,
            onDelete = { onDeleteNote(note.id) },
            onTagClick = { tag -> searchQuery = tag }
          )
        }
      }
    }
  }

  // Create Note Dialog
  if (showAddDialog) {
    AlertDialog(
      onDismissRequest = { showAddDialog = false },
      title = {
        Text("تدوين ملاحظة جديدة", color = ZahraTextPrimary, fontWeight = FontWeight.Bold)
      },
      text = {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
          OutlinedTextField(
            value = newNoteTitle,
            onValueChange = { newNoteTitle = it },
            label = { Text("عنوان الملاحظة (اختياري)") },
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = ZahraTextPrimary,
              unfocusedTextColor = ZahraTextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
          )
          OutlinedTextField(
            value = newNoteContent,
            onValueChange = { newNoteContent = it },
            label = { Text("محتوى الملاحظة...") },
            minLines = 4,
            colors = OutlinedTextFieldDefaults.colors(
              focusedTextColor = ZahraTextPrimary,
              unfocusedTextColor = ZahraTextPrimary
            ),
            modifier = Modifier.fillMaxWidth()
          )
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (newNoteContent.isNotBlank()) {
              onAddNote(newNoteContent, if (newNoteTitle.isNotBlank()) newNoteTitle else null)
              newNoteTitle = ""
              newNoteContent = ""
              showAddDialog = false
            }
          },
          colors = ButtonDefaults.buttonColors(containerColor = ZahraCyan)
        ) {
          Text("حفظ محلياً", color = Color.Black)
        }
      },
      dismissButton = {
        TextButton(onClick = { showAddDialog = false }) {
          Text("إلغاء", color = ZahraTextSecondary)
        }
      },
      containerColor = ZahraSurface
    )
  }
}

@Composable
fun NoteCard(
  note: ZahraNote,
  onDelete: () -> Unit,
  onTagClick: (String) -> Unit
) {
  GlassCard(modifier = Modifier.fillMaxWidth()) {
    Row(
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.Top,
      modifier = Modifier.fillMaxWidth()
    ) {
      Text(
        text = note.title,
        color = ZahraTextPrimary,
        fontSize = 15.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.weight(1f)
      )
      IconButton(onClick = onDelete, modifier = Modifier.size(24.dp)) {
        Icon(
          imageVector = Icons.Default.Delete,
          contentDescription = "Delete Note",
          tint = ZahraTextTertiary.copy(alpha = 0.6f),
          modifier = Modifier.size(16.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(6.dp))

    Text(
      text = note.content,
      color = ZahraTextSecondary,
      fontSize = 13.sp,
      lineHeight = 20.sp
    )

    if (note.tags.isNotEmpty()) {
      Spacer(modifier = Modifier.height(10.dp))
      LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        items(note.tags) { tag ->
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(ZahraPink.copy(alpha = 0.15f))
              .clickable { onTagClick(tag) }
              .padding(horizontal = 8.dp, vertical = 3.dp)
          ) {
            Text(
              text = "#$tag",
              color = ZahraPink,
              fontSize = 11.sp,
              fontWeight = FontWeight.Medium
            )
          }
        }
      }
    }
  }
}
