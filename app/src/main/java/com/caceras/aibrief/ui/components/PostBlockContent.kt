package com.caceras.aibrief.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.caceras.aibrief.data.PostBlock

/**
 * Renders one block of an original post.
 *
 * Every branch draws with type the app already defines, so an authored post
 * reads exactly like the rest of AI Brief and cannot introduce styling of its
 * own. Spacing between blocks is the caller's business, which keeps a post's
 * rhythm consistent no matter which blocks it happens to use.
 */
@Composable
fun PostBlockContent(block: PostBlock, modifier: Modifier = Modifier) {
    when (block) {
        is PostBlock.Paragraph -> Text(
            text = block.text,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = modifier,
        )

        is PostBlock.Heading -> Text(
            text = block.text,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = modifier,
        )

        // Set behind a hairline rather than in quotation marks: the same
        // restraint the rest of the app uses to show hierarchy. IntrinsicSize.Min
        // lets the rule match the height of the text beside it.
        is PostBlock.Quote -> Row(
            modifier = modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min),
        ) {
            Box(
                modifier = Modifier
                    .width(1.dp)
                    .fillMaxHeight()
                    .background(MaterialTheme.colorScheme.outlineVariant),
            )
            Column(modifier = Modifier.padding(start = 16.dp)) {
                Text(
                    text = block.text,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                block.attribution?.let { attribution ->
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = attribution,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        is PostBlock.Bullets -> Column(modifier = modifier.fillMaxWidth()) {
            block.items.forEachIndexed { index, item ->
                if (index > 0) Spacer(Modifier.height(10.dp))
                Row {
                    Text(
                        text = "·",
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onBackground,
                        modifier = Modifier.padding(start = 14.dp),
                    )
                }
            }
        }

        is PostBlock.Image -> Column(modifier = modifier.fillMaxWidth()) {
            AsyncArticleImage(
                imageUrl = block.url,
                contentDescription = block.caption,
                modifier = Modifier.fillMaxWidth(),
            )
            block.caption?.let { caption ->
                Spacer(Modifier.height(10.dp))
                Text(
                    text = caption,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
