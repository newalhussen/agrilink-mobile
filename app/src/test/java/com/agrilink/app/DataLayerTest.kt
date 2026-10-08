package com.agrilink.app

import com.agrilink.app.core.ApiResult
import com.agrilink.app.core.AppError
import com.agrilink.app.core.apiCall
import com.agrilink.app.data.api.ApiFactory
import com.agrilink.app.data.api.AppJson
import com.agrilink.app.data.api.dto.AuthResponse
import com.agrilink.app.data.api.dto.OrderDto
import com.agrilink.app.data.api.dto.OrderListItemDto
import com.agrilink.app.data.api.dto.PageResponse
import com.agrilink.app.data.api.dto.UserDto
import com.agrilink.app.data.prefs.InMemoryKeyValueStore
import com.agrilink.app.data.prefs.SessionStore
import com.agrilink.app.data.repo.Outbox
import com.agrilink.app.data.repo.ProfileRepository
import com.agrilink.app.data.repo.ReplayOutcome
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OutboxRulesTest {
    @Test fun `network failures and server errors are retried`() {
        assertEquals(ReplayOutcome.RETRY, Outbox.outcomeFor(null))
        assertEquals(ReplayOutcome.RETRY, Outbox.outcomeFor(503))
        assertEquals(ReplayOutcome.RETRY, Outbox.outcomeFor(401))
        assertEquals(ReplayOutcome.RETRY, Outbox.outcomeFor(429))
    }

    @Test fun `success completes the action`() {
        assertEquals(ReplayOutcome.DONE, Outbox.outcomeFor(200))
        assertEquals(ReplayOutcome.DONE, Outbox.outcomeFor(204))
    }

    @Test fun `conflicts and validation errors are dropped because replaying cannot help`() {
        assertEquals(ReplayOutcome.DROP, Outbox.outcomeFor(409))
        assertEquals(ReplayOutcome.DROP, Outbox.outcomeFor(404))
        assertEquals(ReplayOutcome.DROP, Outbox.outcomeFor(400))
        assertEquals(ReplayOutcome.DROP, Outbox.outcomeFor(403))
    }
}

class PhotoSamplingTest {
    @Test fun `small photos are not downsampled`() {
        assertEquals(1, ProfileRepository.sampleSize(1200, 800, 1600))
    }

    @Test fun `big camera photos are halved until they fit`() {
        assertEquals(2, ProfileRepository.sampleSize(4000, 3000, 1600))
        assertEquals(4, ProfileRepository.sampleSize(8000, 6000, 1600))
    }
}

private fun user(role: String = "FARMER") = UserDto(id = "u1", phone = "+251911000001", fullName = "Tolosa Bekele", role = role)
private fun auth(access: String, refresh: String) = AuthResponse(access, refresh, user = user())

class SessionStoreTest {
    @Test fun `saves and restores the session`() {
        val store = InMemoryKeyValueStore()
        val first = SessionStore(store)
        assertNull(first.user)
        first.save(auth("a1", "r1"))
        val second = SessionStore(store)
        assertEquals("a1", second.accessToken)
        assertEquals("r1", second.refreshToken)
        assertEquals("Tolosa Bekele", second.user?.fullName)
    }

    @Test fun `clear signs out`() {
        val s = SessionStore(InMemoryKeyValueStore())
        s.save(auth("a", "r"))
        s.clear()
        assertNull(s.session.value)
        s.save(auth("a", "r"))
        s.clear(expired = true)
        assertNull(s.accessToken)
    }

    @Test fun `profile changes keep the tokens`() {
        val s = SessionStore(InMemoryKeyValueStore())
        s.save(auth("a", "r"))
        s.updateUser(user().copy(fullName = "Tolosa B."))
        assertEquals("Tolosa B.", s.user?.fullName)
        assertEquals("a", s.accessToken)
    }
}

class DtoParsingTest {
    @Test fun `parses an order with fields the app does not know`() {
        val json = """
        {"id":"o1","orderNumber":"AL-1001","status":"PAID","futureField":42,
         "buyer":{"id":"b","fullName":"Selam","phone":"+251922000001"},
         "farmer":{"id":"f","fullName":"Tolosa Bekele","verified":true,"ratingAverage":4.8,"ratingCount":12},
         "items":[{"id":"i1","productName":"Maize","unit":"QUINTAL","quantity":4,"unitPrice":4600,"lineTotal":18400,"weightKg":400}],
         "amounts":{"subtotal":18400,"deliveryFee":2760,"platformFee":808,"total":21968,"currency":"ETB"},
         "allowedActions":["MARK_READY"],
         "delivery":{"id":"d1","status":"ASSIGNED","pickupCode":"482913","driverFee":2400}}
        """.trimIndent()
        val order = AppJson.decodeFromString<OrderDto>(json)
        assertEquals("AL-1001", order.orderNumber)
        assertEquals(21968.0, order.amounts.total, 0.0)
        assertEquals("482913", order.delivery?.pickupCode)
        assertEquals(listOf("MARK_READY"), order.allowedActions)
        assertTrue(order.farmer.verified)
        assertNull(order.driver)
    }

    @Test fun `parses a page and tolerates nulls for defaulted fields`() {
        val json = """{"items":[{"id":"o","orderNumber":"AL-1","status":"PENDING","driverName":null,"deadlines":null}],"page":0,"size":20,"totalItems":1,"totalPages":1,"hasNext":false}"""
        val page = AppJson.decodeFromString<PageResponse<OrderListItemDto>>(json)
        assertEquals(1, page.items.size)
        assertNull(page.items[0].deadlines.farmerResponseDeadline)
    }
}

class ApiStackTest {
    private lateinit var server: MockWebServer
    private lateinit var session: SessionStore
    private lateinit var factory: ApiFactory

    @Before fun setUp() {
        server = MockWebServer().also { it.start() }
        session = SessionStore(InMemoryKeyValueStore()).also { it.save(auth("old-access", "old-refresh")) }
        factory = ApiFactory(server.url("/api/v1/").toString(), session, languageTag = { "am" }, debug = false)
    }

    @After fun tearDown() = runCatching { server.shutdown() }.let { }

    private fun json(body: String, code: Int = 200) = MockResponse().setResponseCode(code).setHeader("Content-Type", "application/json").setBody(body)

    @Test fun `sends the bearer token and the language`() = runBlocking {
        server.enqueue(json("""{"balance":10.5,"heldForRelease":2,"currency":"ETB"}"""))
        val wallet = factory.api.wallet()
        assertEquals(10.5, wallet.balance, 0.0)
        val request = server.takeRequest()
        assertEquals("Bearer old-access", request.getHeader("Authorization"))
        assertEquals("am", request.getHeader("Accept-Language"))
        assertEquals("/api/v1/wallet", request.path)
    }

    @Test fun `an expired token is refreshed once and the request is repeated`() = runBlocking {
        server.enqueue(json("""{"status":401,"code":"TOKEN_INVALID","message":"expired"}""", 401))
        server.enqueue(json("""{"accessToken":"new-access","refreshToken":"new-refresh","user":{"id":"u1","phone":"+251911000001","fullName":"Tolosa Bekele","role":"FARMER"}}"""))
        server.enqueue(json("""{"balance":1,"heldForRelease":0}"""))
        val wallet = factory.api.wallet()
        assertEquals(1.0, wallet.balance, 0.0)
        assertEquals("new-access", session.accessToken)
        server.takeRequest()
        val refresh = server.takeRequest()
        assertEquals("/api/v1/auth/refresh", refresh.path)
        assertTrue(refresh.body.readUtf8().contains("old-refresh"))
        assertEquals("Bearer new-access", server.takeRequest().getHeader("Authorization"))
    }

    @Test fun `a rejected refresh signs the user out`() = runBlocking {
        server.enqueue(json("""{"status":401,"code":"TOKEN_INVALID","message":"expired"}""", 401))
        server.enqueue(json("""{"status":401,"code":"TOKEN_REUSED","message":"no"}""", 401))
        val result = apiCall { factory.api.wallet() }
        assertTrue(result is ApiResult.Err)
        assertNull(session.session.value)
    }

    @Test fun `server errors become structured app errors`() = runBlocking {
        server.enqueue(json("""{"status":409,"code":"INVALID_STATE_TRANSITION","message":"Order already answered","fieldErrors":[{"field":"reason","message":"required"}]}""", 409))
        val result = apiCall { factory.api.wallet() }
        val error = (result as ApiResult.Err).error
        assertEquals("INVALID_STATE_TRANSITION", error.code)
        assertEquals("Order already answered", error.message)
        assertEquals(409, error.httpStatus)
        assertEquals("reason", error.fieldErrors.single().field)
    }

    @Test fun `no connection is reported as a network error`() = runBlocking {
        server.shutdown()
        val result = apiCall { factory.api.wallet() }
        val error = (result as ApiResult.Err).error
        assertTrue(error.isNetwork)
        assertNotNull(error.message)
        assertEquals(AppError.NETWORK, error.code)
    }
}
