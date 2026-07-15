package com.example.mydailyactivity.data

import com.example.mydailyactivity.R
import kotlinx.serialization.Serializable
@Serializable
data class Reward(
    val id: Int,
    val quote: String,  //Spruch
    val imageRes: Int? = null,      // für Bilder in res/drawable,
    val imageUri: String? = null,     // für User-Bilder
    val cost: Int       // Preis in Punkten
)

sealed class RewardImage {
    data class DrawableResource(val resId: Int) : RewardImage()
    data class UserImage(val uri: String) : RewardImage()
}
