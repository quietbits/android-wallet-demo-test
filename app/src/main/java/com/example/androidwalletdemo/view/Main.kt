package com.example.androidwalletdemo.view

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.OutlinedButton
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import com.example.androidwalletdemo.NavRoutes
import com.example.androidwalletdemo.component.PageLayout

@Composable
fun Main(navController: NavHostController) {
  PageLayout("Stellar Wallet SDK Demo", navController) {
    OutlinedButton(
      onClick = { navController.navigate(NavRoutes.RecoverAccount.route) },
      modifier = Modifier.fillMaxWidth()
    ) {
      Text("Recover account")
    }
  }
}
