package com.example.customkeyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.KeyboardView;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.text.InputType;

public class MyCustomKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private WebView webView;
    private FrameLayout keyboardContainer;
    private LinearLayout mainKeyboardLayout;
    private LinearLayout keyboardKeysLayout;
    private LinearLayout searchBar;
    private LinearLayout miniKeyboardSection;
    private EditText searchInput;

    private boolean isRussian = true;

    @Override
    public View onCreateInputView() {
        // ВАЖНО: WRAP_CONTENT, как в оригинале
        keyboardContainer = new FrameLayout(this);
        keyboardContainer.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.parseColor("#121212"));

        // ===== Меню — ВСЕГДА видимое =====
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.HORIZONTAL);
        menuLayout.setBackgroundColor(Color.parseColor("#1A1A1A"));
        menuLayout.setPadding(4, 4, 4, 4);

        addMenuButton(menuLayout, "YouTube",  v -> openEmbeddedUrl("https://m.youtube.com"),     "#FF5555");
        addMenuButton(menuLayout, "Telegram", v -> openEmbeddedUrl("https://web.telegram.org"), "#229ED9");
        addMenuButton(menuLayout, "WhatsApp", v -> openEmbeddedUrl("https://web.whatsapp.com"), "#25D366");
        addMenuButton(menuLayout, "✕",        v -> closeEmbeddedScreen(),                        "#FF4444");
        addMenuButton(menuLayout, "▼",        v -> requestHideSelf(0),                           "#AAAAAA");

        root.addView(menuLayout, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 90));

        // ===== Область контента — WRAP_CONTENT =====
        FrameLayout contentArea = new FrameLayout(this);

        // Основная клавиатура
        mainKeyboardLayout = new LinearLayout(this);
        mainKeyboardLayout.setOrientation(LinearLayout.VERTICAL);
        keyboardKeysLayout = new LinearLayout(this);
        keyboardKeysLayout.setOrientation(LinearLayout.VERTICAL);
        keyboardKeysLayout.setPadding(2, 4, 2, 4);
        mainKeyboardLayout.addView(keyboardKeysLayout, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        contentArea.addView(mainKeyboardLayout, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // WebView — как в оригинале, ФИКСИРОВАННАЯ высота
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
        // НИКАКОГО setLayerType — он ломает рендер
        contentArea.addView(webView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 820));

        root.addView(contentArea, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        keyboardContainer.addView(root, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Панель поиска — оверлеем снизу (только в режиме браузера)
        searchBar = buildSearchBar();
        searchBar.setVisibility(View.GONE);
        keyboardContainer.addView(searchBar, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.BOTTOM));

        buildKeyboard();
        return keyboardContainer;
    }

    // ===== Панель поиска =====
    private LinearLayout buildSearchBar() {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setBackgroundColor(Color.parseColor("#1A1A1A"));
        wrapper.setPadding(4, 4, 4, 4);

        LinearLayout rowTop = new LinearLayout(this);
        rowTop.setOrientation(LinearLayout.HORIZONTAL);

        searchInput = new EditText(this);
        searchInput.setHint("Поиск...");
        searchInput.setTextColor(Color.WHITE);
        searchInput.setHintTextColor(Color.GRAY);
        searchInput.setBackgroundColor(Color.parseColor("#2C2C2C"));
        searchInput.setPadding(16, 8, 16, 8);
        searchInput.setInputType(InputType.TYPE_NULL);
        searchInput.setShowSoftInputOnFocus(false);
        searchInput.setFocusable(false);
        searchInput.setFocusableInTouchMode(false);
        searchInput.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        Button btnSearch = new Button(this);
        btnSearch.setText("🔍");
        btnSearch.setTextColor(Color.WHITE);
        btnSearch.setBackgroundColor(Color.parseColor("#3D3D3D"));
        btnSearch.setOnClickListener(v -> doSearch());
        LinearLayout.LayoutParams bsParams = new LinearLayout.LayoutParams(110,
            ViewGroup.LayoutParams.MATCH_PARENT);
        bsParams.setMargins(4, 0, 0, 0);
        btnSearch.setLayoutParams(bsParams);

        Button btnToggleMini = new Button(this);
        btnToggleMini.setText("⌨");
        btnToggleMini.setTextColor(Color.WHITE);
        btnToggleMini.setBackgroundColor(Color.parseColor("#3D3D3D"));
        btnToggleMini.setOnClickListener(v -> {
            if (miniKeyboardSection.getVisibility() == View.VISIBLE) {
                miniKeyboardSection.setVisibility(View.GONE);
            } else {
                miniKeyboardSection.setVisibility(View.VISIBLE);
            }
        });
        LinearLayout.LayoutParams btParams = new LinearLayout.LayoutParams(110,
            ViewGroup.LayoutParams.MATCH_PARENT);
        btParams.setMargins(4, 0, 0, 0);
        btnToggleMini.setLayoutParams(btParams);

        rowTop.addView(searchInput);
        rowTop.addView(btnSearch);
        rowTop.addView(btnToggleMini);
        wrapper.addView(rowTop, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 70));

        miniKeyboardSection = new LinearLayout(this);
        miniKeyboardSection.setOrientation(LinearLayout.VERTICAL);
        rebuildMiniKeyboard();
        wrapper.addView(miniKeyboardSection, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return wrapper;
    }

    private void rebuildMiniKeyboard() {
        if (miniKeyboardSection == null) return;
        miniKeyboardSection.removeAllViews();

        String[][] rows = new String[][]{
            {"1","2","3","4","5","6","7","8","9","0"},
            {"q","w","e","r","t","y","u","i","o","p"},
            {"a","s","d","f","g","h","j","k","l"},
            {"z","x","c","v","b","n","m"}
        };

        for (String[] row : rows) {
            LinearLayout r = new LinearLayout(this);
            r.setOrientation(LinearLayout.HORIZONTAL);
            r.setGravity(Gravity.CENTER);
            for (String k : row) {
                r.addView(makeMiniButton(k, 1f, () -> onMiniKey(k)));
            }
            miniKeyboardSection.addView(r, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 55));
        }

        LinearLayout bottomRow = new LinearLayout(this);
        bottomRow.setOrientation(LinearLayout.HORIZONTAL);
        bottomRow.setGravity(Gravity.CENTER);
        bottomRow.addView(makeMiniButton("ПРОБЕЛ", 4f, () -> onMiniKey(" ")));
        bottomRow.addView(makeMiniButton("⌫", 1.5f, () -> onMiniKey("⌫")));
        bottomRow.addView(makeMiniButton("OK", 1.5f, () -> doSearch()));
        miniKeyboardSection.addView(bottomRow, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 55));
    }

    private Button makeMiniButton(String text, float weight, Runnable onClick) {
        Button b = new Button(this);
        b.setText(text);
        b.setTextSize(12);
        b.setTextColor(Color.WHITE);
        b.setPadding(0, 0, 0, 0);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(8);
        shape.setColor(Color.parseColor("#3D3D3D"));
        b.setBackground(shape);
        b.setOnClickListener(v -> onClick.run());
        LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        p.setMargins(2, 2, 2, 2);
        b.setLayoutParams(p);
        return b;
    }

    private void onMiniKey(String key) {
        if (key.equals("⌫")) {
            String t = searchInput.getText().toString();
            if (t.length() > 0) {
                searchInput.setText(t.substring(0, t.length() - 1));
                searchInput.setSelection(searchInput.getText().length());
            }
        } else if (key.equals(" ")) {
            searchInput.append(" ");
        } else {
            searchInput.append(key);
        }
    }

    private void doSearch() {
        String q = searchInput.getText().toString().trim();
        if (q.isEmpty()) return;
        String url = webView.getUrl();
        if (url != null && url.contains("youtube")) {
            String js =
                "(function(){" +
                "var i=document.querySelector('input[name=search_query]')" +
                "||document.querySelector('input[name=q]')" +
                "||document.querySelector('input[type=search]');" +
                "if(i){i.value=" + jsString(q) + ";" +
                "var f=i.form;if(f){f.submit();return 'ok';}}" +
                "return 'noinput';})()";
            webView.evaluateJavascript(js, null);
        } else {
            webView.loadUrl("https://www.google.com/search?q=" + q);
        }
    }

    private String jsString(String s) {
        return "'" + s.replace("\\", "\\\\")
                      .replace("'", "\\'")
                      .replace("\n", "\\n") + "'";
    }

    private void openEmbeddedUrl(String url) {
        mainKeyboardLayout.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        searchBar.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }

    private void closeEmbeddedScreen() {
        webView.setVisibility(View.GONE);
        searchBar.setVisibility(View.GONE);
        mainKeyboardLayout.setVisibility(View.VISIBLE);
        webView.stopLoading();
        webView.loadUrl("about:blank");
        searchInput.setText("");
    }

    // ===== Основная клавиатура =====
    private void buildKeyboard() {
        if (keyboardKeysLayout == null) return;
        keyboardKeysLayout.removeAllViews();

        String[] numRow = {"1","2","3","4","5","6","7","8","9","0"};
        LinearLayout row0 = new LinearLayout(this);
        row0.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : numRow) addKeyButton(row0, key, v -> typeText(key), 1.0f, "#252525");
        keyboardKeysLayout.addView(row0, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 80));

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
        for (String key : row1Keys) addKeyButton(row1, key.toUpperCase(), v -> typeText(key), 1.0f, "#2C2C2C");
        keyboardKeysLayout.addView(row1, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 90));

        LinearLayout row2 = new LinearLayout(this);
        row2.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : row2Keys) addKeyButton(row2, key.toUpperCase(), v -> typeText(key), 1.0f, "#2C2C2C");
        keyboardKeysLayout.addView(row2, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 90));

        LinearLayout row3 = new LinearLayout(this);
        row3.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : row3Keys) addKeyButton(row3, key.toUpperCase(), v -> typeText(key), 1.0f, "#2C2C2C");
        addKeyButton(row3, "⌫", v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.deleteSurroundingText(1, 0);
        }, 1.5f, "#3D2525");
        keyboardKeysLayout.addView(row3, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 90));

        LinearLayout row4 = new LinearLayout(this);
        row4.setGravity(Gravity.CENTER_HORIZONTAL);
        String langText = isRussian ? "РУС" : "ENG";
        addKeyButton(row4, langText, v -> {
            isRussian = !isRussian;
            buildKeyboard();
        }, 2.0f, "#1F1F1F");
        addKeyButton(row4, "ПРОБЕЛ", v -> typeText(" "), 5.0f, "#2C2C2C");
        keyboardKeysLayout.addView(row4, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 90));
    }

    private void addKeyButton(LinearLayout parent, String text, View.OnClickListener listener, float weight, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(16);
        btn.setPadding(0, 0, 0, 0);
        btn.setGravity(Gravity.CENTER);
        btn.setOnClickListener(listener);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(10);
        shape.setColor(Color.parseColor(colorHex));
        btn.setBackground(shape);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        params.setMargins(2, 3, 2, 3);
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
        btn.setOnClickListener(listener);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(10);
        shape.setColor(Color.parseColor("#222222"));
        btn.setBackground(shape);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        params.setMargins(3, 3, 3, 3);
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
