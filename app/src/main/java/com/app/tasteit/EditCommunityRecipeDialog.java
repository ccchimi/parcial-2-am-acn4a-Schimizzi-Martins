package com.app.tasteit;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;

public class EditCommunityRecipeDialog {

    public interface OnRecipeUpdatedListener {
        void onUpdated(CommunityRecipe updated);
    }

    private final Context context;
    private final CommunityRecipe recipe;
    private final OnRecipeUpdatedListener listener;

    public EditCommunityRecipeDialog(Context context, CommunityRecipe recipe, OnRecipeUpdatedListener listener) {
        this.context = context;
        this.recipe = recipe;
        this.listener = listener;
    }

    public void show() {
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_community_recipe, null);

        EditText etTitle = view.findViewById(R.id.etTitle);
        EditText etDescription = view.findViewById(R.id.etDescription);
        EditText etImageUrl = view.findViewById(R.id.etImageUrl);
        EditText etTime = view.findViewById(R.id.etTime);

        etTitle.setText(recipe.getTitle());
        etDescription.setText(recipe.getDescription());
        etImageUrl.setText(recipe.getImageUrl());
        etTime.setText(recipe.getCookingTime());

        new AlertDialog.Builder(context)
                .setTitle("Editar receta")
                .setView(view)
                .setPositiveButton("Guardar cambios", (dialog, which) -> {
                    String title = etTitle.getText().toString().trim();
                    String desc = etDescription.getText().toString().trim();
                    String url = etImageUrl.getText().toString().trim();
                    String time = etTime.getText().toString().trim();

                    if (title.isEmpty() || desc.isEmpty()) {
                        Toast.makeText(context, "Título y descripción son obligatorios", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    if (time.isEmpty()) time = "Sin especificar";

                    recipe.setTitle(title);
                    recipe.setDescription(desc);
                    recipe.setImageUrl(url);
                    recipe.setCookingTime(time);

                    listener.onUpdated(recipe);
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }
}
