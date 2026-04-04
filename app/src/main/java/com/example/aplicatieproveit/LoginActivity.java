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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmailLogin, etPasswordLogin;
    private TextView tvForgotPassword;
    private Button btnLoginSubmit;
    private DB_functions dbFunctions;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

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

        dbFunctions = new DB_functions();

        // 2. Acțiunea pentru Ai uitat parola
        tvForgotPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ChangePasswordActivity.class);
            startActivity(intent);
        });

        // 3. Acțiunea butonului de conectare (Login de test)
        btnLoginSubmit.setOnClickListener(v -> {
            String email = etEmailLogin.getText().toString().trim();
            String password = etPasswordLogin.getText().toString().trim();

            if (email.isEmpty() || password.isEmpty()) {
                Toast.makeText(LoginActivity.this, "Vă rugăm să introduceți email-ul și parola!", Toast.LENGTH_SHORT).show();
                return;
            }

            executorService.execute(() -> {
                // Încercăm login ca operator (medic) mai întâi, folosind email-ul care ar putea fi cel instituțional
                String operatorCnp = dbFunctions.loginOperator(email, password);
                if (operatorCnp != null) {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Autentificat ca Medic!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, MedicActivity.class);
                        intent.putExtra("OPERATOR_CNP", operatorCnp);
                        startActivity(intent);
                        finish();
                    });
                    return;
                }

                // Dacă nu e medic, încercăm login ca pacient folosind email-ul personal
                String patientCnp = dbFunctions.loginPatientcuMailandParola(email, password);
                
                if (patientCnp != null) {
                    runOnUiThread(() -> {
                        Toast.makeText(LoginActivity.this, "Conectare reușită (Pacient)!", Toast.LENGTH_SHORT).show();
                        Intent intent = new Intent(LoginActivity.this, PacientActivity.class);
                        intent.putExtra("PATIENT_CNP", patientCnp);
                        startActivity(intent);
                        finish();
                    });
                    return;
                }

                // Dacă niciuna nu a mers
                runOnUiThread(() -> {
                    Toast.makeText(LoginActivity.this, "Email sau parolă incorectă!", Toast.LENGTH_SHORT).show();
                });
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}