package com.agrilink.app

import android.app.Application
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.SingletonImageLoader
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.crossfade
import com.agrilink.app.di.AppContainer
import com.agrilink.app.ui.common.LocaleHelper
import com.agrilink.app.ui.common.SystemNotifications
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AgriLinkApp : Application(), SingletonImageLoader.Factory {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        SystemNotifications.createChannel(this)
        container.settings.language.value?.let { LocaleHelper.apply(it) }

        // Background sync only while someone is signed in.
        appScope.launch {
            container.session.session.map { it != null }.collect { signedIn ->
                if (signedIn) container.workScheduler.schedulePeriodicNotificationSync() else container.workScheduler.cancelAll()
            }
        }
    }

    /** Images (public listing photos and private evidence) load through the authenticated OkHttp client. */
    override fun newImageLoader(context: PlatformContext): ImageLoader =
        ImageLoader.Builder(context)
            .components { add(OkHttpNetworkFetcherFactory(callFactory = { container.apiFactory.client })) }
            .crossfade(true)
            .build()
}
