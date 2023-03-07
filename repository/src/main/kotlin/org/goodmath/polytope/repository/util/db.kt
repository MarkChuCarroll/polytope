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

package org.goodmath.polytope.repository.util

import maryk.rocksdb.ColumnFamilyHandle
import maryk.rocksdb.RocksDB
import kotlin.text.Charsets.UTF_8



fun RocksDB.put(key: String, value: String) {
    val keyBytes = key.toByteArray(UTF_8)
    val valueBytes = value.toByteArray(UTF_8)
    this.put(keyBytes, valueBytes)
}

fun RocksDB.put(cf: ColumnFamilyHandle, key: String, value: String) {
    val keyBytes = key.toByteArray(UTF_8)
    val valueBytes = value.toByteArray(UTF_8)
    this.put(cf, keyBytes, valueBytes)
}

fun<T> RocksDB.putTyped(key: String, value: T) {
    this.put(key, ParsingCommons.klaxon.toJsonString(value))
}

fun<T> RocksDB.putTyped(cf: ColumnFamilyHandle, key: String, value: T) {
    this.put(cf, key, ParsingCommons.klaxon.toJsonString(value))
}

inline fun<reified T> RocksDB.getTyped(key: String): T? {
    val keyBytes = key.toByteArray(UTF_8)
    val resultBytes = this.get(keyBytes)
    return ParsingCommons.klaxon.parse<T>(resultBytes)
}

inline fun<reified T> RocksDB.getTyped(cf: ColumnFamilyHandle, key: String): T? {
    val keyBytes = key.toByteArray(UTF_8)
    val resultBytes = this.get(cf, keyBytes)
    return ParsingCommons.klaxon.parse<T>(resultBytes)
}

inline fun<reified T> RocksDB.list(cf: ColumnFamilyHandle,
                                   pred: (T) -> Boolean = {t: T -> true}): List<T> {
    val iter = this.newIterator(cf)
    val result: MutableList<T> = mutableListOf()
    iter.seekToFirst()
    while (iter.isValid) {
        val next = ParsingCommons.klaxon.parse<T>(iter.value())
        if (next != null && pred(next)) {
            result.add(next)
        }
    }
    iter.close()
    return result
}