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
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

public class LoginActivity extends AppCompatActivity {

    private EditText etIdentifier, etPassword;
    private Button btnLogin, btnCreateUser;
    private TextView tvForgot;

    private FirebaseAuth auth;
    private FirebaseFirestore db;

    // Email actual
    public static String currentUser = null;

    // Para autoria de recetas
    public static String currentUid = null;
    public static String currentUsername = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        auth = FirebaseAuth.getInstance();
        db   = FirebaseFirestore.getInstance();

        etIdentifier = findViewById(R.id.etUsername);  // ahora puede ser usuario o email
        etPassword   = findViewById(R.id.etPassword);
        btnLogin     = findViewById(R.id.btnLogin);
        btnCreateUser = findViewById(R.id.btnCreateUser);
        tvForgot     = findViewById(R.id.tvForgot);

        // Si ya esta logueado, lo mando directo al Home
        FirebaseUser user = auth.getCurrentUser();
        if (user != null) {
            currentUser = user.getEmail();
            currentUid  = user.getUid();
            loadCurrentUserData(user);
            startActivity(new Intent(this, MainActivity.class));
            finish();
            return;
        }

        btnLogin.setOnClickListener(v -> loginUser());
        btnCreateUser.setOnClickListener(v -> goToRegister());
        tvForgot.setOnClickListener(v -> resetPassword());
    }

    private void loginUser() {
        final String identifier = etIdentifier.getText().toString().trim(); // usuario o email
        final String pass       = etPassword.getText().toString().trim();

        if (identifier.isEmpty()) {
            etIdentifier.setError("Ingresá usuario o email");
            return;
        }
        if (pass.isEmpty()) {
            etPassword.setError("Ingresá contraseña");
            return;
        }

        // Si parece un email, vamos directo a Firebase Auth
        if (identifier.contains("@")) {
            if (!Patterns.EMAIL_ADDRESS.matcher(identifier).matches()) {
                etIdentifier.setError("Email inválido");
                return;
            }
            signInWithEmail(identifier, pass);
        } else {
            // Si NO tiene @, lo tratamos como username: lo buscamos en Firestore
            db.collection("usuarios")
                    .whereEqualTo("username", identifier)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot == null || snapshot.isEmpty()) {
                            Toast.makeText(this,
                                    "El usuario no existe",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String email = snapshot.getDocuments()
                                .get(0)
                                .getString("email");

                        if (email == null || email.isEmpty()) {
                            Toast.makeText(this,
                                    "Error: el usuario no tiene email asociado",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        signInWithEmail(email, pass);
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Error al buscar el usuario: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void signInWithEmail(String email, String pass) {
        auth.signInWithEmailAndPassword(email, pass)
                .addOnSuccessListener(result -> {
                    FirebaseUser user = result.getUser();
                    if (user != null) {
                        currentUser = user.getEmail();
                        currentUid  = user.getUid();
                        loadCurrentUserData(user);
                        currentUser = email;
                    }

                    Toast.makeText(this, "Bienvenido " + email, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, MainActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    String msg;

                    if (e instanceof FirebaseAuthInvalidUserException) {
                        msg = "El usuario no existe o fue deshabilitado";
                    } else if (e instanceof FirebaseAuthInvalidCredentialsException) {
                        msg = "Contraseña incorrecta";
                    } else {
                        msg = "Error al iniciar sesión: " + e.getMessage();
                    }

                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                });
    }

    private void loadCurrentUserData(FirebaseUser user) {
        if (user == null) return;

        String uid = user.getUid();

        db.collection("usuarios")
                .whereEqualTo("uid", uid)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        String username = doc.getString("username");
                        if (username != null && !username.isEmpty()) {
                            currentUsername = username;
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    // no es grave en realidad, solo no tendriamos username
                });
    }

    private void resetPassword() {
        final String text = etIdentifier.getText().toString().trim();

        // Si lo que hay parece un email, usamos eso
        if (text.contains("@")) {
            if (!Patterns.EMAIL_ADDRESS.matcher(text).matches()) {
                Toast.makeText(this, "Ingresá un email válido primero", Toast.LENGTH_SHORT).show();
                return;
            }

            auth.sendPasswordResetEmail(text)
                    .addOnSuccessListener(v ->
                            Toast.makeText(this,
                                    "Se envió un email para recuperar la contraseña",
                                    Toast.LENGTH_LONG).show()
                    )
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Error: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
        } else {
            // Si puso username, buscamos su email en Firestore y recien ahi mandamos el mail real a su casilla de correo
            if (text.isEmpty()) {
                Toast.makeText(this, "Ingresá usuario o email primero", Toast.LENGTH_SHORT).show();
                return;
            }

            db.collection("usuarios")
                    .whereEqualTo("username", text)
                    .limit(1)
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        if (snapshot == null || snapshot.isEmpty()) {
                            Toast.makeText(this,
                                    "No encontramos un usuario con ese nombre",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        String email = snapshot.getDocuments()
                                .get(0)
                                .getString("email");

                        if (email == null || email.isEmpty()) {
                            Toast.makeText(this,
                                    "El usuario no tiene email asociado",
                                    Toast.LENGTH_SHORT).show();
                            return;
                        }

                        auth.sendPasswordResetEmail(email)
                                .addOnSuccessListener(v ->
                                        Toast.makeText(this,
                                                "Se envió un email a " + email,
                                                Toast.LENGTH_LONG).show()
                                )
                                .addOnFailureListener(e ->
                                        Toast.makeText(this,
                                                "Error: " + e.getMessage(),
                                                Toast.LENGTH_SHORT).show()
                                );
                    })
                    .addOnFailureListener(e ->
                            Toast.makeText(this,
                                    "Error al buscar usuario: " + e.getMessage(),
                                    Toast.LENGTH_SHORT).show()
                    );
        }
    }

    private void goToRegister() {
        startActivity(new Intent(this, RegisterActivity.class));
    }

    public static void logout(AppCompatActivity activity) {
        FirebaseAuth.getInstance().signOut();
        currentUser = null;
        currentUid = null;
        currentUsername = null;
        Toast.makeText(activity, "Sesión cerrada", Toast.LENGTH_SHORT).show();
        activity.startActivity(new Intent(activity, LoginActivity.class));
        activity.finish();
    }
}
