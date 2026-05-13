package com.portfolio.snorerecoder



import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.annotation.RequiresPermission
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    private lateinit var classifier: YamnetClassifier
    private lateinit var recorder: AudioRecorder

    private lateinit var btnRecord: Button
    private lateinit var btnStop: Button
    private lateinit var tvStatus: TextView
    private lateinit var tvCategory: TextView
    private lateinit var tvConfidence: TextView
    private lateinit var tvRawLabel: TextView
    private lateinit var tvLog: TextView

    private val scope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private val logBuilder = StringBuilder()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        btnRecord = findViewById(R.id.btnRecord)
        btnStop = findViewById(R.id.btnStop)
        tvStatus = findViewById(R.id.tvStatus)
        tvCategory = findViewById(R.id.tvCategory)
        tvConfidence = findViewById(R.id.tvConfidence)
        tvRawLabel = findViewById(R.id.tvRawLabel)
        tvLog = findViewById(R.id.tvLog)

        classifier = YamnetClassifier(this)

        btnRecord.setOnClickListener { checkPermissionAndRecord() }
        btnStop.setOnClickListener { stopRecording() }
    }

    private fun checkPermissionAndRecord() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                this, arrayOf(Manifest.permission.RECORD_AUDIO), 101
            )
        } else {
            startRecording()
        }
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    private fun startRecording() {
        btnRecord.isEnabled = false
        btnStop.isEnabled = true
        tvStatus.text = "🔴 Recording & Analyzing..."

        recorder = AudioRecorder(sampleRate = 16000) { audioChunk ->
            // Run classification on IO thread, update UI on Main
            scope.launch(Dispatchers.IO) {
                val result = classifier.classify(audioChunk)

                withContext(Dispatchers.Main) {
                    updateUI(result)
                }
            }
        }
        recorder.startRecording()
    }

    private fun updateUI(result: YamnetClassifier.ClassificationResult) {
        val confidencePct = "%.1f".format(result.confidence * 100)

        tvCategory.text = "${result.category.emoji} ${result.category.displayName}"
        tvConfidence.text = "Confidence: $confidencePct%"
        tvRawLabel.text = "Raw: ${result.label}"

        // Append to log
        logBuilder.insert(0, "[${result.category.displayName}] ${result.label} ($confidencePct%)\n")
        if (logBuilder.length > 2000) logBuilder.setLength(2000) // limit log size
        tvLog.text = logBuilder.toString()
    }

    private fun stopRecording() {
        recorder.stopRecording()
        btnRecord.isEnabled = true
        btnStop.isEnabled = false
        tvStatus.text = "⏹ Stopped. Tap Record to restart."
    }

    @RequiresPermission(Manifest.permission.RECORD_AUDIO)
    override fun onRequestPermissionsResult(
        requestCode: Int, permissions: Array<out String>, grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == 101 && grantResults.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            startRecording()
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        classifier.close()
        scope.cancel()
    }
}