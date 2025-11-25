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
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class CommunityActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    ActionBarDrawerToggle toggle;

    RecyclerView rvCommunity;
    FloatingActionButton fabAddRecipe;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private List<CommunityRecipe> communityRecipes = new ArrayList<>();
    private CommunityRecipeAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community);

        // Firebase
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // Toolbar
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Menu de cuenta (icono)
        ImageView ivAccount = findViewById(R.id.ivAccount);
        AccountMenuHelper.setup(this, ivAccount);

        // Drawer
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
            } else if (id == R.id.nav_recetas) {
                startActivity(new Intent(this, RecipesActivity.class));
            } else if (id == R.id.nav_comunidad) {
                drawerLayout.closeDrawers();
            } else if (id == R.id.nav_favoritos) {
                Intent intent = new Intent(this, RecipesActivity.class);
                intent.putExtra("showFavorites", true);
                startActivity(intent);
            } else if (id == R.id.nav_logout) {
                LoginActivity.logout(this);
            }

            drawerLayout.closeDrawers();
            return true;
        });

        // Recycler
        rvCommunity = findViewById(R.id.rvCommunity);
        rvCommunity.setLayoutManager(new LinearLayoutManager(this));

        adapter = new CommunityRecipeAdapter(this, communityRecipes);
        rvCommunity.setAdapter(adapter);

        // FAB: crear nueva receta
        fabAddRecipe = findViewById(R.id.fabAddRecipe);
        fabAddRecipe.setOnClickListener(v -> {
            if (auth.getCurrentUser() == null) {
                Toast.makeText(this, getString(R.string.must_login_favorites), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                return;
            }

            Intent i = new Intent(this, RecipeFormActivity.class);
            startActivity(i);
        });

        // Escuchar recetas de la comunidad
        listenCommunityRecipes();
    }

    private void listenCommunityRecipes() {
        db.collection("comunidad")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this,
                                "Error al cargar comunidad: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value == null) return;

                    communityRecipes.clear();
                    value.getDocuments().forEach(doc -> {
                        CommunityRecipe r = doc.toObject(CommunityRecipe.class);
                        if (r != null) {
                            r.setId(doc.getId());
                            communityRecipes.add(r);
                        }
                    });

                    adapter.setRecipes(communityRecipes);
                });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }
}
