package com.bdtv.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import com.bdtv.app.R;
import com.bdtv.app.adapters.ChannelAdapter;
import com.bdtv.app.models.Channel;
import com.bdtv.app.utils.ChannelManager;
import com.bdtv.app.utils.M3UFetcher;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity implements ChannelAdapter.OnChannelClickListener {

    private RecyclerView recyclerView;
    private ChannelAdapter adapter;
    private ProgressBar progressBar;
    private TextView tvStatus;
    private View loadingLayout;
    private SwipeRefreshLayout swipeRefresh;
    private ChipGroup chipGroup;
    private SearchView searchView;

    private List<Channel> allChannels = new ArrayList<>();
    private List<Channel> displayChannels = new ArrayList<>();
    private ChannelManager channelManager;
    private String currentCategory = "All";
    private String currentQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Toolbar সেটআপ
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        channelManager = ChannelManager.getInstance(this);
        initViews();
        setupRecyclerView();
        setupChips();
        setupSearch();
        setupSwipeRefresh();
        loadChannels(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "⚙️ সেটিংস")
                .setIcon(R.drawable.ic_settings)
                .setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            startActivity(new Intent(this, SettingsActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // Settings থেকে ফিরলে রিলোড
    @Override
    protected void onResume() {
        super.onResume();
    }

    private void initViews() {
        recyclerView  = findViewById(R.id.recyclerView);
        progressBar   = findViewById(R.id.progressBar);
        tvStatus      = findViewById(R.id.tvStatus);
        loadingLayout = findViewById(R.id.loadingLayout);
        swipeRefresh  = findViewById(R.id.swipeRefresh);
        chipGroup     = findViewById(R.id.chipGroup);
        searchView    = findViewById(R.id.searchView);
    }

    private void setupRecyclerView() {
        adapter = new ChannelAdapter(this, displayChannels, this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
    }

    private void setupChips() {
        String[] labels = {"🌐 All", "⚽ World Cup", "🇧🇩 Bangladesh", "🇮🇳 India", "☪️ Islamic", "📡 International", "⭐ Favorites"};
        String[] keys   = {"All",    "World Cup",    "Bangladesh",     "India",    "Islamic",    "International",    "Favorites"};

        for (int i = 0; i < labels.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(labels[i]);
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            chip.setTextColor(getColorStateList(R.color.chip_text_selector));
            chip.setChipBackgroundColorResource(R.color.chip_bg_selector);
            chip.setChipStrokeColorResource(R.color.chip_bg_selector);
            final String key = keys[i];
            chip.setOnClickListener(v -> { currentCategory = key; applyFilters(); });
            chipGroup.addView(chip);
        }
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override public boolean onQueryTextSubmit(String q) { currentQuery = q; applyFilters(); return true; }
            @Override public boolean onQueryTextChange(String q) { currentQuery = q; applyFilters(); return true; }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.accent_green, R.color.accent_red);
        swipeRefresh.setOnRefreshListener(() -> loadChannels(true));
    }

    private void loadChannels(boolean forceRefresh) {
        // বিশ্বকাপ চ্যানেল সবসময় আগে
        List<Channel> wcChannels = ChannelManager.getWorldCupChannels();

        // Custom URL চেক
        String customUrl = SettingsActivity.getCustomUrl(this);

        // সোর্স তৈরি
        String[] sources  = ChannelManager.M3U_SOURCES;
        String[] countries = ChannelManager.SOURCE_COUNTRIES;

        // Custom URL থাকলে শেষে যোগ করো
        if (!customUrl.isEmpty()) {
            String[] newSources   = new String[sources.length + 1];
            String[] newCountries = new String[countries.length + 1];
            System.arraycopy(sources,   0, newSources,   0, sources.length);
            System.arraycopy(countries, 0, newCountries, 0, countries.length);
            newSources  [sources.length]   = customUrl;
            newCountries[countries.length] = "Bangladesh";
            sources   = newSources;
            countries = newCountries;
        }

        if (!forceRefresh && channelManager.isCacheValid()) {
            List<Channel> cached = channelManager.getCachedChannels();
            if (!cached.isEmpty()) {
                allChannels = new ArrayList<>();
                allChannels.addAll(wcChannels);
                allChannels.addAll(cached);
                applyFilters();
                return;
            }
        }

        showLoading(true, "চ্যানেল লোড হচ্ছে...");
        final String[] finalSources   = sources;
        final String[] finalCountries = countries;

        M3UFetcher.fetchAllSources(finalSources, finalCountries, new M3UFetcher.FetchCallback() {
            @Override
            public void onSuccess(List<Channel> channels) {
                channelManager.saveChannels(channels);
                allChannels = new ArrayList<>();
                allChannels.addAll(wcChannels);
                allChannels.addAll(channels);
                applyFilters();
                showLoading(false, null);
                Toast.makeText(MainActivity.this,
                        "✅ " + allChannels.size() + " টি চ্যানেল লোড হয়েছে", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                List<Channel> cached = channelManager.getCachedChannels();
                allChannels = new ArrayList<>();
                allChannels.addAll(wcChannels);
                if (!cached.isEmpty()) allChannels.addAll(cached);
                applyFilters();
                showLoading(false, null);
            }

            @Override
            public void onProgress(int percent) {
                tvStatus.setText("লোড হচ্ছে... " + percent + "%");
            }
        });
    }

    private void applyFilters() {
        List<Channel> filtered = channelManager.filterByCategory(allChannels, currentCategory);
        filtered = channelManager.searchChannels(filtered, currentQuery);
        displayChannels = filtered;
        adapter.updateChannels(displayChannels);
        swipeRefresh.setRefreshing(false);
        if (displayChannels.isEmpty() && !allChannels.isEmpty()) {
            showLoading(true, "কোনো চ্যানেল পাওয়া যায়নি");
            progressBar.setVisibility(View.GONE);
        } else {
            showLoading(false, null);
        }
    }

    private void showLoading(boolean show, String message) {
        loadingLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(show && message != null && !message.contains("পাওয়া") ? View.VISIBLE : View.GONE);
        if (message != null) { tvStatus.setVisibility(View.VISIBLE); tvStatus.setText(message); }
        else tvStatus.setVisibility(View.GONE);
        if (!show) swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onChannelClick(Channel channel, int position) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("channel_name", channel.getName());
        intent.putExtra("stream_url",   channel.getStreamUrl());
        intent.putExtra("logo_url",     channel.getLogoUrl());
        intent.putExtra("channel_index", position);
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Channel channel, int position) {
        channelManager.toggleFavorite(channel.getName());
        adapter.notifyItemChanged(position);
        Toast.makeText(this,
                channelManager.isFavorite(channel.getName()) ? "⭐ ফেভারিটে যোগ হয়েছে" : "ফেভারিট থেকে সরানো হয়েছে",
                Toast.LENGTH_SHORT).show();
    }
}