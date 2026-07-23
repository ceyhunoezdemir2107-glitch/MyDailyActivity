package com.example.mydailyactivity.management

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.widget.RemoteViews
import com.example.mydailyactivity.MainActivity
import com.example.mydailyactivity.R
import com.example.mydailyactivity.data.GoalDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext

class GoalReminderWidgetProvider : AppWidgetProvider() {

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { appWidgetId ->
            appWidgetManager.updateAppWidget(
                appWidgetId,
                createRemoteViews(context)
            )
        }
    }

    internal fun createRemoteViews(context: Context): RemoteViews {
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return RemoteViews(context.packageName, R.layout.goal_reminder_widget).apply {
            setOnClickPendingIntent(R.id.widget_root, pendingIntent)
            val personalBitmap = loadUnlockedPersonalBitmap(context)
            if (personalBitmap != null) {
                setImageViewBitmap(R.id.widget_image_background, personalBitmap)
                setImageViewBitmap(R.id.widget_image, personalBitmap)
            } else {
                setImageViewResource(R.id.widget_image_background, R.drawable.reward_sunrise)
                setImageViewResource(R.id.widget_image, R.drawable.reward_sunrise)
            }
        }
    }

    private fun loadUnlockedPersonalBitmap(context: Context): Bitmap? {
        return runCatching {
            runBlocking {
                withContext(Dispatchers.IO) {
                    val dataStore = GoalDataStore(context.applicationContext)
                    val unlockedIds = dataStore.unlockedRewardsFlow.first().toSet()
                    val rewards = dataStore.userRewardsFlow
                        .first()
                    val selectedRewardId = dataStore.selectedWidgetRewardIdFlow.first()
                    val selectedReward = rewards.firstOrNull { reward ->
                        reward.id == selectedRewardId &&
                            reward.imageUri != null &&
                            unlockedIds.contains(reward.id)
                    }
                    val fallbackReward = rewards.lastOrNull { reward ->
                        reward.imageUri != null && unlockedIds.contains(reward.id)
                    }
                    val reward = selectedReward ?: fallbackReward

                    reward?.imageUri?.let { uri -> decodeWidgetBitmap(context, uri) }
                }
            }
        }.getOrNull()
    }

    private fun decodeWidgetBitmap(context: Context, uriString: String): Bitmap? {
        val uri = Uri.parse(uriString)
        val bounds = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }

        context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, bounds)
        }

        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null

        val options = BitmapFactory.Options().apply {
            inSampleSize = calculateInSampleSize(bounds, 640, 360)
        }

        return context.contentResolver.openInputStream(uri)?.use { stream ->
            BitmapFactory.decodeStream(stream, null, options)
        }
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        var inSampleSize = 1
        var width = options.outWidth
        var height = options.outHeight

        while (width / 2 >= reqWidth && height / 2 >= reqHeight) {
            width /= 2
            height /= 2
            inSampleSize *= 2
        }

        return inSampleSize
    }
}
