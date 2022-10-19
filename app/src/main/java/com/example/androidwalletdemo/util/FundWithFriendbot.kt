package com.example.androidwalletdemo.util

import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import okhttp3.Request

suspend fun fundWithFriendbot(accountAddress: String): Boolean {
  data class AccountInfo(val successful: Boolean)

  val gson = GsonUtil.instance!!
  val client = OkHttpUtil.unsafeOkhttpClient()

  val url = "https://friendbot.stellar.org/?addr=$accountAddress"

  return CoroutineScope(Dispatchers.IO)
    .async {
      val request = Request.Builder().url(url).build()

      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val accountInfo = gson.fromJson(response.body!!.charStream(), AccountInfo::class.java)

        return@async accountInfo.successful
      }
    }
    .await()
}
