package com.bdtv.app.adapters;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bdtv.app.R;
import com.bdtv.app.models.Playlist;
import com.bdtv.app.utils.PlaylistManager;
import java.util.List;

public class PlaylistAdapter extends RecyclerView.Adapter<PlaylistAdapter.VH> {

    public interface OnPlaylistListener {
        void onPlaylistClick(Playlist playlist);
        void onPlaylistDelete(Playlist playlist);
    }

    private final List<Playlist> playlists;
    private final Context context;
    private final OnPlaylistListener listener;
    private String selectedId = PlaylistManager.ID_ALL;

    public PlaylistAdapter(Context ctx, List<Playlist> playlists, OnPlaylistListener listener) {
        this.context = ctx; this.playlists = playlists; this.listener = listener;
    }

    public void setSelected(String id) { selectedId = id; notifyDataSetChanged(); }

    @NonNull @Override
    public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new VH(LayoutInflater.from(context).inflate(R.layout.item_playlist, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull VH h, int pos) {
        Playlist p = playlists.get(pos);
        h.tvEmoji.setText(p.getEmoji());
        h.tvName.setText(p.getName());
        h.tvCount.setVisibility(p.getChannelCount() > 0 ? View.VISIBLE : View.GONE);
        if (p.getChannelCount() > 0) h.tvCount.setText(p.getChannelCount() + " ch");
        boolean sel = p.getId().equals(selectedId);
        h.itemView.setBackgroundResource(sel ? R.drawable.playlist_item_selected : R.drawable.playlist_item_bg);
        boolean isCustom = PlaylistManager.getInstance(context).isCustomPlaylist(p.getId());
        h.btnDelete.setVisibility(isCustom ? View.VISIBLE : View.GONE);
        h.btnDelete.setOnClickListener(v -> listener.onPlaylistDelete(p));
        h.itemView.setOnClickListener(v -> { setSelected(p.getId()); listener.onPlaylistClick(p); });
    }

    @Override public int getItemCount() { return playlists.size(); }

    public void updateList(List<Playlist> newList) {
        playlists.clear(); playlists.addAll(newList); notifyDataSetChanged();
    }

    static class VH extends RecyclerView.ViewHolder {
        TextView tvEmoji, tvName, tvCount;
        ImageButton btnDelete;
        VH(@NonNull View v) {
            super(v);
            tvEmoji   = v.findViewById(R.id.tvPlaylistEmoji);
            tvName    = v.findViewById(R.id.tvPlaylistName);
            tvCount   = v.findViewById(R.id.tvChannelCount);
            btnDelete = v.findViewById(R.id.btnDeletePlaylist);
        }
    }
}