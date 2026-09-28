package com.spotreminder.app

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Filter
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * An ArrayAdapter for autocomplete dropdowns whose items were already fetched from
 * the network (server-side filtered), so it must not re-filter them itself.
 */
class NoFilterAdapter(ctx: Context) :
    ArrayAdapter<String>(ctx, android.R.layout.simple_dropdown_item_1line, mutableListOf()) {
    private val passthrough = object : Filter() {
        override fun performFiltering(constraint: CharSequence?): FilterResults {
            val snapshot = (0 until count).map { getItem(it) }
            val r = FilterResults()
            r.values = snapshot
            r.count = snapshot.size
            return r
        }
        override fun publishResults(constraint: CharSequence?, results: FilterResults?) {}
    }
    override fun getFilter(): Filter = passthrough
    fun replaceAll(items: List<String>) {
        setNotifyOnChange(false)
        clear()
        addAll(items)
        notifyDataSetChanged()
    }
}

data class SearchHit(val lat: Double, val lon: Double, val name: String)

/** One suggestion offered while typing: a label to show, and the coordinates behind it. */
data class Suggestion(val label: String, val name: String, val lat: Double, val lon: Double)

/** Free OpenStreetMap search (Nominatim for one-shot search/reverse, Photon for as-you-type). */
object Osm {
    private const val BASE = "https://nominatim.openstreetmap.org"
    private const val PHOTON = "https://photon.komoot.io/api/"

    private fun get(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.setRequestProperty("User-Agent", "SpotReminderApp/1.0")
        c.connectTimeout = 12000
        c.readTimeout = 12000
        return c.inputStream.bufferedReader().use { it.readText() }
    }

    /** City/town suggestions as the user types (Photon, free, supports search-as-you-type). */
    fun suggestCities(query: String): List<Suggestion> {
        if (query.trim().length < 2) return emptyList()
        return try {
            val q = URLEncoder.encode(query, "UTF-8")
            parsePhoton(get("$PHOTON?q=$q&limit=6&lang=en"), placesOnly = true)
        } catch (e: Exception) {
            emptyList()
        }
    }

    /** Spot / point-of-interest suggestions, optionally biased near a chosen city. */
    fun suggestSpots(query: String, nearLat: Double?, nearLon: Double?): List<Suggestion> {
        if (query.trim().length < 2) return emptyList()
        return try {
            val q = URLEncoder.encode(query, "UTF-8")
            var url = "$PHOTON?q=$q&limit=8&lang=en"
            if (nearLat != null && nearLon != null) url += "&lat=$nearLat&lon=$nearLon&location_bias_scale=0.9"
            parsePhoton(get(url), placesOnly = false)
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun parsePhoton(text: String, placesOnly: Boolean): List<Suggestion> {
        val out = mutableListOf<Suggestion>()
        val feats = JSONObject(text).optJSONArray("features") ?: return out
        for (i in 0 until feats.length()) {
            val f = feats.getJSONObject(i)
            val p = f.optJSONObject("properties") ?: continue
            val name = p.optString("name", "")
            if (name.isBlank()) continue
            if (placesOnly && p.optString("osm_key", "") != "place") continue
            val coords = f.optJSONObject("geometry")?.optJSONArray("coordinates") ?: continue
            val lon = coords.optDouble(0)
            val lat = coords.optDouble(1)
            val city = p.optString("city", "")
            val state = p.optString("state", "")
            val country = p.optString("country", "")
            val label = listOf(name, city, state, country).filter { it.isNotBlank() }.distinct().joinToString(", ")
            out.add(Suggestion(label, name, lat, lon))
        }
        return out
    }

    /** Picks the most city-like name from a Nominatim address object. */
    private fun cityFrom(address: JSONObject): String? {
        for (k in listOf("city", "town", "municipality", "village", "county", "suburb")) {
            val v = address.optString(k, "")
            if (v.isNotBlank()) return v
        }
        return null
    }

    /** City name for a tapped point, or null if none found / no internet. */
    fun reverseCity(lat: Double, lon: Double): String? {
        return try {
            val text = get("$BASE/reverse?format=jsonv2&zoom=10&addressdetails=1&accept-language=en&lat=$lat&lon=$lon")
            val o = JSONObject(text)
            val a = o.optJSONObject("address")
            (if (a != null) cityFrom(a) else null)
                ?: o.optString("name", "").ifBlank { null }
        } catch (e: Exception) {
            null
        }
    }

    /** Finds a place by name, or null. */
    fun search(query: String): SearchHit? {
        return try {
            val q = URLEncoder.encode(query, "UTF-8")
            val arr = JSONArray(get("$BASE/search?format=jsonv2&addressdetails=1&limit=1&accept-language=en&q=$q"))
            if (arr.length() == 0) return null
            val o = arr.getJSONObject(0)
            val a = o.optJSONObject("address")
            val name = (if (a != null) cityFrom(a) else null)
                ?: o.optString("name", "").ifBlank { null }
                ?: query
            SearchHit(o.getString("lat").toDouble(), o.getString("lon").toDouble(), name)
        } catch (e: Exception) {
            null
        }
    }
}
