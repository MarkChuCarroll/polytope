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
import org.litote.kmongo.*
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
    val id: ObjectId,
    val artifactId: ArtifactId,
    val artifactType: String,
    val timestamp: Instant,
    val creator: String,
    val contentId: ContentId,
    val parents: List<VersionId>,
    val metadata: Map<String, String>,
    val status: Status
) {
    enum class Status {
        Working, Committed, Aborted
    }
}

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


    inner class AuthenticatedArtifacts(val auth: AuthToken) {

        /**
         * Retrieve an artifact from the database.
         * @param project the project containing the artifact.
         * @param id the artifact ID.
         * @return the full Artifact record.
         * @throws PolytopeException if the artifact isn't found, if the user
         *    doesn't have permission to read it, or if there's some internal
         *    error retrieving it.
         */
        fun retrieveArtifact(project: String, id: ArtifactId): Artifact {
            return artifactsCollection.findOne(match(Artifact::id eq id.id))
                ?: throw PolytopeException(PolytopeException.Kind.NotFound, "Artifact $id not found")
        }

        /**
         * Retrieve a version of an artifact from the database.
         * @param id the version ID.
         * @return the full ArtifactVersion record.
         * @throws PolytopeException if the version isn't found, if the user
         *    doesn't have permission to read it, or if there's some internal
         *    error retrieving it.
         */
        fun retrieveVersion(id: VersionId): ArtifactVersion {
            return versionsCollection.findOne(match(ArtifactVersion::id eq id.versionId))
                ?: throw PolytopeException(PolytopeException.Kind.NotFound, "Artifact Version $id not found")}

        /**
         * Store a new artifact from the database.
         * @param project the project containing the artifact.
         * @param artifact the new artifact to save.
         * @throws PolytopeException if the artifact already exists, if the user
         *    doesn't have permission to write it, or if there's some internal
         *    error storing it.
         */
        fun storeArtifact(artifact: Artifact) {
            artifactsCollection.save(artifact)
        }

        fun createWorkingVersion(project: String, base_version: VersionId): ArtifactVersion {
            val base = retrieveVersion(base_version)
            val working = ArtifactVersion(
                id = ObjectId.get(),
                artifactId = base.artifactId,
                artifactType = base.artifactType,
                contentId = base.contentId,
                creator = auth.userId,
                timestamp = Instant.now(),
                metadata = base.metadata,
                parents = listOf(VersionId(base.artifactId, base.id)),
                status = ArtifactVersion.Status.Working)
            versionsCollection.save(working)
            return working
        }

        fun updateWorkingVersion(version: ArtifactVersion): ArtifactVersion {
            val now = Instant.now()
            val newVersion = version.copy(timestamp = now)
            val old = try {
                retrieveVersion(VersionId(version.artifactId,
                    version.id))
            } catch (e: PolytopeException) {
                if (e.kind == PolytopeException.Kind.NotFound) {
                    throw PolytopeException(PolytopeException.Kind.NotFound,
                        "Attempted to update a non-existent working version ")
                } else {
                    throw e
                }
            }

            if (old.contentId != version.contentId) {
                repos.storage.deleteTransientBlob(old.contentId)
            }
            versionsCollection.save(newVersion)
            return newVersion
        }

        fun commitWorkingVersion(version: VersionId) {
            val now = Instant.now()
            val version = try {
                retrieveVersion(version)
            } catch (e: PolytopeException) {
                if (e.kind == PolytopeException.Kind.NotFound) {
                    throw PolytopeException(PolytopeException.Kind.NotFound,
                        "Attempted to commit a non-existent working version ")
                } else {
                    throw e
                }
            }
            val content = repos.storage.retrieveBlob(version.contentId)
            val permanentContentId = repos.storage.storeBlob(content)
            versionsCollection.updateOne(
                match(ArtifactVersion::id eq version.id),
                set(
                    ArtifactVersion::status setTo ArtifactVersion.Status.Committed,
                    ArtifactVersion::contentId setTo permanentContentId,
                    ArtifactVersion::timestamp setTo now
                )
            )
        }

        fun abortWorkingVersion(version: VersionId) {
            val now = Instant.now()
            val version = try {
                retrieveVersion(version)
            } catch (e: PolytopeException) {
                if (e.kind == PolytopeException.Kind.NotFound) {
                    throw PolytopeException(PolytopeException.Kind.NotFound,
                        "Attempted to abort a non-existent working version ")
                } else {
                    throw e
                }
            }
            repos.storage.deleteTransientBlob(version.contentId)
            versionsCollection.updateOne(match(ArtifactVersion::id eq version.id),
                set(ArtifactVersion::status setTo ArtifactVersion.Status.Aborted,
                    ArtifactVersion::timestamp setTo now))

        }

        fun versionStatus(version: VersionId): ArtifactVersion.Status {
            val version = retrieveVersion(version)
            return version.status
        }
    }

    companion object {
        fun initializeStorage(cfg: Config) {

        }
    }


}