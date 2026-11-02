package com.example.jarvis.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.MediaRecorder
import android.os.Build
import android.os.IBinder
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.speech.tts.TextToSpeech
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.jarvis.MainActivity
import com.example.jarvis.api.GeminiClient
import com.example.jarvis.api.GeminiConfig
import com.example.jarvis.api.JarvisResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

class JarvisVoiceService : Service(), TextToSpeech.OnInitListener {

    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var mediaRecorder: MediaRecorder? = null
    private var currentAudioFile: File? = null
    private var isRecording = false
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false

    companion object {
        const val ACTION_TOGGLE = "com.example.jarvis.action.TOGGLE"
        const val ACTION_START = "com.example.jarvis.action.START"
        const val ACTION_STOP = "com.example.jarvis.action.STOP"
        const val CHANNEL_ID = "jarvis_voice_channel"
        const val NOTIFICATION_ID = 1001

        private val _events = MutableSharedFlow<JarvisResponse>(extraBufferCapacity = 10)
        val events = _events.asSharedFlow()

        private val _stateFlow = MutableSharedFlow<String>(extraBufferCapacity = 10)
        val stateFlow = _stateFlow.asSharedFlow()
    }

    override fun onCreate() {
        super.onCreate()
        textToSpeech = TextToSpeech(this, this)
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("JOWELL CDPE listo"))
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START -> startRecording()
            ACTION_STOP -> stopRecordingAndProcess()
            ACTION_TOGGLE -> {
                if (isRecording) {
                    stopRecordingAndProcess()
                } else {
                    startRecording()
                }
            }
        }
        return START_STICKY
    }

    private fun startRecording() {
        if (isRecording) return
        try {
            val audioDir = File(filesDir, "recordings").apply { mkdirs() }
            currentAudioFile = File(audioDir, "reunion_${System.currentTimeMillis()}.mp4")

            mediaRecorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(this)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(96000)
                setAudioSamplingRate(44100)
                setOutputFile(currentAudioFile?.absolutePath)
                prepare()
                start()
            }

            isRecording = true
            vibrate(120)
            updateNotification("🔴 Grabando reunión CDPE...")
            serviceScope.launch { _stateFlow.emit("RECORDING") }
            Log.d("Jarvis", "Grabación iniciada: ${currentAudioFile?.absolutePath}")
        } catch (e: Exception) {
            Log.e("Jarvis", "Error al iniciar grabación", e)
            vibrate(400)
            speak("Error al activar micrófono.")
        }
    }

    private fun stopRecordingAndProcess() {
        if (!isRecording) return
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            mediaRecorder = null
            isRecording = false

            // Doble vibración corta de confirmación
            vibrate(60)
            serviceScope.launch {
                kotlinx.coroutines.delay(120)
                vibrate(60)
            }

            updateNotification("⏳ Procesando reunión con IA CDPE...")
            serviceScope.launch { _stateFlow.emit("PROCESSING") }

            val fileToProcess = currentAudioFile
            if (fileToProcess != null && fileToProcess.exists()) {
                processAudioWithGemini(fileToProcess)
            }
        } catch (e: Exception) {
            Log.e("Jarvis", "Error al detener grabación", e)
            updateNotification("JOWELL CDPE en espera")
            serviceScope.launch { _stateFlow.emit("IDLE") }
        }
    }

    private fun processAudioWithGemini(file: File) {
        serviceScope.launch {
            val prefs = getSharedPreferences("jarvis_prefs", Context.MODE_PRIVATE)
            val customKey = prefs.getString("gemini_api_key", "") ?: ""
            val apiKey = if (customKey.isNotBlank()) customKey else GeminiConfig.DEFAULT_API_KEY

            val client = GeminiClient(apiKey)
            val result = client.analyzeAudio(file)

            Log.d("Jarvis", "Resultado: ${result.spokenSummary}")
            speak(result.spokenSummary)
            updateNotification(result.spokenSummary.take(50))
            _events.emit(result)
            _stateFlow.emit("IDLE")
        }
    }

    private fun speak(text: String) {
        if (ttsReady && textToSpeech != null) {
            textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_tts_${System.currentTimeMillis()}")
        }
    }

    private fun vibrate(durationMs: Long) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(durationMs, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                @Suppress("DEPRECATION")
                vibrator?.vibrate(durationMs)
            }
        } catch (e: Exception) {
            Log.e("Jarvis", "Vibration error", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Jarvis Voice",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Jarvis activo en segundo plano para captura por voz"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(text: String): Notification {
        val openIntent = Intent(this, MainActivity::class.java)
        val pendingOpen = PendingIntent.getActivity(
            this, 0, openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val toggleIntent = Intent(this, JarvisVoiceService::class.java).apply {
            action = ACTION_TOGGLE
        }
        val pendingToggle = PendingIntent.getService(
            this, 1, toggleIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("JOWELL CDPE")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_btn_speak_now)
            .setContentIntent(pendingOpen)
            .addAction(android.R.drawable.ic_media_play, "Grabar / Detener", pendingToggle)
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(text: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(NOTIFICATION_ID, buildNotification(text))
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale.getDefault())
            ttsReady = (result != TextToSpeech.LANG_MISSING_DATA && result != TextToSpeech.LANG_NOT_SUPPORTED)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mediaRecorder?.release()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
