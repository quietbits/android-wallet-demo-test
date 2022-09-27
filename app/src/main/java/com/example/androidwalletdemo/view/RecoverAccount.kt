package com.example.androidwalletdemo.view

import android.content.Context
import android.content.Intent
import android.util.Base64
import android.util.Log
import androidx.activity.compose.ManagedActivityResultLauncher
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt
import androidx.navigation.NavHostController
import com.example.androidwalletdemo.*
import com.example.androidwalletdemo.component.PageLayout
import com.example.androidwalletdemo.component.TextWithLabel
import com.example.androidwalletdemo.util.fetchStellarAddress
import com.example.androidwalletdemo.util.shortenString
import kotlinx.coroutines.launch
import org.stellar.sdk.*
import org.stellar.walletsdk.RecoveryServerAuth
import org.stellar.walletsdk.Wallet

const val logTag = ">>> RecoverAccount"

const val horizonUrl = "https://horizon-testnet.stellar.org"
const val networkPassphrase = "Test SDF Network ; September 2015"

const val recoveryServer1Name = "recoveryServer1"
const val recoveryServer2Name = "recoveryServer2"

fun base64Decoder(baseString: String): ByteArray {
  return Base64.decode(baseString, Base64.DEFAULT)
}

@Composable
fun RecoverAccount(navController: NavHostController) {
  val context = LocalContext.current
  val screenScope = rememberCoroutineScope()

  PageLayout("Recover account", navController) {
    // =============================================================================================
    // VIEW STATE
    // =============================================================================================

    val (inProgress, setInProgress) = remember { mutableStateOf(false) }
    val (txnSuccess, setTxnSuccess) = remember { mutableStateOf(false) }
    val (hasAuthTokens, setHasAuthTokens) = remember { mutableStateOf(false) }
    val (hasRecoverySignatures, setHasRecoverySignatures) = remember { mutableStateOf(false) }

    val (authToken1, setAuthToken1) = remember { mutableStateOf("") }
    val (authToken2, setAuthToken2) = remember { mutableStateOf("") }
    val (accountStellarAddress, setAccountStellarAddress) = remember { mutableStateOf("") }
    val (newDevicePublicKey, setNewDevicePublicKey) = remember { mutableStateOf("") }

    val (txn, setTxn) = remember { mutableStateOf<Transaction?>(null) }

    val userPhoneNumberState = remember { mutableStateOf(TextFieldValue(defaultPhoneNumber)) }
    val userSmsCodeState = remember { mutableStateOf(TextFieldValue(defaultSmsCode)) }

    // =============================================================================================
    // APP BACKEND
    // =============================================================================================
    // Get auth tokens from recovery servers and account Stellar address from DB
    // =============================================================================================

    val rs1Intent =
      createRsIntent(
        context = context,
        recoveryServerName = recoveryServer1Name,
        phoneNumber = userPhoneNumberState.value.text,
        smsCode = userSmsCodeState.value.text
      )

    val rs2Intent =
      createRsIntent(
        context = context,
        recoveryServerName = recoveryServer2Name,
        phoneNumber = userPhoneNumberState.value.text,
        smsCode = userSmsCodeState.value.text
      )

    val startForResult1 = rsIntentResultLauncher(setAuthToken1)
    val startForResult2 = rsIntentResultLauncher(setAuthToken2)

    if (authToken1.isNotBlank() && authToken2.isNotBlank()) {
      setHasAuthTokens(true)
    }

    if (hasAuthTokens && accountStellarAddress.isEmpty()) {
      Log.d(logTag, "authToken1: $authToken1")
      Log.d(logTag, "authToken2: $authToken2")

      // Get account Stellar address using RS1 auth token
      LaunchedEffect(true) {
        screenScope.launch { setAccountStellarAddress(fetchStellarAddress(dbEndpoint, authToken1)) }
      }
    }

    // =============================================================================================
    // WALLET SDK: CREATE TRANSACTION
    // =============================================================================================
    // *** Must be on the client
    // Generate new device keypair, save secret key in KeyStore, create "Add signer" transaction
    // This transaction can be sponsored
    // Remove old signer operation can be added
    // =============================================================================================

    // Init wallet
    val wallet = Wallet(horizonUrl, networkPassphrase)

    if (txn == null && accountStellarAddress.isNotBlank()) {
      Log.d(logTag, "accountStellarAddress: $accountStellarAddress")

      // Generate new device keypair
      val deviceKeypair = wallet.create()
      val devicePublicKey = deviceKeypair.publicKey

      //  Secret key should be stored in KeyStore, saving on the UI for demo only
      val deviceSecretKey = deviceKeypair.secretKey

      Log.d(logTag, "newDevicePublicKey: $devicePublicKey")
      Log.d(logTag, "newDeviceSecretKey: $deviceSecretKey")

      // New transaction to add new device key as a signer with max weight
      LaunchedEffect(true) {
        screenScope.launch {
          setTxn(
            wallet.addAccountSigner(
              sourceAddress = accountStellarAddress,
              signerAddress = devicePublicKey,
              signerWeight = 20,
            )
          )
          setNewDevicePublicKey(devicePublicKey)
        }
      }
    }

    // =============================================================================================
    // WALLET SDK: RECOVERY SERVERS SIGN TRANSACTION
    // =============================================================================================
    // *** Must be on the client
    // Each recovery server signs the transaction
    // New device signer doesn't sign this transaction
    // =============================================================================================

    if (txn != null && !hasRecoverySignatures) {
      LaunchedEffect(true) {
        screenScope.launch {
          setTxn(
            wallet.signWithRecoveryServers(
              transaction = txn,
              accountAddress = accountStellarAddress,
              recoveryServers =
                listOf(
                  RecoveryServerAuth(
                    endpoint = recoveryServer1.endpoint,
                    signerAddress = recoveryServer1.stellarAddress,
                    authToken = authToken1
                  ),
                  RecoveryServerAuth(
                    endpoint = recoveryServer2.endpoint,
                    signerAddress = recoveryServer2.stellarAddress,
                    authToken = authToken2
                  )
                ),
              base64Decoder = ::base64Decoder
            )
          )
          setHasRecoverySignatures(true)
        }
      }
    }

    // =============================================================================================
    // WALLET SDK: SUBMIT TRANSACTION TO THE NETWORK
    // =============================================================================================
    // For sponsored transaction, sign the transaction and use fee bump to cover fees
    // =============================================================================================

    if (hasRecoverySignatures && inProgress) {
      Log.d(logTag, "signed txn: ${txn!!.toEnvelopeXdrBase64()}")

      LaunchedEffect(true) {
        screenScope.launch {
          val success = wallet.submitTransaction(txn)
          setInProgress(false)
          setTxnSuccess(success)

          Log.d(logTag, "submit success: $success")
        }
      }
    }

    // =============================================================================================
    // UI ELEMENTS
    // =============================================================================================

    Text(
      "Enter user's phone number and SMS code to start account recovery",
      modifier = Modifier.padding(bottom = 8.dp)
    )
    TextField(
      value = userPhoneNumberState.value,
      onValueChange = { userPhoneNumberState.value = it },
      label = { Text("User phone number") },
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
    TextField(
      value = userSmsCodeState.value,
      onValueChange = { userSmsCodeState.value = it },
      label = { Text("SMS code") },
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )

    if (!inProgress && !txnSuccess) {
      Button(
        onClick = {
          startForResult1.launch(rs1Intent)
          startForResult2.launch(rs2Intent)
          setInProgress(true)
        },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
      ) {
        Text("Start recovery")
      }
    }

    if (inProgress) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
    }

    if (txn != null || inProgress) {
      TextWithLabel(label = "Auth token 1", text = shortenString(authToken1))
      TextWithLabel(label = "Auth token 2", text = shortenString(authToken2))
      TextWithLabel(label = "Account Stellar address", text = shortenString(accountStellarAddress))
      TextWithLabel(label = "New device public key", text = shortenString(newDevicePublicKey))
    }

    if (txnSuccess) {
      Text(
        "New device signer added",
        color = Color("#8bbe1b".toColorInt()),
        fontSize = 20.sp,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(0.dp, 8.dp)
      )
    }
  }
}

fun createRsIntent(
  context: Context,
  recoveryServerName: String,
  phoneNumber: String,
  smsCode: String,
): Intent {
  val rsIntent = Intent(context, FirebaseRecoverAccountActivity::class.java)

  rsIntent.putExtra("rsName", recoveryServerName)
  rsIntent.putExtra("phoneNumber", phoneNumber)
  rsIntent.putExtra("smsCode", smsCode)

  return rsIntent
}

@Composable
fun rsIntentResultLauncher(
  setState: (String) -> Unit
): ManagedActivityResultLauncher<Intent, ActivityResult> {
  return rememberLauncherForActivityResult(
    contract = ActivityResultContracts.StartActivityForResult(),
    onResult = {
      val authToken = it.data?.extras?.get("authToken")

      if (authToken != null) {
        setState(authToken as String)
      }
    }
  )
}
