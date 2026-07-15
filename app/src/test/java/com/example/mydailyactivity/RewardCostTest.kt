package com.example.mydailyactivity

import com.example.mydailyactivity.userinterface.calculateCost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RewardCostTest {
    @Test
    fun linearCost_increasesByTwentyPerReward() {
        assertEquals(50, calculateCost(n = 0, mode = "linear"))
        assertEquals(70, calculateCost(n = 1, mode = "linear"))
        assertEquals(110, calculateCost(n = 3, mode = "linear"))
    }

    @Test
    fun constantCost_usesConfiguredValue() {
        assertEquals(125, calculateCost(n = 4, mode = "constant", fixedCost = 125))
    }

    @Test
    fun quadraticCost_usesSquareGrowth() {
        assertEquals(0, calculateCost(n = 0, mode = "quadratic"))
        assertEquals(10, calculateCost(n = 1, mode = "quadratic"))
        assertEquals(90, calculateCost(n = 3, mode = "quadratic"))
    }

    @Test
    fun unknownMode_fallsBackToDefaultCost() {
        assertEquals(50, calculateCost(n = 2, mode = "unknown"))
    }

    @Test
    fun randomCost_staysInsideExpectedRange() {
        repeat(100) {
            val cost = calculateCost(n = it, mode = "random")
            assertTrue(cost in 1 until 1000)
        }
    }
}
