package com.example.stockwatchlist.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.stockwatchlist.data.BinanceWatchlistRepository
import com.example.stockwatchlist.domain.PriceDirection
import com.example.stockwatchlist.domain.WatchlistRepository
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.sample
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

@OptIn(FlowPreview::class)
class WatchlistViewModel(
    private val repository: WatchlistRepository = BinanceWatchlistRepository(),
) : ViewModel() {
    private val _state = MutableStateFlow(WatchlistUiState.initial())
    val state: StateFlow<WatchlistUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            repository.priceTicks().sample(UI_UPDATE_PERIOD_MS.milliseconds)
                .collect { tick -> updateQuote(tick.symbol, tick.price) }
        }
    }

    private fun updateQuote(symbol: String, latest: Double) {
        val current = _state.value
        val old = current.quotes[symbol] ?: return
        if (old.price == latest) return
        val direction = if (latest > old.price) PriceDirection.UP else PriceDirection.DOWN
        val updated = old.copy(
            price = latest,
            changePercent = (latest - old.openPrice) / old.openPrice * 100,
            direction = direction,
            sequence = old.sequence + 1,
        )
        _state.value = current.copy(quotes = current.quotes + (symbol to updated), connectionLabel = "LIVE")
    }

    private companion object { const val UI_UPDATE_PERIOD_MS = 500L }
}

@androidx.compose.runtime.Immutable
data class WatchlistUiState(
    val quotes: Map<String, QuoteState>,
    val connectionLabel: String,
) {
    val orderedQuotes: List<QuoteState> get() = ORDER.mapNotNull(quotes::get)

    companion object {
        private val ORDER = listOf(
            "BTCUSDT", "ETHUSDT", "BNBUSDT", "SOLUSDT", "XRPUSDT",
            "ADAUSDT", "DOGEUSDT", "AVAXUSDT", "LINKUSDT", "DOTUSDT",
        )
        fun initial() = WatchlistUiState(
            quotes = linkedMapOf(
                "BTCUSDT" to QuoteState("BTCUSDT", "Bitcoin", 67_248.10),
                "ETHUSDT" to QuoteState("ETHUSDT", "Ethereum", 3_511.42),
                "BNBUSDT" to QuoteState("BNBUSDT", "BNB", 614.82),
                "SOLUSDT" to QuoteState("SOLUSDT", "Solana", 146.17),
                "XRPUSDT" to QuoteState("XRPUSDT", "XRP", 0.5291),
                "ADAUSDT" to QuoteState("ADAUSDT", "Cardano", 0.4418),
                "DOGEUSDT" to QuoteState("DOGEUSDT", "Dogecoin", 0.1262),
                "AVAXUSDT" to QuoteState("AVAXUSDT", "Avalanche", 35.67),
                "LINKUSDT" to QuoteState("LINKUSDT", "Chainlink", 13.82),
                "DOTUSDT" to QuoteState("DOTUSDT", "Polkadot", 4.54),
            ),
            connectionLabel = "CONNECTING",
        )
    }
}

@androidx.compose.runtime.Immutable
data class QuoteState(
    val symbol: String,
    val name: String,
    val price: Double,
    val openPrice: Double = price,
    val changePercent: Double = 0.0,
    val direction: PriceDirection = PriceDirection.UNCHANGED,
    val sequence: Long = 0,
)
