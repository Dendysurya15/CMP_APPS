package com.cbi.mobile_plantation.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.cbi.mobile_plantation.R
import com.cbi.mobile_plantation.ui.view.HomePageActivity

class NotificationReceiver : BroadcastReceiver() {

    companion object {
        const val MORNING_NOTIFICATION_ID = 1001
        const val EVENING_NOTIFICATION_ID = 1002
        const val CHANNEL_ID = "DAILY_REMINDERS"
        const val NOTIFICATION_TYPE_EXTRA = "notification_type"
        const val TYPE_MORNING = "morning"
        const val TYPE_EVENING = "evening"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val type = intent.getStringExtra(NOTIFICATION_TYPE_EXTRA)

        createNotificationChannel(context)

        when (type) {
            TYPE_MORNING -> showMorningNotification(context)
            TYPE_EVENING -> showEveningNotification(context)
        }
    }

    private fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pengingat Harian",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifikasi pengingat sinkronisasi dan upload data"
                enableVibration(true)
                setShowBadge(true)
            }

            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun showMorningNotification(context: Context) {
        val intent = Intent(context, HomePageActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Add your notification icon
            .setContentTitle("Pengingat Sinkronisasi Data")
            .setContentText("Jangan lupa sinkronisasi data")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Jangan lupa sinkronisasi data, untuk memperlancar pekerjaan anda, diharap untuk melakukan sinkronisasi data setiap pagi menggunakan jaringan internet, sinkronisasi data tidak memerlukan bandwidth data yang besar"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(MORNING_NOTIFICATION_ID, notification)
    }

    private fun showEveningNotification(context: Context) {
        val intent = Intent(context, HomePageActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher_round) // Add your notification icon
            .setContentTitle("Pengingat Upload Data")
            .setContentText("Jangan lupa upload data pekerjaan")
            .setStyle(NotificationCompat.BigTextStyle()
                .bigText("Jangan lupa melakukan upload data pekerjaan anda hari ini, sehingga data dapat tersimpan pada sistem dan terlapor dengan baik"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000))
            .build()

        val notificationManager = NotificationManagerCompat.from(context)
        notificationManager.notify(EVENING_NOTIFICATION_ID, notification)
    }
}