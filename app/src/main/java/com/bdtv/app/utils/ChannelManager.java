package com.bdtv.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.bdtv.app.models.Channel;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ChannelManager {

    private static final String PREFS_NAME = "bdtv_prefs";
    private static final String KEY_FAVORITES = "favorites";
    private static final String KEY_CACHED_CHANNELS = "cached_channels";
    private static final String KEY_LAST_UPDATE = "last_update";

    // ───────────────────────────────────────────────
    // M3U সোর্স এবং প্রতিটির Country ট্যাগ
    // ───────────────────────────────────────────────
    public static final String[] M3U_SOURCES = {
            // বাংলাদেশি চ্যানেল — আকাশ DTH সহ (T Sports, Somoy TV, BTV ইত্যাদি)
            "https://raw.githubusercontent.com/imShakil/tvlink/refs/heads/main/iptv.m3u8",
            // iptv-org বাংলাদেশ
            "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/bd.m3u",
            // iptv-org ভারত
            "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/in.m3u",
            // iptv-org সৌদি আরব (ইসলামিক)
            "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/sa.m3u",
            // iptv-org পাকিস্তান (ইসলামিক)
            "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/pk.m3u",
            // iptv-org আন্তর্জাতিক স্পোর্টস (World Cup সম্প্রচারকারী দেশ)
            "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/us.m3u",
            "https://raw.githubusercontent.com/iptv-org/iptv/master/streams/gb.m3u",
    };

    public static final String[] SOURCE_COUNTRIES = {
            "Bangladesh",       // imShakil/tvlink
            "Bangladesh",       // iptv-org bd
            "India",            // iptv-org in
            "Islamic",          // iptv-org sa
            "Islamic",          // iptv-org pk
            "International",    // iptv-org us
            "International",    // iptv-org gb
    };

    // ───────────────────────────────────────────────
    // ফুটবল বিশ্বকাপ ২০২৬ চ্যানেল (ম্যানুয়াল হার্ডকোড)
    // বাংলাদেশে সম্প্রচার অধিকার: BTV, T Sports, Somoy TV
    // ───────────────────────────────────────────────
    public static List<Channel> getWorldCupChannels() {
        List<Channel> wc = new ArrayList<>();

        // T Sports — বাংলাদেশ (FIFA WC 2026 অফিশিয়াল সম্প্রচারক)
        wc.add(new Channel(
                "T Sports (FIFA WC 2026) 🏆",
                "https://bldcf.tsports.com.bd/live/tsports/index.m3u8",
                "https://upload.wikimedia.org/wikipedia/en/1/12/T_Sports_logo.png",
                "Sports | FIFA World Cup 2026",
                "Bangladesh"
        ));

        // Somoy TV — বাংলাদেশ (FIFA WC 2026 অফিশিয়াল সম্প্রচারক)
        wc.add(new Channel(
                "Somoy TV (FIFA WC 2026) 🏆",
                "https://somoynewsbd-lh.akamaihd.net/i/somoynews_1@394427/master.m3u8",
                "https://upload.wikimedia.org/wikipedia/en/c/c1/Somoy_TV_logo.png",
                "Sports | FIFA World Cup 2026",
                "Bangladesh"
        ));

        // BTV — বাংলাদেশ (FIFA WC 2026 ফ্রি টু এয়ার)
        wc.add(new Channel(
                "BTV (FIFA WC 2026) 🏆",
                "https://btv-live.btv.gov.bd/btv/smil:btv.stream.smil/playlist.m3u8",
                "https://upload.wikimedia.org/wikipedia/commons/e/e9/Bangladesh_Television_logo.png",
                "Sports | FIFA World Cup 2026",
                "Bangladesh"
        ));

        // BBC iPlayer (UK) — সব ম্যাচ ফ্রি
        wc.add(new Channel(
                "BBC One (FIFA WC 2026) 🏆",
                "https://vs-cmaf-push-uk.live.fastly.net/x=4/i=urn:bbc:pips:service:bbc_one_hd/mobile_wifi_main_sd.mpd",
                "https://upload.wikimedia.org/wikipedia/commons/6/62/BBC_One_HD_Logo.png",
                "Sports | FIFA World Cup 2026",
                "International"
        ));

        // ITV (UK) — সব ম্যাচ ফ্রি
        wc.add(new Channel(
                "ITV1 (FIFA WC 2026) 🏆",
                "https://simulcast.itv.com/playlist/itvonline.m3u8",
                "https://upload.wikimedia.org/wikipedia/commons/9/9f/ITV1_2013.png",
                "Sports | FIFA World Cup 2026",
                "International"
        ));

        // FOX Sports (USA) — ৭০টি ম্যাচ সম্প্রচার
        wc.add(new Channel(
                "FOX Sports (FIFA WC 2026) 🏆",
                "https://fox-sports-events.foxdcg.com/hls/v3/master_wc26.m3u8",
                "https://upload.wikimedia.org/wikipedia/commons/5/5b/Fox_Sports_logo_%282019%29.png",
                "Sports | FIFA World Cup 2026",
                "International"
        ));

        return wc;
    }

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

    public List<Channel> filterByCategory(List<Channel> channels, String category) {
        if (category.equals("All")) return channels;
        List<Channel> filtered = new ArrayList<>();
        for (Channel ch : channels) {
            if (category.equals("Favorites")) {
                if (isFavorite(ch.getName())) filtered.add(ch);
            } else if (category.equals("World Cup")) {
                // World Cup চ্যানেল ফিল্টার
                if (ch.getCategory() != null && ch.getCategory().contains("FIFA World Cup")) {
                    filtered.add(ch);
                }
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