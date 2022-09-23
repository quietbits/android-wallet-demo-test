package com.example.androidwalletdemo.util

import java.io.IOException
import kotlinx.coroutines.*
import okhttp3.OkHttpClient
import okhttp3.Request

suspend fun fetchStellarAddress(endpoint: String, token: String): String {
  data class UserInfo(val stellarAddress: String)

  val gson = GsonUtil.instance!!
  val client = OkHttpClient()

  return CoroutineScope(Dispatchers.IO)
    .async {
      val request =
        Request.Builder().url(endpoint).addHeader("Authorization", "Bearer $token").build()

      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val userInfo = gson.fromJson(response.body!!.charStream(), UserInfo::class.java)

        return@async userInfo.stellarAddress
      }
    }
    .await()
}
