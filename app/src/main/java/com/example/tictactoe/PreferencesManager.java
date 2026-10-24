package com.example.tictactoe;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREF_NAME = "ttt_prefs";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_LANGUAGE = "language";
    private static final String KEY_CACHED_USERNAME = "cached_username";

    private final SharedPreferences prefs;

    public PreferencesManager(Context context) {
        prefs = context.getApplicationContext().getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }

    public boolean isDarkMode() {
        return prefs.getBoolean(KEY_DARK_MODE, false);
    }

    public void setDarkMode(boolean enabled) {
        prefs.edit().putBoolean(KEY_DARK_MODE, enabled).apply();
    }

    public String getLanguage() {
        return prefs.getString(KEY_LANGUAGE, "en");
    }

    public void setLanguage(String lang) {
        prefs.edit().putString(KEY_LANGUAGE, lang).apply();
    }

    public String getCachedUsername() {
        return prefs.getString(KEY_CACHED_USERNAME, "");
    }

    public void setCachedUsername(String username) {
        prefs.edit().putString(KEY_CACHED_USERNAME, username).apply();
    }
}
