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
import java.util.List;

public class ChannelAdapter extends RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder> {

    public interface OnChannelClickListener {
        void onChannelClick(Channel channel, int position);
        void onFavoriteClick(Channel channel, int position);
    }

    private List<Channel> channels;
    private Context context;
    private OnChannelClickListener listener;
    private ChannelManager channelManager;

    public ChannelAdapter(Context context, List<Channel> channels, OnChannelClickListener listener) {
        this.context = context;
        this.channels = channels;
        this.listener = listener;
        this.channelManager = ChannelManager.getInstance(context);
    }

    @NonNull
    @Override
    public ChannelViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_channel, parent, false);
        return new ChannelViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ChannelViewHolder holder, int position) {
        Channel channel = channels.get(position);

        holder.tvChannelName.setText(channel.getName());
        holder.tvCategory.setText(channel.getCategory() != null ? channel.getCategory() : "General");

        // Load channel logo
        if (channel.getLogoUrl() != null && !channel.getLogoUrl().isEmpty()) {
            Glide.with(context)
                .load(channel.getLogoUrl())
                .placeholder(R.drawable.ic_tv_placeholder)
                .error(R.drawable.ic_tv_placeholder)
                .centerCrop()
                .into(holder.ivChannelLogo);
        } else {
            holder.ivChannelLogo.setImageResource(R.drawable.ic_tv_placeholder);
        }

        // Favorite state
        boolean isFav = channelManager.isFavorite(channel.getName());
        holder.ivFavorite.setImageResource(isFav ? R.drawable.ic_favorite_filled : R.drawable.ic_favorite_border);
        holder.ivFavorite.setColorFilter(isFav ?
            context.getColor(R.color.accent_red) :
            context.getColor(R.color.text_secondary));

        // Country badge color
        setCountryBadge(holder, channel.getCountry());

        holder.itemView.setOnClickListener(v -> listener.onChannelClick(channel, position));
        holder.ivFavorite.setOnClickListener(v -> listener.onFavoriteClick(channel, position));
    }

    private void setCountryBadge(ChannelViewHolder holder, String country) {
        if (country == null) return;
        switch (country) {
            case "Bangladesh":
                holder.tvCountryBadge.setText("🇧🇩 BD");
                holder.tvCountryBadge.setBackgroundResource(R.drawable.badge_bd);
                break;
            case "India":
                holder.tvCountryBadge.setText("🇮🇳 IN");
                holder.tvCountryBadge.setBackgroundResource(R.drawable.badge_in);
                break;
            case "Islamic":
                holder.tvCountryBadge.setText("☪️ Islamic");
                holder.tvCountryBadge.setBackgroundResource(R.drawable.badge_islamic);
                break;
            default:
                holder.tvCountryBadge.setText("🌐 Intl");
                holder.tvCountryBadge.setBackgroundResource(R.drawable.badge_intl);
                break;
        }
    }

    @Override
    public int getItemCount() {
        return channels != null ? channels.size() : 0;
    }

    public void updateChannels(List<Channel> newChannels) {
        this.channels = newChannels;
        notifyDataSetChanged();
    }

    static class ChannelViewHolder extends RecyclerView.ViewHolder {
        ImageView ivChannelLogo, ivFavorite;
        TextView tvChannelName, tvCategory, tvCountryBadge;

        ChannelViewHolder(@NonNull View itemView) {
            super(itemView);
            ivChannelLogo = itemView.findViewById(R.id.ivChannelLogo);
            ivFavorite = itemView.findViewById(R.id.ivFavorite);
            tvChannelName = itemView.findViewById(R.id.tvChannelName);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            tvCountryBadge = itemView.findViewById(R.id.tvCountryBadge);
        }
    }
}
