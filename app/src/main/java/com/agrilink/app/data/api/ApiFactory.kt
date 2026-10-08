package com.agrilink.app.data.api

import com.agrilink.app.data.api.dto.AuthResponse
import com.agrilink.app.data.api.dto.RefreshRequest
import com.agrilink.app.data.prefs.SessionStore
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.serialization.json.Json
import okhttp3.Authenticator
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.http.Body
import retrofit2.http.POST
import java.io.IOException
import java.util.concurrent.TimeUnit

val AppJson = Json {
    ignoreUnknownKeys = true
    coerceInputValues = true
    explicitNulls = false
    encodeDefaults = false
    isLenient = true
}

/** Refresh endpoint on a client without the authenticator, so a failing refresh cannot recurse. */
interface AuthRefreshApi {
    @POST("auth/refresh")
    fun refresh(@Body body: RefreshRequest): Call<AuthResponse>
}

/**
 * Builds the OkHttp/Retrofit stack: bearer token, language header, transparent token refresh on 401
 * (single-flight, because the server rotates refresh tokens) and sign-out when refresh is rejected.
 */
class ApiFactory(
    baseUrl: String,
    private val session: SessionStore,
    private val languageTag: () -> String,
    debug: Boolean,
    private val json: Json = AppJson,
) {
    private val base = baseUrl.toHttpUrl()
    private val jsonType = "application/json".toMediaType()

    private val plainClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private fun retrofit(client: OkHttpClient) = Retrofit.Builder()
        .baseUrl(base)
        .client(client)
        .addConverterFactory(json.asConverterFactory(jsonType))
        .build()

    private val refreshApi: AuthRefreshApi = retrofit(plainClient).create(AuthRefreshApi::class.java)

    val client: OkHttpClient = plainClient.newBuilder()
        .writeTimeout(60, TimeUnit.SECONDS)
        .addInterceptor(headerInterceptor())
        .authenticator(TokenAuthenticator(session, refreshApi))
        .apply {
            if (debug) addInterceptor(HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC })
        }
        .build()

    val api: ApiService = retrofit(client).create(ApiService::class.java)

    private fun headerInterceptor() = Interceptor { chain ->
        val original = chain.request()
        val builder = original.newBuilder().header("Accept-Language", languageTag())
        val token = session.accessToken
        if (token != null && !original.url.encodedPath.contains("/auth/") && original.header("Authorization") == null) {
            builder.header("Authorization", "Bearer $token")
        }
        chain.proceed(builder.build())
    }

    /** Replays a stored request (used by the offline outbox). Returns the HTTP status, or null if there is no signal. */
    fun replay(method: String, path: String, jsonBody: String?): Int? {
        val url = base.newBuilder().addEncodedPathSegments(path.trimStart('/')).build()
        val body = jsonBody?.toRequestBody(jsonType)
        val request = Request.Builder().url(url).method(method, body ?: if (method == "GET") null else "".toRequestBody(jsonType)).build()
        return try {
            client.newCall(request).execute().use { it.code }
        } catch (e: IOException) {
            null
        }
    }
}

internal class TokenAuthenticator(
    private val session: SessionStore,
    private val refreshApi: AuthRefreshApi,
) : Authenticator {

    private val lock = Any()

    override fun authenticate(route: Route?, response: Response): Request? {
        val request = response.request
        if (request.url.encodedPath.contains("/auth/")) return null
        if (responseCount(response) >= 2) return null

        synchronized(lock) {
            val sent = request.header("Authorization")?.removePrefix("Bearer ")
            val current = session.accessToken
            // Another request already refreshed while we waited for the lock: just retry with the new token.
            if (current != null && sent != null && current != sent) return withToken(request, current)

            val refreshToken = session.refreshToken ?: return null
            val result = try {
                refreshApi.refresh(RefreshRequest(refreshToken)).execute()
            } catch (e: IOException) {
                return null
            }
            if (!result.isSuccessful) {
                if (result.code() == 401 || result.code() == 403) session.clear(expired = true)
                return null
            }
            val auth = result.body() ?: return null
            session.save(auth)
            return withToken(request, auth.accessToken)
        }
    }

    private fun withToken(request: Request, token: String) = request.newBuilder().header("Authorization", "Bearer $token").build()

    private fun responseCount(response: Response): Int {
        var count = 1
        var prior = response.priorResponse
        while (prior != null) {
            count++
            prior = prior.priorResponse
        }
        return count
    }
}
