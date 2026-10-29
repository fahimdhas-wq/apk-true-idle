package com.aistudio.trueidle

import com.aistudio.trueidle.core.BackgroundCost
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BackgroundCostTest {
    @Test
    fun testBatteryRangeScalesWithCpu() {
        val idle = BackgroundCost.batteryPctPerHour(0.0)
        val busy = BackgroundCost.batteryPctPerHour(10.0)
        assertTrue(idle.lowPctPerHour > 0.0)
        assertTrue(idle.lowPctPerHour < idle.highPctPerHour)
        assertTrue(busy.highPctPerHour > idle.highPctPerHour)
    }

    @Test
    fun testNegativeCpuIsTreatedAsZero() {
        assertEquals(
            BackgroundCost.batteryPctPerHour(0.0),
            BackgroundCost.batteryPctPerHour(-5.0)
        )
    }
}
