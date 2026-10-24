package com.aistudio.trueidle.data

import com.aistudio.trueidle.model.GameState
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.util.zip.CRC32

@Serializable
data class SaveEnvelope(
    val format: String = "true-idle",
    val schema: Int,
    val savedWallMs: Long,
    val crc32: String, // CRC32 of payload UTF-8 bytes
    val payload: String // JSON of GameState
)

class SaveRepository(private val store: SaveStore, private val json: Json = SaveJson) {
    sealed interface Loaded {
        data object Empty : Loaded
        data class Ok(val state: GameState) : Loaded
        data class Recovered(val state: GameState, val from: String) : Loaded
        data class Failed(val reason: String, val raw: ByteArray?) : Loaded
    }

    fun save(state: GameState) {
        val payload = json.encodeToString(GameState.serializer(), state)
        val env = SaveEnvelope(
            schema = SCHEMA,
            savedWallMs = state.meta.savedWallMs,
            crc32 = crc(payload),
            payload = payload
        )
        val bytes = json.encodeToString(SaveEnvelope.serializer(), env).toByteArray(Charsets.UTF_8)
        check(decode(bytes) != null) { "save failed self-verification" } // verify before swapping
        store.read(SLOT)?.takeIf { decode(it) != null }?.let { store.write(PREV, it) }
        store.write(SLOT, bytes)
    }

    fun load(): Loaded {
        val main = store.read(SLOT)
        decode(main)?.let { return Loaded.Ok(it) }
        val prev = store.read(PREV)
        decode(prev)?.let { return Loaded.Recovered(it, PREV) }
        return if (main == null && prev == null) Loaded.Empty
        else Loaded.Failed("SAVE_UNREADABLE", main ?: prev)
    }

    /** Accepts an envelope, or a legacy raw GameState JSON (the old clipboard export). */
    fun decode(bytes: ByteArray?): GameState? = bytes?.let {
        runCatching {
            val text = it.toString(Charsets.UTF_8)
            val root = json.parseToJsonElement(text).jsonObject
            if (root["format"]?.jsonPrimitive?.content == "true-idle") {
                val env = json.decodeFromJsonElement(SaveEnvelope.serializer(), root)
                if (env.schema > SCHEMA || env.crc32 != crc(env.payload)) return null
                Migrations.apply(json.parseToJsonElement(env.payload).jsonObject, env.schema, json)
            } else {
                Migrations.apply(root, 0, json) // schema 0 = legacy export
            }
        }.getOrNull()
    }

    fun crc(s: String): String =
        CRC32().apply { update(s.toByteArray(Charsets.UTF_8)) }.value.toString(16).padStart(8, '0')

    companion object {
        const val SLOT = "slot"
        const val PREV = "slot.prev"
        const val SCHEMA = 1
    }
}

val SaveJson = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
    isLenient = true
}
