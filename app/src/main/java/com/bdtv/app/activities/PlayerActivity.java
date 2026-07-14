package com.bdtv.app.activities;

import android.app.PictureInPictureParams;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Rational;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.common.TrackSelectionParameters;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.AspectRatioFrameLayout;
import androidx.media3.ui.PlayerView;
import com.bdtv.app.R;
import com.bdtv.app.models.Channel;
import com.bdtv.app.utils.ChannelManager;
import com.bdtv.app.utils.WatchHistoryManager;
import com.bdtv.app.utils.PlaylistManager;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class PlayerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;
    private LinearLayout topOverlay, bottomOverlay;
    private TextView tvChannelName, tvBuffering, tvAspectLabel, tvQualityLabel;
    private ImageView ivChannelLogo;
    private ImageButton btnFavorite, btnBack, btnPrevious, btnPlayPause, btnNext;
    private ImageButton btnAspectRatio, btnPip, btnQuality;
    private View loadingOverlay;

    private String channelName, streamUrl, logoUrl;
    private ChannelManager channelManager;
    private WatchHistoryManager historyManager;
    private List<Channel> channelList = new ArrayList<>();
    private int currentIndex = 0;
    private boolean isPlaying = true;

    // Aspect Ratio
    private static final int[] RESIZE_MODES = {
            AspectRatioFrameLayout.RESIZE_MODE_FIT,
            AspectRatioFrameLayout.RESIZE_MODE_FILL,
            AspectRatioFrameLayout.RESIZE_MODE_ZOOM,
    };
    private static final String[] RESIZE_LABELS = {"FIT", "FILL", "ZOOM"};
    private int resizeModeIndex = 0;

    // Video Quality
    private int qualityIndex = 1; // 0=Low, 1=Auto, 2=High
    private static final String[] QUALITY_LABELS = {"🔵 Low", "🟢 Auto", "🔴 High"};

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

        channelManager  = ChannelManager.getInstance(this);
        historyManager  = WatchHistoryManager.getInstance(this);

        channelName  = getIntent().getStringExtra("channel_name");
        streamUrl    = getIntent().getStringExtra("stream_url");
        logoUrl      = getIntent().getStringExtra("logo_url");
        currentIndex = getIntent().getIntExtra("channel_index", 0);
        String playlistId = getIntent().getStringExtra("playlist_id");

        channelList = loadChannelListForPlaylist(playlistId);
        if (channelList.isEmpty()) {
            channelList.add(new Channel(channelName, streamUrl, logoUrl, "", ""));
            currentIndex = 0;
        }

        initViews();
        setupGesture();
        initPlayer(streamUrl);
        showOverlays();

        // History-তে যোগ করো
        historyManager.addToHistory(new Channel(channelName, streamUrl, logoUrl, "", ""));
    }

    private void initViews() {
        playerView     = findViewById(R.id.playerView);
        topOverlay     = findViewById(R.id.topOverlay);
        bottomOverlay  = findViewById(R.id.bottomOverlay);
        tvChannelName  = findViewById(R.id.tvChannelName);
        ivChannelLogo  = findViewById(R.id.ivChannelLogo);
        btnFavorite    = findViewById(R.id.btnFavorite);
        btnBack        = findViewById(R.id.btnBack);
        btnPrevious    = findViewById(R.id.btnPrevious);
        btnPlayPause   = findViewById(R.id.btnPlayPause);
        btnNext        = findViewById(R.id.btnNext);
        btnAspectRatio = findViewById(R.id.btnAspectRatio);
        btnPip         = findViewById(R.id.btnPip);
        btnQuality     = findViewById(R.id.btnQuality);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        tvBuffering    = findViewById(R.id.tvBuffering);
        tvAspectLabel  = findViewById(R.id.tvAspectLabel);

        updateChannelInfo();

        btnBack.setOnClickListener(v -> finish());

        // ── Favorite ──
        btnFavorite.setOnClickListener(v -> {
            channelManager.toggleFavorite(channelName);
            updateFavoriteButton();
            Toast.makeText(this, channelManager.isFavorite(channelName)
                    ? "⭐ ফেভারিটে যোগ হয়েছে" : "ফেভারিট থেকে সরানো হয়েছে", Toast.LENGTH_SHORT).show();
            resetHideTimer();
        });

        // ── Aspect Ratio ──
        btnAspectRatio.setOnClickListener(v -> {
            resizeModeIndex = (resizeModeIndex + 1) % RESIZE_MODES.length;
            playerView.setResizeMode(RESIZE_MODES[resizeModeIndex]);
            showTempLabel("📐 " + RESIZE_LABELS[resizeModeIndex]);
            resetHideTimer();
        });

        // ── Picture-in-Picture ──
        btnPip.setOnClickListener(v -> {
            enterPipMode();
            resetHideTimer();
        });

        // ── Video Quality ──
        btnQuality.setOnClickListener(v -> {
            showQualityDialog();
            resetHideTimer();
        });

        // ── Play/Pause ──
        btnPlayPause.setOnClickListener(v -> {
            if (isPlaying) { player.pause(); isPlaying = false; btnPlayPause.setImageResource(R.drawable.ic_play); }
            else           { player.play();  isPlaying = true;  btnPlayPause.setImageResource(R.drawable.ic_pause); }
            resetHideTimer();
        });

        // ── Previous ──
        btnPrevious.setOnClickListener(v -> {
            if (currentIndex > 0) { currentIndex--; loadChannel(channelList.get(currentIndex)); }
            else Toast.makeText(this, "এটাই প্রথম চ্যানেল", Toast.LENGTH_SHORT).show();
            resetHideTimer();
        });

        // ── Next ──
        btnNext.setOnClickListener(v -> {
            if (currentIndex < channelList.size() - 1) { currentIndex++; loadChannel(channelList.get(currentIndex)); }
            else Toast.makeText(this, "এটাই শেষ চ্যানেল", Toast.LENGTH_SHORT).show();
            resetHideTimer();
        });
    }

    // ── Picture-in-Picture ──
    private void enterPipMode() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            PictureInPictureParams params = new PictureInPictureParams.Builder()
                    .setAspectRatio(new Rational(16, 9))
                    .build();
            enterPictureInPictureMode(params);
        } else {
            Toast.makeText(this, "এই ফোনে PiP সাপোর্ট নেই", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean isInPipMode, Configuration newConfig) {
        super.onPictureInPictureModeChanged(isInPipMode, newConfig);
        if (isInPipMode) {
            // PiP মোডে overlay লুকাও, player চালু রাখো
            topOverlay.setVisibility(View.GONE);
            bottomOverlay.setVisibility(View.GONE);
            tvAspectLabel.setVisibility(View.GONE);
            loadingOverlay.setVisibility(View.GONE);
            if (player != null) player.play(); // নিশ্চিত করো চলছে
        } else {
            // PiP থেকে ফিরলে
            hideSystemUI();
            if (player != null && isPlaying) player.play();
        }
    }

    // ── Video Quality Dialog ──
    private void showQualityDialog() {
        String[] options = {"🔵 Low (360p)", "🟢 Auto (সেরা)", "🔴 High (720p+)"};
        new AlertDialog.Builder(this, R.style.DarkDialog)
                .setTitle("📺 ভিডিও কোয়ালিটি")
                .setSingleChoiceItems(options, qualityIndex, (dialog, which) -> {
                    qualityIndex = which;
                    applyQuality(which);
                    showTempLabel(QUALITY_LABELS[which]);
                    dialog.dismiss();
                })
                .show();
    }

    private void applyQuality(int index) {
        if (player == null) return;
        TrackSelectionParameters.Builder builder = player.getTrackSelectionParameters().buildUpon();
        switch (index) {
            case 0: // Low
                builder.setMaxVideoSizeSd();
                break;
            case 1: // Auto
                builder.clearVideoSizeConstraints();
                break;
            case 2: // High
                builder.setMinVideoSize(1280, 720);
                break;
        }
        player.setTrackSelectionParameters(builder.build());
    }

    private void showTempLabel(String text) {
        tvAspectLabel.setText(text);
        tvAspectLabel.setVisibility(View.VISIBLE);
        tvAspectLabel.setAlpha(1f);
        hideHandler.postDelayed(() ->
                tvAspectLabel.animate().alpha(0f).setDuration(400)
                        .withEndAction(() -> tvAspectLabel.setVisibility(View.GONE)).start(), 1500);
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
        historyManager.addToHistory(channel);
        applyQuality(qualityIndex);
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
            @Override public boolean onSingleTapUp(MotionEvent e) { toggleOverlays(); return true; }
        });
        playerView.setOnTouchListener((v, e) -> { gestureDetector.onTouchEvent(e); return true; });
    }

    private void showOverlays() {
        topOverlay.setVisibility(View.VISIBLE); topOverlay.animate().alpha(1f).setDuration(200).start();
        bottomOverlay.setVisibility(View.VISIBLE); bottomOverlay.animate().alpha(1f).setDuration(200).start();
        resetHideTimer();
    }

    private void hideOverlays() {
        topOverlay.animate().alpha(0f).setDuration(400).withEndAction(() -> topOverlay.setVisibility(View.GONE)).start();
        bottomOverlay.animate().alpha(0f).setDuration(400).withEndAction(() -> bottomOverlay.setVisibility(View.GONE)).start();
    }

    private void toggleOverlays() {
        if (topOverlay.getVisibility() == View.VISIBLE) { hideHandler.removeCallbacks(hideOverlayRunnable); hideOverlays(); }
        else showOverlays();
    }

    private void resetHideTimer() {
        hideHandler.removeCallbacks(hideOverlayRunnable);
        hideHandler.postDelayed(hideOverlayRunnable, OVERLAY_TIMEOUT);
    }


    // Playlist অনুযায়ী সঠিক channel list লোড
    private List<Channel> loadChannelListForPlaylist(String playlistId) {
        PlaylistManager pm = PlaylistManager.getInstance(this);
        ChannelManager cm  = ChannelManager.getInstance(this);
        if (playlistId == null || playlistId.equals(PlaylistManager.ID_ALL))
            return cm.getCachedChannels();
        if (playlistId.equals(PlaylistManager.ID_WORLDCUP))
            return ChannelManager.getWorldCupChannels();
        if (playlistId.equals(PlaylistManager.ID_FAVORITES))
            return cm.filterByCategory(cm.getCachedChannels(), "Favorites");
        if (playlistId.equals(PlaylistManager.ID_BANGLADESH))
            return cm.filterByCategory(cm.getCachedChannels(), "Bangladesh");
        if (playlistId.equals(PlaylistManager.ID_INDIA))
            return cm.filterByCategory(cm.getCachedChannels(), "India");
        if (playlistId.equals(PlaylistManager.ID_ISLAMIC))
            return cm.filterByCategory(cm.getCachedChannels(), "Islamic");
        if (playlistId.equals(PlaylistManager.ID_INTL))
            return cm.filterByCategory(cm.getCachedChannels(), "International");
        // Custom playlist
        List<Channel> custom = pm.getChannelsForPlaylist(playlistId);
        return custom.isEmpty() ? cm.getCachedChannels() : custom;
    }

    private void initPlayer(String url) {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);
        playerView.setUseController(false);
        playerView.setResizeMode(RESIZE_MODES[resizeModeIndex]);
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)));
        player.prepare();
        player.setPlayWhenReady(true);
        loadingOverlay.setVisibility(View.VISIBLE);
        tvBuffering.setText("লোড হচ্ছে...");
        player.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int s) {
                if (s == Player.STATE_BUFFERING) { loadingOverlay.setVisibility(View.VISIBLE); tvBuffering.setText("বাফারিং..."); }
                else if (s == Player.STATE_READY) loadingOverlay.setVisibility(View.GONE);
                else if (s == Player.STATE_ENDED) { tvBuffering.setText("স্ট্রিম শেষ"); loadingOverlay.setVisibility(View.VISIBLE); }
            }
            @Override public void onPlayerError(PlaybackException e) {
                loadingOverlay.setVisibility(View.VISIBLE);
                tvBuffering.setText("❌ স্ট্রিম লোড হয়নি\nঅন্য চ্যানেল ট্রাই করুন");
            }
        });
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    @Override public void onUserLeaveHint() { super.onUserLeaveHint(); enterPipMode(); }

    @Override
    protected void onPause() {
        super.onPause();
        // PiP মোডে থাকলে player pause করবো না — ভিডিও চলতে থাকবে
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && isInPictureInPictureMode()) {
            return;
        }
        if (player != null) player.pause();
        hideHandler.removeCallbacks(hideOverlayRunnable);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null && !isPlaying) {
            // ব্যবহারকারী নিজে pause করলে auto-play করবো না
        } else if (player != null) {
            player.play();
        }
        hideSystemUI();
    }

    @Override protected void onDestroy() { super.onDestroy(); hideHandler.removeCallbacks(hideOverlayRunnable); if (player != null) { player.release(); player = null; } }
}