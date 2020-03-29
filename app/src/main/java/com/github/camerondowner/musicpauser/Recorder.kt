package com.github.camerondowner.musicpauser

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Process
import android.util.Log


class Recorder {
    private val audioSource = MediaRecorder.AudioSource.DEFAULT
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO
    private val audioEncoding = AudioFormat.ENCODING_PCM_16BIT
    private val sampleRate = 44100
    private var thread: Thread? = null

    fun start(callback: (buffer: ByteArray) -> Unit) {
        if (thread != null) {
            return
        }

        thread = buildThread(callback)
        thread!!.start()
    }

    private fun buildThread(callback: (buffer: ByteArray) -> Unit): Thread {
        return Thread(Runnable {
            Process.setThreadPriority(Process.THREAD_PRIORITY_URGENT_AUDIO)
            val minBufferSize =
                AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioEncoding)
            val recorder = AudioRecord(
                audioSource,
                sampleRate,
                channelConfig,
                audioEncoding,
                minBufferSize
            )
            if (recorder.state == AudioRecord.STATE_UNINITIALIZED) {
                Thread.currentThread().interrupt()
                return@Runnable
            } else {
                Log.i(Recorder::class.java.simpleName, "Started.")
            }
            val buffer = ByteArray(minBufferSize)
            recorder.startRecording()
            while (thread != null && !thread!!.isInterrupted && recorder.read(
                    buffer,
                    0,
                    minBufferSize
                ) > 0
            ) {
                callback(buffer)
            }
            recorder.stop()
            recorder.release()
        }, Recorder::class.java.name)
    }

    fun stop() {
        if (thread != null) {
            thread!!.interrupt()
            thread = null
        }
    }
}