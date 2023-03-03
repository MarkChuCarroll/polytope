/*
 * Copyright 2023 Mark C. Chu-Carroll
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.goodmath.polytope.repository.data

import com.mongodb.client.MongoDatabase
import org.bson.types.ObjectId
import org.goodmath.polytope.org.goodmath.polytope.PolytopeException
import org.goodmath.polytope.repository.Config
import org.goodmath.polytope.repository.Repository
import org.litote.kmongo.eq
import org.litote.kmongo.findOne
import org.litote.kmongo.match
import org.litote.kmongo.save
import java.security.MessageDigest
import java.time.Instant
import kotlin.text.Charsets.UTF_8




// This is garbage.
// This information should be here - plus some secret that allows
// us to check whether the token is valid.
data class AuthToken(val userId: String,
                     val permissions: List<Permission>) {

}

enum class Action {
    Read, Write, Admin, Delete
}
enum class Realm {
    Project, Universe
}
data class Permission(val action: Action, val realm: Realm, val project: String?)

data class User(
    val id: ObjectId,
    val username: String,
    val fullName: String,
    val permissions: List<Permission>,
    val email: String,
    val password: String,
    val timestamp: Instant,
    val active: Boolean
)

class Users(db: MongoDatabase, repos: Repository) {
    val users = db.getCollection("users", User::class.java)

    fun validatePermission(auth: AuthToken, action: Action, project: String): Boolean {
        return (auth.permissions.any {
            (it.realm == Realm.Universe && it.action >= action) ||
                    (it.realm == Realm.Project && project == it.project &&
                            it.action >= action)
        })
    }

    fun throwWithoutPermission(auth: AuthToken, action: Action,project: String) {
        if (!validatePermission(auth, action, project)) {
            throw PolytopeException(PolytopeException.Kind.Permission,
                "User permission denied")
        }
     }

    private fun saltedHash(username: String, pw: String): String {
        // This is trash, but it's a placeholder fol now.
        val salt = (username.chars().reduce { x, y ->
            (x * 37 + y) / 211
        }).toString()
        val bytes = MessageDigest.getInstance("MD5").digest((salt + pw).toByteArray(UTF_8))
        return bytes.joinToString(separator = "") { byte ->
            "%02x".format(byte)
        }
    }

    fun authenticate(username: String,
                     password: String): AuthToken {
        val user = users.findOne { match(User::username eq username) }
            ?: throw PolytopeException(PolytopeException.Kind.NotFound,
                "User ${username} not found")
        val hashedFromUser = saltedHash(username, password)
        if (hashedFromUser != user.password) {
            throw PolytopeException(
                PolytopeException.Kind.Authentication,
                "Authentication failed")
        }
        return AuthToken(username, user.permissions)


    }

    fun withAuth(a: AuthToken): AuthenticatedUsers {
        return AuthenticatedUsers(a)
    }

    inner class AuthenticatedUsers(val auth: AuthToken) {
        fun retrieveUser(username: String): User {
            val result = users.findOne(match(User::username eq username))
                ?: throw PolytopeException(PolytopeException.Kind.NotFound,
                    "User $username not found")
            return result.copy(password="<redacted>")
        }

        fun create(user: User): User {
            if (exists(user.username)) {
                throw PolytopeException(PolytopeException.Kind.InvalidParameter,
                    "User with username '${user.username} already exists")
            }
            val encodedPassword = saltedHash(user.username,
                user.password)
            val saltedUser = user.copy(password = encodedPassword)
            users.save(saltedUser)
            return user.copy(password = "<redacted>")
        }

        fun exists(username: String): Boolean {
            val u = users.findOne(match(User::username eq username))
            return u == null
        }

        fun grantPermissions(username: String, perms: List<Permission>) {
            val u = retrieveUser(username)
            val newPermissions = (u.permissions + perms).distinct()
            users.save(u.copy(permissions = newPermissions))
        }

        fun revokePermission(username: String, perms: List<Permission>) {
            val u = retrieveUser(username)
            val newPermissions = u.permissions - perms
            users.save(u.copy(permissions = newPermissions))
        }

        fun list(): List<User> {
            return users.find().toList().map { it.copy(password="<redacted>") }
        }

    }

    companion object {
        fun initializeStorage(cfg: Config) {

        }
    }

}