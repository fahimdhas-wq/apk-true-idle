package com.aistudio.trueidle.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aistudio.trueidle.engine.GameEngine
import com.aistudio.trueidle.model.GameState
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val engine = GameEngine()
    private val _gameState = MutableStateFlow(engine.state)
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _logFilter = MutableStateFlow("all")
    val logFilter: StateFlow<String> = _logFilter.asStateFlow()

    init {
        startGameLoop()
    }

    private fun startGameLoop() {
        viewModelScope.launch {
            while (true) {
                delay(250L) // 4 ticks per second
                val nextState = engine.tick(0.25f)
                _gameState.value = nextState
            }
        }
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

    fun harvestPlot(plotId: Int) {
        _gameState.value = engine.harvestPlot(plotId)
    }

    fun mineVein(veinId: Int) {
        _gameState.value = engine.mineVein(veinId)
    }

    fun summonMinion(type: String) {
        _gameState.value = engine.summonMinion(type)
    }

    fun mergeBeasts(id1: String, id2: String) {
        _gameState.value = engine.mergeBeasts(id1, id2)
    }

    fun trainNode(nodeId: String) {
        _gameState.value = engine.trainNode(nodeId)
    }

    fun equipItem(itemId: String) {
        _gameState.value = engine.equipItem(itemId)
    }

    fun craftEquipment(name: String, tier: Int, slot: String, reqOre: String, reqQty: Int) {
        _gameState.value = engine.craftEquipment(name, tier, slot, reqOre, reqQty)
    }

    fun brewPotion(name: String, reqHerb: String, reqQty: Int) {
        _gameState.value = engine.brewPotion(name, reqHerb, reqQty)
    }

    fun clearLogs() {
        _gameState.value = engine.clearLogs()
    }

    fun setLogFilter(filter: String) {
        _logFilter.value = filter
    }
}
