package org.goodmath.polytope.repository.data

import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.litote.kmongo.id.StringId
import org.litote.kmongo.newId
import java.util.*

class ProjectVersionSpecifierTest {
    @Test
    fun testParseProjectVersionSpecifier() {
        val one = ProjectVersionSpecifier.fromString("history(pr@mine@17)")
        assertEquals(ProjectVersionSpecifier.HistoryPVS("pr", "mine", 17), one)
        val two = ProjectVersionSpecifier.fromString("history(pr@mine)")
        assertEquals(ProjectVersionSpecifier.HistoryPVS("pr", "mine", null), two)
        val three = ProjectVersionSpecifier.fromString("changeName(pr@thisone)")
        assertEquals(ProjectVersionSpecifier.ChangeNamePVS("pr", "thisone"), three)
        val id = StringId<Change>(UUID.randomUUID().toString())
        val four = ProjectVersionSpecifier.fromString("changeId(ps@$id)")
        assertEquals(ProjectVersionSpecifier.ChangeIdPVS("ps", id).toString(), four.toString())
    }
}