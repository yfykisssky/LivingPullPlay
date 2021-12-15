package com.living.pullplay.decoder

import android.media.MediaCodec
import android.media.MediaCodecInfo
import android.media.MediaFormat
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.Message
import android.view.Surface
import android.view.TextureView
import com.living.pullplay.play.tool.video.gl.TextureVideoFrame
import com.living.pullplay.play.tool.video.gl.ToSurfaceViewFrameRender
import com.living.pullplay.play.tool.video.gl.VideoRender
import com.living.pullplay.utils.CheckUtils
import com.living.pullplay.utils.FrameType
import com.living.pullplay.utils.RecLogUtils
import java.lang.ref.WeakReference
import java.util.*
import java.util.concurrent.LinkedBlockingQueue

class VideoDecoder {

    companion object {
        private const val MSG_RESET_DECODER = 1
    }

    private var frameWidth = 0
    private var frameHeight = 0

    @Volatile
    private var queueVideoFrame: LinkedBlockingQueue<VideoFrame>? = null

    private var codec: MediaCodec? = null
    private var decodeInThread: Thread? = null
    private var decodeOutThread: Thread? = null

    private var isDecoding = false
    var outputSurface: Surface? = null

    private var videoRender: VideoRender? = null
    private var toSurfaceFrameRender: ToSurfaceViewFrameRender? = null

    private var videoDecoderHandlerThread: HandlerThread? = null

    @Volatile
    private var mHandleHandler: HandleHandler? = null

    fun isisDecoding(): Boolean {
        return isDecoding
    }

    fun setDecodeSettings(
        frameWidth: Int,
        frameHeight: Int
    ) {
        this.frameWidth = frameWidth
        this.frameHeight = frameHeight
    }

    fun setRenderView(textureView: TextureView?) {
        if (toSurfaceFrameRender == null) {
            toSurfaceFrameRender = ToSurfaceViewFrameRender()
        }
        toSurfaceFrameRender?.updateRenderTextureView(textureView)
    }

    fun initDecoder(): Boolean {
        try {
            videoRender = VideoRender()
            videoRender?.setVideoRenderCallBack(object : VideoRender.VideoRenderCallBack {
                override fun onDataCallBack(frame: TextureVideoFrame) {
                    toSurfaceFrameRender?.onRenderVideoFrame(frame)
                }
            })
            videoRender?.updateFrameSize(frameWidth, frameHeight)
            outputSurface = videoRender?.initRender()
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

    private class HandleHandler(looper: Looper, reference: VideoDecoder) :
        Handler(looper) {

        private val readerWeakReference = WeakReference(reference)

        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)

            readerWeakReference.get()?.let { reference ->
                when (msg.what) {
                    MSG_RESET_DECODER -> {
                        reference.resetDecode()
                    }
                    else -> {
                    }
                }
            }

        }

    }

    private fun initHandler() {
        videoDecoderHandlerThread = HandlerThread("VideoDecoderThread")
        videoDecoderHandlerThread?.start()
        mHandleHandler = videoDecoderHandlerThread?.looper?.let { HandleHandler(it, this) }
    }

    private fun releaseHandler() {
        videoDecoderHandlerThread?.quit()
    }

    private fun beginDecode() {

        initHandler()

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

    private fun releaseRender(needClear: Boolean = true) {
        toSurfaceFrameRender?.stop(needClear)
        toSurfaceFrameRender = null
        videoRender?.releaseRender()
        videoRender = null
    }

    fun stopDecode() {
        releaseHandler()
        endDecode()
        releaseRender()
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
        format.setInteger(
            MediaFormat.KEY_COLOR_FORMAT,
            MediaCodecInfo.CodecCapabilities.COLOR_FormatSurface
        )
        format.setInteger(MediaFormat.KEY_WIDTH, frameWidth)
        format.setInteger(MediaFormat.KEY_HEIGHT, frameHeight)
        return format
    }

    private fun configEncoder() {
        try {
            codec?.configure(getEncodeFormat(), outputSurface, null, 0)
        } catch (e: Exception) {
            e.printStackTrace()
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

        //重复收到sps,重置解码
        private var isReceivedSps = false

        private fun addDataBackToQueue(data: VideoFrame) {
            val collections = ArrayList<VideoFrame>()
            queueVideoFrame?.drainTo(collections)
            collections.add(0, data)
            queueVideoFrame?.clear()
            queueVideoFrame?.addAll(collections)
        }

        override fun run() {

            try {
                while (isDecoding) {

                    val inIndex = codec?.dequeueInputBuffer(0) ?: -1
                    if (inIndex >= 0) {

                        queueVideoFrame?.take()?.let { frame ->
                            frame.byteArray?.let { array ->

                                if (FrameType.SPS_FRAME == CheckUtils.judgeBytesFrameKind(array)) {
                                    if (isReceivedSps) {
                                        addDataBackToQueue(frame)
                                        mHandleHandler?.sendEmptyMessage(MSG_RESET_DECODER)
                                        isDecoding = false
                                    } else {
                                        isReceivedSps = true
                                    }
                                }

                                if (isDecoding) {
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
                }
            } catch (e: Exception) {

            } finally {

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
            RecLogUtils.logVideoTimeStamp(ptsNew)
            return ptsNew
        }

        override fun run() {
            while (isDecoding) {
                //解码
                var outIndex = codec?.dequeueOutputBuffer(vBufferInfo, 0) ?: -1
                while (outIndex >= 0) {
                    handlePts()
                    codec?.releaseOutputBuffer(outIndex, vBufferInfo.size != 0)
                    outIndex = codec?.dequeueOutputBuffer(vBufferInfo, 0) ?: -1
                }
                when (outIndex) {
                    MediaCodec.INFO_TRY_AGAIN_LATER -> {
                    }
                    MediaCodec.INFO_OUTPUT_FORMAT_CHANGED -> {
                    }
                    else -> {
                    }
                }
            }
        }
    }

    fun onReceived(frame: VideoFrame) {
        queueVideoFrame?.put(frame)
    }

}