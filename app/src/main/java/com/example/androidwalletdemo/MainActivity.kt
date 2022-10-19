package com.example.androidwalletdemo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.androidwalletdemo.view.CreateAccount
import com.example.androidwalletdemo.view.Main
import com.example.androidwalletdemo.view.RecoverAccount

sealed class NavRoutes(val route: String) {
  object Main : NavRoutes("main")
  object CreateAccount : NavRoutes("createAccount")
  object RecoverAccount : NavRoutes("recoverAccount")
}

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    setContent {
      val navController = rememberNavController()

      NavHost(navController = navController, startDestination = NavRoutes.Main.route) {
        composable(NavRoutes.Main.route) { Main(navController) }
        composable(NavRoutes.CreateAccount.route) { CreateAccount(navController) }
        composable(NavRoutes.RecoverAccount.route) { RecoverAccount(navController) }
      }
    }
  }
}
