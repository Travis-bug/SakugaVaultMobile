# SakugaVault Mobile — Functional Prototype

COMP3074 Mobile Application Development I · Team project · Fall 2026

SakugaVault Mobile is a native Android app (Java) for **SakugaVault**, an anime discovery and
streaming platform with a React website and an ASP.NET Core back end
([Travis-bug/SakugaVault](https://github.com/Travis-bug/SakugaVault)).
The website and back end are unchanged; this app is a second front end on the same REST API, so
accounts, watch history and comments are shared between web and mobile.

> **This milestone is the functional prototype.** The app talks to the live SakugaVault API:
> real accounts, real catalog, real search, and watch progress saved to your account. Video
> playback uses a real Media3/ExoPlayer player; see *Video playback* below for why it plays an
> openly licensed demo video rather than an episode.

## Screens

| Screen | Class | What it does |
|---|---|---|
| Login / Register | `LoginActivity` | Signs in or registers against `POST /api/auth/login` and `/register`. The 60-minute access token is renewed automatically, so a saved session skips the login screen. |
| Home | `HomeFragment` | Hero carousel, Continue Watching and genre rows from `GET /api/catalog/home`. Pull to refresh. |
| Search | `SearchFragment` | `GET /api/catalog/search` with a 300 ms debounce, genre filter chips, 2-column grid (4 on tablets). |
| Title & Player | `WatchActivity` | Details, episode list, SUB/DUB, comments and the watchlist toggle. Media3 player with play/pause, seek, subtitles and full screen. Progress is saved every 15 seconds and when you leave. |
| Profile | `ProfileFragment` | Account details, viewing stats and recent history from `GET /api/profile/me`, plus sign out. |
| My Watchlist | `WatchListActivity` | Shows saved on this phone in a Room database, newest first, with Remove. |

Home, Search and Profile are tabs in `MainActivity`. Tapping a title opens `WatchActivity`.

## Running it

The app talks to a SakugaVault back end running on your computer, so start that first.

1. **Start the back end** in the SakugaVault repo:
   ```bash
   docker compose up -d --no-build
   ```
2. **Check it is listening.** `curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8080/api/catalog/home`
   should print `401` (healthy: the endpoint needs a signed-in user). `000` means the API is down;
   `403` with an HTML page means something else has taken port 8080.
3. **Run the app** from Android Studio on an emulator (tested on a phone image, API 37 / Android 17).
   The emulator reaches your computer at `10.0.2.2`, which is what `API_BASE_URL` points to.
4. **Tap Allow** on the "find and connect to devices on your local network" prompt. Android 17 blocks
   local network addresses without it, and the app would simply time out. Debug builds request
   `ACCESS_LOCAL_NETWORK` for this reason; a release build against a public server does not need it.
5. **Sign in** with your SakugaVault website account, or create one in the app. A local test account
   lives in `test-account.local.properties`, which is gitignored and not published.

Build settings: AGP 9.4.1, Gradle 9.6.0, compile/target SDK 37, min SDK 26, Java 11.
Libraries: Retrofit 2.11 + Gson, OkHttp 4.12, Room 2.8.5, Media3 1.11.1, Glide 4.16, AndroidX Lifecycle.

## Video playback

The player is a real AndroidX Media3 (ExoPlayer) player: HLS and MP4, play/pause, seeking,
a subtitle track menu, full screen on the player's own button, a buffering spinner, and an error
panel with Retry. Watch progress comes from the real playback position.

It plays an **openly licensed demo video** by default, because the back end's scrapers currently
return no stream for any title (see *Known limitations*). A **Demo videos** switch on the title page
turns that off and asks the back end for the live stream instead; if none is found, the demo plays
again and the app shows the reason the back end gave. The design proposal allows for this: it says
playback would be "demonstrated with an openly licensed test stream".

Demo videos used, both from the Blender Foundation's open movies and licensed
[CC BY 3.0](https://creativecommons.org/licenses/by/3.0/):

- *Big Buck Bunny* © 2008 Blender Foundation — odd-numbered episodes
- *Tears of Steel* © 2012 Blender Foundation — even-numbered episodes

No anime episodes are downloaded, uploaded or redistributed by this project.

## Tests

```bash
./gradlew :app:testDebugUnitTest        # 24 unit tests, no emulator needed
./gradlew :app:connectedDebugAndroidTest # 16 integration tests (needs an emulator + the back end)
./gradlew :app:lintDebug                 # passes with 0 errors
```

The integration tests register one throwaway account per run and sign out at the end, so the app on
the emulator will ask you to sign in again afterwards. `docs/TESTING.md` lists every test with its
expected and actual result.

## Project structure

```
app/src/main/java/ca/gbc/comp3074/sakugavault/
  LoginActivity, MainActivity, WatchActivity, WatchListActivity   screens
  HomeFragment, SearchFragment, ProfileFragment                   bottom-navigation tabs
  api/        Retrofit client, auth interceptor, token refresh, cookie jar, DTOs
  data/       SessionStore, Anime, PlaybackPrefs
  data/local/ Room database: watchlist tables "Title" and "Metadata"
  player/     Media3 player factory and the demo video list
  services/   one interface + implementation per area, mirroring the back end
  ui/         list adapters and shared UI helpers
```

The `services/` layer mirrors the back end's own structure (`IAuthService`/`AuthService`,
`ICatalogService`/`CatalogService`, and so on). Screens depend on the interfaces, and `AppServices`
hands out the implementations, so one can be swapped without touching a screen.

## Known limitations

- **The back end's scrapers return no streams.** The source sites they read have changed, so
  `resolve-playback` reports that no scraper found a playable stream. The same outage means episode
  lists can show "Episode list is temporarily unavailable", and titles with no description from the
  provider show the back end's placeholder text. This is back-end maintenance, not an app problem.
- **Offline mode is not implemented yet** (FR-10 in the proposal). Room currently stores only the
  watchlist, and there is no background retry of unsent progress.
- **Tablet layouts are partial**: the search grid widens to 4 columns, other screens do not adapt.

## Team

- Eseosa Travis Eweka
- Zuheb Mohamud

## Acknowledgements

Cover art and backdrops are loaded at runtime from the SakugaVault API; no artwork is stored in this
repository. Icons are Google's Material Icons (Apache 2.0). Demo videos are Blender Foundation open
movies under CC BY 3.0, credited above.
Parts of this project were written with help from Claude (Anthropic), as acknowledged in our design
proposal.
