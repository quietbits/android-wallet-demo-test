package com.example.androidwalletdemo.view

import android.util.Log
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import com.example.androidwalletdemo.component.PageLayout
import com.example.androidwalletdemo.defaultPhoneNumber
import com.example.androidwalletdemo.defaultSmsCode
import com.example.androidwalletdemo.recoveryServer1
import com.example.androidwalletdemo.recoveryServer2
import kotlinx.coroutines.launch
import org.stellar.sdk.*
import org.stellar.walletsdk.Wallet
import org.stellar.walletsdk.util.buildTransaction
import org.stellar.walletsdk.util.fetchAccount

// TODO: remove
private val server = Server("https://horizon-testnet.stellar.org")
private val network = Network("Test SDF Network ; September 2015")

const val signerMasterWeight = 20
const val signerRecoveryWeight = 10

@Composable
fun CreateAccount(navController: NavHostController) {
  val logTag = ">>> CreateAccount"

  val context = LocalContext.current
  val screenScope = rememberCoroutineScope()

  PageLayout("Create account", navController) {
    // =============================================================================================
    // VIEW STATE
    // =============================================================================================

    val (inProgress, setInProgress) = remember { mutableStateOf(false) }
    val (isAccountFunded, setIsAccountFunded) = remember { mutableStateOf(false) }

    val userPhoneNumberState = remember { mutableStateOf(TextFieldValue(defaultPhoneNumber)) }
    val userSmsCodeState = remember { mutableStateOf(TextFieldValue(defaultSmsCode)) }

    // =============================================================================================
    // STELLAR OPERATIONS
    // =============================================================================================

    // Init wallet
    val wallet = Wallet(horizonUrl, networkPassphrase)

    var accountPublicKey = ""
    var accountSecretKey = ""

    var devicePublicKey = ""
    var deviceSecretKey = ""

    var txn: Transaction? = null

    if (inProgress && !isAccountFunded) {

      //    TODO: create account keypair
      //    val accountKeypair = wallet.create()
      //    val accountPublicKey = accountKeypair.publicKey
      //    val accountSecretKey = accountKeypair.secretKey

      accountPublicKey = "GA3UFW5QSQSDXMJTZI4WZ74UAKCEWYXS7HUUEUX77ILIAUYCY4U5VOAM"
      accountSecretKey = "SBGFNKUHZ3PMPMJ4WWIJ7PIJ6ABEPFPV2V4YLLP66EL275UG4PRCQ7NV"

      Log.d(logTag, "account public key: $accountPublicKey")
      Log.d(logTag, "account secret key: $accountSecretKey")

      //    TODO: create device keypair
      //    val deviceKeypair = wallet.create()
      //    val devicePublicKey = deviceKeypair.publicKey
      //    val deviceSecretKey = deviceKeypair.secretKey

      devicePublicKey = "GDXPUDJFEKSIUJEWXM6PM335BCNLVJ5E5GL7UVNOFYI7BM6MBST7LGS3"
      deviceSecretKey = "SD5K3ZZHJPI65JHIOGRIUW7EC6TJFW3TDTDL23XE3SOPKX66PQL6EBP3"

      Log.d(logTag, "device public key: $devicePublicKey")
      Log.d(logTag, "device secret key: $deviceSecretKey")

      //    TODO: txn to create account
      //    TODO: fund account with friendbot

      LaunchedEffect(true) {
        screenScope.launch {
          //          TODO: put back
          //          val isFunded = fundWithFriendbot(accountPublicKey)
          val isFunded = true

          if (isFunded) {
            Log.d(logTag, "Friendbot funded account")
            //            setIsAccountFunded(true)

            testStellarStuff(
              wallet = wallet,
              accountPublicKey,
              accountSecretKey,
              devicePublicKey,
              deviceSecretKey
            )

            Log.d(logTag, "testTxn created")
          } else {
            throw Exception("Account was not funded")
          }
        }
      }
    }

    // =============================================================================================
    // UI ELEMENTS
    // =============================================================================================

    Text(
      "Enter user's phone number and SMS code to create account",
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

    Button(
      onClick = { setInProgress(true) },
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    ) {
      Text("Start")
    }
  }
}

suspend fun testStellarStuff(
  wallet: Wallet,
  accountPublicKey: String,
  accountSecretKey: String,
  devicePublicKey: String,
  deviceSecretKey: String
) {
  val testTxn =
    createTransactionBuilder(
      sourceAddress = accountPublicKey,
      server = server,
      network = network,
    )

  //    TODO: set weights for RS signers (10 each) and device key (20)
  testTxn.addOperations(
    listOf(
      addSignerOperation(
        AccountSigner(address = recoveryServer1.stellarAddress, weight = signerRecoveryWeight)
      ),
      addSignerOperation(
        AccountSigner(address = recoveryServer2.stellarAddress, weight = signerRecoveryWeight)
      ),
      addSignerOperation(AccountSigner(address = devicePublicKey, weight = signerMasterWeight))
    )
  )

  //    TODO: set weights for thresholds (all 20)
  testTxn.addOperation(
    setThresholdsOperation(
      low = signerMasterWeight,
      medium = signerMasterWeight,
      high = signerMasterWeight
    )
  )

  val txn = testTxn.build()

  Log.d(logTag, "Transaction: ${txn.toEnvelopeXdrBase64()}")

  //    TODO: sign with master key
  txn.sign(KeyPair.fromSecretSeed(accountSecretKey))

  val success = wallet.submitTransaction(txn)

  if (success) {
    Log.d(logTag, "Transaction submitted successfully")

    //    TODO: once RS is set, lock master key (set weight to 0)
    val newTxn =
      createTransactionBuilder(sourceAddress = accountPublicKey, server = server, network = network)

    newTxn.addOperation(lockMasterKey())
    val lockTxn = newTxn.build()
    //    TODO: sign with device key
    lockTxn.sign(KeyPair.fromSecretSeed(deviceSecretKey))

    val lockSuccess = wallet.submitTransaction(lockTxn)

    if (lockSuccess) {
      Log.d(logTag, "Account master key locked")
    } else {
      throw Exception("Master key lock failed")
    }
  }
}

data class AccountSigner(val address: String, val weight: Int)

fun addSignerOperation(signer: AccountSigner): SetOptionsOperation {
  val signerKeypair = KeyPair.fromAccountId(signer.address)
  val signerKey = Signer.ed25519PublicKey(signerKeypair)

  return SetOptionsOperation.Builder().setSigner(signerKey, signer.weight).build()
}

// TODO: build transaction with multiple operations
suspend fun createTransactionBuilder(
  sourceAddress: String,
  server: Server,
  network: Network,
): TransactionBuilder {
  val sourceAccount = fetchAccount(sourceAddress, server)

  // TODO: add memo
  // TODO: update max fee
  // TODO: add time bounds
  // TODO: custom base fee

  return Transaction.Builder(sourceAccount, network).setBaseFee(500).setTimeout(180)
}

fun setThresholdsOperation(low: Int, medium: Int, high: Int): SetOptionsOperation {
  return SetOptionsOperation.Builder()
    .setLowThreshold(low)
    .setMediumThreshold(medium)
    .setHighThreshold(high)
    .build()
}

fun setMasterKeyWeight(weight: Int): SetOptionsOperation {
  return SetOptionsOperation.Builder().setMasterKeyWeight(weight).build()
}

fun lockMasterKey(): SetOptionsOperation {
  return setMasterKeyWeight(0)
}
