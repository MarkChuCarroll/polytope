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

import maryk.rocksdb.ColumnFamilyHandle
import maryk.rocksdb.RocksDB
import org.goodmath.polytope.PolytopeException
import org.goodmath.polytope.repository.Repository
import org.goodmath.polytope.repository.util.*
import java.time.Instant
import kotlin.text.Charsets.UTF_8

data class Project(
    val name: String,
    val creator: String,
    val timestamp: Instant,
    val description: String
)

class Projects(val db: RocksDB,
               val projectsColumn: ColumnFamilyHandle,
               val repos: Repository) {

    fun withAuth(a: AuthenticatedUser): AuthenticatedProjects {
        return AuthenticatedProjects(a)
    }

    inner class AuthenticatedProjects(val auth: AuthenticatedUser) {
        fun create(projectName: String, description: String): Project {
            val project = Project(
                projectName,
                auth.userId,
                Instant.now(),
                description)
            db.putTyped(projectsColumn, projectName, project)
            return project
        }

        fun retrieveProject(name: String): Project {
            repos.users.validatePermissions(auth, Action.Read, name)
            return db.getTyped<Project>(projectsColumn, name) ?:
                   throw PolytopeException(PolytopeException.Kind.NotFound,
                        "Project '$name' not found")
        }

        fun update(name: String, project: Project) {
            repos.users.validatePermissions(auth, Action.Write, name)
            if (name != project.name) {
                throw PolytopeException(PolytopeException.Kind.InvalidParameter,
                    "Project name must match project being saved")
            }
            db.putTyped(projectsColumn, name, project)
        }

        fun list(): List<Project> {
            repos.users.validatePermissions(auth, Action.Read, null)
            return db.list(projectsColumn)
        }

        fun exists(projectName: String): Boolean {
            val p = db.getTyped<Project>(projectsColumn, projectName)
            return p == null
        }

    }
}