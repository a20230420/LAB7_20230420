# Contexto de LAB6_20230420 para otra PC o una modificación posterior

## Objetivo y ubicación

Esta carpeta contiene el **Lab6 real de sesiones de estudio**, de 1TEL05 2026-2. Es la base de trabajo para un laboratorio posterior. No confundir con el ejemplo de inventario que está en la carpeta hermana `PracticaLab6`.

Java + XML + Groovy, cuatro pantallas: login, listado, formulario agregar/editar y gráficos. FirebaseUI 9.1.1 para correo/contraseña y Google; Cloud Firestore para los datos privados. Las funciones de este Lab6 se describen en README.

## Valores que deben conservarse al abrir

- Raíz: carpeta que contiene `settings.gradle`, `gradlew.bat` y `app/`.
- Módulo: `app`.
- Namespace y applicationId: **`com.example.lab06_20230420`**.
- Firebase elegido: **`lab6-20230420`** (no lleva el cero de `lab06`).
- AGP **9.0.0** en `build.gradle` raíz.
- Gradle **9.1.0** en `gradle/wrapper/gradle-wrapper.properties`, con su checksum asociado.
- compileSdk/targetSdk **36**, minSdk **34** (Android 14).
- Código Java **11**, Gradle JDK compatible; JDK **21** sirve para este conjunto. El proyecto se compiló localmente con el JBR 25 instalado.
- `buildFeatures { viewBinding true }`.
- Repositorios `google()` y `mavenCentral()`; plugins también `gradlePluginPortal()`.

Una captura anterior del laboratorio indicaba que su IDE admitía hasta AGP **9.1.0** y rechazaba AGP **9.3.3**. Por eso esta base conserva AGP 9.0.0. La versión del IDE instalado el día de uso se debe comprobar si vuelve a fallar.

## Cambiar la configuración según el error

| Error | Ajuste |
|---|---|
| IDE no admite AGP | Revisar `build.gradle` raíz y máximo permitido por ese Studio. Restaurar 9.0.0 si una actualización cambió el valor. |
| Gradle incompatible con AGP | Conservar el wrapper 9.1.0 para AGP 9.0.0. Si se cambia la distribución, revisar también el SHA-256 de su ZIP. |
| Error de JDK/JAVA_HOME | Settings > Build, Execution, Deployment > Build Tools > Gradle > Gradle JDK. Usar un JDK compatible instalado en esa PC, por ejemplo 21. |
| SDK location not found | Configurar el SDK local. No copiar la ruta de `local.properties` de otra PC. |
| Falta android-36 | Instalar SDK Platform 36 y Build Tools 36.0.0. minSdk 34 no reemplaza compileSdk 36. |
| Dependencia exige compileSdk mayor | Leer cuál lo pide. No bajar compileSdk a 34 para hacerlo coincidir con el emulador. |
| Could not resolve / timeout | Internet, proxy, repositorios o modo offline de Gradle. Un clon nuevo necesita descargar dependencias. |
| No matching client found for package name | El JSON debe contener exactamente `com.example.lab06_20230420`. Descargar el de la app registrada correcta. |
| Falta JSON / default_web_client_id | Agregar `app/google-services.json` actualizado después de habilitar Google y registrar SHA. Sin OAuth válido el login muestra un aviso. |
| Google falla solo en lab | Ejecutar `signingReport`, registrar las huellas de ESA firma y actualizar el JSON. No bajar AGP para corregir OAuth. |
| Firestore PERMISSION_DENIED | Revisar usuario, ruta `usuarios/uid/sesiones` y reglas. No abrir públicamente la base. |
| Run deshabilitado | Resolver Gradle Sync y abrir la raíz completa, luego seleccionar `app`. |

El `google-services.json` local está excluido de Git y se lleva por separado. Esta versión no necesita `facebook-config.properties`. No guardar cuentas de servicio ni claves privadas dentro del APK o repositorio.

No existe un cambio universal de una sola propiedad: elegir el ajuste por el mensaje de error, conservando un conjunto compatible de IDE + AGP + wrapper + JDK + SDK y dependencias.

## Diagnosticar y verificar

En Git Bash:

```bash
./gradlew.bat --version
./gradlew.bat :app:assembleDebug --stacktrace
```

En PowerShell:

```powershell
.\gradlew.bat --version
.\gradlew.bat :app:assembleDebug --stacktrace
```

Al pedir ayuda, incluir versión de Android Studio, salida de `--version` y el primer bloque `What went wrong`/causa relevante. No compartir contraseñas ni claves privadas. Confirmar que la app abre en API 34 o superior después del ajuste.

## Mapa de clases

- `EstudioApp`: idioma español para toda la app, incluidas las pantallas FirebaseUI.
- `LoginActivity`: FirebaseUI con `firebase_login.xml`, logo, correo y Google, retorno a la sesión existente.
- `PantallaPrivada`: comprueba identidad, redirige si cambia el usuario y aplica insets.
- `SesionesActivity`: lista, listeners de ciclo de vida, alta/edición, confirmación de borrado y cierre de sesión.
- `SesionAdapter`: tarjetas en RecyclerView, fecha/horas/actividad y botones.
- `FormularioSesionActivity`: alta/edición con fecha ISO, curso, horas decimales y actividad.
- `GraficosActivity`: filtro por curso que alimenta ambos gráficos.
- `GraficoHorasView`: barras y torta con Canvas, descripción para accesibilidad.
- `EstadisticasEstudio`: validación, filtrado y **suma de horas**, agrupando meses por año-mes.
- `SesionRepository`: operaciones Firestore bajo UID; recibe snapshots con metadatos de caché y escrituras pendientes.
- `SesionEstudio`: cuatro campos Firestore; ID obtenido del documento.
- `firestore.rules`: autorización real en servidor y validación del esquema.

Conservar la separación entre presentación, cálculos y acceso a datos al modificar la base. Cambiar la configuración de compilación no requiere migrar la app a Kotlin o Compose.
