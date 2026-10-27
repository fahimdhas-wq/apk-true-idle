package com.aistudio.trueidle.core

/**
 * The single minion table. The Genesis screen used to advertise DPS/soak numbers that did not
 * match what the engine actually summoned.
 */
object MinionCatalog {
    const val SOAK = 15.0

    val TYPES = listOf("Skeleton", "Bone Mage", "Dread Knight", "Bone Lich")

    fun normalized(type: String): String =
        if (TYPES.any { it.equals(type, ignoreCase = true) }) {
            TYPES.first { it.equals(type, ignoreCase = true) }
        } else {
            "Skeleton"
        }

    fun dps(type: String): Double = when (normalized(type)) {
        "Bone Mage" -> 8.0
        "Dread Knight" -> 16.0
        "Bone Lich" -> 35.0
        else -> 4.0
    }

    fun cost(type: String): Long = when (normalized(type)) {
        "Bone Mage" -> 25L
        "Dread Knight" -> 50L
        "Bone Lich" -> 100L
        else -> 10L
    }

    fun blurb(type: String): String = when (normalized(type)) {
        "Bone Mage" -> "${dps(type).toInt()} DPS · ranged arcane support"
        "Dread Knight" -> "${dps(type).toInt()} DPS · frontline bruiser"
        "Bone Lich" -> "${dps(type).toInt()} DPS · apex reanimation"
        else -> "${dps(type).toInt()} DPS · cheap frontline fodder"
    }
}
