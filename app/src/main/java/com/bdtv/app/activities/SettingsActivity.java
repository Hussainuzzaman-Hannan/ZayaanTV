package com.bdtv.app.activities;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bdtv.app.R;
import com.bdtv.app.models.Playlist;
import com.bdtv.app.utils.PlaylistManager;
import java.util.List;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS     = "bdtv_prefs";
    private static final String KEY_PIN   = "parental_pin";

    private LinearLayout llSavedPlaylists;
    private EditText etCustomUrl;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("⚙️ সেটিংস");
        }

        etCustomUrl      = findViewById(R.id.etCustomUrl);
        llSavedPlaylists = findViewById(R.id.llSavedPlaylists);
        Button btnSave   = findViewById(R.id.btnSaveUrl);
        Button btnPaste  = findViewById(R.id.btnPasteUrl);
        Button btnClear  = findViewById(R.id.btnClearUrl);
        Button btnChangePin = findViewById(R.id.btnChangePin);

        // সেভ করা URL লোড
        String saved = getCustomUrl(this);
        if (!saved.isEmpty()) etCustomUrl.setText(saved);

        // সেভ করা Playlist লিঙ্কগুলো দেখাও
        loadSavedPlaylists();

        btnPaste.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null) {
                CharSequence text = cm.getPrimaryClip().getItemAt(0).getText();
                if (text != null) etCustomUrl.setText(text.toString().trim());
            }
        });

        btnSave.setOnClickListener(v -> {
            String url = etCustomUrl.getText().toString().trim();
            if (url.isEmpty()) { Toast.makeText(this,"URL খালি রাখা যাবে না",Toast.LENGTH_SHORT).show(); return; }
            if (!url.startsWith("http")) { Toast.makeText(this,"সঠিক URL দিন",Toast.LENGTH_SHORT).show(); return; }
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .putString("custom_m3u_url", url)
                    .remove("cached_channels").remove("last_update").apply();
            Toast.makeText(this,"✅ সেভ হয়েছে! অ্যাপ রিস্টার্ট করুন",Toast.LENGTH_LONG).show();
            finish();
        });

        btnClear.setOnClickListener(v -> {
            etCustomUrl.setText("");
            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                    .remove("custom_m3u_url")
                    .remove("cached_channels").remove("last_update").apply();
            Toast.makeText(this,"URL মুছে গেছে",Toast.LENGTH_SHORT).show();
        });

        // PIN পরিবর্তন
        btnChangePin.setOnClickListener(v -> showChangePinDialog());
    }

    // ── সেভ করা Playlist লিঙ্কগুলো দেখাও ──
    private void loadSavedPlaylists() {
        llSavedPlaylists.removeAllViews();
        List<Playlist> playlists = PlaylistManager.getInstance(this).getCustomPlaylists();

        if (playlists.isEmpty()) {
            TextView empty = new TextView(this);
            empty.setText("এখনো কোনো Playlist যোগ করা হয়নি।\nমূল স্ক্রিনের ➕ বাটন দিয়ে যোগ করুন।");
            empty.setTextColor(0x88FFFFFF);
            empty.setTextSize(13f);
            empty.setPadding(16, 12, 16, 12);
            llSavedPlaylists.addView(empty);
            return;
        }

        for (Playlist p : playlists) {
            View item = LayoutInflater.from(this)
                    .inflate(R.layout.item_saved_playlist, llSavedPlaylists, false);

            TextView tvName  = item.findViewById(R.id.tvSavedName);
            TextView tvUrl   = item.findViewById(R.id.tvSavedUrl);
            ImageButton btnCopy   = item.findViewById(R.id.btnCopyUrl);
            ImageButton btnEdit   = item.findViewById(R.id.btnEditUrl);
            ImageButton btnDelete = item.findViewById(R.id.btnDeleteSaved);

            tvName.setText(p.getEmoji() + " " + p.getName()
                    + (p.getChannelCount() > 0 ? "  (" + p.getChannelCount() + " ch)" : ""));
            tvUrl.setText(p.getUrl());

            // URL Copy
            btnCopy.setOnClickListener(v -> {
                android.content.ClipboardManager cm =
                        (android.content.ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null) {
                    cm.setPrimaryClip(android.content.ClipData.newPlainText("url", p.getUrl()));
                    Toast.makeText(this, "✅ URL কপি হয়েছে", Toast.LENGTH_SHORT).show();
                }
            });

            // URL Edit
            btnEdit.setOnClickListener(v -> showEditDialog(p));

            // Delete
            btnDelete.setOnClickListener(v ->
                    new AlertDialog.Builder(this, R.style.DarkDialog)
                            .setTitle("❌ মুছবেন?")
                            .setMessage("\"" + p.getName() + "\" মুছে যাবে।")
                            .setPositiveButton("মুছুন", (d, w) -> {
                                PlaylistManager.getInstance(this).deletePlaylist(p.getId());
                                loadSavedPlaylists(); // রিফ্রেশ
                                Toast.makeText(this, "মুছে গেছে", Toast.LENGTH_SHORT).show();
                            })
                            .setNegativeButton("বাতিল", null).show()
            );

            llSavedPlaylists.addView(item);
        }
    }

    // ── Playlist Edit Dialog ──
    private void showEditDialog(Playlist playlist) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_playlist, null);
        EditText etName = view.findViewById(R.id.etPlaylistName);
        EditText etUrl  = view.findViewById(R.id.etPlaylistUrl);
        etName.setText(playlist.getName());
        etUrl.setText(playlist.getUrl());

        new AlertDialog.Builder(this, R.style.DarkDialog)
                .setTitle("✏️ Edit Playlist")
                .setView(view)
                .setPositiveButton("সেভ করুন", (d, w) -> {
                    String name = etName.getText().toString().trim();
                    String url  = etUrl.getText().toString().trim();
                    if (name.isEmpty() || !url.startsWith("http")) {
                        Toast.makeText(this, "সঠিক তথ্য দিন", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    playlist.setName(name);
                    playlist.setUrl(url);
                    PlaylistManager.getInstance(this).savePlaylist(playlist);
                    // Cache clear করো যাতে নতুন URL লোড হয়
                    PlaylistManager.getInstance(this).saveChannelsForPlaylist(playlist.getId(), new java.util.ArrayList<>());
                    loadSavedPlaylists();
                    Toast.makeText(this, "✅ আপডেট হয়েছে", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("বাতিল", null).show();
    }

    // ── PIN পরিবর্তন ──
    private void showChangePinDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_pin, null);
        EditText etPin = view.findViewById(R.id.etPin);
        new AlertDialog.Builder(this, R.style.DarkDialog)
                .setTitle("🔒 নতুন PIN দিন (৪ সংখ্যা)")
                .setView(view)
                .setPositiveButton("সেট করুন", (d, w) -> {
                    String pin = etPin.getText() != null ? etPin.getText().toString().trim() : "";
                    if (pin.length() < 4) { Toast.makeText(this,"কমপক্ষে ৪ সংখ্যা দিন",Toast.LENGTH_SHORT).show(); return; }
                    getSharedPreferences(PREFS, MODE_PRIVATE).edit().putString(KEY_PIN, pin).apply();
                    Toast.makeText(this,"✅ PIN পরিবর্তন হয়েছে: " + pin, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("বাতিল", null).show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    public static String getCustomUrl(Context ctx) {
        return ctx.getSharedPreferences("bdtv_prefs", Context.MODE_PRIVATE)
                .getString("custom_m3u_url", "");
    }
}