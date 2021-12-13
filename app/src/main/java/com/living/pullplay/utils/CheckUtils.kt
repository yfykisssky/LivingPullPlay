package com.living.pullplay.utils

import android.util.Log

enum class FrameType {
    SPS_FRAME,
    PPS_FRAME,
    I_FRAME,
    OTHER_FRAME,
    NONE_FRAME
}

class CheckUtils {
    companion object {
        //00 00 00 01后面的16进制数
        //0x67为sps，0x68为pps，0x65为关键帧，0x41不是关键帧
        fun judgeBytesFrameKind(bytes: ByteArray?): FrameType {
            bytes?.let { data ->
                if (data.size >= 5) {
                    when (data[4]) {
                        0x65.toByte() -> {
                            Log.e("FrameType","I")
                            return FrameType.I_FRAME
                        }
                        0x68.toByte() -> {
                            Log.e("FrameType","PPS")
                            return FrameType.PPS_FRAME
                        }
                        0x67.toByte() -> {
                            Log.e("FrameType","SPS")
                            return FrameType.SPS_FRAME
                        }
                        0x41.toByte() -> {
                            Log.e("FrameType","OTHER")
                            return FrameType.OTHER_FRAME
                        }
                    }
                }
            }
            Log.e("FrameType","NONE")
            return FrameType.NONE_FRAME
        }
    }
}