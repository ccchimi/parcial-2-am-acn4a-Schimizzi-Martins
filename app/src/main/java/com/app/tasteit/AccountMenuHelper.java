package com.app.tasteit;

import android.app.Activity;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.PopupMenu;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class AccountMenuHelper {

    public static void setup(final Activity activity, ImageView ivAccount) {
        if (ivAccount == null) return;

        ivAccount.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(activity, ivAccount);

            // Ahora miramos el estado REAL de Firebase
            FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();

            if (user == null) {
                menu.getMenu().add("Login");
            } else {
                menu.getMenu().add("Mi perfil");
                menu.getMenu().add("Logout");
            }

            menu.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();

                if (title.equals("Login")) {
                    activity.startActivity(new Intent(activity, LoginActivity.class));
                } else if (title.equals("Mi perfil")) {
                    activity.startActivity(new Intent(activity, ProfileActivity.class));
                } else if (title.equals("Logout")) {
                    // Centralizamos el logout en LoginActivity
                    LoginActivity.logout((AppCompatActivity) activity);
                }
                return true;
            });

            menu.show();
        });
    }
}
