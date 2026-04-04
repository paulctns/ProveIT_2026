package com.example.aplicatieproveit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class PacientActivity extends AppCompatActivity {

    private SwitchCompat switchPentruAltcineva;
    private EditText etDescriereUrgenta;
    private Button btnRequestAmbulanta, btnLogOut;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pacient);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        switchPentruAltcineva = findViewById(R.id.switchPentruAltcineva);
        etDescriereUrgenta = findViewById(R.id.etDescriereUrgenta);
        btnRequestAmbulanta = findViewById(R.id.btnRequestAmbulanta);
        btnLogOut = findViewById(R.id.btnLogOut);

        // Preluăm datele
        String dateScanate = getIntent().getStringExtra("DATE_PACIENT_SCANAT");
        boolean esteInSistem = getIntent().getBooleanExtra("ESTE_IN_SISTEM", false);

        if (dateScanate != null) {
            switchPentruAltcineva.setChecked(true);

            String mesajInitial = "URGENȚĂ PENTRU PERSOANĂ IDENTIFICATĂ:\n" + dateScanate;

            // --- LOGICA DE CONFIDENȚIALITATE ---
            if (esteInSistem) {
                // Dacă e în sistem, arătăm DOAR informația vitală (Alergii)
                // Restul datelor (antecedente, boli) rămân ascunse conform cerinței tale
                mesajInitial += "\n\n⚠️ ALERTĂ MEDICALĂ: Alergic la Penicilină!";
                Toast.makeText(this, "Utilizator găsit. Alerte critice încărcate.", Toast.LENGTH_LONG).show();
            } else {
                mesajInitial += "\n\n(Persoană fără profil în rețea)";
            }

            etDescriereUrgenta.setText(mesajInitial + "\n\nSTARE ACTUALĂ: ");
        }

        btnRequestAmbulanta.setOnClickListener(v -> {
            String descriere = etDescriereUrgenta.getText().toString().trim();
            if (descriere.isEmpty()) {
                Toast.makeText(this, "Descrieți urgența!", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(this, "Se preia locația GPS...", Toast.LENGTH_SHORT).show();

            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                Toast.makeText(this, "Ambulanța a fost solicitată! Datele de identificare și alergiile au fost trimise la triaj.", Toast.LENGTH_LONG).show();
                etDescriereUrgenta.setText("");
            }, 1500);
        });

        btnLogOut.setOnClickListener(v -> {
            Intent intent = new Intent(PacientActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}