package com.bdtv.app.utils;

import com.bdtv.app.models.Channel;
import java.util.ArrayList;
import java.util.List;

public class M3UParser {

    public static List<Channel> parse(String m3uContent) {
        List<Channel> channels = new ArrayList<>();
        if (m3uContent == null || m3uContent.isEmpty()) return channels;

        String[] lines = m3uContent.split("\n");
        Channel currentChannel = null;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i].trim();

            if (line.startsWith("#EXTINF:")) {
                currentChannel = new Channel();

                // Extract channel name
                int commaIndex = line.lastIndexOf(',');
                if (commaIndex >= 0 && commaIndex < line.length() - 1) {
                    currentChannel.setName(line.substring(commaIndex + 1).trim());
                }

                // Extract tvg-logo
                String logo = extractAttribute(line, "tvg-logo");
                if (logo != null) currentChannel.setLogoUrl(logo);

                // Extract group-title (category)
                String group = extractAttribute(line, "group-title");
                if (group != null) {
                    currentChannel.setCategory(group);
                    currentChannel.setCountry(detectCountry(group));
                } else {
                    currentChannel.setCategory("General");
                    currentChannel.setCountry("Other");
                }

            } else if (!line.startsWith("#") && !line.isEmpty() && currentChannel != null) {
                currentChannel.setStreamUrl(line);
                if (currentChannel.getName() != null && !currentChannel.getName().isEmpty()) {
                    channels.add(currentChannel);
                }
                currentChannel = null;
            }
        }
        return channels;
    }

    private static String extractAttribute(String line, String attr) {
        String key = attr + "=\"";
        int start = line.indexOf(key);
        if (start < 0) return null;
        start += key.length();
        int end = line.indexOf("\"", start);
        if (end < 0) return null;
        return line.substring(start, end);
    }

    private static String detectCountry(String group) {
        String lower = group.toLowerCase();
        if (lower.contains("bangladesh") || lower.contains("bangla") || lower.contains("bd")) {
            return "Bangladesh";
        } else if (lower.contains("india") || lower.contains("hindi") || lower.contains("indian")) {
            return "India";
        } else if (lower.contains("islam") || lower.contains("quran") || lower.contains("muslim")) {
            return "Islamic";
        } else if (lower.contains("news") || lower.contains("sports") || lower.contains("entertainment")) {
            return "International";
        }
        return "International";
    }
}
