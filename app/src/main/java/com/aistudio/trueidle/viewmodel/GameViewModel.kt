package com.aistudio.trueidle.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.trueidle.core.Telemetry
import com.aistudio.trueidle.core.AppLog
import com.aistudio.trueidle.core.DisciplineCategory
import com.aistudio.trueidle.core.GameClock
import com.aistudio.trueidle.core.snapshot
import com.aistudio.trueidle.data.FileSaveStore
import com.aistudio.trueidle.data.SaveRepository
import com.aistudio.trueidle.data.SaveResult
import com.aistudio.trueidle.engine.Balance
import com.aistudio.trueidle.engine.GameEngine
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.runtime.AndroidClock
import com.aistudio.trueidle.runtime.IdleAlertScheduler
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.random.Random

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val clock: GameClock = AndroidClock(app)
    private val repo = SaveRepository(FileSaveStore(File(app.filesDir, "saves")))
    private val engine = GameEngine(clock = clock, random = Random.Default, saves = repo)

    private val _gameState = MutableStateFlow(GameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _recoveryReason = MutableStateFlow<String?>(null)

    /**
     * Non-null while the stored save could not be read. The UI shows recovery options and saving
     * stays blocked so the unreadable bytes are never overwritten.
     */
    val recoveryReason: StateFlow<String?> = _recoveryReason.asStateFlow()

    private val _logFilter = MutableStateFlow("all")
    val logFilter: StateFlow<String> = _logFilter.asStateFlow()

    private var loop: Job? = null
    private var autosave: Job? = null
    private var writer: Job? = null
    private var booted = false

    /**
     * Set when the stored save could not be read. Saving stays blocked so the unreadable bytes are
     * never overwritten before the player decides what to do with them.
     */
    private var savingBlocked = false

    /**
     * Conflated single-writer channel. Callers only push a signal, so nothing on the main thread
     * ever encodes JSON, computes a CRC, fsyncs or moves a file.
     */
    private val saveRequests = Channel<Unit>(Channel.CONFLATED)

    init {
        onForeground()
    }

    fun onForeground() {
        viewModelScope.launch(Dispatchers.Default) {
            if (!booted) {
                boot()
                booted = true
            }
            // The alert only matters while the app is away, so returning cancels it.
            IdleAlertScheduler.cancel(getApplication())
            engine.resume(clock.snapshot())
            _gameState.value = engine.state
            if (engine.consumeEightHourIdleAlert()) {
                AppLog.warn(TAG, "eight-hour idle cap reached; offline report pending")
            }
            if (loop?.isActive != true) {
                loop = viewModelScope.launch { runLoop() }
            }
        }
        if (autosave?.isActive != true) {
            autosave = viewModelScope.launch {
                while (isActive) {
                    delay(AUTOSAVE_MS)
                    requestSave()
                }
            }
        }
        if (writer?.isActive != true) {
            writer = viewModelScope.launch { writeLoop() }
        }
    }

    fun onBackground() {
        loop?.cancel()
        loop = null
        autosave?.cancel()
        autosave = null
        // The writer is deliberately left running so this final save lands.
        requestSave()
        // Fire the idle alert when the window elapses, not when the player comes back.
        IdleAlertScheduler.schedule(getApplication(), clock.wallMs() + Balance.OFFLINE_CAP_MS)
    }

    private suspend fun runLoop() {
        var last = clock.elapsedMs()
        var accumulated = 0L
        while (currentCoroutineContext().isActive) {
            delay(TICK_MS)
            val now = clock.elapsedMs()
            accumulated += (now - last).coerceIn(0L, STEP_MS * MAX_STEPS)
            last = now

            // Ticks are suspended while a report is pending: claiming applies the whole result
            // atomically, and a live tick would otherwise race the claim's floor change.
            if (engine.state.pendingOfflineLoot != null) {
                accumulated = 0L
                continue
            }

            var steps = 0
            while (accumulated >= STEP_MS && steps < MAX_STEPS) {
                engine.tick(STEP_MS / 1000f)
                accumulated -= STEP_MS
                steps++
            }
            if (steps >= MAX_STEPS) accumulated = 0L
            if (steps > 0) _gameState.value = engine.state
        }
    }

    private suspend fun writeLoop() {
        for (unused in saveRequests) {
            // Coalesce a burst (a purchase, a craft, a slider drag) into one write.
            delay(SAVE_DEBOUNCE_MS)
            while (saveRequests.tryReceive().isSuccess) {
                // drain
            }
            if (savingBlocked) continue
            val snapshot = engine.snapshotForSave(clock.snapshot())
            val result = withContext(Dispatchers.IO) { repo.save(snapshot) }
            if (result is SaveResult.Failed) {
                AppLog.warn(TAG, "save failed: ${result.reason}")
            }
        }
    }

    /** Requests a save. Cheap and non-blocking: the actual write happens on Dispatchers.IO. */
    fun saveNow() = requestSave()

    private fun requestSave() {
        if (savingBlocked) return
        saveRequests.trySend(Unit)
    }

    private fun boot() {
        when (val loaded = repo.load()) {
            is SaveRepository.Loaded.Ok -> engine.restore(loaded.state)
            is SaveRepository.Loaded.Recovered -> {
                engine.restore(loaded.state)
                engine.notifyRecovered(loaded.from)
            }
            is SaveRepository.Loaded.Failed -> {
                // Block saving, and surface recovery UI instead of silently starting a new game
                // (which is what used to happen, destroying the raw bytes on the next autosave).
                savingBlocked = true
                _recoveryReason.value = loaded.reason
            }
            SaveRepository.Loaded.Empty -> Unit
        }
    }

    fun retryLoad() {
        when (val loaded = repo.load()) {
            is SaveRepository.Loaded.Ok -> acceptRecovered(loaded.state)
            is SaveRepository.Loaded.Recovered -> acceptRecovered(loaded.state)
            SaveRepository.Loaded.Empty -> clearRecovery()
            is SaveRepository.Loaded.Failed -> _recoveryReason.value = loaded.reason
        }
    }

    private fun acceptRecovered(state: GameState) {
        engine.restore(state)
        clearRecovery()
        _gameState.value = engine.state
    }

    private fun clearRecovery() {
        savingBlocked = false
        _recoveryReason.value = null
    }

    /** Keeps the quarantined bytes on disk but starts a clean game. */
    fun discardCorruptSave() {
        engine.resetGame()
        repo.clearPrevious()
        clearRecovery()
        _gameState.value = engine.state
        requestSave()
    }

    // ------------------------------------------------------------------ actions

    private fun mutate(block: () -> GameState) {
        _gameState.value = block()
        requestSave()
    }

    fun navFloor(delta: Int) = mutate { engine.navFloor(delta) }
    fun setFloor(floor: Int) = mutate { engine.setFloor(floor) }
    fun toggleAutoAdvance() = mutate { engine.toggleAutoAdvance() }
    fun toggleAutoCollect() = mutate { engine.toggleAutoCollect() }
    fun harvestPlot(plotId: Int) = mutate { engine.harvestPlot(plotId) }
    fun mineVein(veinId: Int) = mutate { engine.mineVein(veinId) }
    fun togglePlotAutoQueue(plotId: Int) = mutate { engine.togglePlotAutoQueue(plotId) }
    fun toggleVeinAutoQueue(veinId: Int) = mutate { engine.toggleVeinAutoQueue(veinId) }
    fun summonMinion(type: String) = mutate { engine.summonMinion(type) }
    fun mergeBeasts(id1: String, id2: String) = mutate { engine.mergeBeasts(id1, id2) }
    fun trainNode(nodeId: String) = mutate { engine.trainNode(nodeId) }
    fun equipItem(itemId: String) = mutate { engine.equipItem(itemId) }
    fun unequipItem(itemId: String) = mutate { engine.unequipItem(itemId) }
    fun usePotion(itemId: String) = mutate { engine.usePotion(itemId) }
    fun sellItem(itemId: String, count: Int = 1) = mutate { engine.sellItem(itemId, count) }
    fun craftEquipment(name: String, tier: Int, slot: String, reqOre: String, reqQty: Int) =
        mutate { engine.craftEquipment(name, tier, slot, reqOre, reqQty) }
    fun brewPotion(name: String, reqHerb: String, reqQty: Int) =
        mutate { engine.brewPotion(name, reqHerb, reqQty) }
    fun toggleEssenceAegis() = mutate { engine.toggleEssenceAegis() }
    fun upgradeEssenceSmite() = mutate { engine.upgradeEssenceSmite() }
    fun rerollSlayerTask() = mutate { engine.rerollSlayerTask() }
    fun upgradeDiscipline(category: DisciplineCategory, upgradeId: String) =
        mutate { engine.upgradeDiscipline(category, upgradeId) }
    fun activateTalent(talentId: String) = mutate { engine.activateTalent(talentId) }
    fun upgradeTalent(talentId: String) = mutate { engine.upgradeTalent(talentId) }
    fun claimAchievement(achId: String) = mutate { engine.claimAchievement(achId) }
    fun toggleAutoEquipBestGear() = mutate { engine.toggleAutoEquipBestGear() }
    fun toggleAutoMergeBeasts() = mutate { engine.toggleAutoMergeBeasts() }
    fun setCraftMaterialUsage(pct: Int) = mutate { engine.setCraftMaterialUsage(pct) }
    fun setNotation(notation: String) = mutate { engine.setNotation(notation) }
    fun fightPromotionBoss() = mutate { engine.fightPromotionBoss() }
    fun clearLogs() = mutate { engine.clearLogs() }

    fun claimOfflineLoot() {
        _gameState.value = engine.claimOfflineLoot()
        // A claim can resume the loop, so re-anchor the away window from now.
        requestSave()
    }

    fun setLogFilter(filter: String) {
        _logFilter.value = filter
    }

    fun exportSave(): String = engine.exportSaveJson()

    fun importSave(json: String): Boolean {
        val success = engine.importSaveJson(json)
        if (success) {
            _gameState.value = engine.state
            requestSave()
        }
        return success
    }

    fun resetGame() {
        engine.resetGame()
        // Drop the pre-reset backup, otherwise a later corruption would resurrect the old game.
        repo.clearPrevious()
        _gameState.value = engine.state
        requestSave()
    }

    /** One shared builder for the Settings export buttons; LogView uses the same one. */
    fun buildTelemetryReport(): String = Telemetry.report(_gameState.value)

    private companion object {
        const val TAG = "GameViewModel"
        const val STEP_MS = 250L
        const val MAX_STEPS = 8L
        const val TICK_MS = 50L
        const val AUTOSAVE_MS = 30_000L
        const val SAVE_DEBOUNCE_MS = 750L
    }
}
