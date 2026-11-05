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
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import com.caceras.aibrief.ui.theme.Brief
import com.caceras.aibrief.ui.theme.Space

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
        Spacer(Modifier.height(Space.xl))
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
                style = Brief.type.button,
                color = Brief.colors.ink,
            )
            detail?.let {
                Spacer(Modifier.height(Space.xs))
                Text(
                    text = it,
                    style = Brief.type.meta,
                    color = Brief.colors.inkMuted,
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(Space.l)) {
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
            style = Brief.type.button,
            color = Brief.colors.ink,
        )
        Spacer(Modifier.height(Space.s))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(Space.hairline)
                .background(Brief.colors.rule),
        ) {
            // A zero-width fill is skipped rather than laid out at fraction 0.
            val completed = progress.coerceIn(0f, 1f)
            if (completed > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(fraction = completed)
                        .height(Space.hairline)
                        .background(Brief.colors.ink),
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
        style = if (emphasised) Brief.type.button else Brief.type.deck,
        color = if (emphasised) {
            Brief.colors.ink
        } else {
            Brief.colors.inkMuted
        },
        modifier = Modifier
            .minimumInteractiveComponentSize()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = Space.xs),
    )
}
