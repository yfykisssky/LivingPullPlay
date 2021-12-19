package com.living.pullplay.decoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import com.living.faad2.AccFaad2DecodeTool
import com.living.pullplay.utils.RecLogUtils
import java.util.concurrent.LinkedBlockingQueue

class AudioSoftDecoder {

    companion object {
        private const val MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC
        private const val M_CONFIGURE_FLAG_DECODE = 0
    }

    private var queueAudioFrame: LinkedBlockingQueue<AudioFrame>? = null

    private var decodeInThread: Thread? = null
    private var isDecoding = false

    private var accFaad2DecodeTool: AccFaad2DecodeTool? = null

    interface DecodeDataCallBack {
        fun onDataCallBack(bytes: ByteArray, timeStamp: Long)
    }

    private var deDecodeDataCallBack: DecodeDataCallBack? = null
    fun setDecodeDataCallBack(deDecodeDataCallBack: DecodeDataCallBack?) {
        this.deDecodeDataCallBack = deDecodeDataCallBack
    }

    fun initDecoder(): Boolean {
        try {
            accFaad2DecodeTool = AccFaad2DecodeTool()

            accFaad2DecodeTool?.startFaad2Engine(
                1,
                1,
                AudioConstants.SAMPLE_RATE.toLong(),
                AudioConstants.CHANNEL
            )
            return true
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return false
    }


    fun startDecode() {
        queueAudioFrame = LinkedBlockingQueue<AudioFrame>()
        beginDecode()
    }

    private fun beginDecode() {
        isDecoding = true
        decodeInThread = Thread(DecodeInRunnable())
        decodeInThread?.start()
    }

    private fun endDecode() {
        isDecoding = false
        decodeInThread?.interrupt()
        decodeInThread?.join()
    }

    fun stopDecode() {
        endDecode()
        releaseEncoder()
    }

    private fun releaseEncoder() {
        accFaad2DecodeTool?.stopFaad2Engine()
    }

    private inner class DecodeInRunnable : Runnable {

        val pcmArraySize = AudioConstants.getSampleDataSize()

        override fun run() {
            try {
                while (isDecoding) {

                    queueAudioFrame?.take()?.let { frame ->
                        frame.byteArray?.let { array ->

                            //todo:
                            accFaad2DecodeTool?.convertToPcm(array, 4096)?.let { pcmData ->
                                deDecodeDataCallBack?.onDataCallBack(pcmData, frame.timestamp)
                            }

                        }
                    }

                }
            } catch (e: Exception) {
            }
        }
    }

    fun onReceived(frame: AudioFrame) {
        queueAudioFrame?.put(frame)
    }

}