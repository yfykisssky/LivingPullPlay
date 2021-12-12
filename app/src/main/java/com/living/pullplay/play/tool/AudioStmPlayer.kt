package com.living.pullplay.play.tool

import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import java.io.IOException

class AudioStmPlayer {

    private var audioTrack: AudioTrack? = null

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

    fun startPlay() {
        try {
            audioTrack?.play()
        } catch (e: IOException) {
        }
    }

    fun stopPlay() {
        try {
            audioTrack?.stop()
        } catch (e: IOException) {
        }
        audioTrack?.release()
        audioTrack = null
    }

    fun addAudioBytes(tempBuffer: ByteArray) {
        try {
            audioTrack?.write(tempBuffer, 0, tempBuffer.size)
        } catch (e: IOException) {
        }
    }

}