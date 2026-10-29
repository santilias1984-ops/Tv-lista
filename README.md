# Guía TV España — aplicación Android

**Estado**: proyecto listo para compilar. **Este ZIP no es una APK y no se ha verificado la compilación en un dispositivo real.**

## Forma 1 — APK automática en GitHub Actions (sin instalar Android Studio)

1. Crea un repositorio nuevo en [GitHub](https://github.com/new) (sin claves ni datos personales en este proyecto). Puedes usar uno público.
2. Sube **los archivos y directorios extraídos** de este ZIP a la raíz del repositorio, manteniendo las carpetas `app/`, `.github/workflows/`, `gradle/` y los archivos `.gradle`. Subir el ZIP sin descomprimir no funciona. En un ordenador, puedes arrastrar todos los archivos a *Add file → Upload files*. Desde Android la carga de carpetas puede resultar difícil; en ese caso utiliza un ordenador o cliente Git.
3. En la pestaña **Actions**, abre `Compilar APK - Guia TV Espana`. Si lo pide, habilita Actions.
4. Pulsa **Run workflow**. Si ya se ejecutó al subir el proyecto, abre esa ejecución.
5. Cuando termine correctamente, en **Artifacts** descarga `GuiaTV-Espana-APK`, extrae ese ZIP y tendrás `app-debug.apk`.
6. Pasa la APK al teléfono, ábrela y autoriza la instalación de aplicaciones de esa procedencia si Android lo solicita. Solo instala APKs que hayas compilado tú o de origen fiable.

**Nota**: GitHub Actions tiene límites y condiciones que pueden cambiar. Si falla, revisa los registros de la ejecución.

## Forma 2 — Android Studio en un ordenador

1. Abre esta carpeta como un proyecto en Android Studio.
2. Instala Android SDK 35 y JDK 17 y utiliza Gradle 8.9 (el proyecto no incluye `gradlew`/`gradle-wrapper.jar`).
3. Selecciona **Build → Build Bundle(s) / APK(s) → Build APK(s)**.
4. Encontrarás la APK en `app/build/outputs/apk/debug/app-debug.apk`.

## Funciones

- Consulta de programación ahora, esta noche, mañana y búsqueda, con sinopsis cuando las proporcione el origen.
- Listado de canales y favoritos guardados en el teléfono.
- Descarga de guía XMLTV cada hora mientras la aplicación permanece abierta; los proveedores actuales generan datos diarios.
- Importación de ficheros XMLTV (.xml o .xml.gz) y caché para consultar sin red.
- Carga de URLs XMLTV HTTPS personalizadas.
- Botón Atrás con navegación interna y confirmación antes de cerrar.

## Fuentes públicas preconfiguradas

- https://epg-spain.netlify.app/guide.xml
- https://raw.githubusercontent.com/raulfdeztdo/epg/main/guide.xml

Documentación de la fuente: https://github.com/raulfdeztdo/epg

La programación completa no está garantizada: algunos canales o días pueden carecer de información, sinopsis o logos. La aplicación **no emite canales**, ni permite ver televisión de pago sin suscripción. No está afiliada a Movistar, DAZN ni a ninguna cadena.
