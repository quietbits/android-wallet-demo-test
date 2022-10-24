package com.example.androidwalletdemo.util

import android.net.http.SslError
import android.webkit.SslErrorHandler
import android.webkit.WebView
import android.webkit.WebViewClient

internal class UnsafeWebViewClient : WebViewClient() {
  override fun onReceivedSslError(view: WebView, handler: SslErrorHandler, error: SslError) {
    // Ignore SSL certificate errors
    handler.proceed()
  }
}
