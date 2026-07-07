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
import android.widget.TextView;
import android.graphics.Color;
import android.view.Gravity;

public class MyCustomKeyboard extends InputMethodService {

    private FrameLayout container;
    private WebView webView;
    private LinearLayout keyboardLayout;
    private Button btnCloseScreen;
    
    // Переменные для кликера
    private int clickerScore = 0;

    @Override
    public View onCreateInputView() {
        // Главный вертикальный контейнер
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#1E1E1E"));
        mainLayout.setLayoutParams(new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ));

        // 1. Панель меню верхних кнопок
        LinearLayout menuLayout = new LinearLayout(this);
        menuLayout.setOrientation(LinearLayout.HORIZONTAL);
        menuLayout.setBackgroundColor(Color.parseColor("#111111"));
        
        addButton(menuLayout, "YouTube", v -> { openScreen(); webView.setVisibility(View.VISIBLE); webView.loadUrl("https://m.youtube.com"); });
        addButton(menuLayout, "Клик", v -> { openScreen(); startClickerGame(); });
        addButton(menuLayout, "Тетрис", v -> { openScreen(); startTetrisGame(); });
        addButton(menuLayout, "Змейка", v -> { openScreen(); startSnakeGame(); });
        
        // Кнопка закрытия экрана (крестик)
        btnCloseScreen = new Button(this);
        btnCloseScreen.setText("✕");
        btnCloseScreen.setTextColor(Color.RED);
        btnCloseScreen.setVisibility(View.GONE);
        btnCloseScreen.setOnClickListener(v -> closeScreen());
        LinearLayout.LayoutParams closeParams = new LinearLayout.LayoutParams(100, ViewGroup.LayoutParams.MATCH_PARENT);
        menuLayout.addView(btnCloseScreen, closeParams);

        mainLayout.addView(menuLayout, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 110));

        // 2. Экран для игр и Ютуба (изначально свернут в высоту 0)
        container = new FrameLayout(this);
        container.setBackgroundColor(Color.BLACK);
        LinearLayout.LayoutParams containerParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0);
        mainLayout.addView(container, containerParams);
        
        webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.setVisibility(View.GONE);
        container.addView(webView);

        // 3. Блок клавиш букв (вынесли в отдельный контейнер, чтобы скрывать при открытии игры)
        keyboardLayout = new LinearLayout(this);
        keyboardLayout.setOrientation(LinearLayout.VERTICAL);
        keyboardLayout.setPadding(4, 4, 4, 4);

        // Ряд 1: QWERTYUIOP
        LinearLayout row1 = new LinearLayout(this);
        for (String key : new String[]{"q", "w", "e", "r", "t", "y", "u", "i", "o", "p"}) {
            addButton(row1, key.toUpperCase(), v -> typeText(key));
        }
        keyboardLayout.addView(row1, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 130));

        // Ряд 2: ASDFGHJKL
        LinearLayout row2 = new LinearLayout(this);
        row2.setGravity(Gravity.CENTER_HORIZONTAL);
        for (String key : new String[]{"a", "s", "d", "f", "g", "h", "j", "k", "l"}) {
            addButton(row2, key.toUpperCase(), v -> typeText(key));
        }
        keyboardLayout.addView(row2, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 130));

        // Ряд 3: ZXCVBNM + Стереть
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

        // Ряд 4: Пробел
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
        btn.setTextSize(14);
        btn.setOnClickListener(listener);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.MATCH_PARENT, 1.0f);
        params.setMargins(2, 2, 2, 2);
        parent.addView(btn, params);
    }

    // Открытие экрана игры на всю высоту клавиатуры
    private void openScreen() {
        if (container != null && keyboardLayout != null) {
            keyboardLayout.setVisibility(View.GONE); // Скрываем буквы, чтобы освободить место
            btnCloseScreen.setVisibility(View.VISIBLE); // Показываем крестик закрытия
            
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) container.getLayoutParams();
            lp.height = 510; // Суммарная высота всех скрытых рядов клавиш
            container.setLayoutParams(lp);
            container.removeAllViews();
            webView.setVisibility(View.GONE);
            container.addView(webView);
        }
    }

    // Закрытие экрана и возврат к буквам
    private void closeScreen() {
        if (container != null && keyboardLayout != null) {
            keyboardLayout.setVisibility(View.VISIBLE); // Возвращаем буквы
            btnCloseScreen.setVisibility(View.GONE); // Скрываем крестик
            
            LinearLayout.LayoutParams lp = (LinearLayout.LayoutParams) container.getLayoutParams();
            lp.height = 0; // Сворачиваем экран
            container.setLayoutParams(lp);
            container.removeAllViews();
        }
    }

    private void typeText(String text) {
        InputConnection ic = getCurrentInputConnection();
        if (ic != null) ic.commitText(text, 1);
    }

    // --- ИГРА 1: ОФЛАЙН КЛИКЕР ---
    private void startClickerGame() {
        LinearLayout gameLayout = new LinearLayout(this);
        gameLayout.setOrientation(LinearLayout.VERTICAL);
        gameLayout.setGravity(Gravity.CENTER);
        
        TextView tvScore = new TextView(this);
        clickerScore = 0;
        tvScore.setText("Монеты: " + clickerScore);
        tvScore.setTextColor(Color.WHITE);
        tvScore.setTextSize(20);
        gameLayout.addView(tvScore);

        Button btnClick = new Button(this);
        btnClick.setText("КЛИК!");
        btnClick.setOnClickListener(v -> {
            clickerScore++;
            tvScore.setText("Монеты: " + clickerScore);
        });
        gameLayout.addView(btnClick, new LinearLayout.LayoutParams(250, 120));

        container.addView(gameLayout);
    }

    // --- ИГРА 2: ОФЛАЙН ТЕТРИС (HTML-код внутри WebView без интернета) ---
    private void startTetrisGame() {
        webView.setVisibility(View.VISIBLE);
        String tetrisHtml = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'><style>" +
            "body{background:#111;color:#fff;text-align:center;font-family:sans-serif;margin:0;padding:5px;}" +
            "canvas{background:#000;border:2px solid #555;display:block;margin:0 auto;height:120px;}" +
            ".btn{padding:5px 10px;margin:2px;background:#333;color:#fff;border:none;border-radius:3px;font-size:12px;}" +
            "</style></head><body>" +
            "<canvas id='tetris' width='240' height='240'></canvas><br>" +
            "<button class='btn' onclick='move(-1)'>◀</button>" +
            "<button class='btn' onclick='rotate()'>🔄</button>" +
            "<button class='btn' onclick='drop()'>▼</button>" +
            "<button class='btn' onclick='move(1)'>▶</button>" +
            "<script>" +
            "const canvas=document.getElementById('tetris');const context=canvas.getContext('2d');context.scale(12,12);" +
            "const arena=createMatrix(10,20);const player={pos:{x:3,y:0},matrix:createPiece('T')};" +
            "function createMatrix(w,h){const matrix=[];while(h--)matrix.push(new Array(w).fill(0));return matrix;}" +
            "function createPiece(type){return [[0,1,0],[1,1,1],[0,0,0]];}" +
            "function drawMatrix(matrix,offset){matrix.forEach((row,y)=>{row.forEach((value,x)=>{if(value!==0){context.fillStyle='red';context.fillRect(x+offset.x,y+offset.y,1,1);}});});}" +
            "function draw(){context.fillStyle='#000';context.fillRect(0,0,canvas.width,canvas.height);drawMatrix(arena,{x:0,y:0});drawMatrix(player.matrix,player.pos);}" +
            "function merge(arena,player){player.matrix.forEach((row,y)=>{row.forEach((value,x)=>{if(value!==0){arena[y+player.pos.y][x+player.pos.x]=value;}});});}" +
            "function playerDrop(){player.pos.y++;if(collide(arena,player)){player.pos.y--;merge(arena,player);player.pos.y=0;player.pos.x=3;}dropCounter=0;}" +
            "function collide(arena,player){const [m,o]=[player.matrix,player.pos];for(let y=0;y<m.length;++y){for(let x=0;x<m[y].length;++x){if(m[y][x]!==0&&(arena[y+o.y]&&arena[y+o.y][x+o.x])!==0){return true;}}}return false;}" +
            "function move(dir){player.pos.x+=dir;if(collide(arena,player)){player.pos.x-=dir;}}" +
            "function rotate(){const m=player.matrix;for(let y=0;y<m.length;++y){for(let x=0;x<y;++x)[m[x][y],m[y][x]]=[m[y][x],m[x][y]];}m.forEach(row=>row.reverse());}" +
            "function drop(){playerDrop();draw();}" +
            "let dropCounter=0;let lastTime=0;" +
            "function update(time=0){const deltaTime=time-lastTime;lastTime=time;dropCounter+=deltaTime;if(dropCounter>1000){playerDrop();}draw();requestAnimationFrame(update);}" +
            "update();" +
            "</script></body></html>";
        webView.loadDataWithBaseURL(null, tetrisHtml, "text/html", "UTF-8", null);
    }

    // --- ИГРА 3: ОФЛАЙН ЗМЕЙКА (HTML-код внутри WebView без интернета) ---
    private void startSnakeGame() {
        webView.setVisibility(View.VISIBLE);
        String snakeHtml = "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'><style>" +
            "body{background:#111;color:#fff;text-align:center;font-family:sans-serif;margin:0;padding:5px;}" +
            "canvas{background:#000;border:2px solid #555;display:block;margin:0 auto;height:120px;}" +
            ".btn{padding:5px 12px;margin:2px;background:#333;color:#fff;border:none;border-radius:3px;font-size:12px;}" +
            "</style></head><body>" +
            "<canvas id='gc' width='200' height='200'></canvas><br>" +
            "<button class='btn' onclick='press(38)'>▲</button><br>" +
            "<button class='btn' onclick='press(37)'>◀</button>" +
            "<button class='btn' onclick='press(40)'>▼</button>" +
            "<button class='btn' onclick='press(39)'>▶</button>" +
            "<script>" +
            "canvas=document.getElementById('gc');ctx=canvas.getContext('2d');" +
            "px=py=10;gs=tc=20;ax=ay=15;xv=yv=0;trail=[];tail=5;" +
            "function game(){px+=xv;py+=yv;if(px<0)px=tc-1;if(px>tc-1)px=0;if(py<0)py=tc-1;if(py>tc-1)py=0;" +
            "ctx.fillStyle='black';ctx.fillRect(0,0,canvas.width,canvas.height);ctx.fillStyle='lime';" +
            "for(var i=0;i<trail.length;i++){ctx.fillRect(trail[i].x*gs,trail[i].y*gs,gs-2,gs-2);if(trail[i].x==px&&trail[i].y==py){tail=5;}}" +
            "trail.push({x:px,y:py});while(trail.length>tail)trail.shift();if(ax==px&&ay==py){tail++;ax=Math.floor(Math.random()*tc);ay=Math.floor(Math.random()*tc);}" +
            "ctx.fillStyle='red';ctx.fillRect(ax*gs,ay*gs,gs-2,gs-2);}" +
            "function press(k){if(k==37&&xv!=1){xv=-1;yv=0;}if(k==38&&yv!=1){xv=0;yv=-1;}if(k==39&&xv!=-1){xv=1;yv=0;}if(k==40&&yv!=-1){xv=0;yv=1;}}" +
            "setInterval(game,1000/10);" +
            "</script></body></html>";
        webView.loadDataWithBaseURL(null, snakeHtml, "text/html", "UTF-8", null);
    }
}
