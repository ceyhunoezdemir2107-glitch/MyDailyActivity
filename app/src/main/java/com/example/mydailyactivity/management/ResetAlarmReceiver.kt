package com.example.mydailyactivity.management

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.mydailyactivity.data.GoalDataStore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class ResetAlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dataStore = GoalDataStore(context.applicationContext)

                when (intent.action) {
                    ResetScheduler.ACTION_DAILY_RESET -> {
                        ResetScheduler.runDailyResetIfDue(context)
                        ResetScheduler.scheduleDailyReset(
                            context,
                            dataStore.resetTimeFlow.first()
                        )
                    }

                    ResetScheduler.ACTION_WEEKLY_RESET -> {
                        ResetScheduler.runWeeklyResetIfDue(context)
                        ResetScheduler.scheduleWeeklyReset(
                            context,
                            dataStore.weeklyResetTimeFlow.first(),
                            dataStore.weeklyResetDayFlow.first()
                        )
                    }

                    else -> Log.w("ResetAlarmReceiver", "Unknown reset action: ${intent.action}")
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
