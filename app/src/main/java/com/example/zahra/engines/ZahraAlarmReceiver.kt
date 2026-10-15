package com.example.zahra.engines

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity

class ZahraAlarmReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val scheduleId = intent.getStringExtra("EXTRA_SCHEDULE_ID") ?: "zahra_sched"
    val title = intent.getStringExtra("EXTRA_TITLE") ?: "تنبيه من وكيل زهرة 3"
    val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "حان موعد المهمة المجدولة المعتمدة."

    showNotification(context, scheduleId.hashCode(), title, message)
  }

  private fun showNotification(
    context: Context,
    notificationId: Int,
    title: String,
    message: String
  ) {
    val channelId = "zahra_agent_notifications"
    val notificationManager =
      context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val channel = NotificationChannel(
        channelId,
        "تنبيهات وجدولة زهرة 3",
        NotificationManager.IMPORTANCE_HIGH
      ).apply {
        description = "إشعارات المهام والمواعيد المجدولة للوكيل زهرة"
        enableVibration(true)
      }
      notificationManager.createNotificationChannel(channel)
    }

    val openIntent = Intent(context, MainActivity::class.java).apply {
      flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
    }
    val pendingIntent = PendingIntent.getActivity(
      context,
      notificationId,
      openIntent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val notification = NotificationCompat.Builder(context, channelId)
      .setSmallIcon(android.R.drawable.ic_dialog_info)
      .setContentTitle(title)
      .setContentText(message)
      .setStyle(NotificationCompat.BigTextStyle().bigText(message))
      .setPriority(NotificationCompat.PRIORITY_HIGH)
      .setAutoCancel(true)
      .setContentIntent(pendingIntent)
      .build()

    notificationManager.notify(notificationId, notification)
  }
}
