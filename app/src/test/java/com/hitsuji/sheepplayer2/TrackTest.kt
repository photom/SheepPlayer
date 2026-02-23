package com.hitsuji.sheepplayer2

import org.junit.Test

class TrackTest {

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws for negative duration`() {
        Track(
            id = 1,
            title = "Valid Title",
            artistName = "Artist",
            albumName = "Album",
            duration = -1,
            filePath = "/path/song.mp3"
        )
    }

    @Test(expected = IllegalArgumentException::class)
    fun `constructor throws for blank title`() {
        Track(
            id = 1,
            title = " ",
            artistName = "Artist",
            albumName = "Album",
            duration = 1000,
            filePath = "/path/song.mp3"
        )
    }

    @Test
    fun `constructor succeeds for valid data`() {
        Track(
            id = 1,
            title = "Valid Title",
            artistName = "Artist",
            albumName = "Album",
            duration = 1000,
            filePath = "/path/song.mp3"
        )
    }
}
