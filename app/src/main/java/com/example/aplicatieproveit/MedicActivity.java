package com.example.aplicatieproveit;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MedicActivity extends AppCompatActivity {

    private CardView cardPacient1, cardPacient2, cardPacient3;
    private android.widget.TextView tvMedicWelcome, tvMedicSpecialty;
    private Button btnLogOutMedic;
    private DB_functions dbFunctions;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();
    private String operatorCnp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_medic);

        operatorCnp = getIntent().getStringExtra("OPERATOR_CNP");

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // Mapăm elementele
        tvMedicWelcome = findViewById(R.id.tvMedicWelcome);
        tvMedicSpecialty = findViewById(R.id.tvMedicSpecialty);
        cardPacient1 = findViewById(R.id.cardPacient1);
        cardPacient2 = findViewById(R.id.cardPacient2);
        cardPacient3 = findViewById(R.id.cardPacient3);
        btnLogOutMedic = findViewById(R.id.btnLogOutMedic);

        dbFunctions = new DB_functions();

        // Încărcăm datele medicului din DB
        if (operatorCnp != null) {
            executorService.execute(() -> {
                String name = dbFunctions.getOperatorName(operatorCnp);
                String specialty = dbFunctions.getOperatorSpecialty(operatorCnp);
                runOnUiThread(() -> {
                    tvMedicWelcome.setText("Bine ați venit, " + name);
                    tvMedicSpecialty.setText("Specialitate: " + specialty);
                });
            });
        }

        // Actiune la click pe Pacientul 1 (ROȘU)
        cardPacient1.setOnClickListener(v -> {
            arataDosarPacient(
                    1, // ID simulat
                    "Ion Popescu",
                    "Dureri severe în piept radiind spre brațul stâng, dificultăți majore de respirație, transpirație rece. Risc iminent de infarct miocardic.",
                    "Grupa Sânge: A(II) Pozitiv\nBoli cronice: Hipertensiune arterială, Diabet tip 2\nAlergii: Penicilină\nMedicamente curente: Metformin, Enalapril."
            );
        });

        // Actiune la click pe Pacientul 2 (GALBEN)
        cardPacient2.setOnClickListener(v -> {
            arataDosarPacient(
                    2, // ID simulat
                    "Maria Ionescu",
                    "Tăietură adâncă la brațul drept cauzată de un accident casnic cu sticla. Sângerare controlată, dar necesită sutură urgentă.",
                    "Grupa Sânge: 0(I) Pozitiv\nBoli cronice: Niciuna\nAlergii: Niciuna\nMedicamente curente: Anticoncepționale."
            );
        });

        // Actiune la click pe Pacientul 3 (VERDE)
        cardPacient3.setOnClickListener(v -> {
            arataDosarPacient(
                    3, // ID simulat
                    "Andrei Vasile",
                    "Febră 38.5, durere în gât, tuse seacă de 3 zile. Stare generală alterată.",
                    "Grupa Sânge: B(III) Negativ\nBoli cronice: Astm bronșic ușor\nAlergii: Praf, Polen\nMedicamente curente: Inhalator Ventolin la nevoie."
            );
        });

        // Actiune Log Out
        btnLogOutMedic.setOnClickListener(v -> {
            Intent intent = new Intent(MedicActivity.this, MainActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });
    }

    // Funcția care generează pop-up-ul cu dosarul pe ecran
    private void arataDosarPacient(int queueId, String nume, String diagnosticAI, String istoricMedical) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Dosar: " + nume);

        // Asamblăm textul care apare în interior
        String mesajComplet = "📋 REZUMAT AI (SIMPTOME):\n" + diagnosticAI + "\n\n" +
                "📂 ISTORIC MEDICAL:\n" + istoricMedical;

        builder.setMessage(mesajComplet);
        
        builder.setPositiveButton("Finalizează Intervenția", (dialog, which) -> {
            executorService.execute(() -> {
                boolean success = dbFunctions.updateRequestStatus(queueId, "completed");
                runOnUiThread(() -> {
                    if (success) {
                        Toast.makeText(this, "Intervenție finalizată pentru " + nume, Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "Eroare la actualizarea statusului.", Toast.LENGTH_SHORT).show();
                    }
                });
            });
        });

        builder.setNegativeButton("Închide", (dialog, which) -> dialog.dismiss());

        // Afișăm pop-up-ul
        builder.show();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}