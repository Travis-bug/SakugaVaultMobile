package ca.gbc.comp3074.sakugavault.data.local;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Upsert;

import java.util.List;

/** SQL for the watchlist tables. Room writes the code behind these methods. */
@Dao
public abstract class WatchListDao {

    /** Title row + its Metadata row (with "meta_" column names to match WatchListEntry). */
    static final String SELECT_ENTRIES =
            "SELECT Title.*,"
                    + " Metadata.user_id AS meta_user_id,"
                    + " Metadata.anime_id AS meta_anime_id,"
                    + " Metadata.synopsis AS meta_synopsis,"
                    + " Metadata.backdrop_image_url AS meta_backdrop_image_url,"
                    + " Metadata.episode_count AS meta_episode_count,"
                    + " Metadata.runtime_minutes AS meta_runtime_minutes,"
                    + " Metadata.sub_available AS meta_sub_available,"
                    + " Metadata.dub_available AS meta_dub_available,"
                    + " Metadata.audio_languages AS meta_audio_languages,"
                    + " Metadata.subtitle_languages AS meta_subtitle_languages,"
                    + " Metadata.metadata_synced_at AS meta_metadata_synced_at,"
                    + " Metadata.saved_at AS meta_saved_at"
                    + " FROM Title INNER JOIN Metadata"
                    + " ON Metadata.user_id = Title.user_id AND Metadata.anime_id = Title.anime_id";

    @Upsert
    public abstract void upsertTitle(WatchListTitle title);

    @Upsert
    public abstract void upsertMetadata(WatchListMetadata metadata);

    /** Saves the Title row and its Metadata row together: both are saved, or neither is. */
    @Transaction
    public void save(WatchListTitle title, WatchListMetadata metadata) {
        upsertTitle(title);
        upsertMetadata(metadata);
    }

    /** Deletes a saved show. Its Metadata row is deleted with it (ON DELETE CASCADE). */
    @Query("DELETE FROM Title WHERE user_id = :userId AND anime_id = :animeId")
    public abstract int delete(String userId, String animeId);

    /** The whole watchlist, newest first. LiveData updates the screen whenever the tables change. */
    @Query(SELECT_ENTRIES + " WHERE Title.user_id = :userId ORDER BY Title.added_at DESC")
    public abstract LiveData<List<WatchListEntry>> observeAll(String userId);

    @Query(SELECT_ENTRIES + " WHERE Title.user_id = :userId AND Title.anime_id = :animeId")
    public abstract WatchListEntry find(String userId, String animeId);

    /** Just the saved ids, so buttons can show "In Watchlist". */
    @Query("SELECT anime_id FROM Title WHERE user_id = :userId")
    public abstract LiveData<List<String>> observeIds(String userId);
}
