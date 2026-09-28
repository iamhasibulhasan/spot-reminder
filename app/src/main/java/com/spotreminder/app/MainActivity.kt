package com.spotreminder.app

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.Gravity
import android.view.View
import android.widget.CheckBox
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class MainActivity : AppCompatActivity() {

    private lateinit var statusLabel: TextView
    private lateinit var statusCity: TextView
    private lateinit var statusDetail: TextView
    private lateinit var nudge: LinearLayout
    private lateinit var nudgeText: TextView
    private lateinit var remindCard: LinearLayout
    private lateinit var remindTitle: TextView
    private lateinit var remindList: LinearLayout

    // Only set while the "Add a spot" dialog is on screen — see showAddSpotDialog().
    private var dialogCityInput: android.widget.AutoCompleteTextView? = null
    private var dialogSpotInput: android.widget.AutoCompleteTextView? = null

    private val io = Executors.newSingleThreadExecutor()
    private var checking = false
    private var lastNames: List<String> = emptyList()
    private var currentKey: String? = null
    private var dismissedKey: String? = null

    // City/spot suggestions (free OpenStreetMap search, via Osm.kt)
    private val mainHandler = Handler(Looper.getMainLooper())
    private var citySuggestRunnable: Runnable? = null
    private var spotSuggestRunnable: Runnable? = null
    private var citySuggestions: List<Suggestion> = emptyList()
    private var spotSuggestions: List<Suggestion> = emptyList()
    private lateinit var cityAdapter: NoFilterAdapter
    private lateinit var spotAdapter: NoFilterAdapter
    private var suppressCityWatcher = false
    private var suppressSpotWatcher = false
    private var cityLat: Double? = null
    private var cityLon: Double? = null

    private val mapLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            val name = r.data?.getStringExtra(MapPickerActivity.EXTRA_CITY)
            if (r.resultCode == RESULT_OK && !name.isNullOrBlank()) {
                dialogCityInput?.let { cityIn ->
                    suppressCityWatcher = true
                    cityIn.setText(name)
                    cityIn.dismissDropDown()
                    suppressCityWatcher = false
                }
                if (r.data?.hasExtra(MapPickerActivity.EXTRA_LAT) == true) {
                    cityLat = r.data?.getDoubleExtra(MapPickerActivity.EXTRA_LAT, 0.0)
                    cityLon = r.data?.getDoubleExtra(MapPickerActivity.EXTRA_LON, 0.0)
                }
                dialogSpotInput?.requestFocus()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        statusLabel = findViewById(R.id.statusLabel)
        statusCity = findViewById(R.id.statusCity)
        statusDetail = findViewById(R.id.statusDetail)
        nudge = findViewById(R.id.nudge)
        nudgeText = findViewById(R.id.nudgeText)
        remindCard = findViewById(R.id.remindCard)
        remindTitle = findViewById(R.id.remindTitle)
        remindList = findViewById(R.id.remindList)

        BottomNav.setup(this, BottomNav.Tab.HOME)

        findViewById<View>(R.id.homeAvatar).setOnClickListener {
            startActivity(Intent(this, ProfileActivity::class.java))
        }
        findViewById<View>(R.id.btnBell).setOnClickListener { toast("No new alerts") }
        findViewById<View>(R.id.homeSearchBar).setOnClickListener {
            startActivity(Intent(this, SpotsActivity::class.java))
        }
        findViewById<View>(R.id.quickAdd).setOnClickListener { showAddSpotDialog() }
        findViewById<View>(R.id.quickTrip).setOnClickListener {
            startActivity(Intent(this, SpotsActivity::class.java))
        }
        findViewById<View>(R.id.quickSpots).setOnClickListener {
            startActivity(Intent(this, SpotsActivity::class.java))
        }
        findViewById<View>(R.id.quickStats).setOnClickListener {
            startActivity(Intent(this, StatsActivity::class.java))
        }

        cityAdapter = NoFilterAdapter(this)
        spotAdapter = NoFilterAdapter(this)

        findViewById<View>(R.id.btnSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<View>(R.id.btnNudgeFix).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<View>(R.id.btnCheck).setOnClickListener { onReady() }
        findViewById<View>(R.id.btnGotIt).setOnClickListener {
            dismissedKey = currentKey
            renderReminder()
        }

        Notifier.ensureChannel(this)
    }

    /** "Add a spot" now opens as a popup instead of taking up permanent space on the Home screen. */
    private fun showAddSpotDialog() {
        val view = layoutInflater.inflate(R.layout.dialog_add_spot, null)
        val cityIn = view.findViewById<android.widget.AutoCompleteTextView>(R.id.cityInput)
        val spotIn = view.findViewById<android.widget.AutoCompleteTextView>(R.id.spotInput)

        dialogCityInput = cityIn
        dialogSpotInput = spotIn
        cityLat = null
        cityLon = null

        cityIn.setAdapter(cityAdapter)
        spotIn.setAdapter(spotAdapter)
        cityIn.threshold = 2
        spotIn.threshold = 2

        cityIn.setOnItemClickListener { _, _, position, _ ->
            val s = citySuggestions.getOrNull(position) ?: return@setOnItemClickListener
            cityLat = s.lat
            cityLon = s.lon
            suppressCityWatcher = true
            cityIn.setText(s.name)
            cityIn.setSelection(cityIn.text?.length ?: 0)
            cityIn.dismissDropDown()
            suppressCityWatcher = false
        }
        spotIn.setOnItemClickListener { _, _, position, _ ->
            val s = spotSuggestions.getOrNull(position) ?: return@setOnItemClickListener
            suppressSpotWatcher = true
            spotIn.setText(s.name)
            spotIn.setSelection(spotIn.text?.length ?: 0)
            spotIn.dismissDropDown()
            suppressSpotWatcher = false
        }
        cityIn.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressCityWatcher) return
                cityLat = null; cityLon = null
                val q = s?.toString().orEmpty()
                citySuggestRunnable?.let { mainHandler.removeCallbacks(it) }
                val r = Runnable { fetchCitySuggestions(cityIn, q) }
                citySuggestRunnable = r
                mainHandler.postDelayed(r, 350)
            }
        })
        spotIn.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) {
                if (suppressSpotWatcher) return
                val q = s?.toString().orEmpty()
                spotSuggestRunnable?.let { mainHandler.removeCallbacks(it) }
                val r = Runnable { fetchSpotSuggestions(spotIn, q) }
                spotSuggestRunnable = r
                mainHandler.postDelayed(r, 350)
            }
        })
        view.findViewById<View>(R.id.btnMap).setOnClickListener {
            mapLauncher.launch(Intent(this, MapPickerActivity::class.java))
        }

        val dialog = AlertDialog.Builder(this)
            .setTitle("Add a spot")
            .setView(view)
            .setPositiveButton("Save spot", null)
            .setNegativeButton("Cancel", null)
            .create()
        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
                if (addSpot(cityIn, spotIn)) dialog.dismiss()
            }
        }
        dialog.setOnDismissListener {
            dialogCityInput = null
            dialogSpotInput = null
        }
        dialog.show()
        cityIn.requestFocus()
    }

    override fun onResume() {
        super.onResume()
        updateNudge()
        updateGreeting()
        renderUpcoming()
        onReady()
    }

    private fun updateGreeting() {
        val name = Store.profileName(this).trim()
        findViewById<TextView>(R.id.greetingName).text = "Hi, " + name.ifBlank { "Traveler" }
    }

    /** Shows saved spots that have no completed trip yet, as a horizontal card row. */
    private fun renderUpcoming() {
        val list = findViewById<LinearLayout>(R.id.upcomingList)
        list.removeAllViews()
        val trips = TripStore.loadTrips(this)
        val upcoming = mutableListOf<Pair<CityEntry, Spot>>()
        for (c in Store.load(this)) {
            for (s in c.spots) {
                val visited = trips.any { it.cityKey == c.key && it.spotName == s.name && it.endTime != null }
                if (!visited) upcoming.add(c to s)
            }
        }
        if (upcoming.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "Nothing planned yet — add a spot below."
                setTextColor(color(R.color.river_soft))
                textSize = 13f
            })
            return
        }
        for ((c, s) in upcoming.take(10)) {
            val card = FrameLayout(this).apply {
                background = ContextCompat.getDrawable(this@MainActivity, R.drawable.bg_deal_card)
                outlineProvider = android.view.ViewOutlineProvider.BACKGROUND
                elevation = dp(2).toFloat()
                layoutParams = LinearLayout.LayoutParams(dp(150), dp(110)).apply { marginEnd = dp(10) }
                isClickable = true
                setOnClickListener {
                    startActivity(
                        Intent(this@MainActivity, SpotDetailActivity::class.java)
                            .putExtra("cityKey", c.key)
                            .putExtra("cityName", c.name)
                            .putExtra("spotName", s.name)
                    )
                }
            }
            card.addView(android.widget.ImageView(this).apply {
                setImageResource(R.drawable.ic_stat_pin)
                alpha = 0.35f
                layoutParams = FrameLayout.LayoutParams(dp(56), dp(56)).apply {
                    gravity = Gravity.CENTER
                }
            })
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = FrameLayout.LayoutParams(
                    FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.WRAP_CONTENT
                ).apply { gravity = Gravity.BOTTOM; setMargins(dp(12), 0, dp(12), dp(10)) }
            }
            textCol.addView(TextView(this).apply {
                text = s.name
                setTextColor(android.graphics.Color.WHITE)
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                maxLines = 1
            })
            textCol.addView(TextView(this).apply {
                text = c.name
                setTextColor(android.graphics.Color.parseColor("#D6DEFF"))
                textSize = 11f
                maxLines = 1
            })
            card.addView(textCol)
            list.addView(card)
        }
    }

    // ---------- Permission nudge (full controls live in Settings) ----------

    private fun updateNudge() {
        val missing = mutableListOf<String>()
        if (!LocationChecker.hasLocationPermission(this)) missing.add("location")
        if (!Notifier.hasPermission(this)) missing.add("notifications")
        if (missing.isEmpty()) {
            nudge.visibility = View.GONE
        } else {
            nudge.visibility = View.VISIBLE
            nudgeText.text = "Turn on " + missing.joinToString(" and ") + " for reminders to work."
        }
    }

    // ---------- Location check ----------

    private fun onReady() {
        if (!LocationChecker.hasLocationPermission(this)) {
            setStatus("Location", "Turned off", "Open Settings to turn on reminders.")
            return
        }
        scheduleWorker()
        runCheck()
    }

    private fun scheduleWorker() {
        val req = PeriodicWorkRequestBuilder<LocationWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(applicationContext)
            .enqueueUniquePeriodicWork("city-check", ExistingPeriodicWorkPolicy.KEEP, req)
    }

    private fun runCheck() {
        if (checking) return
        checking = true
        setStatus("Finding your location", "Please wait", "")
        io.execute {
            val r = LocationChecker.run(applicationContext, false)
            runOnUiThread {
                checking = false
                applyResult(r)
            }
        }
    }

    private fun applyResult(r: CheckResult) {
        if (r.error != null) {
            lastNames = emptyList()
            currentKey = null
            setStatus("Location", "Not available", r.error)
        } else {
            lastNames = r.names
            currentKey = r.matched?.key
            val label = if (r.matched != null) "You are in a city with saved spots" else "You are in"
            setStatus(label, r.names.first(), "Detected: " + r.names.joinToString(", "))
        }
        if (currentKey != dismissedKey) dismissedKey = null
        renderReminder()
    }

    private fun recomputeMatch() {
        if (lastNames.isEmpty()) return
        val m = Store.load(this).firstOrNull { c -> lastNames.any { LocationChecker.matches(it, c.key) } }
        if (m != null) Store.setLastNotified(this, m.key)
        applyResult(CheckResult(lastNames, m, null))
    }

    private fun setStatus(label: String, city: String, detail: String) {
        statusLabel.text = label
        statusCity.text = city
        statusDetail.text = detail
        statusDetail.visibility = if (detail.isEmpty()) View.GONE else View.VISIBLE
    }

    // ---------- Add / render ----------

    /** Returns true on success, so the caller knows whether to dismiss the dialog. */
    private fun addSpot(cityInput: android.widget.AutoCompleteTextView, spotInput: android.widget.AutoCompleteTextView): Boolean {
        val city = cityInput.text?.toString()?.trim().orEmpty()
        val spot = spotInput.text?.toString()?.trim().orEmpty()
        if (city.isEmpty()) { toast("Enter a city name"); return false }
        if (spot.isEmpty()) { toast("Enter a spot name"); return false }
        val key = Store.norm(city)
        if (key.isEmpty()) { toast("Use letters or numbers for the city"); return false }

        val list = Store.load(this)
        val entry = list.firstOrNull { it.key == key }
            ?: CityEntry(key, Store.titleCase(city), mutableListOf()).also { list.add(it) }
        if (entry.spots.none { Store.norm(it.name) == Store.norm(spot) }) {
            entry.spots.add(Spot(Store.titleCase(spot), false))
        }
        Store.save(this, list)
        toast("Spot saved.")

        // If you are already in this city, show its reminder right away
        dismissedKey = null
        recomputeMatch()
        renderUpcoming()
        return true
    }

    private fun fetchCitySuggestions(input: android.widget.AutoCompleteTextView, query: String) {
        if (query.trim().length < 2) return
        io.execute {
            val hits = Osm.suggestCities(query)
            runOnUiThread {
                if (input.text?.toString() != query) return@runOnUiThread
                citySuggestions = hits
                cityAdapter.replaceAll(hits.map { it.label })
                if (hits.isNotEmpty() && input.hasFocus()) input.showDropDown()
            }
        }
    }

    private fun fetchSpotSuggestions(input: android.widget.AutoCompleteTextView, query: String) {
        if (query.trim().length < 2) return
        io.execute {
            val hits = Osm.suggestSpots(query, cityLat, cityLon)
            runOnUiThread {
                if (input.text?.toString() != query) return@runOnUiThread
                spotSuggestions = hits
                spotAdapter.replaceAll(hits.map { it.label })
                if (hits.isNotEmpty() && input.hasFocus()) input.showDropDown()
            }
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun color(id: Int) = ContextCompat.getColor(this, id)

    private fun toast(msg: String) =
        android.widget.Toast.makeText(this, msg, android.widget.Toast.LENGTH_SHORT).show()

    private fun renderReminder() {
        val key = currentKey
        val city = Store.load(this).firstOrNull { it.key == key }
        if (city == null || key == dismissedKey) {
            remindCard.visibility = View.GONE
            return
        }
        remindCard.visibility = View.VISIBLE
        remindTitle.text = "Welcome to " + city.name + "! Spots you wanted to visit:"
        remindList.removeAllViews()
        for (s in city.spots) {
            val cb = CheckBox(this).apply {
                text = s.name
                isChecked = s.done
                textSize = 16f
                setTextColor(android.graphics.Color.WHITE)
                setPadding(dp(8), dp(10), dp(8), dp(10))
                // The default checkbox tint can end up nearly invisible against the saffron
                // card background, so force a high-contrast white box + a strikethrough on the
                // text itself — that way "done" is unmistakable even if the tint fails on a device.
                buttonTintList = android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                paintFlags = if (s.done) paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                    else paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                alpha = if (s.done) 0.7f else 1f
                setOnCheckedChangeListener { _, checked ->
                    s.done = checked
                    paintFlags = if (checked) paintFlags or android.graphics.Paint.STRIKE_THRU_TEXT_FLAG
                        else paintFlags and android.graphics.Paint.STRIKE_THRU_TEXT_FLAG.inv()
                    alpha = if (checked) 0.7f else 1f
                    val all = Store.load(this@MainActivity)
                    all.firstOrNull { it.key == city.key }
                        ?.spots?.firstOrNull { it.name == s.name }?.done = checked
                    Store.save(this@MainActivity, all)
                }
            }
            remindList.addView(cb)
        }
    }
}
