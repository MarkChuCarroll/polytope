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

data class Artifact(
    val id: ObjectId,
    val artifactType: String,
    val timestamp: Instant,
    val creator: String,
    val project: String,
    val metadata: Map<String, String>
)

data class ArtifactVersion(
    val id: VersionId,
    val artifactType: String,
    val timestamp: Instant,
    val creator: String,
    val contentId: ContentId,
    val metadata: Map<String, String>,
    val open: Boolean
)

class Artifacts(val db: MongoDatabase, val repos: Repository) {
    val artifactsCollection = db.getCollection("artifacts", Artifact::class.java)
    val versionsCollection = db.getCollection("versions", ArtifactVersion::class.java)

    fun withAuth(auth: AuthToken): AuthenticatedArtifacts {
        return AuthenticatedArtifacts(auth)
    }


    class AuthenticatedArtifacts(val auth: AuthToken) {
        fun retrieveArtifact(project: String, id: ArtifactId): Artifact {
            TODO()
        }

        fun retrieveVersion(project: String, versionId: VersionId): ArtifactVersion {
            TODO()
        }

        fun storeArtifact(artifact: Artifact) {
            TODO()
        }

        fun createWorkingVersion(base_version: VersionId): ArtifactVersion {
            TODO()
        }

        fun updatWorkingVersion(version: ArtifactVersion): ArtifactVersion {
            TODO()
        }

        fun commitWorkingVersion(version: VersionId) {
            TODO()
        }

        fun abortWorkingVersion(version: VersionId) {
            TODO()
        }

        fun versionIsCommitted(version: VersionId): Boolean {
            TODO()
        }
    }

    companion object {
        fun initializeStorage(cfg: Config) {

        }
    }


    }