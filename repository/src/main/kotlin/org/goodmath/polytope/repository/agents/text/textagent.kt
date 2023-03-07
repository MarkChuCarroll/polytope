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

package org.goodmath.polytope.repository.agents.text

import org.goodmath.polytope.repository.agents.Agent
import org.goodmath.polytope.repository.agents.MergeConflict
import org.goodmath.polytope.repository.agents.MergeResult
import org.goodmath.polytope.repository.data.Artifact
import org.goodmath.polytope.repository.data.ArtifactVersion
import org.goodmath.polytope.repository.data.ChangeStep
import org.goodmath.polytope.repository.util.Id
import org.goodmath.polytope.repository.util.ParsingCommons
import org.goodmath.polytope.repository.util.newId
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

data class Text(
    val content: List<String>
)

data class TextMergeConflict(
    val conflictStart: Int,
    val conflictEnd: Int
)

/**
 * An implementation of a (to my knowledge) new 3-way merge algorithm.
 *
 * The idea is, you convert a modified version into a list of labelled
 * lines, where each line has a label that indicates whether it's
 * unmodified; deleted (present in the ancestor but not in the modified),
 * inserted (present in the modified, but not in the ancestor).
 *
 * Once the lines are labeled, then they're coalesced into blocks, by anchoring
 * them against the first unmodified line from the ancestor that follows them.
 *
 * Finally, we do the merge by iterating over the lines from the common
 * ancestor, which are also the anchors for any modifications. If there's
 * one block anchored on a line, then that edit will be part of the merge
 * result; if there's more than one, then we check for compatibility.
 *
 * In practice, this seems to avoid conflicts and generate good results in many places
 * that would have been conflicts in the traditional diff-transform approach.
 */

enum class LineLabel {
    Deleted, Inserted, Unmodified
}

/**
 * Lines labelled with information about how they differ from a base version.
 */
data class LabeledLine(
    val label: LineLabel,
    val content: String,
    val baseLine: Int?,
    val targetLine: Int?,
    val anchorLine: Int
)


/**
 * Check if two labeled lines match.
 * Matching means that the two correspond to an equivalent edit:
 * * deleting the same line;
 * * inserting the same text in the same position;
 * * leaving the same text unmodified.
 */
fun linesMatch(first: LabeledLine, second: LabeledLine): Boolean =
    second.label == first.label
            && second.baseLine == first.baseLine
            && second.anchorLine == first.anchorLine
            && second.content == first.content

/**
 * A representation of a block of modified text from two different edits.
 * A block is anchored by a line of text from the original document which comes before
 * the edits. (This has the somewhat confusing effect that a file with 10 lines will have 11
 * indices - index[10] means "before the invisible line at the end of the file")
 */
data class MergeBlock(
    val anchorLine: Int,
    val srcLines: ArrayList<LabeledLine>,
    val tgtLines: ArrayList<LabeledLine>
) {
    constructor(l: Int) : this(l, ArrayList(), ArrayList())

    /**
     * Checks if the two branches of a merge block correspond to the same edit.
     */
    private fun matches(): Boolean {
        return srcLines.size == tgtLines.size &&
                srcLines.zip(tgtLines).all { (srcEdit, tgtEdit) -> linesMatch(srcEdit, tgtEdit) }
    }

    /**
     * Generate the merge result of the labeled lines anchored at this point.
     */
    fun render(
        sourceLabel: String,
        targetLabel: String,
        changeStep: Id<ChangeStep>,
        artifactId: Id<Artifact>,
        sourceVersionId: Id<ArtifactVersion>,
        targetVersionId: Id<ArtifactVersion>,
        result: ArrayList<String>
    ): List<MergeConflict> {
        val conflicts = ArrayList<MergeConflict>()
        when {
            matches() -> {
                // If the two blocks match - that is, they generate to the same edit -
                // then we just return either one of them.
                for (l in srcLines) {
                    if (l.label == LineLabel.Inserted || l.label == LineLabel.Unmodified) {
                        result.add(l.content)
                    }
                }
            }

            tgtLines.all { l -> l.label == LineLabel.Unmodified } -> {
                // If all of the target lines are unmodified, then the merge result is
                // the lines from the merge source.
                for (l in srcLines) {
                    if (l.label == LineLabel.Inserted || l.label == LineLabel.Unmodified) {
                        result.add(l.content)
                    }
                }
            }

            srcLines.all { l -> l.label == LineLabel.Unmodified } -> {
                // Similarly, if the source lines are unmodified, then the merge result
                // is the ines from the target.
                for (l in tgtLines) {
                    if (l.label == LineLabel.Inserted || l.label == LineLabel.Unmodified) {
                        result.add(l.content)
                    }
                }
            }

            else -> {
                // Otherwise, we have a conflict between the source and target.
                val conflictBlockStart = result.size
                result.add("<<<<<< VERSION FROM $sourceLabel\n")
                for (l in srcLines) {
                    if (l.label == LineLabel.Inserted || l.label == LineLabel.Unmodified) {
                        result.add(l.content)
                    }
                }
                result.add("====== VERSION FROM $targetLabel\n")
                for (l in tgtLines) {
                    if (l.label == LineLabel.Inserted || l.label == LineLabel.Unmodified) {
                        result.add(l.content)
                    }
                }
                result.add(">>>>>>\n")
                val conflictBlockEnd = result.size
                conflicts.add(
                    MergeConflict(
                        id = newId<MergeConflict>("textMerge"),
                        artifactId = artifactId,
                        sourceVersion = sourceVersionId,
                        targetVersion = targetVersionId,
                        changeStep = changeStep,
                        details = ParsingCommons.klaxon.toJsonString(
                            TextMergeConflict(conflictBlockStart, conflictBlockEnd)
                        )
                    )
                )
            }
        }
        return conflicts
    }
}


object TextAgent: Agent<Text> {
    override val artifactType: String = "text"

    override fun fromBytes(bytes: ByteArray): Text {
        val result = ArrayList<String>()
        val input = ByteArrayInputStream(bytes)
        var builder = StringBuffer()
        var next: Int = input.read()
        while (next != -1) {
            val c: Char = next.toChar()
            builder.append(c)
            if (c == '\n') {
                result.add(builder.toString())
                builder = StringBuffer()
            }
            next = input.read()
            }
        if (builder.isNotEmpty()) {
            result.add(builder.toString())
        }
        return Text(result)
    }

    override fun merge(
        changeStep: Id<ChangeStep>,
        ancestor: ArtifactVersion,
        source: ArtifactVersion,
        target: ArtifactVersion
    ): MergeResult =
        doMerge(changeStep,
            ancestor.artifactId,
            ancestor.id,
            source.id,
            target.id,
            fromBytes(ancestor.content),
            fromBytes(source.content),
            fromBytes(target.content))

    /**
     * Convert a text content to an array of bytes.
     */
    override fun toBytes(content: Text): ByteArray {
        val byteChunks = content.content.map { it.toByteArray() }
        val length: Int = byteChunks.map { bytes -> bytes.size }.reduce { a, b -> a + b }
        val bytes = ByteArrayOutputStream(length).use { out ->
            for (chunk in byteChunks) {
                out.write(chunk)
            }
            out
        }
        return bytes.toByteArray()
    }

    private fun performBlockMergeOnContent(
        base: Text,
        source: Text,
        target: Text
    ): ArrayList<MergeBlock> {
        // Get the labeled lists for source and target.
        val srcLabeledLines = createLabelledList(base.content, source.content)
        val tgtLabeledLines = createLabelledList(base.content, target.content)

        // A map from a line number in the base to a collection of lines that occur
        // before that line number in one of the mods.
        val lineMap = HashMap<Int, MergeBlock>()
        for (srcLine in srcLabeledLines) {
            val block = lineMap[srcLine.anchorLine] ?: MergeBlock(srcLine.anchorLine)
            block.srcLines.add(srcLine)
            lineMap[srcLine.anchorLine] = block
        }

        for (tgtLine in tgtLabeledLines) {
            val block = lineMap[tgtLine.anchorLine] ?: MergeBlock(tgtLine.anchorLine)
            block.tgtLines.add(tgtLine)
            lineMap[tgtLine.anchorLine] = block
        }
        val result = ArrayList<MergeBlock>()
        val maxIdx = lineMap.keys.maxOrNull() ?: 0
        for (i in 0..maxIdx) {
            val x = lineMap[i]
            if (x != null) {
                result.add(x)
            }
        }
        return result
    }

    private fun doMerge(
        changeStep: Id<ChangeStep>,
        artifactId: Id<Artifact>,
        ancestorVersionId: Id<ArtifactVersion>,
        sourceVersionId: Id<ArtifactVersion>,
        targetVersionId: Id<ArtifactVersion>,
        base: Text,
        mergeSrc: Text,
        mergeTgt: Text
    ): MergeResult {
        val blocks = performBlockMergeOnContent(base, mergeSrc, mergeTgt)
        val result = ArrayList<String>()
        val allConflicts = ArrayList<MergeConflict>()
        blocks.flatMap {
            it.render(
                sourceLabel = "merge source ($sourceVersionId)",
                targetLabel = "merge target $(targetVersionId)",
                changeStep = changeStep,
                artifactId = artifactId, sourceVersionId, targetVersionId,
                result = result
            )
        }
        return MergeResult(
            artifactType = this.artifactType,
            artifactId = artifactId,
            ancestorVersion = ancestorVersionId,
            sourceVersion = sourceVersionId,
            targetVersion = targetVersionId,
            proposedMerge = toBytes(Text(result)),
            conflicts = allConflicts)
    }


    /**
     * To label a modified version of a file, we'll walk through
     * the lines in the base and the modified.
     *
     * * If a line is in the LCS(base, mod), then that line is Unmodified.
     * * If a line is in the base, and _not_ in the modified then it's Deleted.
     * * If a line is in the modified but not the base, then it's Inserted.
     */
    private fun createLabelledList(base: List<String>, modified: List<String>): List<LabeledLine> {
        val lcs: List<CrossVersionLineMapping> = indexedLcs(base, modified)
        val result = ArrayList<LabeledLine>()
        var firstUnprocessedInBase = 0
        var firstUnprocessedInTarget = 0
        for (line in lcs) {
            if (line.lineNumberInLeft > firstUnprocessedInBase) {
                // Lines between firstUnprocessedInBase and line.first (the start of the LCS
                // segment's position in the base) were deleted before the start of the segment.
                for (l in firstUnprocessedInBase until line.lineNumberInLeft) {
                    result.add(
                        LabeledLine(
                            LineLabel.Deleted, base[l], l, null,
                            line.lineNumberInLeft
                        )
                    )
                }
            }
            if (line.lineNumberInRight > firstUnprocessedInTarget) {
                // Any lines from the target between firstUnprocessedInTarget and line.second (the start of the
                // next LCS segment in mod) should be labeled as inserted
                // before line.first
                for (l in firstUnprocessedInTarget until line.lineNumberInRight) {
                    result.add(
                        LabeledLine(
                            LineLabel.Inserted, modified[l], null, l,
                            line.lineNumberInLeft
                        )
                    )
                }
            }
            // Line from LCS should be labeled as unmodified.
            result.add(
                LabeledLine(
                    LineLabel.Unmodified, base[line.lineNumberInLeft],
                    line.lineNumberInLeft, line.lineNumberInRight,
                    line.lineNumberInLeft + 1
                )
            )
            firstUnprocessedInBase = line.lineNumberInLeft + 1
            firstUnprocessedInTarget = line.lineNumberInRight + 1
        }
        // Anything left over in base is a delete;
        for (line in firstUnprocessedInBase until base.size) {
            result.add(LabeledLine(LineLabel.Deleted, base[line], line, null, line + 1))
        }
        // anything left over in the mod is an insert.
        for (line in firstUnprocessedInTarget until modified.size) {
            result.add(LabeledLine(LineLabel.Inserted, modified[line], null, line, line + 1))
        }
        return result
    }

}