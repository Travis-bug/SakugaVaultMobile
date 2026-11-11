package ca.gbc.comp3074.sakugavault.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import ca.gbc.comp3074.sakugavault.R;
import ca.gbc.comp3074.sakugavault.api.dto.CommentDto;

/** Comments on the title page (from GET /api/watch/{id}; new ones go to POST /api/catalog/comments). */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.Holder> {

    private final List<CommentDto> items = new ArrayList<>();

    public void submit(List<CommentDto> data) {
        items.clear();
        if (data != null) {
            items.addAll(data);
        }
        notifyDataSetChanged();
    }

    public void addFirst(CommentDto comment) {
        items.add(0, comment);
        notifyItemInserted(0);
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_comment, parent, false);
        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {
        CommentDto comment = items.get(position);
        String when = Ui.timeAgo(comment.postedAtUtc);
        holder.header.setText(when.isEmpty() ? comment.userName : comment.userName + " · " + when);
        holder.body.setText(comment.body);
    }

    @Override
    public int getItemCount() {
        return items.size();
    }

    static class Holder extends RecyclerView.ViewHolder {
        final TextView header;
        final TextView body;

        Holder(@NonNull View itemView) {
            super(itemView);
            header = itemView.findViewById(R.id.cmHeader);
            body = itemView.findViewById(R.id.cmBody);
        }
    }
}
