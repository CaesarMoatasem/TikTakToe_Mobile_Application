package com.example.tictactoe;

import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.AttrRes;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HistoryActivity extends AppCompatActivity {

    private static final int STAT_CARD_BG  = 0xFF3A3D7A;
    private static final int MATCH_CARD_BG = 0xFF272958;
    private static final int AVATAR_BG     = 0xFF5C6BC0;

    private FirebaseFirestore db;
    private String currentUid;

    private LinearLayout historyData;
    private ProgressBar progressBar;
    private LinearLayout containerMultiStats;
    private LinearLayout containerMultiMatches;
    private LinearLayout containerSingleStats;
    private LinearLayout containerSingleMatches;
    private View labelMultiRecentMatches;
    private View labelSingleRecentMatches;
    private LinearLayout chartContainerMulti;
    private LinearLayout chartContainerSingle;
    private MaterialButton btnClearMulti;
    private MaterialButton btnClearSingle;
    private View btnHistoryBack;

    static class PlayerStats {
        String playerName;
        int played, won, lost, draw;
        float winPercent;
    }

    static class GameData {
        String player1, player2, winner;
        boolean isSinglePlayer;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) { finish(); return; }

        setContentView(R.layout.activity_history);

        initComponents();
        registerEventHandlers();
        loadData();
    }

    private void initComponents() {
        db = FirebaseFirestore.getInstance();
        currentUid = FirebaseAuth.getInstance().getCurrentUser().getUid();

        historyData            = findViewById(R.id.historyData);
        progressBar            = findViewById(R.id.progressBar);
        containerMultiStats    = findViewById(R.id.containerMultiStats);
        containerMultiMatches  = findViewById(R.id.containerMultiMatches);
        containerSingleStats   = findViewById(R.id.containerSingleStats);
        containerSingleMatches = findViewById(R.id.containerSingleMatches);
        labelMultiRecentMatches  = findViewById(R.id.labelMultiRecentMatches);
        labelSingleRecentMatches = findViewById(R.id.labelSingleRecentMatches);
        chartContainerMulti  = findViewById(R.id.chartContainerMulti);
        chartContainerSingle = findViewById(R.id.chartContainerSingle);
        btnClearMulti        = findViewById(R.id.btnClearMulti);
        btnClearSingle       = findViewById(R.id.btnClearSingle);
        btnHistoryBack       = findViewById(R.id.btnHistoryBack);

        Log.d("INIT", "HistoryActivity initComponents done");
    }

    private void registerEventHandlers() {
        btnHistoryBack.setOnClickListener(v -> finish());

        btnClearMulti.setOnClickListener(v -> clearHistory(false));
        btnClearSingle.setOnClickListener(v -> clearHistory(true));

        Log.d("INIT", "HistoryActivity registerEventHandlers done");
    }

    private void loadData() {
        loadHistory();
        Log.d("INIT", "HistoryActivity loadData done");
    }

    private void loadHistory() {
        progressBar.setVisibility(View.VISIBLE);
        db.collection("users").document(currentUid).collection("games")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(snapshot -> {
                    List<GameData> allGames = new ArrayList<>();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        GameData g = new GameData();
                        g.player1 = doc.getString("player1");
                        g.player2 = doc.getString("player2");
                        g.winner  = doc.getString("winner");
                        Boolean sp = doc.getBoolean("isSinglePlayer");
                        g.isSinglePlayer = Boolean.TRUE.equals(sp);
                        allGames.add(g);
                    }
                    refreshUI(allGames);
                    progressBar.setVisibility(View.GONE);
                    historyData.setVisibility(View.VISIBLE);
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(this, getString(R.string.error_load_history), Toast.LENGTH_SHORT).show();
                });
    }

    private void clearHistory(boolean isSinglePlayer) {
        db.collection("users").document(currentUid).collection("games")
                .whereEqualTo("isSinglePlayer", isSinglePlayer)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (snapshot.isEmpty()) return;
                    WriteBatch batch = db.batch();
                    for (DocumentSnapshot doc : snapshot.getDocuments()) {
                        batch.delete(doc.getReference());
                    }
                    batch.commit().addOnSuccessListener(unused -> loadHistory());
                });
    }

    private void refreshUI(List<GameData> allGames) {
        List<GameData> multiGames  = new ArrayList<>();
        List<GameData> singleGames = new ArrayList<>();
        for (GameData g : allGames) {
            if (g.isSinglePlayer) singleGames.add(g);
            else multiGames.add(g);
        }
        refreshSection(multiGames,  false);
        refreshSection(singleGames, true);
    }

    private void refreshSection(List<GameData> games, boolean isSinglePlayer) {
        LinearLayout statsContainer   = isSinglePlayer ? containerSingleStats   : containerMultiStats;
        LinearLayout matchesContainer = isSinglePlayer ? containerSingleMatches : containerMultiMatches;
        LinearLayout chartContainer   = isSinglePlayer ? chartContainerSingle   : chartContainerMulti;
        View matchesLabel             = isSinglePlayer ? labelSingleRecentMatches : labelMultiRecentMatches;

        statsContainer.removeAllViews();
        matchesContainer.removeAllViews();

        List<PlayerStats> statsList = computeStats(games, isSinglePlayer);

        setupPieChart(chartContainer, statsList);

        if (statsList.isEmpty()) {
            statsContainer.addView(createEmptyView(getString(R.string.no_data_yet)));
        } else {
            for (PlayerStats stats : statsList) {
                statsContainer.addView(createStatsCard(stats));
            }
        }

        if (games.isEmpty()) {
            matchesLabel.setVisibility(View.GONE);
        } else {
            matchesLabel.setVisibility(View.VISIBLE);
            int limit = Math.min(games.size(), 10);
            for (int i = 0; i < limit; i++) {
                matchesContainer.addView(createMatchRow(games.get(i)));
            }
        }
    }

    private List<PlayerStats> computeStats(List<GameData> games, boolean isSinglePlayer) {
        Map<String, PlayerStats> map = new HashMap<>();

        for (GameData g : games) {
            String[] players = isSinglePlayer
                    ? new String[]{g.player1}
                    : new String[]{g.player1, g.player2};

            for (String name : players) {
                if (name == null || name.isEmpty()) continue;
                PlayerStats s = map.get(name);
                if (s == null) {
                    s = new PlayerStats();
                    s.playerName = name;
                    map.put(name, s);
                }
                s.played++;
                boolean isDraw = "Draw".equals(g.winner);
                if (isDraw) {
                    s.draw++;
                } else if (name.equals(g.winner)) {
                    s.won++;
                } else {
                    s.lost++;
                }
            }
        }

        List<PlayerStats> result = new ArrayList<>(map.values());
        for (PlayerStats s : result) {
            s.winPercent = s.played > 0 ? (s.won * 100f / s.played) : 0f;
        }
        result.sort((a, b) -> Float.compare(b.winPercent, a.winPercent));
        return result;
    }

    private MaterialCardView createStatsCard(PlayerStats stats) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardLp);
        card.setRadius(dp(16));
        card.setCardElevation(dp(6));
        card.setCardBackgroundColor(STAT_CARD_BG);

        LinearLayout content = new LinearLayout(this);
        content.setOrientation(LinearLayout.VERTICAL);
        content.setPadding(dp(16), dp(16), dp(16), dp(16));

        LinearLayout headerRow = new LinearLayout(this);
        headerRow.setOrientation(LinearLayout.HORIZONTAL);
        headerRow.setGravity(Gravity.CENTER_VERTICAL);

        String initial = stats.playerName.isEmpty()
                ? "?" : String.valueOf(stats.playerName.charAt(0)).toUpperCase();
        TextView avatar = new TextView(this);
        avatar.setText(initial);
        avatar.setTextSize(17f);
        avatar.setTextColor(Color.WHITE);
        avatar.setTypeface(null, Typeface.BOLD);
        avatar.setGravity(Gravity.CENTER);
        int avatarSize = dp(42);
        LinearLayout.LayoutParams avatarLp = new LinearLayout.LayoutParams(avatarSize, avatarSize);
        avatarLp.setMargins(0, 0, dp(14), 0);
        avatar.setLayoutParams(avatarLp);
        GradientDrawable avatarBg = new GradientDrawable();
        avatarBg.setShape(GradientDrawable.OVAL);
        avatarBg.setColor(AVATAR_BG);
        avatar.setBackground(avatarBg);

        TextView nameView = new TextView(this);
        nameView.setText(stats.playerName);
        nameView.setTextSize(16f);
        nameView.setTextColor(Color.WHITE);
        nameView.setTypeface(null, Typeface.BOLD);
        nameView.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));

        headerRow.addView(avatar);
        headerRow.addView(nameView);
        content.addView(headerRow);

        View divider = new View(this);
        divider.setBackgroundColor(0x33FFFFFF);
        LinearLayout.LayoutParams divLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(1));
        divLp.setMargins(0, dp(14), 0, dp(14));
        divider.setLayoutParams(divLp);
        content.addView(divider);

        LinearLayout statsRow = new LinearLayout(this);
        statsRow.setOrientation(LinearLayout.HORIZONTAL);
        statsRow.setWeightSum(5);

        int winColor;
        if (stats.winPercent >= 60f)      winColor = 0xFF4CAF50;
        else if (stats.winPercent >= 35f) winColor = 0xFFFFD700;
        else                              winColor = 0xFFFF7043;

        statsRow.addView(createStatCell(getString(R.string.stat_played), String.valueOf(stats.played), Color.WHITE));
        statsRow.addView(createStatCell(getString(R.string.stat_won),    String.valueOf(stats.won),    0xFF4CAF50));
        statsRow.addView(createStatCell(getString(R.string.stat_lost),   String.valueOf(stats.lost),   0xFFFF7043));
        statsRow.addView(createStatCell(getString(R.string.stat_draw),   String.valueOf(stats.draw),   0xFFFFD700));
        statsRow.addView(createStatCell(getString(R.string.stat_win_rate),
                String.format("%.0f%%", stats.winPercent), winColor));

        content.addView(statsRow);
        card.addView(content);
        return card;
    }

    private LinearLayout createStatCell(String label, String value, int valueColor) {
        LinearLayout cell = new LinearLayout(this);
        cell.setLayoutParams(new LinearLayout.LayoutParams(
                0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f));
        cell.setOrientation(LinearLayout.VERTICAL);
        cell.setGravity(Gravity.CENTER);

        TextView valView = new TextView(this);
        valView.setText(value);
        valView.setTextSize(20f);
        valView.setTextColor(valueColor);
        valView.setTypeface(null, Typeface.BOLD);
        valView.setGravity(Gravity.CENTER);

        TextView lblView = new TextView(this);
        lblView.setText(label);
        lblView.setTextSize(9f);
        lblView.setTextColor(0xAAFFFFFF);
        lblView.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lblLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lblLp.setMargins(0, dp(4), 0, 0);
        lblView.setLayoutParams(lblLp);

        cell.addView(valView);
        cell.addView(lblView);
        return cell;
    }

    private MaterialCardView createMatchRow(GameData g) {
        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(8));
        card.setLayoutParams(cardLp);
        card.setRadius(dp(12));
        card.setCardElevation(dp(3));
        card.setCardBackgroundColor(MATCH_CARD_BG);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);
        row.setPadding(dp(16), dp(14), dp(16), dp(14));

        TextView p1 = makeMatchText(g.player1 != null ? g.player1 : "", 2.5f, Color.WHITE, Gravity.START);
        p1.setTypeface(null, Typeface.BOLD);

        TextView vsLabel = makeMatchText(getString(R.string.vs), 0f, 0x66FFFFFF, Gravity.CENTER);
        LinearLayout.LayoutParams vsLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        vsLp.setMargins(dp(8), 0, dp(8), 0);
        vsLabel.setLayoutParams(vsLp);
        vsLabel.setTextSize(11f);

        TextView p2 = makeMatchText(g.player2 != null ? g.player2 : "", 2.5f, Color.WHITE, Gravity.END);
        p2.setTypeface(null, Typeface.BOLD);

        boolean isDraw = "Draw".equals(g.winner);
        String winText = isDraw ? getString(R.string.draw_result) : "🏆 " + g.winner;
        int winColor   = isDraw ? 0xFFFFD700 : 0xFF4CAF50;

        TextView dot = makeMatchText("·", 0f, 0x44FFFFFF, Gravity.CENTER);
        LinearLayout.LayoutParams dotLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        dotLp.setMargins(dp(6), 0, dp(6), 0);
        dot.setLayoutParams(dotLp);
        dot.setTextSize(18f);

        TextView winView = makeMatchText(winText, 3f, winColor, Gravity.END);
        winView.setTypeface(null, Typeface.BOLD);
        winView.setTextSize(12f);

        row.addView(p1);
        row.addView(vsLabel);
        row.addView(p2);
        row.addView(dot);
        row.addView(winView);
        card.addView(row);
        return card;
    }

    private TextView makeMatchText(String text, float weight, int color, int gravity) {
        TextView tv = new TextView(this);
        tv.setText(text);
        tv.setTextSize(13f);
        tv.setTextColor(color);
        tv.setGravity(gravity);
        if (weight > 0) {
            tv.setLayoutParams(new LinearLayout.LayoutParams(
                    0, LinearLayout.LayoutParams.WRAP_CONTENT, weight));
        }
        return tv;
    }

    private TextView createEmptyView(String message) {
        TextView tv = new TextView(this);
        tv.setText(message);
        tv.setTextSize(14f);
        tv.setTextColor(resolveAttrColor(android.R.attr.textColorSecondary));
        tv.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, dp(8), 0, dp(20));
        tv.setLayoutParams(lp);
        return tv;
    }

    private void setupPieChart(LinearLayout container, List<PlayerStats> statsList) {
        container.removeAllViews();

        int totalWon = 0, totalLost = 0, totalDraw = 0;
        for (PlayerStats s : statsList) {
            totalWon  += s.won;
            totalLost += s.lost;
            totalDraw += s.draw;
        }
        int totalGames = totalWon + totalLost + totalDraw;

        if (totalGames == 0) {
            container.setVisibility(View.GONE);
            return;
        }
        container.setVisibility(View.VISIBLE);

        boolean isDark      = isDarkMode();
        int textColor       = isDark ? Color.WHITE                    : Color.parseColor("#333333");
        int cardBgColor     = isDark ? STAT_CARD_BG                   : Color.parseColor("#F3F0FF");
        int holeBgColor     = isDark ? Color.parseColor("#272958")     : Color.parseColor("#EBE8FF");

        MaterialCardView card = new MaterialCardView(this);
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        cardLp.setMargins(0, 0, 0, dp(12));
        card.setLayoutParams(cardLp);
        card.setRadius(dp(16));
        card.setCardElevation(dp(4));
        card.setCardBackgroundColor(cardBgColor);

        PieChart pieChart = new PieChart(this);
        LinearLayout.LayoutParams chartLp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(220));
        pieChart.setLayoutParams(chartLp);
        pieChart.setPadding(dp(8), dp(16), dp(8), dp(8));

        List<PieEntry> entries = new ArrayList<>();
        List<Integer>  colors  = new ArrayList<>();
        if (totalWon  > 0) { entries.add(new PieEntry(totalWon,  "Win"));  colors.add(0xFF4CAF50); }
        if (totalLost > 0) { entries.add(new PieEntry(totalLost, "Loss")); colors.add(0xFFFF5252); }
        if (totalDraw > 0) { entries.add(new PieEntry(totalDraw, "Draw")); colors.add(0xFFFFD700); }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(textColor);
        dataSet.setSliceSpace(3f);
        dataSet.setSelectionShift(5f);

        PieData pieData = new PieData(dataSet);
        pieData.setValueFormatter(new PercentFormatter(pieChart));

        pieChart.setData(pieData);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setDrawEntryLabels(false);
        pieChart.setDrawHoleEnabled(true);
        pieChart.setHoleRadius(45f);
        pieChart.setTransparentCircleRadius(50f);
        pieChart.setTransparentCircleAlpha(60);
        pieChart.setCenterText(totalGames + " Games");
        pieChart.setCenterTextSize(14f);
        pieChart.setCenterTextColor(textColor);
        pieChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD);
        pieChart.setHoleColor(holeBgColor);
        pieChart.setBackgroundColor(Color.TRANSPARENT);
        pieChart.setRotationEnabled(true);
        pieChart.setHighlightPerTapEnabled(true);

        Legend legend = pieChart.getLegend();
        legend.setTextColor(textColor);
        legend.setTextSize(12f);
        legend.setForm(Legend.LegendForm.CIRCLE);
        legend.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        legend.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        legend.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        legend.setDrawInside(false);

        pieChart.animateY(1000);

        card.addView(pieChart);
        container.addView(card);
    }

    private boolean isDarkMode() {
        int flags = getResources().getConfiguration().uiMode
                & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return flags == android.content.res.Configuration.UI_MODE_NIGHT_YES;
    }

    private int resolveAttrColor(@AttrRes int attr) {
        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(attr, tv, true);
        return tv.data;
    }

    private int dp(int value) {
        return Math.round(TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, value,
                getResources().getDisplayMetrics()));
    }
}
