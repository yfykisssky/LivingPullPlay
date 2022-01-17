package com.living.pullplay

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.living.pullplay.activity.aoa.AoaPlayActivity
import com.living.pullplay.activity.socket.SocketPlayActivity
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.system.exitProcess

class MainActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        socketBnt?.setOnClickListener {
            startActivity(Intent(this@MainActivity, SocketPlayActivity::class.java))
        }

        aoaBnt?.setOnClickListener {
            startActivity(Intent(this@MainActivity, AoaPlayActivity::class.java))
        }

        exitBnt?.setOnClickListener {
            exitProcess(0)
        }

    }

}