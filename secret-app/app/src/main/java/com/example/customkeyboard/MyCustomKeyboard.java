package com.example.customkeyboard;

import android.inputmethodservice.InputMethodService;
import android.content.Intent;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputConnection;
import android.widget.Button;
import android.widget.LinearLayout;
import android.graphics.Color;
import android.view.Gravity;

public class MyCustomKeyboard extends InputMethodService {

    @Override
    public View onCreateInputView() {
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#1E1E1E"));
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // Панель верхнего меню
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.HORIZONTAL);
        menuLayout.setBackgroundColor(Color.parseColor("#111111"));
        
        addButton(menuLayout, "YouTube", v -> launchGameScreen("youtube"));
        addButton(menuLayout, "Клик", v -> launchGameScreen("clicker"));
        addButton(menuLayout, "Тетрис", v -> launchGameScreen("tetris"));
        addButton(menuLayout, "Змейка", v -> launchGameScreen("snake"));

        mainLayout.addView(menuLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110));

        // Клавиатурный блок (Всегда виден, ряды не прячутся)
        LinearLayout keyboardLayout = new LinearLayout(this);
        keyboardLayout.setOrientation(LinearLayout.VERTICAL);
        keyboardLayout.setPadding(4, 4, 4, 4);

        LinearLayout row1 = new LinearLayout(this);
        for (String key : new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"}) {
            addButton(row1, key.toUpperCase(), v -> typeText(key));
        }
        keyboardLayout.addView(row1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 130));

        LinearLayout row2 = new LinearLayout(this);
        row2.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l"}) {
            addButton(row2, key.toUpperCase(), v -> typeText(key));
        }
        keyboardLayout.addView(row2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 130));

        LinearLayout row3 = new LinearLayout(this);
        for (String key : new String[]{"z", "x", "c", "v", "b", "n", "m"}) {
            addButton(row3, key.toUpperCase(), v -> typeText(key));
        }
        Button btnDel = new Button(this);
        btnDel.setText("⌫");
        btnDel.setBackgroundColor(Color.parseColor("#442222"));
        btnDel.setTextColor(Color.WHITE);
        btnDel.setOnClickListener(v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.deleteSurroundingText(1, 0);
        });
        row3.addView(btnDel, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.5f));
        keyboardLayout.addView(row3, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 130));

        LinearLayout row4 = new LinearLayout(this);
        Button btnSpace = new Button(this);
        btnSpace.setText("ПРОБЕЛ");
        btnSpace.setOnClickListener(v -> typeText(" "));
        row4.addView(btnSpace, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120));
        keyboardLayout.addView(row4);

        mainLayout.addView(keyboardLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        return mainLayout;
    }

    private void addButton(LinearLayout parent, String text, View.OnClickListener listener) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        parent.addView(btn, params);
    }

    // Метод запуска полноэкранного режима
    private void launchGameScreen(String type) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("game_type", type);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK); // Флаг обязателен для запуска из фоновых служб
        startActivity(intent);
    }

    private void typeText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }
}
