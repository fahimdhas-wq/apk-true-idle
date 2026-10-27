package com.aistudio.trueidle.core

/**
 * The disciplines whose upgrade lists `GameEngine.upgradeDiscipline` can spend into. Lives in
 * `core` so both the engine and the UI can reference it without the UI importing the engine.
 */
enum class DisciplineCategory { HARVEST, ARMORY, GENESIS_BEAST, GENESIS_NECRO, ESSENCE }
