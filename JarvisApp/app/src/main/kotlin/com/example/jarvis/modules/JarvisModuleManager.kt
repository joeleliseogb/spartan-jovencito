package com.example.jarvis.modules

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Identificadores únicos para los módulos complementarios e instalables de JOWELL.
 * La prioridad nativa y permanente de la aplicación es el Asistente Personal del CDPE.
 */
enum class ModuleId(
    val title: String,
    val category: String,
    val description: String,
    val sizeMb: String,
    val iconName: String
) {
    RECORDER(
        title = "Grabadora de Campo Hi-Fi",
        category = "Audio & Telemetría",
        description = "Captura de audio con reducción de ruido, visualización espectral y transcripción multimodal con Gemini.",
        sizeMb = "4.2 MB",
        iconName = "🎙"
    ),
    AUDIO_PLAYER(
        title = "Reproductor de Audio Hi-Fi",
        category = "Multimedia",
        description = "Reproducción de grabaciones y audios de trabajo con analizador de ondas en tiempo real.",
        sizeMb = "3.1 MB",
        iconName = "📻"
    ),
    VIDEO_PLAYER(
        title = "Monitor de Video Industrial",
        category = "Monitoreo",
        description = "Pantalla de monitoreo de video de alta definición para supervisión de procesos y plantas.",
        sizeMb = "5.8 MB",
        iconName = "📺"
    ),
    READER(
        title = "Lector Moon+ Narrador PDF",
        category = "Productividad",
        description = "Lectura continua de manuales, actas y libros PDF con síntesis de voz inteligente al oído.",
        sizeMb = "6.5 MB",
        iconName = "📖"
    ),
    MINUTES(
        title = "Actas y Minutas CDPE",
        category = "Gestión Ejecutiva",
        description = "Transcripción multi-hablante, extracción de compromisos y exportación directa de actas de asamblea.",
        sizeMb = "3.8 MB",
        iconName = "📝"
    ),
    AGENDA(
        title = "Agenda & Citas CDPE",
        category = "Planificación",
        description = "Organizador ejecutivo, sincronización de compromisos con cooperativistas y franquicias.",
        sizeMb = "2.9 MB",
        iconName = "📅"
    )
}

data class AppModule(
    val id: ModuleId,
    val isInstalled: Boolean,
    val isEnabled: Boolean
) {
    val title: String get() = id.title
    val category: String get() = id.category
    val description: String get() = id.description
    val sizeMb: String get() = id.sizeMb
    val iconName: String get() = id.iconName
}

class JarvisModuleManager(private val context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences("jowell_modules_prefs", Context.MODE_PRIVATE)

    private val _modulesList = MutableStateFlow<List<AppModule>>(loadModules())
    val modulesList = _modulesList.asStateFlow()

    private fun loadModules(): List<AppModule> {
        // Por defecto, la grabadora, minutas y lector vienen instalados para conveniencia, pero pueden ser desinstalados
        return ModuleId.values().map { id ->
            val isInstalled = prefs.getBoolean("installed_${id.name}", true)
            val isEnabled = prefs.getBoolean("enabled_${id.name}", true)
            AppModule(id = id, isInstalled = isInstalled, isEnabled = isEnabled)
        }
    }

    private fun saveModuleState(module: AppModule) {
        prefs.edit()
            .putBoolean("installed_${module.id.name}", module.isInstalled)
            .putBoolean("enabled_${module.id.name}", module.isEnabled)
            .apply()
    }

    fun installModule(id: ModuleId) {
        val updated = _modulesList.value.map {
            if (it.id == id) it.copy(isInstalled = true, isEnabled = true) else it
        }
        _modulesList.value = updated
        updated.find { it.id == id }?.let { saveModuleState(it) }
    }

    fun uninstallModule(id: ModuleId) {
        val updated = _modulesList.value.map {
            if (it.id == id) it.copy(isInstalled = false, isEnabled = false) else it
        }
        _modulesList.value = updated
        updated.find { it.id == id }?.let { saveModuleState(it) }
    }

    fun toggleModule(id: ModuleId) {
        val updated = _modulesList.value.map {
            if (it.id == id && it.isInstalled) it.copy(isEnabled = !it.isEnabled) else it
        }
        _modulesList.value = updated
        updated.find { it.id == id }?.let { saveModuleState(it) }
    }

    fun isModuleActive(id: ModuleId): Boolean {
        val mod = _modulesList.value.find { it.id == id }
        return mod?.isInstalled == true && mod.isEnabled
    }

    /**
     * Instalar o desinstalar por comando de voz
     */
    fun handleModuleVoiceCommand(command: String): String? {
        val lower = command.lowercase()
        val isInstall = lower.contains("instalar") || lower.contains("activar")
        val isUninstall = lower.contains("desinstalar") || lower.contains("quitar") || lower.contains("remover")

        if (!isInstall && !isUninstall) return null

        val targetModule = when {
            lower.contains("grabadora") || lower.contains("audio de campo") -> ModuleId.RECORDER
            lower.contains("reproductor") || lower.contains("player") -> ModuleId.AUDIO_PLAYER
            lower.contains("video") || lower.contains("monitor") -> ModuleId.VIDEO_PLAYER
            lower.contains("lector") || lower.contains("pdf") || lower.contains("libro") || lower.contains("moon") -> ModuleId.READER
            lower.contains("minuta") || lower.contains("acta") || lower.contains("transcriptor") -> ModuleId.MINUTES
            lower.contains("agenda") || lower.contains("calendario") || lower.contains("citas") -> ModuleId.AGENDA
            else -> return null
        }

        return if (isInstall) {
            installModule(targetModule)
            "Módulo ${targetModule.title} instalado y listo para usar."
        } else {
            uninstallModule(targetModule)
            "Módulo ${targetModule.title} desinstalado de la interfaz principal."
        }
    }
}
