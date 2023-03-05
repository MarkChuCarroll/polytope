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

package org.goodmath.polytope.workspace

import com.mongodb.client.MongoDatabase
import org.goodmath.polytope.repository.Repository
import org.goodmath.polytope.repository.data.*
import org.litote.kmongo.Id
import java.time.Instant

data class WorkspaceDescriptor(
    val project: String,
    val wsName: String,
    val id: Id<Workspace>,
    val creator: String,
    val baselineId: Id<Artifact>
)

data class MergeConflict(
    val id: Id<MergeConflict>
)

data class WorkspaceFile(
    val path: String,
    val artifactId: Id<Artifact>,
    val versionId: Id<ArtifactVersion>,
    val type: String,
    val lastModified: Instant,
    val metadata: Map<String, String>,
    val content: ByteArray
)
class Workspace(
    val auth: AuthenticatedUser,
    val ws: WorkspaceDescriptor,
    val repos: Repository,
    val db: MongoDatabase) {

    fun isDirty(): Boolean {
        TODO()
    }

    fun populate(fromVersion: ProjectVersionSpecifier) {
        TODO()
    }

    fun getWorkspaceFile(path: String): WorkspaceFile? {
        TODO()
    }

    fun getDirtyVersion(path: String): Pair<Id<Artifact>, Id<ArtifactVersion>>? {
        TODO()
    }

    fun addNewFile(path: String, type: String, content: ByteArray, metadata: Map<String, String>): Id<Artifact> {
        TODO()
    }

    fun deleteFile(path: String) {
        TODO()
    }

    fun moveFile(pathBefore: String, pathAfter: String) {
        TODO()
    }

    fun modifyFile(path: String,
                   content: ByteArray,
                   metadata: Map<String, String>) {

    }

    fun updateFromHistory(newBasis: ProjectVersionSpecifier): List<MergeConflict> {
        TODO()
    }

    fun saveChangestep(
        description: String,
        resolvedConflicts: List<Id<MergeConflict>>): Id<ChangeStep> {
        TODO()
    }

    fun startChange(changeName: String,
                    description: String,
                    basis: ProjectVersionSpecifier) {
        TODO()
    }

    fun deliverChange(toHistory: String) {
        TODO()
    }

    fun abortChange() {
        TODO()
    }

    fun pathForArtifact(id: Id<Artifact>): String? {
        TODO()
    }

    fun artifactForPath(path: String): Id<Artifact>? {
        TODO()
    }





}