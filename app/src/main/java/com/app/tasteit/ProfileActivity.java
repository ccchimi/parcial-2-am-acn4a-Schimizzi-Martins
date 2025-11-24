package com.app.tasteit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.drawerlayout.widget.DrawerLayout;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etUsername, etPassword;
    private RecyclerView rvMyRecipes;

    private SharedPreferences userPrefs;
    private SharedPreferences communityPrefs;
    private Gson gson = new Gson();

    private List<CommunityRecipe> myCommunityRecipes = new ArrayList<>();
    private CommunityRecipeAdapter myAdapter;

    private static final String USERS_PREFS = "UsersPrefs";
    private static final String COMMUNITY_PREFS = "CommunityPrefs";
    private static final String COMMUNITY_KEY = "community_recipes";

    // Drawer
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Toolbar + Drawer
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        drawerLayout = findViewById(R.id.drawerLayout);
        navigationView = findViewById(R.id.navigationView);

        toggle = new ActionBarDrawerToggle(
                this,
                drawerLayout,
                toolbar,
                R.string.app_name,
                R.string.app_name
        );
        drawerLayout.addDrawerListener(toggle);
        toggle.syncState();

        navigationView.setNavigationItemSelectedListener(item -> {
            int id = item.getItemId();

            if (id == R.id.nav_inicio) {
                startActivity(new Intent(this, MainActivity.class));
            } else if (id == R.id.nav_recetas) {
                startActivity(new Intent(this, RecipesActivity.class));
            } else if (id == R.id.nav_comunidad) {
                startActivity(new Intent(this, CommunityActivity.class));
            } else if (id == R.id.nav_favoritos) {
                Intent intent = new Intent(this, RecipesActivity.class);
                intent.putExtra("showFavorites", true);
                startActivity(intent);
            } else if (id == R.id.nav_logout) {
                LoginActivity.currentUser = null;
                Toast.makeText(this,
                        getString(R.string.session_closed),
                        Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, LoginActivity.class));
                finish();
            }

            drawerLayout.closeDrawers();
            return true;
        });

        // Menu de cuenta (popup con Mi perfil / Logout)
        ImageView ivAccount = findViewById(R.id.ivAccount);
        AccountMenuHelper.setup(this, ivAccount);

        // Logica de perfil
        userPrefs = getSharedPreferences(USERS_PREFS, MODE_PRIVATE);
        communityPrefs = getSharedPreferences(COMMUNITY_PREFS, MODE_PRIVATE);

        etFirstName = findViewById(R.id.etProfileFirstName);
        etLastName = findViewById(R.id.etProfileLastName);
        etEmail = findViewById(R.id.etProfileEmail);
        etUsername = findViewById(R.id.etProfileUsername);
        etPassword = findViewById(R.id.etProfilePassword);

        rvMyRecipes = findViewById(R.id.rvMyCommunityRecipes);
        rvMyRecipes.setLayoutManager(new LinearLayoutManager(this));

        Button btnSave = findViewById(R.id.btnSaveProfile);
        Button btnLogout = findViewById(R.id.btnLogout);

        String currentUser = LoginActivity.currentUser;

        if (currentUser == null) {
            Toast.makeText(this, "No hay usuario logueado", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        etUsername.setText(currentUser);

        // Cargar datos guardados de perfil
        loadProfileData(currentUser);

        // Cargar recetas propias de la comunidad
        loadMyCommunityRecipes(currentUser);

        myAdapter = new CommunityRecipeAdapter(this, myCommunityRecipes);
        rvMyRecipes.setAdapter(myAdapter);

        btnSave.setOnClickListener(v -> {
            saveProfileData(currentUser);
            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show();
        });

        btnLogout.setOnClickListener(v -> {
            LoginActivity.logout(this, userPrefs);
            finish();
        });
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }

    private void loadProfileData(String username) {
        String firstName = userPrefs.getString("profile_firstName_" + username, "");
        String lastName = userPrefs.getString("profile_lastName_" + username, "");
        String email = userPrefs.getString("profile_email_" + username, "");
        String password = userPrefs.getString("profile_password_" + username, "");

        etFirstName.setText(firstName);
        etLastName.setText(lastName);
        etEmail.setText(email);
        etPassword.setText(password);
    }

    private void saveProfileData(String username) {
        userPrefs.edit()
                .putString("profile_firstName_" + username,
                        etFirstName.getText().toString().trim())
                .putString("profile_lastName_" + username,
                        etLastName.getText().toString().trim())
                .putString("profile_email_" + username,
                        etEmail.getText().toString().trim())
                .putString("profile_password_" + username,
                        etPassword.getText().toString().trim())
                .apply();
    }

    private void loadMyCommunityRecipes(String username) {
        String json = communityPrefs.getString(COMMUNITY_KEY, null);
        if (json == null) {
            myCommunityRecipes = new ArrayList<>();
            return;
        }

        Type type = new TypeToken<List<CommunityRecipe>>(){}.getType();
        List<CommunityRecipe> all = gson.fromJson(json, type);
        if (all == null) {
            myCommunityRecipes = new ArrayList<>();
            return;
        }

        myCommunityRecipes = new ArrayList<>();
        for (CommunityRecipe r : all) {
            if (r.getAuthor() != null && r.getAuthor().equals(username)) {
                myCommunityRecipes.add(r);
            }
        }
    }
}