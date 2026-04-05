package com.example.aplicatieproveit;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

public class MedicActivity extends AppCompatActivity {

    private Button btnTabSpital, btnTabAsteptare, btnLogOutMedic;
    private View layoutInSpital, layoutInProcesare;

    // Listele pentru tab-ul "În Procesare"
    private ListView lvAlerteNoi;
    private ArrayList<String> listaProcesareTitluri, listaProcesareDetalii;
    private ArrayList<Long> listaProcesarePrioritati;
    private ArrayAdapter<String> adapterProcesare;

    // Listele pentru tab-ul "În Spital"
    private ListView lvPacientiInSpital;
    private ArrayList<String> listaSpitalTitluri, listaSpitalDetalii;
    private ArrayList<Long> listaSpitalPrioritati;
    private ArrayAdapter<String> adapterSpital;

    private FirebaseFirestore db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medic);

        db = FirebaseFirestore.getInstance();

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        btnTabSpital = findViewById(R.id.btnTabSpital);
        btnTabAsteptare = findViewById(R.id.btnTabAsteptare);
        layoutInSpital = findViewById(R.id.layoutInSpital);
        layoutInProcesare = findViewById(R.id.layoutInProcesare);
        lvAlerteNoi = findViewById(R.id.lvAlerteNoi);
        lvPacientiInSpital = findViewById(R.id.lvPacientiInSpital);
        btnLogOutMedic = findViewById(R.id.btnLogOutMedic);

        btnTabSpital.setOnClickListener(v -> setViewSpital());
        btnTabAsteptare.setOnClickListener(v -> setViewProcesare());

        btnLogOutMedic.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            startActivity(new Intent(MedicActivity.this, MainActivity.class));
            finish();
        });

        // Inițializăm datele
        listaProcesareTitluri = new ArrayList<>(); listaProcesareDetalii = new ArrayList<>(); listaProcesarePrioritati = new ArrayList<>();
        listaSpitalTitluri = new ArrayList<>(); listaSpitalDetalii = new ArrayList<>(); listaSpitalPrioritati = new ArrayList<>();

        adapterProcesare = creeazaAdapter(listaProcesareTitluri, listaProcesarePrioritati);
        adapterSpital = creeazaAdapter(listaSpitalTitluri, listaSpitalPrioritati);

        lvAlerteNoi.setAdapter(adapterProcesare);
        lvPacientiInSpital.setAdapter(adapterSpital);

        // Click-uri pe liste (Deschide biletul complet al pacientului)
        lvAlerteNoi.setOnItemClickListener((parent, view, position, id) ->
                arataDosarPacient("Fișă: În drum spre Spital", listaProcesareDetalii.get(position)));

        lvPacientiInSpital.setOnItemClickListener((parent, view, position, id) ->
                arataDosarPacient("Fișă: Pacient Internat", listaSpitalDetalii.get(position)));

        ascultaDupaUrgente();
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

    private void ascultaDupaUrgente() {
        db.collection("Urgenti")
                // ⚠️ Aici am pus sortarea: Urgențele cu prioritate 1 (Roșu) vor fi mereu primele!
                .orderBy("prioritate", Query.Direction.ASCENDING)
                .addSnapshotListener((value, error) -> {
                    if (error != null) return;

                    if (value != null) {
                        listaProcesareTitluri.clear(); listaProcesareDetalii.clear(); listaProcesarePrioritati.clear();
                        listaSpitalTitluri.clear(); listaSpitalDetalii.clear(); listaSpitalPrioritati.clear();

                        for (QueryDocumentSnapshot doc : value) {
                            String status = doc.getString("status");
                            String diagnostic = doc.getString("diagnostic_ai");
                            String departament = doc.getString("departament_ai");
                            long prioritate = doc.contains("prioritate") && doc.get("prioritate") != null ? doc.getLong("prioritate") : 4;

                            // Tragem datele noi din Firebase
                            String simptome = doc.contains("descriere") ? doc.getString("descriere") : "Fără simptome raportate";
                            String nume = doc.contains("nume_pacient") ? doc.getString("nume_pacient") : "Pacient Necunoscut";
                            String istoric = doc.contains("istoric_pacient") ? doc.getString("istoric_pacient") : "Fără istoric";

                            String salaAlocata = doc.contains("sala_alocata") ? doc.getString("sala_alocata") : "În așteptare";
                            String medicAlocat = doc.contains("medic_alocat") ? doc.getString("medic_alocat") : "În așteptare";

                            String titluLista = "Cod " + prioritate + " - " + diagnostic;

                            // Creăm biletul complet cu absolut toate detaliile!
                            String detaliiFisa = "PACIENT: " + nume + "\n\n" +
                                    "DIAGNOSTIC A.I.: " + diagnostic + "\n" +
                                    "SECȚIE AFERENTĂ: " + departament + "\n" +
                                    "PRIORITATE (1=Critic, 4=Ușor): " + prioritate + "\n\n" +
                                    "SIMPTOME DECLARATE LIVE:\n" + simptome + "\n\n" +
                                    "ANTECEDENTE MEDICALE CUNOSCUTE:\n" + istoric + "\n\n" +
                                    "REPARTIZARE SPITAL:\nSală: " + salaAlocata + "\nMedic: " + medicAlocat;

                            if ("IN_ASTEPTARE".equals(status)) {
                                listaProcesareTitluri.add("🚑 " + titluLista);
                                listaProcesareDetalii.add(detaliiFisa);
                                listaProcesarePrioritati.add(prioritate);
                            } else if ("IN_SPITAL".equals(status)) {
                                listaSpitalTitluri.add("🏥 " + titluLista);
                                listaSpitalDetalii.add(detaliiFisa);
                                listaSpitalPrioritati.add(prioritate);
                            }
                        }
                        adapterProcesare.notifyDataSetChanged();
                        adapterSpital.notifyDataSetChanged();
                    }
                });
    }

    private ArrayAdapter<String> creeazaAdapter(ArrayList<String> titluri, ArrayList<Long> prioritati) {
        return new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, titluri) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView text = view.findViewById(android.R.id.text1);
                text.setTypeface(null, android.graphics.Typeface.BOLD);
                view.setPadding(30, 40, 30, 40);

                long prioritate = prioritati.isEmpty() ? 4 : prioritati.get(position);

                // Codurile de culori pentru sortare vizuală
                if (prioritate == 1) { view.setBackgroundColor(Color.parseColor("#FFCDD2")); text.setTextColor(Color.parseColor("#B71C1C")); }
                else if (prioritate == 2) { view.setBackgroundColor(Color.parseColor("#FFF9C4")); text.setTextColor(Color.parseColor("#F57F17")); }
                else if (prioritate == 3) { view.setBackgroundColor(Color.parseColor("#C8E6C9")); text.setTextColor(Color.parseColor("#1B5E20")); }
                else { view.setBackgroundColor(Color.parseColor("#BBDEFB")); text.setTextColor(Color.parseColor("#0D47A1")); }

                return view;
            }
        };
    }

    private void arataDosarPacient(String titlu, String detalii) {
        new AlertDialog.Builder(this)
                .setTitle(titlu)
                .setMessage(detalii)
                .setPositiveButton("Închide Dosar", (dialog, which) -> dialog.dismiss())
                .show();
    }
}