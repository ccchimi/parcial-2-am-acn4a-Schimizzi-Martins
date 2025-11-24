package com.app.tasteit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CommunityActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    ActionBarDrawerToggle toggle;

    RecyclerView rvCommunity;
    FloatingActionButton fabAddRecipe;

    private SharedPreferences communityPrefs;
    private Gson gson = new Gson();
    private List<CommunityRecipe> communityRecipes = new ArrayList<>();
    private CommunityRecipeAdapter adapter;

    private static final String PREFS_NAME = "CommunityPrefs";
    private static final String COMMUNITY_KEY = "community_recipes";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        // Toolbar + Drawer
        ImageView ivAccount = findViewById(R.id.ivAccount);
        AccountMenuHelper.setup(this, ivAccount);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, MainActivity.class));
            }
            else if (id == R.id.nav_recetas) {
                startActivity(new Intent(this, RecipesActivity.class));
            }
            else if (id == R.id.nav_comunidad) {
                drawerLayout.closeDrawers();
            }
            else if (id == R.id.nav_favoritos) {
                Intent intent = new Intent(this, RecipesActivity.class);
                intent.putExtra("showFavorites", true);
                startActivity(intent);
            }
            else if (id == R.id.nav_logout) {
                LoginActivity.currentUser = null;
                Toast.makeText(this, getString(R.string.session_closed), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }

            drawerLayout.closeDrawers();
            return true;
        });

        communityPrefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        rvCommunity = findViewById(R.id.rvCommunity);
        rvCommunity.setLayoutManager(new LinearLayoutManager(this));

        loadCommunityRecipes();
        adapter = new CommunityRecipeAdapter(this, communityRecipes);
        rvCommunity.setAdapter(adapter);

        fabAddRecipe = findViewById(R.id.fabAddRecipe);
        fabAddRecipe.setOnClickListener(v -> showCreateRecipeDialog());
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Por si se editaron recetas en el detalle
        loadCommunityRecipes();
        if (adapter != null) {
            adapter.setRecipes(communityRecipes);
        }
    }

    private void loadCommunityRecipes() {
        String json = communityPrefs.getString(COMMUNITY_KEY, null);
        Type type = new TypeToken<List<CommunityRecipe>>(){}.getType();

        if (json == null) {
            communityRecipes = getDefaultCommunityRecipes();
            saveCommunityRecipes();
        } else {
            communityRecipes = gson.fromJson(json, type);
            if (communityRecipes == null) {
                communityRecipes = new ArrayList<>();
            }
        }
    }

    private void saveCommunityRecipes() {
        communityPrefs.edit()
                .putString(COMMUNITY_KEY, gson.toJson(communityRecipes))
                .apply();
    }

    private List<CommunityRecipe> getDefaultCommunityRecipes() {
        List<CommunityRecipe> list = new ArrayList<>();

        list.add(new CommunityRecipe(
                "Tostadas francesas rellenas",
                "Pan brioche relleno con dulce de leche y bañado en huevo, leche y canela. Ideal para un brunch potente.",
                "https://images.pexels.com/photos/4109990/pexels-photo-4109990.jpeg",
                "20 min",
                "Usuario prueba 1"
        ));

        list.add(new CommunityRecipe(
                "Bowl veggie de garbanzos",
                "Garbanzos especiados al horno, mix de hojas verdes, palta, tomate cherry y aderezo de tahini.",
                "https://images.pexels.com/photos/1640770/pexels-photo-1640770.jpeg",
                "25 min",
                "Usuario prueba 2"
        ));

        list.add(new CommunityRecipe(
                "Pasta cremosa de una sola olla",
                "Fideos, caldo, crema y parmesano: todo en una sola olla, sin colar. Súper rápida para los días apurados.",
                "https://images.pexels.com/photos/6287521/pexels-photo-6287521.jpeg",
                "30 min",
                "admin (editable)"
        ));

        return list;
    }

    private void showCreateRecipeDialog() {
        String currentUser = LoginActivity.currentUser;
        if (currentUser == null) {
            Toast.makeText(this, getString(R.string.must_login_favorites), Toast.LENGTH_SHORT).show();
            return;
        }

        CreateCommunityRecipeDialog dialog = new CreateCommunityRecipeDialog(
                this,
                (title, description, imageUrl, time) -> {

                    CommunityRecipe recipe = new CommunityRecipe(
                            title,
                            description,
                            imageUrl,
                            time,
                            currentUser
                    );
                    communityRecipes.add(0, recipe);
                    saveCommunityRecipes();
                    adapter.setRecipes(communityRecipes);
                    Toast.makeText(this, "Receta publicada en la comunidad", Toast.LENGTH_SHORT).show();
                }
        );
        dialog.show();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }
}