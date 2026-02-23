package com.hitsuji.sheepplayer2.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class DurationTest {

    @Test
    fun `equality works for same milliseconds`() {
        assertEquals(Duration(1000), Duration(1000))
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws for negative milliseconds`() {
        Duration(-1)
    }

    @Test
    fun `toFormattedString formats correctly`() {
        assertEquals("0:00", Duration(0).toFormattedString())
        assertEquals("0:01", Duration(1000).toFormattedString())
        assertEquals("1:30", Duration(90000).toFormattedString())
        assertEquals("10:05", Duration(605000).toFormattedString())
    }
}
