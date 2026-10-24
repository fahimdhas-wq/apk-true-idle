package com.aistudio.trueidle.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.trueidle.core.GameClock
import com.aistudio.trueidle.core.snapshot
import com.aistudio.trueidle.data.FileSaveStore
import com.aistudio.trueidle.data.SaveRepository
import com.aistudio.trueidle.engine.GameEngine
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.runtime.AndroidClock
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.io.File
import kotlin.random.Random

class GameViewModel(app: Application) : AndroidViewModel(app) {

    private val clock: GameClock = AndroidClock(app)
    private val repo = SaveRepository(FileSaveStore(File(app.filesDir, "saves")))
    private val engine = GameEngine(clock = clock, random = Random.Default)

    private val _gameState = MutableStateFlow(engine.state)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _recovery = MutableStateFlow<SaveRepository.Loaded.Failed?>(null)
    val recovery: StateFlow<SaveRepository.Loaded.Failed?> = _recovery.asStateFlow()

    private val _logFilter = MutableStateFlow("all")
    val logFilter: StateFlow<String> = _logFilter.asStateFlow()

    private var loop: Job? = null
    private var autosave: Job? = null
    private var booted = false

    init {
        onForeground()
    }

    fun onForeground() {
        if (!booted) {
            boot()
            booted = true
        }
        engine.resume(clock.snapshot())
        _gameState.value = engine.state
        if (loop?.isActive != true) {
            loop = viewModelScope.launch { runLoop() }
        }
        if (autosave?.isActive != true) {
            autosave = viewModelScope.launch {
                while (isActive) {
                    delay(30_000)
                    saveNow()
                }
            }
        }
    }

    fun onBackground() {
        loop?.cancel()
        loop = null
        autosave?.cancel()
        autosave = null
        saveNow()
    }

    private suspend fun CoroutineScope.runLoop() {
        var last = clock.elapsedMs()
        var acc = 0L
        while (isActive) {
            delay(50)
            val now = clock.elapsedMs()
            acc += (now - last).coerceIn(0L, STEP_MS * MAX_STEPS)
            last = now
            var n = 0
            while (acc >= STEP_MS && n < MAX_STEPS) {
                engine.tick(STEP_MS / 1000f)
                acc -= STEP_MS
                n++
            }
            if (n >= MAX_STEPS) acc = 0
            if (n > 0) _gameState.value = engine.state
        }
    }

    private fun boot() {
        when (val r = repo.load()) {
            is SaveRepository.Loaded.Ok -> engine.restore(r.state)
            is SaveRepository.Loaded.Recovered -> {
                engine.restore(r.state)
                engine.notifyRecovered(r.from)
            }
            is SaveRepository.Loaded.Failed -> _recovery.value = r
            SaveRepository.Loaded.Empty -> Unit
        }
    }

    fun saveNow() {
        repo.save(engine.snapshotForSave(clock.snapshot()))
    }

    fun navFloor(delta: Int) {
        _gameState.value = engine.navFloor(delta)
    }

    fun setFloor(floor: Int) {
        _gameState.value = engine.setFloor(floor)
    }

    fun toggleAutoAdvance() {
        _gameState.value = engine.toggleAutoAdvance()
    }

    fun toggleAutoCollect() {
        _gameState.value = engine.toggleAutoCollect()
    }

    fun harvestPlot(plotId: Int) {
        _gameState.value = engine.harvestPlot(plotId)
    }

    fun mineVein(veinId: Int) {
        _gameState.value = engine.mineVein(veinId)
    }

    fun summonMinion(type: String) {
        _gameState.value = engine.summonMinion(type)
        saveNow()
    }

    fun mergeBeasts(id1: String, id2: String) {
        _gameState.value = engine.mergeBeasts(id1, id2)
        saveNow()
    }

    fun trainNode(nodeId: String) {
        _gameState.value = engine.trainNode(nodeId)
        saveNow()
    }

    fun equipItem(itemId: String) {
        _gameState.value = engine.equipItem(itemId)
        saveNow()
    }

    fun unequipItem(itemId: String) {
        _gameState.value = engine.unequipItem(itemId)
        saveNow()
    }

    fun craftEquipment(name: String, tier: Int, slot: String, reqOre: String, reqQty: Int) {
        _gameState.value = engine.craftEquipment(name, tier, slot, reqOre, reqQty)
        saveNow()
    }

    fun brewPotion(name: String, reqHerb: String, reqQty: Int) {
        _gameState.value = engine.brewPotion(name, reqHerb, reqQty)
        saveNow()
    }

    fun usePotion(itemId: String) {
        _gameState.value = engine.usePotion(itemId)
    }

    fun sellItem(itemId: String, count: Int = 1) {
        _gameState.value = engine.sellItem(itemId, count)
        saveNow()
    }

    fun toggleSanctifiedAegis() {
        _gameState.value = engine.toggleSanctifiedAegis()
    }

    fun upgradeSanctifiedSmite() {
        _gameState.value = engine.upgradeSanctifiedSmite()
        saveNow()
    }

    fun rerollSlayerTask() {
        _gameState.value = engine.rerollSlayerTask()
        saveNow()
    }

    fun claimAchievement(achId: String) {
        _gameState.value = engine.claimAchievement(achId)
        saveNow()
    }

    fun clearLogs() {
        _gameState.value = engine.clearLogs()
    }

    fun setLogFilter(filter: String) {
        _logFilter.value = filter
    }

    fun dismissNotification(id: Long) {
        _gameState.value = engine.dismissNotification(id)
    }

    fun toggleNotificationBanners() {
        _gameState.value = engine.toggleNotificationBanners()
    }

    fun clearNotificationHistory() {
        _gameState.value = engine.clearNotificationHistory()
    }

    fun claimOfflineLoot() {
        _gameState.value = engine.claimOfflineLoot()
        saveNow()
    }

    fun exportSave(): String {
        return engine.exportSaveJson()
    }

    fun importSave(json: String): Boolean {
        val success = engine.importSaveJson(json)
        if (success) {
            _gameState.value = engine.state
            saveNow()
        }
        return success
    }

    fun resetGame() {
        _gameState.value = engine.resetGame()
        saveNow()
    }

    private companion object {
        const val STEP_MS = 250L
        const val MAX_STEPS = 8L
    }
}
