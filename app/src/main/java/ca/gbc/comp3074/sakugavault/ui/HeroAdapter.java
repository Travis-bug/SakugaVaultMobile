package ca.gbc.comp3074.sakugavault.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.button.MaterialButton;

import java.util.List;
import java.util.Locale;

import ca.gbc.comp3074.sakugavault.R;
import ca.gbc.comp3074.sakugavault.data.Anime;
import ca.gbc.comp3074.sakugavault.data.SampleData;

/** Featured titles in the swipeable hero carousel on Home. */
public class HeroAdapter extends RecyclerView.Adapter<HeroAdapter.Holder> {

    private final List<Anime> items;
    private final PosterAdapter.OnAnimeClick onWatch;

    public HeroAdapter(List<Anime> items, PosterAdapter.OnAnimeClick onWatch) {
        this.items = items;
        this.onWatch = onWatch;
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
        holder.card.setBackground(Ui.art(holder.itemView.getContext(), anime, 16));
        holder.art.setText(anime.title.toUpperCase(Locale.ROOT));
        holder.genres.setText(anime.genreLine());
        holder.title.setText(anime.title);
        holder.synopsis.setText(anime.synopsis);
        holder.watchNow.setOnClickListener(v -> onWatch.onClick(anime));
        showWatchlistState(holder.watchlist, SampleData.WATCHLIST.contains(anime.id));
        holder.watchlist.setOnClickListener(v -> {
            boolean added;
            if (SampleData.WATCHLIST.contains(anime.id)) {
                SampleData.WATCHLIST.remove(anime.id);
                added = false;
            } else {
                SampleData.WATCHLIST.add(anime.id);
                added = true;
            }
            showWatchlistState(holder.watchlist, added);
            Toast.makeText(v.getContext(),
                    added ? "Added to your watchlist" : "Removed from your watchlist",
                    Toast.LENGTH_SHORT).show();
        });
    }

    private void showWatchlistState(MaterialButton button, boolean inList) {
        button.setText(inList ? "In Watchlist" : "Watchlist");
        button.setIconResource(inList ? R.drawable.ic_check : R.drawable.ic_add);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final View card;
        final TextView art;
        final TextView genres;
        final TextView title;
        final TextView synopsis;
        final MaterialButton watchNow;
        final MaterialButton watchlist;

        Holder(@NonNull View itemView) {
            super(itemView);
            card = itemView.findViewById(R.id.heroCard);
            art = itemView.findViewById(R.id.heroArt);
            genres = itemView.findViewById(R.id.heroGenres);
            title = itemView.findViewById(R.id.heroTitle);
            synopsis = itemView.findViewById(R.id.heroSynopsis);
            watchNow = itemView.findViewById(R.id.btnWatchNow);
            watchlist = itemView.findViewById(R.id.btnWatchlist);
        }
    }
}
