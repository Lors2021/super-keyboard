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
import android.graphics.drawable.GradientDrawable;

public class MyCustomKeyboard extends InputMethodService {

    private boolean isRussian = true; // Переключатель языка
    private LinearLayout keyboardLayout;
    private LinearLayout mainLayout;

    @Override
    public View onCreateInputView() {
        mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#121212")); // Глубокий черный
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 1. СТИЛЬНАЯ ВЕРХНЯЯ ПАНЕЛЬ МЕНЮ
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.HORIZONTAL);
        menuLayout.setBackgroundColor(Color.parseColor("#1A1A1A"));
        menuLayout.setPadding(6, 6, 6, 6);
        
        addMenuButton(menuLayout, "YouTube", v -> launchGameScreen("youtube"), "#FF0000");
        addMenuButton(menuLayout, "Клик", v -> launchGameScreen("clicker"), "#03DAC6");
        addMenuButton(menuLayout, "Тетрис", v -> launchGameScreen("tetris"), "#BB86FC");
        addMenuButton(menuLayout, "Змейка", v -> launchGameScreen("snake"), "#CF6679");

        mainLayout.addView(menuLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120));

        // Контейнер для самих букв
        keyboardLayout = new LinearLayout(this);
        keyboardLayout.setOrientation(LinearLayout.VERTICAL);
        keyboardLayout.setPadding(2, 6, 2, 6);
        mainLayout.addView(keyboardLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        // Отрисовываем клавиши
        buildKeyboard();

        return mainLayout;
    }

    // Метод построения сетки клавиш (вынесен отдельно для динамической смены языка)
    private void buildKeyboard() {
        keyboardLayout.removeAllViews();

        // Раскладки символов
        String[] row1Keys = isRussian ? 
            new String[]{"й", "ц", "у", "к", "е", "н", "г", "ш", "щ", "з", "х", "ъ"} : 
            new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"};

        String[] row2Keys = isRussian ? 
            new String[]{"ф", "ы", "в", "а", "п", "р", "о", "л", "д", "ж", "э"} : 
            new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l"};

        String[] row3Keys = isRussian ? 
            new String[]{"я", "ч", "с", "м", "и", "т", "ь", "б", "ю"} : 
            new String[]{"z", "x", "c", "v", "b", "n", "m"};

        // Ряд 1
        LinearLayout row1 = new LinearLayout(this);
        row1.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : row1Keys) {
            addKeyButton(row1, key.toUpperCase(), v -> typeText(key), 1.0f, "#2C2C2C");
        }
        keyboardLayout.addView(row1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 125));

        // Ряд 2
        LinearLayout row2 = new LinearLayout(this);
        row2.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : row2Keys) {
            addKeyButton(row2, key.toUpperCase(), v -> typeText(key), 1.0f, "#2C2C2C");
        }
        keyboardLayout.addView(row2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 125));

        // Ряд 3 (Буквы + Стереть)
        LinearLayout row3 = new LinearLayout(this);
        row3.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : row3Keys) {
            addKeyButton(row3, key.toUpperCase(), v -> typeText(key), 1.0f, "#2C2C2C");
        }
        // Кнопка Backspace (Стереть)
        addKeyButton(row3, "⌫", v -> {
            InputConnection ic = getCurrentInputConnection();
            if (ic != null) ic.deleteSurroundingText(1, 0);
        }, 1.5f, "#3D2525");
        keyboardLayout.addView(row3, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 125));

        // Ряд 4 (Смена Языка + Пробел)
        LinearLayout row4 = new LinearLayout(this);
        row4.setGravity(Gravity.CENTER_HORIZONTAL);
        
        // Кнопка переключения языка
        String langText = isRussian ? "РУС" : "ENG";
        addKeyButton(row4, langText, v -> {
            isRussian = !isRussian;
            buildKeyboard(); // Перерисовываем клавиатуру с новыми буквами
        }, 2.0f, "#1F1F1F");

        // Кнопка Пробел
        addKeyButton(row4, "ПРОБЕЛ", v -> typeText(" "), 5.0f, "#2C2C2C");
        
        keyboardLayout.addView(row4, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 125));
    }

    // Создание красивой закругленной кнопки для букв
    private void addKeyButton(LinearLayout parent, String text, View.OnClickListener listener, float weight, String colorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(14); // Оптимальный размер, чтобы текст не превращался в точки
        btn.setPadding(0, 0, 0, 0);
        btn.setGravity(Gravity.CENTER);
        btn.setOnClickListener(listener);

        // Создаем закругленные углы программно
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(10); // Радиус закругления
        shape.setColor(Color.parseColor(colorHex));
        btn.setBackground(shape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, weight);
        params.setMargins(4, 4, 4, 4); // Небольшие зазоры между кнопками
        btn.setLayoutParams(params);
        parent.addView(btn);
    }

    // Создание кнопок верхнего меню гейминга
    private void addMenuButton(LinearLayout parent, String text, View.OnClickListener listener, String accentColorHex) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.parseColor(accentColorHex));
        btn.setTextSize(12);
        btn.setGravity(Gravity.CENTER);
        btn.setOnClickListener(listener);

        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(12);
        shape.setColor(Color.parseColor("#252525"));
        shape.setStroke(2, Color.parseColor(accentColorHex)); // Неоновый ободок цвета игры
        btn.setBackground(shape);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        params.setMargins(6, 4, 6, 4);
        btn.setLayoutParams(params);
        parent.addView(btn);
    }

    private void launchGameScreen(String type) {
        Intent intent = new Intent(this, GameActivity.class);
        intent.putExtra("game_type", type);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
    }

    private void typeText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }
}
