package com.bdtv.app.utils;

import android.os.Handler;
import android.os.Looper;
import com.bdtv.app.models.Channel;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class M3UFetcher {

    public interface FetchCallback {
        void onSuccess(List<Channel> channels);
        void onError(String error);
        void onProgress(int percent);
    }

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .build();

    private static final ExecutorService executor = Executors.newFixedThreadPool(4);
    private static final Handler mainHandler = new Handler(Looper.getMainLooper());

    /**
     * সব সোর্স fetch করে — প্রতিটি সোর্সের জন্য নির্দিষ্ট country ট্যাগ সেট করে
     * urls[] এবং countries[] অবশ্যই একই length হতে হবে
     */
    public static void fetchAllSources(String[] urls, String[] countries, FetchCallback callback) {
        executor.execute(() -> {
            List<Channel> allChannels = new ArrayList<>();
            AtomicInteger completed = new AtomicInteger(0);
            int total = urls.length;

            for (int i = 0; i < urls.length; i++) {
                String url = urls[i];
                String country = (countries != null && i < countries.length) ? countries[i] : "International";

                try {
                    String content = fetchUrl(url);
                    if (content != null) {
                        List<Channel> channels = M3UParser.parse(content);

                        // প্রতিটি চ্যানেলে সোর্সের country সেট করো
                        for (Channel ch : channels) {
                            ch.setCountry(country);
                        }

                        synchronized (allChannels) {
                            allChannels.addAll(channels);
                        }
                    }
                } catch (Exception e) {
                    // একটি সোর্স fail করলে বাকিগুলো চলবে
                }

                int done = completed.incrementAndGet();
                int percent = (done * 100) / total;
                mainHandler.post(() -> callback.onProgress(percent));
            }

            mainHandler.post(() -> {
                if (allChannels.isEmpty()) {
                    callback.onError("কোনো চ্যানেল লোড হয়নি। ইন্টারনেট চেক করুন।");
                } else {
                    callback.onSuccess(allChannels);
                }
            });
        });
    }

    private static String fetchUrl(String url) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", "BDTVApp/1.0")
                .build();
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                return response.body().string();
            }
        }
        return null;
    }
}