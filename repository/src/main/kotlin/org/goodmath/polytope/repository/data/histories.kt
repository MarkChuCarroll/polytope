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
import java.time.Instant
import org.goodmath.polytope.repository.Config
import org.goodmath.polytope.repository.Repository
import org.goodmath.polytope.repository.util.Id

data class History(
    val id: Id<History>,
    val name: String,
    val description: String,
    val timestamp: Instant,
    val basis: ProjectVersionSpecifier,
    val latest: Int
)

data class HistoryVersion(
    val id: Id<HistoryVersion>,
    val project: String,
    val historyName: String,
    val version: Int,
    val baselineId: Id<Artifact>,
    val baselineVersionId: Id<ArtifactVersion>
)


class Histories(val db: RocksDB,
    val historiesColumn: ColumnFamilyHandle,
    val historyVersionsColumn: ColumnFamilyHandle,
    val repos: Repository) {

    fun withAuth(auth: AuthenticatedUser) : AuthenticatedHistoriesDatastore {
        return AuthenticatedHistoriesDatastore(auth)

    }

    class AuthenticatedHistoriesDatastore(val auth: AuthenticatedUser) {
        fun retrieveHistory(project: String, history: String): History {
            TODO()

        }

        fun currentVersion(project: String, history: String): Int {
            TODO()
        }

        fun versions(project: String, history: String, limit: Int?): List<HistoryVersion> {
            TODO()
        }

        fun create(
            project: String, name: String, description: String,
            basis: ProjectVersionSpecifier
        ): HistoryVersion {
            TODO()
        }

        fun addVersion(
            project: String, history: String,
            change: Id<Change>, baseline: Id<ArtifactVersion>
        ): HistoryVersion {
            TODO()

        }

        fun list(project: String): List<History> {
            TODO()
        }
    }

}
