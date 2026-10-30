package com.aistudio.trueidle.engine

import com.aistudio.trueidle.model.GameState

/** The resulting state plus whether the action actually completed (callers gate achievements on it). */
internal data class ActionOutcome(val state: GameState, val completed: Boolean)
