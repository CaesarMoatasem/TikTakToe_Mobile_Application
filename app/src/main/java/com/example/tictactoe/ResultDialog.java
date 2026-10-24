package com.example.tictactoe;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;

public class ResultDialog extends Dialog {

    private final String message;
    private final MainActivity mainActivity;

    private TextView messageText;
    private Button startAgainButton;

    public ResultDialog(@NonNull Context context, String message, MainActivity mainActivity) {
        super(context);
        this.message = message;
        this.mainActivity = mainActivity;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_result_dialog);
        initComponents();
        registerEventHandlers();
        loadData();
    }

    private void initComponents() {
        messageText = findViewById(R.id.messageText);
        startAgainButton = findViewById(R.id.startAgainButton);
        Log.d("INIT", "ResultDialog initComponents done");
    }

    private void registerEventHandlers() {
        startAgainButton.setOnClickListener(view -> {
            mainActivity.restartMatch();
            dismiss();
        });
        Log.d("INIT", "ResultDialog registerEventHandlers done");
    }

    private void loadData() {
        messageText.setText(message);
        Log.d("INIT", "ResultDialog loadData done");
    }
}
