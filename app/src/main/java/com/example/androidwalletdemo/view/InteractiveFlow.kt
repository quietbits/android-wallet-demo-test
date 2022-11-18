package com.example.androidwalletdemo.view

import android.view.ViewGroup
import android.webkit.WebView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Button
import androidx.compose.material.LinearProgressIndicator
import androidx.compose.material.Text
import androidx.compose.material.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.navigation.NavHostController
import com.example.androidwalletdemo.*
import com.example.androidwalletdemo.component.PageLayout
import com.example.androidwalletdemo.util.OkHttpUtil
import com.example.androidwalletdemo.util.UnsafeWebViewClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.stellar.sdk.Network
import org.stellar.sdk.Server
import org.stellar.walletsdk.Anchor

@Composable
fun InteractiveFlow(navController: NavHostController) {
  val screenScope = CoroutineScope(Dispatchers.IO)
  val pageContext = LocalContext.current

  PageLayout("Interactive deposit and withdrawal", navController) {
    // =============================================================================================
    // VIEW STATE
    // =============================================================================================

    val (flowType, setFlowType) = remember { mutableStateOf("") }
    val (userStellarAddress, setUserStellarAddress) = remember { mutableStateOf(sep24UserAccount) }

    // NOTE: Using exposed secret key for demo purposes only on testnet. DO NOT do this in
    // production with real accounts. Handle signing properly using KeyStore.
    val (userSecretKey, setUserSecretKey) = remember { mutableStateOf(sep24UserAccountSecret) }
    val (assetCode, setAssetCode) = remember { mutableStateOf(sep24AssetCode) }
    val (homeDomain, setHomeDomain) = remember { mutableStateOf(sep24HomeDomain) }
    val (inProgress, setInProgress) = remember { mutableStateOf(false) }
    val (webUrl, setWebUrl) = remember { mutableStateOf("") }

    // =============================================================================================
    // WALLET SDK: GET INTERACTIVE DEPOSIT OR WITHDRAWAL URL FROM ANCHOR
    // =============================================================================================
    // *** Must be on the client
    // Get interactive deposit or withdrawal URL from anchor to render in the WebView
    // User account must have a trustline to the asset
    // For withdrawal, make sure user has enough funds to withdraw
    // =============================================================================================

    val server = Server(horizonUrl)
    val network = Network(networkPassphrase)
    val httpClient = OkHttpUtil.unsafeOkhttpClient()
    val anchor = Anchor(server, network, homeDomain, httpClient)

    if (inProgress && flowType.isNotEmpty()) {
      LaunchedEffect(true) {
        screenScope.launch {
          var flowUrl = ""
          val toml = anchor.getInfo()

          val authToken =
            anchor.getAuthToken(
              accountAddress = userStellarAddress,
              toml = toml,
              walletSigner = AppWalletSigner(userSecretKey)
            )

          if (flowType == "deposit") {
            flowUrl =
              anchor
                .getInteractiveDeposit(
                  accountAddress = userStellarAddress,
                  assetCode = assetCode,
                  authToken = authToken
                )
                .url
          } else {
            flowUrl =
              anchor
                .getInteractiveWithdrawal(
                  accountAddress = userStellarAddress,
                  assetCode = assetCode,
                  authToken = authToken
                )
                .url
          }

          setWebUrl(flowUrl)
          setInProgress(false)
        }
      }
    }

    // =============================================================================================
    // UI ELEMENTS
    // =============================================================================================

    Text(
      text =
        "Enter user's Stellar address and secret key (for demo purposes only), asset code and " +
          "asset issuer to deposit or withdraw",
      modifier = Modifier.padding(bottom = 8.dp)
    )

    TextField(
      value = userStellarAddress,
      onValueChange = { setUserStellarAddress(it) },
      label = { Text(text = "User Stellar address") },
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
    TextField(
      value = userSecretKey,
      onValueChange = { setUserSecretKey(it) },
      label = { Text(text = "User secret key") },
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
    TextField(
      value = assetCode,
      onValueChange = { setAssetCode(it) },
      label = { Text(text = "Asset code") },
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )
    TextField(
      value = homeDomain,
      onValueChange = { setHomeDomain(it) },
      label = { Text(text = "Anchor home domain") },
      modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
    )

    if (!inProgress && webUrl.isEmpty()) {
      Button(
        onClick = {
          setFlowType("deposit")
          setInProgress(true)
        },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
      ) {
        Text(text = "Start deposit")
      }

      Button(
        onClick = {
          setFlowType("withdraw")
          setInProgress(true)
        },
        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
      ) {
        Text(text = "Start withdrawal")
      }
    }

    if (inProgress) {
      LinearProgressIndicator(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp))
    }

    if (webUrl.isNotEmpty()) {
      AndroidView(
        factory = {
          WebView(pageContext).apply {
            settings.javaScriptEnabled = true
            layoutParams =
              ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
              )
            // TODO: see if we can remove UnsafeWebViewClient (breaks for API 21 with current
            //  Stellar cert)
            webViewClient = UnsafeWebViewClient()
            loadUrl(webUrl)
          }
        },
        update = { it.loadUrl(webUrl) }
      )
    }
  }
}
