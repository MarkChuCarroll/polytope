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


enum class Action {
    Read, Write, Delete, Admin
}

sealed interface Permission {
    fun permitsRepositoryAction(action: Action): Boolean
    fun permitsProjectAction(action: Action, onProject: String): Boolean

    data class ProjectPermission(val project: String, val level: Action): Permission {
        override fun permitsRepositoryAction(action: Action): Boolean = false

        override fun permitsProjectAction(action: Action, onProject: String): Boolean {
            return (project == onProject && action <= level)
        }

        override fun toString(): String {
            return "project($level:$project)"
        }

    }

    data class RepositoryPermission(val level: Action): Permission {
        override fun permitsRepositoryAction(action: Action): Boolean {
            return action <= level
        }

        override fun permitsProjectAction(action: Action, onProject: String): Boolean {
            return false
        }

        override fun toString(): String {
            return "repos($level)"
        }
    }

    data class GlobalPermission(val level: Action): Permission {
        override fun permitsRepositoryAction(action: Action): Boolean {
            return action <= level
        }

        override fun permitsProjectAction(action: Action, onProject: String): Boolean {
            return action <= level
        }

        override fun toString(): String {
            return "global($level)"
        }
    }

    companion object {
        // external syntax:
        // global(ACTION)
        // repos(ACTION)
        // project(ACTION:PROJECT)
        val re = Regex("(\\w+)\\((\\w*)(:\\w+)?\\)")
        fun fromString(perm: String): Permission? {
            val match = re.matchEntire(perm)
            return if (match == null) {
                null
            } else {
                val (kind, actionStr, target) = match.destructured
                val action = Action.valueOf(actionStr)
                when(kind) {
                    "global" -> GlobalPermission(action)
                    "repos" -> RepositoryPermission(action)
                    "project" -> ProjectPermission(target.substring(1), action)
                    else -> throw PolytopeException(
                        PolytopeException.Kind.InvalidParameter,
                        "Invalid permission string $perm")
                }
            }
        }
    }
}