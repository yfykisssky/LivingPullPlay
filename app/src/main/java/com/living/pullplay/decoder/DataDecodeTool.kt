package com.living.pullplay.decoder

import java.nio.ByteBuffer
import java.nio.ByteOrder

class DataDecodeTool {

    companion object {

        private const val SIZE_LENGTH = 4
        private const val TYPE_LENGTH = 1
        private const val TIME_STAMP_LENGTH = 8

        const val EXTRA_DATA_LENGTH = SIZE_LENGTH + TYPE_LENGTH + TIME_STAMP_LENGTH

        private val headerBuffer =
            ByteBuffer.allocate(EXTRA_DATA_LENGTH).order(ByteOrder.LITTLE_ENDIAN)

        private fun byteToBoolean(i: Byte): Boolean {
            return i != 0x00.toByte()
        }

        fun deExtraData(array: ByteArray): Triple<Int, Boolean, Long> {

            headerBuffer.clear()
            headerBuffer.put(array)
            headerBuffer.rewind()
            val size = headerBuffer.int
            val type = byteToBoolean(headerBuffer.get())
            val timeStamp = headerBuffer.long

            return Triple(size, type, timeStamp)
        }

    }

}