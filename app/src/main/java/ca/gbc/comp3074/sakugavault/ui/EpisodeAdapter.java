package ca.gbc.comp3074.sakugavault.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import ca.gbc.comp3074.sakugavault.R;

/** Episode list for the selected season on the Title Detail screen. */
public class EpisodeAdapter extends RecyclerView.Adapter<EpisodeAdapter.Holder> {

    public interface OnEpisodeClick {
        void onEpisode(int number);
    }

    private final OnEpisodeClick listener;
    private int first = 1;
    private int count = 0;
    private int current = 1;

    public EpisodeAdapter(OnEpisodeClick listener) {
        this.listener = listener;
    }

    public void setRange(int first, int count, int current) {
        this.first = first;
        this.count = count;
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
        int number = first + position;
        boolean playing = number == current;
        holder.number.setText("Episode " + number);
        holder.itemView.setBackgroundResource(playing ? R.drawable.bg_episode_active : R.drawable.bg_episode);
        holder.status.setVisibility(playing ? View.VISIBLE : View.GONE);
        holder.chevron.setVisibility(playing ? View.GONE : View.VISIBLE);
        holder.itemView.setOnClickListener(v -> listener.onEpisode(number));
    }

    @Override
    public int getItemCount() {
        return count;
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
