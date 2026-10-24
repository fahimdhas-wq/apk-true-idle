package com.aistudio.trueidle

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class ArchitectureTest {

    @Test
    fun testNoAndroidImportsInCoreEngineData() {
        val rootDirs = listOf("core", "engine", "data")
        val baseDir = File("src/main/java/com/aistudio/trueidle")
        val effectiveBase = if (baseDir.exists()) baseDir else File("app/src/main/java/com/aistudio/trueidle")

        val violations = mutableListOf<String>()

        for (dirName in rootDirs) {
            val dir = File(effectiveBase, dirName)
            if (!dir.exists()) continue

            dir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
                file.readLines().forEachIndexed { lineIdx, line ->
                    val trimmed = line.trim()
                    if (trimmed.startsWith("import android.")) {
                        violations.add("${file.name}:${lineIdx + 1} -> $trimmed")
                    }
                }
            }
        }

        assertTrue("Found prohibited android.* imports in pure modules:\n" + violations.joinToString("\n"), violations.isEmpty())
    }
}
