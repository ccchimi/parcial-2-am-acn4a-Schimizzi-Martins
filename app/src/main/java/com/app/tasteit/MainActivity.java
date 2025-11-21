package com.app.tasteit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;

import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;

public class MainActivity extends AppCompatActivity {

    DrawerLayout drawerLayout;
    NavigationView navigationView;
    ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

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
                Toast.makeText(this, getString(R.string.section_community), Toast.LENGTH_SHORT).show();
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

        // Menú de cuenta
        ImageView ivAccount = findViewById(R.id.ivAccount);
        ivAccount.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(this, ivAccount);
            if(LoginActivity.currentUser == null) menu.getMenu().add("Login");
            else menu.getMenu().add("Logout");

            menu.setOnMenuItemClickListener(item -> {
                if(item.getTitle().equals("Login")) {
                    startActivity(new Intent(this, LoginActivity.class));
                } else {
                    Toast.makeText(this, getString(R.string.session_closed), Toast.LENGTH_SHORT).show();
                    LoginActivity.currentUser = null;
                    startActivity(new Intent(this, LoginActivity.class));
                    finish();
                }
                return true;
            });
            menu.show();
        });

        findViewById(R.id.btnGoRecipes).setOnClickListener(v -> {
            startActivity(new Intent(this, RecipesActivity.class));
        });

        findViewById(R.id.btnGoFav).setOnClickListener(v -> {
            Intent intent = new Intent(this, RecipesActivity.class);
            intent.putExtra("showFavorites", true);
            startActivity(intent);
        });

        findViewById(R.id.btnCommunity).setOnClickListener(v -> {
            Toast.makeText(this, "Comunidad próximamente 🚧", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(toggle != null && toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }
}