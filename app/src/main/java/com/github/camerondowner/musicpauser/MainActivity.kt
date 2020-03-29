package com.github.camerondowner.musicpauser

import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.AudioManager.AUDIOFOCUS_GAIN_TRANSIENT
import android.media.AudioManager.STREAM_MUSIC
import android.media.MediaRecorder
import android.media.audiofx.Visualizer
import android.media.audiofx.Visualizer.OnDataCaptureListener
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.math.log10


class MainActivity : AppCompatActivity() {
    private val audioFocusRequest = AudioFocusRequest.Builder(AUDIOFOCUS_GAIN_TRANSIENT).build()

    private var pauseMusicButton: Button? = null
    private var recordButton: Button? = null
    private var stopRecordButton: Button? = null
    private var currentDbView: TextView? = null
    private var maxDbView: TextView? = null

    private var maxDb: Double = 0.0

    private var mediaRecorder: MediaRecorder? = null

    private var audioOutput: Visualizer? = null
    var intensity =
        0f //intensity is a value between 0 and 1. The intensity in this case is the system output volume

    private fun createVisualizer() {
        val rate = Visualizer.getMaxCaptureRate()
        audioOutput = Visualizer(0) // get output audio stream
        audioOutput!!.setDataCaptureListener(object : OnDataCaptureListener {
            override fun onWaveFormDataCapture(
                visualizer: Visualizer,
                waveform: ByteArray,
                samplingRate: Int
            ) {
                intensity = (waveform[0].toFloat() + 128f) / 256
                Log.i("vis", intensity.toString())
            }

            override fun onFftDataCapture(
                visualizer: Visualizer,
                fft: ByteArray,
                samplingRate: Int
            ) {
            }
        }, rate, true, false) // waveform not freq data
        Log.i("rate", Visualizer.getMaxCaptureRate().toString())
        audioOutput!!.enabled = true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        createVisualizer()

        pauseMusicButton = findViewById(R.id.pause_music_button)
        recordButton = findViewById(R.id.record_mic)
        stopRecordButton = findViewById(R.id.stop_record_mic)
        currentDbView = findViewById(R.id.hello_world)
        maxDbView = findViewById(R.id.max_db)

        Thread {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            while (true) {
                val streamVolume = audioManager.getStreamVolume(STREAM_MUSIC)

                runOnUiThread {
                    updateMusicVolumeView(streamVolume)
                }
                Thread.sleep(500)
            }

        }.start()

        pauseMusicButton?.setOnClickListener {
            val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager

            if (audioManager.isMusicActive) {
                pauseMusic(audioManager)
            } else {
                resumeMusic(audioManager)
            }
        }


        recordButton?.setOnClickListener {
            if (mediaRecorder != null) {
                return@setOnClickListener
            }
            mediaRecorder = MediaRecorder()
            mediaRecorder!!.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setOutputFile("/dev/null")
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                prepare()
                start()
            }

            Thread {
                while (mediaRecorder != null) {
                    runOnUiThread {
                        updateTv()
                    }
                    Thread.sleep(250)
                }
            }.start()
        }

        stopRecordButton!!.setOnClickListener {
            if (mediaRecorder != null) {
                mediaRecorder!!.apply {
                    stop()
                    release()
                }
                mediaRecorder = null
            }
        }
    }

    fun updateMusicVolumeView(volume: Int) {
        maxDbView?.text = volume.toString()
    }

    fun updateTv() {
        val amplitudeEMA = getAmplitudeEMA()
//        if (amplitudeEMA > maxDb) {
//            maxDb = amplitudeEMA
//            maxDbView?.text = maxDb.toString() + " dB"
//        }
        currentDbView?.text = amplitudeEMA.toString() + " dB"
        println(amplitudeEMA)
    }

    fun soundDb(ampl: Double): Double {
        return 20 * log10(getAmplitudeEMA() / ampl)
    }

    fun getAmplitude(): Int {
        return mediaRecorder?.maxAmplitude ?: 0
    }

    private var mEMA = 0.0
    private val EMA_FILTER = 0.6

    private fun getAmplitudeEMA(): Double {
        val amp = getAmplitude()
        mEMA = EMA_FILTER * amp + (1.0 - EMA_FILTER) * mEMA
        return mEMA
    }

    private fun resumeMusic(audioManager: AudioManager) {
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }

    private fun pauseMusic(audioManager: AudioManager) =
        audioManager.requestAudioFocus(audioFocusRequest)
}
