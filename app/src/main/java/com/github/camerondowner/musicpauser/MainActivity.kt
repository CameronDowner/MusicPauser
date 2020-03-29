package com.github.camerondowner.musicpauser


import android.app.Activity
import android.content.Context
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import com.github.camerondowner.musicpauser.audio.AudioCalculator
import java.time.Clock


class MainActivity : Activity() {
    private val audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT).build()


    private var recorder = Recorder()
    private var audioCalculator = AudioCalculator()
    private var handler = Handler(Looper.getMainLooper())

    private var textAmplitude: TextView? = null
    private var textAverageAmplitude: TextView? = null
    private var textMaxAmplitude: TextView? = null
    private var textDecibel: TextView? = null
    private var textFrequency: TextView? = null

    private var editTextThreshold: EditText? = null

    private var shouldStop: TextView? = null
    private var buttonMeasureMusic: Button? = null

    private var averageAmplitude: Double = 0.0
    private var maxAmplitude: Int = 0
    private var count: Int = 0

    private var measureAverage = false

    private var stoppedAt: Long? = null

    fun calculateAverage(newAmplitude: Int) {
        count = count.inc()
        averageAmplitude = (averageAmplitude * count + newAmplitude) / (count + 1)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        textAmplitude = findViewById(R.id.textAmplitude)
        textAverageAmplitude = findViewById(R.id.textAverageAmplitude)
        textMaxAmplitude = findViewById(R.id.textMaxAmplitude)
        textDecibel = findViewById(R.id.textDecibel)
        textFrequency = findViewById(R.id.textFrequency)

        editTextThreshold = findViewById(R.id.threshold)

        shouldStop = findViewById(R.id.shouldStop)
        buttonMeasureMusic = findViewById(R.id.buttonMeasureMusic)

        buttonMeasureMusic!!.setOnClickListener {
            measureAverage = measureAverage.not()
            if (measureAverage) {
                maxAmplitude = 0
                averageAmplitude = 0.0
                count = 0
            }
        }
    }

    private val callback: (buffer: ByteArray) -> Unit = { buffer ->
        audioCalculator.setBytes(buffer)
        val amplitude = audioCalculator.amplitude
        val decibel = audioCalculator.decibel
        val frequency = audioCalculator.frequency
        val amp = "$amplitude Amp"
        val db = "$decibel db"
        val hz = "$frequency Hz"

        if (measureAverage) {
            calculateAverage(amplitude)
            if (amplitude > maxAmplitude) {
                maxAmplitude = amplitude
            }
        } else {
            val foo = editTextThreshold?.text.toString().toDoubleOrNull() ?: 1.5
            if (count > 0 && amplitude > maxAmplitude * foo) {
                stoppedAt = Clock.systemDefaultZone().millis()
                handler.post {
                    pauseMusic()
                    shouldStop!!.text = "STOP MUSIC"
                }
            } else {
                if (stoppedAt != null && Clock.systemDefaultZone().millis() > stoppedAt!! + 2000) {
                    resumeMusic()
                    handler.post {
                        shouldStop!!.text = "PLAY MUSIC"
                    }
                }
            }
        }

        handler.post {
            textAmplitude!!.text = amp
            textDecibel!!.text = db
            textFrequency!!.text = hz
            textAverageAmplitude!!.text = "Avg: ${averageAmplitude.toInt()} Amp"
            textMaxAmplitude!!.text = "Max: $maxAmplitude Amp"
        }
    }

    override fun onResume() {
        super.onResume()
        recorder.start(callback)
    }

    override fun onPause() {
        super.onPause()
        recorder.stop()
    }

    private fun resumeMusic() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocusRequest(audioFocusRequest)
    }

    private fun pauseMusic() {
        val audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.requestAudioFocus(audioFocusRequest)
    }
}
