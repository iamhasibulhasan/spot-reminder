package com.spotreminder.app

import android.graphics.Color
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class StatsActivity : AppCompatActivity() {

    private val categoryColors = mapOf(
        "Bus" to "#F2A516",
        "Food" to "#4CAF7D",
        "Tickets" to "#4D8FD1",
        "Other" to "#8A8FA3"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_stats)
        BottomNav.setup(this, BottomNav.Tab.STATS)
    }

    override fun onResume() {
        super.onResume()
        render()
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()
    private fun color(id: Int) = ContextCompat.getColor(this, id)

    private fun render() {
        val trips = TripStore.loadTrips(this).filter { it.endTime != null }
        val totalCost = trips.sumOf { it.totalCost }
        val totalDistance = trips.sumOf { it.distanceKm }

        findViewById<TextView>(R.id.statTotalCost).text = "%.0f".format(totalCost)
        findViewById<TextView>(R.id.statTotalDistance).text = "%.1f km".format(totalDistance)
        findViewById<TextView>(R.id.statTripCount).text =
            "${trips.size} completed trip${if (trips.size == 1) "" else "s"}"

        val byCategory = linkedMapOf<String, Double>()
        for (cat in TRIP_COST_CATEGORIES) byCategory[cat] = 0.0
        for (t in trips) for (c in t.costs) byCategory[c.category] = (byCategory[c.category] ?: 0.0) + c.amount

        val list = findViewById<LinearLayout>(R.id.breakdownList)
        list.removeAllViews()
        val maxVal = byCategory.values.maxOrNull()?.takeIf { it > 0 } ?: 1.0

        if (totalCost <= 0) {
            list.addView(TextView(this).apply {
                text = "No costs recorded yet. They'll show up here after you end a trip."
                setTextColor(color(R.color.river_soft))
                setPadding(dp(4), dp(12), dp(4), dp(12))
            })
            return
        }

        for ((cat, amount) in byCategory) {
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
            }
            val labelRow = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
            labelRow.addView(TextView(this).apply {
                text = cat
                textSize = 13f
                setTextColor(color(R.color.ink))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            })
            labelRow.addView(TextView(this).apply {
                text = "%.0f".format(amount)
                textSize = 13f
                setTextColor(color(R.color.river_soft))
            })
            row.addView(labelRow)

            val track = LinearLayout(this).apply {
                background = ContextCompat.getDrawable(this@StatsActivity, R.drawable.bg_current)
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, dp(10)
                ).apply { topMargin = dp(4) }
            }
            val fillWidthFraction = (amount / maxVal).coerceIn(0.02, 1.0)
            track.addView(android.view.View(this).apply {
                setBackgroundColor(Color.parseColor(categoryColors[cat] ?: "#F2A516"))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, fillWidthFraction.toFloat())
            })
            track.addView(android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (1 - fillWidthFraction).toFloat())
            })
            row.addView(track)
            list.addView(row)
        }
    }
}
