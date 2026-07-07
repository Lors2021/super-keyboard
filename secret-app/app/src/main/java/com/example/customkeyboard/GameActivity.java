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
import android.os.Handler;
import android.os.Looper;
import android.graphics.drawable.GradientDrawable;

public class GameActivity extends Activity {

    private int clickerScore = 0;
    private int clickPower = 1;
    private int autoClickers = 0;
    private int powerCost = 15;
    private int autoCost = 50;
    private Handler autoClickHandler;
    private Runnable autoClickRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout mainLayout = new LinearLayout(this);
        mainLayout.setOrientation(LinearLayout.VERTICAL);
        mainLayout.setBackgroundColor(Color.parseColor("#121212"));

        // Стильная закругленная кнопка возврата назад
        Button btnBack = new Button(this);
        btnBack.setText("◀   ВЕРНУТЬСЯ К КЛАВИАТУРЕ");
        btnBack.setTextColor(Color.parseColor("#BB86FC"));
        btnBack.setTextSize(14);
        
        GradientDrawable backShape = new GradientDrawable();
        backShape.setCornerRadius(15);
        backShape.setColor(Color.parseColor("#1F1F1F"));
        btnBack.setBackground(backShape);
        
        btnBack.setOnClickListener(v -> finish());
        
        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, 130
        );
        backParams.setMargins(15, 15, 15, 15);
        mainLayout.addView(btnBack, backParams);

        FrameLayout contentFrame = new FrameLayout(this);
        mainLayout.addView(contentFrame, new LinearLayout.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        ));
        setContentView(mainLayout);

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
        layout.setGravity(Gravity.CENTER_HORIZONTAL);
        layout.setPadding(40, 40, 40, 40);

        TextView tvScore = new TextView(this);
        tvScore.setText("Монеты: 0");
        tvScore.setTextColor(Color.parseColor("#03DAC6"));
        tvScore.setTextSize(32);
        layout.addView(tvScore);

        TextView tvStats = new TextView(this);
        tvStats.setText("Клик: +" + clickPower + " | Автокликер: " + autoClickers + "/сек");
        tvStats.setTextColor(Color.GRAY);
        tvStats.setTextSize(14);
        tvStats.setPadding(0, 10, 0, 50);
        layout.addView(tvStats);

        Button btnClick = new Button(this);
        btnClick.setText("КЛИК!");
        btnClick.setTextSize(24);
        btnClick.setTextColor(Color.WHITE);
        GradientDrawable clickShape = new GradientDrawable();
        clickShape.setCornerRadius(100); // Круглая кнопка клика
        clickShape.setColor(Color.parseColor("#6200EE"));
        btnClick.setBackground(clickShape);
        btnClick.setOnClickListener(v -> {
            clickerScore += clickPower;
            tvScore.setText("Монеты: " + clickerScore);
        });
        layout.addView(btnClick, new LinearLayout.LayoutParams(350, 350));

        TextView tvShopTitle = new TextView(this);
        tvShopTitle.setText("МАГАЗИН УЛУЧШЕНИЙ");
        tvShopTitle.setTextColor(Color.WHITE);
        tvShopTitle.setTextSize(18);
        tvShopTitle.setPadding(0, 60, 0, 25);
        layout.addView(tvShopTitle);

        Button btnBuyPower = createShopButton("Улучшить клик (Цена: " + powerCost + ")");
        btnBuyPower.setOnClickListener(v -> {
            if (clickerScore >= powerCost) {
                clickerScore -= powerCost;
                clickPower += 1;
                powerCost = (int)(powerCost * 1.5);
                tvScore.setText("Монеты: " + clickerScore);
                tvStats.setText("Клик: +" + clickPower + " | Автокликер: " + autoClickers + "/сек");
                btnBuyPower.setText("Улучшить клик (Цена: " + powerCost + ")");
            }
        });
        layout.addView(btnBuyPower, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 130));

        Button btnBuyAuto = createShopButton("Купить Автокликер (Цена: " + autoCost + ")");
        LinearLayout.LayoutParams autoParams = new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 130);
        autoParams.setMargins(0, 20, 0, 0);
        btnBuyAuto.setOnClickListener(v -> {
            if (clickerScore >= autoCost) {
                clickerScore -= autoCost;
                autoClickers += 1;
                autoCost = (int)(autoCost * 1.6);
                tvScore.setText("Монеты: " + clickerScore);
                tvStats.setText("Клик: +" + clickPower + " | Автокликер: " + autoClickers + "/сек");
                btnBuyAuto.setText("Купить Автокликер (Цена: " + autoCost + ")");
            }
        });
        layout.addView(btnBuyAuto, autoParams);

        parent.addView(layout);

        autoClickHandler = new Handler(Looper.getMainLooper());
        autoClickRunnable = new Runnable() {
            @Override
            public void run() {
                if (autoClickers > 0) {
                    clickerScore += autoClickers;
                    tvScore.setText("Монеты: " + clickerScore);
                }
                autoClickHandler.postDelayed(this, 1000);
            }
        };
        autoClickHandler.postDelayed(autoClickRunnable, 1000);
    }

    private Button createShopButton(String text) {
        Button btn = new Button(this);
        btn.setText(text);
        btn.setTextColor(Color.WHITE);
        btn.setTextSize(14);
        GradientDrawable shape = new GradientDrawable();
        shape.setCornerRadius(20);
        shape.setColor(Color.parseColor("#222222"));
        shape.setStroke(2, Color.parseColor("#333333"));
        btn.setBackground(shape);
        return btn;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (autoClickHandler != null && autoClickRunnable != null) {
            autoClickHandler.removeCallbacks(autoClickRunnable);
        }
    }

    private String getTetrisHtml() {
        return "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'><style>" +
            "body{background:#121212;color:#fff;text-align:center;font-family:sans-serif;margin:0;padding:15px;}" +
            "h2{color:#BB86FC;margin:5px 0; font-size: 22px;}" +
            "#score{font-size:20px;color:#03DAC6;margin-bottom:15px;}" +
            "canvas{background:#000;border:3px solid #252525;border-radius:10px;display:block;margin:0 auto;height:55vh;}" +
            ".ctrl-panel{margin-top:20px;display:flex;justify-content:center;gap:10px;}" +
            ".btn{padding:15px;background:#1F1F1F;color:#BB86FC;border:2px solid #BB86FC;border-radius:12px;font-size:24px;font-weight:bold;flex:1;max-width:80px;}" +
            "</style></head><body>" +
            "<h2>ТЕТРИС ОФЛАЙН</h2>" +
            "<div id='score'>Счёт: 0</div>" +
            "<canvas id='tetris' width='240' height='400'></canvas>" +
            "<div class='ctrl-panel'>" +
            "<button class='btn' onclick='move(-1)'>◀</button>" +
            "<button class='btn' onclick='rotate()'>🔄</button>" +
            "<button class='btn' onclick='drop()'>▼</button>" +
            "<button class='btn' onclick='move(1)'>▶</button>" +
            "</div>" +
            "<script>" +
            "const canvas=document.getElementById('tetris');const context=canvas.getContext('2d');context.scale(20,20);" +
            "let scoreCount=0;" +
            "const colors=[null,'#FF0D72','#0DC2FF','#0DFF72','#F538FF','#FF8E0D','#FFE138','#3877FF'];" +
            "const arena=createMatrix(12,20);const pieces='ILJOTSZ';const player={pos:{x:0,y:0},matrix:null};" +
            "function createMatrix(w,h){const matrix=[];while(h--)matrix.push(new Array(w).fill(0));return matrix;}" +
            "function createPiece(type){" +
            " if(type==='I')return[[0,1,0,0],[0,1,0,0],[0,1,0,0],[0,1,0,0]];" +
            " else if(type==='L')return[[0,2,0],[0,2,0],[0,2,2]];" +
            " else if(type==='J')return[[0,3,0],[0,3,0],[3,3,0]];" +
            " else if(type==='O')return[[4,4],[4,4]];" +
            " else if(type==='T')return[[0,5,0],[5,5,5],[0,0,0]];" +
            " else if(type==='S')return[[0,6,6],[6,6,0],[0,0,0]];" +
            " else if(type==='Z')return[[7,7,0],[0,7,7],[0,0,0]];" +
            "}" +
            "function drawMatrix(matrix,offset){matrix.forEach((row,y)=>{row.forEach((value,x)=>{if(value!==0){context.fillStyle=colors[value];context.fillRect(x+offset.x,y+offset.y,1,1);}});});}" +
            "function draw(){context.fillStyle='#000';context.fillRect(0,0,canvas.width,canvas.height);drawMatrix(arena,{x:0,y:0});drawMatrix(player.matrix,player.pos);}" +
            "function arenaSweep(){out:for(let y=arena.length-1;y>0;--y){for(let x=0;x<arena[y].length;++x){if(arena[y][x]===0)continue out;}const row=arena.splice(y,1)[0].fill(0);arena.unshift(row);++y;scoreCount+=10;document.getElementById('score').innerText='Счёт: '+scoreCount;}}" +
            "function collide(arena,player){const [m,o]=[player.matrix,player.pos];for(let y=0;y<m.length;++y){for(let x=0;x<m[y].length;++x){if(m[y][x]!==0&&(arena[y+o.y]&&arena[y+o.y][x+o.x])!==0){return true;}}}return false;}" +
            "function merge(arena,player){player.matrix.forEach((row,y)=>{row.forEach((value,x)=>{if(value!==0){arena[y+player.pos.y][x+player.pos.x]=value;}});});}" +
            "function playerReset(){player.matrix=createPiece(pieces[pieces.length*Math.random()|0]);player.pos.y=0;player.pos.x=(arena[0].length/2|0)-(player.matrix[0].length/2|0);if(collide(arena,player)){arena.forEach(row=>row.fill(0));scoreCount=0;document.getElementById('score').innerText='Счёт: 0';}}" +
            "function playerDrop(){player.pos.y++;if(collide(arena,player)){player.pos.y--;merge(arena,player);playerReset();arenaSweep();}dropCounter=0;}" +
            "function move(dir){player.pos.x+=dir;if(collide(arena,player)){player.pos.x-=dir;}}" +
            "function rotate(){const m=player.matrix;for(let y=0;y<m.length;++y){for(let x=0;x<y;++x)[m[x][y],m[y][x]]=[m[y][x],m[x][y]];}m.forEach(row=>row.reverse());if(collide(arena,player)){m.forEach(row=>row.reverse());}}" +
            "function drop(){playerDrop();draw();}" +
            "let dropCounter=0;let lastTime=0;" +
            "function update(time=0){const deltaTime=time-lastTime;lastTime=time;dropCounter+=deltaTime;if(dropCounter>1000){playerDrop();}draw();requestAnimationFrame(update);}" +
            "playerReset();update();" +
            "</script></body></html>";
    }

    private String getSnakeHtml() {
        return "<html><head><meta name='viewport' content='width=device-width, initial-scale=1.0, user-scalable=no'><style>" +
            "body{background:#121212;color:#fff;text-align:center;font-family:sans-serif;margin:0;padding:15px;}" +
            "h2{color:#03DAC6;margin:5px 0; font-size: 22px;}" +
            "#score{font-size:20px;color:#FF0D72;margin-bottom:15px;}" +
            "canvas{background:#000;border:3px solid #252525;border-radius:10px;display:block;margin:0 auto;height:50vh;}" +
            ".ctrl-panel{margin-top:15px;}" +
            ".btn{padding:12px 25px;margin:4px;background:#1F1F1F;color:#03DAC6;border:2px solid #03DAC6;border-radius:12px;font-size:24px;font-weight:bold;min-width:65px;}" +
            "</style></head><body>" +
            "<h2>ЗМЕЙКА ОФЛАЙН</h2>" +
            "<div id='score'>Длина: 5</div>" +
            "<canvas id='gc' width='300' height='300'></canvas>" +
            "<div class='ctrl-panel'>" +
            "<button class='btn' onclick='press(38)'>▲</button><br>" +
            "<button class='btn' onclick='press(37)'>◀</button>" +
            "<button class='btn' onclick='press(40)'>▼</button>" +
            "<button class='btn' onclick='press(39)'>▶</button>" +
            "</div>" +
            "<script>" +
            "const canvas=document.getElementById('gc');const ctx=canvas.getContext('2d');" +
            "let px=10,py=10;let gs=15,tc=20;let ax=5,ay=5;let xv=0,yv=0;let trail=[];let tail=5;let gameStarted=false;" +
            "function game(){" +
            " if(!gameStarted && (xv!==0 || yv!==0)) gameStarted=true;" +
            " px+=xv;py+=yv;" +
            " if(gameStarted){" +
            "  if(px<0 || px>tc-1 || py<0 || py>tc-1){ resetGame(); return; }" +
            " }" +
            " ctx.fillStyle='black';ctx.fillRect(0,0,canvas.width,canvas.height);" +
            " ctx.fillStyle='#03DAC6';" +
            " for(var i=0;i<trail.length;i++){" +
            "  ctx.fillRect(trail[i].x*gs,trail[i].y*gs,gs-2,gs-2);" +
            "  if(gameStarted && trail[i].x===px && trail[i].y===py){ resetGame(); return; }" +
            " }" +
            " trail.push({x:px,y:py});" +
            " while(trail.length>tail)trail.shift();" +
            " if(ax===px && ay===py){ tail++; ax=Math.floor(Math.random()*tc); ay=Math.floor(Math.random()*tc); document.getElementById('score').innerText='Длина: '+tail; }" +
            " ctx.fillStyle='#FF0D72';ctx.fillRect(ax*gs,ay*gs,gs-2,gs-2);" +
            "}" +
            "function resetGame(){ xv=yv=0; px=10; py=10; tail=5; trail=[]; gameStarted=false; document.getElementById('score').innerText='ВРЕЗАЛСЯ! Длина: 5'; ax=Math.floor(Math.random()*tc); ay=Math.floor(Math.random()*tc); }" +
            "function press(k){" +
            " if(k==37 && xv!=1){xv=-1;yv=0;}" +
            " if(k==38 && yv!=1){xv=0;yv=-1;}" +
            " if(k==39 && xv!=-1){xv=1;yv=0;}" +
            " if(k==40 && yv!=-1){xv=0;yv=1;}" +
            "}" +
            "setInterval(game,1000/10);" +
            "</script></body></html>";
    }
}
