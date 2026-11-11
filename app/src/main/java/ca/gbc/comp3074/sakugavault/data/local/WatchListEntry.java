package ca.gbc.comp3074.sakugavault.data.local;

import androidx.room.Embedded;

/** One saved show: its "Title" row joined with its "Metadata" row. */
public class WatchListEntry {

    @Embedded
    public WatchListTitle title;

    /** Metadata columns come back with a "meta_" prefix so they don't clash with the Title columns. */
    @Embedded(prefix = "meta_")
    public WatchListMetadata metadata;
}
