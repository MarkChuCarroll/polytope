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

package org.goodmath.polytope.repository.agents

import org.goodmath.polytope.PolytopeException
import org.goodmath.polytope.repository.data.Artifact
import org.goodmath.polytope.repository.data.ArtifactVersion
import org.goodmath.polytope.repository.data.ChangeStep
import org.goodmath.polytope.repository.util.*

data class Baseline(
    val rootDir: Id<Artifact>,
    val entries: MutableMap<Id<Artifact>, Id<ArtifactVersion>>
) {
    fun contains(artifactId: Id<Artifact>): Boolean =
        entries.containsKey(artifactId)

    fun get(artifactId: Id<Artifact>): Id<ArtifactVersion>? =
        entries[artifactId]

    fun add(artifactId: Id<Artifact>, versionId: Id<ArtifactVersion>) {
        if (contains(artifactId)) {
            throw PolytopeException(PolytopeException.Kind.Conflict,
                "Baseline already contains a mapping for $artifactId")
        } else {
            entries[artifactId] = versionId
        }
    }

    private fun remove(artifactId: Id<Artifact>) {
        if (!entries.containsKey(artifactId)) {
            throw PolytopeException(PolytopeException.Kind.NotFound,
                "Baseline doesn't contain a mapping for $artifactId")
        } else {
            entries.remove(artifactId)
        }
    }
    fun change(artifactId: Id<Artifact>, versionId: Id<ArtifactVersion>) {
        remove(artifactId)
        add(artifactId, versionId)
    }
}


enum class BaselineConflictType {
    MOD_DEL, DEL_MOD, MOD_MOD
}

data class BaselineConflict(
    val type: BaselineConflictType,
    val mergeSourceVersion: String?,
    val mergeTargetVersion: String?,
)

object BaselineAgent: Agent<Baseline> {
    override val artifactType: String = "baseline"

    override fun fromBytes(content: ByteArray): Baseline =
        ParsingCommons.klaxon.parse<Baseline>(content)

    override fun toBytes(content: Baseline): ByteArray =
        ParsingCommons.klaxon.toBytes(content)


    override fun merge(
        changeStep: Id<ChangeStep>,
        ancestor: ArtifactVersion,
        source: ArtifactVersion,
        target: ArtifactVersion
    ): MergeResult {
        val ancestorBaseline = fromBytes(ancestor.content)
        val sourceBaseline = fromBytes(source.content)
        val targetBaseline = fromBytes(target.content)

        val targetVersionMap = targetBaseline.entries
        val targetArtifacts = targetVersionMap.keys
        val ancestorVersions = ancestorBaseline.entries
        val ancestorArtifacts = ancestorVersions.keys
        val sourceVersions = sourceBaseline.entries
        val sourceArtifacts = sourceVersions.keys


        val removedInTarget = ancestorArtifacts.minus(targetArtifacts)
        val removedInSource = ancestorArtifacts.minus(sourceArtifacts)

        val addedInTarget = targetArtifacts.minus(ancestorArtifacts)
        val addedInSource = sourceArtifacts.minus(ancestorArtifacts)

        val modifiedInTarget = targetArtifacts.filter { artId ->
            ancestorArtifacts.contains(artId) && ancestorVersions[artId] != targetVersionMap[artId]
        }
        val modifiedInSource = sourceArtifacts.filter { artId ->
            ancestorArtifacts.contains(artId) && ancestorVersions[artId] != sourceVersions[artId]
        }

        val allArtifacts = sourceArtifacts.union(targetArtifacts).union(ancestorArtifacts)

        val mergedVersionMappings = HashMap<Id<Artifact>, Id<ArtifactVersion>>()
        val deletedArtifacts = ArrayList<Id<Artifact>>()
        val conflicts = ArrayList<MergeConflict>()
        for (artId in allArtifacts) {
            val ancestorArtVersion = ancestorVersions[artId]
            val targetArtVersion = targetVersionMap[artId]
            val sourceArtVersion = sourceVersions[artId]
            val modSource = modifiedInSource.contains(artId)
            val modCurrent = modifiedInTarget.contains(artId)

            if (removedInTarget.contains(artId)) {
                if (modifiedInSource.contains(artId)) {
                    conflicts.add(
                        MergeConflict(
                            id = newId<MergeConflict>("baselineMerge"),
                            artifactId = ancestor.artifactId,
                            changeStep = changeStep,
                            sourceVersion = source.id,
                            targetVersion = target.id,
                            details = ParsingCommons.klaxon.toJsonString(
                                BaselineConflict(BaselineConflictType.DEL_MOD,
                                    sourceArtVersion!!, null))))
                    deletedArtifacts.add(artId)
                } else if (removedInSource.contains(artId)) {
                    if (modifiedInTarget.contains(artId)) {
                        conflicts.add(MergeConflict(
                            id = newId<MergeConflict>("baselineMerge"),
                            artifactId = ancestor.artifactId,
                            changeStep = changeStep,
                            sourceVersion = source.id,
                            targetVersion = target.id,
                            details = ParsingCommons.klaxon.toJsonString(
                                BaselineConflict(BaselineConflictType.MOD_DEL,
                                    null, targetArtVersion))))
                    }
                    deletedArtifacts.add(artId)
                } else if (addedInTarget.contains(artId)) {
                    mergedVersionMappings[artId] = targetArtVersion!!
                } else if (addedInSource.contains(artId)) {
                    mergedVersionMappings[artId] = sourceArtVersion!!
                }
                else if (modCurrent && !modSource) {
                    mergedVersionMappings[artId] = targetArtVersion!!
                } else if (!modCurrent && modSource) {
                    mergedVersionMappings[artId] = sourceArtVersion!!
                } else if (!modCurrent) {  //  modSource must be false here
                    mergedVersionMappings[artId] = sourceArtVersion!!
                } else { // modified in both
                    conflicts.add(
                        MergeConflict(
                            id = newId<MergeConflict>("baselineMerge"),
                            artifactId = ancestor.artifactId,
                            changeStep = changeStep,
                            sourceVersion = source.id,
                            targetVersion = target.id,
                            details = ParsingCommons.klaxon.toJsonString(
                                BaselineConflict(BaselineConflictType.MOD_MOD,
                                    sourceArtVersion, targetArtVersion))))

                }
            }
        }
        return MergeResult(
            artifactType = this.artifactType,
            artifactId = ancestor.artifactId,
            ancestorVersion = ancestor.id,
            sourceVersion = source.id,
            targetVersion = target.id,
            proposedMerge = toBytes(Baseline(targetBaseline.rootDir,
                mergedVersionMappings)),
            conflicts = conflicts)

    }
}
