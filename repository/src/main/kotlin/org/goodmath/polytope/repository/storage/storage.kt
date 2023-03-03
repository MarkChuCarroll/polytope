package org.goodmath.polytope.repository.storage

import org.goodmath.polytope.org.goodmath.polytope.PolytopeException
import java.io.FileInputStream
import java.io.FileOutputStream
import java.nio.file.Path
import java.util.*
import kotlin.io.path.div
import kotlin.io.path.exists


data class ContentId(
    val id: UUID,
    val category: String
)
interface Storage {
    fun storeBlob(content: ByteArray): ContentId
    fun retrieveBlob(id: ContentId): ByteArray

    fun storeTransientBlob(content: ByteArray): ContentId

    fun deleteTransientBlob(id: ContentId)
}


class SimpleFileStorage(val baseDir: Path): Storage {

    init {
        if (!baseDir.exists()) {
            System.err.println("Creating basedir $baseDir")
            if (!baseDir.toFile().mkdirs()) {
                throw PolytopeException(
                    PolytopeException.Kind.Internal,
                    "Unable to create storage directory"
                )
            }
        }
        val permDir = baseDir / "perm"
        if (!permDir.toFile().exists()) {
            if (!permDir.toFile().mkdir()) {
                throw PolytopeException(
                    PolytopeException.Kind.Internal,
                    "Unable to create perm storage"
                )
            }
        }
        val transDir = baseDir / "transient"
        if (!transDir.toFile().exists()) {
            if (!(baseDir / "transient").toFile().mkdir()) {
                throw PolytopeException(PolytopeException.Kind.Internal,
                    "Unable to create transient storage")
            }
        }
    }

    override fun storeBlob(content: ByteArray): ContentId {
        val id = ContentId(UUID.randomUUID(),
            "permanent")
        val path = baseDir / "perm" / id.id.toString()
        FileOutputStream(path.toFile()).use {out ->
            out.write(content)
        }
        return id
    }

    override fun retrieveBlob(id: ContentId): ByteArray {
        val path = when(id.category) {
            "transient" -> baseDir / "transient" / id.id.toString()
            "permanent" -> baseDir / "perm" / id.id.toString()
            else -> throw PolytopeException(PolytopeException.Kind.InvalidParameter,
                "Invalid storage category ${id.category}")
        }
        return FileInputStream(path.toFile()).use { i ->
            i.readAllBytes()
        }
    }

    override fun storeTransientBlob(content: ByteArray): ContentId {
        val id = ContentId(UUID.randomUUID(),
            "transient")
        val path = baseDir / "transient" / id.id.toString()
        FileOutputStream(path.toFile()).use {out ->
            out.write(content)
        }
        return id
    }

    override fun deleteTransientBlob(id: ContentId) {
        if (id.category != "transient") {
            throw PolytopeException(
                PolytopeException.Kind.InvalidParameter,
                "Cannot delete a non-transient blob"
            )
        }
        val path = baseDir / "transient" / id.id.toString()
        path.toFile().delete()
    }
}