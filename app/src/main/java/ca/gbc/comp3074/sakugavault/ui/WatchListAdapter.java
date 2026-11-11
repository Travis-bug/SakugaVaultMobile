package ca.gbc.comp3074.sakugavault.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import ca.gbc.comp3074.sakugavault.R;
import ca.gbc.comp3074.sakugavault.data.Anime;
import ca.gbc.comp3074.sakugavault.data.local.WatchListEntry;

/** Rows on the My Watchlist screen: each one is a Title row plus its Metadata row from Room. */
public class WatchListAdapter extends RecyclerView.Adapter<WatchListAdapter.Holder> {

    public interface Listener {
        void onOpen(WatchListEntry entry);

        void onRemove(WatchListEntry entry);
    }

    private final List<WatchListEntry> items = new ArrayList<>();
    private final Listener listener;

    public WatchListAdapter(Listener listener) {
        this.listener = listener;
    }

    public void submit(List<WatchListEntry> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_watchlist, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        WatchListEntry entry = items.get(position);
        Anime anime = Anime.fromWatchList(entry);
        Ui.setArt(holder.art, anime, 8);
        holder.initials.setText(anime.initials());
        Ui.loadImage(holder.image, entry.title.posterImageUrl);
        holder.title.setText(entry.title.title);

        // Metadata table: episodes, runtime, SUB/DUB, synopsis
        StringBuilder meta = new StringBuilder(anime.badgeLine());
        if (entry.metadata.runtimeMinutes > 0) {
            meta.append(meta.length() > 0 ? " · " : "")
                    .append(String.format(Locale.US, "%d min", entry.metadata.runtimeMinutes));
        }
        holder.meta.setText(meta);
        holder.synopsis.setText(entry.metadata.synopsis);
        holder.synopsis.setVisibility(entry.metadata.synopsis == null || entry.metadata.synopsis.isEmpty()
                ? View.GONE : View.VISIBLE);
        String saved = DateFormat.getDateInstance(DateFormat.MEDIUM).format(new Date(entry.title.addedAt));
        holder.saved.setText("Saved " + saved + " · " + entry.title.videoLink);

        holder.itemView.setOnClickListener(v -> listener.onOpen(entry));
        holder.remove.setOnClickListener(v -> listener.onRemove(entry));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View art;
        final TextView initials;
        final ImageView image;
        final TextView title;
        final TextView meta;
        final TextView synopsis;
        final TextView saved;
        final MaterialButton remove;

        Holder(@NonNull View itemView) {
            super(itemView);
            art = itemView.findViewById(R.id.wlArt);
            initials = itemView.findViewById(R.id.wlInitials);
            image = itemView.findViewById(R.id.wlImage);
            title = itemView.findViewById(R.id.wlTitle);
            meta = itemView.findViewById(R.id.wlMeta);
            synopsis = itemView.findViewById(R.id.wlSynopsis);
            saved = itemView.findViewById(R.id.wlSaved);
            remove = itemView.findViewById(R.id.btnRemove);
        }
    }
}
