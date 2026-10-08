@file:Suppress("unused")

package com.agrilink.app.data.api.dto

import kotlinx.serialization.Serializable

/** Wire format of the AgriLink REST API (see backend/README.md). Money is ETB, weights are kg. */

@Serializable
data class PageResponse<T>(
    val items: List<T> = emptyList(),
    val page: Int = 0,
    val size: Int = 20,
    val totalItems: Long = 0,
    val totalPages: Int = 0,
    val hasNext: Boolean = false,
)

@Serializable
data class FieldViolation(val field: String = "", val message: String = "")

@Serializable
data class ApiErrorBody(
    val status: Int = 0,
    val code: String = "ERROR",
    val message: String = "",
    val path: String? = null,
    val fieldErrors: List<FieldViolation> = emptyList(),
)

// ---------------------------------------------------------------------------------------------- auth & users

@Serializable
data class RegisterRequest(
    val phone: String,
    val fullName: String,
    val role: String,
    val password: String? = null,
    val preferredLanguage: String? = null,
)

@Serializable
data class RegisterResponse(
    val userId: String,
    val phone: String,
    val otpExpiresInSeconds: Int = 300,
    val resendAfterSeconds: Int = 60,
    val devOtp: String? = null,
)

@Serializable
data class OtpRequest(val phone: String, val purpose: String)

@Serializable
data class OtpRequestResponse(
    val expiresInSeconds: Int = 300,
    val resendAfterSeconds: Int = 60,
    val devOtp: String? = null,
)

@Serializable
data class OtpVerifyRequest(val phone: String, val code: String, val purpose: String)

@Serializable
data class LoginRequest(val phone: String, val password: String)

@Serializable
data class RefreshRequest(val refreshToken: String)

@Serializable
data class LogoutRequest(val refreshToken: String)

@Serializable
data class UserDto(
    val id: String,
    val phone: String,
    val email: String? = null,
    val fullName: String,
    val role: String,
    val accountStatus: String = "ACTIVE",
    val verificationStatus: String = "UNVERIFIED",
    val phoneVerified: Boolean = false,
    val preferredLanguage: String = "en",
    val profilePhotoUrl: String? = null,
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0,
    val createdAt: String? = null,
)

@Serializable
data class AuthResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long = 900,
    val user: UserDto,
)

@Serializable
data class UpdateMeRequest(
    val fullName: String? = null,
    val email: String? = null,
    val preferredLanguage: String? = null,
    val profilePhotoFileId: String? = null,
)

// ---------------------------------------------------------------------------------------------- reference data

@Serializable
data class RegionDto(
    val id: String,
    val code: String = "",
    val nameEn: String,
    val nameAm: String? = null,
    val nameOm: String? = null,
)

@Serializable
data class AddressDto(
    val regionId: String? = null,
    val regionName: String? = null,
    val zone: String? = null,
    val woreda: String? = null,
    val town: String? = null,
    val addressLine: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
)

@Serializable
data class CategoryDto(
    val id: String,
    val slug: String = "",
    val nameEn: String,
    val nameAm: String? = null,
    val nameOm: String? = null,
    val icon: String? = null,
    val sortOrder: Int = 0,
    val active: Boolean = true,
)

@Serializable
data class ProductDto(
    val id: String,
    val slug: String = "",
    val nameEn: String,
    val nameAm: String? = null,
    val nameOm: String? = null,
    val categoryId: String = "",
    val categoryName: String = "",
    val defaultUnit: String = "KG",
    val description: String? = null,
    val active: Boolean = true,
)

// ---------------------------------------------------------------------------------------------- listings

@Serializable
data class PhotoDto(
    val id: String,
    val fileId: String? = null,
    val url: String,
    val primary: Boolean = false,
    val sortOrder: Int = 0,
)

@Serializable
data class FarmerSummaryDto(
    val id: String,
    val fullName: String,
    val farmName: String? = null,
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0,
    val completedTrades: Int = 0,
    val verified: Boolean = false,
)

@Serializable
data class ListingDto(
    val id: String,
    val status: String,
    val product: ProductDto,
    val title: String,
    val description: String? = null,
    val qualityGrade: String = "A",
    val qualityNotes: String? = null,
    val packaging: String? = null,
    val harvestDate: String? = null,
    val availableFrom: String? = null,
    val availableUntil: String? = null,
    val unit: String = "KG",
    val unitWeightKg: Double = 1.0,
    val quantityTotal: Double = 0.0,
    val quantityAvailable: Double = 0.0,
    val minOrderQuantity: Double = 1.0,
    val pricePerUnit: Double = 0.0,
    val currency: String = "ETB",
    val organic: Boolean = false,
    val address: AddressDto? = null,
    val photos: List<PhotoDto> = emptyList(),
    val farmer: FarmerSummaryDto,
    val distanceKm: Double? = null,
    val createdAt: String? = null,
)

@Serializable
data class CreateListingRequest(
    val productId: String,
    val title: String? = null,
    val description: String? = null,
    val qualityGrade: String? = null,
    val qualityNotes: String? = null,
    val packaging: String? = null,
    val harvestDate: String? = null,
    val availableFrom: String? = null,
    val availableUntil: String? = null,
    val unit: String,
    val unitWeightKg: Double? = null,
    val quantity: Double,
    val minOrderQuantity: Double? = null,
    val pricePerUnit: Double,
    val organic: Boolean? = null,
    val address: AddressDto? = null,
    val photoFileIds: List<String>? = null,
    val publish: Boolean? = null,
)

@Serializable
data class UpdateListingRequest(
    val title: String? = null,
    val description: String? = null,
    val qualityGrade: String? = null,
    val packaging: String? = null,
    val harvestDate: String? = null,
    val quantityAvailable: Double? = null,
    val minOrderQuantity: Double? = null,
    val pricePerUnit: Double? = null,
    val organic: Boolean? = null,
)

@Serializable
data class ChangeStatusRequest(val status: String)

@Serializable
data class AddPhotoRequest(val fileId: String, val primary: Boolean? = null)

// ---------------------------------------------------------------------------------------------- profiles

@Serializable
data class FarmerProfileDto(
    val userId: String,
    val farmerType: String = "INDIVIDUAL",
    val farmName: String? = null,
    val memberCount: Int? = null,
    val address: AddressDto? = null,
    val landSizeHectares: Double? = null,
    val irrigated: Boolean = false,
    val expectedMonthlySupplyKg: Double? = null,
    val bio: String? = null,
    val faydaIdNumber: String? = null,
    val payoutMethod: String? = null,
    val payoutAccountName: String? = null,
    val payoutAccountNumber: String? = null,
    val completedTrades: Int = 0,
    val verificationStatus: String = "UNVERIFIED",
    val products: List<ProductDto> = emptyList(),
)

@Serializable
data class UpdateFarmerProfileRequest(
    val farmerType: String? = null,
    val farmName: String? = null,
    val memberCount: Int? = null,
    val address: AddressDto? = null,
    val landSizeHectares: Double? = null,
    val irrigated: Boolean? = null,
    val expectedMonthlySupplyKg: Double? = null,
    val bio: String? = null,
    val faydaIdNumber: String? = null,
    val payoutMethod: String? = null,
    val payoutAccountName: String? = null,
    val payoutAccountNumber: String? = null,
    val productIds: List<String>? = null,
)

@Serializable
data class PublicFarmerDto(
    val userId: String,
    val fullName: String,
    val farmName: String? = null,
    val farmerType: String = "INDIVIDUAL",
    val memberCount: Int? = null,
    val address: AddressDto? = null,
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0,
    val completedTrades: Int = 0,
    val verified: Boolean = false,
    val profilePhotoUrl: String? = null,
    val products: List<ProductDto> = emptyList(),
)

@Serializable
data class BuyerProfileDto(
    val userId: String,
    val buyerType: String = "INDIVIDUAL",
    val businessName: String? = null,
    val contactPerson: String? = null,
    val tinNumber: String? = null,
    val tradeLicenseNumber: String? = null,
    val address: AddressDto? = null,
    val deliveryInstructions: String? = null,
    val completedOrders: Int = 0,
    val verificationStatus: String = "UNVERIFIED",
)

@Serializable
data class UpdateBuyerProfileRequest(
    val buyerType: String? = null,
    val businessName: String? = null,
    val contactPerson: String? = null,
    val tinNumber: String? = null,
    val tradeLicenseNumber: String? = null,
    val address: AddressDto? = null,
    val deliveryInstructions: String? = null,
)

@Serializable
data class DriverProfileDto(
    val userId: String,
    val licenseNumber: String? = null,
    val licenseExpiryDate: String? = null,
    val vehicleType: String? = null,
    val vehiclePlate: String? = null,
    val vehicleMakeModel: String? = null,
    val vehicleYear: Int? = null,
    val capacityKg: Double? = null,
    val refrigerated: Boolean = false,
    val regionId: String? = null,
    val regionName: String? = null,
    val availability: String = "OFFLINE",
    val payoutMethod: String? = null,
    val payoutAccountName: String? = null,
    val payoutAccountNumber: String? = null,
    val completedDeliveries: Int = 0,
    val verificationStatus: String = "UNVERIFIED",
)

@Serializable
data class UpdateDriverProfileRequest(
    val licenseNumber: String? = null,
    val licenseExpiryDate: String? = null,
    val vehicleType: String? = null,
    val vehiclePlate: String? = null,
    val vehicleMakeModel: String? = null,
    val vehicleYear: Int? = null,
    val capacityKg: Double? = null,
    val refrigerated: Boolean? = null,
    val regionId: String? = null,
    val payoutMethod: String? = null,
    val payoutAccountName: String? = null,
    val payoutAccountNumber: String? = null,
)

@Serializable
data class AvailabilityRequest(val availability: String)

@Serializable
data class LocationRequest(val latitude: Double, val longitude: Double)

// ---------------------------------------------------------------------------------------------- verification & files

@Serializable
data class FileResponse(
    val id: String,
    val url: String,
    val contentType: String = "",
    val sizeBytes: Long = 0,
    val purpose: String = "",
    val visibility: String = "",
    val originalFilename: String = "",
)

@Serializable
data class VerificationDocumentDto(
    val id: String,
    val type: String,
    val status: String = "PENDING",
    val fileUrl: String = "",
    val note: String? = null,
    val uploadedAt: String? = null,
    val reviewedAt: String? = null,
)

@Serializable
data class VerificationStatusDto(
    val status: String = "UNVERIFIED",
    val requiredDocuments: List<String> = emptyList(),
    val documents: List<VerificationDocumentDto> = emptyList(),
    val latestNote: String? = null,
)

@Serializable
data class AddDocumentRequest(val type: String, val fileId: String, val note: String? = null)

// ---------------------------------------------------------------------------------------------- orders

@Serializable
data class OrderItemRequest(val listingId: String, val quantity: Double)

@Serializable
data class CreateOrderRequest(
    val items: List<OrderItemRequest>,
    val deliveryAddress: AddressDto,
    val deliveryContactName: String? = null,
    val deliveryContactPhone: String? = null,
    val requestedDeliveryDate: String? = null,
    val notes: String? = null,
)

@Serializable
data class QuoteLineDto(
    val listingId: String,
    val title: String = "",
    val unit: String = "KG",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val lineTotal: Double = 0.0,
    val weightKg: Double = 0.0,
)

@Serializable
data class QuoteResponse(
    val lines: List<QuoteLineDto> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val platformFee: Double = 0.0,
    val total: Double = 0.0,
    val currency: String = "ETB",
    val totalWeightKg: Double = 0.0,
    val distanceKm: Double? = null,
    val farmerId: String? = null,
)

@Serializable
data class DeadlinesDto(
    val farmerResponseDeadline: String? = null,
    val paymentDeadline: String? = null,
    val checkWindowEndsAt: String? = null,
)

@Serializable
data class OrderListItemDto(
    val id: String,
    val orderNumber: String,
    val status: String,
    val itemsSummary: String = "",
    val itemCount: Int = 0,
    val buyerName: String = "",
    val farmerName: String = "",
    val driverName: String? = null,
    val totalAmount: Double = 0.0,
    val currency: String = "ETB",
    val totalWeightKg: Double = 0.0,
    val deliveryTown: String? = null,
    val deadlines: DeadlinesDto = DeadlinesDto(),
    val createdAt: String? = null,
    val allowedActions: List<String> = emptyList(),
)

@Serializable
data class PartyViewDto(
    val id: String,
    val fullName: String,
    val displayName: String? = null,
    val phone: String? = null,
    val ratingAverage: Double = 0.0,
    val ratingCount: Int = 0,
    val verified: Boolean = false,
)

@Serializable
data class OrderItemDto(
    val id: String,
    val listingId: String? = null,
    val productId: String? = null,
    val productName: String = "",
    val qualityGrade: String? = null,
    val unit: String = "KG",
    val quantity: Double = 0.0,
    val unitPrice: Double = 0.0,
    val lineTotal: Double = 0.0,
    val weightKg: Double = 0.0,
)

@Serializable
data class AmountsDto(
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val platformFee: Double = 0.0,
    val total: Double = 0.0,
    val currency: String = "ETB",
)

@Serializable
data class PaymentSummaryDto(
    val id: String,
    val status: String,
    val method: String = "",
    val amount: Double = 0.0,
    val failureReason: String? = null,
    val expiresAt: String? = null,
)

@Serializable
data class DeliverySummaryDto(
    val id: String,
    val status: String,
    val driverFee: Double = 0.0,
    val distanceKm: Double? = null,
    val scheduledPickupDate: String? = null,
    val pickupCode: String? = null,
    val pickupQrToken: String? = null,
    val deliveryCode: String? = null,
    val deliveryQrToken: String? = null,
    val pickedUpWeightKg: Double? = null,
    val pickedUpCrateCount: Int? = null,
    val assignedAt: String? = null,
    val pickedUpAt: String? = null,
    val deliveredAt: String? = null,
)

@Serializable
data class TimestampsDto(
    val createdAt: String? = null,
    val acceptedAt: String? = null,
    val paidAt: String? = null,
    val readyAt: String? = null,
    val pickedUpAt: String? = null,
    val deliveredAt: String? = null,
    val completedAt: String? = null,
    val cancelledAt: String? = null,
)

@Serializable
data class OrderDto(
    val id: String,
    val orderNumber: String,
    val status: String,
    val statusReason: String? = null,
    val buyer: PartyViewDto,
    val farmer: PartyViewDto,
    val driver: PartyViewDto? = null,
    val items: List<OrderItemDto> = emptyList(),
    val amounts: AmountsDto = AmountsDto(),
    val totalWeightKg: Double = 0.0,
    val deliveryAddress: AddressDto? = null,
    val deliveryContactName: String? = null,
    val deliveryContactPhone: String? = null,
    val pickupAddress: AddressDto? = null,
    val requestedDeliveryDate: String? = null,
    val buyerNotes: String? = null,
    val deadlines: DeadlinesDto = DeadlinesDto(),
    val timestamps: TimestampsDto = TimestampsDto(),
    val allowedActions: List<String> = emptyList(),
    val payment: PaymentSummaryDto? = null,
    val delivery: DeliverySummaryDto? = null,
)

@Serializable
data class ReasonRequest(val reason: String)

@Serializable
data class TimelineEntryDto(
    val from: String? = null,
    val to: String,
    val at: String,
    val actorRole: String = "SYSTEM",
    val note: String? = null,
)

// ---------------------------------------------------------------------------------------------- payments

@Serializable
data class InitiatePaymentRequest(val method: String, val payerAccount: String? = null)

@Serializable
data class PaymentDto(
    val id: String,
    val orderId: String,
    val status: String,
    val method: String = "",
    val provider: String = "",
    val transactionReference: String = "",
    val amount: Double = 0.0,
    val goodsAmount: Double = 0.0,
    val deliveryAmount: Double = 0.0,
    val platformFeeAmount: Double = 0.0,
    val currency: String = "ETB",
    val checkoutUrl: String? = null,
    val failureReason: String? = null,
    val initiatedAt: String? = null,
    val paidAt: String? = null,
    val expiresAt: String? = null,
    val refundedAmount: Double = 0.0,
)

// ---------------------------------------------------------------------------------------------- delivery

@Serializable
data class LoadLineDto(
    val name: String = "",
    val quantity: Double = 0.0,
    val unit: String = "KG",
    val weightKg: Double = 0.0,
)

@Serializable
data class StopDto(
    val address: AddressDto? = null,
    val contactName: String? = null,
    val contactPhone: String? = null,
)

@Serializable
data class JobDto(
    val id: String,
    val orderNumber: String = "",
    val status: String = "OPEN",
    val pickup: AddressDto? = null,
    val dropoff: AddressDto? = null,
    val distanceKm: Double? = null,
    val totalWeightKg: Double = 0.0,
    val driverFee: Double = 0.0,
    val scheduledPickupDate: String? = null,
    val load: List<LoadLineDto> = emptyList(),
    val createdAt: String? = null,
)

@Serializable
data class DeliveryDto(
    val id: String,
    val orderId: String,
    val orderNumber: String = "",
    val orderStatus: String = "",
    val status: String,
    val pickup: StopDto? = null,
    val dropoff: StopDto? = null,
    val distanceKm: Double? = null,
    val totalWeightKg: Double = 0.0,
    val driverFee: Double = 0.0,
    val scheduledPickupDate: String? = null,
    val load: List<LoadLineDto> = emptyList(),
    val driver: PartyViewDto? = null,
    val pickupCode: String? = null,
    val pickupQrToken: String? = null,
    val deliveryCode: String? = null,
    val deliveryQrToken: String? = null,
    val pickedUpWeightKg: Double? = null,
    val pickedUpCrateCount: Int? = null,
    val pickupNote: String? = null,
    val deliveryNote: String? = null,
    val assignedAt: String? = null,
    val pickedUpAt: String? = null,
    val deliveredAt: String? = null,
    val allowedActions: List<String> = emptyList(),
)

@Serializable
data class PickupRequest(
    val code: String,
    val weighedKg: Double,
    val crateCount: Int? = null,
    val note: String? = null,
    val evidenceFileIds: List<String>? = null,
)

@Serializable
data class DeliverRequest(val code: String, val note: String? = null, val evidenceFileIds: List<String>? = null)

@Serializable
data class LocationPingRequest(val latitude: Double, val longitude: Double, val note: String? = null)

@Serializable
data class DeliveryEventDto(
    val type: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val note: String? = null,
    val at: String,
)

// ---------------------------------------------------------------------------------------------- wallet

@Serializable
data class WalletDto(
    val balance: Double = 0.0,
    val heldForRelease: Double = 0.0,
    val currency: String = "ETB",
)

@Serializable
data class WalletTransactionDto(
    val id: String,
    val type: String,
    val direction: String,
    val amount: Double,
    val balanceAfter: Double = 0.0,
    val orderId: String? = null,
    val description: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class WithdrawRequest(
    val amount: Double,
    val method: String? = null,
    val accountNumber: String? = null,
    val accountName: String? = null,
)

@Serializable
data class PayoutDto(
    val id: String,
    val amount: Double,
    val method: String = "",
    val destinationAccount: String = "",
    val status: String,
    val failureReason: String? = null,
    val createdAt: String? = null,
    val processedAt: String? = null,
)

// ---------------------------------------------------------------------------------------------- ratings & disputes

@Serializable
data class CreateRatingRequest(val target: String, val score: Int, val comment: String? = null)

@Serializable
data class RatingDto(
    val id: String,
    val orderId: String,
    val target: String,
    val score: Int,
    val comment: String? = null,
    val raterName: String = "",
    val createdAt: String? = null,
)

@Serializable
data class OpenDisputeRequest(
    val type: String,
    val description: String,
    val receivedQuantityKg: Double? = null,
    val evidenceFileIds: List<String>? = null,
)

@Serializable
data class AddEvidenceRequest(val kind: String, val fileId: String? = null, val note: String? = null)

@Serializable
data class PersonRefDto(val id: String, val fullName: String = "", val role: String = "")

@Serializable
data class EvidenceDto(
    val id: String,
    val kind: String,
    val note: String? = null,
    val fileUrl: String? = null,
    val submittedBy: PersonRefDto? = null,
    val createdAt: String? = null,
)

@Serializable
data class ResolutionDto(
    val type: String,
    val farmerAmount: Double = 0.0,
    val driverAmount: Double = 0.0,
    val buyerRefundAmount: Double = 0.0,
    val platformRetainedAmount: Double = 0.0,
    val notes: String? = null,
    val resolvedAt: String? = null,
)

@Serializable
data class DisputeDto(
    val id: String,
    val disputeNumber: String,
    val orderId: String,
    val orderNumber: String = "",
    val type: String,
    val status: String,
    val description: String? = null,
    val claimedReceivedQuantityKg: Double? = null,
    val orderedWeightKg: Double = 0.0,
    val totalHeldAmount: Double = 0.0,
    val raisedBy: PersonRefDto? = null,
    val against: PersonRefDto? = null,
    val dueAt: String? = null,
    val overdue: Boolean = false,
    val resolution: ResolutionDto? = null,
    val evidence: List<EvidenceDto> = emptyList(),
    val createdAt: String? = null,
)

// ---------------------------------------------------------------------------------------------- notifications

@Serializable
data class NotificationDto(
    val id: String,
    val type: String,
    val title: String,
    val body: String,
    val referenceType: String? = null,
    val referenceId: String? = null,
    val read: Boolean = false,
    val createdAt: String? = null,
)

@Serializable
data class RegisterDeviceRequest(val token: String, val platform: String = "ANDROID")

@Serializable
data class UnreadCountDto(val unread: Int = 0)

@Serializable
data class PaymentMethodInfoDto(val method: String, val label: String)
