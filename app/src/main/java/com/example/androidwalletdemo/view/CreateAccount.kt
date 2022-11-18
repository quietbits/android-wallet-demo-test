package com.example.androidwalletdemo.view

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.androidwalletdemo.*
import com.example.androidwalletdemo.component.PageLayout
import com.example.androidwalletdemo.component.SuccessMessage
import com.example.androidwalletdemo.component.TextListWithLabel
import com.example.androidwalletdemo.component.TextWithLabel
import com.example.androidwalletdemo.util.fundWithFriendbot
import com.example.androidwalletdemo.util.shortenString
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.stellar.sdk.Network
import org.stellar.sdk.Server
import org.stellar.walletsdk.*

const val logTagCreateAccount = ">>> CreateAccount"

data class RecoverableWallet(
  val address: String,
  val threshold: AccountThreshold,
  val signer: List<AccountSigner>,
  val identity: List<RecoveryAccountIdentity>,
)

@Composable
fun CreateAccount(navController: NavHostController) {
  val screenScope = CoroutineScope(Dispatchers.IO)

  PageLayout("Create account", navController) {
    // =============================================================================================
    // VIEW STATE
    // =============================================================================================

    val (inProgress, setInProgress) = remember { mutableStateOf(false) }
    val userPhoneNumberState = remember { mutableStateOf(TextFieldValue(defaultPhoneNumberCreate)) }
    val (recoverableWallet, setRecoverableWallet) =
      remember { mutableStateOf<RecoverableWallet?>(null) }

    // =============================================================================================
    // WALLET SDK: GENERATE ACCOUNT AND DEVICE KEYPAIRS
    // =============================================================================================
    // *** Must be on the client
    // Generate new account and device keypairs, save secret keys in KeyStore
    // Note: this demo doesn't show app backend/database side
    // =============================================================================================

    // Init wallet
    val server = Server(horizonUrl)
    val network = Network(networkPassphrase)
    val wallet = Wallet(server, network, baseFee)

    if (inProgress) {
      // Generate new account keypair
      val accountKeypair = wallet.create()
      val accountPublicKey = accountKeypair.publicKey
      //  Secret key should be stored in KeyStore, saving on the UI for demo only
      val accountSecretKey = accountKeypair.secretKey

      // Generate new account keypair
      val deviceKeypair = wallet.create()
      val devicePublicKey = deviceKeypair.publicKey
      //  Secret key should be stored in KeyStore, saving on the UI for demo only
      val deviceSecretKey = deviceKeypair.secretKey

      Log.d(logTagCreateAccount, "account public key: $accountPublicKey")
      Log.d(logTagCreateAccount, "account secret key: $accountSecretKey")
      Log.d(logTagCreateAccount, "device public key: $devicePublicKey")
      Log.d(logTagCreateAccount, "device secret key: $deviceSecretKey")

      LaunchedEffect(true) {
        screenScope.launch {

          // =====================================================================================
          // WALLET SDK: CREATE RECOVERABLE WALLET
          // =====================================================================================

          // Account and device transaction signers
          val accountWalletSigner = AppWalletSigner(accountSecretKey)
          val deviceWalletSigner = AppWalletSigner(deviceSecretKey)

          // Register account with recovery servers (can be done for account that does not exist
          // on the network yet)
          val enrolledRecoverySigners =
            wallet.enrollWithRecoveryServer(
              recoveryServers = listOf(recoveryServer1, recoveryServer2),
              accountAddress = accountPublicKey,
              accountIdentity =
                listOf(
                  RecoveryAccountIdentity(
                    // Role can be "owner", "sender", or "receiver"
                    role = "owner",
                    auth_methods =
                      // Account auth methods (phone number, email, etc)
                      listOf(
                        RecoveryAccountAuthMethod(
                          type = "phone_number",
                          value = userPhoneNumberState.value.text
                        )
                      )
                  )
                ),
              walletSigner = accountWalletSigner
            )

          // Creating a list of signers to add to the account
          val recoveryServerSigners =
            enrolledRecoverySigners
              .map { rs -> AccountSigner(address = rs, weight = signerRecoveryWeight) }
              .toTypedArray()

          val signer =
            listOf(
              *recoveryServerSigners,
              AccountSigner(address = devicePublicKey, weight = signerMasterWeight)
            )

          // Fund account with friendbot (only on testnet) or sponsor creation of the account
          val isFunded = fundWithFriendbot(accountPublicKey)

          if (isFunded) {
            Log.d(logTagCreateAccount, "Friendbot funded account")

            // Create transaction to add new signers and thresholds to the account
            // This transaction can be sponsored
            val newWalletTransaction =
              wallet.registerRecoveryServerSigners(
                accountAddress = accountPublicKey,
                accountSigner = signer,
                accountThreshold =
                  AccountThreshold(
                    low = signerMasterWeight,
                    medium = signerMasterWeight,
                    high = signerMasterWeight
                  ),
              )

            // Sign transaction with account master key
            val signedTxn = accountWalletSigner.signWithClientAccount(newWalletTransaction)
            // If sponsored, sponsor signs transaction and creates fee bump transaction (can be
            // done on the backend)

            // Submit transaction to the network (can be done on the backend)
            val txnSuccess = wallet.submitTransaction(signedTxn)

            if (txnSuccess) {
              Log.d(logTagCreateAccount, "New account created")

              // Lock account master key using newly added device key (can be done later)
              val lockTxn =
                wallet.lockAccountMasterKey(
                  accountAddress = accountPublicKey,
                )

              // Sign lock transaction with device secret key
              val signedLockTxn = deviceWalletSigner.signWithClientAccount(lockTxn)
              // If sponsored, sponsor signs transaction and creates fee bump transaction (can be
              // done on the backend)

              // Submit lock master key transaction to the network (can be done on the backend)
              val lockTxnSuccess = wallet.submitTransaction(signedLockTxn)

              if (lockTxnSuccess) {
                Log.d(logTagCreateAccount, "Master key locked")
              }
            }
          } else {
            throw Exception("Account was not funded")
          }

          // =====================================================================================
          // DATA FOR LOGS AND UI
          // =====================================================================================

          val resultData =
            RecoverableWallet(
              address = accountPublicKey,
              threshold =
                AccountThreshold(
                  low = signerMasterWeight,
                  medium = signerMasterWeight,
                  high = signerMasterWeight
                ),
              signer =
                listOf(
                  AccountSigner(address = devicePublicKey, weight = signerMasterWeight),
                  AccountSigner(
                    address = recoveryServer1.stellarAddress,
                    weight = signerRecoveryWeight
                  ),
                  AccountSigner(
                    address = recoveryServer2.stellarAddress,
                    weight = signerRecoveryWeight
                  )
                ),
              identity =
                listOf(
                  RecoveryAccountIdentity(
                    role = "owner",
                    auth_methods =
                      listOf(
                        RecoveryAccountAuthMethod(
                          type = "phone_number",
                          value = userPhoneNumberState.value.text
                        )
                      )
                  )
                )
            )

          logResult(resultData)
          setRecoverableWallet(resultData)
          setInProgress(false)
        }
      }
    }

    // =============================================================================================
    // UI ELEMENTS
    // =============================================================================================

    Text(
      "Enter user's phone number to create a new account",
      modifier = Modifier.padding(bottom = 8.dp)
    )
    TextField(
      value = userPhoneNumberState.value,
      onValueChange = { userPhoneNumberState.value = it },
      label = { Text("User phone number") },
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )

    if (!inProgress && recoverableWallet == null) {
      Button(
        onClick = { setInProgress(true) },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
      ) {
        Text("Start")
      }
    }

    if (inProgress) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
    }

    if (!inProgress && recoverableWallet != null) {
      SuccessMessage(message = "New account created")

      TextWithLabel(label = "Account address", text = shortenString(recoverableWallet.address))

      recoverableWallet.identity.forEach { i ->
        TextWithLabel(label = "Account role", text = i.role)

        i.auth_methods.forEach { m ->
          TextWithLabel(label = "Account auth method", text = "${m.type} : ${m.value}")
        }
      }

      TextListWithLabel(
        label = "Signers",
        text = recoverableWallet.signer.map { s -> "${shortenString(s.address)} : ${s.weight}" }
      )

      val threshold = recoverableWallet.threshold

      TextListWithLabel(
        label = "Threshold",
        text =
          listOf("low : ${threshold.low}", "medium: ${threshold.medium}", "high: ${threshold.high}")
      )

      Text("Master key locked")
    }
  }
}

// =================================================================================================
// HELPERS
// =================================================================================================

fun logResult(recoverableWallet: RecoverableWallet) {
  Log.d(logTagCreateAccount, "Recoverable wallet created")
  Log.d(logTagCreateAccount, "Account address: ${recoverableWallet.address}")

  Log.d(logTagCreateAccount, "Account identity:")
  recoverableWallet.identity.forEach { i ->
    Log.d(logTagCreateAccount, "  Account role: ${i.role}")

    i.auth_methods.forEach { m ->
      Log.d(logTagCreateAccount, "  Account auth method: ${m.type} : ${m.value}")
    }
  }

  Log.d(logTagCreateAccount, "Signers:")
  recoverableWallet.signer.forEach { s ->
    Log.d(logTagCreateAccount, "  ${s.address} : ${s.weight}")
  }

  Log.d(logTagCreateAccount, "Threshold:")
  Log.d(logTagCreateAccount, "  low: ${recoverableWallet.threshold.low}")
  Log.d(logTagCreateAccount, "  medium: ${recoverableWallet.threshold.medium}")
  Log.d(logTagCreateAccount, "  high: ${recoverableWallet.threshold.high}")
}
