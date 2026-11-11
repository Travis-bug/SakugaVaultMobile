package ca.gbc.comp3074.sakugavault.data.local;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;

/**
 * Room table "Metadata": the details grabbed from the back end for a saved show.
 * Linked to its row in "Title"; deleting the title deletes this row too (CASCADE).
 */
@Entity(tableName = "Metadata",
        primaryKeys = {"user_id", "anime_id"},
        foreignKeys = @ForeignKey(
                entity = WatchListTitle.class,
                parentColumns = {"user_id", "anime_id"},
                childColumns = {"user_id", "anime_id"},
                onDelete = ForeignKey.CASCADE))
public class WatchListMetadata {

    @NonNull
    @ColumnInfo(name = "user_id")
    public String userId = "";

    @NonNull
    @ColumnInfo(name = "anime_id")
    public String animeId = "";

    @Nullable
    @ColumnInfo(name = "synopsis")
    public String synopsis;

    @Nullable
    @ColumnInfo(name = "backdrop_image_url")
    public String backdropImageUrl;

    @ColumnInfo(name = "episode_count")
    public int episodeCount;

    @ColumnInfo(name = "runtime_minutes")
    public int runtimeMinutes;

    @ColumnInfo(name = "sub_available")
    public boolean subAvailable;

    @ColumnInfo(name = "dub_available")
    public boolean dubAvailable;

    /** For example "ja, en". */
    @Nullable
    @ColumnInfo(name = "audio_languages")
    public String audioLanguages;

    @Nullable
    @ColumnInfo(name = "subtitle_languages")
    public String subtitleLanguages;

    /** When the back end last refreshed this show's details (ISO date from the API), if known. */
    @Nullable
    @ColumnInfo(name = "metadata_synced_at")
    public String metadataSyncedAt;

    /** When the app grabbed these details (milliseconds since 1970). */
    @ColumnInfo(name = "saved_at")
    public long savedAt;
}
