package com.bdtv.app.models;

public class Channel {
    private String name;
    private String streamUrl;
    private String logoUrl;
    private String category;
    private String country;
    private boolean isFavorite;

    public Channel() {}

    public Channel(String name, String streamUrl, String logoUrl, String category, String country) {
        this.name = name;
        this.streamUrl = streamUrl;
        this.logoUrl = logoUrl;
        this.category = category;
        this.country = country;
        this.isFavorite = false;
    }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getStreamUrl() { return streamUrl; }
    public void setStreamUrl(String streamUrl) { this.streamUrl = streamUrl; }

    public String getLogoUrl() { return logoUrl; }
    public void setLogoUrl(String logoUrl) { this.logoUrl = logoUrl; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getCountry() { return country; }
    public void setCountry(String country) { this.country = country; }

    public boolean isFavorite() { return isFavorite; }
    public void setFavorite(boolean favorite) { isFavorite = favorite; }
}
