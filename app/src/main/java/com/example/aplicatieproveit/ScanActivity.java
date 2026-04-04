package com.example.aplicatieproveit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Base64;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ScanActivity extends AppCompatActivity {

    private ImageView ivPreviewBuletin;
    private Button btnDeschideCamera, btnTrimiteCatreAI;
    private ProgressBar progressBarAI;
    private TextView tvStatusAI, tvRezultatScanare;
    private Bitmap pozaBuletin;

    // Folosim cheia protejată prin BuildConfig
    private final String API_KEY = BuildConfig.API_KEY;

    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    deschideCamera();
                } else {
                    Toast.makeText(this, "Permisiunea este necesară!", Toast.LENGTH_SHORT).show();
                }
            });

    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Bundle extras = result.getData().getExtras();
                    pozaBuletin = (Bitmap) extras.get("data");
                    ivPreviewBuletin.setImageBitmap(pozaBuletin);
                    ivPreviewBuletin.setScaleType(ImageView.ScaleType.FIT_CENTER);

                    btnDeschideCamera.setVisibility(View.GONE);
                    btnTrimiteCatreAI.setVisibility(View.VISIBLE);
                    btnTrimiteCatreAI.setText("Analizează buletinul");
                    tvRezultatScanare.setText("Imagine capturată. Apasă pe 'Analizează'.");
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan);

        View mainView = findViewById(R.id.main);
        if (mainView != null) {
            ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
                Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
                v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
                return insets;
            });
        }

        ivPreviewBuletin = findViewById(R.id.ivPreviewBuletin);
        btnDeschideCamera = findViewById(R.id.btnDeschideCamera);
        btnTrimiteCatreAI = findViewById(R.id.btnTrimiteCatreAI);
        progressBarAI = findViewById(R.id.progressBarAI);
        tvStatusAI = findViewById(R.id.tvStatusAI);
        tvRezultatScanare = findViewById(R.id.tvRezultatScanare);

        btnDeschideCamera.setOnClickListener(v -> {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                deschideCamera();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA);
            }
        });

        btnTrimiteCatreAI.setOnClickListener(v -> trimiteLaGeminiAPI());
    }

    private void deschideCamera() {
        Intent takePictureIntent = new Intent(android.provider.MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(takePictureIntent);
    }

    private void trimiteLaGeminiAPI() {
        if (pozaBuletin == null) return;

        btnTrimiteCatreAI.setVisibility(View.GONE);
        progressBarAI.setVisibility(View.VISIBLE);
        tvStatusAI.setVisibility(View.VISIBLE);
        tvRezultatScanare.setText("Gemini 3 Flash analizează...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            String rezultatAI = cereDateDeLaGoogle(pozaBuletin);
            handler.post(() -> {
                progressBarAI.setVisibility(View.GONE);
                tvStatusAI.setVisibility(View.GONE);
                if (rezultatAI != null && !rezultatAI.startsWith("EROARE_")) {

                    String cnpExtras = extrageCNP(rezultatAI);
                    boolean gasitInSistem = verificaInBazaDeDateSimulata(cnpExtras);

                    if (gasitInSistem) {
                        tvRezultatScanare.setText("✅ UTILIZATOR ÎNREGISTRAT\n\n" + rezultatAI);
                        tvRezultatScanare.setTextColor(android.graphics.Color.parseColor("#008800"));
                    } else {
                        tvRezultatScanare.setText("⚠️ MOD GUEST (NECUNOSCUT)\n\n" + rezultatAI);
                        tvRezultatScanare.setTextColor(android.graphics.Color.parseColor("#FF8800"));
                    }

                    btnTrimiteCatreAI.setVisibility(View.VISIBLE);
                    btnTrimiteCatreAI.setText("CONFIRMĂ ȘI CONTINUĂ");
                    btnTrimiteCatreAI.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"));

                    btnTrimiteCatreAI.setOnClickListener(vNext -> {
                        Intent intent = new Intent(ScanActivity.this, PacientActivity.class);
                        intent.putExtra("DATE_PACIENT_SCANAT", rezultatAI);
                        intent.putExtra("ESTE_IN_SISTEM", gasitInSistem);
                        startActivity(intent);
                    });

                } else {
                    btnTrimiteCatreAI.setVisibility(View.VISIBLE);
                    tvRezultatScanare.setText("Problemă: " + rezultatAI);
                    tvRezultatScanare.setTextColor(android.graphics.Color.RED);
                }
            });
        });
    }

    private String extrageCNP(String text) {
        Matcher m = Pattern.compile("\\d{13}").matcher(text);
        if (m.find()) return m.group();
        return "";
    }

    private boolean verificaInBazaDeDateSimulata(String cnp) {
        // Asigură-te că aceste CNP-uri sunt exact cele de pe buletinele de test
        String cnpPaul = "1900101123456";
        String cnpSilviu = "5031004270014";
        return cnp.equals(cnpPaul) || cnp.equals(cnpSilviu);
    }

    private String cereDateDeLaGoogle(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray partsArray = new JSONArray();

            JSONObject textPart = new JSONObject();
            textPart.put("text", "Extrage din acest buletin românesc: Nume, Prenume, CNP, Serie, Număr. Răspunde doar cu lista.");
            partsArray.put(textPart);

            JSONObject imagePart = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mime_type", "image/jpeg");
            inlineData.put("data", base64Image);
            imagePart.put("inline_data", inlineData);
            partsArray.put(imagePart);

            contentObj.put("parts", partsArray);
            contentsArray.put(contentObj);
            jsonBody.put("contents", contentsArray);

            // ACTUALIZARE URL: Folosim Gemini 3 Flash (Default 2026)
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY);

            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            os.flush(); os.close();

            int responseCode = conn.getResponseCode();
            if (responseCode == 200) {
                Scanner scanner = new Scanner(new InputStreamReader(conn.getInputStream()));
                scanner.useDelimiter("\\A");
                String responseStr = scanner.hasNext() ? scanner.next() : "";
                scanner.close();
                JSONObject jsonResponse = new JSONObject(responseStr);
                return jsonResponse.getJSONArray("candidates")
                        .getJSONObject(0)
                        .getJSONObject("content")
                        .getJSONArray("parts")
                        .getJSONObject(0)
                        .getString("text");
            } else {
                // Citim eroarea detaliată pentru debug în caz că dă 400
                Scanner s = new Scanner(new InputStreamReader(conn.getErrorStream()));
                s.useDelimiter("\\A");
                String error = s.hasNext() ? s.next() : "";
                s.close();
                return "EROARE_API " + responseCode + ": " + error;
            }
        } catch (Exception e) {
            return "EROARE_SISTEM: " + e.getMessage();
        }
    }
}