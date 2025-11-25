package com.app.tasteit;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RecipeAdapter extends RecyclerView.Adapter<RecipeAdapter.RecipeViewHolder> {

    private final Context context;
    private List<Recipe> recipeList;
    private boolean showRemove;

    public RecipeAdapter(Context context, List<Recipe> recipeList, boolean showRemove) {
        this.context = context;
        this.recipeList = recipeList;
        this.showRemove = showRemove;
    }

    public void setRecipes(List<Recipe> recipes) {
        this.recipeList = recipes;
        notifyDataSetChanged();
    }

    public void setShowRemove(boolean showRemove) {
        this.showRemove = showRemove;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RecipeViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_recipe, parent, false);
        return new RecipeViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecipeViewHolder holder, int position) {
        Recipe recipe = recipeList.get(position);

        holder.title.setText(recipe.getTitle());
        holder.description.setText(recipe.getDescription());
        holder.time.setText(recipe.getCookingTime());
        holder.removeFav.setVisibility(showRemove ? View.VISIBLE : View.GONE);

        Glide.with(context)
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.tastel)
                .error(R.drawable.tastel)
                .into(holder.image);

        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, RecipeDetailActivity.class);
            intent.putExtra("title", recipe.getTitle());
            intent.putExtra("description", recipe.getDescription());
            intent.putExtra("imageUrl", recipe.getImageUrl());
            intent.putExtra("time", recipe.getCookingTime());
            context.startActivity(intent);
        });

        holder.removeFav.setOnClickListener(v -> {
            String currentUser = LoginActivity.currentUser;
            if(currentUser == null) {
                Toast.makeText(context, "Debes iniciar sesión", Toast.LENGTH_SHORT).show();
                return;
            }

            SharedPreferences sharedPrefs = context.getSharedPreferences("FavoritesPrefs", Context.MODE_PRIVATE);
            String key = "favorites_" + currentUser;
            String json = sharedPrefs.getString(key, null);
            Type type = new TypeToken<List<Recipe>>(){}.getType();
            List<Recipe> favorites = json == null ? new ArrayList<>() : new Gson().fromJson(json, type);

            for (int i = 0; i < favorites.size(); i++) {
                if(favorites.get(i).getTitle().equals(recipe.getTitle())) {
                    favorites.remove(i);
                    break;
                }
            }

            sharedPrefs.edit().putString(key, new Gson().toJson(favorites)).apply();

            recipeList.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, recipeList.size());

            Toast.makeText(context, "Receta eliminada de favoritos", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public int getItemCount() {
        return recipeList != null ? recipeList.size() : 0;
    }

    public static class RecipeViewHolder extends RecyclerView.ViewHolder {
        TextView title, description, time;
        ImageView image, removeFav;

        public RecipeViewHolder(@NonNull View itemView) {
            super(itemView);
            title = itemView.findViewById(R.id.recipeTitle);
            description = itemView.findViewById(R.id.recipeDescription);
            time = itemView.findViewById(R.id.recipeTime);
            image = itemView.findViewById(R.id.recipeImage);
            removeFav = itemView.findViewById(R.id.btnRemoveFav);
        }
    }
}
