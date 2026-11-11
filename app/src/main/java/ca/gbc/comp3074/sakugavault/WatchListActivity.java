package ca.gbc.comp3074.sakugavault;

import ca.gbc.comp3074.sakugavault.services.DeveloperLogger;

import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Locale;

import ca.gbc.comp3074.sakugavault.data.local.WatchListEntry;
import ca.gbc.comp3074.sakugavault.services.AppServices;
import ca.gbc.comp3074.sakugavault.services.watch.IWatchListService;
import ca.gbc.comp3074.sakugavault.ui.Ui;
import ca.gbc.comp3074.sakugavault.ui.WatchListAdapter;

/** My Watchlist: every show saved in the Room "Title" and "Metadata" tables, newest first. */
public class WatchListActivity extends AppCompatActivity {

    private IWatchListService watchList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_watch_list);
        Ui.padForSystemBars(findViewById(R.id.root));
        watchList = AppServices.watchList(this);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        TextView header = findViewById(R.id.tvHeader);
        TextView empty = findViewById(R.id.tvEmpty);

        RecyclerView list = findViewById(R.id.watchList);
        list.setLayoutManager(new LinearLayoutManager(this));
        WatchListAdapter adapter = new WatchListAdapter(new WatchListAdapter.Listener() {
            @Override
            public void onOpen(WatchListEntry entry) {
                WatchActivity.start(WatchListActivity.this, idFromLink(entry), 1, 0);
            }

            @Override
            public void onRemove(WatchListEntry entry) {
                confirmRemove(entry);
            }
        });
        list.setAdapter(adapter);

        // LiveData from Room: the list redraws by itself whenever a show is added or removed.
        watchList.getAll().observe(this, entries -> {
            adapter.submit(entries);
            int count = entries == null ? 0 : entries.size();
            header.setText(String.format(Locale.US, "My Watchlist (%d)", count));
            empty.setVisibility(count == 0 ? View.VISIBLE : View.GONE);
        });
    }

    /** The stored video link is "/watch/{id}"; the id after it opens the title page. */

    // **********function marked to be unit tested********** //
    private String idFromLink(WatchListEntry entry) {

        try {
            String link = entry.title.videoLink;

            if (link.startsWith("/watch/") && link.length() > "/watch/".length()) {
                return link.substring("/watch/".length());
            }
            return entry.title.animeId;

        }
        catch(Exception e) {
            DeveloperLogger.logErrorToJsonFile(this, entry, e);
            return null;
        }
    }
// **********function marked to be unit tested********** //





    private void confirmRemove(WatchListEntry entry) {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Remove from watchlist?")
                .setMessage("“" + entry.title.title + "” will be removed from this phone's watchlist.")
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Remove", (dialog, which) ->
                        watchList.remove(entry.title.animeId, () ->
                                Toast.makeText(this, "Removed from your watchlist", Toast.LENGTH_SHORT).show()))
                .show();
    }
}
