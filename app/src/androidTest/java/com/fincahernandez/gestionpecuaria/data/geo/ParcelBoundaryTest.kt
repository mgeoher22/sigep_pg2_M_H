package com.fincahernandez.gestionpecuaria.data.geo

import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Valida el mismo analizador JSON que ejecutará la aplicación en Android. */
@RunWith(AndroidJUnit4::class)
class ParcelBoundaryTest {
    @Test
    fun encodeAndDecode_closesPolygonAndPreservesCoordinates() {
        val original = listOf(
            ParcelGeoPoint(14.6201, -90.5401),
            ParcelGeoPoint(14.6201, -90.5391),
            ParcelGeoPoint(14.6191, -90.5391)
        )

        val decoded = decodeParcelBoundary(encodeParcelBoundary(original))

        assertEquals(4, decoded.size)
        assertEquals(original, decoded.dropLast(1))
        assertEquals(decoded.first(), decoded.last())
    }

    @Test
    fun decode_rejectsCoordinatesOutsideWgs84() {
        val invalid = """{"type":"Polygon","coordinates":[[[-90,95],[-89,14],[-90,13],[-90,95]]]}"""

        assertTrue(runCatching { decodeParcelBoundary(invalid) }.isFailure)
    }
}
