# Spartan Jovencito (JOWELL AI - CDPE)

Sistema Asistente de Voz y Telemetría Operativa Inteligente para Smartwatch y Dispositivos Android del CDPE Guatemala.

## 🚀 Módulos y Capacidades

1. **🤖 Asistente Ejecutivo en Vivo (JOWELL):**
   - Reconocimiento de voz continuo y comandos de lenguaje natural.
   - Conectado a Gemini Flash (`gemini-flash-latest`) para razonamiento contextual y transcripción en tiempo real.
   - Lanzamiento y control por voz de aplicaciones instaladas en el dispositivo.

2. **🎙️ Modo Reunión, Actas y Minutas:**
   - Grabación continua de audio en alta fidelidad (`.m4a`).
   - Generación simultánea de archivo de texto estructurado (`.txt`) separando interlocutores (Joel vs JOWELL) con marcas de tiempo.
   - Almacenamiento directo en `Recordings/JOWELL/Reuniones/` accesible para exploradores y computadoras.

3. **🎵 Reproductor de Audio Hi-Fi (Cassette / Vinilo):**
   - Escaneo automático de grabaciones locales, actas de reuniones y la biblioteca completa de música del dispositivo (`MediaStore`).
   - Filtros por categoría: *Todos*, *Reuniones*, *Música* y *Grabaciones*.
   - Mandos táctiles optimizados para pantalla circular de reloj y pantalla rectangular de smartphone.

4. **📹 Monitor de Video:**
   - Detección automática y reproducción de videos almacenados en la memoria y galería del teléfono.
   - Selector manual de videos (`video/*`).

5. **📖 Lector de Documentos (Moon Reader + OCR):**
   - Lectura de archivos PDF, Markdown y texto plano.
   - Motor OCR integrado con Google ML Kit (`play-services-mlkit-text-recognition`) para extracción óptica de texto en libros o páginas escaneadas.
   - Narración por voz párrafo por párrafo con control de velocidad (1.0x, 1.25x, 1.5x, 2.0x).

6. **📅 Agenda y Telemetría CDPE:**
   - Sincronización en vivo con el calendario del dispositivo.
   - Telemetría en tiempo real: clima local, temperatura, humedad, altitud, brújula y coordenadas GPS.

## 📦 Instalación

El archivo ejecutable listo para instalar se encuentra compilado como:
- `JOWELL.apk`
