package com.example.aplicatieproveit;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PacientActivity extends AppCompatActivity {

    private SwitchCompat switchPentruAltcineva;
    private EditText etDescriereUrgenta;
    private Button btnRequestAmbulanta, btnLogOut;
    private View layoutPrincipal;

    private RelativeLayout layoutAnimatieUrmarire;
    private ProgressBar progressBarCircular;
    private TextView tvCountdown;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;
    private final String API_KEY = BuildConfig.API_KEY;

    private double userLat = 0.0, userLon = 0.0;
    private int etaMinute = 12;
    private String textDescriereTemp = "";

    private final ActivityResultLauncher<String[]> cererePermisiuniGPS = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                if (result.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false)) {
                    obtineLocatiaSiProceseazaUrgente(textDescriereTemp);
                } else {
                    obtineLocatiaSiProceseazaUrgente(textDescriereTemp);
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_pacient);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();

        layoutPrincipal = findViewById(R.id.main);
        layoutAnimatieUrmarire = findViewById(R.id.layoutAnimatieUrmarire);
        progressBarCircular = findViewById(R.id.progressBarCircular);
        tvCountdown = findViewById(R.id.tvCountdown);

        switchPentruAltcineva = findViewById(R.id.switchPentruAltcineva);
        etDescriereUrgenta = findViewById(R.id.etDescriereUrgenta);
        btnRequestAmbulanta = findViewById(R.id.btnRequestAmbulanta);
        btnLogOut = findViewById(R.id.btnLogOut);

        btnRequestAmbulanta.setOnClickListener(v -> {
            textDescriereTemp = etDescriereUrgenta.getText().toString().trim();
            if (textDescriereTemp.isEmpty()) {
                Toast.makeText(this, "Descrieți urgența!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                cererePermisiuniGPS.launch(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION});
            } else {
                obtineLocatiaSiProceseazaUrgente(textDescriereTemp);
            }
        });

        btnLogOut.setOnClickListener(v -> {
            mAuth.signOut();
            startActivity(new Intent(PacientActivity.this, MainActivity.class));
            finish();
        });
    }

    private void obtineLocatiaSiProceseazaUrgente(String descriere) {
        btnRequestAmbulanta.setEnabled(false);
        btnRequestAmbulanta.setText("Calculare distanță...");

        LocationManager locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        try {
            Location location = locationManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
            if (location == null) location = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            if (location != null) {
                userLat = location.getLatitude();
                userLon = location.getLongitude();
                Location spital = new Location("");
                spital.setLatitude(47.165); spital.setLongitude(27.582);
                float distantaKm = location.distanceTo(spital) / 1000;
                etaMinute = (int) distantaKm + 3;
                if (etaMinute < 3) etaMinute = 3;
            }
        } catch (SecurityException e) { e.printStackTrace(); }

        pornesteFluxAI(descriere);
    }

    private void pornesteFluxAI(String descriere) {
        btnRequestAmbulanta.setText("AI-ul analizează simptomele...");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            String rezultatGemini = cereSimptomeDeLaGemini(descriere);
            handler.post(() -> {
                if (rezultatGemini.startsWith("EROARE")) {
                    salveazaUrgenta(descriere, "Eroare AI", "Triaj Manual", 1);
                } else {
                    List<String> simptome = Arrays.asList(rezultatGemini.split("\\s*,\\s*"));
                    executaTriajMatematic(descriere, simptome);
                }
            });
        });
    }

    private String cereSimptomeDeLaGemini(String descriere) {
        try {
            String prompt = "Ești un sistem medical. Alege din următoarea listă strict simptomele care se regăsesc în text: durere_piept, transpiratie, durere_brat_stang, respiratie_grea, asimetrie_faciala, amorteala_brat, dificultate_vorbire, confuzie, umflare_fata, respiratie_suieratoare, eruptie_cutanata, puls_rapid, durere_abdomen_dreapta_jos, greata, varsaturi, febra_usoara, tuse_seaca, senzatie_sufocare, durere_intensa_os, deformare_zona, imposibilitate_miscare, umflare, crampe_abdominale, diaree, durere_lombara, durere_iradiata_inghinal, sange_urina, tuse, nas_infundat, durere_gat, mancarime, roseata_piele, stranut, ochi_inlacrimati. Răspunde STRICT cu simptomele găsite, separate prin virgulă. Text: " + descriere;
            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray partsArray = new JSONArray();
            JSONObject textPart = new JSONObject();
            textPart.put("text", prompt);
            partsArray.put(textPart);
            contentObj.put("parts", partsArray);
            contentsArray.put(contentObj);
            jsonBody.put("contents", contentsArray);

            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);
            OutputStream os = conn.getOutputStream();
            os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            os.flush(); os.close();

            if (conn.getResponseCode() == 200) {
                Scanner scanner = new Scanner(new InputStreamReader(conn.getInputStream()));
                scanner.useDelimiter("\\A");
                String responseStr = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
                return new JSONObject(responseStr).getJSONArray("candidates").getJSONObject(0).getJSONObject("content").getJSONArray("parts").getJSONObject(0).getString("text").trim();
            }
            return "EROARE_API";
        } catch (Exception e) { return "EROARE_SISTEM"; }
    }

    private void executaTriajMatematic(String descriere, List<String> simptomePacient) {
        db.collection("Afectiuni").get().addOnSuccessListener(querySnapshots -> {
            Afectiune topBoala = null;
            double maxScor = -1.0;
            for (QueryDocumentSnapshot doc : querySnapshots) {
                Afectiune boala = doc.toObject(Afectiune.class);
                boala.id = doc.getId();
                double scor = TriageEngine.calculeazaScor(boala, simptomePacient);
                if (scor > maxScor) { maxScor = scor; topBoala = boala; }
            }
            if (topBoala != null && maxScor > 0) {
                salveazaUrgenta(descriere, topBoala.nume, topBoala.departament, (int) topBoala.prioritate);
            } else {
                salveazaUrgenta(descriere, "Indisponibil", "Triaj General", 2);
            }
        });
    }

    private void salveazaUrgenta(String descriere, String diagnosticAI, String departament, int prioritate) {
        Map<String, Object> urgenta = new HashMap<>();
        urgenta.put("pacientId", mAuth.getUid());
        urgenta.put("status", "IN_ASTEPTARE");
        urgenta.put("diagnostic_ai", diagnosticAI);
        urgenta.put("departament_ai", departament);
        urgenta.put("prioritate", prioritate);
        urgenta.put("eta_minute", etaMinute);
        urgenta.put("latitudine", userLat);
        urgenta.put("longitudine", userLon);
        urgenta.put("data_solicitare", new Date());

        db.collection("Urgenti").add(urgenta).addOnSuccessListener(doc -> pornesteAnimatieUrmarire(etaMinute));
    }

    private void pornesteAnimatieUrmarire(int minute) {
        layoutPrincipal.setVisibility(View.GONE);
        layoutAnimatieUrmarire.setVisibility(View.VISIBLE);
        long milisecundeTotale = (long) minute * 60 * 1000;
        new CountDownTimer(milisecundeTotale, 1000) {
            public void onTick(long millisUntilFinished) {
                int min = (int) (millisUntilFinished / 1000) / 60;
                int sec = (int) (millisUntilFinished / 1000) % 60;
                tvCountdown.setText(String.format("%02d:%02d", min, sec));
                progressBarCircular.setProgress((int) (millisUntilFinished * 100 / milisecundeTotale));
            }
            public void onFinish() {
                tvCountdown.setText("00:00");
                progressBarCircular.setProgress(0);
                Toast.makeText(PacientActivity.this, "Ambulanța a sosit!", Toast.LENGTH_LONG).show();
            }
        }.start();
    }
}