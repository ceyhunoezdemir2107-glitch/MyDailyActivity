package com.example.mydailyactivity.data

import kotlinx.serialization.Serializable

@Serializable
data class Album(
    val id: Int,
    val name: String,
    val rewardIds: List<Int> = emptyList()
)
