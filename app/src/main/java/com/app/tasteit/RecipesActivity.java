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

public class RecipesActivity extends AppCompatActivity {

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

    private final Object[][] recipesData = {
            {"Spaghetti Bolognesa","Pastas","Clásica pasta italiana con salsa de carne y tomate.",
                    "https://cdn.stoneline.de/media/c5/63/4f/1727429313/spaghetti-bolognese.jpeg?ts=1727429313","30 min"},
            {"Fettuccine Alfredo","Pastas","Crema, manteca y parmesano para una salsa sedosa.",
                    "https://www.thespruceeats.com/thmb/gTjo1gnOuBEVJsttgDW2JljvKY0=/1500x0/filters:no_upscale():max_bytes(150000):strip_icc()/shrimp-fettuccine-alfredo-recipe-5205738-hero-01-1a40571b0e3e4a17ab768b4d700c7836.jpg","25 min"},
            {"Lasagna de Verduras","Pastas","Capas de vegetales asados y bechamel.",
                    "https://www.bekiacocina.com/images/cocina/0000/976-h.jpg","40 min"},
            {"Pollo al horno","Carnes","Jugoso pollo al horno con especias.",
                    "https://i.ytimg.com/vi/DBgGtCWWD5Q/maxresdefault.jpg","50 min"},
            {"Asado clásico","Carnes","Costillar con chimichurri y fuego lento.",
                    "https://www.res.com.ar/media/catalog/product/cache/6c63de560a15562fe08de38c3c766637/a/s/asado_clasico_l.jpg","2 hs"},
            {"Ensalada César","Ensaladas","Lechuga, pollo, crutones y aderezo César.",
                    "https://www.goodnes.com/sites/g/files/jgfbjl321/files/srh_recipes/755f697272cbcdc6e5df2adb44b1b705.jpg","15 min"}
    };

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

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar, R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        sharedPrefs = getSharedPreferences("FavoritesPrefs", Context.MODE_PRIVATE);

        showingFavorites = getIntent().getBooleanExtra("showFavorites", false);

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, MainActivity.class));
            }
            else if (id == R.id.nav_recetas) {
                showingFavorites = false;
                adapter.setRecipes(getAllRecipes());
                createCategoryButtons();
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
        adapter = new RecipeAdapter(this, showingFavorites ? getFavoriteRecipes() : getAllRecipes(), showingFavorites);
        rvRecipes.setAdapter(adapter);

        createCategoryButtons();

        btnSearch.setOnClickListener(v -> {
            String q = etSearch.getText().toString().trim().toLowerCase();
            if (q.isEmpty()) adapter.setRecipes(showingFavorites ? getFavoriteRecipes() : getAllRecipes());
            else adapter.setRecipes(searchRecipes(q));
        });
    }

    private List<Recipe> getAllRecipes() {
        List<Recipe> list = new ArrayList<>();
        for (Object[] data : recipesData) {
            list.add(new Recipe((String) data[0], (String) data[2], (String) data[3], (String) data[4]));
        }
        return list;
    }

    private List<Recipe> searchRecipes(String q) {
        List<Recipe> list = new ArrayList<>();
        for (Object[] data : recipesData) {
            if(((String)data[0]).toLowerCase().contains(q)) {
                list.add(new Recipe((String)data[0], (String)data[2], (String)data[3], (String)data[4]));
            }
        }
        return list;
    }

    private List<Recipe> filterByCategory(String cat) {
        List<Recipe> list = new ArrayList<>();
        for (Object[] data : recipesData) {
            if(((String)data[1]).equals(cat)) {
                list.add(new Recipe((String)data[0], (String)data[2], (String)data[3], (String)data[4]));
            }
        }
        return list;
    }

    private List<Recipe> getFavoriteRecipes() {
        String currentUser = LoginActivity.currentUser;
        if(currentUser == null) return new ArrayList<>();
        String key = "favorites_" + currentUser;
        String json = sharedPrefs.getString(key, null);
        Type type = new TypeToken<List<Recipe>>(){}.getType();
        return json == null ? new ArrayList<>() : new Gson().fromJson(json, type);
    }

    private void createCategoryButtons() {
        categoriesRow.removeAllViews();
        if(showingFavorites) return;

        for(String cat : categories) {
            Button b = new Button(this);
            b.setText(cat);
            b.setAllCaps(false);
            b.setOnClickListener(v -> {
                if(cat.equals(activeCategory)) {
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
        if(favorites.isEmpty()) {
            Toast.makeText(this, getString(R.string.no_favorites), Toast.LENGTH_SHORT).show();
        }
        adapter.setRecipes(favorites);
        createCategoryButtons();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(toggle != null && toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }
}