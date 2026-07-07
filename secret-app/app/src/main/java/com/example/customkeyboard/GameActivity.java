package com.example.customkeyboard;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.view.Gravity;

public class GameActivity extends Activity {

    private int clickerScore = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Главный контейнер на весь экран
        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.BLACK);

        // Кнопка «Назад к клавиатуре» сверху
        Button btnBack = new Button(this);
        btnBack.setText("◀ ВЕРНУТЬСЯ К КЛАВИАТУРЕ");
        btnBack.setBackgroundColor(Color.parseColor("#333333"));
        btnBack.setTextColor(Color.WHITE);
        btnBack.setOnClickListener(v -> finish()); // Закрывает экран и возвращает назад
        mainLayout.addView(btnBack, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 120));

        // Контейнер для содержимого
        FrameLayout contentFrame = new FrameLayout(this);
        mainLayout.addView(contentFrame, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
        setContentView(mainLayout);

        // Получаем тип того, что нужно открыть
        String type = getIntent().getStringExtra("game_type");
        if (type == null) type = "youtube";

        if (type.equals("youtube")) {
            WebView webView = createBaseWebView();
            contentFrame.addView(webView);
            webView.loadUrl("https://m.youtube.com");
        } else if (type.equals("clicker")) {
            setupClicker(contentFrame);
        } else if (type.equals("tetris")) {
            WebView webView = createBaseWebView();
            contentFrame.addView(webView);
            webView.loadDataWithBaseURL(null, getTetrisHtml(), "text/html", "UTF-8", null);
        } else if (type.equals("snake")) {
            WebView webView = createBaseWebView();
            contentFrame.addView(webView);
            webView.loadDataWithBaseURL(null, getSnakeHtml(), "text/html", "UTF-8", null);
        }
    }

    private WebView createBaseWebView() {
        WebView webView = new WebView(this);
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        return webView;
    }

    private void setupClicker(FrameLayout parent) {
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setGravity(Gravity.CENTER);

        TextView tv = new TextView(this);
        tv.setText("Монеты: 0");
        tv.setTextColor(Color.WHITE);
        tv.setTextSize(30);
        layout.addView(tv);

        Button btn = new Button(this);
        btn.setText("КЛИК!");
        btn.setTextSize(24);
        btn.setOnClickListener(v -> {
            clickerScore++;
            tv.setText("Монеты: " + clickerScore);
        });
        layout.addView(btn, new LinearLayout.LayoutParams(400, 200));
        parent.addView(layout);
    }

    private String getTetrisHtml() {
        return "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'><style>" +
            "body{background:#111;color:#fff;text-align:center;font-family:sans-serif;margin:0;padding:20px;}" +
            "canvas{background:#000;border:4px solid #555;display:block;margin:0 auto;height:60%_vh;}" +
            ".btn{padding:15px 25px;margin:5px;background:#333;color:#fff;border:none;border-radius:5px;font-size:20px;}" +
            "</style></head><body>" +
            "<canvas id='tetris' width='240' height='240'></canvas><br><br>" +
            "<button class='btn' onclick='move(-1)'>◀</button>" +
            "<button class='btn' onclick='rotate()'>🔄</button>" +
            "<button class='btn' onclick='drop()'>▼</button>" +
            "<button class='btn' onclick='move(1)'>▶</button>" +
            "<script>" +
            "const canvas=document.getElementById('tetris');const context=canvas.getContext('2d');context.scale(12,12);" +
            "const arena=createMatrix(10,20);const player={pos:{x:3,y:0},matrix:[[0,1,0],[1,1,1],[0,0,0]]};" +
            "function createMatrix(w,h){const matrix=[];while(h--)matrix.push(new Array(w).fill(0));return matrix;}" +
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
    }

    private String getSnakeHtml() {
        return "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'><style>" +
            "body{background:#111;color:#fff;text-align:center;font-family:sans-serif;margin:0;padding:20px;}" +
            "canvas{background:#000;border:4px solid #555;display:block;margin:0 auto;height:50%_vh;}" +
            ".btn{padding:15px 25px;margin:5px;background:#333;color:#fff;border:none;border-radius:5px;font-size:20px;}" +
            "</style></head><body>" +
            "<canvas id='gc' width='200' height='200'></canvas><br><br>" +
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
    }
}
