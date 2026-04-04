package com.example.aplicatieproveit;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PacientActivity extends AppCompatActivity {

    private SwitchCompat switchPentruAltcineva;
    private EditText etDescriereUrgenta;
    private Button btnRequestAmbulanta, btnLogOut;
    private DB_functions dbFunctions;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private String patientCnp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pacient);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Mapăm elementele din interfață
        switchPentruAltcineva = findViewById(R.id.switchPentruAltcineva);
        etDescriereUrgenta = findViewById(R.id.etDescriereUrgenta);
        btnRequestAmbulanta = findViewById(R.id.btnRequestAmbulanta);
        btnLogOut = findViewById(R.id.btnLogOut);

        dbFunctions = new DB_functions();
        patientCnp = getIntent().getStringExtra("PATIENT_CNP");

        // 2. Acțiunea pentru Butonul de Urgență (Request)
        btnRequestAmbulanta.setOnClickListener(v -> {
            String descriere = etDescriereUrgenta.getText().toString().trim();

            if (descriere.isEmpty()) {
                Toast.makeText(PacientActivity.this, "Te rog să descrii urgența!", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(PacientActivity.this, "Se preia locația GPS și se trimite cererea...", Toast.LENGTH_SHORT).show();

            boolean pentruAltcineva = switchPentruAltcineva.isChecked();
            boolean isForSelf = !pentruAltcineva;

            executorService.execute(() -> {
                // Presupunem 'yellow' ca prioritate implicită până la procesarea AI
                // needsAmbulance e true implicit pentru acest buton
                boolean success = dbFunctions.sendEmergencyRequest(patientCnp, "yellow", isForSelf, descriere, true, 15);

                runOnUiThread(() -> {
                    if (success) {
                        if (pentruAltcineva) {
                            Toast.makeText(PacientActivity.this, "Ambulanța a fost chemată la locația ta. Datele TALE medicale NU au fost atașate.", Toast.LENGTH_LONG).show();
                        } else {
                            Toast.makeText(PacientActivity.this, "Ambulanța chemată! Datele tale și descrierea au fost trimise la AI pentru triaj.", Toast.LENGTH_LONG).show();
                        }
                        etDescriereUrgenta.setText("");
                    } else {
                        Toast.makeText(PacientActivity.this, "Eroare la trimiterea cererii! Verifică conexiunea.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        // 3. Acțiunea pentru Log Out
        btnLogOut.setOnClickListener(v -> {
            Intent intent = new Intent(PacientActivity.this, MainActivity.class);
            // Aceste flag-uri șterg istoricul (nu mai poți da 'Back' să revii aici)
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}