package com.bdtv.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import java.util.HashSet;
import java.util.Set;

public class PinManager {
    private static final String PREFS = "bdtv_prefs";
    private static final String KEY_PIN = "parental_pin";
    private static final String KEY_LOCKED = "locked_channels";
    private static final String DEFAULT_PIN = "1234";

    private static PinManager instance;
    private final SharedPreferences prefs;

    private PinManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    public static PinManager getInstance(Context ctx) {
        if (instance == null) instance = new PinManager(ctx);
        return instance;
    }

    public boolean checkPin(String pin) {
        return getPin().equals(pin);
    }

    public String getPin() {
        return prefs.getString(KEY_PIN, DEFAULT_PIN);
    }

    public void setPin(String pin) {
        prefs.edit().putString(KEY_PIN, pin).apply();
    }

    public void lockChannel(String name) {
        Set<String> locked = new HashSet<>(prefs.getStringSet(KEY_LOCKED, new HashSet<>()));
        locked.add(name);
        prefs.edit().putStringSet(KEY_LOCKED, locked).apply();
    }

    public void unlockChannel(String name) {
        Set<String> locked = new HashSet<>(prefs.getStringSet(KEY_LOCKED, new HashSet<>()));
        locked.remove(name);
        prefs.edit().putStringSet(KEY_LOCKED, locked).apply();
    }

    public boolean isLocked(String name) {
        return prefs.getStringSet(KEY_LOCKED, new HashSet<>()).contains(name);
    }
}