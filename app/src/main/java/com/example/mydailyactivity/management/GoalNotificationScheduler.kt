package com.example.mydailyactivity.management

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.mydailyactivity.MainActivity
import com.example.mydailyactivity.R
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId

object GoalNotificationScheduler {
    const val ACTION_GOAL_NOTIFICATION = "com.example.mydailyactivity.action.GOAL_NOTIFICATION"

    private const val CHANNEL_ID = "goal_reminders"
    private const val CHANNEL_NAME = "Ziel-Erinnerungen"
    private const val NOTIFICATION_ID = 2107
    private const val REQUEST_CODE = 2108
    private const val TAG = "GoalNotificationScheduler"
    private val fallbackTime = LocalTime.of(20, 0)

    fun hasNotificationPermission(context: Context): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    fun scheduleDailyNotification(context: Context, time: String) {
        val triggerAt = nextDailyRun(LocalDateTime.now(), parseTime(time))
        val triggerMillis = triggerAt
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = reminderPendingIntent(context)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            Log.w(TAG, "Exact alarms are not allowed. Falling back to an inexact notification alarm.")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    fun cancelDailyNotification(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        alarmManager.cancel(reminderPendingIntent(context))
    }

    fun showNotification(context: Context, message: String) {
        if (!hasNotificationPermission(context)) {
            Log.w(TAG, "Notification permission is missing.")
            return
        }

        createChannel(context)
        val notificationMessage = message.ifBlank { "MyDailyActivity" }

        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Denk an deine Ziele")
            .setContentText(notificationMessage)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notificationMessage))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = "Tägliche Erinnerung an deine Ziele"
        }

        val notificationManager = context.getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun reminderPendingIntent(context: Context): PendingIntent {
        return PendingIntent.getBroadcast(
            context,
            REQUEST_CODE,
            Intent(context, GoalNotificationReceiver::class.java).setAction(ACTION_GOAL_NOTIFICATION),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun nextDailyRun(now: LocalDateTime, time: LocalTime): LocalDateTime {
        var nextRun = now
            .withHour(time.hour)
            .withMinute(time.minute)
            .withSecond(0)
            .withNano(0)

        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1)
        }

        return nextRun
    }

    private fun parseTime(time: String): LocalTime {
        return runCatching {
            val parts = time.split(":")
            LocalTime.of(parts[0].toInt(), parts[1].toInt())
        }.getOrDefault(fallbackTime)
    }
}
