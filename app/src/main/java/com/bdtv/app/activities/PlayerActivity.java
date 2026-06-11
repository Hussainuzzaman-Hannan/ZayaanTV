package com.bdtv.app.activities;

import android.content.pm.ActivityInfo;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.media3.common.MediaItem;
import androidx.media3.common.PlaybackException;
import androidx.media3.common.Player;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import com.bdtv.app.R;
import com.bdtv.app.utils.ChannelManager;
import com.bumptech.glide.Glide;

public class PlayerActivity extends AppCompatActivity {

    private ExoPlayer player;
    private PlayerView playerView;
    private TextView tvChannelName;
    private ImageView ivChannelLogo;
    private ImageButton btnFavorite, btnBack;
    private View loadingOverlay;
    private TextView tvBuffering;

    private String channelName, streamUrl, logoUrl;
    private ChannelManager channelManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Full screen immersive mode
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_FULLSCREEN |
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
        );

        setContentView(R.layout.activity_player);

        channelManager = ChannelManager.getInstance(this);

        channelName = getIntent().getStringExtra("channel_name");
        streamUrl = getIntent().getStringExtra("stream_url");
        logoUrl = getIntent().getStringExtra("logo_url");

        initViews();
        setupPlayerControls();
        initPlayer();
    }

    private void initViews() {
        playerView = findViewById(R.id.playerView);
        tvChannelName = findViewById(R.id.tvChannelName);
        ivChannelLogo = findViewById(R.id.ivChannelLogo);
        btnFavorite = findViewById(R.id.btnFavorite);
        btnBack = findViewById(R.id.btnBack);
        loadingOverlay = findViewById(R.id.loadingOverlay);
        tvBuffering = findViewById(R.id.tvBuffering);

        tvChannelName.setText(channelName);

        if (logoUrl != null && !logoUrl.isEmpty()) {
            Glide.with(this).load(logoUrl)
                .placeholder(R.drawable.ic_tv_placeholder)
                .into(ivChannelLogo);
        }

        updateFavoriteButton();
    }

    private void setupPlayerControls() {
        btnBack.setOnClickListener(v -> finish());

        btnFavorite.setOnClickListener(v -> {
            channelManager.toggleFavorite(channelName);
            updateFavoriteButton();
            boolean isFav = channelManager.isFavorite(channelName);
            Toast.makeText(this,
                isFav ? "⭐ ফেভারিটে যোগ হয়েছে" : "ফেভারিট থেকে সরানো হয়েছে",
                Toast.LENGTH_SHORT).show();
        });
    }

    private void updateFavoriteButton() {
        boolean isFav = channelManager.isFavorite(channelName);
        btnFavorite.setImageResource(isFav ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
        btnFavorite.setColorFilter(isFav ?
            getColor(R.color.accent_red) : getColor(android.R.color.white));
    }

    private void initPlayer() {
        player = new ExoPlayer.Builder(this).build();
        playerView.setPlayer(player);

        MediaItem mediaItem = MediaItem.fromUri(Uri.parse(streamUrl));
        player.setMediaItem(mediaItem);
        player.prepare();
        player.setPlayWhenReady(true);

        loadingOverlay.setVisibility(View.VISIBLE);
        tvBuffering.setText("লোড হচ্ছে...");

        player.addListener(new Player.Listener() {
            @Override
            public void onPlaybackStateChanged(int state) {
                switch (state) {
                    case Player.STATE_BUFFERING:
                        loadingOverlay.setVisibility(View.VISIBLE);
                        tvBuffering.setText("বাফারিং...");
                        break;
                    case Player.STATE_READY:
                        loadingOverlay.setVisibility(View.GONE);
                        break;
                    case Player.STATE_ENDED:
                        tvBuffering.setText("স্ট্রিম শেষ হয়েছে");
                        loadingOverlay.setVisibility(View.VISIBLE);
                        break;
                    case Player.STATE_IDLE:
                        break;
                }
            }

            @Override
            public void onPlayerError(PlaybackException error) {
                loadingOverlay.setVisibility(View.VISIBLE);
                tvBuffering.setText("❌ স্ট্রিম লোড হয়নি\nঅন্য চ্যানেল ট্রাই করুন");
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (player != null) player.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (player != null) player.play();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (player != null) {
            player.release();
            player = null;
        }
    }
}
