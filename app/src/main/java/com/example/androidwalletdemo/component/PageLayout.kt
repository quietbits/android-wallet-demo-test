package com.example.androidwalletdemo.component

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@Composable
fun PageLayout(
  pageTitle: String,
  navController: NavHostController,
  content: @Composable() () -> Unit
) {
  val scaffoldState = rememberScaffoldState()

  MaterialTheme {
    Scaffold(
      scaffoldState = scaffoldState,
      topBar = {
        TopAppBar(
          title = { Text(pageTitle) },
          navigationIcon = {
            if (navController.currentBackStackEntry?.destination?.route != "main") {
              IconButton(onClick = { navController.navigateUp() }) {
                Icon(imageVector = Icons.Filled.ArrowBack, contentDescription = "Back")
              }
            }
          }
        )
        Spacer(modifier = Modifier.height(8.dp))
      },
      content = {
        Column(
          modifier =
            Modifier.fillMaxSize().padding(all = 8.dp).verticalScroll(rememberScrollState())
        ) {
          content()
        }
      }
    )
  }
}
