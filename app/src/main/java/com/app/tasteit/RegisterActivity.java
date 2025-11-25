package com.app.tasteit;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private EditText etFirstName, etLastName, etUsername, etEmail, etPassword;
    private Button btnRegister;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        etFirstName = findViewById(R.id.etRegFirstName);
        etLastName = findViewById(R.id.etRegLastName);
        etUsername = findViewById(R.id.etRegUsername);
        etEmail = findViewById(R.id.etRegEmail);
        etPassword = findViewById(R.id.etRegPassword);
        btnRegister = findViewById(R.id.btnRegisterUser);

        btnRegister.setOnClickListener(v -> createAccount());
    }

    private void createAccount() {
        String first = etFirstName.getText().toString().trim();
        String last = etLastName.getText().toString().trim();
        String username = etUsername.getText().toString().trim();
        String email = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString().trim();

        if (first.isEmpty() || last.isEmpty() || username.isEmpty() ||
                email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Completá todos los campos", Toast.LENGTH_SHORT).show();
            return;
        }

        if (password.length() < 6) {
            Toast.makeText(this, "La contraseña debe tener mínimo 6 caracteres", Toast.LENGTH_SHORT).show();
            return;
        }

        auth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(result -> {

                    String uid = result.getUser().getUid();

                    Map<String, Object> userData = new HashMap<>();
                    userData.put("uid", uid);
                    userData.put("firstName", first);
                    userData.put("lastName", last);
                    userData.put("username", username);
                    userData.put("email", email);
                    userData.put("createdAt", System.currentTimeMillis());

                    db.collection("usuarios")
                            .document(uid)
                            .set(userData)
                            .addOnSuccessListener(aVoid -> {
                                Toast.makeText(this, "Cuenta creada con éxito", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(this, LoginActivity.class));
                                finish();
                            })
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Error guardando datos: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                })
                .addOnFailureListener(e -> {
                    String msg = e.getMessage();
                    if (msg.contains("email address is already in use"))
                        msg = "Ese email ya está registrado.";
                    Toast.makeText(this, "Error: " + msg, Toast.LENGTH_SHORT).show();
                });
    }
}
