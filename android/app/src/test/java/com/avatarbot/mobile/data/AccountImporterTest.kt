package com.avatarbot.mobile.data

import org.junit.Assert.*
import org.junit.Test

class AccountImporterTest {
    @Test fun multiplePcFormats() {
        val parsed = AccountImporter.parse("""
            # demo
            one p1 Main account
            two,p2,Secondary
            three;p3
            four|p4
            five-p5
        """.trimIndent())
        assertEquals(5, parsed.records.size)
        assertTrue(parsed.issues.isEmpty())
        assertEquals("Main account", parsed.records[0].label)
        assertEquals("p5", parsed.records.last().password)
    }

    @Test fun rejectsDuplicatesWithoutLeakingPasswords() {
        val result = AccountImporter.parse("x|private1\nX|private2\ninvalid")
        assertEquals(1, result.records.size)
        assertEquals(2, result.issues.size)
        assertFalse(result.issues.toString().contains("private"))
    }
}
