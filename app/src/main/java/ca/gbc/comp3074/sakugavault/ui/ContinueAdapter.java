package ca.gbc.comp3074.sakugavault.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.progressindicator.LinearProgressIndicator;

import java.util.List;
import java.util.Locale;

import ca.gbc.comp3074.sakugavault.R;
import ca.gbc.comp3074.sakugavault.data.SampleData.HistoryEntry;

/**
 * Watch-history cards. Horizontal cards for the Home "Continue Watching" row,
 * or list rows (item_history) for the Profile screen.
 */
public class ContinueAdapter extends RecyclerView.Adapter<ContinueAdapter.Holder> {

    public interface OnEntryClick {
        void onClick(HistoryEntry entry);
    }

    private final List<HistoryEntry> items;
    private final boolean listRows;
    private final OnEntryClick listener;

    public ContinueAdapter(List<HistoryEntry> items, boolean listRows, OnEntryClick listener) {
        this.items = items;
        this.listRows = listRows;
        this.listener = listener;
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
        HistoryEntry entry = items.get(position);
        holder.art.setBackground(Ui.art(holder.itemView.getContext(), entry.anime, listRows ? 8 : 10));
        holder.initials.setText(entry.anime.initials());
        holder.title.setText(entry.anime.title);
        holder.progress.setProgress(entry.percent);
        String meta;
        if (entry.percent >= 100) {
            meta = String.format(Locale.US, "Episode %d · Watched", entry.episode);
        } else if (listRows) {
            meta = String.format(Locale.US, "Episode %d · %d%%", entry.episode, entry.percent);
        } else {
            meta = String.format(Locale.US, "Ep %d · %d min left", entry.episode, entry.minutesLeft());
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
        final TextView title;
        final TextView meta;
        final LinearProgressIndicator progress;

        Holder(@NonNull View itemView, boolean listRows) {
            super(itemView);
            if (listRows) {
                art = itemView.findViewById(R.id.histArt);
                initials = itemView.findViewById(R.id.histInitials);
                title = itemView.findViewById(R.id.histTitle);
                meta = itemView.findViewById(R.id.histMeta);
                progress = itemView.findViewById(R.id.histProgress);
            } else {
                art = itemView.findViewById(R.id.contArt);
                initials = itemView.findViewById(R.id.contInitials);
                title = itemView.findViewById(R.id.contTitle);
                meta = itemView.findViewById(R.id.contMeta);
                progress = itemView.findViewById(R.id.contProgress);
            }
        }
    }
}
