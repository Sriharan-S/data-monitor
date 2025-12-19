package com.sriharan.datamonitor.util

import java.util.Locale

fun formatDataSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val pre = "KMGTPE"[exp - 1]
    return String.format(Locale.getDefault(), "%.1f %cB", bytes / Math.pow(1024.0, exp.toDouble()), pre)
}
