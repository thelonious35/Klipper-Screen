package com.eren.klipperscreenremote;

import android.app.*;
import android.os.*;
import android.content.*;
import android.content.pm.ActivityInfo;
import android.graphics.Color;
import android.net.Uri;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import java.util.*;

public class MainActivity extends Activity {

    private WebView web;
    private Button settingsBtn;
    private SharedPreferences prefs;
    private Handler handler = new Handler(Looper.getMainLooper());
    private boolean autoReconnect = true;
    private int scalePercent = 100;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("ksr", MODE_PRIVATE);
        applyOrientation();
        applyWindowPrefs();

        FrameLayout root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        web = new WebView(this);
        FrameLayout.LayoutParams webLp = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT);
        root.addView(web, webLp);

        settingsBtn = new Button(this);
        settingsBtn.setText("⚙");
        settingsBtn.setTextSize(22);
        settingsBtn.setTextColor(Color.WHITE);
        settingsBtn.setBackgroundColor(0x66000000);
        FrameLayout.LayoutParams btnLp = new FrameLayout.LayoutParams(68, 68, Gravity.TOP | Gravity.END);
        btnLp.setMargins(0, 18, 18, 0);
        root.addView(settingsBtn, btnLp);

        setContentView(root);

        setupWebView();

        settingsBtn.setOnClickListener(v -> showSettings());
        settingsBtn.setOnLongClickListener(v -> {
            toggleSettingsButton();
            return true;
        });

        loadKlipperScreen();
    }

    private void setupWebView() {
        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setUseWideViewPort(true);
        s.setLoadWithOverviewMode(true);
        s.setBuiltInZoomControls(false);
        s.setDisplayZoomControls(false);
        s.setSupportZoom(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        s.setCacheMode(WebSettings.LOAD_NO_CACHE);

        WebView.setWebContentsDebuggingEnabled(false);

        web.setWebChromeClient(new WebChromeClient());

        web.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageFinished(WebView view, String url) {
                applyScale();
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                if (request.isForMainFrame()) scheduleReconnect();
            }

            @Override
            public void onReceivedHttpError(WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
                if (request.isForMainFrame()) scheduleReconnect();
            }
        });
    }

    private String makeUrl() {
        String host = prefs.getString("host", "klipper.local");
        String port = prefs.getString("port", "6080");
        String path = prefs.getString("path", "/vnc.html");
        String password = prefs.getString("password", "");
        String resize = prefs.getString("resize", "scale");

        if (!path.startsWith("/")) path = "/" + path;

        StringBuilder u = new StringBuilder();
        u.append("http://").append(host);
        if (!port.trim().isEmpty()) u.append(":").append(port.trim());
        u.append(path);
        u.append("?autoconnect=true");
        u.append("&resize=").append(Uri.encode(resize));
        u.append("&quality=9&compression=2");
        u.append("&view_only=false");
        if (!password.isEmpty()) {
            u.append("&password=").append(Uri.encode(password));
        }
        return u.toString();
    }

    private void loadKlipperScreen() {
        autoReconnect = prefs.getBoolean("auto_reconnect", true);
        scalePercent = prefs.getInt("scale", 100);
        web.loadUrl(makeUrl());
    }

    private void scheduleReconnect() {
        if (!autoReconnect) return;
        handler.removeCallbacksAndMessages(null);
        handler.postDelayed(() -> web.loadUrl(makeUrl()), 2500);
    }

    private void applyScale() {
        scalePercent = prefs.getInt("scale", 100);
        web.setInitialScale(scalePercent);
        String js = "(function(){document.documentElement.style.zoom='" + (scalePercent / 100.0) + "';})();";
        web.evaluateJavascript(js, null);
    }

    private void applyWindowPrefs() {
        boolean keepOn = prefs.getBoolean("keep_awake", true);
        if (keepOn) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        boolean fullscreen = prefs.getBoolean("fullscreen", true);
        boolean immersive = prefs.getBoolean("immersive", true);

        if (fullscreen) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            getWindow().clearFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        }

        if (immersive) {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY |
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN |
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            );
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_VISIBLE);
        }
    }

    private void applyOrientation() {
        String o = prefs == null ? "auto" : prefs.getString("orientation", "landscape");
        if ("portrait".equals(o)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        } else if ("landscape".equals(o)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        } else {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        }
    }

    private void toggleSettingsButton() {
        settingsBtn.setVisibility(settingsBtn.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
    }

    private void showSettings() {
        ScrollView sc = new ScrollView(this);
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        int pad = dp(18);
        box.setPadding(pad, pad, pad, pad);
        sc.addView(box);

        EditText host = edit("Pi adresi", prefs.getString("host", "klipper.local"));
        EditText port = edit("noVNC portu", prefs.getString("port", "6080"));
        EditText path = edit("noVNC yolu", prefs.getString("path", "/vnc.html"));
        EditText pass = edit("VNC şifresi (opsiyonel)", prefs.getString("password", ""));
        pass.setInputType(0x00000081);

        box.addView(label("Bağlantı"));
        box.addView(host);
        box.addView(port);
        box.addView(path);
        box.addView(pass);

        box.addView(label("Ekran yönü"));
        Spinner orientation = spinner(new String[]{"Yatay", "Dikey", "Otomatik"});
        String oo = prefs.getString("orientation", "landscape");
        orientation.setSelection("portrait".equals(oo) ? 1 : ("auto".equals(oo) ? 2 : 0));
        box.addView(orientation);

        box.addView(label("noVNC ölçekleme"));
        Spinner resize = spinner(new String[]{"scale", "remote", "off"});
        String rz = prefs.getString("resize", "scale");
        resize.setSelection("remote".equals(rz) ? 1 : ("off".equals(rz) ? 2 : 0));
        box.addView(resize);

        TextView scaleLabel = label("Uygulama ölçeği: " + prefs.getInt("scale", 100) + "%");
        SeekBar scale = new SeekBar(this);
        scale.setMax(100);
        scale.setProgress(prefs.getInt("scale", 100) - 50);
        scale.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                scaleLabel.setText("Uygulama ölçeği: " + (progress + 50) + "%");
            }
            public void onStartTrackingTouch(SeekBar seekBar) {}
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        box.addView(scaleLabel);
        box.addView(scale);

        CheckBox fullscreen = check("Tam ekran", prefs.getBoolean("fullscreen", true));
        CheckBox immersive = check("Gezinme çubuğunu gizle (immersive)", prefs.getBoolean("immersive", true));
        CheckBox keepAwake = check("Ekranı açık tut", prefs.getBoolean("keep_awake", true));
        CheckBox reconnect = check("Bağlantı koparsa otomatik yeniden bağlan", prefs.getBoolean("auto_reconnect", true));

        box.addView(fullscreen);
        box.addView(immersive);
        box.addView(keepAwake);
        box.addView(reconnect);

        new AlertDialog.Builder(this)
                .setTitle("Klipper Screen Ayarları")
                .setView(sc)
                .setNegativeButton("İptal", null)
                .setPositiveButton("Kaydet ve bağlan", (d, w) -> {
                    String orientValue = orientation.getSelectedItemPosition() == 1 ? "portrait" :
                            (orientation.getSelectedItemPosition() == 2 ? "auto" : "landscape");

                    prefs.edit()
                            .putString("host", host.getText().toString().trim())
                            .putString("port", port.getText().toString().trim())
                            .putString("path", path.getText().toString().trim())
                            .putString("password", pass.getText().toString())
                            .putString("orientation", orientValue)
                            .putString("resize", resize.getSelectedItem().toString())
                            .putInt("scale", scale.getProgress() + 50)
                            .putBoolean("fullscreen", fullscreen.isChecked())
                            .putBoolean("immersive", immersive.isChecked())
                            .putBoolean("keep_awake", keepAwake.isChecked())
                            .putBoolean("auto_reconnect", reconnect.isChecked())
                            .apply();

                    applyWindowPrefs();
                    applyOrientation();
                    loadKlipperScreen();
                }).show();
    }

    private EditText edit(String hint, String value) {
        EditText e = new EditText(this);
        e.setHint(hint);
        e.setText(value);
        e.setSingleLine(true);
        e.setTextColor(Color.WHITE);
        e.setHintTextColor(0xFFAAAAAA);
        return e;
    }

    private TextView label(String text) {
        TextView t = new TextView(this);
        t.setText(text);
        t.setTextColor(Color.WHITE);
        t.setTextSize(16);
        t.setPadding(0, dp(12), 0, dp(4));
        return t;
    }

    private Spinner spinner(String[] values) {
        Spinner s = new Spinner(this);
        ArrayAdapter<String> a = new ArrayAdapter<>(this,
                android.R.layout.simple_spinner_dropdown_item, values);
        s.setAdapter(a);
        return s;
    }

    private CheckBox check(String text, boolean checked) {
        CheckBox c = new CheckBox(this);
        c.setText(text);
        c.setTextColor(Color.WHITE);
        c.setChecked(checked);
        return c;
    }

    private int dp(int n) {
        return (int)(n * getResources().getDisplayMetrics().density);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else showSettings();
    }
}
