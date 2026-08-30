package com.caceras.aibrief.update

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File

/**
 * Hands a downloaded APK to the system package installer.
 *
 * Android never installs anything silently: this only ever opens the platform
 * installer, which then asks the person to confirm. Sideloading additionally
 * requires a one-time "install unknown apps" grant, which [canInstall] reports
 * and [openInstallPermissionSettings] takes the person to.
 */
object UpdateInstaller {

    /** True once the person has allowed this app to install other apps. */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    /**
     * Opens the system installer for [apk]. Returns `false` when no installer
     * is available, which should not happen on a normal device.
     */
    fun install(context: Context, apk: File): Boolean {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}$AUTHORITY_SUFFIX", apk)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, APK_MEDIA_TYPE)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return context.startActivitySafely(intent)
    }

    /** Sends the person to the settings screen that grants the install permission. */
    fun openInstallPermissionSettings(context: Context): Boolean {
        val intent = Intent(
            Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
            Uri.parse("package:${context.packageName}"),
        ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return context.startActivitySafely(intent)
    }

    private fun Context.startActivitySafely(intent: Intent): Boolean = try {
        startActivity(intent)
        true
    } catch (_: ActivityNotFoundException) {
        false
    }

    /** Must match the provider authority declared in `src/sideload/AndroidManifest.xml`. */
    private const val AUTHORITY_SUFFIX = ".updates"
    private const val APK_MEDIA_TYPE = "application/vnd.android.package-archive"
}
