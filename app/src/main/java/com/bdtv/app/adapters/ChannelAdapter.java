package com.bdtv.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bdtv.app.R;
import com.bdtv.app.models.Channel;
import com.bdtv.app.utils.ChannelManager;
import com.bumptech.glide.Glide;
import java.util.ArrayList;
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {

    public interface OnChannelClickListener {
        void onChannelClick(Channel channel, int position);
        void onFavoriteClick(Channel channel, int position);
    }

    private List<Channel> channels;
    private final Context context;
    private final OnChannelClickListener listener;
    private final ChannelManager channelManager;

    public ChannelAdapter(Context context, List<Channel> channels, OnChannelClickListener listener) {
        this.context        = context;
        this.channels       = channels != null ? channels : new ArrayList<>();
        this.listener       = listener;
        this.channelManager = ChannelManager.getInstance(context);
    }

    @NonNull @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ChannelViewHolder(LayoutInflater.from(context).inflate(R.layout.item_channel, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder h, int pos) {
        Channel ch = channels.get(pos);
        h.tvChannelName.setText(ch.getName());
        h.tvCategory.setText(ch.getCategory() != null ? ch.getCategory() : "General");

        if (ch.getLogoUrl() != null && !ch.getLogoUrl().isEmpty()) {
            Glide.with(context).load(ch.getLogoUrl())
                    .placeholder(R.drawable.ic_tv_placeholder)
                    .error(R.drawable.ic_tv_placeholder)
                    .centerCrop().into(h.ivChannelLogo);
        } else {
            h.ivChannelLogo.setImageResource(R.drawable.ic_tv_placeholder);
        }

        boolean isFav = channelManager.isFavorite(ch.getName());
        h.ivFavorite.setImageResource(isFav ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
        h.ivFavorite.setColorFilter(isFav ? context.getColor(R.color.accent_red) : context.getColor(R.color.text_secondary));

        setCountryBadge(h, ch.getCountry());

        h.itemView.setOnClickListener(v -> listener.onChannelClick(ch, pos));
        h.ivFavorite.setOnClickListener(v -> listener.onFavoriteClick(ch, pos));
    }

    private void setCountryBadge(ChannelViewHolder h, String country) {
        if (country == null) { h.tvCountryBadge.setText("🌐 Intl"); h.tvCountryBadge.setBackgroundResource(R.drawable.badge_intl); return; }
        switch (country) {
            case "Bangladesh": h.tvCountryBadge.setText("🇧🇩 BD");   h.tvCountryBadge.setBackgroundResource(R.drawable.badge_bd);     break;
            case "India":      h.tvCountryBadge.setText("🇮🇳 IN");   h.tvCountryBadge.setBackgroundResource(R.drawable.badge_in);     break;
            case "Islamic":    h.tvCountryBadge.setText("☪️ Islamic"); h.tvCountryBadge.setBackgroundResource(R.drawable.badge_islamic); break;
            default:           h.tvCountryBadge.setText("🌐 Intl");   h.tvCountryBadge.setBackgroundResource(R.drawable.badge_intl);   break;
        }
    }

    @Override public int getItemCount() { return channels != null ? channels.size() : 0; }

    public void updateChannels(List<Channel> newChannels) {
        this.channels = newChannels != null ? newChannels : new ArrayList<>();
        notifyDataSetChanged();
    }

    // ── বর্তমানে দেখানো channel list ফেরত দেয় ──
    public List<Channel> getCurrentChannels() {
        return channels != null ? channels : new ArrayList<>();
    }

    static class ChannelViewHolder extends RecyclerView.ViewHolder {
        ImageView ivChannelLogo, ivFavorite;
        TextView tvChannelName, tvCategory, tvCountryBadge;
        ChannelViewHolder(@NonNull View v) {
            super(v);
            ivChannelLogo  = v.findViewById(R.id.ivChannelLogo);
            ivFavorite     = v.findViewById(R.id.ivFavorite);
            tvChannelName  = v.findViewById(R.id.tvChannelName);
            tvCategory     = v.findViewById(R.id.tvCategory);
            tvCountryBadge = v.findViewById(R.id.tvCountryBadge);
        }
    }
}