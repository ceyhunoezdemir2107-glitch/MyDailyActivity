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

class GoalNotificationReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != GoalNotificationScheduler.ACTION_GOAL_NOTIFICATION) {
            Log.w("GoalNotificationReceiver", "Unknown action: ${intent.action}")
            return
        }

        val pendingResult = goAsync()

        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                val dataStore = GoalDataStore(context.applicationContext)
                val notificationsEnabled = dataStore.goalNotificationEnabledFlow.first()
                val notificationTime = dataStore.goalNotificationTimeFlow.first()

                if (notificationsEnabled) {
                    GoalNotificationScheduler.showNotification(context)
                    GoalNotificationScheduler.scheduleDailyNotification(context, notificationTime)
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
