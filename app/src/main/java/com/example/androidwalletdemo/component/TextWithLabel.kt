package com.example.androidwalletdemo.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

@Composable
fun TextWithLabel(label: String, text: String) {
    Text(label, fontWeight = FontWeight.Bold)
    Text(text.ifEmpty { "..." }, modifier = Modifier.padding(bottom = 8.dp))
}