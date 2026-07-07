package com.example.customkeyboard

import android.inputmethodservice.InputMethodService
import android.inputmethodservice.Keyboard
import android.inputmethodservice.KeyboardView
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import android.widget.GridLayout

class MyCustomKeyboard : InputMethodService(), KeyboardView.OnKeyboardActionListener {

    private lateinit var container: FrameLayout
    private lateinit var webView: WebView

    override fun onCreateInputView(): View {
        val layout = layoutInflater.inflate(R.layout.keyboard_view, null)

        container = layout.findViewById(R.id.content_container)
        webView = layout.findViewById(R.id.keyboard_webview)
        
        webView.settings.javaScriptEnabled = true
        webView.webViewClient = WebViewClient()

        val kv = layout.findViewById<KeyboardView>(R.id.keyboard_view)
        kv.keyboard = Keyboard(this, R.xml.qwerty_layout)
        kv.setOnKeyboardActionListener(this)

        layout.findViewById<Button>(R.id.btn_youtube).setOnClickListener {
            showWebView()
            webView.loadUrl("https://m.youtube.com")
        }

        layout.findViewById<Button>(R.id.btn_clicker).setOnClickListener { openClicker() }
        layout.findViewById<Button>(R.id.btn_tic_tac).setOnClickListener { openTicTacToe() }
        
        layout.findViewById<Button>(R.id.btn_tetris).setOnClickListener {
            showWebView()
            webView.loadUrl("file:///android_asset/tetris.html")
        }

        layout.findViewById<Button>(R.id.btn_snake).setOnClickListener {
            showWebView()
            webView.loadUrl("file:///android_asset/snake.html")
        }

        return layout
    }

    private fun showWebView() {
        container.removeAllViews()
        webView.visibility = View.VISIBLE
        container.addView(webView)
    }

    private fun openClicker() {
        webView.visibility = View.GONE
        container.removeAllViews()
        val view = layoutInflater.inflate(R.layout.game_clicker, null)
        val tvScore = view.findViewById<TextView>(R.id.tv_score)
        var score = 0
        view.findViewById<Button>(R.id.btn_click).setOnClickListener {
            score++
            tvScore.text = "Монеты: $score"
        }
        container.addView(view)
    }

    private fun openTicTacToe() {
        webView.visibility = View.GONE
        container.removeAllViews()
        val view = layoutInflater.inflate(R.layout.game_tictactoe, null)
        val gridLayout = view.findViewById<GridLayout>(R.id.grid_board)
        var isXTurn = true
        val board = Array(9) { "" }

        for (i in 0 until 9) {
            val button = gridLayout.getChildAt(i) as Button
            button.text = ""
            button.setOnClickListener {
                if (button.text.isEmpty() && board[i].isEmpty()) {
                    button.text = if (isXTurn) "X" else "O"
                    board[i] = button.text.toString()
                    isXTurn = !isXTurn
                }
            }
        }
        view.findViewById<Button>(R.id.btn_reset_tic)?.setOnClickListener { openTicTacToe() }
        container.addView(view)
    }

    override fun onKey(primaryCode: Int, keyCodes: IntArray?) {
        val ic = currentInputConnection ?: return
        when (primaryCode) {
            -5 -> ic.deleteSurroundingText(1, 0)
            else -> ic.commitText(primaryCode.toChar().toString(), 1)
        }
    }
    override fun onPress(primaryCode: Int) {}
    override fun onRelease(primaryCode: Int) {}
    override fun onText(text: CharSequence?) {}
    override fun swipeLeft() {}
    override fun swipeRight() {}
    override fun swipeDown() {}
    override fun swipeUp() {}
}
