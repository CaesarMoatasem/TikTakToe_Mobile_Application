package com.example.tictactoe;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Random;

public class AddPlayers extends AppCompatActivity {

    private PreferencesManager prefs;
    private TextInputEditText etPlayerOne, etPlayerTwo;
    private ImageView imgP1Symbol, imgP2Symbol;
    private MaterialButton btnRandomSymbol;
    private boolean p1IsX = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_players);
        initComponents();
        registerEventHandlers();
        loadData();
    }

    @Override
    protected  void onDestroy() {
        super.onDestroy();
        p1IsX = true;
        btnRandomSymbol.setEnabled(true);
        btnRandomSymbol.setAlpha(1f);
    }


    private void initComponents() {
        prefs = new PreferencesManager(this);
        etPlayerOne = findViewById(R.id.playerOne);
        etPlayerTwo = findViewById(R.id.playerTwo);
        imgP1Symbol = findViewById(R.id.imgP1Symbol);
        imgP2Symbol = findViewById(R.id.imgP2Symbol);
        btnRandomSymbol = findViewById(R.id.btnRandomSymbol);
        Log.d("INIT", "AddPlayers initComponents done");
    }

    private void registerEventHandlers() {
        findViewById(R.id.backBtn).setOnClickListener(v -> finish());

        btnRandomSymbol.setOnClickListener(v -> {
            p1IsX = new Random().nextBoolean();
            updateSymbolPreviews();
            btnRandomSymbol.setEnabled(false);
            btnRandomSymbol.setAlpha(0.4f);
        });

        findViewById(R.id.startGameButton).setOnClickListener(v -> {
            String p1 = etPlayerOne.getText() != null
                    ? etPlayerOne.getText().toString().trim() : "";
            String p2 = etPlayerTwo.getText() != null
                    ? etPlayerTwo.getText().toString().trim() : "";

            if (p1.isEmpty() || p2.isEmpty()) {
                Toast.makeText(this, getString(R.string.fill_all_fields), Toast.LENGTH_SHORT).show();
                return;
            }

            goToMainActivity(p1, p2);
        });

        Log.d("INIT", "AddPlayers registerEventHandlers done");
    }

    private void loadData() {
        String username = prefs.getCachedUsername();
        if (!username.isEmpty()) {
            etPlayerOne.setText(username);
        }
        Log.d("INIT", "AddPlayers loadData done");
    }

    private void updateSymbolPreviews() {
        if (p1IsX) {
            imgP1Symbol.setImageResource(R.drawable.ximage);
            imgP1Symbol.setColorFilter(Color.parseColor("#FF3B30"));
            imgP2Symbol.setImageResource(R.drawable.oimage);
            imgP2Symbol.clearColorFilter();
        } else {
            imgP1Symbol.setImageResource(R.drawable.oimage);
            imgP1Symbol.clearColorFilter();
            imgP2Symbol.setImageResource(R.drawable.ximage);
            imgP2Symbol.setColorFilter(Color.parseColor("#FF3B30"));
        }
    }

    private void goToMainActivity(String p1, String p2) {
        Intent intent = new Intent(this, MainActivity.class);
        intent.putExtra("playerOne", p1);
        intent.putExtra("playerTwo", p2);
        intent.putExtra("isSinglePlayer", false);
        intent.putExtra("player1Symbol", p1IsX ? "X" : "O");
        startActivity(intent);
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left);
    }
}
