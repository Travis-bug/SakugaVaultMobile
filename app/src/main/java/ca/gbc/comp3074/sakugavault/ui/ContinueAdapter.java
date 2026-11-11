package ca.gbc.comp3074.sakugavault.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import ca.gbc.comp3074.sakugavault.R;
import ca.gbc.comp3074.sakugavault.api.dto.WatchHistoryEntryDto;
import ca.gbc.comp3074.sakugavault.data.Anime;

/**
 * Tracked watch history from the back end. Horizontal cards for the Home
 * "Continue Watching" row, or list rows (item_history) for the Profile screen.
 */
public class ContinueAdapter extends RecyclerView.Adapter<ContinueAdapter.Holder> {

    public interface OnEntryClick {
        void onClick(WatchHistoryEntryDto entry);
    }

    private final List<WatchHistoryEntryDto> items = new ArrayList<>();
    private final boolean listRows;
    private final OnEntryClick listener;

    public ContinueAdapter(boolean listRows, OnEntryClick listener) {
        this.listRows = listRows;
        this.listener = listener;
    }

    public void submit(List<WatchHistoryEntryDto> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    /** How far through the episode, 0 to 100. */
    public static int percent(WatchHistoryEntryDto entry) {
        if (entry.completed) {
            return 100;
        }
        if (entry.durationSeconds <= 0) {
            return 0;
        }
        return Math.min(100, entry.positionSeconds * 100 / entry.durationSeconds);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = listRows ? R.layout.item_history : R.layout.item_continue;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new Holder(view, listRows);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        WatchHistoryEntryDto entry = items.get(position);
        Anime anime = Anime.fromHistory(entry);
        Ui.setArt(holder.art, anime, listRows ? 8 : 10);
        holder.initials.setText(anime.initials());
        Ui.loadImage(holder.image, anime.posterUrl);
        holder.title.setText(anime.title);
        int percent = percent(entry);
        holder.progress.setProgress(percent);
        String meta;
        if (percent >= 100) {
            meta = String.format(Locale.US, "Episode %d · Watched", entry.episodeNumber);
        } else if (listRows) {
            meta = String.format(Locale.US, "Episode %d · %d%%", entry.episodeNumber, percent);
        } else {
            int minutesLeft = Math.max(1, (entry.durationSeconds - entry.positionSeconds) / 60);
            meta = String.format(Locale.US, "Ep %d · %d min left", entry.episodeNumber, minutesLeft);
        }
        holder.meta.setText(meta);
        holder.itemView.setOnClickListener(v -> listener.onClick(entry));
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
        final LinearProgressIndicator progress;

        Holder(@NonNull View itemView, boolean listRows) {
            super(itemView);
            if (listRows) {
                art = itemView.findViewById(R.id.histArt);
                initials = itemView.findViewById(R.id.histInitials);
                image = itemView.findViewById(R.id.histImage);
                title = itemView.findViewById(R.id.histTitle);
                meta = itemView.findViewById(R.id.histMeta);
                progress = itemView.findViewById(R.id.histProgress);
            } else {
                art = itemView.findViewById(R.id.contArt);
                initials = itemView.findViewById(R.id.contInitials);
                image = itemView.findViewById(R.id.contImage);
                title = itemView.findViewById(R.id.contTitle);
                meta = itemView.findViewById(R.id.contMeta);
                progress = itemView.findViewById(R.id.contProgress);
            }
        }
    }
}
