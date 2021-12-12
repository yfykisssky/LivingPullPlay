package com.living.pullplay.rec.tool.socket

import com.google.gson.Gson


data class ScanResult(
    var ipAddress: String? = null,
    var port: Int = -1
)

class HostTransTool {

    companion object {
        fun obj2Str(scanResult: ScanResult): String {
            return Gson().toJson(scanResult)
        }
    }

}