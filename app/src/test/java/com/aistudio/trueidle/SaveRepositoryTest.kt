package com.aistudio.trueidle

import com.aistudio.trueidle.data.SaveRepository
import com.aistudio.trueidle.data.SaveStore
import com.aistudio.trueidle.model.GameState
import com.aistudio.trueidle.model.LifetimeStats
import org.junit.Assert.assertEquals
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

        val state = GameState(lifetime = LifetimeStats(totalKills = 42L))
        repo.save(state)

        val loaded = repo.load()
        assertTrue(loaded is SaveRepository.Loaded.Ok)
        assertEquals(42L, (loaded as SaveRepository.Loaded.Ok).state.lifetime.totalKills)
    }

    @Test
    fun testCorruptPayloadTriggersRecoveryOrFailure() {
        val store = MemorySaveStore()
        val repo = SaveRepository(store)

        val state = GameState(lifetime = LifetimeStats(totalKills = 100L))
        repo.save(state)

        // Corrupt main slot
        store.write(SaveRepository.SLOT, "invalid bytes { not json }".toByteArray())

        val loaded = repo.load()
        // If no previous slot, returns Failed
        assertTrue(loaded is SaveRepository.Loaded.Failed)
    }

    @Test
    fun testUnreadableBytesAreQuarantined() {
        val store = MemorySaveStore()
        val repo = SaveRepository(store)

        repo.save(GameState(lifetime = LifetimeStats(totalKills = 7L)))
        store.write(SaveRepository.SLOT, "corrupt".toByteArray())
        store.write(SaveRepository.PREV, "also corrupt".toByteArray())

        val loaded = repo.load()
        assertTrue(loaded is SaveRepository.Loaded.Failed)
        assertEquals(
            "corrupt",
            store.read(SaveRepository.QUARANTINE)?.toString(Charsets.UTF_8)
        )
    }

    @Test
    fun testFallBackToPreviousSlot() {
        val store = MemorySaveStore()
        val repo = SaveRepository(store)

        // First save
        val state1 = GameState(lifetime = LifetimeStats(totalKills = 10L))
        repo.save(state1)

        // Second save (moves state1 to slot.prev)
        val state2 = GameState(lifetime = LifetimeStats(totalKills = 20L))
        repo.save(state2)

        // Corrupt main slot
        store.write(SaveRepository.SLOT, "corrupt".toByteArray())

        val loaded = repo.load()
        assertTrue(loaded is SaveRepository.Loaded.Recovered)
        assertEquals(10L, (loaded as SaveRepository.Loaded.Recovered).state.lifetime.totalKills)
    }

    @Test
    fun testEmptyObjectIsNotTreatedAsASave() {
        val store = MemorySaveStore()
        val repo = SaveRepository(store)
        store.write(SaveRepository.SLOT, "{}".toByteArray())

        assertTrue(repo.load() is SaveRepository.Loaded.Failed)
    }

    @Test
    fun testExportProducesVerifiedEnvelope() {
        val repo = SaveRepository(com.aistudio.trueidle.data.NoOpSaveStore)
        val state = GameState(lifetime = LifetimeStats(totalKills = 5L))
        val exported = repo.exportText(state)

        assertTrue(exported.contains("\"format\":\"true-idle\""))
        assertTrue(exported.contains("\"crc32\""))

        val reimported = repo.decode(exported.toByteArray())
        assertEquals(5L, reimported?.lifetime?.totalKills)
    }

    @Test
    fun testTamperedPayloadIsRejectedByCrc() {
        val repo = SaveRepository(com.aistudio.trueidle.data.NoOpSaveStore)
        val exported = repo.exportText(GameState())
        // The payload is a *string* field of the envelope, so its inner quotes are escaped. Matching
        // the unescaped form did nothing at all, which made this test vacuous.
        val tampered = exported.replace("\\\"lvl\\\":1", "\\\"lvl\\\":99")

        assertTrue("the tamper must actually change the payload", tampered != exported)
        assertEquals(null, repo.decode(tampered.toByteArray()))
    }
}
