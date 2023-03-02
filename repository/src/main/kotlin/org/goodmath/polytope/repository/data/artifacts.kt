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

    /**
     * Get an API that can be used to store and retrieve
     * artifacts and users, under the authorization and
     * privileges of an authenticated user.
     * @param auth an auth token provided by the Users api.
     * @return the artifacts API object.
     * @throws PolytopeException if the users permissions
     *   don't permit them to read or write artifacts in
     *   the repository.
     */
    fun withAuth(auth: AuthToken): AuthenticatedArtifacts {
        return AuthenticatedArtifacts(auth)
    }


    class AuthenticatedArtifacts(val auth: AuthToken) {

        /**
         * Retrieve an artifact from the database.
         * @param project the project containing the artifact.
         * @param id the artifact ID.
         * @return the full Artifact record.
         * @throws PolytopeError if the artifact isn't found, if the user
         *    doesn't have permission to read it, or if there's some internal
         *    error retrieving it.
         */
        fun retrieveArtifact(project: String, id: ArtifactId): Artifact {
            TODO()
        }

        /**
         * Retrieve a version of an artifact from the database.
         * @param project the project containing the artifact.
         * @param id the version ID.
         * @return the full ArtifactVersion record.
         * @throws PolytopeError if the version isn't found, if the user
         *    doesn't have permission to read it, or if there's some internal
         *    error retrieving it.
         */
        fun retrieveVersion(project: String, versionId: VersionId): ArtifactVersion {
            TODO()
        }

        /**
         * Store a new artifact from the database.
         * @param project the project containing the artifact.
         * @param artifact the new artifact to save.
         * @throws PolytopeError if the artifact already exists, if the user
         *    doesn't have permission to write it, or if there's some internal
         *    error storing it.
         */
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