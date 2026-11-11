package ca.gbc.comp3074.sakugavault.api;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.GrantPermissionRule;

import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;

import java.util.List;
import java.util.Locale;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import ca.gbc.comp3074.sakugavault.api.dto.AuthResponseDto;
import ca.gbc.comp3074.sakugavault.api.dto.CatalogSearchResponseDto;
import ca.gbc.comp3074.sakugavault.api.dto.CurrentUserDto;
import ca.gbc.comp3074.sakugavault.api.dto.HomeCatalogDto;
import ca.gbc.comp3074.sakugavault.api.dto.ProfileSummaryDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchHistoryEntryDto;
import ca.gbc.comp3074.sakugavault.api.dto.WatchPageDto;
import ca.gbc.comp3074.sakugavault.data.SessionStore;
import ca.gbc.comp3074.sakugavault.data.local.WatchListEntry;
import ca.gbc.comp3074.sakugavault.services.AppServices;

/**
 * Integration tests against the real SakugaVault back end (BuildConfig.API_BASE_URL, normally the
 * Docker stack on this computer at 10.0.2.2:8080). They go through the same service layer the
 * screens use, so they cover Retrofit, the auth interceptor, token refresh, cookies and Room.
 *
 * One throwaway account is registered per run (the back end allows 5 registrations per 10 minutes).
 * The tests run in name order because later ones depend on the earlier ones.
 * Note: the tests sign out at the end, so the app on the emulator will ask you to sign in again.
 */
@RunWith(AndroidJUnit4.class)
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class BackEndIntegrationTest {

    private static final long TIMEOUT_SECONDS = 30;

    /** Android 17 blocks local-network addresses unless this is granted (the app asks the user for it). */
    @ClassRule
    public static GrantPermissionRule localNetwork =
            GrantPermissionRule.grant(LocalNetworkAccess.PERMISSION);

    private static Context context;
    private static String userName;
    private static String password;
    private static String userId;
    private static String heroId;
    private static String heroTitle;

    /** Waits for an ApiCallback and hands back either the result or the error. */
    private static final class Waiter<T> implements ApiCallback<T> {
        final CountDownLatch latch = new CountDownLatch(1);
        T result;
        ApiError error;

        @Override
        public void onSuccess(T value) {
            result = value;
            latch.countDown();
        }

        @Override
        public void onError(ApiError apiError) {
            error = apiError;
            latch.countDown();
        }

        T await() throws InterruptedException {
            assertTrue("The back end did not answer within " + TIMEOUT_SECONDS + " s",
                    latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            if (error != null) {
                fail("API error " + error.status + ": " + error.message);
            }
            return result;
        }

        ApiError awaitError() throws InterruptedException {
            assertTrue("The back end did not answer within " + TIMEOUT_SECONDS + " s",
                    latch.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
            assertNotNull("Expected the call to fail, but it succeeded", error);
            return error;
        }
    }

    /** The services expect to be called on the main thread, like the screens do. */
    private static void onMain(Runnable r) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync(r);
    }

    @BeforeClass
    public static void registerThrowawayAccount() throws InterruptedException {
        context = ApplicationProvider.getApplicationContext();
        onMain(() -> AppServices.auth(context).clearLocalSession());

        String stamp = Long.toString(System.currentTimeMillis() % 100_000_000L, 36);
        userName = String.format(Locale.US, "svtest_%s", stamp);
        password = "Test-" + stamp + "-pass1";

        Waiter<CurrentUserDto> waiter = new Waiter<>();
        onMain(() -> AppServices.auth(context).register(
                "SV Test " + stamp, userName, userName + "@example.com", password, waiter));
        CurrentUserDto user = waiter.await();
        assertNotNull(user);
        userId = user.id;
    }

    @Test
    public void t01_registerSavedTheSessionOnThePhone() {
        assertTrue(SessionStore.isSignedIn(context));
        assertEquals(userId, SessionStore.userId(context));
        assertEquals(userName, SessionStore.userName(context));
        assertNotNull(SessionStore.accessToken(context));
    }

    @Test
    public void t02_wrongPasswordIsRejectedWithAReadableMessage() throws InterruptedException {
        Waiter<CurrentUserDto> waiter = new Waiter<>();
        onMain(() -> AppServices.auth(context).login(userName, "definitely-wrong-password", waiter));
        ApiError error = waiter.awaitError();
        assertTrue("expected 400 or 401, got " + error.status, error.status == 400 || error.status == 401);
        assertFalse(error.message.trim().isEmpty());
        assertFalse("message should not be raw JSON", error.message.startsWith("{"));
        // The failed attempt must not have replaced the good session.
        assertTrue(SessionStore.isSignedIn(context));
    }

    @Test
    public void t03_signingInAgainWorks() throws InterruptedException {
        Waiter<CurrentUserDto> waiter = new Waiter<>();
        onMain(() -> AppServices.auth(context).login(userName, password, waiter));
        assertEquals(userId, waiter.await().id);
    }

    @Test
    public void t04_homeLoadsTheRealCatalog() throws InterruptedException {
        Waiter<HomeCatalogDto> waiter = new Waiter<>();
        onMain(() -> AppServices.catalog(context).getHome(waiter));
        HomeCatalogDto home = waiter.await();
        assertNotNull(home.heroItems);
        assertFalse("Home has no featured titles", home.heroItems.isEmpty());
        assertNotNull(home.genreRows);
        heroId = home.heroItems.get(0).id;
        heroTitle = home.heroItems.get(0).title;
        assertNotNull(heroId);
        assertNotNull(heroTitle);
    }

    @Test
    public void t05_searchFindsAFeaturedTitle() throws InterruptedException {
        String query = heroTitle.split("[\\s:]+")[0];
        Waiter<CatalogSearchResponseDto> waiter = new Waiter<>();
        onMain(() -> AppServices.catalog(context).search(query, 20, waiter));
        CatalogSearchResponseDto result = waiter.await();
        assertNotNull(result.results);
        boolean found = false;
        for (int i = 0; i < result.results.size(); i++) {
            found |= heroId.equals(result.results.get(i).id);
        }
        assertTrue("Searching \"" + query + "\" did not return " + heroTitle, found);
    }

    @Test
    public void t06_watchPageHasTheTitleDetails() throws InterruptedException {
        Waiter<WatchPageDto> waiter = new Waiter<>();
        onMain(() -> AppServices.watchPage(context).getWatchPage(heroId, waiter));
        WatchPageDto page = waiter.await();
        assertEquals(heroId, page.id);
        assertEquals(heroTitle, page.title);
        assertNotNull(page.seasons);
        assertNotNull(page.comments);
    }

    @Test
    public void t07_progressSavedAndFlushedShowsUpInHistory() throws InterruptedException {
        Waiter<List<WatchHistoryEntryDto>> waiter = new Waiter<>();
        onMain(() -> {
            AppServices.watchHistory(context).saveProgressAndFlush(heroId, 1, 42, 1440, false);
            AppServices.watchHistory(context).getMyHistory(20, waiter); // waits for the flush
        });
        List<WatchHistoryEntryDto> history = waiter.await();
        WatchHistoryEntryDto entry = null;
        for (WatchHistoryEntryDto e : history) {
            if (heroId.equals(e.animeId)) {
                entry = e;
            }
        }
        assertNotNull("History does not contain the show just watched", entry);
        assertEquals(1, entry.episodeNumber);
        assertEquals(42, entry.positionSeconds);
        assertFalse(entry.completed);
    }

    @Test
    public void t08_profileReflectsTheNewAccount() throws InterruptedException {
        Waiter<ProfileSummaryDto> waiter = new Waiter<>();
        onMain(() -> AppServices.profile(context).getMyProfile(waiter));
        ProfileSummaryDto profile = waiter.await();
        assertEquals(userName, profile.user.userName);
        assertTrue(profile.continueWatchingCount >= 1);
    }

    @Test
    public void t09_watchlistAddGrabsMetadataIntoRoomAndRemoveDeletesIt() throws InterruptedException {
        Waiter<WatchListEntry> added = new Waiter<>();
        onMain(() -> AppServices.watchList(context).add(heroId, added));
        WatchListEntry entry = added.await();
        assertEquals(heroId, entry.title.animeId);
        assertEquals(userId, entry.title.userId);
        assertEquals(heroTitle, entry.title.title);
        assertEquals("/watch/" + heroId, entry.title.videoLink);
        assertNotNull(entry.metadata);
        assertEquals(heroId, entry.metadata.animeId);

        AtomicReference<List<String>> ids = new AtomicReference<>();
        CountDownLatch seen = new CountDownLatch(1);
        onMain(() -> AppServices.watchList(context).getSavedIds().observeForever(list -> {
            if (list != null && list.contains(heroId)) {
                ids.set(list);
                seen.countDown();
            }
        }));
        assertTrue("Saved ids never included the added show", seen.await(5, TimeUnit.SECONDS));

        CountDownLatch removed = new CountDownLatch(1);
        onMain(() -> AppServices.watchList(context).remove(heroId, removed::countDown));
        assertTrue(removed.await(5, TimeUnit.SECONDS));
        assertNull(ca.gbc.comp3074.sakugavault.data.local.SakugaVaultDatabase.get(context)
                .watchListDao().find(userId, heroId));
    }

    @Test
    public void t10_aBadAccessTokenIsRefreshedAutomatically() throws InterruptedException {
        // Pretend the 60-minute token went stale: keep the user, break the token.
        AuthResponseDto broken = new AuthResponseDto();
        broken.accessToken = "this-token-is-not-valid";
        broken.accessTokenExpiresAtUtc = "2000-01-01T00:00:00Z";
        SessionStore.saveSession(context, broken);

        Waiter<ProfileSummaryDto> waiter = new Waiter<>();
        onMain(() -> AppServices.profile(context).getMyProfile(waiter));
        ProfileSummaryDto profile = waiter.await(); // 401 -> refresh with the cookie -> retried
        assertEquals(userName, profile.user.userName);
        assertFalse("the refreshed token should replace the broken one",
                "this-token-is-not-valid".equals(SessionStore.accessToken(context)));
    }

    @Test
    public void t11_logoutForgetsTheSession() throws InterruptedException {
        CountDownLatch done = new CountDownLatch(1);
        onMain(() -> AppServices.auth(context).logout(done::countDown));
        assertTrue(done.await(TIMEOUT_SECONDS, TimeUnit.SECONDS));
        assertFalse(SessionStore.isSignedIn(context));
        assertNull(SessionStore.accessToken(context));

        // Without a session (and without the refresh cookie) protected calls are rejected.
        Waiter<ProfileSummaryDto> waiter = new Waiter<>();
        onMain(() -> AppServices.profile(context).getMyProfile(waiter));
        assertEquals(401, waiter.awaitError().status);
    }
}
