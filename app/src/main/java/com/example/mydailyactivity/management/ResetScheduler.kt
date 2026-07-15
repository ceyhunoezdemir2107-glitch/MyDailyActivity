package com.example.mydailyactivity.management

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.mydailyactivity.data.GoalDataStore
import kotlinx.coroutines.flow.first
import java.time.DayOfWeek
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import java.util.concurrent.TimeUnit

object ResetScheduler {
    const val ACTION_DAILY_RESET = "com.example.mydailyactivity.action.DAILY_RESET"
    const val ACTION_WEEKLY_RESET = "com.example.mydailyactivity.action.WEEKLY_RESET"

    private const val DAILY_RESET_WORK_NAME = "daily_reset"
    private const val WEEKLY_RESET_WORK_NAME = "weekly_reset"
    private const val DAILY_ALARM_REQUEST_CODE = 1001
    private const val WEEKLY_ALARM_REQUEST_CODE = 1002
    private const val TAG = "ResetScheduler"

    private val fallbackResetTime = LocalTime.of(4, 0)

    fun canScheduleExactResets(context: Context): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        return alarmManager.canScheduleExactAlarms()
    }

    fun openExactAlarmSettings(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return

        val intent = Intent(
            Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
            Uri.parse("package:${context.packageName}")
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)

        runCatching {
            context.startActivity(intent)
        }.onFailure {
            val fallbackIntent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                .setData(Uri.parse("package:${context.packageName}"))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(fallbackIntent)
        }
    }

    fun scheduleDailyReset(context: Context, time: String) {
        val resetTime = parseResetTime(time)
        val now = LocalDateTime.now()
        val nextRun = nextDailyRun(now, resetTime)

        scheduleExactAlarm(
            context = context,
            action = ACTION_DAILY_RESET,
            requestCode = DAILY_ALARM_REQUEST_CODE,
            triggerAt = nextRun
        )
        scheduleBackupDailyWork(context, now, nextRun)
    }

    fun scheduleWeeklyReset(context: Context, time: String, day: String = "MONDAY") {
        val resetTime = parseResetTime(time)
        val resetDay = parseResetDay(day)
        val now = LocalDateTime.now()
        val nextRun = nextWeeklyRun(now, resetTime, resetDay)

        scheduleExactAlarm(
            context = context,
            action = ACTION_WEEKLY_RESET,
            requestCode = WEEKLY_ALARM_REQUEST_CODE,
            triggerAt = nextRun
        )
        scheduleBackupWeeklyWork(context, now, nextRun)
    }

    suspend fun runDailyResetIfDue(context: Context): Boolean {
        val dataStore = GoalDataStore(context.applicationContext)
        val resetTime = parseResetTime(dataStore.resetTimeFlow.first())
        val now = LocalDateTime.now()
        val period = dailyPeriodId(now, resetTime)
        val lastPeriod = dataStore.getLastDailyResetPeriod()

        if (lastPeriod == period) return false

        if (lastPeriod == null && now.toLocalTime().isBefore(resetTime)) {
            dataStore.markDailyResetPeriod(period)
            return false
        }

        val didReset = dataStore.resetDailyGoalsForPeriod(period)
        if (didReset) {
            Log.d(TAG, "Daily reset completed for period $period")
        }
        return didReset
    }

    suspend fun runWeeklyResetIfDue(context: Context): Boolean {
        val dataStore = GoalDataStore(context.applicationContext)
        val resetTime = parseResetTime(dataStore.weeklyResetTimeFlow.first())
        val resetDay = parseResetDay(dataStore.weeklyResetDayFlow.first())
        val now = LocalDateTime.now()
        val period = weeklyPeriodId(now, resetTime, resetDay)
        val lastPeriod = dataStore.getLastWeeklyResetPeriod()

        if (lastPeriod == period) return false

        if (lastPeriod == null && now.isBefore(thisWeeksReset(now, resetTime, resetDay))) {
            dataStore.markWeeklyResetPeriod(period)
            return false
        }

        val didReset = dataStore.resetWeeklyGoalsForPeriod(period)
        if (didReset) {
            Log.d(TAG, "Weekly reset completed for period $period")
        }
        return didReset
    }

    private fun scheduleExactAlarm(
        context: Context,
        action: String,
        requestCode: Int,
        triggerAt: LocalDateTime
    ) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            Intent(context, ResetAlarmReceiver::class.java).setAction(action),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val triggerMillis = triggerAt
            .atZone(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli()

        if (!canScheduleExactResets(context)) {
            Log.w(TAG, "Exact alarms are not allowed. Falling back to an inexact alarm.")
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                triggerMillis,
                pendingIntent
            )
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerMillis, pendingIntent)
        }
    }

    private fun scheduleBackupDailyWork(context: Context, now: LocalDateTime, nextRun: LocalDateTime) {
        val request = OneTimeWorkRequestBuilder<DailyResetWorker>()
            .setInitialDelay(Duration.between(now, nextRun).toMillis(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            DAILY_RESET_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun scheduleBackupWeeklyWork(context: Context, now: LocalDateTime, nextRun: LocalDateTime) {
        val request = OneTimeWorkRequestBuilder<WeeklyResetWorker>()
            .setInitialDelay(Duration.between(now, nextRun).toMillis(), TimeUnit.MILLISECONDS)
            .build()

        WorkManager.getInstance(context).enqueueUniqueWork(
            WEEKLY_RESET_WORK_NAME,
            ExistingWorkPolicy.REPLACE,
            request
        )
    }

    private fun nextDailyRun(now: LocalDateTime, resetTime: LocalTime): LocalDateTime {
        var nextRun = now
            .withHour(resetTime.hour)
            .withMinute(resetTime.minute)
            .withSecond(0)
            .withNano(0)

        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusDays(1)
        }

        return nextRun
    }

    private fun nextWeeklyRun(
        now: LocalDateTime,
        resetTime: LocalTime,
        resetDay: DayOfWeek
    ): LocalDateTime {
        var nextRun = now
            .with(TemporalAdjusters.nextOrSame(resetDay))
            .withHour(resetTime.hour)
            .withMinute(resetTime.minute)
            .withSecond(0)
            .withNano(0)

        if (!nextRun.isAfter(now)) {
            nextRun = nextRun.plusWeeks(1)
        }

        return nextRun
    }

    private fun dailyPeriodId(now: LocalDateTime, resetTime: LocalTime): String {
        val periodDate = if (now.toLocalTime().isBefore(resetTime)) {
            now.toLocalDate().minusDays(1)
        } else {
            now.toLocalDate()
        }
        return periodDate.toString()
    }

    private fun weeklyPeriodId(
        now: LocalDateTime,
        resetTime: LocalTime,
        resetDay: DayOfWeek
    ): String {
        var resetMoment = thisWeeksReset(now, resetTime, resetDay)
        if (now.isBefore(resetMoment)) {
            resetMoment = resetMoment.minusWeeks(1)
        }
        return resetMoment.toLocalDate().toString()
    }

    private fun thisWeeksReset(
        now: LocalDateTime,
        resetTime: LocalTime,
        resetDay: DayOfWeek
    ): LocalDateTime {
        val today = now.toLocalDate()
        val daysSinceResetDay = (today.dayOfWeek.value - resetDay.value + 7) % 7
        val resetDate = today.minusDays(daysSinceResetDay.toLong())
        return LocalDate.from(resetDate).atTime(resetTime)
    }

    private fun parseResetTime(time: String): LocalTime {
        return runCatching {
            val parts = time.split(":")
            LocalTime.of(parts[0].toInt(), parts[1].toInt())
        }.getOrDefault(fallbackResetTime)
    }

    private fun parseResetDay(day: String): DayOfWeek {
        return runCatching {
            DayOfWeek.valueOf(day.uppercase(Locale.ROOT))
        }.getOrDefault(DayOfWeek.MONDAY)
    }
}
