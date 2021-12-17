package com.living.pullplay.decoder

import java.nio.ByteBuffer
import java.nio.ByteOrder

class DataDecodeTool {

    companion object {

        private const val SIZE_IS_SOCKET_ACK = 1

        private const val SIZE_LENGTH = 4
        private const val TYPE_LENGTH = 1
        private const val TIME_STAMP_LENGTH = 8

        const val EXTRA_DATA_LENGTH =
            SIZE_IS_SOCKET_ACK + SIZE_LENGTH + TYPE_LENGTH + TIME_STAMP_LENGTH

        private val headerBuffer =
            ByteBuffer.allocate(EXTRA_DATA_LENGTH).order(ByteOrder.LITTLE_ENDIAN)

        private fun byteToBoolean(i: Byte): Boolean {
            return i != 0x00.toByte()
        }

        fun deExtraData(array: ByteArray): RecData {

            headerBuffer.clear()
            headerBuffer.put(array)
            headerBuffer.rewind()
            val isAck = byteToBoolean(headerBuffer.get())
            val size = headerBuffer.int
            val type = byteToBoolean(headerBuffer.get())
            val timeStamp = headerBuffer.long

            return RecData(isAck, size, type, timeStamp)
        }

    }

}

data class RecData(
    var isSocketAck: Boolean = false,
    var size: Int = 0,
    var type: Boolean = false,
    var timeStamp: Long = 0L
)