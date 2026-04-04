package com.example.aplicatieproveit;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
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
import androidx.core.content.FileProvider;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanActivity extends AppCompatActivity {

    private ImageView ivPreviewBuletin;
    private Button btnDeschideCamera, btnTrimiteCatreAI;
    private ProgressBar progressBarAI;
    private TextView tvStatusAI, tvRezultatScanare;

    // Variabile pentru salvarea pozei la rezoluție maximă
    private Bitmap pozaBuletinFullResolution;
    private String currentPhotoPath;
    private Uri photoURI;

    private final String API_KEY = "AIzaSyCxcAdg8T9O_yXSlv-qrXUoaOZ6hprUdrs";

    // Launcher pentru cerere permisiune aparat foto
    private final ActivityResultLauncher<String> requestPermissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                if (isGranted) {
                    // Dacă permisiunea e acordată, încercăm să deschidem camera
                    deschideCameraRezolutieMaxima();
                } else {
                    Toast.makeText(this, "Avem nevoie de acces la cameră pentru a scana buletinul!", Toast.LENGTH_LONG).show();
                }
            });

    // Launcher pentru camera foto (versiunea cu salvat fișier)
    private final ActivityResultLauncher<Intent> cameraLauncherFullResolution = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK) {
                    try {
                        // 1. Preluăm poza de rezoluție maximă din fișierul salvat
                        Bitmap bitmap = handleSamplingAndRotationBitmap(photoURI);
                        pozaBuletinFullResolution = bitmap;

                        // 2. Afișăm poza clară în ImageView
                        ivPreviewBuletin.setImageBitmap(pozaBuletinFullResolution);

                        // ASTA REZOLVĂ PROBLEMA TA: Poza va umple ImageView-ul
                        ivPreviewBuletin.setScaleType(ImageView.ScaleType.FIT_CENTER);

                        btnDeschideCamera.setVisibility(View.GONE);
                        btnTrimiteCatreAI.setVisibility(View.VISIBLE);
                        tvRezultatScanare.setText("Poza a fost făcută. Apasă Analizează.");
                        tvRezultatScanare.setTextColor(android.graphics.Color.BLACK);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Toast.makeText(this, "Eroare la preluarea pozei", Toast.LENGTH_SHORT).show();
                    }
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_scan);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        ivPreviewBuletin = findViewById(R.id.ivPreviewBuletin);
        btnDeschideCamera = findViewById(R.id.btnDeschideCamera);
        btnTrimiteCatreAI = findViewById(R.id.btnTrimiteCatreAI);
        progressBarAI = findViewById(R.id.progressBarAI);
        tvStatusAI = findViewById(R.id.tvStatusAI);
        tvRezultatScanare = findViewById(R.id.tvRezultatScanare);

        btnDeschideCamera.setOnClickListener(v -> verificarePermisiuneSiCamera());
        btnTrimiteCatreAI.setOnClickListener(v -> trimiteLaGeminiAPI());
    }

    // Funcția care verifică dacă avem permisiune la camera
    private void verificarePermisiuneSiCamera() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            // Dacă avem permisiune, deschidem camera direct
            deschideCameraRezolutieMaxima();
        } else {
            // Dacă nu, o cerem oficial utilizatorului (pop-up)
            requestPermissionLauncher.launch(Manifest.permission.CAMERA);
        }
    }

    private void deschideCameraRezolutieMaxima() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Creăm fișierul temporar unde se va salva poza
        File photoFile = null;
        try {
            photoFile = createImageFile();
        } catch (IOException ex) {
            Toast.makeText(this, "Eroare fișier: " + ex.getMessage(), Toast.LENGTH_SHORT).show();
            return;
        }

        // Dacă fișierul s-a creat cu succes, deschidem camera
        if (photoFile != null) {
            photoURI = FileProvider.getUriForFile(this,
                    "com.example.aplicatieproveit.fileprovider",
                    photoFile);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
            cameraLauncherFullResolution.launch(takePictureIntent);
        }
    }

    private void trimiteLaGeminiAPI() {
        if (pozaBuletinFullResolution == null) return;

        btnTrimiteCatreAI.setVisibility(View.GONE);
        progressBarAI.setVisibility(View.VISIBLE);
        tvStatusAI.setVisibility(View.VISIBLE);
        tvStatusAI.setText("Gemini analizează imaginea la rezoluție maximă...");
        tvRezultatScanare.setText(""); // Curățăm rezultatul vechi

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            // Trimitem poza mare la AI
            String rezultatAI = cereDateDeLaGoogle(pozaBuletinFullResolution);

            handler.post(() -> {
                progressBarAI.setVisibility(View.GONE);
                tvStatusAI.setVisibility(View.GONE);

                if (rezultatAI != null && !rezultatAI.startsWith("EROARE_")) {
                    tvRezultatScanare.setText("DATE EXTRASE:\n\n" + rezultatAI);
                    tvRezultatScanare.setTextColor(android.graphics.Color.BLUE);
                    Toast.makeText(ScanActivity.this, "Succes!", Toast.LENGTH_SHORT).show();
                } else {
                    btnTrimiteCatreAI.setVisibility(View.VISIBLE);
                    tvRezultatScanare.setText("A apărut o problemă: " + rezultatAI);
                    tvRezultatScanare.setTextColor(android.graphics.Color.RED);
                }
            });
        });
    }

    private String cereDateDeLaGoogle(Bitmap bitmap) {
        try {
            // Transformăm poza mare într-un format text Base64 (pentru a o trimite prin API)
            // Folosim JPEG la 90% calitate pentru un request mai rapid dar precis
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.NO_WRAP);

            JSONObject jsonBody = new JSONObject();
            JSONArray contentsArray = new JSONArray();
            JSONObject contentsObj = new JSONObject();
            JSONArray partsArray = new JSONArray();

            // Prompt mai detaliat pentru a profita de rezoluția mare
            JSONObject textPart = new JSONObject();
            textPart.put("text", "Extrage din acest buletin românesc de înaltă rezoluție următoarele date într-o listă clară: Nume, Prenume (verifică numele cu cratimă), CNP (toate cele 13 cifre exact), Serie, Număr. Fii foarte atent la cifre.");
            partsArray.put(textPart);

            JSONObject imagePart = new JSONObject();
            JSONObject inlineData = new JSONObject();
            inlineData.put("mimeType", "image/jpeg");
            inlineData.put("data", base64Image);
            imagePart.put("inlineData", inlineData);
            partsArray.put(imagePart);

            contentsObj.put("parts", partsArray);
            contentsArray.put(contentsObj);
            jsonBody.put("contents", contentsArray);

            URL url = new URL("https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setDoOutput(true);

            OutputStream os = conn.getOutputStream();
            os.write(jsonBody.toString().getBytes(StandardCharsets.UTF_8));
            os.flush();
            os.close();

            if (conn.getResponseCode() == 200) {
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
                return "EROARE_API: " + conn.getResponseCode();
            }
        } catch (Exception e) {
            return "EROARE_SISTEM: " + e.getMessage();
        }
    }

    // --- FUNCȚII AJUTĂTOARE PENTRU REZOLUȚIE MAXIMĂ ---

    // Creează fișierul temporar unde se salvează poza
    private File createImageFile() throws IOException {
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName, ".jpg", storageDir);
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    // Citește Bitmap-ul din fișier, îl optimizează și îl rotește corect
    private Bitmap handleSamplingAndRotationBitmap(Uri selectedImage) throws IOException {
        // Obținem dimensiunile pozei fără a o încărca în memorie
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;

        InputStream imageStream = getContentResolver().openInputStream(selectedImage);
        BitmapFactory.decodeStream(imageStream, null, options);
        imageStream.close();

        // Calculăm cât de mult trebuie să micșorăm poza pentru AI (să nu fie uriașă în request)
        // 2048px e o rezoluție foarte bună pentru Gemini
        options.inSampleSize = calculateInSampleSize(options, 2048, 2048);
        options.inJustDecodeBounds = false;

        // Încărcăm poza optimizată
        imageStream = getContentResolver().openInputStream(selectedImage);
        Bitmap img = BitmapFactory.decodeStream(imageStream, null, options);
        imageStream.close();

        // Rezolvăm rotația ( Samsung S10e rotește pozele aiurea uneori)
        return rotateImageIfRequired(img, selectedImage);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    private Bitmap rotateImageIfRequired(Bitmap img, Uri selectedImage) throws IOException {
        InputStream input = getContentResolver().openInputStream(selectedImage);
        ExifInterface ei;
        if (android.os.Build.VERSION.SDK_INT > 23)
            ei = new ExifInterface(input);
        else
            ei = new ExifInterface(selectedImage.getPath());
        input.close();

        int orientation = ei.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                return rotateImage(img, 90);
            case ExifInterface.ORIENTATION_ROTATE_180:
                return rotateImage(img, 180);
            case ExifInterface.ORIENTATION_ROTATE_270:
                return rotateImage(img, 270);
            default:
                return img;
        }
    }

    private Bitmap rotateImage(Bitmap img, int degree) {
        Matrix matrix = new Matrix();
        matrix.postRotate(degree);
        Bitmap rotatedImg = Bitmap.createBitmap(img, 0, 0, img.getWidth(), img.getHeight(), matrix, true);
        img.recycle();
        return rotatedImg;
    }
}