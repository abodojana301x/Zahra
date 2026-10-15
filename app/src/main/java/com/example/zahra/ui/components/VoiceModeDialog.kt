package com.example.zahra.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.ui.theme.GptAccentBlue

@Composable
fun VoiceModeDialog(
  onDismiss: () -> Unit,
  onVoiceInputCompleted: (String) -> Unit
) {
  var isMuted by remember { mutableStateOf(false) }

  // Infinite breathing animation for the voice orb
  val infiniteTransition = rememberInfiniteTransition(label = "VoiceOrbTransition")
  val orbScale by infiniteTransition.animateFloat(
    initialValue = 0.92f,
    targetValue = 1.12f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "OrbScale"
  )
  val pulseAlpha by infiniteTransition.animateFloat(
    initialValue = 0.25f,
    targetValue = 0.65f,
    animationSpec = infiniteRepeatable(
      animation = tween(1200, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "PulseAlpha"
  )

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false)
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .background(Color(0xFF0F0F12))
        .windowInsetsPadding(WindowInsets.safeDrawing)
        .padding(24.dp)
        .testTag("voice_mode_dialog")
    ) {
      // Top bar with close button
      IconButton(
        onClick = onDismiss,
        modifier = Modifier
          .align(Alignment.TopEnd)
          .size(44.dp)
          .clip(CircleShape)
          .background(Color(0xFF26262B))
          .testTag("voice_mode_close_button")
      ) {
        Icon(
          imageVector = Icons.Default.Close,
          contentDescription = "إغلاق",
          tint = Color.White
        )
      }

      // Center Voice Orb
      Column(
        modifier = Modifier.align(Alignment.Center),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Box(
          contentAlignment = Alignment.Center,
          modifier = Modifier.size(200.dp)
        ) {
          // Outer subtle pulse ring
          Box(
            modifier = Modifier
              .size(190.dp)
              .scale(orbScale)
              .clip(CircleShape)
              .background(GptAccentBlue.copy(alpha = pulseAlpha * 0.3f))
          )

          // Inner vibrant orb
          Box(
            modifier = Modifier
              .size(120.dp)
              .clip(CircleShape)
              .background(GptAccentBlue)
              .clickable {
                onVoiceInputCompleted("أهلاً يا زهراء، ما هي مواقيت الصلاة لليوم؟")
                onDismiss()
              },
            contentAlignment = Alignment.Center
          ) {
            Icon(
              imageVector = Icons.Default.GraphicEq,
              contentDescription = "موجات الصوت",
              tint = Color.White,
              modifier = Modifier.size(54.dp)
            )
          }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
          text = if (isMuted) "الميكروفون مكتوم" else "زهراء تستمع إليك...",
          color = Color.White,
          fontSize = 18.sp,
          fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
          text = "تحدث بحرية أو انقر على الدائرة لمحاكاة السؤال",
          color = Color(0xFFA1A1AA),
          fontSize = 14.sp
        )
      }

      // Bottom Control Buttons
      Row(
        modifier = Modifier
          .fillMaxWidth()
          .align(Alignment.BottomCenter)
          .padding(bottom = 16.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
      ) {
        // Mute / Unmute Button
        IconButton(
          onClick = { isMuted = !isMuted },
          modifier = Modifier
            .size(56.dp)
            .clip(CircleShape)
            .background(Color(0xFF26262B))
            .testTag("voice_mode_mute_button")
        ) {
          Icon(
            imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
            contentDescription = "كتم الصوت",
            tint = if (isMuted) Color(0xFFEF4444) else Color.White
          )
        }

        // End session button
        Button(
          onClick = onDismiss,
          colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26262B)),
          shape = CircleShape,
          contentPadding = PaddingValues(horizontal = 24.dp, vertical = 14.dp),
          modifier = Modifier.testTag("voice_mode_end_button")
        ) {
          Text("إنهاء المحادثة الصوتية", color = Color.White, fontSize = 14.sp)
        }
      }
    }
  }
}
