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
import org.goodmath.polytope.repository.Config
import org.goodmath.polytope.repository.Repository
import java.time.Instant


data class AuthToken(val userId: String,
                     val permissions: List<Permission>) {

}

data class Permission(val kind: Kind, val realm: String) {
    enum class Kind { Read, Write, Admin, Delete, All }
}

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

    fun authenticate(username: String, hashedPassword: String): AuthToken {
        TODO()
    }

    fun withAuth(a: AuthToken): AuthenticatedUsers {
        return AuthenticatedUsers(a)
    }

    class AuthenticatedUsers(val auth: AuthToken) {
        fun retrieveUser(username: String): User {
            TODO()
        }

        fun create(user: User) {
            TODO()
        }

        fun grantPermissions(username: String, perms: List<Permission>) {
            TODO()
        }

        fun revokePermission(username: String, perms: List<Permission>) {
            TODO()
        }

        fun list(): List<User> {
            TODO()
        }

        fun update(user: User) {
            TODO()
        }
    }

    companion object {
        fun initializeStorage(cfg: Config) {

        }
    }

}