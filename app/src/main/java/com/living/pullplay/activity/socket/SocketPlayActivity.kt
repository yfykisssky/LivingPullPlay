package com.living.pullplay.activity.socket

import android.graphics.Bitmap
import android.os.Bundle
import android.view.View
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsBuildBitmapOption
import com.living.pullplay.R
import com.living.pullplay.activity.base.BasePlayActivity
import com.living.pullplay.decoder.AudioFrame
import com.living.pullplay.decoder.VideoFrame
import com.living.pullplay.rec.tool.socket.HostTransTool
import com.living.pullplay.rec.tool.socket.ScanResult
import com.living.pullplay.rec.tool.socket.SocketServer
import com.living.pullplay.utils.SocketUtils
import kotlinx.android.synthetic.main.activity_socket_play.*
import kotlin.system.exitProcess

class SocketPlayActivity : BasePlayActivity() {

    private var socRecServer: SocketServer? = null

    private var isStart = true

    private fun beginListening() {

        preparedPlay()

        socRecServer?.setDataReceivedCallBack(object : SocketServer.OnDataReceivedCallBack {

            override fun onVideoDataRec(frame: VideoFrame) {
                onVideoFrameRec(frame)
            }

            override fun onAudioDataRec(frame: AudioFrame) {
                onAudioFrameRec(frame)
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
                stopDecoder()
            }

        })

        socRecServer?.openSocket(SocketUtils.LISTENING_PORT)

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_socket_play)

        updateTexturePlayView(surfaceView)
        initTools()

        socRecServer = SocketServer()

        stateBnt?.setOnClickListener {

            isStart = if (isStart) {
                startListen()
                stateBnt?.text = "Stop"
                false
            } else {
                stopListen()
                stateBnt?.text = "Start"
                true
            }

        }

        exit?.setOnClickListener {
            stopDecoder()
            exitProcess(0)
        }

    }

    private fun startListen() {
        sanCodeImg?.visibility = View.VISIBLE
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

    private fun getCreateScanCodeImg(width: Int, height: Int, content: String): Bitmap? {
        val type = 0;//码类型。0=QR Code、1=Data Matrix、2=PDF417、3=Aztec
        val margin = 1;//边距
        val options = HmsBuildBitmapOption.Creator().setBitmapMargin(margin).create()
        return ScanUtil.buildBitmap(content, type, width, height, options)
    }

    private fun stopListen() {
        socRecServer?.closeSocket()
        stopDecoder()
        sanCodeImg?.visibility = View.GONE
    }

}