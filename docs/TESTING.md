# SakugaVault Mobile: test report

**Run on:** 2026-11-11, 11:38 EST
**Code under test:** branch `functional-prototype` (functional prototype + real video player)
**Where:** unit tests on the development Mac (JVM); integration tests on the Android Emulator
`Medium_Phone_API_37.0` (Android 17) against the SakugaVault back end running in Docker
on the same computer (`http://10.0.2.2:8080/`).

**Result: 40 tests, 40 passed, 0 failed, 0 skipped.**

| Suite | Kind | Tests | Passed | Failed |
|---|---|---|---|---|
| `ApiErrorTest` | unit | 8 | 8 | 0 |
| `EpisodeAdapterTest` | unit | 5 | 5 | 0 |
| `UiTimeAgoTest` | unit | 4 | 4 | 0 |
| `ApiClientTest` | unit | 3 | 3 | 0 |
| `DtoJsonTest` | unit | 4 | 4 | 0 |
| `WatchListDaoTest` | integration (Room on the emulator) | 5 | 5 | 0 |
| `BackEndIntegrationTest` | integration (emulator ↔ live back end) | 11 | 11 | 0 |

No app code was changed to make a test pass. The tests were written against the code as it is;
the only non-test change is one new test dependency (`androidx.test:rules`, for granting the
Android 17 local-network permission inside the test).

---

## How to run them

```bash
# Unit tests (no emulator needed)
./gradlew :app:testDebugUnitTest

# Integration tests: start the back end and an emulator first
cd "/Users/traviseweka/Documents/Personal Project/SakugaVault" && docker compose up -d --no-build
./gradlew :app:connectedDebugAndroidTest
```

Reports land in `app/build/reports/tests/testDebugUnitTest/index.html` and
`app/build/reports/androidTests/connected/debug/index.html`.

Notes for the integration tests:

- `BackEndIntegrationTest` registers **one throwaway account per run** (`svtest_<stamp>`).
  The back end allows 5 registrations per 10 minutes, so do not run it in a loop.
- It signs out at the end, so the app on the emulator asks you to sign in again afterwards.
- `WatchListDaoTest` uses an in-memory database; it never touches the app's real `sakugavault.db`.

---

## Unit tests (`app/src/test`)

### `api/ApiErrorTest` — turning failed API answers into readable messages

| Test | Expected | Actual |
|---|---|---|
| `validationMessageWinsOverDetailAndTitle` | A ProblemDetails body with `errors`, `detail` and `title` gives the first validation message: "Password must be at least 8 characters long." | ✅ as expected |
| `detailIsUsedWhenThereAreNoValidationErrors` | 401 body with `detail` "Invalid username/email or password." gives that detail | ✅ as expected |
| `titleIsUsedWhenDetailIsMissing` | 404 body with only `title` "Anime not found." gives the title | ✅ as expected |
| `technicalDetailFallsBackToStatusMessage` | A `detail` containing "System.NullReferenceException…" is hidden; 500 gives "The server hit a problem. Please try again in a moment." | ✅ as expected |
| `htmlBodyFallsBackToStatusMessage` | An HTML page with status 403 (what Jenkins answers when it sits on port 8080) gives "You do not have permission to do that." | ✅ as expected |
| `emptyBodyUsesStatusMessages` | No body: 400, 401, 404, 409, 429, 503 and 418 each give their fixed message ("Request failed with status 418" for unknown codes) | ✅ as expected |
| `networkFailureHasStatusZero` | `fromFailure(IOException)` → status 0, `isNetworkError()` true, message starts with "Can't reach the SakugaVault server" | ✅ as expected |
| `unauthorizedFlag` | `isUnauthorized()` is true for 401 only | ✅ as expected |

### `ui/EpisodeAdapterTest` — episode labels in the episode list

| Test | Expected | Actual |
|---|---|---|
| `missingLabelShowsJustTheNumber` | label `null`, `""` or `"Episode 1"` → "Episode 1" | ✅ as expected |
| `repeatedEpisodeNumberIsDroppedOnce` | "Episode 1 · Episode 1" → "Episode 1" | ✅ as expected |
| `realTitleIsKept` | "Episode 3 · The Journey's End" and "The Journey's End" → "Episode 3 · The Journey's End" | ✅ as expected |
| `similarNumbersAreNotStripped` | episode 1 with label "Episode 12" → "Episode 1 · Episode 12" (12 is not 1) | ✅ as expected |
| `prefixMatchingIgnoresCaseAndSeparators` | "EPISODE 7: Reunion" and "episode7 - Reunion" → "Episode 7 · Reunion" | ✅ as expected |

### `ui/UiTimeAgoTest` — "5m / 3h / 2d" stamps on comments and history

| Test | Expected | Actual |
|---|---|---|
| `emptyOrBadInputGivesEmptyString` | `null`, `""`, "not a date" → "" | ✅ as expected |
| `underAMinuteIsNow` | now → "now"; a date 5 min in the future → "now" (never negative) | ✅ as expected |
| `minutesHoursDays` | 5 min → "5m", 59 → "59m", 60 → "1h", 23 h 30 → "23h", 24 h → "1d", 29 days → "29d" | ✅ as expected |
| `olderThanAMonthShowsTheDate` | "2026-01-15T10:30:00Z" → "2026-01-15" | ✅ as expected |

### `api/ApiClientTest` — making the back end's relative image links absolute

| Test | Expected | Actual |
|---|---|---|
| `absoluteUrlsAreLeftAlone` | `https://…` and `http://…` links are returned unchanged | ✅ as expected |
| `relativeUrlsGetTheApiBase` | "/images/x.jpg" and "images/x.jpg" → `API_BASE_URL + "images/x.jpg"` | ✅ as expected |
| `emptyUrlsBecomeNull` | `null`, `""`, `"   "` → `null` | ✅ as expected |

### `api/DtoJsonTest` — the back end's JSON maps onto the app's DTOs

| Test | Expected | Actual |
|---|---|---|
| `authResponseParses` | `accessToken`, `accessTokenExpiresAtUtc` and the nested `user` are filled | ✅ as expected |
| `searchResponseParses` | `totalResults`, `results[0].title`, sub/dub flags and `genres` are filled | ✅ as expected |
| `watchPageIgnoresUnknownFields` | A field the app doesn't know (`somethingNew`) is skipped, not an error | ✅ as expected |
| `requestBodiesUseTheBackEndsFieldNames` | `LoginRequestDto` serialises to `{"identifier":…,"password":…}`; the progress body has `animeId`, `episodeNumber`, `positionSeconds`, `durationSeconds`, `completed` | ✅ as expected |

---

## Integration tests (`app/src/androidTest`)

### `data/local/WatchListDaoTest` — the Room watchlist (tables `Title` and `Metadata`)

Runs on a real SQLite database on the emulator, created in memory for each test.

| Test | Expected | Actual |
|---|---|---|
| `saveStoresTitleAndMetadataTogether` | After `save()`, `find()` returns the title, video link "/watch/a1", synopsis and episode count; both tables have 1 row | ✅ as expected |
| `savingTheSameShowAgainUpdatesInsteadOfDuplicating` | Saving the same (user, anime) twice leaves 1 row per table with the newer values | ✅ as expected |
| `observeAllReturnsNewestFirst` | Three saves with different `added_at` come back newest first via LiveData | ✅ as expected |
| `deleteRemovesTheMetadataRowToo` | `delete()` returns 1, the Title row is gone and its Metadata row is gone too (ON DELETE CASCADE); a second delete returns 0 | ✅ as expected |
| `eachUserHasTheirOwnList` | u1 sees 1 show, u2 sees 2; `observeIds` lists u2's ids; deleting for u2 leaves u1's row alone | ✅ as expected |

### `api/BackEndIntegrationTest` — emulator ↔ the live back end, through the app's service layer

These tests call the same `AppServices` the screens use, so they exercise Retrofit, Gson, the
auth interceptor, token refresh, the cookie jar, the Android 17 local-network permission and Room,
against the real API. They run in name order; a throwaway account is registered first.

| Test | Expected | Actual |
|---|---|---|
| (setup) register `svtest_<stamp>` via `POST /api/auth/register` | 200 with a user id | ✅ account created |
| `t01_registerSavedTheSessionOnThePhone` | `SessionStore` is signed in with that user id, username and an access token | ✅ as expected |
| `t02_wrongPasswordIsRejectedWithAReadableMessage` | `login(user, wrong password)` fails with 400 or 401, a non-empty plain-text message, and the good session is kept | ✅ as expected (401) |
| `t03_signingInAgainWorks` | `login(user, password)` succeeds and returns the same user id | ✅ as expected |
| `t04_homeLoadsTheRealCatalog` | `GET /api/catalog/home` returns at least one featured title and a genre-rows list | ✅ as expected |
| `t05_searchFindsAFeaturedTitle` | Searching the first word of the featured title's name returns that title's id | ✅ as expected |
| `t06_watchPageHasTheTitleDetails` | `GET /api/watch/{id}` returns the same id and title, with `seasons` and `comments` lists | ✅ as expected |
| `t07_progressSavedAndFlushedShowsUpInHistory` | `saveProgressAndFlush(id, ep 1, 42 s, 1440 s)` then `getMyHistory` (which waits for the flush) lists the show at episode 1, position 42, not completed | ✅ as expected |
| `t08_profileReflectsTheNewAccount` | `GET /api/profile/me` returns the test username and `continueWatchingCount ≥ 1` | ✅ as expected |
| `t09_watchlistAddGrabsMetadataIntoRoomAndRemoveDeletesIt` | `watchList.add(id)` stores a Title row (title, "/watch/{id}") and a Metadata row for this user; `getSavedIds` includes the id; `remove(id)` deletes it from the database | ✅ as expected |
| `t10_aBadAccessTokenIsRefreshedAutomatically` | With the stored access token replaced by garbage, `GET /api/profile/me` still succeeds (401 → refresh with the `SakugaVault.Refresh` cookie → retry) and the stored token is replaced | ✅ as expected |
| `t11_logoutForgetsTheSession` | `logout()` clears the session and the cookies; `GET /api/profile/me` then fails with 401 | ✅ as expected |

---

## What is not covered yet

- **The video player with a live stream.** No scraper currently returns a stream, so the
  `resolve-playback → stream proxy → ExoPlayer` path can only be checked by hand with demo
  videos (done on the emulator on 2026-11-10, see `HANDOFF.md`), not asserted in a test.
- **Screen (Espresso) tests.** The screens are exercised manually; there are no UI tests yet.
  They are planned for the Dec 4 milestone together with tablet checks.
- **Offline mode (FR-10)** is not built yet, so there is nothing to test.
