package com.bdtv.app.activities;

import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import com.bdtv.app.R;
import com.bdtv.app.models.Channel;
import com.bdtv.app.utils.ChannelManager;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;

    private LinearLayout topOverlay, bottomOverlay;
    private TextView tvChannelName, tvBuffering, tvAspectLabel;
    private ImageView ivChannelLogo;
    private ImageButton btnFavorite, btnBack, btnPrevious, btnPlayPause, btnNext, btnAspectRatio;
    private View loadingOverlay;

    private String channelName, streamUrl, logoUrl;
    private ChannelManager channelManager;
    private List<Channel> channelList = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isPlaying = true;

    // Aspect ratio মোডগুলো ক্রমানুসারে
    // FIT → FILL → ZOOM → FIT → ...
    private static final int[] RESIZE_MODES = {
            AspectRatioFrameLayout.RESIZE_MODE_FIT,    // 0 — কালো বার, সবটুকু দেখা যায়
            AspectRatioFrameLayout.RESIZE_MODE_FILL,   // 1 — ফুল স্ক্রিন, stretch
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM,   // 2 — ফুল স্ক্রিন, crop
    };
    private static final String[] RESIZE_LABELS = { "FIT", "FILL (Full)", "ZOOM (Crop)" };
    private int resizeModeIndex = 0; // শুরুতে FIT

    private final Handler hideHandler = new Handler();
    private static final long OVERLAY_TIMEOUT = 3000;
    private final Runnable hideOverlayRunnable = this::hideOverlays;
    private GestureDetector gestureDetector;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hideSystemUI();
        setContentView(R.layout.activity_player);

        channelManager = ChannelManager.getInstance(this);
        channelName  = getIntent().getStringExtra("channel_name");
        streamUrl    = getIntent().getStringExtra("stream_url");
        logoUrl      = getIntent().getStringExtra("logo_url");
        currentIndex = getIntent().getIntExtra("channel_index", 0);

        channelList = channelManager.getCachedChannels();
        if (channelList.isEmpty()) {
            channelList.add(new Channel(channelName, streamUrl, logoUrl, "", ""));
            currentIndex = 0;
        }

        initViews();
        setupGesture();
        initPlayer(streamUrl);
        showOverlays();
    }

    private void initViews() {
        playerView      = findViewById(R.id.playerView);
        topOverlay      = findViewById(R.id.topOverlay);
        bottomOverlay   = findViewById(R.id.bottomOverlay);
        tvChannelName   = findViewById(R.id.tvChannelName);
        ivChannelLogo   = findViewById(R.id.ivChannelLogo);
        btnFavorite     = findViewById(R.id.btnFavorite);
        btnBack         = findViewById(R.id.btnBack);
        btnPrevious     = findViewById(R.id.btnPrevious);
        btnPlayPause    = findViewById(R.id.btnPlayPause);
        btnNext         = findViewById(R.id.btnNext);
        btnAspectRatio  = findViewById(R.id.btnAspectRatio);
        loadingOverlay  = findViewById(R.id.loadingOverlay);
        tvBuffering     = findViewById(R.id.tvBuffering);
        tvAspectLabel   = findViewById(R.id.tvAspectLabel);

        updateChannelInfo();

        btnBack.setOnClickListener(v -> finish());

        btnFavorite.setOnClickListener(v -> {
            channelManager.toggleFavorite(channelName);
            updateFavoriteButton();
            Toast.makeText(this,
                    channelManager.isFavorite(channelName) ? "⭐ ফেভারিটে যোগ হয়েছে" : "ফেভারিট থেকে সরানো হয়েছে",
                    Toast.LENGTH_SHORT).show();
            resetHideTimer();
        });

        // Aspect Ratio toggle
        btnAspectRatio.setOnClickListener(v -> {
            resizeModeIndex = (resizeModeIndex + 1) % RESIZE_MODES.length;
            playerView.setResizeMode(RESIZE_MODES[resizeModeIndex]);
            showAspectLabel(RESIZE_LABELS[resizeModeIndex]);
            resetHideTimer();
        });

        btnPlayPause.setOnClickListener(v -> {
            if (isPlaying) {
                player.pause();
                isPlaying = false;
                btnPlayPause.setImageResource(R.drawable.ic_play);
            } else {
                player.play();
                isPlaying = true;
                btnPlayPause.setImageResource(R.drawable.ic_pause);
            }
            resetHideTimer();
        });

        btnPrevious.setOnClickListener(v -> {
            if (currentIndex > 0) {
                currentIndex--;
                loadChannel(channelList.get(currentIndex));
            } else {
                Toast.makeText(this, "এটাই প্রথম চ্যানেল", Toast.LENGTH_SHORT).show();
            }
            resetHideTimer();
        });

        btnNext.setOnClickListener(v -> {
            if (currentIndex < channelList.size() - 1) {
                currentIndex++;
                loadChannel(channelList.get(currentIndex));
            } else {
                Toast.makeText(this, "এটাই শেষ চ্যানেল", Toast.LENGTH_SHORT).show();
            }
            resetHideTimer();
        });
    }

    // মাঝখানে Aspect Label ১.৫ সেকেন্ড দেখিয়ে hide
    private void showAspectLabel(String label) {
        tvAspectLabel.setText("📐 " + label);
        tvAspectLabel.setVisibility(View.VISIBLE);
        tvAspectLabel.setAlpha(1f);
        hideHandler.postDelayed(() ->
                        tvAspectLabel.animate().alpha(0f).setDuration(400)
                                .withEndAction(() -> tvAspectLabel.setVisibility(View.GONE)).start(),
                1500
        );
    }

    private void loadChannel(Channel channel) {
        channelName = channel.getName();
        streamUrl   = channel.getStreamUrl();
        logoUrl     = channel.getLogoUrl();
        updateChannelInfo();
        player.stop();
        player.setMediaItem(MediaItem.fromUri(Uri.parse(streamUrl)));
        player.prepare();
        player.setPlayWhenReady(true);
        isPlaying = true;
        btnPlayPause.setImageResource(R.drawable.ic_pause);
        loadingOverlay.setVisibility(View.VISIBLE);
        tvBuffering.setText("লোড হচ্ছে...");
    }

    private void updateChannelInfo() {
        tvChannelName.setText(channelName);
        if (logoUrl != null && !logoUrl.isEmpty()) {
            Glide.with(this).load(logoUrl).placeholder(R.drawable.ic_tv_placeholder).into(ivChannelLogo);
        } else {
            ivChannelLogo.setImageResource(R.drawable.ic_tv_placeholder);
        }
        updateFavoriteButton();
        btnPrevious.setAlpha(currentIndex > 0 ? 1f : 0.3f);
        btnNext.setAlpha(currentIndex < channelList.size() - 1 ? 1f : 0.3f);
    }

    private void updateFavoriteButton() {
        boolean isFav = channelManager.isFavorite(channelName);
        btnFavorite.setImageResource(isFav ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
        btnFavorite.setColorFilter(isFav ? getColor(R.color.accent_red) : getColor(android.R.color.white));
    }

    private void setupGesture() {
        gestureDetector = new GestureDetector(this, new GestureDetector.SimpleOnGestureListener() {
            @Override
            public boolean onSingleTapUp(MotionEvent e) {
                toggleOverlays();
                return true;
            }
        });
        playerView.setOnTouchListener((v, event) -> { gestureDetector.onTouchEvent(event); return true; });
    }

    private void showOverlays() {
        topOverlay.setVisibility(View.VISIBLE);
        topOverlay.animate().alpha(1f).setDuration(200).start();
        bottomOverlay.setVisibility(View.VISIBLE);
        bottomOverlay.animate().alpha(1f).setDuration(200).start();
        resetHideTimer();
    }

    private void hideOverlays() {
        topOverlay.animate().alpha(0f).setDuration(400).withEndAction(() -> topOverlay.setVisibility(View.GONE)).start();
        bottomOverlay.animate().alpha(0f).setDuration(400).withEndAction(() -> bottomOverlay.setVisibility(View.GONE)).start();
    }

    private void toggleOverlays() {
        if (topOverlay.getVisibility() == View.VISIBLE) {
            hideHandler.removeCallbacks(hideOverlayRunnable);
            hideOverlays();
        } else {
            showOverlays();
        }
    }

    private void resetHideTimer() {
        hideHandler.removeCallbacks(hideOverlayRunnable);
        hideHandler.postDelayed(hideOverlayRunnable, OVERLAY_TIMEOUT);
    }

    private void initPlayer(String url) {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        playerView.setUseController(false);
        playerView.setResizeMode(RESIZE_MODES[resizeModeIndex]); // শুরুতে FIT

        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
        player.prepare();
        player.setPlayWhenReady(true);
        loadingOverlay.setVisibility(View.VISIBLE);
        tvBuffering.setText("লোড হচ্ছে...");

        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                switch (state) {
                    case Player.STATE_BUFFERING: loadingOverlay.setVisibility(View.VISIBLE); tvBuffering.setText("বাফারিং..."); break;
                    case Player.STATE_READY:     loadingOverlay.setVisibility(View.GONE); break;
                    case Player.STATE_ENDED:     tvBuffering.setText("স্ট্রিম শেষ"); loadingOverlay.setVisibility(View.VISIBLE); break;
                }
            }
            @Override public void onPlayerError(PlaybackException error) {
                loadingOverlay.setVisibility(View.VISIBLE);
                tvBuffering.setText("❌ স্ট্রিম লোড হয়নি\nঅন্য চ্যানেল ট্রাই করুন");
            }
        });
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override protected void onPause()   { super.onPause();   if (player != null) player.pause(); hideHandler.removeCallbacks(hideOverlayRunnable); }
    @Override protected void onResume()  { super.onResume();  if (player != null) player.play();  hideSystemUI(); }
    @Override protected void onDestroy() { super.onDestroy(); hideHandler.removeCallbacks(hideOverlayRunnable); if (player != null) { player.release(); player = null; } }
}