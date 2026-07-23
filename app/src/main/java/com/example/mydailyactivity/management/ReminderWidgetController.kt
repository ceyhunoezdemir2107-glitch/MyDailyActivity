package com.example.mydailyactivity.management

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log

object ReminderWidgetController {
    private const val TAG = "ReminderWidgetController"

    enum class WidgetPinResult {
        PinRequested,
        UpdatedExisting,
        NotSupported
    }

    fun setEnabled(context: Context, enabled: Boolean) {
        val component = ComponentName(context, GoalReminderWidgetProvider::class.java)
        val state = if (enabled) {
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED
        } else {
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED
        }

        context.packageManager.setComponentEnabledSetting(
            component,
            state,
            PackageManager.DONT_KILL_APP
        )
    }

    fun requestSingleWidget(context: Context): WidgetPinResult {
        setEnabled(context, true)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, GoalReminderWidgetProvider::class.java)
        val existingWidgetIds = runCatching {
            appWidgetManager.getAppWidgetIds(component)
        }.getOrDefault(IntArray(0))

        updateWidgetPreview(context)

        if (existingWidgetIds.isNotEmpty()) {
            updateWidgets(context)
            return WidgetPinResult.UpdatedExisting
        }

        if (!runCatching { appWidgetManager.isRequestPinAppWidgetSupported }.getOrDefault(false)) {
            return WidgetPinResult.NotSupported
        }

        return runCatching {
            appWidgetManager.requestPinAppWidget(component, null, null)
            WidgetPinResult.PinRequested
        }.getOrElse { error ->
            Log.w(TAG, "Widget pin request could not be sent.", error)
            WidgetPinResult.NotSupported
        }
    }

    fun updateWidgets(context: Context) {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, GoalReminderWidgetProvider::class.java)
        val widgetIds = appWidgetManager.getAppWidgetIds(component)

        updateWidgetPreview(context)

        if (widgetIds.isNotEmpty()) {
            GoalReminderWidgetProvider().onUpdate(context, appWidgetManager, widgetIds)
        }
    }

    private fun updateWidgetPreview(context: Context) {
        if (Build.VERSION.SDK_INT < 35) return

        runCatching {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val component = ComponentName(context, GoalReminderWidgetProvider::class.java)
            val preview = GoalReminderWidgetProvider().createRemoteViews(context)

            appWidgetManager.setWidgetPreview(
                component,
                AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
                preview
            )
        }.onFailure { error ->
            Log.w(TAG, "Widget preview could not be updated.", error)
        }
    }
}
