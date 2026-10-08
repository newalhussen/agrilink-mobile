package com.agrilink.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.agrilink.app.R
import com.agrilink.app.ui.theme.Organic
import com.agrilink.app.ui.theme.PillShape

/** Top bar with back arrow, a Caprasimo title and optional trailing actions. */
@Composable
fun AgriTopBar(title: String, onBack: (() -> Unit)?, modifier: Modifier = Modifier, actions: @Composable () -> Unit = {}) {
    Row(
        modifier.fillMaxWidth().statusBarsPadding().height(60.dp).padding(horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (onBack != null) IconAction(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.action_back), onBack)
        else Box(Modifier.size(12.dp))
        Text(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f).padding(horizontal = 4.dp), maxLines = 1)
        actions()
    }
}

data class NavTab(val route: String, val label: Int, val icon: ImageVector, val badge: Int = 0)

/** Bottom navigation in the design's style: the selected tab sits in a soft accent pill. */
@Composable
fun AgriBottomBar(tabs: List<NavTab>, selectedRoute: String?, onSelect: (String) -> Unit, modifier: Modifier = Modifier) {
    Row(
        modifier.fillMaxWidth().background(Organic.Bg).windowInsetsPadding(WindowInsets.navigationBars).padding(top = 8.dp, bottom = 8.dp),
        horizontalArrangement = Arrangement.SpaceAround,
    ) {
        tabs.forEach { tab ->
            val selected = tab.route == selectedRoute
            Column(
                Modifier.weight(1f).clickable(onClick = { onSelect(tab.route) }, role = androidx.compose.ui.semantics.Role.Tab),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(3.dp),
            ) {
                Box(
                    Modifier.height(32.dp).size(width = 60.dp, height = 32.dp).clip(PillShape).background(if (selected) Organic.Accent200 else androidx.compose.ui.graphics.Color.Transparent),
                    contentAlignment = Alignment.Center,
                ) {
                    BadgedBox(badge = { if (tab.badge > 0) Badge(containerColor = Organic.Accent, contentColor = Organic.Bg) { Text("${tab.badge}") } }) {
                        Icon(tab.icon, null, Modifier.size(22.dp), tint = if (selected) Organic.Accent800 else Organic.Neutral700)
                    }
                }
                Text(
                    stringResource(tab.label),
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = if (selected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.SemiBold),
                    color = if (selected) Organic.Accent800 else Organic.Neutral700,
                    maxLines = 1,
                )
            }
        }
    }
}

/** Rounded sheet-like footer for the primary action of a screen. */
@Composable
fun BottomActionBar(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Column(
        modifier.fillMaxWidth().background(Organic.Bg).windowInsetsPadding(WindowInsets.navigationBars).padding(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) { content() }
}

@Composable
fun SurfaceRowClip(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(modifier.clip(RoundedCornerShape(24.dp)).background(Organic.Surface)) { content() }
}
