package com.bdtv.app.utils;

import android.content.Context;
import android.content.SharedPreferences;
import com.bdtv.app.models.Channel;
import com.bdtv.app.models.Playlist;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlaylistManager {
    private static final String PREFS = "bdtv_prefs";
    private static final String KEY_PLAYLISTS = "playlists";
    private static final String KEY_PLAYLIST_CHANNELS = "playlist_channels_";

    private static PlaylistManager instance;
    private final SharedPreferences prefs;
    private final Gson gson;

    // Default built-in playlist গুলো
    public static final String ID_ALL        = "all";
    public static final String ID_BANGLADESH = "bangladesh";
    public static final String ID_INDIA      = "india";
    public static final String ID_ISLAMIC    = "islamic";
    public static final String ID_INTL       = "international";
    public static final String ID_WORLDCUP   = "worldcup";
    public static final String ID_FAVORITES  = "favorites";

    private PlaylistManager(Context ctx) {
        prefs = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        gson  = new Gson();
    }

    public static PlaylistManager getInstance(Context ctx) {
        if (instance == null) instance = new PlaylistManager(ctx);
        return instance;
    }

    // Built-in playlist গুলো
    public List<Playlist> getBuiltinPlaylists() {
        List<Playlist> list = new ArrayList<>();
        list.add(new Playlist(ID_ALL,        "সব চ্যানেল",    "", "🌐"));
        list.add(new Playlist(ID_WORLDCUP,   "World Cup 2026","", "⚽"));
        list.add(new Playlist(ID_BANGLADESH, "Bangladesh",    "", "🇧🇩"));
        list.add(new Playlist(ID_INDIA,      "India",         "", "🇮🇳"));
        list.add(new Playlist(ID_ISLAMIC,    "Islamic",       "", "☪️"));
        list.add(new Playlist(ID_INTL,       "International", "", "📡"));
        list.add(new Playlist(ID_FAVORITES,  "Favorites",     "", "⭐"));
        return list;
    }

    // Custom playlist সেভ করা
    public void savePlaylist(Playlist playlist) {
        List<Playlist> playlists = getCustomPlaylists();
        // update if exists
        boolean found = false;
        for (int i = 0; i < playlists.size(); i++) {
            if (playlists.get(i).getId().equals(playlist.getId())) {
                playlists.set(i, playlist);
                found = true;
                break;
            }
        }
        if (!found) playlists.add(playlist);
        prefs.edit().putString(KEY_PLAYLISTS, gson.toJson(playlists)).apply();
    }

    // নতুন playlist তৈরি
    public Playlist createPlaylist(String name, String url, String emoji) {
        Playlist p = new Playlist(UUID.randomUUID().toString(), name, url, emoji);
        savePlaylist(p);
        return p;
    }

    // Playlist delete
    public void deletePlaylist(String id) {
        List<Playlist> playlists = getCustomPlaylists();
        playlists.removeIf(p -> p.getId().equals(id));
        prefs.edit()
                .putString(KEY_PLAYLISTS, gson.toJson(playlists))
                .remove(KEY_PLAYLIST_CHANNELS + id)
                .apply();
    }

    // Custom playlist গুলো লোড
    public List<Playlist> getCustomPlaylists() {
        String json = prefs.getString(KEY_PLAYLISTS, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Playlist>>(){}.getType();
        List<Playlist> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    // সব playlist (built-in + custom)
    public List<Playlist> getAllPlaylists() {
        List<Playlist> all = new ArrayList<>(getBuiltinPlaylists());
        all.addAll(getCustomPlaylists());
        return all;
    }

    // Playlist-এর চ্যানেল সেভ
    public void saveChannelsForPlaylist(String playlistId, List<Channel> channels) {
        prefs.edit()
                .putString(KEY_PLAYLIST_CHANNELS + playlistId, gson.toJson(channels))
                .apply();
        // count আপডেট
        List<Playlist> customs = getCustomPlaylists();
        for (Playlist p : customs) {
            if (p.getId().equals(playlistId)) {
                p.setChannelCount(channels.size());
                break;
            }
        }
        prefs.edit().putString(KEY_PLAYLISTS, gson.toJson(customs)).apply();
    }

    // Playlist-এর চ্যানেল লোড
    public List<Channel> getChannelsForPlaylist(String playlistId) {
        String json = prefs.getString(KEY_PLAYLIST_CHANNELS + playlistId, null);
        if (json == null) return new ArrayList<>();
        Type type = new TypeToken<List<Channel>>(){}.getType();
        List<Channel> list = gson.fromJson(json, type);
        return list != null ? list : new ArrayList<>();
    }

    public boolean isCustomPlaylist(String id) {
        for (Playlist p : getCustomPlaylists()) {
            if (p.getId().equals(id)) return true;
        }
        return false;
    }
}