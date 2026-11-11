package ca.gbc.comp3074.sakugavault.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import ca.gbc.comp3074.sakugavault.R;
import ca.gbc.comp3074.sakugavault.api.dto.WatchEpisodeDto;

/** Episode list for the selected season on the title page. */
public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.Holder> {

    public interface OnEpisodeClick {
        void onEpisode(int number);
    }

    private final OnEpisodeClick listener;
    private final List<WatchEpisodeDto> episodes = new ArrayList<>();
    private int current = 1;

    public EpisodeAdapter(OnEpisodeClick listener) {
        this.listener = listener;
    }

    public void submit(List<WatchEpisodeDto> data, int current) {
        episodes.clear();
        if (data != null) {
            episodes.addAll(data);
        }
        this.current = current;
        notifyDataSetChanged();
    }

    public void setCurrent(int current) {
        this.current = current;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_episode, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        WatchEpisodeDto episode = episodes.get(position);
        int number = episode.episodeNumber;
        boolean playing = number == current;
        holder.number.setText(label(episode));
        holder.itemView.setBackgroundResource(playing ? R.drawable.bg_episode_active : R.drawable.bg_episode);
        holder.status.setVisibility(playing ? View.VISIBLE : View.GONE);
        holder.chevron.setVisibility(playing ? View.GONE : View.VISIBLE);
        holder.itemView.setOnClickListener(v -> listener.onEpisode(number));
    }

    /**
     * "Episode 3", or "Episode 3 · The title" when the provider gives a real episode name.
     * The back end sends "Episode 3 · <provider title>", and many providers use "Episode 3" as the
     * title too, so a repeated "Episode 3" is dropped.
     */
    static String label(WatchEpisodeDto episode) {
        String base = String.format(Locale.US, "Episode %d", episode.episodeNumber);
        String title = episode.label == null ? "" : episode.label.trim();
        Pattern prefix = Pattern.compile("^episode\\s*" + episode.episodeNumber + "(?!\\d)\\s*[·:\\-]?\\s*",
                Pattern.CASE_INSENSITIVE);
        for (int i = 0; i < 2; i++) {
            title = prefix.matcher(title).replaceFirst("").trim();
        }
        return title.isEmpty() ? base : base + " · " + title;
    }

    @Override
    public int getItemCount() {
        return episodes.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView number;
        final TextView status;
        final View chevron;

        Holder(@NonNull View itemView) {
            super(itemView);
            number = itemView.findViewById(R.id.epNumber);
            status = itemView.findViewById(R.id.epStatus);
            chevron = itemView.findViewById(R.id.epChevron);
        }
    }
}
