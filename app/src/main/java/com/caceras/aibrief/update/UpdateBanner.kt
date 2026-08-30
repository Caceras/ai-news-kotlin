package com.caceras.aibrief.update

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/**
 * A single quiet line offering the newest direct-install build.
 *
 * Deliberately typographic rather than a card or dialog: an update is useful
 * information, not an interruption, and the feed below it stays readable.
 * Renders nothing at all in [UpdateState.Idle], which is every Play build.
 */
@Composable
fun UpdateBanner(
    state: UpdateState,
    onInstall: (UpdateManifest) -> Unit,
    onGrantPermission: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (state is UpdateState.Idle) return

    Column(modifier = modifier.fillMaxWidth()) {
        when (state) {
            is UpdateState.Available -> ActionRow(
                label = "new build ready  ·  ${state.manifest.displayVersion}",
                detail = state.manifest.notes.takeIf { it.isNotBlank() },
                actionText = "install",
                onAction = { onInstall(state.manifest) },
                onDismiss = onDismiss,
            )

            is UpdateState.Downloading -> ProgressRow(progress = state.progress)

            // The install resumes on its own once the person returns from settings.
            is UpdateState.NeedsPermission -> ActionRow(
                label = "AI Brief needs permission to install updates",
                detail = "grant it once and this update continues automatically",
                actionText = "settings",
                onAction = onGrantPermission,
                onDismiss = onDismiss,
            )

            is UpdateState.Failed -> ActionRow(
                label = "update could not be downloaded",
                detail = null,
                actionText = "retry",
                onAction = { onInstall(state.manifest) },
                onDismiss = onDismiss,
            )

            UpdateState.Idle -> Unit
        }
        Spacer(Modifier.height(28.dp))
    }
}

/** Label on the left, a single verb on the right, dismiss underneath. */
@Composable
private fun ActionRow(
    label: String,
    detail: String?,
    actionText: String,
    onAction: () -> Unit,
    onDismiss: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground,
            )
            detail?.let {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            BannerAction(text = actionText, emphasised = true, onClick = onAction)
            BannerAction(text = "later", emphasised = false, onClick = onDismiss)
        }
    }
}

/** A hairline that fills as the APK arrives, replacing any spinner or percentage. */
@Composable
private fun ProgressRow(progress: Float) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "downloading update",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(MaterialTheme.colorScheme.outlineVariant),
        ) {
            // A zero-width fill is skipped rather than laid out at fraction 0.
            val completed = progress.coerceIn(0f, 1f)
            if (completed > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = completed)
                        .height(1.dp)
                        .background(MaterialTheme.colorScheme.onBackground),
                )
            }
        }
    }
}

@Composable
private fun BannerAction(
    text: String,
    emphasised: Boolean,
    onClick: () -> Unit,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = if (emphasised) FontWeight.Medium else FontWeight.Normal,
        color = if (emphasised) {
            MaterialTheme.colorScheme.onBackground
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 4.dp),
    )
}
