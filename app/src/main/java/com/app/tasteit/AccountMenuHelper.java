package com.app.tasteit;

import android.app.Activity;
import android.content.Intent;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

public class AccountMenuHelper {

    public static void setup(final Activity activity, ImageView ivAccount) {
        if (ivAccount == null) return;

        ivAccount.setOnClickListener(v -> {
            PopupMenu menu = new PopupMenu(activity, ivAccount);

            if (LoginActivity.currentUser == null) {
                menu.getMenu().add("Login");
            } else {
                menu.getMenu().add("Mi perfil");
                menu.getMenu().add("Logout");
            }

            menu.setOnMenuItemClickListener(item -> {
                String title = item.getTitle().toString();

                if (title.equals("Login")) {
                    activity.startActivity(new Intent(activity, LoginActivity.class));
                }
                else if (title.equals("Mi perfil")) {
                    activity.startActivity(new Intent(activity, ProfileActivity.class));
                }
                else if (title.equals("Logout")) {
                    Toast.makeText(activity,
                            activity.getString(R.string.session_closed),
                            Toast.LENGTH_SHORT).show();
                    LoginActivity.currentUser = null;
                    activity.startActivity(new Intent(activity, LoginActivity.class));
                    activity.finish();
                }
                return true;
            });

            menu.show();
        });
    }
}