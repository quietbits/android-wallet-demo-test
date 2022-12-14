package com.example.androidwalletdemo.view

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import com.example.androidwalletdemo.NavRoutes
import com.example.androidwalletdemo.component.ActionButton
import com.example.androidwalletdemo.component.PageLayout

@Composable
fun Main(navController: NavHostController) {
  PageLayout("Stellar Wallet SDK Demo", navController) {
    ActionButton(
      label = "Create account",
      onClick = { navController.navigate(NavRoutes.CreateAccount.route) }
    )

    ActionButton(
      label = "Recover account",
      onClick = { navController.navigate(NavRoutes.RecoverAccount.route) }
    )

    ActionButton(
      label = "Interactive flow",
      onClick = { navController.navigate(NavRoutes.InteractiveFlow.route) }
    )
  }
}
