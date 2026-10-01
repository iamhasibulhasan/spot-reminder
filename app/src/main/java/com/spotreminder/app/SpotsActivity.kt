package com.spotreminder.app

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.UUID

class SpotsActivity : AppCompatActivity() {

    private lateinit var spotsList: LinearLayout
    private lateinit var activeBanner: LinearLayout
    private lateinit var activeSpot: TextView
    private lateinit var activeStats: TextView
    private lateinit var activeCostsList: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spots)

        spotsList = findViewById(R.id.spotsList)
        activeBanner = findViewById(R.id.activeBanner)
        activeSpot = findViewById(R.id.activeSpot)
        activeStats = findViewById(R.id.activeStats)
        activeCostsList = findViewById(R.id.activeCostsList)

        BottomNav.setup(this, BottomNav.Tab.SPOTS)

        findViewById<View>(R.id.btnEndActive).setOnClickListener {
            TripStore.activeTrip(this)?.let { showEndTripDialog(it) }
        }
        findViewById<View>(R.id.btnAddCost).setOnClickListener {
            if (TripStore.activeTrip(this) != null) {
                CostDialogs.showAddCost(this) { item ->
                    TripStore.addCostToActiveTrip(this, item)
                    render()
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)

    /** Builds one rounded, borderless input row (value left, small caption right) for dialogs. */
    private fun fieldRow(caption: String, inputTypeFlags: Int): Pair<LinearLayout, TextInputEditText> {
        val input = TextInputEditText(this).apply {
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            setTextColor(color(R.color.ink))
            setHintTextColor(color(R.color.river_soft))
            textSize = 16f
            inputType = inputTypeFlags
        }
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            background = ContextCompat.getDrawable(this@SpotsActivity, R.drawable.bg_field_row)
            setPadding(dp(14), dp(4), dp(14), dp(4))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(56)
            ).apply { topMargin = dp(8) }
        }
        row.addView(input, LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f))
        row.addView(TextView(this).apply {
            text = caption
            textSize = 12f
            setTextColor(color(R.color.river_soft))
            setPadding(dp(8), 0, 0, 0)
        })
        return row to input
    }

    /** A small tappable icon (edit/delete/add/chevron) with a ripple, used instead of text buttons. */
    private fun iconButton(drawableRes: Int, tintColor: Int, sizeDp: Int = 36, onClick: (() -> Unit)? = null): View =
        ImageView(this).apply {
            setImageResource(drawableRes)
            setColorFilter(tintColor)
            val pad = dp(7)
            setPadding(pad, pad, pad, pad)
            background = ContextCompat.getDrawable(this@SpotsActivity, android.R.drawable.list_selector_background)
            isClickable = onClick != null
            isFocusable = onClick != null
            layoutParams = LinearLayout.LayoutParams(dp(sizeDp), dp(sizeDp))
            onClick?.let { setOnClickListener { _ -> it() } }
        }

    private val expandedCities = mutableSetOf<String>()

    private fun render() {
        val active = TripStore.activeTrip(this)
        if (active == null) {
            activeBanner.visibility = View.GONE
        } else {
            activeBanner.visibility = View.VISIBLE
            activeSpot.text = active.spotName + " · " + active.cityName
            activeStats.text = "%.1f km so far · %.0f spent".format(active.distanceKm, active.totalCost)
            activeCostsList.removeAllViews()
            for (c in active.costs) {
                val row = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    gravity = Gravity.CENTER_VERTICAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(4) }
                }
                row.addView(TextView(this).apply {
                    text = findCostCategory(c.category).emoji
                    textSize = 13f
                    setPadding(0, 0, dp(6), 0)
                })
                row.addView(TextView(this).apply {
                    text = "${c.displayTitle} · ${c.paymentMethod}"
                    textSize = 13f
                    maxLines = 1
                    ellipsize = android.text.TextUtils.TruncateAt.END
                    setTextColor(Color.WHITE)
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                row.addView(TextView(this).apply {
                    text = "%.0f".format(c.amount)
                    textSize = 13f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(Color.WHITE)
                })
                activeCostsList.addView(row)
            }
        }

        spotsList.removeAllViews()
        val cities = Store.load(this)
        if (cities.isEmpty()) {
            spotsList.addView(TextView(this).apply {
                text = "Nothing saved yet. Add a city and a spot from the Home tab."
                setTextColor(color(R.color.river_soft))
                setPadding(dp(4), dp(16), dp(4), dp(16))
            })
            return
        }
        // First city opens by default so the list isn't empty-looking on first visit
        if (expandedCities.isEmpty()) expandedCities.add(cities.first().key)

        for (c in cities) {
            val isOpen = c.key in expandedCities
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(this@SpotsActivity, R.drawable.bg_paper)
                outlineProvider = ViewOutlineProvider.BACKGROUND
                elevation = dp(2).toFloat()
                setPadding(dp(14), dp(6), dp(10), dp(6))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(12) }
            }

            // --- Accordion header ---
            val head = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setPadding(0, dp(6), 0, dp(6))
                isClickable = true
                setOnClickListener {
                    if (isOpen) expandedCities.remove(c.key) else expandedCities.add(c.key)
                    render()
                }
            }
            val chevron = ImageView(this).apply {
                setImageResource(R.drawable.ic_expand_more)
                setColorFilter(color(R.color.river_soft))
                rotation = if (isOpen) 180f else 0f
                layoutParams = LinearLayout.LayoutParams(dp(22), dp(22)).apply { marginEnd = dp(8) }
            }
            head.addView(chevron)
            head.addView(TextView(this).apply {
                text = c.name + "  (${c.spots.size})"
                textSize = 16f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(color(R.color.ink))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            head.addView(iconButton(R.drawable.ic_add, color(R.color.accent)) { showAddSpotDialog(c.key, c.name) })
            head.addView(iconButton(R.drawable.ic_delete, color(R.color.brick)) {
                AlertDialog.Builder(this)
                    .setTitle("Remove " + c.name + "?")
                    .setMessage("This deletes the city and all its spots. Trip history is kept.")
                    .setPositiveButton("Remove") { _, _ ->
                        val all = Store.load(this)
                        all.removeAll { it.key == c.key }
                        Store.save(this, all)
                        expandedCities.remove(c.key)
                        render()
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            })
            card.addView(head)

            // --- Spot rows (collapsible) ---
            if (isOpen) {
                if (c.spots.isEmpty()) {
                    card.addView(TextView(this).apply {
                        text = "No spots yet — tap + above to add one."
                        textSize = 12f
                        setTextColor(color(R.color.river_soft))
                        setPadding(dp(30), dp(4), 0, dp(10))
                    })
                }
                for (s in c.spots) {
                    val trips = TripStore.tripsForCity(this, c.key).filter { it.spotName == s.name && it.endTime != null }
                    val totalCost = trips.sumOf { it.totalCost }
                    val isActiveHere = active != null && active.cityKey == c.key && active.spotName == s.name

                    val row = LinearLayout(this).apply {
                        orientation = LinearLayout.HORIZONTAL
                        gravity = Gravity.CENTER_VERTICAL
                        setPadding(dp(2), dp(8), 0, dp(8))
                        isClickable = true
                        setOnClickListener {
                            startActivity(
                                Intent(this@SpotsActivity, SpotDetailActivity::class.java)
                                    .putExtra("cityKey", c.key)
                                    .putExtra("cityName", c.name)
                                    .putExtra("spotName", s.name)
                            )
                        }
                    }
                    // Colored icon badge: saffron pin normally, green check once visited
                    row.addView(ImageView(this).apply {
                        setImageResource(if (trips.isNotEmpty()) R.drawable.ic_check_small else R.drawable.ic_pin_small)
                        background = ContextCompat.getDrawable(this@SpotsActivity, R.drawable.bg_icon_circle)
                        // Visited spots badge green (matches the accent), unvisited ones badge blue (matches the hero color).
                        backgroundTintList = android.content.res.ColorStateList.valueOf(
                            if (trips.isNotEmpty()) color(R.color.accent) else color(R.color.river)
                        )
                        val pad = dp(7)
                        setPadding(pad, pad, pad, pad)
                        layoutParams = LinearLayout.LayoutParams(dp(32), dp(32)).apply { marginEnd = dp(10) }
                    })

                    val textCol = LinearLayout(this).apply {
                        orientation = LinearLayout.VERTICAL
                        layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                    }
                    textCol.addView(TextView(this).apply {
                        text = s.name
                        textSize = 15f
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        setTextColor(color(R.color.ink))
                    })
                    textCol.addView(TextView(this).apply {
                        text = when {
                            isActiveHere -> "Recording now…"
                            trips.isEmpty() -> "No trips yet"
                            else -> "${trips.size} trip${if (trips.size == 1) "" else "s"} · ${"%.0f".format(totalCost)} spent"
                        }
                        textSize = 12f
                        maxLines = 1
                        ellipsize = android.text.TextUtils.TruncateAt.END
                        setTextColor(if (isActiveHere) color(R.color.accent) else color(R.color.river_soft))
                    })
                    row.addView(textCol)

                    if (isActiveHere) {
                        row.addView(iconButton(R.drawable.ic_stat_pin, color(R.color.accent)) { showEndTripDialog(active!!) })
                    } else if (active == null) {
                        row.addView(MaterialButton(this, null, android.R.attr.borderlessButtonStyle).apply {
                            text = "Start trip"
                            isAllCaps = false
                            textSize = 12f
                            setTextColor(color(R.color.accent))
                            setPadding(dp(10), 0, dp(2), 0)
                            setOnClickListener { startTrip(c.key, c.name, s.name) }
                        })
                    }
                    // "Add cost" works whether this spot has a trip running right now or not.
                    row.addView(iconButton(R.drawable.ic_cost, color(R.color.accent)) {
                        if (isActiveHere) {
                            CostDialogs.showAddCost(this) { item ->
                                TripStore.addCostToActiveTrip(this, item)
                                render()
                            }
                        } else {
                            addQuickCost(c.key, c.name, s.name)
                        }
                    })
                    row.addView(iconButton(R.drawable.ic_edit, color(R.color.river_soft)) { showEditSpotDialog(c.key, s.name) })
                    row.addView(iconButton(R.drawable.ic_delete, color(R.color.brick)) {
                        val all = Store.load(this)
                        val entry = all.firstOrNull { it.key == c.key }
                        entry?.spots?.removeAll { it.name == s.name }
                        if (entry != null && entry.spots.isEmpty()) all.remove(entry)
                        Store.save(this, all)
                        render()
                    })
                    card.addView(row)
                }
            }
            spotsList.addView(card)
        }
    }

    private fun showAddSpotDialog(cityKey: String, cityName: String) {
        val (row, input) = fieldRow("Spot name", android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        val wrap = FrameLayout(this).apply {
            val pad = dp(20)
            setPadding(pad, dp(8), pad, 0)
            addView(row)
        }
        AlertDialog.Builder(this)
            .setTitle("Add a spot in $cityName")
            .setView(wrap)
            .setPositiveButton("Add") { _, _ ->
                val name = input.text?.toString()?.trim().orEmpty()
                if (name.isEmpty()) return@setPositiveButton
                val all = Store.load(this)
                val entry = all.firstOrNull { it.key == cityKey } ?: return@setPositiveButton
                if (entry.spots.none { Store.norm(it.name) == Store.norm(name) }) {
                    entry.spots.add(Spot(Store.titleCase(name), false))
                }
                Store.save(this, all)
                expandedCities.add(cityKey)
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun startTrip(cityKey: String, cityName: String, spotName: String) {
        if (!LocationChecker.hasLocationPermission(this)) {
            AlertDialog.Builder(this)
                .setTitle("Location needed")
                .setMessage("Turn on location in Settings to record your trip.")
                .setPositiveButton("Open Settings") { _, _ ->
                    startActivity(Intent(this, SettingsActivity::class.java))
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }
        val trip = Trip(
            id = UUID.randomUUID().toString(),
            cityKey = cityKey,
            cityName = cityName,
            spotName = spotName,
            startTime = System.currentTimeMillis()
        )
        TripStore.setActiveTrip(this, trip)
        TripTrackingService.start(this, spotName)
        render()
    }

    /** Costs are added as they happen via CostDialogs; ending a trip just confirms and saves them. */
    private fun showEndTripDialog(trip: Trip) {
        AlertDialog.Builder(this)
            .setTitle("End trip?")
            .setMessage(
                "%.1f km recorded · %.0f spent so far.".format(trip.distanceKm, trip.totalCost)
            )
            .setPositiveButton("End trip") { _, _ ->
                trip.endTime = System.currentTimeMillis()
                TripTrackingService.stop(this)
                TripStore.addTrip(this, trip)
                TripStore.setActiveTrip(this, null)
                render()
                android.widget.Toast.makeText(this, "Trip saved.", android.widget.Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Keep recording", null)
            .show()
    }

    /**
     * Logs a cost for a spot that has no trip running right now. It's saved as its own
     * zero-duration trip record, so it shows up in history and stats immediately — useful for
     * a quick expense you don't want to wait to start a full trip for.
     */
    private fun addQuickCost(cityKey: String, cityName: String, spotName: String) {
        CostDialogs.showAddCost(this) { item ->
            val now = System.currentTimeMillis()
            val trip = Trip(
                id = UUID.randomUUID().toString(),
                cityKey = cityKey,
                cityName = cityName,
                spotName = spotName,
                startTime = now,
                endTime = now,
                costs = mutableListOf(item)
            )
            TripStore.addTrip(this, trip)
            render()
            android.widget.Toast.makeText(this, "Cost saved.", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditSpotDialog(cityKey: String, oldName: String) {
        val (row, input) = fieldRow("Spot name", android.text.InputType.TYPE_CLASS_TEXT or android.text.InputType.TYPE_TEXT_FLAG_CAP_WORDS)
        input.setText(oldName)
        input.setSelection(input.text?.length ?: 0)
        val wrap = FrameLayout(this).apply {
            val pad = dp(20)
            setPadding(pad, dp(8), pad, 0)
            addView(row)
        }
        AlertDialog.Builder(this)
            .setTitle("Edit spot name")
            .setView(wrap)
            .setPositiveButton("Save") { _, _ ->
                val newName = input.text?.toString()?.trim().orEmpty()
                if (newName.isEmpty()) return@setPositiveButton
                val all = Store.load(this)
                val entry = all.firstOrNull { it.key == cityKey } ?: return@setPositiveButton
                val spot = entry.spots.firstOrNull { it.name == oldName } ?: return@setPositiveButton
                val clash = entry.spots.any { it !== spot && Store.norm(it.name) == Store.norm(newName) }
                if (clash) entry.spots.remove(spot) else spot.name = Store.titleCase(newName)
                Store.save(this, all)
                render()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}
