package com.example.mydailyactivity.management

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object ReminderWidgetController {
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
        val appWidgetManager = context.getSystemService(AppWidgetManager::class.java)
        val component = ComponentName(context, GoalReminderWidgetProvider::class.java)
        val existingWidgetIds = appWidgetManager.getAppWidgetIds(component)

        updateWidgetPreview(context)

        if (existingWidgetIds.isNotEmpty()) {
            updateWidgets(context)
            return WidgetPinResult.UpdatedExisting
        }

        if (!appWidgetManager.isRequestPinAppWidgetSupported) {
            return WidgetPinResult.NotSupported
        }

        appWidgetManager.requestPinAppWidget(component, null, null)
        return WidgetPinResult.PinRequested
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

        val appWidgetManager = AppWidgetManager.getInstance(context)
        val component = ComponentName(context, GoalReminderWidgetProvider::class.java)
        val preview = GoalReminderWidgetProvider().createRemoteViews(context)

        appWidgetManager.setWidgetPreview(
            component,
            AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN,
            preview
        )
    }
}
