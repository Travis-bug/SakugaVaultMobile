package ca.gbc.comp3074.sakugavault.ui;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

import ca.gbc.comp3074.sakugavault.R;
import ca.gbc.comp3074.sakugavault.data.SampleData.Comment;

/** Comments on the Title Detail screen (POST /api/catalog/comments in the real app). */
public class CommentAdapter extends RecyclerView.Adapter<CommentAdapter.Holder> {

    private final List<Comment> items;

    public CommentAdapter(List<Comment> items) {
        this.items = items;
    }

    public void addFirst(Comment comment) {
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
        Comment comment = items.get(position);
        holder.header.setText(comment.user + " · " + comment.when);
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
