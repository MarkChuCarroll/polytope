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

import org.goodmath.polytope.PolytopeException
import org.litote.kmongo.Id
import org.litote.kmongo.id.StringId
import org.litote.kmongo.toId

sealed interface ProjectVersionSpecifier {
    enum class Kind {
        History, ChangeId, ChangeName, ChangeStepId
    }

    val kind: Kind
    val project: String

    data class HistoryPVS(
        override val project: String,
        val historyName: String,
        val version: Int?
    ) : ProjectVersionSpecifier {
        override val kind: Kind = Kind.History
        override fun toString(): String {
            return if (version == null) {
                return "history($project@$historyName)"
            } else {
                return "history($project@historyName@$version)"
            }
        }
    }

    data class ChangeIdPVS(
        override val project: String,
        val changeId: Id<Change>
    ) : ProjectVersionSpecifier {
        override val kind: Kind = Kind.ChangeId

        override fun toString(): String {
            return "changeId($project@$changeId)"
        }
    }

    data class ChangeNamePVS(
        override val project: String,
        val changeName: String
    ) : ProjectVersionSpecifier {
        override val kind: Kind = Kind.ChangeName
        override fun toString(): String {
            return "changeName($project@$changeName)"
        }
    }

    data class ChangeStepPVS(
        override val project: String,
        val changeStepId: Id<ChangeStep>
    ) : ProjectVersionSpecifier {
        override val kind: Kind = Kind.ChangeStepId
        override fun toString(): String {
            return "changeStep($project@$changeStepId)"
        }
    }

    companion object {
        private val pvsRe = Regex("(\\w*)\\((.*)\\)")

        // history(project@history) | history(project@history@version)
        // changeName(project@name)
        // changeId(project@id)
        // changeSteph(project@id)
        fun fromString(s: String): ProjectVersionSpecifier {
            val matches = pvsRe.matchEntire(s) ?: throw PolytopeException(
                    PolytopeException.Kind.InvalidParameter,
                    "Invalid project version specifier"
                )
            val (kind, spec) = matches.destructured
            return when (kind) {
                "history" -> {
                    val specParts = spec.split("@")
                    return if (specParts.size == 2) {
                        ProjectVersionSpecifier.HistoryPVS(specParts[0], specParts[1], null)
                    } else if (specParts.size == 3) {
                        ProjectVersionSpecifier.HistoryPVS(
                            specParts[0],
                            specParts[1], specParts[2].toInt()
                        )
                    } else {
                        throw PolytopeException(
                            PolytopeException.Kind.InvalidParameter,
                            "Invalid project version specifier string '$s'"
                        )
                    }
                }
                "changeName" -> {
                    val specParts = spec.split("@")
                    return if (specParts.size == 2) {
                        ProjectVersionSpecifier.ChangeNamePVS(
                            specParts[0],
                            specParts[1]
                        )
                    } else {
                        throw PolytopeException(
                            PolytopeException.Kind.InvalidParameter,
                            "Invalid project version specifier string '$s'"
                        )
                    }
                }
                "changeId" -> {
                    val specParts = spec.split("@")
                    return if (specParts.size == 2) {
                        ProjectVersionSpecifier.ChangeIdPVS(
                            specParts[0], specParts[1].toId()
                        )
                    } else {
                        throw PolytopeException(
                            PolytopeException.Kind.InvalidParameter,
                            "Invalid project version specifier string '$s'"
                        )
                    }
                }
                "changeStepId" -> {
                    val specParts = spec.split("@")
                    return if (specParts.size == 2) {
                        ProjectVersionSpecifier.ChangeStepPVS(
                            specParts[0], StringId<ChangeStep>(specParts[1])
                        )
                    } else {
                        throw PolytopeException(
                            PolytopeException.Kind.InvalidParameter,
                            "Invalid project version specifier string '$s'"
                        )
                    }
                }
                else ->
                    throw PolytopeException(
                        PolytopeException.Kind.InvalidParameter,
                        "Invalid project version specifiec type in '$s'"
                    )
            }
        }
    }
}