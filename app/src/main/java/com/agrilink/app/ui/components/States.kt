package com.agrilink.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudOff
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.agrilink.app.R
import com.agrilink.app.core.AppError
import com.agrilink.app.ui.common.errorText
import com.agrilink.app.ui.theme.Organic

@Composable
fun LoadingBlock(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        CircularProgressIndicator(color = Organic.Accent, strokeWidth = 3.dp)
    }
}

/** Skeleton rows shown on a cold start instead of a spinner. */
@Composable
fun SkeletonList(rows: Int = 5, modifier: Modifier = Modifier) {
    Column(modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(rows) {
            Box(Modifier.fillMaxWidth().height(72.dp).clip(RoundedCornerShape(24.dp)).background(Organic.Neutral300.copy(alpha = 0.6f)))
        }
    }
}

@Composable
private fun CenteredMessage(icon: ImageVector, title: String, body: String?, modifier: Modifier, action: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth().padding(horizontal = 32.dp, vertical = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Box(Modifier.size(64.dp).clip(CircleShape).background(Organic.Neutral300), contentAlignment = Alignment.Center) {
            Icon(icon, null, tint = Organic.Neutral700, modifier = Modifier.size(30.dp))
        }
        Text(title, style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
        if (body != null) Text(body, style = MaterialTheme.typography.bodyMedium, color = Organic.Neutral700, textAlign = TextAlign.Center)
        action()
    }
}

@Composable
fun EmptyState(title: String, body: String? = null, modifier: Modifier = Modifier, icon: ImageVector = Icons.Outlined.Inbox, action: @Composable () -> Unit = {}) =
    CenteredMessage(icon, title, body, modifier, action)

@Composable
fun ErrorState(error: AppError, onRetry: (() -> Unit)?, modifier: Modifier = Modifier) {
    val offline = error.isNetwork
    CenteredMessage(
        icon = if (offline) Icons.Outlined.CloudOff else Icons.Outlined.ErrorOutline,
        title = if (offline) stringResource(R.string.error_offline_title) else stringResource(R.string.error_generic_title),
        body = error.errorText(),
        modifier = modifier,
    ) {
        if (onRetry != null) AgriButton(stringResource(R.string.action_try_again), onRetry, kind = ButtonKind.Secondary, height = 44)
    }
}

/** "Offline · showing prices from 08:12" strip. */
@Composable
fun OfflineBanner(text: String, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Organic.Neutral300).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.CloudOff, null, Modifier.size(18.dp), tint = Organic.Neutral800)
        Text(text, style = MaterialTheme.typography.bodySmall, color = Organic.Neutral900)
    }
}

/** "Saved on this phone. It will send when you have signal." (design: offline queued action) */
@Composable
fun QueuedBanner(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Organic.Accent100).padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(Icons.Outlined.CloudOff, null, Modifier.size(18.dp), tint = Organic.Accent800)
        Text(
            androidx.compose.ui.res.pluralStringResource(R.plurals.queued_actions, count, count),
            style = MaterialTheme.typography.bodySmall, color = Organic.Accent900,
        )
    }
}
