package com.bdtv.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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

    private RecyclerView recyclerView;
    private ChannelAdapter channelAdapter;
    private ProgressBar progressBar;
    private TextView tvStatus, tvPlaylistTitle;
    private View loadingLayout;
    private SwipeRefreshLayout swipeRefresh;
    private SearchView searchView;
    private RecyclerView rvPlaylists;
    private PlaylistAdapter playlistAdapter;
    private LinearLayout btnAddPlaylist;

    private List<Channel> allChannels = new ArrayList<>();
    // ── বর্তমান playlist-এ যা দেখাচ্ছে ──
    private List<Channel> currentPlaylistChannels = new ArrayList<>();

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

        // থিম restore করো
        isDarkTheme = getSharedPreferences("bdtv_prefs", MODE_PRIVATE)
                .getBoolean("is_dark_theme", true);
        AppCompatDelegate.setDefaultNightMode(isDarkTheme
                ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO);

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
                        .setNegativeButton("বাতিল", null).show();
            }
        });
        rvPlaylists.setLayoutManager(new LinearLayoutManager(this));
        rvPlaylists.setAdapter(playlistAdapter);
        btnAddPlaylist.setOnClickListener(v -> showAddPlaylistDialog());
    }

    private void refreshPlaylistPanel() {
        playlistAdapter.updateList(playlistManager.getAllPlaylists());
    }

    private void showAddPlaylistDialog() {
        View view = getLayoutInflater().inflate(R.layout.dialog_add_playlist, null);
        androidx.appcompat.widget.AppCompatEditText etName = view.findViewById(R.id.etPlaylistName);
        androidx.appcompat.widget.AppCompatEditText etUrl  = view.findViewById(R.id.etPlaylistUrl);

        new AlertDialog.Builder(this, R.style.DarkDialog)
                .setTitle("➕ নতুন Playlist")
                .setView(view)
                .setPositiveButton("তৈরি করুন", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String url  = etUrl.getText().toString().trim();
                    if (name.isEmpty()) { Toast.makeText(this,"নাম দিন",Toast.LENGTH_SHORT).show(); return; }
                    if (!url.startsWith("http")) { Toast.makeText(this,"সঠিক URL দিন",Toast.LENGTH_SHORT).show(); return; }
                    Playlist p = playlistManager.createPlaylist(name, url, "📋");
                    refreshPlaylistPanel();
                    currentPlaylistId = p.getId();
                    playlistAdapter.setSelected(p.getId());
                    tvPlaylistTitle.setText(p.getEmoji() + " " + p.getName());
                    loadCustomPlaylist(p);
                })
                .setNegativeButton("বাতিল", null).show();
    }

    private void loadPlaylist(Playlist playlist) {
        String id = playlist.getId();
        List<Channel> channels;

        switch (id) {
            case PlaylistManager.ID_ALL:
                setCurrentChannels(allChannels); return;
            case PlaylistManager.ID_WORLDCUP:
                setCurrentChannels(ChannelManager.getWorldCupChannels()); return;
            case PlaylistManager.ID_FAVORITES:
                setCurrentChannels(channelManager.filterByCategory(allChannels, "Favorites")); return;
            case PlaylistManager.ID_BANGLADESH:
                setCurrentChannels(channelManager.filterByCategory(allChannels, "Bangladesh")); return;
            case PlaylistManager.ID_INDIA:
                setCurrentChannels(channelManager.filterByCategory(allChannels, "India")); return;
            case PlaylistManager.ID_ISLAMIC:
                setCurrentChannels(channelManager.filterByCategory(allChannels, "Islamic")); return;
            case PlaylistManager.ID_INTL:
                setCurrentChannels(channelManager.filterByCategory(allChannels, "International")); return;
        }

        // Custom playlist
        List<Channel> cached = playlistManager.getChannelsForPlaylist(id);
        if (!cached.isEmpty()) {
            setCurrentChannels(cached);
        } else {
            loadCustomPlaylist(playlist);
        }
    }

    // ── বর্তমান playlist-এর চ্যানেল সেট ও UI আপডেট ──
    private void setCurrentChannels(List<Channel> channels) {
        currentPlaylistChannels = new ArrayList<>(channels);
        channelAdapter.updateChannels(currentPlaylistChannels);
        updateCount(currentPlaylistChannels.size());
    }

    private void loadCustomPlaylist(Playlist playlist) {
        showLoading(true, "\"" + playlist.getName() + "\" লোড হচ্ছে...");
        M3UFetcher.fetchAllSources(
                new String[]{ playlist.getUrl() },
                new String[]{ "Custom" },
                new M3UFetcher.FetchCallback() {
                    @Override public void onSuccess(List<Channel> channels) {
                        for (Channel ch : channels) ch.setCategory(playlist.getName());
                        playlistManager.saveChannelsForPlaylist(playlist.getId(), channels);
                        playlist.setChannelCount(channels.size());
                        playlistManager.savePlaylist(playlist);
                        refreshPlaylistPanel();
                        setCurrentChannels(channels);
                        showLoading(false, null);
                        Toast.makeText(MainActivity.this,"✅ "+channels.size()+" টি চ্যানেল",Toast.LENGTH_SHORT).show();
                    }
                    @Override public void onError(String e) {
                        showLoading(false, null);
                        Toast.makeText(MainActivity.this,"❌ "+e,Toast.LENGTH_LONG).show();
                    }
                    @Override public void onProgress(int p) { tvStatus.setText("লোড হচ্ছে... "+p+"%"); }
                }
        );
    }

    private void loadDefaultChannels() {
        List<Channel> wc = ChannelManager.getWorldCupChannels();
        if (channelManager.isCacheValid()) {
            List<Channel> cached = channelManager.getCachedChannels();
            if (!cached.isEmpty()) {
                allChannels = new ArrayList<>();
                allChannels.addAll(wc); allChannels.addAll(cached);
                setCurrentChannels(allChannels); return;
            }
        }
        showLoading(true, "চ্যানেল লোড হচ্ছে...");
        String customUrl = SettingsActivity.getCustomUrl(this);
        String[] sources = ChannelManager.M3U_SOURCES, countries = ChannelManager.SOURCE_COUNTRIES;
        if (!customUrl.isEmpty()) {
            String[] ns=new String[sources.length+1],nc=new String[countries.length+1];
            System.arraycopy(sources,0,ns,0,sources.length); System.arraycopy(countries,0,nc,0,countries.length);
            ns[sources.length]=customUrl; nc[countries.length]="Bangladesh"; sources=ns; countries=nc;
        }
        final String[] fs=sources, fc=countries;
        M3UFetcher.fetchAllSources(fs, fc, new M3UFetcher.FetchCallback() {
            @Override public void onSuccess(List<Channel> ch) {
                channelManager.saveChannels(ch);
                allChannels=new ArrayList<>(); allChannels.addAll(wc); allChannels.addAll(ch);
                setCurrentChannels(allChannels); showLoading(false,null);
            }
            @Override public void onError(String e) {
                List<Channel> c=channelManager.getCachedChannels();
                allChannels=new ArrayList<>(); allChannels.addAll(wc);
                if(!c.isEmpty()) allChannels.addAll(c);
                setCurrentChannels(allChannels); showLoading(false,null);
            }
            @Override public void onProgress(int p) { tvStatus.setText("লোড হচ্ছে... "+p+"%"); }
        });
    }

    private void setupChannelList() {
        channelAdapter = new ChannelAdapter(this, currentPlaylistChannels, this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(channelAdapter);
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { filterSearch(q); return true; }
            @Override public boolean onQueryTextChange(String q) { filterSearch(q); return true; }
        });
    }

    private void filterSearch(String q) {
        currentQuery = q;
        List<Channel> filtered = channelManager.searchChannels(currentPlaylistChannels, q);
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

    private void updateCount(int count) { tvPlaylistTitle.setText(count + " টি চ্যানেল"); }


    private void toggleTheme() {
        isDarkTheme = !isDarkTheme;
        int mode = isDarkTheme
                ? AppCompatDelegate.MODE_NIGHT_YES
                : AppCompatDelegate.MODE_NIGHT_NO;
        AppCompatDelegate.setDefaultNightMode(mode);
        // Preference সেভ করো
        getSharedPreferences("bdtv_prefs", MODE_PRIVATE)
                .edit().putBoolean("is_dark_theme", isDarkTheme).apply();
        Toast.makeText(this,
                isDarkTheme ? "🌙 Dark Mode" : "☀️ Light Mode",
                Toast.LENGTH_SHORT).show();
    }

    private void showLoading(boolean show, String msg) {
        loadingLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(show && msg!=null && !msg.contains("পাওয়া") ? View.VISIBLE : View.GONE);
        if (msg!=null){tvStatus.setVisibility(View.VISIBLE);tvStatus.setText(msg);}else tvStatus.setVisibility(View.GONE);
        if (!show) swipeRefresh.setRefreshing(false);
    }

    @Override public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0,1,0,"⚙️ সেটিংস").setIcon(R.drawable.ic_settings).setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        menu.add(0,2,0,"🌙 থিম").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        menu.add(0,3,0,"🕐 দেখার ইতিহাস").setShowAsAction(MenuItem.SHOW_AS_ACTION_NEVER);
        return true;
    }

    @Override public boolean onOptionsItemSelected(MenuItem item) {
        switch(item.getItemId()){
            case 1: startActivity(new Intent(this,SettingsActivity.class)); return true;
            case 2:
                isDarkTheme = !isDarkTheme;
                AppCompatDelegate.setDefaultNightMode(
                        isDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
                );
                recreate(); // Activity রিস্টার্ট করে নতুন থিম apply করো
                return true;
            case 3: showHistory(); return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showHistory() {
        List<Channel> h=historyManager.getHistory();
        if(h.isEmpty()){Toast.makeText(this,"এখনো কিছু দেখা হয়নি",Toast.LENGTH_SHORT).show();return;}
        String[] names=h.stream().map(Channel::getName).toArray(String[]::new);
        new AlertDialog.Builder(this,R.style.DarkDialog).setTitle("🕐 সম্প্রতি দেখা চ্যানেল")
                .setItems(names,(d,w)->openPlayer(h.get(w), h, w))
                .setNegativeButton("ইতিহাস মুছুন",(d,w)->{historyManager.clearHistory();Toast.makeText(this,"মুছে গেছে",Toast.LENGTH_SHORT).show();})
                .setPositiveButton("বন্ধ করুন",null).show();
    }

    @Override
    public void onChannelClick(Channel channel, int position) {
        // বর্তমানে যা দেখাচ্ছে সেই list থেকে index বের করো
        List<Channel> displayed = channelAdapter.getCurrentChannels();
        int idx = displayed.indexOf(channel);
        if (idx == -1) idx = position;

        if (pinManager.isLocked(channel.getName())) {
            View v = getLayoutInflater().inflate(R.layout.dialog_pin, null);
            androidx.appcompat.widget.AppCompatEditText et = v.findViewById(R.id.etPin);
            final int finalIdx = idx;
            new AlertDialog.Builder(this,R.style.DarkDialog).setTitle("🔒 পিন দিন").setView(v)
                    .setPositiveButton("ঠিক আছে",(d,w)->{
                        String pin=et.getText()!=null?et.getText().toString():"";
                        if(pinManager.checkPin(pin)) openPlayer(channel, displayed, finalIdx);
                        else Toast.makeText(this,"❌ ভুল পিন",Toast.LENGTH_SHORT).show();
                    }).setNegativeButton("বাতিল",null).show();
        } else {
            openPlayer(channel, displayed, idx);
        }
    }

    // ── গুরুত্বপূর্ণ: বর্তমান playlist-এর channel list পাঠাচ্ছি ──
    private void openPlayer(Channel channel, List<Channel> channelList, int index) {
        historyManager.addToHistory(channel);
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("channel_name", channel.getName());
        intent.putExtra("stream_url",   channel.getStreamUrl());
        intent.putExtra("logo_url",     channel.getLogoUrl());
        intent.putExtra("channel_index", index);
        intent.putExtra("playlist_id",  currentPlaylistId); // ← playlist ID পাঠাচ্ছি
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Channel channel, int position) {
        channelManager.toggleFavorite(channel.getName());
        channelAdapter.notifyItemChanged(position);
        Toast.makeText(this,channelManager.isFavorite(channel.getName())?"⭐ ফেভারিটে যোগ হয়েছে":"সরানো হয়েছে",Toast.LENGTH_SHORT).show();
    }
}