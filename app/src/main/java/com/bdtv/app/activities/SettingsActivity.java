package com.bdtv.app.activities;

import android.content.ClipboardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import com.bdtv.app.R;

public class SettingsActivity extends AppCompatActivity {

    private static final String PREFS = "bdtv_prefs";
    private static final String KEY_CUSTOM_URL = "custom_m3u_url";

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

        etCustomUrl = findViewById(R.id.etCustomUrl);
        Button btnSave   = findViewById(R.id.btnSaveUrl);
        Button btnPaste  = findViewById(R.id.btnPasteUrl);
        Button btnClear  = findViewById(R.id.btnClearUrl);
        TextView tvHint  = findViewById(R.id.tvHint);

        // বিশ্বকাপ চ্যানেল দেখার টিপস
        tvHint.setText(
                "💡 বিশ্বকাপ ২০২৬ দেখার উপায়:\n\n" +
                        "• Toffee অ্যাপ (Banglalink) — সব ম্যাচ\n" +
                        "• Bioscope অ্যাপ (Grameenphone) — সব ম্যাচ\n" +
                        "• T Sports YouTube চ্যানেল\n\n" +
                        "অথবা নিচে M3U লিংক পেস্ট করুন। " +
                        "Telegram গ্রুপ বা iptv-org থেকে লিংক পেয়ে এখানে যোগ করুন।"
        );

        // সেভ করা URL লোড
        SharedPreferences prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        String saved = prefs.getString(KEY_CUSTOM_URL, "");
        if (!saved.isEmpty()) etCustomUrl.setText(saved);

        btnPaste.setOnClickListener(v -> {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip() != null) {
                CharSequence text = cm.getPrimaryClip().getItemAt(0).getText();
                if (text != null) etCustomUrl.setText(text.toString().trim());
            }
        });

        btnSave.setOnClickListener(v -> {
            String url = etCustomUrl.getText().toString().trim();
            if (url.isEmpty()) {
                Toast.makeText(this, "URL খালি রাখা যাবে না", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!url.startsWith("http")) {
                Toast.makeText(this, "সঠিক URL দিন (http দিয়ে শুরু)", Toast.LENGTH_SHORT).show();
                return;
            }
            prefs.edit().putString(KEY_CUSTOM_URL, url).apply();
            // cache clear করো যাতে নতুন URL থেকে লোড হয়
            prefs.edit().remove("cached_channels").remove("last_update").apply();
            Toast.makeText(this, "✅ সেভ হয়েছে! অ্যাপ রিস্টার্ট করুন", Toast.LENGTH_LONG).show();
            finish();
        });

        btnClear.setOnClickListener(v -> {
            etCustomUrl.setText("");
            prefs.edit().remove(KEY_CUSTOM_URL).apply();
            prefs.edit().remove("cached_channels").remove("last_update").apply();
            Toast.makeText(this, "URL মুছে গেছে", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) { finish(); return true; }
        return super.onOptionsItemSelected(item);
    }

    // static helper — অন্য জায়গা থেকে URL পড়তে
    public static String getCustomUrl(Context ctx) {
        return ctx.getSharedPreferences("bdtv_prefs", Context.MODE_PRIVATE)
                .getString("custom_m3u_url", "");
    }
}