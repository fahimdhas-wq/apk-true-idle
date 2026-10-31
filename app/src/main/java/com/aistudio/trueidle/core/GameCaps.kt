package com.aistudio.trueidle.core

/** Engine caps shared by the engine and the screens that display them. */
object GameCaps {
    const val BEAST = 8
    const val MINION = 12
    const val LOG = 40
    const val MAX_FLOOR = 100

    /** Maximum level of the player and of every class (necro, essence, slayer, training, ...). */
    const val MAX_LEVEL = 100
    const val RESERVED_ID_FLOOR = 100L

    /** Aegis barrier damage reduction, before the Radiant Capacitor upgrade scales it. */
    const val AEGIS_DAMAGE_REDUCTION = 0.2

    /** Offline window: the single source for the 8-hour cap quoted in the engine and the UI. */
    const val OFFLINE_CAP_HOURS = 8
    const val OFFLINE_EFFICIENCY_PCT = 65
}
