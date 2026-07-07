package com.example.customkeyboard;

import android.inputmethodservice.InputMethodService;
import android.inputmethodservice.Keyboard;
import android.inputmethodservice.KeyboardView;
import android.view.LayoutInflater;
import android.view.View;
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

    @Override
    public View onCreateInputView() {
        // Явно берем ресурсы нашего приложения через его пакет
        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(
            getResources().getIdentifier("keyboard_view", "layout", getPackageName()), null
        );

        int containerId = getResources().getIdentifier("content_container", "id", getPackageName());
        int webViewId = getResources().getIdentifier("keyboard_webview", "id", getPackageName());
        int keyboardViewId = getResources().getIdentifier("keyboard_view", "id", getPackageName());

        container = layout.findViewById(containerId);
        webView = layout.findViewById(webViewId);
        
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());

        KeyboardView kv = layout.findViewById(keyboardViewId);
        int qwertyId = getResources().getIdentifier("qwerty_layout", "xml", getPackageName());
        Keyboard keyboard = new Keyboard(this, qwertyId);
        kv.setKeyboard(keyboard);
        kv.setOnKeyboardActionListener(this);

        // Поиск кнопок меню
        setupMenuButton(layout, "btn_youtube", "https://m.youtube.com", true);
        setupMenuButton(layout, "btn_tetris", "file:///android_asset/tetris.html", true);
        setupMenuButton(layout, "btn_snake", "file:///android_asset/snake.html", true);

        int btnClickerId = getResources().getIdentifier("btn_clicker", "id", getPackageName());
        layout.findViewById(btnClickerId).setOnClickListener(v -> openClicker());

        int btnTicTacId = getResources().getIdentifier("btn_tic_tac", "id", getPackageName());
        layout.findViewById(btnTicTacId).setOnClickListener(v -> openTicTacToe());

        return layout;
    }

    private void setupMenuButton(View layout, String idName, String url, boolean isWeb) {
        int id = getResources().getIdentifier(idName, "id", getPackageName());
        layout.findViewById(id).setOnClickListener(v -> {
            showWebView();
            webView.loadUrl(url);
        });
    }

    private void showWebView() {
        container.removeAllViews();
        webView.setVisibility(View.VISIBLE);
        container.addView(webView);
    }

    private void openClicker() {
        webView.setVisibility(View.GONE);
        container.removeAllViews();
        
        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(getResources().getIdentifier("game_clicker", "layout", getPackageName()), null);
        
        TextView tvScore = view.findViewById(getResources().getIdentifier("tv_score", "id", getPackageName()));
        Button btnClick = view.findViewById(getResources().getIdentifier("btn_click", "id", getPackageName()));
        
        clickerScore = 0;
        tvScore.setText("Монеты: " + clickerScore);
        
        btnClick.setOnClickListener(v -> {
            clickerScore++;
            tvScore.setText("Монеты: " + clickerScore);
        });
        
        container.addView(view);
    }

    private boolean isXTurn = true;
    private String[] board = new String[9];

    private void openTicTacToe() {
        webView.setVisibility(View.GONE);
        container.removeAllViews();
        
        isXTurn = true;
        for(int i=0; i<9; i++) board[i] = "";

        LayoutInflater inflater = getLayoutInflater();
        View view = inflater.inflate(getResources().getIdentifier("game_tictactoe", "layout", getPackageName()), null);
        GridLayout gridLayout = view.findViewById(getResources().getIdentifier("grid_board", "id", getPackageName()));

        for (int i = 0; i < 9; i++) {
            final int index = i;
            Button button = (Button) gridLayout.getChildAt(i);
            button.setText("");
            button.setOnClickListener(v -> {
                if (button.getText().toString().isEmpty() && board[index].isEmpty()) {
                    button.setText(isXTurn ? "X" : "O");
                    board[index] = button.getText().toString();
                    isXTurn = !isXTurn;
                }
            });
        }
        
        int btnResetId = getResources().getIdentifier("btn_reset_tic", "id", getPackageName());
        view.findViewById(btnResetId).setOnClickListener(v -> openTicTacToe());
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
