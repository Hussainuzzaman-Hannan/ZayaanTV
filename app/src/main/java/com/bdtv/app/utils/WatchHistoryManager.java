package com.bdtv.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.bdtv.app.models.Channel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class WatchHistoryManager {
    private static final String PREFS = "bdtv_prefs";
    private static final String KEY_HISTORY = "watch_history";
    private static final int MAX_HISTORY = 20;

    private static WatchHistoryManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    private WatchHistoryManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        gson = new Gson();
    }

    public static WatchHistoryManager getInstance(Context ctx) {
        if (instance == null) instance = new WatchHistoryManager(ctx);
        return instance;
    }

    public void addToHistory(Channel channel) {
        List<Channel> history = getHistory();
        // ডুপ্লিকেট সরাও
        history.removeIf(c -> c.getName().equals(channel.getName()));
        // সবার আগে যোগ করো
        history.add(0, channel);
        // MAX_HISTORY এর বেশি হলে শেষেরটা সরাও
        if (history.size() > MAX_HISTORY) history.remove(history.size() - 1);
        prefs.edit().putString(KEY_HISTORY, gson.toJson(history)).apply();
    }

    public List<Channel> getHistory() {
        String json = prefs.getString(KEY_HISTORY, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Channel>>(){}.getType();
        List<Channel> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public void clearHistory() {
        prefs.edit().remove(KEY_HISTORY).apply();
    }
}