package com.living.pullplay.activity.aoa

import android.os.Bundle
import android.view.View
import com.living.pullplay.R
import com.living.pullplay.activity.aoa.view.DeviceListAdapter
import com.living.pullplay.activity.base.BasePlayActivity
import com.living.pullplay.decoder.AudioFrame
import com.living.pullplay.decoder.VideoFrame
import com.living.pullplay.rec.tool.usbaoa.UsbHostTool
import com.living.pullplay.utils.ToolUtils
import kotlinx.android.synthetic.main.activity_aoa_play.*
import kotlin.system.exitProcess

class AoaPlayActivity : BasePlayActivity() {

    private var usbHostTool: UsbHostTool? = null
    private var deviceListAdapter: DeviceListAdapter? = null

    private fun initView() {
        deviceListAdapter = DeviceListAdapter(this)
        deviceListAdapter?.setOnItemConnListener(
            object : DeviceListAdapter.OnItemConnListener {
                override fun onItemConn(pid: String, vid: String) {
                    startConnect(pid, vid)
                }
            })
        deviceLists?.adapter = deviceListAdapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_aoa_play)

        initView()
        initTools()

        usbHostTool = UsbHostTool()
        usbHostTool?.initTool(this)

        checkBnt?.setOnClickListener {
            val mapDevices = usbHostTool?.getNowConPidVidLists()
            ToolUtils.tranMapToList(mapDevices).let { devices ->
                deviceLists?.visibility = View.VISIBLE
                deviceListAdapter?.updateData(devices)
                deviceListAdapter?.notifyDataSetChanged()
            }
        }

        stopBnt?.setOnClickListener {
            stopConnect()
        }

        exit?.setOnClickListener {
            stopDecoder()
            exitProcess(0)
        }

    }

    private fun startConnect(pid: String, vid: String) {

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

            override fun onIsOneMoreDeviceInterface() {

            }

            override fun onConnected() {
                runOnUiThread {
                    deviceLists?.visibility = View.GONE
                }
            }

            override fun onDisConnected() {
                runOnUiThread {

                }
                stopDecoder()
            }

        })

        usbHostTool?.connectToAoaDevice(pid, vid)
    }

    private fun stopConnect() {
        stopDecoder()
        usbHostTool?.stopConnect()
    }

}