package com.bdtv.app.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
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

        channelManager = ChannelManager.getInstance(this);
        initViews();
        setupRecyclerView();
        setupChips();
        setupSearch();
        setupSwipeRefresh();
        loadChannels(false);
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        tvStatus = findViewById(R.id.tvStatus);
        loadingLayout = findViewById(R.id.loadingLayout);
        swipeRefresh = findViewById(R.id.swipeRefresh);
        chipGroup = findViewById(R.id.chipGroup);
        searchView = findViewById(R.id.searchView);
    }

    private void setupRecyclerView() {
        adapter = new ChannelAdapter(this, displayChannels, this);
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
    }

    private void setupChips() {
        String[] categories = {"All", "Bangladesh", "India", "Islamic", "International", "Favorites"};
        String[] icons      = {"🌐",  "🇧🇩",         "🇮🇳",   "☪️",      "📡",            "⭐"};

        for (int i = 0; i < categories.length; i++) {
            Chip chip = new Chip(this);
            chip.setText(icons[i] + " " + categories[i]);
            chip.setCheckable(true);
            chip.setChecked(i == 0);
            chip.setTextColor(getColorStateList(R.color.chip_text_selector));
            chip.setChipBackgroundColorResource(R.color.chip_bg_selector);
            chip.setChipStrokeColorResource(R.color.chip_bg_selector);
            final String cat = categories[i];
            chip.setOnClickListener(v -> {
                currentCategory = cat;
                applyFilters();
            });
            chipGroup.addView(chip);
        }
    }

    private void setupSearch() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                currentQuery = query;
                applyFilters();
                return true;
            }
            @Override
            public boolean onQueryTextChange(String newText) {
                currentQuery = newText;
                applyFilters();
                return true;
            }
        });
    }

    private void setupSwipeRefresh() {
        swipeRefresh.setColorSchemeResources(R.color.accent_green, R.color.accent_red);
        swipeRefresh.setOnRefreshListener(() -> loadChannels(true));
    }

    private void loadChannels(boolean forceRefresh) {
        if (!forceRefresh && channelManager.isCacheValid()) {
            List<Channel> cached = channelManager.getCachedChannels();
            if (!cached.isEmpty()) {
                allChannels = cached;
                applyFilters();
                return;
            }
        }

        showLoading(true, "চ্যানেল লোড হচ্ছে...");

        M3UFetcher.fetchAllSources(ChannelManager.M3U_SOURCES, ChannelManager.SOURCE_COUNTRIES, new M3UFetcher.FetchCallback() {
            @Override
            public void onSuccess(List<Channel> channels) {
                allChannels = channels;
                channelManager.saveChannels(channels);
                applyFilters();
                showLoading(false, null);
                Toast.makeText(MainActivity.this,
                        channels.size() + " টি চ্যানেল লোড হয়েছে ✅", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onError(String error) {
                List<Channel> cached = channelManager.getCachedChannels();
                if (!cached.isEmpty()) {
                    allChannels = cached;
                    applyFilters();
                    showLoading(false, null);
                } else {
                    showLoading(true, error);
                    swipeRefresh.setRefreshing(false);
                }
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
        } else if (!displayChannels.isEmpty()) {
            showLoading(false, null);
        }
    }

    private void showLoading(boolean show, String message) {
        loadingLayout.setVisibility(show ? View.VISIBLE : View.GONE);
        progressBar.setVisibility(show && message != null && message.contains("%") || (show && !message.contains("পাওয়া")) ? View.VISIBLE : View.GONE);
        if (message != null) {
            tvStatus.setVisibility(View.VISIBLE);
            tvStatus.setText(message);
        } else {
            tvStatus.setVisibility(View.GONE);
        }
        if (!show) swipeRefresh.setRefreshing(false);
    }

    @Override
    public void onChannelClick(Channel channel, int position) {
        Intent intent = new Intent(this, PlayerActivity.class);
        intent.putExtra("channel_name", channel.getName());
        intent.putExtra("stream_url", channel.getStreamUrl());
        intent.putExtra("logo_url", channel.getLogoUrl());
        startActivity(intent);
    }

    @Override
    public void onFavoriteClick(Channel channel, int position) {
        channelManager.toggleFavorite(channel.getName());
        adapter.notifyItemChanged(position);
        boolean isFav = channelManager.isFavorite(channel.getName());
        Toast.makeText(this,
                isFav ? "⭐ ফেভারিটে যোগ হয়েছে" : "ফেভারিট থেকে সরানো হয়েছে",
                Toast.LENGTH_SHORT).show();
    }
}