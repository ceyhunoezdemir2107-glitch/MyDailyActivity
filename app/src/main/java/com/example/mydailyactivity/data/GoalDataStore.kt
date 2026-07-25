package com.example.mydailyactivity.data

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

// DataStore-Instanz
//val Context.goalDataStore by preferencesDataStore(name = "goals")

class GoalDataStore(private val context: Context) {


    // Ziele
    private val GOALS_KEY = stringSetPreferencesKey("goals_list")

    // Ziele als Flow lesen
    val goals: Flow<Set<String>> = context.goalDataStore.data.map { preferences ->
        preferences[GOALS_KEY] ?: emptySet()
    }
    // Erledigte Ziele
    private val COMPLETED_KEY = stringSetPreferencesKey("completed_goals")

    val completedGoalsFlow: Flow<Set<String>> = context.goalDataStore.data
        .map { prefs -> prefs[COMPLETED_KEY] ?: emptySet() }

    //Gesamtpunkte
    private val POINTS_KEY = intPreferencesKey("points")

    val pointsFlow: Flow<Int> = context.goalDataStore.data
        .map { prefs -> prefs[POINTS_KEY] ?: 0 }

    // Punkte pro Ziel
    private val GOAL_POINTS_KEY = stringSetPreferencesKey("goal_points")

    val goalPointsFlow: Flow<Map<String, Int>> = context.goalDataStore.data
        .map { prefs ->
            prefs[GOAL_POINTS_KEY]
                ?.mapNotNull {
                    val parts = it.split(":")
                    if (parts.size == 2) parts[0] to parts[1].toInt() else null
                }
                ?.toMap()
                ?: emptyMap()
        }

    private val WEEKLY_GOALS_KEY = stringSetPreferencesKey("weekly_goals_list")

    val weeklyGoals: Flow<Set<String>> = context.goalDataStore.data.map { preferences ->
        preferences[WEEKLY_GOALS_KEY] ?: emptySet()
    }

    private val WEEKLY_COMPLETED_KEY = stringSetPreferencesKey("weekly_completed_goals")

    val weeklyCompletedGoalsFlow: Flow<Set<String>> = context.goalDataStore.data
        .map { prefs -> prefs[WEEKLY_COMPLETED_KEY] ?: emptySet() }

    private val WEEKLY_GOAL_POINTS_KEY = stringSetPreferencesKey("weekly_goal_points")

    val weeklyGoalPointsFlow: Flow<Map<String, Int>> = context.goalDataStore.data
        .map { prefs ->
            prefs[WEEKLY_GOAL_POINTS_KEY]
                ?.mapNotNull {
                    val parts = it.split(":")
                    if (parts.size == 2) parts[0] to parts[1].toInt() else null
                }
                ?.toMap()
                ?: emptyMap()
        }

    private val UNLOCKED_REWARDS_KEY = stringPreferencesKey("unlocked_rewards")

    val unlockedRewardsFlow = context.goalDataStore.data.map { prefs ->
        prefs[UNLOCKED_REWARDS_KEY]
            ?.split(",")
            ?.mapNotNull { it.toIntOrNull() }
            ?: emptyList()
    }

    private val USER_REWARDS_KEY = stringPreferencesKey("user_rewards")

    val userRewardsFlow: Flow<List<Reward>> =
        context.goalDataStore.data.map { prefs ->
            val json = prefs[USER_REWARDS_KEY] ?: "[]"

            // Typ explizit angeben.
            Json.decodeFromString<List<Reward>>(json)
        }

    // Reset-Zeitpunkt speichern
    private val RESET_TIME = stringPreferencesKey("reset_time")

    val resetTimeFlow = context.goalDataStore.data.map { prefs ->
        prefs[RESET_TIME] ?: "04:00"
    }

    suspend fun updateResetTime(time: String) {
        context.goalDataStore.edit { prefs ->
            prefs[RESET_TIME] = time
        }
    }

    private val WEEKLY_RESET_TIME = stringPreferencesKey("weekly_reset_time")

    val weeklyResetTimeFlow = context.goalDataStore.data.map { prefs ->
        prefs[WEEKLY_RESET_TIME] ?: "04:00"
    }

    suspend fun updateWeeklyResetTime(time: String) {
        context.goalDataStore.edit { prefs ->
            prefs[WEEKLY_RESET_TIME] = time
        }
    }

    private val WEEKLY_RESET_DAY = stringPreferencesKey("weekly_reset_day")

    val weeklyResetDayFlow = context.goalDataStore.data.map { prefs ->
        prefs[WEEKLY_RESET_DAY] ?: "MONDAY"
    }

    suspend fun updateWeeklyResetDay(day: String) {
        context.goalDataStore.edit { prefs ->
            prefs[WEEKLY_RESET_DAY] = day
        }
    }

    private val LAST_DAILY_RESET_PERIOD = stringPreferencesKey("last_daily_reset_period")
    private val LAST_WEEKLY_RESET_PERIOD = stringPreferencesKey("last_weekly_reset_period")

    private val REMINDERS_ENABLED_KEY = booleanPreferencesKey("reminders_enabled")
    private val SELECTED_WIDGET_REWARD_ID_KEY = intPreferencesKey("selected_widget_reward_id")
    private val GOAL_NOTIFICATION_ENABLED_KEY = booleanPreferencesKey("goal_notification_enabled")
    private val GOAL_NOTIFICATION_TIME_KEY = stringPreferencesKey("goal_notification_time")

    val remindersEnabledFlow = context.goalDataStore.data.map { prefs ->
        prefs[REMINDERS_ENABLED_KEY] ?: false
    }

    val selectedWidgetRewardIdFlow: Flow<Int?> = context.goalDataStore.data.map { prefs ->
        prefs[SELECTED_WIDGET_REWARD_ID_KEY]
    }

    val goalNotificationEnabledFlow = context.goalDataStore.data.map { prefs ->
        prefs[GOAL_NOTIFICATION_ENABLED_KEY] ?: false
    }

    val goalNotificationTimeFlow = context.goalDataStore.data.map { prefs ->
        prefs[GOAL_NOTIFICATION_TIME_KEY] ?: "20:00"
    }

    suspend fun updateRemindersEnabled(enabled: Boolean) {
        context.goalDataStore.edit { prefs ->
            prefs[REMINDERS_ENABLED_KEY] = enabled
        }
    }

    suspend fun updateSelectedWidgetRewardId(rewardId: Int?) {
        context.goalDataStore.edit { prefs ->
            if (rewardId == null) {
                prefs.remove(SELECTED_WIDGET_REWARD_ID_KEY)
            } else {
                prefs[SELECTED_WIDGET_REWARD_ID_KEY] = rewardId
            }
        }
    }

    suspend fun updateGoalNotificationEnabled(enabled: Boolean) {
        context.goalDataStore.edit { prefs ->
            prefs[GOAL_NOTIFICATION_ENABLED_KEY] = enabled
        }
    }

    suspend fun updateGoalNotificationTime(time: String) {
        context.goalDataStore.edit { prefs ->
            prefs[GOAL_NOTIFICATION_TIME_KEY] = time
        }
    }

    // Falls Konstant bei Belohnungen eingestellt, fixe Kosten setzen
    private val FIXED_COST_KEY = intPreferencesKey("fixed_cost")

    val fixedCostFlow = context.goalDataStore.data.map { prefs ->
        prefs[FIXED_COST_KEY] ?: 50   // Standardwert
    }

    suspend fun updateFixedCost(value: Int) {
        context.goalDataStore.edit { prefs ->
            prefs[FIXED_COST_KEY] = value
        }
    }

    companion object {
        val RESET_TIME = stringPreferencesKey("reset_time")
    }

    suspend fun resetDailyGoals() {
        context.goalDataStore.edit { prefs ->
            //prefs[GOALS_KEY] = emptySet()
            prefs[COMPLETED_KEY] = emptySet()
        }
    }

    suspend fun resetWeeklyGoals() {
        context.goalDataStore.edit { prefs ->
            prefs[WEEKLY_COMPLETED_KEY] = emptySet()
        }
    }

    suspend fun getLastDailyResetPeriod(): String? {
        return context.goalDataStore.data.first()[LAST_DAILY_RESET_PERIOD]
    }

    suspend fun getLastWeeklyResetPeriod(): String? {
        return context.goalDataStore.data.first()[LAST_WEEKLY_RESET_PERIOD]
    }

    suspend fun markDailyResetPeriod(period: String) {
        context.goalDataStore.edit { prefs ->
            prefs[LAST_DAILY_RESET_PERIOD] = period
        }
    }

    suspend fun markWeeklyResetPeriod(period: String) {
        context.goalDataStore.edit { prefs ->
            prefs[LAST_WEEKLY_RESET_PERIOD] = period
        }
    }

    suspend fun resetDailyGoalsForPeriod(period: String): Boolean {
        var changed = false
        context.goalDataStore.edit { prefs ->
            if (prefs[LAST_DAILY_RESET_PERIOD] != period) {
                prefs[COMPLETED_KEY] = emptySet()
                prefs[LAST_DAILY_RESET_PERIOD] = period
                changed = true
            }
        }
        return changed
    }

    suspend fun resetWeeklyGoalsForPeriod(period: String): Boolean {
        var changed = false
        context.goalDataStore.edit { prefs ->
            if (prefs[LAST_WEEKLY_RESET_PERIOD] != period) {
                prefs[WEEKLY_COMPLETED_KEY] = emptySet()
                prefs[LAST_WEEKLY_RESET_PERIOD] = period
                changed = true
            }
        }
        return changed
    }


    // Zielmodus anzeigen bei den Zielen
    private val GOAL_MODE = stringPreferencesKey("goal_mode")

    val goalModeFlow: Flow<String> = context.goalDataStore.data.map { prefs ->
        prefs[GOAL_MODE] ?: "daily"   // Standard: täglich
    }

    suspend fun updateGoalMode(mode: String) {
        context.goalDataStore.edit { prefs ->
            prefs[GOAL_MODE] = mode
        }
    }

     // Kostenmodus
    private val COST_MODE = stringPreferencesKey("cost_mode")

    val costModeFlow = context.goalDataStore.data.map { prefs ->
        prefs[COST_MODE] ?: "random"
    }

    suspend fun updateCostMode(mode: String) {
        context.goalDataStore.edit { prefs ->
            prefs[COST_MODE] = mode
        }
    }

    // Ziel hinzufügen
    suspend fun addGoal(goal: String) {
        context.goalDataStore.edit { preferences ->
            val current = preferences[GOALS_KEY] ?: emptySet()
            preferences[GOALS_KEY] = current + goal
        }
    }

    // Ziel entfernen (optional)
    suspend fun removeGoal(goal: String) {
        context.goalDataStore.edit { preferences ->
            val current = preferences[GOALS_KEY] ?: emptySet()
            preferences[GOALS_KEY] = current - goal
        }
    }
    // Alle Ziele auf einmal löschen
    suspend fun clear() {
        context.goalDataStore.edit { it.clear() }
    }

    suspend fun clearDailyGoals() {
        context.goalDataStore.edit { prefs ->
            prefs[GOALS_KEY] = emptySet()
            prefs[COMPLETED_KEY] = emptySet()
            prefs[GOAL_POINTS_KEY] = emptySet()
        }
    }
    // Ziele abhaken
    suspend fun setGoalCompleted(goal: String, completed: Boolean) {
        context.goalDataStore.edit { prefs ->
            val current = prefs[COMPLETED_KEY] ?: emptySet()
            prefs[COMPLETED_KEY] =
                if (completed) current + goal else current - goal
        }
    }

    // Bei erfolgreichem Abhaken Punkte hinzufügen
    suspend fun addPoints(amount: Int) {
        context.goalDataStore.edit { prefs ->
            val current = prefs[POINTS_KEY] ?: 0
            prefs[POINTS_KEY] = current + amount
        }
    }
    // Beim Entfernen des Hakens Punkte abziehen
    suspend fun removePoints(amount: Int) {
        context.goalDataStore.edit { prefs ->
            val current = prefs[POINTS_KEY] ?: 0
            prefs[POINTS_KEY] = (current - amount).coerceAtLeast(0)
        }
    }
    // Zum Setzen der Punkte beim Erstellen eines Ziels
    suspend fun setGoalPoints(goal: String, points: Int) {
        context.goalDataStore.edit { prefs ->
            val current = prefs[GOAL_POINTS_KEY] ?: emptySet()
            val filtered = current.filterNot { it.startsWith("$goal:") }.toSet()
            prefs[GOAL_POINTS_KEY] = filtered + "$goal:$points"
        }
    }

    suspend fun addWeeklyGoal(goal: String) {
        context.goalDataStore.edit { preferences ->
            val current = preferences[WEEKLY_GOALS_KEY] ?: emptySet()
            preferences[WEEKLY_GOALS_KEY] = current + goal
        }
    }

    suspend fun removeWeeklyGoal(goal: String) {
        context.goalDataStore.edit { preferences ->
            val current = preferences[WEEKLY_GOALS_KEY] ?: emptySet()
            preferences[WEEKLY_GOALS_KEY] = current - goal

            val completed = preferences[WEEKLY_COMPLETED_KEY] ?: emptySet()
            preferences[WEEKLY_COMPLETED_KEY] = completed - goal

            val points = preferences[WEEKLY_GOAL_POINTS_KEY] ?: emptySet()
            preferences[WEEKLY_GOAL_POINTS_KEY] = points.filterNot { it.startsWith("$goal:") }.toSet()
        }
    }

    suspend fun clearWeeklyGoals() {
        context.goalDataStore.edit { prefs ->
            prefs[WEEKLY_GOALS_KEY] = emptySet()
            prefs[WEEKLY_COMPLETED_KEY] = emptySet()
            prefs[WEEKLY_GOAL_POINTS_KEY] = emptySet()
        }
    }

    suspend fun setWeeklyGoalCompleted(goal: String, completed: Boolean) {
        context.goalDataStore.edit { prefs ->
            val current = prefs[WEEKLY_COMPLETED_KEY] ?: emptySet()
            prefs[WEEKLY_COMPLETED_KEY] =
                if (completed) current + goal else current - goal
        }
    }

    suspend fun setWeeklyGoalPoints(goal: String, points: Int) {
        context.goalDataStore.edit { prefs ->
            val current = prefs[WEEKLY_GOAL_POINTS_KEY] ?: emptySet()
            val filtered = current.filterNot { it.startsWith("$goal:") }.toSet()
            prefs[WEEKLY_GOAL_POINTS_KEY] = filtered + "$goal:$points"
        }
    }

    // Zum Freischalten eines Rewards
    suspend fun unlockReward(id: Int) {
        context.goalDataStore.edit { prefs ->
            val current = prefs[UNLOCKED_REWARDS_KEY]
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                ?: emptyList()

            val updated = (current + id).distinct()

            prefs[UNLOCKED_REWARDS_KEY] = updated.joinToString(",")
        }
    }

    // Zum Hinzufügen eigener Bilder als Belohnung
    suspend fun addUserReward(reward: Reward) {
        context.goalDataStore.edit { prefs ->
            val currentJson = prefs[USER_REWARDS_KEY] ?: "[]"

            // Typ explizit angeben.
            val currentList = Json.decodeFromString<List<Reward>>(currentJson)

            val updatedList = currentList + reward

            prefs[USER_REWARDS_KEY] = Json.encodeToString(updatedList)
        }
    }

    suspend fun deleteReward(rewardId: Int) {
        context.goalDataStore.edit { prefs ->

            // 1. Rewards aktualisieren
            val currentJson = prefs[USER_REWARDS_KEY] ?: "[]"
            val currentList = Json.decodeFromString<List<Reward>>(currentJson)
            val updatedList = currentList.filter { it.id != rewardId }
            prefs[USER_REWARDS_KEY] = Json.encodeToString(updatedList)

            // 2. Unlocks aktualisieren
            val unlocked = prefs[UNLOCKED_REWARDS_KEY]
                ?.split(",")
                ?.mapNotNull { it.toIntOrNull() }
                ?: emptyList()

            val updatedUnlocked = unlocked.filter { it != rewardId }
            prefs[UNLOCKED_REWARDS_KEY] = updatedUnlocked.joinToString(",")
        }
    }

    // Zum Löschen von allen Rewards
    suspend fun clearUserRewards() {
        context.goalDataStore.edit { prefs ->
            prefs[USER_REWARDS_KEY] = "[]"
        }
    }

    // Zum Setzen der Punkte beim Erstellen eines Ziels
    suspend fun setPoints(value: Int) {
        context.goalDataStore.edit { prefs ->
            prefs[POINTS_KEY] = value
        }
    }

    suspend fun clearUnlockedRewards() {
        context.goalDataStore.edit { prefs ->
            prefs[UNLOCKED_REWARDS_KEY] = ""
        }
    }
}






