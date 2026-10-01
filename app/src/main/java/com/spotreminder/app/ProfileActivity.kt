package com.spotreminder.app

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.Scope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.net.URL
import java.util.concurrent.Executors

class ProfileActivity : AppCompatActivity() {

    private val io = Executors.newSingleThreadExecutor()

    private lateinit var avatar: ImageView
    private lateinit var profileName: TextView
    private lateinit var profileEmail: TextView
    private lateinit var btnSignIn: MaterialButton
    private lateinit var btnSignOut: MaterialButton
    private lateinit var backupActions: android.widget.LinearLayout
    private lateinit var nameInput: TextInputEditText
    private lateinit var phoneInput: TextInputEditText
    private lateinit var addressInput: TextInputEditText

    private lateinit var googleClient: GoogleSignInClient
    private var account: GoogleSignInAccount? = null

    private val signInLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
            val task = GoogleSignIn.getSignedInAccountFromIntent(r.data)
            try {
                account = task.getResult(ApiException::class.java)
                updateHeader()
            } catch (e: ApiException) {
                Toast.makeText(this, "Sign-in didn't complete (code ${e.statusCode}).", Toast.LENGTH_LONG).show()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        avatar = findViewById(R.id.avatar)
        profileName = findViewById(R.id.profileName)
        profileEmail = findViewById(R.id.profileEmail)
        btnSignIn = findViewById(R.id.btnSignIn)
        btnSignOut = findViewById(R.id.btnSignOut)
        backupActions = findViewById(R.id.backupActions)
        nameInput = findViewById(R.id.profileNameInput)
        phoneInput = findViewById(R.id.profilePhoneInput)
        addressInput = findViewById(R.id.profileAddressInput)

        avatar.clipToOutline = true
        avatar.outlineProvider = ViewOutlineProvider.BACKGROUND
        avatar.imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.river))

        BottomNav.setup(this, BottomNav.Tab.PROFILE)

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestScopes(Scope("https://www.googleapis.com/auth/drive.appdata"))
            .build()
        googleClient = GoogleSignIn.getClient(this, gso)
        account = GoogleSignIn.getLastSignedInAccount(this)

        btnSignIn.setOnClickListener { signInLauncher.launch(googleClient.signInIntent) }
        btnSignOut.setOnClickListener {
            googleClient.signOut().addOnCompleteListener {
                account = null
                updateHeader()
            }
        }
        findViewById<View>(R.id.btnBackupNow).setOnClickListener { doBackup() }
        findViewById<View>(R.id.btnRestore).setOnClickListener { doRestore() }

        nameInput.setText(Store.profileName(this))
        phoneInput.setText(Store.profilePhone(this))
        addressInput.setText(Store.profileAddress(this))
        findViewById<View>(R.id.btnSaveProfile).setOnClickListener {
            Store.saveProfile(
                this,
                nameInput.text?.toString()?.trim().orEmpty(),
                phoneInput.text?.toString()?.trim().orEmpty(),
                addressInput.text?.toString()?.trim().orEmpty()
            )
            updateHeader()
            Toast.makeText(this, "Profile saved.", Toast.LENGTH_SHORT).show()
        }
        findViewById<View>(R.id.btnOpenSettings).setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        updateHeader()
    }

    override fun onResume() {
        super.onResume()
        updateHeader()
    }

    private fun updateHeader() {
        val a = account
        val localName = Store.profileName(this)
        profileName.text = when {
            !localName.isBlank() -> localName
            a?.displayName?.isNotBlank() == true -> a.displayName!!
            else -> "Add your name"
        }
        profileEmail.text = a?.email ?: "Not signed in"

        if (a == null) {
            btnSignIn.visibility = View.VISIBLE
            btnSignOut.visibility = View.GONE
            backupActions.visibility = View.GONE
            avatar.imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.river))
            avatar.setImageResource(R.drawable.ic_person)
            avatar.setPadding(dp(18), dp(18), dp(18), dp(18))
        } else {
            btnSignIn.visibility = View.GONE
            btnSignOut.visibility = View.VISIBLE
            backupActions.visibility = View.VISIBLE
            loadAvatar(a.photoUrl)
        }
    }

    private fun loadAvatar(uri: Uri?) {
        avatar.imageTintList = android.content.res.ColorStateList.valueOf(getColor(R.color.river))
        avatar.setImageResource(R.drawable.ic_person)
        avatar.setPadding(dp(18), dp(18), dp(18), dp(18))
        if (uri == null) return
        io.execute {
            val bmp: Bitmap? = try {
                URL(uri.toString()).openStream().use { BitmapFactory.decodeStream(it) }
            } catch (e: Exception) {
                null
            }
            if (bmp != null) {
                runOnUiThread {
                    avatar.imageTintList = null
                    avatar.setPadding(0, 0, 0, 0)
                    avatar.setImageBitmap(bmp)
                }
            }
        }
    }

    private fun dp(v: Int) = (v * resources.displayMetrics.density).toInt()

    private fun doBackup() {
        val email = account?.email
        if (email == null) { Toast.makeText(this, "Sign in first.", Toast.LENGTH_SHORT).show(); return }
        val json = Store.rawData(this)
        Toast.makeText(this, "Backing up...", Toast.LENGTH_SHORT).show()
        io.execute {
            try {
                DriveBackup.backup(this, email, json)
                runOnUiThread { Toast.makeText(this, "Backed up to Google Drive.", Toast.LENGTH_SHORT).show() }
            } catch (e: DriveBackup.NeedsConsent) {
                runOnUiThread { Toast.makeText(this, "Please sign in again to allow Drive access.", Toast.LENGTH_LONG).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Backup failed. Check your internet and try again.", Toast.LENGTH_LONG).show() }
            }
        }
    }

    private fun doRestore() {
        val email = account?.email
        if (email == null) { Toast.makeText(this, "Sign in first.", Toast.LENGTH_SHORT).show(); return }
        Toast.makeText(this, "Checking for a backup...", Toast.LENGTH_SHORT).show()
        io.execute {
            try {
                val json = DriveBackup.restore(this, email)
                runOnUiThread {
                    if (json == null) {
                        Toast.makeText(this, "No backup found yet.", Toast.LENGTH_LONG).show()
                    } else {
                        AlertDialog.Builder(this)
                            .setTitle("Restore backup?")
                            .setMessage("This replaces the cities and spots saved on this phone with your Google Drive backup.")
                            .setPositiveButton("Restore") { _, _ ->
                                if (Store.setRawData(this, json)) {
                                    Toast.makeText(this, "Restored.", Toast.LENGTH_LONG).show()
                                } else {
                                    Toast.makeText(this, "That backup looked corrupted.", Toast.LENGTH_LONG).show()
                                }
                            }
                            .setNegativeButton("Cancel", null)
                            .show()
                    }
                }
            } catch (e: DriveBackup.NeedsConsent) {
                runOnUiThread { Toast.makeText(this, "Please sign in again to allow Drive access.", Toast.LENGTH_LONG).show() }
            } catch (e: Exception) {
                runOnUiThread { Toast.makeText(this, "Restore failed. Check your internet and try again.", Toast.LENGTH_LONG).show() }
            }
        }
    }
}
