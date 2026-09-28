package com.spotreminder.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.AutoCompleteTextView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.button.MaterialButton
import org.osmdroid.config.Configuration
import org.osmdroid.events.MapEventsReceiver
import org.osmdroid.tileprovider.tilesource.TileSourceFactory
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.MapView
import org.osmdroid.views.overlay.MapEventsOverlay
import org.osmdroid.views.overlay.Marker
import java.io.File
import java.util.concurrent.Executors

/** OpenStreetMap screen: search a city (with suggestions) or tap the map, then return the city name. */
class MapPickerActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_CITY = "city"
        const val EXTRA_LAT = "lat"
        const val EXTRA_LON = "lon"
    }

    private lateinit var map: MapView
    private lateinit var searchInput: AutoCompleteTextView
    private lateinit var resultText: TextView
    private lateinit var btnUse: MaterialButton
    private lateinit var searchAdapter: NoFilterAdapter

    private val io = Executors.newSingleThreadExecutor()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var suggestRunnable: Runnable? = null
    private var suggestions: List<Suggestion> = emptyList()
    private var suppressWatcher = false

    private var marker: Marker? = null
    private var pickedName: String? = null
    private var pickedLat: Double? = null
    private var pickedLon: Double? = null
    private var requestId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Keep map tiles inside the app's own folders (no storage permission needed)
        val cfg = Configuration.getInstance()
        cfg.userAgentValue = packageName
        cfg.osmdroidBasePath = File(filesDir, "osmdroid")
        cfg.osmdroidTileCache = File(cacheDir, "osmdroid-tiles")

        setContentView(R.layout.activity_map)

        map = findViewById(R.id.map)
        searchInput = findViewById(R.id.searchInput)
        resultText = findViewById(R.id.resultText)
        btnUse = findViewById(R.id.btnUse)

        map.setTileSource(TileSourceFactory.MAPNIK)
        map.setMultiTouchControls(true)

        val start = lastKnown()
        if (start != null) {
            map.controller.setZoom(11.0)
            map.controller.setCenter(start)
        } else {
            map.controller.setZoom(3.0)
            map.controller.setCenter(GeoPoint(20.0, 0.0))
        }

        val receiver = object : MapEventsReceiver {
            override fun singleTapConfirmedHelper(p: GeoPoint): Boolean {
                pick(p)
                return true
            }
            override fun longPressHelper(p: GeoPoint): Boolean = false
        }
        map.overlays.add(0, MapEventsOverlay(receiver))

        searchAdapter = NoFilterAdapter(this)
        searchInput.setAdapter(searchAdapter)
        searchInput.threshold = 2
        searchInput.setOnItemClickListener { _, _, position, _ ->
            val s = suggestions.getOrNull(position) ?: return@setOnItemClickListener
            suppressWatcher = true
            searchInput.setText(s.name)
            searchInput.setSelection(searchInput.text?.length ?: 0)
            searchInput.dismissDropDown()
            suppressWatcher = false
            (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
                .hideSoftInputFromWindow(searchInput.windowToken, 0)
            val p = GeoPoint(s.lat, s.lon)
            placeMarker(p)
            map.controller.setZoom(12.0)
            map.controller.animateTo(p)
            showResult(s.name, s.lat, s.lon)
        }
        searchInput.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressWatcher) return
                val q = s?.toString().orEmpty()
                suggestRunnable?.let { mainHandler.removeCallbacks(it) }
                val r = Runnable { fetchSuggestions(q) }
                suggestRunnable = r
                mainHandler.postDelayed(r, 350)
            }
        })

        findViewById<MaterialButton>(R.id.btnSearch).setOnClickListener { doSearch() }
        searchInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) { doSearch(); true } else false
        }

        btnUse.setOnClickListener {
            val name = pickedName
            if (!name.isNullOrBlank()) {
                val intent = Intent().putExtra(EXTRA_CITY, name)
                pickedLat?.let { intent.putExtra(EXTRA_LAT, it) }
                pickedLon?.let { intent.putExtra(EXTRA_LON, it) }
                setResult(RESULT_OK, intent)
                finish()
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

    @SuppressLint("MissingPermission")
    private fun lastKnown(): GeoPoint? {
        if (!LocationChecker.hasLocationPermission(this)) return null
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        var best: android.location.Location? = null
        for (p in lm.getProviders(true)) {
            val l = try {
                lm.getLastKnownLocation(p)
            } catch (e: SecurityException) {
                null
            } ?: continue
            val b = best
            if (b == null || l.time > b.time) best = l
        }
        val b = best
        return if (b != null) GeoPoint(b.latitude, b.longitude) else null
    }

    private fun placeMarker(p: GeoPoint) {
        marker?.let { map.overlays.remove(it) }
        val m = Marker(map)
        m.position = p
        m.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM)
        m.setOnMarkerClickListener { _, _ -> true }
        map.overlays.add(m)
        marker = m
        map.invalidate()
    }

    private fun showFinding() {
        pickedName = null
        pickedLat = null
        pickedLon = null
        btnUse.isEnabled = false
        resultText.text = "Finding city name..."
    }

    private fun showResult(name: String?, lat: Double? = null, lon: Double? = null) {
        if (name.isNullOrBlank()) {
            pickedName = null
            pickedLat = null
            pickedLon = null
            btnUse.isEnabled = false
            resultText.text = "No city found here. Tap a different spot or check your internet."
        } else {
            pickedName = name
            pickedLat = lat
            pickedLon = lon
            btnUse.isEnabled = true
            resultText.text = name
        }
    }

    private fun pick(p: GeoPoint) {
        placeMarker(p)
        showFinding()
        val id = ++requestId
        io.execute {
            val name = Osm.reverseCity(p.latitude, p.longitude)
            runOnUiThread { if (id == requestId) showResult(name, p.latitude, p.longitude) }
        }
    }

    private fun fetchSuggestions(query: String) {
        if (query.trim().length < 2) return
        io.execute {
            val hits = Osm.suggestCities(query)
            runOnUiThread {
                if (searchInput.text?.toString() != query) return@runOnUiThread
                suggestions = hits
                searchAdapter.replaceAll(hits.map { it.label })
                if (hits.isNotEmpty() && searchInput.hasFocus()) searchInput.showDropDown()
            }
        }
    }

    private fun doSearch() {
        val q = searchInput.text?.toString()?.trim().orEmpty()
        if (q.isEmpty()) return
        searchInput.dismissDropDown()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(searchInput.windowToken, 0)
        showFinding()
        resultText.text = "Searching..."
        val id = ++requestId
        io.execute {
            val hit = Osm.search(q)
            runOnUiThread {
                if (id != requestId) return@runOnUiThread
                if (hit == null) {
                    showResult(null)
                    resultText.text = "Could not find \"$q\". Check the spelling or your internet."
                } else {
                    val p = GeoPoint(hit.lat, hit.lon)
                    placeMarker(p)
                    map.controller.setZoom(12.0)
                    map.controller.animateTo(p)
                    showResult(hit.name, hit.lat, hit.lon)
                }
            }
        }
    }
}
