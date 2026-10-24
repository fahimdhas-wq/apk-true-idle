package com.aistudio.trueidle

import com.aistudio.trueidle.data.SaveRepository
import com.aistudio.trueidle.data.SaveStore
import com.aistudio.trueidle.model.GameState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MemorySaveStore : SaveStore {
    private val files = mutableMapOf<String, ByteArray>()

    override fun read(name: String): ByteArray? = files[name]

    override fun write(name: String, bytes: ByteArray) {
        files[name] = bytes
    }
}

class SaveRepositoryTest {

    @Test
    fun testSaveAndLoadRoundTrip() {
        val store = MemorySaveStore()
        val repo = SaveRepository(store)

        val state = GameState(totalKills = 42L)
        repo.save(state)

        val loaded = repo.load()
        assertTrue(loaded is SaveRepository.Loaded.Ok)
        assertEquals(42L, (loaded as SaveRepository.Loaded.Ok).state.totalKills)
    }

    @Test
    fun testCorruptPayloadTriggersRecoveryOrFailure() {
        val store = MemorySaveStore()
        val repo = SaveRepository(store)

        val state = GameState(totalKills = 100L)
        repo.save(state)

        // Corrupt main slot
        store.write(SaveRepository.SLOT, "invalid bytes { not json }".toByteArray())

        val loaded = repo.load()
        // If no previous slot, returns Failed
        assertTrue(loaded is SaveRepository.Loaded.Failed)
    }

    @Test
    fun testFallBackToPreviousSlot() {
        val store = MemorySaveStore()
        val repo = SaveRepository(store)

        // First save
        val state1 = GameState(totalKills = 10L)
        repo.save(state1)

        // Second save (moves state1 to slot.prev)
        val state2 = GameState(totalKills = 20L)
        repo.save(state2)

        // Corrupt main slot
        store.write(SaveRepository.SLOT, "corrupt".toByteArray())

        val loaded = repo.load()
        assertTrue(loaded is SaveRepository.Loaded.Recovered)
        assertEquals(10L, (loaded as SaveRepository.Loaded.Recovered).state.totalKills)
    }
}
