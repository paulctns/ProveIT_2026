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

    private final String API_KEY = BuildConfig.API_KEY;

    // --- INSTANȚIEM BAZA DE DATE ---
    private DB_functions db = new DB_functions();

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
        tvRezultatScanare.setText("Gemini analizează și caută în baza de date...");

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            // 1. Obținem datele de la AI
            String rezultatAI = cereDateDeLaGoogle(pozaBuletin);

            // 2. Extragem CNP-ul din text
            String cnpExtras = extrageCNP(rezultatAI);

            // 3. Verificăm în baza de date MySQL (pe background thread)
            boolean gasitInSistem = false;
            if (!cnpExtras.isEmpty()) {
                gasitInSistem = db.isCnpRegistered(cnpExtras);
            }

            // 4. Trimitem rezultatul înapoi pe UI Thread
            final boolean finalGasitInSistem = gasitInSistem;
            handler.post(() -> {
                progressBarAI.setVisibility(View.GONE);
                tvStatusAI.setVisibility(View.GONE);

                if (rezultatAI != null && !rezultatAI.startsWith("EROARE_")) {

                    if (finalGasitInSistem) {
                        tvRezultatScanare.setText("✅ PACIENT GĂSIT ÎN BAZA DE DATE\n\n" + rezultatAI);
                        tvRezultatScanare.setTextColor(android.graphics.Color.parseColor("#008800"));
                    } else {
                        tvRezultatScanare.setText("⚠️ PACIENT NOU (GUEST MODE)\n\n" + rezultatAI);
                        tvRezultatScanare.setTextColor(android.graphics.Color.parseColor("#FF8800"));
                    }

                    btnTrimiteCatreAI.setVisibility(View.VISIBLE);
                    btnTrimiteCatreAI.setText("CONFIRMĂ ȘI SOLICITĂ AJUTOR");
                    btnTrimiteCatreAI.setBackgroundColor(android.graphics.Color.parseColor("#2196F3"));

                    btnTrimiteCatreAI.setOnClickListener(vNext -> {
                        Intent intent = new Intent(ScanActivity.this, PacientActivity.class);
                        intent.putExtra("DATE_PACIENT_SCANAT", rezultatAI);
                        intent.putExtra("CNP_PACIENT", cnpExtras); // Trimitem CNP-ul pentru a fi salvat în DB în ecranul următor
                        intent.putExtra("ESTE_IN_SISTEM", finalGasitInSistem);
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

    private String cereDateDeLaGoogle(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject contentObj = new JSONObject();
            JSONArray partsArray = new JSONArray();

            JSONObject textPart = new JSONObject();
            textPart.put("text", "Extrage din acest buletin românesc datele: Nume, Prenume, CNP, Serie, Număr. Răspunde direct cu lista lor.");
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

            // Am schimbat în gemini-2.5-flash
            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key=" + API_KEY);
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
                InputStreamReader isr = (conn.getErrorStream() != null) ?
                        new InputStreamReader(conn.getErrorStream()) :
                        new InputStreamReader(conn.getInputStream());
                Scanner s = new Scanner(isr);
                s.useDelimiter("\\A");
                String errorMsg = s.hasNext() ? s.next() : "";
                s.close();
                return "EROARE_API " + responseCode + ": " + errorMsg;
            }
        } catch (Exception e) {
            return "EROARE_SISTEM: " + e.getMessage();
        }
    }
}