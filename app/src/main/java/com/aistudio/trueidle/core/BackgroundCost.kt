package com.aistudio.trueidle.core

/**
 * Rough cost model for background play. Pure arithmetic so it is testable off-device.
 *
 * The battery figure is an estimate, not a measurement: Android exposes no per-app battery reading
 * to the app itself. It scales the measured CPU load by an assumed active-core current draw and
 * adds a fixed overhead for holding the process at foreground priority and keeping the notification.
 */
object BackgroundCost {
    /** Assumed average current of one busy mobile core, in mA. */
    const val ASSUMED_CORE_MA = 250.0

    /** Assumed overhead of a foreground service that is otherwise idle, in mA. */
    const val SERVICE_OVERHEAD_MA = 8.0

    /** Assumed battery capacity used to turn mA into a percentage, in mAh. */
    const val ASSUMED_BATTERY_MAH = 4500.0

    /** Spread applied around the central estimate, since devices differ widely. */
    const val LOW_FACTOR = 0.6
    const val HIGH_FACTOR = 2.0

    data class Range(val lowPctPerHour: Double, val highPctPerHour: Double)

    /** Battery drain in percent of capacity per hour for the given measured CPU load. */
    fun batteryPctPerHour(cpuPercentOfOneCore: Double): Range {
        val cpu = cpuPercentOfOneCore.coerceAtLeast(0.0)
        val ma = cpu / 100.0 * ASSUMED_CORE_MA + SERVICE_OVERHEAD_MA
        val central = ma / ASSUMED_BATTERY_MAH * 100.0
        return Range(central * LOW_FACTOR, central * HIGH_FACTOR)
    }
}
