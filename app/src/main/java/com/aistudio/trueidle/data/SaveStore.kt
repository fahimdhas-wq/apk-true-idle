package com.aistudio.trueidle.data

import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption

interface SaveStore {
    fun read(name: String): ByteArray?
    fun write(name: String, bytes: ByteArray) // atomic

    /** Removes a slot. Default no-op so lightweight in-memory stores do not have to implement it. */
    fun delete(name: String) = Unit
}

/** Store used when only the pure codec is needed (export/import, tests). */
object NoOpSaveStore : SaveStore {
    override fun read(name: String): ByteArray? = null
    override fun write(name: String, bytes: ByteArray) = Unit
    override fun delete(name: String) = Unit
}

class FileSaveStore(private val dir: File) : SaveStore {
    override fun read(name: String): ByteArray? =
        File(dir, name).takeIf { it.isFile }?.readBytes()

    override fun write(name: String, bytes: ByteArray) {
        dir.mkdirs()
        val tmp = File(dir, "$name.tmp")
        FileOutputStream(tmp).use {
            it.write(bytes)
            it.fd.sync()
        }
        Files.move(
            tmp.toPath(),
            File(dir, name).toPath(),
            StandardCopyOption.ATOMIC_MOVE,
            StandardCopyOption.REPLACE_EXISTING
        )
    }

    override fun delete(name: String) {
        File(dir, name).delete()
    }
}
