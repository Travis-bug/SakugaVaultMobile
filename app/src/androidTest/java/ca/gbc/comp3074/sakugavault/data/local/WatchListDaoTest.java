package ca.gbc.comp3074.sakugavault.data.local;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import android.content.Context;
import android.database.Cursor;

import androidx.lifecycle.LiveData;
import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Integration tests for the Room watchlist (tables "Title" and "Metadata").
 * Runs against a real SQLite database on the emulator, kept in memory so nothing is left behind.
 */
@RunWith(AndroidJUnit4.class)
public class WatchListDaoTest {

    private SakugaVaultDatabase db;
    private WatchListDao dao;

    @Before
    public void openDatabase() {
        Context context = ApplicationProvider.getApplicationContext();
        db = Room.inMemoryDatabaseBuilder(context, SakugaVaultDatabase.class)
                .allowMainThreadQueries()
                .build();
        dao = db.watchListDao();
    }

    @After
    public void closeDatabase() {
        db.close();
    }

    private static WatchListTitle title(String user, String animeId, String name, long addedAt) {
        WatchListTitle t = new WatchListTitle();
        t.userId = user;
        t.animeId = animeId;
        t.title = name;
        t.posterImageUrl = "https://example.com/" + animeId + ".jpg";
        t.videoLink = "/watch/" + animeId;
        t.addedAt = addedAt;
        return t;
    }

    private static WatchListMetadata metadata(String user, String animeId, int episodes) {
        WatchListMetadata m = new WatchListMetadata();
        m.userId = user;
        m.animeId = animeId;
        m.synopsis = "Synopsis of " + animeId;
        m.episodeCount = episodes;
        m.runtimeMinutes = 24;
        m.subAvailable = true;
        m.dubAvailable = false;
        m.audioLanguages = "ja";
        m.subtitleLanguages = "en";
        m.savedAt = System.currentTimeMillis();
        return m;
    }

    /** Reads the current value of a LiveData (observers must be attached on the main thread). */
    private static <T> T value(LiveData<T> liveData) throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<T> result = new AtomicReference<>();
        InstrumentationRegistry.getInstrumentation().runOnMainSync(() -> liveData.observeForever(v -> {
            result.set(v);
            latch.countDown();
        }));
        assertTrue("LiveData never delivered a value", latch.await(5, TimeUnit.SECONDS));
        return result.get();
    }

    private int rowCount(String table) {
        try (Cursor c = db.query("SELECT COUNT(*) FROM " + table, null)) {
            c.moveToFirst();
            return c.getInt(0);
        }
    }

    @Test
    public void saveStoresTitleAndMetadataTogether() {
        dao.save(title("u1", "a1", "Frieren", 1000), metadata("u1", "a1", 28));

        WatchListEntry entry = dao.find("u1", "a1");
        assertNotNull(entry);
        assertEquals("Frieren", entry.title.title);
        assertEquals("/watch/a1", entry.title.videoLink);
        assertEquals("Synopsis of a1", entry.metadata.synopsis);
        assertEquals(28, entry.metadata.episodeCount);
        assertTrue(entry.metadata.subAvailable);
        assertEquals(1, rowCount("Title"));
        assertEquals(1, rowCount("Metadata"));
    }

    @Test
    public void savingTheSameShowAgainUpdatesInsteadOfDuplicating() {
        dao.save(title("u1", "a1", "Old name", 1000), metadata("u1", "a1", 12));
        dao.save(title("u1", "a1", "New name", 2000), metadata("u1", "a1", 24));

        assertEquals(1, rowCount("Title"));
        assertEquals(1, rowCount("Metadata"));
        WatchListEntry entry = dao.find("u1", "a1");
        assertEquals("New name", entry.title.title);
        assertEquals(24, entry.metadata.episodeCount);
    }

    @Test
    public void observeAllReturnsNewestFirst() throws InterruptedException {
        dao.save(title("u1", "old", "Old", 1000), metadata("u1", "old", 1));
        dao.save(title("u1", "new", "New", 3000), metadata("u1", "new", 1));
        dao.save(title("u1", "mid", "Mid", 2000), metadata("u1", "mid", 1));

        List<WatchListEntry> all = value(dao.observeAll("u1"));
        assertEquals(3, all.size());
        assertEquals("new", all.get(0).title.animeId);
        assertEquals("mid", all.get(1).title.animeId);
        assertEquals("old", all.get(2).title.animeId);
    }

    @Test
    public void deleteRemovesTheMetadataRowToo() {
        dao.save(title("u1", "a1", "A", 1000), metadata("u1", "a1", 1));
        dao.save(title("u1", "a2", "B", 2000), metadata("u1", "a2", 1));

        assertEquals(1, dao.delete("u1", "a1"));

        assertNull(dao.find("u1", "a1"));
        assertNotNull(dao.find("u1", "a2"));
        assertEquals(1, rowCount("Title"));
        assertEquals("ON DELETE CASCADE should remove the Metadata row", 1, rowCount("Metadata"));
        assertEquals(0, dao.delete("u1", "a1")); // already gone
    }

    @Test
    public void eachUserHasTheirOwnList() throws InterruptedException {
        dao.save(title("u1", "a1", "A", 1000), metadata("u1", "a1", 1));
        dao.save(title("u2", "a1", "A", 1000), metadata("u2", "a1", 1));
        dao.save(title("u2", "a2", "B", 2000), metadata("u2", "a2", 1));

        assertEquals(1, value(dao.observeAll("u1")).size());
        assertEquals(2, value(dao.observeAll("u2")).size());
        List<String> ids = value(dao.observeIds("u2"));
        assertTrue(ids.contains("a1") && ids.contains("a2"));

        dao.delete("u2", "a1");
        assertNotNull("deleting for u2 must not touch u1", dao.find("u1", "a1"));
    }
}
