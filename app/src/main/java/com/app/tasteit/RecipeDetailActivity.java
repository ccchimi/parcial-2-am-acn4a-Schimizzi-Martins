package com.app.tasteit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class RecipeDetailActivity extends AppCompatActivity {

    ImageView detailImage;
    TextView detailTitle, detailDescription;
    Button btnFavorite, btnBack;

    SharedPreferences sharedPrefs;
    Gson gson = new Gson();

    String recipeTitle, recipeDescription, recipeTime, recipeImageUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        // Toolbar + menú de cuenta
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ImageView ivAccount = findViewById(R.id.ivAccount);
        AccountMenuHelper.setup(this, ivAccount);

        detailImage = findViewById(R.id.detailImage);
        detailTitle = findViewById(R.id.detailTitle);
        detailDescription = findViewById(R.id.detailDescription);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnBack = findViewById(R.id.btnBack);

        sharedPrefs = getSharedPreferences("FavoritesPrefs", Context.MODE_PRIVATE);

        recipeTitle = getIntent().getStringExtra("title");
        recipeDescription = getIntent().getStringExtra("description");
        recipeTime = getIntent().getStringExtra("time");
        recipeImageUrl = getIntent().getStringExtra("imageUrl");

        if (recipeTime == null) recipeTime = "";

        detailTitle.setText(recipeTitle);
        detailDescription.setText(recipeDescription);

        Glide.with(this)
                .load(recipeImageUrl)
                .placeholder(R.drawable.tastel)
                .error(R.drawable.tastel)
                .into(detailImage);

        updateFavoriteButton();

        btnBack.setOnClickListener(v -> finish());
        btnFavorite.setOnClickListener(v -> toggleFavorite());
    }

    private void toggleFavorite() {
        String currentUser = LoginActivity.currentUser;
        if(currentUser == null) {
            Toast.makeText(this, getString(R.string.must_login_favorites), Toast.LENGTH_SHORT).show();
            return;
        }

        String key = "favorites_" + currentUser;
        String json = sharedPrefs.getString(key, null);
        Type type = new TypeToken<List<Recipe>>() {}.getType();
        List<Recipe> favorites = json == null ? new ArrayList<>() : gson.fromJson(json, type);

        boolean exists = false;
        for(int i = 0; i < favorites.size(); i++) {
            if(favorites.get(i).getTitle().equals(recipeTitle)) {
                favorites.remove(i);
                exists = true;
                break;
            }
        }

        if(exists) {
            Toast.makeText(this, getString(R.string.removed_favorite), Toast.LENGTH_SHORT).show();
        } else {
            favorites.add(new Recipe(recipeTitle, recipeDescription, recipeImageUrl, recipeTime));
            Toast.makeText(this, getString(R.string.added_favorite), Toast.LENGTH_SHORT).show();
        }

        sharedPrefs.edit().putString(key, gson.toJson(favorites)).apply();
        updateFavoriteButton();
    }

    private void updateFavoriteButton() {
        String currentUser = LoginActivity.currentUser;
        if(currentUser == null) return;

        String key = "favorites_" + currentUser;
        String json = sharedPrefs.getString(key, null);
        Type type = new TypeToken<List<Recipe>>() {}.getType();
        List<Recipe> favorites = json == null ? new ArrayList<>() : gson.fromJson(json, type);

        boolean isFavorite = false;
        for(Recipe r : favorites) {
            if(r.getTitle().equals(recipeTitle)) {
                isFavorite = true;
                break;
            }
        }

        btnFavorite.setText(
                isFavorite
                        ? getString(R.string.recipe_remove_fav)
                        : getString(R.string.recipe_add_fav)
        );
    }
}
