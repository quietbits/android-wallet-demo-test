package com.example.androidwalletdemo.util

fun shortenString(text: String, start: Int = 10, end: Int = 10): String {
  if (text.length < 20) {
    return text
  }

  return "${text.take(start)}...${text.takeLast(end)}"
}
