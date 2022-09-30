package com.example.androidwalletdemo.util

import android.annotation.SuppressLint
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.stellar.walletsdk.util.GsonUtils

object OkHttpUtil {
  private val gson = GsonUtils.instance!!
  private const val jsonContentType = "application/json; charset=utf-8"
  private val jsonContentMediaType = jsonContentType.toMediaType()

  fun buildStringGetRequest(url: String, authToken: String? = null): Request {
    val request = Request.Builder().url(url)

    if (authToken != null) {
      request.addHeader("Authorization", "Bearer $authToken")
    }

    return request.build()
  }

  fun <T> buildJsonPostRequest(url: String, requestParams: T, authToken: String? = null):
          Request {
    val request = Request.Builder().url(url).header("Content-Type", jsonContentType)

    if (authToken != null) {
      request.addHeader("Authorization", "Bearer $authToken")
    }

    val params = gson.toJson(requestParams).toRequestBody(jsonContentMediaType)

    return request.post(params).build()
  }

  fun <T> buildJsonPutRequest(url: String, requestParams: T, authToken: String? = null): Request {
    val request = Request.Builder().url(url).header("Content-Type", jsonContentType)

    if (authToken != null) {
      request.addHeader("Authorization", "Bearer $authToken")
    }

    return request.put(gson.toJson(requestParams).toRequestBody(jsonContentMediaType)).build()
  }

  //  Ignore certificates when testing
  private val trustAllCerts: Array<TrustManager> =
    arrayOf(
      @SuppressLint("CustomX509TrustManager")
      object : X509TrustManager {
        @SuppressLint("TrustAllX509TrustManager")
        override fun checkClientTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
        @SuppressLint("TrustAllX509TrustManager")
        override fun checkServerTrusted(chain: Array<X509Certificate?>?, authType: String?) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> {
          return arrayOf()
        }
      }
    )

  private val sslContext: SSLContext =
    SSLContext.getInstance("SSL").apply { init(null, trustAllCerts, SecureRandom()) }

  fun unsafeOkhttpClient(): OkHttpClient {
    val unsafeBuilder = OkHttpClient().newBuilder()
    unsafeBuilder.sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
    unsafeBuilder.hostnameVerifier(hostnameVerifier = { _, _ -> true })

    return unsafeBuilder.build()
  }
}
