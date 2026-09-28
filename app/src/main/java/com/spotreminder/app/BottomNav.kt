package com.spotreminder.app

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat

/** Wires the <include layout="@layout/bottom_nav"/> bar included in each tab's screen. */
object BottomNav {

    enum class Tab { HOME, SPOTS, STATS, PROFILE }

    fun setup(activity: AppCompatActivity, current: Tab) {
        val selected = ContextCompat.getColor(activity, R.color.accent)
        val unselected = ContextCompat.getColor(activity, R.color.river_soft)

        fun style(iconId: Int, labelId: Int, tab: Tab) {
            val icon = activity.findViewById<ImageView>(iconId)
            val label = activity.findViewById<TextView>(labelId)
            val color = if (tab == current) selected else unselected
            icon.setColorFilter(color)
            label.setTextColor(color)
        }
        style(R.id.navHomeIcon, R.id.navHomeLabel, Tab.HOME)
        style(R.id.navSpotsIcon, R.id.navSpotsLabel, Tab.SPOTS)
        style(R.id.navStatsIcon, R.id.navStatsLabel, Tab.STATS)
        style(R.id.navProfileIcon, R.id.navProfileLabel, Tab.PROFILE)

        fun go(tab: Tab, cls: Class<*>) {
            activity.findViewById<android.view.View>(
                when (tab) {
                    Tab.HOME -> R.id.navHome
                    Tab.SPOTS -> R.id.navSpots
                    Tab.STATS -> R.id.navStats
                    Tab.PROFILE -> R.id.navProfile
                }
            ).setOnClickListener {
                if (tab != current) {
                    activity.startActivity(Intent(activity, cls))
                    activity.finish()
                }
            }
        }
        go(Tab.HOME, MainActivity::class.java)
        go(Tab.SPOTS, SpotsActivity::class.java)
        go(Tab.STATS, StatsActivity::class.java)
        go(Tab.PROFILE, ProfileActivity::class.java)
    }
}
