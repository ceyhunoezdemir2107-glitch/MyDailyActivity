package com.example.mydailyactivity.userinterface

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.mydailyactivity.data.AlbumDataStore
import com.example.mydailyactivity.data.GoalDataStore
import com.example.mydailyactivity.data.Reward
import kotlinx.coroutines.launch

@Composable
fun AlbumDetailScreen(
    albumId: Int,
    albumDataStore: AlbumDataStore,
    goalDataStore: GoalDataStore,
    navController: NavController
) {
    val albums by albumDataStore.albumsFlow.collectAsState(initial = emptyList())
    val rewards by goalDataStore.userRewardsFlow.collectAsState(initial = emptyList())
    val scope = rememberCoroutineScope()

    val album = albums.firstOrNull { it.id == albumId }
    val albumRewards = album?.rewardIds
        ?.mapNotNull { rewardId -> rewards.firstOrNull { it.id == rewardId } }
        ?: emptyList()

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .backSwipe { navController.popBackStack() },
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = album?.name ?: "Album",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "${albumRewards.size} gespeicherte Belohnungen",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(onClick = { navController.popBackStack() }) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Zurück")
                }
            }
        }

        if (album == null) {
            item { AlbumDetailEmptyState("Album nicht gefunden", "Dieses Album existiert nicht mehr.") }
        } else if (albumRewards.isEmpty()) {
            item {
                AlbumDetailEmptyState(
                    title = "Noch keine Belohnungen",
                    text = "Füge freigeschaltete Belohnungen aus der Belohnungsansicht zu diesem Album hinzu."
                )
            }
        } else {
            items(albumRewards, key = { it.id }) { reward ->
                AlbumRewardCard(
                    reward = reward,
                    onRemove = {
                        scope.launch { albumDataStore.removeRewardFromAlbum(albumId, reward.id) }
                    }
                )
            }
        }
    }
}

@Composable
private fun AlbumRewardCard(
    reward: Reward,
    onRemove: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            RewardImageView(reward, isUnlocked = true)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = reward.quote,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f)
                )

                IconButton(onClick = onRemove) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Aus Album entfernen",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun AlbumDetailEmptyState(title: String, text: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(
                text = text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Modifier.backSwipe(onBack: () -> Unit): Modifier = pointerInput(Unit) {
    var triggered = false
    detectHorizontalDragGestures(
        onHorizontalDrag = { _, dragAmount ->
            if (!triggered && dragAmount > 30) {
                triggered = true
                onBack()
            }
        },
        onDragEnd = { triggered = false }
    )
}
