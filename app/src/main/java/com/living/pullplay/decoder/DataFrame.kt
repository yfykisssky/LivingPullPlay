package com.living.pullplay.decoder

class VideoFrame(
    var byteArray: ByteArray? = null,
    var timestamp: Long = 0L
)

class AudioFrame(
    var byteArray: ByteArray? = null,
    var timestamp: Long = 0L,
    var isHeader: Boolean = false
)
