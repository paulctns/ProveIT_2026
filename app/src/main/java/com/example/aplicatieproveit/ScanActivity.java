package com.example.aplicatieproveit;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
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
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ScanActivity extends AppCompatActivity {

    private ImageView ivPreviewBuletin;
    private Button btnDeschideCamera, btnTrimiteCatreAI;
    private ProgressBar progressBarAI;
    private TextView tvStatusAI;
    private Bitmap pozaBuletin; // Aici stocăm poza făcută

    // Aceasta este funcția modernă care deschide camera și așteaptă rezultatul
    private final ActivityResultLauncher<Intent> cameraLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    // Am primit poza!
                    Bundle extras = result.getData().getExtras();
                    pozaBuletin = (Bitmap) extras.get("data");

                    // Afișăm poza pe ecran
                    ivPreviewBuletin.setImageBitmap(pozaBuletin);

                    // Ascundem butonul de cameră și îl arătăm pe cel de trimitere la AI
                    btnDeschideCamera.setVisibility(View.GONE);
                    btnTrimiteCatreAI.setVisibility(View.VISIBLE);
                } else {
                    Toast.makeText(this, "Scanare anulată", Toast.LENGTH_SHORT).show();
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

        // La click, deschidem camera telefonului
        btnDeschideCamera.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(takePictureIntent);
        });

        // La click pe Analizare
        btnTrimiteCatreAI.setOnClickListener(v -> {
            // Ascundem butonul și arătăm animația de încărcare
            btnTrimiteCatreAI.setVisibility(View.GONE);
            progressBarAI.setVisibility(View.VISIBLE);
            tvStatusAI.setVisibility(View.VISIBLE);

            // Simulăm timpul de gândire al AI-ului (2.5 secunde) pentru prezentare
            new Handler(Looper.getMainLooper()).postDelayed(() -> {
                // Aici vom adăuga ulterior codul REAL care trimite poza la Google Gemini

                Toast.makeText(ScanActivity.this, "Scanare reușită! Datele au fost extrase.", Toast.LENGTH_LONG).show();

                // Trimitem pacientul logat în pagina principală (Dashboard)
                Intent intent = new Intent(ScanActivity.this, PacientActivity.class);
                startActivity(intent);
                finish(); // Închide pagina de scanare

            }, 2500); // 2500 milisecunde = 2.5 secunde
        });
    }
}