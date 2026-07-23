package com.example.mydailyactivity.userinterface

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mydailyactivity.data.AlbumDataStore
import com.example.mydailyactivity.data.GoalDataStore
import com.example.mydailyactivity.data.MotivationRepository
import com.example.mydailyactivity.data.Reward
import com.example.mydailyactivity.management.ReminderWidgetController
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.ln
import kotlin.random.Random

@Composable
fun RewardScreen(
    goalDataStore: GoalDataStore,
    albumDataStore: AlbumDataStore,
    navController: NavController
) {
    val points by goalDataStore.pointsFlow.collectAsState(initial = 0)
    val unlockedRewards by goalDataStore.unlockedRewardsFlow.collectAsState(initial = emptyList())
    val userRewards by goalDataStore.userRewardsFlow.collectAsState(initial = emptyList())

    val totalRewards = userRewards.size
    val unlockedCount = userRewards.count { reward -> unlockedRewards.contains(reward.id) }
    val lockedCount = totalRewards - unlockedCount

    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    val randomQuote = MotivationRepository.quotes.random()

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            try {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            } catch (e: Exception) {
                e.printStackTrace()
            }

            scope.launch {
                val costMode = goalDataStore.costModeFlow.first()
                val fixedCost = goalDataStore.fixedCostFlow.first()
                val cost = calculateCost(userRewards.size, costMode, fixedCost)

                val reward = Reward(
                    id = Random.nextInt(),
                    quote = randomQuote,
                    imageUri = uri.toString(),
                    cost = cost
                )

                goalDataStore.addUserReward(reward)
                ReminderWidgetController.updateWidgets(context)
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Belohnungen",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Sammle Bilder, die du dir mit erledigten Zielen freischaltest.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            RewardSummaryCard(
                points = points,
                totalRewards = totalRewards,
                unlockedCount = unlockedCount,
                lockedCount = lockedCount
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Button(
                    onClick = { launcher.launch(arrayOf("image/*")) },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Belohnungsbild hinzufügen")
                }

                FilledTonalButton(
                    onClick = { navController.navigate("albums") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Alben anzeigen")
                }
            }
        }

        item {
            Text(
                text = "Deine Belohnungen",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (userRewards.isEmpty()) {
            item {
                RewardEmptyState()
            }
        } else {
            items(userRewards, key = { it.id }) { reward ->
                val missingPoints = (reward.cost - points).coerceAtLeast(0)
                RewardCard(
                    reward = reward,
                    isUnlocked = unlockedRewards.contains(reward.id),
                    canBuy = missingPoints == 0,
                    missingPoints = missingPoints,
                    onBuy = {
                        if (points >= reward.cost) {
                            scope.launch {
                                goalDataStore.removePoints(reward.cost)
                                goalDataStore.unlockReward(reward.id)
                                ReminderWidgetController.updateWidgets(context)
                            }
                        }
                    },
                    onAddToAlbum = {
                        navController.navigate("albumSelect/${reward.id}")
                    },
                    onDelete = {
                        scope.launch {
                            goalDataStore.deleteReward(reward.id)
                            albumDataStore.removeRewardFromAllAlbums(reward.id)
                            ReminderWidgetController.updateWidgets(context)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun RewardSummaryCard(
    points: Int,
    totalRewards: Int,
    unlockedCount: Int,
    lockedCount: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "$points Punkte verfügbar",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                RewardMetric("Gesamt", totalRewards.toString(), Modifier.weight(1f))
                RewardMetric("Frei", unlockedCount.toString(), Modifier.weight(1f))
                RewardMetric("Offen", lockedCount.toString(), Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun RewardMetric(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.78f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun RewardEmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Noch keine Belohnungen", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Füge ein Bild hinzu, damit deine Punkte ein sichtbares Ziel bekommen.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

fun calculateCost(n: Int, mode: String, fixedCost: Int = 50): Int {
    return when (mode) {
        "random" -> Random.nextInt(1, 1000)
        "linear" -> 50 + 20 * n
        "quadratic" -> 10 * n * n
        "exponential" -> (20 * Math.pow(1.5, n.toDouble())).toInt()
        "logarithmic" -> (30 * ln(n + 1.0)).toInt()
        "constant" -> fixedCost
        else -> 50
    }
}
