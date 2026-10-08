package com.agrilink.app.core

import com.agrilink.app.data.api.dto.ApiErrorBody
import com.agrilink.app.data.api.dto.FieldViolation
import kotlinx.serialization.json.Json
import retrofit2.HttpException
import java.io.IOException

/** Everything that can go wrong talking to the API, in one shape the UI can render. */
data class AppError(
    val code: String,
    val message: String,
    val httpStatus: Int = 0,
    val fieldErrors: List<FieldViolation> = emptyList(),
) {
    /** No signal, timeout or DNS failure: the request never reached the server. */
    val isNetwork: Boolean get() = code == NETWORK
    val isUnauthorized: Boolean get() = httpStatus == 401

    companion object {
        const val NETWORK = "NETWORK"
        const val UNKNOWN = "UNKNOWN"
    }
}

/** Outcome of a repository call. Repositories never throw for expected failures. */
sealed interface ApiResult<out T> {
    data class Ok<T>(val value: T) : ApiResult<T>
    data class Err(val error: AppError) : ApiResult<Nothing>
}

inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Ok -> ApiResult.Ok(transform(value))
    is ApiResult.Err -> this
}

inline fun <T> ApiResult<T>.onOk(block: (T) -> Unit): ApiResult<T> = also { if (it is ApiResult.Ok) block(it.value) }
inline fun <T> ApiResult<T>.onErr(block: (AppError) -> Unit): ApiResult<T> = also { if (it is ApiResult.Err) block(it.error) }
fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Ok)?.value

internal val errorJson = Json { ignoreUnknownKeys = true; coerceInputValues = true; isLenient = true }

/** Runs [block], converting HTTP and IO failures into [ApiResult.Err] with the server's structured error. */
suspend fun <T> apiCall(block: suspend () -> T): ApiResult<T> = try {
    ApiResult.Ok(block())
} catch (e: HttpException) {
    ApiResult.Err(e.toAppError())
} catch (e: IOException) {
    ApiResult.Err(AppError(AppError.NETWORK, e.message ?: "No connection"))
} catch (e: kotlinx.coroutines.CancellationException) {
    throw e
} catch (e: Exception) {
    ApiResult.Err(AppError(AppError.UNKNOWN, e.message ?: "Something went wrong"))
}

fun HttpException.toAppError(): AppError {
    val raw = runCatching { response()?.errorBody()?.string() }.getOrNull()
    val body = raw?.takeIf { it.isNotBlank() }?.let { runCatching { errorJson.decodeFromString<ApiErrorBody>(it) }.getOrNull() }
    return AppError(
        code = body?.code ?: "HTTP_${code()}",
        message = body?.message?.takeIf { it.isNotBlank() } ?: message(),
        httpStatus = code(),
        fieldErrors = body?.fieldErrors.orEmpty(),
    )
}
