package com.example.tictactoe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class LoginActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private PreferencesManager prefs;
    private TextInputEditText etEmail, etPassword;
    private MaterialButton btnLogin, btnRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            goToMainMenu();
            return;
        }

        setContentView(R.layout.activity_login);
        initComponents();
        registerEventHandlers();
        loadData();
    }

    private void initComponents() {
        mAuth = FirebaseAuth.getInstance();
        prefs = new PreferencesManager(this);
        etEmail = findViewById(R.id.etLoginEmail);
        etPassword = findViewById(R.id.etLoginPassword);
        btnLogin = findViewById(R.id.btnLogin);
        btnRegister = findViewById(R.id.tvGoToRegister);
        View loginCard = findViewById(R.id.loginCard);
        loginCard.startAnimation(AnimationUtils.loadAnimation(this, R.anim.slide_up_fade_in));

        Log.d("INIT", "LoginActivity initComponents done");
    }

    private void registerEventHandlers() {
        btnLogin.setOnClickListener(v -> attemptLogin());

        btnRegister.setOnClickListener(v -> {
            startActivity(new Intent(this, RegisterActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        Log.d("INIT", "LoginActivity registerEventHandlers done");
    }

    private void loadData() {
        Log.d("INIT", "LoginActivity loadData done");
    }

    private void attemptLogin() {
        String email = etEmail.getText() != null ? etEmail.getText().toString().trim() : "";
        String password = etPassword.getText() != null ? etPassword.getText().toString() : "";

        if (email.isEmpty() || password.isEmpty()) {
            Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
            return;
        }

        btnLogin.setEnabled(false);
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnSuccessListener(authResult -> {
                    FirebaseUser user = authResult.getUser();
                    if (user == null) { btnLogin.setEnabled(true); return; }
                    FirebaseFirestore.getInstance()
                            .collection("users").document(user.getUid())
                            .get()
                            .addOnSuccessListener(doc -> {
                                String username = doc.getString("username");
                                if (username != null) prefs.setCachedUsername(username);
                                goToMainMenu();
                            })
                            .addOnFailureListener(e -> goToMainMenu());
                })
                .addOnFailureListener(e -> {
                    btnLogin.setEnabled(true);
                    Toast.makeText(this, getString(R.string.invalid_credentials), Toast.LENGTH_SHORT).show();
                });
    }

    private void goToMainMenu() {
        Intent intent = new Intent(this, MainMenuActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }
}
