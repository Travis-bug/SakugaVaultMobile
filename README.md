# SakugaVault Mobile — UI Prototype

COMP3074 Mobile Application Development I · Team project · Fall 2026

SakugaVault Mobile is a native Android app (Java) for **SakugaVault**, an anime discovery and streaming
platform with a React website and an ASP.NET Core back end
([Travis-bug/SakugaVault](https://github.com/Travis-bug/SakugaVault)).
The website and back end stay as they are; this app is a new front end that will use the same REST API,
so accounts, watch history and comments are shared between web and mobile.

> **This milestone is the UI prototype.** All five screens and the navigation between them work,
> using **sample data**. No back end is connected yet — that is the next milestone.

## Screens

| Screen | Class | What works in the prototype |
|---|---|---|
| Login / Register | `LoginActivity` | Sign In / Create Account tabs, field checks (same rules as the API), error message, loading spinner, saved session skips login on next launch |
| Home | `HomeFragment` | Auto-advancing hero carousel with page dots, Watch Now, + Watchlist, Continue Watching row with progress, six genre rows, pull to refresh |
| Search | `SearchFragment` | Debounced search (300 ms), genre filter chips, "Trending now" when empty, results grid (2 columns on phones, 4 on tablets), empty state |
| Title Detail & Player | `WatchActivity` | Simulated player (play/pause, seek bar, time), full-screen in landscape, SUB/DUB switch, season picker and episode list, progress saved every 15 s, comments, "More like this" |
| Profile | `ProfileFragment` | Account header, viewing stats, recent history, watchlist, sign out |

Home, Search and Profile are tabs in `MainActivity` (bottom navigation). Tapping any title opens `WatchActivity`.

## Run it

1. Open this folder in Android Studio and let Gradle sync.
2. Run the `app` configuration on an emulator (tested target: API 37 phone).
3. Sign in with any username and a password of 8+ characters.

Build settings: AGP 9.4.1, Gradle 9.6.0, compile/target SDK 37, min SDK 26, Java.

## Project structure

```
app/src/main/java/ca/gbc/comp3074/sakugavault/
  LoginActivity, MainActivity, WatchActivity      screens (activities)
  HomeFragment, SearchFragment, ProfileFragment   bottom-navigation tabs
  data/   Anime, SampleData, SessionStore         sample data + saved session
  ui/     adapters for the lists and rows, Ui helpers
```

## Next milestones (from the design proposal)

- Retrofit + OkHttp client for the SakugaVault API (JWT sign-in, token refresh, cookies)
- Room database for offline caching of the catalog and watch progress
- AndroidX Media3 (ExoPlayer) for real HLS playback
- Glide for real cover art

## Team

- Eseosa Travis Eweka (team lead)
- [Member 2]
- [Member 3]
- [Member 4]

## Acknowledgements

Placeholder artwork is generated from colour gradients; no copyrighted images are included.
Icons are from Google's Material Icons (Apache 2.0).
Parts of this prototype were written with help from Claude (Anthropic), as acknowledged in our design proposal.
