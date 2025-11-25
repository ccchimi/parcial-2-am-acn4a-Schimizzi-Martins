package com.app.tasteit;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class CommunityRecipeDetailActivity extends AppCompatActivity {

    private ImageView detailImage;
    private TextView detailTitle, detailAuthor, detailTime, detailDescription;
    private Button btnFav, btnDownload, btnEdit, btnBack, btnDelete;

    private CommunityRecipe recipe;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private Gson gson = new Gson();

    // Necesario para borrar en Firestore
    private String recipeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_community_recipe_detail);

        // Toolbar + menu de cuenta
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
        btnDelete = findViewById(R.id.btnDelete);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        // ID de la receta que viene desde CommunityActivity / adapter
        recipeId = getIntent().getStringExtra("recipe_id");
        if (recipeId == null || recipeId.isEmpty()) {
            finish();
            return;
        }

        // Ocultamos el boton de borrar hasta saber si es del dueño
        btnDelete.setVisibility(View.GONE);

        loadRecipeFromFirestore(recipeId);

        btnBack.setOnClickListener(v -> finish());

        btnFav.setOnClickListener(v -> {
            if (recipe != null) {
                addToFavoritesFromCommunity(recipe);
            }
        });

        btnDownload.setOnClickListener(v -> {
            if (recipe != null) {
                downloadRecipe(recipe);
            }
        });

        btnEdit.setOnClickListener(v -> {
            if (recipe == null) return;
            if (auth.getCurrentUser() == null) return;

            String uid = auth.getCurrentUser().getUid();
            if (recipe.getAuthorId() != null && recipe.getAuthorId().equals(uid)) {
                Intent i = new Intent(this, RecipeFormActivity.class);
                i.putExtra("recipeId", recipe.getId());
                startActivity(i);
            } else {
                Toast.makeText(this,
                        "No podés editar esta receta (no sos el dueño).",
                        Toast.LENGTH_SHORT).show();
            }
        });

        btnDelete.setOnClickListener(v -> {
            if (recipe == null || auth.getCurrentUser() == null) return;

            String uid = auth.getCurrentUser().getUid();
            if (recipe.getAuthorId() != null && recipe.getAuthorId().equals(uid)) {
                confirmDelete();
            } else {
                Toast.makeText(this,
                        "No podés eliminar esta receta (no sos el dueño).",
                        Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadRecipeFromFirestore(String recipeId) {
        db.collection("comunidad")
                .document(recipeId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) {
                        Toast.makeText(this, "La receta no existe", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }

                    recipe = doc.toObject(CommunityRecipe.class);
                    if (recipe == null) {
                        Toast.makeText(this, "Error al leer la receta", Toast.LENGTH_SHORT).show();
                        finish();
                        return;
                    }
                    recipe.setId(doc.getId());

                    bindData();
                    updateButtonsState();
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(this,
                            "Error al cargar la receta: " + e.getMessage(),
                            Toast.LENGTH_SHORT).show();
                    finish();
                });
    }

    private void bindData() {
        detailTitle.setText(recipe.getTitle());
        detailAuthor.setText("Por @" + recipe.getAuthor());
        detailTime.setText("Tiempo: " + recipe.getCookingTime() + " min");
        detailDescription.setText(recipe.getDescription());

        Glide.with(this)
                .load(recipe.getImageUrl())
                .placeholder(R.drawable.tastel)
                .error(R.drawable.tastel)
                .into(detailImage);
    }

    private void updateButtonsState() {
        if (auth.getCurrentUser() == null || recipe == null) {
            btnEdit.setEnabled(false);
            btnEdit.setAlpha(0.4f);
            btnDelete.setVisibility(View.GONE);
            return;
        }

        String uid = auth.getCurrentUser().getUid();
        boolean isOwner = recipe.getAuthorId() != null && recipe.getAuthorId().equals(uid);

        if (isOwner) {
            btnEdit.setEnabled(true);
            btnEdit.setAlpha(1f);
            btnDelete.setVisibility(View.VISIBLE);
        } else {
            btnEdit.setEnabled(false);
            btnEdit.setAlpha(0.4f);
            btnDelete.setVisibility(View.GONE);
        }
    }

    // ----- Confirmacion y borrado -----

    private void confirmDelete() {
        new AlertDialog.Builder(this)
                .setTitle("Eliminar receta")
                .setMessage("¿Seguro que querés eliminar esta receta?")
                .setPositiveButton("Eliminar", (dialog, which) -> deleteRecipe())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteRecipe() {
        if (auth.getCurrentUser() == null || recipeId == null) return;

        String uid = auth.getCurrentUser().getUid();

        // Primero borramos de comunidad
        db.collection("comunidad")
                .document(recipeId)
                .delete()
                .addOnSuccessListener(v -> {
                    // Luego borramos de usuarios/{uid}/recetas
                    db.collection("usuarios")
                            .document(uid)
                            .collection("recetas")
                            .document(recipeId)
                            .delete();

                    Toast.makeText(this, "Receta eliminada", Toast.LENGTH_SHORT).show();
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error al eliminar receta: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    // ----- Favoritos -----

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
        List<Recipe> favorites = json == null ? new ArrayList<>() : gson.fromJson(json, type);

        for (Recipe r : favorites) {
            if (r.getTitle().equals(cRecipe.getTitle())
                    && "Comunidad".equals(r.getCategory())) {
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
        sharedPrefs.edit().putString(key, gson.toJson(favorites)).apply();
        Toast.makeText(this, getString(R.string.added_favorite), Toast.LENGTH_SHORT).show();
    }

    private void downloadRecipe(CommunityRecipe cRecipe) {
        String body = cRecipe.getTitle() + "\n\n" +
                "Autor: @" + cRecipe.getAuthor() + "\n" +
                "Tiempo: " + cRecipe.getCookingTime() + " min\n\n" +
                cRecipe.getDescription();

        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, "Receta de Tastel");
        share.putExtra(Intent.EXTRA_TEXT, body);
        startActivity(Intent.createChooser(share, "Compartir / guardar receta"));
    }
}
