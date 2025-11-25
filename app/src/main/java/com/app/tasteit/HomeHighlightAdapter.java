package com.app.tasteit;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class HomeHighlightAdapter extends RecyclerView.Adapter<HomeHighlightAdapter.HighlightViewHolder> {

    private final Context context;
    private final List<HomeHighlight> items;

    public HomeHighlightAdapter(Context context, List<HomeHighlight> items) {
        this.context = context;
        this.items = items;
    }

    @NonNull
    @Override
    public HighlightViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_home_highlight, parent, false);
        return new HighlightViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HighlightViewHolder holder, int position) {
        HomeHighlight item = items.get(position);

        holder.title.setText(item.getTitle());
        holder.description.setText(item.getDescription());

        Glide.with(context)
                .load(item.getImageUrl())
                .placeholder(R.drawable.tastel)
                .error(R.drawable.tastel)
                .into(holder.image);
    }

    @Override
    public int getItemCount() {
        return items != null ? items.size() : 0;
    }

    static class HighlightViewHolder extends RecyclerView.ViewHolder {
        ImageView image;
        TextView title, description;

        public HighlightViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.highlightImage);
            title = itemView.findViewById(R.id.highlightTitle);
            description = itemView.findViewById(R.id.highlightDescription);
        }
    }
}
