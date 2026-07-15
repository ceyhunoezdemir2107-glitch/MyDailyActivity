package com.example.mydailyactivity.data

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.goalDataStore by preferencesDataStore(name = "goals")
class GoalDataStoreModule {
}