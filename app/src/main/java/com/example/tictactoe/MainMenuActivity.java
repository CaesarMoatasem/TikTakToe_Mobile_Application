package com.example.tictactoe;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;
import androidx.core.view.GravityCompat;
import androidx.drawerlayout.widget.DrawerLayout;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;

public class MainMenuActivity extends AppCompatActivity {

    private DrawerLayout drawerLayout;
    private PreferencesManager prefs;
    private SwitchMaterial switchDark;
    private View btnSettings;
    private View cardMultiplayer;
    private View cardSingular;
    private View btnHistory;
    private View drawerBtnEnglish;
    private View drawerBtnFrench;
    private View drawerBtnGerman;
    private View drawerBtnTurkish;
    private View btnLogout;
    private TextView tvUsernameHeader;
    private TextView tvDrawerUsername;
    private TextView tvDrawerEmail;
    private TextView tvAvatarInitials;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (FirebaseAuth.getInstance().getCurrentUser() == null) {
            redirectToLogin();
            return;
        }

        setContentView(R.layout.activity_main_menu);
        initComponents();
        registerEventHandlers();
        loadData();
    }

    private void initComponents() {
        prefs        = new PreferencesManager(this);
        drawerLayout = findViewById(R.id.drawerLayout);
        switchDark   = findViewById(R.id.switchDarkMode);
        switchDark.setChecked(prefs.isDarkMode());

        btnSettings      = findViewById(R.id.btnSettings);
        cardMultiplayer  = findViewById(R.id.cardMultiplayer);
        cardSingular     = findViewById(R.id.cardSingular);
        btnHistory       = findViewById(R.id.btnHistory);
        drawerBtnEnglish = findViewById(R.id.drawerBtnEnglish);
        drawerBtnFrench  = findViewById(R.id.drawerBtnFrench);
        drawerBtnGerman  = findViewById(R.id.drawerBtnGerman);
        drawerBtnTurkish = findViewById(R.id.drawerBtnTurkish);
        btnLogout        = findViewById(R.id.btnLogout);

        tvUsernameHeader = findViewById(R.id.tvUsernameHeader);
        tvDrawerUsername = findViewById(R.id.tvDrawerUsername);
        tvDrawerEmail    = findViewById(R.id.tvDrawerEmail);
        tvAvatarInitials = findViewById(R.id.tvAvatarInitials);

        Log.d("INIT", "MainMenuActivity initComponents done");
    }

    private void registerEventHandlers() {
        btnSettings.setOnClickListener(v ->
                drawerLayout.openDrawer(GravityCompat.END));

        cardMultiplayer.setOnClickListener(v -> {
            startActivity(new Intent(this, AddPlayers.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        cardSingular.setOnClickListener(v -> showDifficultySheet());

        btnHistory.setOnClickListener(v -> {
            startActivity(new Intent(this, HistoryActivity.class));
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
        });

        switchDark.setOnCheckedChangeListener((btn, checked) -> {
            prefs.setDarkMode(checked);
            AppCompatDelegate.setDefaultNightMode(
                    checked ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);
        });

        drawerBtnEnglish.setOnClickListener(v -> changeLanguage("en"));
        drawerBtnFrench.setOnClickListener(v -> changeLanguage("fr"));
        drawerBtnGerman.setOnClickListener(v -> changeLanguage("de"));
        drawerBtnTurkish.setOnClickListener(v -> changeLanguage("tr"));

        btnLogout.setOnClickListener(v -> {
            FirebaseAuth.getInstance().signOut();
            prefs.setCachedUsername("");
            redirectToLogin();
        });

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (drawerLayout.isDrawerOpen(GravityCompat.END)) {
                    drawerLayout.closeDrawer(GravityCompat.END);
                } else {
                    setEnabled(false);
                    getOnBackPressedDispatcher().onBackPressed();
                }
            }
        });

        Log.d("INIT", "MainMenuActivity registerEventHandlers done");
    }

    private void loadData() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        refreshDrawerHeader(user);

        FirebaseFirestore.getInstance()
                .collection("users").document(user.getUid())
                .get()
                .addOnSuccessListener(doc -> {
                    String username = doc.getString("username");
                    if (username != null && !username.isEmpty()) {
                        prefs.setCachedUsername(username);
                        refreshDrawerHeader(FirebaseAuth.getInstance().getCurrentUser());
                    }
                });

        Log.d("INIT", "MainMenuActivity loadData done");
    }

    @Override
    protected void onResume() {
        super.onResume();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { redirectToLogin(); return; }
        refreshDrawerHeader(user);
    }

    private void refreshDrawerHeader(FirebaseUser user) {
        if (user == null) return;
        String username = prefs.getCachedUsername();
        if (username.isEmpty()) username = user.getEmail() != null ? user.getEmail() : "";
        String email = user.getEmail() != null ? user.getEmail() : "";

        tvUsernameHeader.setText(getString(R.string.hi_user, username));
        tvDrawerUsername.setText(username);
        tvDrawerEmail.setText(email);
        tvAvatarInitials.setText(username.isEmpty() ? "?" : String.valueOf(username.charAt(0)).toUpperCase());
    }

    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
    }

    private void showDifficultySheet() {
        BottomSheetDialog sheet = new BottomSheetDialog(this);
        View view = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_difficulty, null);
        sheet.setContentView(view);

        String username = prefs.getCachedUsername();

        view.findViewById(R.id.btnEasy).setOnClickListener(v -> {
            launchSinglePlayer(username, "Easy");
            sheet.dismiss();
        });
        view.findViewById(R.id.btnMedium).setOnClickListener(v -> {
            launchSinglePlayer(username, "Medium");
            sheet.dismiss();
        });
        view.findViewById(R.id.btnHard).setOnClickListener(v -> {
            launchSinglePlayer(username, "Hard");
            sheet.dismiss();
        });

        sheet.show();
    }

    private void launchSinglePlayer(String playerName, String difficulty) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("playerOne", playerName);
        intent.putExtra("playerTwo", "AI (" + difficulty + ")");
        intent.putExtra("isSinglePlayer", true);
        intent.putExtra("difficulty", difficulty);
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }

    private void changeLanguage(String languageCode) {
        prefs.setLanguage(languageCode);
        AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(languageCode));
    }
}
