package com.aistudio.trueidle.ui.state

import androidx.compose.runtime.compositionLocalOf
import com.aistudio.trueidle.core.Derived

/** Derived stats shared by every screen; declared outside the app shell so screens don't depend on it. */
val LocalDerived = compositionLocalOf<Derived> { error("Derived not provided") }
