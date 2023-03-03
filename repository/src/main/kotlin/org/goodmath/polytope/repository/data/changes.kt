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
import org.goodmath.polytope.repository.Config
import org.goodmath.polytope.repository.Repository
import org.litote.kmongo.Id
import java.time.Instant

data class Change(
    val _id: Id<Change>,
    val project: String,
    val name: String,
    val history: String,
    val basis: ProjectVersionSpecifier,
    val description: String,
    val timestamp: Instant,
    val baseline: Id<ArtifactVersion>,
    val steps: List<Id<ChangeStep>>,
    val is_open: Boolean)

data class ArtifactChange(
    val id: Id<ChangeStep>,
    val versionIdBefore: Id<ArtifactVersion>?,
    val versionIdAfter: Id<ArtifactVersion>?)


data class ChangeStep(
    val _id: Id<ChangeStep>,
    val changeId: Id<Change>,
    val creator: String,
    val description: String,
    val basis: ProjectVersionSpecifier,
    val baselineVersion: Id<ArtifactVersion>,
    val artifactChanges: List<ArtifactChange>,
    val timestamp: Instant)

class Changes(val db: MongoDatabase, val repos: Repository) {
    val changes = db.getCollection("changes", Change::class.java)
    val steps = db.getCollection("changesteps", ChangeStep::class.java)

    fun withAuth(auth: AuthenticatedUser): AuthenticatedChanges {
        return AuthenticatedChanges(auth)
    }

    class AuthenticatedChanges(val auth: AuthenticatedUser) {
        fun retrieveChange(id: Id<Change>): Change {
            TODO()
        }

        fun saveChange(change: Change) {
            TODO()
        }

        fun close(project: String, change: Id<Change>)  {
            TODO()
        }

        fun isOpen(project: String, change: Id<Change>): Boolean {
            TODO()
        }

        fun saveChangestep(project: String, step: ChangeStep) {
            TODO()
        }

        fun retrieveStep(project: String, stepId: Id<ChangeStep>): ChangeStep {
            TODO()
        }

        fun listSteps(project: String, changeId: Id<Change>):List<ChangeStep> {
            TODO()
        }
    }

    companion object {
        fun initializeStorage(cfg: Config, db: MongoDatabase) {

        }
    }
}
