# Gestión Pecuaria

Aplicación móvil Android para apoyar el control productivo de una finca ganadera. El proyecto busca centralizar el registro de animales, lotes y pesajes, conservar el historial de cada ejemplar y facilitar la consulta de información desde el teléfono.

Actualmente se encuentra en desarrollo como parte de un proyecto de graduación y cuenta con un flujo funcional de demostración para la gestión de animales.

## Funciones disponibles

### Módulo de animales

- Visualización del inventario de animales mediante tarjetas.
- Búsqueda local por código de identificación, nombre o categoría.
- Registro de un animal mediante un formulario con:
  - código de identificación;
  - nombre;
  - raza y sexo;
  - categoría productiva;
  - origen del animal;
  - fecha de nacimiento o ingreso;
  - estado de salud;
  - peso inicial;
  - procedencia y observaciones.
- Validación de los campos obligatorios antes de confirmar el registro.
- Selección de fechas mediante calendario.
- Pantalla de confirmación del registro.
- Consulta del perfil y resumen productivo del animal.
- Navegación entre listado, formulario, confirmación y detalle.
- Interfaz adaptable con Material Design 3 y soporte para tema claro u oscuro.

> [!IMPORTANT]
> El inventario visible utiliza datos temporales conservados en memoria. Los registros se pierden al cerrar la aplicación porque la interfaz todavía no está conectada a Room.

## Persistencia local preparada

El proyecto incluye una base de datos Room y operaciones de acceso a datos para:

- registrar, actualizar, consultar y desactivar animales sin borrar su historial;
- crear, actualizar y cerrar lotes;
- asignar animales a lotes y conservar el historial de movimientos;
- impedir que un animal pertenezca a dos lotes activos simultáneamente;
- registrar y actualizar pesajes individuales;
- consultar el último pesaje y el historial de un animal;
- consultar pesajes y calcular el peso promedio de un lote por período;
- evitar códigos duplicados y pesos inválidos mediante restricciones y validaciones.

Esta capa se encuentra implementada, pero aún debe integrarse con `ViewModel` y las pantallas de la aplicación.

## Tecnologías utilizadas

- Kotlin 2.0.21
- Android SDK 35 (Android 7.0 o superior, API 24+)
- Jetpack Compose
- Material Design 3
- Navigation Compose
- Room Database
- Kotlin Coroutines y Flow
- KSP
- Gradle con Kotlin DSL

## Arquitectura actual

El código está organizado por responsabilidad:

```text
app/src/main/java/com/fincahernandez/gestionpecuaria/
├── MainActivity.kt
├── data/local/
│   ├── dao/                 # Consultas y operaciones de Room
│   ├── database/            # Configuración de la base de datos
│   └── entity/              # Animales, lotes, asignaciones y pesajes
└── ui/
    ├── components/          # Componentes reutilizables
    ├── navigation/          # Rutas y flujo de navegación
    ├── screens/animals/     # Pantallas del módulo de animales
    └── theme/               # Colores, tipografía y tema visual
```

## Modelo de datos

La base local contiene cuatro entidades principales:

| Entidad | Descripción |
| --- | --- |
| `AnimalEntity` | Información general y estado de cada animal. |
| `LoteEntity` | Datos de los grupos o lotes de la finca. |
| `LoteAnimalEntity` | Historial de ingreso y salida de animales en los lotes. |
| `PesajeEntity` | Mediciones de peso relacionadas con animales y, opcionalmente, lotes. |

## Requisitos

- Android Studio compatible con Android Gradle Plugin 8.9.1.
- JDK 17 o 21. Android Studio incluye un JDK compatible.
- Android SDK 35 instalado.
- Un emulador o dispositivo con Android 7.0 (API 24) o superior.

## Instalación y ejecución

1. Clona el repositorio:

   ```bash
   git clone URL_DE_TU_REPOSITORIO
   ```

2. Abre la carpeta del proyecto en Android Studio.
3. Espera a que Gradle descargue y sincronice las dependencias.
4. Selecciona un emulador o conecta un dispositivo Android.
5. Ejecuta el módulo `app` con el botón **Run**.

También puedes compilar una versión de depuración desde la terminal:

```bash
# Windows
gradlew.bat assembleDebug

# macOS o Linux
./gradlew assembleDebug
```

El APK generado se encontrará en `app/build/outputs/apk/debug/`.

## Pruebas

Para ejecutar las pruebas unitarias:

```bash
# Windows
gradlew.bat test

# macOS o Linux
./gradlew test
```

Para las pruebas instrumentadas, inicia un emulador o conecta un dispositivo y ejecuta:

```bash
# Windows
gradlew.bat connectedAndroidTest

# macOS o Linux
./gradlew connectedAndroidTest
```

## Estado del proyecto

### Implementado

- Diseño y navegación del flujo principal de animales.
- Registro temporal, búsqueda y consulta visual de animales.
- Esquema local de Room para animales, lotes, asignaciones y pesajes.
- DAO con consultas y reglas básicas del dominio.

### Próximos pasos

- Conectar las pantallas con Room mediante repositorios y `ViewModel`.
- Implementar la edición de animales y el registro de pesajes desde la interfaz.
- Crear las pantallas de lotes y movimientos.
- Activar los módulos de inicio, tareas y mapa.
- Incorporar los módulos sanitario y financiero.
- Sustituir los datos de demostración por información persistente.
- Ampliar la cobertura de pruebas unitarias e instrumentadas.

## Contexto académico

Este repositorio forma parte de la propuesta de un sistema automatizado para el control productivo y financiero de una finca ganadera. Su desarrollo se realiza de manera incremental, por lo que algunas funciones representan prototipos o componentes preparados para iteraciones posteriores.

## Autoría

Proyecto desarrollado con fines académicos para la Universidad Mariano Gálvez de Guatemala.

---

Si deseas usar este proyecto como referencia, puedes crear un *fork* o abrir una incidencia con tus comentarios.
