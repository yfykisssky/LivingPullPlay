package com.living.pullplay.rec.tool.socket

import com.living.pullplay.decoder.AudioFrame
import com.living.pullplay.decoder.DataDecodeTool
import com.living.pullplay.decoder.VideoFrame
import com.living.pullplay.utils.RecLogUtils
import java.io.DataInputStream
import java.net.ServerSocket

class SocketServer {

    private var serverSocket: ServerSocket? = null
    private var inPutStream: DataInputStream? = null
    private var isReading = false
    private var isListening = false

    interface OnDataReceivedCallBack {
        fun onVideoDataRec(frame: VideoFrame)
        fun onAudioDataRec(frame: AudioFrame)
    }

    private var onDataReceivedCallBack: OnDataReceivedCallBack? = null
    fun setDataReceivedCallBack(onDataReceivedCallBack: OnDataReceivedCallBack?) {
        this.onDataReceivedCallBack = onDataReceivedCallBack
    }

    interface OnConnectListener {
        fun onConnected()
        fun onDisConnected()
    }

    private var onConnectListener: OnConnectListener? = null
    fun setOnConnectListener(onConnectListener: OnConnectListener?) {
        this.onConnectListener = onConnectListener
    }

    inner class ConnectThread(private val port: Int) : Thread() {
        override fun run() {
            super.run()
            try {
                serverSocket = ServerSocket(port)
                serverSocket?.let { socket ->
                    while (isListening) {
                        try {
                            socket.accept()?.let { client ->
                                onConnectListener?.onConnected()
                                isReading = true
                                inPutStream = DataInputStream(client.getInputStream())
                                while (isReading) {

                                    val extraData = ByteArray(DataDecodeTool.EXTRA_DATA_LENGTH)
                                    inPutStream?.readFully(extraData)

                                    DataDecodeTool.deExtraData(extraData).let { rec ->

                                        val byteData = ByteArray(rec.first)
                                        inPutStream?.readFully(byteData)
                                        if (rec.second) {
                                            val frame = VideoFrame()
                                            frame.byteArray = byteData
                                            frame.timestamp = rec.third
                                            onDataReceivedCallBack?.onVideoDataRec(frame)
                                        } else {
                                            val frame = AudioFrame()
                                            frame.byteArray = byteData
                                            frame.timestamp = rec.third
                                            onDataReceivedCallBack?.onAudioDataRec(frame)
                                        }
                                    }

                                }
                            }
                        } catch (e: Exception) {
                            RecLogUtils.log("Socket ERR " + e.localizedMessage)
                        } finally {
                            onConnectListener?.onDisConnected()
                        }
                    }
                }
            } catch (e: Exception) {
                RecLogUtils.log("Socket Listen ERR " + e.localizedMessage)
            }
        }
    }

    fun openSocket(port: Int) {
        isListening = true
        ConnectThread(port).start()
    }


    fun closeSocket() {
        serverSocket?.close()
    }

}