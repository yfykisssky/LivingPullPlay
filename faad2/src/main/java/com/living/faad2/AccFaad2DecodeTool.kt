package com.living.faad2

class AccFaad2DecodeTool {

    fun startFaad2Engine(
        type: Int,
        fomtType: Int,
        sampleRate: Long,
        channels: Int
    ) {
        AccFaad2NativeJni.startFaad2Engine(type, fomtType, sampleRate, channels)
    }

    fun stopFaad2Engine() {
        AccFaad2NativeJni.stopFaad2Engine()
    }

    fun convertToPcm(aacBytes: ByteArray, pcmSizes: Int): ByteArray? {
        return AccFaad2NativeJni.convertToPcm(aacBytes, pcmSizes)
    }

}