package com.example.aplicatieproveit;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

// Importuri Firebase
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class PacientActivity extends AppCompatActivity {

    private SwitchCompat switchPentruAltcineva;
    private EditText etDescriereUrgenta;
    private Button btnRequestAmbulanta, btnLogOut;

    // --- INSTANȚIEM FIREBASE ---
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private String cnpPacientScanat = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pacient);

        // Inițializăm Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        // Mapăm elementele UI
        switchPentruAltcineva = findViewById(R.id.switchPentruAltcineva);
        etDescriereUrgenta = findViewById(R.id.etDescriereUrgenta);
        btnRequestAmbulanta = findViewById(R.id.btnRequestAmbulanta);
        btnLogOut = findViewById(R.id.btnLogOut);

        // Preluăm datele din Scanare (Gemini AI)
        String dateScanate = getIntent().getStringExtra("DATE_PACIENT_SCANAT");
        boolean esteInSistem = getIntent().getBooleanExtra("ESTE_IN_SISTEM", false);
        cnpPacientScanat = getIntent().getStringExtra("CNP_PACIENT");

        if (dateScanate != null) {
            switchPentruAltcineva.setChecked(true);
            String mesajInitial = "URGENȚĂ PENTRU PERSOANĂ IDENTIFICATĂ:\n" + dateScanate;

            if (esteInSistem) {
                mesajInitial += "\n\n⚠️ PACIENT VERIFICAT ÎN SISTEM.";
                Toast.makeText(this, "Profil găsit în Firebase.", Toast.LENGTH_SHORT).show();
            } else {
                mesajInitial += "\n\n(Persoană fără profil creat)";
            }
            etDescriereUrgenta.setText(mesajInitial + "\n\nSTARE ACTUALĂ: ");
        }

        // --- BUTON SOLICITARE AMBULANȚĂ (FIRESTORE) ---
        btnRequestAmbulanta.setOnClickListener(v -> {
            String descriere = etDescriereUrgenta.getText().toString().trim();
            if (descriere.isEmpty()) {
                Toast.makeText(this, "Vă rugăm descrieți urgența!", Toast.LENGTH_SHORT).show();
                return;
            }

            btnRequestAmbulanta.setEnabled(false);
            btnRequestAmbulanta.setText("Se trimite alerta...");

            // Pregătim datele pentru colecția "Urgenti"
            Map<String, Object> urgenta = new HashMap<>();
            urgenta.put("pacientId", mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "Anonim");
            urgenta.put("cnp_vizat", cnpPacientScanat != null ? cnpPacientScanat : "Nespecificat");
            urgenta.put("descriere", descriere);
            urgenta.put("pentruAltcineva", switchPentruAltcineva.isChecked());
            urgenta.put("status", "IN_ASTEPTARE"); // Status inițial pentru Medic
            urgenta.put("data_solicitare", new Date()); // Timestamp automat

            // Salvăm în Firestore
            db.collection("Urgenti")
                    .add(urgenta)
                    .addOnSuccessListener(documentReference -> {
                        Toast.makeText(PacientActivity.this, "Ambulanța a fost solicitată! Cerere salvată în Cloud.", Toast.LENGTH_LONG).show();
                        btnRequestAmbulanta.setEnabled(true);
                        btnRequestAmbulanta.setText("SOLICITĂ AMBULANȚĂ");
                        etDescriereUrgenta.setText("");
                    })
                    .addOnFailureListener(e -> {
                        btnRequestAmbulanta.setEnabled(true);
                        btnRequestAmbulanta.setText("SOLICITĂ AMBULANȚĂ");
                        Toast.makeText(PacientActivity.this, "Eroare Firebase: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    });
        });

        // --- LOG OUT ---
        btnLogOut.setOnClickListener(v -> {
            mAuth.signOut(); // Deconectare din Firebase
            Intent intent = new Intent(PacientActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }
}