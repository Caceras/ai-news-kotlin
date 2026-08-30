package com.caceras.aibrief.update

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.caceras.aibrief.BuildConfig
import java.io.File
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * What the update banner is currently showing.
 *
 * [Idle] covers every case where nothing should be displayed: the Play build,
 * an app that is already current, and a check that quietly failed.
 */
sealed interface UpdateState {
    data object Idle : UpdateState

    /** A newer build exists and is waiting to be downloaded. */
    data class Available(val manifest: UpdateManifest) : UpdateState

    /** The APK is transferring; [progress] runs from `0f` to `1f`. */
    data class Downloading(val progress: Float) : UpdateState

    /**
     * The APK is on the device but could not be opened yet because this app is
     * not allowed to install other apps.
     */
    data class NeedsPermission(val apk: File) : UpdateState

    /** The download or handoff failed; the person can retry. */
    data class Failed(val manifest: UpdateManifest) : UpdateState
}

/**
 * Drives the in-app updater for direct-install builds.
 *
 * On builds distributed through Google Play the whole feature is compiled out
 * by [BuildConfig.SELF_UPDATE_ENABLED], and this view model stays [UpdateState.Idle]
 * for the life of the process.
 */
class UpdateViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = UpdateRepository(BuildConfig.UPDATE_MANIFEST_URL)
    private val downloadDirectory = File(application.cacheDir, DOWNLOAD_DIRECTORY)

    private val _state = MutableStateFlow<UpdateState>(UpdateState.Idle)
    val state = _state.asStateFlow()

    init {
        checkForUpdate()
    }

    /** Looks for a newer published build, leaving the banner hidden if there is none. */
    fun checkForUpdate() {
        if (!BuildConfig.SELF_UPDATE_ENABLED) return
        if (_state.value is UpdateState.Downloading) return

        viewModelScope.launch {
            val manifest = repository.latestManifest() ?: return@launch
            if (manifest.isNewerThan(BuildConfig.VERSION_CODE)) {
                _state.value = UpdateState.Available(manifest)
            }
        }
    }

    /** Downloads [manifest] and opens the system installer once it lands. */
    fun downloadAndInstall(manifest: UpdateManifest) {
        if (_state.value is UpdateState.Downloading) return

        _state.value = UpdateState.Downloading(progress = 0f)
        viewModelScope.launch {
            // Only ever keep the build currently being installed.
            repository.clearDownloads(downloadDirectory)
            val apk = repository.downloadApk(manifest, downloadDirectory) { progress ->
                _state.value = UpdateState.Downloading(progress)
            }
            _state.value = if (apk == null) UpdateState.Failed(manifest) else install(apk, manifest)
        }
    }

    /**
     * Retries the handoff after the person has granted the install permission,
     * so returning from settings finishes the update without a fresh download.
     */
    fun retryInstall(apk: File) {
        if (!apk.exists()) {
            _state.value = UpdateState.Idle
            return
        }
        val context = getApplication<Application>()
        val handedOver = UpdateInstaller.canInstall(context) && UpdateInstaller.install(context, apk)
        _state.value = if (handedOver) UpdateState.Idle else UpdateState.NeedsPermission(apk)
    }

    /** Sends the person to the settings screen that allows installing this update. */
    fun requestInstallPermission() {
        UpdateInstaller.openInstallPermissionSettings(getApplication<Application>())
    }

    /** Hides the banner until the next update check. */
    fun dismiss() {
        _state.value = UpdateState.Idle
    }

    private fun install(apk: File, manifest: UpdateManifest): UpdateState {
        val context = getApplication<Application>()
        if (!UpdateInstaller.canInstall(context)) return UpdateState.NeedsPermission(apk)
        return if (UpdateInstaller.install(context, apk)) UpdateState.Idle else UpdateState.Failed(manifest)
    }

    private companion object {
        const val DOWNLOAD_DIRECTORY = "updates"
    }
}
