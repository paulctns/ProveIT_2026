package com.example.aplicatieproveit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private Button btnGoToLogIn, btnGoToSignIn, btnGoToScanCI;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Conectăm butoanele
        btnGoToLogIn = findViewById(R.id.btnGoToLogIn);
        btnGoToSignIn = findViewById(R.id.btnGoToSignIn);
        btnGoToScanCI = findViewById(R.id.btnGoToScanCI);

        // Navigare
        btnGoToLogIn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, LoginActivity.class);
            startActivity(intent);
        });

        btnGoToSignIn.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, SignInActivity.class);
            startActivity(intent);
        });

        btnGoToScanCI.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ScanActivity.class);
            startActivity(intent);
        });

        // =========================================================
        // Am comentat linia de mai jos ca să nu mai adauge date noi la fiecare Run.
        // populeaza50Afectiuni();
        // =========================================================
    }

    // =============================================================
    // Metodele de populare rămân în cod (în caz că mai ai nevoie), dar nu se mai apelează automat.
    // =============================================================
    private void populeaza50Afectiuni() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();

        // URGENȚE VITALE (COD 1)
        adaugaAfectiune(db, "Infarct Miocardic", "Cardiologie", 1, 0.05, new HashMap<String, Double>() {{
            put("durere_piept", 0.9); put("transpiratie", 0.7); put("durere_brat_stang", 0.6);
        }});
        adaugaAfectiune(db, "AVC", "Neurologie", 1, 0.04, new HashMap<String, Double>() {{
            put("asimetrie_faciala", 0.9); put("dificultate_vorbire", 0.9); put("amorteala_brat", 0.8);
        }});
        adaugaAfectiune(db, "Soc Anafilactic", "Alergologie", 1, 0.02, new HashMap<String, Double>() {{
            put("umflare_fata", 0.9); put("respiratie_suieratoare", 0.9); put("eruptie_cutanata", 0.8);
        }});

        // URGENȚE MARI (COD 2)
        adaugaAfectiune(db, "Apendicita", "Chirurgie", 2, 0.06, new HashMap<String, Double>() {{
            put("durere_abdomen_dreapta_jos", 0.9); put("greata", 0.7); put("varsaturi", 0.6);
        }});
        adaugaAfectiune(db, "Fractura Deschisa", "Ortopedie", 2, 0.04, new HashMap<String, Double>() {{
            put("durere_intensa_os", 0.9); put("deformare_zona", 0.9); put("umflare", 0.7);
        }});

        // URGENȚE MEDII (COD 3)
        adaugaAfectiune(db, "Pneumonie", "Pneumologie", 3, 0.08, new HashMap<String, Double>() {{
            put("tuse", 0.8); put("febra_usoara", 0.7); put("respiratie_grea", 0.6);
        }});
        adaugaAfectiune(db, "Zona Zoster", "Dermatologie", 3, 0.03, new HashMap<String, Double>() {{
            put("eruptie_cutanata", 0.9); put("mancarime", 0.8); put("roseata_piele", 0.7);
        }});

        // AFECȚIUNI UȘOARE (COD 4)
        adaugaAfectiune(db, "Viroza / Raceala", "Triaj General", 4, 0.35, new HashMap<String, Double>() {{
            put("tuse", 0.6); put("nas_infundat", 0.8); put("durere_gat", 0.7);
        }});
        adaugaAfectiune(db, "Indigestie", "Gastroenterologie", 4, 0.25, new HashMap<String, Double>() {{
            put("greata", 0.7); put("crampe_abdominale", 0.6); put("diaree", 0.5);
        }});

        Toast.makeText(this, "Baza de date a fost populată!", Toast.LENGTH_LONG).show();
    }

    private void adaugaAfectiune(FirebaseFirestore db, String nume, String dep, int prio, double prob, Map<String, Double> simptome) {
        Map<String, Object> boala = new HashMap<>();
        boala.put("nume", nume);
        boala.put("departament", dep);
        boala.put("prioritate", prio);
        boala.put("probabilitate_generala", prob);
        boala.put("simptome", simptome);
        db.collection("Afectiuni").add(boala);
    }
}