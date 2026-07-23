package com.example.mydailyactivity.userinterface

import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.mydailyactivity.data.GoalDataStore
import com.example.mydailyactivity.data.Reward
import kotlinx.coroutines.launch
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.util.Locale

@Composable
fun WeeklyScreen(goalDataStore: GoalDataStore) {
    val weeklyGoals by goalDataStore.weeklyGoals.collectAsState(initial = emptySet())
    val completedGoals by goalDataStore.weeklyCompletedGoalsFlow.collectAsState(initial = emptySet())
    val points by goalDataStore.pointsFlow.collectAsState(initial = 0)
    val weeklyGoalPoints by goalDataStore.weeklyGoalPointsFlow.collectAsState(initial = emptyMap())
    val unlockedIds by goalDataStore.unlockedRewardsFlow.collectAsState(initial = emptyList())
    val userRewards by goalDataStore.userRewardsFlow.collectAsState(initial = emptyList())

    var newGoal by remember { mutableStateOf("") }
    var pendingGoal by remember { mutableStateOf("") }
    var pendingPoints by remember { mutableStateOf("50") }
    var showPointsDialog by remember { mutableStateOf(false) }
    var showDeleteAllDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()
    val completedCount = weeklyGoals.count { completedGoals.contains(it) }
    val weekRange = remember { currentWeekRange() }
    val unlockedRewardObjects = userRewards.filter { unlockedIds.contains(it.id) }
    val randomReward = remember(unlockedRewardObjects) { unlockedRewardObjects.randomOrNull() }

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
                    text = "Diese Woche",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = weekRange,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            WeeklyStatusCard(
                points = points,
                completedCount = completedCount,
                totalCount = weeklyGoals.size
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
                    Text("Wochenziel hinzufügen", style = MaterialTheme.typography.titleMedium)

                    OutlinedTextField(
                        value = newGoal,
                        onValueChange = { newGoal = it },
                        label = { Text("Was soll diese Woche passieren?") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = { showDeleteAllDialog = true },
                            enabled = weeklyGoals.isNotEmpty(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Leeren")
                        }

                        Button(
                            onClick = {
                                if (newGoal.isNotBlank()) {
                                    pendingGoal = newGoal.trim()
                                    pendingPoints = "50"
                                    newGoal = ""
                                    showPointsDialog = true
                                }
                            },
                            enabled = newGoal.isNotBlank(),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Hinzufügen")
                        }
                    }
                }
            }
        }

        randomReward?.let { reward ->
            item {
                WeeklyMotivationCard(reward)
            }
        }

        item {
            Text(
                text = "Wochenziele",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        if (weeklyGoals.isEmpty()) {
            item {
                WeeklyEmptyState()
            }
        } else {
            items(weeklyGoals.toList(), key = { it }) { goal ->
                WeeklyGoalRow(
                    goal = goal,
                    isChecked = completedGoals.contains(goal),
                    points = weeklyGoalPoints[goal] ?: 50,
                    onCheckedChange = { checked ->
                        val pointsForGoal = weeklyGoalPoints[goal] ?: 50
                        scope.launch {
                            goalDataStore.setWeeklyGoalCompleted(goal, checked)
                            if (checked) goalDataStore.addPoints(pointsForGoal)
                            else goalDataStore.removePoints(pointsForGoal)
                        }
                    },
                    onDelete = {
                        scope.launch { goalDataStore.removeWeeklyGoal(goal) }
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
                    label = { Text("Punkte für dieses Wochenziel") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val pointsForGoal = pendingPoints.toIntOrNull() ?: 50
                    scope.launch {
                        goalDataStore.addWeeklyGoal(pendingGoal)
                        goalDataStore.setWeeklyGoalPoints(pendingGoal, pointsForGoal)
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
            title = { Text("Wochenziele löschen") },
            text = { Text("Möchtest du alle Wochenziele und deren Haken löschen? Deine Punkte und Einstellungen bleiben erhalten.") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteAllDialog = false
                    scope.launch { goalDataStore.clearWeeklyGoals() }
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
private fun WeeklyStatusCard(points: Int, completedCount: Int, totalCount: Int) {
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
                    text = "$completedCount von $totalCount Wochenzielen erledigt",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }

            Text(
                text = if (totalCount == 0) "Plan" else "$completedCount/$totalCount",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}

@Composable
private fun WeeklyMotivationCard(reward: Reward) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            WeeklyRewardPreviewImage(reward)

            Text(
                text = reward.quote,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )
        }
    }
}

@Composable
private fun WeeklyGoalRow(
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
                    contentDescription = "Wochenziel löschen",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}

@Composable
private fun WeeklyEmptyState() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(14.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text("Noch keine Wochenziele", style = MaterialTheme.typography.titleMedium)
            Text(
                text = "Plane ein größeres Ziel für diese Woche und passe es an, wenn sich dein Fokus ändert.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun WeeklyRewardPreviewImage(reward: Reward) {
    val modifier = Modifier
        .fillMaxWidth()
        .height(168.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(MaterialTheme.colorScheme.surfaceVariant)

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

private fun currentWeekRange(): String {
    val today = LocalDate.now()
    val start = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
    val end = today.with(TemporalAdjusters.nextOrSame(DayOfWeek.SUNDAY))
    val formatter = DateTimeFormatter.ofPattern("dd. MMMM", Locale.GERMAN)
    return "${start.format(formatter)} bis ${end.format(formatter)}"
}
