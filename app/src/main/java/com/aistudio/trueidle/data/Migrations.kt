package com.aistudio.trueidle.data

import com.aistudio.trueidle.model.GameState
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

/**
 * Stepwise save migrations. Each step operates on the raw [JsonObject] so a structurally old save
 * can be reshaped *before* it is ever decoded into the current [GameState].
 */
object Migrations {
    const val CURRENT_SCHEMA = 1

    private val OLD_TO_NEW_KEYS = mapOf(
        "gathering" to "harvest",
        "crafting" to "armory",
        "sanctified" to "essence"
    )

    fun apply(root: JsonObject, schema: Int, json: Json): GameState {
        var obj = root
        var current = schema
        if (current < 1) {
            obj = migrateV0ToV1(obj)
            current = 1
        }
        require(current == CURRENT_SCHEMA) { "no migration path from schema $schema" }
        return json.decodeFromJsonElement(GameState.serializer(), obj)
    }

    /** schema 0 -> 1: discipline renames + recomputed crafted-equipment stats. */
    private fun migrateV0ToV1(root: JsonObject): JsonObject {
        val out = LinkedHashMap<String, kotlinx.serialization.json.JsonElement>(root)

        // 1. The gathering/crafting/sanctified -> harvest/armory/essence rename had no migration,
        //    so guards against rename loss are applied here.
        for ((oldKey, newKey) in OLD_TO_NEW_KEYS) {
            val oldValue = out.remove(oldKey) ?: continue
            if (!out.containsKey(newKey)) out[newKey] = oldValue
        }

        // 2. Recompute ATK/DEF for crafted equipment from the displayed stat text.
        val inventory = (out["inventory"] as? JsonArray)?.map { element ->
            val item = element.jsonObject
            if (item["category"]?.jsonPrimitive?.contentOrNull() != "Equipment") return@map element
            val slot = item["slot"]?.jsonPrimitive?.contentOrNull() ?: ""
            val tier = item["tier"]?.jsonPrimitive?.intOrNull ?: 1
            val statDesc = item["statDesc"]?.jsonPrimitive?.contentOrNull() ?: ""
            // Only weapons/armor carry a plain "+N Damage"/"+N Defense" description. Rings and
            // amulets used to fall through to the model default and lose their bonus entirely.
            val parsed = STAT_VALUE.find(statDesc)?.groupValues?.get(1)?.toDoubleOrNull()
            val atk = when (slot) {
                "Weapon" -> parsed ?: (tier * 12.0)
                "Ring" -> item["atk"]?.jsonPrimitive?.doubleOrNull ?: (tier * 3.0)
                else -> item["atk"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            }
            val def = when (slot) {
                "Armor" -> parsed ?: (tier * 8.0)
                "Amulet" -> item["def"]?.jsonPrimitive?.doubleOrNull ?: (tier * 3.0)
                else -> item["def"]?.jsonPrimitive?.doubleOrNull ?: 0.0
            }
            JsonObject(
                LinkedHashMap<String, kotlinx.serialization.json.JsonElement>(item).apply {
                    this["atk"] = JsonPrimitive(atk)
                    this["def"] = JsonPrimitive(def)
                }
            )
        }?.let { JsonArray(it) }
        if (inventory != null) out["inventory"] = inventory

        // 3. Ids: scan every id-bearing collection, not just logs.
        val meta = (out["meta"] as? JsonObject)?.let { LinkedHashMap(it) } ?: LinkedHashMap()
        val nextId = maxOf(
            meta["nextId"]?.jsonPrimitive?.contentOrNull()?.toLongOrNull() ?: 0L,
            highestNumericSuffix(out),
            highestLogId(out) + 1L,
            100L
        )
        meta["nextId"] = JsonPrimitive(nextId)
        out["meta"] = JsonObject(meta)

        // 4. `lastSaveTimestamp` has been removed from the model; fold it into meta.savedWallMs.
        out.remove("lastSaveTimestamp")
        out.remove("totalKills")
        out.remove("rank")

        return JsonObject(out)
    }

    private fun highestLogId(root: Map<String, kotlinx.serialization.json.JsonElement>): Long =
        (root["logs"] as? JsonArray)
            ?.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull()?.toLongOrNull() }
            ?.maxOrNull() ?: 0L

    /** Highest trailing `_<number>` across inventory ids, beast ids and minion ids. */
    private fun highestNumericSuffix(root: Map<String, kotlinx.serialization.json.JsonElement>): Long {
        var max = 0L
        fun scanIds(array: JsonArray?) {
            array?.forEach { element ->
                val id = element.jsonObject["id"]?.jsonPrimitive?.contentOrNull() ?: return@forEach
                val suffix = SUFFIX.find(id)?.groupValues?.get(1)?.toLongOrNull() ?: return@forEach
                if (suffix > max) max = suffix
            }
        }
        scanIds(root["inventory"] as? JsonArray)
        scanIds((root["beasts"] as? JsonObject)?.get("beasts") as? JsonArray)
        scanIds((root["necro"] as? JsonObject)?.get("army") as? JsonArray)
        return max
    }

    private val STAT_VALUE = Regex("""\+(\d+(?:\.\d+)?)""")
    private val SUFFIX = Regex("""[_-](\d+)$""")
}

private fun JsonPrimitive.contentOrNull(): String? = content
