package com.example.mydailyactivity.userinterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation.NavController
import coil.compose.AsyncImage
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
    var selectedRewardIndex by remember { mutableStateOf<Int?>(null) }

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
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Zurück",
                        tint = MaterialTheme.colorScheme.onBackground
                    )
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
            itemsIndexed(albumRewards, key = { _, reward -> reward.id }) { index, reward ->
                AlbumRewardCard(
                    reward = reward,
                    onOpen = { selectedRewardIndex = index },
                    onRemove = {
                        scope.launch { albumDataStore.removeRewardFromAlbum(albumId, reward.id) }
                    }
                )
            }
        }
    }

    val currentIndex = selectedRewardIndex
    if (currentIndex != null && albumRewards.isNotEmpty()) {
        AlbumImageViewer(
            rewards = albumRewards,
            selectedIndex = currentIndex.coerceIn(0, albumRewards.lastIndex),
            onIndexChange = { selectedRewardIndex = it },
            onDismiss = { selectedRewardIndex = null }
        )
    }
}

@Composable
private fun AlbumRewardCard(
    reward: Reward,
    onOpen: () -> Unit,
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
            Box(
                modifier = Modifier.pointerInput(reward.id) {
                    detectTapGestures(
                        onTap = { onOpen() },
                        onDoubleTap = { onOpen() }
                    )
                }
            ) {
                RewardImageView(reward, isUnlocked = true)
            }

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
private fun AlbumImageViewer(
    rewards: List<Reward>,
    selectedIndex: Int,
    onIndexChange: (Int) -> Unit,
    onDismiss: () -> Unit
) {
    val selectedReward = rewards[selectedIndex]

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(selectedIndex, rewards.size) {
                    var dragDistance = 0f

                    detectHorizontalDragGestures(
                        onDragStart = { dragDistance = 0f },
                        onHorizontalDrag = { _, dragAmount ->
                            dragDistance += dragAmount
                        },
                        onDragEnd = {
                            when {
                                dragDistance > 80f -> {
                                    onIndexChange(
                                        if (selectedIndex == 0) rewards.lastIndex else selectedIndex - 1
                                    )
                                }
                                dragDistance < -80f -> {
                                    onIndexChange((selectedIndex + 1) % rewards.size)
                                }
                            }
                        }
                    )
                }
                .pointerInput(selectedIndex) {
                    detectTapGestures(onTap = { onDismiss() })
                }
        ) {
            FullscreenRewardImage(
                reward = selectedReward,
                modifier = Modifier.fillMaxSize()
            )

            Text(
                text = "${selectedIndex + 1} / ${rewards.size}",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(24.dp)
            )
        }
    }
}

@Composable
private fun FullscreenRewardImage(
    reward: Reward,
    modifier: Modifier = Modifier
) {
    when {
        reward.imageUri != null -> {
            AsyncImage(
                model = reward.imageUri,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = modifier
            )
        }
        reward.imageRes != null -> {
            Image(
                painter = painterResource(reward.imageRes),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = modifier
            )
        }
        else -> {
            Box(
                modifier = modifier.background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(android.R.drawable.ic_menu_gallery),
                    contentDescription = null
                )
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
