package com.example.androidwalletdemo.util

import com.google.gson.Gson
import java.io.IOException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.stellar.sdk.Transaction

// TODO: move to Wallet SDK
suspend fun signWithRecoveryServer(
  rsEndpoint: String,
  transaction: Transaction,
  token: String,
  accountAddress: String,
  signerAddress: String
): String {
  data class AuthTransaction(val transaction: String)
  data class AuthSignature(val signature: String)

  // TODO: optimize gson
  val gson = Gson()
  val client = OkHttpClient()

  val jsonContentType = "application/json; charset=utf-8"
  val jsonContentMediaType = jsonContentType.toMediaType()

  return CoroutineScope(Dispatchers.IO)
    .async {
      val request =
        Request.Builder()
          .url("$rsEndpoint/accounts/$accountAddress/sign/$signerAddress")
          .header("Content-Type", jsonContentType)
          .addHeader("Authorization", "Bearer $token")
          .post(
            gson
              .toJson(AuthTransaction(transaction.toEnvelopeXdrBase64()))
              .toRequestBody(jsonContentMediaType)
          )
          .build()

      client.newCall(request).execute().use { response ->
        if (!response.isSuccessful) throw IOException("Unexpected code $response")

        val authResponse = gson.fromJson(response.body!!.charStream(), AuthSignature::class.java)

        return@async authResponse.signature
      }
    }
    .await()
}
