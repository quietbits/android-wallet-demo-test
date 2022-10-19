package com.example.androidwalletdemo.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.toColorInt

@Composable
fun SuccessMessage(message: String) {
  Text(
    message,
    color = Color("#8bbe1b".toColorInt()),
    fontSize = 20.sp,
    textAlign = TextAlign.Center,
    modifier = Modifier.fillMaxWidth().padding(0.dp, 8.dp)
  )
}
