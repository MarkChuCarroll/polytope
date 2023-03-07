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
import org.goodmath.polytope.repository.storage.ContentId
import org.goodmath.polytope.repository.util.Id
import org.goodmath.polytope.repository.util.getTyped
import org.goodmath.polytope.repository.util.newId
import org.goodmath.polytope.repository.util.putTyped
import java.time.Instant
import java.util.*


/**
 * The record for an artifact in the repository.
 */
data class Artifact(
    val id: Id<Artifact>,
    val artifactType: String,
    val timestamp: Instant,
    val creator: String,
    val project: String,
    val metadata: Map<String, String>
)

data class ArtifactVersion(
    val id: Id<ArtifactVersion>,
    val artifactId: Id<Artifact>,
    val artifactType: String,
    val timestamp: Instant,
    val creator: String,
    val content: ByteArray,
    val parents: List<Id<ArtifactVersion>>,
    val metadata: Map<String, String>,
    val status: Status
) {
    enum class Status {
        Working, Committed, Aborted
    }
}

class Artifacts(val db: RocksDB,
                val artifactsColumn: ColumnFamilyHandle,
                val versionsColumn: ColumnFamilyHandle,
                val repos: Repository) {

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
    fun withAuth(auth: AuthenticatedUser): AuthenticatedArtifacts {
        return AuthenticatedArtifacts(auth)
    }


    inner class AuthenticatedArtifacts(val auth: AuthenticatedUser) {

        /**
         * Retrieve an artifact from the database.
         * @param project the project containing the artifact.
         * @param id the artifact ID.
         * @return the full Artifact record.
         * @throws PolytopeException if the artifact isn't found, if the user
         *    doesn't have permission to read it, or if there's some internal
         *    error retrieving it.
         */
        fun retrieveArtifact(project: String, id: Id<Artifact>): Artifact {
            repos.users.validatePermissions(auth, Action.Read, project)
            return db.getTyped<Artifact>(artifactsColumn, id)
                ?: throw PolytopeException(PolytopeException.Kind.NotFound, "Artifact $id not found")
        }

        /**
         * Retrieve a version of an artifact from the database.
         * @param project the project containing the artifact.
         * @param id the version ID.
         * @return the full ArtifactVersion record.
         * @throws PolytopeException if the version isn't found, if the user
         *    doesn't have permission to read it, or if there's some internal
         *    error retrieving it.
         */
        fun retrieveVersion(project: String,
                            artifactId: Id<Artifact>,
                            versionId: Id<ArtifactVersion>): ArtifactVersion {
            // TODO: validate that the version belongs to an artifact in the project.
            repos.users.validatePermissions(auth, Action.Read, project)
            return db.getTyped<ArtifactVersion>(versionsColumn, versionId)
                ?: throw PolytopeException(PolytopeException.Kind.NotFound, "Artifact Version $versionId not found")
        }

        /**
         * Store a new artifact from the database.
         * @param project the project containing the artifact.
         * @param artifact the new artifact to save.
         * @param initialVersion the first version of the new artifact.
         * @throws PolytopeException if the artifact already exists, if the user
         *    doesn't have permission to write it, or if there's some internal
         *    error storing it.
         */
        fun createArtifact(project: String,
                           artifactType: String,
                           metadata: Map<String, String>): Artifact {
            repos.users.validatePermissions(auth, Action.Write, project)
            val artifact = Artifact(
                id = newId<Artifact>("art" ),
                artifactType = artifactType,
                Instant.now(),
                auth.userId,
                project,
                metadata)
            db.putTyped(artifactsColumn, artifact.id, artifact)
            return artifact
        }

        fun createVersion(project: String,
                          artifactId: Id<Artifact>,
                          artifactType: String,
                          content: ByteArray,
                          parents: List<Id<ArtifactVersion>>,
                          metadata: Map<String, String>): ArtifactVersion {
            repos.users.validatePermissions(auth, Action.Write, project)
            val ver = ArtifactVersion(
                id = newId<ArtifactVersion>("ver"),
                artifactId = artifactId,
                creator = auth.userId,
                content = content,
                timestamp = Instant.now(),
                parents = parents,
                metadata = metadata,
                artifactType = artifactType,
                status = ArtifactVersion.Status.Committed)
            db.putTyped(versionsColumn, ver.id, ver)
            return ver
        }


        /**
         * Create a working version of an artifact for an in-progress change.
         * @param project the project containing the artifact
         * @param base_version the version that's going to be edited into
         *    a new version.
         * @return a new working version.
         */
        fun createWorkingVersion(project: String,
                                 artifactId: Id<Artifact>,
                                 baseVersion: Id<ArtifactVersion>): ArtifactVersion {
            repos.users.validatePermissions(auth, Action.Write, project)
            val base = retrieveVersion(project, artifactId, baseVersion)
            val working = ArtifactVersion(
                id = newId<ArtifactVersion>("artifact"),
                artifactId = base.artifactId,
                artifactType = base.artifactType,
                content = base.content,
                creator = auth.userId,
                timestamp = Instant.now(),
                metadata = base.metadata,
                parents = listOf(base.id),
                status = ArtifactVersion.Status.Working)
            db.putTyped(versionsColumn, working.id, working)
            return working
        }

        /**
         * Update a working version
         * @param version the updated working version.
         * @return the updated version.
         */
        fun updateWorkingVersion(project: String,
                                 artifactId: Id<Artifact>,
                                 version: ArtifactVersion): ArtifactVersion {
            repos.users.validatePermissions(auth, Action.Write, project)
            val now = Instant.now()
            val newVersion = version.copy(timestamp = now)
            val old = try {
                retrieveVersion(project, version.artifactId,
                    version.id)
            } catch (e: PolytopeException) {
                if (e.kind == PolytopeException.Kind.NotFound) {
                    throw PolytopeException(
                        PolytopeException.Kind.NotFound,
                        "Attempted to update a non-existent working version ")
                } else {
                    throw e
                }
            }
            db.putTyped(versionsColumn, newVersion.id, newVersion)
            return newVersion
        }

        /**
         * Commit a working version as a final, immutable version in
         * the artifact history.
         * @param version the ID of the version to commit.
         */
        fun commitWorkingVersion(project: String,
                                 artifactId: Id<Artifact>,
                                 versionId: Id<ArtifactVersion>) {
            repos.users.validatePermissions(auth, Action.Write, project)
            val now = Instant.now()
            val version = try {
                retrieveVersion(project,  artifactId, versionId)
            } catch (e: PolytopeException) {
                if (e.kind == PolytopeException.Kind.NotFound) {
                    throw PolytopeException(
                        PolytopeException.Kind.NotFound,
                        "Attempted to commit a non-existent working version ")
                } else {
                    throw e
                }
            }
            db.putTyped(versionsColumn, version.id, version.copy(
                status = ArtifactVersion.Status.Committed,
                timestamp = now
            ))
        }

        /**
         * Abort an in-progress version, discarding its content
         * and removing it from the repository.
         * @param versionId the versionId of the working version.
         */
        fun abortWorkingVersion(project: String,
                                artifactId: Id<Artifact>,
                                versionId: Id<ArtifactVersion>) {
            repos.users.validatePermissions(auth, Action.Write, project)
            val now = Instant.now()
            val version = try {
                retrieveVersion(project, artifactId, versionId)
            } catch (e: PolytopeException) {
                if (e.kind == PolytopeException.Kind.NotFound) {
                    throw PolytopeException(
                        PolytopeException.Kind.NotFound,
                        "Attempted to abort a non-existent working version ")
                } else {
                    throw e
                }
            }
            db.putTyped(versionsColumn, version.id,
                version.copy(
                    status = ArtifactVersion.Status.Aborted,
                    timestamp = now,
                    content = ByteArray(0)
                ))

        }

        /**
         * Check the status of a version.
         */
        fun versionStatus(project: String,
                          artifactId: Id<Artifact>,
                          versionId: Id<ArtifactVersion>): ArtifactVersion.Status {
            val version = retrieveVersion(project, artifactId, versionId)
            return version.status
        }
    }

}