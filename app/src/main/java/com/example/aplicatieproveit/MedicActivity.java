package com.example.aplicatieproveit;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.TextView;
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

    private Button btnTabSpital, btnTabAsteptare;
    private ScrollView layoutInSpital;
    private View layoutInProcesare;

    private CardView cardPacient1, cardPacient2, cardPacient3;
    private Button btnLogOutMedic;

    private ListView lvAlerteNoi;
    private ArrayList<String> listaAlerte;
    private ArrayList<String> listaDetaliiComplete;
    private ArrayList<Long> listaPrioritati; // Păstrăm prioritatea pentru a colora corect rândul
    private ArrayAdapter<String> adapter;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medic);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Mapare UI
        btnTabSpital = findViewById(R.id.btnTabSpital);
        btnTabAsteptare = findViewById(R.id.btnTabAsteptare);
        layoutInSpital = findViewById(R.id.layoutInSpital);
        layoutInProcesare = findViewById(R.id.layoutInProcesare);
        cardPacient1 = findViewById(R.id.cardPacient1);
        cardPacient2 = findViewById(R.id.cardPacient2);
        cardPacient3 = findViewById(R.id.cardPacient3);
        lvAlerteNoi = findViewById(R.id.lvAlerteNoi);
        btnLogOutMedic = findViewById(R.id.btnLogOutMedic);

        btnTabSpital.setOnClickListener(v -> setViewSpital());
        btnTabAsteptare.setOnClickListener(v -> setViewProcesare());

        listaAlerte = new ArrayList<>();
        listaDetaliiComplete = new ArrayList<>();
        listaPrioritati = new ArrayList<>();

        // ADAPTOR CUSTOM: Colorează fiecare rând în funcție de lista de priorități
        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, listaAlerte) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);

                // Pentru a arăta mai bine, facem textul un pic mai îngroșat și îi dăm padding
                text.setTypeface(null, android.graphics.Typeface.BOLD);
                view.setPadding(30, 40, 30, 40);

                long prioritate = listaPrioritati.get(position);

                // Setăm fundalurile și culorile textului pe coduri de triaj
                if (prioritate == 1) { // ROȘU
                    view.setBackgroundColor(Color.parseColor("#FFCDD2")); // Roșu pastel
                    text.setTextColor(Color.parseColor("#B71C1C")); // Roșu închis
                } else if (prioritate == 2) { // GALBEN
                    view.setBackgroundColor(Color.parseColor("#FFF9C4")); // Galben pastel
                    text.setTextColor(Color.parseColor("#F57F17")); // Portocaliu închis
                } else if (prioritate == 3) { // VERDE
                    view.setBackgroundColor(Color.parseColor("#C8E6C9")); // Verde pastel
                    text.setTextColor(Color.parseColor("#1B5E20")); // Verde închis
                } else if (prioritate == 4) { // ALBASTRU
                    view.setBackgroundColor(Color.parseColor("#BBDEFB")); // Albastru pastel
                    text.setTextColor(Color.parseColor("#0D47A1")); // Albastru închis
                } else {
                    view.setBackgroundColor(Color.parseColor("#F5F5F5")); // Gri standard
                    text.setTextColor(Color.parseColor("#333333"));
                }

                return view;
            }
        };

        lvAlerteNoi.setAdapter(adapter);

        // Click pe o alertă din listă pentru detalii complete
        lvAlerteNoi.setOnItemClickListener((parent, view, position, id) -> {
            String detalii = listaDetaliiComplete.get(position);
            arataDosarPacient("DETALII URGENȚĂ LIVE", detalii, "Calculat prin A.I. Naive Bayes Classifier");
        });

        // Carduri statice (Demo)
        cardPacient1.setOnClickListener(v -> arataDosarPacient("Ion Popescu", "Risc Infarct", "Cod Roșu - Sala 1"));
        cardPacient2.setOnClickListener(v -> arataDosarPacient("Maria Ionescu", "Tăietură braț", "Cod Galben - Sala 4"));

        btnLogOutMedic.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(MedicActivity.this, MainActivity.class));
            finish();
        });

        ascultaDupaUrgenteNoi();
    }

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

    private void ascultaDupaUrgenteNoi() {
        db.collection("Urgenti")
                .orderBy("data_solicitare", Query.Direction.DESCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        listaAlerte.clear();
                        listaDetaliiComplete.clear();
                        listaPrioritati.clear();

                        for (QueryDocumentSnapshot doc : value) {
                            String diagnostic = doc.getString("diagnostic_ai");
                            String departament = doc.getString("departament_ai");
                            String descriereSimpla = doc.getString("descriere");

                            // Ne asigurăm că dacă nu există prioritate (urgențe vechi), o punem default pe 4
                            long prioritate = doc.contains("prioritate") && doc.get("prioritate") != null
                                    ? doc.getLong("prioritate") : 4;

                            // Nu mai punem emoji, punem direct textul clar
                            String titluLista = diagnostic != null ? diagnostic.toUpperCase() + " (" + departament + ")" : "Urgență Necunoscută";

                            String detaliiPopUp = "SIMPTOME PACIENT:\n" + descriereSimpla +
                                    "\n\nDIAGNOSTIC A.I.: " + diagnostic +
                                    "\nDEPARTAMENT RECOMANDAT: " + departament +
                                    "\nNIVEL URGENȚĂ (1-4): " + prioritate;

                            listaAlerte.add(titluLista);
                            listaDetaliiComplete.add(detaliiPopUp);
                            listaPrioritati.add(prioritate); // Salvăm prioritatea pentru a ști ce culoare dăm rândului
                        }
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void arataDosarPacient(String nume, String diagnosticAI, String istoricMedical) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(nume);
        builder.setMessage(diagnosticAI + "\n\n📂 INFO SISTEM:\n" + istoricMedical);
        builder.setPositiveButton("ÎNREGISTREAZĂ ÎN SPITAL", (dialog, which) -> {
            // Aici va veni codul pentru Opțiunea 2 (Alocare Sală și Medic)
            Toast.makeText(this, "Funcție în lucru pentru alocare sală...", Toast.LENGTH_SHORT).show();
        });
        builder.setNegativeButton("Închide", (dialog, which) -> dialog.dismiss());
        builder.show();
    }
}