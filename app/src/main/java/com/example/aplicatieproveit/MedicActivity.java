package com.example.aplicatieproveit;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MedicActivity extends AppCompatActivity {

    // Tab-uri și Layouts
    private Button btnTabSpital, btnTabAsteptare;
    private ScrollView layoutInSpital;
    private View layoutInProcesare;

    // Elementele pentru In Spital
    private CardView cardPacient1, cardPacient2, cardPacient3;
    private Button btnLogOutMedic;

    // Elementele pentru In Procesare
    private ListView lvAlerteNoi;
    private ArrayList<String> listaAlerte;
    private ArrayList<String> listaDetaliiAlerte; // Ca să știm ce detaliu afișăm la click
    private ArrayAdapter<String> adapter;

    // Firebase
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medic);

        // Inițializare Firebase
        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // 1. Mapare UI
        btnTabSpital = findViewById(R.id.btnTabSpital);
        btnTabAsteptare = findViewById(R.id.btnTabAsteptare);
        layoutInSpital = findViewById(R.id.layoutInSpital);
        layoutInProcesare = findViewById(R.id.layoutInProcesare);

        cardPacient1 = findViewById(R.id.cardPacient1);
        cardPacient2 = findViewById(R.id.cardPacient2);
        cardPacient3 = findViewById(R.id.cardPacient3);

        lvAlerteNoi = findViewById(R.id.lvAlerteNoi);
        btnLogOutMedic = findViewById(R.id.btnLogOutMedic);

        // 2. Logica de comutare Tab-uri
        btnTabSpital.setOnClickListener(v -> setViewSpital());
        btnTabAsteptare.setOnClickListener(v -> setViewProcesare());

        // 3. Setup pentru Lista Live
        listaAlerte = new ArrayList<>();
        listaDetaliiAlerte = new ArrayList<>();
        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listaAlerte);
        lvAlerteNoi.setAdapter(adapter);

        // Când medicul dă click pe o alertă nouă din listă
        lvAlerteNoi.setOnItemClickListener((parent, view, position, id) -> {
            arataDosarPacient("URGENȚĂ ÎN AȘTEPTARE", listaDetaliiAlerte.get(position), "CNP Nespecificat. Așteaptă decizie AI.");
        });

        // 4. Acțiuni pentru Cardurile Statice (În Spital)
        cardPacient1.setOnClickListener(v -> arataDosarPacient("Ion Popescu", "Dureri piept, risc infarct.", "Grupa A, Alergic la Penicilină"));
        cardPacient2.setOnClickListener(v -> arataDosarPacient("Maria Ionescu", "Tăietură adâncă braț.", "Grupa 0, Fără alergii"));
        cardPacient3.setOnClickListener(v -> arataDosarPacient("Elena Popa", "Dificultăți respiratorii.", "Grupa B, Astm bronșic"));

        // Log Out
        btnLogOutMedic.setOnClickListener(v -> {
            mAuth.signOut();
            Intent intent = new Intent(MedicActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // Pornim receptorul de Firebase
        ascultaDupaUrgenteNoi();
    }

    // Funcții pentru schimbarea Tab-urilor
    private void setViewSpital() {
        layoutInSpital.setVisibility(View.VISIBLE);
        layoutInProcesare.setVisibility(View.GONE);
        btnTabSpital.setAlpha(1.0f);
        btnTabAsteptare.setAlpha(0.6f);
    }

    private void setViewProcesare() {
        layoutInSpital.setVisibility(View.GONE);
        layoutInProcesare.setVisibility(View.VISIBLE);
        btnTabSpital.setAlpha(0.6f);
        btnTabAsteptare.setAlpha(1.0f);
    }

    // Funcția care aduce datele Live din Firebase
    private void ascultaDupaUrgenteNoi() {
        db.collection("Urgenti")
                .orderBy("data_solicitare", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) {
                        Log.e("FIREBASE", "Eroare: " + error.getMessage());
                        return;
                    }

                    if (value != null) {
                        listaAlerte.clear();
                        listaDetaliiAlerte.clear();

                        for (QueryDocumentSnapshot doc : value) {
                            String descriere = doc.getString("descriere");
                            String status = doc.getString("status");

                            // Textul care apare pe rând în listă
                            String titluScurt = "🚨 PACIENT NOU - Status: " + status;

                            listaAlerte.add(titluScurt);
                            listaDetaliiAlerte.add(descriere); // Salvăm descrierea ca să o arătăm la click
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    // Funcția pentru Pop-up
    private void arataDosarPacient(String nume, String diagnosticAI, String istoricMedical) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Dosar: " + nume);

        String mesajComplet = "📋 REZUMAT SIMPTOME:\n" + diagnosticAI + "\n\n" +
                "📂 DATE SISTEM:\n" + istoricMedical;

        builder.setMessage(mesajComplet);
        builder.setPositiveButton("Închide", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}