package com.living.pullplay.rec.tool.usbaoa

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.*

class UsbHostTool {

    companion object {
        private const val USB_TIMEOUT_IN_MS = 10
        private const val BUFFER_SIZE_IN_BYTES = 256

        private const val GOOGLE_AOA_PRODUCT_ID_1 = 0x2D00
        private const val GOOGLE_AOA_PRODUCT_ID_2 = 0x2D01

        private const val PERMISSION_RECEIVER = "usbHostTool.aoa.usb_permission"
        private const val PERMISSION_CONNECT_RECEIVER = "usbHostTool.aoaconnect.usb_permission"
        private const val ACTION_USB_STATE = "android.hardware.usb.action.USB_STATE"
    }

    private var con: Context? = null
    private var mUsbManager: UsbManager? = null

    @Volatile
    private var isRegisPerListener = false

    @Volatile
    private var isRegisAttDetListener = false

    @Volatile
    private var isRegisConPertListener = false

    @Volatile
    private var isConnecting = false

    private var endpointIn: UsbEndpoint? = null
    private var endpointOut: UsbEndpoint? = null

    private var connAoa: UsbDeviceConnection? = null
    private var connDataAoa: UsbDeviceConnection? = null

    private var usbInterface: UsbInterface? = null

    private var readDataThread: Thread? = null
    private var writeDataThread: Thread? = null

    private var usbConnectListener: UsbConnectListener? = null
    fun setUsbConnectListener(usbConnectListener: UsbConnectListener?) {
        this.usbConnectListener = usbConnectListener
    }

    interface UsbConnectListener {
        fun onConnectStartError()
        fun onDataReceive(bytes: ByteArray)
    }

    fun initTool(con: Context?) {
        this.con = con
        mUsbManager = con?.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    private fun checkIsGranted(intent: Intent): Boolean {
        return intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
    }

    private fun getUsbDevice(intent: Intent): UsbDevice? {
        return intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
    }

    private val usbPermissionReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intentRec: Intent?) {

            intentRec?.let { intent ->

                when (intent.action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                        startConnect()
                    }
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    }
                    //ACTION_USB_STATE部分机型也比较慢
                    ACTION_USB_STATE -> {
                        if (!intent.getBooleanExtra("connected", false)) {
                            stopConnect()
                            return
                        } else {
                        }
                    }
                    PERMISSION_RECEIVER -> {
                        getUsbDevice(intent)?.let { device ->
                            //是否授权成功
                            if (checkIsGranted(intent)) {
                                toConnect(device)
                            } else {

                            }
                        }
                    }
                    PERMISSION_CONNECT_RECEIVER -> {
                        getUsbDevice(intent)?.let { device ->
                            //是否授权成功
                            if (checkIsGranted(intent)) {
                                toConnectAoa(device)
                            } else {

                            }
                        }
                    }
                    else -> {
                    }
                }

            }

        }
    }

    private fun findDevicesWithGoogleIds(): UsbDevice? {

        mUsbManager?.deviceList?.let { deviceList ->
            for (device in deviceList.values) {
                if (device.productId == GOOGLE_AOA_PRODUCT_ID_1
                    || device.productId == GOOGLE_AOA_PRODUCT_ID_2
                ) {
                    return device
                }
            }
        }

        return null
    }

    fun getNowConPidVidLists(): List<String> {
        val arrayLists = ArrayList<String>()
        mUsbManager?.deviceList?.forEach { device ->
            val value = device.value
            val pid = Integer.toHexString(value.productId)
            val vid = Integer.toHexString(value.vendorId)
            arrayLists.add("$pid:$vid")
        }
        return arrayLists
    }

    //枚举usb设备，并修改为Accessory模式
    fun connectToAoaDevice(conDevStr: String): Boolean {

        mUsbManager?.deviceList?.let { deviceList ->
            for (device in deviceList.values) {

                val pid = Integer.toHexString(device.productId)
                val vid = Integer.toHexString(device.vendorId)
                val pidVid = "$pid:$vid"

                if (conDevStr == pidVid) {
                    if (mUsbManager?.hasPermission(device) == false) {
                        synchronized(isRegisPerListener) {
                            if (!isRegisPerListener) {
                                try {
                                    regisFilter(PERMISSION_RECEIVER)
                                    reqDevicePermission(device, PERMISSION_RECEIVER)
                                    isRegisPerListener = true
                                } catch (e: IllegalArgumentException) {
                                }
                            }
                        }
                    } else {
                        toConnect(device)
                    }
                }
            }
        }

        return false
    }

    private fun regisFilter(filterStr: String) {
        val filter = IntentFilter(filterStr)
        con?.registerReceiver(usbPermissionReceiver, filter)
    }

    private fun reqDevicePermission(
        device: UsbDevice,
        reveiverStr: String
    ) {
        val pendingIntent = PendingIntent.getBroadcast(
            con, 0,
            Intent(reveiverStr), 0
        )
        mUsbManager?.requestPermission(device, pendingIntent)
    }

    private fun registerStateReceiver() {
        synchronized(isRegisAttDetListener) {
            if (!isRegisAttDetListener) {
                val filter = IntentFilter(PERMISSION_RECEIVER)
                filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                filter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
                con?.registerReceiver(usbPermissionReceiver, filter)
                isRegisAttDetListener = true
            }
        }
    }

    private fun unRegisterStateReceiver() {
        synchronized(isRegisAttDetListener) {
            if (isRegisAttDetListener) {
                con?.unregisterReceiver(usbPermissionReceiver)
                isRegisAttDetListener = false
            }
        }
    }

    private fun toConnect(device: UsbDevice) {
        registerStateReceiver()
        if (!initAccessory(device)) {
            unRegisterStateReceiver()
        }
    }

    fun toConnectAoa(device: UsbDevice) {

        device.getInterface(0).let { interf ->

            usbInterface = interf
            for (i in 0 until interf.endpointCount) {
                val endpoint = interf.getEndpoint(i)
                /* USB_ENDPOINT_XFER_CONTROL 0 --控制传输
                   USB_ENDPOINT_XFER_ISOC 1 --等时传输
                   USB_ENDPOINT_XFER_BULK 2 --块传输
                   USB_ENDPOINT_XFER_INT 3 --中断传输 */
                if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (endpoint.direction == UsbConstants.USB_DIR_IN) {
                        endpointIn = endpoint
                    }
                    if (endpoint.direction == UsbConstants.USB_DIR_OUT) {
                        endpointOut = endpoint
                    }
                }
            }
            if (endpointIn == null) {
                usbConnectListener?.onConnectStartError()
                return
            }
            if (endpointOut == null) {
                usbConnectListener?.onConnectStartError()
                return
            }

            connDataAoa = mUsbManager?.openDevice(device)
            if (connDataAoa == null) {
                usbConnectListener?.onConnectStartError()
                return
            }

            val claimResult = connDataAoa?.claimInterface(usbInterface, true)
            if (claimResult == false) {
                usbConnectListener?.onConnectStartError()
            } else {

                isConnecting = true
                readDataThread = Thread(ReadDataRunnable(connDataAoa))
                readDataThread?.start()
                writeDataThread = Thread(WriteDataRunnable(connDataAoa))
                writeDataThread?.start()

            }
        }
    }

    private fun startConnect() {
        findDevicesWithGoogleIds()?.let { device ->
            if (mUsbManager?.hasPermission(device) == false) {
                synchronized(isRegisConPertListener) {
                    if (!isRegisConPertListener) {
                        try {
                            regisFilter(PERMISSION_CONNECT_RECEIVER)
                            reqDevicePermission(device, PERMISSION_CONNECT_RECEIVER)
                            isRegisConPertListener = true
                        } catch (e: IllegalArgumentException) {
                        }
                    }
                }
            } else {
                toConnectAoa(device)
            }
        }
    }

    private inner class ReadDataRunnable
    constructor(private val conAoa: UsbDeviceConnection?) : Runnable {

        private val buff = ByteArray(BUFFER_SIZE_IN_BYTES)

        override fun run() {

            try {

                while (isConnecting) {
                    val ret = conAoa?.bulkTransfer(
                        endpointIn,
                        buff,
                        buff.size,
                        USB_TIMEOUT_IN_MS
                    ) ?: -1
                    if (ret > 0) {
                        usbConnectListener?.onDataReceive(buff)
                    }
                }

            } catch (e: Exception) {

            }

        }
    }

    private inner class WriteDataRunnable
    constructor(private val conAoa: UsbDeviceConnection?) : Runnable {

        override fun run() {

            try {
                while (isConnecting) {
                    val sendBuff = ByteArray(100)
                    val ret = conAoa?.bulkTransfer(
                        endpointOut,
                        sendBuff,
                        sendBuff.size,
                        USB_TIMEOUT_IN_MS
                    ) ?: -1
                    if (ret > 0) {

                    }

                    Thread.sleep(100)
                }
            } catch (e: Exception) {

            }

        }
    }

    fun stopConnect() {

        isConnecting = false

        readDataThread?.interrupt()
        readDataThread?.join()

        writeDataThread?.interrupt()
        writeDataThread?.join()

        connAoa?.close()

        usbInterface?.let { interf ->
            connDataAoa?.releaseInterface(interf)
        }
        connDataAoa?.close()

    }

    private class DeviceInfoConstants {
        companion object {
            const val MANUFACTURER = "Android2AndroidAccessory"
            const val MODEL = "PushingAOA"
            const val DESCRIPTION = "RecAOA"
            const val VERSION = "1.0"
            const val URI = "http://android2android"
            const val SERIAL = "42"
        }
    }

    private fun initAccessory(device: UsbDevice): Boolean {

        mUsbManager?.openDevice(device)?.let { connection ->
            connAoa = connection
        }
        if (connAoa == null) {
            return false
        }
        //发送序号52的USB请求报文
        //通过Index字段携带配件自身信息，包括制造商、型号、版本、设备描述、序 列号URI等。
        //手机根据这些信息启动响应的APP
        //MANUFACTURER
        var res = initStringControlTransfer(connAoa, 0, DeviceInfoConstants.MANUFACTURER)
        //MODEL
        res = res && initStringControlTransfer(connAoa, 1, DeviceInfoConstants.MODEL)
        //DESCRIPTION
        res = res && initStringControlTransfer(connAoa, 2, DeviceInfoConstants.DESCRIPTION)
        //VERSION
        res = res && initStringControlTransfer(connAoa, 3, DeviceInfoConstants.VERSION)
        //URI
        res = res && initStringControlTransfer(connAoa, 4, DeviceInfoConstants.URI)
        //SERIAL
        res = res && initStringControlTransfer(connAoa, 5, DeviceInfoConstants.SERIAL)

        //发送序号53的USB请求报文，切换USB模式，主要是根据切换的vendorID和productID
        connAoa?.controlTransfer(
            0x40,
            53,
            0,
            0,
            ByteArray(0),
            0,
            USB_TIMEOUT_IN_MS
        )

        connAoa?.close()

        return res

    }

    private fun initStringControlTransfer(
        deviceConnection: UsbDeviceConnection?,
        index: Int,
        string: String
    ): Boolean {
        return deviceConnection?.controlTransfer(
            0x40,
            52,
            0,
            index,
            string.toByteArray(),
            string.length,
            USB_TIMEOUT_IN_MS
        ) ?: -1 >= 0
    }


}