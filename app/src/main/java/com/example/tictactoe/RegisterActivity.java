package com.example.tictactoe;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.HashMap;
import java.util.Map;

public class RegisterActivity extends AppCompatActivity {

    private TextInputLayout tilEmail, tilPassword, tilConfirmPassword;
    private TextInputEditText etUsername, etEmail, etPassword, etConfirmPassword;
    private MaterialButton btnRegister, btnLogin;
    private FirebaseAuth mAuth;
    private PreferencesManager prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);
        initComponents();
        registerEventHandlers();
        loadData();
    }

    private void initComponents() {
        mAuth = FirebaseAuth.getInstance();
        prefs = new PreferencesManager(this);

        etUsername        = findViewById(R.id.etRegUsername);
        etEmail           = findViewById(R.id.etRegEmail);
        etPassword        = findViewById(R.id.etRegPassword);
        etConfirmPassword = findViewById(R.id.etRegConfirmPassword);
        tilEmail          = findViewById(R.id.tilEmail);
        tilPassword       = findViewById(R.id.tilPassword);
        tilConfirmPassword = findViewById(R.id.tilConfirmPassword);
        btnRegister       = findViewById(R.id.btnRegister);
        btnLogin          = findViewById(R.id.tvGoToLogin);

        btnRegister.setEnabled(false);

        View registerCard = findViewById(R.id.registerCard);
        registerCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in));

        Log.d("INIT", "RegisterActivity initComponents done");
    }

    private void registerEventHandlers() {
        etUsername.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String username = s.toString().trim();
                if (username.length() < 3) {
                    etUsername.setError(getString(R.string.username_too_short));
                } else {
                    etUsername.setError(null);
                }
                updateButtonState();
            }
        });

        etEmail.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String email = s.toString().trim();
                if (!email.isEmpty() && (!email.contains("@") || !email.contains("."))) {
                    tilEmail.setError(getString(R.string.invalid_email));
                } else {
                    tilEmail.setError(null);
                }
                updateButtonState();
            }
        });

        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pwd = s.toString();
                if (!pwd.isEmpty() && pwd.length() < 8) {
                    tilPassword.setError(getString(R.string.password_too_short));
                } else {
                    tilPassword.setError(null);
                }
                revalidateConfirm(pwd);
                updateButtonState();
            }
        });

        etConfirmPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String pwd = etPassword.getText() != null ? etPassword.getText().toString() : "";
                revalidateConfirm(pwd);
                updateButtonState();
            }
        });

        btnRegister.setOnClickListener(v -> attemptRegistration());

        btnLogin.setOnClickListener(v -> {
            finish();
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right);
        });

        Log.d("INIT", "RegisterActivity registerEventHandlers done");
    }

    private void loadData() {
        Log.d("INIT", "RegisterActivity loadData done");
    }

    private void attemptRegistration() {
        String username = etUsername.getText() != null
                ? etUsername.getText().toString().trim() : "";

        if (username.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        String email    = etEmail.getText().toString().trim();
        String password = etPassword.getText().toString();

        btnRegister.setEnabled(false);
        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    String uid = authResult.getUser().getUid();
                    Map<String, Object> userDoc = new HashMap<>();
                    userDoc.put("username", username);
                    userDoc.put("email", email);
                    prefs.setCachedUsername(username);
                    FirebaseFirestore.getInstance()
                            .collection("users").document(uid)
                            .set(userDoc)
                            .addOnCompleteListener(task -> {
                                Toast.makeText(this, getString(R.string.registration_success),
                                        Toast.LENGTH_SHORT).show();
                                goToMainMenu();
                            });
                })
                .addOnFailureListener(e -> {
                    btnRegister.setEnabled(true);
                    String msg = e.getMessage() != null ? e.getMessage() : getString(R.string.error_auth);
                    Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                });
    }

    private void revalidateConfirm(String currentPassword) {
        String confirm = etConfirmPassword.getText() != null
                ? etConfirmPassword.getText().toString() : "";
        if (!confirm.isEmpty() && !confirm.equals(currentPassword)) {
            tilConfirmPassword.setError(getString(R.string.passwords_dont_match));
        } else {
            tilConfirmPassword.setError(null);
        }
    }

    private void updateButtonState() {
        String username = etUsername.getText() != null
                ? etUsername.getText().toString().trim() : "";
        String email   = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String pwd     = etPassword.getText() != null ? etPassword.getText().toString() : "";
        String confirm = etConfirmPassword.getText() != null
                ? etConfirmPassword.getText().toString() : "";

        boolean usernameValid = !username.isEmpty() && username.length() >= 3;
        boolean emailValid   = !email.isEmpty() && email.contains("@") && email.contains(".");
        boolean pwdValid     = pwd.length() >= 8;
        boolean confirmValid = !confirm.isEmpty() && confirm.equals(pwd);

        btnRegister.setEnabled(usernameValid && emailValid && pwdValid && confirmValid);
    }

    private void goToMainMenu() {
        Intent intent = new Intent(this, MainMenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
