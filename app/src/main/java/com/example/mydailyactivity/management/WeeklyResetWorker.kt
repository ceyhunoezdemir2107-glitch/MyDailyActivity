package com.example.mydailyactivity.management

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mydailyactivity.data.GoalDataStore
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

class WeeklyResetWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("WeeklyResetWorker", "Worker gestartet um: ${LocalDateTime.now()}")

        val dataStore = GoalDataStore(applicationContext)
        ResetScheduler.runWeeklyResetIfDue(applicationContext)

        Log.d("WeeklyResetWorker", "Worker beendet um: ${LocalDateTime.now()}")

        ResetScheduler.scheduleWeeklyReset(
            applicationContext,
            dataStore.weeklyResetTimeFlow.first(),
            dataStore.weeklyResetDayFlow.first()
        )

        return Result.success()
    }
}


