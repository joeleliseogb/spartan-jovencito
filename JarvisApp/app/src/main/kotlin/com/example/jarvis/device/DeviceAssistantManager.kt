package com.example.jarvis.device

import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentResolver
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.provider.Settings
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

data class ContactRecord(val name: String, val number: String)

data class CalendarEventItem(
    val title: String,
    val timeFormatted: String,
    val startMillis: Long
)

/**
 * Gestor Integral de Asistente de Dispositivo:
 * Contactos, Alarmas, Temporizadores, Agenda / Calendario y Ajustes de Desarrollador / Sistema.
 */
class DeviceAssistantManager(private val context: Context) {

    // ==================== 1. GESTIÓN DE CONTACTOS ====================
    fun searchContacts(query: String): List<ContactRecord> {
        val results = mutableListOf<ContactRecord>()
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) != PackageManager.PERMISSION_GRANTED) {
            return results
        }

        try {
            val resolver: ContentResolver = context.contentResolver
            val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
            val projection = arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            )
            val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ? OR ${ContactsContract.CommonDataKinds.Phone.NUMBER} LIKE ?"
            val selectionArgs = arrayOf("%$query%", "%$query%")

            resolver.query(uri, projection, selection, selectionArgs, null)?.use { cursor ->
                val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val numberIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (cursor.moveToNext() && results.size < 10) {
                    val name = cursor.getString(nameIdx) ?: "Contacto"
                    val number = cursor.getString(numberIdx) ?: ""
                    results.add(ContactRecord(name, number))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return results
    }

    @SuppressLint("MissingPermission")
    fun callContactOrNumber(queryOrNumber: String): String {
        val digits = queryOrNumber.filter { it.isDigit() }
        var targetNumber = if (digits.length >= 7) digits else ""
        var targetName = queryOrNumber

        if (targetNumber.isEmpty()) {
            val contacts = searchContacts(queryOrNumber)
            if (contacts.isNotEmpty()) {
                targetNumber = contacts.first().number
                targetName = contacts.first().name
            }
        }

        if (targetNumber.isEmpty()) {
            targetNumber = queryOrNumber
        }

        val hasCallPermission = ContextCompat.checkSelfPermission(context, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
        val intentAction = if (hasCallPermission) Intent.ACTION_CALL else Intent.ACTION_DIAL

        return try {
            val intent = Intent(intentAction, Uri.parse("tel:$targetNumber")).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Iniciando llamada a $targetName ($targetNumber)..."
        } catch (e: Exception) {
            "No se pudo iniciar la llamada: ${e.message}"
        }
    }

    // ==================== 2. ALARMAS Y TEMPORIZADORES ====================
    fun setAlarm(hour: Int, minute: Int, message: String = "Alarma CDPE"): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
                putExtra(AlarmClock.EXTRA_HOUR, hour)
                putExtra(AlarmClock.EXTRA_MINUTES, minute)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute)
            "¡Listo! Configuré tu alarma para las $timeStr con la nota: \"$message\"."
        } catch (e: Exception) {
            "Error al configurar la alarma: ${e.message}"
        }
    }

    fun setTimer(seconds: Int, message: String = "Temporizador CDPE"): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
                putExtra(AlarmClock.EXTRA_LENGTH, seconds)
                putExtra(AlarmClock.EXTRA_MESSAGE, message)
                putExtra(AlarmClock.EXTRA_SKIP_UI, false)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            val mins = seconds / 60
            val secs = seconds % 60
            val durStr = if (mins > 0) "$mins min ${if (secs > 0) "$secs s" else ""}" else "$secs segundos"
            "¡Listo! Temporizador de $durStr iniciado para: \"$message\"."
        } catch (e: Exception) {
            "Error al configurar el temporizador: ${e.message}"
        }
    }

    fun showAlarms(): String {
        return try {
            val intent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Abriendo el reloj y tus alarmas configuradas."
        } catch (e: Exception) {
            "No se pudieron abrir las alarmas: ${e.message}"
        }
    }

    // ==================== 3. AGENDA Y EVENTOS DE CALENDARIO ====================
    fun createCalendarEvent(title: String, dateDescription: String, timeDescription: String, description: String = ""): String {
        val timeDisplay = if (timeDescription.isNotBlank()) timeDescription else "10:00 AM"
        val fullDateDisplay = "$dateDescription $timeDisplay".trim()

        // Registro inmediato en la base de datos local de eventos de Jowell
        addDirectAgendaEvent(
            title = title,
            timeFormatted = fullDateDisplay,
            dateMillis = System.currentTimeMillis() + 86400000L,
            description = description.ifEmpty { "Compromiso de negocios CDPE" }
        )

        return try {
            val intent = Intent(Intent.ACTION_INSERT).apply {
                data = CalendarContract.Events.CONTENT_URI
                putExtra(CalendarContract.Events.TITLE, title)
                putExtra(CalendarContract.Events.DESCRIPTION, description.ifEmpty { "Evento CDPE" })
                putExtra(CalendarContract.Events.EVENT_LOCATION, "Oficina CDPE / En línea")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "¡Perfecto! Agendé \"$title\" para $fullDateDisplay en tu agenda y calendario."
        } catch (e: Exception) {
            "¡Listo! Agendé \"$title\" para $fullDateDisplay en tu agenda."
        }
    }

    fun getTodayEvents(): String {
        val list = getStructuredTodayEvents()
        if (list.isEmpty()) {
            return "No tienes citas programadas para hoy en tu agenda. ¡Día libre para avanzar en proyectos!"
        }
        val lines = list.take(5).map { "• ${it.title} (${it.timeFormatted})" }
        return "Tienes ${list.size} compromiso(s) en tu agenda:\n" + lines.joinToString("\n")
    }

    private val customEventsPrefs by lazy {
        context.getSharedPreferences("jowell_custom_events", Context.MODE_PRIVATE)
    }

    fun addDirectAgendaEvent(title: String, timeFormatted: String, dateMillis: Long, description: String = "") {
        try {
            val currentRaw = customEventsPrefs.getString("events_json", "[]") ?: "[]"
            val array = org.json.JSONArray(currentRaw)
            val newObj = org.json.JSONObject().apply {
                put("title", title)
                put("timeFormatted", timeFormatted)
                put("startMillis", dateMillis)
                put("description", description)
            }
            array.put(newObj)
            customEventsPrefs.edit().putString("events_json", array.toString()).apply()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_CALENDAR) == PackageManager.PERMISSION_GRANTED) {
            try {
                val values = ContentValues().apply {
                    put(CalendarContract.Events.DTSTART, dateMillis)
                    put(CalendarContract.Events.DTEND, dateMillis + 3600000L)
                    put(CalendarContract.Events.TITLE, title)
                    put(CalendarContract.Events.DESCRIPTION, description)
                    put(CalendarContract.Events.CALENDAR_ID, 1)
                    put(CalendarContract.Events.EVENT_TIMEZONE, TimeZone.getDefault().id)
                }
                context.contentResolver.insert(CalendarContract.Events.CONTENT_URI, values)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun getStructuredTodayEvents(): List<CalendarEventItem> {
        val list = mutableListOf<CalendarEventItem>()

        // 1. Cargar eventos agendados automáticamente por reuniones y cliente
        try {
            val raw = customEventsPrefs.getString("events_json", "[]") ?: "[]"
            val array = org.json.JSONArray(raw)
            for (i in (array.length() - 1) downTo 0) {
                val obj = array.getJSONObject(i)
                val t = obj.optString("title", "Reunión de Negocios CDPE")
                val tf = obj.optString("timeFormatted", "10:00 AM")
                val sm = obj.optLong("startMillis", System.currentTimeMillis())
                list.add(CalendarEventItem(t, tf, sm))
                if (list.size >= 15) break
            }
        } catch (e: Exception) {}

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) != PackageManager.PERMISSION_GRANTED) {
            return list
        }
        try {
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }.timeInMillis

            val endOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 23)
                set(Calendar.MINUTE, 59)
                set(Calendar.SECOND, 59)
            }.timeInMillis

            val uri = CalendarContract.Events.CONTENT_URI
            val projection = arrayOf(
                CalendarContract.Events.TITLE,
                CalendarContract.Events.DTSTART
            )
            val selection = "${CalendarContract.Events.DTSTART} >= ? AND ${CalendarContract.Events.DTSTART} <= ?"
            val selectionArgs = arrayOf(startOfDay.toString(), endOfDay.toString())
            val sortOrder = "${CalendarContract.Events.DTSTART} ASC"

            context.contentResolver.query(uri, projection, selection, selectionArgs, sortOrder)?.use { cursor ->
                val titleIdx = cursor.getColumnIndex(CalendarContract.Events.TITLE)
                val startIdx = cursor.getColumnIndex(CalendarContract.Events.DTSTART)
                val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())

                while (cursor.moveToNext() && list.size < 12) {
                    val title = cursor.getString(titleIdx) ?: "Reunión CDPE"
                    val startMillis = cursor.getLong(startIdx)
                    val timeStr = timeFormat.format(Date(startMillis))
                    list.add(CalendarEventItem(title, timeStr, startMillis))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return list
    }

    // ==================== 4. OPCIONES DE DESARROLLADOR Y SISTEMA ====================
    fun openDeveloperSettings(): String {
        return try {
            val intent = Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Abriendo opciones de desarrollador del sistema."
        } catch (e: Exception) {
            try {
                val fallbackIntent = Intent(Settings.ACTION_DEVICE_INFO_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(fallbackIntent)
                "Abriendo información del dispositivo y ajustes del sistema."
            } catch (ex: Exception) {
                "No fue posible abrir las opciones de desarrollador en este equipo."
            }
        }
    }

    fun openSystemSetting(settingType: String): String {
        val lower = settingType.lowercase()
        val action = when {
            lower.contains("wifi") || lower.contains("red") -> Settings.ACTION_WIFI_SETTINGS
            lower.contains("bluetooth") -> Settings.ACTION_BLUETOOTH_SETTINGS
            lower.contains("sonido") || lower.contains("volumen") || lower.contains("audio") -> Settings.ACTION_SOUND_SETTINGS
            lower.contains("pantalla") || lower.contains("brillo") -> Settings.ACTION_DISPLAY_SETTINGS
            lower.contains("fecha") || lower.contains("hora") -> Settings.ACTION_DATE_SETTINGS
            lower.contains("aplicacion") || lower.contains("apps") -> Settings.ACTION_APPLICATION_SETTINGS
            lower.contains("bateria") -> Settings.ACTION_BATTERY_SAVER_SETTINGS
            else -> Settings.ACTION_SETTINGS
        }

        return try {
            val intent = Intent(action).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Abriendo ajustes de $settingType."
        } catch (e: Exception) {
            "No se pudieron abrir los ajustes solicitados."
        }
    }
}
