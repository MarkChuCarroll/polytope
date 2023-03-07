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

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.goodmath.polytope.PolytopeException
import org.goodmath.polytope.repository.util.Id

@Serializable
sealed class ProjectVersionSpecifier {
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
            when (kind) {
                "history" -> {
                    val specParts = spec.split("@")
                    return when (specParts.size) {
                        2 -> {
                            HistoryPVS(specParts[0], specParts[1], null)
                        }
                        3 -> {
                            HistoryPVS(
                                specParts[0],
                                specParts[1], specParts[2].toInt()
                            )
                        }
                        else -> {
                            throw PolytopeException(
                                PolytopeException.Kind.InvalidParameter,
                                "Invalid project version specifier string '$s'"
                            )
                        }
                    }
                }
                "changeName" -> {
                    val specParts = spec.split("@")
                    return if (specParts.size == 2) {
                        ChangeNamePVS(
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
                        ChangeIdPVS(
                            specParts[0], specParts[1]
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
                        ChangeStepPVS(
                            specParts[0], specParts[1]
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

@Serializable
@SerialName("history")
data class HistoryPVS(
    val project: String,
    val historyName: String,
    val version: Int?
) : ProjectVersionSpecifier() {
    override fun toString(): String =
        if (version == null) {
            "history($project@$historyName)"
        } else {
            "history($project@historyName@$version)"
        }
}

@Serializable
@SerialName("changeid")
data class ChangeIdPVS(
    val project: String,
    val changeId: Id<Change>
) : ProjectVersionSpecifier() {
    override fun toString(): String {
        return "changeId($project@$changeId)"
    }
}

@Serializable
@SerialName("changename")
data class ChangeNamePVS(
    val project: String,
    val changeName: String
) : ProjectVersionSpecifier() {
    override fun toString(): String =
        "changeName($project@$changeName)"
}

@Serializable
@SerialName("changestep")
data class ChangeStepPVS(
    val project: String,
    val changeStepId: Id<ChangeStep>
) : ProjectVersionSpecifier() {
    override fun toString(): String =
        "changeStep($project@$changeStepId)"
}

