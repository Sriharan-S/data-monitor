package com.sriharan.datamonitor.data

import com.sriharan.datamonitor.data.model.AppUsageInfo
import kotlinx.coroutines.flow.Flow

interface UsageRepository {
    suspend fun getUsageForInterval(startTime: Long, endTime: Long): List<AppUsageInfo>
    suspend fun getTodayUsage(): List<AppUsageInfo>
    suspend fun getPastHourUsage(): List<AppUsageInfo>
    fun hasPermission(): Boolean
}
