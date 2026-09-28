package com.spotreminder.app

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.CancellationSignal
import android.os.Looper
import androidx.core.content.ContextCompat
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

data class CheckResult(val names: List<String>, val matched: CityEntry?, val error: String?)

object LocationChecker {

    fun hasLocationPermission(ctx: Context): Boolean =
        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    /** True when a detected place name matches a saved city key (both normalised). */
    fun matches(placeName: String, cityKey: String): Boolean {
        val n = Store.norm(placeName)
        if (n.isEmpty() || cityKey.isEmpty()) return false
        return n == cityKey || " $n ".contains(" $cityKey ") || " $cityKey ".contains(" $n ")
    }

    /**
     * Blocking. Call from a background thread.
     * Gets the location, finds the city name, matches it to saved cities.
     * If a saved city matches and we have not announced it yet, optionally shows a notification.
     */
    fun run(ctx: Context, showNotification: Boolean): CheckResult {
        try {
            if (!hasLocationPermission(ctx)) {
                return CheckResult(emptyList(), null, "Location permission is off.")
            }
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val on = lm.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
            if (!on) return CheckResult(emptyList(), null, "Location is turned off. Switch on location in your phone settings.")

            val loc = getLocation(ctx, lm)
                ?: return CheckResult(emptyList(), null, "Could not get your position yet. Try again outdoors or with internet on.")

            val names = placeNames(ctx, loc)
            if (names.isEmpty()) {
                return CheckResult(emptyList(), null, "Could not find a city name. Check your internet connection.")
            }

            val cities = Store.load(ctx)
            val match = cities.firstOrNull { c -> names.any { matches(it, c.key) } }

            if (match != null) {
                if (Store.lastNotified(ctx) != match.key) {
                    Store.setLastNotified(ctx, match.key)
                    if (showNotification) Notifier.show(ctx, match)
                }
            } else {
                Store.setLastNotified(ctx, null)
            }
            return CheckResult(names, match, null)
        } catch (e: Exception) {
            return CheckResult(emptyList(), null, "Something went wrong: " + (e.message ?: "unknown error"))
        }
    }

    @SuppressLint("MissingPermission")
    private fun getLocation(ctx: Context, lm: LocationManager): Location? {
        var best: Location? = null
        for (p in lm.getProviders(true)) {
            val l = try {
                lm.getLastKnownLocation(p)
            } catch (e: SecurityException) {
                null
            } ?: continue
            val b = best
            if (b == null || l.time > b.time) best = l
        }
        val b = best
        if (b != null && System.currentTimeMillis() - b.time < 30 * 60 * 1000L) return b
        return requestFresh(ctx, lm) ?: b
    }

    @SuppressLint("MissingPermission")
    @Suppress("DEPRECATION")
    private fun requestFresh(ctx: Context, lm: LocationManager): Location? {
        val provider = when {
            lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER) -> LocationManager.NETWORK_PROVIDER
            lm.isProviderEnabled(LocationManager.GPS_PROVIDER) -> LocationManager.GPS_PROVIDER
            else -> return null
        }
        val latch = CountDownLatch(1)
        var result: Location? = null
        try {
            if (Build.VERSION.SDK_INT >= 30) {
                val cancel = CancellationSignal()
                lm.getCurrentLocation(provider, cancel, ContextCompat.getMainExecutor(ctx)) { loc ->
                    result = loc
                    latch.countDown()
                }
                if (!latch.await(25, TimeUnit.SECONDS)) cancel.cancel()
            } else {
                val listener = object : LocationListener {
                    override fun onLocationChanged(location: Location) {
                        result = location
                        latch.countDown()
                    }
                    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
                    override fun onProviderEnabled(provider: String) {}
                    override fun onProviderDisabled(provider: String) {}
                }
                lm.requestSingleUpdate(provider, listener, Looper.getMainLooper())
                if (!latch.await(25, TimeUnit.SECONDS)) lm.removeUpdates(listener)
            }
        } catch (e: Exception) {
            return null
        }
        return result
    }

    @Suppress("DEPRECATION")
    private fun placeNames(ctx: Context, loc: Location): List<String> {
        val names = LinkedHashSet<String>()
        try {
            if (Geocoder.isPresent()) {
                val g = Geocoder(ctx, Locale.ENGLISH)
                val list = g.getFromLocation(loc.latitude, loc.longitude, 3)
                list?.forEach { a ->
                    listOf(a.locality, a.subAdminArea, a.subLocality).forEach { n ->
                        if (!n.isNullOrBlank()) names.add(n)
                    }
                }
            }
        } catch (e: Exception) {
            // fall through to online lookup
        }
        if (names.isEmpty()) names.addAll(nominatim(loc))
        return names.toList()
    }

    private fun nominatim(loc: Location): List<String> {
        val out = ArrayList<String>()
        try {
            val url = URL(
                "https://nominatim.openstreetmap.org/reverse?format=jsonv2&zoom=12&addressdetails=1&accept-language=en" +
                    "&lat=" + loc.latitude + "&lon=" + loc.longitude
            )
            val c = url.openConnection() as HttpURLConnection
            c.setRequestProperty("User-Agent", "SpotReminderApp/1.0")
            c.connectTimeout = 10000
            c.readTimeout = 10000
            val text = c.inputStream.bufferedReader().use { it.readText() }
            val a = JSONObject(text).optJSONObject("address") ?: return out
            for (k in listOf("city", "town", "municipality", "village", "suburb", "county")) {
                val v = a.optString(k, "")
                if (v.isNotBlank()) out.add(v)
            }
        } catch (e: Exception) {
            // no internet or blocked
        }
        return out
    }
}
