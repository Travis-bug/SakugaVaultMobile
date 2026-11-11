package ca.gbc.comp3074.sakugavault.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

/**
 * Room table "Title": one row per show the user saved to their watchlist.
 * The key is (user_id, anime_id), so two accounts on the same phone keep separate lists.
 */
@Entity(tableName = "Title", primaryKeys = {"user_id", "anime_id"})
public class WatchListTitle {

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId = "";

    @NonNull
    @ColumnInfo(name = "anime_id")
    public String animeId = "";

    @NonNull
    @ColumnInfo(name = "title")
    public String title = "";

    @Nullable
    @ColumnInfo(name = "poster_image_url")
    public String posterImageUrl;

    /**
     * The link to the video: the same "/watch/{id}" route the website uses.
     * The app does not store the actual stream address, because the back end's stream
     * links expire. Pressing play asks the back end for a fresh one.
     */
    @NonNull
    @ColumnInfo(name = "video_link")
    public String videoLink = "";

    /** When the show was added (milliseconds since 1970). */
    @ColumnInfo(name = "added_at")
    public long addedAt;
}
