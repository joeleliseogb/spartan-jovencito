package com.example.jarvis.modules

/**
 * Pantallas y modos operativos principales de JOWELL.
 * Pantalla 1: ROBOT (Asistente en vivo)
 * Pantalla 2: STANDBY (Reloj, Clima y Telemetría completa)
 * Pantallas 3..N: Módulos instalables del CDPE
 */
enum class JarvisMode(val title: String) {
    ROBOT("ROBOT LIVE"),
    WATCHFACE("ESFERA REALISTA"),
    STANDBY("RELOJ & TELEMETRÍA"),
    RECORDER("GRABADORA"),
    AUDIO_PLAYER("AUDIO PLAYER"),
    VIDEO_PLAYER("VIDEO MONITOR"),
    READER("NARRADOR PDF"),
    MINUTES("MINUTAS"),
    AGENDA("AGENDA / CALENDARIO")
}
