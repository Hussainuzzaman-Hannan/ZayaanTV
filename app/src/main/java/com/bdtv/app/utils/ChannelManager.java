package com.bdtv.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.bdtv.app.models.Channel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChannelManager {

    private static final String PREFS_NAME = "bdtv_prefs";
    private static final String KEY_FAVORITES = "favorites";
    private static final String KEY_CACHED_CHANNELS = "cached_channels";
    private static final String KEY_LAST_UPDATE = "last_update";

    // M3U সোর্স এবং প্রতিটির country ট্যাগ
    public static final String[] M3U_SOURCES = {
            // বাংলাদেশি চ্যানেল — আকাশ DTH সহ (imShakil/tvlink)
            "https://raw.githubusercontent.com/imShakil/tvlink/refs/heads/main/iptv.m3u8",
            // iptv-org বাংলাদেশ
            "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/bd.m3u",
            // iptv-org ভারত
            "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/in.m3u",
            // ইসলামিক চ্যানেল (সৌদি আরব)
            "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/sa.m3u",
            // ইসলামিক চ্যানেল (পাকিস্তান)
            "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/pk.m3u",
    };

    // প্রতিটি সোর্সের country — M3U_SOURCES-এর index অনুযায়ী
    public static final String[] SOURCE_COUNTRIES = {
            "Bangladesh",     // imShakil/tvlink — আকাশ DTH সহ বাংলাদেশি চ্যানেল
            "Bangladesh",     // iptv-org bd
            "India",          // iptv-org in
            "Islamic",        // iptv-org sa
            "Islamic",        // iptv-org pk
    };

    private static ChannelManager instance;
    private final Context context;
    private final SharedPreferences prefs;
    private final Gson gson;

    private ChannelManager(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
    }

    public static ChannelManager getInstance(Context context) {
        if (instance == null) {
            instance = new ChannelManager(context);
        }
        return instance;
    }

    public void saveChannels(List<Channel> channels) {
        String json = gson.toJson(channels);
        prefs.edit()
                .putString(KEY_CACHED_CHANNELS, json)
                .putLong(KEY_LAST_UPDATE, System.currentTimeMillis())
                .apply();
    }

    public List<Channel> getCachedChannels() {
        String json = prefs.getString(KEY_CACHED_CHANNELS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Channel>>(){}.getType();
        List<Channel> channels = gson.fromJson(json, type);
        return channels != null ? channels : new ArrayList<>();
    }

    public boolean isCacheValid() {
        long lastUpdate = prefs.getLong(KEY_LAST_UPDATE, 0);
        long oneHour = 60 * 60 * 1000;
        return (System.currentTimeMillis() - lastUpdate) < oneHour;
    }

    // cache জোর করে clear করার জন্য
    public void clearCache() {
        prefs.edit()
                .remove(KEY_CACHED_CHANNELS)
                .remove(KEY_LAST_UPDATE)
                .apply();
    }

    public void toggleFavorite(String channelName) {
        Set<String> favorites = getFavoriteNames();
        if (favorites.contains(channelName)) {
            favorites.remove(channelName);
        } else {
            favorites.add(channelName);
        }
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply();
    }

    public boolean isFavorite(String channelName) {
        return getFavoriteNames().contains(channelName);
    }

    public Set<String> getFavoriteNames() {
        return new HashSet<>(prefs.getStringSet(KEY_FAVORITES, new HashSet<>()));
    }

    // country field দিয়ে সরাসরি ফিল্টার
    public List<Channel> filterByCategory(List<Channel> channels, String category) {
        if (category.equals("All")) return channels;

        List<Channel> filtered = new ArrayList<>();
        for (Channel ch : channels) {
            if (category.equals("Favorites")) {
                if (isFavorite(ch.getName())) filtered.add(ch);
            } else {
                if (category.equals(ch.getCountry())) filtered.add(ch);
            }
        }
        return filtered;
    }

    public List<Channel> searchChannels(List<Channel> channels, String query) {
        if (query == null || query.trim().isEmpty()) return channels;
        String lower = query.toLowerCase().trim();
        List<Channel> result = new ArrayList<>();
        for (Channel ch : channels) {
            if (ch.getName() != null && ch.getName().toLowerCase().contains(lower)) {
                result.add(ch);
            }
        }
        return result;
    }
}