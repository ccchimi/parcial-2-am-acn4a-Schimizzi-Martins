package com.app.tasteit;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private Button btnLogin, btnCreateUser;
    private TextView tvForgot;

    private FirebaseAuth auth;

    public static String currentUser = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();

        etEmail = findViewById(R.id.etUsername);     // ahora es email real por la integracion con firebase
        etPassword = findViewById(R.id.etPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnCreateUser = findViewById(R.id.btnCreateUser);
        tvForgot = findViewById(R.id.tvForgot);

        // Si ya estaba logueado lo mando al Home
        if (auth.getCurrentUser() != null) {
            currentUser = auth.getCurrentUser().getEmail();
            startActivity(new Intent(this, MainActivity.class));
            finish();
        }

        btnLogin.setOnClickListener(v -> loginUser());
        btnCreateUser.setOnClickListener(v -> goToRegister());
        tvForgot.setOnClickListener(v -> resetPassword());
    }

    private void loginUser() {
        String email = etEmail.getText().toString().trim();
        String pass = etPassword.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            etEmail.setError("Email inválido");
            return;
        }
        if (pass.isEmpty()) {
            etPassword.setError("Ingrese contraseña");
            return;
        }

        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    currentUser = email;
                    Toast.makeText(this, "Bienvenido " + email, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void resetPassword() {
        String email = etEmail.getText().toString().trim();

        if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            Toast.makeText(this, "Ingresá un email válido primero", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.sendPasswordResetEmail(email)
                .addOnSuccessListener(v ->
                        Toast.makeText(this, "Se envió un email para recuperar contraseña", Toast.LENGTH_LONG).show()
                )
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
    }

    private void goToRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
    }

    // Método usado por ProfileActivity
    public static void logout(AppCompatActivity activity) {
        FirebaseAuth.getInstance().signOut();
        currentUser = null;

        Toast.makeText(activity, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        activity.startActivity(new Intent(activity, LoginActivity.class));
        activity.finish();
    }
}