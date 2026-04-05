package com.example.aplicatieproveit;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class ChangePasswordActivity extends AppCompatActivity {

    private EditText etEmailReset, etCodConfirmare;
    private Button btnChangePassword;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_change_password);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        etEmailReset = findViewById(R.id.etEmailReset);
        etCodConfirmare = findViewById(R.id.etCodConfirmare);
        btnChangePassword = findViewById(R.id.btnChangePassword);

        btnChangePassword.setOnClickListener(v -> {
            String email = etEmailReset.getText().toString().trim();
            String cod = etCodConfirmare.getText().toString().trim();

            if (email.isEmpty() || cod.isEmpty()) {
                Toast.makeText(ChangePasswordActivity.this, "Completează ambele câmpuri!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(ChangePasswordActivity.this, "Parola a fost schimbată cu succes!", Toast.LENGTH_LONG).show();
                // După ce a schimbat parola, îl trimitem înapoi la ecranul de Login
                Intent intent = new Intent(ChangePasswordActivity.this, LoginActivity.class);
                intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startActivity(intent);
                finish();
            }
        });
    }
}