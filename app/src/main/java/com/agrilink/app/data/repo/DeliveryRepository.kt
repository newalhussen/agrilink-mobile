package com.agrilink.app.data.repo

import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.apiCall
import com.agrilink.app.data.api.ApiService
import com.agrilink.app.data.api.dto.AvailabilityRequest
import com.agrilink.app.data.api.dto.DeliverRequest
import com.agrilink.app.data.api.dto.DeliveryDto
import com.agrilink.app.data.api.dto.DeliveryEventDto
import com.agrilink.app.data.api.dto.DriverProfileDto
import com.agrilink.app.data.api.dto.JobDto
import com.agrilink.app.data.api.dto.LocationPingRequest
import com.agrilink.app.data.api.dto.PageResponse
import com.agrilink.app.data.api.dto.PickupRequest

/** The driver's side of delivery: the job board, handover steps and tracking pings. */
class DeliveryRepository(private val api: ApiService) {

    suspend fun availableJobs(page: Int = 0): ApiResult<PageResponse<JobDto>> = apiCall { api.availableJobs(page = page) }

    suspend fun mine(statuses: List<String>?, page: Int = 0): ApiResult<PageResponse<DeliveryDto>> =
        apiCall { api.myDeliveries(statuses?.takeIf { it.isNotEmpty() }, page) }

    suspend fun get(id: String): ApiResult<DeliveryDto> = apiCall { api.delivery(id) }

    suspend fun events(id: String): ApiResult<List<DeliveryEventDto>> = apiCall { api.deliveryEvents(id) }

    suspend fun accept(id: String): ApiResult<DeliveryDto> = apiCall { api.acceptJob(id) }

    suspend fun release(id: String): ApiResult<DeliveryDto> = apiCall { api.releaseJob(id) }

    suspend fun pickup(id: String, code: String, weighedKg: Double, crates: Int?, note: String?, evidence: List<String>): ApiResult<DeliveryDto> =
        apiCall { api.pickup(id, PickupRequest(code.trim(), weighedKg, crates, note?.takeIf { it.isNotBlank() }, evidence.takeIf { it.isNotEmpty() })) }

    suspend fun start(id: String): ApiResult<DeliveryDto> = apiCall { api.startTrip(id) }

    suspend fun deliver(id: String, code: String, note: String?, evidence: List<String>): ApiResult<DeliveryDto> =
        apiCall { api.deliver(id, DeliverRequest(code.trim(), note?.takeIf { it.isNotBlank() }, evidence.takeIf { it.isNotEmpty() })) }

    suspend fun ping(id: String, latitude: Double, longitude: Double): ApiResult<Unit> =
        apiCall { api.pingLocation(id, LocationPingRequest(latitude, longitude)) }

    suspend fun setAvailability(availability: String): ApiResult<DriverProfileDto> =
        apiCall { api.setAvailability(AvailabilityRequest(availability)) }
}
