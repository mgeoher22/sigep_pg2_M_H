package com.fincahernandez.gestionpecuaria.ui.screens.parcels

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Landscape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.fincahernandez.gestionpecuaria.BuildConfig
import com.fincahernandez.gestionpecuaria.data.geo.decodeParcelBoundary
import com.fincahernandez.gestionpecuaria.ui.components.BrandedTopAppBar
import org.maplibre.android.MapLibre
import org.maplibre.android.annotations.PolygonOptions
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.geometry.LatLngBounds
import org.maplibre.android.maps.MapView

/**
 * Mapa satelital reutilizable. Las imágenes provienen de MapTiler y MapLibre se
 * encarga de dibujarlas sin depender de los servicios facturables de Google.
 */
@Composable
fun ParcelSatelliteMap(
    boundaryGeoJson: String,
    modifier: Modifier = Modifier,
    liteMode: Boolean = true,
    onMapClick: (() -> Unit)? = null
) {
    val points = remember(boundaryGeoJson) {
        runCatching { decodeParcelBoundary(boundaryGeoJson) }.getOrDefault(emptyList())
    }
    if (BuildConfig.MAPTILER_API_KEY.isBlank() || points.isEmpty()) {
        ParcelMapUnavailable(
            message = if (points.isEmpty()) "Límites geográficos no disponibles"
            else "Configura la clave gratuita de MapTiler para ver la imagen satelital",
            modifier = modifier
        )
        return
    }

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val mapView = remember(liteMode) {
        // La inicialización no requiere token de MapLibre; MapTiler autentica las teselas.
        MapLibre.getInstance(context.applicationContext)
        MapView(context).apply { onCreate(null) }
    }
    val mapConfigured = remember(mapView, boundaryGeoJson) {
        java.util.concurrent.atomic.AtomicBoolean(false)
    }

    // MapView no es Compose y necesita recibir el ciclo de vida de la pantalla.
    DisposableEffect(mapView, lifecycleOwner) {
        var destroyed = false
        var started = false
        var resumed = false
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_START -> {
                    mapView.onStart()
                    started = true
                }
                Lifecycle.Event.ON_RESUME -> {
                    mapView.onResume()
                    resumed = true
                }
                Lifecycle.Event.ON_PAUSE -> {
                    mapView.onPause()
                    resumed = false
                }
                Lifecycle.Event.ON_STOP -> {
                    mapView.onStop()
                    started = false
                }
                Lifecycle.Event.ON_DESTROY -> {
                    mapView.onDestroy()
                    destroyed = true
                }
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            if (!destroyed) {
                if (resumed) mapView.onPause()
                if (started) mapView.onStop()
                mapView.onDestroy()
            }
        }
    }

    Box(modifier = modifier) {
        AndroidView(
            factory = { mapView },
            modifier = Modifier.fillMaxSize(),
            update = { view ->
                view.getMapAsync { map ->
                    // Evita volver a descargar el estilo cada vez que Compose recompone la tarjeta.
                    if (!mapConfigured.compareAndSet(false, true)) return@getMapAsync
                    val coordinates = points.map { LatLng(it.latitude, it.longitude) }
                    map.uiSettings.apply {
                        isScrollGesturesEnabled = !liteMode
                        isZoomGesturesEnabled = !liteMode
                        isRotateGesturesEnabled = !liteMode
                        isTiltGesturesEnabled = !liteMode
                        isCompassEnabled = !liteMode
                        // El logo y la atribución permanecen visibles por licencia.
                        isLogoEnabled = true
                        isAttributionEnabled = true
                    }
                    val key = android.net.Uri.encode(BuildConfig.MAPTILER_API_KEY)
                    map.setStyle("https://api.maptiler.com/maps/satellite-v4/style.json?key=$key") {
                        map.clear()
                        @Suppress("DEPRECATION")
                        map.addPolygon(
                            PolygonOptions()
                                .addAll(coordinates)
                                .strokeColor(0xFF1B6E2A.toInt())
                                .fillColor(0x381B6E2A)
                        )
                        val bounds = LatLngBounds.fromLatLngs(coordinates)
                        view.post {
                            if (view.width > 0 && view.height > 0) {
                                map.moveCamera(
                                    CameraUpdateFactory.newLatLngBounds(
                                        bounds,
                                        if (liteMode) 28 else 64
                                    )
                                )
                            }
                        }
                    }
                }
            }
        )
        // En miniaturas un toque abre la ficha y no intenta mover el mapa.
        if (liteMode && onMapClick != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(onClick = onMapClick)
            )
        }
    }
}

@Composable
fun ParcelMapUnavailable(message: String, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(Icons.Default.Landscape, contentDescription = null)
            Text(message, modifier = Modifier.padding(12.dp), color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** Vista ampliada accesible desde la ficha de la parcela. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ParcelMapScreen(parcel: ParcelUiModel, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            BrandedTopAppBar(
                title = { Text("Ubicación · ${parcel.name}") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Regresar")
                    }
                }
            )
        }
    ) { padding ->
        val boundary = parcel.boundaryGeoJson
        if (boundary == null) {
            ParcelMapUnavailable(
                message = "Esta parcela todavía no tiene límites importados",
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        } else {
            ParcelSatelliteMap(
                boundaryGeoJson = boundary,
                liteMode = false,
                modifier = Modifier.fillMaxSize().padding(padding)
            )
        }
    }
}
