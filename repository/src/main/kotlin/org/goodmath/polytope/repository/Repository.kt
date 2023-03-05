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
package org.goodmath.polytope.repository

import com.mongodb.client.MongoClients
import com.mongodb.client.MongoDatabase
import maryk.rocksdb.RocksDB
import maryk.rocksdb.openRocksDB
import org.goodmath.polytope.repository.data.*
import org.goodmath.polytope.repository.storage.SimpleFileStorage
import org.rocksdb.ColumnFamilyDescriptor
import org.rocksdb.ColumnFamilyHandle
import java.nio.file.Path
import kotlin.text.Charsets.UTF_8


data class StorageConfig(
    val kind: String,
    val location: String
)

data class MongoConfig(
    val serverUrl: String,
    val dbName: String
)

data class Config(
    val storage: StorageConfig,
    val db: MongoConfig,
    val rootUser: String,
    val password: String
)

class Repository(val cfg: Config) {
    companion object {
        val families= listOf(
            "users", "arifacts", "versions", "changes",
            "steps", "histories", "historyVersions", "projects"
        )
        fun initializeStorage(cfg: Config) {
            //val mongoClient = MongoClients.create(cfg.db.serverUrl)
            //val db = mongoClient.getDatabase(cfg.db.dbName)
            val rocksDb = openRocksDB(cfg.db.dbName)
            rocksDb.createColumnFamilies(families.map {
                ColumnFamilyDescriptor(it.toByteArray(UTF_8))
            })
            rocksDb.close()
            /*
            Users.initializeStorage(cfg, db)
            Artifacts.initializeStorage(cfg, db)
            Changes.initializeStorage(cfg, db)
            Histories.initializeStorage(cfg,db)
            Projects.initializeStorage(cfg, db)
            */
         }
    }

    val db: RocksDB
    val cfHandles: Map<String, ColumnFamilyHandle>

    init {
        val handles: MutableList<ColumnFamilyHandle> = mutableListOf()
        val descriptors = families.map { ColumnFamilyDescriptor(it.toByteArray(UTF_8)) }
        db = openRocksDB(cfg.db.dbName, descriptors, handles)
        cfHandles = handles.associateBy { h -> h.name.toString(UTF_8) }

    }

    val users = Users(db, cfHandles["users"]!!, this)
    val artifacts = Artifacts(db, cfHandles["artifacts"]!!,
        cfHandles["versions"]!!, this)
    val changes = Changes(db, cfHandles["changes"]!!,
        cfHandles["steps"]!!, this)
    val histories = Histories(db, cfHandles["histories"]!!,
        cfHandles["historyVersions"]!!, this)
    val projects = Projects(db, cfHandles["projects"]!!, this)

    val storage = SimpleFileStorage(Path.of(cfg.storage.location))


}