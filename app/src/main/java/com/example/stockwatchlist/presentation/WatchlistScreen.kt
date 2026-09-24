package com.example.stockwatchlist.presentation

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.stockwatchlist.domain.PriceDirection
import java.text.NumberFormat
import java.util.Locale

private val Background = Color(0xFF0B0F14)
private val RowSurface = Color(0xFF131A22)
private val Muted = Color(0xFF91A0B3)
private val Positive = Color(0xFF35D07F)
private val Negative = Color(0xFFFF6370)

@Composable
fun WatchlistRoute(viewModel: WatchlistViewModel) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WatchlistScreen(state)
}

@Composable
private fun WatchlistScreen(state: WatchlistUiState) {
    Surface(color = Background, modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.safeDrawingPadding().padding(top = 18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Watchlist", color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("Crypto markets · USDT", color = Muted, fontSize = 14.sp)
                }
                LivePill(state.connectionLabel)
            }
            Spacer(Modifier.height(18.dp))
            Header()
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(
                    items = state.orderedQuotes,
                    key = { it.symbol },
                    contentType = { "quote" },
                ) { quote -> QuoteRow(quote) }
            }
        }
    }
}

@Composable
private fun LivePill(label: String) {
    val connected = label == "LIVE"
    Row(
        modifier = Modifier.clip(RoundedCornerShape(50)).background(if (connected) Positive.copy(.14f) else Color.White.copy(.08f)).padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(if (connected) Positive else Muted))
        Spacer(Modifier.width(6.dp))
        Text(label, color = if (connected) Positive else Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun Header() {
    Row(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 6.dp)) {
        Text("ASSET", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
        Text("LAST PRICE", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(112.dp))
        Text("CHANGE", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.End, modifier = Modifier.width(84.dp))
    }
}

@Composable
private fun QuoteRow(quote: QuoteState) {
    // Animation state lives inside the keyed row: an update invalidates only this row, rather
    // than causing a list-wide animation or allocation.
    val flash = remember { Animatable(Color.Transparent) }
    val currentDirection by rememberUpdatedState(quote.direction)
    LaunchedEffect(quote.sequence) {
        if (quote.sequence == 0L) return@LaunchedEffect
        val color = if (currentDirection == PriceDirection.UP) Positive else Negative
        flash.snapTo(color.copy(alpha = .24f))
        flash.animateTo(Color.Transparent, animationSpec = tween(520, easing = FastOutSlowInEasing))
    }
    val trendColor = when (quote.direction) {
        PriceDirection.UP -> Positive
        PriceDirection.DOWN -> Negative
        PriceDirection.UNCHANGED -> Muted
    }
    Row(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(RowSurface).background(flash.value).padding(horizontal = 12.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AssetMark(quote.symbol)
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(quote.symbol.removeSuffix("USDT"), color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 16.sp)
            Text(quote.name, color = Muted, fontSize = 12.sp)
        }
        Text(formatPrice(quote.price), color = Color.White, fontFamily = FontFamily.Monospace, fontSize = 14.sp, textAlign = TextAlign.End, modifier = Modifier.width(112.dp))
        Text(
            text = "${if (quote.changePercent >= 0) "+" else ""}${String.format(Locale.US, "%.2f", quote.changePercent)}%",
            color = if (quote.changePercent == 0.0) Muted else trendColor,
            fontFamily = FontFamily.Monospace,
            fontSize = 13.sp,
            textAlign = TextAlign.End,
            modifier = Modifier.width(84.dp),
        )
    }
}

@Composable
private fun AssetMark(symbol: String) {
    val color = remember(symbol) { Color(0xFF1C2B3B) }
    Box(Modifier.size(38.dp).clip(CircleShape).background(color), contentAlignment = Alignment.Center) {
        Text(symbol.take(1), color = Color.White, fontWeight = FontWeight.Bold)
    }
}

private fun formatPrice(price: Double): String {
    val decimals = when {
        price >= 1_000 -> 2
        price >= 1 -> 3
        else -> 4
    }
    return "$" + NumberFormat.getNumberInstance(Locale.US).apply {
        minimumFractionDigits = decimals
        maximumFractionDigits = decimals
    }.format(price)
}
