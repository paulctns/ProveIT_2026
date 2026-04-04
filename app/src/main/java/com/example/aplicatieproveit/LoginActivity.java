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

public class LoginActivity extends AppCompatActivity {

    private EditText etEmailLogin, etPasswordLogin;
    private TextView tvForgotPassword;
    private Button btnLoginSubmit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Conectăm variabilele cu XML-ul
        etEmailLogin = findViewById(R.id.etEmailLogin);
        etPasswordLogin = findViewById(R.id.etPasswordLogin);
        tvForgotPassword = findViewById(R.id.tvForgotPassword);
        btnLoginSubmit = findViewById(R.id.btnLoginSubmit);

        // 2. Acțiunea pentru Ai uitat parola
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        // 3. Acțiunea butonului de conectare (Login de test)
        btnLoginSubmit.setOnClickListener(v -> {
            String email = etEmailLogin.getText().toString().trim();
            String password = etPasswordLogin.getText().toString().trim();

            if (email.equals("admin") && password.equals("admin")) {
                Toast.makeText(LoginActivity.this, "Conectare reușită!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, PacientActivity.class);
                startActivity(intent);
                finish();
            } else if (email.equals("medic") && password.equals("medic")) {
                Toast.makeText(LoginActivity.this, "Autentificat ca Medic!", Toast.LENGTH_SHORT).show();
                Intent intent = new Intent(LoginActivity.this, MedicActivity.class);
                startActivity(intent);
                finish();
            } else {
                Toast.makeText(LoginActivity.this, "Date incorecte! Încearcă: admin / admin", Toast.LENGTH_SHORT).show();
            }
        });
    }
}