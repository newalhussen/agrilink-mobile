package com.agrilink.app.data.api

import com.agrilink.app.data.api.dto.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

/** The whole AgriLink REST API used by the app. Paths are relative to `.../api/v1/`. */
interface ApiService {

    // ------------------------------------------------------------------------------------------ auth
    @POST("auth/register") suspend fun register(@Body body: RegisterRequest): RegisterResponse
    @POST("auth/otp/request") suspend fun requestOtp(@Body body: OtpRequest): OtpRequestResponse
    @POST("auth/otp/verify") suspend fun verifyOtp(@Body body: OtpVerifyRequest): AuthResponse
    @POST("auth/login") suspend fun login(@Body body: LoginRequest): AuthResponse
    @POST("auth/logout") suspend fun logout(@Body body: LogoutRequest)

    // ------------------------------------------------------------------------------------------ account
    @GET("users/me") suspend fun me(): UserDto
    @PATCH("users/me") suspend fun updateMe(@Body body: UpdateMeRequest): UserDto

    @GET("farmers/me") suspend fun farmerProfile(): FarmerProfileDto
    @PATCH("farmers/me") suspend fun updateFarmerProfile(@Body body: UpdateFarmerProfileRequest): FarmerProfileDto
    @GET("farmers/{id}/public") suspend fun publicFarmer(@Path("id") id: String): PublicFarmerDto

    @GET("buyers/me") suspend fun buyerProfile(): BuyerProfileDto
    @PATCH("buyers/me") suspend fun updateBuyerProfile(@Body body: UpdateBuyerProfileRequest): BuyerProfileDto

    @GET("drivers/me") suspend fun driverProfile(): DriverProfileDto
    @PATCH("drivers/me") suspend fun updateDriverProfile(@Body body: UpdateDriverProfileRequest): DriverProfileDto
    @PUT("drivers/me/availability") suspend fun setAvailability(@Body body: AvailabilityRequest): DriverProfileDto
    @POST("drivers/me/location") suspend fun sendDriverLocation(@Body body: LocationRequest)

    @GET("verification") suspend fun verificationStatus(): VerificationStatusDto
    @POST("verification/documents") suspend fun addDocument(@Body body: AddDocumentRequest): VerificationDocumentDto
    @POST("verification/submit") suspend fun submitVerification(): VerificationStatusDto

    @Multipart
    @POST("files")
    suspend fun upload(@Part file: MultipartBody.Part, @Part("purpose") purpose: RequestBody): FileResponse

    // ------------------------------------------------------------------------------------------ reference data
    @GET("regions") suspend fun regions(): List<RegionDto>
    @GET("categories") suspend fun categories(): List<CategoryDto>
    @GET("products") suspend fun products(
        @Query("categoryId") categoryId: String? = null,
        @Query("q") q: String? = null,
    ): List<ProductDto>

    // ------------------------------------------------------------------------------------------ marketplace
    @GET("listings") suspend fun searchListings(
        @Query("q") q: String? = null,
        @Query("categoryId") categoryId: String? = null,
        @Query("productId") productId: String? = null,
        @Query("regionId") regionId: String? = null,
        @Query("farmerId") farmerId: String? = null,
        @Query("minPrice") minPrice: Double? = null,
        @Query("maxPrice") maxPrice: Double? = null,
        @Query("grade") grade: String? = null,
        @Query("organic") organic: Boolean? = null,
        @Query("minQuantity") minQuantity: Double? = null,
        @Query("lat") lat: Double? = null,
        @Query("lon") lon: Double? = null,
        @Query("sort") sort: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 20,
    ): PageResponse<ListingDto>

    @GET("listings/{id}") suspend fun listing(@Path("id") id: String): ListingDto
    @POST("listings") suspend fun createListing(@Body body: CreateListingRequest): ListingDto
    @PATCH("listings/{id}") suspend fun updateListing(@Path("id") id: String, @Body body: UpdateListingRequest): ListingDto
    @PUT("listings/{id}/status") suspend fun changeListingStatus(@Path("id") id: String, @Body body: ChangeStatusRequest): ListingDto
    @POST("listings/{id}/photos") suspend fun addListingPhoto(@Path("id") id: String, @Body body: AddPhotoRequest): ListingDto
    @DELETE("listings/{id}/photos/{photoId}") suspend fun deleteListingPhoto(@Path("id") id: String, @Path("photoId") photoId: String): ListingDto
    @GET("farmers/me/listings") suspend fun myListings(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 50,
    ): PageResponse<ListingDto>

    // ------------------------------------------------------------------------------------------ orders
    @POST("orders/quote") suspend fun quote(@Body body: CreateOrderRequest): QuoteResponse
    @POST("orders") suspend fun createOrder(@Body body: CreateOrderRequest): OrderDto
    @GET("orders") suspend fun orders(
        @Query("status") status: List<String>? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30,
    ): PageResponse<OrderListItemDto>

    @GET("orders/{id}") suspend fun order(@Path("id") id: String): OrderDto
    @GET("orders/{id}/timeline") suspend fun orderTimeline(@Path("id") id: String): List<TimelineEntryDto>
    @POST("orders/{id}/accept") suspend fun acceptOrder(@Path("id") id: String): OrderDto
    @POST("orders/{id}/reject") suspend fun rejectOrder(@Path("id") id: String, @Body body: ReasonRequest): OrderDto
    @POST("orders/{id}/ready") suspend fun markReady(@Path("id") id: String): OrderDto
    @POST("orders/{id}/confirm-delivery") suspend fun confirmDelivery(@Path("id") id: String): OrderDto
    @POST("orders/{id}/cancel") suspend fun cancelOrder(@Path("id") id: String, @Body body: ReasonRequest): OrderDto

    // ------------------------------------------------------------------------------------------ payments
    @GET("payments/methods") suspend fun paymentMethods(): List<PaymentMethodInfoDto>
    @POST("orders/{id}/payments") suspend fun pay(@Path("id") orderId: String, @Body body: InitiatePaymentRequest): PaymentDto

    // ------------------------------------------------------------------------------------------ delivery
    @GET("deliveries/available") suspend fun availableJobs(
        @Query("regionId") regionId: String? = null,
        @Query("minFee") minFee: Double? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30,
    ): PageResponse<JobDto>

    @GET("deliveries/mine") suspend fun myDeliveries(
        @Query("status") status: List<String>? = null,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30,
    ): PageResponse<DeliveryDto>

    @GET("deliveries/{id}") suspend fun delivery(@Path("id") id: String): DeliveryDto
    @GET("deliveries/{id}/events") suspend fun deliveryEvents(@Path("id") id: String): List<DeliveryEventDto>
    @POST("deliveries/{id}/accept") suspend fun acceptJob(@Path("id") id: String): DeliveryDto
    @POST("deliveries/{id}/release") suspend fun releaseJob(@Path("id") id: String): DeliveryDto
    @POST("deliveries/{id}/pickup") suspend fun pickup(@Path("id") id: String, @Body body: PickupRequest): DeliveryDto
    @POST("deliveries/{id}/start") suspend fun startTrip(@Path("id") id: String): DeliveryDto
    @POST("deliveries/{id}/location") suspend fun pingLocation(@Path("id") id: String, @Body body: LocationPingRequest)
    @POST("deliveries/{id}/deliver") suspend fun deliver(@Path("id") id: String, @Body body: DeliverRequest): DeliveryDto

    // ------------------------------------------------------------------------------------------ wallet
    @GET("wallet") suspend fun wallet(): WalletDto
    @GET("wallet/transactions") suspend fun walletTransactions(
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30,
    ): PageResponse<WalletTransactionDto>

    @POST("wallet/withdrawals") suspend fun withdraw(@Body body: WithdrawRequest): PayoutDto

    // ------------------------------------------------------------------------------------------ ratings & disputes
    @POST("orders/{id}/ratings") suspend fun rate(@Path("id") orderId: String, @Body body: CreateRatingRequest): RatingDto
    @GET("orders/{id}/ratings") suspend fun ratingsForOrder(@Path("id") orderId: String): List<RatingDto>
    @POST("orders/{id}/disputes") suspend fun openDispute(@Path("id") orderId: String, @Body body: OpenDisputeRequest): DisputeDto
    @GET("orders/{id}/disputes") suspend fun disputesForOrder(@Path("id") orderId: String): List<DisputeDto>
    @GET("disputes/{id}") suspend fun dispute(@Path("id") id: String): DisputeDto
    @POST("disputes/{id}/evidence") suspend fun addEvidence(@Path("id") id: String, @Body body: AddEvidenceRequest): DisputeDto

    // ------------------------------------------------------------------------------------------ notifications
    @GET("notifications") suspend fun notifications(
        @Query("unreadOnly") unreadOnly: Boolean = false,
        @Query("page") page: Int = 0,
        @Query("size") size: Int = 30,
    ): PageResponse<NotificationDto>

    @GET("notifications/unread-count") suspend fun unreadCount(): UnreadCountDto
    @POST("notifications/{id}/read") suspend fun markNotificationRead(@Path("id") id: String)
    @POST("notifications/read-all") suspend fun markAllNotificationsRead()
    @POST("devices") suspend fun registerDevice(@Body body: RegisterDeviceRequest)
}
