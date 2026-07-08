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
    
    private boolean isRussian = true; // Русский язык по умолчанию

    @Override
    public View onCreateInputView() {
        // 1. Главный контейнер-пачка (FrameLayout)
        keyboardContainer = new FrameLayout(this);
        keyboardContainer.setLayoutParams(new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 2. Основной макет клавиатуры (Кнопки меню + Буквы)
        mainKeyboardLayout = new LinearLayout(this);
        mainKeyboardLayout.setOrientation(LinearLayout.VERTICAL);
        mainKeyboardLayout.setBackgroundColor(Color.parseColor("#121212"));

        // Верхняя панель меню гейминга
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.HORIZONTAL);
        menuLayout.setBackgroundColor(Color.parseColor("#1A1A1A"));
        menuLayout.setPadding(4, 4, 4, 4);
        
        addMenuButton(menuLayout, "YouTube", v -> openEmbeddedUrl("https://m.youtube.com"), "#FF0000");
        addMenuButton(menuLayout, "Клик", v -> openEmbeddedUrl("https://orteil.dashnet.org/cookieclicker/"), "#03DAC6");
        addMenuButton(menuLayout, "Тетрис", v -> openEmbeddedUrl("https://www.tetris.com"), "#BB86FC");
        addMenuButton(menuLayout, "Змейка", v -> openEmbeddedUrl("https://www.google.com/search?q=play+snake"), "#CF6679");

        mainKeyboardLayout.addView(menuLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110));

        // Контейнер, куда будут динамически генерироваться ряды букв
        keyboardKeysLayout = new LinearLayout(this);
        keyboardKeysLayout.setOrientation(LinearLayout.VERTICAL);
        keyboardKeysLayout.setPadding(2, 4, 2, 4);
        mainKeyboardLayout.addView(keyboardKeysLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // 3. Контейнер для встроенного WebView (Браузера)
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setVisibility(View.GONE); // Изначально скрыт
        
        // Кнопка закрытия браузера (Крестик поверх видео)
        Button btnClose = new Button(this);
        btnClose.setText("✕");
        btnClose.setTextColor(Color.RED);
        btnClose.setTextSize(16);
        btnClose.setBackgroundColor(Color.TRANSPARENT);
        btnClose.setOnClickListener(v -> closeEmbeddedScreen());
        
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(100, 100);
        closeParams.gravity = Gravity.TOP | Gravity.RIGHT;
        closeParams.setMargins(0, 10, 10, 0);

        // Собираем всё в один пирог
        keyboardContainer.addView(mainKeyboardLayout);
        keyboardContainer.addView(webView, new FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 600 // Высота скрытого Ютуб-окна в шторке
        ));
        keyboardContainer.addView(btnClose, closeParams);
        btnClose.setVisibility(View.GONE); // Скрыта вместе с браузером
        webView.setTag(btnClose); // Сохраняем ссылку на кнопку, чтобы легко её прятать

        // Отрисовываем кнопки букв
        buildKeyboard();

        return keyboardContainer;
    }

    private void openEmbeddedUrl(String url) {
        mainKeyboardLayout.setVisibility(View.GONE);
        webView.setVisibility(View.VISIBLE);
        
        Button btnClose = (Button) webView.getTag();
        if (btnClose != null) btnClose.setVisibility(View.VISIBLE);
        
        webView.loadUrl(url);
    }

    private void closeEmbeddedScreen() {
        webView.setVisibility(View.GONE);
        mainKeyboardLayout.setVisibility(View.VISIBLE);
        
        Button btnClose = (Button) webView.getTag();
        if (btnClose != null) btnClose.setVisibility(View.GONE);
        
        webView.stopLoading();
        webView.loadUrl("about:blank");
    }

    // Динамическая генерация клавиш без XML
    private void buildKeyboard() {
        if (keyboardKeysLayout == null) return;
        keyboardKeysLayout.removeAllViews();

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

        // Ряд 3 + Бэкспейс
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

        // Ряд 4: Язык + Пробел
        LinearLayout row4 = new LinearLayout(this);
        row4.setGravity(Gravity.CENTER_HORIZONTAL);
        
        String langText = isRussian ? "РУС" : "ENG";
        addKeyButton(row4, langText, v -> {
            isRussian = !isRussian;
            buildKeyboard(); // Переключаем раскладку прямо в памяти!
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

    private void addMenuButton(LinearLayout parent, String text, View.OnClickListener listener, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor(colorHex));
        btn.setTextSize(11);
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

    // --- Обязательные методы системного интерфейса, чтобы Gradle не ругался ---
    @Override public void onPress(int primaryCode) {}
    @Override public void onRelease(int primaryCode) {}
    @Override public void onKey(int primaryCode, int[] keyCodes) {}
    @Override public void onText(CharSequence text) {}
    @Override public void swipeLeft() {}
    @Override public void swipeRight() {}
    @Override public void swipeDown() {}
    @Override public void swipeUp() {}
}
