package com.spotreminder.app

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat

/** Records GPS points every ~10 seconds / 15 meters while a trip is running. */
class TripTrackingService : Service() {

    companion object {
        private const val CHANNEL = "trip_tracking"
        private const val NOTIF_ID = 2001
        const val ACTION_STOP = "com.spotreminder.app.STOP_TRIP"
        const val EXTRA_SPOT_NAME = "spot_name"

        fun start(ctx: Context, spotName: String) {
            val i = Intent(ctx, TripTrackingService::class.java).putExtra(EXTRA_SPOT_NAME, spotName)
            ContextCompat.startForegroundService(ctx, i)
        }

        fun stop(ctx: Context) {
            ctx.stopService(Intent(ctx, TripTrackingService::class.java))
        }
    }

    private var locationManager: LocationManager? = null
    private var listener: LocationListener? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        val spotName = intent?.getStringExtra(EXTRA_SPOT_NAME) ?: "your spot"
        startInForeground(spotName)
        beginTracking()
        return START_STICKY
    }

    private fun startInForeground(spotName: String) {
        if (Build.VERSION.SDK_INT >= 26) {
            val ch = NotificationChannel(CHANNEL, "Trip recording", NotificationManager.IMPORTANCE_LOW)
            ch.description = "Shows when your route to a spot is being recorded"
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(ch)
        }
        val stopIntent = Intent(this, TripTrackingService::class.java).setAction(ACTION_STOP)
        val stopPending = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notif: Notification = NotificationCompat.Builder(this, CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_pin)
            .setContentTitle("Recording your trip to $spotName")
            .setContentText("Tap Stop when you finish, or end it from the app.")
            .setContentIntent(openIntent)
            .addAction(0, "Stop", stopPending)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(NOTIF_ID, notif, android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(NOTIF_ID, notif)
        }
    }

    @SuppressLint("MissingPermission")
    private fun beginTracking() {
        if (!LocationChecker.hasLocationPermission(this)) return
        val lm = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager = lm
        val l = object : LocationListener {
            override fun onLocationChanged(location: Location) {
                TripStore.appendActivePoint(
                    applicationContext,
                    TrackPoint(location.latitude, location.longitude, System.currentTimeMillis())
                )
            }
            override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
            override fun onProviderEnabled(provider: String) {}
            override fun onProviderDisabled(provider: String) {}
        }
        listener = l
        try {
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000L, 15f, l)
            }
            if (lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                lm.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 10000L, 15f, l)
            }
        } catch (e: SecurityException) {
            // permission revoked mid-flight; recording simply stops
        }
    }

    override fun onDestroy() {
        listener?.let { locationManager?.removeUpdates(it) }
        super.onDestroy()
    }
}
