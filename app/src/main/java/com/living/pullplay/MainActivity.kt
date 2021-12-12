package com.living.pullplay

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import com.huawei.hms.hmsscankit.ScanUtil
import com.huawei.hms.ml.scan.HmsBuildBitmapOption
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.system.exitProcess

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sanCodeImg?.post {
            val width = sanCodeImg?.width ?: 0
            val height = sanCodeImg?.height ?: 0
            getCreateScanCodeImg(width, height, "133234324")?.let { bitmap ->
                sanCodeImg?.setImageBitmap(bitmap)
            }
        }

        start?.setOnClickListener {

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