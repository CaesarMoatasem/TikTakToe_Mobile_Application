package com.example.tictactoe;

import android.app.Application;

import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.os.LocaleListCompat;

public class TicTacToeApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        PreferencesManager prefs = new PreferencesManager(this);

        int nightMode = prefs.isDarkMode()
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(nightMode);

        String lang = prefs.getLanguage();
        if (lang != null && !lang.isEmpty()) {
            AppCompatDelegate.setApplicationLocales(LocaleListCompat.forLanguageTags(lang));
        }
    }
}
