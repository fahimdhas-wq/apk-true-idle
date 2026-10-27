package com.aistudio.trueidle

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArchitectureTest {

    private val base: File
        get() {
            val inApp = File("src/main/java/com/aistudio/trueidle")
            return if (inApp.exists()) inApp else File("app/src/main/java/com/aistudio/trueidle")
        }

    private fun sourceFiles(directory: File): List<File> =
        if (directory.exists()) directory.walkTopDown().filter { it.extension == "kt" }.toList()
        else emptyList()

    private fun scan(directory: File, predicate: (String) -> Boolean): List<String> {
        val violations = mutableListOf<String>()
        sourceFiles(directory).forEach { file ->
            file.readLines().forEachIndexed { index, line ->
                val trimmed = line.trim()
                // Prose may *name* a banned API ("without importing android.util.Log"); only real
                // code references are violations.
                if (isComment(trimmed)) return@forEachIndexed
                if (predicate(trimmed)) violations += "${file.name}:${index + 1} -> $trimmed"
            }
        }
        return violations
    }

    private fun isComment(trimmed: String): Boolean =
        trimmed.startsWith("//") || trimmed.startsWith("*") ||
            trimmed.startsWith("/*") || trimmed.startsWith("*/")

    /**
     * `core`, `engine` and `data` are pure Kotlin: no Android or AndroidX dependency at all. The old
     * check only looked for `import android.`, so `androidx.*` and fully-qualified references slipped
     * through.
     */
    @Test
    fun testPureModulesHaveNoAndroidDependencies() {
        val violations = mutableListOf<String>()
        for (name in listOf("core", "engine", "data")) {
            violations += scan(File(base, name)) { line ->
                // Catches imports *and* fully-qualified references such as android.util.Log.d(...).
                line.startsWith("import android.") ||
                    line.startsWith("import androidx.") ||
                    line.contains("android.") ||
                    line.contains("androidx.")
            }
        }
        assertTrue(
            "Found prohibited Android imports in pure modules:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    /** The UI layer renders state; it must not reach into the engine or the persistence layer. */
    @Test
    fun testUiLayerDoesNotImportEngineOrData() {
        val violations = scan(File(base, "ui")) { line ->
            line.startsWith("import com.aistudio.trueidle.engine.") ||
                line.startsWith("import com.aistudio.trueidle.data.") ||
                line.contains("com.aistudio.trueidle.engine.") ||
                line.contains("com.aistudio.trueidle.data.")
        }
        assertTrue(
            "Found UI -> engine/data imports:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }
}
