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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SignInActivity extends AppCompatActivity {

    private RadioGroup rgTipUtilizator;
    private RadioButton rbPacient, rbMedic;
    private LinearLayout layoutDateMedicale, layoutDateMedic;
    private EditText etEmail, etPassword, etCNP, etVarsta, etGrupaSange, etAlergii, etBoli, etMedicamente;
    private EditText etFirstName, etLastName, etSeries, etNumber;
    private EditText etCodParafa, etEmailInstitutional, etDepartament;
    private Button btnSubmitSignIn;
    private DB_functions dbFunctions;
    private ExecutorService executorService = Executors.newSingleThreadExecutor();

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

        // Mapare campuri
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etFirstName = findViewById(R.id.etFirstName);
        etLastName = findViewById(R.id.etLastName);
        etCNP = findViewById(R.id.etCNP);
        etSeries = findViewById(R.id.etSeries);
        etNumber = findViewById(R.id.etNumber);
        etVarsta = findViewById(R.id.etVarsta);
        etGrupaSange = findViewById(R.id.etGrupaSange);
        etAlergii = findViewById(R.id.etAlergii);
        etBoli = findViewById(R.id.etBoli);
        etMedicamente = findViewById(R.id.etMedicamente);

        etCodParafa = findViewById(R.id.etCodParafa);
        etEmailInstitutional = findViewById(R.id.etEmailInstitutional);
        etDepartament = findViewById(R.id.etDepartament);

        btnSubmitSignIn = findViewById(R.id.btnSubmitSignIn);
        dbFunctions = new DB_functions();

        rgTipUtilizator.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.rbPacient) {
                layoutDateMedicale.setVisibility(View.VISIBLE);
                layoutDateMedic.setVisibility(View.GONE);
                etEmail.setVisibility(View.VISIBLE);
                etGrupaSange.setVisibility(View.VISIBLE);
                etAlergii.setVisibility(View.VISIBLE);
                etBoli.setVisibility(View.VISIBLE);
                etMedicamente.setVisibility(View.VISIBLE);
            } else if (checkedId == R.id.rbMedic) {
                layoutDateMedicale.setVisibility(View.VISIBLE);
                layoutDateMedic.setVisibility(View.VISIBLE);
                etEmail.setVisibility(View.GONE);
                etGrupaSange.setVisibility(View.GONE);
                etAlergii.setVisibility(View.GONE);
                etBoli.setVisibility(View.GONE);
                etMedicamente.setVisibility(View.GONE);
            }
        });

        btnSubmitSignIn.setOnClickListener(v -> {
            boolean isMedic = rbMedic.isChecked();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String fName = etFirstName.getText().toString().trim();
            String lName = etLastName.getText().toString().trim();
            String cnp = etCNP.getText().toString().trim();
            String series = etSeries.getText().toString().trim();
            String number = etNumber.getText().toString().trim();
            String varstaStr = etVarsta.getText().toString().trim();

            if (password.isEmpty() || cnp.isEmpty() || varstaStr.isEmpty() || fName.isEmpty() || lName.isEmpty()) {
                Toast.makeText(this, "Te rugăm să completezi câmpurile obligatorii!", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!isMedic && email.isEmpty()) {
                Toast.makeText(this, "Email-ul personal este obligatoriu pentru pacienți!", Toast.LENGTH_SHORT).show();
                return;
            }

            int varsta = Integer.parseInt(varstaStr);

            String bloodType = etGrupaSange.getText().toString().trim();
            String allergies = etAlergii.getText().toString().trim();
            String chronicDiseases = etBoli.getText().toString().trim();
            String medications = etMedicamente.getText().toString().trim();

            executorService.execute(() -> {
                boolean success = false;
                if (!isMedic) {
                    // Logică Pacient
                    if (dbFunctions.isCnpRegistered(cnp)) {
                        runOnUiThread(() -> Toast.makeText(this, "Acest CNP este deja înregistrat ca pacient!", Toast.LENGTH_LONG).show());
                        return;
                    }
                    if (dbFunctions.isEmailRegistered(email)) {
                        runOnUiThread(() -> Toast.makeText(this, "Acest Email este deja utilizat!", Toast.LENGTH_LONG).show());
                        return;
                    }

                    success = dbFunctions.registerPatient(cnp, fName, lName, series, number,
                                                        email, password, varsta, 
                                                        bloodType, allergies, chronicDiseases, medications);
                    
                    final boolean finalSuccess = success;
                    runOnUiThread(() -> {
                        if (finalSuccess) {
                            Toast.makeText(this, "Cont Pacient creat cu succes!", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Eroare la crearea contului!", Toast.LENGTH_SHORT).show();
                        }
                    });
                } else {
                    // Logică Medic (Operator)
                    String institutionalEmail = etEmailInstitutional.getText().toString().trim();
                    String specialty = etDepartament.getText().toString().trim();
                    String codParafaStr = etCodParafa.getText().toString().trim();
                    int codParafa = codParafaStr.isEmpty() ? 0 : Integer.parseInt(codParafaStr);

                    if (institutionalEmail.isEmpty()) {
                        runOnUiThread(() -> Toast.makeText(this, "Email-ul instituțional este obligatoriu pentru medici!", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    if (dbFunctions.isOperatorCnpRegistered(cnp)) {
                        runOnUiThread(() -> Toast.makeText(this, "Acest medic este deja înregistrat cu acest CNP!", Toast.LENGTH_LONG).show());
                        return;
                    }
                    if (dbFunctions.isOperatorEmailRegistered(institutionalEmail)) {
                        runOnUiThread(() -> Toast.makeText(this, "Acest Email instituțional este deja utilizat!", Toast.LENGTH_LONG).show());
                        return;
                    }

                    // Verificare ca email-ul instituțional să fie diferit de cel personal (dacă există deja cont de pacient)
                    String existingPersonalEmail = dbFunctions.getPatientEmail(cnp);
                    if (existingPersonalEmail != null && existingPersonalEmail.equalsIgnoreCase(institutionalEmail)) {
                        runOnUiThread(() -> Toast.makeText(this, "Email-ul instituțional trebuie să fie diferit de email-ul personal deja înregistrat!", Toast.LENGTH_LONG).show());
                        return;
                    }

                    // Dacă nu există în tabela pacienți, îl adăugăm (DOAR datele de bază, fără medical_history)
                    if (!dbFunctions.isCnpRegistered(cnp)) {
                        dbFunctions.registerPatientBasic(cnp, fName, lName, series, number, 
                                                   institutionalEmail, password, varsta);
                    }
                    
                    // Înregistrăm datele de medic
                    success = dbFunctions.registerOperator(cnp, fName, lName, series, number, 
                                                          institutionalEmail, password, null, specialty, codParafa);

                    final boolean finalSuccess = success;
                    runOnUiThread(() -> {
                        if (finalSuccess) {
                            Toast.makeText(this, "Cont Medic creat cu succes!", Toast.LENGTH_LONG).show();
                            finish();
                        } else {
                            Toast.makeText(this, "Eroare la crearea contului de medic!", Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        });
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();
        executorService.shutdown();
    }
}