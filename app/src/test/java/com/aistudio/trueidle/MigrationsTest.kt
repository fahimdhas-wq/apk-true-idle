package com.aistudio.trueidle

import com.aistudio.trueidle.core.GameCaps
import com.aistudio.trueidle.data.Migrations
import com.aistudio.trueidle.data.SaveJson
import com.aistudio.trueidle.model.GameState
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MigrationsTest {

    /** Reshapes a current save into the legacy layout: old section keys, no baked atk/def. */
    private fun legacyRoot(from: GameState): JsonObject {
        val root = SaveJson.encodeToJsonElement(GameState.serializer(), from).jsonObject
        val renamed = LinkedHashMap<String, JsonElement>(root)
        val renames = mapOf("harvest" to "gathering", "armory" to "crafting", "essence" to "sanctified")
        for ((newKey, oldKey) in renames) {
            renamed[oldKey] = renamed.remove(newKey)!!
        }
        renamed["inventory"] = JsonArray((renamed["inventory"] as JsonArray).map { element ->
            JsonObject(LinkedHashMap(element.jsonObject).apply { remove("atk"); remove("def") })
        })
        // Three fields the pre-1.0 model still carried and kept writing.
        renamed["lastSaveTimestamp"] = JsonPrimitive(1234L)
        renamed["totalKills"] = JsonPrimitive(11)
        renamed["rank"] = JsonPrimitive(3)
        return JsonObject(renamed)
    }

    @Test
    fun testRenamedSectionsAndEquipmentStatsSurviveMigration() {
        val original = GameState()
        val migrated = Migrations.apply(legacyRoot(original), 0, SaveJson)

        // The gathering/crafting/sanctified rename used to drop all of this data silently.
        assertEquals(original.harvest, migrated.harvest)
        assertEquals(original.armory, migrated.armory)
        assertEquals(original.essence, migrated.essence)
        // Rings and amulets used to fall through to a zero bonus; weapons/armor to the regex.
        assertEquals(original.inventory, migrated.inventory)
    }

    @Test
    fun testMigrationRaisesNextIdAboveTheReservedFloor() {
        val original = GameState().copy(meta = GameState().meta.copy(nextId = 3L))
        val migrated = Migrations.apply(legacyRoot(original), 0, SaveJson)
        assertTrue(
            "nextId must clear the ids the default save already uses",
            migrated.meta.nextId >= GameCaps.RESERVED_ID_FLOOR
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun testFutureSchemaIsRejectedRatherThanDowngraded() {
        val root = SaveJson.encodeToJsonElement(GameState.serializer(), GameState()).jsonObject
        Migrations.apply(root, 7, SaveJson)
    }
}
