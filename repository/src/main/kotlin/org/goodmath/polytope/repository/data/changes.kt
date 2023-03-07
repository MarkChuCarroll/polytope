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

import kotlinx.serialization.Serializable
import maryk.rocksdb.ColumnFamilyHandle
import maryk.rocksdb.RocksDB
import org.goodmath.polytope.PolytopeException
import org.goodmath.polytope.repository.Repository
import org.goodmath.polytope.repository.util.*
import java.time.Instant


@Serializable
data class Change(
    val id: Id<Change>,
    val project: String,
    val name: String,
    val history: String,
    val basis: ProjectVersionSpecifier,
    val description: String,
    val timestamp: Long,
    val baseline: Id<ArtifactVersion>,
    val steps: List<Id<ChangeStep>>,
    val isOpen: Boolean)

@Serializable
data class ArtifactChange(
    val id: Id<ChangeStep>,
    val versionIdBefore: Id<ArtifactVersion>?,
    val versionIdAfter: Id<ArtifactVersion>?)

@Serializable
data class ChangeStep(
    val id: Id<ChangeStep>,
    val changeId: Id<Change>,
    val creator: String,
    val description: String,
    val basis: ProjectVersionSpecifier,
    val baselineVersion: Id<ArtifactVersion>,
    val artifactChanges: List<ArtifactChange>,
    val timestamp: Long)

class Changes(val db: RocksDB, val changesColumn: ColumnFamilyHandle,
              val stepsColumn: ColumnFamilyHandle,
              val repos: Repository) {

    fun withAuth(auth: AuthenticatedUser): AuthenticatedChanges {
        return AuthenticatedChanges(auth)
    }

    inner class AuthenticatedChanges(val auth: AuthenticatedUser) {
        fun retrieveChange(project: String, id: Id<Change>): Change {
            repos.users.validatePermissions(auth, Action.Read, project)
            val c = db.getTyped<Change>(changesColumn, id) ?:
                throw PolytopeException(PolytopeException.Kind.NotFound,
                    "Change $id not found")
            if (c.project != project) {
                throw PolytopeException(PolytopeException.Kind.InvalidParameter,
                    "Change $id doesn't belong to project $project")
            }
            return c
        }

        fun createChange(
            projectName: String,
            changeName: String,
            history: String,
            basis: ProjectVersionSpecifier,
            description: String): Change {
            repos.users.validatePermissions(auth, Action.Write, projectName)
            val project = repos.projects.withAuth(auth).retrieveProject(projectName)
            val change = Change(id=newId<Change>("change"),
                project=projectName,
                name=changeName,
                history=history,
                baseline = project.baseline,
                basis = basis,
                description = description,
                steps = listOf(),
                isOpen = true,
                timestamp = Instant.now().toEpochMilli())
            db.putTyped(changesColumn, change.id, change)
            return change
        }

        fun close(project: String, changeId: Id<Change>)  {
            repos.users.validatePermissions(auth, Action.Write, project)
            val change = retrieveChange(project, changeId)
            db.putTyped(changesColumn, changeId, change)
        }

        fun isOpen(project: String, changeId: Id<Change>): Boolean {
            repos.users.validatePermissions(auth, Action.Write, project)
            val change = retrieveChange(project, changeId)
            return change.isOpen
        }

        fun saveChangestep(project: String,
                           change: Id<Change>,
                           artifactChanges: List<ArtifactChange>,
                           description: String,
                           basis: ProjectVersionSpecifier,
                           baselineVersion: Id<ArtifactVersion>): ChangeStep {
            repos.users.validatePermissions(auth, Action.Write, project)
            val step = ChangeStep(
                id = newId<ChangeStep>("cs"),
                changeId = change,
                artifactChanges = artifactChanges,
                basis = basis,
                baselineVersion = baselineVersion,
                creator = auth.userId,
                description = description,
                timestamp = Instant.now().toEpochMilli())
            db.putTyped(stepsColumn, step.id, step)
            return step
        }

        fun retrieveStep(project: String, stepId: Id<ChangeStep>): ChangeStep {
            repos.users.validatePermissions(auth, Action.Read, project)
            return db.getTyped<ChangeStep>(stepsColumn, stepId)
                ?: throw PolytopeException(PolytopeException.Kind.NotFound,
                    "Change step not found")
        }

        fun listSteps(project: String, changeId: Id<Change>):List<ChangeStep> {
            repos.users.validatePermissions(auth, Action.Read, project)
            return db.list(stepsColumn) { it.changeId == changeId  }
        }

        fun listChanges(project: String, historyName: String):List<Change> {
            repos.users.validatePermissions(auth, Action.Read, project)
            return db.list(changesColumn) {
                it.history == historyName
            }
        }
    }
}
