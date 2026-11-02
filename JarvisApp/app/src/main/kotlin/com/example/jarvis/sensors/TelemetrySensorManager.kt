package com.example.jarvis.sensors

import android.Manifest
import android.annotation.SuppressLint
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.BatteryManager
import android.os.Bundle
import android.os.Looper
import androidx.core.content.ContextCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.math.roundToInt

data class TelemetrySnapshot(
    val timeFormatted: String = "12:00:00",
    val dateFormatted: String = "Lunes, 2 de Noviembre de 2026",
    val dateShort: String = "02/11/2026",
    val latitude: Double? = null,
    val longitude: Double? = null,
    val altitude: Double? = null,
    val gpsAccuracy: Float? = null,
    val gpsProvider: String = "Iniciando",
    val isGpsActive: Boolean = false,
    val headingDegrees: Int = 0,
    val cardinalDirection: String = "N",
    val batteryLevel: Int = 100,
    val isCharging: Boolean = false,
    val batteryTempCelsius: Float? = null,
    val lightLux: Float? = null,
    val pressureHpa: Float? = null,
    val accelerationMagnitude: Float = 9.8f,
    val temperatureCelsius: Float? = 23f,
    val humidityPercent: Int? = 65,
    val weatherCondition: String = "Despejado ☀️",
    val localityName: String = "Ubicación actual"
)

/**
 * Gestor Integral de Telemetría de Dispositivo:
 * Hora, Fecha, GPS (Latitud, Longitud, Altitud), Brújula / Grados de Orientación
 * y Sensores de Hardware (Acelerómetro, Luz, Presión, Batería).
 */
class TelemetrySensorManager(private val context: Context) : SensorEventListener, LocationListener {

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as? SensorManager
    private val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager

    private val _telemetry = MutableStateFlow(TelemetrySnapshot())
    val telemetry: StateFlow<TelemetrySnapshot> = _telemetry.asStateFlow()

    // Sensores de Orientación / Brújula
    private val accelerometerReading = FloatArray(3)
    private val magnetometerReading = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientationAngles = FloatArray(3)
    private var hasAccelerometer = false
    private var hasMagnetometer = false

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()
    private var lastWeatherFetchTime = 0L
    private var lastWeatherLat = 0.0
    private var lastWeatherLon = 0.0

    private val batteryReceiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            if (intent?.action == Intent.ACTION_BATTERY_CHANGED) {
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val tempRaw = intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, -1)

                val pct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
                val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
                val tempC = if (tempRaw > 0) tempRaw / 10.0f else null

                _telemetry.value = _telemetry.value.copy(
                    batteryLevel = pct,
                    isCharging = charging,
                    batteryTempCelsius = tempC
                )
            }
        }
    }

    init {
        updateTimeAndDate()
        startSensorMonitoring()
        startLocationMonitoring()
        try {
            val filter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            context.registerReceiver(batteryReceiver, filter)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            val lastLoc = locationManager?.getLastKnownLocation(LocationManager.GPS_PROVIDER)
                ?: locationManager?.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            if (lastLoc != null) {
                _telemetry.value = _telemetry.value.copy(
                    latitude = lastLoc.latitude,
                    longitude = lastLoc.longitude,
                    isGpsActive = true
                )
                checkAndFetchWeather(lastLoc.latitude, lastLoc.longitude)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun updateTimeAndDate() {
        val now = Date()
        val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
        val dateFormat = SimpleDateFormat("EEEE, d 'de' MMMM 'de' yyyy", Locale("es", "GT"))
        val dateShortFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        val timeStr = timeFormat.format(now)
        val dateStr = dateFormat.format(now).replaceFirstChar { it.uppercase() }
        val dateShortStr = dateShortFormat.format(now)

        _telemetry.value = _telemetry.value.copy(
            timeFormatted = timeStr,
            dateFormatted = dateStr,
            dateShort = dateShortStr
        )
    }

    private fun startSensorMonitoring() {
        val sm = sensorManager ?: return

        // 1. Acelerómetro
        sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            hasAccelerometer = true
        }

        // 2. Campo Magnético (Brújula)
        sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
            hasMagnetometer = true
        }

        // 3. Vector de Rotación (si está disponible, es más preciso)
        sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_UI)
        }

        // 4. Sensor de Luz
        sm.getDefaultSensor(Sensor.TYPE_LIGHT)?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }

        // 5. Sensor de Presión / Barómetro
        sm.getDefaultSensor(Sensor.TYPE_PRESSURE)?.let {
            sm.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
        }
    }

    @SuppressLint("MissingPermission")
    fun startLocationMonitoring() {
        val lm = locationManager ?: return
        val hasFine = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        val hasCoarse = ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

        if (!hasFine && !hasCoarse) {
            _telemetry.value = _telemetry.value.copy(gpsProvider = "Permiso requerido", isGpsActive = false)
            return
        }

        try {
            // Intentar obtener última ubicación conocida inmediatamente
            val lastGps = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            val lastNet = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            val bestLast = when {
                lastGps != null && lastNet != null -> if (lastGps.time >= lastNet.time) lastGps else lastNet
                lastGps != null -> lastGps
                else -> lastNet
            }
            bestLast?.let { onLocationChanged(it) }

            // Registrar oyentes para actualizaciones continuas
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 5000L, 2.0f, this, Looper.getMainLooper())
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000L, 5.0f, this, Looper.getMainLooper())
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onSensorChanged(event: SensorEvent?) {
        if (event == null) return

        when (event.sensor.type) {
            Sensor.TYPE_ROTATION_VECTOR -> {
                SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values)
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthRad = orientationAngles[0]
                var degrees = Math.toDegrees(azimuthRad.toDouble()).roundToInt()
                if (degrees < 0) degrees += 360
                val cardinal = getCardinal(degrees)
                _telemetry.value = _telemetry.value.copy(
                    headingDegrees = degrees,
                    cardinalDirection = cardinal
                )
            }

            Sensor.TYPE_ACCELEROMETER -> {
                System.arraycopy(event.values, 0, accelerometerReading, 0, 3)
                val mag = kotlin.math.sqrt(
                    event.values[0] * event.values[0] +
                    event.values[1] * event.values[1] +
                    event.values[2] * event.values[2]
                )
                _telemetry.value = _telemetry.value.copy(accelerationMagnitude = mag)
                updateOrientationFromAccMag()
            }

            Sensor.TYPE_MAGNETIC_FIELD -> {
                System.arraycopy(event.values, 0, magnetometerReading, 0, 3)
                updateOrientationFromAccMag()
            }

            Sensor.TYPE_LIGHT -> {
                _telemetry.value = _telemetry.value.copy(lightLux = event.values[0])
            }

            Sensor.TYPE_PRESSURE -> {
                _telemetry.value = _telemetry.value.copy(pressureHpa = event.values[0])
            }
        }
    }

    private fun updateOrientationFromAccMag() {
        if (hasAccelerometer && hasMagnetometer) {
            val success = SensorManager.getRotationMatrix(
                rotationMatrix,
                null,
                accelerometerReading,
                magnetometerReading
            )
            if (success) {
                SensorManager.getOrientation(rotationMatrix, orientationAngles)
                val azimuthRad = orientationAngles[0]
                var degrees = Math.toDegrees(azimuthRad.toDouble()).roundToInt()
                if (degrees < 0) degrees += 360
                val cardinal = getCardinal(degrees)
                _telemetry.value = _telemetry.value.copy(
                    headingDegrees = degrees,
                    cardinalDirection = cardinal
                )
            }
        }
    }

    private fun getCardinal(deg: Int): String {
        return when (deg) {
            in 338..360, in 0..22 -> "N"
            in 23..67 -> "NE"
            in 68..112 -> "E"
            in 113..157 -> "SE"
            in 158..202 -> "S"
            in 203..247 -> "SO"
            in 248..292 -> "O"
            in 293..337 -> "NO"
            else -> "N"
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}

    override fun onLocationChanged(location: Location) {
        _telemetry.value = _telemetry.value.copy(
            latitude = location.latitude,
            longitude = location.longitude,
            altitude = if (location.hasAltitude()) location.altitude else null,
            gpsAccuracy = if (location.hasAccuracy()) location.accuracy else null,
            gpsProvider = location.provider ?: "GPS",
            isGpsActive = true
        )
        checkAndFetchWeather(location.latitude, location.longitude)
    }

    private fun checkAndFetchWeather(lat: Double, lon: Double) {
        val now = System.currentTimeMillis()
        val distMoved = kotlin.math.abs(lat - lastWeatherLat) + kotlin.math.abs(lon - lastWeatherLon)
        if (now - lastWeatherFetchTime > 15 * 60 * 1000L || distMoved > 0.015 || _telemetry.value.localityName == "Ubicación actual") {
            lastWeatherFetchTime = now
            lastWeatherLat = lat
            lastWeatherLon = lon
            scope.launch {
                fetchWeatherAndLocality(lat, lon)
            }
        }
    }

    private fun fetchWeatherAndLocality(lat: Double, lon: Double) {
        var resolvedName = _telemetry.value.localityName
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            @Suppress("DEPRECATION")
            val addresses = geocoder.getFromLocation(lat, lon, 1)
            val addr = addresses?.firstOrNull()
            val name = addr?.locality ?: addr?.subAdminArea ?: addr?.adminArea ?: addr?.featureName
            if (!name.isNullOrBlank()) {
                resolvedName = name
            }
        } catch (e: Exception) {
            // Geocoder offline fallback
        }

        var tempC = _telemetry.value.temperatureCelsius
        var humidity = _telemetry.value.humidityPercent
        var condition = _telemetry.value.weatherCondition

        try {
            val url = "https://api.open-meteo.com/v1/forecast?latitude=$lat&longitude=$lon&current=temperature_2m,relative_humidity_2m,weather_code"
            val request = Request.Builder().url(url).build()
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: ""
                val root = JSONObject(bodyStr)
                val current = root.optJSONObject("current")
                if (current != null) {
                    tempC = current.optDouble("temperature_2m", 24.0).toFloat()
                    humidity = current.optInt("relative_humidity_2m", 65)
                    val code = current.optInt("weather_code", 0)
                    condition = parseWeatherCode(code)
                }
            }
        } catch (e: Exception) {
            // Fallback offline
        }

        _telemetry.value = _telemetry.value.copy(
            localityName = resolvedName,
            temperatureCelsius = tempC,
            humidityPercent = humidity,
            weatherCondition = condition
        )
    }

    private fun parseWeatherCode(code: Int): String {
        return when (code) {
            0 -> "Despejado ☀️"
            1, 2, 3 -> "Parcialmente nublado ⛅"
            45, 48 -> "Niebla 🌫️"
            51, 53, 55 -> "Llovizna 🌦️"
            61, 63, 65 -> "Lluvia 🌧️"
            80, 81, 82 -> "Chubascos 🌧️"
            95, 96, 99 -> "Tormenta eléctrica ⛈️"
            else -> "Templado 🌤️"
        }
    }

    override fun onProviderEnabled(provider: String) {
        _telemetry.value = _telemetry.value.copy(gpsProvider = "$provider activo", isGpsActive = true)
    }

    override fun onProviderDisabled(provider: String) {
        _telemetry.value = _telemetry.value.copy(gpsProvider = "$provider desactivado", isGpsActive = false)
    }

    @Deprecated("Deprecated in Java")
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    /**
     * Resumen hablado completo de la telemetría para el asistente de voz
     */
    fun getTelemetrySpokenReport(): String {
        updateTimeAndDate()
        val data = _telemetry.value

        val locationText = if (data.latitude != null && data.longitude != null) {
            val latStr = String.format(Locale.US, "%.4f", data.latitude)
            val lonStr = String.format(Locale.US, "%.4f", data.longitude)
            val altText = if (data.altitude != null) ", a ${data.altitude.roundToInt()} metros de altitud" else ""
            "Coordenadas GPS: Latitud $latStr°, Longitud $lonStr°$altText"
        } else {
            "Buscando señal satelital de GPS"
        }

        val headingText = "Orientación de la brújula: ${data.headingDegrees} grados hacia el ${data.cardinalDirection}"
        val batteryText = "Batería: ${data.batteryLevel}% ${if (data.isCharging) "(conectada a la corriente)" else ""}"
        val weatherText = "Clima en ${data.localityName}: ${data.temperatureCelsius?.roundToInt() ?: 24}°C (${data.weatherCondition}), humedad ${data.humidityPercent ?: 65}%"

        return "Son las ${data.timeFormatted} del ${data.dateFormatted}. $locationText. $weatherText. $headingText. $batteryText."
    }

    /**
     * Contexto estructurado en vivo para inyectar en Gemini
     */
    fun getTelemetryContextForGemini(): String {
        updateTimeAndDate()
        val data = _telemetry.value

        val latStr = data.latitude?.let { String.format(Locale.US, "%.5f°", it) } ?: "No disponible aún"
        val lonStr = data.longitude?.let { String.format(Locale.US, "%.5f°", it) } ?: "No disponible aún"
        val altStr = data.altitude?.let { "${it.roundToInt()} metros" } ?: "N/D"
        val accStr = data.gpsAccuracy?.let { "±${it.roundToInt()}m" } ?: "N/D"

        return """
            TELEMETRÍA EN TIEMPO REAL DEL DISPOSITIVO / SMARTWATCH:
            - Hora exacta: ${data.timeFormatted}
            - Fecha oficial: ${data.dateFormatted} (${data.dateShort})
            - Clima y Temperatura local actual: ${data.localityName} ${data.temperatureCelsius?.roundToInt() ?: 24}°C (${data.weatherCondition}), Humedad ${data.humidityPercent ?: 65}%
            - Brújula / Grados de orientación: ${data.headingDegrees}° (Rumbo ${data.cardinalDirection})
            - Latitud GPS: $latStr
            - Longitud GPS: $lonStr
            - Altitud sobre el nivel del mar: $altStr (Precisión: $accStr)
            - Proveedor de ubicación: ${data.gpsProvider}
            - Nivel de batería: ${data.batteryLevel}% ${if (data.isCharging) "[Cargando ⚡]" else ""}
            - Sensores físicos activos: Acelerómetro (${String.format(Locale.US, "%.1f", data.accelerationMagnitude)} m/s²)${if (data.lightLux != null) ", Luz (${data.lightLux.roundToInt()} lux)" else ""}${if (data.pressureHpa != null) ", Barómetro (${data.pressureHpa.roundToInt()} hPa)" else ""}
            DIRECTIVA SUPREMA DE TELEMETRÍA:
            - TÚ TIENES ACCESO TOTAL al GPS, clima y temperatura del lugar actual, a la hora, fecha, brújula (grados) y sensores del reloj/teléfono.
            - Cuando el usuario pregunte la hora, fecha, temperatura, clima, humedad, su ubicación, latitud, longitud, grados de la brújula o batería, responde con los datos exactos reportados arriba. ¡NUNCA digas que no tienes acceso al GPS o sensores!
        """.trimIndent()
    }

    fun destroy() {
        sensorManager?.unregisterListener(this)
        locationManager?.removeUpdates(this)
        try {
            context.unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
