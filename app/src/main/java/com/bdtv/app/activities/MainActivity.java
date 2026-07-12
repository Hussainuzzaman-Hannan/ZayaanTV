package com.bdtv.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bdtv.app.R;
import com.bdtv.app.adapters.ChannelAdapter;
import com.bdtv.app.adapters.PlaylistAdapter;
import com.bdtv.app.models.Channel;
import com.bdtv.app.models.Playlist;
import com.bdtv.app.utils.ChannelManager;
import com.bdtv.app.utils.M3UFetcher;
import com.bdtv.app.utils.PinManager;
import com.bdtv.app.utils.PlaylistManager;
import com.bdtv.app.utils.WatchHistoryManager;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ChannelAdapter.OnChannelClickListener {

    // Channel list (ডান)
    private RecyclerView recyclerView;
    private ChannelAdapter channelAdapter;
    private ProgressBar progressBar;
    private TextView tvStatus, tvPlaylistTitle;
    private View loadingLayout;
    private SwipeRefreshLayout swipeRefresh;
    private SearchView searchView;

    // Playlist panel (বাম)
    private RecyclerView rvPlaylists;
    private PlaylistAdapter playlistAdapter;
    private LinearLayout btnAddPlaylist;

    private List<Channel> allChannels = new ArrayList<>();
    private List<Channel> displayChannels = new ArrayList<>();

    private ChannelManager channelManager;
    private PlaylistManager playlistManager;
    private WatchHistoryManager historyManager;
    private PinManager pinManager;

    private String currentPlaylistId = PlaylistManager.ID_ALL;
    private String currentQuery = "";
    private boolean isDarkTheme = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        channelManager  = ChannelManager.getInstance(this);
        playlistManager = PlaylistManager.getInstance(this);
        historyManager  = WatchHistoryManager.getInstance(this);
        pinManager      = PinManager.getInstance(this);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayShowTitleEnabled(false);

        initViews();
        setupPlaylistPanel();
        setupChannelList();
        setupSearch();
        setupSwipeRefresh();
        loadDefaultChannels();
    }

    private void initViews() {
        recyclerView    = findViewById(R.id.recyclerView);
        progressBar     = findViewById(R.id.progressBar);
        tvStatus        = findViewById(R.id.tvStatus);
        tvPlaylistTitle = findViewById(R.id.tvPlaylistTitle);
        loadingLayout   = findViewById(R.id.loadingLayout);
        swipeRefresh    = findViewById(R.id.swipeRefresh);
        searchView      = findViewById(R.id.searchView);
        rvPlaylists     = findViewById(R.id.rvPlaylists);
        btnAddPlaylist  = findViewById(R.id.btnAddPlaylist);
    }

    // ── Playlist Panel (বাম) ──
    private void setupPlaylistPanel() {
        List<Playlist> playlists = playlistManager.getAllPlaylists();
        playlistAdapter = new PlaylistAdapter(this, playlists, new PlaylistAdapter.OnPlaylistListener() {
            @Override
            public void onPlaylistClick(Playlist playlist) {
                currentPlaylistId = playlist.getId();
                tvPlaylistTitle.setText(playlist.getEmoji() + " " + playlist.getName());
                loadPlaylist(playlist);
            }
            @Override
            public void onPlaylistDelete(Playlist playlist) {
                new AlertDialog.Builder(MainActivity.this, R.style.DarkDialog)
                        .setTitle("❌ মুছে ফেলবেন?")
                        .setMessage("\"" + playlist.getName() + "\" playlist মুছে যাবে।")
                        .setPositiveButton("মুছুন", (d, w) -> {
                            playlistManager.deletePlaylist(playlist.getId());
                            refreshPlaylistPanel();
                            if (currentPlaylistId.equals(playlist.getId())) {
                                currentPlaylistId = PlaylistManager.ID_ALL;
                                loadDefaultChannels();
                            }
                        })
                        .setNegativeButton("বাতিল", null)
                        .show();
            }
        });
        rvPlaylists.setLayoutManager(new LinearLayoutManager(this));
        rvPlaylists.setAdapter(playlistAdapter);

        // নতুন playlist যোগ বাটন
        btnAddPlaylist.setOnClickListener(v -> showAddPlaylistDialog());
    }

    private void refreshPlaylistPanel() {
        playlistAdapter.updateList(playlistManager.getAllPlaylists());
    }

    // ── নতুন Playlist তৈরির Dialog ──
    private void showAddPlaylistDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_playlist, null);
        EditText etName = view.findViewById(R.id.etPlaylistName);
        EditText etUrl  = view.findViewById(R.id.etPlaylistUrl);

        String[] emojis = {"📋", "🎬", "🎵", "📰", "🏆", "🌍", "🔞", "🎭", "📺", "🎮"};
        final String[] selectedEmoji = {"📋"};

        androidx.recyclerview.widget.RecyclerView rvEmoji = view.findViewById(R.id.rvEmoji);
        // Simple emoji selector
        rvEmoji.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        new AlertDialog.Builder(this, R.style.DarkDialog)
                .setTitle("➕ নতুন Playlist")
                .setView(view)
                .setPositiveButton("তৈরি করুন", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String url  = etUrl.getText().toString().trim();
                    if (name.isEmpty()) { Toast.makeText(this, "নাম দিন", Toast.LENGTH_SHORT).show(); return; }
                    if (url.isEmpty())  { Toast.makeText(this, "URL দিন", Toast.LENGTH_SHORT).show(); return; }
                    if (!url.startsWith("http")) { Toast.makeText(this, "সঠিক URL দিন", Toast.LENGTH_SHORT).show(); return; }

                    Playlist p = playlistManager.createPlaylist(name, url, selectedEmoji[0]);
                    refreshPlaylistPanel();
                    Toast.makeText(this, "✅ \"" + name + "\" তৈরি হয়েছে, লোড হচ্ছে...", Toast.LENGTH_SHORT).show();

                    // নতুন playlist-এর চ্যানেল লোড করো
                    currentPlaylistId = p.getId();
                    playlistAdapter.setSelected(p.getId());
                    tvPlaylistTitle.setText(p.getEmoji() + " " + p.getName());
                    loadCustomPlaylist(p);
                })
                .setNegativeButton("বাতিল", null)
                .show();
    }

    private void loadPlaylist(Playlist playlist) {
        String id = playlist.getId();

        // Built-in playlist
        switch (id) {
            case PlaylistManager.ID_ALL:
                loadDefaultChannels(); return;
            case PlaylistManager.ID_WORLDCUP:
                displayChannels = ChannelManager.getWorldCupChannels();
                channelAdapter.updateChannels(displayChannels);
                updateCount(displayChannels.size()); return;
            case PlaylistManager.ID_FAVORITES:
                displayChannels = channelManager.filterByCategory(allChannels, "Favorites");
                channelAdapter.updateChannels(displayChannels);
                updateCount(displayChannels.size()); return;
            case PlaylistManager.ID_BANGLADESH:
            case PlaylistManager.ID_INDIA:
            case PlaylistManager.ID_ISLAMIC:
            case PlaylistManager.ID_INTL:
                String cat = id.equals(PlaylistManager.ID_BANGLADESH) ? "Bangladesh"
                        : id.equals(PlaylistManager.ID_INDIA)      ? "India"
                        : id.equals(PlaylistManager.ID_ISLAMIC)     ? "Islamic" : "International";
                displayChannels = channelManager.filterByCategory(allChannels, cat);
                channelAdapter.updateChannels(displayChannels);
                updateCount(displayChannels.size()); return;
        }

        // Custom playlist
        if (playlistManager.isCustomPlaylist(id)) {
            List<Channel> cached = playlistManager.getChannelsForPlaylist(id);
            if (!cached.isEmpty()) {
                displayChannels = cached;
                channelAdapter.updateChannels(displayChannels);
                updateCount(displayChannels.size());
            } else {
                loadCustomPlaylist(playlist);
            }
        }
    }

    // Custom playlist URL থেকে চ্যানেল লোড
    private void loadCustomPlaylist(Playlist playlist) {
        showLoading(true, "\"" + playlist.getName() + "\" লোড হচ্ছে...");
        M3UFetcher.fetchAllSources(
                new String[]{ playlist.getUrl() },
                new String[]{ "Custom" },
                new M3UFetcher.FetchCallback() {
                    @Override public void onSuccess(List<Channel> channels) {
                        // প্রতিটি চ্যানেলে playlist name সেট করো (category হিসেবে)
                        for (Channel ch : channels) ch.setCategory(playlist.getName());
                        playlistManager.saveChannelsForPlaylist(playlist.getId(), channels);
                        // channelCount আপডেট
                        playlist.setChannelCount(channels.size());
                        playlistManager.savePlaylist(playlist);
                        refreshPlaylistPanel();

                        displayChannels = channels;
                        channelAdapter.updateChannels(displayChannels);
                        showLoading(false, null);
                        updateCount(channels.size());
                        Toast.makeText(MainActivity.this,
                                "✅ " + channels.size() + " টি চ্যানেল লোড হয়েছে", Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onError(String e) {
                        showLoading(false, null);
                        Toast.makeText(MainActivity.this, "❌ লোড হয়নি: " + e, Toast.LENGTH_LONG).show();
                    }
                    @Override public void onProgress(int p) {
                        tvStatus.setText("লোড হচ্ছে... " + p + "%");
                    }
                }
        );
    }

    private void loadDefaultChannels() {
        List<Channel> wcChannels = ChannelManager.getWorldCupChannels();

        if (channelManager.isCacheValid()) {
            List<Channel> cached = channelManager.getCachedChannels();
            if (!cached.isEmpty()) {
                allChannels = new ArrayList<>();
                allChannels.addAll(wcChannels);
                allChannels.addAll(cached);
                displayChannels = new ArrayList<>(allChannels);
                channelAdapter.updateChannels(displayChannels);
                updateCount(displayChannels.size());
                return;
            }
        }

        showLoading(true, "চ্যানেল লোড হচ্ছে...");
        String customUrl = SettingsActivity.getCustomUrl(this);
        String[] sources   = ChannelManager.M3U_SOURCES;
        String[] countries = ChannelManager.SOURCE_COUNTRIES;
        if (!customUrl.isEmpty()) {
            String[] ns = new String[sources.length+1]; String[] nc = new String[countries.length+1];
            System.arraycopy(sources,0,ns,0,sources.length); System.arraycopy(countries,0,nc,0,countries.length);
            ns[sources.length]=customUrl; nc[countries.length]="Bangladesh";
            sources=ns; countries=nc;
        }

        M3UFetcher.fetchAllSources(sources, countries, new M3UFetcher.FetchCallback() {
            @Override public void onSuccess(List<Channel> channels) {
                channelManager.saveChannels(channels);
                allChannels = new ArrayList<>(); allChannels.addAll(wcChannels); allChannels.addAll(channels);
                displayChannels = new ArrayList<>(allChannels);
                channelAdapter.updateChannels(displayChannels);
                showLoading(false, null);
                updateCount(displayChannels.size());
            }
            @Override public void onError(String e) {
                List<Channel> cached = channelManager.getCachedChannels();
                allChannels = new ArrayList<>(); allChannels.addAll(wcChannels);
                if (!cached.isEmpty()) allChannels.addAll(cached);
                displayChannels = new ArrayList<>(allChannels);
                channelAdapter.updateChannels(displayChannels);
                showLoading(false, null);
                updateCount(displayChannels.size());
            }
            @Override public void onProgress(int p) { tvStatus.setText("লোড হচ্ছে... " + p + "%"); }
        });
    }

    private void setupChannelList() {
        channelAdapter = new ChannelAdapter(this, displayChannels, this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(channelAdapter);
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { filterSearch(q); return true; }
            @Override public boolean onQueryTextChange(String q) { filterSearch(q); return true; }
        });
    }

    private void filterSearch(String query) {
        currentQuery = query;
        List<Channel> filtered = channelManager.searchChannels(displayChannels, query);
        channelAdapter.updateChannels(filtered);
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.accent_green, R.color.accent_red);
        swipeRefresh.setOnRefreshListener(() -> {
            channelManager.clearCache();
            loadDefaultChannels();
            swipeRefresh.setRefreshing(false);
        });
    }

    private void updateCount(int count) {
        tvPlaylistTitle.setText(count + " টি চ্যানেল");
    }

    private void showLoading(boolean show, String msg) {
        loadingLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(show && msg != null && !msg.contains("পাওয়া") ? View.VISIBLE : View.GONE);
        if (msg != null) { tvStatus.setVisibility(View.VISIBLE); tvStatus.setText(msg); }
        else tvStatus.setVisibility(View.GONE);
        if (!show) swipeRefresh.setRefreshing(false);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0,1,0,"⚙️ সেটিংস").setIcon(R.drawable.ic_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0,2,0,"🌙 থিম").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0,3,0,"🕐 দেখার ইতিহাস").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1: startActivity(new Intent(this, SettingsActivity.class)); return true;
            case 2: isDarkTheme=!isDarkTheme; AppCompatDelegate.setDefaultNightMode(isDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO); return true;
            case 3: showHistory(); return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showHistory() {
        List<Channel> history = historyManager.getHistory();
        if (history.isEmpty()) { Toast.makeText(this,"এখনো কিছু দেখা হয়নি",Toast.LENGTH_SHORT).show(); return; }
        String[] names = history.stream().map(Channel::getName).toArray(String[]::new);
        new AlertDialog.Builder(this, R.style.DarkDialog)
                .setTitle("🕐 সম্প্রতি দেখা চ্যানেল")
                .setItems(names,(d,w)->onChannelClick(history.get(w),w))
                .setNegativeButton("ইতিহাস মুছুন",(d,w)->{ historyManager.clearHistory(); Toast.makeText(this,"মুছে গেছে",Toast.LENGTH_SHORT).show(); })
                .setPositiveButton("বন্ধ করুন",null)
                .show();
    }

    @Override
    public void onChannelClick(Channel channel, int position) {
        if (pinManager.isLocked(channel.getName())) {
            View v = getLayoutInflater().inflate(R.layout.dialog_pin, null);
            androidx.appcompat.widget.AppCompatEditText et = v.findViewById(R.id.etPin);
            new AlertDialog.Builder(this, R.style.DarkDialog).setTitle("🔒 পিন দিন").setView(v)
                    .setPositiveButton("ঠিক আছে",(d,w)->{
                        String pin = et.getText()!=null?et.getText().toString():"";
                        if(pinManager.checkPin(pin)) openPlayer(channel,position);
                        else Toast.makeText(this,"❌ ভুল পিন",Toast.LENGTH_SHORT).show();
                    }).setNegativeButton("বাতিল",null).show();
        } else openPlayer(channel, position);
    }

    private void openPlayer(Channel channel, int position) {
        historyManager.addToHistory(channel);
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("channel_name", channel.getName());
        intent.putExtra("stream_url", channel.getStreamUrl());
        intent.putExtra("logo_url", channel.getLogoUrl());
        intent.putExtra("channel_index", position);
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Channel channel, int position) {
        channelManager.toggleFavorite(channel.getName());
        channelAdapter.notifyItemChanged(position);
        Toast.makeText(this, channelManager.isFavorite(channel.getName()) ? "⭐ ফেভারিটে যোগ হয়েছে" : "সরানো হয়েছে", Toast.LENGTH_SHORT).show();
    }
}