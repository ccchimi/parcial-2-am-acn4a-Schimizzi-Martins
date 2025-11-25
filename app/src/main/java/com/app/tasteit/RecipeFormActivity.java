package com.app.tasteit;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RecipeFormActivity extends AppCompatActivity {

    private EditText etTitle, etDesc, etImage;
    private NumberPicker npTime;
    private Button btnSave, btnDelete;

    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private String recipeId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_form);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        etTitle = findViewById(R.id.etTitle);
        etDesc  = findViewById(R.id.etDescription);
        etImage = findViewById(R.id.etImageUrl);
        npTime  = findViewById(R.id.npCookingTime);
        btnSave = findViewById(R.id.btnSaveRecipe);
        btnDelete = findViewById(R.id.btnDeleteRecipe);

        // Configuracion del NumberPicker
        npTime.setMinValue(1);
        npTime.setMaxValue(300);
        npTime.setWrapSelectorWheel(true);

        // Ver si estamos editando
        recipeId = getIntent().getStringExtra("recipeId");

        if (recipeId != null) {
            loadRecipe();
            btnDelete.setVisibility(Button.VISIBLE);
            btnDelete.setOnClickListener(v -> confirmDelete());
        } else {
            // Si es nueva receta, ocultamos el boton de eliminar
            btnDelete.setVisibility(Button.GONE);
        }

        btnSave.setOnClickListener(v -> saveRecipe());
    }

    private void loadRecipe() {
        if (recipeId == null) return;

        db.collection("comunidad")
                .document(recipeId)
                .get()
                .addOnSuccessListener(doc -> {
                    if (!doc.exists()) return;

                    etTitle.setText(doc.getString("title"));
                    etDesc.setText(doc.getString("description"));
                    etImage.setText(doc.getString("imageUrl"));

                    String ct = doc.getString("cookingTime");
                    int minutes = 20;

                    if (ct != null) {
                        ct = ct.replaceAll("[^0-9]", "");
                        try {
                            minutes = Integer.parseInt(ct);
                        } catch (NumberFormatException ignored) {}
                    }

                    if (minutes < 1) minutes = 1;
                    if (minutes > 300) minutes = 300;
                    npTime.setValue(minutes);
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this,
                                "Error al cargar la receta: " + e.getMessage(),
                                Toast.LENGTH_SHORT).show()
                );
    }

    private void saveRecipe() {

        String title = etTitle.getText().toString().trim();
        String desc  = etDesc.getText().toString().trim();
        String img   = etImage.getText().toString().trim();
        int minutes  = npTime.getValue();

        if (title.isEmpty()) { etTitle.setError("Título requerido"); return; }
        if (desc.isEmpty())  { etDesc.setError("Descripción requerida"); return; }

        if (auth.getCurrentUser() == null) {
            Toast.makeText(this, "Debés iniciar sesión para publicar", Toast.LENGTH_SHORT).show();
            return;
        }

        String uid   = auth.getCurrentUser().getUid();
        String email = auth.getCurrentUser().getEmail();

        Map<String,Object> data = new HashMap<>();
        data.put("title", title);
        data.put("description", desc);
        data.put("imageUrl", img);
        data.put("cookingTime", String.valueOf(minutes));

        // Autor visible: username si lo tenemos, si no, algo generico
        String visibleAuthor = LoginActivity.currentUsername;
        if (visibleAuthor == null || visibleAuthor.isEmpty()) {
            visibleAuthor = "usuario";
        }

        data.put("author", visibleAuthor);
        data.put("authorId", uid);
        data.put("authorEmail", email);

        if (recipeId == null) {
            // CREAR
            data.put("createdAt", System.currentTimeMillis());

            db.collection("comunidad")
                    .add(data)
                    .addOnSuccessListener(docRef -> {

                        String newId = docRef.getId();

                        // guardamos el ID dentro del documento
                        docRef.update("id", newId);

                        // guardar tambien en usuarios/{uid}/recetas
                        db.collection("usuarios")
                                .document(uid)
                                .collection("recetas")
                                .document(newId)
                                .set(data);

                        Toast.makeText(this, "Receta creada", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Error al crear receta: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );

        } else {
            // EDITAR – Solo si es dueño
            db.collection("comunidad")
                    .document(recipeId)
                    .get()
                    .addOnSuccessListener(doc -> {
                        if (!doc.exists()) {
                            Toast.makeText(this, "La receta ya no existe", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String owner = doc.getString("authorId");

                        if (owner == null || !owner.equals(uid)) {
                            Toast.makeText(this,
                                    "No podés editar esta receta (no sos el dueño).",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        // actualizar ambos lugares
                        db.collection("comunidad")
                                .document(recipeId)
                                .update(data);

                        db.collection("usuarios")
                                .document(uid)
                                .collection("recetas")
                                .document(recipeId)
                                .update(data);

                        Toast.makeText(this, "Receta actualizada", Toast.LENGTH_SHORT).show();
                        finish();
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Error al verificar dueño: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void confirmDelete() {
        if (recipeId == null || auth.getCurrentUser() == null) return;

        new AlertDialog.Builder(this)
                .setTitle("Eliminar receta")
                .setMessage("¿Seguro que querés eliminar esta receta?")
                .setPositiveButton("Eliminar", (dialog, which) -> deleteRecipe())
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private void deleteRecipe() {
        String uid = auth.getCurrentUser().getUid();

        // Borrar de comunidad
        db.collection("comunidad")
                .document(recipeId)
                .delete();

        // Borrar de usuarios/{uid}/recetas
        db.collection("usuarios")
                .document(uid)
                .collection("recetas")
                .document(recipeId)
                .delete();

        Toast.makeText(this, "Receta eliminada", Toast.LENGTH_SHORT).show();
        finish();
    }
}
