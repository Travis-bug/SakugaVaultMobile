package ca.gbc.comp3074.sakugavault.services.watch;

import java.util.List;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.api.dto.WatchHistoryEntryDto;

/** The tracked watch history. Mirrors the back end's Services/Watch/IWatchHistoryService. */
public interface IWatchHistoryService {

    /** GET /api/watch/history/me: one entry per show (its latest episode), newest first. */
    void getMyHistory(int pageSize, ApiCallback<List<WatchHistoryEntryDto>> callback);

    /** POST /api/watch/history: saves where the user is. Errors are ignored so watching is never interrupted. */
    void saveProgress(String animeId, int episodeNumber, int positionSeconds, int durationSeconds, boolean completed);

    /**
     * Saves the final position, then flushes it, in that order. Used when leaving the title page, so
     * Continue Watching (which reads MySQL) shows the latest position right away.
     */
    void saveProgressAndFlush(String animeId, int episodeNumber, int positionSeconds, int durationSeconds,
                              boolean completed);

    /** POST /api/watch/history/flush: writes progress the server is holding in Redis to MySQL right away. */
    void flush();
}
