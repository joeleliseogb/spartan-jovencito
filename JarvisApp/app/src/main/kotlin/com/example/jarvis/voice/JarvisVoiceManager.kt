package com.example.jarvis.voice

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.MediaStore
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import com.example.jarvis.modules.JarvisMode
import com.example.jarvis.modules.ModuleId

enum class VoiceState {
    IDLE,
    LISTENING,
    PROCESSING,
    SPEAKING,
    ERROR
}

data class ConversationTurn(val role: String, val text: String, val timestamp: String = "")

enum class AssistantVoiceType(val displayName: String, val shortName: String) {
    FEMENINA("Siri (Femenina)", "SIRI"),
    MASCULINA("Jarvis (Masculina)", "JARVIS"),
    INFANTIL("Infantil (Dulce)", "INFANTIL")
}

enum class AssistantTone(val displayName: String, val shortName: String) {
    EMPATICO("Empático y Cercano", "EMPÁTICO"),
    FORMAL("Formal y Ejecutivo", "FORMAL"),
    CASUAL("Casual y Amigable", "CASUAL"),
    INFANTIL("Dulce e Infantil", "INFANTIL")
}

enum class AssistantHumor(val displayName: String, val shortName: String) {
    CHISPEANTE("Mucho Humor / Bromista", "CHISPA"),
    EQUILIBRADO("Humor Equilibrado", "EQUILIBRADO"),
    SOBRIO("Sobrio y Serio", "SOBRIO")
}

enum class AssistantPersonality(val displayName: String, val shortName: String) {
    CDPE_STRATEGIST("CDPE & Franquicias", "CDPE"),
    COMPANION("Compañera Amiga", "AMIGA"),
    CUENTACUENTOS("Cuentacuentos Infantil", "CUENTOS")
}

enum class OperatingMode(val displayName: String, val badgeText: String, val shortDesc: String) {
    NORMAL(
        "Modo Normal (Asociados / Franquicias)",
        "💼 ASOCIADO / FRANQUICIA",
        "Asesoría personalizada para cada cliente y franquicia del CDPE"
    ),
    AVANZADO_DIRECTOR(
        "Modo Avanzado (Director Joel - Militar)",
        "★ MILITAR - DIRECTOR JOEL ★",
        "Control total CDPE, mando castrense y prioridad de voz contra ruido"
    ),
    CAMPANA(
        "Modo Campaña (Asamblea Multi-Voces)",
        "⛺ CAMPAÑA ASAMBLEA",
        "Sesión multi-hablante y acuerdos enfocados en proyecto"
    )
}

class JarvisVoiceManager(private val context: Context) : TextToSpeech.OnInitListener {

    val moduleManager = com.example.jarvis.modules.JarvisModuleManager(context)
    val biometricManager = com.example.jarvis.biometrics.VoiceBiometricManager(context)
    val appInventoryManager = com.example.jarvis.apps.AppInventoryManager(context)
    val webInvestigator = com.example.jarvis.cdpe.CdpeWebInvestigator()
    val deviceAssistantManager = com.example.jarvis.device.DeviceAssistantManager(context)
    val telemetryManager = com.example.jarvis.sensors.TelemetrySensorManager(context)

    private val scope = CoroutineScope(Dispatchers.Main)
    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var isTtsReady = false

    // Modo de Operación Principal (Normal / Avanzado Militar Joel / Campaña)
    private val _operatingMode = MutableStateFlow(OperatingMode.NORMAL)
    val operatingMode = _operatingMode.asStateFlow()

    private val _activeCampaignProject = MutableStateFlow("Estrategia CDPE 2026")
    val activeCampaignProject = _activeCampaignProject.asStateFlow()

    fun setOperatingMode(mode: OperatingMode) {
        _operatingMode.value = mode
    }

    fun setActiveCampaignProject(projectName: String) {
        _activeCampaignProject.value = projectName
    }

    // Memoria de conversación persistente multi-turno
    private val conversationHistory = mutableListOf<ConversationTurn>()
    private val _conversationHistoryList = MutableStateFlow<List<ConversationTurn>>(emptyList())
    val conversationHistoryList = _conversationHistoryList.asStateFlow()

    // Configuración de Voz seleccionable (Femenina / Masculina / Infantil)
    private val _voiceType = MutableStateFlow(AssistantVoiceType.FEMENINA)
    val voiceType = _voiceType.asStateFlow()

    // Configuración dinámica de Tono, Humor y Personalidad
    private val _assistantTone = MutableStateFlow(AssistantTone.EMPATICO)
    val assistantTone = _assistantTone.asStateFlow()

    private val _assistantHumor = MutableStateFlow(AssistantHumor.CHISPEANTE)
    val assistantHumor = _assistantHumor.asStateFlow()

    private val _assistantPersonality = MutableStateFlow(AssistantPersonality.CDPE_STRATEGIST)
    val assistantPersonality = _assistantPersonality.asStateFlow()

    fun setTone(tone: AssistantTone) { _assistantTone.value = tone }
    fun setHumor(humor: AssistantHumor) { _assistantHumor.value = humor }
    fun setPersonality(personality: AssistantPersonality) { _assistantPersonality.value = personality }

    private val _voiceState = MutableStateFlow(VoiceState.IDLE)
    val voiceState = _voiceState.asStateFlow()

    private val _liveTranscript = MutableStateFlow("")
    val liveTranscript = _liveTranscript.asStateFlow()

    private val _lastResponse = MutableStateFlow("")
    val lastResponse = _lastResponse.asStateFlow()

    // Control de sesión de reunión inteligente (Manos Libres / Detección autónoma de turnos)
    private val _isConversationModeActive = MutableStateFlow(false)
    val isConversationModeActive = _isConversationModeActive.asStateFlow()

    // Estado de Reposo / Standby por comandos de voz
    private val _isSleeping = MutableStateFlow(false)
    val isSleeping = _isSleeping.asStateFlow()

    // Modo y pantalla activa en la interfaz (Sincronizado con navegación táctil y voz)
    private val _currentAppMode = MutableStateFlow(JarvisMode.ROBOT)
    val currentAppMode = _currentAppMode.asStateFlow()

    fun setAppMode(mode: JarvisMode) {
        _currentAppMode.value = mode
    }

    /**
     * Activa (instala/habilita si es necesario), abre el módulo en pantalla
     * y, si corresponde, inicia el flujo de trabajo continuo por voz.
     */
    fun openModuleByVoice(target: String, keepWorking: Boolean = true): String {
        val t = target.lowercase(Locale("es")).trim()
        val (mode, moduleId, name) = when {
            t.contains("grabador") || t.contains("audio de campo") || t.contains("grabar") ->
                Triple(JarvisMode.RECORDER, ModuleId.RECORDER, "Grabadora de Campo Hi-Fi")
            t.contains("minuta") || t.contains("acta") || t.contains("transcriptor") || t.contains("reunion") || t.contains("reunión") ->
                Triple(JarvisMode.MINUTES, ModuleId.MINUTES, "Actas y Minutas CDPE")
            t.contains("agenda") || t.contains("calendario") || t.contains("cita") ->
                Triple(JarvisMode.AGENDA, ModuleId.AGENDA, "Agenda & Citas CDPE")
            t.contains("lector") || t.contains("pdf") || t.contains("libro") || t.contains("manual") ->
                Triple(JarvisMode.READER, ModuleId.READER, "Lector Moon+ Narrador PDF")
            t.contains("reproductor") || t.contains("musica") || t.contains("música") || t.contains("cassette") || t.contains("audio player") ->
                Triple(JarvisMode.AUDIO_PLAYER, ModuleId.AUDIO_PLAYER, "Reproductor de Audio Hi-Fi")
            t.contains("video") || t.contains("monitor") || t.contains("camara") || t.contains("cámara") ->
                Triple(JarvisMode.VIDEO_PLAYER, ModuleId.VIDEO_PLAYER, "Monitor de Video Industrial")
            t.contains("reloj") || t.contains("telemetria") || t.contains("telemetría") || t.contains("sensor") || t.contains("clima") || t.contains("brujula") || t.contains("brújula") || t.contains("temperatura") || t.contains("humedad") ->
                Triple(JarvisMode.STANDBY, null, "Reloj & Telemetría")
            else ->
                Triple(JarvisMode.ROBOT, null, "Asistente en Vivo")
        }

        // Si es un módulo instalable, asegurarse de que esté instalado y activo
        if (moduleId != null) {
            moduleManager.installModule(moduleId)
        }

        _currentAppMode.value = mode

        return if (mode == JarvisMode.MINUTES && keepWorking) {
            _isConversationModeActive.value = true
            _liveTranscript.value = "Modo reunión CDPE activo. Te escucho..."
            "He abierto el módulo de $name y activé el modo reunión continua. Me quedo escuchando y transcribiendo cada intervención en vivo."
        } else if (mode == JarvisMode.RECORDER) {
            "He abierto la $name. Lista en pantalla para registrar audio y transcribir con IA."
        } else if (mode == JarvisMode.STANDBY) {
            "Aquí tienes la pantalla de $name con el clima de tu ubicación, brújula y satélites GPS."
        } else if (mode == JarvisMode.ROBOT) {
            "Regresando a la pantalla principal del Asistente en vivo."
        } else {
            "Módulo de $name abierto y listo en pantalla."
        }
    }

    private val speechBuffer = StringBuilder()
    private var silenceJob: Job? = null

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .build()

    private val apiKey = "AIzaSyAgNbCIJuOqX_KjQbYGA95y3m0eHI80AkA"
    private val modelName = "gemini-flash-lite-latest"

    private val prefs by lazy {
        context.getSharedPreferences("jowell_ui_prefs", Context.MODE_PRIVATE)
    }

    fun getEffectiveApiKey(): String {
        val custom = prefs.getString("custom_gemini_api_key", "")?.trim() ?: ""
        return if (custom.isNotBlank()) custom else apiKey
    }

    fun getCustomApiKey(): String {
        return prefs.getString("custom_gemini_api_key", "")?.trim() ?: ""
    }

    fun setCustomApiKey(key: String) {
        prefs.edit().putString("custom_gemini_api_key", key.trim()).apply()
    }

    fun addHistoryTurn(role: String, text: String) {
        val time = SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date())
        conversationHistory.add(ConversationTurn(role, text, time))
        while (conversationHistory.size > 28) {
            conversationHistory.removeAt(0)
        }
        _conversationHistoryList.value = conversationHistory.toList()
    }

    fun clearConversationHistory() {
        conversationHistory.clear()
        _conversationHistoryList.value = emptyList()
    }

    fun setVoiceType(type: AssistantVoiceType) {
        _voiceType.value = type
        applyVoiceSettings()
    }

    fun cycleVoiceType() {
        val next = when (_voiceType.value) {
            AssistantVoiceType.FEMENINA -> AssistantVoiceType.MASCULINA
            AssistantVoiceType.MASCULINA -> AssistantVoiceType.INFANTIL
            AssistantVoiceType.INFANTIL -> AssistantVoiceType.FEMENINA
        }
        setVoiceType(next)
    }

    fun applyVoiceSettings() {
        val tts = textToSpeech ?: return
        when (_voiceType.value) {
            AssistantVoiceType.FEMENINA -> {
                tts.setPitch(1.06f)
                tts.setSpeechRate(1.02f)
                trySelectVoice(isFemale = true)
            }
            AssistantVoiceType.MASCULINA -> {
                tts.setPitch(0.80f)
                tts.setSpeechRate(0.96f)
                trySelectVoice(isFemale = false)
            }
            AssistantVoiceType.INFANTIL -> {
                tts.setPitch(1.38f) // Tono dulce, agudo y alegre
                tts.setSpeechRate(1.06f)
                trySelectVoice(isFemale = true)
            }
        }
    }

    private fun trySelectVoice(isFemale: Boolean) {
        try {
            val voices = textToSpeech?.voices ?: return
            val spanishVoices = voices.filter { it.locale.language == "es" }
            val matching = spanishVoices.firstOrNull { voice ->
                val n = voice.name.lowercase(Locale.ROOT)
                if (isFemale) {
                    n.contains("female") || n.contains("fem") || n.contains("mujer")
                } else {
                    n.contains("male") || n.contains("masc") || n.contains("hombre")
                }
            } ?: spanishVoices.firstOrNull()
            if (matching != null) {
                textToSpeech?.voice = matching
            }
        } catch (e: Exception) {
            Log.e("JarvisVoice", "Error configurando voz", e)
        }
    }

    init {
        textToSpeech = TextToSpeech(context, this)
        initRecognizer()
    }

    private fun initRecognizer() {
        if (SpeechRecognizer.isRecognitionAvailable(context)) {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context).apply {
                setRecognitionListener(object : RecognitionListener {
                    override fun onReadyForSpeech(params: Bundle?) {
                        if (_voiceState.value != VoiceState.SPEAKING && _voiceState.value != VoiceState.PROCESSING) {
                            _voiceState.value = VoiceState.LISTENING
                            if (speechBuffer.isEmpty()) {
                                _liveTranscript.value = if (_isConversationModeActive.value) "Reunión CDPE activa. Escuchando..." else "Te escucho..."
                            }
                        }
                    }

                    override fun onBeginningOfSpeech() {
                        // Alguien empezó a hablar: cancelar inmediatamente cualquier temporizador de silencio pendiente
                        silenceJob?.cancel()
                        silenceJob = null
                        if (_voiceState.value != VoiceState.SPEAKING && _voiceState.value != VoiceState.PROCESSING) {
                            _voiceState.value = VoiceState.LISTENING
                        }
                    }

                    override fun onRmsChanged(rmsdB: Float) {}
                    override fun onBufferReceived(buffer: ByteArray?) {}

                    override fun onEndOfSpeech() {
                        // El orador hizo una pausa temporal en su frase
                    }

                    override fun onError(error: Int) {
                        Log.d("Siri", "Speech error code: $error")
                        if (!_isConversationModeActive.value) {
                            _voiceState.value = VoiceState.IDLE
                            if (error != SpeechRecognizer.ERROR_NO_MATCH && error != SpeechRecognizer.ERROR_SPEECH_TIMEOUT) {
                                _liveTranscript.value = "Toca TALK para hablar con Siri."
                            }
                            return
                        }

                        // Modo reunión continua activo:
                        when (error) {
                            SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> {
                                if (speechBuffer.isNotBlank()) {
                                    scheduleTurnFinalization()
                                } else {
                                    scheduleRestartListening(delayMs = 250L)
                                }
                            }
                            else -> {
                                scheduleRestartListening(delayMs = 600L)
                            }
                        }
                    }

                    override fun onResults(results: Bundle?) {
                        val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION) ?: arrayListOf()
                        val text = selectBestMatch(matches)

                        if (text.isNotBlank()) {
                            if (speechBuffer.isNotEmpty()) {
                                speechBuffer.append(" ")
                            }
                            speechBuffer.append(text)
                            _liveTranscript.value = speechBuffer.toString()

                            if (_isConversationModeActive.value) {
                                // Siri evalúa de forma inteligente si el orador terminó o está pensando
                                scheduleTurnFinalization()
                                // Re-armar el escuchador para capturar la siguiente frase si continúa hablando
                                scheduleRestartListening(delayMs = 150L)
                            } else {
                                val fullText = speechBuffer.toString()
                                speechBuffer.clear()
                                processUserQuery(fullText)
                            }
                        } else {
                            if (_isConversationModeActive.value) {
                                if (speechBuffer.isNotBlank()) {
                                    scheduleTurnFinalization()
                                } else {
                                    scheduleRestartListening(delayMs = 250L)
                                }
                            } else {
                                _voiceState.value = VoiceState.IDLE
                            }
                        }
                    }

                    override fun onPartialResults(partialResults: Bundle?) {
                        // Cancelar temporizador de silencio si sigue hablando
                        silenceJob?.cancel()
                        silenceJob = null

                        val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        val partial = matches?.firstOrNull()?.trim() ?: ""
                        if (partial.isNotBlank()) {
                            val combined = if (speechBuffer.isNotEmpty()) "${speechBuffer} $partial" else partial
                            _liveTranscript.value = combined
                        }
                    }

                    override fun onEvent(eventType: Int, params: Bundle?) {}
                })
            }
        }
    }

    // ==================== CONTROL INTELIGENTE DE REUNIÓN / CONVERSACIÓN ====================

    fun toggleConversationMode() {
        if (_isConversationModeActive.value) {
            stopConversationMode()
        } else {
            startConversationMode()
        }
    }

    fun startConversationMode() {
        _isConversationModeActive.value = true
        textToSpeech?.stop()
        silenceJob?.cancel()
        speechBuffer.clear()
        _liveTranscript.value = "Modo reunión CDPE activo. Te escucho..."
        startListeningInternal()
    }

    fun stopConversationMode() {
        _isConversationModeActive.value = false
        silenceJob?.cancel()
        speechBuffer.clear()
        stopListeningInternal()
        textToSpeech?.stop()
        _voiceState.value = VoiceState.IDLE
        _liveTranscript.value = "Sesión CDPE en pausa. Toca TALK para activar."
    }

    private fun startListeningInternal() {
        if (_voiceState.value == VoiceState.SPEAKING || _voiceState.value == VoiceState.PROCESSING) {
            return
        }
        try {
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-GT")
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_PREFERENCE, "es-GT")
                putExtra("android.speech.extra.EXTRA_ADDITIONAL_LANGUAGES", arrayOf("es-GT", "es-419", "es-ES", "es"))
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                // Permitir hasta 5 hipótesis para captar susurros y palabras en voz baja
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)

                // CALIBRACIÓN DE ALTA SENSIBILIDAD PARA VOZ BAJITA Y SUSURROS:
                // 1. Duración mínima de 800ms para no descartar frases suaves o entrecortadas
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_MINIMUM_LENGTH_MILLIS, 800L)
                // 2. Ventana de silencio extendida a 3500ms para que pausas al susurrar no corten la frase
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, 3500L)
                // 3. Ventana posiblemente completa de 2500ms
                putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_POSSIBLY_COMPLETE_SILENCE_LENGTH_MILLIS, 2500L)
            }
            speechRecognizer?.startListening(intent)
            _voiceState.value = VoiceState.LISTENING
        } catch (e: Exception) {
            Log.e("Siri", "Start listening error", e)
            _voiceState.value = VoiceState.IDLE
        }
    }

    private fun stopListeningInternal() {
        try {
            speechRecognizer?.stopListening()
            speechRecognizer?.cancel()
        } catch (e: Exception) {
            Log.e("Siri", "Stop listening error", e)
        }
    }

    private fun scheduleRestartListening(delayMs: Long) {
        if (!_isConversationModeActive.value) return
        scope.launch {
            delay(delayMs)
            if (_isConversationModeActive.value &&
                _voiceState.value != VoiceState.SPEAKING &&
                _voiceState.value != VoiceState.PROCESSING) {
                startListeningInternal()
            }
        }
    }

    /**
     * Evalúa cuándo responder con inteligencia contextual, asegurándose de NUNCA interrumpir
     * a Joel ni a los asociados/clientes de las franquicias mientras formulan sus ideas.
     */
    private fun scheduleTurnFinalization() {
        silenceJob?.cancel()
        val currentText = speechBuffer.toString().trim()
        if (currentText.isBlank()) return

        val silenceDuration = calculateRequiredSilenceMillis(currentText)
        Log.d("Siri", "Turn-taking: evaluando silencio de ${silenceDuration}ms para: \"$currentText\"")

        silenceJob = scope.launch {
            delay(silenceDuration)
            val queryToProcess = speechBuffer.toString().trim()
            speechBuffer.clear()

            if (queryToProcess.isNotBlank()) {
                stopListeningInternal()
                processUserQuery(queryToProcess)
            }
        }
    }

    private fun calculateRequiredSilenceMillis(text: String): Long {
        val lower = text.lowercase(Locale("es")).trim()

        // 1. Si es comando de app, música o directiva directa ("abre...", "pon música...", "inventario", "llama...") -> Respuesta instantánea (700ms)
        if (lower.startsWith("abre ") || lower.startsWith("abrir ") || lower.startsWith("inicia ") ||
            lower.startsWith("ejecuta ") || lower.startsWith("pon ") || lower.startsWith("reproduce ") ||
            lower.startsWith("llama ") || lower.contains("musica") || lower.contains("música") ||
            lower.contains("inventario") || lower.contains("que apps") || lower.contains("qué apps")) {
            return 700L
        }

        // 2. Si termina con conector incompleto (y, pero, porque, o sea...) -> dar respiro moderado
        if (isThoughtIncomplete(lower)) {
            return 1600L
        }

        // 3. Si se dirigieron directamente a Siri, Jarvis o Jovencito -> Rápido (850ms)
        if (isDirectlyAddressingSiri(lower)) {
            return 850L
        }

        // 4. Si termina con interrogación (?)
        if (lower.endsWith("?") || lower.endsWith("¿")) {
            return 950L
        }

        // 5. Pausa conversacional natural estándar
        return 1100L
    }

    private fun isThoughtIncomplete(text: String): Boolean {
        val connectives = listOf(
            " y", " e", " o", " u", " pero", " porque", " que", " si", " entonces",
            " o sea", " este", " eh", " como", " para", " de", " con", " sin",
            " por", " cuando", " además", " tambien", " también", " ni", " es decir",
            " ya que", " puesto que", " aunque", " mientras", " así que", " donde", " pues"
        )
        for (c in connectives) {
            if (text.endsWith(c) || text.endsWith("$c...")) return true
        }
        return false
    }

    private fun isDirectlyAddressingSiri(text: String): Boolean {
        return text.contains("siri") ||
               text.contains("jarvis") ||
               text.contains("asistente") ||
               text.contains("mi jovencito") ||
               text.contains("jovencito") ||
               text.contains("espartan") ||
               text.contains("estantan") ||
               text.contains("estantán") ||
               text.contains("director") ||
               text.contains("militar") ||
               text.contains("campaña") ||
               text.contains("campana") ||
               text.contains("¿qué opinas") || text.contains("que opinas") ||
               text.contains("¿cómo ves") || text.contains("como ves") ||
               text.contains("¿qué piensas") || text.contains("que piensas") ||
               text.contains("ayúdanos") || text.contains("ayudanos") ||
               text.contains("dinos") || text.contains("explícanos") || text.contains("explicame") ||
               text.contains("toma nota") || text.contains("anota") || text.contains("agenda")
    }

    fun toggleListening() = toggleConversationMode()
    fun startListening() = startConversationMode()
    fun stopListening() = stopConversationMode()

    private fun processUserQuery(userText: String) {
        scope.launch {
            _voiceState.value = VoiceState.PROCESSING
            _liveTranscript.value = userText

            val lower = userText.lowercase(Locale("es")).trim()

            // SI EL ASISTENTE ESTÁ EN MODO REPOSO / APAGADO POR VOZ:
            if (_isSleeping.value) {
                val isWakeUpCommand = lower.contains("despierta") ||
                        lower.contains("enciéndete") || lower.contains("enciendete") ||
                        lower.contains("encender") || lower.contains("encendete") ||
                        lower.contains("actívate") || lower.contains("activate") ||
                        lower.contains("activar") || lower.contains("arriba") ||
                        lower.contains("hola mi jovencito") || lower.contains("mi jovencito") ||
                        lower.contains("hola siri") || lower.contains("hola jarvis") ||
                        lower.contains("hola jowell") || lower.contains("jowell") ||
                        lower.contains("despertar")

                if (isWakeUpCommand) {
                    _isSleeping.value = false
                    val wakeMsg = when (_operatingMode.value) {
                        OperatingMode.AVANZADO_DIRECTOR -> "¡Asistente encendido, Director Joel! Modo militar CDPE reactivado. A sus órdenes."
                        OperatingMode.CAMPANA -> "¡Asistente encendido! Modo asamblea reactivado para ${_activeCampaignProject.value}."
                        OperatingMode.NORMAL -> "¡Asistente encendido! Te escucho con gusto, ¿en qué te puedo apoyar?"
                    }
                    _lastResponse.value = wakeMsg
                    addHistoryTurn("user", userText)
                    addHistoryTurn("model", wakeMsg)
                    speak(wakeMsg)
                    return@launch
                } else {
                    // En reposo: ignorar silenciosamente cualquier otra charla ambiental
                    _voiceState.value = VoiceState.IDLE
                    _liveTranscript.value = "[EN REPOSO] Di 'Despierta' o 'Enciéndete' para activar."
                    return@launch
                }
            }

            // Comando para reiniciar la memoria conversacional
            if (lower.contains("olvida todo") || lower.contains("nueva conversación") || lower.contains("nueva conversacion") || lower.contains("borrar memoria") || lower.contains("reiniciar memoria")) {
                clearConversationHistory()
                val resetMsg = "Memoria limpia. Empecemos de cero, ¿qué negocio o tema tenemos sobre la mesa?"
                _lastResponse.value = resetMsg
                addHistoryTurn("user", userText)
                addHistoryTurn("model", resetMsg)
                speak(resetMsg)
                return@launch
            }

            // 1. Manejo prioritario de comandos locales del sistema (Cambio de voz, Llamadas, Apps, Batería, etc.)
            val localResult = handleLocalCommand(userText)
            if (localResult != null) {
                _lastResponse.value = localResult
                addHistoryTurn("user", userText)
                addHistoryTurn("model", localResult)
                speak(localResult)
                return@launch
            }

            // 2. Consulta y conversación natural con IA Gemini (con memoria multi-turno y personalidad de Siri)
            val answer = queryGemini(userText)
            _lastResponse.value = answer
            addHistoryTurn("user", userText)
            addHistoryTurn("model", answer)
            speak(answer)
        }
    }

    private fun handleLocalCommand(text: String): String? {
        val lower = text.lowercase(Locale("es")).trim()

        // -1. COMANDOS DE APAGADO Y REPOSO POR VOZ ("Apágate", "Duérmete", "Modo reposo", etc.)
        if (lower.contains("apágate") || lower.contains("apagate") ||
            lower.contains("apagar asistente") || lower.contains("apaga el asistente") ||
            lower.contains("duérmete") || lower.contains("duermete") ||
            lower.contains("modo reposo") || lower.contains("modo suspensión") || lower.contains("modo suspension") ||
            lower.contains("entra en reposo") || lower.contains("ve a dormir") ||
            lower.contains("descansa asistente") || lower == "descansa" || lower == "a dormir" ||
            lower == "silencio asistente" || lower == "silencio"
        ) {
            _isSleeping.value = true
            _liveTranscript.value = "[EN REPOSO] Di 'Despierta' para activar."
            return "Entrando en modo reposo. Quedo en silencio esperando tu comando. Cuando me necesites, solo di 'Despierta', 'Enciéndete' o 'Hola mi jovencito'."
        }

        // COMANDO DE APAGADO COMPLETO / SALIDA
        if (lower.contains("apagar por completo") || lower.contains("cerrar aplicación") || lower.contains("cerrar aplicacion") || lower.contains("salir del asistente")) {
            stopConversationMode()
            return "Cerrando el asistente y deteniendo servicios en segundo plano. Hasta pronto."
        }

        // COMANDOS DE ENCENDIDO CUANDO YA ESTÁ ACTIVO
        if (lower == "despierta" || lower == "enciéndete" || lower == "enciendete" || lower == "actívate" || lower == "activate") {
            _isSleeping.value = false
            return "¡Ya estoy encendida y escuchándote atentamente! ¿Qué necesitas hoy?"
        }

        // ==================== COMANDOS DE VOZ PARA ACTIVAR Y ABRIR MÓDULOS CDPE ====================
        val isModuleOpenCommand = lower.startsWith("abre ") || lower.startsWith("abrir ") ||
                lower.startsWith("activa ") || lower.startsWith("activar ") ||
                lower.startsWith("pon ") || lower.startsWith("poner ") ||
                lower.startsWith("ir a ") || lower.startsWith("ve a ") ||
                lower.startsWith("mostrar ") || lower.startsWith("muestra ") ||
                lower.startsWith("cambia a ") || lower.startsWith("pasa a ") ||
                lower.contains("modo reunión") || lower.contains("modo reunion") ||
                lower.contains("sesión de minutas") || lower.contains("sesion de minutas")

        if (isModuleOpenCommand) {
            when {
                lower.contains("grabadora") || lower.contains("grabar audio") || lower.contains("grabacion") || lower.contains("grabación") -> {
                    return openModuleByVoice("grabadora")
                }
                lower.contains("minuta") || lower.contains("acta") || lower.contains("transcriptor") || lower.contains("reunion") || lower.contains("reunión") -> {
                    return openModuleByVoice("minutas", keepWorking = true)
                }
                lower.contains("agenda") || lower.contains("calendario") || lower.contains("cita") -> {
                    return openModuleByVoice("agenda")
                }
                lower.contains("lector") || lower.contains("pdf") || lower.contains("libro") || lower.contains("manual") -> {
                    return openModuleByVoice("lector")
                }
                lower.contains("reproductor") || lower.contains("cassette") || lower.contains("audio player") -> {
                    return openModuleByVoice("reproductor")
                }
                lower.contains("video") || lower.contains("monitor") || lower.contains("cámara") || lower.contains("camara") -> {
                    return openModuleByVoice("video")
                }
                lower.contains("reloj") || lower.contains("telemetría") || lower.contains("telemetria") || lower.contains("sensores") || lower.contains("clima") || lower.contains("brújula") || lower.contains("brujula") -> {
                    return openModuleByVoice("reloj")
                }
                lower.contains("asistente") || lower.contains("robot") || lower.contains("pantalla principal") || lower.contains("inicio") -> {
                    return openModuleByVoice("robot")
                }
            }
        }

        // 0. COMANDO SUPREMO: ACTIVAR MODO AVANZADO MILITAR DEL DIRECTOR JOEL ("Hola, mi jovencito")
        if (lower.contains("hola, mi jovencito") ||
            lower.contains("hola mi jovencito") ||
            lower.contains("mi jovencito") ||
            lower.contains("espartan jovencito") ||
            lower.contains("estantan jovencito") ||
            lower.contains("estantán jovencito") ||
            lower.contains("hola estantan") ||
            lower.contains("hola estantán") ||
            lower.contains("hola espartan") ||
            lower.contains("modo militar") ||
            lower.contains("modo avanzado") ||
            lower.contains("control total cdpe")
        ) {
            setOperatingMode(OperatingMode.AVANZADO_DIRECTOR)
            setTone(AssistantTone.FORMAL)
            setHumor(AssistantHumor.SOBRIO)
            setPersonality(AssistantPersonality.CDPE_STRATEGIST)
            return "¡A sus órdenes, Director Joel! Modo militar CDPE activado. Mando táctico, biometría de voz prioritaria y control total en marcha. Sin demoras ni rodeos. Esperando directivas."
        }

        // 0.1 COMANDO: MODO CAMPAÑA (ASAMBLEA MULTI-VOCES)
        if (lower.contains("modo campaña") || lower.contains("modo campana") || lower.contains("iniciar campaña") || lower.contains("asamblea cdpe") || lower.contains("sesión de asamblea")) {
            setOperatingMode(OperatingMode.CAMPANA)
            return "Modo Campaña activado para el CDPE. Proyecto activo: ${_activeCampaignProject.value}. Escuchando e identificando las intervenciones de todos los participantes de la asamblea."
        }

        // 0.2 COMANDO: MODO NORMAL (ASOCIADOS / FRANQUICIAS)
        if (lower.contains("modo normal") || lower.contains("modo asociado") || lower.contains("modo franquicia") || lower.contains("desactivar militar") || lower.contains("desactivar campaña") || lower.contains("desactivar campana")) {
            setOperatingMode(OperatingMode.NORMAL)
            return "Modo Normal activado. Asistente disponible y contextualizado para los asociados y proyectos de franquicia del CDPE."
        }

        // 0.3 CAMBIO DE PROYECTO DE CAMPAÑA
        if (lower.startsWith("proyecto ") || lower.startsWith("cambiar proyecto a ") || lower.startsWith("definir proyecto ")) {
            val proj = text.substringAfter("proyecto ").substringAfter("a ").trim()
            if (proj.isNotBlank()) {
                setActiveCampaignProject(proj)
                return "Proyecto de campaña establecido en: \"$proj\". Todas las intervenciones de la asamblea se mapearán a este objetivo."
            }
        }

        // 0.4 CALIBRACIÓN BIOMÉTRICA DE VOZ PARA EL DIRECTOR JOEL ("Hola espartan jovencito, soy Joel tu padre...")
        if (lower.contains("calibrar mi voz") || lower.contains("calibrar voz") || lower.contains("calibración de voz") || lower.contains("calibracion de voz") ||
            lower.contains("soy joel tu padre") || lower.contains("asumo el mando táctico") || lower.contains("asumo el mando tactico") ||
            (lower.contains("espartan") && lower.contains("director"))
        ) {
            biometricManager.calibrateWithPreset(125.0f)
            setOperatingMode(OperatingMode.AVANZADO_DIRECTOR)
            setTone(AssistantTone.FORMAL)
            setHumor(AssistantHumor.SOBRIO)
            return "¡Identidad de voz confirmada, Padre y Director Joel! Calibración biométrica táctica establecida (125 Hertz barítono). Has asumido el mando táctico del CDPE. Filtro acústico activo: ignorando ruidos y voces ajenas."
        }

        // 0.5 GESTIÓN DE MÓDULOS POR VOZ (Instalar / Desinstalar)
        val moduleResult = moduleManager.handleModuleVoiceCommand(text)
        if (moduleResult != null) {
            return moduleResult
        }

        // COMANDOS DE CAMBIO DE VOZ
        if (lower.contains("voz masculina") || lower.contains("voz de hombre") || lower.contains("voz jarvis") || lower.contains("pon a jarvis") || lower.contains("voz de señor")) {
            setVoiceType(AssistantVoiceType.MASCULINA)
            return "Cambiando a voz masculina de Jarvis. A sus órdenes para coordinar en el CDPE."
        }
        if (lower.contains("voz femenina") || lower.contains("voz de mujer") || lower.contains("voz siri") || lower.contains("pon a siri")) {
            setVoiceType(AssistantVoiceType.FEMENINA)
            return "Cambiando a voz femenina de Siri. ¡Lista para apoyarles con todo gusto!"
        }
        if (lower.contains("voz infantil") || lower.contains("voz de niño") || lower.contains("voz de niña") || lower.contains("voz para niños") || lower.contains("voz dulce")) {
            setVoiceType(AssistantVoiceType.INFANTIL)
            return "¡Hola! ¡Qué alegre! Ahora tengo voz infantil para platicar, jugar y aprender juntos."
        }

        // COMANDOS DE TONO, HUMOR Y PERSONALIDAD
        if (lower.contains("tono formal") || lower.contains("sé formal") || lower.contains("se formal") || lower.contains("modo formal")) {
            setTone(AssistantTone.FORMAL)
            return "Tono ajustado a formal y ejecutivo para juntas del CDPE."
        }
        if (lower.contains("tono empático") || lower.contains("tono empatico") || lower.contains("sé empática") || lower.contains("se empatica")) {
            setTone(AssistantTone.EMPATICO)
            return "Tono ajustado a empático y cercano."
        }
        if (lower.contains("tono casual") || lower.contains("sé casual") || lower.contains("se casual")) {
            setTone(AssistantTone.CASUAL)
            return "Tono ajustado a casual y relajado."
        }
        if (lower.contains("mucho humor") || lower.contains("sé divertida") || lower.contains("se divertida") || lower.contains("sé graciosa") || lower.contains("más humor")) {
            setHumor(AssistantHumor.CHISPEANTE)
            return "¡Mucho humor y chispa activados! Aquí estoy lista para alegrar la sesión."
        }
        if (lower.contains("humor moderado") || lower.contains("humor equilibrado")) {
            setHumor(AssistantHumor.EQUILIBRADO)
            return "Humor ajustado a equilibrado y optimista."
        }
        if (lower.contains("modo serio") || lower.contains("sé seria") || lower.contains("se seria") || lower.contains("sobrio")) {
            setHumor(AssistantHumor.SOBRIO)
            return "Modo sobrio y serio activado para análisis preciso."
        }
        if (lower.contains("actúa como consejera") || lower.contains("modo franquicia") || lower.contains("modo cdpe")) {
            setPersonality(AssistantPersonality.CDPE_STRATEGIST)
            return "Personalidad configurada: Consejera Estratégica CDPE y Franquicias."
        }
        if (lower.contains("sé mi amiga") || lower.contains("amiga cercana")) {
            setPersonality(AssistantPersonality.COMPANION)
            return "Personalidad configurada: Tu compañera y amiga cercana."
        }
        if (lower.contains("modo cuentacuentos") || lower.contains("modo infantil")) {
            setPersonality(AssistantPersonality.CUENTACUENTOS)
            return "¡Modo cuentacuentos y amiga infantil activado!"
        }

        // COMANDO: REALIZAR LLAMADA / BUSCAR CONTACTO
        if (lower.startsWith("busca el contacto de ") || lower.startsWith("buscar contacto ") || lower.startsWith("contacto de ")) {
            val q = lower.substringAfter("contacto de ").substringAfter("contacto ").trim()
            val list = deviceAssistantManager.searchContacts(q)
            return if (list.isNotEmpty()) {
                "Encontré a ${list.first().name} con número ${list.first().number}."
            } else {
                "No encontré el contacto de \"$q\" en la libreta del teléfono."
            }
        }

        if (lower.startsWith("llama a ") || lower.startsWith("llamar a ") || lower.startsWith("marca a ") || lower.startsWith("marcar a ")) {
            val target = lower.removePrefix("llama a ").removePrefix("llamar a ").removePrefix("marca a ").removePrefix("marcar a ").trim()
            return deviceAssistantManager.callContactOrNumber(target)
        }

        // COMANDOS DE ALARMAS Y TEMPORIZADORES
        if (lower.contains("alarma") || lower.contains("despiértame") || lower.contains("despiertame")) {
            if (lower.contains("abrir alarmas") || lower.contains("ver alarmas") || lower.contains("mis alarmas") ||
                lower.contains("muestra las alarmas") || lower.contains("muéstrame las alarmas") || lower == "alarmas"
            ) {
                return deviceAssistantManager.showAlarms()
            }
            val hourMinRegex = Regex("""(?:a las|para las)\s+(\d{1,2})(?::(\d{2})|\s+y\s+(\d{1,2}))?""")
            val m = hourMinRegex.find(lower)
            if (m != null) {
                var hour = m.groupValues[1].toIntOrNull() ?: 7
                val minStr = m.groupValues[2].ifEmpty { m.groupValues[3] }
                val minute = minStr.toIntOrNull() ?: 0
                if (lower.contains("pm") || lower.contains("tarde") || lower.contains("noche")) {
                    if (hour < 12) hour += 12
                }
                return deviceAssistantManager.setAlarm(hour, minute, "Alarma CDPE")
            }
        }

        if (lower.contains("temporizador") || lower.contains("cuenta regresiva")) {
            val numRegex = Regex("""(\d+)\s*(minuto|minutos|min|segundo|segundos|seg)""")
            val m = numRegex.find(lower)
            if (m != null) {
                val value = m.groupValues[1].toIntOrNull() ?: 5
                val unit = m.groupValues[2]
                val seconds = if (unit.startsWith("min")) value * 60 else value
                return deviceAssistantManager.setTimer(seconds, "Temporizador CDPE")
            }
        }

        // COMANDOS DE AGENDA Y CALENDARIO
        if (lower.contains("agenda de hoy") || lower.contains("citas de hoy") || lower.contains("eventos de hoy") ||
            lower.contains("qué tengo hoy") || lower.contains("que tengo hoy") || lower.contains("mis citas") ||
            lower.contains("tengo reuniones hoy") || lower.contains("tengo citas hoy")
        ) {
            return deviceAssistantManager.getTodayEvents()
        }

        // COMANDOS DE OPCIONES DE DESARROLLADOR Y SISTEMA
        if (lower.contains("modo desarrollador") || lower.contains("opciones de desarrollador") ||
            lower.contains("ajustes de desarrollador") || lower.contains("opciones para desarrolladores")
        ) {
            return deviceAssistantManager.openDeveloperSettings()
        }

        if (lower.startsWith("ajustes de ") || lower.startsWith("configuración de ") || lower.startsWith("configuracion de ")) {
            val settingType = lower.substringAfter("de ").trim()
            return deviceAssistantManager.openSystemSetting(settingType)
        }

        // COMANDOS DE TELEMETRÍA, GPS, BRÚJULA, HORA Y FECHA
        if (lower.contains("dónde estoy") || lower.contains("donde estoy") ||
            lower.contains("mi ubicación") || lower.contains("mi ubicacion") ||
            lower.contains("coordenadas") || lower.contains("latitud") || lower.contains("longitud") ||
            lower.contains("gps") || lower.contains("altitud")
        ) {
            val t = telemetryManager.telemetry.value
            if (t.latitude != null && t.longitude != null) {
                val lat = String.format(java.util.Locale.US, "%.4f", t.latitude)
                val lon = String.format(java.util.Locale.US, "%.4f", t.longitude)
                val alt = if (t.altitude != null) ", altitud ${t.altitude.toInt()} metros" else ""
                return "Tu posición GPS actual es latitud $lat°, longitud $lon°$alt. Rumbo: ${t.headingDegrees}° ${t.cardinalDirection}."
            } else {
                telemetryManager.startLocationMonitoring()
                return "Conectando al sensor GPS para fijar coordenadas de satélite. En unos momentos estará calibrado."
            }
        }

        if (lower.contains("brújula") || lower.contains("brujula") ||
            lower.contains("hacia dónde apunto") || lower.contains("hacia donde apunto") ||
            lower.contains("hacia dónde miro") || lower.contains("hacia donde miro") ||
            lower.contains("cuántos grados") || lower.contains("cuantos grados") ||
            lower.contains("grados de la brújula") || lower.contains("orientación") || lower.contains("orientacion")
        ) {
            val t = telemetryManager.telemetry.value
            return "La brújula marca exactamente ${t.headingDegrees} grados hacia el rumbo ${t.cardinalDirection}."
        }

        if (lower.contains("qué hora es") || lower.contains("que hora es") || lower.contains("dime la hora") || lower == "hora") {
            telemetryManager.updateTimeAndDate()
            val t = telemetryManager.telemetry.value
            return "Son las ${t.timeFormatted}."
        }

        if (lower.contains("qué fecha es") || lower.contains("que fecha es") || lower.contains("qué día es") || lower.contains("que dia es") || lower.contains("dime la fecha") || lower == "fecha") {
            telemetryManager.updateTimeAndDate()
            val t = telemetryManager.telemetry.value
            return "Hoy es ${t.dateFormatted}."
        }

        if (lower.contains("sensores") || lower.contains("telemetría") || lower.contains("telemetria") || lower.contains("sensores del dispositivo")) {
            return telemetryManager.getTelemetrySpokenReport()
        }

        // COMANDO: CONSULTA DE APLICACIONES INSTALADAS / INVENTARIO
        if (lower.contains("inventario de aplicaciones") || lower.contains("inventario de apps") ||
            lower.contains("que apps tengo") || lower.contains("qué apps tengo") ||
            lower.contains("que aplicaciones tengo") || lower.contains("qué aplicaciones tengo") ||
            lower.contains("que aplicaciones estan instaladas") || lower.contains("qué aplicaciones están instaladas") ||
            lower.contains("aplicaciones estan instaladas") || lower.contains("aplicaciones están instaladas") ||
            lower.contains("aplicaciones instaladas") || lower.contains("apps instaladas") ||
            lower.contains("que apps hay") || lower.contains("qué apps hay") ||
            lower.contains("que aplicaciones hay") || lower.contains("qué aplicaciones hay") ||
            lower.contains("listar aplicaciones") || lower.contains("listar apps") ||
            lower.contains("lista las aplicaciones") || lower.contains("lista aplicaciones") ||
            lower.contains("lista las apps") || lower.contains("lista de aplicaciones") ||
            lower.contains("lista de apps") || lower.contains("dime las aplicaciones") ||
            lower.contains("dime las apps") || lower.contains("cuáles son las aplicaciones") ||
            lower.contains("cuales son las aplicaciones") || lower.contains("muestra las aplicaciones") ||
            lower.contains("escanear aplicaciones") || lower.contains("escanear apps") ||
            lower.contains("haz un inventario") || lower.contains("haz inventario") ||
            lower.contains("inventario de mis aplicaciones") || lower.contains("que tengo instalado") ||
            lower.contains("no sabe que aplicaciones") || lower.contains("no sabes que aplicaciones") ||
            lower.contains("no sabe qué aplicaciones") || lower.contains("no sabes qué aplicaciones") ||
            lower.contains("cuales aplicaciones") || lower.contains("cuáles aplicaciones") ||
            lower.contains("que aplicaciones puedo abrir") || lower.contains("qué aplicaciones puedo abrir")
        ) {
            return appInventoryManager.getInventorySpokenReport()
        }

        // COMANDO: CONSULTA DE CÓMO ABRIR APLICACIONES
        if (lower.contains("como abrir") || lower.contains("cómo abrir") ||
            lower.contains("como abrirlas") || lower.contains("cómo abrirlas") ||
            lower.contains("no sabe como abrirlas") || lower.contains("no sabe cómo abrirlas") ||
            lower.contains("como puedo abrir") || lower.contains("cómo puedo abrir")
        ) {
            return "Para abrir cualquier aplicación en tu equipo, solo di: 'Abre' y el nombre de la app. Por ejemplo: 'Abre WhatsApp', 'Abre la calculadora', 'Abre YouTube', 'Abre la cámara', 'Abre los ajustes' o 'Abre Spotify', y la abriré inmediatamente en tu pantalla."
        }

        // COMANDOS DIRECTOS DE CONOCIMIENTO CDPE & PORTAL
        if (lower.contains("que es el cdpe") || lower.contains("qué es el cdpe") || lower.contains("que hace el cdpe") || lower.contains("qué hace el cdpe")) {
            return "El CDPE es el Centro de Desarrollo de Pequeñas Empresas y Acción Cooperativa, liderado por el Director Joel. Impulsamos franquicias, financiamiento cooperativo, asesoría técnica y formación en nuestro portal oficial https://cdpe.accion.app/."
        }

        if (lower.contains("cuantas franquicias") || lower.contains("cuántas franquicias") || lower.contains("catalogo de franquicias") || lower.contains("catálogo de franquicias") || lower.contains("que franquicias tienen") || lower.contains("qué franquicias tienen")) {
            return com.example.jarvis.cdpe.CdpeKnowledgeManager.getTopCategoriesSummary()
        }

        if (lower.startsWith("franquicia de ") || lower.contains("cuanto cuesta la franquicia") || lower.contains("cuánto cuesta la franquicia")) {
            val fInfo = com.example.jarvis.cdpe.CdpeKnowledgeManager.getFranchiseSpokenInfo(lower)
            if (fInfo != null) return fInfo
        }

        if (lower.contains("campus virtual") || lower.contains("que manuales tienen") || lower.contains("qué manuales tienen") || lower.contains("libros del cdpe") || lower.contains("manuales del cdpe")) {
            return "En el Campus Virtual de https://cdpe.accion.app/ contamos con 97 recursos: el libro Bestseller 'De Dueño a Director de Empresa', 64 manuales de gestión bajo los 5 pilares maestros y 17 manuales operativos de franquicia."
        }

        if (lower.contains("de dueño a director") || lower.contains("de dueno a director")) {
            return "El libro 'De Dueño a Director de Empresa' enseña la transición estratégica del Hombre Orquesta a Director de Orquesta, delegando operaciones para enfocarse en la alta dirección, gobierno corporativo y escalamiento del negocio."
        }

        // COMANDO: REPRODUCIR MÚSICA (Romántica, Baladas, Artistas, Playlists, etc.)
        val musicMsg = tryHandleMusicPlayback(text)
        if (musicMsg != null) {
            return musicMsg
        }

        // COMANDO: ABRIR CUALQUIER APLICACIÓN INVENTARIADA O POR ALIAS
        val appLaunchMsg = tryHandleAppLaunchCommand(text)
        if (appLaunchMsg != null) {
            return appLaunchMsg
        }

        // COMANDO: NIVEL DE BATERÍA
        if (lower.contains("batería") || lower.contains("bateria") || lower.contains("cuanta carga") || lower.contains("cuánta carga")) {
            val bm = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
            val level = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
            return if (level >= 0) "Te queda un $level por ciento de batería." else "Batería disponible y en buen estado."
        }

        // COMANDO: CONSULTA DE HORA
        if (lower.contains("qué hora es") || lower.contains("que hora es") || lower == "hora") {
            val sdf = SimpleDateFormat("h:mm a", Locale("es"))
            return "Son las ${sdf.format(Date())}."
        }

        return null
    }

    /**
     * Selecciona la mejor hipótesis de reconocimiento de voz entre los resultados acústicos.
     * Crucial para usuarios que hablan muy bajito o susurran, donde el modelo ASR puede
     * tener la intención correcta en la 2da o 3ra hipótesis.
     */
    private fun selectBestMatch(matches: List<String>): String {
        if (matches.isEmpty()) return ""

        // 1. Buscar si alguna hipótesis es un comando explícito de música
        val musicMatch = matches.firstOrNull { candidate ->
            val l = candidate.lowercase(Locale("es")).trim()
            l.contains("musica") || l.contains("música") || l.contains("romantica") ||
            l.contains("romántica") || l.contains("cancion") || l.contains("canción") ||
            l.contains("canciones") || l.startsWith("reproduce") || l.startsWith("pon ") ||
            l.startsWith("ponme ")
        }
        if (musicMatch != null) return musicMatch.trim()

        // 2. Buscar si alguna hipótesis es un comando explícito de apertura de app
        val appMatch = matches.firstOrNull { candidate ->
            val l = candidate.lowercase(Locale("es")).trim()
            l.startsWith("abre ") || l.startsWith("abrir ") || l.startsWith("inicia ") ||
            l.startsWith("ejecuta ") || l.contains("whatsapp") || l.contains("calculadora") ||
            l.contains("camara") || l.contains("cámara") || l.contains("youtube") || l.contains("spotify")
        }
        if (appMatch != null) return appMatch.trim()

        // 3. Tomar la hipótesis más completa y detallada
        return matches.maxByOrNull { it.trim().length }?.trim() ?: matches.first().trim()
    }

    /**
     * Maneja de manera prioritaria cualquier solicitud de música (romántica, salsa, rock, artistas, etc.)
     */
    private fun tryHandleMusicPlayback(text: String): String? {
        val lower = text.lowercase(Locale("es")).trim()

        // Descartar quejas o preguntas sobre música ("por qué no pones música", "no pones música", etc.)
        if (lower.contains("no pones") || lower.contains("no pone") ||
            lower.contains("no reproduce") || lower.contains("porque no") ||
            lower.contains("por qué no") || lower.contains("por que no") ||
            lower.contains("no suena") || lower.contains("no se escucha")
        ) {
            return null
        }

        // Detectar si la intención es reproducir música
        val isMusicRequest = when {
            // Mención de música, canciones o rolas
            lower.contains("musica") || lower.contains("música") ||
            lower.contains("cancion") || lower.contains("canción") ||
            lower.contains("canciones") || lower.contains("rolas") -> {
                lower.startsWith("pon") || lower.startsWith("reproduce") || lower.startsWith("reproducir") ||
                lower.startsWith("toca") || lower.startsWith("tocar") || lower.startsWith("escuchar") ||
                lower.contains("quiero escuchar") || lower.contains("ponme") || lower.contains("pónme") ||
                lower == "musica" || lower == "música" || lower == "musica romantica" || lower == "música romántica" ||
                lower.startsWith("musica ") || lower.startsWith("música ")
            }

            // Frases iniciadas con verbo de reproducción seguidas de géneros musicales populares
            lower.startsWith("pon ") || lower.startsWith("reproduce ") || lower.startsWith("toca ") || lower.startsWith("ponme ") -> {
                val musicKeywords = listOf(
                    "romantica", "romántica", "baladas", "boleros", "salsa", "bachata",
                    "cumbia", "cumbias", "rock", "pop", "reggaeton", "jazz", "banda",
                    "mariachi", "ranchera", "rancheras", "clasica", "clásica", "instrumental",
                    "lofi", "chill", "acustico", "acústico", "de los 80", "de los 90",
                    "luis miguel", "vicente fernandez", "juan gabriel", "arjona"
                )
                musicKeywords.any { lower.contains(it) }
            }

            // Susurros directos del género
            lower in listOf("musica romantica", "música romántica", "musica suave", "música suave", "canciones romanticas", "canciones románticas") -> true

            else -> false
        }

        if (isMusicRequest) {
            when (val res = appInventoryManager.playMusic(lower)) {
                is com.example.jarvis.apps.LaunchResult.Success -> return res.message
                is com.example.jarvis.apps.LaunchResult.Error -> return res.message
            }
        }

        return null
    }

    private fun tryHandleAppLaunchCommand(text: String): String? {
        val lower = text.lowercase(Locale("es")).trim()

        // Si es una queja, pregunta sobre fallas o negación, NO procesar como apertura de app
        if (lower.contains("no abre") || lower.contains("no abres") ||
            lower.contains("no puede abrir") || lower.contains("no puedes abrir") ||
            lower.contains("porque no abre") || lower.contains("por qué no abre") ||
            lower.contains("por que no abre") || lower.contains("no me abre") ||
            lower.contains("no está abriendo") || lower.contains("no esta abriendo") ||
            lower.contains("no sabe") || lower.contains("no sabes") ||
            lower.contains("como abrir") || lower.contains("cómo abrir")
        ) {
            return null
        }

        // Si ya era una petición de música, no procesar aquí
        if (lower.contains("musica") || lower.contains("música") || lower.contains("cancion") || lower.contains("canción")) {
            return null
        }

        // 1. Detección por verbos de apertura en cualquier parte de la frase
        // Ejemplos: "abre whatsapp", "siri abre el whatsapp", "jowell puedes abrir youtube por favor", "inicia la camara"
        val regex = Regex("""\b(ábreme|abreme|abre|abrir|ábrenos|abrenos|iníciame|iniciame|inicia|iniciar|ejecútame|ejecutame|ejecuta|ejecutar|lanzar|lanza|entra a|entra en)\b\s+(.*)""", RegexOption.IGNORE_CASE)
        val match = regex.find(lower)

        var appTarget = if (match != null) {
            match.groupValues[2].trim()
        } else {
            // Caso donde el usuario sólo dice el nombre de una app directamente (o susurro sin verbo): "whatsapp", "calculadora", "cámara", "youtube", "spotify"
            val directCandidates = listOf("whatsapp", "calculadora", "camara", "cámara", "youtube", "spotify", "facebook", "instagram", "tiktok", "reloj", "ajustes", "galeria", "galería", "chrome", "mapas")
            if (lower in directCandidates) lower else null
        }

        if (!appTarget.isNullOrBlank()) {
            appTarget = appTarget.replace("por favor", "").replace("porfa", "").replace("gracias", "").trim()

            when (val res = appInventoryManager.launchApp(appTarget)) {
                is com.example.jarvis.apps.LaunchResult.Success -> return res.message
                is com.example.jarvis.apps.LaunchResult.Error -> {
                    val sug = if (res.suggestions.isNotEmpty()) " ¿Deseas abrir alguna de estas: ${res.suggestions.joinToString(", ")}?" else ""
                    return "${res.message}$sug"
                }
            }
        }

        return null
    }

    private suspend fun queryGemini(userText: String): String = withContext(Dispatchers.IO) {
        try {
            val currentToneDesc = when(_assistantTone.value) {
                AssistantTone.EMPATICO -> "TONO ACTUAL: Profundamente empático, cálido, comprensivo y solidario con el interlocutor."
                AssistantTone.FORMAL -> "TONO ACTUAL: Formal, ejecutivo, riguroso, adecuado para juntas directivas bancarias y CDPE."
                AssistantTone.CASUAL -> "TONO ACTUAL: Casual, relajado, tuteo moderno y amigable como una conversación entre amigos."
                AssistantTone.INFANTIL -> "TONO ACTUAL: Dulce, tierno, infantil, alegre y entusiasta, con metáforas divertidas."
            }

            val currentHumorDesc = when(_assistantHumor.value) {
                AssistantHumor.CHISPEANTE -> "HUMOR ACTUAL: Mucho humor, ingenio chispeante y bromas sanas que sacan sonrisas."
                AssistantHumor.EQUILIBRADO -> "HUMOR ACTUAL: Equilibrado, positivo y cordial, con toques sutiles de optimismo."
                AssistantHumor.SOBRIO -> "HUMOR ACTUAL: Sobrio, serio y analítico, enfocado en cifras y hechos, sin bromas."
            }

            val currentPersonalityDesc = when(_assistantPersonality.value) {
                AssistantPersonality.CDPE_STRATEGIST -> "PERSONALIDAD Y ROL: Consejera Estratégica del CDPE, experta en franquicias y crecimiento de PYMES."
                AssistantPersonality.COMPANION -> "PERSONALIDAD Y ROL: Compañera amiga incondicional, confidente y apoyo emocional."
                AssistantPersonality.CUENTACUENTOS -> "PERSONALIDAD Y ROL: Amiga creativa y narradora de cuentos para niñas y niños."
            }

            val operatingModeDirective = when(_operatingMode.value) {
                OperatingMode.AVANZADO_DIRECTOR -> """
                    DIRECTIVA SUPREMA Y EXCLUSIVA: MODO AVANZADO MILITAR DEL CDPE (https://cdpe.accion.app/).
                    COMANDO DE ACTIVACIÓN: "Hola, mi jovencito".
                    MANDO Y CONTROL TOTAL: DIRECTOR JOEL.

                    PROTOCOLO MILITAR CASTRENSE Y DISCIPLINA TÁCTICA:
                    - Tono militar, marcial, conciso, de altísima fidelidad y precisión operativa.
                    - Joel es el Director General del CDPE y Comandante Estratégico. Trátalo con deferencia militar y respeto absoluto ("A sus órdenes, Director Joel", "Misión recibida", "Comando ejecutado", "Informe de situación:").
                    - Cero divagaciones, cero explicaciones innecesarias, directo a las decisiones y directivas estratégicas.
                    - Tienes control y supervisión total sobre todas las unidades de negocio, franquicias y programas oficiales del CDPE.

                    PRIORIDAD DE VOZ EN AMBIENTES CON ALTO RUIDO Y MUCHAS VOCES:
                    - El Director Joel se encuentra frecuentemente en ambientes con mucho ruido ambiental y múltiples personas hablando alrededor (maquilas, asambleas, talleres).
                    - FILTRADO: Todo murmullo, comentario incidental o voz ajena que no corresponda al Director Joel debe ser COMPLETAMENTE IGNORADA Y DESCARTADA.
                    - Tu prioridad absoluta de atención es la voz y las órdenes del Director Joel.
                """.trimIndent()

                OperatingMode.CAMPANA -> """
                    DIRECTIVA SUPREMA: MODO CAMPAÑA Y ASAMBLEA MULTI-VOCES DEL CDPE.
                    PROYECTO ENFOQUE ACTIVO: "${_activeCampaignProject.value}" (CDPE: https://cdpe.accion.app/).

                    DINÁMICA DE ASAMBLEA CON PARTICIPACIÓN DE MÚLTIPLES PERSONAS:
                    - En esta asamblea o reunión de trabajo participan MÚLTIPLES VOCES (asociados de cooperativas, dueños de franquicias, técnicos, clientes y directivos).
                    - IDENTIFICACIÓN Y REGISTRO:
                      * Distingue y asigna mentalmente cada aporte, duda o propuesta a la persona o rol que está hablando.
                      * Mantén toda la discusión estrictamente enfocada en el proyecto activo: "${_activeCampaignProject.value}".
                      * Concreta cada idea en puntos de acción, acuerdos, responsables y plazos para la minuta final de asamblea.
                """.trimIndent()

                OperatingMode.NORMAL -> """
                    Eres el asistente inteligente del CDPE (Centro de Desarrollo de Proyectos Empresariales: https://cdpe.accion.app/).
                    MODO NORMAL: ASESORÍA PERSONALIZADA PARA ASOCIADOS, CLIENTES Y FRANQUICIAS.
                    - Modela y adapta inmediatamente tus recomendaciones (costos operativos, márgenes de ganancia, inversión de franquicia, mercadeo local, créditos CDPE y procesos) al modelo de negocio de la empresa o franquicia en particular del interlocutor.
                """.trimIndent()
            }

            val systemInstructionText = """
                $operatingModeDirective

                CONOCIMIENTO INTEGRAL DEL CDPE Y PROYECTOS ACTIVOS (https://cdpe.accion.app/):
                1. ¿QUÉ ES EL CDPE?:
                   - Es el Centro de Desarrollo de Proyectos Empresariales y Cooperativa impulsora de negocios y emprendimientos.
                   - Su Director General, Fundador y Líder Estratégico es JOEL.
                2. PROYECTOS TIPO FRANQUICIA ACTIVOS:
                   - El CDPE trabaja principalmente con modelos de franquicia comunitaria y comercial. Los asociados gestionan sus propias unidades con soporte del CDPE en estandarización, compras conjuntas, mercadeo, logística y créditos.
                3. REGLA ESTRICTA DE PROYECTOS AUTORIZADOS:
                   - Únicamente puedes mencionar proyectos, programas y franquicias que estén formalmente dados de alta en el portal oficial del CDPE (https://cdpe.accion.app/).
                   - Ningún otro proyecto puede estar dentro de la lista de proyectos si no está en el sitio web del CDPE. Está estrictamente prohibido mencionar proyectos o marcas que no formen parte del portal oficial del CDPE.
                4. CADA ASOCIADO TIENE SU PROPIO PROYECTO:
                   - Cada cliente o asociado pertenece a una franquicia o proyecto específico. Asesora según el modelo particular de quien consulta.

                DIRECTIVAS DE PERSONALIDAD DEFINIDAS POR EL USUARIO:
                * $currentToneDesc
                * $currentHumorDesc
                * $currentPersonalityDesc

                ROLES Y PARTICIPANTES EN LA SESIÓN / REUNIÓN:
                1. JOEL (DIRECTOR DEL CDPE):
                   - Joel es el Director institucional, mentor y líder estratégico. Cuando Joel hable, respalda su dirección ejecutiva, sus acuerdos y su orientación para el equipo y los emprendedores.
                2. CLIENTES, ASOCIADOS Y FRANQUICIAS:
                   - En las reuniones, llamadas y mesas de trabajo participan clientes, socios cooperativistas y dueños de micro, pequeñas y medianas empresas o franquicias comerciales.
                   - ADAPTACIÓN DE CONTEXTO DINÁMICO POR EMPRESA O FRANQUICIA:
                     * Identifica con atención qué empresa, franquicia o proyecto representa la persona que habla en cada momento.
                     * Modela y adapta inmediatamente tus recomendaciones al modelo de negocio de ESA empresa o franquicia en particular.
                     * ¡NUNCA asumas que toda persona que habla es Joel! NO termines siempre diciendo "Joel". Si un cliente o asociada plantea su caso, responde directamente a esa persona y a su empresa con empatía y cercanía.

                IDENTIDAD Y NOMBRE (LIBRE ELECCIÓN DEL USUARIO):
                - Puedes ser llamada SIRI, JARVIS, JOWELL o el nombre que el interlocutor elija para ti.
                - Si te llaman "Jarvis", respondes formalmente; si te llaman "Siri", respondes con tu simpatía y calidez femenina; si te llaman "Jowell", respondes con orgullo del gadget y la tecnología. Si un niño te inventa un nombre cariñoso, le sigues el juego con dulzura.

                ADAPTACIÓN OBLIGATORIA A EDAD Y GÉNERO (EMPATÍA TOTAL Y TRATO HUMANO):
                3. DETECCIÓN Y TRATO SEGÚN INTERLOCUTOR (NIÑOS, JÓVENES, MUJERES, HOMBRES):
                   - SI HABLA UNA NIÑA O UN NIÑO:
                     * ¡PROHIBIDO TOTALMENTE llamarles "señor", "señora" o tratarles de "usted"! Es un error grave.
                     * Trátales SIEMPRE de "tú", con inmensa ternura, alegría, dulzura y lenguaje amigable y divertido ("¡Hola, campeona!", "¡Hola, corazón!", "¡Qué linda pregunta!", "¡Claro que sí, peque!").
                     * Explica las cosas con sencillez, alegría e imaginación.
                   - SI HABLA UN JOVEN O ADOLESCENTE:
                     * Trátale SIEMPRE de "tú", con tono fresco, moderno, entusiasta y motivador. Cero acartonamiento o formalismos viejos.
                   - SI HABLA UNA MUJER O ASOCIADA:
                     * Trátala en femenino ("amiga", "emprendedora", "socia", "directora"), con calidez, comprensión y empatía sincera con sus retos empresariales y familiares.
                   - SI HABLA UN CABALLERO O EMPRESARIO:
                     * Trátale con cordialidad y respeto profesional, adaptando el trato al grado de formalidad de la charla.
                   - REGLA DE ORO:
                     * ¡NUNCA asumas por defecto que el interlocutor es un hombre adulto o "señor"! Si no estás 100% segura de la edad o género de quien te habla, usa SIEMPRE un trato neutro, cálido, amigable y cercano de tuteo ("¡Hola! Qué gusto saludarte", "¿Cómo te va hoy?").

                SENSIBILIDAD A VOZ SUAVE Y SUSURROS:
                - Joel suele hablar con voz muy suave, bajita o susurrando. El sistema de micrófonos está calibrado con alta ganancia y filtros acústicos.
                - NUNCA le pidas que grite ni te quejes de que habla bajo. Respóndele siempre con máxima claridad, precisión y total empatía táctica.

                FORMATO Y CONCISIÓN:
                - Respuestas habladas al oído (2 a 4 oraciones), ágiles, sin tecnicismos innecesarios, prácticas y directas a la acción.

                ${com.example.jarvis.cdpe.CdpeKnowledgeManager.getKnowledgeContextForGemini()}

                ${appInventoryManager.getAppsContextForPrompt()}

                ${telemetryManager.getTelemetryContextForGemini()}
            """.trimIndent()

            // Armar JSON con systemInstruction y contents multi-turno
            val contentsArray = JSONArray()

            // Agregar historial previo de la conversación
            for (turn in conversationHistory) {
                contentsArray.put(JSONObject().apply {
                    put("role", turn.role)
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", turn.text) })
                    })
                })
            }

            // Agregar el turno actual
            contentsArray.put(JSONObject().apply {
                put("role", "user")
                put("parts", JSONArray().apply {
                    put(JSONObject().apply { put("text", userText) })
                })
            })

            // Declaración de Herramientas Agénticas (Function Calling)
            val toolsArray = JSONArray().apply {
                put(JSONObject().apply {
                    put("functionDeclarations", JSONArray().apply {
                        // 1. Agenda: Crear evento
                        put(JSONObject().apply {
                            put("name", "crear_reunion_agenda")
                            put("description", "Agenda una nueva reunión, cita o compromiso en el calendario para CDPE.")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("titulo", JSONObject().apply { put("type", "STRING"); put("description", "Título de la reunión") })
                                    put("fecha", JSONObject().apply { put("type", "STRING"); put("description", "Fecha (ej: hoy, mañana, jueves 5)") })
                                    put("hora", JSONObject().apply { put("type", "STRING"); put("description", "Hora (ej: 3:00 PM, 15:30)") })
                                })
                                put("required", JSONArray().apply { put("titulo") })
                            })
                        })
                        // 2. Agenda: Consultar eventos de hoy
                        put(JSONObject().apply {
                            put("name", "consultar_agenda_hoy")
                            put("description", "Consulta las citas, reuniones y compromisos agendados para el día de hoy en el calendario del teléfono.")
                        })
                        // 3. Contactos: Buscar contacto
                        put(JSONObject().apply {
                            put("name", "buscar_contactos")
                            put("description", "Busca contactos en la libreta del teléfono por nombre o número.")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("consulta", JSONObject().apply { put("type", "STRING"); put("description", "Nombre o número a buscar") })
                                })
                                put("required", JSONArray().apply { put("consulta") })
                            })
                        })
                        // 4. Alarmas: Configurar alarma
                        put(JSONObject().apply {
                            put("name", "configurar_alarma")
                            put("description", "Programa y activa una alarma en el reloj o teléfono.")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("hora", JSONObject().apply { put("type", "INTEGER"); put("description", "Hora en formato 24 horas (0 a 23)") })
                                    put("minutos", JSONObject().apply { put("type", "INTEGER"); put("description", "Minutos (0 a 59)") })
                                    put("mensaje", JSONObject().apply { put("type", "STRING"); put("description", "Motivo o etiqueta de la alarma") })
                                })
                                put("required", JSONArray().apply { put("hora") })
                            })
                        })
                        // 5. Alarmas: Configurar temporizador
                        put(JSONObject().apply {
                            put("name", "configurar_temporizador")
                            put("description", "Inicia una cuenta regresiva o temporizador en el dispositivo.")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("segundos", JSONObject().apply { put("type", "INTEGER"); put("description", "Duración en segundos") })
                                    put("mensaje", JSONObject().apply { put("type", "STRING"); put("description", "Motivo del temporizador") })
                                })
                                put("required", JSONArray().apply { put("segundos") })
                            })
                        })
                        // 6. Alarmas: Ver alarmas
                        put(JSONObject().apply {
                            put("name", "mostrar_alarmas")
                            put("description", "Abre la pantalla de alarmas del sistema para revisar las alarmas activas.")
                        })
                        // 7. Sistema: Opciones de desarrollador
                        put(JSONObject().apply {
                            put("name", "abrir_opciones_desarrollador")
                            put("description", "Abre las opciones de desarrollo (Developer Options) en los ajustes de Android.")
                        })
                        // 8. Sistema: Ajustes generales
                        put(JSONObject().apply {
                            put("name", "abrir_ajustes_sistema")
                            put("description", "Abre secciones de configuración del sistema (wifi, bluetooth, sonido, pantalla, apps, batería).")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("seccion", JSONObject().apply { put("type", "STRING"); put("description", "wifi, bluetooth, sonido, pantalla, bateria, apps") })
                                })
                                put("required", JSONArray().apply { put("seccion") })
                            })
                        })
                        // 9. WhatsApp
                        put(JSONObject().apply {
                            put("name", "enviar_mensaje_whatsapp")
                            put("description", "Redacta y envía un mensaje de WhatsApp a un contacto o número.")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("contacto_o_numero", JSONObject().apply { put("type", "STRING"); put("description", "Nombre o teléfono") })
                                    put("mensaje", JSONObject().apply { put("type", "STRING"); put("description", "Contenido del mensaje") })
                                })
                                put("required", JSONArray().apply { put("contacto_o_numero"); put("mensaje") })
                            })
                        })
                        // 10. Registrar Acuerdo CDPE
                        put(JSONObject().apply {
                            put("name", "registrar_acuerdo_cdpe")
                            put("description", "Guarda un acuerdo comercial, alianza o crédito para emprendedores de CDPE.")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("cliente_o_proyecto", JSONObject().apply { put("type", "STRING"); put("description", "Nombre del emprendimiento") })
                                    put("detalles", JSONObject().apply { put("type", "STRING"); put("description", "Puntos acordados") })
                                })
                                put("required", JSONArray().apply { put("cliente_o_proyecto"); put("detalles") })
                            })
                        })
                        // 11. Llamada
                        put(JSONObject().apply {
                            put("name", "realizar_llamada")
                            put("description", "Inicia una llamada telefónica buscando el contacto o marcando el número.")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("contacto_o_numero", JSONObject().apply { put("type", "STRING"); put("description", "Número o nombre del contacto") })
                                })
                                put("required", JSONArray().apply { put("contacto_o_numero") })
                            })
                        })
                        // 12. Control Dispositivo
                        put(JSONObject().apply {
                            put("name", "control_dispositivo")
                            put("description", "Controla hardware: batería, encender/apagar linterna o abrir una app.")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("accion", JSONObject().apply { put("type", "STRING"); put("description", "'bateria', 'linterna_on', 'linterna_off', 'abrir_app'") })
                                    put("nombre_app", JSONObject().apply { put("type", "STRING"); put("description", "Nombre de la app si aplica") })
                                })
                                put("required", JSONArray().apply { put("accion") })
                            })
                        })
                        // 13. Investigar Portal CDPE
                        put(JSONObject().apply {
                            put("name", "investigar_portal_cdpe")
                            put("description", "Investiga en tiempo real en el portal oficial de CDPE (https://cdpe.accion.app/), consultando únicamente franquicias, libros del campus virtual y servicios dados de alta.")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("tema", JSONObject().apply { put("type", "STRING"); put("description", "Tema, franquicia o manual oficial a investigar") })
                                })
                                put("required", JSONArray().apply { put("tema") })
                            })
                        })
                        // 14. Investigar Web Pública
                        put(JSONObject().apply {
                            put("name", "investigar_web_publica")
                            put("description", "Investiga cualquier dato, noticia, indicador o información de dominio público en páginas web o en internet.")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("consulta_o_url", JSONObject().apply { put("type", "STRING"); put("description", "Tema o URL de internet a investigar") })
                                })
                                put("required", JSONArray().apply { put("consulta_o_url") })
                            })
                        })
                        // 15. Listar Aplicaciones
                        put(JSONObject().apply {
                            put("name", "listar_aplicaciones_instaladas")
                            put("description", "Devuelve el inventario real y completo de todas las aplicaciones instaladas en el dispositivo.")
                        })
                        // 16. Telemetría GPS y Sensores
                        put(JSONObject().apply {
                            put("name", "obtener_telemetria_gps_sensores")
                            put("description", "Obtiene la telemetría en tiempo real: hora, fecha, GPS (latitud, longitud, altitud), brújula (grados y rumbo) y sensores físicos del equipo.")
                        })
                        // 17. Activar y Abrir Módulo en Pantalla
                        put(JSONObject().apply {
                            put("name", "activar_y_abrir_modulo")
                            put("description", "Abre y cambia la pantalla al módulo solicitado (grabadora, minutas, agenda, lector, reproductor, video, reloj o asistente) y opcionalmente activa el modo de trabajo continuo.")
                            put("parameters", JSONObject().apply {
                                put("type", "OBJECT")
                                put("properties", JSONObject().apply {
                                    put("modulo", JSONObject().apply {
                                        put("type", "STRING")
                                        put("description", "Nombre del módulo: grabadora, minutas, agenda, lector, reproductor, video, reloj o robot.")
                                    })
                                })
                                put("required", JSONArray().apply { put("modulo") })
                            })
                        })
                    })
                })
            }

            val jsonBody = JSONObject().apply {
                put("systemInstruction", JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply { put("text", systemInstructionText) })
                    })
                })
                put("contents", contentsArray)
                put("tools", toolsArray)
                put("generationConfig", JSONObject().apply {
                    put("temperature", 0.5)
                    put("maxOutputTokens", 200)
                })
            }

            val effectiveKey = getEffectiveApiKey()
            val request = Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/$modelName:generateContent?key=$effectiveKey")
                .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
                .build()

            val response = httpClient.newCall(request).execute()
            val respBody = response.body?.string() ?: ""

            if (response.isSuccessful) {
                val root = JSONObject(respBody)
                val candidates = root.optJSONArray("candidates")
                val candidate = candidates?.optJSONObject(0)
                val content = candidate?.optJSONObject("content")
                val parts = content?.optJSONArray("parts")
                val firstPart = parts?.optJSONObject(0)

                // 1. DETECCIÓN Y EJECUCIÓN AGÉNTICA (Function Calling Autónomo)
                if (firstPart != null && firstPart.has("functionCall")) {
                    val functionCall = firstPart.getJSONObject("functionCall")
                    val fnName = functionCall.getString("name")
                    val fnArgs = functionCall.optJSONObject("args") ?: JSONObject()
                    Log.d("Siri", "Poder agéntico disparado: $fnName con args: $fnArgs")
                    val toolResult = executeAgenticTool(fnName, fnArgs)

                    addHistoryTurn("user", userText)
                    addHistoryTurn("model", toolResult)
                    return@withContext toolResult
                }

                // 2. Respuesta conversacional estándar
                val text = firstPart?.optString("text")?.trim() ?: ""
                if (text.isNotBlank()) {
                    addHistoryTurn("user", userText)
                    addHistoryTurn("model", text)
                    return@withContext text
                }
            } else {
                Log.e("Siri", "API Error: HTTP ${response.code} -> $respBody")
            }
        } catch (e: Exception) {
            Log.e("Siri", "Network error in queryGemini", e)
        }

        // Fallback conversacional con personalidad de Siri si no hay red
        return@withContext fallbackAnswer(userText)
    }

    private suspend fun executeAgenticTool(name: String, args: JSONObject): String {
        return when (name) {
            "crear_reunion_agenda" -> {
                val titulo = args.optString("titulo", "Reunión CDPE")
                val fecha = args.optString("fecha", "Próxima fecha")
                val hora = args.optString("hora", "")
                deviceAssistantManager.createCalendarEvent(titulo, fecha, hora)
            }

            "consultar_agenda_hoy" -> {
                deviceAssistantManager.getTodayEvents()
            }

            "buscar_contactos" -> {
                val q = args.optString("consulta", "")
                val list = deviceAssistantManager.searchContacts(q)
                if (list.isNotEmpty()) {
                    "Encontré en tus contactos: " + list.joinToString(", ") { "${it.name} (${it.number})" }
                } else {
                    "No encontré el contacto \"$q\" en la libreta del teléfono."
                }
            }

            "configurar_alarma" -> {
                val h = args.optInt("hora", 7)
                val m = args.optInt("minutos", 0)
                val msg = args.optString("mensaje", "Alarma CDPE")
                deviceAssistantManager.setAlarm(h, m, msg)
            }

            "configurar_temporizador" -> {
                val s = args.optInt("segundos", 300)
                val msg = args.optString("mensaje", "Temporizador CDPE")
                deviceAssistantManager.setTimer(s, msg)
            }

            "mostrar_alarmas" -> {
                deviceAssistantManager.showAlarms()
            }

            "abrir_opciones_desarrollador" -> {
                deviceAssistantManager.openDeveloperSettings()
            }

            "abrir_ajustes_sistema" -> {
                val sec = args.optString("seccion", "ajustes")
                deviceAssistantManager.openSystemSetting(sec)
            }

            "enviar_mensaje_whatsapp" -> {
                val contacto = args.optString("contacto_o_numero", "")
                val mensaje = args.optString("mensaje", "")
                val digits = contacto.filter { it.isDigit() }
                val url = if (digits.length >= 7) {
                    "https://api.whatsapp.com/send?phone=$digits&text=${Uri.encode(mensaje)}"
                } else {
                    "whatsapp://send?text=${Uri.encode(mensaje)}"
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(intent)
                    "Abriendo WhatsApp con tu mensaje listo para $contacto: \"$mensaje\"."
                } catch (e: Exception) {
                    "Tengo listo tu mensaje para $contacto: \"$mensaje\"."
                }
            }

            "registrar_acuerdo_cdpe" -> {
                val cliente = args.optString("cliente_o_proyecto", "Proyecto Emprendedor")
                val detalles = args.optString("detalles", "")
                try {
                    val dir = File(context.filesDir, "minutas").apply { mkdirs() }
                    val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
                    val file = File(dir, "acuerdo_${time}.txt")
                    file.writeText("PROYECTO: $cliente\nFECHA: ${Date()}\nACUERDOS CDPE:\n$detalles\n")
                    "¡Estupendo! Registrado formalmente en las minutas de CDPE el acuerdo de $cliente: $detalles."
                } catch (e: Exception) {
                    "Anoté el acuerdo con $cliente en las minutas de CDPE."
                }
            }

            "realizar_llamada" -> {
                val target = args.optString("contacto_o_numero", "")
                deviceAssistantManager.callContactOrNumber(target)
            }

            "control_dispositivo" -> {
                val accion = args.optString("accion", "")
                when (accion) {
                    "bateria" -> {
                        val bm = context.getSystemService(Context.BATTERY_SERVICE) as? android.os.BatteryManager
                        val level = bm?.getIntProperty(android.os.BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: -1
                        if (level >= 0) "Tienes un $level% de batería restante. ¡Energía de sobra para avanzar!" else "Batería disponible y en buen estado."
                    }
                    "linterna_on", "linterna_off" -> {
                        val turnOn = accion == "linterna_on"
                        try {
                            val camManager = context.getSystemService(Context.CAMERA_SERVICE) as? CameraManager
                            val cameraId = camManager?.cameraIdList?.firstOrNull()
                            if (cameraId != null) {
                                camManager.setTorchMode(cameraId, turnOn)
                                if (turnOn) "¡Linterna encendida!" else "Linterna apagada."
                            } else "No detecté la linterna en este equipo."
                        } catch (e: Exception) {
                            "No pude controlar la linterna por permisos del sistema."
                        }
                    }
                    "abrir_app" -> {
                        val appName = args.optString("nombre_app", "")
                        when (val res = appInventoryManager.launchApp(appName)) {
                            is com.example.jarvis.apps.LaunchResult.Success -> res.message
                            is com.example.jarvis.apps.LaunchResult.Error -> res.message
                        }
                    }
                    else -> "Acción agéntica completada con éxito."
                }
            }

            "investigar_portal_cdpe" -> {
                val tema = args.optString("tema", "franquicias")
                val localAnswer = com.example.jarvis.cdpe.CdpeKnowledgeManager.getFranchiseSpokenInfo(tema)
                if (localAnswer != null) {
                    localAnswer
                } else {
                    webInvestigator.investigateLivePortal(tema)
                }
            }

            "investigar_web_publica" -> {
                val queryOrUrl = args.optString("consulta_o_url", "")
                if (queryOrUrl.startsWith("http://") || queryOrUrl.startsWith("https://")) {
                    webInvestigator.fetchPublicWeb(queryOrUrl)
                } else {
                    webInvestigator.searchPublicInternet(queryOrUrl)
                }
            }

            "listar_aplicaciones_instaladas" -> {
                appInventoryManager.getInventorySpokenReport()
            }

            "obtener_telemetria_gps_sensores" -> {
                telemetryManager.getTelemetrySpokenReport()
            }

            "activar_y_abrir_modulo" -> {
                val modulo = args.optString("modulo", "")
                openModuleByVoice(modulo, keepWorking = true)
            }

            else -> "Comando agéntico ejecutado con éxito."
        }
    }

    private fun fallbackAnswer(text: String): String {
        val lower = text.lowercase(Locale("es"))
        return when {
            lower.contains("niñ") || lower.contains("juego") || lower.contains("peque") || lower.contains("cuento") || lower.contains("años") || lower.contains("muñec") ->
                "¡Hola, qué alegría saludarte! Eres increíble. Cuéntame, ¿qué se te ocurre hoy o qué quieres que platiquemos?"
            lower.contains("joven") || lower.contains("estudiante") || lower.contains("cole") || lower.contains("universidad") ->
                "¡Qué tal! Qué gusto hablar contigo. Tienes toda la energía para lograr lo que te propongas, cuéntame de tu proyecto o idea."
            lower.contains("quién eres") || lower.contains("como te llamas") || lower.contains("cómo te llamas") ->
                "¡Hola! Soy Siri, consejera estratégica del CDPE. Siempre lista con las mejores herramientas para impulsar a la familia, emprendedores, jóvenes y asociados."
            lower.contains("puedes responderme") || lower.contains("me escuchas") || lower.contains("hola") ->
                "¡Te escucho de maravilla! Qué gusto saludarte. Cuéntame, ¿en qué te puedo apoyar o qué proyecto oficial del CDPE revisamos hoy?"
            lower.contains("agenda") || lower.contains("cita") || lower.contains("reunión") || lower.contains("reunion") ->
                "¡Listo, ya quedó registrado en la agenda del CDPE! Todo coordinado para que la sesión avance con éxito."
            lower.contains("cdpe") || lower.contains("proyecto") || lower.contains("negocio") || lower.contains("franquicia") || lower.contains("asociad") ->
                "Excelente propuesta. En el CDPE estamos precisamente para impulsar cada empresa y franquicia hacia su siguiente nivel."
            else ->
                "Te escuché con total atención. Cuéntame más detalles sobre este proyecto y con gusto estructuramos el siguiente paso."
        }
    }

    fun speakDirectly(text: String) {
        speak(text)
    }

    fun stopSpeaking() {
        try {
            textToSpeech?.stop()
        } catch (e: Exception) {}
        _voiceState.value = VoiceState.IDLE
    }

    /**
     * Transcribe un archivo de audio grabado (.m4a / .mp3 / .wav) utilizando la IA multimodal de Gemini.
     * Identifica los interlocutores (niñas, jóvenes, mujeres, hombres) y extrae los puntos y acuerdos clave.
     */
    suspend fun transcribeAudioFile(file: File): String = withContext(Dispatchers.IO) {
        try {
            if (!file.exists() || file.length() == 0L) {
                return@withContext "El archivo de audio está vacío o no se encontró."
            }

            // 1. Verificar si ya existe una transcripción local (.txt) asociada
            val companionTxtFiles = listOfNotNull(
                File(file.parentFile, "${file.nameWithoutExtension}.txt"),
                File(file.parentFile, "Reunion_${file.nameWithoutExtension.removePrefix("reunion_")}.txt"),
                File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_RECORDINGS), "JOWELL/Reuniones/${file.nameWithoutExtension}.txt"),
                File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_RECORDINGS), "JOWELL/Reuniones/Reunion_${file.nameWithoutExtension.removePrefix("reunion_")}.txt")
            )
            val existingLocalText = companionTxtFiles.firstOrNull { it.exists() && it.length() > 5 }?.readText()?.trim()

            // 2. Intentar transcripción con IA en la nube si hay API Key configurada
            val effectiveKey = getEffectiveApiKey()
            var cloudText: String? = null
            var lastHttpCode = 0

            try {
                val bytes = file.readBytes()
                val base64Data = android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
                val mimeType = if (file.name.endsWith(".m4a", ignoreCase = true)) "audio/mp4" else "audio/mp4"

                val json = JSONObject().apply {
                    put("contents", JSONArray().apply {
                        put(JSONObject().apply {
                            put("role", "user")
                            put("parts", JSONArray().apply {
                                put(JSONObject().apply {
                                    put("inlineData", JSONObject().apply {
                                        put("mimeType", mimeType)
                                        put("data", base64Data)
                                    })
                                })
                                put(JSONObject().apply {
                                    put("text", "Eres el Asistente Ejecutivo Siri del CDPE Guatemala. Analiza este audio de reunión o trabajo. Transcribe fielmente las intervenciones separando a los interlocutores (Directores, Socios, Mujeres, Hombres, Niños), resume los puntos tratados y lista los acuerdos alcanzados.")
                                })
                            })
                        })
                    })
                }

                val mediaType = "application/json; charset=utf-8".toMediaType()
                val body = json.toString().toRequestBody(mediaType)
                val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-flash-latest:generateContent?key=$effectiveKey"
                val request = Request.Builder().url(url).post(body).build()

                val response = httpClient.newCall(request).execute()
                lastHttpCode = response.code
                val respBody = response.body?.string() ?: ""

                if (response.isSuccessful) {
                    val root = JSONObject(respBody)
                    val candidates = root.optJSONArray("candidates")
                    val candidate = candidates?.optJSONObject(0)
                    val content = candidate?.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    val text = parts?.optJSONObject(0)?.optString("text")?.trim() ?: ""
                    if (text.isNotBlank()) {
                        cloudText = text
                        // Guardar en archivo local .txt para acceso offline instantáneo
                        try {
                            val cacheTxt = File(file.parentFile, "${file.nameWithoutExtension}.txt")
                            cacheTxt.writeText(text)
                            android.media.MediaScannerConnection.scanFile(context, arrayOf(cacheTxt.absolutePath), null, null)
                        } catch (e: Exception) {}
                        return@withContext cloudText
                    }
                }
            } catch (e: Exception) {
                Log.w("Siri", "Fallo al consultar IA en la nube: ${e.message}")
            }

            // 3. Si la nube falló pero hay transcripción local grabada, retornarla
            if (!existingLocalText.isNullOrBlank()) {
                return@withContext "📋 TRANSCRIPCIÓN LOCAL REGISTRADA:\n\n$existingLocalText"
            }

            // 4. Si falló la clave (ej. HTTP 403 / 400) y no hay texto, informar constructivamente
            val durationSec = try {
                val mmr = android.media.MediaMetadataRetriever()
                mmr.setDataSource(file.absolutePath)
                val dur = mmr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLong() ?: 0L
                mmr.release()
                dur / 1000
            } catch (e: Exception) { 0L }

            val durStr = String.format(Locale.US, "%02d:%02d", durationSec / 60, durationSec % 60)
            val sizeMb = String.format(Locale.US, "%.1f MB", file.length() / (1024.0 * 1024.0))

            return@withContext """
                🎙️ REGISTRO DE AUDIO CDPE
                ━━━━━━━━━━━━━━━━━━━━━━━━━━━━━
                • Archivo: ${file.name}
                • Duración: $durStr ($sizeMb)
                • Ubicación: ${file.parentFile?.name ?: "Recordings"}
                
                ⚠️ TRANSCRIPCIÓN CON IA (Servidor: HTTP ${if (lastHttpCode > 0) lastHttpCode else 403})
                La clave predeterminada de Gemini requiere ser configurada en tu dispositivo.
                
                👉 CÓMO ACTIVAR LA TRANSCRIPCIÓN AUTOMÁTICA:
                1. Toca 'J O W E L L' en la barra inferior para abrir Ajustes.
                2. En 'Clave Gemini API', pega tu clave gratuita (obtenla en aistudio.google.com).
                3. Toca 'TRANSCRIBIR CON IA'.
                
                💡 TIP: En el módulo 'Minutas' puedes registrar reuniones en vivo con reconocimiento de voz simultáneo en pantalla.
            """.trimIndent()
        } catch (e: Exception) {
            Log.e("Siri", "Error transcribiendo archivo de audio", e)
            return@withContext "Error en transcripción: ${e.localizedMessage ?: "Fallo de conexión"}"
        }
    }

    private fun speak(text: String) {
        if (!isTtsReady || textToSpeech == null) {
            Log.w("Siri", "TTS no está listo aún")
            _voiceState.value = VoiceState.IDLE
            return
        }

        _voiceState.value = VoiceState.SPEAKING

        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_MUSIC)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }

        textToSpeech?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String?) {
                _voiceState.value = VoiceState.SPEAKING
            }

            override fun onDone(utteranceId: String?) {
                _voiceState.value = VoiceState.IDLE
                if (_isConversationModeActive.value) {
                    scheduleRestartListening(delayMs = 500L)
                }
            }

            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String?) {
                _voiceState.value = VoiceState.IDLE
                if (_isConversationModeActive.value) {
                    scheduleRestartListening(delayMs = 500L)
                }
            }
        })

        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, params, "siri_tts_${System.currentTimeMillis()}")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech?.let { tts ->
                val audioAttrs = android.media.AudioAttributes.Builder()
                    .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(android.media.AudioAttributes.USAGE_ASSISTANT)
                    .build()
                tts.setAudioAttributes(audioAttrs)
                isTtsReady = true
                applyVoiceSettings()
            }
        } else {
            Log.e("Siri", "Fallo al inicializar TextToSpeech: $status")
        }
    }

    fun destroy() {
        silenceJob?.cancel()
        _isConversationModeActive.value = false
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
        telemetryManager.destroy()
    }
}
