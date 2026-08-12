package com.example.mydailyactivity.data

import kotlinx.serialization.Serializable

@Serializable
data class DailyStat(
    val date: String,
    val completedGoals: Int,
    val points: Int
)
