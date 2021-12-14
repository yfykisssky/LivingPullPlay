package com.living.pullplay

import android.app.Activity
import android.graphics.Bitmap
import android.media.AudioFormat
import android.os.Bundle
import android.os.Handler
import android.view.SurfaceHolder
import android.view.View
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsBuildBitmapOption
import com.living.pullplay.decoder.*
import com.living.pullplay.play.tool.audio.AudioStmPlayer
import com.living.pullplay.rec.tool.socket.HostTransTool
import com.living.pullplay.rec.tool.socket.ScanResult
import com.living.pullplay.rec.tool.socket.SocketServer
import com.living.pullplay.utils.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.system.exitProcess

class MainActivity : Activity() {

    private var videoDecoder: VideoDecoder? = null

    private var audioDecoder: AudioDecoder? = null

    private var audioStmPlayer: AudioStmPlayer? = null

    private var socRecServer: SocketServer? = null

    private fun initTools() {
        videoDecoder = VideoDecoder()
        audioDecoder = AudioDecoder()

        socRecServer = SocketServer()

        audioStmPlayer = AudioStmPlayer()
    }

    private fun beginListening() {

        socRecServer?.openSocket(SocketUtils.LISTENING_PORT)

        audioDecoder?.setDecodeDataCallBack(object : AudioDecoder.DecodeDataCallBack {
            override fun onDataCallBack(bytes: ByteArray, timeStamp: Long) {
                audioStmPlayer?.addAudioBytes(bytes)
            }
        })
        audioDecoder?.initDecoder()
        //audioDecoder?.startDecode()

        audioStmPlayer?.initPlayer(
            AudioConstants.SAMPLE_RATE, AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.CHANNEL_IN_STEREO, AudioConstants.getSampleDataSize()
        )
        audioStmPlayer?.startPlay()

        socRecServer?.setDataReceivedCallBack(object : SocketServer.OnDataReceivedCallBack {

            override fun onVideoDataRec(frame: VideoFrame) {
                if (CheckUtils.judgeBytesFrameKind(frame.byteArray) == FrameType.SPS_FRAME) {
                    RecJavaUtils.getSizeFromSps(frame.byteArray)?.let { size ->
                        RecLogUtils.logWH(size.width, size.height)
                        beginVideoDecoding(size.width, size.height)
                    }
                }
                videoDecoder?.onReceived(frame)
            }

            override fun onAudioDataRec(frame: AudioFrame) {
                audioDecoder?.onReceived(frame)
            }
        })

        socRecServer?.setOnConnectListener(object : SocketServer.OnConnectListener {

            override fun onConnected() {
                runOnUiThread {
                    sanCodeImg?.visibility = View.GONE
                }
            }

            override fun onDisConnected() {
                runOnUiThread {
                    sanCodeImg?.visibility = View.VISIBLE
                }
                videoDecoder?.stopDecode()
            }

        })

    }

    private fun beginVideoDecoding(withFrame: Int, heightFrame: Int) {

        videoDecoder?.stopDecode()
        videoDecoder?.setDecodeSettings(withFrame, heightFrame)
        videoDecoder?.setRenderView(surfaceView)

        videoDecoder?.initDecoder()
        videoDecoder?.startDecode()

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initTools()

        start?.setOnClickListener {

            sanCodeImg?.post {
                val width = sanCodeImg?.width ?: 0
                val height = sanCodeImg?.height ?: 0

                val content = HostTransTool.obj2Str(
                    ScanResult(
                        SocketUtils.getHostIp(this),
                        SocketUtils.LISTENING_PORT
                    )
                )

                getCreateScanCodeImg(width, height, content)?.let { bitmap ->
                    sanCodeImg?.setImageBitmap(bitmap)
                }
            }

            beginListening()
        }

        stop?.setOnClickListener {

        }

        exit?.setOnClickListener {
            exitProcess(0)
        }

    }

    private fun getCreateScanCodeImg(width: Int, height: Int, content: String): Bitmap? {
        val type = 0;//码类型。0=QR Code、1=Data Matrix、2=PDF417、3=Aztec
        val margin = 1;//边距
        val options = HmsBuildBitmapOption.Creator().setBitmapMargin(margin).create()
        return ScanUtil.buildBitmap(content, type, width, height, options)
    }
}