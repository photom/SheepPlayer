package com.hitsuji.sheepplayer2.domain.model

@JvmInline
value class Duration(val milliseconds: Long) {
    init {
        require(milliseconds >= 0) { "Duration cannot be negative" }
    }

    fun toFormattedString(): String {
        val totalSeconds = milliseconds / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return "%d:%02d".format(minutes, seconds)
    }

    companion object {
        val ZERO = Duration(0)
    }
}
