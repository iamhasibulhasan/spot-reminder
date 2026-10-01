package com.spotreminder.app

import android.graphics.Color
import android.graphics.DashPathEffect
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.util.concurrent.Executors

/**
 * Shows your current location and a saved spot on a map, with the driving route, distance and
 * estimated time between them (via the free OSRM routing service — no live traffic; that needs a
 * paid API this project doesn't use). Falls back to a straight line if no route can be fetched.
 */
class NavigateActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private lateinit var subtitle: TextView
    private lateinit var distanceText: TextView
    private lateinit var timeText: TextView
    private lateinit var noteText: TextView

    private val io = Executors.newSingleThreadExecutor()
    private var mePoint: GeoPoint? = null
    private var destPoint: GeoPoint? = null
    private var spotName: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cfg = Configuration.getInstance()
        cfg.userAgentValue = packageName
        cfg.osmdroidBasePath = File(filesDir, "osmdroid")
        cfg.osmdroidTileCache = File(cacheDir, "osmdroid-tiles")

        setContentView(R.layout.activity_navigate)
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)
        map.controller.setZoom(5.0)

        subtitle = findViewById(R.id.navSubtitle)
        distanceText = findViewById(R.id.navDistance)
        timeText = findViewById(R.id.navTime)
        noteText = findViewById(R.id.navNote)

        val cityKey = intent.getStringExtra("cityKey").orEmpty()
        val cityName = intent.getStringExtra("cityName").orEmpty()
        spotName = intent.getStringExtra("spotName").orEmpty()

        findViewById<TextView>(R.id.navTitle).text = "$spotName · $cityName"
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnRecenter).setOnClickListener { recenter() }

        subtitle.text = "Finding your location and the route…"

        if (!LocationChecker.hasLocationPermission(this)) {
            subtitle.text = "Location is off"
            noteText.text = "Turn on location in Settings to navigate from where you are."
            return
        }

        io.execute {
            val loc = LocationChecker.currentLocation(this)
            val dest = resolveSpotLocation(cityKey, cityName, spotName)
            runOnUiThread {
                when {
                    loc == null -> {
                        subtitle.text = "Couldn't get your location"
                        noteText.text = "Try again outdoors, or with location turned on."
                    }
                    dest == null -> {
                        subtitle.text = "Couldn't find \"$spotName\""
                        noteText.text = "No saved location for this spot, and searching for it found nothing."
                    }
                    else -> {
                        mePoint = GeoPoint(loc.latitude, loc.longitude)
                        destPoint = dest
                        showPoints()
                        fetchRoute()
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        map.onResume()
    }

    override fun onPause() {
        map.onPause()
        super.onPause()
    }

    override fun onDestroy() {
        map.onDetach()
        super.onDestroy()
    }

    /** Spot's own saved coordinates, else a fresh geocode (cached back onto the spot), else the city's. */
    private fun resolveSpotLocation(cityKey: String, cityName: String, spotName: String): GeoPoint? {
        val all = Store.load(this)
        val city = all.firstOrNull { it.key == cityKey }
        val spot = city?.spots?.firstOrNull { it.name == spotName }
        val savedLat = spot?.lat
        val savedLon = spot?.lon
        if (savedLat != null && savedLon != null) return GeoPoint(savedLat, savedLon)

        val hit = Osm.search("$spotName, $cityName")
        if (hit != null) {
            if (spot != null) {
                spot.lat = hit.lat
                spot.lon = hit.lon
                Store.save(this, all)
            }
            return GeoPoint(hit.lat, hit.lon)
        }

        val cityLat = city?.lat
        val cityLon = city?.lon
        return if (cityLat != null && cityLon != null) GeoPoint(cityLat, cityLon) else null
    }

    private fun showPoints() {
        val me = mePoint ?: return
        val dest = destPoint ?: return
        map.overlays.clear()
        map.overlays.add(Marker(map).apply {
            position = me
            title = "You"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(this@NavigateActivity, R.drawable.ic_map_pin_large)
                ?.mutate()?.apply { setTint(ContextCompat.getColor(this@NavigateActivity, R.color.river)) }
        })
        map.overlays.add(Marker(map).apply {
            position = dest
            title = spotName
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
            icon = ContextCompat.getDrawable(this@NavigateActivity, R.drawable.ic_map_pin_large)
                ?.mutate()?.apply { setTint(ContextCompat.getColor(this@NavigateActivity, R.color.accent)) }
        })
        map.invalidate()
        recenter()
    }

    private fun recenter() {
        val me = mePoint ?: return
        val dest = destPoint ?: return
        map.post {
            val lats = listOf(me.latitude, dest.latitude)
            val lons = listOf(me.longitude, dest.longitude)
            val box = BoundingBox(lats.max(), lons.max(), lats.min(), lons.min())
            map.zoomToBoundingBox(box, true, 120)
        }
    }

    private fun fetchRoute() {
        val me = mePoint ?: return
        val dest = destPoint ?: return
        io.execute {
            val result = Routing.route(me, dest)
            runOnUiThread {
                if (result != null) {
                    map.overlays.add(0, Polyline(map).apply {
                        setPoints(result.points)
                        outlinePaint.color = Color.parseColor("#2F6FED")
                        outlinePaint.strokeWidth = 12f
                    })
                    map.invalidate()
                    subtitle.text = "Driving route"
                    distanceText.text = formatDistance(result.distanceMeters)
                    timeText.text = formatDuration(result.durationSeconds)
                    noteText.text = "Estimated from typical road speeds — not live traffic."
                } else {
                    map.overlays.add(0, Polyline(map).apply {
                        setPoints(listOf(me, dest))
                        outlinePaint.color = Color.parseColor("#6B7280")
                        outlinePaint.strokeWidth = 6f
                        outlinePaint.pathEffect = DashPathEffect(floatArrayOf(20f, 14f), 0f)
                    })
                    map.invalidate()
                    val km = Trip.haversineKm(
                        TrackPoint(me.latitude, me.longitude, 0L),
                        TrackPoint(dest.latitude, dest.longitude, 0L)
                    )
                    subtitle.text = "Straight-line distance only"
                    distanceText.text = "%.1f km".format(km)
                    timeText.text = ""
                    noteText.text = "Couldn't fetch a driving route (check your internet). Showing a straight line instead."
                }
            }
        }
    }

    private fun formatDistance(meters: Double): String =
        if (meters >= 1000) "%.1f km".format(meters / 1000.0) else "%.0f m".format(meters)

    private fun formatDuration(seconds: Double): String {
        val totalMin = (seconds / 60).toInt()
        return if (totalMin >= 60) "${totalMin / 60} h ${totalMin % 60} min" else "$totalMin min"
    }
}
