package com.aistudio.trueidle.data

import com.aistudio.trueidle.core.AppLog
import com.aistudio.trueidle.model.GameState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.zip.CRC32

@Serializable
data class SaveEnvelope(
    val format: String = SaveRepository.FORMAT,
    val schema: Int = Migrations.CURRENT_SCHEMA,
    val savedWallMs: Long = 0L,
    val crc32: String = "",
    val payload: String = ""
)

/** Outcome of a write attempt. `save` never throws; callers decide what to surface. */
sealed interface SaveResult {
    data object Saved : SaveResult
    data class Failed(val reason: String, val cause: Throwable? = null) : SaveResult
}

class SaveRepository(private val store: SaveStore, private val json: Json = SaveJson) {

    sealed interface Loaded {
        data object Empty : Loaded
        data class Ok(val state: GameState) : Loaded
        data class Recovered(val state: GameState, val from: String) : Loaded

        /**
         * Not a `data class`: it holds a `ByteArray`, which has reference identity, so generated
         * equals/hashCode would be wrong and misleading.
         */
        class Failed(val reason: String, val raw: ByteArray?) : Loaded
    }

    /**
     * Encodes, self-verifies, then swaps slots. A failure (unverifiable payload or an IO error)
     * leaves the previous slot untouched instead of throwing into the game loop.
     */
    fun save(state: GameState): SaveResult = try {
        val bytes = encode(state)
        if (decode(bytes) == null) {
            AppLog.warn(TAG, "save self-verification failed; keeping previous slot")
            SaveResult.Failed("SAVE_SELF_VERIFY_FAILED")
        } else {
            runCatching {
                store.read(SLOT)?.takeIf { decode(it) != null }?.let { store.write(PREV, it) }
            }.onFailure { AppLog.warn(TAG, "could not rotate previous save slot", it) }
            store.write(SLOT, bytes)
            SaveResult.Saved
        }
    } catch (t: Throwable) {
        // FileSaveStore.write can throw IOException; never let a save crash the app.
        AppLog.warn(TAG, "save failed; previous slot retained", t)
        SaveResult.Failed(t::class.java.simpleName, t)
    }

    fun load(): Loaded {
        val main = runCatching { store.read(SLOT) }.getOrNull()
        decode(main)?.let { return Loaded.Ok(it) }
        val prev = runCatching { store.read(PREV) }.getOrNull()
        decode(prev)?.let { return Loaded.Recovered(it, PREV) }
        if (main == null && prev == null) return Loaded.Empty
        val raw = main ?: prev
        // Quarantine the unreadable bytes: the next autosave must not overwrite the only copy of a
        // corrupt or newer-schema save.
        runCatching { raw?.let { store.write(QUARANTINE, it) } }
            .onFailure { AppLog.warn(TAG, "could not quarantine unreadable save", it) }
        return Loaded.Failed("SAVE_UNREADABLE", raw)
    }

    /** The envelope JSON (payload + CRC) that export shares with the user. */
    fun exportText(state: GameState): String = encode(state).toString(Charsets.UTF_8)

    /**
     * Drops the fallback slot. Used by a factory reset: otherwise the pre-reset save would be
     * promoted into `slot.prev` and could resurrect the old game after a later corruption.
     */
    fun clearPrevious() = runCatching { store.delete(PREV) }.onFailure {
        AppLog.warn(TAG, "could not clear previous slot", it)
    }

    /**
     * Accepts an envelope produced by [exportText], or a legacy raw `GameState` JSON body (the old
     * clipboard export), which is migrated as schema 0. Returns null when the bytes are not a save.
     */
    fun decode(bytes: ByteArray?): GameState? = bytes?.let {
        runCatching {
            val root = json.parseToJsonElement(it.toString(Charsets.UTF_8)).jsonObject
            if (root["format"]?.jsonPrimitive?.content == FORMAT) {
                val env = json.decodeFromJsonElement(SaveEnvelope.serializer(), root)
                if (env.schema > Migrations.CURRENT_SCHEMA) return null
                if (env.crc32 != crc(env.payload)) return null
                // Future schemas are rejected rather than silently downgraded.
                Migrations.apply(json.parseToJsonElement(env.payload).jsonObject, env.schema, json)
            } else {
                // A bare object is only treated as a legacy save when it has the shape of one;
                // `{}` or an unrelated JSON object must not silently become a default game.
                if (!looksLikeGameState(root)) return null
                Migrations.apply(root, 0, json)
            }
        }.getOrNull()
    }

    private fun looksLikeGameState(root: JsonObject): Boolean =
        root.containsKey("player") || root.containsKey("meta") || root.containsKey("lifetime")

    fun encode(state: GameState): ByteArray {
        val payload = json.encodeToString(GameState.serializer(), state)
        val env = SaveEnvelope(
            schema = Migrations.CURRENT_SCHEMA,
            savedWallMs = state.meta.savedWallMs,
            crc32 = crc(payload),
            payload = payload
        )
        return json.encodeToString(SaveEnvelope.serializer(), env).toByteArray(Charsets.UTF_8)
    }

    fun crc(s: String): String =
        CRC32().apply { update(s.toByteArray(Charsets.UTF_8)) }.value.toString(16).padStart(8, '0')

    companion object {
        const val FORMAT = "true-idle"
        const val SLOT = "slot"
        const val PREV = "slot.prev"
        const val QUARANTINE = "slot.quarantine"
        private const val TAG = "SaveRepository"
    }
}

val SaveJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
    // A field of the wrong type falls back to its default instead of failing the whole save.
    coerceInputValues = true
}
