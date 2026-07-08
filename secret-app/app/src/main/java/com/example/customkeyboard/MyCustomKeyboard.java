package com.example.customkeyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputConnection;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.GridLayout;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;

public class MyCustomKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private WebView webView;
    private FrameLayout keyboardContainer;
    private FrameLayout gameContainer;
    private LinearLayout keyboardLayout;
    
    private boolean isRussian = true; // Переключатель языка по умолчанию

    // Переменные для игр
    private int clickerScore = 0;
    private boolean isXTurn = true;
    private String[] board = new String[9];

    @Override
    public View onCreateInputView() {
        // Главный контейнер
        keyboardContainer = new FrameLayout(this);
        
        LayoutInflater inflater = getLayoutInflater();
        
        // Загружаем основной макет клавиатуры (мы создадим его в Шаге 2)
        int layoutId = getResources().getIdentifier("keyboard_main_layout", "layout", getPackageName());
        keyboardLayout = (LinearLayout) inflater.inflate(layoutId, null);
        
        // Контейнер для встроенного YouTube/Игр
        gameContainer = new FrameLayout(this);
        gameContainer.setVisibility(View.GONE); // Изначально скрыт
        gameContainer.setBackgroundColor(Color.BLACK);
        
        // Добавляем WebView прямо в клавиатуру
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        gameContainer.addView(webView, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT
        ));
        
        // Добавляем кнопку закрытия встроенного окна
        Button btnClose = new Button(this);
        btnClose.setText("✕");
        btnClose.setTextColor(Color.RED);
        btnClose.setBackgroundColor(Color.TRANSPARENT);
        btnClose.setOnClickListener(v -> closeEmbeddedScreen());
        FrameLayout.LayoutParams closeParams = new FrameLayout.LayoutParams(100, 100);
        closeParams.gravity = android.view.Gravity.TOP | android.view.Gravity.RIGHT;
        gameContainer.addView(btnClose, closeParams);

        keyboardContainer.addView(keyboardLayout);
        keyboardContainer.addView(gameContainer, new FrameLayout.LayoutParams(
            FrameLayout.LayoutParams.MATCH_PARENT, 500 // Высота встроенного окна
        ));

        // Инициализируем кнопки меню
        setupMenuButtons(keyboardLayout);
        
        // Отрисовываем клавиши
        buildKeyboard();

        return keyboardContainer;
    }

    // Метод настройки кнопок меню (Ютуб, Игры)
    private void setupMenuButtons(View layout) {
        setupMenuButton(layout, "btn_youtube", v -> openEmbeddedUrl("https://m.youtube.com"));
        setupMenuButton(layout, "btn_clicker", v -> openEmbeddedClicker());
        setupMenuButton(layout, "btn_tic_tac", v -> openEmbeddedTicTacToe());
        setupMenuButton(layout, "btn_tetris", v -> openEmbeddedUrl("file:///android_asset/tetris.html"));
        setupMenuButton(layout, "btn_snake", v -> openEmbeddedUrl("file:///android_asset/snake.html"));
    }

    private void setupMenuButton(View layout, String idName, View.OnClickListener listener) {
        int id = getResources().getIdentifier(idName, "id", getPackageName());
        View button = layout.findViewById(id);
        if (button != null) {
            button.setOnClickListener(listener);
        }
    }

    // --- ОТКРЫТИЕ ВСТРОЕННЫХ ЭКРАНОВ ---
    
    private void openEmbeddedUrl(String url) {
        keyboardLayout.setVisibility(View.GONE); // Скрываем клавиши
        gameContainer.setVisibility(View.VISIBLE); // Показываем окно
        webView.setVisibility(View.VISIBLE);
        webView.loadUrl(url);
    }

    private void openEmbeddedClicker() {
        // Логика кликера, встроенная в клавиатуру
        // (аналогично тому, что было в GameActivity, но внутри gameContainer)
    }
    
    private void openEmbeddedTicTacToe() {
        // Логика крестиков-ноликов
    }

    private void closeEmbeddedScreen() {
        gameContainer.setVisibility(View.GONE); // Скрываем окно
        keyboardLayout.setVisibility(View.VISIBLE); // Показываем клавиши
        webView.stopLoading();
        webView.setVisibility(View.GONE);
    }

    // --- ЛОГИКА КЛАВИАТУРЫ (ПЕРЕКЛЮЧАТЕЛЬ И БУКВЫ) ---

    // (Сюда мы вставим обновленный метод buildKeyboard иaddKeyButton из предыдущего ответа)
}
