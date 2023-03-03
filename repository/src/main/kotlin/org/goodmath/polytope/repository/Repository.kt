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
import org.goodmath.polytope.repository.data.*
import org.goodmath.polytope.repository.storage.SimpleFileStorage
import java.nio.file.Path

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
        fun initializeStorage(cfg: Config) {
            val mongoClient = MongoClients.create(cfg.db.serverUrl)
            val db = mongoClient.getDatabase(cfg.db.dbName)
            Users.initializeStorage(cfg, db)
            Artifacts.initializeStorage(cfg, db)
            Changes.initializeStorage(cfg, db)
            Histories.initializeStorage(cfg,db)
            Projects.initializeStorage(cfg, db)
        }
    }
    private val mongoClient = MongoClients.create(cfg.db.serverUrl)
    private val db: MongoDatabase = mongoClient.getDatabase(cfg.db.dbName)

    val users = Users(db, this)
    val artifacts = Artifacts(db, this)
    val changes = Changes(db, this)
    val histories = Histories(db, this)
    val projects = Projects(db, this)
    val storage = SimpleFileStorage(Path.of(cfg.storage.location))


}