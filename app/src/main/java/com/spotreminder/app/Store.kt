package com.spotreminder.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.text.Normalizer

/** lat/lon are filled in when known (picked on a map, chosen from a suggestion, or resolved once by
 *  geocoding) so the spot can be shown on the navigate-to-spot map without asking again. */
data class Spot(var name: String, var done: Boolean = false, var lat: Double? = null, var lon: Double? = null)
data class CityEntry(val key: String, val name: String, val spots: MutableList<Spot>, var lat: Double? = null, var lon: Double? = null)

object Store {
    private const val PREFS = "spot_reminder"
    private const val KEY_DATA = "data"
    private const val KEY_LAST = "last_notified_city"

    fun norm(s: String): String =
        Normalizer.normalize(s.lowercase(), Normalizer.Form.NFD)
            .replace(Regex("\\p{InCombiningDiacriticalMarks}+"), "")
            .replace(Regex("[^\\p{L}\\p{N} ]"), "")
            .replace(Regex("\\s+"), " ")
            .trim()

    fun titleCase(s: String): String =
        s.trim().split(Regex("\\s+")).joinToString(" ") { w ->
            w.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
        }

    private fun prefs(ctx: Context) = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    @Synchronized
    fun load(ctx: Context): MutableList<CityEntry> {
        val out = mutableListOf<CityEntry>()
        val raw = prefs(ctx).getString(KEY_DATA, null) ?: return out
        try {
            val arr = JSONArray(raw)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val spots = mutableListOf<Spot>()
                val sa = o.getJSONArray("spots")
                for (j in 0 until sa.length()) {
                    val s = sa.getJSONObject(j)
                    spots.add(
                        Spot(
                            s.getString("name"),
                            s.optBoolean("done", false),
                            if (s.has("lat")) s.optDouble("lat") else null,
                            if (s.has("lon")) s.optDouble("lon") else null
                        )
                    )
                }
                out.add(
                    CityEntry(
                        o.getString("key"),
                        o.getString("name"),
                        spots,
                        if (o.has("lat")) o.optDouble("lat") else null,
                        if (o.has("lon")) o.optDouble("lon") else null
                    )
                )
            }
        } catch (e: Exception) {
            // ignore corrupted data
        }
        return out
    }

    @Synchronized
    fun save(ctx: Context, list: List<CityEntry>) {
        val arr = JSONArray()
        for (c in list) {
            val o = JSONObject()
            o.put("key", c.key)
            o.put("name", c.name)
            c.lat?.let { o.put("lat", it) }
            c.lon?.let { o.put("lon", it) }
            val sa = JSONArray()
            for (s in c.spots) {
                val so = JSONObject()
                so.put("name", s.name)
                so.put("done", s.done)
                s.lat?.let { so.put("lat", it) }
                s.lon?.let { so.put("lon", it) }
                sa.put(so)
            }
            o.put("spots", sa)
            arr.put(o)
        }
        prefs(ctx).edit().putString(KEY_DATA, arr.toString()).apply()
    }

    fun lastNotified(ctx: Context): String? = prefs(ctx).getString(KEY_LAST, null)

    fun setLastNotified(ctx: Context, key: String?) {
        prefs(ctx).edit().putString(KEY_LAST, key).apply()
    }

    /** The raw saved JSON, used for backup/restore. Never null; empty list if nothing saved. */
    fun rawData(ctx: Context): String = prefs(ctx).getString(KEY_DATA, null) ?: "[]"

    /** Overwrites all saved cities/spots from a backup. Validates the JSON before committing. */
    fun setRawData(ctx: Context, json: String): Boolean {
        return try {
            JSONArray(json) // validate shape
            prefs(ctx).edit().putString(KEY_DATA, json).apply()
            true
        } catch (e: Exception) {
            false
        }
    }

    // Theme: "system" (default), "light", or "dark"
    private const val KEY_THEME = "theme_mode"

    fun themeMode(ctx: Context): String = prefs(ctx).getString(KEY_THEME, "system") ?: "system"

    fun setThemeMode(ctx: Context, mode: String) {
        prefs(ctx).edit().putString(KEY_THEME, mode).apply()
    }

    // Editable local profile (separate from the Google account used for backup)
    private const val KEY_PROFILE_NAME = "profile_name"
    private const val KEY_PROFILE_PHONE = "profile_phone"
    private const val KEY_PROFILE_ADDRESS = "profile_address"

    fun profileName(ctx: Context): String = prefs(ctx).getString(KEY_PROFILE_NAME, "") ?: ""
    fun profilePhone(ctx: Context): String = prefs(ctx).getString(KEY_PROFILE_PHONE, "") ?: ""
    fun profileAddress(ctx: Context): String = prefs(ctx).getString(KEY_PROFILE_ADDRESS, "") ?: ""

    fun saveProfile(ctx: Context, name: String, phone: String, address: String) {
        prefs(ctx).edit()
            .putString(KEY_PROFILE_NAME, name)
            .putString(KEY_PROFILE_PHONE, phone)
            .putString(KEY_PROFILE_ADDRESS, address)
            .apply()
    }
}
