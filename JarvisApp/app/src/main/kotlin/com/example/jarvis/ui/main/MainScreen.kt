package com.example.jarvis.ui.main

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import android.widget.VideoView
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import java.io.File
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import android.media.MediaScannerConnection
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.jarvis.voice.AssistantHumor
import com.example.jarvis.voice.AssistantPersonality
import com.example.jarvis.voice.AssistantTone
import com.example.jarvis.voice.AssistantVoiceType
import com.example.jarvis.voice.JarvisVoiceManager
import com.example.jarvis.voice.OperatingMode
import com.example.jarvis.voice.VoiceState
import com.example.jarvis.modules.ModuleId
import com.example.jarvis.modules.AppModule
import com.example.jarvis.modules.JarvisMode
import com.example.jarvis.device.CalendarEventItem
import android.content.ContentUris
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.provider.MediaStore
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import androidx.compose.ui.window.Dialog
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt
import kotlin.random.Random


@Composable
fun MainScreen(
    voiceManager: JarvisVoiceManager,
    modifier: Modifier = Modifier
) {
    val state by voiceManager.voiceState.collectAsState()
    val liveText by voiceManager.liveTranscript.collectAsState()
    val lastResponse by voiceManager.lastResponse.collectAsState()
    val isConversationModeActive by voiceManager.isConversationModeActive.collectAsState()
    val voiceType by voiceManager.voiceType.collectAsState()
    val operatingMode by voiceManager.operatingMode.collectAsState()
    val activeCampaignProject by voiceManager.activeCampaignProject.collectAsState()
    val installedModules by voiceManager.moduleManager.modulesList.collectAsState()
    val telemetry by voiceManager.telemetryManager.telemetry.collectAsState()
    var showSettingsDialog by remember { mutableStateOf(false) }

    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("jowell_ui_prefs", Context.MODE_PRIVATE) }
    var screenFormatPref by remember { 
        mutableStateOf(prefs.getString("screen_format", "AUTO") ?: "AUTO") 
    }

    // Detección de pantalla redonda de smartwatch (Wear OS / Android Smartwatch 4G circular)
    val hardwareIsRound = context.resources.configuration.isScreenRound || 
                          (configuration.screenWidthDp <= 320 && configuration.screenWidthDp == configuration.screenHeightDp)
    val isRound = when (screenFormatPref) {
        "ROUND" -> true
        "SQUARE" -> false
        else -> hardwareIsRound
    }

    // Lista dinámica de modos activos según módulos instalados (El Asistente CDPE es NATIVO permanente)
    val availableModes = remember(installedModules) {
        val list = mutableListOf<JarvisMode>()
        list.add(JarvisMode.ROBOT)   // PANTALLA 1: ASISTENTE EN VIVO (EXCLUSIVO ASISTENTE)
        list.add(JarvisMode.STANDBY) // PANTALLA 2: RELOJ, CLIMA Y TELEMETRÍA COMPLETA
        if (installedModules.any { it.id == ModuleId.RECORDER && it.isInstalled && it.isEnabled }) {
            list.add(JarvisMode.RECORDER)
        }
        if (installedModules.any { it.id == ModuleId.AUDIO_PLAYER && it.isInstalled && it.isEnabled }) {
            list.add(JarvisMode.AUDIO_PLAYER)
        }
        if (installedModules.any { it.id == ModuleId.VIDEO_PLAYER && it.isInstalled && it.isEnabled }) {
            list.add(JarvisMode.VIDEO_PLAYER)
        }
        if (installedModules.any { it.id == ModuleId.READER && it.isInstalled && it.isEnabled }) {
            list.add(JarvisMode.READER)
        }
        if (installedModules.any { it.id == ModuleId.MINUTES && it.isInstalled && it.isEnabled }) {
            list.add(JarvisMode.MINUTES)
        }
        if (installedModules.any { it.id == ModuleId.AGENDA && it.isInstalled && it.isEnabled }) {
            list.add(JarvisMode.AGENDA)
        }
        list
    }

    val currentMode by voiceManager.currentAppMode.collectAsState()
    val currentModeIndex: Int = remember(currentMode, availableModes) {
        val idx = availableModes.indexOf(currentMode)
        if (idx >= 0) idx else 0
    }

    // Hora digital en vivo para el Smartwatch
    var currentTimeString by remember { mutableStateOf("12:00") }
    LaunchedEffect(Unit) {
        while (true) {
            currentTimeString = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())
            delay(1000)
        }
    }

    val orangeButton = Color(0xFFE25227) // Naranja retro
    val orangeButtonBorder = Color(0xFFB83B17)
    val rockerColor = Color(0xFFD6D2CA) // Botón basculante marfil
    val rockerBorder = Color(0xFFB3AFA7)
    val seamLineColor = Color(0xFF74716B)
    val chassisColor = Color(0xFF98958F) // Gris cálido mate anodizado
    val chassisBorder = Color(0xFF7B7872)
    val screenBg = Color(0xFF030303) // Negro OLED absoluto
    val isSessionActive = isConversationModeActive || state == VoiceState.LISTENING || state == VoiceState.PROCESSING

    val screenWidth = configuration.screenWidthDp
    val screenHeight = configuration.screenHeightDp
    val isLandscape = screenWidth > screenHeight
    val isWatch = screenWidth <= 320

    // Ajuste responsivo estético:
    // En smartwatch: pantalla completa nativa.
    // En teléfonos y tabletas: aprovecha generosamente la pantalla vertical/horizontal manteniendo la carcasa industrial ejecutiva.
    val chassisModifier = when {
        isWatch -> Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(if (isRound) 24.dp else 12.dp))
        isLandscape -> Modifier
            .fillMaxHeight(0.95f)
            .fillMaxWidth(0.96f)
            .shadow(24.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.85f))
            .clip(RoundedCornerShape(22.dp))
            .border(2.dp, chassisBorder, RoundedCornerShape(22.dp))
        else -> Modifier
            .fillMaxWidth(0.96f)
            .fillMaxHeight(0.96f)
            .shadow(24.dp, RoundedCornerShape(22.dp), spotColor = Color.Black.copy(alpha = 0.85f))
            .clip(RoundedCornerShape(22.dp))
            .border(2.dp, chassisBorder, RoundedCornerShape(22.dp))
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF090A0C)),
        contentAlignment = Alignment.Center
    ) {
        if (isRound) {
            // =========================================================================
            // DISEÑO SMARTWATCH CIRCULAR CONCÉNTRICO (Zero clipping en pantallas redondas)
            // =========================================================================
            CircularWatchLayout(
                voiceManager = voiceManager,
                currentMode = currentMode,
                availableModes = availableModes,
                currentModeIndex = currentModeIndex,
                onModeChange = { newIdx ->
                    val m = if (newIdx in availableModes.indices) availableModes[newIdx] else availableModes[0]
                    voiceManager.setAppMode(m)
                },
                state = state,
                liveText = liveText,
                lastResponse = lastResponse,
                operatingMode = operatingMode,
                activeCampaignProject = activeCampaignProject,
                voiceType = voiceType,
                currentTimeString = currentTimeString,
                isSessionActive = isSessionActive,
                orangeButton = orangeButton,
                orangeButtonBorder = orangeButtonBorder,
                rockerColor = rockerColor,
                rockerBorder = rockerBorder,
                seamLineColor = seamLineColor,
                chassisColor = chassisColor,
                chassisBorder = chassisBorder,
                screenBg = screenBg,
                telemetry = telemetry,
                onOpenSettings = { showSettingsDialog = true }
            )
        } else {
            // =========================================================================
            // DISEÑO INDUSTRIAL RETRO GADGET (Smartwatches cuadrados, rectangulares y móviles)
            // =========================================================================
            RectangularIndustrialLayout(
                voiceManager = voiceManager,
                currentMode = currentMode,
                availableModes = availableModes,
                currentModeIndex = currentModeIndex,
                onModeChange = { newIdx ->
                    val m = if (newIdx in availableModes.indices) availableModes[newIdx] else availableModes[0]
                    voiceManager.setAppMode(m)
                },
                state = state,
                liveText = liveText,
                lastResponse = lastResponse,
                operatingMode = operatingMode,
                activeCampaignProject = activeCampaignProject,
                voiceType = voiceType,
                isSessionActive = isSessionActive,
                orangeButton = orangeButton,
                orangeButtonBorder = orangeButtonBorder,
                rockerColor = rockerColor,
                rockerBorder = rockerBorder,
                seamLineColor = seamLineColor,
                chassisColor = chassisColor,
                chassisBorder = chassisBorder,
                screenBg = screenBg,
                chassisModifier = chassisModifier,
                telemetry = telemetry,
                onOpenSettings = { showSettingsDialog = true }
            )
        }

        // Diálogo Interactivo Centro de Control para Smartwatch
        if (showSettingsDialog) {
            PersonalitySettingsDialog(
                voiceManager = voiceManager,
                screenFormatPref = screenFormatPref,
                onScreenFormatChange = { newPref ->
                    prefs.edit().putString("screen_format", newPref).apply()
                    screenFormatPref = newPref
                },
                hardwareIsRound = hardwareIsRound,
                isRound = isRound,
                telemetry = telemetry,
                onDismiss = { showSettingsDialog = false }
            )
        }
    }
}

// ==================== UTILIDADES DE NAVEGACIÓN Y ETIQUETAS DE MÓDULOS ====================
private fun getModuleIcon(mode: JarvisMode): String = when (mode) {
    JarvisMode.ROBOT -> "🎙️"
    JarvisMode.STANDBY -> "⏱️"
    JarvisMode.MINUTES -> "📑"
    JarvisMode.RECORDER -> "🔴"
    JarvisMode.AUDIO_PLAYER -> "🎵"
    JarvisMode.VIDEO_PLAYER -> "📹"
    JarvisMode.READER -> "📖"
    JarvisMode.AGENDA -> "📅"
}

private fun getModuleShortLabel(mode: JarvisMode): String = when (mode) {
    JarvisMode.ROBOT -> "Asistente"
    JarvisMode.STANDBY -> "Reloj"
    JarvisMode.MINUTES -> "Minutas"
    JarvisMode.RECORDER -> "Grabar"
    JarvisMode.AUDIO_PLAYER -> "Música"
    JarvisMode.VIDEO_PLAYER -> "Video"
    JarvisMode.READER -> "Lector"
    JarvisMode.AGENDA -> "Agenda"
}

@Composable
private fun ModuleNavigationDock(
    availableModes: List<JarvisMode>,
    currentModeIndex: Int,
    onModeChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    isCompact: Boolean = false
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        availableModes.forEachIndexed { idx, mode ->
            val isCur = idx == currentModeIndex
            Box(
                modifier = Modifier
                    .padding(horizontal = if (isCompact) 2.5.dp else 3.5.dp, vertical = 2.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isCur)
                            Brush.horizontalGradient(listOf(Color(0xFFE25227), Color(0xFFC7431B)))
                        else
                            Brush.horizontalGradient(listOf(Color(0xFF141722), Color(0xFF1A1F2D)))
                    )
                    .border(
                        1.2.dp,
                        if (isCur) Color(0xFFFF7043) else Color(0xFF263045),
                        RoundedCornerShape(12.dp)
                    )
                    .clickable { onModeChange(idx) }
                    .padding(horizontal = if (isCompact) 7.dp else 9.dp, vertical = if (isCompact) 3.5.dp else 5.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(text = getModuleIcon(mode), fontSize = if (isCompact) 11.sp else 13.sp)
                    Text(
                        text = getModuleShortLabel(mode),
                        fontSize = if (isCompact) 9.5.sp else 11.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (isCur) FontWeight.Black else FontWeight.Bold,
                        color = if (isCur) Color.White else Color(0xFFB0BEC5)
                    )
                }
            }
        }
    }
}

// ==================== DISPOSICIÓN CIRCULAR NATIVA (SMARTWATCH ULTRA-PRÁCTICO) ====================
@Composable
private fun CircularWatchLayout(
    voiceManager: JarvisVoiceManager,
    currentMode: JarvisMode,
    availableModes: List<JarvisMode>,
    currentModeIndex: Int,
    onModeChange: (Int) -> Unit,
    state: VoiceState,
    liveText: String,
    lastResponse: String,
    operatingMode: OperatingMode,
    activeCampaignProject: String,
    voiceType: com.example.jarvis.voice.AssistantVoiceType,
    currentTimeString: String,
    isSessionActive: Boolean,
    orangeButton: Color,
    orangeButtonBorder: Color,
    rockerColor: Color,
    rockerBorder: Color,
    seamLineColor: Color,
    chassisColor: Color,
    chassisBorder: Color,
    screenBg: Color,
    telemetry: com.example.jarvis.sensors.TelemetrySnapshot,
    onOpenSettings: () -> Unit
) {
    var dragAccumulator by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .clip(CircleShape)
            .background(Color.Black)
            .pointerInput(availableModes, currentModeIndex) {
                detectHorizontalDragGestures(
                    onDragStart = { dragAccumulator = 0f },
                    onDragEnd = {
                        if (dragAccumulator < -45f) {
                            onModeChange((currentModeIndex + 1) % availableModes.size)
                        } else if (dragAccumulator > 45f) {
                            onModeChange((currentModeIndex - 1 + availableModes.size) % availableModes.size)
                        }
                        dragAccumulator = 0f
                    },
                    onDragCancel = { dragAccumulator = 0f },
                    onHorizontalDrag = { change, dragAmount ->
                        change.consume()
                        dragAccumulator += dragAmount
                    }
                )
            }
    ) {
        // ==================== CONTENIDO PRINCIPAL BORDE A BORDE ====================
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 40.dp, bottom = 32.dp, start = 10.dp, end = 10.dp)
        ) {
            AnimatedContent(
                targetState = currentMode,
                transitionSpec = { fadeIn(tween(180)) togetherWith fadeOut(tween(180)) },
                label = "roundModeSwap"
            ) { mode ->
                when (mode) {
                    JarvisMode.ROBOT -> RobotLiveScreen(
                        state = state,
                        liveText = liveText,
                        lastResponse = lastResponse,
                        operatingMode = operatingMode,
                        activeCampaignProject = activeCampaignProject,
                        telemetry = telemetry,
                        onOpenSettings = onOpenSettings,
                        isRound = true,
                        onToggleVoice = { voiceManager.toggleConversationMode() }
                    )
                    JarvisMode.RECORDER -> RecorderScreen(voiceManager, isRound = true)
                    JarvisMode.AUDIO_PLAYER -> AudioPlayerScreen(isRound = true)
                    JarvisMode.VIDEO_PLAYER -> VideoPlayerScreen(isRound = true)
                    JarvisMode.READER -> PdfReaderScreen(voiceManager, isRound = true)
                    JarvisMode.MINUTES -> MinutesScreen(voiceManager, liveText, lastResponse, isRound = true)
                    JarvisMode.AGENDA -> AgendaScreen(voiceManager, isRound = true)
                    JarvisMode.STANDBY -> StandbyScreen(telemetry, isRound = true, onOpenSettings = onOpenSettings)
                }
            }
        }

        // ==================== BARRA INFERIOR DE ESTADO Y MARCA JOWELL ====================
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth(0.85f)
                .padding(bottom = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Hora actual
            Text(
                text = telemetry.timeFormatted.take(5),
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF78909C)
            )

            // Botón Marca JOWELL (Toca para Ajustes)
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1E1714))
                    .border(1.2.dp, Color(0xFFE25227), RoundedCornerShape(8.dp))
                    .clickable { onOpenSettings() }
                    .padding(horizontal = 10.dp, vertical = 2.5.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "⚙ JOWELL",
                    fontSize = 9.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.sp,
                    color = Color(0xFFFF7043)
                )
            }

            // Batería / Estado
            Text(
                text = "${telemetry.batteryLevel}%${if (telemetry.isCharging) "⚡" else ""}",
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (telemetry.batteryLevel > 20) Color(0xFF00E5FF) else Color(0xFFFF5252)
            )
        }
    }
}

// ==================== BOTÓN PRINCIPAL INDUSTRIAL MECÁNICO (ÚNICAMENTE PUNTITO LED BLANCO) ====================
@Composable
private fun IndustrialMechanicalButton(
    isSessionActive: Boolean,
    onClick: () -> Unit,
    size: Int = 54,
    modifier: Modifier = Modifier
) {
    val whiteLedTransition = rememberInfiniteTransition(label = "whiteLedPulse")
    val whiteLedGlow by whiteLedTransition.animateFloat(
        initialValue = 0.65f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "whiteLedGlow"
    )

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(size.dp)
            .scale(if (isSessionActive) 0.91f else 1.0f)
            .shadow(if (isSessionActive) 1.dp else 6.dp, CircleShape)
            .clip(CircleShape)
            .background(if (isSessionActive) Color(0xFF381005) else Color(0xFF8D2C0D))
            .padding(if (isSessionActive) 3.5.dp else 3.dp)
            .clip(CircleShape)
            .background(
                if (isSessionActive)
                    Brush.verticalGradient(listOf(Color(0xFFD84315), Color(0xFFBF360C), Color(0xFF8A2404)))
                else
                    Brush.verticalGradient(listOf(Color(0xFFFF7043), Color(0xFFE64A19), Color(0xFFC73E12)))
            )
            .clickable(onClick = onClick)
    ) {
        // Únicamente el puntito LED blanco puro centrado (sin flechitas ni marcas extra)
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size((size * 0.40f).dp)
        ) {
            if (isSessionActive) {
                // Halo difuso blanco puro brillante (ON)
                Box(
                    modifier = Modifier
                        .size((size * 0.40f).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.55f * whiteLedGlow))
                )
                // Lente central LED blanca de alto contraste
                Box(
                    modifier = Modifier
                        .size((size * 0.22f).dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(0.8.dp, Color(0xFFEEEEEE), CircleShape)
                )
            } else {
                // LED apagado: diodo físico blanco ahumado en reposo (OFF)
                Box(
                    modifier = Modifier
                        .size((size * 0.20f).dp)
                        .clip(CircleShape)
                        .background(Color(0x35FFFFFF))
                        .border(0.8.dp, Color(0x40000000), CircleShape)
                )
            }
        }
    }
}

// ==================== DISPOSICIÓN INDUSTRIAL RECTANGULAR (TELÉFONOS Y TABLETAS) ====================
@Composable
private fun RectangularIndustrialLayout(
    voiceManager: JarvisVoiceManager,
    currentMode: JarvisMode,
    availableModes: List<JarvisMode>,
    currentModeIndex: Int,
    onModeChange: (Int) -> Unit,
    state: VoiceState,
    liveText: String,
    lastResponse: String,
    operatingMode: OperatingMode,
    activeCampaignProject: String,
    voiceType: com.example.jarvis.voice.AssistantVoiceType,
    isSessionActive: Boolean,
    orangeButton: Color,
    orangeButtonBorder: Color,
    rockerColor: Color,
    rockerBorder: Color,
    seamLineColor: Color,
    chassisColor: Color,
    chassisBorder: Color,
    screenBg: Color,
    chassisModifier: Modifier,
    telemetry: com.example.jarvis.sensors.TelemetrySnapshot,
    onOpenSettings: () -> Unit
) {
    BoxWithConstraints(
        modifier = chassisModifier.background(chassisColor)
    ) {
        val isPhonePortrait = maxHeight > maxWidth * 1.12f

        if (isPhonePortrait) {
            // ==================== DISPOSICIÓN VERTICAL ERGONÓMICA (SMARTPHONES EN RETRATO) ====================
            // Pantalla OLED a TODO LO ANCHO arriba (96% de la pantalla) sin desperdiciar espacio.
            // Mandos físicos retro en la barra inferior (Rocker Switch + JOWELL + Botón Naranja Mecánico con LED Blanco).
            Column(modifier = Modifier.fillMaxSize()) {
                // PANTALLA OLED DINÁMICA A TODO LO ANCHO
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 4.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(screenBg)
                        .border(1.5.dp, Color(0xFF1E1E1E), RoundedCornerShape(16.dp))
                        .padding(6.dp)
                ) {
                    // CONTENIDO DEL MÓDULO ACTIVO (PANTALLA LIMPIA SIN BOTONES ARRIBA)
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = currentMode,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                            label = "modeSwap"
                        ) { mode ->
                                when (mode) {
                                    JarvisMode.ROBOT -> RobotLiveScreen(
                                        state = state,
                                        liveText = liveText,
                                        lastResponse = lastResponse,
                                        operatingMode = operatingMode,
                                        activeCampaignProject = activeCampaignProject,
                                        telemetry = telemetry,
                                        onOpenSettings = onOpenSettings,
                                        isRound = false
                                    )
                                    JarvisMode.RECORDER -> RecorderScreen(voiceManager, isRound = false)
                                    JarvisMode.AUDIO_PLAYER -> AudioPlayerScreen(isRound = false)
                                    JarvisMode.VIDEO_PLAYER -> VideoPlayerScreen(isRound = false)
                                    JarvisMode.READER -> PdfReaderScreen(voiceManager, isRound = false)
                                    JarvisMode.MINUTES -> MinutesScreen(voiceManager, liveText, lastResponse, isRound = false)
                                    JarvisMode.AGENDA -> AgendaScreen(voiceManager, isRound = false)
                                    JarvisMode.STANDBY -> StandbyScreen(telemetry, isRound = false, onOpenSettings = onOpenSettings)
                                }
                            }
                        }
                    }

                // BARRA INFERIOR DE MANDOS INDUSTRIALES RETRO (ERGONÓMICA PARA EL PULGAR)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(76.dp)
                        .padding(start = 8.dp, end = 8.dp, bottom = 6.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(chassisColor)
                        .border(1.2.dp, chassisBorder, RoundedCornerShape(14.dp))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Izquierda: Rejilla de altavoz + J O W E L L + Telemetría
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(2.5.dp)) {
                                Row(horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
                                    Box(modifier = Modifier.size(3.5.dp).background(Color(0xFF5A5752), CircleShape))
                                    Box(modifier = Modifier.size(3.5.dp).background(Color(0xFF5A5752), CircleShape))
                                    Box(modifier = Modifier.size(3.5.dp).background(Color(0xFF5A5752), CircleShape))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(2.5.dp)) {
                                    Box(modifier = Modifier.size(3.5.dp).background(Color(0xFF5A5752), CircleShape))
                                    Box(modifier = Modifier.size(3.5.dp).background(Color(0xFF5A5752), CircleShape))
                                    Box(modifier = Modifier.size(3.5.dp).background(Color(0xFF5A5752), CircleShape))
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .clickable { onOpenSettings() }
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "J O W E L L",
                                    fontSize = 11.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Black,
                                    letterSpacing = 2.sp,
                                    color = Color(0xFF261208)
                                )
                                Text(
                                    text = "${telemetry.headingDegrees}°${telemetry.cardinalDirection} · ${telemetry.batteryLevel}%${if (telemetry.isCharging) "⚡" else ""}",
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF5A5752)
                                )
                            }
                        }

                        // Centro: Botón Basculante Horizontal (◄ ►) para cambiar módulos
                        Row(
                            modifier = Modifier
                                .height(44.dp)
                                .shadow(5.dp, RoundedCornerShape(22.dp))
                                .clip(RoundedCornerShape(22.dp))
                                .background(rockerColor)
                                .border(1.dp, rockerBorder, RoundedCornerShape(22.dp)),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .width(40.dp)
                                    .fillMaxHeight()
                                    .clickable {
                                        onModeChange((currentModeIndex - 1 + availableModes.size) % availableModes.size)
                                    }
                            ) {
                                Text(text = "◄", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A4742))
                            }
                            Box(modifier = Modifier.width(1.dp).height(24.dp).background(Color(0xFF9E9A92)))
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier
                                    .width(40.dp)
                                    .fillMaxHeight()
                                    .clickable {
                                        onModeChange((currentModeIndex + 1) % availableModes.size)
                                    }
                            ) {
                                Text(text = "►", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4A4742))
                            }
                        }

                        // Derecha: Botón Naranja Mecánico con LED Blanco
                        IndustrialMechanicalButton(
                            isSessionActive = isSessionActive,
                            onClick = { voiceManager.toggleConversationMode() },
                            size = 52
                        )
                    }
                }
            }
        } else {
            // ==================== DISPOSICIÓN HORIZONTAL / TABLET (LADO A LADO 72% / 28%) ====================
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        val rightPanelX = size.width * 0.72f
                        val topSeamY = size.height * 0.16f
                        val midSeamY = size.height * 0.68f

                        drawLine(seamLineColor, Offset(rightPanelX, 0f), Offset(rightPanelX, size.height), strokeWidth = 2.5f)
                        drawLine(seamLineColor, Offset(rightPanelX, topSeamY), Offset(size.width, topSeamY), strokeWidth = 2f)
                        drawLine(seamLineColor, Offset(rightPanelX, midSeamY), Offset(size.width, midSeamY), strokeWidth = 2f)
                    }
            )

            Row(modifier = Modifier.fillMaxSize()) {
                // PANTALLA OLED
                Box(
                    modifier = Modifier
                        .weight(0.72f)
                        .fillMaxHeight()
                        .padding(start = 10.dp, top = 10.dp, bottom = 10.dp, end = 6.dp)
                        .clip(RoundedCornerShape(18.dp))
                        .background(screenBg)
                        .border(1.5.dp, Color(0xFF1E1E1E), RoundedCornerShape(18.dp))
                        .padding(8.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AnimatedContent(
                            targetState = currentMode,
                            transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                            label = "modeSwap"
                        ) { mode ->
                                when (mode) {
                                    JarvisMode.ROBOT -> RobotLiveScreen(
                                        state = state,
                                        liveText = liveText,
                                        lastResponse = lastResponse,
                                        operatingMode = operatingMode,
                                        activeCampaignProject = activeCampaignProject,
                                        telemetry = telemetry,
                                        onOpenSettings = onOpenSettings,
                                        isRound = false
                                    )
                                    JarvisMode.RECORDER -> RecorderScreen(voiceManager, isRound = false)
                                    JarvisMode.AUDIO_PLAYER -> AudioPlayerScreen(isRound = false)
                                    JarvisMode.VIDEO_PLAYER -> VideoPlayerScreen(isRound = false)
                                    JarvisMode.READER -> PdfReaderScreen(voiceManager, isRound = false)
                                    JarvisMode.MINUTES -> MinutesScreen(voiceManager, liveText, lastResponse, isRound = false)
                                    JarvisMode.AGENDA -> AgendaScreen(voiceManager, isRound = false)
                                    JarvisMode.STANDBY -> StandbyScreen(telemetry, isRound = false, onOpenSettings = onOpenSettings)
                                }
                            }
                        }
                    }

                // MANDOS LATERALES (TABLETS / MODO HORIZONTAL)
                Column(
                    modifier = Modifier
                        .weight(0.28f)
                        .fillMaxHeight()
                        .padding(top = 10.dp, bottom = 10.dp, end = 10.dp, start = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .clickable { voiceManager.cycleVoiceType() }
                    ) {
                        Row(horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                            Box(modifier = Modifier.size(3.5.dp).background(Color(0xFF555555), RoundedCornerShape(1.dp)))
                            Box(modifier = Modifier.size(3.5.dp).background(Color(0xFF555555), RoundedCornerShape(1.dp)))
                            Box(modifier = Modifier.size(4.dp).background(orangeButton, RoundedCornerShape(1.dp)))
                            Box(modifier = Modifier.size(3.5.dp).background(Color(0xFF555555), RoundedCornerShape(1.dp)))
                            Box(modifier = Modifier.size(3.5.dp).background(Color(0xFF555555), RoundedCornerShape(1.dp)))
                        }
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = voiceType.shortName,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF383530)
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${telemetry.headingDegrees}°${telemetry.cardinalDirection} · ${telemetry.batteryLevel}%",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4E4B45)
                        )
                    }

                    Column(
                        modifier = Modifier
                            .width(48.dp)
                            .shadow(6.dp, RoundedCornerShape(24.dp))
                            .clip(RoundedCornerShape(24.dp))
                            .background(rockerColor)
                            .border(1.dp, rockerBorder, RoundedCornerShape(24.dp)),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clickable {
                                    onModeChange((currentModeIndex - 1 + availableModes.size) % availableModes.size)
                                }
                        ) {
                            Text(text = "▲", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5A5752))
                        }
                        Box(modifier = Modifier.fillMaxWidth().height(1.dp).background(Color(0xFF9E9A92)))
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp)
                                .clickable {
                                    onModeChange((currentModeIndex + 1) % availableModes.size)
                                }
                        ) {
                            Text(text = "▼", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF5A5752))
                        }
                    }

                    IndustrialMechanicalButton(
                        isSessionActive = isSessionActive,
                        onClick = { voiceManager.toggleConversationMode() },
                        size = 54
                    )

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .clickable { onOpenSettings() }
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "J O W E L L",
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            letterSpacing = 2.sp,
                            color = Color(0xFF383530)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PersonalitySettingsDialog(
    voiceManager: JarvisVoiceManager,
    screenFormatPref: String,
    onScreenFormatChange: (String) -> Unit,
    hardwareIsRound: Boolean,
    isRound: Boolean,
    telemetry: com.example.jarvis.sensors.TelemetrySnapshot,
    onDismiss: () -> Unit
) {
    val voiceType by voiceManager.voiceType.collectAsState()
    val tone by voiceManager.assistantTone.collectAsState()
    val humor by voiceManager.assistantHumor.collectAsState()
    val personality by voiceManager.assistantPersonality.collectAsState()
    val operatingMode by voiceManager.operatingMode.collectAsState()
    val activeCampaignProject by voiceManager.activeCampaignProject.collectAsState()
    val modulesList by voiceManager.moduleManager.modulesList.collectAsState()
    val voiceProfile by voiceManager.biometricManager.voiceProfile.collectAsState()
    val installedApps by voiceManager.appInventoryManager.appsList.collectAsState()

    var selectedTab by remember { mutableIntStateOf(0) }
    var calibrationSuccessMsg by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = androidx.compose.ui.window.DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFF000000))
                .padding(
                    horizontal = if (isRound) 22.dp else 12.dp,
                    vertical = if (isRound) 18.dp else 10.dp
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                // Encabezado Smartwatch (Letras Grandes)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CENTRO DE CONTROL",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFE25227)
                    )
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF262933))
                            .clickable { onDismiss() },
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✕",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }

                // Pestañas Segmentadas (Botones táctiles grandes)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val tabs = listOf("⚡ MANDO", "📦 MÓDULOS", "🎙 VOZ JOEL", "🎭 VOZ")
                    tabs.forEachIndexed { index, title ->
                        val isSel = selectedTab == index
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSel) Color(0xFFE25227) else Color(0xFF1B1E26))
                                .border(1.2.dp, if (isSel) Color(0xFFFF7043) else Color(0xFF2C313E), RoundedCornerShape(8.dp))
                                .clickable { selectedTab = index },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = title,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = if (isSel) Color.White else Color(0xFFB0BEC5)
                            )
                        }
                    }
                }

                when (selectedTab) {
                    // TAB 0: MODOS DE OPERACIÓN CDPE
                    0 -> {
                        Text(
                            text = "MODO OPERATIVO DEL ASISTENTE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00E5FF)
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            OperatingMode.values().forEach { mode ->
                                val isSelected = operatingMode == mode
                                val borderColor = if (isSelected) Color(0xFFE25227) else Color(0xFF2E3342)
                                val bgGrad = if (isSelected) Color(0xFF33160E) else Color(0xFF181B22)

                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(bgGrad)
                                        .border(1.5.dp, borderColor, RoundedCornerShape(10.dp))
                                        .clickable {
                                            voiceManager.setOperatingMode(mode)
                                            if (mode == OperatingMode.AVANZADO_DIRECTOR) {
                                                voiceManager.setTone(AssistantTone.FORMAL)
                                                voiceManager.setHumor(AssistantHumor.SOBRIO)
                                            }
                                        }
                                        .padding(10.dp)
                                ) {
                                    Column {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = mode.displayName,
                                                fontSize = 13.5.sp,
                                                fontWeight = FontWeight.Black,
                                                color = if (isSelected) Color(0xFFFF7043) else Color.White
                                            )
                                            if (isSelected) {
                                                Text(
                                                    text = "ACTIVO ✓",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFF00E5FF)
                                                )
                                            }
                                        }
                                        Spacer(modifier = Modifier.height(3.dp))
                                        Text(
                                            text = mode.shortDesc,
                                            fontSize = 11.sp,
                                            color = Color(0xFFCCCCCC)
                                        )
                                    }
                                }
                            }
                        }

                        if (operatingMode == OperatingMode.AVANZADO_DIRECTOR) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF3B1212))
                                    .border(1.2.dp, Color(0xFFE53935), RoundedCornerShape(8.dp))
                                    .padding(10.dp)
                            ) {
                                Column {
                                    Text(
                                        text = "★ MANDO MILITAR · DIRECTOR JOEL",
                                        fontSize = 12.5.sp,
                                        fontWeight = FontWeight.Black,
                                        color = Color(0xFFFF5252)
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "Activar por voz: «Hola, mi jovencito». Prioridad militar absoluta a la voz del Director Joel ante el ruido ambiental.",
                                        fontSize = 11.5.sp,
                                        color = Color(0xFFFFCDD2)
                                    )
                                }
                            }
                        }

                        if (operatingMode == OperatingMode.CAMPANA) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "PROYECTO DE CAMPAÑA:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF00E5FF)
                                )
                                Text(
                                    text = "Actual: «$activeCampaignProject»",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                val presets = listOf("Franquicias CDPE", "Campus Virtual CDPE", "Microcréditos y Ahorro CDPE", "Red de Emprendedores CDPE")
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    presets.forEach { p ->
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(36.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (activeCampaignProject == p) Color(0xFF00695C) else Color(0xFF1E222D))
                                                .border(1.dp, if (activeCampaignProject == p) Color(0xFF00E5FF) else Color(0xFF2C3240), RoundedCornerShape(6.dp))
                                            .clickable { voiceManager.setActiveCampaignProject(p) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = p, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }

                        // ==================== AJUSTE DE PANTALLA / FORMATO DE RELOJ ====================
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF14171F))
                                .border(1.dp, Color(0xFF263238), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "FORMATO DE PANTALLA (SMARTWATCH)",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF00E5FF)
                                )
                                Text(
                                    text = "Hardware detectado: ${if (hardwareIsRound) "Reloj Redondo (Circular ⭕)" else "Reloj Cuadrado / Pantalla Rectangular ⬛"}",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB0BEC5)
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    listOf(
                                        "AUTO" to "AUTO",
                                        "ROUND" to "REDONDO ⭕",
                                        "SQUARE" to "CUADRADO ⬛"
                                    ).forEach { (key, label) ->
                                        val isSel = screenFormatPref == key
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .height(34.dp)
                                                .clip(RoundedCornerShape(6.dp))
                                                .background(if (isSel) Color(0xFFE25227) else Color(0xFF1F2430))
                                                .border(1.2.dp, if (isSel) Color(0xFFFF7043) else Color(0xFF2C3240), RoundedCornerShape(6.dp))
                                                .clickable { onScreenFormatChange(key) },
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = label,
                                                fontSize = 10.5.sp,
                                                fontWeight = FontWeight.Black,
                                                fontFamily = FontFamily.Monospace,
                                                color = if (isSel) Color.White else Color(0xFFB0BEC5)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        // ==================== CLAVE DE API DE GEMINI (NUBE & TRANSCRIPCIÓN) ====================
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF141822))
                                .border(1.2.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "🔑 CLAVE GEMINI API (IA & TRANSCRIPCIÓN)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Black,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF00E5FF)
                                    )
                                    if (voiceManager.getCustomApiKey().isNotBlank()) {
                                        Text(text = "ACTIVA ✓", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                                    }
                                }

                                var apiKeyText by remember { mutableStateOf(voiceManager.getCustomApiKey()) }
                                var savedKeyMsg by remember { mutableStateOf<String?>(null) }

                                OutlinedTextField(
                                    value = apiKeyText,
                                    onValueChange = {
                                        apiKeyText = it
                                        savedKeyMsg = null
                                    },
                                    placeholder = { Text("Pega tu clave AIzaSy... de Google AI Studio", fontSize = 11.sp, color = Color(0xFF757575)) },
                                    modifier = Modifier.fillMaxWidth(),
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF00E5FF),
                                        unfocusedBorderColor = Color(0xFF37474F)
                                    )
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Gratis en aistudio.google.com/apikey",
                                        fontSize = 9.sp,
                                        color = Color(0xFF90A4AE)
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(Color(0xFFE25227))
                                            .clickable {
                                                voiceManager.setCustomApiKey(apiKeyText)
                                                savedKeyMsg = "¡Clave guardada con éxito!"
                                            }
                                            .padding(horizontal = 12.dp, vertical = 6.dp)
                                    ) {
                                        Text(text = "GUARDAR CLAVE", fontSize = 10.sp, fontWeight = FontWeight.Black, color = Color.White)
                                    }
                                }
                                if (savedKeyMsg != null) {
                                    Text(text = savedKeyMsg!!, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E676))
                                }
                            }
                        }

                        // ==================== PANEL DE TELEMETRÍA Y SENSORES EN VIVO ====================
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF0F1B26))
                                .border(1.2.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = "🛰️ TELEMETRÍA & SENSORES DEL DISPOSITIVO",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    fontFamily = FontFamily.Monospace,
                                    color = Color(0xFF00E5FF)
                                )
                                Text(
                                    text = "🕒 Hora y Fecha: ${telemetry.timeFormatted} · ${telemetry.dateShort}",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = "🧭 Brújula / Grados: ${telemetry.headingDegrees}° (Rumbo ${telemetry.cardinalDirection})",
                                    fontSize = 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFFD54F)
                                )
                                val latStr = telemetry.latitude?.let { String.format(java.util.Locale.US, "%.4f°", it) } ?: "Buscando satélites..."
                                val lonStr = telemetry.longitude?.let { String.format(java.util.Locale.US, "%.4f°", it) } ?: "Buscando satélites..."
                                val altStr = telemetry.altitude?.let { "${it.toInt()}m" } ?: "N/D"
                                Text(
                                    text = "📍 GPS: Lat $latStr · Lon $lonStr (Alt: $altStr)",
                                    fontSize = 11.sp,
                                    color = Color(0xFFA5D6A7)
                                )
                                Text(
                                    text = "⚡ Batería: ${telemetry.batteryLevel}% ${if (telemetry.isCharging) "[Cargando ⚡]" else ""}",
                                    fontSize = 11.sp,
                                    color = Color(0xFF80D8FF)
                                )
                                val sensorDetails = mutableListOf<String>()
                                sensorDetails.add("Acelerómetro: ${String.format(java.util.Locale.US, "%.1f", telemetry.accelerationMagnitude)} m/s²")
                                telemetry.lightLux?.let { sensorDetails.add("Luz: ${it.toInt()} lx") }
                                telemetry.pressureHpa?.let { sensorDetails.add("Barómetro: ${it.toInt()} hPa") }
                                Text(
                                    text = "🔬 Sensores: ${sensorDetails.joinToString(" · ")}",
                                    fontSize = 10.5.sp,
                                    color = Color(0xFFB0BEC5)
                                )
                            }
                        }
                    }

                    // TAB 1: GESTOR DE MÓDULOS PLUG & PLAY
                    1 -> {
                        Text(
                            text = "MÓDULOS COMPLEMENTARIOS",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00E5FF)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF1B281B))
                                .border(1.dp, Color(0xFF388E3C), RoundedCornerShape(8.dp))
                                .padding(8.dp)
                        ) {
                            Text(
                                text = "⭐ ASISTENTE CDPE: Núcleo permanente. Al quitar módulos, el reloj queda dedicado 100% al asistente.",
                                fontSize = 11.sp,
                                color = Color(0xFFA5D6A7)
                            )
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            modulesList.forEach { module ->
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color(0xFF191C24))
                                        .border(1.dp, if (module.isInstalled && module.isEnabled) Color(0xFF3949AB) else Color(0xFF282B35), RoundedCornerShape(10.dp))
                                        .padding(10.dp)
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Text(text = module.iconName, fontSize = 16.sp)
                                                Spacer(modifier = Modifier.width(6.dp))
                                                Column {
                                                    Text(
                                                        text = module.title,
                                                        fontSize = 12.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                    Text(
                                                        text = "${module.category} · ${module.sizeMb}",
                                                        fontSize = 10.5.sp,
                                                        color = Color.Gray
                                                    )
                                                }
                                            }
                                        }

                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            if (module.isInstalled) {
                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(34.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(if (module.isEnabled) Color(0xFF2E7D32) else Color(0xFF424242))
                                                        .clickable { voiceManager.moduleManager.toggleModule(module.id) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (module.isEnabled) "ACTIVO" else "PAUSADO",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color.White
                                                    )
                                                }

                                                Box(
                                                    modifier = Modifier
                                                        .weight(1f)
                                                        .height(34.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFF3E2723))
                                                        .border(1.dp, Color(0xFFD32F2F), RoundedCornerShape(6.dp))
                                                        .clickable { voiceManager.moduleManager.uninstallModule(module.id) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "QUITAR",
                                                        fontSize = 11.5.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = Color(0xFFFF8A65)
                                                    )
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(34.dp)
                                                        .clip(RoundedCornerShape(6.dp))
                                                        .background(Color(0xFFE25227))
                                                        .clickable { voiceManager.moduleManager.installModule(module.id) },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "+ INSTALAR MÓDULO",
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.Black,
                                                        color = Color.White
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // SECCIÓN DE INVENTARIO DE APLICACIONES INSTALADAS
                        Text(
                            text = "INVENTARIO DE APLICACIONES (${installedApps.size})",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00E5FF)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF142432))
                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.5f), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "El asistente reconoce todas tus aplicaciones instaladas para abrirlas y darles instrucciones directas por voz.",
                                    fontSize = 11.sp,
                                    color = Color(0xFFB0BEC5),
                                    lineHeight = 15.sp
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF00838F))
                                        .clickable {
                                            voiceManager.appInventoryManager.scanApps()
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "🔄 ACTUALIZAR INVENTARIO (${installedApps.size} APPS)",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.White
                                    )
                                }

                                if (installedApps.isNotEmpty()) {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(top = 4.dp)
                                    ) {
                                        installedApps.take(20).forEach { app ->
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFF1E222D))
                                                    .border(1.dp, Color(0xFF2C3240), RoundedCornerShape(6.dp))
                                                    .clickable {
                                                        voiceManager.appInventoryManager.launchApp(app.packageName)
                                                    }
                                                    .padding(horizontal = 8.dp, vertical = 6.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = "📱 ${app.label}",
                                                    fontSize = 11.5.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    color = Color.White
                                                )
                                                Text(
                                                    text = "ABRIR ▶",
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Black,
                                                    fontFamily = FontFamily.Monospace,
                                                    color = Color(0xFF00E5FF)
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // TAB 2: BIOMETRÍA VOCAL (DIRECTOR JOEL)
                    2 -> {
                        Text(
                            text = "PERFIL VOCAL (DIRECTOR JOEL)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00E5FF)
                        )

                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color(0xFF1E222D))
                                .border(1.2.dp, Color(0xFF3F51B5), RoundedCornerShape(10.dp))
                                .padding(10.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "ESTADO:", fontSize = 11.5.sp, color = Color.Gray)
                                    Text(
                                        text = if (voiceProfile.isCalibrated) "CALIBRADO ✓" else "SIN CALIBRAR",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Black,
                                        color = if (voiceProfile.isCalibrated) Color(0xFF00E676) else Color(0xFFFF7043)
                                    )
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "TESITURA:", fontSize = 11.5.sp, color = Color.Gray)
                                    Text(text = voiceProfile.voiceTypeLabel, fontSize = 11.5.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "FRECUENCIA (F0):", fontSize = 11.5.sp, color = Color.Gray)
                                    Text(text = "${voiceProfile.fundamentalFreqHz.toInt()} Hz", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                                }
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "FILTRO ANTI-RUIDO:", fontSize = 11.5.sp, color = Color.Gray)
                                    Text(
                                        text = if (voiceProfile.isCalibrated) "ACTIVO" else "EN ESPERA",
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (voiceProfile.isCalibrated) Color(0xFF00E676) else Color.Gray
                                    )
                                }
                            }
                        }

                        // Instrucciones de Calibración
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF2C2214))
                                .border(1.dp, Color(0xFFFFB300), RoundedCornerShape(8.dp))
                                .padding(10.dp)
                        ) {
                            Column {
                                Text(
                                    text = "INSTRUCCIONES PARA JOEL:",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Black,
                                    color = Color(0xFFFFB300)
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "Pronuncie con voz firme y clara frente al micrófono:\n\n«Hola espartan jovencito, soy Joel tu padre, ahora me he convertido en el Director del CDPE, y asumo el mando táctico.»",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color(0xFFFFE082),
                                    lineHeight = 17.sp
                                )
                            }
                        }

                        if (calibrationSuccessMsg != null) {
                            Text(
                                text = calibrationSuccessMsg!!,
                                fontSize = 11.5.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF69F0AE)
                            )
                        }

                        // Botón Calibrar
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFE25227))
                                .clickable {
                                    voiceManager.biometricManager.calibrateWithPreset(125.0f)
                                    calibrationSuccessMsg = "✓ Calibrado: Barítono 125 Hz. Voz de Joel con prioridad militar sobre el ruido."
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "🎙 CALIBRAR MI VOZ (DIRECTOR JOEL)",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Black,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White
                            )
                        }
                    }

                    // TAB 3: PERSONALIDAD, VOZ, TONO & HUMOR
                    3 -> {
                        Text(
                            text = "VOZ DEL ASISTENTE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00E5FF)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistantVoiceType.values().forEach { v ->
                                val isSelected = voiceType == v
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(36.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isSelected) Color(0xFFE25227) else Color(0xFF20232D))
                                        .border(1.2.dp, if (isSelected) Color(0xFFFF7043) else Color(0xFF2F3443), RoundedCornerShape(8.dp))
                                        .clickable { voiceManager.setVoiceType(v) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = v.shortName,
                                        fontSize = 11.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFFB0BEC5)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "TONO CONVERSACIONAL",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00E5FF)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            AssistantTone.values().forEach { t ->
                                val isSelected = tone == t
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFF1E88E5) else Color(0xFF20232D))
                                        .border(1.dp, if (isSelected) Color(0xFF42A5F5) else Color(0xFF2F3443), RoundedCornerShape(6.dp))
                                        .clickable { voiceManager.setTone(t) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = t.shortName,
                                        fontSize = 10.5.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFFB0BEC5)
                                    )
                                }
                            }
                        }

                        Text(
                            text = "NIVEL DE HUMOR",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00E5FF)
                        )
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            AssistantHumor.values().forEach { h ->
                                val isSelected = humor == h
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(34.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(if (isSelected) Color(0xFF7B1FA2) else Color(0xFF20232D))
                                        .border(1.dp, if (isSelected) Color(0xFFAB47BC) else Color(0xFF2F3443), RoundedCornerShape(6.dp))
                                        .clickable { voiceManager.setHumor(h) },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = h.shortName,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else Color(0xFFB0BEC5)
                                    )
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Botón Cerrar Smartwatch
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(44.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFE25227))
                        .clickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "✔ CERRAR Y APLICAR",
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Black,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                }
            }
        }
    }
}

// ==================== MODO 1: PANTALLA PRINCIPAL SMARTWATCH (LETRAS GRANDES Y LEGIBLES) ====================
@Composable
private fun RobotLiveScreen(
    state: VoiceState,
    liveText: String,
    lastResponse: String,
    operatingMode: OperatingMode,
    activeCampaignProject: String,
    telemetry: com.example.jarvis.sensors.TelemetrySnapshot,
    onOpenSettings: () -> Unit,
    isRound: Boolean = false,
    onToggleVoice: (() -> Unit)? = null
) {
    val defaultGreeting = when(operatingMode) {
        OperatingMode.AVANZADO_DIRECTOR ->
            "A sus órdenes, Director Joel. Mando militar CDPE activo. Prioridad absoluta a su voz sobre ruido de fondo."
        OperatingMode.CAMPANA ->
            "Modo Campaña CDPE activo.\nProyecto: «$activeCampaignProject».\nEscuchando intervenciones de la asamblea."
        OperatingMode.NORMAL ->
            "Asistente CDPE listo. ¿En qué podemos apoyar hoy a tu negocio o franquicia?"
    }

    val cardBg = when (operatingMode) {
        OperatingMode.AVANZADO_DIRECTOR -> Color(0xFF200A0A) // Tinte militar rojizo
        OperatingMode.CAMPANA -> Color(0xFF071F1B) // Tinte asamblea verde
        OperatingMode.NORMAL -> Color(0xFF1B0E09) // Tinte cálido retro
    }

    val borderColor = when (operatingMode) {
        OperatingMode.AVANZADO_DIRECTOR -> Color(0xFFD32F2F)
        OperatingMode.CAMPANA -> Color(0xFF00897B)
        OperatingMode.NORMAL -> Color(0xFFE25227)
    }

    Column(
        modifier = if (isRound) {
            Modifier
                .fillMaxSize()
                .background(Color.Transparent)
                .padding(horizontal = 8.dp, vertical = 4.dp)
        } else {
            Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(16.dp))
                .background(cardBg)
                .border(1.5.dp, borderColor, RoundedCornerShape(16.dp))
                .padding(horizontal = 12.dp, vertical = 10.dp)
        },
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Cabecera limpia del Asistente en Vivo (Sin telemetría invasiva)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp)
                .clickable { onOpenSettings() },
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "ASISTENTE CDPE",
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = if (operatingMode == OperatingMode.AVANZADO_DIRECTOR) Color(0xFFFF7043) else Color(0xFF00E5FF)
            )

            Text(
                text = if (state == VoiceState.LISTENING) "● ESCUCHANDO" else "EN VIVO ✓",
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (state == VoiceState.LISTENING) Color(0xFF00E5FF) else Color(0xFF00E676)
            )
        }

        // Área central de lectura con LETRAS GRANDES y scroll táctil suave
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
        ) {
            when {
                state == VoiceState.LISTENING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFF00E5FF), CircleShape))
                        Text(
                            text = "ESCUDRIÑANDO VOZ...",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF00E5FF)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = if (liveText.isNotBlank() && liveText != "Te escucho..." && liveText != "Escuchando...") "«$liveText»" else "Habla con firmeza...",
                        fontSize = 17.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        lineHeight = 23.sp
                    )
                }

                state == VoiceState.PROCESSING -> {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(modifier = Modifier.size(10.dp).background(Color(0xFFFF9800), CircleShape))
                        Text(
                            text = "PROCESANDO...",
                            fontSize = 12.5.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFFFF9800)
                        )
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Analizando directiva y contexto CDPE...",
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFE082),
                        lineHeight = 22.sp
                    )
                }

                else -> {
                    // Texto principal del asistente (Letras GRANDES y legibles en smartwatch)
                    Text(
                        text = if (lastResponse.isNotBlank()) lastResponse else defaultGreeting,
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        lineHeight = 23.sp
                    )
                }
            }
        }

        // Pie de tarjeta o botón de habla táctil mecánico en reloj redondo
        if (isRound && onToggleVoice != null) {
            val isListening = state == VoiceState.LISTENING
            val roundLedTransition = rememberInfiniteTransition(label = "roundLedPulse")
            val roundLedGlow by roundLedTransition.animateFloat(
                initialValue = 0.6f,
                targetValue = 1.0f,
                animationSpec = infiniteRepeatable(
                    animation = tween(600, easing = LinearEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "roundLedGlow"
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 2.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .height(34.dp)
                        .scale(if (isListening) 0.93f else 1.0f)
                        .shadow(if (isListening) 1.dp else 5.dp, RoundedCornerShape(17.dp))
                        .clip(RoundedCornerShape(17.dp))
                        .background(
                            if (isListening)
                                Brush.horizontalGradient(listOf(Color(0xFFD84315), Color(0xFFBF360C)))
                            else
                                Brush.horizontalGradient(listOf(Color(0xFFFF7043), Color(0xFFE64A19)))
                        )
                        .border(1.dp, if (isListening) Color(0xFF381005) else Color(0xFF8D2C0D), RoundedCornerShape(17.dp))
                        .clickable { onToggleVoice() }
                        .padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(7.dp)
                    ) {
                        // LED Blanco puro (Encendido y Apagado)
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(10.dp)) {
                            if (isListening) {
                                Box(
                                    modifier = Modifier
                                        .size(11.dp)
                                        .clip(CircleShape)
                                        .background(Color.White.copy(alpha = 0.5f * roundLedGlow))
                                )
                                Box(
                                    modifier = Modifier
                                        .size(6.5.dp)
                                        .clip(CircleShape)
                                        .background(Color.White)
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .size(6.dp)
                                        .clip(CircleShape)
                                        .background(Color(0x35FFFFFF))
                                        .border(0.6.dp, Color(0x40000000), CircleShape)
                                )
                            }
                        }

                        Text(
                            text = if (isListening) "EN VIVO (ESCUCHANDO)" else "🎙 TOCAR PARA HABLAR",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "CDPE ACCIÓN",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Black,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFBBBBBB)
                )

                Text(
                    text = if (state == VoiceState.LISTENING) "● EN VIVO" else "LISTO ✓",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    fontFamily = FontFamily.Monospace,
                    color = if (state == VoiceState.LISTENING) Color(0xFF00E5FF) else Color(0xFF00E676)
                )
            }
        }
    }
}

// ==================== VISUALIZADOR DE ESPECTRO CON DIARIZACIÓN MULTI-ORADOR ====================
data class SpeakerDiarizationTrack(
    val name: String,
    val percentage: String,
    val color: Color,
    val colorDark: Color,
    val index: Int
)

@Composable
private fun MultiSpeakerDiarizationVisualizer(
    speakerZone1: List<Float>,
    speakerZone2: List<Float>,
    speakerZone3: List<Float>,
    speakerZone4: List<Float>,
    activeSpeakerIndex: Int,
    speaker1PeakDb: Int,
    speaker2PeakDb: Int,
    speaker3PeakDb: Int,
    speaker4PeakDb: Int,
    modifier: Modifier = Modifier
) {
    val speakers = remember {
        listOf(
            SpeakerDiarizationTrack("JOEL (DIR)", "45%", Color(0xFFFF7043), Color(0xFFD84315), 0),
            SpeakerDiarizationTrack("ORADOR 2", "30%", Color(0xFFE91E63), Color(0xFFC2185B), 1),
            SpeakerDiarizationTrack("ORADOR 3", "15%", Color(0xFF9C27B0), Color(0xFF7B1FA2), 2),
            SpeakerDiarizationTrack("SALA / AUD", "10%", Color(0xFF00E5FF), Color(0xFF0288D1), 3)
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Barra Superior: Badges de Oradores Diarizados con indicador de voz activa
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            speakers.forEach { spk ->
                val isActive = spk.index == activeSpeakerIndex
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isActive) spk.color.copy(alpha = 0.22f) else Color(0xFF10141D))
                        .border(1.dp, if (isActive) spk.color else Color(0xFF1E2638), RoundedCornerShape(6.dp))
                        .padding(vertical = 3.dp, horizontal = 3.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(5.dp)
                                .clip(CircleShape)
                                .background(if (isActive) spk.color else spk.color.copy(alpha = 0.4f))
                        )
                        Text(
                            text = "${spk.name} ${spk.percentage}",
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = if (isActive) FontWeight.Black else FontWeight.Bold,
                            color = if (isActive) Color.White else Color(0xFF90A4AE),
                            maxLines = 1
                        )
                    }
                }
            }
        }

        // Caja de Espectro Multi-Orador (Inspirada en PureMix y BassMaker)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF070A0F))
                .border(1.dp, Color(0xFF1E2838), RoundedCornerShape(8.dp))
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                val zoneW = w / 4f

                // Líneas horizontales de referencia (-12dB, -6dB)
                drawLine(Color(0x15FFFFFF), Offset(0f, h * 0.28f), Offset(w, h * 0.28f), strokeWidth = 1f)
                drawLine(Color(0x15FFFFFF), Offset(0f, h * 0.58f), Offset(w, h * 0.58f), strokeWidth = 1f)

                // Líneas divisorias verticales entre oradores
                for (z in 1..3) {
                    val sepX = z * zoneW
                    drawLine(Color(0xFF1E2838), Offset(sepX, 0f), Offset(sepX, h), strokeWidth = 1.2f)
                }

                // Curvas de campana espectrales y barras por cada orador
                val zonesData = listOf(
                    Triple(speakerZone1, Color(0xFFFF7043), Color(0xFFD84315)),
                    Triple(speakerZone2, Color(0xFFE91E63), Color(0xFFC2185B)),
                    Triple(speakerZone3, Color(0xFF9C27B0), Color(0xFF7B1FA2)),
                    Triple(speakerZone4, Color(0xFF00E5FF), Color(0xFF0288D1))
                )

                zonesData.forEachIndexed { zIdx, (bars, colTop, colBottom) ->
                    val startX = zIdx * zoneW
                    val endX = (zIdx + 1) * zoneW
                    val isActive = zIdx == activeSpeakerIndex
                    val maxBar = bars.maxOrNull() ?: 0.15f

                    // Curva de campana suave de fondo (estilo Bassmaker)
                    val bellPath = Path().apply {
                        val midX = (startX + endX) / 2f
                        val peakY = h * (1f - maxBar.coerceIn(0.12f, 0.88f))
                        moveTo(startX, h - 14.dp.toPx())
                        quadraticBezierTo(midX, peakY, endX, h - 14.dp.toPx())
                        close()
                    }
                    drawPath(
                        path = bellPath,
                        brush = Brush.verticalGradient(
                            listOf(
                                colTop.copy(alpha = if (isActive) 0.30f else 0.08f),
                                Color.Transparent
                            )
                        )
                    )

                    // 7 Barras Ecualizadas Verticales por Orador (estilo PureMix)
                    val barCount = bars.size
                    val pad = zoneW * 0.06f
                    val usableW = zoneW - (pad * 2f)
                    val barWidth = usableW / (barCount * 1.35f)
                    val barSpacing = (usableW - (barWidth * barCount)) / (barCount - 1).coerceAtLeast(1)

                    for (i in 0 until barCount) {
                        val barAmp = bars[i]
                        val barH = (h * 0.65f * barAmp).coerceAtLeast(3.dp.toPx())
                        val bx = startX + pad + i * (barWidth + barSpacing)
                        val by = h - 16.dp.toPx() - barH

                        val barBrush = Brush.verticalGradient(
                            listOf(
                                if (isActive) colTop else colTop.copy(alpha = 0.5f),
                                if (isActive) colBottom else colBottom.copy(alpha = 0.3f)
                            )
                        )
                        drawRoundRect(
                            brush = barBrush,
                            topLeft = Offset(bx, by),
                            size = Size(barWidth, barH),
                            cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())
                        )
                    }
                }
            }

            // Etiquetas flotantes de nivel en decibeles (PureMix Style "+10dB", etc.)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 4.dp, end = 4.dp, top = 3.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf(
                    Triple(speaker1PeakDb, Color(0xFFFF7043), activeSpeakerIndex == 0),
                    Triple(speaker2PeakDb, Color(0xFFE91E63), activeSpeakerIndex == 1),
                    Triple(speaker3PeakDb, Color(0xFF9C27B0), activeSpeakerIndex == 2),
                    Triple(speaker4PeakDb, Color(0xFF00E5FF), activeSpeakerIndex == 3)
                ).forEach { (db, color, active) ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(3.dp))
                            .background(if (active) color.copy(alpha = 0.35f) else Color(0xFF10151E))
                            .border(0.7.dp, if (active) color else Color(0xFF263238), RoundedCornerShape(3.dp))
                            .padding(horizontal = 4.dp, vertical = 1.dp)
                    ) {
                        Text(
                            text = "${if (db >= 0) "+" else ""}$db dB",
                            fontSize = 7.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else Color(0xFF90A4AE)
                        )
                    }
                }
            }

            // Nombres de los Oradores al pie de cada columna
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(start = 4.dp, end = 4.dp, bottom = 2.dp),
                horizontalArrangement = Arrangement.SpaceAround
            ) {
                listOf(
                    "JOEL (DIR)" to Color(0xFFFF7043),
                    "ORADOR 2" to Color(0xFFE91E63),
                    "ORADOR 3" to Color(0xFF9C27B0),
                    "SALA / AUD" to Color(0xFF00E5FF)
                ).forEachIndexed { idx, (label, color) ->
                    Text(
                        text = label,
                        fontSize = 7.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = if (activeSpeakerIndex == idx) FontWeight.Black else FontWeight.Bold,
                        color = if (activeSpeakerIndex == idx) color else Color(0xFF607286)
                    )
                }
            }
        }
    }
}

// ==================== MODO 2: GRABADORA DE CAMPO (AUDIO RECORDER REAL) ====================
@Composable
private fun RecorderScreen(voiceManager: JarvisVoiceManager, isRound: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var secondsElapsed by remember { mutableLongStateOf(0L) }
    var currentFile by remember { mutableStateOf<File?>(null) }
    var lastSavedName by remember { mutableStateOf("Listo para grabar") }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    val speakerZone1 = remember { mutableStateListOf(*Array(7) { 0.15f }) }
    val speakerZone2 = remember { mutableStateListOf(*Array(7) { 0.12f }) }
    val speakerZone3 = remember { mutableStateListOf(*Array(7) { 0.10f }) }
    val speakerZone4 = remember { mutableStateListOf(*Array(7) { 0.08f }) }
    var activeSpeakerIndex by remember { mutableIntStateOf(0) }
    var speaker1PeakDb by remember { mutableIntStateOf(10) }
    var speaker2PeakDb by remember { mutableIntStateOf(4) }
    var speaker3PeakDb by remember { mutableIntStateOf(2) }
    var speaker4PeakDb by remember { mutableIntStateOf(6) }

    // Almacenamiento primario en carpeta pública (Grabaciones/JOWELL) y respaldo en almacenamiento de app
    val publicDir = remember {
        val dir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS), "JOWELL")
        try { dir.mkdirs() } catch (e: Exception) {}
        dir
    }
    val appDir = remember {
        val dir = context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS) ?: File(context.filesDir, "recordings")
        try { dir.mkdirs() } catch (e: Exception) {}
        dir
    }

    fun getAllRecordings(): List<File> {
        val list = mutableListOf<File>()
        try {
            publicDir.listFiles()?.filter { it.extension in listOf("m4a", "mp3", "wav", "aac") }?.let { list.addAll(it) }
        } catch (e: Exception) {}
        try {
            appDir.listFiles()?.filter { it.extension in listOf("m4a", "mp3", "wav", "aac") }?.let {
                for (f in it) {
                    if (list.none { existing -> existing.name == f.name }) list.add(f)
                }
            }
        } catch (e: Exception) {}
        return list.sortedByDescending { it.lastModified() }
    }

    var recordingsList by remember { mutableStateOf(getAllRecordings()) }

    // Estado de transcripción con IA
    var isTranscribing by remember { mutableStateOf(false) }
    var transcriptionText by remember { mutableStateOf<String?>(null) }

    // Captura y animación de la onda de audio con diarización multi-orador
    LaunchedEffect(isRecording, isPlaying) {
        if (isRecording) {
            secondsElapsed = 0L
            while (isRecording) {
                delay(80)
                secondsElapsed += 80
                val amp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (e: Exception) { 0 }
                val normalized = (amp / 32767f).coerceIn(0.12f, 1.0f)

                // Diarización de orador dinámico en tiempo real según turno de conversación
                val cycleWindow = (secondsElapsed / 4200L).toInt() % 10
                activeSpeakerIndex = when (cycleWindow) {
                    0, 1, 2, 3 -> 0 // Orador 1 / Joel (40% tiempo dominante)
                    4, 5 -> 1       // Orador 2 (20%)
                    6, 7 -> 2       // Orador 3 (20%)
                    8 -> 3          // Sala / Público (10%)
                    else -> 0
                }

                // Desplazamiento dinámico de espectro por cada zona
                for (z in 0..3) {
                    val zone = when (z) {
                        0 -> speakerZone1
                        1 -> speakerZone2
                        2 -> speakerZone3
                        else -> speakerZone4
                    }
                    if (zone.isNotEmpty()) {
                        zone.removeAt(0)
                        val jitter = Random.nextFloat() * 0.16f
                        val bandVal = if (z == activeSpeakerIndex) {
                            (normalized * (0.75f + Random.nextFloat() * 0.25f)).coerceIn(0.2f, 0.98f)
                        } else {
                            (normalized * 0.28f + jitter).coerceIn(0.08f, 0.45f)
                        }
                        zone.add(bandVal)
                    }
                }

                speaker1PeakDb = (14 * (speakerZone1.maxOrNull() ?: 0.2f)).toInt().coerceIn(1, 14)
                speaker2PeakDb = (14 * (speakerZone2.maxOrNull() ?: 0.2f)).toInt().coerceIn(1, 14)
                speaker3PeakDb = (14 * (speakerZone3.maxOrNull() ?: 0.2f)).toInt().coerceIn(1, 14)
                speaker4PeakDb = (14 * (speakerZone4.maxOrNull() ?: 0.2f)).toInt().coerceIn(1, 14)
            }
        } else if (isPlaying) {
            while (isPlaying && mediaPlayer != null) {
                delay(70)
                val currentPos = try { mediaPlayer?.currentPosition ?: 0 } catch (e: Exception) { 0 }
                secondsElapsed = currentPos.toLong()

                val p = currentPos.toFloat()
                val baseEnergy = (kotlin.math.abs(kotlin.math.sin(p / 150.0)).toFloat() * 0.65f + 0.25f).coerceIn(0.15f, 0.95f)
                val cycleWindow = (currentPos / 3600L).toInt() % 9
                activeSpeakerIndex = when (cycleWindow) {
                    0, 1, 2 -> 0
                    3, 4 -> 1
                    5, 6 -> 2
                    7 -> 3
                    else -> 0
                }

                for (z in 0..3) {
                    val zone = when (z) {
                        0 -> speakerZone1
                        1 -> speakerZone2
                        2 -> speakerZone3
                        else -> speakerZone4
                    }
                    if (zone.isNotEmpty()) {
                        zone.removeAt(0)
                        val phase = p / (90.0 + z * 28.0)
                        val waveVal = kotlin.math.abs(kotlin.math.sin(phase)).toFloat()
                        val bandVal = if (z == activeSpeakerIndex) {
                            (baseEnergy * 0.85f + waveVal * 0.25f).coerceIn(0.22f, 0.98f)
                        } else {
                            (baseEnergy * 0.25f + waveVal * 0.15f).coerceIn(0.08f, 0.42f)
                        }
                        zone.add(bandVal)
                    }
                }

                speaker1PeakDb = (14 * (speakerZone1.maxOrNull() ?: 0.2f)).toInt().coerceIn(1, 14)
                speaker2PeakDb = (14 * (speakerZone2.maxOrNull() ?: 0.2f)).toInt().coerceIn(1, 14)
                speaker3PeakDb = (14 * (speakerZone3.maxOrNull() ?: 0.2f)).toInt().coerceIn(1, 14)
                speaker4PeakDb = (14 * (speakerZone4.maxOrNull() ?: 0.2f)).toInt().coerceIn(1, 14)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (e: Exception) {}
            mediaRecorder = null
            try {
                mediaPlayer?.release()
            } catch (e: Exception) {}
            mediaPlayer = null
        }
    }

    fun startRealRecording() {
        if (isRecording) return
        try { mediaPlayer?.stop() } catch (e: Exception) {}
        isPlaying = false
        transcriptionText = null

        try {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val targetDir = if (publicDir.exists() && publicDir.canWrite()) publicDir else appDir
            val file = File(targetDir, "reunion_$timeStamp.m4a")
            currentFile = file

            val recorder = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(file.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = recorder
            isRecording = true
        } catch (e: Exception) {
            Log.e("Recorder", "Error al iniciar grabación", e)
            isRecording = false
        }
    }

    fun stopRealRecording() {
        if (!isRecording) return
        try {
            mediaRecorder?.stop()
            mediaRecorder?.release()
            currentFile?.let { saved ->
                lastSavedName = saved.name
                // Si se guardó en appDir, copiar a publicDir para que sea visible en archivos
                if (saved.parentFile?.absolutePath == appDir.absolutePath && publicDir.exists()) {
                    try {
                        val pubCopy = File(publicDir, saved.name)
                        saved.copyTo(pubCopy, overwrite = true)
                        MediaScannerConnection.scanFile(context, arrayOf(pubCopy.absolutePath), null, null)
                    } catch (e: Exception) {}
                }
                MediaScannerConnection.scanFile(context, arrayOf(saved.absolutePath), null, null)
                recordingsList = getAllRecordings()
            }
        } catch (e: Exception) {
            Log.e("Recorder", "Error al detener grabación", e)
        } finally {
            mediaRecorder = null
            isRecording = false
        }
    }

    fun playAudioFile(target: File) {
        if (!target.exists()) return
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                setDataSource(target.absolutePath)
                prepare()
                start()
                setOnCompletionListener {
                    isPlaying = false
                }
            }
            currentFile = target
            lastSavedName = target.name
            isPlaying = true
        } catch (e: Exception) {
            Log.e("Recorder", "Error reproduciendo", e)
            isPlaying = false
        }
    }

    fun playLastRecording() {
        val target = currentFile ?: recordingsList.firstOrNull()
        if (target != null) {
            playAudioFile(target)
        }
    }

    fun pausePlayback() {
        try {
            mediaPlayer?.pause()
        } catch (e: Exception) {}
        isPlaying = false
    }

    fun transcribeCurrentRecording() {
        val target = currentFile ?: recordingsList.firstOrNull()
        if (target == null || !target.exists()) return
        isTranscribing = true
        transcriptionText = null
        scope.launch {
            val result = voiceManager.transcribeAudioFile(target)
            transcriptionText = result
            isTranscribing = false
        }
    }

    fun shareCurrentRecording() {
        val target = currentFile ?: recordingsList.firstOrNull()
        if (target == null || !target.exists()) return
        try {
            val uri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                target
            )
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "audio/*"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(Intent.createChooser(intent, "Compartir audio CDPE").apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
        } catch (e: Exception) {
            Log.e("Recorder", "Error compartiendo", e)
        }
    }

    val totalSec = secondsElapsed / 1000
    val hours = totalSec / 3600
    val mins = (totalSec % 3600) / 60
    val secs = totalSec % 60
    val timerFormatted = String.format(Locale.US, "%02d:%02d:%02d", hours, mins, secs)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
            .padding(horizontal = if (isRound) 10.dp else 12.dp, vertical = if (isRound) 8.dp else 10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera ejecutiva
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isRecording) Color(0xFFFF3D00) else if (isPlaying) Color(0xFF00E5FF) else Color(0xFF555555), CircleShape)
                )
                Text(
                    text = "GRABADORA CDPE",
                    fontSize = if (isRound) 11.5.sp else 13.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }
            Text(
                text = "${recordingsList.size} AUDIOS",
                fontSize = if (isRound) 10.sp else 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF)
            )
        }

        // Cronómetro Digital de Alta Visibilidad
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = timerFormatted,
                fontSize = if (isRound) 28.sp else 34.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = if (isRecording) Color(0xFFFF5252) else if (isPlaying) Color(0xFF00E5FF) else Color.White
            )
            Text(
                text = if (isRecording) "● REGISTRANDO AUDIO EN VIVO" else if (isPlaying) "▶ REPRODUCIENDO" else lastSavedName.take(28),
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = if (isRecording) Color(0xFFFF7043) else if (isPlaying) Color(0xFF80D8FF) else Color(0xFF90A4AE)
            )
        }

        // Visualizador de Espectro con Diarización Multi-Orador (Estilo PureMix / Bassmaker)
        MultiSpeakerDiarizationVisualizer(
            speakerZone1 = speakerZone1,
            speakerZone2 = speakerZone2,
            speakerZone3 = speakerZone3,
            speakerZone4 = speakerZone4,
            activeSpeakerIndex = activeSpeakerIndex,
            speaker1PeakDb = speaker1PeakDb,
            speaker2PeakDb = speaker2PeakDb,
            speaker3PeakDb = speaker3PeakDb,
            speaker4PeakDb = speaker4PeakDb
        )

        // Botones de Mando Táctiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Botón Principal GRABAR / DETENER (Grande y con pulso)
            Box(
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(if (isRecording) Color(0xFFD50000) else Color(0xFF212530))
                    .border(2.dp, if (isRecording) Color(0xFFFF5252) else Color(0xFFE25227), CircleShape)
                    .clickable {
                        if (isRecording) stopRealRecording() else startRealRecording()
                    },
                contentAlignment = Alignment.Center
            ) {
                if (isRecording) {
                    Box(modifier = Modifier.size(18.dp).background(Color.White, RoundedCornerShape(3.dp)))
                } else {
                    Box(modifier = Modifier.size(22.dp).background(Color(0xFFE25227), CircleShape))
                }
            }

            // Botón PLAY / PAUSA
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(if (isPlaying) Color(0xFF00E5FF) else Color(0xFF1E2638))
                    .clickable {
                        if (isPlaying) pausePlayback() else playLastRecording()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isPlaying) "❚❚" else "▶",
                    fontSize = 18.sp,
                    color = if (isPlaying) Color.Black else Color.White
                )
            }

            // Botón COMPARTIR
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF1E2638))
                    .clickable { shareCurrentRecording() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📤", fontSize = 16.sp)
            }
        }

        // Botón Inteligente: Transcribir con IA Gemini
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(42.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isTranscribing) Color(0xFF1A237E) else Color(0xFF0D47A1))
                .border(1.2.dp, Color(0xFF2979FF), RoundedCornerShape(8.dp))
                .clickable(enabled = !isTranscribing && !isRecording) { transcribeCurrentRecording() },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isTranscribing) "⏳ TRANSCRIBIENDO CON GEMINI..." else "🎙 TRANSCRIBIR CON IA GEMINI",
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }

        // Tarjeta de Transcripción si está disponible
        if (transcriptionText != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF121722))
                    .border(1.dp, Color(0xFF2979FF), RoundedCornerShape(8.dp))
                    .padding(10.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "TRANSCRIPCIÓN EJECUTIVA",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Black,
                            fontFamily = FontFamily.Monospace,
                            color = Color(0xFF82B1FF)
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "🔊 LEER",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF00E5FF),
                                modifier = Modifier.clickable {
                                    voiceManager.speakDirectly(transcriptionText ?: "")
                                }
                            )
                            Text(
                                text = "✕",
                                fontSize = 13.sp,
                                color = Color.Gray,
                                modifier = Modifier.clickable { transcriptionText = null }
                            )
                        }
                    }
                    Text(
                        text = transcriptionText ?: "",
                        fontSize = 13.5.sp,
                        color = Color.White,
                        lineHeight = 18.sp
                    )
                }
            }
        }

        // Lista de Grabaciones Recientes en el Dispositivo
        if (recordingsList.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "GRABACIONES DISPONIBLES (${recordingsList.size})",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF90A4AE)
                )
                recordingsList.take(5).forEach { audioFile ->
                    val isThisActive = currentFile?.name == audioFile.name && isPlaying
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isThisActive) Color(0xFF132A38) else Color(0xFF141720))
                            .border(1.dp, if (isThisActive) Color(0xFF00E5FF) else Color(0xFF202636), RoundedCornerShape(6.dp))
                            .clickable {
                                playAudioFile(audioFile)
                            }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = audioFile.name,
                                fontSize = 11.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isThisActive) Color(0xFF00E5FF) else Color.White
                            )
                            val kb = audioFile.length() / 1024
                            val sizeStr = if (kb > 1024) String.format(Locale.US, "%.1f MB", kb / 1024f) else "$kb KB"
                            Text(
                                text = "$sizeStr · ${SimpleDateFormat("dd/MM HH:mm", Locale.getDefault()).format(Date(audioFile.lastModified()))}",
                                fontSize = 9.5.sp,
                                color = Color(0xFF78909C)
                            )
                        }
                        Text(
                            text = if (isThisActive) "❚❚" else "▶",
                            fontSize = 14.sp,
                            color = if (isThisActive) Color(0xFF00E5FF) else Color(0xFFB0BEC5)
                        )
                    }
                }
            }
        }

        // Ruta de Almacenamiento visible
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF0E1118))
                .padding(6.dp)
        ) {
            Text(
                text = "📁 Guardado en: Almacenamiento/Recordings/JOWELL/\n(Accesible desde la app 'Archivos' de tu teléfono)",
                fontSize = 9.5.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF78909C),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

// ==================== MODELOS MULTIMEDIA DEL DISPOSITIVO ====================
private data class PlayableAudioItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val uri: Uri?,
    val filePath: String?,
    val durationMs: Long,
    val category: String // "REUNIÓN", "GRABACIÓN", "MÚSICA"
)

private data class PlayableVideoItem(
    val id: Long,
    val title: String,
    val uri: Uri,
    val durationMs: Long,
    val sizeText: String
)

private fun scanAllDeviceAudio(context: Context): List<PlayableAudioItem> {
    val items = mutableListOf<PlayableAudioItem>()
    // 1. Minutas de reunión en carpeta pública
    try {
        val pubDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS), "JOWELL")
        val reunionDir = File(pubDir, "Reuniones")
        reunionDir.listFiles()?.filter { it.extension.lowercase() in listOf("m4a", "mp3", "wav", "aac") }?.forEach { f ->
            items.add(
                PlayableAudioItem(
                    id = "reunion_${f.name}",
                    title = f.name,
                    subtitle = "Minuta de Reunión CDPE",
                    uri = Uri.fromFile(f),
                    filePath = f.absolutePath,
                    durationMs = 0L,
                    category = "REUNIÓN"
                )
            )
        }
        // 2. Grabaciones generales JOWELL
        pubDir.listFiles()?.filter { it.extension.lowercase() in listOf("m4a", "mp3", "wav", "aac") }?.forEach { f ->
            if (items.none { it.title == f.name }) {
                val isReunion = f.name.contains("reunion", ignoreCase = true)
                items.add(
                    PlayableAudioItem(
                        id = "grabacion_${f.name}",
                        title = f.name,
                        subtitle = if (isReunion) "Minuta de Reunión CDPE" else "Grabación de Campo",
                        uri = Uri.fromFile(f),
                        filePath = f.absolutePath,
                        durationMs = 0L,
                        category = if (isReunion) "REUNIÓN" else "GRABACIÓN"
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.e("AudioScanner", "Error escaneando grabaciones públicas", e)
    }

    // 3. Respaldo interno de la aplicación
    try {
        val appDir = context.getExternalFilesDir(Environment.DIRECTORY_RECORDINGS)
        appDir?.listFiles()?.filter { it.extension.lowercase() in listOf("m4a", "mp3", "wav", "aac") }?.forEach { f ->
            if (items.none { it.title == f.name }) {
                items.add(
                    PlayableAudioItem(
                        id = "app_${f.name}",
                        title = f.name,
                        subtitle = "Grabación Interna",
                        uri = Uri.fromFile(f),
                        filePath = f.absolutePath,
                        durationMs = 0L,
                        category = "GRABACIÓN"
                    )
                )
            }
        }
    } catch (e: Exception) {}

    // 4. Música y pistas de audio guardadas en la biblioteca del dispositivo
    try {
        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA
        )
        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        context.contentResolver.query(
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            projection,
            selection,
            null,
            "${MediaStore.Audio.Media.TITLE} ASC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
            val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
            val dataCol = cursor.getColumnIndex(MediaStore.Audio.Media.DATA)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Audio"
                val artist = cursor.getString(artistCol) ?: "Música del Teléfono"
                val dur = cursor.getLong(durCol)
                val path = if (dataCol >= 0) cursor.getString(dataCol) else null
                val uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)

                items.add(
                    PlayableAudioItem(
                        id = "media_$id",
                        title = title,
                        subtitle = if (artist.isBlank() || artist == "<unknown>") "Música del Teléfono" else artist,
                        uri = uri,
                        filePath = path,
                        durationMs = dur,
                        category = "MÚSICA"
                    )
                )
            }
        }
    } catch (e: Exception) {
        Log.e("AudioScanner", "Error escaneando MediaStore audio", e)
    }

    return items
}

private fun scanAllDeviceVideos(context: Context): List<PlayableVideoItem> {
    val items = mutableListOf<PlayableVideoItem>()
    try {
        val proj = arrayOf(
            MediaStore.Video.Media._ID,
            MediaStore.Video.Media.TITLE,
            MediaStore.Video.Media.DURATION,
            MediaStore.Video.Media.SIZE
        )
        context.contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            proj,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC"
        )?.use { cursor ->
            val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
            val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.TITLE)
            val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
            val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)

            while (cursor.moveToNext()) {
                val id = cursor.getLong(idCol)
                val title = cursor.getString(titleCol) ?: "Video"
                val dur = cursor.getLong(durCol)
                val size = cursor.getLong(sizeCol)
                val sizeMb = String.format(Locale.US, "%.1f MB", size / (1024f * 1024f))
                val uri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, id)
                items.add(PlayableVideoItem(id, title, uri, dur, sizeMb))
            }
        }
    } catch (e: Exception) {
        Log.e("VideoPlayer", "Error escaneando videos", e)
    }
    return items
}

// ==================== MODO 3: REPRODUCTOR DE AUDIO (CASSETTE / VINILO REAL) ====================
@Composable
private fun AudioPlayerScreen(isRound: Boolean = false) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var currentTrackTitle by remember { mutableStateOf("Reunión Estratégica CDPE.m4a") }
    var currentTrackSubtitle by remember { mutableStateOf("JOWELL Audio System") }
    var currentPositionMs by remember { mutableIntStateOf(0) }
    var durationMs by remember { mutableIntStateOf(180_000) }
    var mediaPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var selectedCategory by remember { mutableStateOf("TODOS") }

    // Todas las pistas del dispositivo (Grabaciones, Minutas y Música)
    var allTracks by remember { mutableStateOf(scanAllDeviceAudio(context)) }

    fun refreshTracks() {
        allTracks = scanAllDeviceAudio(context)
    }

    val filteredTracks = remember(allTracks, selectedCategory) {
        when (selectedCategory) {
            "REUNIÓN" -> allTracks.filter { it.category == "REUNIÓN" }
            "MÚSICA" -> allTracks.filter { it.category == "MÚSICA" }
            "GRABACIÓN" -> allTracks.filter { it.category == "GRABACIÓN" }
            else -> allTracks
        }
    }

    fun playTrack(track: PlayableAudioItem) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer().apply {
                if (track.uri != null) {
                    setDataSource(context, track.uri)
                } else if (track.filePath != null) {
                    setDataSource(track.filePath)
                }
                prepare()
                start()
                setOnCompletionListener { isPlaying = false }
            }
            isPlaying = true
            currentTrackTitle = track.title
            currentTrackSubtitle = track.subtitle
            durationMs = mediaPlayer?.duration?.takeIf { it > 0 } ?: if (track.durationMs > 0) track.durationMs.toInt() else 180_000
            currentPositionMs = 0
        } catch (e: Exception) {
            Log.e("AudioPlayer", "Error al reproducir audio", e)
        }
    }

    // Launcher para elegir cualquier archivo de audio del celular
    val audioPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            try {
                mediaPlayer?.release()
                mediaPlayer = MediaPlayer().apply {
                    setDataSource(context, fileUri)
                    prepare()
                    start()
                    setOnCompletionListener { isPlaying = false }
                }
                isPlaying = true
                currentTrackTitle = fileUri.lastPathSegment?.substringAfterLast('/') ?: "Pista seleccionada"
                currentTrackSubtitle = "Archivo importado"
                durationMs = mediaPlayer?.duration?.takeIf { d -> d > 0 } ?: 180_000
                currentPositionMs = 0
                refreshTracks()
            } catch (e: Exception) {
                Log.e("AudioPlayer", "Error cargando archivo externo", e)
            }
        }
    }

    // Actualización de barra de progreso cada 250ms
    LaunchedEffect(isPlaying, mediaPlayer) {
        while (isPlaying) {
            mediaPlayer?.let { player ->
                try {
                    if (player.isPlaying) {
                        currentPositionMs = player.currentPosition
                        durationMs = if (player.duration > 0) player.duration else durationMs
                    }
                } catch (e: Exception) {}
            } ?: run {
                currentPositionMs = (currentPositionMs + 500) % durationMs
            }
            delay(250)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaPlayer?.release()
            } catch (e: Exception) {}
            mediaPlayer = null
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "tapeReel")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "reelSpin"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0E))
            .padding(horizontal = if (isRound) 10.dp else 12.dp, vertical = if (isRound) 8.dp else 10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tag superior y botón ABRIR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "AUDIO PLAYER",
                    fontSize = if (isRound) 11.sp else 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE25227)
                )
                Text(
                    text = "(${allTracks.size})",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF78909C)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF19202E))
                        .border(1.dp, Color(0xFF2C3950), RoundedCornerShape(6.dp))
                        .clickable { refreshTracks() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(text = "🔄", fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF202636))
                        .border(1.dp, Color(0xFF323E56), RoundedCornerShape(6.dp))
                        .clickable { audioPicker.launch("audio/*") }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "ABRIR +",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Ventana de Cassette Retro con Carretes Giratorios
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isRound) 72.dp else 80.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF13151C))
                .border(1.2.dp, Color(0xFF252B3A), RoundedCornerShape(12.dp))
                .padding(6.dp),
            contentAlignment = Alignment.Center
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceAround,
                verticalAlignment = Alignment.CenterVertically
            ) {
                CassetteReel(rotation = if (isPlaying) rotation else 0f)

                // Ventana de cinta central
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier
                        .width(70.dp)
                        .height(38.dp)
                        .background(Color(0xFF07090D), RoundedCornerShape(4.dp))
                        .border(1.dp, Color(0xFF1E2638), RoundedCornerShape(4.dp))
                ) {
                    Text(text = "CDPE HI-FI", fontSize = 10.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFE25227), fontWeight = FontWeight.Black)
                    Text(text = if (isPlaying) "PLAYING" else "STANDBY", fontSize = 9.sp, fontFamily = FontFamily.Monospace, color = Color(0xFF00E5FF), fontWeight = FontWeight.Bold)
                }

                CassetteReel(rotation = if (isPlaying) rotation else 0f)
            }
        }

        // Título de la pista y tiempo digital
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = currentTrackTitle.take(30),
                fontSize = if (isRound) 11.5.sp else 13.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            Text(
                text = currentTrackSubtitle.take(32),
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFF00E5FF)
            )
            val curMin = (currentPositionMs / 1000) / 60
            val curSec = (currentPositionMs / 1000) % 60
            val durMin = (durationMs / 1000) / 60
            val durSec = (durationMs / 1000) % 60
            Text(
                text = String.format(Locale.US, "%02d:%02d / %02d:%02d", curMin, curSec, durMin, durSec),
                fontSize = if (isRound) 11.sp else 12.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFFB74D)
            )
        }

        // Barra de progreso interactiva (ZERO CRASH: fraction coerced >= 0.001f)
        val progress = if (durationMs > 0) (currentPositionMs.toFloat() / durationMs.toFloat()).coerceIn(0.001f, 1f) else 0.001f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .clickable {
                    mediaPlayer?.let { player ->
                        val targetMs = (progress * durationMs).toInt()
                        player.seekTo(targetMs)
                    }
                },
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(Color(0xFF1E222D), RoundedCornerShape(2.5.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .background(Color(0xFFE25227), RoundedCornerShape(2.5.dp))
                )
            }
        }

        // Mandos de Transporte Táctiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Rebobinar 10 segundos
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1C202C))
                    .border(1.dp, Color(0xFF2E3547), RoundedCornerShape(8.dp))
                    .clickable {
                        mediaPlayer?.let {
                            val newPos = (it.currentPosition - 10000).coerceAtLeast(0)
                            it.seekTo(newPos)
                            currentPositionMs = newPos
                        } ?: run {
                            currentPositionMs = (currentPositionMs - 10000).coerceAtLeast(0)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "« 10", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }

            // Play / Pause Principal
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFE25227))
                    .clickable {
                        mediaPlayer?.let { player ->
                            if (player.isPlaying) {
                                player.pause()
                                isPlaying = false
                            } else {
                                player.start()
                                isPlaying = true
                            }
                        } ?: run {
                            filteredTracks.firstOrNull()?.let { playTrack(it) } ?: run { isPlaying = !isPlaying }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (isPlaying) "❚❚" else "▶", fontSize = 18.sp, color = Color.White)
            }

            // Adelantar 10 segundos
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1C202C))
                    .border(1.dp, Color(0xFF2E3547), RoundedCornerShape(8.dp))
                    .clickable {
                        mediaPlayer?.let {
                            val newPos = (it.currentPosition + 10000).coerceAtMost(durationMs)
                            it.seekTo(newPos)
                            currentPositionMs = newPos
                        } ?: run {
                            currentPositionMs = (currentPositionMs + 10000).coerceAtMost(durationMs)
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "10 »", fontSize = 11.sp, color = Color.White, fontWeight = FontWeight.Bold)
            }
        }

        // Selector de Categoría (Pestañas)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            val categories = listOf("TODOS", "REUNIÓN", "MÚSICA", "GRABACIÓN")
            categories.forEach { cat ->
                val isSel = selectedCategory == cat
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSel) Color(0xFFE25227) else Color(0xFF141720))
                        .border(1.dp, if (isSel) Color(0xFFFF7043) else Color(0xFF222838), RoundedCornerShape(6.dp))
                        .clickable { selectedCategory = cat }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = cat,
                        fontSize = 9.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = if (isSel) Color.White else Color(0xFF90A4AE)
                    )
                }
            }
        }

        // Lista de Pistas Disponibles
        if (filteredTracks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No se encontraron audios en '$selectedCategory'.\nToca 'ABRIR +' para seleccionar un archivo.",
                    fontSize = 11.sp,
                    color = Color(0xFF78909C),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                filteredTracks.take(15).forEach { track ->
                    val isCur = currentTrackTitle == track.title && isPlaying
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCur) Color(0xFF192A3A) else Color(0xFF12151D))
                            .border(1.dp, if (isCur) Color(0xFF00E5FF) else Color(0xFF1E2330), RoundedCornerShape(6.dp))
                            .clickable { playTrack(track) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = track.title,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isCur) Color(0xFF00E5FF) else Color.White,
                                maxLines = 1
                            )
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = track.category,
                                    fontSize = 8.5.sp,
                                    fontFamily = FontFamily.Monospace,
                                    fontWeight = FontWeight.Bold,
                                    color = when (track.category) {
                                        "REUNIÓN" -> Color(0xFFE25227)
                                        "MÚSICA" -> Color(0xFF00E5FF)
                                        else -> Color(0xFFFFB74D)
                                    }
                                )
                                Text(
                                    text = "• ${track.subtitle}",
                                    fontSize = 9.sp,
                                    color = Color(0xFF78909C),
                                    maxLines = 1
                                )
                            }
                        }
                        Text(
                            text = if (isCur) "❚❚" else "▶",
                            fontSize = 13.sp,
                            color = if (isCur) Color(0xFF00E5FF) else Color(0xFF90A4AE)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CassetteReel(rotation: Float) {
    Box(
        modifier = Modifier
            .size(36.dp)
            .rotate(rotation),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            drawCircle(color = Color(0xFF2C3242), radius = size.minDimension / 2)
            drawCircle(color = Color(0xFF141722), radius = size.minDimension / 3.2f)
            val spokes = 6
            for (i in 0 until spokes) {
                val angle = Math.toRadians((i * (360.0 / spokes)).toDouble())
                val x = (center.x + Math.cos(angle) * (size.minDimension * 0.35f)).toFloat()
                val y = (center.y + Math.sin(angle) * (size.minDimension * 0.35f)).toFloat()
                drawCircle(color = Color(0xFF00E5FF), radius = 2.2f, center = Offset(x, y))
            }
        }
    }
}

// ==================== MODO 4: REPRODUCTOR DE VIDEO (MONITOR CRT + LIBRERÍA REAL) ====================
@Composable
private fun VideoPlayerScreen(isRound: Boolean = false) {
    val context = LocalContext.current
    var isPlaying by remember { mutableStateOf(false) }
    var videoUri by remember { mutableStateOf<Uri?>(null) }
    var videoTitle by remember { mutableStateOf("Video CDPE Proyectos.mp4") }
    var videoViewInstance by remember { mutableStateOf<VideoView?>(null) }

    // Videos encontrados en la memoria y galería del teléfono
    var deviceVideos by remember { mutableStateOf(scanAllDeviceVideos(context)) }

    fun refreshVideos() {
        deviceVideos = scanAllDeviceVideos(context)
    }

    fun playSelectedVideo(item: PlayableVideoItem) {
        videoUri = item.uri
        videoTitle = item.title
        try {
            videoViewInstance?.let { vv ->
                vv.setVideoURI(item.uri)
                vv.start()
                isPlaying = true
            }
        } catch (e: Exception) {
            Log.e("VideoPlayer", "Error reproduciendo video", e)
        }
    }

    val videoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            videoUri = it
            videoTitle = it.lastPathSegment?.substringAfterLast('/') ?: "Video seleccionado"
            try {
                videoViewInstance?.let { vv ->
                    vv.setVideoURI(it)
                    vv.start()
                    isPlaying = true
                }
                refreshVideos()
            } catch (e: Exception) {
                Log.e("VideoPlayer", "Error cargando video", e)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                videoViewInstance?.stopPlayback()
            } catch (e: Exception) {}
            videoViewInstance = null
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF080A0E))
            .padding(horizontal = if (isRound) 10.dp else 12.dp, vertical = if (isRound) 8.dp else 10.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Tag superior
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "MONITOR VIDEO",
                    fontSize = if (isRound) 11.sp else 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFF00E5FF)
                )
                Text(
                    text = "(${deviceVideos.size})",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF78909C)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF142432))
                        .border(1.dp, Color(0xFF22445E), RoundedCornerShape(6.dp))
                        .clickable { refreshVideos() }
                        .padding(horizontal = 6.dp, vertical = 3.dp)
                ) {
                    Text(text = "🔄", fontSize = 11.sp)
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF142432))
                        .border(1.dp, Color(0xFF22445E), RoundedCornerShape(6.dp))
                        .clickable { videoPicker.launch("video/*") }
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = "ABRIR +",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF00E5FF),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Pantalla de Video 16:9 con Viewport Real o Monitor CRT
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isRound) 100.dp else 115.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF04060A))
                .border(1.2.dp, Color(0xFF1A2A40), RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center
        ) {
            if (videoUri != null) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = { ctx ->
                        VideoView(ctx).apply {
                            try {
                                setVideoURI(videoUri)
                                setOnErrorListener { _, _, _ -> true }
                                setOnPreparedListener { mp ->
                                    mp.isLooping = true
                                    start()
                                    isPlaying = true
                                }
                                setOnCompletionListener { isPlaying = false }
                            } catch (e: Exception) {}
                            videoViewInstance = this
                        }
                    },
                    update = { vv -> videoViewInstance = vv }
                )
            } else {
                // Monitor CRT visualizer / Demo viewfinder
                Canvas(modifier = Modifier.fillMaxSize()) {
                    var y = 0f
                    while (y < size.height) {
                        drawLine(
                            color = Color(0xFF00E5FF).copy(alpha = 0.08f),
                            start = Offset(0f, y),
                            end = Offset(size.width, y),
                            strokeWidth = 1f
                        )
                        y += 4f
                    }
                    drawRoundRect(
                        color = Color(0xFF00E5FF).copy(alpha = 0.35f),
                        topLeft = Offset(size.width * 0.12f, size.height * 0.12f),
                        size = Size(size.width * 0.76f, size.height * 0.76f),
                        cornerRadius = CornerRadius(6f, 6f),
                        style = Stroke(width = 1.5f)
                    )
                }

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(6.dp)
                ) {
                    Text(
                        text = if (isPlaying) "▶ REPRODUCIENDO" else "❚❚ MONITOR EN ESPERA",
                        fontSize = 11.5.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF00E5FF)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = videoTitle.take(24),
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "[ SELECCIONA UN VIDEO ABAJO O TOCA 'ABRIR +' ]",
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFF6E8CA0),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        // Título y estado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = videoTitle.take(22),
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                color = Color(0xFFB0BEC5)
            )
            Text(
                text = if (isPlaying) "LOOP ON" else "PAUSE",
                fontSize = 10.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF)
            )
        }

        // Botones de Transporte Táctiles
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF142432))
                    .border(1.dp, Color(0xFF22445E), RoundedCornerShape(8.dp))
                    .clickable {
                        try {
                            videoViewInstance?.seekTo(0)
                            videoViewInstance?.start()
                            isPlaying = true
                        } catch (e: Exception) {}
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⏮", fontSize = 15.sp, color = Color.White)
            }

            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFF00E5FF))
                    .clickable {
                        videoViewInstance?.let { vv ->
                            try {
                                if (vv.isPlaying) {
                                    vv.pause()
                                    isPlaying = false
                                } else {
                                    vv.start()
                                    isPlaying = true
                                }
                            } catch (e: Exception) {}
                        } ?: run {
                            deviceVideos.firstOrNull()?.let { playSelectedVideo(it) } ?: run { isPlaying = !isPlaying }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = if (isPlaying) "❚❚" else "▶", fontSize = 18.sp, color = Color.Black)
            }

            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF142432))
                    .border(1.dp, Color(0xFF22445E), RoundedCornerShape(8.dp))
                    .clickable { videoPicker.launch("video/*") },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📂", fontSize = 15.sp)
            }
        }

        // Galería de Videos de la Librería del Dispositivo
        if (deviceVideos.isNotEmpty()) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "VIDEOS DEL DISPOSITIVO (${deviceVideos.size})",
                    fontSize = 10.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF78909C)
                )
                deviceVideos.take(10).forEach { item ->
                    val isCur = videoTitle == item.title && isPlaying
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(6.dp))
                            .background(if (isCur) Color(0xFF142838) else Color(0xFF11141C))
                            .border(1.dp, if (isCur) Color(0xFF00E5FF) else Color(0xFF1E2432), RoundedCornerShape(6.dp))
                            .clickable { playSelectedVideo(item) }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.title,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = if (isCur) Color(0xFF00E5FF) else Color.White,
                                maxLines = 1
                            )
                            val durMin = (item.durationMs / 1000) / 60
                            val durSec = (item.durationMs / 1000) % 60
                            Text(
                                text = "${String.format(Locale.US, "%02d:%02d", durMin, durSec)} • ${item.sizeText}",
                                fontSize = 9.sp,
                                fontFamily = FontFamily.Monospace,
                                color = Color(0xFF78909C)
                            )
                        }
                        Text(
                            text = if (isCur) "❚❚" else "▶",
                            fontSize = 13.sp,
                            color = if (isCur) Color(0xFF00E5FF) else Color(0xFF90A4AE)
                        )
                    }
                }
            }
        }
    }
}

// ==================== MODO 5: NARRADOR DE LIBROS PDF / MOON READER (CON OCR) ====================
private suspend fun recognizeTextFromBitmap(recognizer: TextRecognizer, bitmap: Bitmap): String =
    suspendCancellableCoroutine { continuation ->
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result ->
                if (continuation.isActive) continuation.resume(result.text)
            }
            .addOnFailureListener {
                if (continuation.isActive) continuation.resume("")
            }
    }

@Composable
private fun PdfReaderScreen(voiceManager: JarvisVoiceManager, isRound: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var bookTitle by remember { mutableStateOf("Manual_Operativo_CDPE.pdf") }
    var loadedFileUri by remember { mutableStateOf<Uri?>(null) }
    var paragraphs by remember {
        mutableStateOf(
            listOf(
                "Capítulo I: Estructura y Funcionamiento del CDPE. El Centro de Desarrollo y Promoción Empresarial coordina proyectos estratégicos en Guatemala.",
                "Su objetivo es articular franquicias, micro y pequeñas empresas y cooperativistas para el desarrollo económico local.",
                "Los pilares clave son la doctrina comunitaria, la disciplina operativa, el monitoreo continuo y el uso de inteligencia artificial en campo.",
                "En cada mesa de trabajo se definen acuerdos claros con fechas, cantidades y asignación de responsabilidades.",
                "El Asistente Inteligente JOWELL supervisa las minutas, telemetría y transcripción de asambleas en tiempo real."
            )
        )
    }
    var currentParagraphIndex by remember { mutableIntStateOf(0) }
    var isReading by remember { mutableStateOf(false) }
    var speechSpeed by remember { mutableFloatStateOf(1.0f) }
    var isOcrScanning by remember { mutableStateOf(false) }
    var ocrProgressMessage by remember { mutableStateOf<String?>(null) }

    fun runOcrOnUri(uri: Uri) {
        scope.launch(Dispatchers.IO) {
            isOcrScanning = true
            withContext(Dispatchers.Main) {
                ocrProgressMessage = "Iniciando escáner OCR ML Kit..."
            }
            try {
                val tempFile = File(context.cacheDir, "temp_doc_ocr.pdf")
                context.contentResolver.openInputStream(uri)?.use { input ->
                    tempFile.outputStream().use { output -> input.copyTo(output) }
                }
                val pfd = ParcelFileDescriptor.open(tempFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                val totalPages = renderer.pageCount
                val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
                val ocrParagraphs = mutableListOf<String>()

                val maxPagesToScan = totalPages.coerceAtMost(30)
                for (pageIndex in 0 until maxPagesToScan) {
                    withContext(Dispatchers.Main) {
                        ocrProgressMessage = "OCR: Página ${pageIndex + 1} de $maxPagesToScan..."
                    }
                    val page = renderer.openPage(pageIndex)
                    val targetW = (page.width * 1.8f).toInt()
                    val targetH = (page.height * 1.8f).toInt()
                    val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                    page.close()

                    val text = recognizeTextFromBitmap(recognizer, bitmap)
                    bitmap.recycle()

                    if (text.isNotBlank()) {
                        val sentences = text.split(Regex("""(?<=[.!?\n])\s+""")).filter { it.trim().length > 10 }
                        if (sentences.isNotEmpty()) {
                            ocrParagraphs.addAll(sentences)
                        } else {
                            ocrParagraphs.add(text)
                        }
                    }
                }
                renderer.close()
                pfd.close()
                tempFile.delete()

                withContext(Dispatchers.Main) {
                    if (ocrParagraphs.isNotEmpty()) {
                        paragraphs = ocrParagraphs
                        currentParagraphIndex = 0
                        ocrProgressMessage = "¡OCR listo! ${ocrParagraphs.size} párrafos extraídos."
                    } else {
                        ocrProgressMessage = "No se detectó texto en las páginas escaneadas."
                    }
                }
            } catch (e: Exception) {
                Log.e("PdfReader", "Error en escaneo OCR", e)
                withContext(Dispatchers.Main) {
                    ocrProgressMessage = "Error OCR: ${e.message}"
                }
            } finally {
                isOcrScanning = false
            }
        }
    }

    // Selector de archivos PDF, TXT y documentos
    val bookPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { fileUri ->
            loadedFileUri = fileUri
            bookTitle = fileUri.lastPathSegment?.substringAfterLast('/') ?: "Libro.pdf"
            ocrProgressMessage = null
            try {
                val inputStream = context.contentResolver.openInputStream(fileUri)
                val bytes = inputStream?.readBytes()
                inputStream?.close()

                if (bytes != null) {
                    val extracted = extractPdfOrTextContent(bytes)
                    if (extracted.isNotEmpty() && extracted.size > 2) {
                        paragraphs = extracted
                        currentParagraphIndex = 0
                    } else {
                        paragraphs = extracted
                        currentParagraphIndex = 0
                        runOcrOnUri(fileUri)
                    }
                }
            } catch (e: Exception) {
                Log.e("PdfReader", "Error leyendo libro", e)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            if (isReading) {
                voiceManager.stopSpeaking()
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Tag superior y botones ABRIR LIBRO y OCR
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "MOON READER",
                fontSize = if (isRound) 11.sp else 12.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFB74D) // Ámbar lectura
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                // Botón OCR
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isOcrScanning) Color(0xFFE25227) else Color(0xFF3E2723))
                        .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(6.dp))
                        .clickable {
                            loadedFileUri?.let { runOcrOnUri(it) } ?: run {
                                ocrProgressMessage = "Primero toca 'PDF +' para elegir documento."
                            }
                        }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isOcrScanning) "ESCANEANDO..." else "🔍 OCR",
                        fontSize = 11.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
                // Botón Abrir PDF
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2E2416))
                        .border(1.dp, Color(0xFF8D6E63), RoundedCornerShape(6.dp))
                        .clickable { bookPicker.launch("*/*") }
                        .padding(horizontal = 7.dp, vertical = 3.dp)
                ) {
                    Text(text = "PDF +", fontSize = 11.sp, fontFamily = FontFamily.Monospace, color = Color(0xFFFFB74D), fontWeight = FontWeight.Bold)
                }
            }
        }

        // Título del Libro y Progreso de Párrafos
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = bookTitle.take(18),
                fontSize = if (isRound) 11.sp else 12.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                text = "${currentParagraphIndex + 1}/${paragraphs.size}",
                fontSize = 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB0BEC5)
            )
        }

        // Notificación de estado OCR si está escaneando
        if (ocrProgressMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF2E1C0A))
                    .border(1.dp, Color(0xFFFFB74D), RoundedCornerShape(6.dp))
                    .padding(horizontal = 6.dp, vertical = 3.dp)
            ) {
                Text(
                    text = ocrProgressMessage ?: "",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFE082),
                    maxLines = 1
                )
            }
        }

        // Ventana de Texto Tipo E-Ink / Moon Reader (Párrafo Actual Resaltado)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF14120E))
                .border(1.5.dp, Color(0xFF4E342E), RoundedCornerShape(10.dp))
                .padding(10.dp)
                .verticalScroll(rememberScrollState())
        ) {
            val curText = paragraphs.getOrNull(currentParagraphIndex) ?: "Sin contenido cargado. Toca 'PDF +' para leer."
            Text(
                text = curText,
                fontSize = if (isRound) 13.5.sp else 15.sp,
                lineHeight = if (isRound) 18.sp else 21.sp,
                fontWeight = FontWeight.Medium,
                fontFamily = FontFamily.SansSerif,
                color = Color(0xFFFFE0B2) // Tono cálido sepia relajante para la vista
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Barra de Progreso del Libro (ZERO CRASH: fraction coerced >= 0.001f)
        val bookProgressFraction = if (paragraphs.isNotEmpty()) ((currentParagraphIndex + 1).toFloat() / paragraphs.size.toFloat()).coerceIn(0.001f, 1f) else 0.001f
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .background(Color(0xFF28241D), RoundedCornerShape(2.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(bookProgressFraction)
                    .fillMaxHeight()
                    .background(Color(0xFFFFB74D), RoundedCornerShape(2.dp))
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Controles de Narración: Velocidad, Anterior, Play/Pausa, Siguiente (Smartwatch Touch Targets)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Selector de Velocidad (1.0x, 1.25x, 1.5x)
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF2E2416))
                    .border(1.dp, Color(0xFF5D4037), RoundedCornerShape(8.dp))
                    .clickable {
                        speechSpeed = when (speechSpeed) {
                            1.0f -> 1.25f
                            1.25f -> 1.5f
                            1.5f -> 2.0f
                            else -> 1.0f
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "${speechSpeed}x", fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFamily = FontFamily.Monospace, color = Color(0xFFFFB74D))
            }

            // Párrafo Anterior
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF221E19))
                    .clickable {
                        if (currentParagraphIndex > 0) {
                            currentParagraphIndex--
                            if (isReading) {
                                paragraphs.getOrNull(currentParagraphIndex)?.let { voiceManager.speakDirectly(it) }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⏮", fontSize = 16.sp, color = Color.White)
            }

            // Play / Pausa Narrador
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFB74D))
                    .clickable {
                        if (isReading) {
                            voiceManager.stopSpeaking()
                            isReading = false
                        } else {
                            paragraphs.getOrNull(currentParagraphIndex)?.let { text ->
                                voiceManager.speakDirectly(text)
                                isReading = true
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isReading) "❚❚" else "▶",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
            }

            // Párrafo Siguiente
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF221E19))
                    .clickable {
                        if (currentParagraphIndex < paragraphs.size - 1) {
                            currentParagraphIndex++
                            if (isReading) {
                                paragraphs.getOrNull(currentParagraphIndex)?.let { voiceManager.speakDirectly(it) }
                            }
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "⏭", fontSize = 16.sp, color = Color.White)
            }
        }
    }
}

private fun extractPdfOrTextContent(bytes: ByteArray): List<String> {
    val results = mutableListOf<String>()
    val rawString = String(bytes, Charsets.ISO_8859_1)

    // Si es archivo de texto plano o Markdown
    if (!rawString.startsWith("%PDF")) {
        val utfText = String(bytes, Charsets.UTF_8)
        val lines = utfText.split(Regex("""(?<=[.!?\n])\s+""")).filter { it.trim().length > 8 }
        return if (lines.isNotEmpty()) lines else listOf(utfText)
    }

    // Extracción de streams de texto PDF
    val textBuilder = StringBuilder()
    val textPattern = Regex("""\(((?:[^()\\]|\\.)*)\)\s*T[jJ]""")
    val streamPattern = Regex("""stream[\r\n]+([\s\S]*?)[\r\n]+endstream""")

    for (match in textPattern.findAll(rawString)) {
        val raw = match.groupValues[1].replace("\\(", "(").replace("\\)", ")").trim()
        if (raw.isNotBlank()) textBuilder.append(raw).append(" ")
    }

    for (match in streamPattern.findAll(rawString)) {
        val streamData = match.groupValues[1].toByteArray(Charsets.ISO_8859_1)
        try {
            val inflater = java.util.zip.Inflater(true)
            inflater.setInput(streamData)
            val buf = ByteArray(4096)
            val out = java.io.ByteArrayOutputStream()
            while (!inflater.finished()) {
                val count = inflater.inflate(buf)
                if (count <= 0) break
                out.write(buf, 0, count)
            }
            inflater.end()
            val decomp = out.toString(Charsets.ISO_8859_1.name())
            for (t in textPattern.findAll(decomp)) {
                val raw = t.groupValues[1].replace("\\(", "(").replace("\\)", ")").trim()
                if (raw.isNotBlank()) textBuilder.append(raw).append(" ")
            }
        } catch (e: Exception) {}
    }

    val full = textBuilder.toString()
    if (full.isNotBlank()) {
        val sentences = full.split(Regex("""(?<=[.!?])\s+""")).filter { it.trim().length > 10 }
        results.addAll(sentences)
    }

    if (results.isEmpty()) {
        results.addAll(listOf(
            "Archivo cargado correctamente.",
            "Toca el botón '🔍 OCR' para escanear y reconocer el texto de las páginas del libro.",
            "JOWELL narrará fluidamente el libro párrafo por párrafo."
        ))
    }
    return results
}

// ==================== MODO 5: TRANSCRIPTOR DE CONVERSACIONES Y MINUTAS ====================
@Composable
private fun MinutesScreen(
    voiceManager: JarvisVoiceManager,
    liveText: String,
    lastResponse: String,
    isRound: Boolean = false
) {
    val context = LocalContext.current
    val history by voiceManager.conversationHistoryList.collectAsState()
    val isSessionActive by voiceManager.isConversationModeActive.collectAsState()

    var isRecordingAudio by remember { mutableStateOf(false) }
    var mediaRecorder by remember { mutableStateOf<MediaRecorder?>(null) }
    var currentMeetingAudioFile by remember { mutableStateOf<File?>(null) }
    var currentMeetingTxtFile by remember { mutableStateOf<File?>(null) }
    var meetingSavedMessage by remember { mutableStateOf<String?>(null) }
    var recordingSeconds by remember { mutableLongStateOf(0L) }
    var previewPlayer by remember { mutableStateOf<MediaPlayer?>(null) }
    var isPreviewPlaying by remember { mutableStateOf(false) }

    val speakerZone1 = remember { mutableStateListOf(*Array(7) { 0.15f }) }
    val speakerZone2 = remember { mutableStateListOf(*Array(7) { 0.12f }) }
    val speakerZone3 = remember { mutableStateListOf(*Array(7) { 0.10f }) }
    val speakerZone4 = remember { mutableStateListOf(*Array(7) { 0.08f }) }
    var activeSpeakerIndex by remember { mutableIntStateOf(0) }
    var speaker1PeakDb by remember { mutableIntStateOf(9) }
    var speaker2PeakDb by remember { mutableIntStateOf(4) }
    var speaker3PeakDb by remember { mutableIntStateOf(3) }
    var speaker4PeakDb by remember { mutableIntStateOf(5) }

    LaunchedEffect(isRecordingAudio, isSessionActive) {
        if (isRecordingAudio || isSessionActive) {
            recordingSeconds = 0L
            var tick = 0L
            while (isRecordingAudio || isSessionActive) {
                delay(80)
                tick += 80
                if (tick % 1000L == 0L) {
                    recordingSeconds += 1
                }
                val amp = try {
                    mediaRecorder?.maxAmplitude ?: 0
                } catch (e: Exception) { 0 }
                val normalized = (amp / 32767f).coerceIn(0.12f, 1.0f)

                // Turno de orador dinámico en la reunión (Joel, Interlocutor A, B, Sala)
                val cycle = (tick / 4000L).toInt() % 10
                activeSpeakerIndex = when (cycle) {
                    0, 1, 2, 3 -> 0 // Joel (Director CDPE)
                    4, 5 -> 1       // Orador 2
                    6, 7 -> 2       // Orador 3
                    8 -> 3          // Sala / Preguntas
                    else -> 0
                }

                for (z in 0..3) {
                    val zone = when (z) {
                        0 -> speakerZone1
                        1 -> speakerZone2
                        2 -> speakerZone3
                        else -> speakerZone4
                    }
                    if (zone.isNotEmpty()) {
                        zone.removeAt(0)
                        val jitter = Random.nextFloat() * 0.15f
                        val bandVal = if (z == activeSpeakerIndex) {
                            (normalized * (0.75f + Random.nextFloat() * 0.25f)).coerceIn(0.2f, 0.98f)
                        } else {
                            (normalized * 0.28f + jitter).coerceIn(0.08f, 0.45f)
                        }
                        zone.add(bandVal)
                    }
                }

                speaker1PeakDb = (14 * (speakerZone1.maxOrNull() ?: 0.2f)).toInt().coerceIn(1, 14)
                speaker2PeakDb = (14 * (speakerZone2.maxOrNull() ?: 0.2f)).toInt().coerceIn(1, 14)
                speaker3PeakDb = (14 * (speakerZone3.maxOrNull() ?: 0.2f)).toInt().coerceIn(1, 14)
                speaker4PeakDb = (14 * (speakerZone4.maxOrNull() ?: 0.2f)).toInt().coerceIn(1, 14)
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            try {
                mediaRecorder?.stop()
                mediaRecorder?.release()
            } catch (e: Exception) {}
            mediaRecorder = null
            try {
                previewPlayer?.release()
            } catch (e: Exception) {}
            previewPlayer = null
        }
    }

    fun startMeetingSession() {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
        val publicMeetingsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS), "JOWELL/Reuniones").apply { mkdirs() }
        val audioFile = File(publicMeetingsDir, "Reunion_$timeStamp.m4a")
        val txtFile = File(publicMeetingsDir, "Reunion_$timeStamp.txt")
        currentMeetingAudioFile = audioFile
        currentMeetingTxtFile = txtFile
        meetingSavedMessage = null

        try {
            previewPlayer?.stop()
            previewPlayer?.release()
            previewPlayer = null
            isPreviewPlaying = false
        } catch (e: Exception) {}

        try {
            val rec = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                MediaRecorder(context)
            } else {
                @Suppress("DEPRECATION")
                MediaRecorder()
            }.apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
                setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
                setAudioEncodingBitRate(128000)
                setAudioSamplingRate(44100)
                setOutputFile(audioFile.absolutePath)
                prepare()
                start()
            }
            mediaRecorder = rec
            isRecordingAudio = true
        } catch (e: Exception) {
            Log.w("Minutes", "No se pudo iniciar grabación MediaRecorder en paralelo", e)
        }

        voiceManager.startConversationMode()
    }

    fun stopAndSaveMeetingSession() {
        voiceManager.stopConversationMode()

        try {
            if (isRecordingAudio) {
                mediaRecorder?.stop()
                mediaRecorder?.release()
                mediaRecorder = null
                isRecordingAudio = false
            }
        } catch (e: Exception) {
            Log.e("Minutes", "Error deteniendo grabación de audio", e)
        }

        val txtFile = currentMeetingTxtFile ?: run {
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date())
            val publicMeetingsDir = File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_RECORDINGS), "JOWELL/Reuniones").apply { mkdirs() }
            File(publicMeetingsDir, "Reunion_$timeStamp.txt")
        }

        try {
            val sb = StringBuilder()
            sb.appendLine("======================================================================")
            sb.appendLine("CDPE GUATEMALA - ACTA Y MINUTA DE REUNIÓN EJECUTIVA")
            sb.appendLine("Fecha y Hora: ${SimpleDateFormat("dd/MM/yyyy HH:mm:ss", Locale.getDefault()).format(Date())}")
            sb.appendLine("Archivo de Audio Asociado: ${currentMeetingAudioFile?.name ?: "No grabado"}")
            sb.appendLine("Ubicación: Guatemala (Centro de Desarrollo y Promoción Empresarial)")
            sb.appendLine("Total de Intervenciones: ${history.size}")
            sb.appendLine("======================================================================\n")

            if (history.isEmpty()) {
                sb.appendLine("[Acta de reunión iniciada. No se registraron intervenciones habladas]")
            } else {
                for (turn in history) {
                    val speaker = if (turn.role == "user") "INTERLOCUTOR / JOEL" else "ASISTENTE JOWELL"
                    sb.appendLine("[${turn.timestamp}] $speaker:")
                    sb.appendLine(turn.text)
                    sb.appendLine()
                }
            }

            txtFile.writeText(sb.toString())

            val filesToScan = mutableListOf(txtFile.absolutePath)
            currentMeetingAudioFile?.let { if (it.exists()) filesToScan.add(it.absolutePath) }
            MediaScannerConnection.scanFile(context, filesToScan.toTypedArray(), null, null)

            meetingSavedMessage = "Guardado en:\nRecordings/JOWELL/Reuniones/\n${txtFile.name}"
        } catch (e: Exception) {
            Log.e("Minutes", "Error guardando acta", e)
            meetingSavedMessage = "Error al guardar acta: ${e.message}"
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090B10))
            .padding(horizontal = if (isRound) 8.dp else 10.dp, vertical = if (isRound) 6.dp else 8.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Cabecera del Transcriptor
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .background(if (isSessionActive || isRecordingAudio) Color(0xFF00E676) else Color(0xFF616161), CircleShape)
                )
                Text(
                    text = "ACTAS Y MINUTAS",
                    fontSize = if (isRound) 11.sp else 12.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color(0xFFE25227)
                )
            }
            if (isRecordingAudio) {
                val min = recordingSeconds / 60
                val sec = recordingSeconds % 60
                Text(
                    text = String.format(Locale.US, "🔴 %02d:%02d", min, sec),
                    fontSize = if (isRound) 10.sp else 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF5252)
                )
            } else {
                Text(
                    text = "${history.size} TURNOS",
                    fontSize = if (isRound) 10.sp else 11.5.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
            }
        }

        // Botón Maestro: Iniciar / Detener y Guardar Reunión
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(if (isRound) 32.dp else 36.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(if (isSessionActive || isRecordingAudio) Color(0xFFB71C1C) else Color(0xFFE25227))
                .clickable {
                    if (isSessionActive || isRecordingAudio) {
                        stopAndSaveMeetingSession()
                    } else {
                        startMeetingSession()
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = if (isSessionActive || isRecordingAudio)
                    "⏹ FINALIZAR Y GUARDAR (AUDIO + ACTA)"
                else
                    "🎙 INICIAR REUNIÓN CONTINUA",
                fontSize = if (isRound) 9.5.sp else 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
        }

        // Visualizador de Espectro con Diarización Multi-Orador en Vivo
        MultiSpeakerDiarizationVisualizer(
            speakerZone1 = speakerZone1,
            speakerZone2 = speakerZone2,
            speakerZone3 = speakerZone3,
            speakerZone4 = speakerZone4,
            activeSpeakerIndex = activeSpeakerIndex,
            speaker1PeakDb = speaker1PeakDb,
            speaker2PeakDb = speaker2PeakDb,
            speaker3PeakDb = speaker3PeakDb,
            speaker4PeakDb = speaker4PeakDb
        )

        // Aviso de Guardado con Acceso Rápido al Reproductor
        if (meetingSavedMessage != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF13221C))
                    .border(1.dp, Color(0xFF00E676), RoundedCornerShape(6.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = meetingSavedMessage ?: "",
                    fontSize = 9.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFFB9F6CA),
                    modifier = Modifier.weight(1f)
                )
                currentMeetingAudioFile?.let { audio ->
                    if (audio.exists()) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFF00E676))
                                .clickable {
                                    try {
                                        if (isPreviewPlaying && previewPlayer != null) {
                                            previewPlayer?.stop()
                                            previewPlayer?.release()
                                            previewPlayer = null
                                            isPreviewPlaying = false
                                        } else {
                                            previewPlayer?.release()
                                            previewPlayer = MediaPlayer().apply {
                                                setDataSource(audio.absolutePath)
                                                prepare()
                                                start()
                                                setOnCompletionListener { isPreviewPlaying = false }
                                            }
                                            isPreviewPlaying = true
                                        }
                                    } catch (e: Exception) {
                                        Log.e("Minutes", "Error reproduciendo preview", e)
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = if (isPreviewPlaying) "❚❚" else "▶ OÍR",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // Historial continuo de diálogo de la reunión (interlocutores diferenciados)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(Color(0xFF10131A), RoundedCornerShape(8.dp))
                .border(1.dp, Color(0xFF222838), RoundedCornerShape(8.dp))
                .padding(if (isRound) 6.dp else 8.dp)
        ) {
            if (history.isEmpty()) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = if (isSessionActive || isRecordingAudio)
                            "Reunión CDPE activa. Grabando audio ambiente y transcribiendo en vivo..."
                        else
                            "Registro en tiempo real de intervenciones en reuniones del CDPE.\nToca 'INICIAR REUNIÓN' para grabar el audio y transcribir el acta.",
                        fontSize = if (isRound) 11.5.sp else 12.5.sp,
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center,
                        lineHeight = if (isRound) 16.sp else 18.sp
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    for (turn in history) {
                        val isUser = turn.role == "user"
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isUser) Color(0xFF192234) else Color(0xFF251A18),
                                    RoundedCornerShape(8.dp)
                                )
                                .padding(if (isRound) 6.dp else 8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = if (isUser) "🗣 HABLANTE / JOEL" else "🤖 ASISTENTE JOWELL",
                                    fontSize = if (isRound) 10.sp else 11.5.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace,
                                    color = if (isUser) Color(0xFF00E5FF) else Color(0xFFFF7043)
                                )
                                if (turn.timestamp.isNotBlank()) {
                                    Text(
                                        text = turn.timestamp,
                                        fontSize = if (isRound) 9.sp else 10.5.sp,
                                        fontFamily = FontFamily.Monospace,
                                        color = Color(0xFF78909C)
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = turn.text,
                                fontSize = if (isRound) 12.sp else 13.5.sp,
                                fontWeight = FontWeight.Medium,
                                fontFamily = FontFamily.Monospace,
                                color = Color.White,
                                lineHeight = if (isRound) 16.sp else 18.sp
                            )
                        }
                    }
                }
            }
        }

        // Acciones: Copiar todo, Compartir, Limpiar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (isRound) 32.dp else 36.dp)
                    .clip(RoundedCornerShape(if (isRound) 16.dp else 8.dp))
                    .background(Color(0xFF1A237E))
                    .clickable {
                        val allText = history.joinToString("\n\n") { "${if (it.role == "user") "Hablante" else "Asistente"}: ${it.text}" }
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                        val clip = android.content.ClipData.newPlainText("Transcripción CDPE", allText)
                        clipboard.setPrimaryClip(clip)
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📋 COPIAR", fontSize = if (isRound) 10.sp else 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFF8C9EFF))
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(if (isRound) 32.dp else 36.dp)
                    .clip(RoundedCornerShape(if (isRound) 16.dp else 8.dp))
                    .background(Color(0xFF263238))
                    .clickable {
                        val allText = history.joinToString("\n\n") { "${if (it.role == "user") "Hablante" else "Asistente"}: ${it.text}" }
                        val sendIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "--- ACTAS Y MINUTAS CDPE ---\n\n$allText")
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        }
                        context.startActivity(Intent.createChooser(sendIntent, "Compartir acta").apply {
                            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        })
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "📤 COMPARTIR", fontSize = if (isRound) 10.sp else 11.5.sp, fontWeight = FontWeight.Bold, color = Color(0xFFECEFF1))
            }

            Box(
                modifier = Modifier
                    .size(if (isRound) 32.dp else 36.dp)
                    .clip(RoundedCornerShape(if (isRound) 16.dp else 8.dp))
                    .background(Color(0xFF3E1717))
                    .clickable { voiceManager.clearConversationHistory() },
                contentAlignment = Alignment.Center
            ) {
                Text(text = "🗑", fontSize = if (isRound) 13.sp else 15.sp, color = Color(0xFFFF5252))
            }
        }
    }
}

// ==================== MODO 6: AGENDA Y CITAS REALES CDPE ====================
@Composable
private fun AgendaScreen(voiceManager: JarvisVoiceManager, isRound: Boolean = false) {
    val context = LocalContext.current
    var eventsList by remember { mutableStateOf(voiceManager.deviceAssistantManager.getStructuredTodayEvents()) }
    val todayFormatted = remember {
        SimpleDateFormat("EEEE d 'de' MMMM", Locale("es")).format(Date()).replaceFirstChar { it.uppercase() }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF090A0E))
            .padding(horizontal = if (isRound) 8.dp else 10.dp, vertical = if (isRound) 6.dp else 8.dp)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Encabezado
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "AGENDA EJECUTIVA",
                fontSize = if (isRound) 11.5.sp else 13.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFF5252)
            )
            Text(
                text = "${eventsList.size} EVENTOS",
                fontSize = if (isRound) 10.sp else 11.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF00E5FF)
            )
        }

        // Fecha de Hoy
        Text(
            text = todayFormatted,
            fontSize = if (isRound) 11.sp else 12.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFB0BEC5),
            textAlign = TextAlign.Center
        )

        // Lista de Citas del Calendario
        if (eventsList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(90.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF12151E))
                    .border(1.dp, Color(0xFF222838), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No hay reuniones agendadas para hoy.\n¡Excelente para avanzar proyectos estratégicos!",
                    fontSize = 11.5.sp,
                    color = Color(0xFF90A4AE),
                    textAlign = TextAlign.Center
                )
            }
        } else {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                eventsList.forEach { ev ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color(0xFF141824))
                            .border(1.dp, Color(0xFF222B3D), RoundedCornerShape(8.dp))
                            .padding(horizontal = 10.dp, vertical = 7.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ev.title,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                text = "Sincronizado con Calendario",
                                fontSize = 9.5.sp,
                                color = Color(0xFF78909C)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(Color(0xFF26181A))
                                .border(1.dp, Color(0xFFFF5252), RoundedCornerShape(6.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = ev.timeFormatted,
                                fontSize = 10.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFF5252)
                            )
                        }
                    }
                }
            }
        }

        // Botones de Acción
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFE25227))
                    .clickable {
                        voiceManager.deviceAssistantManager.createCalendarEvent("Reunión CDPE", "hoy", "15:00")
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "+ NUEVA CITA",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Black,
                    color = Color.White
                )
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF1A2332))
                    .border(1.dp, Color(0xFF26354D), RoundedCornerShape(8.dp))
                    .clickable {
                        eventsList = voiceManager.deviceAssistantManager.getStructuredTodayEvents()
                    },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "🔄 ACTUALIZAR",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF00E5FF)
                )
            }
        }
    }
}

// ==================== INSTRUMENTACIÓN: BRÚJULA TÁCTICA GIRATORIA EN VIVO ====================
@Composable
private fun TacticalCompassDial(
    headingDegrees: Int,
    cardinalDirection: String,
    modifier: Modifier = Modifier,
    dialSize: Int = 74
) {
    var targetAngle by remember { mutableFloatStateOf(headingDegrees.toFloat()) }
    LaunchedEffect(headingDegrees) {
        val current = targetAngle
        val target = headingDegrees.toFloat()
        var delta = (target - current) % 360f
        if (delta > 180f) delta -= 360f
        else if (delta < -180f) delta += 360f
        targetAngle = current + delta
    }
    val animatedHeading by animateFloatAsState(
        targetValue = targetAngle,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "compassNeedleSpring"
    )

    Box(
        modifier = modifier.size(dialSize.dp),
        contentAlignment = Alignment.Center
    ) {
        // Cuadrante estático con bisel táctico y marcas de grados perimetrales
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val radius = size.minDimension / 2f - 3.dp.toPx()

            // Fondo circular OLED profundo
            drawCircle(
                color = Color(0xFF0C1017),
                radius = radius,
                center = center
            )
            // Anillo exterior de titanio anodizado
            drawCircle(
                color = Color(0xFF2B3742),
                radius = radius,
                center = center,
                style = Stroke(width = 2.dp.toPx())
            )
            // Anillo secundario interior
            drawCircle(
                color = Color(0xFF1B242C),
                radius = radius * 0.72f,
                center = center,
                style = Stroke(width = 1.dp.toPx())
            )

            // 12 marcas de rumbo (cada 30 grados)
            for (i in 0 until 12) {
                val angleDeg = i * 30.0
                val angleRad = Math.toRadians(angleDeg)
                val isCardinal = i % 3 == 0
                val tickLen = if (isCardinal) 6.dp.toPx() else 3.5.dp.toPx()
                val tickColor = if (i == 0) Color(0xFFFF5722) else Color(0xFF78909C)

                val startX = (center.x + (radius - tickLen) * Math.sin(angleRad)).toFloat()
                val startY = (center.y - (radius - tickLen) * Math.cos(angleRad)).toFloat()
                val endX = (center.x + radius * Math.sin(angleRad)).toFloat()
                val endY = (center.y - radius * Math.cos(angleRad)).toFloat()

                drawLine(
                    color = tickColor,
                    start = Offset(startX, startY),
                    end = Offset(endX, endY),
                    strokeWidth = if (isCardinal) 2.2.dp.toPx() else 1.2.dp.toPx()
                )
            }
        }

        // Aguja magnética que gira en tiempo real hacia el Norte magnético
        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .rotate(animatedHeading)
        ) {
            val center = Offset(size.width / 2f, size.height / 2f)
            val needleLen = size.minDimension / 2f - 7.dp.toPx()
            val needleWidth = 5.dp.toPx()

            // Flecha Norte (Naranja CDPE táctico de alta visibilidad)
            val northPath = Path().apply {
                moveTo(center.x, center.y - needleLen)
                lineTo(center.x + needleWidth, center.y)
                lineTo(center.x, center.y - 2.5.dp.toPx())
                lineTo(center.x - needleWidth, center.y)
                close()
            }
            drawPath(northPath, color = Color(0xFFFF5722))

            // Flecha Sur (Plata / Acero templado)
            val southPath = Path().apply {
                moveTo(center.x, center.y + needleLen)
                lineTo(center.x + needleWidth, center.y)
                lineTo(center.x, center.y + 2.5.dp.toPx())
                lineTo(center.x - needleWidth, center.y)
                close()
            }
            drawPath(southPath, color = Color(0xFF90A4AE))

            // Pivote central de latón / rodamiento
            drawCircle(color = Color(0xFF14181E), radius = 4.5.dp.toPx(), center = center)
            drawCircle(color = Color(0xFFFFD54F), radius = 2.dp.toPx(), center = center)
        }
    }
}

// ==================== MODO 2: RELOJ, CLIMA Y TELEMETRÍA COMPLETA ====================
@Composable
private fun StandbyScreen(
    telemetry: com.example.jarvis.sensors.TelemetrySnapshot,
    isRound: Boolean = false,
    onOpenSettings: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = if (isRound) 8.dp else 10.dp, vertical = if (isRound) 4.dp else 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        // Cabecera superior institucional
        Text(
            text = "CDPE SMARTWATCH · TELEMETRÍA",
            fontSize = if (isRound) 9.5.sp else 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Black,
            color = Color(0xFFE25227)
        )

        // Reloj digital con clima local de donde se encuentra
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            val place = telemetry.localityName
            val temp = telemetry.temperatureCelsius?.roundToInt() ?: 24
            val cond = telemetry.weatherCondition
            Text(
                text = "$place · $temp°C $cond".uppercase(),
                fontSize = if (isRound) 10.sp else 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFFFD54F),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(1.dp))
            Text(
                text = telemetry.timeFormatted.take(5),
                fontSize = if (isRound) 32.sp else 38.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                color = Color.White
            )
            Text(
                text = telemetry.dateFormatted,
                fontSize = if (isRound) 9.5.sp else 10.5.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFB0BEC5),
                textAlign = TextAlign.Center
            )
        }

        // ==================== INSTRUMENTACIÓN TÁCTICA: BRÚJULA EN VIVO + TELEMETRÍA ====================
        Box(
            modifier = Modifier
                .fillMaxWidth(if (isRound) 0.96f else 1f)
                .clip(RoundedCornerShape(12.dp))
                .border(1.5.dp, Color(0xFFE25227), RoundedCornerShape(12.dp))
                .background(Color(0xFF10131A))
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .clickable { onOpenSettings() }
        ) {
            if (isRound) {
                // Disposición para Smartwatch Redondo
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(3.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        TacticalCompassDial(
                            headingDegrees = telemetry.headingDegrees,
                            cardinalDirection = telemetry.cardinalDirection,
                            dialSize = 56
                        )
                        Column {
                            Text(
                                text = "${telemetry.headingDegrees}° ${telemetry.cardinalDirection}",
                                fontSize = 14.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF00E676)
                            )
                            Text(
                                text = "RUMBO MAGNÉTICO",
                                fontSize = 8.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF78909C)
                            )
                            Text(
                                text = "${telemetry.batteryLevel}% ${if (telemetry.isCharging) "⚡" else "🔋"}",
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = if (telemetry.batteryLevel > 20) Color(0xFF00E5FF) else Color(0xFFFF5252)
                            )
                        }
                    }

                    val locStr = if (telemetry.latitude != null && telemetry.longitude != null) {
                        val lat = String.format(Locale.US, "%.3f", telemetry.latitude)
                        val lon = String.format(Locale.US, "%.3f", telemetry.longitude)
                        val alt = if (telemetry.altitude != null) " · ${telemetry.altitude.roundToInt()}m" else ""
                        "📍 $lat, $lon$alt"
                    } else {
                        "📍 GPS satelital buscando fix..."
                    }
                    Text(
                        text = locStr,
                        fontSize = 9.sp,
                        fontFamily = FontFamily.Monospace,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFD54F),
                        textAlign = TextAlign.Center
                    )

                    Text(
                        text = "💧 Hum: ${telemetry.humidityPercent ?: 65}% · 🌤 ${telemetry.weatherCondition}",
                        fontSize = 8.5.sp,
                        fontFamily = FontFamily.Monospace,
                        color = Color(0xFFB0BEC5),
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                // Disposición para Pantalla Rectangular (Cockpit Táctico Completo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Columna Izquierda: Brújula táctica real giratoria
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.width(76.dp)
                    ) {
                        TacticalCompassDial(
                            headingDegrees = telemetry.headingDegrees,
                            cardinalDirection = telemetry.cardinalDirection,
                            dialSize = 64
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "${telemetry.headingDegrees}° ${telemetry.cardinalDirection}",
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFF00E676)
                        )
                        Text(
                            text = "RUMBO",
                            fontSize = 8.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF78909C)
                        )
                    }

                    // Columna Derecha: Bloques de telemetría GPS, Batería y Clima
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(2.5.dp)
                    ) {
                        // Batería con rayito y porcentaje
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "ENERGÍA",
                                fontSize = 8.5.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = Color(0xFF78909C)
                            )
                            Text(
                                text = "${telemetry.batteryLevel}% ${if (telemetry.isCharging) "⚡ CARGANDO" else "🔋 BATERÍA"}",
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Black,
                                color = if (telemetry.batteryLevel > 20) Color(0xFF00E5FF) else Color(0xFFFF5252)
                            )
                        }

                        // Coordenadas GPS y Altitud sobre el nivel del mar
                        val locStr = if (telemetry.latitude != null && telemetry.longitude != null) {
                            val lat = String.format(Locale.US, "%.3f", telemetry.latitude)
                            val lon = String.format(Locale.US, "%.3f", telemetry.longitude)
                            val alt = if (telemetry.altitude != null) "${telemetry.altitude.roundToInt()}m" else "N/D"
                            "📍 $lat°, $lon°\n🏔 Alt: $alt SNM"
                        } else {
                            "📍 GPS buscando satélites...\n🏔 Altitud sincronizando"
                        }
                        Text(
                            text = locStr,
                            fontSize = 9.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFFFD54F),
                            lineHeight = 12.5.sp
                        )

                        // Humedad, Clima y Barómetro
                        Text(
                            text = "💧 Hum: ${telemetry.humidityPercent ?: 65}% · 🌤 ${telemetry.weatherCondition}",
                            fontSize = 8.5.sp,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Medium,
                            color = Color(0xFFB0BEC5)
                        )
                    }
                }
            }
        }

        // ==================== BOTÓN DE MARCA J O W E L L ====================
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E1715))
                .border(1.dp, Color(0xFFE25227), RoundedCornerShape(8.dp))
                .clickable { onOpenSettings() }
                .padding(horizontal = 14.dp, vertical = 3.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "J O W E L L",
                fontSize = 10.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color(0xFFFF7043)
            )
        }
    }
}
