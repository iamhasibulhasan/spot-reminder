package com.spotreminder.app

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.RadioGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat

class SettingsActivity : AppCompatActivity() {

    private lateinit var themeGroup: RadioGroup
    private lateinit var permStatus: TextView

    private val permLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
            updatePermStatus()
            if (hasLocation() && !hasBackground()) askBackground()
        }

    private val bgLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { updatePermStatus() }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        findViewById<View>(R.id.btnBack).setOnClickListener { finish() }

        themeGroup = findViewById(R.id.themeGroup)
        permStatus = findViewById(R.id.permStatus)

        // Theme
        when (Store.themeMode(this)) {
            "light" -> themeGroup.check(R.id.themeLight)
            "dark" -> themeGroup.check(R.id.themeDark)
            else -> themeGroup.check(R.id.themeSystem)
        }
        themeGroup.setOnCheckedChangeListener { _, checkedId ->
            val mode = when (checkedId) {
                R.id.themeLight -> "light"
                R.id.themeDark -> "dark"
                else -> "system"
            }
            if (mode != Store.themeMode(this)) {
                Store.setThemeMode(this, mode)
                App.applyTheme(mode)
            }
        }

        // Permissions
        findViewById<View>(R.id.btnGrant).setOnClickListener { requestPermissions() }
        findViewById<View>(R.id.btnAppSettings).setOnClickListener { openAppSettings() }
        findViewById<View>(R.id.btnBattery).setOnClickListener {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (e: Exception) {
                openAppSettings()
            }
        }
        updatePermStatus()
    }

    override fun onResume() {
        super.onResume()
        updatePermStatus()
    }

    private fun granted(p: String) =
        ContextCompat.checkSelfPermission(this, p) == PackageManager.PERMISSION_GRANTED

    private fun hasLocation() = LocationChecker.hasLocationPermission(this)

    private fun hasBackground() =
        Build.VERSION.SDK_INT < 29 || granted(Manifest.permission.ACCESS_BACKGROUND_LOCATION)

    private fun hasNotif() =
        Build.VERSION.SDK_INT < 33 || granted(Manifest.permission.POST_NOTIFICATIONS)

    private fun updatePermStatus() {
        val missing = mutableListOf<String>()
        if (!hasLocation()) missing.add("location")
        else if (!hasBackground()) missing.add("location \"Allow all the time\"")
        if (!hasNotif()) missing.add("notifications")
        permStatus.text = if (missing.isEmpty())
            "All set. Reminders can run in the background."
        else
            "Missing: " + missing.joinToString(" and ") + "."
    }

    private fun requestPermissions() {
        val need = mutableListOf<String>()
        if (!hasLocation()) {
            need.add(Manifest.permission.ACCESS_FINE_LOCATION)
            need.add(Manifest.permission.ACCESS_COARSE_LOCATION)
        }
        if (!hasNotif()) need.add(Manifest.permission.POST_NOTIFICATIONS)
        if (need.isNotEmpty()) {
            permLauncher.launch(need.toTypedArray())
        } else if (!hasBackground()) {
            askBackground()
        }
    }

    private fun askBackground() {
        if (Build.VERSION.SDK_INT < 29 || hasBackground()) return
        AlertDialog.Builder(this)
            .setTitle("Allow location all the time")
            .setMessage(
                "To remind you when you arrive in a city, even when the app is closed, " +
                    "set Location to \"Allow all the time\"."
            )
            .setPositiveButton("Continue") { _, _ ->
                if (Build.VERSION.SDK_INT >= 30) openAppSettings()
                else bgLauncher.launch(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
            }
            .setNegativeButton("Not now", null)
            .show()
    }

    private fun openAppSettings() {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$packageName")))
    }
}
