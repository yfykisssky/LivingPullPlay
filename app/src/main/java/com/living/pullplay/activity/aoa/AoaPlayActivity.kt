package com.living.pullplay.activity.aoa

import android.os.Bundle
import com.living.pullplay.R
import com.living.pullplay.activity.base.BasePlayActivity
import com.living.pullplay.decoder.AudioFrame
import com.living.pullplay.decoder.VideoFrame
import com.living.pullplay.rec.tool.usbaoa.UsbHostTool
import kotlinx.android.synthetic.main.activity_aoa_play.*
import kotlin.system.exitProcess

class AoaPlayActivity : BasePlayActivity() {

    private var usbHostTool: UsbHostTool? = null

    private var isStart = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aoa_play)

        initTools()

        usbHostTool = UsbHostTool()
        usbHostTool?.initTool(this)

        checkBnt?.setOnClickListener {
            var showText=""
            usbHostTool?.getNowConPidVidLists()?.forEach {
                showText
            }
        }


        stateBnt?.setOnClickListener {

            isStart = if (isStart) {
                startConnect()
                stateBnt?.text = "Stop"
                false
            } else {
                stopConnect()
                stateBnt?.text = "Start"
                true
            }

        }

        exit?.setOnClickListener {
            stopDecoder()
            exitProcess(0)
        }

    }

    private fun startConnect(conDevStr: String) {

        preparedPlay()

        usbHostTool?.setDataReceivedCallBack(object : UsbHostTool.OnDataReceivedCallBack {

            override fun onVideoDataRec(frame: VideoFrame) {
                onVideoFrameRec(frame)
            }

            override fun onAudioDataRec(frame: AudioFrame) {
                onAudioFrameRec(frame)
            }
        })

        usbHostTool?.setUsbConnectListener(object : UsbHostTool.UsbConnectListener {

            override fun onConnectStartError() {

            }

            override fun onConnected() {
                runOnUiThread {
                    //sanCodeImg?.visibility = View.GONE
                }
            }

            override fun onDisConnected() {
                runOnUiThread {
                    //sanCodeImg?.visibility = View.VISIBLE
                }
                stopDecoder()
            }

        })

        usbHostTool?.connectToAoaDevice(conDevStr)
    }

    private fun stopConnect() {
        stopDecoder()
    }

}