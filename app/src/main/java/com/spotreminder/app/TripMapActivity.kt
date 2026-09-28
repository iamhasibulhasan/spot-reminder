package com.spotreminder.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import org.osmdroid.config.Configuration
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.BoundingBox
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.Marker
import org.osmdroid.views.overlay.Polyline
import java.io.File
import java.io.FileOutputStream

class TripMapActivity : AppCompatActivity() {

    private lateinit var map: MapView
    private var trip: Trip? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val cfg = Configuration.getInstance()
        cfg.userAgentValue = packageName
        cfg.osmdroidBasePath = File(filesDir, "osmdroid")
        cfg.osmdroidTileCache = File(cacheDir, "osmdroid-tiles")

        setContentView(R.layout.activity_trip_map)
        map = findViewById(R.id.map)
        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnShare).setOnClickListener { shareRoute() }

        val tripId = intent.getStringExtra("tripId")
        val t = TripStore.loadTrips(this).firstOrNull { it.id == tripId }
        trip = t
        if (t == null || t.path.size < 2) {
            findViewById<TextView>(R.id.mapTitle).text = "Route unavailable"
            findViewById<TextView>(R.id.routeStats).text =
                "No recorded path for this trip (location may have been off during travel)."
            findViewById<View>(R.id.btnShare).isEnabled = false
            return
        }

        findViewById<TextView>(R.id.mapTitle).text = t.spotName + " · " + t.cityName
        findViewById<TextView>(R.id.routeStats).text =
            "%.1f km · %d min · %.0f total".format(t.distanceKm, t.durationMinutes, t.totalCost)

        val points = t.path.map { GeoPoint(it.lat, it.lon) }
        val line = Polyline(map).apply {
            setPoints(points)
            outlinePaint.color = Color.parseColor("#F2A516")
            outlinePaint.strokeWidth = 10f
        }
        map.overlays.add(line)

        val startMarker = Marker(map).apply {
            position = points.first()
            title = "Start"
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        val endMarker = Marker(map).apply {
            position = points.last()
            title = "End: " + t.spotName
            setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        }
        map.overlays.add(startMarker)
        map.overlays.add(endMarker)

        map.post {
            val lats = points.map { it.latitude }
            val lons = points.map { it.longitude }
            val box = BoundingBox(lats.max(), lons.max(), lats.min(), lons.min())
            map.zoomToBoundingBox(box, false, 80)
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

    private fun shareRoute() {
        val t = trip ?: return
        try {
            val bmp = Bitmap.createBitmap(map.width, map.height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            map.draw(canvas)

            val dir = File(cacheDir, "trip_maps").apply { mkdirs() }
            val file = File(dir, "trip_${t.id}.png")
            FileOutputStream(file).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }

            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", file)
            val send = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(
                    Intent.EXTRA_TEXT,
                    "My trip to ${t.spotName}, ${t.cityName} — ${"%.1f".format(t.distanceKm)} km"
                )
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(send, "Share route"))
        } catch (e: Exception) {
            Toast.makeText(this, "Couldn't create the share image.", Toast.LENGTH_LONG).show()
        }
    }
}
