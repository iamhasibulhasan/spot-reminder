package com.spotreminder.app

import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.view.Gravity
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StatsActivity : AppCompatActivity() {

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

        renderBreakdown(trips)
        renderLedger(trips)
    }

    private fun renderBreakdown(trips: List<Trip>) {
        val byCategory = linkedMapOf<String, Double>()
        for (t in trips) for (c in t.costs) byCategory[c.category] = (byCategory[c.category] ?: 0.0) + c.amount
        val sorted = byCategory.entries.sortedByDescending { it.value }

        val list = findViewById<LinearLayout>(R.id.breakdownList)
        list.removeAllViews()
        if (sorted.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "No costs recorded yet. They'll show up here after you add one."
                setTextColor(color(R.color.river_soft))
                setPadding(dp(4), dp(12), dp(4), dp(12))
            })
            return
        }
        val maxVal = sorted.first().value.takeIf { it > 0 } ?: 1.0

        for ((cat, amount) in sorted) {
            val meta = findCostCategory(cat)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(10) }
            }
            val labelRow = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
            }
            labelRow.addView(TextView(this).apply {
                text = meta.emoji
                textSize = 13f
                setPadding(0, 0, dp(6), 0)
            })
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
                setBackgroundColor(Color.parseColor(meta.colorHex))
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, fillWidthFraction.toFloat())
            })
            track.addView(android.view.View(this).apply {
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.MATCH_PARENT, (1 - fillWidthFraction).toFloat())
            })
            row.addView(track)
            list.addView(row)
        }
    }

    /** All cost entries (from completed trips and the one currently active, if any), newest first,
     *  grouped by calendar day with a per-day subtotal — the "ledger" view from the reference. */
    private fun renderLedger(completedTrips: List<Trip>) {
        val list = findViewById<LinearLayout>(R.id.ledgerList)
        list.removeAllViews()

        data class Entry(val cost: CostItem, val trip: Trip)
        val all = mutableListOf<Entry>()
        for (t in completedTrips) for (c in t.costs) all.add(Entry(c, t))
        TripStore.activeTrip(this)?.let { active -> for (c in active.costs) all.add(Entry(c, active)) }
        all.sortByDescending { it.cost.time }

        if (all.isEmpty()) {
            list.addView(TextView(this).apply {
                text = "Nothing logged yet."
                setTextColor(color(R.color.river_soft))
                setPadding(dp(4), dp(8), dp(4), dp(12))
            })
            return
        }

        val dayFormat = SimpleDateFormat("d MMM yyyy", Locale.getDefault())
        val today = dayKey(System.currentTimeMillis())
        val yesterday = dayKey(System.currentTimeMillis() - 86_400_000L)

        var currentDay: String? = null
        var dayTotal = 0.0
        var dayHeaderView: TextView? = null

        fun dayLabel(key: String, millis: Long) = when (key) {
            today -> "Today"
            yesterday -> "Yesterday"
            else -> dayFormat.format(Date(millis))
        }

        for (entry in all) {
            val key = dayKey(entry.cost.time)
            if (key != currentDay) {
                currentDay = key
                dayTotal = 0.0
                val headerRow = LinearLayout(this).apply {
                    orientation = LinearLayout.HORIZONTAL
                    layoutParams = LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                    ).apply { topMargin = dp(14) }
                }
                headerRow.addView(TextView(this).apply {
                    text = dayLabel(key, entry.cost.time)
                    textSize = 12f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(color(R.color.river_soft))
                    layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
                })
                val totalLabel = TextView(this).apply {
                    textSize = 12f
                    setTypeface(typeface, Typeface.BOLD)
                    setTextColor(color(R.color.river_soft))
                }
                headerRow.addView(totalLabel)
                dayHeaderView = totalLabel
                list.addView(headerRow)
            }
            dayTotal += entry.cost.amount
            dayHeaderView?.text = "%.0f".format(dayTotal)

            val meta = findCostCategory(entry.cost.category)
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                background = ContextCompat.getDrawable(this@StatsActivity, R.drawable.bg_paper)
                setPadding(dp(10), dp(8), dp(12), dp(8))
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply { topMargin = dp(6) }
            }
            row.addView(TextView(this).apply {
                text = meta.emoji
                textSize = 16f
                gravity = Gravity.CENTER
                background = ContextCompat.getDrawable(this@StatsActivity, R.drawable.bg_icon_circle)
                backgroundTintList = ColorStateList.valueOf(Color.parseColor(meta.colorHex))
                layoutParams = LinearLayout.LayoutParams(dp(34), dp(34)).apply { marginEnd = dp(10) }
            })
            val textCol = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                layoutParams = LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f)
            }
            textCol.addView(TextView(this).apply {
                text = entry.cost.displayTitle
                textSize = 14f
                maxLines = 1
                setTextColor(color(R.color.ink))
            })
            textCol.addView(TextView(this).apply {
                text = "${entry.cost.paymentMethod} · ${entry.trip.spotName}"
                textSize = 11f
                maxLines = 1
                setTextColor(color(R.color.river_soft))
            })
            row.addView(textCol)
            row.addView(TextView(this).apply {
                text = "%.0f".format(entry.cost.amount)
                textSize = 14f
                setTypeface(typeface, Typeface.BOLD)
                setTextColor(color(R.color.ink))
            })
            list.addView(row)
        }
    }

    private fun dayKey(millis: Long): String {
        val cal = Calendar.getInstance()
        cal.timeInMillis = millis
        return "${cal.get(Calendar.YEAR)}-${cal.get(Calendar.DAY_OF_YEAR)}"
    }
}
