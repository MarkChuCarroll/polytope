package org.goodmath.polytope.repository.agents

import org.goodmath.polytope.repository.data.Artifact
import org.goodmath.polytope.repository.data.ArtifactVersion
import org.goodmath.polytope.repository.data.ChangeStep
import org.goodmath.polytope.repository.util.Id
import kotlin.reflect.jvm.internal.impl.platform.TargetPlatformVersion

data class MergeConflict(
    val id: Id<MergeConflict>,
    val artifactId: Id<Artifact>,
    val sourceVersion: Id<ArtifactVersion>,
    val targetVersion: Id<ArtifactVersion>,
    val changeStep: Id<ChangeStep>,
    val details: String
)

data class MergeResult(
    val artifactType: String,
    val artifactId: Id<Artifact>,
    val ancestorVersion: Id<ArtifactVersion>,
    val sourceVersion: Id<ArtifactVersion>,
    val targetVersion: Id<ArtifactVersion>,
    val proposedMerge: ByteArray,
    val conflicts: List<MergeConflict>
)
interface Agent<T> {
    val artifactType: String
    fun toBytes(content: T): ByteArray
    fun fromBytes(content: ByteArray): T

    fun merge(
        changeStep: Id<ChangeStep>,
        ancestor: ArtifactVersion,
        source: ArtifactVersion,
        target: ArtifactVersion): MergeResult

}



