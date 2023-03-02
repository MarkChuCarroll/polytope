package org.goodmath.polytope.repository.storage

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import java.nio.charset.Charset
import java.nio.file.Path

class SimpleFileStorageTest {

    lateinit var storage: SimpleFileStorage

    @BeforeEach
    fun setupStorage() {

        val path = Path.of(".", "storage_test")
        System.err.println("Setup $path")
        storage = SimpleFileStorage(path)
    }

    @AfterEach
    fun cleanupStorage() {
        val path = Path.of(".", "storage_test")
        path.toFile().deleteRecursively()
    }

    @Test
    fun testReadAndWriteBlob() {
        val b = storage.storeBlob("abcdefg".toByteArray())
        val c = storage.storeTransientBlob("0123456789".toByteArray())

        assertEquals("abcdefg", storage.retrieveBlob(b).toString(Charset.defaultCharset()))
        assertEquals("0123456789", storage.retrieveBlob(c).toString(Charset.defaultCharset()))
        assertDoesNotThrow( { storage.deleteTransientBlob(c) })

    }
}