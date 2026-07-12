package com.bdtv.app.models;

import java.util.ArrayList;
import java.util.List;

public class Playlist {
    private String id;
    private String name;
    private String url;
    private String emoji;
    private long createdAt;
    private int channelCount;

    public Playlist() {}

    public Playlist(String id, String name, String url, String emoji) {
        this.id = id;
        this.name = name;
        this.url = url;
        this.emoji = emoji;
        this.createdAt = System.currentTimeMillis();
        this.channelCount = 0;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getEmoji() { return emoji != null ? emoji : "📋"; }
    public void setEmoji(String emoji) { this.emoji = emoji; }
    public long getCreatedAt() { return createdAt; }
    public int getChannelCount() { return channelCount; }
    public void setChannelCount(int count) { this.channelCount = count; }
}