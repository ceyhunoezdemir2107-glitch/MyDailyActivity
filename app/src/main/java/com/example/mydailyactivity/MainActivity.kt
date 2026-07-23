package com.example.mydailyactivity

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.Tab
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.mydailyactivity.data.AlbumDataStore
import com.example.mydailyactivity.data.GoalDataStore
import com.example.mydailyactivity.management.ReminderWidgetController
import com.example.mydailyactivity.management.ResetScheduler
import com.example.mydailyactivity.ui.theme.MyDailyActivityTheme
import com.example.mydailyactivity.userinterface.AlbumDetailScreen
import com.example.mydailyactivity.userinterface.AlbumScreen
import com.example.mydailyactivity.userinterface.AlbumSelectScreen
import com.example.mydailyactivity.userinterface.DailyScreen
import com.example.mydailyactivity.userinterface.RewardScreen
import com.example.mydailyactivity.userinterface.SettingsScreen
import com.example.mydailyactivity.userinterface.WeeklyScreen
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val goalDataStore = GoalDataStore(this)
        val albumDataStore = AlbumDataStore(this)

        lifecycleScope.launch {
            goalDataStore.resetTimeFlow.collect { time ->
                ResetScheduler.scheduleDailyReset(this@MainActivity, time)
            }
        }

        lifecycleScope.launch {
            combine(
                goalDataStore.weeklyResetTimeFlow,
                goalDataStore.weeklyResetDayFlow
            ) { time, day -> time to day }
                .collect { (time, day) ->
                    ResetScheduler.scheduleWeeklyReset(this@MainActivity, time, day)
                }
        }

        lifecycleScope.launch {
            combine(
                goalDataStore.remindersEnabledFlow,
                goalDataStore.userRewardsFlow,
                goalDataStore.unlockedRewardsFlow
            ) { remindersEnabled, rewards, unlockedIds ->
                val hasUnlockedPersonalImage = rewards.any { reward ->
                    reward.imageUri != null && unlockedIds.contains(reward.id)
                }
                remindersEnabled to hasUnlockedPersonalImage
            }.collect { (remindersEnabled, hasUnlockedPersonalImage) ->
                val canEnableReminders = remindersEnabled && hasUnlockedPersonalImage
                ReminderWidgetController.setEnabled(this@MainActivity, canEnableReminders)

                if (remindersEnabled && !hasUnlockedPersonalImage) {
                    goalDataStore.updateRemindersEnabled(false)
                    goalDataStore.updateSelectedWidgetRewardId(null)
                }
            }
        }

        setContent {
            MyDailyActivityTheme {
                val navController = rememberNavController()

                NavHost(
                    navController = navController,
                    startDestination = "mainPager"
                ) {
                    composable("mainPager") {
                        MainPager(goalDataStore, albumDataStore, navController)
                    }
                    composable("albums") {
                        AlbumScreen(goalDataStore, albumDataStore, navController)
                    }
                    composable("albumDetail/{id}") { backStack ->
                        val id = backStack.arguments?.getString("id")!!.toInt()
                        AlbumDetailScreen(id, albumDataStore, goalDataStore, navController)
                    }
                    composable("albumSelect/{rewardId}") { backStack ->
                        val rewardId = backStack.arguments?.getString("rewardId")!!.toInt()
                        AlbumSelectScreen(rewardId, albumDataStore, navController)
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        lifecycleScope.launch {
            ResetScheduler.runDailyResetIfDue(this@MainActivity)
            ResetScheduler.runWeeklyResetIfDue(this@MainActivity)
        }
    }
}

@Preview(showBackground = true)
@Composable
fun DailyScreenPreview() {
    val context = LocalContext.current
    val dummyStore = GoalDataStore(context)
    MyDailyActivityTheme {
        DailyScreen(dummyStore)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MainPager(
    goalDataStore: GoalDataStore,
    albumDataStore: AlbumDataStore,
    navController: NavController
) {
    val goalMode by goalDataStore.goalModeFlow.collectAsState(initial = "daily")
    val pagerState = rememberPagerState(pageCount = { 3 })
    val scope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(WindowInsets.statusBars.asPaddingValues())
    ) {
        PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
            Tab(
                selected = pagerState.currentPage == 0,
                onClick = { scope.launch { pagerState.animateScrollToPage(0) } },
                text = { Text("Ziele") }
            )
            Tab(
                selected = pagerState.currentPage == 1,
                onClick = { scope.launch { pagerState.animateScrollToPage(1) } },
                text = { Text("Belohnungen") }
            )
            Tab(
                selected = pagerState.currentPage == 2,
                onClick = { scope.launch { pagerState.animateScrollToPage(2) } },
                text = { Text("Einstellungen") }
            )
        }

        HorizontalPager(state = pagerState) { page ->
            when (page) {
                0 -> when (goalMode) {
                    "daily" -> DailyScreen(goalDataStore)
                    "weekly" -> WeeklyScreen(goalDataStore)
                }
                1 -> RewardScreen(goalDataStore, albumDataStore, navController)
                2 -> SettingsScreen(goalDataStore, albumDataStore)
            }
        }
    }
}

