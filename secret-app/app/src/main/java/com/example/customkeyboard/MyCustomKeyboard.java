package com.example.customkeyboard;

import android.inputmethodservice.InputMethodService;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.GridLayout;
import android.widget.LinearLayout;

public class MyCustomKeyboard extends InputMethodService {

    private FrameLayout container;
    private WebView webView;
    private int clickerScore = 0;

    @Override
    public View onCreateInputView() {
        LayoutInflater inflater = getLayoutInflater();
        int layoutId = getResources().getIdentifier("keyboard_view", "layout", getPackageName());
        View layout = inflater.inflate(layoutId, null);

        // Принудительно заставляем корневой контейнер занять всю ширину и высоту
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.WRAP_CONTENT
        );
        layout.setLayoutParams(params);

        container = layout.findViewById(getResources().getIdentifier("content_container", "id", getPackageName()));
        webView = layout.findViewById(getResources().getIdentifier("keyboard_webview", "id", getPackageName()));
        
        if (webView != null) {
            WebSettings settings = webView.getSettings();
            settings.setJavaScriptEnabled(true);
            settings.setDomStorageEnabled(true);
            webView.setWebViewClient(new WebViewClient());
        }

        // Кнопки букв
        setupKey(layout, "key_q", "q");
        setupKey(layout, "key_w", "w");
        setupKey(layout, "key_e", "e");
        setupKey(layout, "key_r", "r");
        setupKey(layout, "key_t", "t");
        setupKey(layout, "key_y", "y");
        setupKey(layout, "key_u", "u");
        setupKey(layout, "key_i", "i");
        setupKey(layout, "key_o", "o");
        setupKey(layout, "key_p", "p");
        setupKey(layout, "key_space", " ");

        int btnDeleteId = getResources().getIdentifier("key_delete", "id", getPackageName());
        View btnDelete = layout.findViewById(btnDeleteId);
        if (btnDelete != null) {
            btnDelete.setOnClickListener(v -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.deleteSurroundingText(1, 0);
            });
        }

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
    }

    private void setupKey(View layout, String idName, final String textToType) {
        int id = getResources().getIdentifier(idName, "id", getPackageName());
        View key = layout.findViewById(id);
        if (key != null) {
            key.setOnClickListener(v -> {
                InputConnection ic = getCurrentInputConnection();
                if (ic != null) ic.commitText(textToType, 1);
            });
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

    private boolean isXTurn = true;
    private String[] board = new String[9];

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
}
