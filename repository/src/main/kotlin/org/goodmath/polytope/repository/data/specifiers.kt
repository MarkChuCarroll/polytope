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

sealed interface ProjectVersionSpecifier {
    enum class Kind {
        History, ChangeId, ChangeName, ChangeStepId
    }

    val kind: Kind
    val project: String

    data class HistoryPVS(
        override val project: String,
        val historyName: String,
        val version: Integer?
    ) : ProjectVersionSpecifier {
        override val kind: Kind = Kind.History
    }

    data class ChangeIdPVS(
        override val project: String,
        val changeId: ChangeId
    ) : ProjectVersionSpecifier {
        override val kind: Kind = Kind.ChangeId
    }

    data class ChangeNamePVS(
        override val project: String,
        val changeName: String
    ) : ProjectVersionSpecifier {
        override val kind: Kind = Kind.ChangeName
    }

    data class ChangeStepPVS(
        override val project: String,
        val changeStepId: ChangeStepId
    ) : ProjectVersionSpecifier {
        override val kind: Kind = Kind.ChangeStepId
    }
}