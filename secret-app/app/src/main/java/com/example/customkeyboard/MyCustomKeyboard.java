package com.example.customkeyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.KeyboardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;

public class MyCustomKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private WebView webView;
    private FrameLayout keyboardContainer;
    private FrameLayout gameContainer;
    private LinearLayout keyboardLayout;
    private LinearLayout keyboardKeysLayout;
    
    private boolean isRussian = true; // Русский по умолчанию

    @Override
    public View onCreateInputView() {
        keyboardContainer = new FrameLayout(this);
        
        LayoutInflater inflater = getLayoutInflater();
        int layoutId = getResources().getIdentifier("keyboard_main_layout", "layout", getPackageName());
        keyboardLayout = (LinearLayout) inflater.inflate(layoutId, null);
        
        // Находим контейнер для кнопок букв внутри XML
        int keysLayoutId = getResources().getIdentifier("keyboard_keys_layout", "id", getPackageName());
        keyboardKeysLayout = keyboardLayout.findViewById(keysLayoutId);

        // Контейнер для встроенного YouTube и Игр
        gameContainer = new FrameLayout(this);
        gameContainer.setVisibility(View.GONE);
        gameContainer.setBackgroundColor(Color.BLACK);
        
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        gameContainer.addView(webView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ));
        
        // Кнопка закрытия Ютуба/Игр (Крестик)
        Button btnClose = new Button(this);
        btnClose.setText("✕");
        btnClose.setTextColor(Color.RED);
        btnClose.setTextSize(18);
        btnClose.setBackgroundColor(Color.TRANSPARENT);
        btnClose.setOnClickListener(v -> closeEmbeddedScreen());
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(120, 120);
        closeParams.gravity = Gravity.TOP | Gravity.RIGHT;
        gameContainer.addView(btnClose, closeParams);

        keyboardContainer.addView(keyboardLayout);
        keyboardContainer.addView(gameContainer, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 650 // Оптимальная высота для игр и видео
        ));

        setupMenuButtons(keyboardLayout);
        buildKeyboard();

        return keyboardContainer;
    }

    private void setupMenuButtons(View layout) {
        setupMenuButton(layout, "btn_youtube", v -> openEmbeddedUrl("https://m.youtube.com"));
        setupMenuButton(layout, "btn_clicker", v -> openEmbeddedUrl("https://orteil.dashnet.org/cookieclicker/")); 
        setupMenuButton(layout, "btn_tetris", v -> openEmbeddedUrl("https://www.tetris.com")); // Временные ссылки для теста обхода блокировок
        setupMenuButton(layout, "btn_snake", v -> openEmbeddedUrl("https://www.google.com/search?q=play+snake"));
    }

    private void setupMenuButton(View layout, String idName, View.OnClickListener listener) {
        int id = getResources().getIdentifier(idName, "id", getPackageName());
        View button = layout.findViewById(id);
        if (button != null) {
            button.setOnClickListener(listener);
        }
    }

    private void openEmbeddedUrl(String url) {
        keyboardLayout.setVisibility(View.GONE);
        gameContainer.setVisibility(View.VISIBLE);
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }

    private void closeEmbeddedScreen() {
        gameContainer.setVisibility(View.GONE);
        keyboardLayout.setVisibility(View.VISIBLE);
        webView.stopLoading();
        webView.setVisibility(View.GONE);
    }

    // --- ГАРАНТИРОВАННАЯ КРАСИВАЯ РАСКЛАДКА БЕЗ ТОЧЕК ---
    private void buildKeyboard() {
        if (keyboardKeysLayout == null) return;
        keyboardKeysLayout.removeAllViews();

        // Оптимальное распределение букв, чтобы всё помещалось на любом экране
        String[] row1Keys = isRussian ? 
            new String[]{"й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з"} : 
            new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"};

        String[] row2Keys = isRussian ? 
            new String[]{"ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"} : 
            new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l"};

        String[] row3Keys = isRussian ? 
            new String[]{"я", "ч", "с", "м", "и", "т", "ь", "б", "ю", "х", "ъ"} : 
            new String[]{"z", "x", "c", "v", "b", "n", "m"};

        // Ряд 1
        LinearLayout row1 = new LinearLayout(this);
        row1.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : row1Keys) {
            addKeyButton(row1, key.toUpperCase(), v -> typeText(key), 1.0f, "#2C2C2C");
        }
        keyboardKeysLayout.addView(row1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120));

        // Ряд 2
        LinearLayout row2 = new LinearLayout(this);
        row2.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : row2Keys) {
            addKeyButton(row2, key.toUpperCase(), v -> typeText(key), 1.0f, "#2C2C2C");
        }
        keyboardKeysLayout.addView(row2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120));

        // Ряд 3 + Стереть
        LinearLayout row3 = new LinearLayout(this);
        row3.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : row3Keys) {
            addKeyButton(row3, key.toUpperCase(), v -> typeText(key), 1.0f, "#2C2C2C");
        }
        addKeyButton(row3, "⌫", v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.deleteSurroundingText(1, 0);
        }, 1.5f, "#3D2525");
        keyboardKeysLayout.addView(row3, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120));

        // Ряд 4: Смена языка и Пробел
        LinearLayout row4 = new LinearLayout(this);
        row4.setGravity(Gravity.CENTER_HORIZONTAL);
        
        String langText = isRussian ? "РУС" : "ENG";
        addKeyButton(row4, langText, v -> {
            isRussian = !isRussian;
            buildKeyboard(); // Переключаем язык!
        }, 2.0f, "#1F1F1F");

        addKeyButton(row4, "ПРОБЕЛ", v -> typeText(" "), 5.0f, "#2C2C2C");
        keyboardKeysLayout.addView(row4, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120));
    }

    private void addKeyButton(LinearLayout parent, String text, View.OnClickListener listener, float weight, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(14);
        btn.setPadding(0, 0, 0, 0);
        btn.setGravity(Gravity.CENTER);
        btn.setOnClickListener(listener);

        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(10);
        shape.setColor(Color.parseColor(colorHex));
        btn.setBackground(shape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        params.setMargins(3, 4, 3, 4);
        btn.setLayoutParams(params);
        parent.addView(btn);
    }

    private void typeText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }

    // --- ОБЯЗАТЕЛЬНЫЕ МЕТОДЫ ИНТЕРФЕЙСА ДЛЯ ИСПРАВЛЕНИЯ ОШИБКИ СБОРКИ ---
    @Override public void onPress(int primaryCode) {}
    @Override public void onRelease(int primaryCode) {}
    @Override public void onKey(int primaryCode, int[] keyCodes) {}
    @Override public void onText(CharSequence text) {}
    @Override public void swipeLeft() {}
    @Override public void swipeRight() {}
    @Override public void swipeDown() {}
    @Override public void swipeUp() {}
}
