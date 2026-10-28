package com.example.customkeyboard;

import android.widget.ListView;
import android.widget.GridLayout;
import android.widget.ArrayAdapter;
import android.widget.Toast;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.widget.GridView;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ImageButton;
import android.widget.AdapterView;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.Manifest;
import android.app.ActivityManager;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.camera2.CameraManager;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.KeyboardView;
import android.location.GnssStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.media.MediaPlayer;
import android.media.ToneGenerator;
import android.media.audiofx.Visualizer;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.TrafficStats;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Base64;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.HapticFeedbackConstants;
import android.view.KeyEvent;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyCustomKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private LinearLayout notificationPanel;
    private LinearLayout launcherPanel;
    private TextView alertBanner;
    private int lastUnreadCount = 0;
    private TextView notificationText;
    private ListView musicListView;
    private WebView webView;
    private FrameLayout keyboardContainer;
    private LinearLayout mainKeyboardLayout;
    private LinearLayout keyboardKeysLayout;
    private LinearLayout miniKeyboard;
    private ValueCallback<Uri[]> mFilePathCallback;
    private LinearLayout settingsPanel;
    private float kbHeightPercent = 0.60f;
    private LinearLayout photoPanel;
    private GridView photoGrid;
    private boolean photoForWebView = false;

    private ScrollView toolsPanel;
    private LinearLayout monitorPanel, noisePanel, networkPanel, wifiPanel, devicePanel,
                         cryptoPanel, flashlightPanel, gpsPanel, cameraPanel, morsePanel,
                         musicPanel, speedTestPanel;

    private TextView tvCpu, tvRam, tvBattery, tvNet, tvUptime;
    private Handler monitorHandler = new Handler(Looper.getMainLooper());
    private long lastRx = 0, lastTx = 0, lastMonTime = 0;
    private long prevCpuTotal = 0, prevCpuIdle = 0;

    private AudioTrack audioTrack;
    private Thread noiseThread;
    private volatile boolean noisePlaying = false;
    private volatile int noiseType = 0;
    private volatile float noiseVolume = 0.6f;
    private float pinkB0, pinkB1, pinkB2, pinkB3, pinkB4, pinkB5, pinkB6;
    private float brownLast = 0, wavePhase = 0, rainEnv = 0;
    private final Random rnd = new Random();
    private Button playBtn;
    private TextView noiseStatus;

    private TextView networkText, wifiText, deviceText;
    private ScrollView networkScroll, wifiScroll, deviceScroll;

    private TextView cryptoInput, cryptoOutput;

    private Button flashBtn, strobeBtn;
    private TextView flashStatus;
    private boolean flashOn = false, strobeOn = false;
    private Handler strobeHandler;
    private CameraManager cameraManager;
    private String cameraId;

    private TextView gpsLat, gpsLon, gpsAlt, gpsSpeed, gpsSats;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private GnssStatus.Callback gnssCallback;

    private Camera camera;
    private SurfaceView cameraPreview;
    private boolean isFrontCamera = false;
    private FrameLayout cameraFrame;
    private TextView cameraStatus;

    private TextView morseInput, morseOutput;
    private boolean morsePlaying = false;

    private MediaPlayer mediaPlayer;
    private Visualizer visualizer;
    private VisualizerView visualizerView;
    private List<File> musicFiles = new ArrayList<>();
    private int currentTrack = 0;
    private TextView musicStatus;

    private TextView speedText;
    private boolean speedTesting = false;

    private ExecutorService executor = Executors.newFixedThreadPool(8);

    private boolean isRussian = true;
    private boolean miniRussian = false;
    private boolean symbolsMode = false;
    private boolean miniSymbolsMode = false;

    @Override
    public View onCreateInputView() {
        keyboardContainer = new FrameLayout(this);
        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#0F0F0F"));

        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.HORIZONTAL);
        menuLayout.setBackgroundColor(Color.parseColor("#151515"));
        menuLayout.setPadding(6, 6, 6, 6);

        addMenuButton(menuLayout, "🏠 HOME", v -> openLauncher(), "#FFFFFF");
        addMenuButton(menuLayout, "TG", v -> openWeb("https://web.telegram.org"), "#229ED9");
        addMenuButton(menuLayout, "YT", v -> openWeb("https://m.youtube.com"), "#FF5555");
        addMenuButton(menuLayout, "HACK", v -> openTools(), "#44DD88");
        addMenuButton(menuLayout, "✕", v -> closeAll(), "#FF4444");

        rootLayout.addView(menuLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100));

        FrameLayout contentArea = new FrameLayout(this);
        mainKeyboardLayout = new LinearLayout(this);
        mainKeyboardLayout.setOrientation(LinearLayout.VERTICAL);
        keyboardKeysLayout = new LinearLayout(this);
        keyboardKeysLayout.setOrientation(LinearLayout.VERTICAL);
        keyboardKeysLayout.setPadding(4, 6, 4, 6);
        mainKeyboardLayout.addView(keyboardKeysLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentArea.addView(mainKeyboardLayout, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int screenH = getResources().getDisplayMetrics().heightPixels;
        SharedPreferences prefs = getSharedPreferences("kb_prefs", MODE_PRIVATE);
        kbHeightPercent = prefs.getFloat("height", 0.60f);
        int panelH = (int)(screenH * kbHeightPercent);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
                       webView.setWebViewClient(new WebViewClient() {
            // Уведомления
            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                if (url.contains("telegram.org")) {
                    view.evaluateJavascript(
                        "setInterval(function() {" +
                        "  var title = document.title;" +
                        "  var match = title.match(/^\\((\\d+)\\)/);" +
                        "  if (match) {" +
                        "    var count = match[1];" +
                        "    if (window.lastCount !== count) {" +
                        "      window.lastCount = count;" +
                        "      AndroidBridge.onNewMessage('Telegram Web', count);" +
                        "    }" +
                        "  } else {" +
                        "    window.lastCount = '0';" +
                        "  }" +
                        "}, 3000);", null
                    );
                }
            }

            // AdBlock (находится ВНУТРИ того же класса WebViewClient)
            @Override
            public android.webkit.WebResourceResponse shouldInterceptRequest(WebView view, android.webkit.WebResourceRequest request) {
                String url = request.getUrl().toString();
                if (url.contains("doubleclick.net") || url.contains("googleads") || 
                    url.contains("googlesyndication") || url.contains("adservice") || 
                    url.contains("banner") || url.contains("ad.") || 
                    url.contains("analytics") || url.contains("facebook.com/tr")) {
                    return new android.webkit.WebResourceResponse("text/plain", "UTF-8", new java.io.ByteArrayInputStream("".getBytes()));
                }
                return super.shouldInterceptRequest(view, request);
            }
        }); // <--- ЗДЕСЬ ЗАКРЫВАЕТСЯ КЛАСС (и ставится точка с запятой)
                webView.setWebChromeClient(new WebChromeClient() {
            @Override
            public boolean onShowFileChooser(WebView view, ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
                if (mFilePathCallback != null) mFilePathCallback.onReceiveValue(null);
                mFilePathCallback = filePathCallback;
                photoForWebView = true;
                openPhotoGallery();
                return true;
            }
        });
                // Включаем JavaScript мост
        webView.getSettings().setJavaScriptEnabled(true);
        webView.addJavascriptInterface(new WebAppInterface(this), "AndroidBridge");

        // Создаем красную плашку для уведомлений
        alertBanner = new TextView(this);
        alertBanner.setTextColor(Color.WHITE);
        alertBanner.setBackgroundColor(Color.RED);
        alertBanner.setTextSize(14);
        alertBanner.setGravity(Gravity.CENTER);
        alertBanner.setPadding(10, 10, 10, 10);
        alertBanner.setVisibility(View.GONE);
        FrameLayout.LayoutParams bannerParams = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 80, Gravity.TOP);
        keyboardContainer.addView(alertBanner, bannerParams);
        webView.setBackgroundColor(Color.WHITE);
        webView.setVisibility(View.GONE);
        contentArea.addView(webView, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        toolsPanel = buildToolsPanel();
        toolsPanel.setVisibility(View.GONE);
        contentArea.addView(toolsPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelH));
        settingsPanel = buildSettingsPanel();
        settingsPanel.setVisibility(View.GONE);
        contentArea.addView(settingsPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        monitorPanel = buildMonitorPanel();
        monitorPanel.setVisibility(View.GONE);
        contentArea.addView(monitorPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        noisePanel = buildNoisePanel();
        noisePanel.setVisibility(View.GONE);
        contentArea.addView(noisePanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        networkPanel = buildScrollPanel("◤ NETWORK ◢", "#44DD88", v -> openTools());
        networkScroll = (ScrollView) networkPanel.getChildAt(1);
        networkText = (TextView) networkScroll.getChildAt(0);
        networkPanel.setVisibility(View.GONE);
        contentArea.addView(networkPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        wifiPanel = buildScrollPanel("◤ WIFI SCAN ◢", "#44AAFF", v -> openTools());
        wifiScroll = (ScrollView) wifiPanel.getChildAt(1);
        wifiText = (TextView) wifiScroll.getChildAt(0);
        wifiPanel.setVisibility(View.GONE);
        contentArea.addView(wifiPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        devicePanel = buildScrollPanel("◤ DEVICE ◢", "#FFAA44", v -> openTools());
        deviceScroll = (ScrollView) devicePanel.getChildAt(1);
        deviceText = (TextView) deviceScroll.getChildAt(0);
        devicePanel.setVisibility(View.GONE);
        contentArea.addView(devicePanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        cryptoPanel = buildCryptoPanel();
        cryptoPanel.setVisibility(View.GONE);
        contentArea.addView(cryptoPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        flashlightPanel = buildFlashlightPanel();
        flashlightPanel.setVisibility(View.GONE);
        contentArea.addView(flashlightPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        gpsPanel = buildGpsPanel();
        gpsPanel.setVisibility(View.GONE);
        contentArea.addView(gpsPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        cameraPanel = buildCameraPanel();
        cameraPanel.setVisibility(View.GONE);
        contentArea.addView(cameraPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        morsePanel = buildMorsePanel();
        morsePanel.setVisibility(View.GONE);
        contentArea.addView(morsePanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        musicPanel = buildMusicPanel();
        musicPanel.setVisibility(View.GONE);
        contentArea.addView(musicPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        speedTestPanel = buildSpeedTestPanel();
        speedTestPanel.setVisibility(View.GONE);
        contentArea.addView(speedTestPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, panelH));

                // Добавляем область контента в корневой лайаут
        rootLayout.addView(contentArea, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Создаем мини-клавиатуру
        miniKeyboard = buildMiniKeyboard();
        miniKeyboard.setVisibility(View.GONE);

        // ВАЖНО: добавляем мини-клавиатуру НАПРЯМУЮ в keyboardContainer (а не в rootLayout),
        // прижимая её к низу (Gravity.BOTTOM). Теперь она будет поверх контента.
        FrameLayout.LayoutParams miniParams = new FrameLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 
                ViewGroup.LayoutParams.WRAP_CONTENT, 
                Gravity.BOTTOM
        );
        keyboardContainer.addView(miniKeyboard, miniParams);

        // Добавляем основной лайаут в контейнер
        keyboardContainer.addView(rootLayout);
        buildKeyboard();
        return keyboardContainer;
    }

    private ScrollView buildToolsPanel() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setBackgroundColor(Color.parseColor("#0A0A0A"));
        p.setPadding(20, 20, 20, 20);
        TextView title = new TextView(this);
        title.setText("◤ TOOLS ◢"); title.setTextColor(Color.parseColor("#44DD88")); title.setTextSize(18); title.setTypeface(Typeface.MONOSPACE); title.setPadding(0, 0, 0, 18);
        p.addView(title);

        p.addView(makeToolTile("📊  SYSTEM MONITOR", "CPU, RAM, батарея, сеть", "#44DD88", v -> openMonitor()));
        p.addView(makeToolTile("🌊  ШУМ", "Белый / розовый / дождь", "#44AAFF", v -> openNoise()));
        p.addView(makeToolTile("📡  NETWORK", "Пинг, IP, GeoIP, DNS", "#44DD88", v -> openNetwork()));
        p.addView(makeToolTile("📶  WIFI", "Все сети + MAC", "#44AAFF", v -> openWifi()));
        p.addView(makeToolTile("📱  DEVICE", "Батарея и система", "#FFAA44", v -> openDevice()));
        p.addView(makeToolTile("🔐  ШИФР", "Base64 / ROT13 / XOR", "#AA88FF", v -> openCrypto()));
        p.addView(makeToolTile("🔦  ФОНАРИК", "Свет + стробоскоп", "#FFDD44", v -> openFlashlight()));
        p.addView(makeToolTile("🛰️  GPS", "Координаты, спутники", "#FF66AA", v -> openGps()));
        p.addView(makeToolTile("👁️  КАМЕРА", "Скрытая съемка", "#FF4444", v -> openCamera()));
        p.addView(makeToolTile("💻  МОРЗЕ", "Мигание фонариком", "#00FFFF", v -> openMorse()));
        p.addView(makeToolTile("🎵  МУЗЫКА", "Плеер + визуализатор", "#FF00FF", v -> openMusic()));
        p.addView(makeToolTile("🚀  SPEED TEST", "Скорость интернета", "#00FF00", v -> openSpeedTest()));
        p.addView(makeToolTile("🛠  НАСТРОЙКИ КЛАВЫ", "Размер клавиатуры", "#FFFFFF", v -> openSettings()));
        p.addView(makeToolTile("🔄  ПОВОРОТ ЭКРАНА", "Открыть настройки экрана", "#FFAA44", v -> rotateScreen()));
        p.addView(makeToolTile("⚙️  НАСТРОЙКИ", "Системные настройки", "#AAAAAA", v -> {
            try { startActivity(new android.content.Intent(Settings.ACTION_SETTINGS).addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)); } catch (Exception e) {}
        }));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0A0A0A"));
        scroll.addView(p);
        return scroll;
    }

    private LinearLayout makeToolTile(String title, String desc, String color, View.OnClickListener l) {
        LinearLayout tile = new LinearLayout(this); tile.setOrientation(LinearLayout.VERTICAL); tile.setPadding(20, 12, 20, 12);
        GradientDrawable bg = new GradientDrawable(); bg.setCornerRadius(14); bg.setColor(Color.parseColor("#151515")); bg.setStroke(2, Color.parseColor("#2A3A2A")); tile.setBackground(bg); tile.setOnClickListener(l);
        TextView t = new TextView(this); t.setText(title); t.setTextColor(Color.parseColor(color)); t.setTextSize(14); t.setTypeface(Typeface.MONOSPACE); tile.addView(t);
        TextView d = new TextView(this); d.setText(desc); d.setTextColor(Color.parseColor("#668866")); d.setTextSize(10); d.setPadding(0, 3, 0, 0); tile.addView(d);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT); lp.setMargins(0, 0, 0, 10); tile.setLayoutParams(lp);
        return tile;
    }

    private void openSettings() { hideAll(); settingsPanel.setVisibility(View.VISIBLE); }
    private void openTools() { hideAll(); toolsPanel.setVisibility(View.VISIBLE); }
        private void openLauncher() {
        hideAll();
        if (launcherPanel == null) {
            launcherPanel = new LinearLayout(this);
            launcherPanel.setOrientation(LinearLayout.VERTICAL);
            launcherPanel.setBackgroundColor(Color.parseColor("#0F0F0F"));
            launcherPanel.setPadding(20, 20, 20, 20);

            TextView title = new TextView(this);
            title.setText("◤ МОЯ ОС ◢");
            title.setTextColor(Color.WHITE);
            title.setTextSize(18);
            title.setTypeface(Typeface.MONOSPACE);
            launcherPanel.addView(title);

            GridLayout grid = new GridLayout(this);
            grid.setColumnCount(3);
            grid.setRowCount(4);
            
            String[][] apps = {
                {"YT", "https://m.youtube.com", "#FF5555"},
                {"TG", "https://web.telegram.org", "#229ED9"},
                {"Discord", "https://discord.com/app", "#5865F2"},
                {"Spotify", "https://open.spotify.com", "#1DB954"},
                {"Twitch", "https://m.twitch.tv", "#9146FF"},
                {"Reddit", "https://www.reddit.com", "#FF4500"},
                {"X", "https://x.com", "#FFFFFF"},
                {"WA", "https://web.whatsapp.com", "#25D366"}
            };

            for (String[] app : apps) {
                Button btn = new Button(this);
                btn.setText(app[0]);
                btn.setTextColor(Color.parseColor(app[2]));
                btn.setTextSize(16);
                btn.setAllCaps(false);
                GradientDrawable bg = new GradientDrawable();
                bg.setCornerRadius(16);
                bg.setColor(Color.parseColor("#1A1A1A"));
                bg.setStroke(2, Color.parseColor("#333333"));
                btn.setBackground(bg);
                btn.setOnClickListener(v -> openWeb(app[1]));
                
                GridLayout.LayoutParams lp = new GridLayout.LayoutParams();
                lp.width = 0;
                lp.height = 180;
                lp.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
                lp.setMargins(10, 10, 10, 10);
                btn.setLayoutParams(lp);
                grid.addView(btn);
            }
            launcherPanel.addView(grid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
            
                     FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    ViewGroup.LayoutParams.MATCH_PARENT, 
                    Gravity.TOP // <--- ВОТ ЭТО ДОБАВЛЯЕТСЯ
            );
            keyboardContainer.addView(launcherPanel, lp);
        }
        launcherPanel.setVisibility(View.VISIBLE);
        launcherPanel.bringToFront();
    }
    private void rotateScreen() {
        try {
            startActivity(new android.content.Intent(android.provider.Settings.ACTION_DISPLAY_SETTINGS)
                    .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
        } catch (Exception e) {
            try {
                startActivity(new android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
                        .addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK));
            } catch (Exception e2) {}
        }
    }

    private LinearLayout buildScrollPanel(String titleText, String color, View.OnClickListener back) {
        LinearLayout wrapper = new LinearLayout(this); wrapper.setOrientation(LinearLayout.VERTICAL); wrapper.setBackgroundColor(Color.parseColor("#0A0A0A")); wrapper.setPadding(14, 14, 14, 14);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this); title.setText(titleText); title.setTextColor(Color.parseColor(color)); title.setTextSize(14); title.setTypeface(Typeface.MONOSPACE); title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title); top.addView(makeSmallBtn("←", color, back));
        wrapper.addView(top);
        ScrollView scroll = new ScrollView(this); scroll.setBackgroundColor(Color.parseColor("#0A0A0A")); scroll.setPadding(0, 12, 0, 0);
        TextView text = new TextView(this); text.setTextColor(Color.parseColor("#44DD88")); text.setTextSize(12); text.setTypeface(Typeface.MONOSPACE); text.setText("...");
        scroll.addView(text); wrapper.addView(scroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return wrapper;
    }

    private Button makeSmallBtn(String text, String color, View.OnClickListener l) {
        Button b = new Button(this); b.setText(text); b.setTextColor(Color.parseColor(color)); b.setTextSize(16); b.setPadding(0, 0, 0, 0); b.setAllCaps(false);
        GradientDrawable s = new GradientDrawable(); s.setCornerRadius(10); s.setColor(Color.parseColor("#152515")); s.setStroke(2, Color.parseColor("#2A4A2A")); b.setBackground(s);
        b.setOnClickListener(l); b.setLayoutParams(new LinearLayout.LayoutParams(70, 56)); return b;
    }

    private LinearLayout buildMonitorPanel() {
        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setBackgroundColor(Color.parseColor("#0A0A0A")); p.setPadding(20, 16, 20, 16);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this); title.setText("◤ SYSTEM MONITOR ◢"); title.setTextColor(Color.parseColor("#44DD88")); title.setTextSize(14); title.setTypeface(Typeface.MONOSPACE); title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title); top.addView(makeSmallBtn("←", "#44DD88", v -> openTools())); p.addView(top);
        p.addView(new View(this), new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 18));
        tvUptime = makeMonitorRow(p, "UPTIME "); tvCpu = makeMonitorRow(p, "CPU    "); tvRam = makeMonitorRow(p, "RAM    "); tvBattery = makeMonitorRow(p, "BATTERY"); tvNet = makeMonitorRow(p, "NET    ");
        return p;
    }

    private TextView makeMonitorRow(LinearLayout parent, String label) {
        LinearLayout row = new LinearLayout(this); row.setOrientation(LinearLayout.HORIZONTAL); row.setPadding(0, 9, 0, 9);
        TextView lbl = new TextView(this); lbl.setText(label + " : "); lbl.setTextColor(Color.parseColor("#556655")); lbl.setTextSize(13); lbl.setTypeface(Typeface.MONOSPACE);
        TextView val = new TextView(this); val.setText("..."); val.setTextColor(Color.parseColor("#44DD88")); val.setTextSize(13); val.setTypeface(Typeface.MONOSPACE); val.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        row.addView(lbl); row.addView(val); parent.addView(row);
        View div = new View(this); div.setBackgroundColor(Color.parseColor("#152015")); parent.addView(div, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 1));
        return val;
    }

    private void openMonitor() {
        hideAll(); monitorPanel.setVisibility(View.VISIBLE);
        lastRx = TrafficStats.getTotalRxBytes(); lastTx = TrafficStats.getTotalTxBytes(); lastMonTime = System.currentTimeMillis(); prevCpuTotal = 0; prevCpuIdle = 0;
        monitorHandler.removeCallbacksAndMessages(null); monitorHandler.post(monitorTick);
    }

    private Runnable monitorTick = new Runnable() {
        @Override public void run() {
            if (monitorPanel.getVisibility() != View.VISIBLE) return;
            long up = SystemClock.elapsedRealtime() / 1000;
            tvUptime.setText(String.format(Locale.US, "%02d:%02d:%02d", up/3600, (up%3600)/60, up%60));
            int cpu = readCpuUsage();
            tvCpu.setText(cpu >= 0 ? (cpu + " %") : ("n/a"));
            try {
                ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE); ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo(); am.getMemoryInfo(mi);
                tvRam.setText(((mi.totalMem - mi.availMem)/1024/1024) + " / " + (mi.totalMem/1024/1024) + " MB");
            } catch (Exception e) { tvRam.setText("n/a"); }
            try {
                BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
                tvBattery.setText(bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) + " %");
            } catch (Exception e) { tvBattery.setText("n/a"); }
            long nowRx = TrafficStats.getTotalRxBytes(), nowTx = TrafficStats.getTotalTxBytes(), now = System.currentTimeMillis(), dt = Math.max(1, now - lastMonTime);
            tvNet.setText(String.format(Locale.US, "↓ %.1f KB/s ↑ %.1f KB/s", (nowRx - lastRx) * 1000.0 / dt / 1024.0, (nowTx - lastTx) * 1000.0 / dt / 1024.0));
            lastRx = nowRx; lastTx = nowTx; lastMonTime = now;
            monitorHandler.postDelayed(this, 1000);
        }
    };

    private int readCpuUsage() {
        try {
            BufferedReader r = new BufferedReader(new FileReader("/proc/stat")); String line = r.readLine(); r.close();
            if (line == null) return -1; String[] p = line.trim().split("\\s+"); if (p.length < 5) return -1;
            long idle = Long.parseLong(p[4]), total = 0; for (int i = 1; i < p.length; i++) { try { total += Long.parseLong(p[i]); } catch (Exception ignored) {} }
            if (prevCpuTotal == 0) { prevCpuTotal = total; prevCpuIdle = idle; return -1; }
            long dT = total - prevCpuTotal, dI = idle - prevCpuIdle; prevCpuTotal = total; prevCpuIdle = idle;
            return dT <= 0 ? -1 : (int)(100 * (dT - dI) / dT);
        } catch (Exception e) { return -1; }
    }
           
        private void openNetwork() { hideAll(); networkPanel.setVisibility(View.VISIBLE); networkText.setText("scanning...\n"); executor.execute(this::runNetworkScanAll); }

    private void runNetworkScanAll() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== LOCAL ===\n");
        sb.append("local ip : ").append(getLocalIp() != null ? getLocalIp() : "n/a").append("\n");
        sb.append("type     : ").append(getNetworkType()).append("\n\n");
        sb.append("=== PUBLIC ===\n");
        String pubIp = httpGet("https://api.ipify.org");
        sb.append("public ip: ").append(pubIp != null ? pubIp.trim() : "failed").append("\n");
        if (pubIp != null) {
            String geo = httpGet("http://ip-api.com/json/" + pubIp.trim() + "?fields=country,city,isp,org");
            if (geo != null) sb.append("geo      : ").append(geo.trim()).append("\n");
        }
        sb.append("\n=== DNS ===\n");
        appendDns(sb, "google.com"); appendDns(sb, "youtube.com"); appendDns(sb, "t.me");
        sb.append("\n=== PING ===\n");
        appendPing(sb, "8.8.8.8"); appendPing(sb, "1.1.1.1"); appendPing(sb, "ya.ru");
        sb.append("\n[✓] done");
        final String result = sb.toString();
        monitorHandler.post(() -> networkText.setText(result));
    }

    private void appendDns(StringBuilder sb, String host) {
        try {
            InetAddress[] all = InetAddress.getAllByName(host);
            sb.append(host).append(": ");
            for (int i = 0; i < all.length && i < 3; i++) { if (i > 0) sb.append(", "); sb.append(all[i].getHostAddress()); }
            sb.append("\n");
        } catch (Exception e) { sb.append(host).append(": failed\n"); }
    }

    private void appendPing(StringBuilder sb, String host) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            long t0 = System.currentTimeMillis(); boolean ok = addr.isReachable(2000); long ms = System.currentTimeMillis() - t0;
            sb.append(host).append(": ").append(ok ? ("OK " + ms + " ms") : "FAILED").append("\n");
        } catch (Exception e) { sb.append(host).append(": FAILED\n"); }
    }

    private String getLocalIp() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> ifs = java.net.NetworkInterface.getNetworkInterfaces();
            while (ifs.hasMoreElements()) {
                java.net.NetworkInterface ni = ifs.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                java.util.Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (!a.isLoopbackAddress() && a.getHostAddress().indexOf(':') < 0) return a.getHostAddress();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    private String getNetworkType() {
        try {
            ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (nc == null) return "none";
            if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) return "wifi";
            if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) return "cellular";
            if (nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) return "ethernet";
        } catch (Exception ignored) {}
        return "unknown";
    }

    private String httpGet(String urlStr) {
        try {
            URL url = new URL(urlStr); HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(4000); c.setReadTimeout(4000);
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder(); String line;
            while ((line = r.readLine()) != null) sb.append(line); r.close(); return sb.toString();
        } catch (Exception e) { return null; }
    }

    private void openWifi() { hideAll(); wifiPanel.setVisibility(View.VISIBLE); wifiText.setText("scanning...\n"); executor.execute(this::runWifiScan); }

    private void runWifiScan() {
        StringBuilder sb = new StringBuilder();
        try {
            WifiManager wm = (WifiManager) getApplicationContext().getSystemService(Context.WIFI_SERVICE);
            WifiInfo wi = wm.getConnectionInfo();
            sb.append("=== CURRENT ===\n");
            if (wi != null && wi.getSSID() != null) {
                sb.append("ssid     : ").append(wi.getSSID().replace("\"", "")).append("\n");
                sb.append("signal   : ").append(wi.getRssi()).append(" dBm\n");
                sb.append("bssid    : ").append(wi.getBSSID()).append("\n");
            } else { sb.append("not connected\n"); }
            sb.append("\n");
            List<ScanResult> results = wm.getScanResults();
            if (results == null || results.isEmpty()) { wm.startScan(); Thread.sleep(2500); results = wm.getScanResults(); }
            if (results == null || results.isEmpty()) {
                sb.append("=== NEARBY ===\nno results. Нужно разрешение на локацию.\n");
            } else {
                sb.append("=== NEARBY (").append(results.size()).append(") ===\n");
                results.sort((a, b) -> Integer.compare(b.level, a.level));
                for (ScanResult r : results) {
                    sb.append(r.SSID.isEmpty() ? "<hidden>" : r.SSID).append("\n");
                    sb.append("  ch=").append(freqToCh(r.frequency)).append("  ").append(r.level).append("dBm  ").append(security(r.capabilities)).append("\n");
                    sb.append("  mac=").append(r.BSSID).append("\n");
                }
            }
        } catch (Exception e) { sb.append("error: ").append(e.getMessage()).append("\n"); }
        sb.append("\n[✓] done");
        final String result = sb.toString(); monitorHandler.post(() -> wifiText.setText(result));
    }

    private int freqToCh(int freq) {
        if (freq == 2484) return 14; if (freq >= 2412 && freq <= 2472) return (freq - 2407) / 5; if (freq >= 5170 && freq <= 5825) return (freq - 5000) / 5; return 0;
    }

    private String security(String caps) {
        if (caps == null) return "OPEN"; if (caps.contains("WPA3")) return "WPA3"; if (caps.contains("WPA2")) return "WPA2";
        if (caps.contains("WPA")) return "WPA"; if (caps.contains("WEP")) return "WEP"; return "OPEN";
    }

    private void openDevice() { hideAll(); devicePanel.setVisibility(View.VISIBLE); deviceText.setText(buildDeviceInfo()); }

    private String buildDeviceInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DEVICE ===\n"); sb.append("brand    : ").append(Build.MANUFACTURER).append("\n");
        sb.append("model    : ").append(Build.MODEL).append("\n"); sb.append("device   : ").append(Build.DEVICE).append("\n");
        sb.append("android  : ").append(Build.VERSION.RELEASE).append("\n"); sb.append("sdk      : ").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("kernel   : ").append(System.getProperty("os.version")).append("\n"); sb.append("arch     : ").append(System.getProperty("os.arch")).append("\n");
        sb.append("cores    : ").append(Runtime.getRuntime().availableProcessors()).append("\n\n");
        DisplayMetrics dm = getResources().getDisplayMetrics();
        sb.append("=== SCREEN ===\n"); sb.append("size     : ").append(dm.widthPixels).append("x").append(dm.heightPixels).append("\n");
        sb.append("density  : ").append(dm.densityDpi).append(" dpi\n\n");
        try {
            BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
            int level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            long chargeCounter = bm.getLongProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
            android.content.IntentFilter ifilter = new android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED);
            android.content.Intent batteryStatus = registerReceiver(null, ifilter);
            int temp = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) : 0;
            int volt = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) : 0;
            int health = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_HEALTH, 0) : 0;
            int status = batteryStatus != null ? batteryStatus.getIntExtra(BatteryManager.EXTRA_STATUS, 0) : 0;
            sb.append("=== BATTERY ===\n"); sb.append("level    : ").append(level).append(" %\n");
            sb.append("temp     : ").append(temp / 10.0).append(" °C\n"); sb.append("voltage  : ").append(volt).append(" mV\n");
            sb.append("charge   : ").append(chargeCounter / 1000).append(" mAh\n"); sb.append("health   : ").append(healthStr(health)).append("\n");
            sb.append("status   : ").append(statusStr(status)).append("\n\n");
        } catch (Exception e) { sb.append("battery: error\n"); }
        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo(); am.getMemoryInfo(mi);
        sb.append("=== MEMORY ===\n"); sb.append("total    : ").append(mi.totalMem / 1024 / 1024).append(" MB\n");
        sb.append("avail    : ").append(mi.availMem / 1024 / 1024).append(" MB\n"); sb.append("low      : ").append(mi.lowMemory ? "yes" : "no").append("\n\n");
        java.io.File dataDir = getFilesDir(); long free = dataDir.getFreeSpace() / 1024 / 1024; long total = dataDir.getTotalSpace() / 1024 / 1024;
        sb.append("=== STORAGE ===\n"); sb.append("free     : ").append(free).append(" MB\n"); sb.append("total    : ").append(total).append(" MB\n");
        return sb.toString();
    }

    private String healthStr(int h) {
        switch (h) { case BatteryManager.BATTERY_HEALTH_GOOD: return "GOOD"; case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "OVERHEAT";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "DEAD"; case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "OVER_VOLTAGE";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "FAILURE"; case BatteryManager.BATTERY_HEALTH_COLD: return "COLD"; }
        return "UNKNOWN";
    }

    private String statusStr(int s) {
        switch (s) { case BatteryManager.BATTERY_STATUS_CHARGING: return "CHARGING"; case BatteryManager.BATTERY_STATUS_DISCHARGING: return "DISCHARGING";
            case BatteryManager.BATTERY_STATUS_FULL: return "FULL"; case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "NOT CHARGING"; }
        return "UNKNOWN";
    }
        public class WebAppInterface {
        Context mContext;
        WebAppInterface(Context c) { mContext = c; }

        @android.webkit.JavascriptInterface
        public void onNewMessage(String appName, String count) {
            monitorHandler.post(() -> {
                if (alertBanner != null) {
                    alertBanner.setText("🔔 Новое сообщение в " + appName + " (" + count + ")");
                    alertBanner.setVisibility(View.VISIBLE);
                    // Прячем плашку через 5 секунд
                    monitorHandler.postDelayed(() -> alertBanner.setVisibility(View.GONE), 5000);
                }
                // Проигрываем звук
                try {
                    ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_NOTIFICATION, 100);
                    tg.startTone(ToneGenerator.TONE_PROP_BEEP, 200);
                } catch (Exception e) {}
            });
        }
    }
    
        private LinearLayout buildNoisePanel() {
        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setBackgroundColor(Color.parseColor("#0A0A0A")); p.setPadding(20, 16, 20, 16);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this); title.setText("◤ NOISE GEN ◢"); title.setTextColor(Color.parseColor("#44AAFF")); title.setTextSize(14); title.setTypeface(Typeface.MONOSPACE); title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title); top.addView(makeSmallBtn("←", "#44AAFF", v -> openTools())); p.addView(top);
        noiseStatus = new TextView(this); noiseStatus.setText("● STOPPED"); noiseStatus.setTextColor(Color.parseColor("#666666")); noiseStatus.setTextSize(13); noiseStatus.setTypeface(Typeface.MONOSPACE); noiseStatus.setPadding(0, 24, 0, 16); p.addView(noiseStatus);
        String[] types = {"БЕЛЫЙ", "РОЗОВЫЙ", "КОРИЧН.", "ДОЖДЬ", "ВОЛНЫ"}; final Button[] typeBtns = new Button[types.length];
        LinearLayout row1 = new LinearLayout(this); row1.setOrientation(LinearLayout.HORIZONTAL); LinearLayout row2 = new LinearLayout(this); row2.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < types.length; i++) {
            final int idx = i; Button b = new Button(this); b.setText(types[i]); b.setTextSize(10); b.setTextColor(Color.WHITE); b.setAllCaps(false); b.setPadding(0, 0, 0, 0);
            GradientDrawable s = new GradientDrawable(); s.setCornerRadius(10); s.setColor(Color.parseColor("#152535")); s.setStroke(2, Color.parseColor("#2A4A6A")); b.setBackground(s);
            b.setOnClickListener(v -> { noiseType = idx; for (int j = 0; j < typeBtns.length; j++) { GradientDrawable g = new GradientDrawable(); g.setCornerRadius(10); g.setColor(Color.parseColor(j == idx ? "#2A5A8A" : "#152535")); g.setStroke(2, Color.parseColor("#2A4A6A")); typeBtns[j].setBackground(g); } });
            typeBtns[i] = b; LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 76, 1f); lp.setMargins(4, 4, 4, 4); b.setLayoutParams(lp); if (i < 3) row1.addView(b); else row2.addView(b);
        }
        p.addView(row1); p.addView(row2);
        TextView volLabel = new TextView(this); volLabel.setText("ГРОМКОСТЬ"); volLabel.setTextColor(Color.parseColor("#556655")); volLabel.setTextSize(11); volLabel.setTypeface(Typeface.MONOSPACE); volLabel.setPadding(4, 20, 0, 6); p.addView(volLabel);
        SeekBar sb = new SeekBar(this); sb.setMax(100); sb.setProgress(60); sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() { public void onProgressChanged(SeekBar s, int v, boolean f) { noiseVolume = v / 100f; } public void onStartTrackingTouch(SeekBar s) {} public void onStopTrackingTouch(SeekBar s) {} }); p.addView(sb);
        playBtn = new Button(this); playBtn.setText("▶  ПУСК"); playBtn.setTextColor(Color.WHITE); playBtn.setTextSize(15); playBtn.setAllCaps(false);
        GradientDrawable pb = new GradientDrawable(); pb.setCornerRadius(14); pb.setColor(Color.parseColor("#2A5A8A")); playBtn.setBackground(pb);
        playBtn.setOnClickListener(v -> { if (noisePlaying) stopNoise(); else startNoise(); });
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 90); plp.setMargins(0, 24, 0, 0); playBtn.setLayoutParams(plp); p.addView(playBtn);
        GradientDrawable g = new GradientDrawable(); g.setCornerRadius(10); g.setColor(Color.parseColor("#2A5A8A")); g.setStroke(2, Color.parseColor("#2A4A6A")); typeBtns[0].setBackground(g);
        return p;
    }

    private void openNoise() { hideAll(); noisePanel.setVisibility(View.VISIBLE); }

    private void startNoise() {
        if (noisePlaying) return;
        try {
            int sr = 44100; int minBuf = AudioTrack.getMinBufferSize(sr, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT); if (minBuf <= 0) minBuf = 4096;
            audioTrack = new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(sr).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(minBuf * 2).setTransferMode(AudioTrack.MODE_STREAM).build();
            audioTrack.play(); noisePlaying = true;
            playBtn.setText("■  СТОП"); GradientDrawable pb = new GradientDrawable(); pb.setCornerRadius(14); pb.setColor(Color.parseColor("#8A2A2A")); playBtn.setBackground(pb);
            noiseStatus.setText("● PLAYING"); noiseStatus.setTextColor(Color.parseColor("#44AAFF"));
            noiseThread = new Thread(this::noiseLoop); noiseThread.start();
        } catch (Exception e) { noiseStatus.setText("● ERROR"); noiseStatus.setTextColor(Color.parseColor("#FF4444")); noisePlaying = false; }
    }

    private void stopNoise() {
        noisePlaying = false;
        if (noiseThread != null) { try { noiseThread.join(500); } catch (InterruptedException ignored) {} noiseThread = null; }
        if (audioTrack != null) { try { audioTrack.stop(); } catch (Exception ignored) {} try { audioTrack.release(); } catch (Exception ignored) {} audioTrack = null; }
        if (playBtn != null) { playBtn.setText("▶  ПУСК"); GradientDrawable pb = new GradientDrawable(); pb.setCornerRadius(14); pb.setColor(Color.parseColor("#2A5A8A")); playBtn.setBackground(pb); }
        if (noiseStatus != null) { noiseStatus.setText("● STOPPED"); noiseStatus.setTextColor(Color.parseColor("#666666")); }
    }

    private void noiseLoop() {
        short[] buf = new short[2048];
        while (noisePlaying) {
            for (int i = 0; i < buf.length; i++) {
                float s = generateSample() * noiseVolume; if (s > 1f) s = 1f; if (s < -1f) s = -1f; buf[i] = (short)(s * 32767);
            }
            try { audioTrack.write(buf, 0, buf.length); } catch (Exception e) { break; }
        }
    }

    private float generateSample() {
        float white = rnd.nextFloat() * 2f - 1f;
        switch (noiseType) {
            case 0: return white;
            case 1: { pinkB0 = 0.99886f * pinkB0 + white * 0.0555179f; pinkB1 = 0.99332f * pinkB1 + white * 0.0750759f; pinkB2 = 0.96900f * pinkB2 + white * 0.1538520f; pinkB3 = 0.86650f * pinkB3 + white * 0.3104856f; pinkB4 = 0.55000f * pinkB4 + white * 0.5329522f; pinkB5 = -0.7616f * pinkB5 - white * 0.0168980f; float pink = pinkB0+pinkB1+pinkB2+pinkB3+pinkB4+pinkB5+pinkB6+white*0.5362f; pinkB6 = white * 0.115926f; return pink * 0.11f; }
            case 2: { brownLast = (brownLast + 0.02f*white)/1.02f; return brownLast*3.5f; }
            case 3: { float bg = white * 0.15f; if (rnd.nextFloat() < 0.0008f) rainEnv = 1.0f; rainEnv *= 0.9985f; return bg + rainEnv * (rnd.nextFloat()*2f - 1f) * 0.7f; }
            case 4: { wavePhase += 0.00015f; if (wavePhase > 6.2831853f) wavePhase -= 6.2831853f; float mod = 0.35f + 0.65f * (float)((Math.sin(wavePhase)+1)*0.5); return white * mod; }
        }
        return white;
    }

       private LinearLayout buildMusicPanel() {
        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setBackgroundColor(Color.parseColor("#0A0A0A")); p.setPadding(20, 16, 20, 16);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this); title.setText("◤ MUSIC PLAYER ◢"); title.setTextColor(Color.parseColor("#FF00FF")); title.setTextSize(14); title.setTypeface(Typeface.MONOSPACE); title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title); top.addView(makeSmallBtn("←", "#FF00FF", v -> openTools())); p.addView(top);
        
        // Визуализатор
        visualizerView = new VisualizerView(this); 
        p.addView(visualizerView, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200));
        
        musicStatus = new TextView(this); musicStatus.setText("Нажми Play для загрузки"); musicStatus.setTextColor(Color.parseColor("#AAAAAA")); musicStatus.setGravity(Gravity.CENTER); musicStatus.setPadding(0, 10, 0, 10); p.addView(musicStatus);
        
        // СПИСОК ТРЕКОВ
        ListView trackList = new ListView(this);
        trackList.setBackgroundColor(Color.parseColor("#151515"));
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f);
        listParams.setMargins(0, 10, 0, 10);
        trackList.setLayoutParams(listParams);
        trackList.setOnItemClickListener((parent, view, position, id) -> {
            currentTrack = position;
            playTrack(0); // Играем выбранный трек
        });
        p.addView(trackList);

        // Нужно обновить адаптер, когда музыка загрузится. Сделаем это при запуске.
        this.musicListView = trackList;
        if (!musicFiles.isEmpty()) {
            updateMusicList();
        } else {
            loadMusicFiles(); // Загружаем сразу при открытии
            updateMusicList();
        }

        LinearLayout controls = new LinearLayout(this); controls.setOrientation(LinearLayout.HORIZONTAL); controls.setGravity(Gravity.CENTER);
        Button prevBtn = new Button(this); prevBtn.setText("⏮"); prevBtn.setTextSize(24); prevBtn.setOnClickListener(v -> playTrack(-1)); controls.addView(prevBtn);
        Button playBtn = new Button(this); playBtn.setText("▶"); playBtn.setTextSize(24); playBtn.setOnClickListener(v -> togglePlayMusic()); controls.addView(playBtn);
        Button nextBtn = new Button(this); nextBtn.setText("⏭"); nextBtn.setTextSize(24); nextBtn.setOnClickListener(v -> playTrack(1)); controls.addView(nextBtn);
        p.addView(controls, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100));
        return p;
    }

    private void openMusic() {
        hideAll(); musicPanel.setVisibility(View.VISIBLE);
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) musicStatus.setText("Дай разрешение на микрофон для визуализатора!");
    }

       private void loadMusicFiles() {
        musicFiles.clear();
        String[] projection = {MediaStore.Audio.Media.DATA};
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Audio.Media.TITLE + " ASC");
        if (cursor != null) {
            int col = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA);
            while (cursor.moveToNext()) {
                musicFiles.add(new File(cursor.getString(col)));
            }
            cursor.close();
        }
        if (musicFiles.isEmpty()) musicStatus.setText("Нет музыки на телефоне");
        updateMusicList();
    }
        private void updateMusicList() {
        if (musicListView == null) return;
        String[] names = new String[musicFiles.size()];
        for (int i = 0; i < musicFiles.size(); i++) {
            names[i] = musicFiles.get(i).getName();
        }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, names);
        musicListView.setAdapter(adapter);
    }
    private void togglePlayMusic() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) { mediaPlayer.pause(); musicStatus.setText("Пауза"); }
        else if (mediaPlayer != null) { mediaPlayer.start(); musicStatus.setText("Играет"); }
        else { loadMusicFiles(); if (!musicFiles.isEmpty()) playTrack(0); }
    }

    private void playTrack(int offset) {
        if (mediaPlayer != null) { mediaPlayer.release(); mediaPlayer = null; }
        if (visualizer != null) { visualizer.release(); visualizer = null; }
        if (musicFiles.isEmpty()) { loadMusicFiles(); if (musicFiles.isEmpty()) return; }
        currentTrack = (currentTrack + offset + musicFiles.size()) % musicFiles.size();
        try {
            mediaPlayer = new MediaPlayer(); mediaPlayer.setDataSource(musicFiles.get(currentTrack).getAbsolutePath());
            mediaPlayer.prepare(); mediaPlayer.start(); musicStatus.setText(musicFiles.get(currentTrack).getName());
            setupVisualizer();
        } catch (Exception e) { musicStatus.setText("Ошибка: " + e.getMessage()); }
    }

    private void setupVisualizer() {
        if (mediaPlayer == null || checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) return;
        try {
            visualizer = new Visualizer(mediaPlayer.getAudioSessionId()); visualizer.setCaptureSize(Visualizer.getCaptureSizeRange()[1]);
            visualizer.setDataCaptureListener(new Visualizer.OnDataCaptureListener() {
                @Override public void onWaveFormDataCapture(Visualizer vis, byte[] waveform, int sr) { if (visualizerView != null) visualizerView.updateVisualizer(waveform); }
                @Override public void onFftDataCapture(Visualizer vis, byte[] fft, int sr) {}
            }, Visualizer.getMaxCaptureRate() / 2, true, false);
            visualizer.setEnabled(true);
        } catch (Exception e) {}
    }

    private static class VisualizerView extends View {
        private byte[] bytes; private Paint paint = new Paint();
        public VisualizerView(Context context) { super(context); paint.setStrokeWidth(8f); paint.setAntiAlias(true); paint.setColor(Color.parseColor("#FF00FF")); }
        public void updateVisualizer(byte[] bytes) { this.bytes = bytes; invalidate(); }
        @Override protected void onDraw(Canvas canvas) {
            super.onDraw(canvas); if (bytes == null) return; int height = getHeight(), width = getWidth(), lines = 50, spacing = width / lines;
            for (int i = 0; i < lines; i++) { int val = bytes[i * (bytes.length / lines)] + 128; int h = (val * height) / 255; if (h < 10) h = 10; int x = i * spacing + spacing / 2; canvas.drawLine(x, height - h, x, height, paint); }
        }
    }

    private LinearLayout buildSpeedTestPanel() {
        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setBackgroundColor(Color.parseColor("#0A0A0A")); p.setPadding(20, 16, 20, 16);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this); title.setText("◤ SPEED TEST ◢"); title.setTextColor(Color.parseColor("#00FF00")); title.setTextSize(14); title.setTypeface(Typeface.MONOSPACE); title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title); top.addView(makeSmallBtn("←", "#00FF00", v -> openTools())); p.addView(top);
        speedText = new TextView(this); speedText.setText("Нажми 'СТАРТ'"); speedText.setTextColor(Color.WHITE); speedText.setTextSize(18); speedText.setTypeface(Typeface.MONOSPACE); speedText.setGravity(Gravity.CENTER); speedText.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)); p.addView(speedText);
        Button startBtn = new Button(this); startBtn.setText("▶ СТАРТ"); startBtn.setTextColor(Color.BLACK); startBtn.setTextSize(16); startBtn.setBackgroundColor(Color.parseColor("#00FF00")); startBtn.setOnClickListener(v -> startSpeedTest());
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100); startBtn.setLayoutParams(bp); p.addView(startBtn);
        return p;
    }

    private void openSpeedTest() { hideAll(); speedTestPanel.setVisibility(View.VISIBLE); }

    private void startSpeedTest() {
        if (speedTesting) return; speedTesting = true; speedText.setText("Тестируем...\nПодожди...");
        new Thread(() -> {
            try {
                URL url = new URL("https://speed.cloudflare.com/__down?bytes=10000000"); HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setConnectTimeout(5000); conn.setReadTimeout(10000);
                long startTime = System.currentTimeMillis(), totalBytes = 0; InputStream in = conn.getInputStream(); byte[] buffer = new byte[8192]; int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) totalBytes += bytesRead; in.close();
                long durationMs = System.currentTimeMillis() - startTime; double seconds = durationMs / 1000.0; double megabytes = totalBytes / (1024.0 * 1024.0);
                double mbps = (megabytes * 8) / seconds;
                monitorHandler.post(() -> { speedText.setText(String.format(Locale.US, "Скорость: %.2f Мбит/с\nВремя: %.1f сек", mbps, seconds)); speedTesting = false; });
            } catch (Exception e) { monitorHandler.post(() -> { speedText.setText("Ошибка: " + e.getMessage()); speedTesting = false; }); }
        }).start();
    }

    private LinearLayout buildGpsPanel() {
        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setBackgroundColor(Color.parseColor("#0A0A0A")); p.setPadding(20, 16, 20, 16);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this); title.setText("◤ GPS TRACKER ◢"); title.setTextColor(Color.parseColor("#FF66AA")); title.setTextSize(14); title.setTypeface(Typeface.MONOSPACE); title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title); top.addView(makeSmallBtn("←", "#FF66AA", v -> openTools())); p.addView(top);
        gpsLat = makeMonitorRow(p, "LAT    "); gpsLon = makeMonitorRow(p, "LON    "); gpsAlt = makeMonitorRow(p, "ALT    "); gpsSpeed = makeMonitorRow(p, "SPEED  "); gpsSats = makeMonitorRow(p, "SATS   ");
        return p;
    }

    private void openGps() {
        hideAll(); gpsPanel.setVisibility(View.VISIBLE);
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) { gpsLat.setText("НЕТ РАЗРЕШЕНИЯ"); return; }
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        locationListener = new LocationListener() {
            @Override public void onLocationChanged(Location loc) {
                if (loc != null) { gpsLat.setText(String.format(Locale.US, "%.6f", loc.getLatitude())); gpsLon.setText(String.format(Locale.US, "%.6f", loc.getLongitude())); gpsAlt.setText(String.format(Locale.US, "%.1f m", loc.getAltitude())); gpsSpeed.setText(String.format(Locale.US, "%.1f km/h", loc.getSpeed() * 3.6)); }
            }
            @Override public void onStatusChanged(String provider, int status, Bundle extras) {} @Override public void onProviderEnabled(String provider) {} @Override public void onProviderDisabled(String provider) {}
        };
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, locationListener);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                GnssStatus.Callback callback = new GnssStatus.Callback() { @Override public void onSatelliteStatusChanged(GnssStatus status) { gpsSats.setText(String.valueOf(status.getSatelliteCount())); } };
                locationManager.registerGnssStatusCallback(callback, null); gnssCallback = callback;
            }
        } catch (Exception e) {}
    }

    private void stopGps() {
        if (locationManager != null && locationListener != null) { locationManager.removeUpdates(locationListener); locationListener = null; }
        if (locationManager != null && gnssCallback != null) { locationManager.unregisterGnssStatusCallback(gnssCallback); gnssCallback = null; }
    }

    private LinearLayout buildCameraPanel() {
        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setBackgroundColor(Color.parseColor("#000000")); p.setPadding(10, 10, 10, 10);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this); title.setText("◤ SPY CAM ◢"); title.setTextColor(Color.parseColor("#FF4444")); title.setTextSize(14); title.setTypeface(Typeface.MONOSPACE); title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title); top.addView(makeSmallBtn("←", "#FF4444", v -> openTools())); p.addView(top);
        cameraFrame = new FrameLayout(this); cameraPreview = new SurfaceView(this);
        cameraFrame.addView(cameraPreview, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        cameraStatus = new TextView(this); cameraStatus.setText("Нажми для старта"); cameraStatus.setTextColor(Color.WHITE); cameraStatus.setGravity(Gravity.CENTER);
        cameraFrame.addView(cameraStatus, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        p.addView(cameraFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout btns = new LinearLayout(this); btns.setOrientation(LinearLayout.HORIZONTAL);
        Button snapBtn = new Button(this); snapBtn.setText("📸 ФОТО"); snapBtn.setOnClickListener(v -> takePicture()); btns.addView(snapBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        Button switchBtn = new Button(this); switchBtn.setText("🔄 СМЕНИТЬ"); switchBtn.setOnClickListener(v -> { isFrontCamera = !isFrontCamera; openCamera(); }); btns.addView(switchBtn, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        p.addView(btns);
        return p;
    }

    private void openCamera() {
        hideAll(); cameraPanel.setVisibility(View.VISIBLE); cameraStatus.setText("Инициализация...");
        try {
            if (camera != null) { camera.stopPreview(); camera.release(); camera = null; }
            int camId = isFrontCamera ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
            camera = Camera.open(camId); camera.setPreviewDisplay(cameraPreview.getHolder()); camera.startPreview(); cameraStatus.setText("");
        } catch (Exception e) { cameraStatus.setText("Ошибка камеры: " + e.getMessage()); }
    }

    private void takePicture() {
        if (camera == null) return;
        camera.takePicture(null, null, (data, cam) -> {
            try {
                File dir = new File(getExternalFilesDir(null), "SpyPics"); if (!dir.exists()) dir.mkdirs();
                File file = new File(dir, "img_" + System.currentTimeMillis() + ".jpg");
                FileOutputStream fos = new FileOutputStream(file); fos.write(data); fos.close();
                cameraStatus.post(() -> cameraStatus.setText("✅ " + file.getName()));
                ContentValues values = new ContentValues(); values.put(MediaStore.Images.Media.DATA, file.getAbsolutePath()); values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
                getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
            } catch (Exception e) { cameraStatus.post(() -> cameraStatus.setText("Ошибка сохранения")); }
        });
    }

    private void stopCamera() {
        if (camera != null) { try { camera.stopPreview(); camera.release(); } catch (Exception e) {} camera = null; }
    }

    private LinearLayout buildMorsePanel() {
        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setBackgroundColor(Color.parseColor("#0A0A0A")); p.setPadding(20, 16, 20, 16);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this); title.setText("◤ MORSE ◢"); title.setTextColor(Color.parseColor("#00FFFF")); title.setTextSize(14); title.setTypeface(Typeface.MONOSPACE); title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title); top.addView(makeSmallBtn("←", "#00FFFF", v -> openTools())); p.addView(top);
        morseInput = new TextView(this); morseInput.setText("Введи текст в чате и нажми 'Взять'"); morseInput.setTextColor(Color.WHITE); morseInput.setBackgroundColor(Color.parseColor("#1A1A1A")); morseInput.setPadding(16, 12, 16, 12); morseInput.setTextSize(14); morseInput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100)); p.addView(morseInput);
        Button getBtn = new Button(this); getBtn.setText("Взять из чата"); getBtn.setOnClickListener(v -> { InputConnection ic = getCurrentInputConnection(); if (ic != null) { CharSequence text = ic.getTextBeforeCursor(500, 0); if (text != null) morseInput.setText(text.toString()); } }); p.addView(getBtn);
        morseOutput = new TextView(this); morseOutput.setTextColor(Color.parseColor("#00FFFF")); morseOutput.setTextSize(16); morseOutput.setTypeface(Typeface.MONOSPACE); morseOutput.setGravity(Gravity.CENTER); morseOutput.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f)); p.addView(morseOutput);
        Button playBtn = new Button(this); playBtn.setText("▶ ВОСПРОИЗВЕСТИ"); playBtn.setTextColor(Color.BLACK); playBtn.setBackgroundColor(Color.parseColor("#00FFFF")); playBtn.setOnClickListener(v -> playMorse(morseInput.getText().toString())); p.addView(playBtn);
        return p;
    }

    private void openMorse() { hideAll(); morsePanel.setVisibility(View.VISIBLE); }

    private void playMorse(final String text) {
        if (morsePlaying) return; morsePlaying = true;
        new Thread(() -> {
            ToneGenerator tg = new ToneGenerator(AudioManager.STREAM_MUSIC, 100); StringBuilder display = new StringBuilder();
            for (char c : text.toUpperCase().toCharArray()) {
                String code = getMorse(c); if (code == null) continue;
                display.append(c).append(": ").append(code).append("\n"); final String d = display.toString(); monitorHandler.post(() -> morseOutput.setText(d));
                for (char symbol : code.toCharArray()) {
                    if (symbol == '.') { setTorch(true); tg.startTone(ToneGenerator.TONE_DTMF_0, 100); sleep(100); setTorch(false); sleep(100); }
                    else if (symbol == '-') { setTorch(true); tg.startTone(ToneGenerator.TONE_DTMF_0, 300); sleep(300); setTorch(false); sleep(100); }
                }
                sleep(300);
            }
            tg.release(); morsePlaying = false;
        }).start();
    }

    private String getMorse(char c) {
        switch (c) {
            case 'A': return ".-"; case 'B': return "-..."; case 'C': return "-.-."; case 'D': return "-.."; case 'E': return "."; case 'F': return "..-."; case 'G': return "--."; case 'H': return "...."; case 'I': return ".."; case 'J': return ".---"; case 'K': return "-.-"; case 'L': return ".-.."; case 'M': return "--"; case 'N': return "-."; case 'O': return "---"; case 'P': return ".--."; case 'Q': return "--.-"; case 'R': return ".-."; case 'S': return "..."; case 'T': return "-"; case 'U': return "..-"; case 'V': return "...-"; case 'W': return ".--"; case 'X': return "-..-"; case 'Y': return "-.--"; case 'Z': return "--.."; case '1': return ".----"; case '2': return "..---"; case '3': return "...--"; case '4': return "....-"; case '5': return "....."; case '6': return "-...."; case '7': return "--..."; case '8': return "---.."; case '9': return "----."; case '0': return "-----"; default: return null;
        }
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (Exception e) {} }

    private LinearLayout buildCryptoPanel() {
        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setBackgroundColor(Color.parseColor("#0A0A0A")); p.setPadding(20, 16, 20, 16);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this); title.setText("◤ CRYPTO ◢"); title.setTextColor(Color.parseColor("#AA88FF")); title.setTextSize(14); title.setTypeface(Typeface.MONOSPACE); title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title); top.addView(makeSmallBtn("←", "#AA88FF", v -> openTools())); p.addView(top);
        cryptoInput = new TextView(this); cryptoInput.setText("Нажми 'Вставить'..."); cryptoInput.setTextColor(Color.WHITE); cryptoInput.setBackgroundColor(Color.parseColor("#1A1A2A")); cryptoInput.setPadding(16, 12, 16, 12); cryptoInput.setTextSize(14); cryptoInput.setTextIsSelectable(true);
        LinearLayout.LayoutParams ilp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120); ilp.setMargins(0, 16, 0, 12); cryptoInput.setLayoutParams(ilp); p.addView(cryptoInput);
        LinearLayout inputBtns = new LinearLayout(this); inputBtns.setOrientation(LinearLayout.HORIZONTAL);
        Button pasteBtn = makeCryptoBtn("📋 Вставить", "#44DD88"); pasteBtn.setOnClickListener(v -> { ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); if (cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) cryptoInput.setText(cm.getPrimaryClip().getItemAt(0).getText()); }); inputBtns.addView(pasteBtn);
        Button getFromFieldBtn = makeCryptoBtn("Взять из чата", "#44DD88"); getFromFieldBtn.setOnClickListener(v -> { InputConnection ic = getCurrentInputConnection(); if (ic != null) { CharSequence before = ic.getTextBeforeCursor(500, 0); CharSequence after = ic.getTextAfterCursor(500, 0); if (before != null || after != null) cryptoInput.setText((before != null ? before.toString() : "") + (after != null ? after.toString() : "")); } }); inputBtns.addView(getFromFieldBtn);
        p.addView(inputBtns, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 80));
        LinearLayout btns = new LinearLayout(this); btns.setOrientation(LinearLayout.HORIZONTAL);
        Button b64 = makeCryptoBtn("Base64", "#AA88FF"); b64.setOnClickListener(v -> cryptoOutput.setText(encodeBase64(cryptoInput.getText().toString()))); btns.addView(b64);
        Button rot = makeCryptoBtn("ROT13", "#AA88FF"); rot.setOnClickListener(v -> cryptoOutput.setText(encodeRot13(cryptoInput.getText().toString()))); btns.addView(rot);
        Button xor = makeCryptoBtn("XOR", "#AA88FF"); xor.setOnClickListener(v -> cryptoOutput.setText(encodeXor(cryptoInput.getText().toString()))); btns.addView(xor);
        p.addView(btns, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 80));
        TextView resLabel = new TextView(this); resLabel.setText("РЕЗУЛЬТАТ:"); resLabel.setTextColor(Color.parseColor("#556655")); resLabel.setTextSize(11); resLabel.setTypeface(Typeface.MONOSPACE); resLabel.setPadding(4, 10, 0, 6); p.addView(resLabel);
        cryptoOutput = new TextView(this); cryptoOutput.setText(""); cryptoOutput.setTextColor(Color.parseColor("#AA88FF")); cryptoOutput.setTextSize(13); cryptoOutput.setTypeface(Typeface.MONOSPACE); cryptoOutput.setBackgroundColor(Color.parseColor("#121220")); cryptoOutput.setPadding(16, 12, 16, 12); cryptoOutput.setTextIsSelectable(true);
        ScrollView outScroll = new ScrollView(this); outScroll.addView(cryptoOutput); p.addView(outScroll, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        Button copyBtn = new Button(this); copyBtn.setText("📋  Копировать результат"); copyBtn.setTextColor(Color.WHITE); copyBtn.setTextSize(14); copyBtn.setAllCaps(false);
        GradientDrawable cb = new GradientDrawable(); cb.setCornerRadius(14); cb.setColor(Color.parseColor("#3A2A5A")); copyBtn.setBackground(cb);
        copyBtn.setOnClickListener(v -> { ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE); cm.setPrimaryClip(ClipData.newPlainText("crypto", cryptoOutput.getText().toString())); copyBtn.setText("✓ Скопировано"); monitorHandler.postDelayed(() -> copyBtn.setText("📋  Копировать результат"), 1500); });
        LinearLayout.LayoutParams cblp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 80); cblp.setMargins(0, 8, 0, 0); copyBtn.setLayoutParams(cblp); p.addView(copyBtn);
        return p;
    }

    private Button makeCryptoBtn(String text, String color) {
        Button b = new Button(this); b.setText(text); b.setTextColor(Color.parseColor(color)); b.setTextSize(13); b.setAllCaps(false);
        GradientDrawable s = new GradientDrawable(); s.setCornerRadius(10); s.setColor(Color.parseColor("#1A1A2A")); s.setStroke(2, Color.parseColor("#3A2A5A")); b.setBackground(s);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1f); lp.setMargins(4, 4, 4, 4); b.setLayoutParams(lp); return b;
    }

    private void openCrypto() { hideAll(); cryptoPanel.setVisibility(View.VISIBLE); }
    private String encodeBase64(String s) { try { return Base64.encodeToString(s.getBytes("UTF-8"), Base64.NO_WRAP); } catch (Exception e) { return "error"; } }
    private String encodeRot13(String s) {
        StringBuilder sb = new StringBuilder();
        for (char c : s.toCharArray()) {
            if (c >= 'a' && c <= 'z') sb.append((char)((c - 'a' + 13) % 26 + 'a'));
            else if (c >= 'A' && c <= 'Z') sb.append((char)((c - 'A' + 13) % 26 + 'A'));
            else sb.append(c);
        }
        return sb.toString();
    }
    private String encodeXor(String s) {
        StringBuilder sb = new StringBuilder(); String key = "HACKER";
        for (int i = 0; i < s.length(); i++) sb.append(String.format("%02X", (byte)(s.charAt(i) ^ key.charAt(i % key.length()))));
        return sb.toString();
    }

    private LinearLayout buildFlashlightPanel() {
        LinearLayout p = new LinearLayout(this); p.setOrientation(LinearLayout.VERTICAL); p.setBackgroundColor(Color.parseColor("#0A0A0A")); p.setPadding(20, 16, 20, 16);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this); title.setText("◤ FLASHLIGHT ◢"); title.setTextColor(Color.parseColor("#FFDD44")); title.setTextSize(14); title.setTypeface(Typeface.MONOSPACE); title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title); top.addView(makeSmallBtn("←", "#FFDD44", v -> openTools())); p.addView(top);
        flashStatus = new TextView(this); flashStatus.setText("● ВЫКЛЮЧЕН"); flashStatus.setTextColor(Color.parseColor("#666666")); flashStatus.setTextSize(13); flashStatus.setTypeface(Typeface.MONOSPACE); flashStatus.setPadding(0, 24, 0, 24); flashStatus.setGravity(Gravity.CENTER_HORIZONTAL); p.addView(flashStatus);
        flashBtn = new Button(this); flashBtn.setText("🔦  ВКЛЮЧИТЬ"); flashBtn.setTextColor(Color.BLACK); flashBtn.setTextSize(16); flashBtn.setAllCaps(false);
        GradientDrawable fb = new GradientDrawable(); fb.setCornerRadius(16); fb.setColor(Color.parseColor("#FFDD44")); flashBtn.setBackground(fb);
        flashBtn.setOnClickListener(v -> toggleFlashlight()); LinearLayout.LayoutParams flp = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110); flp.setMargins(0, 0, 0, 16); flashBtn.setLayoutParams(flp); p.addView(flashBtn);
        strobeBtn = new Button(this); strobeBtn.setText("⚡  СТРОБОСКОП"); strobeBtn.setTextColor(Color.WHITE); strobeBtn.setTextSize(16); strobeBtn.setAllCaps(false);
        GradientDrawable sb = new GradientDrawable(); sb.setCornerRadius(16); sb.setColor(Color.parseColor("#3A2A0A")); sb.setStroke(2, Color.parseColor("#FFDD44")); strobeBtn.setBackground(sb);
        strobeBtn.setOnClickListener(v -> toggleStrobe()); strobeBtn.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110)); p.addView(strobeBtn);
        return p;
    }

    private void openFlashlight() { hideAll(); flashlightPanel.setVisibility(View.VISIBLE); }

    private boolean initCameraManager() {
        try {
            if (cameraManager == null) cameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
            if (cameraId == null) {
                for (String id : cameraManager.getCameraIdList()) {
                    android.hardware.camera2.CameraCharacteristics ch = cameraManager.getCameraCharacteristics(id);
                    Boolean flashAvailable = ch.get(android.hardware.camera2.CameraCharacteristics.FLASH_INFO_AVAILABLE);
                    Integer facing = ch.get(android.hardware.camera2.CameraCharacteristics.LENS_FACING);
                    if (flashAvailable != null && flashAvailable && facing != null && facing == android.hardware.camera2.CameraCharacteristics.LENS_FACING_BACK) { cameraId = id; return true; }
                }
            } else return true;
        } catch (Exception e) {}
        return false;
    }

    private void setTorch(boolean on) {
        try { if (cameraManager == null || cameraId == null) initCameraManager(); if (cameraManager != null && cameraId != null) cameraManager.setTorchMode(cameraId, on); } catch (Exception e) {}
    }

    private void toggleFlashlight() {
        if (strobeOn) toggleStrobe(); flashOn = !flashOn; setTorch(flashOn);
        if (flashOn) { flashBtn.setText("🔦  ВЫКЛЮЧИТЬ"); flashStatus.setText("● ВКЛЮЧЕН"); flashStatus.setTextColor(Color.parseColor("#FFDD44")); }
        else { flashBtn.setText("🔦  ВКЛЮЧИТЬ"); flashStatus.setText("● ВЫКЛЮЧЕН"); flashStatus.setTextColor(Color.parseColor("#666666")); }
    }

    private void toggleStrobe() {
        strobeOn = !strobeOn;
        if (strobeOn) {
            if (!initCameraManager()) { strobeOn = false; flashStatus.setText("● НЕТ ФОНАРИКА"); flashStatus.setTextColor(Color.parseColor("#FF4444")); return; }
            if (strobeHandler == null) strobeHandler = new Handler(Looper.getMainLooper());
            strobeBtn.setText("⚡  СТОП"); GradientDrawable sb = new GradientDrawable(); sb.setCornerRadius(16); sb.setColor(Color.parseColor("#8A2A2A")); strobeBtn.setBackground(sb);
            flashStatus.setText("● СТРОБОСКОП 5 Hz"); flashStatus.setTextColor(Color.parseColor("#FFDD44")); strobeHandler.post(strobeRunnable);
        } else {
            if (strobeHandler != null) strobeHandler.removeCallbacksAndMessages(null); setTorch(false);
            strobeBtn.setText("⚡  СТРОБОСКОП"); GradientDrawable sb = new GradientDrawable(); sb.setCornerRadius(16); sb.setColor(Color.parseColor("#3A2A0A")); sb.setStroke(2, Color.parseColor("#FFDD44")); strobeBtn.setBackground(sb);
            flashStatus.setText("● ВЫКЛЮЧЕН"); flashStatus.setTextColor(Color.parseColor("#666666"));
        }
    }

    private boolean strobeState = false;
    private final Runnable strobeRunnable = new Runnable() { @Override public void run() { if (!strobeOn) return; strobeState = !strobeState; setTorch(strobeState); if (strobeHandler != null) strobeHandler.postDelayed(this, 100); } };

    private void openWeb(String url) { hideAll(); webView.setVisibility(View.VISIBLE); miniKeyboard.setVisibility(View.VISIBLE); webView.loadUrl(url); miniKeyboard.bringToFront();}

    private void hideAll() {
        if (launcherPanel != null) launcherPanel.setVisibility(View.GONE);
        webView.setVisibility(View.GONE); toolsPanel.setVisibility(View.GONE); monitorPanel.setVisibility(View.GONE); noisePanel.setVisibility(View.GONE);
        networkPanel.setVisibility(View.GONE); wifiPanel.setVisibility(View.GONE); devicePanel.setVisibility(View.GONE); cryptoPanel.setVisibility(View.GONE);
        flashlightPanel.setVisibility(View.GONE); gpsPanel.setVisibility(View.GONE); cameraPanel.setVisibility(View.GONE); morsePanel.setVisibility(View.GONE);
        musicPanel.setVisibility(View.GONE); speedTestPanel.setVisibility(View.GONE); settingsPanel.setVisibility(View.GONE); // <--- добавил настройки
        if (photoPanel != null) photoPanel.setVisibility(View.GONE); // <--- ДОБАВИТЬ ВОТ ЭТУ СТРОЧКУ
        miniKeyboard.setVisibility(View.GONE); mainKeyboardLayout.setVisibility(View.GONE);
        stopGps(); stopCamera(); stopNoise();
    }

    private void closeAll() {
        hideAll(); mainKeyboardLayout.setVisibility(View.VISIBLE);
        if (webView != null) { webView.stopLoading(); webView.loadUrl("about:blank"); }
        if (mediaPlayer != null) { try { mediaPlayer.stop(); mediaPlayer.release(); } catch (Exception e) {} mediaPlayer = null; }
        if (visualizer != null) { try { visualizer.release(); } catch (Exception e) {} visualizer = null; }
    }

    @Override public void onDestroy() {
        hideAll(); if (strobeHandler != null) strobeHandler.removeCallbacksAndMessages(null); monitorHandler.removeCallbacksAndMessages(null);
        if (executor != null) executor.shutdownNow();
        if (mediaPlayer != null) { try { mediaPlayer.release(); } catch (Exception e) {} }
        if (visualizer != null) { try { visualizer.release(); } catch (Exception e) {} }
        super.onDestroy();
    }

    private LinearLayout buildMiniKeyboard() {
        LinearLayout wrapper = new LinearLayout(this); wrapper.setOrientation(LinearLayout.VERTICAL); wrapper.setBackgroundColor(Color.parseColor("#151515")); wrapper.setPadding(4, 4, 4, 6);
        LinearLayout header = new LinearLayout(this); header.setOrientation(LinearLayout.HORIZONTAL); header.setGravity(Gravity.CENTER_VERTICAL);
        Button hideBtn = new Button(this); hideBtn.setText("⌄"); hideBtn.setTextColor(Color.WHITE); hideBtn.setTextSize(16); hideBtn.setPadding(0, 0, 0, 0); hideBtn.setAllCaps(false);
        GradientDrawable hs = new GradientDrawable(); hs.setCornerRadius(10); hs.setColor(Color.parseColor("#252525")); hideBtn.setBackground(hs);
        hideBtn.setOnClickListener(v -> {
            if (miniKeyboard == null || miniKeyboard.getChildCount() < 2) return; int vis = miniKeyboard.getChildAt(1).getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE;
            for (int i = 1; i < miniKeyboard.getChildCount(); i++) miniKeyboard.getChildAt(i).setVisibility(vis); hideBtn.setText(vis == View.VISIBLE ? "⌄" : "⌃");
        });
        LinearLayout.LayoutParams hb = new LinearLayout.LayoutParams(70, 52); hb.setMargins(4, 2, 4, 2); hideBtn.setLayoutParams(hb); header.addView(hideBtn);
        Button backBtn = new Button(this); backBtn.setText("◁ Назад"); backBtn.setTextColor(Color.WHITE); backBtn.setTextSize(12); backBtn.setAllCaps(false); backBtn.setBackground(hs);
        backBtn.setOnClickListener(v -> { if (webView.canGoBack()) webView.goBack(); }); LinearLayout.LayoutParams bb = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 52); bb.setMargins(4, 2, 4, 2); backBtn.setLayoutParams(bb); header.addView(backBtn);
        Button clearBtn = new Button(this); clearBtn.setText("Очистить"); clearBtn.setTextColor(Color.parseColor("#FF9999")); clearBtn.setTextSize(12); clearBtn.setAllCaps(false); clearBtn.setBackground(hs);
        clearBtn.setOnClickListener(v -> clearActiveField()); LinearLayout.LayoutParams cb = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, 52); cb.setMargins(4, 2, 4, 2); clearBtn.setLayoutParams(cb); header.addView(clearBtn);
        wrapper.addView(header, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 56));

        String[][] rows;
        if (miniSymbolsMode) { rows = new String[][]{ {"1","2","3","4","5","6","7","8","9","0"}, {"@","#","$","%","&","*","-","+","(",")"}, {"/","\"","'",":",";","!",",","?",".","_"} }; }
        else if (miniRussian) { rows = new String[][]{ {"1","2","3","4","5","6","7","8","9","0"}, {"й","ц","у","к","е","н","г","ш","щ","з","х","ъ"}, {"ф","ы","в","а","п","р","о","л","д","ж","э"}, {"я","ч","с","м","и","т","ь","б","ю","."} }; }
        else { rows = new String[][]{ {"1","2","3","4","5","6","7","8","9","0"}, {"q","w","e","r","t","y","u","i","o","p"}, {"a","s","d","f","g","h","j","k","l"}, {"z","x","c","v","b","n","m","@","."} }; }

        for (String[] row : rows) { LinearLayout r = new LinearLayout(this); r.setOrientation(LinearLayout.HORIZONTAL); r.setGravity(Gravity.CENTER); for (String k : row) r.addView(makeMiniKey(k, 1f, () -> onMiniKey(k))); wrapper.addView(r, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 66)); }
        LinearLayout bottomRow = new LinearLayout(this); bottomRow.setOrientation(LinearLayout.HORIZONTAL); bottomRow.setGravity(Gravity.CENTER);
        if (!miniSymbolsMode) { bottomRow.addView(makeMiniKey(miniRussian ? "РУС" : "ENG", 1.2f, () -> { miniRussian = !miniRussian; if (miniKeyboard == null) return; miniKeyboard.removeAllViews(); LinearLayout rebuilt = buildMiniKeyboard(); while (rebuilt.getChildCount() > 0) { View c = rebuilt.getChildAt(0); rebuilt.removeViewAt(0); miniKeyboard.addView(c); } })); }
        bottomRow.addView(makeMiniKey(miniSymbolsMode ? "АБВ" : "123", 1.2f, () -> { miniSymbolsMode = !miniSymbolsMode; if (miniKeyboard == null) return; miniKeyboard.removeAllViews(); LinearLayout rebuilt = buildMiniKeyboard(); while (rebuilt.getChildCount() > 0) { View c = rebuilt.getChildAt(0); rebuilt.removeViewAt(0); miniKeyboard.addView(c); } }));
        bottomRow.addView(makeMiniKey("←", 1f, () -> moveCursorInPage(-1)));
        bottomRow.addView(makeMiniKey("ПРОБЕЛ", 2f, () -> onMiniKey(" ")));
        bottomRow.addView(makeMiniKey("→", 1f, () -> moveCursorInPage(1)));
        bottomRow.addView(makeMiniKey("⌫", 1.3f, () -> onMiniKey("⌫")));
        bottomRow.addView(makeMiniKey("↵", 1.3f, () -> sendEnter()));
        wrapper.addView(bottomRow, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 66));
        return wrapper;
    }

    private Button makeMiniKey(String text, float weight, Runnable onClick) {
        Button b = new Button(this); b.setText(text); b.setTextSize(16); b.setTextColor(Color.WHITE); b.setPadding(0, 0, 0, 0); b.setAllCaps(false); b.setGravity(Gravity.CENTER);
        GradientDrawable shape = new GradientDrawable(); shape.setCornerRadius(12); shape.setColor(Color.parseColor("#2A2A2A")); b.setBackground(shape);
        b.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); onClick.run(); });
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight); p.setMargins(3, 3, 3, 3); b.setLayoutParams(p); return b;
    }

    private void moveCursorInPage(int delta) {
        String js = "(function(){var el=document.activeElement;if(!el)return;" + "if(el.tagName==='INPUT'||el.tagName==='TEXTAREA'){" + "var p=(el.selectionStart!=null)?el.selectionStart:0;" + "p=p+(" + delta + ");" + "if(p<0)p=0;if(p>el.value.length)p=el.value.length;" + "el.setSelectionRange(p,p);el.focus();}" + "else if(el.isContentEditable){" + "var sel=window.getSelection();" + "if(sel&&sel.rangeCount>0){" + "var r=sel.getRangeAt(0);" + "var node=r.startContainer;" + "var off=r.startOffset+(" + delta + ");" + "if(node.nodeType===3){" + "if(off<0)off=0;if(off>node.length)off=node.length;" + "r.setStart(node,off);r.setEnd(node,off);" + "sel.removeAllRanges();sel.addRange(r);}}}" + "})()";
        webView.evaluateJavascript(js, null);
    }

    private void onMiniKey(String key) { if (key.equals("⌫")) backspaceInPage(); else injectIntoPage(key); }

    private void injectIntoPage(String text) {
        String js = "(function(){" + "var el=document.activeElement;if(!el)return;" + "var s=" + jsString(text) + ";" + "if(el.tagName==='INPUT'||el.tagName==='TEXTAREA'){" + "var start=(el.selectionStart!=null)?el.selectionStart:el.value.length;" + "var end=(el.selectionEnd!=null)?el.selectionEnd:el.value.length;" + "el.value=el.value.slice(0,start)+s+el.value.slice(end);" + "var np=start+s.length;" + "el.setSelectionRange(np,np);" + "el.dispatchEvent(new Event('input',{bubbles:true}));}" + "else if(el.isContentEditable){" + "document.execCommand('insertText',false,s);}" + "})()";
        webView.evaluateJavascript(js, null);
    }

    private void backspaceInPage() {
        String js = "(function(){" + "var el=document.activeElement;if(!el)return;" + "if(el.tagName==='INPUT'||el.tagName==='TEXTAREA'){" + "var start=(el.selectionStart!=null)?el.selectionStart:el.value.length;" + "var end=(el.selectionEnd!=null)?el.selectionEnd:el.value.length;" + "if(start!==end){" + "el.value=el.value.slice(0,start)+el.value.slice(end);" + "}else if(start>0){" + "el.value=el.value.slice(0,start-1)+el.value.slice(start);start--;" + "}" + "el.setSelectionRange(start,start);" + "el.dispatchEvent(new Event('input',{bubbles:true}));}" + "else if(el.isContentEditable){" + "var sel=window.getSelection();" + "if(sel&&sel.rangeCount>0){" + "var r=sel.getRangeAt(0);" + "if(!r.collapsed){r.deleteContents();}" + "else if(r.startOffset>0){r.setStart(r.startContainer,r.startOffset-1);r.deleteContents();}}}" + "})()";
        webView.evaluateJavascript(js, null);
    }

    private void clearActiveField() {
        String js = "(function(){var el=document.activeElement;if(!el)return;if(el.tagName==='INPUT'||el.tagName==='TEXTAREA'){el.value='';el.dispatchEvent(new Event('input',{bubbles:true}));}else if(el.isContentEditable){el.textContent='';el.dispatchEvent(new Event('input',{bubbles:true}));}})()";
        webView.evaluateJavascript(js, null);
    }

    private void sendEnter() {
        String js = "(function(){var el=document.activeElement;if(!el)return;['keydown','keypress','keyup'].forEach(function(t){el.dispatchEvent(new KeyboardEvent(t,{key:'Enter',keyCode:13,which:13,bubbles:true}));});})()";
        webView.evaluateJavascript(js, null);
    }

    private String jsString(String s) { return "'" + s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n") + "'"; }

       private void openPhotoGallery() {
        hideAll();
        if (photoPanel == null) {
            photoPanel = new LinearLayout(this);
            photoPanel.setOrientation(LinearLayout.VERTICAL);
            photoPanel.setBackgroundColor(Color.parseColor("#0A0A0A"));
            
            LinearLayout top = new LinearLayout(this);
            top.setOrientation(LinearLayout.HORIZONTAL);
            top.setGravity(Gravity.CENTER_VERTICAL);
            TextView title = new TextView(this);
            title.setText("◤ ВЫБЕРИ ФОТО ◢");
            title.setTextColor(Color.WHITE);
            title.setTextSize(14);
            title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            top.addView(title);

            Button closeBtn = new Button(this);
            closeBtn.setText("✕");
            closeBtn.setTextColor(Color.RED);
            closeBtn.setOnClickListener(v -> {
                photoPanel.setVisibility(View.GONE);
                if (mFilePathCallback != null) { mFilePathCallback.onReceiveValue(null); mFilePathCallback = null; }
                mainKeyboardLayout.setVisibility(View.VISIBLE);
            });
            top.addView(closeBtn);
            photoPanel.addView(top);

            photoGrid = new GridView(this);
            photoGrid.setNumColumns(3);
            photoGrid.setHorizontalSpacing(4);
            photoGrid.setVerticalSpacing(4);
            photoGrid.setPadding(4, 4, 4, 4);
            photoGrid.setAdapter(new PhotoAdapter());
            photoGrid.setOnItemClickListener((parent, view, position, id) -> {
                String path = ((String) view.getTag());
                File file = new File(path);
                Uri uri = Uri.fromFile(file);
                if (mFilePathCallback != null) {
                    mFilePathCallback.onReceiveValue(new Uri[]{uri});
                    mFilePathCallback = null;
                }
                photoPanel.setVisibility(View.GONE);
                if (photoForWebView) {
                    webView.setVisibility(View.VISIBLE);
                    miniKeyboard.setVisibility(View.VISIBLE);
                } else {
                    mainKeyboardLayout.setVisibility(View.VISIBLE);
                }
            });
            photoPanel.addView(photoGrid, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
            
            keyboardContainer.addView(photoPanel, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        }
        photoPanel.setVisibility(View.VISIBLE);
        photoGrid.invalidateViews();
    } 
        private class PhotoAdapter extends BaseAdapter {
        private List<String> paths = new ArrayList<>();
        public PhotoAdapter() {
            String[] projection = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, projection, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");
            if (cursor != null) {
                int col = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                while (cursor.moveToNext()) {
                    paths.add(cursor.getString(col));
                }
                cursor.close();
            }
        }
        @Override public int getCount() { return paths.size(); }
        @Override public Object getItem(int i) { return paths.get(i); }
        @Override public long getItemId(int i) { return i; }
        @Override public View getView(int i, View view, ViewGroup viewGroup) {
            ImageView iv = new ImageView(MyCustomKeyboard.this);
            iv.setLayoutParams(new GridView.LayoutParams(300, 300));
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            String path = paths.get(i);
            iv.setTag(path);
            BitmapFactory.Options opts = new BitmapFactory.Options();
            opts.inSampleSize = 4;
            Bitmap bmp = BitmapFactory.decodeFile(path, opts);
            iv.setImageBitmap(bmp);
            return iv;
        }
    }
           private LinearLayout buildSettingsPanel() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL); p.setBackgroundColor(Color.parseColor("#0A0A0A")); p.setPadding(20, 16, 20, 16);
        LinearLayout top = new LinearLayout(this); top.setOrientation(LinearLayout.HORIZONTAL); top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this); title.setText("◤ НАСТРОЙКИ ◢"); title.setTextColor(Color.WHITE); title.setTextSize(14); title.setTypeface(Typeface.MONOSPACE);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title); top.addView(makeSmallBtn("←", "#FFFFFF", v -> openTools())); p.addView(top);

        TextView lbl = new TextView(this); lbl.setText("Высота клавиатуры: " + (int)(kbHeightPercent * 100) + "%"); lbl.setTextColor(Color.WHITE); lbl.setTextSize(14); lbl.setPadding(0, 24, 0, 12); p.addView(lbl);
        
        SeekBar sb = new SeekBar(this);
        sb.setMax(45); 
        sb.setProgress((int)(kbHeightPercent * 100) - 40);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean f) {
                lbl.setText("Высота клавиатуры: " + (v + 40) + "%");
            }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {
                kbHeightPercent = (s.getProgress() + 40) / 100f;
                getSharedPreferences("kb_prefs", MODE_PRIVATE).edit().putFloat("height", kbHeightPercent).apply();
            }
        });
        p.addView(sb);

        Button applyBtn = new Button(this);
        applyBtn.setText("ПРИМЕНИТЬ РАЗМЕР");
        applyBtn.setTextColor(Color.WHITE);
        applyBtn.setBackgroundColor(Color.parseColor("#44DD88"));
        applyBtn.setOnClickListener(v -> {
            // Принудительно пересобираем клавиатуру
            setInputView(onCreateInputView()); 
            Toast.makeText(MyCustomKeyboard.this, "Размер применен", Toast.LENGTH_SHORT).show();
        });
        LinearLayout.LayoutParams applyParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100);
        applyParams.setMargins(0, 24, 0, 0);
        applyBtn.setLayoutParams(applyParams);
        p.addView(applyBtn);

        return p;
    }

    private void buildKeyboard() {
        if (keyboardKeysLayout == null) return; keyboardKeysLayout.removeAllViews();
        if (symbolsMode) {
            String[] sym1 = {"1","2","3","4","5","6","7","8","9","0"}; String[] sym2 = {"@","#","$","%","&","*","-","+","(",")"}; String[] sym3 = {"/","\"","'",":",";","!",",","?",".","_"};
            LinearLayout r1 = new LinearLayout(this); r1.setGravity(Gravity.CENTER_HORIZONTAL); for (String k : sym1) addKeyButton(r1, k, v -> typeText(k), 1f, "#1E1E1E"); keyboardKeysLayout.addView(r1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110));
            LinearLayout r2 = new LinearLayout(this); r2.setGravity(Gravity.CENTER_HORIZONTAL); for (String k : sym2) addKeyButton(r2, k, v -> typeText(k), 1f, "#2A2A2A"); keyboardKeysLayout.addView(r2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110));
            LinearLayout r3 = new LinearLayout(this); r3.setGravity(Gravity.CENTER_HORIZONTAL); for (String k : sym3) addKeyButton(r3, k, v -> typeText(k), 1f, "#2A2A2A"); keyboardKeysLayout.addView(r3, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110));
            LinearLayout r4 = new LinearLayout(this); r4.setGravity(Gravity.CENTER_HORIZONTAL);
            addKeyButton(r4, "🖼️", v -> { photoForWebView = false; openPhotoGallery(); }, 1f, "#333333");
            addKeyButton(r4, "АБВ", v -> { symbolsMode = false; buildKeyboard(); }, 1.5f, "#1A1A1A"); addKeyButton(r4, "←", v -> sendArrow(KeyEvent.KEYCODE_DPAD_LEFT), 1f, "#252525");
            addKeyButton(r4, "ПРОБЕЛ", v -> typeText(" "), 3f, "#2A2A2A"); addKeyButton(r4, "→", v -> sendArrow(KeyEvent.KEYCODE_DPAD_RIGHT), 1f, "#252525");
            addKeyButton(r4, "⌫", v -> { InputConnection ic = getCurrentInputConnection(); if (ic != null) ic.deleteSurroundingText(1, 0); }, 1.5f, "#4A2525");
            keyboardKeysLayout.addView(r4, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110));
        } else {
            String[] numRow = {"1","2","3","4","5","6","7","8","9","0"}; LinearLayout row0 = new LinearLayout(this); row0.setGravity(Gravity.CENTER_HORIZONTAL);
            for (String key : numRow) addKeyButton(row0, key, v -> typeText(key), 1.0f, "#1E1E1E"); keyboardKeysLayout.addView(row0, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 105));
            String[] row1Keys = isRussian ? new String[]{"й","ц","у","к","е","н","г","ш","щ","з","х","ъ"} : new String[]{"q","w","e","r","t","y","u","i","o","p"};
            String[] row2Keys = isRussian ? new String[]{"ф","ы","в","а","п","р","о","л","д","ж","э"} : new String[]{"a","s","d","f","g","h","j","k","l"};
            String[] row3Keys = isRussian ? new String[]{"я","ч","с","м","и","т","ь","б","ю"} : new String[]{"z","x","c","v","b","n","m"};
            LinearLayout row1 = new LinearLayout(this); row1.setGravity(Gravity.CENTER_HORIZONTAL); for (String key : row1Keys) addKeyButton(row1, key.toUpperCase(), v -> typeText(key), 1.0f, "#2A2A2A"); keyboardKeysLayout.addView(row1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 125));
            LinearLayout row2 = new LinearLayout(this); row2.setGravity(Gravity.CENTER_HORIZONTAL); for (String key : row2Keys) addKeyButton(row2, key.toUpperCase(), v -> typeText(key), 1.0f, "#2A2A2A"); keyboardKeysLayout.addView(row2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 125));
            LinearLayout row3 = new LinearLayout(this); row3.setGravity(Gravity.CENTER_HORIZONTAL); for (String key : row3Keys) addKeyButton(row3, key.toUpperCase(), v -> typeText(key), 1.0f, "#2A2A2A");
            addKeyButton(row3, "⌫", v -> { InputConnection ic = getCurrentInputConnection(); if (ic != null) ic.deleteSurroundingText(1, 0); }, 1.5f, "#4A2525"); keyboardKeysLayout.addView(row3, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 125));
            LinearLayout row4 = new LinearLayout(this); row4.setGravity(Gravity.CENTER_HORIZONTAL);
            addKeyButton(row4, isRussian ? "РУС" : "ENG", v -> { isRussian = !isRussian; buildKeyboard(); }, 1.8f, "#1A1A1A");
            addKeyButton(row4, "123", v -> { symbolsMode = true; buildKeyboard(); }, 1.2f, "#1A1A1A");
            Button spaceBtn = new Button(this); spaceBtn.setText("ПРОБЕЛ"); spaceBtn.setTextColor(Color.WHITE); spaceBtn.setTextSize(18); spaceBtn.setPadding(0, 0, 0, 0); spaceBtn.setGravity(Gravity.CENTER); spaceBtn.setAllCaps(false);
            GradientDrawable spaceShape = new GradientDrawable(); spaceShape.setCornerRadius(14); spaceShape.setColor(Color.parseColor("#2A2A2A")); spaceBtn.setBackground(spaceShape);
            spaceBtn.setOnTouchListener(new View.OnTouchListener() {
                float lastX = 0;
                @Override public boolean onTouch(View v, MotionEvent event) {
                    if (event.getAction() == MotionEvent.ACTION_DOWN) { lastX = event.getX(); v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); return false; }
                    else if (event.getAction() == MotionEvent.ACTION_MOVE) {
                        float dx = event.getX() - lastX; if (Math.abs(dx) > 30) { if (dx > 0) sendArrow(KeyEvent.KEYCODE_DPAD_RIGHT); else sendArrow(KeyEvent.KEYCODE_DPAD_LEFT); lastX = event.getX(); }
                        return true;
                    } return false;
                }
            });
            spaceBtn.setOnClickListener(v -> typeText(" "));
            LinearLayout.LayoutParams sp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 3f); sp.setMargins(3, 4, 3, 4); spaceBtn.setLayoutParams(sp); row4.addView(spaceBtn);
            addKeyButton(row4, "←", v -> sendArrow(KeyEvent.KEYCODE_DPAD_LEFT), 1f, "#252525"); addKeyButton(row4, "→", v -> sendArrow(KeyEvent.KEYCODE_DPAD_RIGHT), 1f, "#252525");
            keyboardKeysLayout.addView(row4, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 115));
        }
    }

    private void sendArrow(int keyCode) {
        InputConnection ic = getCurrentInputConnection(); if (ic == null) return;
        try {
            android.view.inputmethod.ExtractedText et = ic.getExtractedText(new android.view.inputmethod.ExtractedTextRequest(), 0);
            if (et != null) {
                int pos = et.selectionStart;
                if (keyCode == KeyEvent.KEYCODE_DPAD_LEFT && pos > 0) { ic.setSelection(pos - 1, pos - 1); return; }
                else if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && pos < et.text.length()) { ic.setSelection(pos + 1, pos + 1); return; }
            }
        } catch (Exception e) {}
        ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, keyCode)); ic.sendKeyEvent(new KeyEvent(KeyEvent.ACTION_UP, keyCode));
    }

    private void addKeyButton(LinearLayout parent, String text, View.OnClickListener listener, float weight, String colorHex) {
        Button btn = new Button(this); btn.setText(text); btn.setTextColor(Color.WHITE); btn.setTextSize(18); btn.setPadding(0, 0, 0, 0); btn.setGravity(Gravity.CENTER); btn.setAllCaps(false);
        btn.setOnClickListener(v -> { v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY); listener.onClick(v); });
        GradientDrawable shape = new GradientDrawable(); shape.setCornerRadius(14); shape.setColor(Color.parseColor(colorHex)); btn.setBackground(shape);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight); params.setMargins(3, 4, 3, 4); btn.setLayoutParams(params); parent.addView(btn);
    }

    private void addMenuButton(LinearLayout parent, String text, View.OnClickListener listener, String colorHex) {
        Button btn = new Button(this); btn.setText(text); btn.setTextColor(Color.parseColor(colorHex)); btn.setTextSize(11); btn.setGravity(Gravity.CENTER); btn.setPadding(0, 0, 0, 0); btn.setAllCaps(false); btn.setOnClickListener(listener);
        GradientDrawable shape = new GradientDrawable(); shape.setCornerRadius(14); shape.setColor(Color.parseColor("#202020")); btn.setBackground(shape);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f); params.setMargins(4, 5, 4, 5); btn.setLayoutParams(params); parent.addView(btn);
    }

    private void typeText(String text) { InputConnection ic = getCurrentInputConnection(); if (ic != null) ic.commitText(text, 1); }

    @Override public void onPress(int primaryCode) {}
    @Override public void onRelease(int primaryCode) {}
    @Override public void onKey(int primaryCode, int[] keyCodes) {}
    @Override public void onText(CharSequence text) {}
    @Override public void swipeLeft() {}
    @Override public void swipeRight() {}
    @Override public void swipeDown() {}
    @Override public void swipeUp() {}
}
