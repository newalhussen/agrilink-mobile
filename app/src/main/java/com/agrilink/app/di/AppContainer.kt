package com.agrilink.app.di

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.agrilink.app.BuildConfig
import com.agrilink.app.data.api.ApiFactory
import com.agrilink.app.data.local.AppDatabase
import com.agrilink.app.data.prefs.SessionStore
import com.agrilink.app.data.prefs.SettingsStore
import com.agrilink.app.data.prefs.SharedPrefsKeyValueStore
import com.agrilink.app.data.repo.AuthRepository
import com.agrilink.app.data.repo.DeliveryRepository
import com.agrilink.app.data.repo.MarketRepository
import com.agrilink.app.data.repo.NotificationRepository
import com.agrilink.app.data.repo.OrderRepository
import com.agrilink.app.data.repo.Outbox
import com.agrilink.app.data.repo.ProfileRepository
import com.agrilink.app.data.repo.WalletRepository
import com.agrilink.app.work.WorkScheduler

/** Hand-written dependency graph: one instance for the process, created by [com.agrilink.app.AgriLinkApp]. */
class AppContainer(val context: Context) {

    val session = SessionStore(SharedPrefsKeyValueStore(securePrefs(context)))
    val settings = SettingsStore(SharedPrefsKeyValueStore(context.getSharedPreferences("agrilink_settings", Context.MODE_PRIVATE)))

    val database: AppDatabase by lazy { AppDatabase.create(context) }
    val workScheduler = WorkScheduler(context)
    val outbox: Outbox by lazy { Outbox(database.pendingActions(), schedule = workScheduler::enqueueOutbox) }

    val apiFactory = ApiFactory(
        baseUrl = BuildConfig.API_BASE_URL,
        session = session,
        languageTag = { settings.language.value ?: "en" },
        debug = BuildConfig.DEBUG,
    )
    private val api get() = apiFactory.api

    val auth: AuthRepository by lazy { AuthRepository(api, session, settings, database.pendingActions()) }
    val market: MarketRepository by lazy { MarketRepository(api, database.listingCache()) }
    val orders: OrderRepository by lazy { OrderRepository(api, outbox) }
    val delivery: DeliveryRepository by lazy { DeliveryRepository(api) }
    val wallet: WalletRepository by lazy { WalletRepository(api) }
    val profile: ProfileRepository by lazy { ProfileRepository(api, context) }
    val notifications: NotificationRepository by lazy { NotificationRepository(api) }

    private companion object {
        /** Tokens go into encrypted preferences; if the device keystore misbehaves we fall back to private plain prefs. */
        fun securePrefs(context: Context): SharedPreferences = try {
            val key = MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build()
            EncryptedSharedPreferences.create(
                context, "agrilink_session", key,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
            )
        } catch (e: Exception) {
            context.getSharedPreferences("agrilink_session_fallback", Context.MODE_PRIVATE)
        }
    }
}
