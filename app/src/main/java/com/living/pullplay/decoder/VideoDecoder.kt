package com.living.pullplay.decoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.Surface
import com.living.pullplay.utils.CheckUtils
import com.living.pullplay.utils.FrameType
import java.util.ArrayList
import java.util.concurrent.LinkedBlockingQueue

class VideoDecoder {

    private var bitRate = 0
    private var maxFps = 0
    private var frameWith = 0
    private var frameHeight = 0

    private var queueVideoFrame: LinkedBlockingQueue<VideoFrame>? = null

    private var codec: MediaCodec? = null
    private var decodeInThread: Thread? = null
    private var decodeOutThread: Thread? = null
    private var isDecoding = false
    private var outputSurface: Surface? = null

    fun updateDecodeSettings(
        bitRate: Int,
        maxFps: Int,
        frameWith: Int,
        frameHeight: Int,
        outputSurface: Surface?
    ) {
        this.bitRate = bitRate
        this.maxFps = maxFps
        this.frameWith = frameWith
        this.frameHeight = frameHeight
        this.outputSurface = outputSurface
    }

    fun initDecoder(): Boolean {
        try {
            codec = MediaCodec.createDecoderByType(MediaFormat.MIMETYPE_VIDEO_AVC)
            configEncoder()
            return true
        } catch (e: Exception) {
            e.printStackTrace()
            //dataCallBackListener?.onLogTest(e.message ?: "")
        }
        return false
    }

    fun startDecode() {
        queueVideoFrame = LinkedBlockingQueue<VideoFrame>()
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
        // decodeInThread?.join()
        decodeOutThread?.join()
    }

    fun stopDecode() {
        endDecode()
        releaseEncoder()
    }

    fun resetDecode() {
        endDecode()
        codec?.reset()
        configEncoder()
        beginDecode()
    }

    private fun getEncodeFormat(): MediaFormat {
        val format = MediaFormat()
        format.setString(MediaFormat.KEY_MIME, MediaFormat.MIMETYPE_VIDEO_AVC)
        format.setInteger(MediaFormat.KEY_BIT_RATE, bitRate)
        format.setInteger(MediaFormat.KEY_FRAME_RATE, maxFps)
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_WIDTH, frameWith)
        format.setInteger(MediaFormat.KEY_HEIGHT, frameHeight)
        return format
    }

    private fun configEncoder() {
        try {
            codec?.configure(getEncodeFormat(), outputSurface, null, 0)
        } catch (e: Exception) {
            e.printStackTrace()
            // dataCallBackListener?.onLogTest(e.message ?: "")
        }
    }

    private fun releaseEncoder() {
        codec?.stop()
        codec?.release()
    }

    private var timeStamp = 0L

    private inner class DecodeInRunnable : Runnable {

        var isSpsDecode = false

        override fun run() {

            while (isDecoding) {

                val inIndex = codec?.dequeueInputBuffer(0) ?: -1
                if (inIndex >= 0) {

                    queueVideoFrame?.take()?.let { frame ->
                        frame.byteArray?.let { array ->

                            if (FrameType.SPS_FRAME == CheckUtils.judgeBytesFrameKind(array)) {
                                if (isSpsDecode) {
                                    val collections = ArrayList<VideoFrame>()
                                    queueVideoFrame?.drainTo(collections)
                                    collections.add(0, frame)
                                    queueVideoFrame?.clear()
                                    queueVideoFrame?.addAll(collections)
                                    timeStamp = System.currentTimeMillis()
                                    Looper.prepare()
                                    Handler().post {
                                        resetDecode()
                                    }
                                    Looper.loop()

                                    return
                                } else {
                                    isSpsDecode = true
                                }
                            }

                            //填充数据
                            val byteBuffer = codec?.getInputBuffer(inIndex)
                            byteBuffer?.clear()
                            byteBuffer?.put(array)
                            codec?.queueInputBuffer(
                                inIndex,
                                0,
                                array.size,
                                frame.timestamp,
                                0
                            )

                        }
                    }
                }
            }

        }
    }

    private inner class DecodeOutRunnable : Runnable {

        override fun run() {
            val info = MediaCodec.BufferInfo()
            while (isDecoding) {
                //解码
                var outIndex = codec?.dequeueOutputBuffer(info, 0) ?: -1
                while (outIndex >= 0) {
                    codec?.releaseOutputBuffer(outIndex, info.size != 0)
                    outIndex = codec?.dequeueOutputBuffer(info, 0) ?: -1
                }
                /*    if (outIndex >= 0) {
                        codec?.releaseOutputBuffer(outIndex, info.size != 0)
                    } else {
                        when (outIndex) {
                            MediaCodec.INFO_TRY_AGAIN_LATER -> {
                            }
                            MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                                val newFormat = codec?.outputFormat
                                val videoWidth = newFormat?.getInteger("width")
                                val videoHeight = newFormat?.getInteger("height")
                            }
                            else -> {
                            }
                        }
                    }*/
            }
        }
    }

    fun onReceived(frame: VideoFrame) {
        queueVideoFrame?.put(frame)
    }

}