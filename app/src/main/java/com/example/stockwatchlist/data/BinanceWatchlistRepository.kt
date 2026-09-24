package com.example.stockwatchlist.data

import com.example.stockwatchlist.domain.PriceTick
import com.example.stockwatchlist.domain.WatchlistRepository
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.delay
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import kotlin.time.Duration.Companion.milliseconds

/** Public, keyless WebSocket feed. Reconnects after the server or network closes it. */
class BinanceWatchlistRepository(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .pingInterval(20, TimeUnit.SECONDS)
        .build(),
) : WatchlistRepository {
    override fun priceTicks(): Flow<PriceTick> = flow {
        while (currentCoroutineContext().isActive) {
            try {
                socketTicks().collect { emit(it) }
            } catch (_: Exception) {
                // A reconnect keeps the screen useful across network transitions.
            }
            delay(RECONNECT_DELAY_MS.milliseconds)
        }
    }

    private fun socketTicks(): Flow<PriceTick> = callbackFlow {
        val request = Request.Builder().url(STREAM_URL).build()
        val socket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                // Combined-stream payload: { stream: ..., data: { s: "BTCUSDT", c: "..." } }.
                val symbol = SYMBOL_REGEX.find(text)?.groupValues?.get(1) ?: return
                val price = PRICE_REGEX.find(text)?.groupValues?.get(1)?.toDoubleOrNull() ?: return
                trySend(PriceTick(symbol, price))
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                close(t)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                close()
            }
        })
        awaitClose { socket.cancel() }
    }

    private companion object {
        const val RECONNECT_DELAY_MS = 2_000L
        const val STREAM_URL = "wss://stream.binance.com:9443/stream?streams=" +
            "btcusdt@ticker/ethusdt@ticker/bnbusdt@ticker/solusdt@ticker/" +
            "xrpusdt@ticker/adausdt@ticker/dogeusdt@ticker/avaxusdt@ticker/" +
            "linkusdt@ticker/dotusdt@ticker"
        val SYMBOL_REGEX = "\\\"s\\\":\\\"([^\\\"]+)\\\"".toRegex()
        val PRICE_REGEX = "\\\"c\\\":\\\"([^\\\"]+)\\\"".toRegex()
    }
}
