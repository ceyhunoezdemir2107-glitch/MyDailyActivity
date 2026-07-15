package com.example.mydailyactivity.management

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.mydailyactivity.data.GoalDataStore
import kotlinx.coroutines.flow.first
import java.time.LocalDateTime

class DailyResetWorker(
    context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result {
        Log.d("DailyResetWorker", "Worker gestartet um: ${LocalDateTime.now()}")

        val dataStore = GoalDataStore(applicationContext)
        ResetScheduler.runDailyResetIfDue(applicationContext)

        Log.d("DailyResetWorker", "Worker beendet um: ${LocalDateTime.now()}")

        ResetScheduler.scheduleDailyReset(
            applicationContext,
            dataStore.resetTimeFlow.first()
        )

        return Result.success()
    }
}
