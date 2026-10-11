package com.example.customkeyboard;

import android.content.Context;
import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.KeyboardView;
import android.net.ConnectivityManager;
import android.net.DhcpInfo;
import android.net.NetworkCapabilities;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.BatteryManager;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MyCustomKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private WebView webView;
    private FrameLayout keyboardContainer;
    private LinearLayout mainKeyboardLayout;
    private LinearLayout keyboardKeysLayout;
    private LinearLayout miniKeyboard;
    private LinearLayout hackerScreen;
    private TextView hackerText;
    private ScrollView hackerScroll;

    private boolean isRussian = true;
    private boolean miniRussian = false;

    private Handler hackerHandler = new Handler(Looper.getMainLooper());
    private SimpleDateFormat timeFmt = new SimpleDateFormat("HH:mm:ss", Locale.US);
    private ExecutorService executor = Executors.newFixedThreadPool(32);

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
        addMenuButton(menuLayout, "HACK",     v -> openHackerScreen(),                   "#00FF66");
        addMenuButton(menuLayout, "✕",        v -> closeWeb(),                          "#FF4444");

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
        int webH = (int)(screenH * 0.60);

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
            ViewGroup.LayoutParams.MATCH_PARENT, webH));

        hackerScreen = buildHackerScreen();
        hackerScreen.setVisibility(View.GONE);
        contentArea.addView(hackerScreen, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, webH));

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

    // ===== Hacker terminal =====
    private LinearLayout buildHackerScreen() {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setBackgroundColor(Color.parseColor("#000000"));

        LinearLayout topBar = new LinearLayout(this);
        topBar.setOrientation(LinearLayout.HORIZONTAL);
        topBar.setBackgroundColor(Color.parseColor("#0A1A0A"));
        topBar.setPadding(10, 6, 6, 6);
        topBar.setGravity(Gravity.CENTER_VERTICAL);

        TextView title = new TextView(this);
        title.setText("root@super-keyboard:~# id");
        title.setTextColor(Color.parseColor("#00FF66"));
        title.setTextSize(11);
        title.setTypeface(Typeface.MONOSPACE);
        title.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        topBar.addView(title);

        Button rerunBtn = makeHackerTopBtn("⟳", v -> startHackerFeed());
        topBar.addView(rerunBtn);

        Button scanBtn = makeHackerTopBtn("📡", v -> runNetworkScan());
        topBar.addView(scanBtn);

        Button closeBtn = makeHackerTopBtn("✕", v -> closeHackerScreen());
        closeBtn.setTextColor(Color.parseColor("#FF4444"));
        topBar.addView(closeBtn);

        wrapper.addView(topBar, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 60));

        hackerScroll = new ScrollView(this);
        hackerScroll.setBackgroundColor(Color.parseColor("#000000"));
        hackerScroll.setPadding(14, 10, 14, 10);

        hackerText = new TextView(this);
        hackerText.setTextColor(Color.parseColor("#00FF66"));
        hackerText.setTextSize(12);
        hackerText.setTypeface(Typeface.MONOSPACE);
        hackerText.setText("");
        hackerScroll.addView(hackerText);

        wrapper.addView(hackerScroll, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        return wrapper;
    }

    private Button makeHackerTopBtn(String text, View.OnClickListener l) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextColor(Color.parseColor("#00FF66"));
        b.setTextSize(14);
        b.setPadding(0, 0, 0, 0);
        b.setAllCaps(false);
        GradientDrawable s = new GradientDrawable();
        s.setCornerRadius(10);
        s.setColor(Color.parseColor("#102010"));
        s.setStroke(2, Color.parseColor("#005522"));
        b.setBackground(s);
        b.setOnClickListener(l);
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(60, 50);
        p.setMargins(4, 0, 0, 0);
        b.setLayoutParams(p);
        return b;
    }

    private void openHackerScreen() {
        mainKeyboardLayout.setVisibility(View.GONE);
        webView.setVisibility(View.GONE);
        miniKeyboard.setVisibility(View.GONE);
        hackerScreen.setVisibility(View.VISIBLE);
        hackerText.setText("");
        startHackerFeed();
    }

    private void closeHackerScreen() {
        hackerScreen.setVisibility(View.GONE);
        mainKeyboardLayout.setVisibility(View.VISIBLE);
    }

    private void append(String line) {
        hackerHandler.post(() -> {
            String stamped = "[" + timeFmt.format(new Date()) + "] " + line;
            CharSequence cur = hackerText.getText();
            String combined = cur.length() > 0 ? cur + "\n" + stamped : stamped;
            String[] arr = combined.split("\n");
            if (arr.length > 300) {
                StringBuilder sb = new StringBuilder();
                for (int i = arr.length - 300; i < arr.length; i++) {
                    if (sb.length() > 0) sb.append("\n");
                    sb.append(arr[i]);
                }
                combined = sb.toString();
            }
            hackerText.setText(combined);
            hackerScroll.post(() -> hackerScroll.fullScroll(View.FOCUS_DOWN));
        });
    }

    private void startHackerFeed() {
        hackerText.setText("");
        append("initializing...");
        executor.execute(() -> {
            sleep(200); append("uid=0(root) gid=0(root) groups=0(root)");
            sleep(150); append("");
            sleep(150); append("=== SYSTEM ===");
            appendSysInfo();
            sleep(150); append("");
            append("=== NETWORK ===");
            appendNetInfo();
            sleep(150); append("");
            append("=== CONNECTIVITY ===");
            appendPublicIp();
            appendDnsLookup("google.com");
            appendDnsLookup("youtube.com");
            appendPing();
            sleep(150); append("");
            append("[✓] done. use 📡 to scan local network");
        });
    }

    // ===== Реальная инфа о системе =====
    private void appendSysInfo() {
        append("model    : " + android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL);
        append("android  : " + android.os.Build.VERSION.RELEASE);
        append("sdk      : " + android.os.Build.VERSION.SDK_INT);
        append("kernel   : " + System.getProperty("os.version"));
        append("arch     : " + System.getProperty("os.arch"));

        try {
            BatteryManager bm = (BatteryManager) getSystemService(Context.BATTERY_SERVICE);
            int level = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            append("battery  : " + level + "%");
        } catch (Exception e) {
            append("battery  : n/a");
        }

        Runtime rt = Runtime.getRuntime();
        long totalMb = rt.totalMemory() / 1024 / 1024;
        long freeMb = rt.freeMemory() / 1024 / 1024;
        long usedMb = totalMb - freeMb;
        append("jvm mem  : " + usedMb + " / " + totalMb + " MB");
    }

    // ===== Реальная инфа о сети =====
    private void appendNetInfo() {
        try {
            ConnectivityManager cm = (ConnectivityManager)
                getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkCapabilities nc = cm.getNetworkCapabilities(cm.getActiveNetwork());
            if (nc != null) {
                String type = "unknown";
                if (nc.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) type = "wifi";
                else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) type = "cellular";
                else if (nc.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) type = "ethernet";
                append("type     : " + type);
            }

            // Local IP
            String localIp = getLocalIp();
            append("local ip : " + (localIp != null ? localIp : "n/a"));

            // Wi-Fi SSID
            try {
                WifiManager wm = (WifiManager) getApplicationContext()
                    .getSystemService(Context.WIFI_SERVICE);
                WifiInfo wi = wm.getConnectionInfo();
                if (wi != null && wi.getSSID() != null) {
                    String ssid = wi.getSSID().replace("\"", "");
                    append("ssid     : " + ssid);
                }
            } catch (Exception e) {
                append("ssid     : n/a (need permission)");
            }

            // MAC
            String mac = getMacAddress();
            append("mac      : " + (mac != null ? mac : "n/a"));
        } catch (Exception e) {
            append("net err  : " + e.getMessage());
        }
    }

    private String getLocalIp() {
        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            while (ifs.hasMoreElements()) {
                NetworkInterface ni = ifs.nextElement();
                if (!ni.isUp() || ni.isLoopback()) continue;
                Enumeration<InetAddress> addrs = ni.getInetAddresses();
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

    private String getMacAddress() {
        try {
            Enumeration<NetworkInterface> ifs = NetworkInterface.getNetworkInterfaces();
            while (ifs.hasMoreElements()) {
                NetworkInterface ni = ifs.nextElement();
                if (ni.getName().equals("wlan0") || ni.getName().startsWith("wlan")) {
                    byte[] mac = ni.getHardwareAddress();
                    if (mac == null) continue;
                    StringBuilder sb = new StringBuilder();
                    for (int i = 0; i < mac.length; i++) {
                        sb.append(String.format("%02X%s", mac[i],
                            (i < mac.length - 1) ? ":" : ""));
                    }
                    return sb.toString();
                }
            }
        } catch (Exception ignored) {}
        return null;
    }

    // ===== Public IP =====
    private void appendPublicIp() {
        try {
            URL url = new URL("https://api.ipify.org");
            HttpURLConnection c = (HttpURLConnection) url.openConnection();
            c.setConnectTimeout(5000);
            c.setReadTimeout(5000);
            BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream()));
            String ip = r.readLine();
            r.close();
            append("public ip: " + (ip != null ? ip : "n/a"));
        } catch (Exception e) {
            append("public ip: failed (" + e.getMessage() + ")");
        }
    }

    // ===== DNS lookup =====
    private void appendDnsLookup(String host) {
        try {
            InetAddress[] addrs = InetAddress.getAllByName(host);
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < addrs.length && i < 3; i++) {
                if (i > 0) sb.append(", ");
                sb.append(addrs[i].getHostAddress());
            }
            append("dns " + pad(host, 12) + ": " + sb);
        } catch (Exception e) {
            append("dns " + host + " : failed");
        }
    }

    // ===== Ping =====
    private void appendPing() {
        try {
            InetAddress addr = InetAddress.getByName("8.8.8.8");
            long start = System.currentTimeMillis();
            boolean ok = addr.isReachable(3000);
            long ms = System.currentTimeMillis() - start;
            if (ok) {
                append("ping 8.8.8.8 : OK  " + ms + "ms");
            } else {
                // fallback через Runtime ping
                Process p = Runtime.getRuntime().exec("ping -c 1 -W 2 8.8.8.8");
                int exit = p.waitFor();
                if (exit == 0) {
                    append("ping 8.8.8.8 : OK (via shell)");
                } else {
                    append("ping 8.8.8.8 : FAILED");
                }
            }
        } catch (Exception e) {
            append("ping 8.8.8.8 : failed");
        }
    }

    // ===== Скан локальной сети =====
    private void runNetworkScan() {
        append("");
        append("=== LAN SCAN ===");
        String myIp = getLocalIp();
        if (myIp == null || !myIp.contains(".")) {
            append("no local ip, abort");
            return;
        }
        String base = myIp.substring(0, myIp.lastIndexOf('.'));
        append("scanning " + base + ".0/24 ...");
        append("this may take ~10 sec");

        final List<String> found = Collections.synchronizedList(new ArrayList<>());
        final int[] done = {0};

        for (int i = 1; i <= 254; i++) {
            final String ip = base + "." + i;
            executor.execute(() -> {
                try {
                    InetAddress a = InetAddress.getByName(ip);
                    if (a.isReachable(600)) {
                        String host = a.getCanonicalHostName();
                        if (host == null || host.equals(ip)) host = "";
                        found.add(ip + (host.isEmpty() ? "" : "  " + host));
                    }
                } catch (Exception ignored) {}
                synchronized (done) {
                    done[0]++;
                    if (done[0] == 254) {
                        append("scan complete. found " + found.size() + " device(s):");
                        if (found.isEmpty()) {
                            append("  (нет ответов — возможно Wi-Fi блокирует)");
                        } else {
                            for (String f : found) append("  > " + f);
                        }
                    }
                }
            });
        }
    }

    private void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }

    private String pad(String s, int n) {
        StringBuilder sb = new StringBuilder(s);
        while (sb.length() < n) sb.append(' ');
        return sb.toString();
    }

    // ===== Мини-клавиатура =====
    private LinearLayout buildMiniKeyboard() {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setBackgroundColor(Color.parseColor("#0A0A0A"));
        wrapper.setPadding(4, 4, 4, 6);

        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);

        Button hideBtn = new Button(this);
        hideBtn.setText("⌄");
        hideBtn.setTextColor(Color.parseColor("#00FF66"));
        hideBtn.setTextSize(16);
        hideBtn.setPadding(0, 0, 0, 0);
        hideBtn.setAllCaps(false);
        GradientDrawable hs = new GradientDrawable();
        hs.setCornerRadius(10);
        hs.setColor(Color.parseColor("#101A10"));
        hs.setStroke(2, Color.parseColor("#005522"));
        hideBtn.setBackground(hs);
        hideBtn.setOnClickListener(v -> {
            int vis = wrapper.getChildAt(1).getVisibility() == View.VISIBLE
                    ? View.GONE : View.VISIBLE;
            for (int i = 1; i < wrapper.getChildCount(); i++)
                wrapper.getChildAt(i).setVisibility(vis);
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
        cs.setColor(Color.parseColor("#101A10"));
        cs.setStroke(2, Color.parseColor("#005522"));
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
            for (String k : row) {
                r.addView(makeMiniKey(k, 1f, () -> onMiniKey(k)));
            }
            wrapper.addView(r, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 66));
        }

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.CENTER);

        bottomRow.addView(makeMiniKey(miniRussian ? "РУС" : "ENG", 1.3f, () -> {
            miniRussian = !miniRussian;
            LinearLayout parent = miniKeyboard;
            parent.removeAllViews();
            LinearLayout rebuilt = buildMiniKeyboard();
            while (rebuilt.getChildCount() > 0) {
                View c = rebuilt.getChildAt(0);
                rebuilt.removeViewAt(0);
                parent.addView(c);
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
        b.setTextColor(Color.parseColor("#00FF66"));
        b.setPadding(0, 0, 0, 0);
        b.setAllCaps(false);
        b.setGravity(Gravity.CENTER);
        b.setTypeface(Typeface.MONOSPACE);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(10);
        shape.setColor(Color.parseColor("#0A140A"));
        shape.setStroke(2, Color.parseColor("#005522"));
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

    private void openWeb(String url) {
        hackerScreen.setVisibility(View.GONE);
        mainKeyboardLayout.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        miniKeyboard.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }

    private void closeWeb() {
        webView.setVisibility(View.GONE);
        miniKeyboard.setVisibility(View.GONE);
        hackerScreen.setVisibility(View.GONE);
        mainKeyboardLayout.setVisibility(View.VISIBLE);
        webView.stopLoading();
        webView.loadUrl("about:blank");
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
        addKeyButton(row4, langText, v -> {
            isRussian = !isRussian;
            buildKeyboard();
        }, 2.0f, "#1A1A1A");
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
        btn.setTypeface(Typeface.MONOSPACE);
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
