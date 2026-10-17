package com.example.zahra.engines

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/**
 * System broadcast receiver for device reboot and time adjustment reconciliation.
 * Exported exclusively to receive system broadcasts (BOOT_COMPLETED, TIME_SET, TIMEZONE_CHANGED)
 * and protected by the system-level android.permission.RECEIVE_BOOT_COMPLETED.
 * Reconciles alarms, cancels expired one-off reminders, and restores pending schedules deterministically.
 * Does NOT accept or process internal ALARM_TRIGGER actions.
 */
class ZahraBootReceiver : BroadcastReceiver() {

  override fun onReceive(context: Context, intent: Intent) {
    val action = intent.action
    if (action == Intent.ACTION_BOOT_COMPLETED ||
      action == Intent.ACTION_MY_PACKAGE_REPLACED ||
      action == Intent.ACTION_TIME_CHANGED ||
      action == Intent.ACTION_TIMEZONE_CHANGED
    ) {
      val scheduleEngine = ScheduleEngine(context)
      scheduleEngine.reconcileSchedules()
    }
  }
}
