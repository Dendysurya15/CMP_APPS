package com.cbi.mobile_plantation.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import java.util.*

class NotificationScheduler(private val context: Context) {

    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    fun scheduleDailyNotifications() {
        scheduleMorningNotification()
        scheduleEveningNotification()
    }

    private fun scheduleMorningNotification() {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.NOTIFICATION_TYPE_EXTRA, NotificationReceiver.TYPE_MORNING)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationReceiver.MORNING_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 6)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

//            // If it's past 6 AM today, schedule for tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // Schedule repeating alarm
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    private fun scheduleEveningNotification() {
        val intent = Intent(context, NotificationReceiver::class.java).apply {
            putExtra(NotificationReceiver.NOTIFICATION_TYPE_EXTRA, NotificationReceiver.TYPE_EVENING)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationReceiver.EVENING_NOTIFICATION_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val calendar = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 18) // 6 PM
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)

//            // If it's past 6 PM today, schedule for tomorrow
            if (before(Calendar.getInstance())) {
                add(Calendar.DAY_OF_MONTH, 1)
            }
        }

        // Schedule repeating alarm
        alarmManager.setRepeating(
            AlarmManager.RTC_WAKEUP,
            calendar.timeInMillis,
            AlarmManager.INTERVAL_DAY,
            pendingIntent
        )
    }

    fun cancelAllNotifications() {
        // Cancel morning notification
        val morningIntent = Intent(context, NotificationReceiver::class.java)
        val morningPendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationReceiver.MORNING_NOTIFICATION_ID,
            morningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(morningPendingIntent)

        // Cancel evening notification
        val eveningIntent = Intent(context, NotificationReceiver::class.java)
        val eveningPendingIntent = PendingIntent.getBroadcast(
            context,
            NotificationReceiver.EVENING_NOTIFICATION_ID,
            eveningIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarmManager.cancel(eveningPendingIntent)
    }
}
