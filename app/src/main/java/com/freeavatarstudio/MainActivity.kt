package com.freeavatarstudio

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.webkit.JavascriptInterface
import android.webkit.ValueCallback
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import java.util.Locale

class MainActivity : Activity(), TextToSpeech.OnInitListener {
    private lateinit var webView: WebView
    private var tts: TextToSpeech? = null
    private var ttsReady = false
    private var fileCallback: ValueCallback<Array<Uri>>? = null
    private val pickAudio = 1001

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        tts = TextToSpeech(this, this)
        webView = WebView(this)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.settings.allowFileAccess = true
        webView.webViewClient = WebViewClient()
        webView.addJavascriptInterface(AndroidBridge(), "Android")
        webView.webChromeClient = object : WebChromeClient() {
            override fun onShowFileChooser(view: WebView?, callback: ValueCallback<Array<Uri>>?, params: FileChooserParams?): Boolean {
                fileCallback?.onReceiveValue(null)
                fileCallback = callback
                val intent = Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                    addCategory(Intent.CATEGORY_OPENABLE)
                    type = "audio/*"
                }
                startActivityForResult(intent, pickAudio)
                return true
            }
        }
        setContentView(webView)
        webView.loadUrl("file:///android_asset/index.html")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            ttsReady = true
            tts?.language = Locale.getDefault()
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(id: String?) = runOnUiThread { webView.evaluateJavascript("window.setTalking(true)", null) }
                override fun onDone(id: String?) = runOnUiThread { webView.evaluateJavascript("window.setTalking(false)", null) }
                @Deprecated("Deprecated in Java")
                override fun onError(id: String?) = runOnUiThread { webView.evaluateJavascript("window.setTalking(false)", null) }
            })
        }
    }

    inner class AndroidBridge {
        @JavascriptInterface fun speak(text: String) {
            runOnUiThread {
                if (ttsReady) tts?.speak(text.take(3000), TextToSpeech.QUEUE_FLUSH, null, "avatar-utterance")
                else webView.evaluateJavascript("window.showMessage('Text-to-speech is not ready yet')", null)
            }
        }
        @JavascriptInterface fun stop() {
            runOnUiThread {
                tts?.stop()
                webView.evaluateJavascript("window.setTalking(false)", null)
            }
        }
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == pickAudio) {
            val uri = if (resultCode == Activity.RESULT_OK) data?.data else null
            if (uri != null) {
                try { contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION) } catch (_: Exception) {}
                fileCallback?.onReceiveValue(arrayOf(uri))
            } else fileCallback?.onReceiveValue(null)
            fileCallback = null
        }
    }

    override fun onDestroy() {
        tts?.stop()
        tts?.shutdown()
        webView.destroy()
        super.onDestroy()
    }
}
