package com.app.tasteit;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
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

public class CommunityRecipeDetailActivity extends AppCompatActivity {

    private ImageView detailImage;
    private TextView detailTitle, detailAuthor, detailTime, detailDescription;
    private Button btnFav, btnDownload, btnEdit, btnBack;

    private SharedPreferences communityPrefs;
    private Gson gson = new Gson();
    private List<CommunityRecipe> communityRecipes = new ArrayList<>();
    private int recipeIndex = -1;
    private CommunityRecipe recipe;

    private static final String PREFS_NAME = "CommunityPrefs";
    private static final String COMMUNITY_KEY = "community_recipes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_recipe_detail);

        // Toolbar + menú de cuenta
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        ImageView ivAccount = findViewById(R.id.ivAccount);
        AccountMenuHelper.setup(this, ivAccount);

        detailImage = findViewById(R.id.detailImage);
        detailTitle = findViewById(R.id.detailTitle);
        detailAuthor = findViewById(R.id.detailAuthor);
        detailTime = findViewById(R.id.detailTime);
        detailDescription = findViewById(R.id.detailDescription);
        btnFav = findViewById(R.id.btnFav);
        btnDownload = findViewById(R.id.btnDownload);
        btnEdit = findViewById(R.id.btnEdit);
        btnBack = findViewById(R.id.btnBack);

        communityPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        loadCommunityRecipes();

        recipeIndex = getIntent().getIntExtra("recipe_index", -1);
        if (recipeIndex < 0 || recipeIndex >= communityRecipes.size()) {
            finish();
            return;
        }

        recipe = communityRecipes.get(recipeIndex);
        bindData();

        btnBack.setOnClickListener(v -> finish());

        btnFav.setOnClickListener(v -> addToFavoritesFromCommunity(recipe));

        btnDownload.setOnClickListener(v -> downloadRecipe(recipe));

        String currentUser = LoginActivity.currentUser;
        if (currentUser == null || !currentUser.equals(recipe.getAuthor())) {
            btnEdit.setEnabled(false);
            btnEdit.setAlpha(0.4f);
        } else {
            btnEdit.setOnClickListener(v -> {
                EditCommunityRecipeDialog dialog = new EditCommunityRecipeDialog(
                        this,
                        recipe,
                        updated -> {
                            communityRecipes.set(recipeIndex, updated);
                            saveCommunityRecipes();
                            recipe = updated;
                            bindData();
                            Toast.makeText(this, "Receta actualizada", Toast.LENGTH_SHORT).show();
                        }
                );
                dialog.show();
            });
        }
    }

    private void bindData() {
        detailTitle.setText(recipe.getTitle());
        detailAuthor.setText("Por @" + recipe.getAuthor());
        detailTime.setText("Tiempo: " + recipe.getCookingTime());
        detailDescription.setText(recipe.getDescription());

        Glide.with(this)
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.tastel)
                .error(R.drawable.tastel)
                .into(detailImage);
    }

    private void loadCommunityRecipes() {
        String json = communityPrefs.getString(COMMUNITY_KEY, null);
        Type type = new TypeToken<List<CommunityRecipe>>(){}.getType();
        communityRecipes = json == null ? new ArrayList<>() : gson.fromJson(json, type);
        if (communityRecipes == null) communityRecipes = new ArrayList<>();
    }

    private void saveCommunityRecipes() {
        communityPrefs.edit()
                .putString(COMMUNITY_KEY, gson.toJson(communityRecipes))
                .apply();
    }

    private void addToFavoritesFromCommunity(CommunityRecipe cRecipe) {
        String currentUser = LoginActivity.currentUser;
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.must_login_favorites), Toast.LENGTH_SHORT).show();
            return;
        }

        SharedPreferences sharedPrefs = getSharedPreferences("FavoritesPrefs", Context.MODE_PRIVATE);
        String key = "favorites_" + currentUser;
        String json = sharedPrefs.getString(key, null);
        Type type = new TypeToken<List<Recipe>>(){}.getType();
        List<Recipe> favorites = json == null ? new ArrayList<>() : new Gson().fromJson(json, type);

        for (Recipe r : favorites) {
            if (r.getTitle().equals(cRecipe.getTitle())) {
                Toast.makeText(this, getString(R.string.already_favorite), Toast.LENGTH_SHORT).show();
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
        sharedPrefs.edit().putString(key, new Gson().toJson(favorites)).apply();
        Toast.makeText(this, getString(R.string.added_favorite), Toast.LENGTH_SHORT).show();
    }

    private void downloadRecipe(CommunityRecipe cRecipe) {
        String body = cRecipe.getTitle() + "\n\n" +
                "Autor: @" + cRecipe.getAuthor() + "\n" +
                "Tiempo: " + cRecipe.getCookingTime() + "\n\n" +
                cRecipe.getDescription();

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, "Receta de Tastel");
        share.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(share, "Compartir / guardar receta"));
    }
}