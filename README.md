# LAB6_20230420 · Mis horas de estudio

Aplicación del enunciado **1TEL05, Laboratorio 06, 2026-2: Sistema de Registro de Horas de Estudio**. Java, XML y Groovy; Android 14/API 34 o superior. Preparada como base para el siguiente laboratorio.

Esta es la versión de **sesiones de estudio** del enunciado real. El ejemplo de inventario anterior permanece en la carpeta independiente `PracticaLab6`.

## Estado verificado · 17 de noviembre de 2026

- Compilación del APK aprobada con el JSON actualizado de `lab6-20230420`.
- **8 pruebas unitarias y 3 instrumentadas aprobadas**, sin fallos, en emulador Android 14/API 34.
- Verificados registro e inicio por correo mediante FirebaseUI, apertura de recuperación sin enviar correos de prueba, cierre de sesión, CRUD desde la interfaz, gráficos/filtro y restauración del listado.
- Verificados en Firebase real el listener, CRUD, rechazo de campos inválidos y lectura/escritura denegadas a otro usuario.
- Lint: **0 errores y 66 advertencias**; estas últimas incluyen recursos/textos y recomendaciones de estilo/compatibilidad.
- Correo y Google habilitados; reglas privadas publicadas; SHA-1/SHA-256 debug registradas. Google con una cuenta real sigue pendiente de prueba manual.

## Abrir y ejecutar

1. En Android Studio, File > Open > seleccionar **toda esta carpeta**, donde está `settings.gradle`.
2. Usar un JDK compatible para Gradle; JDK 21 es una opción para la PC del laboratorio. Instalar SDK Platform 36 y Build Tools 36.0.0. El dispositivo puede ser Android 14; no es necesario que sea Android 16.
3. Colocar por separado el archivo válido de Firebase en **`app/google-services.json`**. Esta copia local ya lo contiene, con la configuración OAuth actualizada.
4. Sincronizar Gradle. Seleccionar `app`, un emulador/celular API 34 o superior y pulsar Run.

La app aparece como **Mis horas de estudio**. El login ofrece correo y Google mediante FirebaseUI con una pantalla personalizada, logo y textos en español. Al entrar por correo se muestran las opciones de registrarse e iniciar sesión; desde el flujo de contraseña se puede recuperar el acceso. La sesión permanece guardada hasta cerrarla.

## Firebase de esta versión

| Configuración | Valor |
|---|---|
| Proyecto Firebase | `lab6-20230420` |
| Paquete Android y applicationId | `com.example.lab06_20230420` |
| Base | Cloud Firestore `(default)` existente, región `nam5` |
| Autenticación | Correo/contraseña y Google |
| Reglas | `firestore.rules`, publicadas con aislamiento por UID |

Se reutiliza la app Android ya registrada dentro de ese Firebase. El paquete lleva **lab06**, aunque el ID del proyecto Firebase lleva **lab6**: ambos valores son intencionales y distintos.

El título del PDF menciona Realtime Database, pero el requisito específico exige **Cloud Firestore**; esta implementación utiliza Firestore.

SHA-1 y SHA-256 debug de la PC de desarrollo registradas. En otra PC ejecutar `signingReport`, agregar sus huellas en Firebase > Configuración > Tus apps, y descargar de nuevo el JSON si cambia la configuración OAuth. No copiar claves privadas al repositorio.

**`google-services.json` está excluido de Git.** Un clon necesita copiarlo por separado. Si falta, la app explica cómo configurarlo. Si el JSON todavía no incluye Google/OAuth, muestra un aviso en lugar de cerrar la aplicación. No se requiere un App Secret de Facebook, una cuenta de servicio ni un archivo de Meta para este Lab6.

## Las cuatro pantallas

| Requisito del enunciado | Implementación |
|---|---|
| Login FirebaseUI personalizado, logo y español | `LoginActivity`, `firebase_login.xml`, `ic_estudio.xml`, `EstudioApp` |
| Listado personal en tiempo real y cerrar sesión | `SesionesActivity`, `SesionAdapter`, `activity_sesiones.xml` |
| Agregar y editar en una pantalla propia | `FormularioSesionActivity` y `activity_formulario_sesion.xml` |
| Eliminar una sesión | Confirmación en el listado y eliminación en Firestore |
| Barras con horas por mes | `GraficosActivity`, `GraficoHorasView`, `EstadisticasEstudio.porMes` |
| Torta con proporción de horas por actividad | `EstadisticasEstudio.porActividad`; suma horas decimales |
| Filtrar ambos gráficos por curso | Un selector compartido alimenta ambos gráficos y el total |
| Cada usuario solo ve sus sesiones | Ruta bajo UID y reglas de servidor |

Los meses se agrupan por **año-mes**, por ejemplo `2026-11`, para no mezclar noviembre de años distintos. Los cálculos suman **horas**, no cantidades de documentos. El filtro admite «Todos los cursos» y agrupa nombres de curso sin duplicarlos únicamente por mayúsculas.

## Datos y validación

```text
usuarios/{uid}/sesiones/{idAutomatico}
  curso: "Servicios y Aplicaciones para IoT"
  fecha: "2026-11-17"
  horas: 1.5
  tipoActividad: "Laboratorio"
```

`fecha` es texto ISO `yyyy-MM-dd`; la pantalla la muestra `dd/MM/yyyy`. `horas` es un número, admite decimales y debe ser mayor que 0 y como máximo 24 por sesión. El formulario acepta coma o punto decimal. Actividades permitidas: **Teoría**, **Práctica**, **Laboratorio**. Curso obligatorio de hasta 100 caracteres.

El identificador del documento no se guarda como un quinto campo. Los campos se serializan explícitamente desde `SesionEstudio.datos()`. El servidor valida los cuatro campos y rechaza campos extra, horas fuera del rango y accesos de otros usuarios. La app indica cuándo muestra caché o hay escrituras pendientes; guardar en servidor requiere recuperar la conexión.

Las reglas no se publican al compilar el APK: ya se publicaron en este proyecto. Si se reutiliza otro Firebase, publicar allí el contenido de `firestore.rules`. Nunca resolver un error de permisos con una regla global pública.

## Compilación y pruebas

| Herramienta | Versión |
|---|---|
| AGP | 9.0.0 |
| Gradle wrapper | 9.1.0 |
| compileSdk / targetSdk | 36 / 36 |
| minSdk | 34 |
| Nivel del código Java | 11 |
| FirebaseUI | 9.1.1 |

En Git Bash:

```bash
./gradlew.bat :app:assembleDebug testDebugUnitTest lintDebug
./gradlew.bat signingReport
```

En PowerShell:

```powershell
.\gradlew.bat :app:assembleDebug testDebugUnitTest lintDebug
```

Revisar [CONTEXTO_COMPILACION_LAB.md](CONTEXTO_COMPILACION_LAB.md) si el IDE no admite el plugin, falta SDK/JDK o falla la sincronización. La [guía offline con todas las fuentes](docs/GUIA_COMPLETA_OFFLINE.html) reúne README, contexto y código de esta versión.

Las 8 pruebas unitarias cubren horas decimales, filtrado, meses de años diferentes, validación y estado vacío. Las pruebas instrumentadas verifican el CRUD contra Firebase real, rechazo de accesos de otro usuario, listener en tiempo real, formularios, gráficos y flujos de FirebaseUI. Crean únicamente sus propias cuentas ficticias/documentos y los retiran al finalizar.

```bash
# Solo contra lab6-20230420, con Internet y un dispositivo conectado:
./gradlew.bat connectedDebugAndroidTest
```

El login interactivo de Google con una cuenta personal/institucional requiere comprobación manual en el dispositivo; no se simula una sesión de Google como prueba exitosa.

## Probar manualmente

1. Crear cuenta por correo, cerrar sesión y entrar otra vez. Abrir recuperación sin compartir la contraseña.
2. Probar Google con una cuenta y la firma registrada del dispositivo/PC.
3. Crear sesiones de dos cursos, con diferentes actividades y meses; incluir 1,5 horas.
4. Editar curso, fecha, horas y actividad. Verificar el cambio en el listado y en Firestore.
5. Filtrar por cada curso y comprobar que cambian barras, torta y total.
6. Eliminar con confirmación. Probar que una sesión cancelada no se elimina.
7. Entrar con otro usuario y comprobar que no aparecen datos de la primera cuenta.

## GitHub y revisión

Repositorio de esta versión: [a20230420/LAB6_20230420_ESTUDIO](https://github.com/a20230420/LAB6_20230420_ESTUDIO). Se utiliza ese nombre para distinguirla del repositorio `LAB6_20230420` que ya existía en la cuenta. `app/`, `gradle/`, `settings.gradle` y los demás archivos de compilación se encuentran directamente en la raíz; al clonar, abrir esa raíz completa en Android Studio.

Se publican código, XML, wrapper y reglas, manteniendo fuera `google-services.json`, `local.properties`, cachés, builds y claves privadas. Llevar el JSON por separado a la PC del laboratorio y copiarlo en `app/`. El enunciado original solicita el nombre `LAB6_20230420` para su entrega; esta copia de estudio se distingue con el sufijo `_ESTUDIO`.

El PDF también pide visibilidad pública e invitar a `angie.alejandro@pucp.edu.pe` a Firebase para revisar. Este repositorio de estudio es público. **No se ha enviado esa invitación a Firebase.** Si se vuelve a entregar bajo esas condiciones, revisar los permisos necesarios y realizar esa invitación personalmente o autorizarla de manera explícita.
