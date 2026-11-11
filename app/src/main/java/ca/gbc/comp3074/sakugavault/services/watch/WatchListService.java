package ca.gbc.comp3074.sakugavault.services.watch;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.ApiError;
import ca.gbc.comp3074.sakugavault.api.dto.WatchPageDto;
import ca.gbc.comp3074.sakugavault.data.SessionStore;
import ca.gbc.comp3074.sakugavault.data.local.SakugaVaultDatabase;
import ca.gbc.comp3074.sakugavault.data.local.WatchListDao;
import ca.gbc.comp3074.sakugavault.data.local.WatchListEntry;
import ca.gbc.comp3074.sakugavault.data.local.WatchListMetadata;
import ca.gbc.comp3074.sakugavault.data.local.WatchListTitle;

public final class WatchListService implements IWatchListService {

    private final Context context;
    private final IWatchPageService watchPages;
    private final WatchListDao dao;
    /** Room does not allow database work on the main thread, so writes run here. */
    private final ExecutorService io = Executors.newSingleThreadExecutor();
    private final Handler main = new Handler(Looper.getMainLooper());

    public WatchListService(Context context, IWatchPageService watchPages) {
        this.context = context;
        this.watchPages = watchPages;
        this.dao = SakugaVaultDatabase.get(context).watchListDao();
    }

    @Override
    public void add(String animeId, ApiCallback<WatchListEntry> callback) {
        final String userId = SessionStore.userId(context);
        // 1. Grab the title and its metadata from the back end.
        watchPages.getWatchPage(animeId, new ApiCallback<WatchPageDto>() {
            @Override
            public void onSuccess(WatchPageDto page) {
                if (page == null || page.id == null) {
                    callback.onError(new ApiError(404, "That title could not be found."));
                    return;
                }
                long now = System.currentTimeMillis();
                WatchListTitle title = toTitle(userId, page, now);
                WatchListMetadata metadata = toMetadata(userId, page, now);
                // 2. Save both rows in Room (off the main thread), then report back on the main thread.
                io.execute(() -> {
                    dao.save(title, metadata);
                    WatchListEntry saved = dao.find(userId, title.animeId);
                    main.post(() -> callback.onSuccess(saved));
                });
            }

            @Override
            public void onError(ApiError error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void remove(String animeId, Runnable done) {
        final String userId = SessionStore.userId(context);
        io.execute(() -> {
            dao.delete(userId, animeId);
            if (done != null) {
                main.post(done);
            }
        });
    }

    @Override
    public LiveData<List<WatchListEntry>> getAll() {
        return dao.observeAll(SessionStore.userId(context));
    }

    @Override
    public LiveData<List<String>> getSavedIds() {
        return dao.observeIds(SessionStore.userId(context));
    }

    /** "Title" row: what the show is called, its poster, and the link to watch it. */
    static WatchListTitle toTitle(String userId, WatchPageDto page, long now) {
        WatchListTitle title = new WatchListTitle();
        title.userId = userId;
        title.animeId = page.id;
        title.title = page.title == null ? "Untitled" : page.title;
        title.posterImageUrl = page.posterImageUrl;
        title.videoLink = "/watch/" + page.id;
        title.addedAt = now;
        return title;
    }

    /** "Metadata" row: the rest of the details from the back end. */
    static WatchListMetadata toMetadata(String userId, WatchPageDto page, long now) {
        WatchListMetadata metadata = new WatchListMetadata();
        metadata.userId = userId;
        metadata.animeId = page.id;
        metadata.synopsis = page.synopsis;
        metadata.backdropImageUrl = page.backdropImageUrl;
        metadata.episodeCount = page.episodeCount;
        metadata.runtimeMinutes = page.runtimeMinutes;
        metadata.subAvailable = page.subAvailable;
        metadata.dubAvailable = page.dubAvailable;
        metadata.audioLanguages = page.availableAudioLanguages == null ? null
                : String.join(", ", page.availableAudioLanguages);
        metadata.subtitleLanguages = page.availableSubtitleLanguages == null ? null
                : String.join(", ", page.availableSubtitleLanguages);
        metadata.metadataSyncedAt = page.metadataLastSyncedAtUtc;
        metadata.savedAt = now;
        return metadata;
    }
}
