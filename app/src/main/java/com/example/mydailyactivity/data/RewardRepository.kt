package com.example.mydailyactivity.data

import com.example.mydailyactivity.R


object RewardRepository {

    val rewards = listOf(
        Reward(
            id = 1,
            imageRes = R.drawable.reward_sunrise,
            quote = "Gib niemals auf!",
            cost = 50
        ),
        Reward(
            id = 2,
            imageRes = R.drawable.reward_mountain,
            quote = "Du schaffst das!",
            cost = 75
        ),
        Reward(
            id = 3,
            imageRes = R.drawable.reward_forest,
            quote = "Jeder Tag ist eine Chance.",
            cost = 100
        )
    )
}