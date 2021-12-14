package com.living.pullplay.play.tool.audio

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import com.living.pullplay.utils.RecLogUtils
import java.io.IOException
import java.util.concurrent.LinkedBlockingQueue

class AudioStmPlayer {

    private var audioTrack: AudioTrack? = null
    private var playPCMThread: PlayPCMThread? = null
    private var audioPcmQueue: LinkedBlockingQueue<ByteArray>? = null

    private var isPlaying = false

    //使用stream模式
    //sampleRate 对应pcm音频的采样率
    //channel 对应pcm音频的声道
    //audioFramat 对应pcm音频的格式
    //bufferSize buffer的长度
    fun initPlayer(sampleRate: Int, audioFramat: Int, channel: Int, bufferSize: Int) {
        audioTrack = AudioTrack(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_MEDIA)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build(),
            AudioFormat.Builder().setSampleRate(sampleRate)
                .setEncoding(audioFramat)
                .setChannelMask(channel)
                .build(),
            bufferSize,
            AudioTrack.MODE_STREAM,
            AudioManager.AUDIO_SESSION_ID_GENERATE
        )
    }

    private inner class PlayPCMThread : Thread() {

        override fun run() {
            try {
                audioTrack?.play()
                if(audioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING){
                    while (isPlaying) {
                        audioPcmQueue?.take()?.let { tempBuffer ->
                            RecLogUtils.playPcm()
                            audioTrack?.write(tempBuffer, 0, tempBuffer.size)
                        }
                    }
                }else{

                }
            } catch (e: IOException) {
            } finally {
                try {
                    audioTrack?.stop()
                } catch (e: IOException) {
                }
                audioTrack?.release()

                audioPcmQueue?.clear()
                audioPcmQueue = null
            }
        }

    }

    fun startPlay() {
        isPlaying = true
        audioPcmQueue = LinkedBlockingQueue<ByteArray>()
        playPCMThread = PlayPCMThread()
        playPCMThread?.start()
    }

    fun stopPlay() {
        isPlaying = false
        playPCMThread?.join()
        audioTrack = null
    }

    fun addAudioBytes(tempBuffer: ByteArray) {
        audioPcmQueue?.put(tempBuffer)
    }

}