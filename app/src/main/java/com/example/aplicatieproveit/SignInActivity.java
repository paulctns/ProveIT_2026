package com.example.aplicatieproveit;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class SignInActivity extends AppCompatActivity {

    private RadioGroup rgTipUtilizator;
    private RadioButton rbPacient, rbMedic;
    private LinearLayout layoutDateMedicale, layoutDateMedic;
    private EditText etVarsta, etEmailInstitutional, etIDInstitutional;
    private Button btnSubmitSignIn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_sign_in);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        rgTipUtilizator = findViewById(R.id.rgTipUtilizator);
        rbPacient = findViewById(R.id.rbPacient);
        rbMedic = findViewById(R.id.rbMedic);
        layoutDateMedicale = findViewById(R.id.layoutDateMedicale);
        layoutDateMedic = findViewById(R.id.layoutDateMedic);

        // Noile campuri
        etVarsta = findViewById(R.id.etVarsta);
        etEmailInstitutional = findViewById(R.id.etEmailInstitutional);
        etIDInstitutional = findViewById(R.id.etIDInstitutional);

        btnSubmitSignIn = findViewById(R.id.btnSubmitSignIn);

        rgTipUtilizator.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbPacient) {
                layoutDateMedicale.setVisibility(View.VISIBLE);
                layoutDateMedic.setVisibility(View.GONE);
            } else if (checkedId == R.id.rbMedic) {
                layoutDateMedicale.setVisibility(View.VISIBLE);
                layoutDateMedic.setVisibility(View.VISIBLE);
            }
        });

        btnSubmitSignIn.setOnClickListener(v -> {
            String varsta = etVarsta.getText().toString();
            if (varsta.isEmpty()) {
                Toast.makeText(this, "Te rugăm să introduci vârsta!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (rbPacient.isChecked()) {
                Toast.makeText(this, "Cont Pacient creat!", Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(this, "Cont Medic creat! Se verifică ID-ul instituțional.", Toast.LENGTH_LONG).show();
            }
        });
    }
}