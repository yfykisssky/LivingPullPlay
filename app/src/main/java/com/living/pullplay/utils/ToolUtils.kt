package com.living.pullplay.utils

import android.hardware.usb.UsbDevice
import java.util.HashMap

class ToolUtils {

    companion object {

        fun <T> tranMapToList(map: HashMap<String?, T?>?): List<T?> {
            val list = ArrayList<T?>()
            val keySet = map?.keys
            keySet?.iterator()?.let { iterator ->
                while (iterator.hasNext()) {
                    list.add(map[iterator.next()])
                }
            }
            return list
        }

    }

}