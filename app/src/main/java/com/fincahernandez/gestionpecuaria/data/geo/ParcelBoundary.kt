package com.fincahernandez.gestionpecuaria.data.geo

import org.json.JSONArray
import org.json.JSONObject

/** Coordenada geográfica WGS84 utilizada por Google Earth y Google Maps. */
data class ParcelGeoPoint(
    val latitude: Double,
    val longitude: Double
)

private const val MAX_BOUNDARY_POINTS = 2_000

/**
 * Convierte una secuencia de puntos a GeoJSON Polygon. El primer punto se repite al
 * final porque GeoJSON representa los límites mediante anillos cerrados.
 */
fun encodeParcelBoundary(points: List<ParcelGeoPoint>): String {
    val normalized = normalizeBoundary(points)
    val ring = JSONArray()
    normalized.forEach { point ->
        // GeoJSON usa longitud primero y latitud después.
        ring.put(JSONArray().put(point.longitude).put(point.latitude))
    }
    return JSONObject()
        .put("type", "Polygon")
        .put("coordinates", JSONArray().put(ring))
        .toString()
}

/** Lee y valida el polígono almacenado antes de entregarlo a las vistas o a Room. */
fun decodeParcelBoundary(geoJson: String): List<ParcelGeoPoint> {
    require(geoJson.length <= 200_000) { "Los límites geográficos son demasiado grandes." }
    val root = runCatching { JSONObject(geoJson) }
        .getOrElse { throw IllegalArgumentException("El GeoJSON de la parcela no es válido.") }
    require(root.optString("type") == "Polygon") {
        "La ubicación debe contener un polígono GeoJSON."
    }
    val rings = root.optJSONArray("coordinates")
        ?: throw IllegalArgumentException("El polígono no contiene coordenadas.")
    val ring = rings.optJSONArray(0)
        ?: throw IllegalArgumentException("El polígono no contiene un límite exterior.")
    require(ring.length() in 4..MAX_BOUNDARY_POINTS) {
        "El polígono debe tener entre 3 y ${MAX_BOUNDARY_POINTS - 1} puntos."
    }
    val points = buildList(ring.length()) {
        repeat(ring.length()) { index ->
            val coordinate = ring.optJSONArray(index)
                ?: throw IllegalArgumentException("Hay una coordenada geográfica inválida.")
            require(coordinate.length() >= 2) { "Hay una coordenada geográfica incompleta." }
            add(
                ParcelGeoPoint(
                    latitude = coordinate.getDouble(1),
                    longitude = coordinate.getDouble(0)
                ).validated()
            )
        }
    }
    require(points.first() == points.last()) { "El polígono geográfico no está cerrado." }
    require(points.dropLast(1).distinct().size >= 3) {
        "Se necesitan al menos tres puntos diferentes para delimitar la parcela."
    }
    return points
}

private fun normalizeBoundary(points: List<ParcelGeoPoint>): List<ParcelGeoPoint> {
    require(points.size <= MAX_BOUNDARY_POINTS) { "El polígono contiene demasiados puntos." }
    val validated = points.map(ParcelGeoPoint::validated)
    val withoutRepeatedEnd = if (
        validated.size > 1 && validated.first() == validated.last()
    ) validated.dropLast(1) else validated
    require(withoutRepeatedEnd.distinct().size >= 3) {
        "Se necesitan al menos tres puntos diferentes para delimitar la parcela."
    }
    return withoutRepeatedEnd + withoutRepeatedEnd.first()
}

private fun ParcelGeoPoint.validated(): ParcelGeoPoint {
    require(latitude.isFinite() && latitude in -90.0..90.0) { "Latitud no válida." }
    require(longitude.isFinite() && longitude in -180.0..180.0) { "Longitud no válida." }
    return this
}
