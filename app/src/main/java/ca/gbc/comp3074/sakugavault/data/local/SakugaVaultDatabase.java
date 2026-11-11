package ca.gbc.comp3074.sakugavault.data.local;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/** The app's Room (SQLite) database. Right now it holds the watchlist: tables "Title" and "Metadata". */
@Database(
        entities = {WatchListTitle.class, WatchListMetadata.class},
        version = 1,
        exportSchema = false)
public abstract class SakugaVaultDatabase extends RoomDatabase {

    private static volatile SakugaVaultDatabase instance;

    public abstract WatchListDao watchListDao();

    /** Only one database instance is created for the whole app. */
    public static SakugaVaultDatabase get(Context context) {
        if (instance == null) {
            synchronized (SakugaVaultDatabase.class) {
                if (instance == null) {
                    instance = Room.databaseBuilder(context.getApplicationContext(),
                            SakugaVaultDatabase.class, "sakugavault.db").build();
                }
            }
        }
        return instance;
    }
}
