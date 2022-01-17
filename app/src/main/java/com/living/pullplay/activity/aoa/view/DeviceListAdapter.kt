package com.living.pullplay.activity.aoa.view

import android.annotation.SuppressLint
import android.content.Context
import android.hardware.usb.UsbDevice
import android.widget.ArrayAdapter
import android.view.ViewGroup
import android.view.LayoutInflater
import android.view.View
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.TextView
import com.living.pullplay.R

class DeviceListAdapter constructor(
    private val context: Context
) : BaseAdapter() {

    private var devicesList: List<UsbDevice?>? = null

    fun updateData(devicesList: List<UsbDevice?>?) {
        this.devicesList = devicesList
    }

    private var onItemConnListener: OnItemConnListener? = null
    fun setOnItemConnListener(onItemStartListener: OnItemConnListener?) {
        this.onItemConnListener = onItemStartListener
    }

    interface OnItemConnListener {
        fun onItemConn(pid: String, vid: String)
    }

    override fun getCount(): Int {
        return devicesList?.size ?: 0
    }

    override fun getItem(p0: Int): Any? {
        return null
    }

    override fun getItemId(p0: Int): Long {
        return 0
    }

    @SuppressLint("SetTextI18n")
    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val device = devicesList?.get(position)
        val view: View
        val viewHolder: ViewHolder
        if (convertView == null) {
            view = LayoutInflater.from(context).inflate(
                R.layout.list_item_aoa_devices,
                parent, false
            )
            viewHolder = ViewHolder()
            viewHolder.nameTex = view.findViewById(R.id.nameTex)
            viewHolder.pidTex = view.findViewById(R.id.pidTex)
            viewHolder.vidTex = view.findViewById(R.id.vidTex)
            viewHolder.startBnt = view.findViewById(R.id.startBnt)
            view.tag = viewHolder
        } else {
            view = convertView
            viewHolder = view.tag as ViewHolder
        }
        viewHolder.nameTex?.text = device?.productName

        val pidStr = Integer.toHexString(device?.productId ?: 0)
        val vidStr = Integer.toHexString(device?.vendorId ?: 0)
        viewHolder.pidTex?.text = "pid:$pidStr"
        viewHolder.vidTex?.text = "vid:$vidStr"
        viewHolder.startBnt?.setOnClickListener {
            onItemConnListener?.onItemConn(pidStr, vidStr)
        }

        return view
    }

    internal inner class ViewHolder {
        var nameTex: TextView? = null
        var pidTex: TextView? = null
        var vidTex: TextView? = null
        var startBnt: Button? = null
    }

}