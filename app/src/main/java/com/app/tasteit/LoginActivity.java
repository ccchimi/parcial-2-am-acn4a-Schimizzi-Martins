package com.app.tasteit;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

public class LoginActivity extends AppCompatActivity {

    EditText etUsername, etPassword;
    Button btnLogin, btnCreateUser;
    TextView tvForgot;

    public static String currentUser = null;
    SharedPreferences sharedPrefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        etUsername = findViewById(R.id.etUsername);
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnCreateUser = findViewById(R.id.btnCreateUser);
        tvForgot = findViewById(R.id.tvForgot);

        sharedPrefs = getSharedPreferences("UsersPrefs", Context.MODE_PRIVATE);
        currentUser = sharedPrefs.getString("currentUser", null);

        if(!sharedPrefs.contains("admin")) {
            sharedPrefs.edit().putString("admin", "1234").apply();
        }

        btnLogin.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            String storedPass = sharedPrefs.getString(user, null);
            if(storedPass != null && storedPass.equals(pass)) {
                currentUser = user;
                sharedPrefs.edit().putString("currentUser", currentUser).apply();
                Toast.makeText(this, getString(R.string.login_success, user), Toast.LENGTH_SHORT).show();
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else {
                Toast.makeText(this, getString(R.string.login_error), Toast.LENGTH_SHORT).show();
            }
        });

        btnCreateUser.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            String pass = etPassword.getText().toString().trim();

            if(user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, getString(R.string.empty_fields), Toast.LENGTH_SHORT).show();
            } else if(sharedPrefs.contains(user)) {
                Toast.makeText(this, getString(R.string.user_exists), Toast.LENGTH_SHORT).show();
            } else {
                sharedPrefs.edit().putString(user, pass).apply();
                Toast.makeText(this, getString(R.string.user_created), Toast.LENGTH_SHORT).show();
            }
        });

        tvForgot.setOnClickListener(v -> {
            String user = etUsername.getText().toString().trim();
            if(sharedPrefs.contains(user)) {
                EditText input = new EditText(this);
                input.setHint(getString(R.string.login_password));

                new AlertDialog.Builder(this)
                        .setTitle(getString(R.string.change_password_title))
                        .setMessage(getString(R.string.change_password_message, user))
                        .setView(input)
                        .setPositiveButton(getString(R.string.accept), (dialog, which) -> {
                            String newPass = input.getText().toString().trim();
                            sharedPrefs.edit().putString(user, newPass).apply();
                            Toast.makeText(this, getString(R.string.user_updated), Toast.LENGTH_SHORT).show();
                        })
                        .setNegativeButton(getString(R.string.cancel), null)
                        .show();
            } else {
                Toast.makeText(this, getString(R.string.user_not_found), Toast.LENGTH_SHORT).show();
            }
        });
    }

    public static void logout(Context context, SharedPreferences prefs) {
        currentUser = null;
        prefs.edit().remove("currentUser").apply();
        Toast.makeText(context, context.getString(R.string.session_closed), Toast.LENGTH_SHORT).show();
    }
}