package com.spotreminder.app

import org.json.JSONArray
import org.json.JSONObject

/** One recorded GPS point along a trip's path. */
data class TrackPoint(val lat: Double, val lon: Double, val time: Long)

/**
 * One expense entry. `category` is one of [COST_CATEGORIES]' names (or any custom text).
 * `title` is an optional one-line label shown instead of the category name when set.
 * `time` is this entry's own timestamp — a trip can span days, so each cost is dated
 * individually (that's what lets a "today" vs "total" view work).
 */
data class CostItem(
    val category: String,
    val amount: Double,
    val title: String = "",
    val paymentMethod: String = "Cash",
    val note: String = "",
    val time: Long = System.currentTimeMillis(),
    val lat: Double? = null,
    val lon: Double? = null,
    val placeLabel: String? = null
) {
    /** What to show as the entry's headline: the custom title if any, else the category name. */
    val displayTitle: String get() = title.ifBlank { category }
}

/** A single visit to a spot: when it started/ended, the path taken, and what it cost. */
data class Trip(
    val id: String,
    val cityKey: String,
    val cityName: String,
    val spotName: String,
    val startTime: Long,
    var endTime: Long? = null,
    val path: MutableList<TrackPoint> = mutableListOf(),
    var costs: MutableList<CostItem> = mutableListOf(),
    var note: String = ""
) {
    val totalCost: Double get() = costs.sumOf { it.amount }

    /** Straight-line-segment distance along the recorded path, in kilometers. */
    val distanceKm: Double
        get() {
            if (path.size < 2) return 0.0
            var total = 0.0
            for (i in 1 until path.size) total += haversineKm(path[i - 1], path[i])
            return total
        }

    val durationMinutes: Long
        get() {
            val end = endTime ?: return 0
            return (end - startTime) / 60000L
        }

    companion object {
        /** Distance between two points on Earth, in kilometers (haversine formula). */
        fun haversineKm(a: TrackPoint, b: TrackPoint): Double {
            val r = 6371.0
            val dLat = Math.toRadians(b.lat - a.lat)
            val dLon = Math.toRadians(b.lon - a.lon)
            val la1 = Math.toRadians(a.lat)
            val la2 = Math.toRadians(b.lat)
            val h = Math.sin(dLat / 2).let { it * it } +
                Math.cos(la1) * Math.cos(la2) * Math.sin(dLon / 2).let { it * it }
            return 2 * r * Math.asin(Math.sqrt(h))
        }

        fun toJson(t: Trip): JSONObject {
            val o = JSONObject()
            o.put("id", t.id)
            o.put("cityKey", t.cityKey)
            o.put("cityName", t.cityName)
            o.put("spotName", t.spotName)
            o.put("startTime", t.startTime)
            o.put("endTime", t.endTime ?: JSONObject.NULL)
            o.put("note", t.note)
            val path = JSONArray()
            for (p in t.path) {
                val po = JSONObject()
                po.put("lat", p.lat); po.put("lon", p.lon); po.put("time", p.time)
                path.put(po)
            }
            o.put("path", path)
            val costs = JSONArray()
            for (c in t.costs) {
                val co = JSONObject()
                co.put("category", c.category)
                co.put("amount", c.amount)
                co.put("title", c.title)
                co.put("paymentMethod", c.paymentMethod)
                co.put("note", c.note)
                co.put("time", c.time)
                c.lat?.let { co.put("lat", it) }
                c.lon?.let { co.put("lon", it) }
                c.placeLabel?.let { co.put("placeLabel", it) }
                costs.put(co)
            }
            o.put("costs", costs)
            return o
        }

        fun fromJson(o: JSONObject): Trip {
            val path = mutableListOf<TrackPoint>()
            val pa = o.optJSONArray("path")
            if (pa != null) {
                for (i in 0 until pa.length()) {
                    val po = pa.getJSONObject(i)
                    path.add(TrackPoint(po.getDouble("lat"), po.getDouble("lon"), po.getLong("time")))
                }
            }
            val costs = mutableListOf<CostItem>()
            val ca = o.optJSONArray("costs")
            if (ca != null) {
                for (i in 0 until ca.length()) {
                    val co = ca.getJSONObject(i)
                    costs.add(
                        CostItem(
                            category = co.getString("category"),
                            amount = co.getDouble("amount"),
                            title = co.optString("title", ""),
                            paymentMethod = co.optString("paymentMethod", "Cash"),
                            note = co.optString("note", ""),
                            time = if (co.has("time")) co.optLong("time") else o.optLong("startTime"),
                            lat = if (co.has("lat")) co.optDouble("lat") else null,
                            lon = if (co.has("lon")) co.optDouble("lon") else null,
                            placeLabel = if (co.has("placeLabel")) co.optString("placeLabel") else null
                        )
                    )
                }
            }
            return Trip(
                id = o.getString("id"),
                cityKey = o.getString("cityKey"),
                cityName = o.getString("cityName"),
                spotName = o.getString("spotName"),
                startTime = o.getLong("startTime"),
                endTime = if (o.isNull("endTime")) null else o.getLong("endTime"),
                path = path,
                costs = costs,
                note = o.optString("note", "")
            )
        }
    }
}
