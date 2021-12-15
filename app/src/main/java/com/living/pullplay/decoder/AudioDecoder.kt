package com.living.pullplay.decoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import com.living.pullplay.utils.RecLogUtils
import java.util.concurrent.LinkedBlockingQueue

class AudioDecoder {

    companion object {
        private const val MIME_TYPE = MediaFormat.MIMETYPE_AUDIO_AAC
        private const val M_CONFIGURE_FLAG_DECODE = 0
    }

    private var queueAudioFrame: LinkedBlockingQueue<AudioFrame>? = null

    private var codec: MediaCodec? = null
    private var decodeInThread: Thread? = null
    private var decodeOutThread: Thread? = null
    private var isDecoding = false

    interface DecodeDataCallBack {
        fun onDataCallBack(bytes: ByteArray, timeStamp: Long)
    }

    private var deDecodeDataCallBack: DecodeDataCallBack? = null
    fun setDecodeDataCallBack(deDecodeDataCallBack: DecodeDataCallBack?) {
        this.deDecodeDataCallBack = deDecodeDataCallBack
    }

    fun initDecoder(): Boolean {
        try {
            codec = MediaCodec.createDecoderByType(MIME_TYPE)
            configEncoder()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            //dataCallBackListener?.onLogTest(e.message ?: "")
        }
        return false
    }

    fun startDecode() {
        queueAudioFrame = LinkedBlockingQueue<AudioFrame>()
        beginDecode()
    }

    private fun beginDecode() {
        codec?.start()
        isDecoding = true
        decodeInThread = Thread(DecodeInRunnable())
        decodeInThread?.start()
        decodeOutThread = Thread(DecodeOutRunnable())
        decodeOutThread?.start()
    }

    private fun endDecode() {
        isDecoding = false
        decodeInThread?.interrupt()
        decodeInThread?.join()
        decodeOutThread?.join()
    }

    fun stopDecode() {
        endDecode()
        releaseEncoder()
    }

    private fun getEncodeFormat(): MediaFormat {
        val format = MediaFormat.createAudioFormat(
            MIME_TYPE,
            AudioConstants.SAMPLE_RATE,
            AudioConstants.CHANNEL
        )
        format.setInteger(
            MediaFormat.KEY_AAC_PROFILE,
            MediaCodecInfo.CodecProfileLevel.AACObjectLC
        )
        return format
    }

    private fun configEncoder() {
        try {
            codec?.configure(getEncodeFormat(), null, null, 0)
        } catch (e: Exception) {
            e.printStackTrace()
            // dataCallBackListener?.onLogTest(e.message ?: "")
        }
    }

    private fun releaseEncoder() {
        try {
            codec?.stop()
        } catch (e: Exception) {
        }
        codec?.release()
    }

    private inner class DecodeInRunnable : Runnable {

        override fun run() {
            try {
                while (isDecoding) {

                    val inIndex = codec?.dequeueInputBuffer(0) ?: -1
                    if (inIndex >= 0) {

                        queueAudioFrame?.take()?.let { frame ->
                            frame.byteArray?.let { array ->

                                //填充数据
                                val byteBuffer = codec?.getInputBuffer(inIndex)
                                byteBuffer?.clear()
                                byteBuffer?.put(array)
                                codec?.queueInputBuffer(
                                    inIndex,
                                    0,
                                    array.size,
                                    frame.timestamp * 1000,
                                    0
                                )

                            }
                        }
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private inner class DecodeOutRunnable : Runnable {

        private val vBufferInfo = MediaCodec.BufferInfo()
        private var decodeStartTimeStamp = 0L

        fun handlePts(): Long {
            if (decodeStartTimeStamp == 0L) {
                decodeStartTimeStamp = vBufferInfo.presentationTimeUs
            }
            val ptsNew = (vBufferInfo.presentationTimeUs - decodeStartTimeStamp) / 1000
            RecLogUtils.logAudioTimeStamp(ptsNew)
            return ptsNew
        }

        override fun run() {

            while (isDecoding) {
                //解码
                var outIndex = codec?.dequeueOutputBuffer(vBufferInfo, 0) ?: -1
                while (outIndex >= 0) {
                    codec?.getOutputBuffer(outIndex)?.let { encodedData ->
                        val dataToWrite = ByteArray(vBufferInfo.size)
                        encodedData[dataToWrite, 0, vBufferInfo.size]
                        deDecodeDataCallBack?.onDataCallBack(dataToWrite, handlePts())
                    }
                    codec?.releaseOutputBuffer(outIndex, false)
                    outIndex = codec?.dequeueOutputBuffer(vBufferInfo, 0) ?: -1
                }
            }
        }
    }

    fun onReceived(frame: AudioFrame) {
        queueAudioFrame?.put(frame)
    }

}