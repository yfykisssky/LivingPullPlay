package com.living.pullplay.activity.base

import android.app.Activity
import android.media.AudioFormat
import android.view.TextureView
import com.living.pullplay.decoder.*
import com.living.pullplay.play.tool.audio.AudioStmPlayer
import com.living.pullplay.utils.CheckUtils
import com.living.pullplay.utils.FrameType
import com.living.pullplay.utils.RecJavaUtils
import com.living.pullplay.utils.RecLogUtils

open class BasePlayActivity : Activity() {

    private var surfaceRenderView: TextureView? = null
    private var videoDecoder: VideoDecoder? = null

    private var audioDecoder: AudioSoftDecoder? = null

    private var audioStmPlayer: AudioStmPlayer? = null

    protected fun updateTexturePlayView(surfaceRenderView: TextureView?) {
        this.surfaceRenderView = surfaceRenderView
    }

    protected fun initTools() {
        videoDecoder = VideoDecoder()
        audioDecoder = AudioSoftDecoder()

        audioStmPlayer = AudioStmPlayer()
    }

    protected fun onVideoFrameRec(frame: VideoFrame) {
        //初始化解码器
        if (CheckUtils.checkBytesFrameKind(frame.byteArray) == FrameType.SPS_FRAME
            && videoDecoder?.isDecoding() == false
        ) {
            RecJavaUtils.getSizeFromSps(frame.byteArray)?.let { size ->
                RecLogUtils.logWH(size.width, size.height)
                beginVideoDecoding(size.width, size.height)
            }
        }
        videoDecoder?.onReceived(frame)
    }

    protected fun onAudioFrameRec(frame: AudioFrame) {
        audioDecoder?.onReceived(frame)
    }

    protected fun preparedPlay() {

        audioDecoder?.setDecodeDataCallBack(object : AudioSoftDecoder.DecodeDataCallBack {
            override fun onDataCallBack(bytes: ByteArray, timeStamp: Long) {
                audioStmPlayer?.addAudioBytes(bytes)
            }
        })
        audioDecoder?.initDecoder()
        audioDecoder?.startDecode()

        audioStmPlayer?.initPlayer(
            AudioConstants.SAMPLE_RATE, AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.CHANNEL_IN_STEREO, AudioConstants.getSampleDataSize()
        )
        audioStmPlayer?.startPlay()

    }

    private fun beginVideoDecoding(withFrame: Int, heightFrame: Int) {
        surfaceRenderView?.let { surfaceView ->
            videoDecoder?.stopDecode()
            videoDecoder?.setDecodeSettings(withFrame, heightFrame)
            videoDecoder?.setRenderView(surfaceView)

            videoDecoder?.initDecoder()
            videoDecoder?.startDecode()
        }
    }

    protected fun stopDecoder() {
        videoDecoder?.stopDecode()
        audioDecoder?.stopDecode()
    }

}