package com.example.customkeyboard;

import android.inputmethodservice.InputMethodService;
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

public class MyCustomKeyboard extends InputMethodService {

    private FrameLayout container;
    private WebView webView;

    @Override
    public View onCreateInputView() {
        // Создаем главный контейнер программно
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#1E1E1E"));
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Панель меню (кнопки)
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.HORIZONTAL);
        menuLayout.setBackgroundColor(Color.parseColor("#111111"));
        
        addButton(menuLayout, "YouTube", v -> openUrl("https://m.youtube.com"));
        addButton(menuLayout, "Тетрис", v -> openUrl("https://html5game.com")); // Временная замена локального файла для теста
        addButton(menuLayout, "Змейка", v -> openUrl("https://google.com/search?q=snake+game"));

        mainLayout.addView(menuLayout, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 120 // Высота меню в пикселях
        ));

        // Игровой контейнер и WebView
        container = new FrameLayout(this);
        container.setBackgroundColor(Color.BLACK);
        
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setVisibility(View.GONE);
        container.addView(webView);

        mainLayout.addView(container, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 400 // Высота игрового поля
        ));

        // Ряд клавиш клавиатуры
        LinearLayout row1 = new LinearLayout(this);
        row1.setOrientation(LinearLayout.HORIZONTAL);
        
        String[] keys = {"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"};
        for (String key : keys) {
            addButton(row1, key.toUpperCase(), v -> typeText(key));
        }
        mainLayout.addView(row1, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 140
        ));

        // Ряд управления (Пробел и Стереть)
        LinearLayout row2 = new LinearLayout(this);
        row2.setOrientation(LinearLayout.HORIZONTAL);
        
        addButton(row2, "ПРОБЕЛ", v -> typeText(" "));
        addButton(row2, "⌫ СТЕРЕТЬ", v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.deleteSurroundingText(1, 0);
        });
        
        mainLayout.addView(row2, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 140
        ));

        return mainLayout;
    }

    private void addButton(LinearLayout parent, String text, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f
        );
        params.setMargins(2, 2, 2, 2);
        parent.addView(btn, params);
    }

    private void openUrl(String url) {
        if (webView != null && container != null) {
            webView.setVisibility(View.VISIBLE);
            webView.loadUrl(url);
        }
    }

    private void typeText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) {
            ic.commitText(text, 1);
        }
    }
}
