package com.agrilink.app

import android.content.Intent
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.agrilink.app.ui.AgriLinkRoot
import com.agrilink.app.ui.common.LocaleHelper
import com.agrilink.app.ui.common.SystemNotifications
import com.agrilink.app.ui.nav.DeepLink
import com.agrilink.app.ui.theme.AgriLinkTheme

class MainActivity : AppCompatActivity() {

    private var deepLink by mutableStateOf<DeepLink?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        deepLink = intent.toDeepLink()
        setContent {
            AgriLinkTheme(language = LocaleHelper.current()) {
                AgriLinkRoot(deepLink = deepLink, onDeepLinkHandled = { deepLink = null })
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        deepLink = intent.toDeepLink()
    }

    private fun Intent.toDeepLink(): DeepLink? {
        val type = getStringExtra(SystemNotifications.EXTRA_REF_TYPE) ?: return null
        val id = getStringExtra(SystemNotifications.EXTRA_REF_ID) ?: return null
        return DeepLink(type, id)
    }
}
