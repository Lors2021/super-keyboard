package com.example.customkeyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.GridLayout;

public class MyCustomKeyboard extends InputMethodService implements KeyboardView.OnKeyboardActionListener {

    private FrameLayout container;
    private WebView webView;
    private int clickerScore = 0;
    private boolean isXTurn = true;
    private String[] board = new String[9];

    @Override
    public View onCreateInputView() {
        // Защита: создаем разметку через try-catch, чтобы если что-то пойдет не так, приложение не падало
        try {
            LayoutInflater inflater = getLayoutInflater();
            int layoutId = getResources().getIdentifier("keyboard_view", "layout", getPackageName());
            View layout = inflater.inflate(layoutId, null);

            int containerId = getResources().getIdentifier("content_container", "id", getPackageName());
            int webViewId = getResources().getIdentifier("keyboard_webview", "id", getPackageName());
            int keyboardViewId = getResources().getIdentifier("keyboard_view", "id", getPackageName());

            container = layout.findViewById(containerId);
            webView = layout.findViewById(webViewId);
            
            // Настройка WebView с защитой
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true); // Важно для HTML5 игр (Тетрис, Змейка)
            webView.setWebViewClient(new WebViewClient());

            // Инициализация стандартных клавиш ввода
            KeyboardView kv = layout.findViewById(keyboardViewId);
            int qwertyId = getResources().getIdentifier("qwerty_layout", "xml", getPackageName());
            Keyboard keyboard = new Keyboard(this, qwertyId);
            kv.setKeyboard(keyboard);
            kv.setOnKeyboardActionListener(this);

            // Безопасное подключение кнопок
            setupMenuButton(layout, "btn_youtube", "https://m.youtube.com");
            setupMenuButton(layout, "btn_tetris", "file:///android_asset/tetris.html");
            setupMenuButton(layout, "btn_snake", "file:///android_asset/snake.html");

            int btnClickerId = getResources().getIdentifier("btn_clicker", "id", getPackageName());
            View btnClicker = layout.findViewById(btnClickerId);
            if (btnClicker != null) btnClicker.setOnClickListener(v -> openClicker());

            int btnTicTacId = getResources().getIdentifier("btn_tic_tac", "id", getPackageName());
            View btnTicTac = layout.findViewById(btnTicTacId);
            if (btnTicTac != null) btnTicTac.setOnClickListener(v -> openTicTacToe());

            return layout;
        } catch (Exception e) {
            e.printStackTrace();
            // Возвращаем пустую view вместо вылета приложения, если произошел сбой
            return new View(this);
        }
    }

    private void setupMenuButton(View layout, String idName, String url) {
        int id = getResources().getIdentifier(idName, "id", getPackageName());
        View button = layout.findViewById(id);
        if (button != null) {
            button.setOnClickListener(v -> {
                showWebView();
                webView.loadUrl(url);
            });
        }
    }

    private void showWebView() {
        if (container != null && webView != null) {
            container.removeAllViews();
            webView.setVisibility(View.VISIBLE);
            container.addView(webView);
        }
    }

    private void openClicker() {
        if (container == null) return;
        webView.setVisibility(View.GONE);
        container.removeAllViews();
        
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(getResources().getIdentifier("game_clicker", "layout", getPackageName()), null);
        
        TextView tvScore = view.findViewById(getResources().getIdentifier("tv_score", "id", getPackageName()));
        Button btnClick = view.findViewById(getResources().getIdentifier("btn_click", "id", getPackageName()));
        
        clickerScore = 0;
        if (tvScore != null) tvScore.setText("Монеты: " + clickerScore);
        
        if (btnClick != null) {
            btnClick.setOnClickListener(v -> {
                clickerScore++;
                if (tvScore != null) tvScore.setText("Монеты: " + clickerScore);
            });
        }
        
        container.addView(view);
    }

    private void openTicTacToe() {
        if (container == null) return;
        webView.setVisibility(View.GONE);
        container.removeAllViews();
        
        isXTurn = true;
        for(int i=0; i<9; i++) board[i] = "";

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(getResources().getIdentifier("game_tictactoe", "layout", getPackageName()), null);
        GridLayout gridLayout = view.findViewById(getResources().getIdentifier("grid_board", "id", getPackageName()));

        if (gridLayout != null) {
            for (int i = 0; i < 9; i++) {
                final int index = i;
                Button button = (Button) gridLayout.getChildAt(i);
                if (button != null) {
                    button.setText("");
                    button.setOnClickListener(v -> {
                        if (button.getText().toString().isEmpty() && board[index].isEmpty()) {
                            button.setText(isXTurn ? "X" : "O");
                            board[index] = button.getText().toString();
                            isXTurn = !isXTurn;
                        }
                    });
                }
            }
        }
        
        int btnResetId = getResources().getIdentifier("btn_reset_tic", "id", getPackageName());
        View btnReset = view.findViewById(btnResetId);
        if (btnReset != null) btnReset.setOnClickListener(v -> openTicTacToe());
        container.addView(view);
    }

    @Override
    public void onKey(int primaryCode, int[] keyCodes) {
        android.view.inputmethod.InputConnection ic = getCurrentInputConnection();
        if (ic == null) return;
        if (primaryCode == -5) {
            ic.deleteSurroundingText(1, 0);
        } else {
            ic.commitText(String.valueOf((char) primaryCode), 1);
        }
    }

    @Override public void onPress(int primaryCode) {}
    @Override public void onRelease(int primaryCode) {}
    @Override public void onText(CharSequence text) {}
    @Override public void swipeLeft() {}
    @Override public void swipeRight() {}
    @Override public void swipeDown() {}
    @Override public void swipeUp() {}
}
