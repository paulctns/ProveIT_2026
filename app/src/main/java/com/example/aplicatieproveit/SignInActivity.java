package com.example.aplicatieproveit;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.FirebaseApp;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SignInActivity extends AppCompatActivity {

    private RadioGroup rgTipUtilizator;
    private RadioButton rbPacient, rbMedic;
    private LinearLayout layoutDateMedic;

    private EditText etEmail, etPassword;
    private EditText etCNP, etVarsta, etGrupaSange, etAlergii, etBoli, etMedicamente;
    private EditText etCodParafa, etEmailInstitutional, etIDInstitutional, etUnitateMedicala, etDepartament;

    private Button btnSubmitSignIn;

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        // 1. Inițializăm Firebase
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 2. Legăm componentele UI
        rgTipUtilizator = findViewById(R.id.rgTipUtilizator);
        rbPacient = findViewById(R.id.rbPacient);
        rbMedic = findViewById(R.id.rbMedic);
        layoutDateMedic = findViewById(R.id.layoutDateMedic);

        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etCNP = findViewById(R.id.etCNP);
        etVarsta = findViewById(R.id.etVarsta);
        etGrupaSange = findViewById(R.id.etGrupaSange);
        etAlergii = findViewById(R.id.etAlergii);
        etBoli = findViewById(R.id.etBoli);
        etMedicamente = findViewById(R.id.etMedicamente);

        etCodParafa = findViewById(R.id.etCodParafa);
        etEmailInstitutional = findViewById(R.id.etEmailInstitutional);
        etIDInstitutional = findViewById(R.id.etIDInstitutional);
        etUnitateMedicala = findViewById(R.id.etUnitateMedicala);
        etDepartament = findViewById(R.id.etDepartament);

        btnSubmitSignIn = findViewById(R.id.btnSubmitSignIn);

        // 3. Switch vizibilitate Medic/Pacient
        rgTipUtilizator.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbPacient) {
                layoutDateMedic.setVisibility(View.GONE);
            } else {
                layoutDateMedic.setVisibility(View.VISIBLE);
            }
        });

        btnSubmitSignIn.setOnClickListener(v -> creazaContFirebase());
    }

    private void creazaContFirebase() {
        String email = etEmail.getText().toString().trim();
        String parola = etPassword.getText().toString().trim();

        if (email.isEmpty() || parola.isEmpty() || parola.length() < 6) {
            Toast.makeText(this, "Email necesar și parolă minim 6 caractere!", Toast.LENGTH_SHORT).show();
            return;
        }

        btnSubmitSignIn.setEnabled(false);
        btnSubmitSignIn.setText("Se lucrează...");

        mAuth.createUserWithEmailAndPassword(email, parola)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        if (user != null) {
                            salveazaDateInFirestore(user.getUid(), email);
                        }
                    } else {
                        btnSubmitSignIn.setEnabled(true);
                        btnSubmitSignIn.setText("Creează Cont");
                        String error = task.getException() != null ? task.getException().getMessage() : "Eroare necunoscută";
                        Toast.makeText(SignInActivity.this, "Eroare Auth: " + error, Toast.LENGTH_LONG).show();
                    }
                });
    }

    private void salveazaDateInFirestore(String userId, String email) {
        Map<String, Object> userData = new HashMap<>();
        userData.put("email", email);

        // Preluăm textele din câmpuri
        String cnp = etCNP.getText().toString().trim();
        String varsta = etVarsta.getText().toString().trim();
        String grupaSange = etGrupaSange.getText().toString().trim();
        String boliText = etBoli.getText().toString().trim();
        String alergiiText = etAlergii.getText().toString().trim();
        String medicamente = etMedicamente.getText().toString().trim();

        userData.put("cnp", cnp);
        userData.put("varsta", varsta);
        userData.put("grupa_sange", grupaSange);
        userData.put("boli_cronice", boliText);
        userData.put("alergii", alergiiText);
        userData.put("medicamente", medicamente);

        // ⚠️ AICI E REZOLVAREA PENTRU AI-UL TĂU ⚠️
        // Transformăm ce a scris omul la boli și alergii într-o listă pentru Naive Bayes
        List<String> istoricMedical = new ArrayList<>();
        if (!boliText.isEmpty()) {
            istoricMedical.addAll(Arrays.asList(boliText.split("\\s*,\\s*")));
        }
        if (!alergiiText.isEmpty()) {
            istoricMedical.addAll(Arrays.asList(alergiiText.split("\\s*,\\s*")));
        }
        // Salvăm lista sub numele exact pe care îl caută motorul de triaj
        userData.put("istoric_medical", istoricMedical);

        if (rbPacient.isChecked()) {
            userData.put("tip_utilizator", "pacient");
        } else {
            userData.put("tip_utilizator", "medic");
            userData.put("cod_parafa", etCodParafa.getText().toString().trim());
            userData.put("email_institutional", etEmailInstitutional.getText().toString().trim());
            userData.put("id_institutional", etIDInstitutional.getText().toString().trim());
            userData.put("unitate_medicala", etUnitateMedicala.getText().toString().trim());
            userData.put("departament", etDepartament.getText().toString().trim());
        }

        db.collection("Users").document(userId).set(userData)
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(SignInActivity.this, "Cont creat cu succes!", Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(SignInActivity.this, LoginActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> {
                    btnSubmitSignIn.setEnabled(true);
                    btnSubmitSignIn.setText("Creează Cont");
                    Toast.makeText(SignInActivity.this, "Eroare Firestore: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }
}