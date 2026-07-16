package com.bdtv.app.utils;

import android.os.Handler;
import android.os.Looper;
import com.bdtv.app.models.Channel;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class ChannelTester {

    public interface TestCallback {
        void onProgress(int tested, int total, int broken, String channelName);
        void onComplete(List<Channel> working, List<Channel> broken);
        void onCancelled();
    }

    private static final OkHttpClient client = new OkHttpClient.Builder()
            .connectTimeout(4, TimeUnit.SECONDS)
            .readTimeout(4, TimeUnit.SECONDS)
            .build();

    private static final Handler mainHandler = new Handler(Looper.getMainLooper());
    private volatile boolean cancelled = false;
    private ExecutorService executor;

    private static final int THREAD_COUNT = 30;

    public void testChannels(List<Channel> channels, TestCallback callback) {
        cancelled = false;
        executor = Executors.newFixedThreadPool(THREAD_COUNT);

        List<Channel> working = new ArrayList<>();
        List<Channel> broken  = new ArrayList<>();
        AtomicInteger tested  = new AtomicInteger(0);
        AtomicInteger brokenCount = new AtomicInteger(0);
        int total = channels.size();

        for (Channel channel : channels) {
            executor.submit(() -> {
                boolean isWorking = false;
                try {
                    if (cancelled) return;
                    isWorking = testUrl(channel.getStreamUrl());
                } catch (Throwable t) {
                    isWorking = false;
                }

                if (cancelled) return;

                synchronized (working) {
                    if (isWorking) working.add(channel);
                    else broken.add(channel);
                }

                int doneCount = tested.incrementAndGet();
                int brokenNow = isWorking ? brokenCount.get() : brokenCount.incrementAndGet();
                String chName = channel.getName();

                mainHandler.post(() ->
                        callback.onProgress(doneCount, total, brokenNow, chName)
                );

                if (doneCount == total) {
                    mainHandler.post(() -> {
                        if (cancelled) callback.onCancelled();
                        else callback.onComplete(working, broken);
                    });
                }
            });
        }
    }

    // ── HEAD দিয়ে চেষ্টা, ব্যর্থ হলে GET (Range header সহ) দিয়ে fallback ──
    private boolean testUrl(String url) {
        if (url == null || url.isEmpty()) return false;
        if (tryRequest(url, true)) return true;   // HEAD
        return tryRequest(url, false);            // GET fallback
    }

    private boolean tryRequest(String url, boolean isHead) {
        try {
            Request.Builder builder = new Request.Builder()
                    .url(url)
                    .header("User-Agent", "ZayaanTV/1.0");
            if (isHead) {
                builder.head();
            } else {
                // পুরো স্ট্রিম না নামিয়ে শুধু প্রথম কিছু byte চেক করা
                builder.get().header("Range", "bytes=0-2048");
            }
            try (Response res = client.newCall(builder.build()).execute()) {
                int code = res.code();
                return code == 200 || code == 206 || code == 301 || code == 302;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public void cancel() {
        cancelled = true;
        if (executor != null) executor.shutdownNow();
    }
}