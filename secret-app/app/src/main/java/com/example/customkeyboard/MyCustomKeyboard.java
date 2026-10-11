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
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;

public class MyCustomKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private WebView webView;
    private FrameLayout keyboardContainer;
    private LinearLayout mainKeyboardLayout;
    private LinearLayout keyboardKeysLayout;
    private LinearLayout miniKeyboard;

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

        // ===== Меню =====
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.HORIZONTAL);
        menuLayout.setBackgroundColor(Color.parseColor("#151515"));
        menuLayout.setPadding(6, 6, 6, 6);

        addMenuButton(menuLayout, "YouTube",  v -> openWeb("https://m.youtube.com"),     "#FF5555");
        addMenuButton(menuLayout, "Telegram", v -> openWeb("https://web.telegram.org"), "#229ED9");
        addMenuButton(menuLayout, "Google",   v -> openWeb("https://www.google.com"),   "#FBBC05");
        addMenuButton(menuLayout, "✕",        v -> closeWeb(),                          "#FF4444");

        rootLayout.addView(menuLayout, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 100));

        // ===== Область контента =====
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

    private void openWeb(String url) {
        mainKeyboardLayout.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        miniKeyboard.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }

    private void closeWeb() {
        webView.setVisibility(View.GONE);
        miniKeyboard.setVisibility(View.GONE);
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
        btn.setTextSize(12);
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
        params.setMargins(5, 5, 5, 5);
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
