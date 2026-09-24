# Pulse Watchlist

An Android Jetpack Compose watchlist that subscribes to Binance's public, keyless WebSocket feed and renders ten continuously updating crypto/USDT instruments.

## Run

1. Open this folder in Android Studio (JDK 17).
2. Let Gradle sync, then run the `app` configuration on an emulator or Android 7.0+ device.
3. The app needs internet access. Initial seed prices are shown while the WebSocket connects; the badge changes to `LIVE` after the first update.

## Architecture

The code follows a compact MVVM/Clean split:

- `data/`: `BinanceWatchlistRepository` owns the WebSocket lifecycle and reconnect policy.
- `domain/`: feed contract and small immutable quote models.
- `presentation/`: a lifecycle-aware `WatchlistViewModel` exposes one `StateFlow`, and Compose renders it.

Raw socket callbacks are bridged with `callbackFlow`. The ViewModel samples that stream every 500 ms before reducing it into UI state. This gives the renderer a bounded update cadence during bursts while always using the latest received tick in a window. Quote models are immutable and rows use stable keys/content types; unchanged row arguments can be skipped by Compose. Each row owns its flash `Animatable`, so a tick only starts work for that row instead of animating the full list.

## Trade-offs

- Binance provides crypto quotes rather than equities because it is public and does not require embedding an API key. Symbols and stream URL are isolated in the repository for swapping to Finnhub or a broker feed.
- The change percentage is measured from the session's first observed/seeded price, not Binance's official 24-hour change. This keeps the UI model independent of a particular provider payload; production code would include a server-supplied reference price.
- Sampling deliberately favors smoothness and battery use over displaying every network event. Lower `UI_UPDATE_PERIOD_MS` for a more aggressive display cadence.
