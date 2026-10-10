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
    private EditText searchInput;
    private Button btnClose;

    private boolean isRussian = true;

    @Override
    public View onCreateInputView() {
        keyboardContainer = new FrameLayout(this);
        keyboardContainer.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));

        // === Основной макет клавиатуры ===
        mainKeyboardLayout = new LinearLayout(this);
        mainKeyboardLayout.setOrientation(LinearLayout.VERTICAL);
        mainKeyboardLayout.setBackgroundColor(Color.parseColor("#121212"));

        // Верхнее меню
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.HORIZONTAL);
        menuLayout.setBackgroundColor(Color.parseColor("#1A1A1A"));
        menuLayout.setPadding(4, 4, 4, 4);

        addMenuButton(menuLayout, "YouTube",  v -> openEmbeddedUrl("https://m.youtube.com"),        "#FF0000");
        addMenuButton(menuLayout, "Telegram", v -> openEmbeddedUrl("https://web.telegram.org"),    "#229ED9");
        addMenuButton(menuLayout, "WhatsApp", v -> openEmbeddedUrl("https://web.whatsapp.com"),    "#25D366");

        mainKeyboardLayout.addView(menuLayout, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 80));

        // Контейнер букв
        keyboardKeysLayout = new LinearLayout(this);
        keyboardKeysLayout.setOrientation(LinearLayout.VERTICAL);
        keyboardKeysLayout.setPadding(2, 4, 2, 4);
        mainKeyboardLayout.addView(keyboardKeysLayout, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // === WebView ===
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setUserAgentString(
            "Mozilla/5.0 (Linux; Android 10) AppleWebKit/537.36 (KHTML, like Gecko) " +
            "Chrome/120.0.0.0 Mobile Safari/537.36");
        webView.setWebViewClient(new WebViewClient());
        webView.setVisibility(View.GONE);

        // === Кнопка закрытия ===
        btnClose = new Button(this);
        btnClose.setText("✕");
        btnClose.setTextColor(Color.RED);
        btnClose.setTextSize(18);
        btnClose.setBackgroundColor(Color.parseColor("#99000000"));
        btnClose.setOnClickListener(v -> closeEmbeddedScreen());

        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(90, 90);
        closeParams.gravity = Gravity.TOP | Gravity.RIGHT;
        closeParams.setMargins(0, 8, 8, 0);

        // === Панель поиска с мини-клавиатурой ===
        searchBar = buildSearchBar();
        searchBar.setVisibility(View.GONE);

        // === Сборка ===
        keyboardContainer.addView(mainKeyboardLayout, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        keyboardContainer.addView(webView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        keyboardContainer.addView(searchBar, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 170, Gravity.BOTTOM));

        keyboardContainer.addView(btnClose, closeParams);

        buildKeyboard();
        return keyboardContainer;
    }

    // === Панель поиска: строка ввода + мини-клавиатура ===
    private LinearLayout buildSearchBar() {
        LinearLayout wrapper = new LinearLayout(this);
        wrapper.setOrientation(LinearLayout.VERTICAL);
        wrapper.setBackgroundColor(Color.parseColor("#1A1A1A"));

        // Верхний ряд: поле ввода + кнопка поиска
        LinearLayout rowTop = new LinearLayout(this);
        rowTop.setOrientation(LinearLayout.HORIZONTAL);
        rowTop.setPadding(6, 6, 6, 4);

        searchInput = new EditText(this);
        searchInput.setHint("Поиск...");
        searchInput.setTextColor(Color.WHITE);
        searchInput.setHintTextColor(Color.GRAY);
        searchInput.setBackgroundColor(Color.parseColor("#2C2C2C"));
        searchInput.setPadding(16, 8, 16, 8);
        // Отключаем системную клавиатуру, чтобы не было рекурсии
        searchInput.setInputType(InputType.TYPE_NULL);
        searchInput.setShowSoftInputOnFocus(false);
        searchInput.setFocusableInTouchMode(false);
        searchInput.setLayoutParams(new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 1f));

        Button btnSearch = new Button(this);
        btnSearch.setText("🔍");
        btnSearch.setTextColor(Color.WHITE);
        btnSearch.setBackgroundColor(Color.parseColor("#3D3D3D"));
        btnSearch.setOnClickListener(v -> doSearch());
        LinearLayout.LayoutParams bsParams = new LinearLayout.LayoutParams(140,
            ViewGroup.LayoutParams.MATCH_PARENT);
        bsParams.setMargins(6, 0, 0, 0);
        btnSearch.setLayoutParams(bsParams);

        rowTop.addView(searchInput);
        rowTop.addView(btnSearch);
        wrapper.addView(rowTop, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 80));

        // Нижний ряд: мини-клавиатура для поиска
        LinearLayout miniKeys = new LinearLayout(this);
        miniKeys.setOrientation(LinearLayout.HORIZONTAL);
        miniKeys.setPadding(4, 2, 4, 4);
        miniKeys.setGravity(Gravity.CENTER);

        String[] miniRow = {"q","w","e","r","t","y","u","i","o","p",
                            "a","s","d","f","g","h","j","k","l",
                            "⌫", "␣", "OK"};
        for (String k : miniRow) {
            Button b = new Button(this);
            b.setText(k);
            b.setTextSize(12);
            b.setTextColor(Color.WHITE);
            b.setPadding(0, 0, 0, 0);
            GradientDrawable shape = new GradientDrawable();
            shape.setCornerRadius(8);
            shape.setColor(Color.parseColor("#2C2C2C"));
            b.setBackground(shape);
            b.setOnClickListener(v -> onMiniKey(k));
            LinearLayout.LayoutParams p = new LinearLayout.LayoutParams(
                0, ViewGroup.LayoutParams.MATCH_PARENT, 1f);
            p.setMargins(2, 2, 2, 2);
            b.setLayoutParams(p);
            miniKeys.addView(b);
        }
        wrapper.addView(miniKeys, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 80));

        return wrapper;
    }

    private void onMiniKey(String key) {
        switch (key) {
            case "⌫": {
                String t = searchInput.getText().toString();
                if (t.length() > 0)
                    searchInput.setText(t.substring(0, t.length() - 1));
                searchInput.setSelection(searchInput.getText().length());
                break;
            }
            case "␣": {
                searchInput.append(" ");
                break;
            }
            case "OK": {
                doSearch();
                break;
            }
            default: {
                searchInput.append(key);
                break;
            }
        }
    }

    // Поиск: вставляем текст в поле YouTube и отправляем форму
    private void doSearch() {
        String q = searchInput.getText().toString().trim();
        if (q.isEmpty()) return;
        // Для YouTube
        if (webView.getUrl() != null && webView.getUrl().contains("youtube")) {
            String js =
                "(function(){" +
                "var i=document.querySelector('input[name=search_query]')" +
                "||document.querySelector('input[name=q]')" +
                "||document.querySelector('input[type=search]');" +
                "if(i){i.value=" + jsString(q) + ";" +
                "var f=i.form;if(f){f.submit();return 'ok';}" +
                "var e=new KeyboardEvent('keydown',{key:'Enter',keyCode:13,bubbles:true});" +
                "i.dispatchEvent(e);return 'enter';}" +
                "return 'noinput';})()";
            webView.evaluateJavascript(js, null);
        } else {
            // Для остальных сайтов — просто грузим поиск Google
            webView.loadUrl("https://www.google.com/search?q=" + q);
        }
    }

    // Экранирование строки для JS
    private String jsString(String s) {
        return "'" + s.replace("\\", "\\\\")
                      .replace("'", "\\'")
                      .replace("\n", "\\n") + "'";
    }

    private void openEmbeddedUrl(String url) {
        mainKeyboardLayout.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        searchBar.setVisibility(View.VISIBLE);
        btnClose.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }

    private void closeEmbeddedScreen() {
        webView.setVisibility(View.GONE);
        searchBar.setVisibility(View.GONE);
        btnClose.setVisibility(View.GONE);
        mainKeyboardLayout.setVisibility(View.VISIBLE);
        webView.stopLoading();
        webView.loadUrl("about:blank");
        searchInput.setText("");
    }

    // === Основная клавиатура ===
    private void buildKeyboard() {
        if (keyboardKeysLayout == null) return;
        keyboardKeysLayout.removeAllViews();

        String[] row1Keys = isRussian ?
            new String[]{"й","ц","у","к","е","н","г","ш","щ","з"} :
            new String[]{"q","w","e","r","t","y","u","i","o","p"};

        String[] row2Keys = isRussian ?
            new String[]{"ф","ы","в","а","п","р","о","л","д","ж","э"} :
            new String[]{"a","s","d","f","g","h","j","k","l"};

        String[] row3Keys = isRussian ?
            new String[]{"я","ч","с","м","и","т","ь","б","ю","х","ъ"} :
            new String[]{"z","x","c","v","b","n","m"};

        LinearLayout row1 = new LinearLayout(this);
        row1.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : row1Keys) addKeyButton(row1, key.toUpperCase(), v -> typeText(key), 1.0f, "#2C2C2C");
        keyboardKeysLayout.addView(row1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100));

        LinearLayout row2 = new LinearLayout(this);
        row2.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : row2Keys) addKeyButton(row2, key.toUpperCase(), v -> typeText(key), 1.0f, "#2C2C2C");
        keyboardKeysLayout.addView(row2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100));

        LinearLayout row3 = new LinearLayout(this);
        row3.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : row3Keys) addKeyButton(row3, key.toUpperCase(), v -> typeText(key), 1.0f, "#2C2C2C");
        addKeyButton(row3, "⌫", v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.deleteSurroundingText(1, 0);
        }, 1.5f, "#3D2525");
        keyboardKeysLayout.addView(row3, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100));

        LinearLayout row4 = new LinearLayout(this);
        row4.setGravity(Gravity.CENTER_HORIZONTAL);
        String langText = isRussian ? "РУС" : "ENG";
        addKeyButton(row4, langText, v -> {
            isRussian = !isRussian;
            buildKeyboard();
        }, 2.0f, "#1F1F1F");
        addKeyButton(row4, "ПРОБЕЛ", v -> typeText(" "), 5.0f, "#2C2C2C");
        keyboardKeysLayout.addView(row4, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 100));
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

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        params.setMargins(2, 3, 2, 3);
        btn.setLayoutParams(params);
        parent.addView(btn);
    }

    private void addMenuButton(LinearLayout parent, String text, View.OnClickListener listener, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor(colorHex));
        btn.setTextSize(12);
        btn.setGravity(Gravity.CENTER);
        btn.setOnClickListener(listener);

        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(10);
        shape.setColor(Color.parseColor("#222222"));
        btn.setBackground(shape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        params.setMargins(4, 4, 4, 4);
        btn.setLayoutParams(params);
        parent.addView(btn);
    }

    private void typeText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }

    // Просим систему дать максимум места под IME
    @Override
    public boolean onEvaluateFullscreenMode() {
        return true;
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
