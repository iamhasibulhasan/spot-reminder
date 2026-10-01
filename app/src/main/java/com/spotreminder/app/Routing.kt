package com.spotreminder.app

import org.json.JSONObject
import org.osmdroid.util.GeoPoint
import java.net.HttpURLConnection
import java.net.URL

/** A driving route between two points: the road geometry plus total distance/time. */
data class RouteResult(val points: List<GeoPoint>, val distanceMeters: Double, val durationSeconds: Double)

/**
 * Free public OSRM demo server for turn-free driving directions (distance + time), geocoded via
 * OpenStreetMap road data. It has no live traffic — that needs a paid API (Google Directions,
 * Mapbox, etc.) which this project doesn't use. Best-effort/rate-limited; not for heavy/production use.
 */
object Routing {
    private const val BASE = "https://router.project-osrm.org/route/v1/driving"

    fun route(from: GeoPoint, to: GeoPoint): RouteResult? {
        return try {
            val url = "$BASE/${from.longitude},${from.latitude};${to.longitude},${to.latitude}" +
                "?overview=full&geometries=geojson"
            val c = URL(url).openConnection() as HttpURLConnection
            c.setRequestProperty("User-Agent", "SpotReminderApp/1.0")
            c.connectTimeout = 12000
            c.readTimeout = 12000
            val text = c.inputStream.bufferedReader().use { it.readText() }
            val root = JSONObject(text)
            if (root.optString("code") != "Ok") return null
            val route = root.getJSONArray("routes").getJSONObject(0)
            val coords = route.getJSONObject("geometry").getJSONArray("coordinates")
            val points = (0 until coords.length()).map { i ->
                val pair = coords.getJSONArray(i)
                GeoPoint(pair.getDouble(1), pair.getDouble(0))
            }
            RouteResult(points, route.getDouble("distance"), route.getDouble("duration"))
        } catch (e: Exception) {
            null
        }
    }
}
