package com.app.tasteit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.android.material.navigation.NavigationView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    ActionBarDrawerToggle toggle;

    RecyclerView rvHighlights;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar + Drawer
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        toggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
                R.string.app_name, R.string.app_name);
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                drawerLayout.closeDrawers();
            }
            else if (id == R.id.nav_recetas) {
                startActivity(new Intent(this, RecipesActivity.class));
            }
            else if (id == R.id.nav_comunidad) {
                startActivity(new Intent(this, CommunityActivity.class));
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

        // Menu de cuenta (icono arriba a la derecha)
        ImageView ivAccount = findViewById(R.id.ivAccount);
        AccountMenuHelper.setup(this, ivAccount);

        // Botones de accesos rápidos
        findViewById(R.id.btnGoRecipes).setOnClickListener(v ->
                startActivity(new Intent(this, RecipesActivity.class))
        );

        findViewById(R.id.btnGoFav).setOnClickListener(v -> {
            Intent intent = new Intent(this, RecipesActivity.class);
            intent.putExtra("showFavorites", true);
            startActivity(intent);
        });

        findViewById(R.id.btnCommunity).setOnClickListener(v ->
                startActivity(new Intent(this, CommunityActivity.class))
        );

        // HERO
        ImageView heroImage = findViewById(R.id.heroImage);
        Glide.with(this)
                .load("https://images.pexels.com/photos/6287521/pexels-photo-6287521.jpeg")
                .into(heroImage);

        // DESTACADOS
        rvHighlights = findViewById(R.id.rvHighlights);
        rvHighlights.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)
        );

        List<HomeHighlight> highlights = new ArrayList<>();
        highlights.add(new HomeHighlight(
                "Clásicos para todos los días",
                "Pastas, guisos y platos fáciles para el día a día.",
                "https://images.pexels.com/photos/1437267/pexels-photo-1437267.jpeg"
        ));
        highlights.add(new HomeHighlight(
                "Recetas para invitar",
                "Ideas para sorprender cuando tenés gente en casa.",
                "https://images.pexels.com/photos/4099235/pexels-photo-4099235.jpeg"
        ));
        highlights.add(new HomeHighlight(
                "Postres que no fallan",
                "Tiramisú, budines y algo dulce para cerrar.",
                "https://pekis.net/sites/default/files/styles/social_share_1200/public/2025-04/tiramisu.jpg?itok=lVwjUayR"
        ));

        HomeHighlightAdapter highlightAdapter = new HomeHighlightAdapter(this, highlights);
        rvHighlights.setAdapter(highlightAdapter);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }
}
