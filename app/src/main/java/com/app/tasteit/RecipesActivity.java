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
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class        RecipesActivity extends AppCompatActivity {

    EditText etSearch;
    Button btnSearch;
    LinearLayout categoriesRow;
    RecyclerView rvRecipes;

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    ActionBarDrawerToggle toggle;

    private final String[] categories = {
            "Pastas", "Carnes", "Veggie", "Postres", "Sopas",
            "Arroces", "Ensaladas", "Pescados & Mariscos", "Tapas & Snacks", "Sin TACC"
    };

    // Lista principal que viene de la API
    private List<Recipe> allRecipes = new ArrayList<>();

    private RecipeAdapter adapter;
    private String activeCategory = null;
    private SharedPreferences sharedPrefs;
    private boolean showingFavorites = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipes);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Drawer
        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);
        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        // Menu cuenta
        ImageView ivAccount = findViewById(R.id.ivAccount);
        AccountMenuHelper.setup(this, ivAccount);

        sharedPrefs = getSharedPreferences("FavoritesPrefs", Context.MODE_PRIVATE);

        showingFavorites = getIntent().getBooleanExtra("showFavorites", false);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, MainActivity.class));
            }
            else if (id == R.id.nav_recetas) {
                showingFavorites = false;
                if (allRecipes.isEmpty()) {
                    loadRecipesFromApi();
                } else {
                    adapter.setRecipes(getAllRecipes());
                    createCategoryButtons();
                }
            }
            else if (id == R.id.nav_comunidad) {
                startActivity(new Intent(this, CommunityActivity.class));
            }
            else if (id == R.id.nav_favoritos) {
                showingFavorites = true;
                loadFavorites();
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

        etSearch = findViewById(R.id.etSearch);
        btnSearch = findViewById(R.id.btnSearch);
        categoriesRow = findViewById(R.id.categoriesLayout);
        rvRecipes = findViewById(R.id.rvRecipes);

        rvRecipes.setLayoutManager(new LinearLayoutManager(this));
        adapter = new RecipeAdapter(this, new ArrayList<>(), showingFavorites);
        rvRecipes.setAdapter(adapter);

        if (showingFavorites) {
            loadFavorites();
        } else {
            loadRecipesFromApi();
        }

        btnSearch.setOnClickListener(v -> {
            String q = etSearch.getText().toString().trim().toLowerCase();
            if (q.isEmpty()) {
                adapter.setRecipes(showingFavorites ? getFavoriteRecipes() : getAllRecipes());
            } else {
                adapter.setRecipes(searchRecipes(q));
            }
        });
    }

    // Cargar recetas desde la API (Retrofit)
    private void loadRecipesFromApi() {
        RecipesApiService api = RetrofitClient.getApiService();

        api.getRecipes().enqueue(new Callback<List<Recipe>>() {
            @Override
            public void onResponse(Call<List<Recipe>> call, Response<List<Recipe>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    allRecipes = response.body();
                    adapter.setRecipes(getAllRecipes());
                    createCategoryButtons();
                } else {
                    Toast.makeText(RecipesActivity.this,
                            getString(R.string.error_loading_recipes),
                            Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<Recipe>> call, Throwable t) {
                t.printStackTrace();
                Toast.makeText(RecipesActivity.this,
                        getString(R.string.error_loading_recipes),
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private List<Recipe> getAllRecipes() {
        return new ArrayList<>(allRecipes);
    }

    private List<Recipe> searchRecipes(String q) {
        List<Recipe> list = new ArrayList<>();
        List<Recipe> source = showingFavorites ? getFavoriteRecipes() : allRecipes;

        for (Recipe r : source) {
            if (r.getTitle() != null &&
                    r.getTitle().toLowerCase().contains(q)) {
                list.add(r);
            }
        }
        return list;
    }

    private List<Recipe> filterByCategory(String cat) {
        List<Recipe> list = new ArrayList<>();
        for (Recipe r : allRecipes) {
            if (r.getCategory() != null && r.getCategory().equals(cat)) {
                list.add(r);
            }
        }
        return list;
    }

    private List<Recipe> getFavoriteRecipes() {
        String currentUser = LoginActivity.currentUser;
        if (currentUser == null) return new ArrayList<>();
        String key = "favorites_" + currentUser;
        String json = sharedPrefs.getString(key, null);
        Type type = new TypeToken<List<Recipe>>() {}.getType();
        return json == null ? new ArrayList<>() : new Gson().fromJson(json, type);
    }

    private void createCategoryButtons() {
        categoriesRow.removeAllViews();
        if (showingFavorites) return;

        for (String cat : categories) {
            Button b = new Button(this);
            b.setText(cat);
            b.setAllCaps(false);
            b.setOnClickListener(v -> {
                if (cat.equals(activeCategory)) {
                    activeCategory = null;
                    adapter.setRecipes(getAllRecipes());
                } else {
                    activeCategory = cat;
                    adapter.setRecipes(filterByCategory(cat));
                }
            });
            categoriesRow.addView(b);
        }
    }

    private void loadFavorites() {
        List<Recipe> favorites = getFavoriteRecipes();
        if (favorites.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_favorites), Toast.LENGTH_SHORT).show();
        }
        adapter.setRecipes(favorites);
        createCategoryButtons();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }
}
