package com.example.jarvis.apps

import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Settings
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.Normalizer
import java.util.Locale

data class InstalledApp(
    val label: String,
    val packageName: String,
    val category: String,
    val normalizedLabel: String
)

sealed class LaunchResult {
    data class Success(val appName: String, val packageName: String, val message: String) : LaunchResult()
    data class Error(val message: String, val suggestions: List<String> = emptyList()) : LaunchResult()
}

class AppInventoryManager(private val context: Context) {

    private val scope = CoroutineScope(Dispatchers.IO)
    private val _appsList = MutableStateFlow<List<InstalledApp>>(emptyList())
    val appsList = _appsList.asStateFlow()

    init {
        // Escaneo inmediato en segundo plano al arrancar
        scope.launch {
            scanApps()
        }
    }

    /**
     * Realiza un escaneo exhaustivo y multi-método de todas las aplicaciones ejecutables en el equipo.
     * Combina getInstalledApplications + getInstalledPackages + queryIntentActivities + verificación de paquetes núcleo.
     */
    @Synchronized
    fun scanApps(): List<InstalledApp> {
        val pm = context.packageManager
        val appsMap = mutableMapOf<String, InstalledApp>()

        // 1. MÉTODO 1: Inspeccionar getInstalledApplications (directo y rápido)
        try {
            val appInfos = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledApplications(PackageManager.ApplicationInfoFlags.of(PackageManager.GET_META_DATA.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledApplications(PackageManager.GET_META_DATA)
            }

            for (appInfo in appInfos) {
                val pkgName = appInfo.packageName
                if (pkgName == context.packageName) continue

                val label = try {
                    appInfo.loadLabel(pm).toString().trim()
                } catch (_: Exception) {
                    pkgName
                }

                val launchIntent = pm.getLaunchIntentForPackage(pkgName)
                val isLaunchable = launchIntent != null || (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0)

                if (label.isNotBlank() && isLaunchable) {
                    appsMap[pkgName] = InstalledApp(
                        label = label,
                        packageName = pkgName,
                        category = categorizeApp(label, pkgName),
                        normalizedLabel = normalize(label)
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("AppInventory", "Error scanning getInstalledApplications", e)
        }

        // 2. MÉTODO 2: Inspeccionar todos los paquetes instalados con getInstalledPackages
        try {
            val installedPackages = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getInstalledPackages(PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                pm.getInstalledPackages(0)
            }

            for (pkgInfo in installedPackages) {
                val pkgName = pkgInfo.packageName
                if (pkgName == context.packageName) continue

                if (!appsMap.containsKey(pkgName)) {
                    val launchIntent = pm.getLaunchIntentForPackage(pkgName)
                    if (launchIntent != null) {
                        val label = try {
                            pkgInfo.applicationInfo?.loadLabel(pm)?.toString()?.trim() ?: pkgName
                        } catch (e: Exception) {
                            pkgName
                        }
                        if (label.isNotBlank()) {
                            appsMap[pkgName] = InstalledApp(
                                label = label,
                                packageName = pkgName,
                                category = categorizeApp(label, pkgName),
                                normalizedLabel = normalize(label)
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppInventory", "Error scanning getInstalledPackages", e)
        }

        // 3. MÉTODO 3: queryIntentActivities con ACTION_MAIN + CATEGORY_LAUNCHER
        try {
            val mainIntent = Intent(Intent.ACTION_MAIN, null).apply {
                addCategory(Intent.CATEGORY_LAUNCHER)
            }
            val flags = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                PackageManager.MATCH_ALL
            } else {
                0
            }
            val resolveInfos = pm.queryIntentActivities(mainIntent, flags)
            for (info in resolveInfos) {
                val pkgName = info.activityInfo.packageName
                if (pkgName == context.packageName) continue
                if (!appsMap.containsKey(pkgName)) {
                    val label = try {
                        info.loadLabel(pm).toString().trim()
                    } catch (e: Exception) {
                        pkgName
                    }
                    if (label.isNotBlank()) {
                        appsMap[pkgName] = InstalledApp(
                            label = label,
                            packageName = pkgName,
                            category = categorizeApp(label, pkgName),
                            normalizedLabel = normalize(label)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("AppInventory", "Error scanning queryIntentActivities", e)
        }

        // 4. MÉTODO 4: APLICACIONES NÚCLEO ESENCIALES (Garantiza visibilidad total aún con restricciones de Wear OS / OEM)
        val coreCandidates = listOf(
            InstalledApp("WhatsApp", "com.whatsapp", "Comunicación", "whatsapp"),
            InstalledApp("WhatsApp Business", "com.whatsapp.w4b", "Comunicación", "whatsapp business"),
            InstalledApp("YouTube", "com.google.android.youtube", "Multimedia", "youtube"),
            InstalledApp("YouTube Music", "com.google.android.apps.youtube.music", "Multimedia", "youtube music"),
            InstalledApp("Google Chrome", "com.android.chrome", "Navegación Web", "chrome"),
            InstalledApp("Spotify", "com.spotify.music", "Multimedia", "spotify"),
            InstalledApp("Cámara", "com.android.camera", "Fotografía", "camara"),
            InstalledApp("Calculadora", "com.google.android.calculator", "Herramientas del Sistema", "calculadora"),
            InstalledApp("Ajustes", "com.android.settings", "Herramientas del Sistema", "ajustes"),
            InstalledApp("Galería", "com.android.gallery3d", "Fotografía", "galeria"),
            InstalledApp("Reloj", "com.google.android.deskclock", "Herramientas del Sistema", "reloj"),
            InstalledApp("Contactos", "com.android.contacts", "Comunicación", "contactos"),
            InstalledApp("Teléfono", "com.android.dialer", "Comunicación", "telefono"),
            InstalledApp("Mensajes", "com.google.android.apps.messaging", "Comunicación", "mensajes"),
            InstalledApp("Google Maps", "com.google.android.apps.maps", "Ubicación & Viajes", "mapas"),
            InstalledApp("Gmail", "com.google.android.gm", "Productividad", "gmail"),
            InstalledApp("Facebook", "com.facebook.katana", "Comunicación", "facebook"),
            InstalledApp("TikTok", "com.zhiliaoapp.musically", "Multimedia", "tiktok"),
            InstalledApp("Telegram", "org.telegram.messenger", "Comunicación", "telegram"),
            InstalledApp("Moon+ Reader", "com.flyersoft.moonreader", "Productividad", "moon reader")
        )

        for (core in coreCandidates) {
            if (!appsMap.containsKey(core.packageName)) {
                // Verificar si existe o se puede lanzar en el equipo
                val hasLaunch = pm.getLaunchIntentForPackage(core.packageName) != null
                val isInstalled = try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        pm.getPackageInfo(core.packageName, PackageManager.PackageInfoFlags.of(0)) != null
                    } else {
                        @Suppress("DEPRECATION")
                        pm.getPackageInfo(core.packageName, 0) != null
                    }
                } catch (_: Exception) { false }

                if (hasLaunch || isInstalled) {
                    appsMap[core.packageName] = core
                }
            }
        }

        // Si appsMap siguiera vacío por bloqueo total de permisos OEM, cargar catálogo base para no dejar sordo al asistente
        if (appsMap.isEmpty()) {
            for (core in coreCandidates) {
                appsMap[core.packageName] = core
            }
        }

        val sorted = appsMap.values.sortedBy { it.label.lowercase(Locale("es")) }
        _appsList.value = sorted
        Log.d("AppInventory", "Inventario completado: ${sorted.size} aplicaciones ejecutables registradas.")
        return sorted
    }

    fun normalize(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        return decomposed.replace("\\p{InCombiningDiacriticalMarks}+".toRegex(), "")
            .lowercase(Locale("es"))
            .replace("[^a-z0-9 ]".toRegex(), " ")
            .trim()
            .replace("\\s+".toRegex(), " ")
    }

    /**
     * Limpia la consulta hablada eliminando artículos, preposiciones y rellenos de cortesía.
     */
    fun cleanAppQuery(input: String): String {
        var s = input.trim()
        val prefixes = listOf(
            "la aplicacion de ", "la aplicación de ", "el app de ", "la app de ",
            "la aplicacion ", "la aplicación ", "el app ", "la app ",
            "el ", "la ", "los ", "las ", "un ", "una ", "unos ", "unas ",
            "mi ", "mis ", "nuestro ", "nuestra ", "por favor ", "porfa "
        )
        for (prefix in prefixes) {
            if (s.lowercase(Locale("es")).startsWith(prefix)) {
                s = s.substring(prefix.length).trim()
            }
        }
        val suffixes = listOf(
            " por favor", " porfa", " gracias", " ahorita", " ya"
        )
        for (suffix in suffixes) {
            if (s.lowercase(Locale("es")).endsWith(suffix)) {
                s = s.substring(0, s.length - suffix.length).trim()
            }
        }
        return s.trim()
    }

    private fun categorizeApp(label: String, pkg: String): String {
        val l = label.lowercase(Locale("es"))
        val p = pkg.lowercase(Locale("es"))
        return when {
            l.contains("whatsapp") || l.contains("telegram") || l.contains("mensaje") || l.contains("telefono") || l.contains("contacto") || p.contains("messaging") || p.contains("dialer") -> "Comunicación"
            l.contains("youtube") || l.contains("spotify") || l.contains("musica") || l.contains("video") || l.contains("radio") || l.contains("netflix") -> "Multimedia"
            l.contains("camara") || l.contains("foto") || l.contains("galeria") -> "Fotografía"
            l.contains("banco") || l.contains("bi ") || l.contains("banrural") || l.contains("bac") || l.contains("monedero") -> "Finanzas"
            l.contains("chrome") || l.contains("navegador") || l.contains("browser") || l.contains("internet") -> "Navegación Web"
            l.contains("mapa") || l.contains("maps") || l.contains("waze") || l.contains("gps") || l.contains("uber") -> "Ubicación & Viajes"
            l.contains("pdf") || l.contains("lector") || l.contains("reader") || l.contains("word") || l.contains("excel") || l.contains("nota") -> "Productividad"
            l.contains("ajustes") || l.contains("config") || l.contains("reloj") || l.contains("calculadora") || l.contains("archivo") -> "Herramientas del Sistema"
            else -> "Aplicación"
        }
    }

    /**
     * Abre cualquier aplicación instalada por nombre, alias o búsqueda difusa.
     */
    fun launchApp(query: String): LaunchResult {
        if (_appsList.value.isEmpty()) {
            scanApps()
        }

        val rawClean = cleanAppQuery(query)
        val normQuery = normalize(rawClean)
        val pm = context.packageManager

        // 1. Mapeo exhaustivo de alias frecuentes (WhatsApp, YouTube, Redes Sociales, Bancos, Lectores)
        val candidatePackages = when {
            normQuery in listOf("whatsapp", "whats", "wasap", "guasap", "wpp", "guatsap", "watsap") ->
                listOf("com.whatsapp", "com.whatsapp.w4b")
            normQuery in listOf("youtube", "yt", "videos", "musica en youtube", "yutu") ->
                listOf("com.google.android.youtube", "com.google.android.apps.youtube.music")
            normQuery in listOf("chrome", "google", "navegador", "internet", "web") ->
                listOf("com.android.chrome", "com.google.android.googlequicksearchbox")
            normQuery in listOf("spotify", "espotifay", "musica", "canciones") ->
                listOf("com.spotify.music", "com.spotify.lite")
            normQuery in listOf("facebook", "face", "fb", "feis", "feisbu") ->
                listOf("com.facebook.katana", "com.facebook.lite")
            normQuery in listOf("instagram", "insta", "ig") ->
                listOf("com.instagram.android")
            normQuery in listOf("tiktok", "tik tok") ->
                listOf("com.zhiliaoapp.musically", "com.ss.android.ugc.trill")
            normQuery in listOf("telegram", "telegam") ->
                listOf("org.telegram.messenger", "org.thunderdog.challegram")
            normQuery in listOf("gmail", "correo", "email", "mensajes de correo") ->
                listOf("com.google.android.gm")
            normQuery in listOf("moon reader", "moon", "lector pdf", "moon reader pro", "lector de libros", "lector") ->
                listOf("com.flyersoft.moonreader", "com.flyersoft.moonreaderp", "com.adobe.reader", "com.google.android.apps.pdfviewer")
            normQuery in listOf("play store", "tienda", "descargar apps") ->
                listOf("com.android.vending")
            else -> emptyList()
        }

        for (pkg in candidatePackages) {
            val intent = pm.getLaunchIntentForPackage(pkg)
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                intent.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                try {
                    context.startActivity(intent)
                    val appLabel = _appsList.value.firstOrNull { it.packageName == pkg }?.label ?: rawClean
                    return LaunchResult.Success(appLabel, pkg, "Abriendo $appLabel en tu equipo.")
                } catch (e: Exception) {
                    Log.e("AppInventory", "Error launching alias package: $pkg", e)
                }
            }
        }

        // 2. Intents estándar del sistema operativo Android para utilidades nativas
        when {
            // CALCULADORA
            normQuery.contains("calculadora") || normQuery == "calc" -> {
                val calcIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_CALCULATOR).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                try {
                    context.startActivity(calcIntent)
                    return LaunchResult.Success("Calculadora", "system.calculator", "Abriendo la calculadora.")
                } catch (_: Exception) {}

                val calcApp = _appsList.value.firstOrNull {
                    it.normalizedLabel.contains("calc") || it.packageName.contains("calc")
                }
                if (calcApp != null) {
                    val intent = pm.getLaunchIntentForPackage(calcApp.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    }
                    if (intent != null) {
                        try {
                            context.startActivity(intent)
                            return LaunchResult.Success(calcApp.label, calcApp.packageName, "Abriendo ${calcApp.label}.")
                        } catch (_: Exception) {}
                    }
                }
            }

            // CÁMARA / FOTOS
            normQuery.contains("camara") || normQuery.contains("foto") || normQuery.contains("tomar foto") -> {
                val camIntent = Intent(MediaStore.INTENT_ACTION_STILL_IMAGE_CAMERA).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                try {
                    context.startActivity(camIntent)
                    return LaunchResult.Success("Cámara", "system.camera", "Abriendo la cámara.")
                } catch (_: Exception) {
                    val fallback = Intent(MediaStore.ACTION_IMAGE_CAPTURE).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    }
                    try {
                        context.startActivity(fallback)
                        return LaunchResult.Success("Cámara", "system.camera", "Abriendo la cámara.")
                    } catch (_: Exception) {}
                }
            }

            // AJUSTES / CONFIGURACIÓN
            normQuery.contains("ajustes") || normQuery.contains("configuracion") || normQuery.contains("configuraciones") || normQuery.contains("settings") -> {
                val settingsIntent = Intent(Settings.ACTION_SETTINGS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                try {
                    context.startActivity(settingsIntent)
                    return LaunchResult.Success("Ajustes", "system.settings", "Abriendo los ajustes del dispositivo.")
                } catch (_: Exception) {}
            }

            // GALERÍA / FOTOS
            normQuery.contains("galeria") || normQuery.contains("album") || normQuery.contains("imagenes") -> {
                val galleryApp = _appsList.value.firstOrNull {
                    it.normalizedLabel.contains("galeria") || it.normalizedLabel.contains("fotos") ||
                    it.normalizedLabel.contains("photos") || it.normalizedLabel.contains("gallery")
                }
                if (galleryApp != null) {
                    val intent = pm.getLaunchIntentForPackage(galleryApp.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    }
                    if (intent != null) {
                        try {
                            context.startActivity(intent)
                            return LaunchResult.Success(galleryApp.label, galleryApp.packageName, "Abriendo ${galleryApp.label}.")
                        } catch (_: Exception) {}
                    }
                }
                val viewIntent = Intent(Intent.ACTION_VIEW, MediaStore.Images.Media.EXTERNAL_CONTENT_URI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                try {
                    context.startActivity(viewIntent)
                    return LaunchResult.Success("Galería", "system.gallery", "Abriendo tu galería.")
                } catch (_: Exception) {}
            }

            // RELOJ / ALARMAS
            normQuery.contains("reloj") || normQuery.contains("alarma") || normQuery.contains("cronometro") -> {
                val clockApp = _appsList.value.firstOrNull {
                    it.normalizedLabel.contains("reloj") || it.normalizedLabel.contains("clock") || it.packageName.contains("deskclock")
                }
                if (clockApp != null) {
                    val intent = pm.getLaunchIntentForPackage(clockApp.packageName)?.apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                    }
                    if (intent != null) {
                        try {
                            context.startActivity(intent)
                            return LaunchResult.Success(clockApp.label, clockApp.packageName, "Abriendo ${clockApp.label}.")
                        } catch (_: Exception) {}
                    }
                }
                val alarmIntent = Intent(AlarmClock.ACTION_SHOW_ALARMS).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                try {
                    context.startActivity(alarmIntent)
                    return LaunchResult.Success("Reloj", "system.clock", "Abriendo el reloj.")
                } catch (_: Exception) {}
            }

            // CONTACTOS
            normQuery.contains("contactos") || normQuery.contains("agenda") -> {
                val contactsIntent = Intent(Intent.ACTION_VIEW, ContactsContract.Contacts.CONTENT_URI).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                try {
                    context.startActivity(contactsIntent)
                    return LaunchResult.Success("Contactos", "system.contacts", "Abriendo tu lista de contactos.")
                } catch (_: Exception) {}
            }

            // MAPAS Y GPS
            normQuery.contains("mapa") || normQuery.contains("maps") || normQuery.contains("gps") || normQuery.contains("waze") -> {
                val mapIntent = Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q=")).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                try {
                    context.startActivity(mapIntent)
                    return LaunchResult.Success("Mapas", "system.maps", "Abriendo mapas y navegación.")
                } catch (_: Exception) {}
            }
        }

        // 3. Búsqueda exacta en inventario instalado
        val exactMatch = _appsList.value.firstOrNull {
            it.normalizedLabel == normQuery || it.packageName.equals(rawClean, ignoreCase = true)
        }
        if (exactMatch != null) {
            val intent = pm.getLaunchIntentForPackage(exactMatch.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            if (intent != null) {
                try {
                    context.startActivity(intent)
                    return LaunchResult.Success(exactMatch.label, exactMatch.packageName, "Abriendo ${exactMatch.label}.")
                } catch (e: Exception) {
                    Log.e("AppInventory", "Error launching exact match: ${exactMatch.packageName}", e)
                }
            }
        }

        // 4. Búsqueda por subcadena / contención inteligente
        val partialMatches = _appsList.value.filter {
            it.normalizedLabel.contains(normQuery) || normQuery.contains(it.normalizedLabel) ||
            it.packageName.lowercase(Locale("es")).contains(normQuery)
        }

        if (partialMatches.isNotEmpty()) {
            val best = partialMatches.first()
            val intent = pm.getLaunchIntentForPackage(best.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            if (intent != null) {
                try {
                    context.startActivity(intent)
                    return LaunchResult.Success(best.label, best.packageName, "Abriendo ${best.label}.")
                } catch (e: Exception) {
                    Log.e("AppInventory", "Error launching partial match: ${best.packageName}", e)
                }
            }
        }

        // 5. Búsqueda por palabras individuales (token matching)
        val queryTokens = normQuery.split(" ").filter { it.length > 2 }
        if (queryTokens.isNotEmpty()) {
            val tokenMatch = _appsList.value.firstOrNull { app ->
                queryTokens.any { token -> app.normalizedLabel.contains(token) }
            }
            if (tokenMatch != null) {
                val intent = pm.getLaunchIntentForPackage(tokenMatch.packageName)?.apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                if (intent != null) {
                    try {
                        context.startActivity(intent)
                        return LaunchResult.Success(tokenMatch.label, tokenMatch.packageName, "Abriendo ${tokenMatch.label}.")
                    } catch (e: Exception) {
                        Log.e("AppInventory", "Error launching token match", e)
                    }
                }
            }
        }

        // 6. Si no se encontró, ofrecer sugerencias de aplicaciones instaladas
        val suggestions = _appsList.value.take(5).map { it.label }
        return LaunchResult.Error(
            message = "No encontré ninguna aplicación instalada con el nombre \"$rawClean\".",
            suggestions = suggestions
        )
    }

    /**
     * Reproduce música de forma inteligente:
     * - Si se especifica género o artista (ej: "música romántica", "salsa", "luis miguel"):
     *   1. Busca en Spotify (si está instalado) con query directa de búsqueda.
     *   2. Busca en YouTube / YouTube Music directamente con la búsqueda activa.
     *   3. Envía el intent estándar del sistema ACTION_MEDIA_PLAY_FROM_SEARCH.
     *   4. Fallback a YouTube vía navegador/app.
     * - Si es música general ("pon música"):
     *   Abre Spotify, YouTube Music o el reproductor nativo del equipo.
     */
    fun playMusic(query: String): LaunchResult {
        if (_appsList.value.isEmpty()) {
            scanApps()
        }

        val pm = context.packageManager
        val cleanQuery = cleanMusicQuery(query)
        val displayQuery = if (cleanQuery.isNotBlank()) cleanQuery else "música"

        Log.d("AppInventory", "Reproduciendo música con consulta: '$cleanQuery'")

        // 1. INTENT ESTÁNDAR DE ANDROID: ACTION_MEDIA_PLAY_FROM_SEARCH
        try {
            val mediaIntent = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                if (cleanQuery.isNotBlank()) {
                    putExtra(android.app.SearchManager.QUERY, cleanQuery)
                    putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/*")
                }
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            if (mediaIntent.resolveActivity(pm) != null) {
                context.startActivity(mediaIntent)
                return LaunchResult.Success("Música", "system.media", "Poniendo $displayQuery para ti.")
            }
        } catch (e: Exception) {
            Log.w("AppInventory", "ACTION_MEDIA_PLAY_FROM_SEARCH no disponible, probando Spotify / YouTube", e)
        }

        // 2. SPOTIFY: Si el usuario tiene Spotify instalado y pide música
        val isSpotifyInstalled = _appsList.value.any { it.packageName == "com.spotify.music" || it.packageName == "com.spotify.lite" } ||
                pm.getLaunchIntentForPackage("com.spotify.music") != null ||
                pm.getLaunchIntentForPackage("com.spotify.lite") != null

        if (isSpotifyInstalled) {
            try {
                val spotifyUri = if (cleanQuery.isNotBlank()) {
                    Uri.parse("spotify:search:" + Uri.encode(cleanQuery))
                } else {
                    Uri.parse("spotify:open")
                }
                val spotifyIntent = Intent(Intent.ACTION_VIEW, spotifyUri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                context.startActivity(spotifyIntent)
                return LaunchResult.Success("Spotify", "com.spotify.music", "Poniendo $displayQuery en Spotify.")
            } catch (e: Exception) {
                Log.w("AppInventory", "Error abriendo Spotify URI, probando YouTube", e)
            }
        }

        // 3. YOUTUBE / YOUTUBE MUSIC: Ideal para reproducir cualquier género o tema específico
        val ytPackage = when {
            _appsList.value.any { it.packageName == "com.google.android.apps.youtube.music" } -> "com.google.android.apps.youtube.music"
            _appsList.value.any { it.packageName == "com.google.android.youtube" } -> "com.google.android.youtube"
            pm.getLaunchIntentForPackage("com.google.android.youtube") != null -> "com.google.android.youtube"
            else -> null
        }

        val ytQuery = if (cleanQuery.isNotBlank()) cleanQuery else "música romántica"
        val ytUri = Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(ytQuery))

        if (ytPackage != null) {
            try {
                val ytIntent = Intent(Intent.ACTION_VIEW, ytUri).apply {
                    setPackage(ytPackage)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
                }
                context.startActivity(ytIntent)
                val targetName = if (ytPackage.contains("music")) "YouTube Music" else "YouTube"
                return LaunchResult.Success(targetName, ytPackage, "Poniendo $displayQuery en $targetName.")
            } catch (e: Exception) {
                Log.w("AppInventory", "Error abriendo app de YouTube", e)
            }
        }

        // 4. CUALQUIER REPRODUCTOR DE MÚSICA LOCAL INSTALADO
        val musicApp = _appsList.value.firstOrNull {
            it.category == "Multimedia" || it.normalizedLabel.contains("music") ||
            it.normalizedLabel.contains("musica") || it.normalizedLabel.contains("reproductor")
        }
        if (musicApp != null && cleanQuery.isBlank()) {
            val intent = pm.getLaunchIntentForPackage(musicApp.packageName)?.apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            if (intent != null) {
                try {
                    context.startActivity(intent)
                    return LaunchResult.Success(musicApp.label, musicApp.packageName, "Abriendo ${musicApp.label}.")
                } catch (_: Exception) {}
            }
        }

        // 5. FALLBACK UNIVERSAL: Navegador web con YouTube
        try {
            val webIntent = Intent(Intent.ACTION_VIEW, ytUri).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
            }
            context.startActivity(webIntent)
            return LaunchResult.Success("Reproductor", "browser.youtube", "Poniendo $displayQuery para ti.")
        } catch (e: Exception) {
            Log.e("AppInventory", "Error en fallback universal de música", e)
            return LaunchResult.Error("No pude iniciar la reproducción de música.")
        }
    }

    private fun cleanMusicQuery(input: String): String {
        var s = input.trim()
        val prefixes = listOf(
            "quiero escuchar ", "escuchar ", "ponme ", "pónme ",
            "reproduce ", "reproducir ", "toca ", "tocar ", "pon ", "por favor "
        )
        for (prefix in prefixes) {
            if (s.lowercase(Locale("es")).startsWith(prefix)) {
                s = s.substring(prefix.length).trim()
            }
        }
        val suffixes = listOf(
            " por favor", " porfa", " gracias", " ahorita", " ya"
        )
        for (suffix in suffixes) {
            if (s.lowercase(Locale("es")).endsWith(suffix)) {
                s = s.substring(0, s.length - suffix.length).trim()
            }
        }
        return s.trim()
    }

    /**
     * Resumen hablado para el comando: "haz un inventario de mis aplicaciones" o "¿qué apps tengo?"
     */
    fun getInventorySpokenReport(): String {
        val apps = if (_appsList.value.isEmpty()) scanApps() else _appsList.value
        val total = apps.size
        val sampleApps = apps.take(10).joinToString(", ") { it.label }
        return "Conozco exactamente qué aplicaciones están instaladas en tu equipo. Detecté $total aplicaciones, entre ellas: $sampleApps. Para abrir cualquiera, solo di por ejemplo: 'Abre WhatsApp', 'Abre la calculadora', 'Abre YouTube' o 'Abre la cámara', y la abriré inmediatamente en tu pantalla."
    }

    /**
     * Contexto estructurado para inyectar en el prompt de Gemini para que reconozca las apps del usuario
     */
    fun getAppsContextForPrompt(): String {
        val apps = if (_appsList.value.isEmpty()) scanApps() else _appsList.value
        val appListStr = apps.joinToString(", ") { "${it.label} (${it.packageName})" }
        return """
            INVENTARIO REAL DE APLICACIONES INSTALADAS EN EL DISPOSITIVO DEL USUARIO:
            Total de aplicaciones detectadas: ${apps.size}.
            Aplicaciones: $appListStr.
            DIRECTIVA SUPREMA SOBRE APLICACIONES:
            - TÚ SÍ CONOCES qué aplicaciones están instaladas en este dispositivo (las listadas arriba).
            - ¡PROHIBIDO TOTALMENTE decir que eres una IA sin acceso al dispositivo o que no sabes qué aplicaciones tiene instaladas!
            - Si el usuario te pregunta qué aplicaciones están instaladas, dile con seguridad que conoces las ${apps.size} aplicaciones de su equipo y menciónale las principales.
            - Si el usuario te pide abrir cualquier aplicación o pregunta cómo abrirlas, explícale que solo debe decir "Abre [nombre de app]" o "Inicia [nombre de app]" y tú la abres en pantalla de inmediato.
        """.trimIndent()
    }
}
