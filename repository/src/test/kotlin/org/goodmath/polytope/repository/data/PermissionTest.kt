package org.goodmath.polytope.repository.data

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import io.mockk.every
import io.mockk.mockk
import maryk.rocksdb.ColumnFamilyHandle
import maryk.rocksdb.RocksDB
import org.goodmath.polytope.repository.Repository
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class PermissionTest {
    @Test
    fun testValidateSingleReadPermission() {
        val db = mockk<RocksDB>()
        val cf = mockk<ColumnFamilyHandle>()
        val repo = mockk<Repository>()
        val users = Users(db, cf, repo)
        val perm = Permission.fromString("project(Read:foo)")!!
        val auth = AuthenticatedUser("markcc", listOf(perm))
        assertTrue(users.permits(auth, Action.Read, "foo"))
        assertFalse(users.permits(auth, Action.Write, "foo"))
        assertFalse(users.permits(auth, Action.Admin, "foo"))
        assertFalse(users.permits(auth, Action.Delete, null))
    }

    @Test
    fun testValidateSingleWritePermission() {
        val db = mockk<RocksDB>()
        val cf = mockk<ColumnFamilyHandle>()
        val repo = mockk<Repository>()
        val users = Users(db, cf, repo)
        val perm = Permission.fromString("project(Write:foo)")!!
        val auth = AuthenticatedUser("markcc", listOf(perm))
        assertTrue(users.permits(auth, Action.Read, "foo"))
        assertTrue(users.permits(auth, Action.Write, "foo"))
        assertFalse(users.permits(auth, Action.Admin, "foo"))
        assertFalse(users.permits(auth, Action.Delete, null))
    }

    @Test
    fun testValidateSingleAdminPermission() {
        val db = mockk<RocksDB>()
        val cf = mockk<ColumnFamilyHandle>()
        val repo = mockk<Repository>()
        val users = Users(db, cf, repo)
        val perm = Permission.fromString("project(Admin:foo)")!!

        val auth = AuthenticatedUser("markcc", listOf(perm))
        assertTrue(users.permits(auth, Action.Read, "foo"))
        assertTrue(users.permits(auth, Action.Write, "foo"))
        assertTrue(users.permits(auth, Action.Admin, "foo"))
        assertFalse(users.permits(auth, Action.Delete, null))
    }

    @Test
    fun testValidateMultiplePermissions() {
        val db = mockk<RocksDB>()
        val cf = mockk<ColumnFamilyHandle>()
        val repo = mockk<Repository>()
        val users = Users(db, cf, repo)
        val perms = listOf(
            Permission.fromString("project(Admin:foo)")!!,
            Permission.fromString("project(Read:bar)")!!,
            Permission.fromString("project(Write:twip)")!!)
        val auth = AuthenticatedUser("markcc", perms)
        assertTrue(users.permits(auth, Action.Read, "foo"))
        assertTrue(users.permits(auth, Action.Write, "foo"))
        assertTrue(users.permits(auth, Action.Read, "twip"))
        assertTrue(users.permits(auth, Action.Write, "twip"))
        assertTrue(users.permits(auth, Action.Admin, "foo"))
        assertFalse(users.permits(auth, Action.Write, "bar"))
        assertFalse(users.permits(auth, Action.Admin, "twip"))
        assertFalse(users.permits(auth, Action.Delete, null))
    }

    @Test
    fun testValidateAdminUniversePermission() {
        val db = mockk<RocksDB>()
        val cf = mockk<ColumnFamilyHandle>()
        val repo = mockk<Repository>()
        val users = Users(db, cf, repo)
        val perms = listOf(
            Permission.fromString("project(Read:foo)")!!,
            Permission.fromString("global(Admin)")!!)
        val auth = AuthenticatedUser("markcc", perms)
        assertTrue(users.permits(auth, Action.Read, "Foo"))
        assertTrue(users.permits(auth, Action.Write, "Foo"))
        assertTrue(users.permits(auth, Action.Read, "twip"))
        assertTrue(users.permits(auth, Action.Write, "twip"))
        assertTrue(users.permits(auth, Action.Admin, "Foo"))
        assertTrue(users.permits(auth, Action.Write, "bar"))
        assertTrue(users.permits(auth, Action.Admin, "twip"))
        assertTrue(users.permits(auth, Action.Delete, null))
    }
}