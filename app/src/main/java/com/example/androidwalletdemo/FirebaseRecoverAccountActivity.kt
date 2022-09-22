package com.example.androidwalletdemo

import android.app.Activity
import android.os.Bundle
import android.util.Log
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseException
import com.google.firebase.FirebaseOptions
import com.google.firebase.auth.*
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.firebase.ktx.app
import com.google.firebase.ktx.initialize
import java.util.concurrent.TimeUnit
import kotlin.Exception

class FirebaseRecoverAccountActivity : Activity() {
  val logTag = ">>> Firebase"

  private lateinit var auth: FirebaseAuth
  private lateinit var callbacks: PhoneAuthProvider.OnVerificationStateChangedCallbacks

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    val rsConfigName =
      intent.extras?.get("rsName") ?: throw Exception("Recovery server name is required")

    val config =
      if (rsConfigName == "recoveryServer1") {
        rsConfig1
      } else {
        rsConfig2
      }

    val options =
      FirebaseOptions.Builder()
        .setProjectId(config.projectId)
        .setApplicationId(config.appId)
        .setApiKey(config.apiKey)
        .build()

    Firebase.initialize(this, options, rsConfigName as String)
    val firebaseApp = Firebase.app(rsConfigName)
    auth = Firebase.auth(firebaseApp)

    callbacks =
      object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {

        override fun onVerificationCompleted(credential: PhoneAuthCredential) {
          Log.d(logTag, "onVerificationCompleted:$credential")
        }

        override fun onVerificationFailed(e: FirebaseException) {
          Log.w(logTag, "onVerificationFailed", e)
        }

        override fun onCodeSent(
          verificationId: String,
          token: PhoneAuthProvider.ForceResendingToken
        ) {
          Log.d(logTag, "onCodeSent:$verificationId")

          val smsCode = intent.extras?.get("smsCode") ?: throw Exception("SMS code not provided")
          val credential = PhoneAuthProvider.getCredential(verificationId, smsCode as String)

          signInWithPhoneAuthCredential(credential, auth)
        }
      }
  }

  override fun onStart() {
    super.onStart()

    val phoneNumber = intent.extras?.get("phoneNumber")

    if (phoneNumber != null) {
      startPhoneNumberVerification(phoneNumber as String, auth)
    }
  }

  private fun startPhoneNumberVerification(phoneNumber: String, auth: FirebaseAuth) {
    val options =
      PhoneAuthOptions.newBuilder(auth)
        .setPhoneNumber(phoneNumber)
        .setTimeout(60L, TimeUnit.SECONDS)
        .setActivity(this)
        .setCallbacks(callbacks)
        .build()

    PhoneAuthProvider.verifyPhoneNumber(options)
  }

  private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, auth: FirebaseAuth) {
    auth.signInWithCredential(credential).addOnCompleteListener(this) { task ->
      if (task.isSuccessful) {
        // Sign in success, update UI with the signed-in user's information
        Log.d(logTag, "signInWithCredential:success")

        val user = task.result?.user

        user
          ?.getIdToken(true)
          ?.addOnCompleteListener(
            OnCompleteListener<GetTokenResult>() {
              if (it.isSuccessful) {
                val token = it.result.token

                if (token != null) {
                  intent.putExtra("authToken", token)
                  setResult(200, intent)
                  finish()
                } else {
                  Log.d(logTag, "No token")
                }
              }
            }
          )
      } else {
        // Sign in failed, display a message and update the UI
        Log.w(logTag, "signInWithCredential:failure", task.exception)
      }
    }
  }
}
