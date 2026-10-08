package com.agrilink.app.data.repo

import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.apiCall
import com.agrilink.app.core.onOk
import com.agrilink.app.data.api.ApiService
import com.agrilink.app.data.api.AppJson
import com.agrilink.app.data.api.dto.AddPhotoRequest
import com.agrilink.app.data.api.dto.CategoryDto
import com.agrilink.app.data.api.dto.ChangeStatusRequest
import com.agrilink.app.data.api.dto.CreateListingRequest
import com.agrilink.app.data.api.dto.ListingDto
import com.agrilink.app.data.api.dto.PageResponse
import com.agrilink.app.data.api.dto.ProductDto
import com.agrilink.app.data.api.dto.PublicFarmerDto
import com.agrilink.app.data.api.dto.RegionDto
import com.agrilink.app.data.api.dto.UpdateListingRequest
import com.agrilink.app.data.local.ListingCacheDao
import com.agrilink.app.data.local.ListingCacheEntity
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.encodeToString
import kotlinx.serialization.serializer

/** Filters of the market screen. The first page of every distinct query is cached for offline use. */
data class ListingQuery(
    val q: String? = null,
    val categoryId: String? = null,
    val productId: String? = null,
    val regionId: String? = null,
    val grade: String? = null,
    val organic: Boolean? = null,
    val minPrice: Double? = null,
    val maxPrice: Double? = null,
    val sort: String? = null,
    val lat: Double? = null,
    val lon: Double? = null,
) {
    fun cacheKey(): String = listOf(q?.trim()?.lowercase(), categoryId, productId, regionId, grade, organic, minPrice, maxPrice, sort)
        .joinToString("|") { it?.toString() ?: "" }
}

data class CachedListings(val page: PageResponse<ListingDto>, val savedAt: Long)

/** Catalogue, regions, market search, public farmer profiles and the farmer's own listings. */
class MarketRepository(
    private val api: ApiService,
    private val cache: ListingCacheDao,
    private val now: () -> Long = System::currentTimeMillis,
) {
    private var regionsCache: List<RegionDto>? = null
    private var categoriesCache: List<CategoryDto>? = null
    private val pageSerializer = PageResponse.serializer(ListingDto.serializer())

    suspend fun regions(): ApiResult<List<RegionDto>> {
        regionsCache?.let { return ApiResult.Ok(it) }
        return apiCall { api.regions() }.onOk { regionsCache = it }
    }

    suspend fun categories(): ApiResult<List<CategoryDto>> {
        categoriesCache?.let { return ApiResult.Ok(it) }
        return apiCall { api.categories() }.onOk { categoriesCache = it }
    }

    suspend fun products(categoryId: String? = null, q: String? = null): ApiResult<List<ProductDto>> =
        apiCall { api.products(categoryId, q) }

    suspend fun search(query: ListingQuery, page: Int = 0): ApiResult<PageResponse<ListingDto>> =
        apiCall {
            api.searchListings(
                q = query.q?.takeIf { it.isNotBlank() }, categoryId = query.categoryId, productId = query.productId,
                regionId = query.regionId, minPrice = query.minPrice, maxPrice = query.maxPrice, grade = query.grade,
                organic = query.organic, lat = query.lat, lon = query.lon, sort = query.sort, page = page,
            )
        }.onOk { if (page == 0) saveCache(query, it) }

    suspend fun cached(query: ListingQuery): CachedListings? {
        val entity = cache.get(query.cacheKey()) ?: return null
        val page = runCatching { AppJson.decodeFromString(pageSerializer, entity.json) }.getOrNull() ?: return null
        return CachedListings(page, entity.savedAt)
    }

    private suspend fun saveCache(query: ListingQuery, page: PageResponse<ListingDto>) {
        cache.put(ListingCacheEntity(query.cacheKey(), AppJson.encodeToString(pageSerializer, page), now()))
        cache.deleteOlderThan(now() - SEVEN_DAYS)
    }

    suspend fun listing(id: String): ApiResult<ListingDto> = apiCall { api.listing(id) }

    suspend fun publicFarmer(id: String): ApiResult<PublicFarmerDto> = apiCall { api.publicFarmer(id) }

    // ----------------------------------------------------------------------------------- farmer side

    suspend fun myListings(status: String? = null): ApiResult<PageResponse<ListingDto>> = apiCall { api.myListings(status) }

    suspend fun createListing(request: CreateListingRequest): ApiResult<ListingDto> = apiCall { api.createListing(request) }

    suspend fun updateListing(id: String, request: UpdateListingRequest): ApiResult<ListingDto> =
        apiCall { api.updateListing(id, request) }

    suspend fun changeListingStatus(id: String, status: String): ApiResult<ListingDto> =
        apiCall { api.changeListingStatus(id, ChangeStatusRequest(status)) }

    suspend fun addPhoto(listingId: String, fileId: String, primary: Boolean = false): ApiResult<ListingDto> =
        apiCall { api.addListingPhoto(listingId, AddPhotoRequest(fileId, primary)) }

    private companion object {
        const val SEVEN_DAYS = 7L * 24 * 60 * 60 * 1000
    }
}
