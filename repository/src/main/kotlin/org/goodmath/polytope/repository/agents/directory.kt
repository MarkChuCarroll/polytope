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


/**
 * A directory is just a list of entries, each of which is a name/id pair.
 */
data class Directory(
    val entries: List<DirectoryEntry>
) {
    data class DirectoryEntry(
        val name: String,
        val artifact: Id<Artifact>
    )
    fun removeBinding(name: String): Directory =
        Directory(entries.filter { it.name != name })

    fun addBinding(name: String, artifactId: Id<Artifact>): Directory =
        if (entries.any { it.name == name}) {
            throw PolytopeException(
                PolytopeException.Kind.Conflict,
                "Binding already exists for $name"
            )
        } else {
            Directory(entries + DirectoryEntry(name, artifactId))
        }

    fun containsBinding(name: String): Boolean =
        entries.any { it.name == name }
}

/**
 * For computing merges, a structure that describes the changes
 * between a base version and a modified version of a directory.
 */
data class DirectoryChange(
    val type: Kind,
    val artifactId: Id<Artifact>,
    val nameBefore: String?,
    val nameAfter: String?
) {
    enum class Kind {
        Add, Rename, Remove
    }

    /**
     * Apply a change, produced from one directory comparison,
     * to a different directory. This merges a change from one
     * modified version to another.
     */
    fun applyTo(dir: Directory): Directory {
        return when (type) {
            Kind.Rename ->
                dir.removeBinding(nameBefore!!)
                    .addBinding(nameAfter!!, artifactId)

            Kind.Add ->
                if (nameAfter != null && !dir.containsBinding(nameAfter)) {
                    dir.addBinding(nameAfter, artifactId)
                } else {
                    throw IllegalArgumentException("Add must have a non-null new binding name")
                }

            Kind.Remove ->
                dir.removeBinding(nameBefore!!)

        }
    }
}

/**
 * Information about a marge conflict that's specific to directories.
 * This will be the contents of the 'details' field of the conflict record.
 */
data class DirectoryMergeConflict(
    val kind: ConflictKind,
    val nameBefore: String?,
    val nameInMergeSource: String?,
    val nameInMergeTarget: String?,
    val referencedId: Id<Artifact>?
) {
    enum class ConflictKind {
        ADD_ADD, MOD_DEL, DEL_MOD, MOD_MOD
    }

}

/**
 * The agent for working with directory artifacts.
 */
class DirectoryAgent() : Agent<Directory> {
    override val artifactType: String = "directory"
    override fun fromBytes(content: ByteArray): Directory {
        return ParsingCommons.klaxon.parse<Directory>(content)
    }

    override fun merge(changeStep: Id<ChangeStep>, ancestor: ArtifactVersion, source: ArtifactVersion, target: ArtifactVersion): MergeResult {
        val ancDir = fromBytes(ancestor.content)
        val srcDir = fromBytes(source.content)
        val tgtDir = fromBytes(target.content)
        val ancBindings = DualMapping.fromDirectory(ancDir)
        val srcBindings = DualMapping.fromDirectory(srcDir)
        val tgtBindings = DualMapping.fromDirectory(tgtDir)

        // Step 1: compare each of the merge source and merge target to
        // the common ancestor, gathering the changes.
        val changesInMergeSource = computeDirectoryChanges(ancBindings, srcBindings)
        val changesInMergeTarget = computeDirectoryChanges(ancBindings, tgtBindings)

        // At this point, we've got a collection of the changes to the directory
        // in both the merge source and merge target. What we need to do now
        // is walk through those.
        //
        // For each directory change in the merge source, we need to check to see if there's
        // a conflicting change in the merge target, and vice versa. If we find
        // a conflict, then we need to put a best-guess into the merge result, and
        // add a conflict record.

        val conflicts = ArrayList<MergeConflict>()
        val proposedMergeResult = tgtDir.copy()


        for (changeInSource in changesInMergeSource) {
            // Check for add-add: both source and target have an ADD change,
            // with the same name.
            handleAddAddConflict(changeInSource, changesInMergeTarget, ancestor, source, target, changeStep, conflicts)

            // See if there's a change in the target that's working with the same artifact ID
            // as the change in the source.

            val changeInTarget = changesInMergeTarget.findLast { m -> m.artifactId == changeInSource.artifactId }
            if (changeInTarget != null) {
                if (changeInSource != changeInTarget) {
                    when {
                        changeInSource.type == DirectoryChange.Kind.Add &&
                                changeInTarget.type == DirectoryChange.Kind.Add ->
                            conflicts.add(
                                MergeConflict(
                                    newId<MergeConflict>("dirMerge"),
                                    artifactId = ancestor.artifactId,
                                    sourceVersion = source.id,
                                    targetVersion = target.id,
                                    changeStep = changeStep,
                                    details = ParsingCommons.klaxon.toJsonString(
                                        DirectoryMergeConflict(
                                            kind = DirectoryMergeConflict.ConflictKind.ADD_ADD,
                                            nameBefore = null,
                                            nameInMergeSource = changeInSource.nameAfter,
                                            nameInMergeTarget = changeInTarget.nameAfter,
                                            referencedId = ancestor.artifactId
                                        )
                                    )
                                )
                            )

                        changeInSource.type == DirectoryChange.Kind.Remove &&
                                changeInTarget.type == DirectoryChange.Kind.Rename ->
                            conflicts.add(
                                MergeConflict(
                                    id = newId<MergeConflict>("dirMerge"),
                                    artifactId = ancestor.artifactId,
                                    sourceVersion = source.id,
                                    targetVersion = target.id,
                                    changeStep = changeStep,
                                    details = ParsingCommons.klaxon.toJsonString(
                                        DirectoryMergeConflict(
                                            DirectoryMergeConflict.ConflictKind.DEL_MOD,
                                            nameBefore = null,
                                            nameInMergeSource = changeInSource.nameAfter,
                                            nameInMergeTarget = changeInTarget.nameAfter,
                                            referencedId = ancestor.artifactId
                                        )
                                    )
                                )
                            )

                        changeInSource.type == DirectoryChange.Kind.Rename &&
                                changeInTarget.type == DirectoryChange.Kind.Remove ->
                            conflicts.add(
                                MergeConflict(
                                    id = newId<MergeConflict>("dirMerge"),
                                    artifactId = ancestor.artifactId,
                                    sourceVersion = source.id,
                                    targetVersion = target.id,
                                    changeStep = changeStep,
                                    details = ParsingCommons.klaxon.toJsonString(
                                        DirectoryMergeConflict(
                                            DirectoryMergeConflict.ConflictKind.DEL_MOD,
                                            changeInSource.nameBefore,
                                            null,
                                            changeInTarget.nameAfter,
                                            ancestor.artifactId
                                        )
                                    )
                                )
                            )

                        changeInSource.type == DirectoryChange.Kind.Rename &&
                                changeInTarget.type == DirectoryChange.Kind.Rename ->
                            conflicts.add(
                                MergeConflict(
                                    id = newId<MergeConflict>("dirMerge"),
                                    artifactId = ancestor.artifactId,
                                    sourceVersion = source.id,
                                    targetVersion = target.id,
                                    changeStep = changeStep,
                                    details = ParsingCommons.klaxon.toJsonString(
                                        DirectoryMergeConflict(
                                            DirectoryMergeConflict.ConflictKind.MOD_MOD,
                                            changeInSource.nameBefore,
                                            changeInSource.nameAfter,
                                            changeInTarget.nameAfter,
                                            ancestor.artifactId
                                        )
                                    )
                                )
                            )
                    }
                }
            } else {
                // apply the mod to the proposed result.
                changeInSource.applyTo(proposedMergeResult)
            }
        }
        // assemble into a merge result.
        return MergeResult(
            artifactType = this.artifactType,
            artifactId = ancestor.artifactId,
            ancestorVersion = ancestor.id,
            sourceVersion = source.id,
            targetVersion = target.id,
            proposedMerge = ParsingCommons.klaxon.toBytes(proposedMergeResult),
            conflicts = conflicts)
    }

    private fun handleAddAddConflict(
        srcMod: DirectoryChange,
        targetChanges: List<DirectoryChange>,
        ancestor: ArtifactVersion,
        source: ArtifactVersion,
        target: ArtifactVersion,
        changeStep: Id<ChangeStep>,
        conflicts: ArrayList<MergeConflict>
    ) {
        if (srcMod.type == DirectoryChange.Kind.Add) {
            val sameNameMod = targetChanges.findLast { m ->
                m.nameAfter == srcMod.nameAfter && m.type == DirectoryChange.Kind.Add &&
                        m.artifactId != srcMod.artifactId
            }
            if (sameNameMod != null) {
                val details = DirectoryMergeConflict(
                    DirectoryMergeConflict.ConflictKind.ADD_ADD,
                    srcMod.nameBefore,
                    srcMod.nameAfter,
                    srcMod.nameAfter,
                    null
                )
                val conflict = MergeConflict(
                    id = newId<MergeConflict>("dirMerge"),
                    artifactId = ancestor.artifactId,
                    sourceVersion = source.id,
                    targetVersion = target.id,
                    changeStep = changeStep,
                    details = ParsingCommons.klaxon.toJsonString(
                        details
                    )
                )
                conflicts.add(conflict)
            }
        }
    }

    override fun toBytes(content: Directory): ByteArray {
        return ParsingCommons.klaxon.toBytes(content)
    }


    // Helper functions for computing diff/merge.

    /**
     * For directory merges, we need both the primary mapping from
     * name to artifact, and also the secondary mapping from artifact to name.
     */
    data class DualMapping(
        val byName: Map<String, Id<Artifact>>,
        val byArtifact: Map<Id<Artifact>, String>
    ) {
        companion object {
            fun fromDirectory(dir: Directory): DualMapping {
                return DualMapping(
                    dir.entries.associate { Pair(it.name, it.artifact)},
                    dir.entries.associate { Pair(it.artifact, it.name)}
                )
            }
        }
    }

    private fun unmodified(id: String, baseVersion: DualMapping, modifiedVersion: DualMapping): Boolean =
         baseVersion.byArtifact.containsKey(id) && baseVersion.byArtifact[id] == modifiedVersion.byArtifact[id]

    private fun added(id: String, baseVersion: DualMapping, modifiedVersion: DualMapping): Boolean =
        modifiedVersion.byArtifact[id] != null && baseVersion.byArtifact[id] == null

    private fun renamed(id: String, baseVersion: DualMapping, modifiedVersion: DualMapping): Boolean =
        baseVersion.byArtifact.containsKey(id) && modifiedVersion.byArtifact.containsKey(id) &&
            baseVersion.byArtifact[id] != modifiedVersion.byArtifact[id]

    private fun removed(id: String, baseVersion: DualMapping, modifiedVersion: DualMapping): Boolean =
        baseVersion.byArtifact[id] != null && modifiedVersion.byArtifact[id] == null


    private fun computeDirectoryChanges(baseVersion: DualMapping, modifiedVersion: DualMapping): List<DirectoryChange> {
        val allIds = HashSet(baseVersion.byArtifact.keys + modifiedVersion.byArtifact.keys)
        return allIds.mapNotNull { id ->
            if (!unmodified(id, baseVersion, modifiedVersion)) {
                when {
                    added(id, baseVersion, modifiedVersion) -> {
                        DirectoryChange(
                            DirectoryChange.Kind.Add, id, null, modifiedVersion.byArtifact[id]!!)

                    }
                    removed(id, baseVersion, modifiedVersion) -> {
                            DirectoryChange(
                                DirectoryChange.Kind.Remove, id, baseVersion.byArtifact[id]!!, null)
                    }
                    renamed(id, baseVersion, modifiedVersion) -> {
                        DirectoryChange(
                            DirectoryChange.Kind.Rename,
                            baseVersion.byArtifact[id]!!,
                            modifiedVersion.byArtifact[id]!!,
                            id
                        )
                    }
                    else -> {
                        null // do nothing.
                    }
                }
            } else {
                null
            }
        }
    }

}
