package com.caceras.aibrief.ui.components

import android.content.ActivityNotFoundException
import android.content.Intent
import androidx.core.net.toUri
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.ui.Alignment
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.CollectionInfo
import androidx.compose.ui.semantics.CollectionItemInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.collectionInfo
import androidx.compose.ui.semantics.collectionItemInfo
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import com.caceras.aibrief.data.PostBlock
import com.caceras.aibrief.ui.theme.Brief
import com.caceras.aibrief.ui.theme.BriefShapes
import com.caceras.aibrief.ui.theme.Space

/**
 * Renders one block of an original post.
 *
 * Every branch draws with type the app already defines, so an authored post
 * reads exactly like the rest of AI Brief and cannot introduce styling of its
 * own. Spacing between blocks is the caller's business, which keeps a post's
 * rhythm consistent no matter which blocks it happens to use.
 */
@Composable
fun PostBlockContent(block: PostBlock, modifier: Modifier = Modifier, opinion: Boolean = false) {
    val c = Brief.colors
    val bodyStyle = if (opinion) Brief.type.opinionBody else Brief.type.body
    when (block) {
        is PostBlock.Paragraph -> Text(block.text, style = bodyStyle, color = c.ink, modifier = modifier)

        // TalkBack readers move between a post's sections by heading.
        is PostBlock.Heading -> Text(block.text, style = Brief.type.bodyHeading, color = c.ink,
            modifier = modifier.semantics { heading() })

        // A pull quote: a heavy accent bar and serif italic, the same in every post.
        is PostBlock.Quote -> Row(modifier.fillMaxWidth().height(IntrinsicSize.Min)) {
            Box(Modifier.width(Space.xs).fillMaxHeight()
                .background(if (opinion) c.opinionAccent else c.accent))
            Column(Modifier.padding(start = Space.gutter)) {
                Text(block.text, style = Brief.type.quote, color = c.ink)
                block.attribution?.let { attribution ->
                    Spacer(Modifier.height(Space.s))
                    Text(attribution.uppercase(java.util.Locale.US), style = Brief.type.tag, color = c.inkMuted)
                }
            }
        }

        // TalkBack announces "list, N items" and each item's place in it.
        is PostBlock.Bullets -> Column(modifier.fillMaxWidth()
            .semantics { collectionInfo = CollectionInfo(rowCount = block.items.size, columnCount = 1) },
            verticalArrangement = Arrangement.spacedBy(Space.m)) {
            block.items.forEachIndexed { index, item ->
                Row(Modifier.semantics(mergeDescendants = true) {
                    collectionItemInfo = CollectionItemInfo(rowIndex = index, rowSpan = 1, columnIndex = 0, columnSpan = 1)
                }) {
                    // The dot sits on the middle of the first line, whichever body face is in use.
                    Box(Modifier.padding(top = if (opinion) Space.m else Space.s + Space.xxs).size(Space.s)
                        .background(if (opinion) c.opinionAccent else c.accent, BriefShapes.round))
                    Text(item, style = bodyStyle, color = c.ink, modifier = Modifier.padding(start = Space.m))
                }
            }
        }

        // The caption printed under the picture is its text alternative, so the
        // picture and caption are one TalkBack stop and the caption is read once.
        is PostBlock.Image -> Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) {}) {
            AsyncArticleImage(block.url, null, Modifier.fillMaxWidth())
            block.caption?.let { caption ->
                Spacer(Modifier.height(Space.s))
                Text(caption, style = Brief.type.meta, color = c.inkMuted)
            }
        }

        is PostBlock.Video -> {
            val context = LocalContext.current
            Column(modifier.fillMaxWidth().semantics(mergeDescendants = true) {}
                .clickable(role = Role.Button, onClickLabel = "Watch, opens outside AI Brief") {
                    try {
                        context.startActivity(Intent(Intent.ACTION_VIEW, block.url.toUri()))
                    } catch (_: ActivityNotFoundException) {
                        Toast.makeText(context, "No app is available to play this video.", Toast.LENGTH_LONG).show()
                    }
                }) {
                Box(Modifier.fillMaxWidth().aspectRatio(Ratio.VIDEO).background(c.masthead, BriefShapes.thumb),
                    contentAlignment = Alignment.Center) {
                    block.imageUrl?.let { AsyncArticleImage(it, null, Modifier.matchParentSize(), aspectRatio = null) }
                    Box(Modifier.size(Space.section + Space.xl).background(c.breaking, BriefShapes.round),
                        contentAlignment = Alignment.Center) {
                        Icon(Icons.Filled.PlayArrow, null, Modifier.size(Space.xxl), tint = c.onBreaking)
                    }
                }
                Spacer(Modifier.height(Space.s))
                Text("▶  Watch: ${block.title}", style = Brief.type.button, color = c.ink,
                    modifier = Modifier.spokenAs("Watch: ${block.title}"))
                Text("Opens in your video app or browser", style = Brief.type.meta, color = c.inkMuted)
            }
        }

        is PostBlock.Link -> {
            val openLink = rememberLinkOpener()
            Text(
                text = "${block.text} ↗",
                style = Brief.type.body.copy(textDecoration = TextDecoration.Underline),
                color = c.accent,
                modifier = modifier
                    .minimumInteractiveComponentSize()
                    .spokenAs(block.text)
                    .clickable(role = Role.Button, onClickLabel = "Open in browser") { openLink(block.url) }
                    .padding(vertical = Space.s),
            )
        }
    }
}
