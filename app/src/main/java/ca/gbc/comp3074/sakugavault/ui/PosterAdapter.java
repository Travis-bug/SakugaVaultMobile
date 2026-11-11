package ca.gbc.comp3074.sakugavault.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ca.gbc.comp3074.sakugavault.R;
import ca.gbc.comp3074.sakugavault.data.Anime;

/** Poster cards, used for the Home genre rows, "More like this" and (grid mode) Search results. */
public class PosterAdapter extends RecyclerView.Adapter<PosterAdapter.Holder> {

    public interface OnAnimeClick {
        void onClick(Anime anime);
    }

    private final List<Anime> items = new ArrayList<>();
    private final boolean grid;
    private final OnAnimeClick listener;

    public PosterAdapter(List<Anime> data, boolean grid, OnAnimeClick listener) {
        this.items.addAll(data);
        this.grid = grid;
        this.listener = listener;
    }

    public void submit(List<Anime> data) {
        items.clear();
        items.addAll(data);
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_poster, parent, false);
        if (grid) {
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) view.getLayoutParams();
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            int side = Ui.dp(parent.getContext(), 6);
            params.setMargins(side, 0, side, Ui.dp(parent.getContext(), 16));
            params.setMarginStart(side);
            params.setMarginEnd(side);
            view.setLayoutParams(params);
            View art = view.findViewById(R.id.posterArt);
            art.getLayoutParams().height = Ui.dp(parent.getContext(), 220);
        }
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Anime anime = items.get(position);
        Ui.setArt(holder.art, anime, 10);
        holder.initials.setText(anime.initials());
        Ui.loadImage(holder.image, anime.posterUrl);
        holder.title.setText(anime.title);
        String meta = anime.badgeLine();
        holder.meta.setText(meta);
        holder.meta.setVisibility(meta.isEmpty() ? View.GONE : View.VISIBLE);
        holder.itemView.setOnClickListener(v -> listener.onClick(anime));
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

        Holder(@NonNull View itemView) {
            super(itemView);
            art = itemView.findViewById(R.id.posterArt);
            initials = itemView.findViewById(R.id.posterInitials);
            image = itemView.findViewById(R.id.posterImage);
            title = itemView.findViewById(R.id.posterTitle);
            meta = itemView.findViewById(R.id.posterMeta);
        }
    }
}
