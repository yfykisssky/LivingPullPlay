package com.living.pullplay.utils

import android.util.Log

class RecLogUtils {
    companion object {
        private const val TAG = "PULL LOG"

        private const val isDebug = true

        fun log(log: String?) {
            if (!isDebug) {
                return
            }
            Log.e(TAG, log ?: "null")
        }

        fun logWH(with: Int, height: Int) {
            if (!isDebug) {
                return
            }
            Log.e(TAG, "with:$with,height:$height")
        }

        private var lastPtsAudio = 0L
        private var lastPtsVideo = 0L

        fun logVideoTimeStamp(timeStamp: Long) {
            if (!isDebug) {
                return
            }
            if (lastPtsVideo == 0L) {
                lastPtsVideo = timeStamp
            }
            Log.e(TAG, "videoTimeStamp:$timeStamp" + " last:" + (timeStamp - lastPtsVideo))
            lastPtsVideo = timeStamp
        }

        fun logAudioTimeStamp(timeStamp: Long) {
            if (!isDebug) {
                return
            }
            if (lastPtsAudio == 0L) {
                lastPtsAudio = timeStamp
            }
            Log.e(TAG, "audioTimeStamp:$timeStamp" + " last:" + (timeStamp - lastPtsAudio))
            lastPtsAudio = timeStamp
        }

        private var playPcmTimeStamp = 0L

        fun playPcm() {
            if (!isDebug) {
                return
            }
            Log.e(
                TAG,
                "playPcm:" + (System.currentTimeMillis() - playPcmTimeStamp)
            )
            playPcmTimeStamp = System.currentTimeMillis()
        }

    }
}