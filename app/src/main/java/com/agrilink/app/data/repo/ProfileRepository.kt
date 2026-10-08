package com.agrilink.app.data.repo

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.AppError
import com.agrilink.app.core.apiCall
import com.agrilink.app.data.api.ApiService
import com.agrilink.app.data.api.dto.AddDocumentRequest
import com.agrilink.app.data.api.dto.BuyerProfileDto
import com.agrilink.app.data.api.dto.DriverProfileDto
import com.agrilink.app.data.api.dto.FarmerProfileDto
import com.agrilink.app.data.api.dto.FileResponse
import com.agrilink.app.data.api.dto.UpdateBuyerProfileRequest
import com.agrilink.app.data.api.dto.UpdateDriverProfileRequest
import com.agrilink.app.data.api.dto.UpdateFarmerProfileRequest
import com.agrilink.app.data.api.dto.VerificationDocumentDto
import com.agrilink.app.data.api.dto.VerificationStatusDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import kotlin.math.max

/** Role profiles, the verification checklist and file uploads (listing photos, ID documents, evidence). */
class ProfileRepository(
    private val api: ApiService,
    private val context: Context,
) {
    suspend fun farmer(): ApiResult<FarmerProfileDto> = apiCall { api.farmerProfile() }
    suspend fun updateFarmer(request: UpdateFarmerProfileRequest): ApiResult<FarmerProfileDto> = apiCall { api.updateFarmerProfile(request) }

    suspend fun buyer(): ApiResult<BuyerProfileDto> = apiCall { api.buyerProfile() }
    suspend fun updateBuyer(request: UpdateBuyerProfileRequest): ApiResult<BuyerProfileDto> = apiCall { api.updateBuyerProfile(request) }

    suspend fun driver(): ApiResult<DriverProfileDto> = apiCall { api.driverProfile() }
    suspend fun updateDriver(request: UpdateDriverProfileRequest): ApiResult<DriverProfileDto> = apiCall { api.updateDriverProfile(request) }

    suspend fun verification(): ApiResult<VerificationStatusDto> = apiCall { api.verificationStatus() }
    suspend fun submitVerification(): ApiResult<VerificationStatusDto> = apiCall { api.submitVerification() }

    suspend fun addDocument(type: String, fileId: String): ApiResult<VerificationDocumentDto> =
        apiCall { api.addDocument(AddDocumentRequest(type, fileId)) }

    /** Reads an image the user picked or captured, downsizes it to fit a data plan, and uploads it. */
    suspend fun uploadImage(uri: Uri, purpose: String): ApiResult<FileResponse> {
        val bytes = withContext(Dispatchers.IO) { compress(uri) }
            ?: return ApiResult.Err(AppError("FILE_UNREADABLE", "That photo could not be read"))
        val part = MultipartBody.Part.createFormData("file", "photo.jpg", bytes.toRequestBody("image/jpeg".toMediaType()))
        val purposeBody = purpose.toRequestBody("text/plain".toMediaType())
        return apiCall { api.upload(part, purposeBody) }
    }

    private fun compress(uri: Uri): ByteArray? = runCatching {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val sample = sampleSize(bounds.outWidth, bounds.outHeight, MAX_EDGE)
        val bitmap = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: return null
        ByteArrayOutputStream().use { out ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, QUALITY, out)
            bitmap.recycle()
            out.toByteArray()
        }
    }.getOrNull()

    companion object {
        const val MAX_EDGE = 1600
        const val QUALITY = 82

        /** Power-of-two decode sample so the longest edge ends up at most [maxEdge] pixels. */
        fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
            var sample = 1
            var longest = max(width, height)
            while (longest / 2 >= maxEdge) {
                longest /= 2
                sample *= 2
            }
            return sample
        }
    }
}
