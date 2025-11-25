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

public class CommunityRecipeAdapter extends RecyclerView.Adapter<CommunityRecipeAdapter.CommunityViewHolder> {

    private final Context context;
    private List<CommunityRecipe> recipes;
    private final Gson gson = new Gson();

    public CommunityRecipeAdapter(Context context, List<CommunityRecipe> recipes) {
        this.context = context;
        this.recipes = recipes;
    }

    public void setRecipes(List<CommunityRecipe> recipes) {
        this.recipes = recipes;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public CommunityViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_community_recipe, parent, false);
        return new CommunityViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommunityViewHolder holder, int position) {
        CommunityRecipe recipe = recipes.get(position);

        holder.title.setText(recipe.getTitle());
        holder.author.setText("Por @" + recipe.getAuthor());
        holder.time.setText(recipe.getCookingTime() + " min");
        holder.description.setText(recipe.getDescription());

        Glide.with(context)
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.tastel)
                .error(R.drawable.tastel)
                .into(holder.image);

        boolean isFav = isInFavorites(recipe);
        holder.setFavorite(isFav);

        // Ir al detalle – ahora pasamos el ID de Firestore
        holder.itemView.setOnClickListener(v -> {
            Intent intent = new Intent(context, CommunityRecipeDetailActivity.class);
            intent.putExtra("recipe_id", recipe.getId());
            context.startActivity(intent);
        });

        // Toggle de favoritos desde comunidad
        holder.btnFav.setOnClickListener(v -> {
            if (holder.isFavorite) {
                // Quitar de favoritos
                removeFromFavorites(recipe);
                holder.setFavorite(false);
                Toast.makeText(context, "Eliminado de favoritos", Toast.LENGTH_SHORT).show();
            } else {
                // Agregar a favoritos
                addToFavoritesFromCommunity(recipe);
                holder.setFavorite(true);
                // El mensaje del metodo de abajo ya muestra el Toast de agregado
            }
        });
    }

    @Override
    public int getItemCount() {
        return recipes != null ? recipes.size() : 0;
    }

    static class CommunityViewHolder extends RecyclerView.ViewHolder {
        ImageView image, btnFav;
        TextView title, author, description, time;
        boolean isFavorite = false;

        public CommunityViewHolder(@NonNull View itemView) {
            super(itemView);
            image = itemView.findViewById(R.id.communityImage);
            btnFav = itemView.findViewById(R.id.btnFavFromCommunity);
            title = itemView.findViewById(R.id.communityTitle);
            author = itemView.findViewById(R.id.communityAuthor);
            description = itemView.findViewById(R.id.communityDescription);
            time = itemView.findViewById(R.id.communityTime);
        }

        void setFavorite(boolean favorite) {
            isFavorite = favorite;
            btnFav.setImageResource(
                    favorite ? android.R.drawable.btn_star_big_on : android.R.drawable.btn_star_big_off
            );
            btnFav.setContentDescription(
                    favorite ? "Quitar de favoritos" : "Agregar a favoritos"
            );
        }
    }

    // ----- Helpers de favoritos -----

    private boolean isInFavorites(CommunityRecipe cRecipe) {
        String currentUser = LoginActivity.currentUser;
        if (currentUser == null) return false;

        SharedPreferences sharedPrefs = context.getSharedPreferences("FavoritesPrefs", Context.MODE_PRIVATE);
        String key = "favorites_" + currentUser;
        String json = sharedPrefs.getString(key, null);
        if (json == null) return false;

        Type type = new TypeToken<List<Recipe>>() {}.getType();
        List<Recipe> favorites = gson.fromJson(json, type);
        if (favorites == null) return false;

        for (Recipe r : favorites) {
            if (r.getTitle().equals(cRecipe.getTitle())
                    && "Comunidad".equals(r.getCategory())) {
                return true;
            }
        }
        return false;
    }

    private void addToFavoritesFromCommunity(CommunityRecipe cRecipe) {
        String currentUser = LoginActivity.currentUser;
        if (currentUser == null) {
            Toast.makeText(context, context.getString(R.string.must_login_favorites), Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPrefs = context.getSharedPreferences("FavoritesPrefs", Context.MODE_PRIVATE);
        String key = "favorites_" + currentUser;
        String json = sharedPrefs.getString(key, null);
        Type type = new TypeToken<List<Recipe>>() {}.getType();
        List<Recipe> favorites = json == null ? new ArrayList<>() : gson.fromJson(json, type);

        // Evitar duplicar la misma receta de comunidad
        for (Recipe r : favorites) {
            if (r.getTitle().equals(cRecipe.getTitle())
                    && "Comunidad".equals(r.getCategory())) {
                Toast.makeText(context, context.getString(R.string.already_favorite), Toast.LENGTH_SHORT).show();
                return;
            }
        }

        String descWithAuthor = cRecipe.getDescription()
                + "\n\nSubida por: @" + cRecipe.getAuthor() + " (Comunidad)";

        Recipe fav = new Recipe(
                cRecipe.getTitle(),
                "Comunidad",
                descWithAuthor,
                cRecipe.getImageUrl(),
                cRecipe.getCookingTime()
        );

        favorites.add(fav);
        sharedPrefs.edit().putString(key, gson.toJson(favorites)).apply();
        Toast.makeText(context, context.getString(R.string.added_favorite), Toast.LENGTH_SHORT).show();
    }

    private void removeFromFavorites(CommunityRecipe cRecipe) {
        String currentUser = LoginActivity.currentUser;
        if (currentUser == null) {
            return;
        }

        SharedPreferences sharedPrefs = context.getSharedPreferences("FavoritesPrefs", Context.MODE_PRIVATE);
        String key = "favorites_" + currentUser;
        String json = sharedPrefs.getString(key, null);
        if (json == null) {
            return;
        }

        Type type = new TypeToken<List<Recipe>>() {}.getType();
        List<Recipe> favorites = gson.fromJson(json, type);
        if (favorites == null || favorites.isEmpty()) {
            return;
        }

        for (int i = 0; i < favorites.size(); i++) {
            Recipe r = favorites.get(i);
            if (r.getTitle().equals(cRecipe.getTitle())
                    && "Comunidad".equals(r.getCategory())) {
                favorites.remove(i);
                break;
            }
        }

        sharedPrefs.edit().putString(key, gson.toJson(favorites)).apply();
    }
}
