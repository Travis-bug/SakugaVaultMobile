package ca.gbc.comp3074.sakugavault.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import ca.gbc.comp3074.sakugavault.R;
import ca.gbc.comp3074.sakugavault.data.Anime;

/** Featured titles in the swipeable hero carousel on Home, each with Watch Now and Watchlist buttons. */
public class HeroAdapter extends RecyclerView.Adapter<HeroAdapter.Holder> {

    public interface OnWatchlistToggle {
        void onToggle(Anime anime, boolean currentlySaved, MaterialButton button);
    }

    private final List<Anime> items;
    private final PosterAdapter.OnAnimeClick onWatch;
    private final OnWatchlistToggle onToggle;
    private final Set<String> savedIds = new HashSet<>();

    public HeroAdapter(List<Anime> items, PosterAdapter.OnAnimeClick onWatch, OnWatchlistToggle onToggle) {
        this.items = items;
        this.onWatch = onWatch;
        this.onToggle = onToggle;
    }

    /** Called whenever the watchlist changes, so each button shows "Watchlist" or "In Watchlist". */
    public void setSavedIds(Collection<String> ids) {
        savedIds.clear();
        if (ids != null) {
            savedIds.addAll(ids);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_hero, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        Anime anime = items.get(position);
        Ui.setArt(holder.card, anime, 16);
        String image = anime.backdropUrl != null ? anime.backdropUrl : anime.posterUrl;
        Ui.loadImage(holder.image, image);
        holder.art.setText(anime.title.toUpperCase(Locale.ROOT));
        holder.art.setVisibility(image == null ? View.VISIBLE : View.GONE);
        String genres = anime.genreLine();
        holder.genres.setText(genres.isEmpty() ? "Featured" : genres);
        holder.title.setText(anime.title);
        holder.synopsis.setText(anime.synopsis);
        holder.watchNow.setOnClickListener(v -> onWatch.onClick(anime));

        boolean saved = savedIds.contains(anime.id);
        holder.watchlist.setEnabled(true);
        holder.watchlist.setText(saved ? "In Watchlist" : "Watchlist");
        holder.watchlist.setIconResource(saved ? R.drawable.ic_check : R.drawable.ic_add);
        holder.watchlist.setOnClickListener(v -> onToggle.onToggle(anime, saved, holder.watchlist));
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View card;
        final ImageView image;
        final TextView art;
        final TextView genres;
        final TextView title;
        final TextView synopsis;
        final MaterialButton watchNow;
        final MaterialButton watchlist;

        Holder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.heroCard);
            image = itemView.findViewById(R.id.heroImage);
            art = itemView.findViewById(R.id.heroArt);
            genres = itemView.findViewById(R.id.heroGenres);
            title = itemView.findViewById(R.id.heroTitle);
            synopsis = itemView.findViewById(R.id.heroSynopsis);
            watchNow = itemView.findViewById(R.id.btnWatchNow);
            watchlist = itemView.findViewById(R.id.btnWatchlist);
        }
    }
}
