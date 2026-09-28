package com.spotreminder.app

import android.content.Context
import org.json.JSONArray

/** Standard cost categories offered when ending a trip. */
val TRIP_COST_CATEGORIES = listOf("Fuel", "Food", "Tickets", "Other")

object TripStore {
    private const val PREFS = "trip_store"
    private const val KEY_TRIPS = "trips"
    private const val KEY_ACTIVE = "active_trip"

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun loadTrips(ctx: Context): MutableList<Trip> {
        val out = mutableListOf<Trip>()
        val raw = prefs(ctx).getString(KEY_TRIPS, null) ?: return out
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) out.add(Trip.fromJson(arr.getJSONObject(i)))
        } catch (e: Exception) {
            // ignore corrupted data
        }
        return out
    }

    @Synchronized
    fun saveTrips(ctx: Context, trips: List<Trip>) {
        val arr = JSONArray()
        for (t in trips) arr.put(Trip.toJson(t))
        prefs(ctx).edit().putString(KEY_TRIPS, arr.toString()).apply()
    }

    fun addTrip(ctx: Context, trip: Trip) {
        val all = loadTrips(ctx)
        all.add(0, trip)
        saveTrips(ctx, all)
    }

    fun tripsForCity(ctx: Context, cityKey: String): List<Trip> =
        loadTrips(ctx).filter { it.cityKey == cityKey }

    /** The trip currently being recorded (started but not yet ended), or null. */
    fun activeTrip(ctx: Context): Trip? {
        val raw = prefs(ctx).getString(KEY_ACTIVE, null) ?: return null
        return try {
            Trip.fromJson(org.json.JSONObject(raw))
        } catch (e: Exception) {
            null
        }
    }

    fun setActiveTrip(ctx: Context, trip: Trip?) {
        if (trip == null) {
            prefs(ctx).edit().remove(KEY_ACTIVE).apply()
        } else {
            prefs(ctx).edit().putString(KEY_ACTIVE, Trip.toJson(trip).toString()).apply()
        }
    }

    /** Appends one GPS point to the active trip, if one is running. */
    @Synchronized
    fun appendActivePoint(ctx: Context, point: TrackPoint) {
        val t = activeTrip(ctx) ?: return
        t.path.add(point)
        setActiveTrip(ctx, t)
    }
}
