package com.example.androidwalletdemo.util

import com.google.gson.Gson
import com.google.gson.GsonBuilder

object GsonUtil {
  var instance: Gson? = null
    get() {
      if (field == null) field = builder().create()
      return field
    }
    private set

  private fun builder(): GsonBuilder {
    return GsonBuilder()
  }
}
