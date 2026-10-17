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

/**
 * Internal Android BroadcastReceiver for Zahra 3 alarms.
 * Configured with android:exported="false" to prevent spoofing or injection of reminder broadcasts by 3rd-party apps.
 * AlarmManager delivers the PendingIntent with system token to unexported receivers safely.
 */
class ZahraAlarmReceiver : BroadcastReceiver() {

  companion object {
    const val ACTION_ALARM_TRIGGER = "com.example.zahra.ALARM_TRIGGER"
  }

  override fun onReceive(context: Context, intent: Intent) {
    // Only accept ACTION_ALARM_TRIGGER
    if (intent.action != ACTION_ALARM_TRIGGER) return

    val scheduleId = intent.getStringExtra("EXTRA_SCHEDULE_ID") ?: return
    val title = intent.getStringExtra("EXTRA_TITLE") ?: "تنبيه من زهرة 3"
    val message = intent.getStringExtra("EXTRA_MESSAGE") ?: "حان موعد المهمة المجدولة المعتمدة."

    val scheduleEngine = ScheduleEngine(context)
    // Duplicate delivery prevention: Check with schedule engine before displaying
    val shouldDeliver = scheduleEngine.onAlarmTriggered(scheduleId)
    if (shouldDeliver) {
      showNotification(context, scheduleId.hashCode(), title, message)
    }
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
