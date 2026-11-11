package ca.gbc.comp3074.sakugavault.services.watch;

import androidx.lifecycle.LiveData;

import java.util.List;

import ca.gbc.comp3074.sakugavault.api.ApiCallback;
import ca.gbc.comp3074.sakugavault.data.local.WatchListEntry;

/**
 * The watchlist: shows the user saved to watch later.
 * Kept on the phone in Room (tables "Title" and "Metadata"); the back end has no watchlist.
 */
public interface IWatchListService {

    /**
     * Grabs the title and metadata of the anime from the back end (GET /api/watch/{id})
     * and saves them in the "Title" and "Metadata" tables.
     */
    void add(String animeId, ApiCallback<WatchListEntry> callback);

    /** Removes a saved show (its Title row, and its Metadata row with it). */
    void remove(String animeId, Runnable done);

    /** Every saved show for the signed-in user, newest first. Updates by itself when the tables change. */
    LiveData<List<WatchListEntry>> getAll();

    /** The ids of the saved shows, for "Watchlist" / "In Watchlist" buttons. */
    LiveData<List<String>> getSavedIds();
}
