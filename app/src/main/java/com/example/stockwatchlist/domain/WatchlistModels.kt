package com.example.stockwatchlist.domain

enum class PriceDirection { UP, DOWN, UNCHANGED }

data class PriceTick(val symbol: String, val price: Double)

interface WatchlistRepository {
    fun priceTicks(): kotlinx.coroutines.flow.Flow<PriceTick>
}
