package com.example.jarvis.biometrics

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.sqrt

data class VoiceProfile(
    val isCalibrated: Boolean,
    val fundamentalFreqHz: Float, // Frecuencia fundamental media (F0 en Hz)
    val pitchMinHz: Float,
    val pitchMaxHz: Float,
    val baselineRmsEnergy: Float,
    val lastCalibrationDate: String,
    val voiceTypeLabel: String // ej. "Barítono Táctico (Joel)"
)

class VoiceBiometricManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("joel_voice_biometrics_prefs", Context.MODE_PRIVATE)

    private val _voiceProfile = MutableStateFlow(loadVoiceProfile())
    val voiceProfile = _voiceProfile.asStateFlow()

    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating = _isCalibrating.asStateFlow()

    private fun loadVoiceProfile(): VoiceProfile {
        val calibrated = prefs.getBoolean("is_calibrated", false)
        val f0 = prefs.getFloat("fundamental_f0", 125.0f) // Barítono masculino de referencia
        val minHz = prefs.getFloat("pitch_min", 90.0f)
        val maxHz = prefs.getFloat("pitch_max", 165.0f)
        val energy = prefs.getFloat("energy_rms", 1800.0f)
        val date = prefs.getString("calib_date", "Sin calibrar") ?: "Sin calibrar"
        val label = prefs.getString("voice_label", if (calibrated) "Barítono Calibrado (Director Joel)" else "Perfil No Calibrado")
            ?: "Perfil No Calibrado"

        return VoiceProfile(
            isCalibrated = calibrated,
            fundamentalFreqHz = f0,
            pitchMinHz = minHz,
            pitchMaxHz = maxHz,
            baselineRmsEnergy = energy,
            lastCalibrationDate = date,
            voiceTypeLabel = label
        )
    }

    /**
     * Calibra la tesitura y frecuencia fundamental F0 a partir de un búfer de audio PCM de 16-bit.
     */
    fun calibrateFromPcmBuffer(pcmData: ShortArray, sampleRate: Int = 16000): VoiceProfile {
        var sumSquares = 0.0
        for (sample in pcmData) {
            sumSquares += (sample * sample)
        }
        val rms = sqrt(sumSquares / pcmData.size.toDouble()).toFloat()

        // Estimación de F0 mediante autocorrelación simplificada en rango vocal humano (70Hz a 300Hz)
        val minLag = (sampleRate / 300).coerceAtLeast(1)
        val maxLag = (sampleRate / 70).coerceAtMost(pcmData.size / 2)

        var bestLag = -1
        var maxCorr = Double.NEGATIVE_INFINITY

        for (lag in minLag..maxLag) {
            var corr = 0.0
            for (i in 0 until (pcmData.size - lag)) {
                corr += (pcmData[i].toDouble() * pcmData[i + lag].toDouble())
            }
            if (corr > maxCorr) {
                maxCorr = corr
                bestLag = lag
            }
        }

        val estimatedF0 = if (bestLag > 0) {
            (sampleRate.toFloat() / bestLag).coerceIn(80.0f, 240.0f)
        } else {
            125.0f // Fallback de frecuencia barítono adulto
        }

        val minHz = (estimatedF0 * 0.75f).coerceAtLeast(75.0f)
        val maxHz = (estimatedF0 * 1.35f).coerceAtMost(280.0f)

        val voiceClass = when {
            estimatedF0 < 110f -> "Bajo Profundo (Director Joel)"
            estimatedF0 in 110f..145f -> "Barítono Táctico (Director Joel)"
            estimatedF0 in 145f..180f -> "Tenor Firme (Director Joel)"
            else -> "Voz Aguda / Mixta"
        }

        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es")).format(Date())

        val newProfile = VoiceProfile(
            isCalibrated = true,
            fundamentalFreqHz = estimatedF0,
            pitchMinHz = minHz,
            pitchMaxHz = maxHz,
            baselineRmsEnergy = rms.coerceAtLeast(500f),
            lastCalibrationDate = dateStr,
            voiceTypeLabel = voiceClass
        )

        prefs.edit()
            .putBoolean("is_calibrated", true)
            .putFloat("fundamental_f0", newProfile.fundamentalFreqHz)
            .putFloat("pitch_min", newProfile.pitchMinHz)
            .putFloat("pitch_max", newProfile.pitchMaxHz)
            .putFloat("energy_rms", newProfile.baselineRmsEnergy)
            .putString("calib_date", newProfile.lastCalibrationDate)
            .putString("voice_label", newProfile.voiceTypeLabel)
            .apply()

        _voiceProfile.value = newProfile
        Log.d("VoiceBiometrics", "Calibración completada: F0=$estimatedF0 Hz, Clase=$voiceClass, RMS=$rms")
        return newProfile
    }

    /**
     * Simula o realiza calibración instantánea con preset auditivo para Director Joel
     * cuando no hay micrófono en vivo durante la invocación en pruebas.
     */
    fun calibrateWithPreset(f0Hz: Float = 124.5f): VoiceProfile {
        val dateStr = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("es")).format(Date())
        val profile = VoiceProfile(
            isCalibrated = true,
            fundamentalFreqHz = f0Hz,
            pitchMinHz = 92.0f,
            pitchMaxHz = 160.0f,
            baselineRmsEnergy = 2100.0f,
            lastCalibrationDate = dateStr,
            voiceTypeLabel = "Barítono Táctico (Director Joel - Prioridad Máxima)"
        )
        prefs.edit()
            .putBoolean("is_calibrated", true)
            .putFloat("fundamental_f0", profile.fundamentalFreqHz)
            .putFloat("pitch_min", profile.pitchMinHz)
            .putFloat("pitch_max", profile.pitchMaxHz)
            .putFloat("energy_rms", profile.baselineRmsEnergy)
            .putString("calib_date", profile.lastCalibrationDate)
            .putString("voice_label", profile.voiceTypeLabel)
            .apply()

        _voiceProfile.value = profile
        return profile
    }

    /**
     * Evalúa si un fragmento acústico corresponde a la tesitura del Director Joel
     * para filtrar ruido de fondo y voces no autorizadas en ambientes ruidosos.
     */
    fun evaluateSpeakerMatch(estimatedF0: Float, energyRms: Float): Pair<Boolean, Float> {
        val profile = _voiceProfile.value
        if (!profile.isCalibrated) return Pair(true, 1.0f) // Sin calibrar no filtra

        val pitchDiff = abs(estimatedF0 - profile.fundamentalFreqHz)
        val inPitchWindow = estimatedF0 in profile.pitchMinHz..profile.pitchMaxHz
        val energyRatio = (energyRms / profile.baselineRmsEnergy).coerceIn(0.1f, 3.0f)

        val score = when {
            inPitchWindow && energyRatio > 0.4f -> 0.95f - (pitchDiff / 100f).coerceAtMost(0.3f)
            inPitchWindow -> 0.70f
            else -> 0.25f
        }

        val isJoel = score >= 0.65f
        return Pair(isJoel, score)
    }

    fun resetCalibration() {
        prefs.edit().clear().apply()
        _voiceProfile.value = loadVoiceProfile()
    }
}
