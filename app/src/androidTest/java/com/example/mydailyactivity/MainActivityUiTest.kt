package com.example.mydailyactivity

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MainActivityUiTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun mainTabs_areVisible() {
        composeRule.onNodeWithText("Ziele").assertExists()
        composeRule.onNodeWithText("Belohnungen").assertExists()
        composeRule.onNodeWithText("Einstellungen").assertExists()
    }

    @Test
    fun dailyTab_showsGoalCreationWorkflow() {
        composeRule.onNodeWithText("Ziele").performClick()

        composeRule.onNodeWithText("Heute").assertExists()
        composeRule.onNodeWithText("Neues Ziel").assertExists()
        composeRule.onNodeWithText("Was möchtest du schaffen?").assertExists()
    }

    @Test
    fun rewardsTab_showsRewardsAndAlbumNavigation() {
        composeRule.onNodeWithText("Belohnungen").performClick()

        composeRule.onNodeWithText("Deine Belohnungen").assertExists()
        composeRule.onNodeWithText("Belohnungsbild hinzufügen").assertExists()
        composeRule.onNodeWithText("Alben anzeigen").assertExists()
    }

    @Test
    fun settingsTab_showsResetAndReminderControls() {
        composeRule.onNodeWithText("Einstellungen").performClick()

        composeRule.onNodeWithText("Reset-Zeitpunkte").assertExists()
        composeRule.onNodeWithText("Exakte Reset-Zeit").assertExists()
        composeRule.onNodeWithText("Erinnerungen").assertExists()
        composeRule.onNodeWithText("Alle Daten löschen").assertExists()
    }
}
