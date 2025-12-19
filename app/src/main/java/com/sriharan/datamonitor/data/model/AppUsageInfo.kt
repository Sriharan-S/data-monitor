package com.sriharan.datamonitor.data.model

import android.graphics.drawable.Drawable

data class AppUsageInfo(
    val packageName: String,
    val appName: String,
    val icon: Drawable?,
    val receivedBytes: Long,
    val transmittedBytes: Long,
    val totalBytes: Long = receivedBytes + transmittedBytes
)
