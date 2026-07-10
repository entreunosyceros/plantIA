# PlantIA Android (LAN)

App Android (Kotlin + Jetpack Compose) para usar PlantIA desde el móvil en **red local**:

- Hacer foto con **CameraX**
- Enviar la foto a PlantIA: `POST /api/plantas/identificar`
- Ver **Mis plantas** (`GET /api/plantas`)
- Ver ficha (`GET /api/plantas/{id}`)
- Guardar notas (`POST /api/plantas/{id}/notas` o `PATCH /api/plantas/{id}`)
- Borrar (`DELETE /api/plantas/{id}`)

## UX móvil

- **Mis plantas**: grid de 2 columnas con miniaturas, badges de confianza, búsqueda con debounce, pull-to-refresh y FAB para identificar
- **Orden y paginación**: selector de orden y carga automática al hacer scroll
- **Regar hoy**: desliza una tarjeta a la derecha para marcar «Regada hoy»; panel superior con plantas que tocan regar según recordatorios
- **Ficha**: foto ampliable, calendario lunar, cuidados en chips, candidatos con miniatura y enlace a Wikipedia
- **Ficha ampliada**: secciones de características, mantenimiento (poda, trasplante, maceta) y seguridad (toxicidad, plagas, problemas)
- **Re-identificar**: botón en la ficha para tomar o elegir otra foto y comparar con la identificación actual
- **Cuaderno**: botón «Regada hoy», chips rápidos, historial tipo timeline y guardado en servidor
- **Recordatorios de riego**: notificación local configurable por planta (cada 1–14 días) con **hora del aviso** (6:00–21:00)
- **Ajustes**: URL del servidor editable sin recompilar y **tema** Sistema / Claro / Oscuro
- **Cámara**: overlay de instrucciones, **elegir de galería**, estado de carga durante identificación y gestión de permisos
- La barra inferior se oculta en la ficha de detalle y al re-identificar para más espacio de lectura
- **Icono y splash**: usa `plantia/static/img/logo.png` (mismo que el servidor), con splash de 4 s y barra de progreso al abrir la app
- **Acerca de**: en Ajustes → *Acerca de PlantIA*, con enlace al repositorio en GitHub

## Requisitos
- Android 10+ (minSdk 29)
- Servidor PlantIA accesible en LAN en `http://192.168.1.96:8000`

## Abrir el proyecto

1. Abre Android Studio
2. `Open` → selecciona la carpeta `android/PlantIAAndroid/` (no la raíz de PlantIA)
3. Espera a que Gradle sincronice (puede tardar unos minutos la primera vez)
4. Conecta un móvil Android 10+ por USB (o usa un emulador) en la **misma red Wi‑Fi/LAN** que el servidor
5. Pulsa **Run** ▶

### Si Android Studio no deja iniciar la app

| Problema | Solución |
|----------|----------|
| *Gradle sync failed* | `File → Sync Project with Gradle Files` |
| *SDK not found* | `File → Project Structure → SDK Location` y apunta a tu Android SDK |
| *No run configuration* | Asegúrate de abrir `android/PlantIAAndroid/`, no el repo entero |
| *compileSdk / platform* | Instala **Android 15 (API 35)** en SDK Manager |
| *Dispositivo no aparece* | Activa **Opciones de desarrollador** y **Depuración USB** en el móvil |

El proyecto incluye `gradlew` para compilar desde terminal:

```bash
cd android/PlantIAAndroid
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`.

## Configuración de IP/servidor

La URL base por defecto está en `Config.DEFAULT_BASE_URL`. En la app, ve a **Ajustes** y edita la URL del servidor (se guarda en el dispositivo).

Ejemplo:

```
http://192.168.1.96:8000/
```

## HTTP en Android (LAN)

La app permite HTTP en red local para conectarse al servidor PlantIA configurado en Ajustes.

