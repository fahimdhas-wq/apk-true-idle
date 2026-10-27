package com.aistudio.trueidle.core

/**
 * Canonical log entry types. The engine only ever emits these, so the combat-log filters in the UI
 * actually match something.
 */
object LogType {
    const val COMBAT = "combat"
    const val LOOT = "loot"
    const val SKILL = "skill"
    const val SYSTEM = "system"
    const val MILESTONE = "milestone"
    const val GAIN = "gain"
}
