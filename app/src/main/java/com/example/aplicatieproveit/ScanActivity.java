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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ScanActivity extends AppCompatActivity {

    private ImageView ivPreviewBuletin;
    private Button btnDeschideCamera, btnTrimiteCatreAI;
    private ProgressBar progressBarAI;
    private TextView tvStatusAI;
    private Bitmap pozaBuletin;
    private DB_functions dbFunctions;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

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

        dbFunctions = new DB_functions();

        // La click, deschidem camera telefonului
        btnDeschideCamera.setOnClickListener(v -> {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            cameraLauncher.launch(takePictureIntent);
        });

        // La click pe Analizare
        btnTrimiteCatreAI.setOnClickListener(v -> {
            btnTrimiteCatreAI.setVisibility(View.GONE);
            progressBarAI.setVisibility(View.VISIBLE);
            tvStatusAI.setVisibility(View.VISIBLE);

            executorService.execute(() -> {
                // Simulăm procesul AI care extrage datele dintr-un buletin real
                // În realitate, aici am trimite poza către Gemini AI
                try {
                    Thread.sleep(2500); // Simulăm latența AI
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Date simulate extrase din buletinul scanat
                String cnpSimulat = "1900101123456";
                String serieSimulata = "RX";
                String numarSimulat = "123456";

                // Verificăm în baza de date
                String patientCnp = dbFunctions.loginPatientDateBuletin(cnpSimulat, serieSimulata, numarSimulat);

                runOnUiThread(() -> {
                    if (patientCnp != null) {
                        Toast.makeText(ScanActivity.this, "Identificare reușită!", Toast.LENGTH_LONG).show();
                        Intent intent = new Intent(ScanActivity.this, PacientActivity.class);
                        intent.putExtra("PATIENT_CNP", patientCnp);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(ScanActivity.this, "Pacientul nu a fost găsit. Vă rugăm să vă înregistrați.", Toast.LENGTH_LONG).show();
                        btnTrimiteCatreAI.setVisibility(View.VISIBLE);
                        progressBarAI.setVisibility(View.GONE);
                        tvStatusAI.setVisibility(View.GONE);
                    }
                });
            });
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}