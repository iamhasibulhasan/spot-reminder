package com.spotreminder.app

import android.content.Context
import com.google.android.gms.auth.GoogleAuthException
import com.google.android.gms.auth.GoogleAuthUtil
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Backs the app's saved cities/spots up to a private file in the signed-in user's
 * Google Drive "app data" folder (a hidden folder only this app can see, not visible
 * in the user's normal Drive). Talks to the Drive v3 REST API directly, no extra
 * Drive client library needed.
 */
object DriveBackup {
    private const val SCOPE = "oauth2:https://www.googleapis.com/auth/drive.appdata"
    private const val FILE_NAME = "spot-reminder-backup.json"
    private const val API = "https://www.googleapis.com/drive/v3/files"
    private const val UPLOAD_API = "https://www.googleapis.com/upload/drive/v3/files"

    class NeedsConsent(cause: Throwable) : Exception(cause)

    /** Blocking. Call from a background thread. May throw NeedsConsent if the user must re-approve access. */
    @Throws(IOException::class, NeedsConsent::class)
    private fun token(ctx: Context, accountEmail: String): String {
        return try {
            GoogleAuthUtil.getToken(ctx, accountEmail, SCOPE)
        } catch (e: com.google.android.gms.auth.UserRecoverableAuthException) {
            throw NeedsConsent(e)
        } catch (e: GoogleAuthException) {
            throw IOException("Could not get permission from Google.", e)
        }
    }

    private fun findFileId(token: String): String? {
        val q = URLEncoder.encode("name='$FILE_NAME' and trashed=false", "UTF-8")
        val url = URL("$API?spaces=appDataFolder&q=$q&fields=files(id)")
        val c = url.openConnection() as HttpURLConnection
        c.setRequestProperty("Authorization", "Bearer $token")
        c.connectTimeout = 15000
        c.readTimeout = 15000
        if (c.responseCode !in 200..299) throw IOException("Drive lookup failed (${c.responseCode})")
        val body = c.inputStream.bufferedReader().use { it.readText() }
        val arr = JSONObject(body).optJSONArray("files") ?: return null
        return if (arr.length() > 0) arr.getJSONObject(0).getString("id") else null
    }

    /** Uploads the given JSON as the backup, replacing any previous backup. */
    @Throws(IOException::class, NeedsConsent::class)
    fun backup(ctx: Context, accountEmail: String, json: String) {
        val tok = token(ctx, accountEmail)
        val existingId = findFileId(tok)
        if (existingId != null) {
            val url = URL("$UPLOAD_API/$existingId?uploadType=media")
            val c = url.openConnection() as HttpURLConnection
            c.requestMethod = "PATCH"
            c.doOutput = true
            c.setRequestProperty("Authorization", "Bearer $tok")
            c.setRequestProperty("Content-Type", "application/json; charset=utf-8")
            c.outputStream.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            if (c.responseCode !in 200..299) throw IOException("Backup update failed (${c.responseCode})")
        } else {
            val boundary = "spotreminder-${System.currentTimeMillis()}"
            val metadata = JSONObject().apply {
                put("name", FILE_NAME)
                put("parents", org.json.JSONArray().put("appDataFolder"))
            }
            val body = buildString {
                append("--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(metadata.toString())
                append("\r\n--$boundary\r\n")
                append("Content-Type: application/json; charset=UTF-8\r\n\r\n")
                append(json)
                append("\r\n--$boundary--")
            }
            val url = URL("$UPLOAD_API?uploadType=multipart")
            val c = url.openConnection() as HttpURLConnection
            c.requestMethod = "POST"
            c.doOutput = true
            c.setRequestProperty("Authorization", "Bearer $tok")
            c.setRequestProperty("Content-Type", "multipart/related; boundary=$boundary")
            c.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (c.responseCode !in 200..299) throw IOException("Backup create failed (${c.responseCode})")
        }
    }

    /** Returns the saved backup JSON, or null if no backup exists yet. */
    @Throws(IOException::class, NeedsConsent::class)
    fun restore(ctx: Context, accountEmail: String): String? {
        val tok = token(ctx, accountEmail)
        val id = findFileId(tok) ?: return null
        val url = URL("$API/$id?alt=media")
        val c = url.openConnection() as HttpURLConnection
        c.setRequestProperty("Authorization", "Bearer $tok")
        c.connectTimeout = 15000
        c.readTimeout = 15000
        if (c.responseCode !in 200..299) throw IOException("Restore failed (${c.responseCode})")
        return c.inputStream.bufferedReader().use { it.readText() }
    }
}
