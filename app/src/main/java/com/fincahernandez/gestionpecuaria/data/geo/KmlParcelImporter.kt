package com.fincahernandez.gestionpecuaria.data.geo

import android.content.Context
import android.net.Uri
import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

/** Límite encontrado dentro de un archivo exportado por Google Earth. */
data class ImportedParcelBoundary(
    val name: String,
    val geoJson: String,
    val pointCount: Int
)

private const val MAX_SOURCE_BYTES = 5 * 1024 * 1024
private const val MAX_KML_BYTES = 2 * 1024 * 1024
private const val MAX_IMPORTED_POLYGONS = 100

/**
 * Importa KML o KMZ usando el selector seguro de Android. No se conserva acceso al
 * archivo: las coordenadas se convierten a GeoJSON para sincronizarlas entre equipos.
 */
fun importParcelBoundaries(context: Context, uri: Uri): List<ImportedParcelBoundary> {
    val source = context.contentResolver.openInputStream(uri)
        ?.use { it.readBounded(MAX_SOURCE_BYTES, "El archivo KML/KMZ supera 5 MB.") }
        ?: throw IllegalArgumentException("No se pudo abrir el archivo seleccionado.")
    val kml = if (source.isZipFile()) extractKmlFromKmz(source) else source
    return parseKmlParcelBoundaries(kml).also { boundaries ->
        require(boundaries.isNotEmpty()) {
            "No se encontró ningún polígono de parcela en el archivo."
        }
    }
}

/** Lee los Polygon/coordinates estándar producidos por Google Earth. */
private fun parseKmlParcelBoundaries(kml: ByteArray): List<ImportedParcelBoundary> {
    require(kml.size <= MAX_KML_BYTES) { "El contenido KML supera 2 MB." }
    val parser = Xml.newPullParser().apply {
        setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        // null permite respetar UTF-8, UTF-16 o la codificación declarada por el KML.
        setInput(ByteArrayInputStream(kml), null)
    }
    val results = mutableListOf<ImportedParcelBoundary>()
    var placemarkDepth = -1
    var polygonDepth = -1
    var polygonCaptured = false
    var placemarkName = "Parcela sin nombre"
    var event = parser.eventType
    while (event != XmlPullParser.END_DOCUMENT) {
        when (event) {
            XmlPullParser.START_TAG -> when (parser.name.substringAfter(':')) {
                "Placemark" -> {
                    placemarkDepth = parser.depth
                    placemarkName = "Parcela sin nombre"
                }
                "name" -> if (placemarkDepth > 0 && polygonDepth < 0) {
                    placemarkName = parser.nextText().trim().ifBlank { "Parcela sin nombre" }
                }
                "Polygon" -> {
                    polygonDepth = parser.depth
                    polygonCaptured = false
                }
                // El primer coordinates de Polygon corresponde al límite exterior;
                // los anillos interiores no representan parcelas independientes.
                "coordinates" -> if (polygonDepth > 0 && !polygonCaptured) {
                    polygonCaptured = true
                    val points = parseCoordinateText(parser.nextText())
                    if (points.size >= 3) {
                        require(results.size < MAX_IMPORTED_POLYGONS) {
                            "El archivo contiene más de $MAX_IMPORTED_POLYGONS polígonos."
                        }
                        results += ImportedParcelBoundary(
                            name = placemarkName,
                            geoJson = encodeParcelBoundary(points),
                            pointCount = points.let {
                                if (it.firstOrNull() == it.lastOrNull()) it.size - 1 else it.size
                            }
                        )
                    }
                }
            }
            XmlPullParser.END_TAG -> when (parser.name.substringAfter(':')) {
                "Polygon" -> if (parser.depth == polygonDepth) polygonDepth = -1
                "Placemark" -> if (parser.depth == placemarkDepth) placemarkDepth = -1
            }
        }
        event = parser.next()
    }
    return results
}

private fun parseCoordinateText(value: String): List<ParcelGeoPoint> = value
    .trim()
    .split(Regex("\\s+"))
    .filter(String::isNotBlank)
    .map { tuple ->
        val values = tuple.split(',')
        require(values.size >= 2) { "El KML contiene una coordenada incompleta." }
        ParcelGeoPoint(
            latitude = values[1].toDoubleOrNull()
                ?: throw IllegalArgumentException("El KML contiene una latitud inválida."),
            longitude = values[0].toDoubleOrNull()
                ?: throw IllegalArgumentException("El KML contiene una longitud inválida.")
        )
    }

private fun extractKmlFromKmz(source: ByteArray): ByteArray {
    ZipInputStream(ByteArrayInputStream(source)).use { zip ->
        while (true) {
            val entry = zip.nextEntry ?: break
            if (!entry.isDirectory && entry.name.endsWith(".kml", ignoreCase = true)) {
                return zip.readBounded(MAX_KML_BYTES, "El KML comprimido supera 2 MB.")
            }
        }
    }
    throw IllegalArgumentException("El archivo KMZ no contiene un documento KML.")
}

private fun ByteArray.isZipFile(): Boolean =
    size >= 4 && this[0] == 0x50.toByte() && this[1] == 0x4B.toByte()

private fun InputStream.readBounded(maxBytes: Int, message: String): ByteArray {
    val buffer = ByteArray(8_192)
    val output = java.io.ByteArrayOutputStream()
    while (true) {
        val count = read(buffer)
        if (count < 0) break
        require(output.size() + count <= maxBytes) { message }
        output.write(buffer, 0, count)
    }
    return output.toByteArray()
}
