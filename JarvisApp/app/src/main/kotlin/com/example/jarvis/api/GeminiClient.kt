package com.example.jarvis.api

import android.util.Base64
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

data class JarvisResponse(
    val spokenSummary: String,
    val clientName: String? = null,
    val agreements: List<String> = emptyList(),
    val calendarEvent: String? = null,
    val isOffline: Boolean = false,
    val rawText: String = ""
)

object GeminiConfig {
    const val DEFAULT_API_KEY = "AIzaSyAgNbCIJuOqX_KjQbYGA95y3m0eHI80AkA"
    const val MODEL_NAME = "gemini-flash-latest"
}

class GeminiClient(private val apiKey: String = GeminiConfig.DEFAULT_API_KEY) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun analyzeAudio(audioFile: File): JarvisResponse = withContext(Dispatchers.IO) {
        if (!audioFile.exists() || audioFile.length() == 0L) {
            return@withContext JarvisResponse(
                spokenSummary = "Audio no detectado.",
                rawText = "Archivo vacío."
            )
        }

        val audioBytes = audioFile.readBytes()
        val base64Audio = Base64.encodeToString(audioBytes, Base64.NO_WRAP)
        val mimeType = when (audioFile.extension.lowercase()) {
            "mp3" -> "audio/mp3"
            "wav" -> "audio/wav"
            "aac" -> "audio/aac"
            else -> "audio/mp4"
        }

        val prompt = """
            Eres el Asistente Ejecutivo JOWELL del CDPE (Centro de Desarrollo y Promoción Empresarial).
            Analiza el audio adjunto de una reunión de trabajo, asamblea, acuerdo con cooperativistas o comando de voz del Director Joel en Guatemala.
            
            Tu objetivo:
            1. Transcribir y comprender los puntos ejecutivos y comerciales clave.
            2. Si es una reunión de trabajo: extrae acuerdos de proyectos, compromisos de asamblea, acuerdos de comisiones y nombres de los participantes o cooperativistas.
            3. Si es una instrucción directa del Director Joel: resume la acción requerida (agendar cita, seguimiento, recordatorio).
            4. Genera un "spoken_summary" en español guatemalteco ejecutivo, ultra conciso (máximo 2 oraciones directas) para ser escuchado al oído por audífonos.
            
            Responde ÚNICAMENTE un JSON válido con esta estructura:
            {
              "spoken_summary": "Resumen conciso y profesional para hablar por voz",
              "client_name": "Nombre del socio o participante si se mencionó, o null",
              "agreements": ["Compromiso 1", "Compromiso 2"],
              "calendar_event": "Título y fecha para la cita si aplica, o null"
            }
        """.trimIndent()

        val jsonBody = JSONObject().apply {
            val contents = JSONArray().apply {
                val contentObj = JSONObject().apply {
                    val parts = JSONArray().apply {
                        put(JSONObject().apply {
                            put("inlineData", JSONObject().apply {
                                put("mimeType", mimeType)
                                put("data", base64Audio)
                            })
                        })
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                    }
                    put("parts", parts)
                }
                put(contentObj)
            }
            put("contents", contents)
            put("generationConfig", JSONObject().apply {
                put("response_mime_type", "application/json")
                put("temperature", 0.2)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/${GeminiConfig.MODEL_NAME}:generateContent?key=$apiKey"
        val request = Request.Builder()
            .url(url)
            .post(jsonBody.toString().toRequestBody("application/json".toMediaType()))
            .build()

        try {
            val response = client.newCall(request).execute()
            val body = response.body?.string() ?: ""
            if (!response.isSuccessful) {
                Log.e("Jarvis", "API Error: HTTP ${response.code} -> $body")
                return@withContext JarvisResponse(
                    spokenSummary = "Error al conectar con los servidores de Jarvis.",
                    rawText = "HTTP ${response.code}: $body"
                )
            }

            val root = JSONObject(body)
            val candidates = root.optJSONArray("candidates")
            val candidate = candidates?.optJSONObject(0)
            val content = candidate?.optJSONObject("content")
            val parts = content?.optJSONArray("parts")
            val responseText = parts?.optJSONObject(0)?.optString("text") ?: ""

            parseJsonResponse(responseText)
        } catch (e: java.net.UnknownHostException) {
            // Sin conexión a Internet
            JarvisResponse(
                spokenSummary = "Sin conexión a internet. La grabación fue guardada localmente.",
                isOffline = true,
                rawText = "Sin señal de red"
            )
        } catch (e: Exception) {
            Log.e("Jarvis", "Error processing audio", e)
            JarvisResponse(
                spokenSummary = "Reunión procesada.",
                rawText = e.localizedMessage ?: "Error desconocido"
            )
        }
    }

    private fun parseJsonResponse(jsonStr: String): JarvisResponse {
        return try {
            val cleanJson = jsonStr.trim().removePrefix("```json").removePrefix("```").removeSuffix("```").trim()
            val obj = JSONObject(cleanJson)
            val summary = obj.optString("spoken_summary", "Reunión registrada con éxito.")
            val client = if (obj.has("client_name") && !obj.isNull("client_name")) obj.getString("client_name") else null
            val agreementsList = mutableListOf<String>()
            val arr = obj.optJSONArray("agreements")
            if (arr != null) {
                for (i in 0 until arr.length()) {
                    agreementsList.add(arr.getString(i))
                }
            }
            val cal = if (obj.has("calendar_event") && !obj.isNull("calendar_event")) obj.getString("calendar_event") else null

            JarvisResponse(
                spokenSummary = summary,
                clientName = client,
                agreements = agreementsList,
                calendarEvent = cal,
                rawText = jsonStr
            )
        } catch (e: Exception) {
            JarvisResponse(
                spokenSummary = "Reunión registrada correctamente.",
                rawText = jsonStr
            )
        }
    }
}
