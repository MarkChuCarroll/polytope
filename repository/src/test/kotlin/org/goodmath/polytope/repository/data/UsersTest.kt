package org.goodmath.polytope.repository.data

import com.mongodb.client.MongoCollection
import com.mongodb.client.MongoDatabase
import io.mockk.every
import io.mockk.mockk
import net.bytebuddy.matcher.ElementMatchers.any
import org.goodmath.polytope.repository.Repository
import org.junit.jupiter.api.Test

import org.junit.jupiter.api.Assertions.*

class UsersTest {
    @Test
    fun testValidateSingleReadPermission() {
        val db = mockk<MongoDatabase>()
        val col = mockk<MongoCollection<User>>()
        every { db invoke "getCollection" withArguments listOf("users", User::class.java) } returns col
        val repo = mockk<Repository>()
        val users = Users(db, repo)
        val perm = Permission(Action.Read, Realm.Project, "Foo")
        val auth = AuthToken("markcc", listOf(perm))
        assertTrue(users.validatePermission(auth, Action.Read, "Foo"))
        assertFalse(users.validatePermission(auth, Action.Write, "Foo"))
        assertFalse(users.validatePermission(auth, Action.Admin, "Foo"))
    }

    @Test
    fun testValidateSingleWritePermission() {
        val db = mockk<MongoDatabase>()
        val col = mockk<MongoCollection<User>>()
        every { db invoke "getCollection" withArguments listOf("users", User::class.java) } returns col
        val repo = mockk<Repository>()
        val users = Users(db, repo)
        val perm = Permission(Action.Write, Realm.Project, "Foo")
        val auth = AuthToken("markcc", listOf(perm))
        assertTrue(users.validatePermission(auth, Action.Read, "Foo"))
        assertTrue(users.validatePermission(auth, Action.Write, "Foo"))
        assertFalse(users.validatePermission(auth, Action.Admin, "Foo"))
    }

    @Test
    fun testValidateSingleAdminPermission() {
        val db = mockk<MongoDatabase>()
        val col = mockk<MongoCollection<User>>()
        every { db invoke "getCollection" withArguments listOf("users", User::class.java) } returns col
        val repo = mockk<Repository>()
        val users = Users(db, repo)
        val perm = Permission(Action.Admin, Realm.Project, "Foo")
        val auth = AuthToken("markcc", listOf(perm))
        assertTrue(users.validatePermission(auth, Action.Read, "Foo"))
        assertTrue(users.validatePermission(auth, Action.Write, "Foo"))
        assertTrue(users.validatePermission(auth, Action.Admin, "Foo"))
    }

    @Test
    fun testValidateMultiplePermissions() {
        val db = mockk<MongoDatabase>()
        val col = mockk<MongoCollection<User>>()
        every { db invoke "getCollection" withArguments listOf("users", User::class.java) } returns col
        val repo = mockk<Repository>()
        val users = Users(db, repo)
        val perms = listOf(Permission(Action.Admin, Realm.Project, "Foo"),
            Permission(Action.Read, Realm.Project, "bar"),
            Permission(Action.Write, Realm.Project, "twip"))
        val auth = AuthToken("markcc", perms)
        assertTrue(users.validatePermission(auth, Action.Read, "Foo"))
        assertTrue(users.validatePermission(auth, Action.Write, "Foo"))
        assertTrue(users.validatePermission(auth, Action.Read, "twip"))
        assertTrue(users.validatePermission(auth, Action.Write, "twip"))
        assertTrue(users.validatePermission(auth, Action.Admin, "Foo"))
        assertFalse(users.validatePermission(auth, Action.Write, "bar"))
        assertFalse(users.validatePermission(auth, Action.Admin, "twip"))
    }

    @Test
    fun testValidateAdminUniversePermission() {
        val db = mockk<MongoDatabase>()
        val col = mockk<MongoCollection<User>>()
        every { db invoke "getCollection" withArguments listOf("users", User::class.java) } returns col
        val repo = mockk<Repository>()
        val users = Users(db, repo)
        val perms = listOf(Permission(Action.Read, Realm.Project, "Foo"),
            Permission(Action.Read, Realm.Project, "bar"),
            Permission(Action.Read, Realm.Project, "twip"),
            Permission(Action.Admin, Realm.Universe, null))
        val auth = AuthToken("markcc", perms)
        assertTrue(users.validatePermission(auth, Action.Read, "Foo"))
        assertTrue(users.validatePermission(auth, Action.Write, "Foo"))
        assertTrue(users.validatePermission(auth, Action.Read, "twip"))
        assertTrue(users.validatePermission(auth, Action.Write, "twip"))
        assertTrue(users.validatePermission(auth, Action.Admin, "Foo"))
        assertTrue(users.validatePermission(auth, Action.Write, "bar"))
        assertTrue(users.validatePermission(auth, Action.Admin, "twip"))
    }
}