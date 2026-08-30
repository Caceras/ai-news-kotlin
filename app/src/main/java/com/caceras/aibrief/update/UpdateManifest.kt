package com.caceras.aibrief.update

import org.json.JSONObject

/**
 * Describes the newest build published to the direct-install channel.
 *
 * The release pipeline writes this document alongside the APK in every GitHub
 * Release, so the installed app can discover updates with a single request and
 * without depending on the shape of the GitHub API.
 */
data class UpdateManifest(
    val versionCode: Int,
    val versionName: String,
    val buildNumber: Int,
    val apkUrl: String,
    val notes: String,
) {
    /** Human-readable build identity, matching how the app reports its own. */
    val displayVersion: String get() = "$versionName ($buildNumber)"

    /**
     * Android will only install an APK whose [versionCode] exceeds the one
     * already on the device, so that comparison alone decides whether an
     * update is worth offering.
     */
    fun isNewerThan(installedVersionCode: Int): Boolean = versionCode > installedVersionCode
}

/** Reads the `update.json` document published with every direct-install release. */
object UpdateManifestParser {

    /**
     * Returns the manifest described by [raw], or `null` when the document is
     * malformed or fails validation.
     *
     * The APK URL is required to be HTTPS: the app downloads and installs
     * whatever it points at, so a plaintext or relative URL is rejected rather
     * than trusted.
     */
    fun parse(raw: String): UpdateManifest? {
        val json = runCatching { JSONObject(raw) }.getOrNull() ?: return null

        val versionCode = json.optInt("versionCode", 0)
        if (versionCode <= 0) return null

        val apkUrl = json.optString("apkUrl").trim()
        if (!apkUrl.startsWith("https://")) return null

        return UpdateManifest(
            versionCode = versionCode,
            versionName = json.optString("versionName").trim().ifBlank { UNKNOWN_VERSION_NAME },
            buildNumber = json.optInt("buildNumber", 0),
            apkUrl = apkUrl,
            notes = json.optString("notes").trim(),
        )
    }

    private const val UNKNOWN_VERSION_NAME = "unknown"
}
