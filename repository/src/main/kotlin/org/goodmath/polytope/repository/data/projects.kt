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
import org.litote.kmongo.Id
import java.time.Instant

data class Project(
    val _id: Id<Project>,
    val name: String,
    val creator: String,
    val timestamp: Instant,
    val description: String
)

class Projects(val db: MongoDatabase, val repos: Repository) {

    fun withAuth(a: AuthenticatedUser): AuthenticatedProjects {
        return AuthenticatedProjects(a)
    }

    class AuthenticatedProjects(val auth: AuthenticatedUser) {
        fun create(project: Project) {
            TODO()
        }

        fun retrieveProject(name: String): Project {
            TODO()
        }

        fun update(project: Project) {
            TODO()
        }

        fun list(): List<Project> {
            TODO()
        }
    }

    companion object {
        fun initializeStorage(cfg: Config, db: MongoDatabase) {

        }
    }
}