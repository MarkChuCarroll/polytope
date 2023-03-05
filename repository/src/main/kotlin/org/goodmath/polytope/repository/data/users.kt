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

import maryk.rocksdb.RocksDB
import org.goodmath.polytope.PolytopeException
import org.goodmath.polytope.repository.Repository
import org.goodmath.polytope.repository.util.*
import org.rocksdb.ColumnFamilyHandle
import java.security.MessageDigest
import java.time.Instant
import kotlin.text.Charsets.UTF_8


// This is garbage.
// This information should be here - plus some secret that allows
// us to check whether the token is valid.
data class AuthenticatedUser(val userId: String,
                             val permissions: List<Permission>) {

}


data class User(
    val username: String,
    val fullName: String,
    val permissions: List<Permission>,
    val email: String,
    val password: String,
    val timestamp: Instant,
    val active: Boolean
)

class Users(val db: RocksDB, val usersColumn: ColumnFamilyHandle, val repos: Repository) {

    fun permits(auth: AuthenticatedUser, action: Action, project: String?): Boolean {
        return auth.permissions.any {
            if (project == null) {
                it.permitsRepositoryAction(action)
            } else {
                it.permitsProjectAction(action, project)
            }
        }
    }

    fun validatePermissions(auth: AuthenticatedUser, action: Action, project: String?) {
        if (!permits(auth, action, project)) {
            throw PolytopeException(
                PolytopeException.Kind.Permission,
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
                     password: String): AuthenticatedUser {
        val user = db.getTyped<User>(usersColumn, username)
            ?: throw PolytopeException(
                PolytopeException.Kind.NotFound,
                "User ${username} not found")
        val hashedFromUser = saltedHash(username, password)
        if (hashedFromUser != user.password) {
            throw PolytopeException(
                PolytopeException.Kind.Authentication,
                "Authentication failed")
        }
        return AuthenticatedUser(username, user.permissions)
    }

    fun withAuth(a: AuthenticatedUser): AuthenticatedUsers {
        return AuthenticatedUsers(a)
    }

    inner class AuthenticatedUsers(val auth: AuthenticatedUser) {
        fun retrieveUser(username: String): User {
            validatePermissions(auth, Action.Admin, null)
            val result = db.getTyped<User>(usersColumn, username)
                ?: throw PolytopeException(
                    PolytopeException.Kind.NotFound,
                    "User $username not found")
            return result.copy(password="<redacted>")
        }

        fun create(userName: String,
                   fullName: String,
                   email: String,
                   permissions: List<Permission>,
                   password: String): User {
            validatePermissions(auth, Action.Admin, null)

            if (exists(userName)) {
                throw PolytopeException(
                    PolytopeException.Kind.InvalidParameter,
                    "User with username '${userName} already exists")
            }

            val encodedPassword = saltedHash(userName,
                password)

            val user = User(
                userName,
                fullName,
                permissions,
                email,
                encodedPassword,
                Instant.now(),
                true)

            db.putTyped(usersColumn, user.username, user)
            return user.copy(password = "<redacted>")
        }

        fun exists(username: String): Boolean {
            validatePermissions(auth, Action.Admin, null)

            val u = db.getTyped<User>(username)
            return u == null
        }

        fun grantPermissions(username: String, perms: List<Permission>) {
            validatePermissions(auth, Action.Admin, null)
            val u = retrieveUser(username)
            val newPermissions = (u.permissions + perms).distinct()
            db.putTyped(username, u.copy(permissions = newPermissions))
        }

        fun revokePermission(username: String, perms: List<Permission>) {
            validatePermissions(auth, Action.Admin, null)
            val u = retrieveUser(username)
            val newPermissions = u.permissions - perms
            db.putTyped(username, u.copy(permissions = newPermissions))
        }

        fun list(): List<User> {
            validatePermissions(auth, Action.Admin, null)
            return db.list<User>(usersColumn)
        }
    }

}