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
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.material.navigation.NavigationView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseAuthException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etEmail, etUsername, etPassword;
    private RecyclerView rvMyRecipes;

    private List<CommunityRecipe> myCommunityRecipes = new ArrayList<>();
    private CommunityRecipeAdapter myAdapter;

    // Drawer
    private DrawerLayout drawerLayout;
    private NavigationView navigationView;
    private ActionBarDrawerToggle toggle;

    // Firebase
    private FirebaseAuth auth;
    private FirebaseUser firebaseUser;
    private FirebaseFirestore db;

    private String currentEmail = "";
    private String userDocId = null; // id del doc en "usuarios"

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // ---- Firebase ----
        auth = FirebaseAuth.getInstance();
        firebaseUser = auth.getCurrentUser();
        db = FirebaseFirestore.getInstance();

        if (firebaseUser == null) {
            startActivity(new Intent(this, LoginActivity.class));
            finish();
            return;
        }

        if (firebaseUser.getEmail() != null) {
            currentEmail = firebaseUser.getEmail();
        }

        // ---- Toolbar + Drawer ----
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
                LoginActivity.logout(this);
            }

            drawerLayout.closeDrawers();
            return true;
        });

        // ---- Menu de cuenta (popup) ----
        ImageView ivAccount = findViewById(R.id.ivAccount);
        AccountMenuHelper.setup(this, ivAccount);

        // ---- Views de perfil ----
        etFirstName = findViewById(R.id.etProfileFirstName);
        etLastName  = findViewById(R.id.etProfileLastName);
        etEmail     = findViewById(R.id.etProfileEmail);
        etUsername  = findViewById(R.id.etProfileUsername);
        etPassword  = findViewById(R.id.etProfilePassword);

        // username NO editable
        etUsername.setEnabled(false);

        rvMyRecipes = findViewById(R.id.rvMyCommunityRecipes);
        rvMyRecipes.setLayoutManager(new LinearLayoutManager(this));

        Button btnSave   = findViewById(R.id.btnSaveProfile);
        Button btnLogout = findViewById(R.id.btnLogout);

        etEmail.setText(currentEmail);

        // Adapter (usa el click interno para detalle/favs)
        myAdapter = new CommunityRecipeAdapter(this, myCommunityRecipes);
        rvMyRecipes.setAdapter(myAdapter);

        // Cargar datos del usuario desde Firestore
        loadUserProfileFromFirestore();

        // Cargar recetas del usuario
        listenMyCommunityRecipes();

        btnSave.setOnClickListener(v -> saveProfile());
        btnLogout.setOnClickListener(v -> LoginActivity.logout(this));
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull android.view.MenuItem item) {
        if (toggle != null && toggle.onOptionsItemSelected(item)) return true;
        return super.onOptionsItemSelected(item);
    }

    private void loadUserProfileFromFirestore() {
        // Buscamos el doc del usuario por email
        db.collection("usuarios")
                .whereEqualTo("email", currentEmail)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        userDocId = doc.getId();

                        String firstName = doc.getString("firstName");
                        String lastName  = doc.getString("lastName");
                        String email     = doc.getString("email");
                        String username  = doc.getString("username");

                        etFirstName.setText(firstName != null ? firstName : "");
                        etLastName.setText(lastName != null ? lastName : "");
                        etEmail.setText(email != null ? email : currentEmail);

                        // username fijo
                        if (username != null && !username.isEmpty()) {
                            etUsername.setText(username);
                        } else if (LoginActivity.currentUsername != null) {
                            etUsername.setText(LoginActivity.currentUsername);
                        }
                    } else {
                        // Si no hay doc, al menos mostramos lo básico
                        etEmail.setText(currentEmail);
                        if (LoginActivity.currentUsername != null) {
                            etUsername.setText(LoginActivity.currentUsername);
                        }
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error al cargar perfil: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void listenMyCommunityRecipes() {
        String uid = firebaseUser.getUid();

        db.collection("usuarios")
                .document(uid)
                .collection("recetas")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Toast.makeText(this,
                                "Error al cargar tus recetas: " + error.getMessage(),
                                Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (value == null) return;

                    myCommunityRecipes.clear();

                    value.getDocuments().forEach(doc -> {
                        CommunityRecipe r = doc.toObject(CommunityRecipe.class);
                        if (r != null) {
                            r.setId(doc.getId());
                            myCommunityRecipes.add(r);
                        }
                    });

                    myAdapter.setRecipes(myCommunityRecipes);
                });
    }

    private void saveProfile() {
        String newFirstName = etFirstName.getText().toString().trim();
        String newLastName  = etLastName.getText().toString().trim();
        String newEmail     = etEmail.getText().toString().trim();
        String newPassword  = etPassword.getText().toString().trim();

        if (newEmail.isEmpty()) {
            etEmail.setError("El email no puede estar vacío");
            return;
        }

        FirebaseUser user = firebaseUser;

        // 1) CAMBIO DE EMAIL – ahora con verifyBeforeUpdateEmail
        if (!newEmail.equals(currentEmail)) {

            user.verifyBeforeUpdateEmail(newEmail)
                    .addOnSuccessListener(v -> {
                        // el email en Auth se actualizara recién cuando
                        // el usuario confirme el link del correo (que le llegara a su mail real)
                        currentEmail = newEmail;

                        Toast.makeText(this,
                                "Te enviamos un mail a " + newEmail +
                                        " para confirmar el cambio de email.",
                                Toast.LENGTH_LONG).show();
                        updateUserDocument(newFirstName, newLastName, newEmail);
                    })
                    .addOnFailureListener(e -> {
                        String msg = "No se pudo iniciar el cambio de email: " + e.getMessage();
                        Toast.makeText(this, msg, Toast.LENGTH_LONG).show();
                    });

        } else {
            // Email no cambio, solo actualizamos nombre/apellido en Firestore
            updateUserDocument(newFirstName, newLastName, newEmail);
        }

        // 2) CAMBIO DE CONTRASEÑA
        if (!newPassword.isEmpty()) {
            user.updatePassword(newPassword)
                    .addOnSuccessListener(v ->
                            Toast.makeText(this,
                                    "Contraseña actualizada",
                                    Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Error al actualizar contraseña: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void updateUserDocument(String newFirstName, String newLastName, String newEmail) {
        if (userDocId != null) {
            Map<String, Object> updates = new HashMap<>();
            updates.put("firstName", newFirstName);
            updates.put("lastName", newLastName);
            updates.put("email", newEmail);

            db.collection("usuarios")
                    .document(userDocId)
                    .update(updates)
                    .addOnSuccessListener(v ->
                            Toast.makeText(this, "Perfil actualizado", Toast.LENGTH_SHORT).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Error al actualizar perfil: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
        } else {
            Toast.makeText(this,
                    "No se encontró documento de usuario en Firestore",
                    Toast.LENGTH_SHORT).show();
        }
    }
}
