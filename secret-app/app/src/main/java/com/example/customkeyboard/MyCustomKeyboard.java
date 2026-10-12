package com.example.customkeyboard;

import android.app.ActivityManager;
import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.KeyboardView;
import android.media.AudioAttributes;
import android.media.AudioFormat;
import android.media.AudioTrack;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.TrafficStats;
import android.net.wifi.ScanResult;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.DisplayMetrics;
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
import android.view.Gravity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.URL;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyCustomKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private WebView webView;
    private FrameLayout keyboardContainer;
    private LinearLayout mainKeyboardLayout;
    private LinearLayout keyboardKeysLayout;
    private LinearLayout miniKeyboard;

    private LinearLayout toolsPanel, monitorPanel, noisePanel,
                         networkPanel, wifiPanel, devicePanel;

    // Monitor
    private TextView tvCpu, tvRam, tvBattery, tvNet, tvUptime;
    private Handler monitorHandler = new Handler(Looper.getMainLooper());
    private long lastRx = 0, lastTx = 0, lastMonTime = 0;
    private long prevCpuTotal = 0, prevCpuIdle = 0;

    // Noise
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

    // Scroll-панели
    private TextView networkText, wifiText, deviceText;
    private ScrollView networkScroll, wifiScroll, deviceScroll;

    private ExecutorService executor = Executors.newFixedThreadPool(8);

    private boolean isRussian = true;
    private boolean miniRussian = false;

    @Override
    public View onCreateInputView() {
        keyboardContainer = new FrameLayout(this);
        keyboardContainer.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout rootLayout = new LinearLayout(this);
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setBackgroundColor(Color.parseColor("#0F0F0F"));

        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.HORIZONTAL);
        menuLayout.setBackgroundColor(Color.parseColor("#151515"));
        menuLayout.setPadding(6, 6, 6, 6);

        addMenuButton(menuLayout, "YouTube",  v -> openWeb("https://m.youtube.com"),     "#FF5555");
        addMenuButton(menuLayout, "Telegram", v -> openWeb("https://web.telegram.org"), "#229ED9");
        addMenuButton(menuLayout, "Google",   v -> openWeb("https://www.google.com"),   "#FBBC05");
        addMenuButton(menuLayout, "HACK",     v -> openTools(),                          "#44DD88");
        addMenuButton(menuLayout, "✕",        v -> closeAll(),                          "#FF4444");

        rootLayout.addView(menuLayout, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 100));

        FrameLayout contentArea = new FrameLayout(this);

        mainKeyboardLayout = new LinearLayout(this);
        mainKeyboardLayout.setOrientation(LinearLayout.VERTICAL);
        keyboardKeysLayout = new LinearLayout(this);
        keyboardKeysLayout.setOrientation(LinearLayout.VERTICAL);
        keyboardKeysLayout.setPadding(4, 6, 4, 6);
        mainKeyboardLayout.addView(keyboardKeysLayout, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentArea.addView(mainKeyboardLayout, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        int screenH = getResources().getDisplayMetrics().heightPixels;
        int panelH = (int)(screenH * 0.60);

        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setLoadWithOverviewMode(true);
        settings.setUseWideViewPort(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        webView.setWebViewClient(new WebViewClient());
        webView.setBackgroundColor(Color.WHITE);
        webView.setVisibility(View.GONE);
        contentArea.addView(webView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        toolsPanel = buildToolsPanel();
        toolsPanel.setVisibility(View.GONE);
        contentArea.addView(toolsPanel, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        monitorPanel = buildMonitorPanel();
        monitorPanel.setVisibility(View.GONE);
        contentArea.addView(monitorPanel, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        noisePanel = buildNoisePanel();
        noisePanel.setVisibility(View.GONE);
        contentArea.addView(noisePanel, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        networkPanel = buildScrollPanel("◤ NETWORK ◢", "#44DD88", v -> openTools());
        networkScroll = (ScrollView) networkPanel.getChildAt(1);
        networkText = (TextView) networkScroll.getChildAt(0);
        networkPanel.setVisibility(View.GONE);
        contentArea.addView(networkPanel, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        wifiPanel = buildScrollPanel("◤ WIFI SCAN ◢", "#44AAFF", v -> openTools());
        wifiScroll = (ScrollView) wifiPanel.getChildAt(1);
        wifiText = (TextView) wifiScroll.getChildAt(0);
        wifiPanel.setVisibility(View.GONE);
        contentArea.addView(wifiPanel, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        devicePanel = buildScrollPanel("◤ DEVICE ◢", "#FFAA44", v -> openTools());
        deviceScroll = (ScrollView) devicePanel.getChildAt(1);
        deviceText = (TextView) deviceScroll.getChildAt(0);
        devicePanel.setVisibility(View.GONE);
        contentArea.addView(devicePanel, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, panelH));

        rootLayout.addView(contentArea, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        miniKeyboard = buildMiniKeyboard();
        miniKeyboard.setVisibility(View.GONE);
        rootLayout.addView(miniKeyboard, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        keyboardContainer.addView(rootLayout, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        buildKeyboard();
        return keyboardContainer;
    }
        // ===== Tools =====
    private LinearLayout buildToolsPanel() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setBackgroundColor(Color.parseColor("#0A0A0A"));
        p.setPadding(20, 20, 20, 20);

        TextView title = new TextView(this);
        title.setText("◤ TOOLS ◢");
        title.setTextColor(Color.parseColor("#44DD88"));
        title.setTextSize(18);
        title.setTypeface(Typeface.MONOSPACE);
        title.setPadding(0, 0, 0, 18);
        p.addView(title);

        p.addView(makeToolTile("📊  SYSTEM MONITOR", "CPU, RAM, батарея, сеть, uptime",
                "#44DD88", v -> openMonitor()));
        p.addView(makeToolTile("🌊  ГЕНЕРАТОР ШУМА", "Белый / розовый / коричневый / дождь / волны",
                "#44AAFF", v -> openNoise()));
        p.addView(makeToolTile("📡  NETWORK", "Пинг, IP, GeoIP, DNS",
                "#44DD88", v -> openNetwork()));
        p.addView(makeToolTile("📶  WIFI SCAN", "Все сети вокруг",
                "#44AAFF", v -> openWifi()));
        p.addView(makeToolTile("📱  DEVICE", "Батарея и детали системы",
                "#FFAA44", v -> openDevice()));

        return p;
    }

    private LinearLayout makeToolTile(String title, String desc, String color, View.OnClickListener l) {
        LinearLayout tile = new LinearLayout(this);
        tile.setOrientation(LinearLayout.VERTICAL);
        tile.setPadding(20, 12, 20, 12);
        GradientDrawable bg = new GradientDrawable();
        bg.setCornerRadius(14);
        bg.setColor(Color.parseColor("#151515"));
        bg.setStroke(2, Color.parseColor("#2A3A2A"));
        tile.setBackground(bg);
        tile.setOnClickListener(l);

        TextView t = new TextView(this);
        t.setText(title);
        t.setTextColor(Color.parseColor(color));
        t.setTextSize(14);
        t.setTypeface(Typeface.MONOSPACE);
        tile.addView(t);

        TextView d = new TextView(this);
        d.setText(desc);
        d.setTextColor(Color.parseColor("#668866"));
        d.setTextSize(10);
        d.setPadding(0, 3, 0, 0);
        tile.addView(d);

        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.setMargins(0, 0, 0, 10);
        tile.setLayoutParams(lp);

        return tile;
    }

    private void openTools() {
        hideAll();
        toolsPanel.setVisibility(View.VISIBLE);
    }

    // ===== Универсальная scroll-панель =====
    private LinearLayout buildScrollPanel(String titleText, String color, View.OnClickListener back) {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setBackgroundColor(Color.parseColor("#0A0A0A"));
        wrapper.setPadding(14, 14, 14, 14);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText(titleText);
        title.setTextColor(Color.parseColor(color));
        title.setTextSize(14);
        title.setTypeface(Typeface.MONOSPACE);
        title.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title);

        Button backBtn = new Button(this);
        backBtn.setText("←");
        backBtn.setTextColor(Color.parseColor(color));
        backBtn.setTextSize(16);
        backBtn.setPadding(0, 0, 0, 0);
        backBtn.setAllCaps(false);
        GradientDrawable s = new GradientDrawable();
        s.setCornerRadius(10);
        s.setColor(Color.parseColor("#152515"));
        s.setStroke(2, Color.parseColor("#2A4A2A"));
        backBtn.setBackground(s);
        backBtn.setOnClickListener(back);
        LinearLayout.LayoutParams bp = new LinearLayout.LayoutParams(70, 56);
        backBtn.setLayoutParams(bp);
        top.addView(backBtn);

        wrapper.addView(top, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(Color.parseColor("#0A0A0A"));
        scroll.setPadding(0, 12, 0, 0);

        TextView text = new TextView(this);
        text.setTextColor(Color.parseColor("#44DD88"));
        text.setTextSize(12);
        text.setTypeface(Typeface.MONOSPACE);
        text.setText("...");
        scroll.addView(text);

        wrapper.addView(scroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return wrapper;
    }

    // ===== SYSTEM MONITOR (как было) =====
    private LinearLayout buildMonitorPanel() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setBackgroundColor(Color.parseColor("#0A0A0A"));
        p.setPadding(20, 16, 20, 16);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("◤ SYSTEM MONITOR ◢");
        title.setTextColor(Color.parseColor("#44DD88"));
        title.setTextSize(14);
        title.setTypeface(Typeface.MONOSPACE);
        title.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title);
        top.addView(makeSmallBtn("←", "#44DD88", v -> openTools()));
        p.addView(top, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        View sp = new View(this);
        p.addView(sp, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 18));

        tvUptime  = makeMonitorRow(p, "UPTIME ");
        tvCpu     = makeMonitorRow(p, "CPU    ");
        tvRam     = makeMonitorRow(p, "RAM    ");
        tvBattery = makeMonitorRow(p, "BATTERY");
        tvNet     = makeMonitorRow(p, "NET    ");

        return p;
    }

    private TextView makeMonitorRow(LinearLayout parent, String label) {
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setPadding(0, 9, 0, 9);

        TextView lbl = new TextView(this);
        lbl.setText(label + " : ");
        lbl.setTextColor(Color.parseColor("#556655"));
        lbl.setTextSize(13);
        lbl.setTypeface(Typeface.MONOSPACE);

        TextView val = new TextView(this);
        val.setText("...");
        val.setTextColor(Color.parseColor("#44DD88"));
        val.setTextSize(13);
        val.setTypeface(Typeface.MONOSPACE);
        val.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        row.addView(lbl);
        row.addView(val);
        parent.addView(row);

        View div = new View(this);
        div.setBackgroundColor(Color.parseColor("#152015"));
        parent.addView(div, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 1));

        return val;
    }

    private Button makeSmallBtn(String text, String color, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.parseColor(color));
        b.setTextSize(16);
        b.setPadding(0, 0, 0, 0);
        b.setAllCaps(false);
        GradientDrawable s = new GradientDrawable();
        s.setCornerRadius(10);
        s.setColor(Color.parseColor("#152515"));
        s.setStroke(2, Color.parseColor("#2A4A2A"));
        b.setBackground(s);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(70, 60);
        b.setLayoutParams(p);
        return b;
    }

    private void openMonitor() {
        hideAll();
        monitorPanel.setVisibility(View.VISIBLE);
        lastRx = TrafficStats.getTotalRxBytes();
        lastTx = TrafficStats.getTotalTxBytes();
        lastMonTime = System.currentTimeMillis();
        prevCpuTotal = 0;
        prevCpuIdle = 0;
        monitorHandler.removeCallbacksAndMessages(null);
        monitorHandler.post(monitorTick);
    }

    private Runnable monitorTick = new Runnable() {
        @Override
        public void run() {
            if (monitorPanel.getVisibility() != View.VISIBLE) return;
            updateMonitor();
            monitorHandler.postDelayed(this, 1000);
        }
    };

    private void updateMonitor() {
        long up = SystemClock.elapsedRealtime() / 1000;
        long h = up / 3600, m = (up % 3600) / 60, s = up % 60;
        tvUptime.setText(String.format(Locale.US, "%02d:%02d:%02d", h, m, s));

        int cpu = readCpuUsage();
        if (cpu >= 0) {
            tvCpu.setText(cpu + " %");
        } else {
            int cores = Runtime.getRuntime().availableProcessors();
            tvCpu.setText("n/a (" + cores + " cores)");
        }

        try {
            ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
            ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
            am.getMemoryInfo(mi);
            long usedMb = (mi.totalMem - mi.availMem) / 1024 / 1024;
            long totalMb = mi.totalMem / 1024 / 1024;
            int pct = (int)(100 * usedMb / Math.max(1, totalMb));
            tvRam.setText(usedMb + " / " + totalMb + " MB   (" + pct + "%)");
        } catch (Exception e) { tvRam.setText("n/a"); }

        try {
            BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
            int level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            tvBattery.setText(level + " %");
        } catch (Exception e) { tvBattery.setText("n/a"); }

        long nowRx = TrafficStats.getTotalRxBytes();
        long nowTx = TrafficStats.getTotalTxBytes();
        long now = System.currentTimeMillis();
        long dt = Math.max(1, now - lastMonTime);
        double rxKbs = (nowRx - lastRx) * 1000.0 / dt / 1024.0;
        double txKbs = (nowTx - lastTx) * 1000.0 / dt / 1024.0;
        tvNet.setText(String.format(Locale.US, "↓ %.1f KB/s   ↑ %.1f KB/s", rxKbs, txKbs));
        lastRx = nowRx; lastTx = nowTx; lastMonTime = now;
    }

    private int readCpuUsage() {
        try {
            BufferedReader r = new BufferedReader(new FileReader("/proc/stat"));
            String line = r.readLine();
            r.close();
            if (line == null) return -1;
            String[] p = line.trim().split("\\s+");
            if (p.length < 5) return -1;
            long idle = Long.parseLong(p[4]);
            long total = 0;
            for (int i = 1; i < p.length; i++) {
                try { total += Long.parseLong(p[i]); } catch (Exception ignored) {}
            }
            if (prevCpuTotal == 0) { prevCpuTotal = total; prevCpuIdle = idle; return -1; }
            long dT = total - prevCpuTotal, dI = idle - prevCpuIdle;
            prevCpuTotal = total; prevCpuIdle = idle;
            if (dT <= 0) return -1;
            return (int)(100 * (dT - dI) / dT);
        } catch (Exception e) { return -1; }
    }

    // ===== NOISE =====
    private LinearLayout buildNoisePanel() {
        LinearLayout p = new LinearLayout(this);
        p.setOrientation(LinearLayout.VERTICAL);
        p.setBackgroundColor(Color.parseColor("#0A0A0A"));
        p.setPadding(20, 16, 20, 16);

        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView title = new TextView(this);
        title.setText("◤ NOISE GEN ◢");
        title.setTextColor(Color.parseColor("#44AAFF"));
        title.setTextSize(14);
        title.setTypeface(Typeface.MONOSPACE);
        title.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(title);
        top.addView(makeSmallBtn("←", "#44AAFF", v -> openTools()));
        p.addView(top);

        noiseStatus = new TextView(this);
        noiseStatus.setText("● STOPPED");
        noiseStatus.setTextColor(Color.parseColor("#666666"));
        noiseStatus.setTextSize(13);
        noiseStatus.setTypeface(Typeface.MONOSPACE);
        noiseStatus.setPadding(0, 24, 0, 16);
        p.addView(noiseStatus);

        String[] types = {"БЕЛЫЙ", "РОЗОВЫЙ", "КОРИЧН.", "ДОЖДЬ", "ВОЛНЫ"};
        final Button[] typeBtns = new Button[types.length];
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        for (int i = 0; i < types.length; i++) {
            final int idx = i;
            Button b = new Button(this);
            b.setText(types[i]);
            b.setTextSize(10);
            b.setTextColor(Color.WHITE);
            b.setAllCaps(false);
            b.setPadding(0, 0, 0, 0);
            GradientDrawable s = new GradientDrawable();
            s.setCornerRadius(10);
            s.setColor(Color.parseColor("#152535"));
            s.setStroke(2, Color.parseColor("#2A4A6A"));
            b.setBackground(s);
            b.setOnClickListener(v -> {
                noiseType = idx;
                for (int j = 0; j < typeBtns.length; j++) {
                    GradientDrawable g = new GradientDrawable();
                    g.setCornerRadius(10);
                    g.setColor(Color.parseColor(j == idx ? "#2A5A8A" : "#152535"));
                    g.setStroke(2, Color.parseColor("#2A4A6A"));
                    typeBtns[j].setBackground(g);
                }
            });
            typeBtns[i] = b;
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, 76, 1f);
            lp.setMargins(4, 4, 4, 4);
            b.setLayoutParams(lp);
            if (i < 3) row1.addView(b); else row2.addView(b);
        }
        p.addView(row1);
        p.addView(row2);

        TextView volLabel = new TextView(this);
        volLabel.setText("ГРОМКОСТЬ");
        volLabel.setTextColor(Color.parseColor("#556655"));
        volLabel.setTextSize(11);
        volLabel.setTypeface(Typeface.MONOSPACE);
        volLabel.setPadding(4, 20, 0, 6);
        p.addView(volLabel);

        SeekBar sb = new SeekBar(this);
        sb.setMax(100);
        sb.setProgress(60);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar s, int v, boolean f) { noiseVolume = v / 100f; }
            public void onStartTrackingTouch(SeekBar s) {}
            public void onStopTrackingTouch(SeekBar s) {}
        });
        p.addView(sb);

        playBtn = new Button(this);
        playBtn.setText("▶  ПУСК");
        playBtn.setTextColor(Color.WHITE);
        playBtn.setTextSize(15);
        playBtn.setAllCaps(false);
        GradientDrawable pb = new GradientDrawable();
        pb.setCornerRadius(14);
        pb.setColor(Color.parseColor("#2A5A8A"));
        playBtn.setBackground(pb);
        playBtn.setOnClickListener(v -> {
            if (noisePlaying) stopNoise(); else startNoise();
        });
        LinearLayout.LayoutParams plp = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 90);
        plp.setMargins(0, 24, 0, 0);
        playBtn.setLayoutParams(plp);
        p.addView(playBtn);

        GradientDrawable g = new GradientDrawable();
        g.setCornerRadius(10);
        g.setColor(Color.parseColor("#2A5A8A"));
        g.setStroke(2, Color.parseColor("#2A4A6A"));
        typeBtns[0].setBackground(g);

        return p;
    }

    private void openNoise() {
        hideAll();
        noisePanel.setVisibility(View.VISIBLE);
    }

    private void startNoise() {
        if (noisePlaying) return;
        try {
            int sr = 44100;
            int minBuf = AudioTrack.getMinBufferSize(sr,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            if (minBuf <= 0) minBuf = 4096;
            audioTrack = new AudioTrack.Builder()
                .setAudioAttributes(new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(new AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sr)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(minBuf * 2)
                .setTransferMode(AudioTrack.MODE_STREAM)
                .build();
            audioTrack.play();
            noisePlaying = true;
            playBtn.setText("■  СТОП");
            GradientDrawable pb = new GradientDrawable();
            pb.setCornerRadius(14);
            pb.setColor(Color.parseColor("#8A2A2A"));
            playBtn.setBackground(pb);
            noiseStatus.setText("● PLAYING: " + noiseName(noiseType));
            noiseStatus.setTextColor(Color.parseColor("#44AAFF"));
            noiseThread = new Thread(this::noiseLoop);
            noiseThread.start();
        } catch (Exception e) {
            noiseStatus.setText("● ERROR: " + e.getMessage());
            noiseStatus.setTextColor(Color.parseColor("#FF4444"));
            noisePlaying = false;
        }
    }

    private String noiseName(int t) {
        switch (t) {
            case 0: return "БЕЛЫЙ";
            case 1: return "РОЗОВЫЙ";
            case 2: return "КОРИЧНЕВЫЙ";
            case 3: return "ДОЖДЬ";
            case 4: return "ВОЛНЫ";
        }
        return "?";
    }

    private void stopNoise() {
        noisePlaying = false;
        if (noiseThread != null) {
            try { noiseThread.join(500); } catch (InterruptedException ignored) {}
            noiseThread = null;
        }
        if (audioTrack != null) {
            try { audioTrack.stop(); } catch (Exception ignored) {}
            try { audioTrack.release(); } catch (Exception ignored) {}
            audioTrack = null;
        }
        if (playBtn != null) {
            playBtn.setText("▶  ПУСК");
            GradientDrawable pb = new GradientDrawable();
            pb.setCornerRadius(14);
            pb.setColor(Color.parseColor("#2A5A8A"));
            playBtn.setBackground(pb);
        }
        if (noiseStatus != null) {
            noiseStatus.setText("● STOPPED");
            noiseStatus.setTextColor(Color.parseColor("#666666"));
        }
    }

    private void noiseLoop() {
        short[] buf = new short[2048];
        while (noisePlaying) {
            for (int i = 0; i < buf.length; i++) {
                float s = generateSample() * noiseVolume;
                if (s > 1f) s = 1f; if (s < -1f) s = -1f;
                buf[i] = (short)(s * 32767);
            }
            try { audioTrack.write(buf, 0, buf.length); } catch (Exception e) { break; }
        }
    }

    private float generateSample() {
        float white = rnd.nextFloat() * 2f - 1f;
        switch (noiseType) {
            case 0: return white;
            case 1: {
                pinkB0 = 0.99886f * pinkB0 + white * 0.0555179f;
                pinkB1 = 0.99332f * pinkB1 + white * 0.0750759f;
                pinkB2 = 0.96900f * pinkB2 + white * 0.1538520f;
                pinkB3 = 0.86650f * pinkB3 + white * 0.3104856f;
                pinkB4 = 0.55000f * pinkB4 + white * 0.5329522f;
                pinkB5 = -0.7616f * pinkB5 - white * 0.0168980f;
                float pink = pinkB0+pinkB1+pinkB2+pinkB3+pinkB4+pinkB5+pinkB6+white*0.5362f;
                pinkB6 = white * 0.115926f;
                return pink * 0.11f;
            }
            case 2: { brownLast = (brownLast + 0.02f*white)/1.02f; return brownLast*3.5f; }
            case 3: {
                float bg = white * 0.15f;
                if (rnd.nextFloat() < 0.0008f) rainEnv = 1.0f;
                rainEnv *= 0.9985f;
                return bg + rainEnv * (rnd.nextFloat()*2f - 1f) * 0.7f;
            }
            case 4: {
                wavePhase += 0.00015f;
                if (wavePhase > 6.2831853f) wavePhase -= 6.2831853f;
                float mod = 0.35f + 0.65f * (float)((Math.sin(wavePhase)+1)*0.5);
                return white * mod;
            }
        }
        return white;
    }
        // ===== NETWORK =====
    private void openNetwork() {
        hideAll();
        networkPanel.setVisibility(View.VISIBLE);
        networkText.setText("scanning...\n");
        executor.execute(this::runNetworkScanAll);
    }

    private void runNetworkScanAll() {
        StringBuilder sb = new StringBuilder();

        // Локальный IP
        sb.append("=== LOCAL ===\n");
        String localIp = getLocalIp();
        sb.append("local ip : ").append(localIp != null ? localIp : "n/a").append("\n");
        sb.append("type     : ").append(getNetworkType()).append("\n\n");

        // Публичный IP
        sb.append("=== PUBLIC ===\n");
        String pubIp = httpGet("https://api.ipify.org");
        sb.append("public ip: ").append(pubIp != null ? pubIp.trim() : "failed").append("\n");

        // GeoIP
        if (pubIp != null) {
            String geo = httpGet("http://ip-api.com/json/" + pubIp.trim() + "?fields=country,city,isp,org");
            if (geo != null) {
                sb.append("geo      : ").append(geo.trim()).append("\n");
            }
        }
        sb.append("\n");

        // DNS
        sb.append("=== DNS ===\n");
        appendDns(sb, "google.com");
        appendDns(sb, "youtube.com");
        appendDns(sb, "t.me");
        sb.append("\n");

        // Пинг
        sb.append("=== PING ===\n");
        appendPing(sb, "8.8.8.8");
        appendPing(sb, "1.1.1.1");
        appendPing(sb, "ya.ru");

        sb.append("\n[✓] done");

        final String result = sb.toString();
        monitorHandler.post(() -> networkText.setText(result));
    }

    private void appendDns(StringBuilder sb, String host) {
        try {
            InetAddress[] all = InetAddress.getAllByName(host);
            sb.append(host).append(": ");
            for (int i = 0; i < all.length && i < 3; i++) {
                if (i > 0) sb.append(", ");
                sb.append(all[i].getHostAddress());
            }
            sb.append("\n");
        } catch (Exception e) {
            sb.append(host).append(": failed\n");
        }
    }

    private void appendPing(StringBuilder sb, String host) {
        try {
            InetAddress addr = InetAddress.getByName(host);
            long t0 = System.currentTimeMillis();
            boolean ok = addr.isReachable(2000);
            long ms = System.currentTimeMillis() - t0;
            sb.append(host).append(": ").append(ok ? ("OK " + ms + " ms") : "FAILED").append("\n");
        } catch (Exception e) {
            sb.append(host).append(": FAILED\n");
        }
    }

    private String getLocalIp() {
        try {
            java.util.Enumeration<java.net.NetworkInterface> ifs =
                java.net.NetworkInterface.getNetworkInterfaces();
            while (ifs.hasMoreElements()) {
                java.net.NetworkInterface ni = ifs.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                java.util.Enumeration<InetAddress> addrs = ni.getInetAddresses();
                while (addrs.hasMoreElements()) {
                    InetAddress a = addrs.nextElement();
                    if (!a.isLoopbackAddress() && a.getHostAddress().indexOf(':') < 0) {
                        return a.getHostAddress();
                    }
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
            URL url = new URL(urlStr);
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(4000);
            c.setReadTimeout(4000);
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = r.readLine()) != null) sb.append(line);
            r.close();
            return sb.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // ===== WIFI SCAN =====
    private void openWifi() {
        hideAll();
        wifiPanel.setVisibility(View.VISIBLE);
        wifiText.setText("scanning...\n");
        executor.execute(this::runWifiScan);
    }

    private void runWifiScan() {
        StringBuilder sb = new StringBuilder();
        try {
            WifiManager wm = (WifiManager) getApplicationContext()
                .getSystemService(Context.WIFI_SERVICE);

            WifiInfo wi = wm.getConnectionInfo();
            sb.append("=== CURRENT ===\n");
            if (wi != null && wi.getSSID() != null) {
                sb.append("ssid     : ").append(wi.getSSID().replace("\"", "")).append("\n");
                sb.append("signal   : ").append(wi.getRssi()).append(" dBm\n");
            } else {
                sb.append("not connected\n");
            }
            sb.append("\n");

            wm.startScan();
            Thread.sleep(1500);
            List<ScanResult> results = wm.getScanResults();
            if (results == null || results.isEmpty()) {
                sb.append("=== NEARBY ===\n");
                sb.append("no results. Нужно разрешение на локацию.\n");
            } else {
                sb.append("=== NEARBY (").append(results.size()).append(") ===\n");
                results.sort((a, b) -> Integer.compare(b.level, a.level));
                for (ScanResult r : results) {
                    sb.append(r.SSID.isEmpty() ? "<hidden>" : r.SSID).append("\n");
                    sb.append("  ch=").append(freqToCh(r.frequency));
                    sb.append("  ").append(r.level).append("dBm");
                    sb.append("  ").append(security(r.capabilities));
                    sb.append("\n");
                }
            }
        } catch (Exception e) {
            sb.append("error: ").append(e.getMessage()).append("\n");
        }
        sb.append("\n[✓] done");
        final String result = sb.toString();
        monitorHandler.post(() -> wifiText.setText(result));
    }

    private int freqToCh(int freq) {
        if (freq == 2484) return 14;
        if (freq >= 2412 && freq <= 2472) return (freq - 2407) / 5;
        if (freq >= 5170 && freq <= 5825) return (freq - 5000) / 5;
        return 0;
    }

    private String security(String caps) {
        if (caps == null) return "OPEN";
        if (caps.contains("WPA3")) return "WPA3";
        if (caps.contains("WPA2")) return "WPA2";
        if (caps.contains("WPA")) return "WPA";
        if (caps.contains("WEP")) return "WEP";
        return "OPEN";
    }

    // ===== DEVICE =====
    private void openDevice() {
        hideAll();
        devicePanel.setVisibility(View.VISIBLE);
        deviceText.setText(buildDeviceInfo());
    }

    private String buildDeviceInfo() {
        StringBuilder sb = new StringBuilder();
        sb.append("=== DEVICE ===\n");
        sb.append("brand    : ").append(Build.MANUFACTURER).append("\n");
        sb.append("model    : ").append(Build.MODEL).append("\n");
        sb.append("device   : ").append(Build.DEVICE).append("\n");
        sb.append("android  : ").append(Build.VERSION.RELEASE).append("\n");
        sb.append("sdk      : ").append(Build.VERSION.SDK_INT).append("\n");
        sb.append("kernel   : ").append(System.getProperty("os.version")).append("\n");
        sb.append("arch     : ").append(System.getProperty("os.arch")).append("\n");
        sb.append("cores    : ").append(Runtime.getRuntime().availableProcessors()).append("\n\n");

        DisplayMetrics dm = getResources().getDisplayMetrics();
        sb.append("=== SCREEN ===\n");
        sb.append("size     : ").append(dm.widthPixels).append("x").append(dm.heightPixels).append("\n");
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

            sb.append("=== BATTERY ===\n");
            sb.append("level    : ").append(level).append(" %\n");
            sb.append("temp     : ").append(temp / 10.0).append(" °C\n");
            sb.append("voltage  : ").append(volt).append(" mV\n");
            sb.append("charge   : ").append(chargeCounter / 1000).append(" mAh\n");
            sb.append("health   : ").append(healthStr(health)).append("\n");
            sb.append("status   : ").append(statusStr(status)).append("\n\n");
        } catch (Exception e) {
            sb.append("battery: error\n");
        }

        ActivityManager am = (ActivityManager) getSystemService(Context.ACTIVITY_SERVICE);
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        am.getMemoryInfo(mi);
        sb.append("=== MEMORY ===\n");
        sb.append("total    : ").append(mi.totalMem / 1024 / 1024).append(" MB\n");
        sb.append("avail    : ").append(mi.availMem / 1024 / 1024).append(" MB\n");
        sb.append("low      : ").append(mi.lowMemory ? "yes" : "no").append("\n\n");

        java.io.File dataDir = getFilesDir();
        long free = dataDir.getFreeSpace() / 1024 / 1024;
        long total = dataDir.getTotalSpace() / 1024 / 1024;
        sb.append("=== STORAGE ===\n");
        sb.append("free     : ").append(free).append(" MB\n");
        sb.append("total    : ").append(total).append(" MB\n");

        return sb.toString();
    }

    private String healthStr(int h) {
        switch (h) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "GOOD";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "OVERHEAT";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "DEAD";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "OVER_VOLTAGE";
            case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE: return "FAILURE";
            case BatteryManager.BATTERY_HEALTH_COLD: return "COLD";
        }
        return "UNKNOWN";
    }

    private String statusStr(int s) {
        switch (s) {
            case BatteryManager.BATTERY_STATUS_CHARGING: return "CHARGING";
            case BatteryManager.BATTERY_STATUS_DISCHARGING: return "DISCHARGING";
            case BatteryManager.BATTERY_STATUS_FULL: return "FULL";
            case BatteryManager.BATTERY_STATUS_NOT_CHARGING: return "NOT CHARGING";
        }
        return "UNKNOWN";
    }

    // ===== Web / панели =====
    private void openWeb(String url) {
        hideAll();
        webView.setVisibility(View.VISIBLE);
        miniKeyboard.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }

    private void hideAll() {
        webView.setVisibility(View.GONE);
        toolsPanel.setVisibility(View.GONE);
        monitorPanel.setVisibility(View.GONE);
        noisePanel.setVisibility(View.GONE);
        networkPanel.setVisibility(View.GONE);
        wifiPanel.setVisibility(View.GONE);
        devicePanel.setVisibility(View.GONE);
        miniKeyboard.setVisibility(View.GONE);
        mainKeyboardLayout.setVisibility(View.GONE);
    }

    private void closeAll() {
        stopNoise();
        monitorHandler.removeCallbacksAndMessages(null);
        hideAll();
        mainKeyboardLayout.setVisibility(View.VISIBLE);
        if (webView != null) {
            webView.stopLoading();
            webView.loadUrl("about:blank");
        }
    }

    @Override
    public void onDestroy() {
        stopNoise();
        monitorHandler.removeCallbacksAndMessages(null);
        if (executor != null) executor.shutdownNow();
        super.onDestroy();
    }

    // ===== Мини-клавиатура =====
    private LinearLayout buildMiniKeyboard() {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setBackgroundColor(Color.parseColor("#151515"));
        wrapper.setPadding(4, 4, 4, 6);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button hideBtn = new Button(this);
        hideBtn.setText("⌄");
        hideBtn.setTextColor(Color.WHITE);
        hideBtn.setTextSize(16);
        hideBtn.setPadding(0, 0, 0, 0);
        hideBtn.setAllCaps(false);
        GradientDrawable hs = new GradientDrawable();
        hs.setCornerRadius(10);
        hs.setColor(Color.parseColor("#252525"));
        hideBtn.setBackground(hs);
        hideBtn.setOnClickListener(v -> {
            if (miniKeyboard == null || miniKeyboard.getChildCount() < 2) return;
            int vis = miniKeyboard.getChildAt(1).getVisibility() == View.VISIBLE
                    ? View.GONE : View.VISIBLE;
            for (int i = 1; i < miniKeyboard.getChildCount(); i++)
                miniKeyboard.getChildAt(i).setVisibility(vis);
            hideBtn.setText(vis == View.VISIBLE ? "⌄" : "⌃");
        });
        LinearLayout.LayoutParams hb = new LinearLayout.LayoutParams(90, 52);
        hb.setMargins(4, 2, 4, 2);
        hideBtn.setLayoutParams(hb);
        header.addView(hideBtn);

        Button clearBtn = new Button(this);
        clearBtn.setText("Очистить");
        clearBtn.setTextColor(Color.parseColor("#FF9999"));
        clearBtn.setTextSize(12);
        clearBtn.setPadding(0, 0, 0, 0);
        clearBtn.setAllCaps(false);
        GradientDrawable cs = new GradientDrawable();
        cs.setCornerRadius(10);
        cs.setColor(Color.parseColor("#252525"));
        clearBtn.setBackground(cs);
        clearBtn.setOnClickListener(v -> clearActiveField());
        LinearLayout.LayoutParams cb = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, 52);
        cb.setMargins(4, 2, 4, 2);
        clearBtn.setLayoutParams(cb);
        header.addView(clearBtn);

        wrapper.addView(header, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 56));

        String[][] rows;
        if (miniRussian) {
            rows = new String[][]{
                {"1","2","3","4","5","6","7","8","9","0"},
                {"й","ц","у","к","е","н","г","ш","щ","з","х","ъ"},
                {"ф","ы","в","а","п","р","о","л","д","ж","э"},
                {"я","ч","с","м","и","т","ь","б","ю","."}
            };
        } else {
            rows = new String[][]{
                {"1","2","3","4","5","6","7","8","9","0"},
                {"q","w","e","r","t","y","u","i","o","p"},
                {"a","s","d","f","g","h","j","k","l"},
                {"z","x","c","v","b","n","m","@","."}
            };
        }

        for (String[] row : rows) {
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setGravity(Gravity.CENTER);
            for (String k : row) r.addView(makeMiniKey(k, 1f, () -> onMiniKey(k)));
            wrapper.addView(r, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 66));
        }

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.CENTER);

        bottomRow.addView(makeMiniKey(miniRussian ? "РУС" : "ENG", 1.3f, () -> {
            miniRussian = !miniRussian;
            if (miniKeyboard == null) return;
            miniKeyboard.removeAllViews();
            LinearLayout rebuilt = buildMiniKeyboard();
            while (rebuilt.getChildCount() > 0) {
                View c = rebuilt.getChildAt(0);
                rebuilt.removeViewAt(0);
                miniKeyboard.addView(c);
            }
        }));
        bottomRow.addView(makeMiniKey("ПРОБЕЛ", 3f, () -> onMiniKey(" ")));
        bottomRow.addView(makeMiniKey("-", 1f, () -> onMiniKey("-")));
        bottomRow.addView(makeMiniKey("_", 1f, () -> onMiniKey("_")));
        bottomRow.addView(makeMiniKey("⌫", 1.5f, () -> onMiniKey("⌫")));
        bottomRow.addView(makeMiniKey("↵", 1.5f, () -> sendEnter()));
        wrapper.addView(bottomRow, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 66));

        return wrapper;
    }

    private Button makeMiniKey(String text, float weight, Runnable onClick) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(16);
        b.setTextColor(Color.WHITE);
        b.setPadding(0, 0, 0, 0);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(12);
        shape.setColor(Color.parseColor("#2A2A2A"));
        b.setBackground(shape);
        b.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        p.setMargins(3, 3, 3, 3);
        b.setLayoutParams(p);
        return b;
    }

    private void onMiniKey(String key) {
        if (key.equals("⌫")) backspaceInPage();
        else injectIntoPage(key);
    }

    private void injectIntoPage(String text) {
        String js = "(function(){var el=document.activeElement;if(!el)return;" +
            "if(el.tagName==='INPUT'||el.tagName==='TEXTAREA'){el.value=el.value+" + jsString(text) + ";" +
            "el.dispatchEvent(new Event('input',{bubbles:true}));}" +
            "else if(el.isContentEditable){el.textContent=el.textContent+" + jsString(text) + ";" +
            "el.dispatchEvent(new Event('input',{bubbles:true}));}})()";
        webView.evaluateJavascript(js, null);
    }

    private void backspaceInPage() {
        String js = "(function(){var el=document.activeElement;if(!el)return;" +
            "if(el.tagName==='INPUT'||el.tagName==='TEXTAREA'){el.value=el.value.slice(0,-1);" +
            "el.dispatchEvent(new Event('input',{bubbles:true}));}" +
            "else if(el.isContentEditable){el.textContent=el.textContent.slice(0,-1);" +
            "el.dispatchEvent(new Event('input',{bubbles:true}));}})()";
        webView.evaluateJavascript(js, null);
    }

    private void clearActiveField() {
        String js = "(function(){var el=document.activeElement;if(!el)return;" +
            "if(el.tagName==='INPUT'||el.tagName==='TEXTAREA'){el.value='';" +
            "el.dispatchEvent(new Event('input',{bubbles:true}));}" +
            "else if(el.isContentEditable){el.textContent='';" +
            "el.dispatchEvent(new Event('input',{bubbles:true}));}})()";
        webView.evaluateJavascript(js, null);
    }

    private void sendEnter() {
        String js = "(function(){var el=document.activeElement;if(!el)return;" +
            "['keydown','keypress','keyup'].forEach(function(t){" +
            "el.dispatchEvent(new KeyboardEvent(t,{key:'Enter',keyCode:13,which:13,bubbles:true}));});})()";
        webView.evaluateJavascript(js, null);
    }

    private String jsString(String s) {
        return "'" + s.replace("\\", "\\\\").replace("'", "\\'").replace("\n", "\\n") + "'";
    }

    // ===== Основная клавиатура =====
    private void buildKeyboard() {
        if (keyboardKeysLayout == null) return;
        keyboardKeysLayout.removeAllViews();

        String[] numRow = {"1","2","3","4","5","6","7","8","9","0"};
        LinearLayout row0 = new LinearLayout(this);
        row0.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : numRow) addKeyButton(row0, key, v -> typeText(key), 1.0f, "#1E1E1E");
        keyboardKeysLayout.addView(row0, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 105));

        String[] row1Keys = isRussian ?
            new String[]{"й","ц","у","к","е","н","г","ш","щ","з","х","ъ"} :
            new String[]{"q","w","e","r","t","y","u","i","o","p"};
        String[] row2Keys = isRussian ?
            new String[]{"ф","ы","в","а","п","р","о","л","д","ж","э"} :
            new String[]{"a","s","d","f","g","h","j","k","l"};
        String[] row3Keys = isRussian ?
            new String[]{"я","ч","с","м","и","т","ь","б","ю"} :
            new String[]{"z","x","c","v","b","n","m"};

        LinearLayout row1 = new LinearLayout(this);
        row1.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : row1Keys) addKeyButton(row1, key.toUpperCase(), v -> typeText(key), 1.0f, "#2A2A2A");
        keyboardKeysLayout.addView(row1, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 125));

        LinearLayout row2 = new LinearLayout(this);
        row2.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : row2Keys) addKeyButton(row2, key.toUpperCase(), v -> typeText(key), 1.0f, "#2A2A2A");
        keyboardKeysLayout.addView(row2, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 125));

        LinearLayout row3 = new LinearLayout(this);
        row3.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : row3Keys) addKeyButton(row3, key.toUpperCase(), v -> typeText(key), 1.0f, "#2A2A2A");
        addKeyButton(row3, "⌫", v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.deleteSurroundingText(1, 0);
        }, 1.5f, "#4A2525");
        keyboardKeysLayout.addView(row3, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 125));

        LinearLayout row4 = new LinearLayout(this);
        row4.setGravity(Gravity.CENTER_HORIZONTAL);
        String langText = isRussian ? "РУС" : "ENG";
        addKeyButton(row4, langText, v -> { isRussian = !isRussian; buildKeyboard(); }, 2.0f, "#1A1A1A");
        addKeyButton(row4, "ПРОБЕЛ", v -> typeText(" "), 5.0f, "#2A2A2A");
        keyboardKeysLayout.addView(row4, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 125));
    }

    private void addKeyButton(LinearLayout parent, String text, View.OnClickListener listener, float weight, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(20);
        btn.setPadding(0, 0, 0, 0);
        btn.setGravity(Gravity.CENTER);
        btn.setAllCaps(false);
        btn.setOnClickListener(listener);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(14);
        shape.setColor(Color.parseColor(colorHex));
        btn.setBackground(shape);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        params.setMargins(3, 4, 3, 4);
        btn.setLayoutParams(params);
        parent.addView(btn);
    }

    private void addMenuButton(LinearLayout parent, String text, View.OnClickListener listener, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor(colorHex));
        btn.setTextSize(11);
        btn.setGravity(Gravity.CENTER);
        btn.setPadding(0, 0, 0, 0);
        btn.setAllCaps(false);
        btn.setOnClickListener(listener);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(14);
        shape.setColor(Color.parseColor("#202020"));
        btn.setBackground(shape);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        params.setMargins(4, 5, 4, 5);
        btn.setLayoutParams(params);
        parent.addView(btn);
    }

    private void typeText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }

    @Override public void onPress(int primaryCode) {}
    @Override public void onRelease(int primaryCode) {}
    @Override public void onKey(int primaryCode, int[] keyCodes) {}
    @Override public void onText(CharSequence text) {}
    @Override public void swipeLeft() {}
    @Override public void swipeRight() {}
    @Override public void swipeDown() {}
    @Override public void swipeUp() {}
}
