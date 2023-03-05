package org.goodmath.polytope.repository.data

import com.beust.klaxon.Klaxon
import maryk.rocksdb.RocksDB
import maryk.rocksdb.openRocksDB
import org.goodmath.polytope.repository.util.getTyped
import org.goodmath.polytope.repository.util.putTyped
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

data class MyTest(
    val one: String,
    val two: Int,
    val three: List<String>
)

class RocksDbTest {
    @Test
    fun testRocks() {
        val k = Klaxon()
        val db = openRocksDB("mytest")
        val mt = MyTest("one", 2, listOf("t", "h", "r", "e", "e"))
        db.putTyped("first", mt)
        val back = db.getTyped<MyTest>("first")
        assertEquals(mt, back)
        db.close()
    }
}