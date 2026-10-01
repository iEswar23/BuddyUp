package io.github.ieswar23.buddyup.data.remote

import android.content.res.AssetManager
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import java.io.IOException
import kotlin.random.Random

/**
 * Serves canned JSON from `assets/api` so the app exercises a real Retrofit/OkHttp stack while
 * running fully offline. Adds a small random latency to mimic a mobile network.
 */
class MockInterceptor(
    private val assets: AssetManager,
    private val minLatencyMs: Long = 300,
    private val maxLatencyMs: Long = 700,
    private val random: Random = Random.Default,
) : Interceptor {

    private val routes = mapOf(
        "/v1/people/nearby" to "api/people.json",
        "/v1/waves/incoming" to "api/requests.json",
        "/v1/friends" to "api/friends.json",
        "/v1/meetups" to "api/meetups.json",
    )

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val path = request.url.encodedPath
        simulateLatency()

        val asset = routes[path]
        val (code, body) = if (asset == null || request.method != "GET") {
            404 to """{"error":"Not found","path":"$path"}"""
        } else {
            try {
                200 to assets.open(asset).bufferedReader().use { it.readText() }
            } catch (e: IOException) {
                500 to """{"error":"Unable to read $asset"}"""
            }
        }

        return Response.Builder()
            .request(request)
            .protocol(Protocol.HTTP_1_1)
            .code(code)
            .message(if (code == 200) "OK" else "Error")
            .body(body.toResponseBody(JSON))
            .addHeader("Content-Type", "application/json")
            .build()
    }

    private fun simulateLatency() {
        val delay = if (maxLatencyMs > minLatencyMs) random.nextLong(minLatencyMs, maxLatencyMs) else minLatencyMs
        try {
            Thread.sleep(delay)
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("Request cancelled", e)
        }
    }

    private companion object {
        val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
