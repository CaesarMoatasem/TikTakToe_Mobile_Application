package com.example.tictactoe;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.media.MediaPlayer;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.example.tictactoe.databinding.ActivityMainBinding;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class MainActivity extends AppCompatActivity {

    ActivityMainBinding binding;
    private FirebaseFirestore db;
    private String currentUid;
    private String playerOneName, playerTwoName;
    private boolean isSinglePlayer = false;
    private String difficulty = "Easy";
    private PreferencesManager prefs;
    private boolean isDark = false;
    private final List<int[]> combinationList = new ArrayList<>();
    private int[] boxPositions = {0, 0, 0, 0, 0, 0, 0, 0, 0};
    private int playerTurn = 1;
    private int totalSelectedBoxes = 1;
    private int p1Score = 0;
    private int p2Score = 0;
    private boolean aiThinking = false;
    private boolean useLocalAI = false;
    private MediaPlayer mpWin, mpLose, mpDraw;
    private int player1ImageRes = R.drawable.ximage;
    private int player2ImageRes = R.drawable.oimage;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        initComponents();
        registerEventHandlers();
        loadData();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mpWin  != null) mpWin.release();
        if (mpLose != null) mpLose.release();
        if (mpDraw != null) mpDraw.release();
    }

    private void initComponents() {
        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();
        prefs = new PreferencesManager(this);

        combinationList.add(new int[]{0, 1, 2});
        combinationList.add(new int[]{3, 4, 5});
        combinationList.add(new int[]{6, 7, 8});
        combinationList.add(new int[]{0, 3, 6});
        combinationList.add(new int[]{1, 4, 7});
        combinationList.add(new int[]{2, 5, 8});
        combinationList.add(new int[]{2, 4, 6});
        combinationList.add(new int[]{0, 4, 8});

        mpWin  = MediaPlayer.create(this, R.raw.win_sound);
        mpLose = MediaPlayer.create(this, R.raw.lose_sound);
        mpDraw = MediaPlayer.create(this, R.raw.draw_sound);
        isDark = prefs.isDarkMode();

        playerOneName = getIntent().getStringExtra("playerOne");
        playerTwoName = getIntent().getStringExtra("playerTwo");
        isSinglePlayer = getIntent().getBooleanExtra("isSinglePlayer", false);
        String diffExtra = getIntent().getStringExtra("difficulty");
        if (diffExtra != null) difficulty = diffExtra;

        String p1Symbol = getIntent().getStringExtra("player1Symbol");
        if ("O".equals(p1Symbol)) {
            player1ImageRes = R.drawable.oimage;
            player2ImageRes = R.drawable.ximage;
        } else {
            player1ImageRes = R.drawable.ximage;
            player2ImageRes = R.drawable.oimage;
        }

        Log.d("INIT", "MainActivity initComponents done");
    }

    private void registerEventHandlers() {
        binding.mainMenuBtn.setOnClickListener(view -> finish());

        binding.image1.setOnClickListener(v -> tryMove((ImageView) v, 0));
        binding.image2.setOnClickListener(v -> tryMove((ImageView) v, 1));
        binding.image3.setOnClickListener(v -> tryMove((ImageView) v, 2));
        binding.image4.setOnClickListener(v -> tryMove((ImageView) v, 3));
        binding.image5.setOnClickListener(v -> tryMove((ImageView) v, 4));
        binding.image6.setOnClickListener(v -> tryMove((ImageView) v, 5));
        binding.image7.setOnClickListener(v -> tryMove((ImageView) v, 6));
        binding.image8.setOnClickListener(v -> tryMove((ImageView) v, 7));
        binding.image9.setOnClickListener(v -> tryMove((ImageView) v, 8));

        Log.d("INIT", "MainActivity registerEventHandlers done");
    }

    private void loadData() {
        binding.playerOneName.setText(playerOneName != null ? playerOneName : "");
        binding.playerTwoName.setText(playerTwoName != null ? playerTwoName : "");

        binding.imgP1Symbol.setImageResource(player1ImageRes);
        if (player1ImageRes == R.drawable.ximage) {
            binding.imgP1Symbol.setColorFilter(Color.parseColor("#FF3B30"));
        } else {
            binding.imgP1Symbol.clearColorFilter();
        }

        binding.imgP2Symbol.setImageResource(player2ImageRes);
        if (player2ImageRes == R.drawable.ximage) {
            binding.imgP2Symbol.setColorFilter(Color.parseColor("#FF3B30"));
        } else {
            binding.imgP2Symbol.clearColorFilter();
        }

        String p1 = playerOneName != null ? playerOneName : getString(R.string.player_one);
        binding.tvTurnIndicator.setText(p1 + "'s Turn");
        Log.d("INIT", "MainActivity loadData done");
    }

    private void tryMove(ImageView view, int pos) {
        if (isBoxSelectable(pos) && !aiThinking && (playerTurn == 1 || !isSinglePlayer)) {
            performAction(view, pos);

        }
    }

    private void performAction(ImageView imageView, int selectedBoxPosition) {
        boxPositions[selectedBoxPosition] = playerTurn;

        if (playerTurn == 1) {
            imageView.setImageResource(player1ImageRes);
            if (player1ImageRes == R.drawable.ximage) {
                imageView.setColorFilter(Color.parseColor("#FF3B30"));
            } else {
                imageView.clearColorFilter();
            }
            if (checkResults()) {
                p1Score++;
                binding.playerOneScore.setText(getString(R.string.score_text, p1Score));
                saveGameResult(playerOneName);
                if (isSinglePlayer) {
                    playVictorySound();
                    showResult("🏆 " + getString(R.string.winner_msg, playerOneName));
                } else {
                    showResult(getString(R.string.winner_msg, playerOneName));
                }
            } else if (totalSelectedBoxes == 9) {
                saveGameResult("Draw");
                if (isSinglePlayer) playDrawSound();
                showResult(getString(R.string.draw_msg));
            } else {
                changePlayerTurn(2);
                totalSelectedBoxes++;
                if (isSinglePlayer) {
                    new android.os.Handler(android.os.Looper.getMainLooper())
                            .postDelayed(this::performAiMove, 500);
                }
            }
        } else {
            imageView.setImageResource(player2ImageRes);
            if (player2ImageRes == R.drawable.ximage) {
                imageView.setColorFilter(Color.parseColor("#FF3B30"));
            } else {
                imageView.clearColorFilter();
            }
            if (checkResults()) {
                p2Score++;
                binding.playerTwoScore.setText(getString(R.string.score_text, p2Score));
                saveGameResult(playerTwoName);
                if (isSinglePlayer) {
                    playDefeatSound();
                    flashBoardRed();
                    vibrateDevice();
                }
                showResult(getString(R.string.winner_msg, playerTwoName));
            } else if (totalSelectedBoxes == 9) {
                saveGameResult("Draw");
                if (isSinglePlayer) playDrawSound();
                showResult(getString(R.string.draw_msg));
            } else {
                changePlayerTurn(1);
                totalSelectedBoxes++;
            }
        }
    }

    // ── Victory: ascending three-tone chime ──────────────────────────────────

    private void playVictorySound() {
        if (mpWin != null) {
            mpWin.seekTo(0); // rewind to start in case it was played before
            mpWin.start();
        }
    }

    private void playDefeatSound() {
        if (mpLose != null) {
            mpLose.seekTo(0);
            mpLose.start();
        }
    }

    private void playDrawSound() {
        if (mpDraw != null) {
            mpDraw.seekTo(0);
            mpDraw.start();
        }
    }
    // ── Haptic: 500ms vibration on defeat ────────────────────────────────────

    @SuppressWarnings("deprecation")
    private void vibrateDevice() {
        Vibrator vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator == null || !vibrator.hasVibrator()) return;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(500, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            vibrator.vibrate(500);
        }
    }

    // ── Visual: red flash on activity background ─────────────────────────────

    private void flashBoardRed() {
        int bgColor = getResources().getColor(R.color.primary_bg, null);
        ObjectAnimator animator = ObjectAnimator.ofArgb(
                binding.getRoot(),
                "backgroundColor",
                bgColor,
                0xFFCC2222,
                bgColor
        );
        animator.setDuration(700);
        animator.start();
    }

    // ────────────────────────────────────────────────────────────────────────

    private void saveGameResult(String winner) {
        if (currentUid == null) return;
        Map<String, Object> game = new HashMap<>();
        game.put("player1", playerOneName);
        game.put("player2", playerTwoName);
        game.put("winner", winner);
        game.put("isSinglePlayer", isSinglePlayer);
        game.put("timestamp", FieldValue.serverTimestamp());
        db.collection("users").document(currentUid).collection("games").add(game);
    }

    private void performAiMove() {
        if (totalSelectedBoxes > 9 || checkResults()) return;
        if (useLocalAI) {
            makeAiMove(getLocalAIMove());
            return;
        }
        setAiThinking(true);
        GeminiHelper.getBestMove(boxPositions, difficulty, new GeminiHelper.GeminiCallback() {
            @Override
            public void onResult(int position) {
                setAiThinking(false);
                makeAiMove(position);
            }

            @Override
            public void onError() {
                aiThinking = false;
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle(getString(R.string.connection_error_title))
                        .setMessage(getString(R.string.connection_error_msg))
                        .setPositiveButton(getString(R.string.yes), (dialog, which) -> switchToLocalAI())
                        .setNegativeButton(getString(R.string.no), (dialog, which) -> finish())
                        .setCancelable(false)
                        .show();
            }
        });
    }

    private void switchToLocalAI() {
        useLocalAI = true;
        makeAiMove(getLocalAIMove());
    }

    private int getLocalAIMove() {
        return getRandomFallbackMove();
    }



    private int getRandomFallbackMove() {
        List<Integer> available = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            if (boxPositions[i] == 0) available.add(i);
        }
        if (available.isEmpty()) return -1;
        return available.get(new Random().nextInt(available.size()));
    }

    private void setAiThinking(boolean thinking) {
        aiThinking = thinking;
        if (isSinglePlayer) setTurnIndicator(null, thinking);
    }

    private void setTurnIndicator(String name, boolean isAiTurn) {
        String label = isAiTurn
                ? getString(R.string.ai_thinking)
                : (name != null ? name : getString(R.string.player_one)) + "'s Turn";
        int bgRes = !isAiTurn ? R.drawable.turn_indicator_human : R.drawable.turn_indicator_ai;
        int textColor = isAiTurn ? 0xFFFFFFFF : 0xFF000000;

        binding.tvTurnIndicator.animate()
                .alpha(0f)
                .setDuration(120)
                .withEndAction(() -> {
                    binding.tvTurnIndicator.setText(label);
                    binding.tvTurnIndicator.setBackgroundResource(bgRes);
                    binding.tvTurnIndicator.setTextColor(textColor);
                    binding.tvTurnIndicator.animate().alpha(1f).setDuration(120).start();
                })
                .start();
    }

    private void makeAiMove(int pos) {
        if (pos == -1) return;
        ImageView iv = getImageViewByPosition(pos);
        if (iv != null) performAction(iv, pos);
    }

    private void showResult(String message) {
        ResultDialog dialog = new ResultDialog(this, message, this);
        dialog.setCancelable(false);
        dialog.show();
    }

    private void changePlayerTurn(int turn) {
        playerTurn = turn;
        if (playerTurn == 1) {
            binding.playerOnelayout.setBackgroundResource(R.drawable.black_border);
            binding.playerTwolayout.setBackgroundResource(R.drawable.white_box);
            setTurnIndicator(playerOneName, false);
        } else {
            binding.playerTwolayout.setBackgroundResource(R.drawable.black_border);
            binding.playerOnelayout.setBackgroundResource(R.drawable.white_box);
            if (!isSinglePlayer) setTurnIndicator(playerTwoName, false);
        }
    }

    private ImageView getImageViewByPosition(int pos) {
        switch (pos) {
            case 0: return binding.image1;
            case 1: return binding.image2;
            case 2: return binding.image3;
            case 3: return binding.image4;
            case 4: return binding.image5;
            case 5: return binding.image6;
            case 6: return binding.image7;
            case 7: return binding.image8;
            case 8: return binding.image9;
            default: return null;
        }
    }

    private boolean checkResults() {
        for (int[] c : combinationList) {
            if (boxPositions[c[0]] == playerTurn
                    && boxPositions[c[1]] == playerTurn
                    && boxPositions[c[2]] == playerTurn) {
                return true;
            }
        }
        return false;
    }

    private boolean isBoxSelectable(int pos) {
        return boxPositions[pos] == 0;
    }

    public void restartMatch() {
        boxPositions = new int[]{0, 0, 0, 0, 0, 0, 0, 0, 0};
        playerTurn = 1;
        totalSelectedBoxes = 1;
        aiThinking = false;

        for (int i = 0; i < 9; i++) {
            ImageView iv = getImageViewByPosition(i);
            if (iv != null) { iv.setImageDrawable(null); iv.clearColorFilter(); }
        }

        binding.playerOnelayout.setBackgroundResource(R.drawable.black_border);
        binding.playerTwolayout.setBackgroundResource(R.drawable.white_box);
        setTurnIndicator(playerOneName, false);
    }
}
