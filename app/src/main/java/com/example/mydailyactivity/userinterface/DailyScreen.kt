package com.example.mydailyactivity.userinterface

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mydailyactivity.data.GoalDataStore
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun DailyScreen(goalDataStore: GoalDataStore) {
    val goals by goalDataStore.goals.collectAsState(initial = emptySet())
    var newGoal by remember { mutableStateOf("") }
    val completedGoals by goalDataStore.completedGoalsFlow.collectAsState(initial = emptySet())
    val points by goalDataStore.pointsFlow.collectAsState(initial = 0)
    val goalPointsMap by goalDataStore.goalPointsFlow.collectAsState(initial = emptyMap())
    var showPointsDialog by remember { mutableStateOf(false) }
    var pendingGoal by remember { mutableStateOf("") }
    var pendingPoints by remember { mutableStateOf("10") }
    var showDeleteAllDialog by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val unlockedIds by goalDataStore.unlockedRewardsFlow.collectAsState(initial = emptyList())
    val userRewards by goalDataStore.userRewardsFlow.collectAsState(initial = emptyList())
    val unlockedRewardObjects = userRewards.filter { unlockedIds.contains(it.id) }
    val randomReward = remember(unlockedRewardObjects) { unlockedRewardObjects.randomOrNull() }

    val completedCount = goals.count { completedGoals.contains(it) }
    val dateText = remember {
        LocalDate.now().format(DateTimeFormatter.ofPattern("EEEE, dd. MMMM", Locale.GERMAN))
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
                    text = "Heute",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = dateText.replaceFirstChar { it.titlecase(Locale.GERMAN) },
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            DailyStatusCard(
                points = points,
                completedCount = completedCount,
                totalCount = goals.size
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
                shape = RoundedCornerShape(14.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("Neues Ziel", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = newGoal,
                        onValueChange = { newGoal = it },
                        label = { Text("Was möchtest du schaffen?") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteAllDialog = true },
                            enabled = goals.isNotEmpty(),
                            modifier = Modifier.weight(0.9f),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Leeren")
                        }

                        Button(
                            onClick = {
                                if (newGoal.isNotBlank()) {
                                    pendingGoal = newGoal.trim()
                                    pendingPoints = "10"
                                    newGoal = ""
                                    showPointsDialog = true
                                }
                            },
                            enabled = newGoal.isNotBlank(),
                            modifier = Modifier.weight(1.1f),
                            contentPadding = PaddingValues(horizontal = 10.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(6.dp))
                            Text("Hinzufügen", maxLines = 1, softWrap = false)
                        }
                    }
                }
            }
        }

        randomReward?.let { reward ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                    shape = RoundedCornerShape(14.dp)
                ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    RewardPreviewImage(reward)

                    Text(
                            text = reward.quote,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }

        item {
            Text(
                text = "Tagesziele",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (goals.isEmpty()) {
            item {
                EmptyStateCard(
                    title = "Noch keine Ziele",
                    text = "Lege dein erstes Ziel für heute an und sammle Punkte für deine Belohnungen."
                )
            }
        } else {
            items(goals.toList(), key = { it }) { goal ->
                GoalRow(
                    goal = goal,
                    isChecked = completedGoals.contains(goal),
                    points = goalPointsMap[goal] ?: 10,
                    onCheckedChange = { checked ->
                        scope.launch {
                            goalDataStore.setGoalCompleted(goal, checked)
                            if (checked) goalDataStore.addPoints(goalPointsMap[goal] ?: 10)
                            else goalDataStore.removePoints(goalPointsMap[goal] ?: 10)
                        }
                    },
                    onDelete = {
                        scope.launch { goalDataStore.removeGoal(goal) }
                    }
                )
            }
        }
    }

    if (showPointsDialog) {
        AlertDialog(
            onDismissRequest = { showPointsDialog = false },
            title = { Text("Punkte vergeben") },
            text = {
                OutlinedTextField(
                    value = pendingPoints,
                    onValueChange = { pendingPoints = it },
                    label = { Text("Punkte für dieses Ziel") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val pointsForGoal = pendingPoints.toIntOrNull() ?: 10
                    scope.launch {
                        goalDataStore.addGoal(pendingGoal)
                        goalDataStore.setGoalPoints(pendingGoal, pointsForGoal)
                    }
                    showPointsDialog = false
                }) {
                    Text("Speichern")
                }
            },
            dismissButton = {
                TextButton(onClick = { showPointsDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            title = { Text("Tagesziele löschen") },
            text = { Text("Möchtest du alle Tagesziele und deren Haken löschen? Deine Punkte und Einstellungen bleiben erhalten.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAllDialog = false
                    scope.launch { goalDataStore.clearDailyGoals() }
                }) {
                    Text("Löschen", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text("Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun DailyStatusCard(points: Int, completedCount: Int, totalCount: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "$points Punkte",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Text(
                    text = "$completedCount von $totalCount Zielen erledigt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = if (totalCount == 0) "Start" else "$completedCount/$totalCount",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun GoalRow(
    goal: String,
    isChecked: Boolean,
    points: Int,
    onCheckedChange: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 10.dp, top = 8.dp, end = 6.dp, bottom = 8.dp)
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = onCheckedChange
            )

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = goal,
                    style = MaterialTheme.typography.bodyLarge,
                    color = if (isChecked) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface,
                    textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                )
                Text(
                    text = "$points Punkte",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Ziel löschen",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun EmptyStateCard(title: String, text: String) {
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

@Composable
private fun RewardPreviewImage(reward: com.example.mydailyactivity.data.Reward) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(168.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)

    when {
        reward.imageUri != null -> {
            Box(modifier = modifier) {
                AsyncImage(
                    model = reward.imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .matchParentSize()
                        .blur(18.dp)
                        .alpha(0.42f)
                )
                AsyncImage(
                    model = reward.imageUri,
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.matchParentSize()
                )
            }
        }

        reward.imageRes != null -> {
            Image(
                painter = painterResource(reward.imageRes),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier
            )
        }

        else -> {
            Image(
                painter = painterResource(android.R.drawable.ic_menu_gallery),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = modifier.background(Color.LightGray)
            )
        }
    }
}
