package com.example.mydailyactivity.userinterface

import android.app.TimePickerDialog
import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.mydailyactivity.data.AlbumDataStore
import com.example.mydailyactivity.data.GoalDataStore
import com.example.mydailyactivity.management.ReminderWidgetController
import com.example.mydailyactivity.management.ResetScheduler
import kotlinx.coroutines.launch

@Composable
fun SettingsScreen(goalDataStore: GoalDataStore, albumDataStore: AlbumDataStore) {
    val dailyResetTime by goalDataStore.resetTimeFlow.collectAsState(initial = "04:00")
    val weeklyResetTime by goalDataStore.weeklyResetTimeFlow.collectAsState(initial = "04:00")
    val weeklyResetDay by goalDataStore.weeklyResetDayFlow.collectAsState(initial = "MONDAY")
    val goalMode by goalDataStore.goalModeFlow.collectAsState(initial = "daily")
    val costMode by goalDataStore.costModeFlow.collectAsState(initial = "random")
    val fixedCost by goalDataStore.fixedCostFlow.collectAsState(initial = 50)
    val remindersEnabled by goalDataStore.remindersEnabledFlow.collectAsState(initial = false)
    val unlockedRewards by goalDataStore.unlockedRewardsFlow.collectAsState(initial = emptyList())
    val userRewards by goalDataStore.userRewardsFlow.collectAsState(initial = emptyList())
    val selectedWidgetRewardId by goalDataStore.selectedWidgetRewardIdFlow.collectAsState(initial = null)

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var showResetDialog by remember { mutableStateOf(false) }
    var fixedCostInput by remember { mutableStateOf(fixedCost.toString()) }
    var exactResetsAllowed by remember {
        mutableStateOf(ResetScheduler.canScheduleExactResets(context))
    }
    var widgetPinMessage by remember { mutableStateOf<String?>(null) }

    val goalModeGerman = when (goalMode) {
        "daily" -> "Täglich"
        "weekly" -> "Wöchentlich"
        else -> goalMode
    }

    val costModeGerman = when (costMode) {
        "random" -> "Zufällig"
        "linear" -> "Linear"
        "quadratic" -> "Quadratisch"
        "exponential" -> "Exponentiell"
        "logarithmic" -> "Logarithmisch"
        "constant" -> "Konstant"
        else -> costMode
    }
    val weeklyResetDayGerman = weeklyResetDayToGerman(weeklyResetDay)
    val unlockedWidgetRewards = userRewards.filter { reward ->
        reward.imageUri != null && unlockedRewards.contains(reward.id)
    }
    val canUseReminders = unlockedWidgetRewards.isNotEmpty()
    val effectiveRemindersEnabled = remindersEnabled && canUseReminders
    val defaultWidgetRewardId = unlockedWidgetRewards.lastOrNull()?.id
    val selectedWidgetRewardExists = unlockedWidgetRewards.any { reward ->
        reward.id == selectedWidgetRewardId
    }
    val widgetImageOptions =
        unlockedWidgetRewards.mapIndexed { index, reward -> "Bild ${index + 1}" to reward.id }
    val selectedWidgetImageLabel = widgetImageOptions
        .firstOrNull { (_, rewardId) -> rewardId == selectedWidgetRewardId }
        ?.first
        ?: widgetImageOptions
            .firstOrNull { (_, rewardId) -> rewardId == defaultWidgetRewardId }
            ?.first
        ?: "Kein Bild"

    LaunchedEffect(canUseReminders, remindersEnabled, selectedWidgetRewardId, defaultWidgetRewardId) {
        if (!canUseReminders && remindersEnabled) {
            ReminderWidgetController.setEnabled(context, false)
            goalDataStore.updateRemindersEnabled(false)
            goalDataStore.updateSelectedWidgetRewardId(null)
        } else if (canUseReminders && (!selectedWidgetRewardExists || selectedWidgetRewardId == null)) {
            goalDataStore.updateSelectedWidgetRewardId(defaultWidgetRewardId)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = "Einstellungen",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.onBackground
        )

        SettingsCard(title = "Reset-Zeitpunkte") {
            ResetTimeRow(
                title = "Reset-Zeitpunkt täglich",
                subtitle = "Setzt erledigte Tagesziele jeden Tag zurück.",
                time = dailyResetTime,
                onClick = {
                    showTimePicker(context, dailyResetTime) { newTime ->
                        scope.launch { goalDataStore.updateResetTime(newTime) }
                        ResetScheduler.scheduleDailyReset(context, newTime)
                    }
                }
            )

            Spacer(Modifier.height(12.dp))

            WeeklyResetDayRow(
                selectedDay = weeklyResetDayGerman,
                onSelect = { germanDay ->
                    val day = germanWeekdayToValue(germanDay)
                    scope.launch { goalDataStore.updateWeeklyResetDay(day) }
                    ResetScheduler.scheduleWeeklyReset(context, weeklyResetTime, day)
                }
            )

            Spacer(Modifier.height(12.dp))

            ResetTimeRow(
                title = "Reset-Zeitpunkt wöchentlich",
                subtitle = "Setzt erledigte Wochenziele am gewählten Tag zurück.",
                time = weeklyResetTime,
                onClick = {
                    showTimePicker(context, weeklyResetTime) { newTime ->
                        scope.launch { goalDataStore.updateWeeklyResetTime(newTime) }
                        ResetScheduler.scheduleWeeklyReset(context, newTime, weeklyResetDay)
                    }
                }
            )
        }

        SettingsCard(title = "Exakte Reset-Zeit") {
            Text(
                text = if (exactResetsAllowed) {
                    "Exakte Alarme sind erlaubt. Der Reset kann zur gewählten Uhrzeit ausgelöst werden."
                } else {
                    "Android blockiert exakte Alarme. Ohne diese Freigabe kann der Reset später als zur gewählten Uhrzeit passieren."
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    ResetScheduler.openExactAlarmSettings(context)
                    exactResetsAllowed = ResetScheduler.canScheduleExactResets(context)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Berechtigung öffnen")
            }
        }

        SettingsCard(title = "Erinnerungen") {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(2.dp)
                ) {
                        Text("Erinnerungen aktivieren", style = MaterialTheme.typography.bodyLarge)
                        Text(
                        text = if (effectiveRemindersEnabled) {
                            "Das Ziel-Erinnerungswidget ist im System verfügbar."
                        } else if (!canUseReminders) {
                            "Schalte zuerst ein persönliches Belohnungsbild frei."
                        } else {
                            "Das Ziel-Erinnerungswidget bleibt deaktiviert."
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Switch(
                    checked = effectiveRemindersEnabled,
                    enabled = canUseReminders,
                    onCheckedChange = { enabled ->
                        val shouldEnable = enabled && canUseReminders
                        ReminderWidgetController.setEnabled(context, shouldEnable)
                        if (shouldEnable) {
                            ReminderWidgetController.updateWidgets(context)
                        }
                        widgetPinMessage = null
                        scope.launch {
                            goalDataStore.updateRemindersEnabled(shouldEnable)
                            if (!shouldEnable) {
                                goalDataStore.updateSelectedWidgetRewardId(null)
                            }
                        }
                    }
                )
            }

            if (unlockedWidgetRewards.isNotEmpty()) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Widget-Bild", style = MaterialTheme.typography.bodyLarge)
                        Text(
                            text = "Wähle ein freigeschaltetes persönliches Bild.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    DropdownMenuButton(
                        selected = selectedWidgetImageLabel,
                        options = widgetImageOptions.map { it.first },
                        onSelect = { label ->
                            val selectedRewardId = widgetImageOptions
                                .firstOrNull { (optionLabel, _) -> optionLabel == label }
                                ?.second

                            scope.launch {
                                goalDataStore.updateSelectedWidgetRewardId(selectedRewardId)
                                ReminderWidgetController.updateWidgets(context)
                            }
                        }
                    )
                }
            } else {
                Text(
                    text = "Noch kein persönliches freigeschaltetes Bild für das Widget verfügbar.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (effectiveRemindersEnabled) {
                Button(
                    onClick = {
                        ReminderWidgetController.setEnabled(context, true)
                        ReminderWidgetController.updateWidgets(context)
                        widgetPinMessage = when (ReminderWidgetController.requestSingleWidget(context)) {
                            ReminderWidgetController.WidgetPinResult.PinRequested ->
                                "Widget-Anfrage wurde an den Launcher gesendet."
                            ReminderWidgetController.WidgetPinResult.UpdatedExisting ->
                                "Vorhandenes Widget wurde aktualisiert."
                            ReminderWidgetController.WidgetPinResult.NotSupported ->
                                "Dieser Launcher unterstützt das direkte Hinzufügen nicht."
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Widget hinzufügen")
                }
            }

            widgetPinMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        SettingsCard(title = "Zielmodus") {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Anzeige im ersten Tab")
                DropdownMenuButton(
                    selected = goalModeGerman,
                    options = listOf("Täglich", "Wöchentlich"),
                    onSelect = { german ->
                        val mode = when (german) {
                            "Täglich" -> "daily"
                            "Wöchentlich" -> "weekly"
                            else -> "daily"
                        }
                        scope.launch { goalDataStore.updateGoalMode(mode) }
                    }
                )
            }
        }

        SettingsCard(title = "Belohnungsmodus") {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Kosten berechnen")
                DropdownMenuButton(
                    selected = costModeGerman,
                    options = listOf("Zufällig", "Linear", "Quadratisch", "Exponentiell", "Logarithmisch", "Konstant"),
                    onSelect = { german ->
                        val mode = when (german) {
                            "Zufällig" -> "random"
                            "Linear" -> "linear"
                            "Quadratisch" -> "quadratic"
                            "Exponentiell" -> "exponential"
                            "Logarithmisch" -> "logarithmic"
                            "Konstant" -> "constant"
                            else -> "random"
                        }
                        scope.launch { goalDataStore.updateCostMode(mode) }
                    }
                )
            }

            if (costMode == "constant") {
                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = fixedCostInput,
                    onValueChange = { fixedCostInput = it },
                    label = { Text("Fester Belohnungswert") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(8.dp))

                Button(
                    onClick = {
                        val value = fixedCostInput.toIntOrNull() ?: 50
                        scope.launch { goalDataStore.updateFixedCost(value) }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Wert speichern")
                }
            }
        }

        SettingsCard(title = "Alle Daten löschen") {
            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Alle Daten löschen")
            }
        }
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Alle Daten löschen") },
            text = { Text("Möchtest du alle Ziele, Belohnungen, Punkte, Freischaltungen und Alben löschen? Deine Einstellungen bleiben erhalten.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showResetDialog = false
                        scope.launch {
                            goalDataStore.clearDailyGoals()
                            goalDataStore.clearWeeklyGoals()
                            goalDataStore.clearUserRewards()
                            goalDataStore.clearUnlockedRewards()
                            goalDataStore.updateRemindersEnabled(false)
                            goalDataStore.updateSelectedWidgetRewardId(null)
                            goalDataStore.setPoints(0)
                            albumDataStore.clearAlbums()
                            ReminderWidgetController.setEnabled(context, false)
                            ReminderWidgetController.updateWidgets(context)
                        }
                    }
                ) {
                    Text("Daten löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun SettingsCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.tertiaryContainer,
            contentColor = MaterialTheme.colorScheme.onTertiaryContainer
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
            content()
        }
    }
}

@Composable
private fun ResetTimeRow(
    title: String,
    subtitle: String,
    time: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Button(onClick = onClick) {
            Text(time)
        }
    }
}

@Composable
private fun WeeklyResetDayRow(
    selectedDay: String,
    onSelect: (String) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text("Reset-Tag wöchentlich", style = MaterialTheme.typography.bodyLarge)
            Text(
                text = "Wählt den Tag für den Wochenreset.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        DropdownMenuButton(
            selected = selectedDay,
            options = listOf("Montag", "Dienstag", "Mittwoch", "Donnerstag", "Freitag", "Samstag", "Sonntag"),
            onSelect = onSelect
        )
    }
}
@Composable
fun DropdownMenuButton(
    selected: String,
    options: List<String>,
    onSelect: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Button(onClick = { expanded = true }) {
            Text(selected)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option) },
                    onClick = {
                        onSelect(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

private fun showTimePicker(context: Context, currentTime: String, onTimeSelected: (String) -> Unit) {
    val (hour, minute) = parseTime(currentTime)

    TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            onTimeSelected("%02d:%02d".format(selectedHour, selectedMinute))
        },
        hour,
        minute,
        true
    ).show()
}

private fun parseTime(time: String): Pair<Int, Int> {
    return runCatching {
        val parts = time.split(":")
        parts[0].toInt() to parts[1].toInt()
    }.getOrDefault(4 to 0)
}




private fun weeklyResetDayToGerman(day: String): String {
    return when (day) {
        "MONDAY" -> "Montag"
        "TUESDAY" -> "Dienstag"
        "WEDNESDAY" -> "Mittwoch"
        "THURSDAY" -> "Donnerstag"
        "FRIDAY" -> "Freitag"
        "SATURDAY" -> "Samstag"
        "SUNDAY" -> "Sonntag"
        else -> "Montag"
    }
}

private fun germanWeekdayToValue(day: String): String {
    return when (day) {
        "Montag" -> "MONDAY"
        "Dienstag" -> "TUESDAY"
        "Mittwoch" -> "WEDNESDAY"
        "Donnerstag" -> "THURSDAY"
        "Freitag" -> "FRIDAY"
        "Samstag" -> "SATURDAY"
        "Sonntag" -> "SUNDAY"
        else -> "MONDAY"
    }
}



