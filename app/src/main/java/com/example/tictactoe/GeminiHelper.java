package com.example.tictactoe;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class GeminiHelper {

    private static final String TAG = "GeminiAI";
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1/models/gemini-2.5-flash:generateContent?key=";

    public interface GeminiCallback {
        void onResult(int position);
        void onError();
    }

    public static void getBestMove(int[] boardPositions, String difficulty, GeminiCallback callback) {
        Handler mainHandler = new Handler(Looper.getMainLooper());

        new Thread(() -> {
            try {
                StringBuilder visualBoard = new StringBuilder();
                List<Integer> emptyList = new ArrayList<>();

                for (int i = 0; i < 9; i++) {
                    String cell;
                    if (boardPositions[i] == 1) cell = "X";
                    else if (boardPositions[i] == 2) cell = "O";
                    else {
                        cell = String.valueOf(i);
                        emptyList.add(i);
                    }
                    visualBoard.append(" ").append(cell).append(" ");
                    if ((i + 1) % 3 != 0) visualBoard.append("|");
                    if ((i + 1) % 3 == 0 && i < 8) visualBoard.append("\n-----------\n");
                }

                String prompt = buildPrompt(visualBoard.toString(), difficulty, emptyList.toString());

                JSONObject body = new JSONObject();
                JSONArray contents = new JSONArray();
                JSONObject content = new JSONObject();
                JSONArray parts = new JSONArray();
                JSONObject part = new JSONObject();
                part.put("text", prompt);
                parts.put(part);
                content.put("parts", parts);
                contents.put(content);
                body.put("contents", contents);

                URL url = new URL(API_URL + com.example.tictactoe.BuildConfig.GEMINI_API_KEY);
                Log.d(TAG, "URL: " + url.toString());

                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setRequestProperty("Content-Type", "application/json");
                conn.setDoOutput(true);
                conn.setConnectTimeout(8000);

                try (OutputStream os = conn.getOutputStream()) {
                    os.write(body.toString().getBytes(StandardCharsets.UTF_8));
                }

                if (conn.getResponseCode() == HttpURLConnection.HTTP_OK) {
                    BufferedReader br = new BufferedReader(
                            new InputStreamReader(conn.getInputStream()));
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = br.readLine()) != null) sb.append(line);

                    JSONObject response = new JSONObject(sb.toString());
                    String aiResponse = response.getJSONArray("candidates")
                            .getJSONObject(0)
                            .getJSONObject("content")
                            .getJSONArray("parts")
                            .getJSONObject(0)
                            .getString("text").trim();

                    Log.d(TAG, "AI Response: " + aiResponse);

                    int chosenPos = -1;
                    String digits = aiResponse.replaceAll("[^0-8]", "");
                    for (char c : digits.toCharArray()) {
                        int val = Character.getNumericValue(c);
                        if (emptyList.contains(val)) {
                            chosenPos = val;
                            break;
                        }
                    }

                    if (chosenPos != -1) {
                        int finalResult = chosenPos;
                        mainHandler.post(() -> callback.onResult(finalResult));
                    } else {
                        Log.e(TAG, "No valid move in AI response");
                        mainHandler.post(callback::onError);
                    }
                } else {
                    Log.e(TAG, "Server Error Code: " + conn.getResponseCode());
                    mainHandler.post(callback::onError);
                }
                conn.disconnect();

            } catch (Exception e) {
                Log.e(TAG, "Exception: " + e.getMessage());
                mainHandler.post(callback::onError);
            }
        }).start();
    }

    private static String buildPrompt(String board, String difficulty, String emptyPositions) {
        String base = "You are a Tic-Tac-Toe Pro. You are player O. Your opponent is X.\n" +
                "Current Board:\n" + board + "\n" +
                "Available indices: " + emptyPositions + "\n\n";

        switch (difficulty) {
            case "Easy":
                return base + "Instruction: Play casually. Do not try to win or block. " +
                        "Pick any available index randomly.\n" +
                        "Response: Return ONLY the digit.";

            case "Medium":
                return base + "Instruction: Play logically.\n" +
                        "1. If you (O) can win now, take it.\n" +
                        "2. If X has two in a row, block it 70% of the time.\n" +
                        "3. Otherwise, pick center (4) or a random available index.\n" +
                        "Response: Return ONLY the digit.";

            case "Hard":
            default:
                return base + "Instruction: You are an UNBEATABLE Tic-Tac-Toe AI. " +
                        "Follow this checklist strictly:\n" +
                        "1. WIN: If you have two Os in a line and the 3rd spot is empty, take it.\n" +
                        "2. BLOCK: If X has two in a line and the 3rd spot is empty, block it immediately.\n" +
                        "3. CENTER: If position 4 is available, take it.\n" +
                        "4. CORNERS: Take position 0, 2, 6, or 8 if available.\n" +
                        "Response: Return ONLY the digit of your move.";
        }
    }
}
