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
import android.speech.RecognizerIntent;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class PacientActivity extends AppCompatActivity {

    private SwitchCompat switchPentruAltcineva;
    private EditText etDescriereUrgenta;
    private Button btnRequestAmbulanta, btnLogOut;
    private ImageButton btnVoiceInput;
    private View layoutPrincipal;

    private RelativeLayout layoutAnimatieUrmarire;
    private ProgressBar progressBarCircular;
    private TextView tvCountdown;
    private TextView tvMesajStatus;

    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    private final String API_KEY = "AIzaSyDrGnxDs3aaFDT32Ir-6VPZwH9i8MQsE3A";

    private double userLat = 0.0, userLon = 0.0;
    private int etaMinute = 1;
    private String textDescriereTemp = "";
    private String idDocumentCurent = "";

    private final ActivityResultLauncher<Intent> voiceLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    ArrayList<String> matches = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    if (matches != null && !matches.isEmpty()) {
                        etDescriereUrgenta.setText(matches.get(0));
                    }
                }
            });

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
        TriageEngine.descarcaSiAntreneaza("10.177.102.17");

        layoutPrincipal = findViewById(R.id.main);
        layoutAnimatieUrmarire = findViewById(R.id.layoutAnimatieUrmarire);
        progressBarCircular = findViewById(R.id.progressBarCircular);
        tvCountdown = findViewById(R.id.tvCountdown);
        tvMesajStatus = (TextView) layoutAnimatieUrmarire.getChildAt(2);

        switchPentruAltcineva = findViewById(R.id.switchPentruAltcineva);
        etDescriereUrgenta = findViewById(R.id.etDescriereUrgenta);
        btnRequestAmbulanta = findViewById(R.id.btnRequestAmbulanta);
        btnLogOut = findViewById(R.id.btnLogOut);
        btnVoiceInput = findViewById(R.id.btnVoiceInput);

        btnVoiceInput.setOnClickListener(v -> {
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
            intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Descrieți simptomele...");
            try { voiceLauncher.launch(intent); }
            catch (Exception e) { Toast.makeText(this, "Microfon indisponibil.", Toast.LENGTH_SHORT).show(); }
        });

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
                userLat = location.getLatitude(); userLon = location.getLongitude();
                etaMinute = 1;
            }
        } catch (SecurityException e) { e.printStackTrace(); }
        pornesteFluxAI(descriere);
    }

    private void pornesteFluxAI(String descriere) {
        btnRequestAmbulanta.setText("AI-ul analizează simptomele...");
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            // 1. Apelăm Gemini pentru a extrage cuvintele cheie
            String rezultatGemini = cereSimptomeDeLaGemini(descriere);
            handler.post(() -> {
                if (rezultatGemini.startsWith("EROARE")) {
                    salveazaUrgenta(descriere, rezultatGemini, "Triaj Manual", 2, "Pacient", "Fără istoric");
                } else {
                    List<String> simptome = Arrays.asList(rezultatGemini.split("\\s*,\\s*"));
                    // 2. Trecem la AI-ul nostru de Machine Learning
                    executaTriajMatematic(descriere, simptome);
                }
            });
        });
    }

    private String cereSimptomeDeLaGemini(String descriere) {
        try {
            String prompt = "Ești un sistem medical. Alege din următoarea listă strict simptomele care se regăsesc în text: durere_piept, transpiratie, durere_brat_stang, respiratie_grea, asimetrie_faciala, amorteala_brat, dificultate_vorbire, confuzie, umflare_fata, respiratie_suieratoare, eruptie_cutanata, puls_rapid, durere_abdomen_dreapta_jos, greata, varsaturi, febra_usoara, tuse_seaca, senzatie_sufocare, durere_intensa_os, deformare_zona, imposibilitate_miscare, umflare, crampe_abdominale, diaree, durere_lombara, durere_iradiata_inghinal, sange_urina, tuse, nas_infundat, durere_gat, mancarime, roseata_piele, stranut, ochi_inlacrimati, oboseala. Răspunde STRICT cu simptomele găsite, separate prin virgulă. Text: " + descriere;
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

            String cleanApiKey = API_KEY.trim();
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + cleanApiKey);
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
            return "EROARE_API: " + conn.getResponseCode();
        } catch (Exception e) { return "EROARE_SISTEM: " + e.getMessage(); }
    }

    private void executaTriajMatematic(String descriere, List<String> simptomePacient) {
        String uid = mAuth.getCurrentUser() != null ? mAuth.getCurrentUser().getUid() : "anonim";

        db.collection("Users").document(uid).get().addOnSuccessListener(userDoc -> {
            String numePacient = "Pacient Necunoscut";
            String istoricPentruMedic = "Fără antecedente medicale declarate.";

            if (userDoc.exists()) {
                if (userDoc.contains("nume")) numePacient = userDoc.getString("nume");
                else if (userDoc.contains("email")) numePacient = userDoc.getString("email");

                if (userDoc.contains("istoric_medical")) {
                    List<String> istoricMedical = (List<String>) userDoc.get("istoric_medical");
                    if (istoricMedical != null && !istoricMedical.isEmpty()) {
                        istoricPentruMedic = android.text.TextUtils.join(", ", istoricMedical);
                    }
                }
            }

            final String finalNume = numePacient;
            final String finalIstoricText = istoricPentruMedic;

            // =========================================================
            // 1. CONVERTIM SIMPTOMELE GEMINI PENTRU PYTHON AI
            // =========================================================
            boolean fever = simptomePacient.contains("febra_usoara");
            boolean cough = simptomePacient.contains("tuse") || simptomePacient.contains("tuse_seaca");
            boolean fatigue = simptomePacient.contains("oboseala");
            boolean breathing = simptomePacient.contains("respiratie_grea") || simptomePacient.contains("senzatie_sufocare");

            // Extragem Vârsta din Firebase (dacă ai salvat-o ca String la SignIn)
            int age = 30; // Default
            if (userDoc.contains("varsta") && userDoc.getString("varsta") != null) {
                try { age = Integer.parseInt(userDoc.getString("varsta")); } catch (Exception ignored) {}
            }

            // Trimitem parametrii default pentru Gen (1=Masculin), Tensiune(1=Normal), Colesterol(1=Normal)
            int gen = 1;
            int tensiune = 1;
            int colesterol = 1;

            // =========================================================
            // 2. APELĂM NOUL MOTOR DE MACHINE LEARNING DIN PYTHON
            // =========================================================
            TriageEngine.evalueazaCuAI(fever, cough, fatigue, breathing, age, gen, tensiune, colesterol, new TriageEngine.AICallback() {
                @Override
                public void onSuccess(String diagnostic_ai, String departament_ai, long prioritate, double probabilitate) {
                    runOnUiThread(() -> {
                        // AI-ul a răspuns cu succes! Salvăm urgența în Firebase
                        salveazaUrgenta(descriere, diagnostic_ai, departament_ai, (int) prioritate, finalNume, finalIstoricText);
                    });
                }

                @Override
                public void onError(String mesajEroare) {
                    runOnUiThread(() -> {
                        Toast.makeText(PacientActivity.this, "Eroare AI: " + mesajEroare, Toast.LENGTH_LONG).show();
                        // Dacă pică Python-ul, punem un sistem de rezervă (Fallback) ca să nu se blocheze aplicația
                        salveazaUrgenta(descriere, "Diagnostic Neclar (Eroare Server AI)", "Triaj General", 4, finalNume, finalIstoricText);
                    });
                }
            });
        });
    }

    private void salveazaUrgenta(String descriere, String diagnosticAI, String departament, int prioritate, String numePacient, String istoricText) {
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

        // DATELE NOI SALVATE PENTRU MEDIC:
        urgenta.put("descriere", descriere);
        urgenta.put("nume_pacient", numePacient);
        urgenta.put("istoric_pacient", istoricText);

        db.collection("Urgenti").add(urgenta).addOnSuccessListener(doc -> {
            idDocumentCurent = doc.getId();
            pornesteAnimatieUrmarireDus(etaMinute, departament);
        });
    }

    private void pornesteAnimatieUrmarireDus(int minute, String departamentAI) {
        layoutPrincipal.setVisibility(View.GONE);
        layoutAnimatieUrmarire.setVisibility(View.VISIBLE);
        tvMesajStatus.setText("Ambulanța vine spre tine!");
        tvCountdown.setTextColor(android.graphics.Color.parseColor("#D32F2F"));

        long milisecundeTotale = (long) minute * 60 * 1000;
        new CountDownTimer(milisecundeTotale, 1000) {
            public void onTick(long millisUntilFinished) {
                int min = (int) (millisUntilFinished / 1000) / 60;
                int sec = (int) (millisUntilFinished / 1000) % 60;
                tvCountdown.setText(String.format("%02d:%02d", min, sec));
                progressBarCircular.setProgress((int) (millisUntilFinished * 100 / milisecundeTotale));
            }
            public void onFinish() {
                Toast.makeText(PacientActivity.this, "Ambulanța a ajuns! Te preluăm...", Toast.LENGTH_SHORT).show();
                pornesteAnimatieUrmarireIntors(minute, departamentAI);
            }
        }.start();
    }

    private void pornesteAnimatieUrmarireIntors(int minute, String departamentAI) {
        tvMesajStatus.setText("Ne îndreptăm spre spital...");
        tvCountdown.setTextColor(android.graphics.Color.parseColor("#FF9800"));

        long milisecundeTotale = (long) minute * 60 * 1000;
        new CountDownTimer(milisecundeTotale, 1000) {
            public void onTick(long millisUntilFinished) {
                int min = (int) (millisUntilFinished / 1000) / 60;
                int sec = (int) (millisUntilFinished / 1000) % 60;
                tvCountdown.setText(String.format("%02d:%02d", min, sec));
                progressBarCircular.setProgress((int) (millisUntilFinished * 100 / milisecundeTotale));
            }
            public void onFinish() {
                tvMesajStatus.setText("Am ajuns. Se alocă resursele...");
                alocaResurseSiInterneaza(departamentAI);
            }
        }.start();
    }

    private void alocaResurseSiInterneaza(String departamentAI) {
        db.collection("Sali").whereEqualTo("departament", departamentAI).whereEqualTo("este_libera", true)
                .limit(1).get().addOnSuccessListener(sali -> {

                    String numeSala = "Fără sală alocată (Hol)";
                    if (!sali.isEmpty()) {
                        QueryDocumentSnapshot docSala = (QueryDocumentSnapshot) sali.getDocuments().get(0);
                        numeSala = docSala.getString("nume");
                        db.collection("Sali").document(docSala.getId()).update("este_libera", false);
                    }
                    final String finalNumeSala = numeSala;

                    db.collection("Medici").whereEqualTo("specializare", departamentAI).whereEqualTo("este_liber", true)
                            .limit(1).get().addOnSuccessListener(medici -> {

                                String numeMedic = "Medic indisponibil";
                                if (!medici.isEmpty()) {
                                    QueryDocumentSnapshot docMedic = (QueryDocumentSnapshot) medici.getDocuments().get(0);
                                    numeMedic = docMedic.getString("nume");
                                    db.collection("Medici").document(docMedic.getId()).update("este_liber", false);
                                }

                                Map<String, Object> updateSpital = new HashMap<>();
                                updateSpital.put("status", "IN_SPITAL");
                                updateSpital.put("sala_alocata", finalNumeSala);
                                updateSpital.put("medic_alocat", numeMedic);

                                db.collection("Urgenti").document(idDocumentCurent).update(updateSpital)
                                        .addOnSuccessListener(aVoid -> {
                                            Toast.makeText(PacientActivity.this, "Ai fost internat în " + finalNumeSala, Toast.LENGTH_LONG).show();
                                            layoutAnimatieUrmarire.setVisibility(View.GONE);
                                            layoutPrincipal.setVisibility(View.VISIBLE);
                                            etDescriereUrgenta.setText("");
                                            btnRequestAmbulanta.setText("🚨 SOLICITĂ AMBULANȚĂ (GPS)");
                                            btnRequestAmbulanta.setEnabled(true);
                                        });
                            });
                });
    }
}