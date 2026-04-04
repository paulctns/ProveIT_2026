package com.example.aplicatieproveit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Importuri Firebase
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    // Folosim ID-urile TALE din XML
    private EditText etEmailLogin, etPasswordLogin;
    private TextView tvForgotPassword;
    private Button btnLoginSubmit;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        // Inițializăm Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Conectăm cu XML-ul (ID-urile tale)
        etEmailLogin = findViewById(R.id.etEmailLogin);
        etPasswordLogin = findViewById(R.id.etPasswordLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnLoginSubmit = findViewById(R.id.btnLoginSubmit);

        // Resetare parolă
        tvForgotPassword.setOnClickListener(v -> {
            startActivity(new Intent(LoginActivity.this, ChangePasswordActivity.class));
        });

        // Butonul de Login cu Firebase
        btnLoginSubmit.setOnClickListener(v -> logheazaUtilizatorFirebase());
    }

    private void logheazaUtilizatorFirebase() {
        String email = etEmailLogin.getText().toString().trim();
        String password = etPasswordLogin.getText().toString().trim();

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, "Introdu email și parolă!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnLoginSubmit.setEnabled(false);
        btnLoginSubmit.setText("Se verifică...");

        // Logare în Firebase Auth
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Dacă logarea e OK, verificăm rolul (Medic/Pacient) în Firestore
                        verificaRolUtilizator(mAuth.getCurrentUser().getUid());
                    } else {
                        btnLoginSubmit.setEnabled(true);
                        btnLoginSubmit.setText("CONECTARE");
                        Toast.makeText(LoginActivity.this, "Eroare: " + task.getException().getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void verificaRolUtilizator(String uid) {
        db.collection("Users").document(uid).get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        DocumentSnapshot doc = task.getResult();
                        if (doc.exists()) {
                            String tip = doc.getString("tip_utilizator");

                            if ("medic".equalsIgnoreCase(tip)) {
                                Toast.makeText(this, "Autentificat ca Medic!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, MedicActivity.class));
                            } else {
                                Toast.makeText(this, "Conectare reușită (Pacient)!", Toast.LENGTH_SHORT).show();
                                startActivity(new Intent(LoginActivity.this, PacientActivity.class));
                            }
                            finish();
                        } else {
                            Toast.makeText(this, "Eroare: Profilul nu există în baza de date!", Toast.LENGTH_LONG).show();
                        }
                    }
                });
    }
}