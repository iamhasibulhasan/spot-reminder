package com.spotreminder.app

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Locale

class SpotDetailActivity : AppCompatActivity() {

    private lateinit var cityKey: String
    private lateinit var cityName: String
    private lateinit var spotName: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_spot_detail)

        cityKey = intent.getStringExtra("cityKey").orEmpty()
        cityName = intent.getStringExtra("cityName").orEmpty()
        spotName = intent.getStringExtra("spotName").orEmpty()

        findViewById<TextView>(R.id.spotTitle).text = spotName
        findViewById<TextView>(R.id.spotSubtitle).text = cityName
        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }
        findViewById<View>(R.id.btnNavigate).setOnClickListener {
            startActivity(
                Intent(this, NavigateActivity::class.java)
                    .putExtra("cityKey", cityKey)
                    .putExtra("cityName", cityName)
                    .putExtra("spotName", spotName)
            )
        }
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)

    private fun render() {
        val trips = TripStore.tripsForCity(this, cityKey)
            .filter { it.spotName == spotName && it.endTime != null }
            .sortedByDescending { it.startTime }

        findViewById<TextView>(R.id.totalCost).text = "%.0f".format(trips.sumOf { it.totalCost })
        findViewById<TextView>(R.id.totalDistance).text = "%.1f km".format(trips.sumOf { it.distanceKm })

        val list = findViewById<LinearLayout>(R.id.tripList)
        list.removeAllViews()
        if (trips.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "No completed trips yet. Start one from the Spots tab."
                setTextColor(color(R.color.river_soft))
                setPadding(dp(4), dp(12), dp(4), dp(12))
            })
            return
        }

        val df = SimpleDateFormat("d MMM yyyy, HH:mm", Locale.getDefault())
        for (t in trips) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                background = ContextCompat.getDrawable(this@SpotDetailActivity, R.drawable.bg_paper)
                outlineProvider = ViewOutlineProvider.BACKGROUND
                elevation = dp(2).toFloat()
                setPadding(dp(14), dp(12), dp(14), dp(12))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
                isClickable = true
                setOnClickListener {
                    startActivity(Intent(this@SpotDetailActivity, TripMapActivity::class.java).putExtra("tripId", t.id))
                }
            }
            val headerRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            headerRow.addView(TextView(this).apply {
                text = df.format(java.util.Date(t.startTime))
                textSize = 13f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(color(R.color.ink))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            // Lets you log a cost you forgot to add while this (already-finished) trip was active.
            headerRow.addView(ImageView(this).apply {
                setImageResource(R.drawable.ic_cost)
                setColorFilter(color(R.color.accent))
                val pad = dp(6)
                setPadding(pad, pad, pad, pad)
                background = ContextCompat.getDrawable(this@SpotDetailActivity, android.R.drawable.list_selector_background)
                layoutParams = LinearLayout.LayoutParams(dp(32), dp(32))
                setOnClickListener {
                    CostDialogs.showAddCost(this@SpotDetailActivity) { item ->
                        TripStore.addCostToTrip(this@SpotDetailActivity, t.id, item)
                        render()
                    }
                }
            })
            card.addView(headerRow)
            val statsRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(4) }
            }
            statsRow.addView(TextView(this).apply {
                text = "%.1f km · %d min".format(t.distanceKm, t.durationMinutes)
                textSize = 12f
                setTextColor(color(R.color.river_soft))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            statsRow.addView(TextView(this).apply {
                text = "%.0f total".format(t.totalCost)
                textSize = 15f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(color(R.color.accent))
            })
            card.addView(statsRow)

            if (t.costs.isNotEmpty()) {
                val chipRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(6) }
                }
                for (c in t.costs) {
                    chipRow.addView(TextView(this).apply {
                        text = "${findCostCategory(c.category).emoji} ${c.displayTitle}: ${"%.0f".format(c.amount)}"
                        textSize = 11f
                        setTextColor(color(R.color.river_soft))
                        setPadding(dp(8), dp(4), dp(8), dp(4))
                        background = ContextCompat.getDrawable(this@SpotDetailActivity, R.drawable.bg_current)
                        layoutParams = LinearLayout.LayoutParams(
                            LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT
                        ).apply { marginEnd = dp(6) }
                    })
                }
                card.addView(chipRow)
            }

            card.addView(TextView(this).apply {
                text = "Tap to view route on map →"
                textSize = 11f
                setTextColor(color(R.color.river_soft))
                setPadding(0, dp(8), 0, 0)
            })

            list.addView(card)
        }
    }
}
