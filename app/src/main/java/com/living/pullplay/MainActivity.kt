package com.living.pullplay

import android.app.Activity
import android.graphics.Bitmap
import android.media.AudioFormat
import android.os.Bundle
import android.view.SurfaceHolder
import android.view.View
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsBuildBitmapOption
import com.living.pullplay.decoder.*
import com.living.pullplay.play.tool.AudioStmPlayer
import com.living.pullplay.rec.tool.socket.HostTransTool
import com.living.pullplay.rec.tool.socket.ScanResult
import com.living.pullplay.rec.tool.socket.SocketServer
import com.living.pullplay.utils.SocketUtils
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.system.exitProcess

class MainActivity : Activity() {

    private var videoDecoder: VideoDecoder? = null
    private var decodeVideoBitRate = 8000
    private var decodeFps = 30

    private var audioDecoder: AudioDecoder? = null
    private var decodeAudioBitRate = 128

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

        videoDecoder?.initDecoder()
        videoDecoder?.startDecode()

        audioDecoder?.updateDecodeSettings(decodeAudioBitRate)
        audioDecoder?.setDecodeDataCallBack(object : AudioDecoder.DecodeDataCallBack {
            override fun onDataCallBack(bytes: ByteArray, timeStamp: Long) {
                audioStmPlayer?.addAudioBytes(bytes)
            }
        })
        audioDecoder?.initDecoder()
        audioDecoder?.startDecode()


        audioStmPlayer?.initPlayer(AudioConstants.SAMPLE_RATE, AudioFormat.ENCODING_PCM_16BIT,
            AudioFormat.CHANNEL_IN_STEREO,AudioConstants.getSampleDataSize())
        audioStmPlayer?.startPlay()

        socRecServer?.setDataReceivedCallBack(object : SocketServer.OnDataReceivedCallBack {

            override fun onVideoDataRec(frame: VideoFrame) {
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
            }

        })

    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initTools()

        surfaceView?.holder?.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                videoDecoder?.updateDecodeSettings(
                    decodeVideoBitRate,
                    decodeFps,
                    1280,
                    720,
                    holder.surface
                )
            }

            override fun surfaceChanged(p0: SurfaceHolder, p1: Int, p2: Int, p3: Int) {

            }

            override fun surfaceDestroyed(p0: SurfaceHolder) {

            }

        })

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