package com.spotreminder.app

import org.json.JSONArray
import org.json.JSONObject

/** One recorded GPS point along a trip's path. */
data class TrackPoint(val lat: Double, val lon: Double, val time: Long)

/** One line item in a trip's cost breakdown. */
data class CostItem(val category: String, val amount: Double)

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
                co.put("category", c.category); co.put("amount", c.amount)
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
                    costs.add(CostItem(co.getString("category"), co.getDouble("amount")))
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
