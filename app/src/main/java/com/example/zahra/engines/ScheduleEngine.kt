package com.example.zahra.engines

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.os.Build
import com.example.zahra.model.ZahraSchedule
import org.json.JSONArray
import org.json.JSONObject
import java.util.Calendar
import java.util.Locale

class ScheduleEngine(private val context: Context) {

  private val prefs: SharedPreferences =
    context.getSharedPreferences("zahra_schedules_prefs", Context.MODE_PRIVATE)

  private val schedules = mutableListOf<ZahraSchedule>()

  init {
    loadSchedules()
    if (schedules.isEmpty()) {
      // Seed initial helpful schedule
      val defaultSchedule = ZahraSchedule(
        id = "default_sched_morning",
        title = "تحفيز وأذكار الصباح",
        message = "صباح الخير! ابدأ يومك بإيجابية وتوكل على الله.",
        timeDisplay = "08:00 ص",
        hour = 8,
        minute = 0,
        isDaily = true,
        isEnabled = true,
        actionType = "DAILY_MOTIVATION"
      )
      addSchedule(defaultSchedule)
    }
  }

  fun getSchedules(): List<ZahraSchedule> = schedules.toList()

  fun addSchedule(schedule: ZahraSchedule) {
    schedules.removeAll { it.id == schedule.id }
    schedules.add(0, schedule)
    saveSchedules()
    if (schedule.isEnabled) {
      registerSystemAlarm(schedule)
    }
  }

  fun toggleSchedule(id: String): Boolean {
    val index = schedules.indexOfFirst { it.id == id }
    if (index != -1) {
      val item = schedules[index]
      val updated = item.copy(isEnabled = !item.isEnabled)
      schedules[index] = updated
      saveSchedules()
      if (updated.isEnabled) {
        registerSystemAlarm(updated)
      } else {
        cancelSystemAlarm(updated)
      }
      return updated.isEnabled
    }
    return false
  }

  fun deleteSchedule(id: String) {
    val item = schedules.find { it.id == id }
    if (item != null) {
      cancelSystemAlarm(item)
      schedules.remove(item)
      saveSchedules()
    }
  }

  fun testFireNotification(schedule: ZahraSchedule) {
    val intent = Intent(context, ZahraAlarmReceiver::class.java).apply {
      putExtra("EXTRA_SCHEDULE_ID", schedule.id)
      putExtra("EXTRA_TITLE", "🔔 [تجربة فورية] ${schedule.title}")
      putExtra("EXTRA_MESSAGE", schedule.message)
    }
    context.sendBroadcast(intent)
  }

  private fun registerSystemAlarm(schedule: ZahraSchedule) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

    val intent = Intent(context, ZahraAlarmReceiver::class.java).apply {
      putExtra("EXTRA_SCHEDULE_ID", schedule.id)
      putExtra("EXTRA_TITLE", schedule.title)
      putExtra("EXTRA_MESSAGE", schedule.message)
    }

    val pendingIntent = PendingIntent.getBroadcast(
      context,
      schedule.id.hashCode(),
      intent,
      PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    val calendar = Calendar.getInstance().apply {
      set(Calendar.HOUR_OF_DAY, schedule.hour)
      set(Calendar.MINUTE, schedule.minute)
      set(Calendar.SECOND, 0)
      set(Calendar.MILLISECOND, 0)
      if (timeInMillis <= System.currentTimeMillis()) {
        add(Calendar.DAY_OF_YEAR, 1)
      }
    }

    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        alarmManager.setExactAndAllowWhileIdle(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          pendingIntent
        )
      } else {
        alarmManager.set(
          AlarmManager.RTC_WAKEUP,
          calendar.timeInMillis,
          pendingIntent
        )
      }
    } catch (_: SecurityException) {
      // In case exact alarm permission is restricted on specific OEM, fallback gracefully
      alarmManager.set(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        pendingIntent
      )
    }
  }

  private fun cancelSystemAlarm(schedule: ZahraSchedule) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
    val intent = Intent(context, ZahraAlarmReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
      context,
      schedule.id.hashCode(),
      intent,
      PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
    )
    if (pendingIntent != null) {
      alarmManager.cancel(pendingIntent)
      pendingIntent.cancel()
    }
  }

  fun parseAndCreateFromNaturalLanguage(text: String): ZahraSchedule {
    val lower = text.lowercase(Locale.ROOT)

    // Detect Hour & Minute
    var hour = 9
    var minute = 0
    var isPm = lower.contains("مساء") || lower.contains("عصر") || lower.contains("مغرب") ||
               lower.contains("عشاء") || lower.contains("ليل") || lower.contains("pm")

    // Match patterns like "الساعة 8" or "8:30" or "8 ونص"
    val timeRegex = Regex("""(?:الساعة\s*)?(\d{1,2})(?::(\d{1,2}))?""")
    val match = timeRegex.find(text)
    if (match != null) {
      val rawHour = match.groupValues[1].toIntOrNull() ?: 9
      val rawMinute = match.groupValues[2].toIntOrNull() ?: 0
      minute = rawMinute
      hour = if (isPm && rawHour < 12) rawHour + 12 else if (!isPm && rawHour == 12) 0 else rawHour
    } else {
      // Textual hours
      if (lower.contains("ثماني") || lower.contains("تمانية")) hour = if (isPm) 20 else 8
      if (lower.contains("تسع")) hour = if (isPm) 21 else 9
      if (lower.contains("عشر")) hour = if (isPm) 22 else 10
      if (lower.contains("سبع")) hour = if (isPm) 19 else 7
      if (lower.contains("ست")) hour = if (isPm) 18 else 6
      if (lower.contains("خمس")) hour = if (isPm) 17 else 5
      if (lower.contains("اربع")) hour = if (isPm) 16 else 4
    }

    if (lower.contains("نص") || lower.contains("نصف") || lower.contains("30")) {
      minute = 30
    } else if (lower.contains("ربع") || lower.contains("15")) {
      minute = 15
    }

    val isDaily = lower.contains("يومي") || lower.contains("كل يوم") || lower.contains("دايما") || lower.contains("daily")

    // Clean title and message
    var cleanTitle = text
      .replace(Regex("""(فكرني|ذكرني|جدول|ابعتلي|كل يوم|يومياً|الساعة|\d{1,2}|صباحاً|مساءً|ونص|وربع)"""), "")
      .trim()
    if (cleanTitle.isBlank() || cleanTitle.length < 3) {
      cleanTitle = "مهمة وتذكير مجدول"
    }

    val displayPeriod = if (hour < 12) "ص" else "م"
    val displayHour = if (hour % 12 == 0) 12 else hour % 12
    val timeDisplay = String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, displayPeriod)

    val schedule = ZahraSchedule(
      title = cleanTitle,
      message = "تذكير من زهرة: حان موعد [$cleanTitle]. استعن بالله وأنجز هدفك!",
      timeDisplay = timeDisplay,
      hour = hour,
      minute = minute,
      isDaily = isDaily,
      isEnabled = true,
      actionType = if (isDaily) "DAILY_TASK" else "REMINDER"
    )

    addSchedule(schedule)
    return schedule
  }

  private fun saveSchedules() {
    val array = JSONArray()
    for (s in schedules) {
      val obj = JSONObject().apply {
        put("id", s.id)
        put("title", s.title)
        put("message", s.message)
        put("timeDisplay", s.timeDisplay)
        put("hour", s.hour)
        put("minute", s.minute)
        put("isDaily", s.isDaily)
        put("isEnabled", s.isEnabled)
        put("actionType", s.actionType)
        put("createdAt", s.createdAt)
      }
      array.put(obj)
    }
    prefs.edit().putString("schedules_data", array.toString()).apply()
  }

  private fun loadSchedules() {
    schedules.clear()
    val raw = prefs.getString("schedules_data", null) ?: return
    try {
      val array = JSONArray(raw)
      for (i in 0 until array.length()) {
        val obj = array.getJSONObject(i)
        schedules.add(
          ZahraSchedule(
            id = obj.getString("id"),
            title = obj.getString("title"),
            message = obj.getString("message"),
            timeDisplay = obj.getString("timeDisplay"),
            hour = obj.getInt("hour"),
            minute = obj.getInt("minute"),
            isDaily = obj.optBoolean("isDaily", true),
            isEnabled = obj.optBoolean("isEnabled", true),
            actionType = obj.optString("actionType", "REMINDER"),
            createdAt = obj.optLong("createdAt", System.currentTimeMillis())
          )
        )
      }
    } catch (_: Exception) {
      // Fallback
    }
  }
}
