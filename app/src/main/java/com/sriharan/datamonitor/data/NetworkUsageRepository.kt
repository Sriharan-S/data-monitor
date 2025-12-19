package com.sriharan.datamonitor.data

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Process
import android.os.RemoteException
import com.sriharan.datamonitor.data.model.AppUsageInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar
import javax.inject.Inject

class NetworkUsageRepository @Inject constructor(
    @ApplicationContext private val context: Context
) : UsageRepository {

    private val networkStatsManager = context.getSystemService(Context.NETWORK_STATS_SERVICE) as NetworkStatsManager
    private val packageManager = context.packageManager

    override fun hasPermission(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        val mode = appOps.unsafeCheckOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        )
        return mode == AppOpsManager.MODE_ALLOWED
    }

    override suspend fun getTodayUsage(): List<AppUsageInfo> {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        val startTime = calendar.timeInMillis
        val endTime = System.currentTimeMillis()
        return getUsageForInterval(startTime, endTime)
    }

    override suspend fun getPastHourUsage(): List<AppUsageInfo> {
        val endTime = System.currentTimeMillis()
        val startTime = endTime - (60 * 60 * 1000)
        return getUsageForInterval(startTime, endTime)
    }

    override suspend fun getUsageForInterval(startTime: Long, endTime: Long): List<AppUsageInfo> {
        return withContext(Dispatchers.IO) {
            if (!hasPermission()) return@withContext emptyList()

            val usageMap = mutableMapOf<Int, Pair<Long, Long>>() // Uid -> (Rx, Tx)

            try {
                // Query Mobile Data
                queryNetworkStats(ConnectivityManager.TYPE_MOBILE, startTime, endTime, usageMap)
                // Query Wi-Fi Data
                queryNetworkStats(ConnectivityManager.TYPE_WIFI, startTime, endTime, usageMap)
            } catch (e: SecurityException) {
                e.printStackTrace()
                return@withContext emptyList()
            } catch (e: RemoteException) {
                e.printStackTrace()
                return@withContext emptyList()
            }

            // Map UIDs to Apps
            usageMap.mapNotNull { (uid, usage) ->
                val (rx, tx) = usage
                if (rx + tx == 0L) return@mapNotNull null

                try {
                    val packages = packageManager.getPackagesForUid(uid)
                    if (packages.isNullOrEmpty()) return@mapNotNull null
                    
                    // Taking the first package name for the UID (usually sufficient for standard apps)
                    val packageName = packages[0]
                    val appInfo = packageManager.getApplicationInfo(packageName, 0)
                    val appName = packageManager.getApplicationLabel(appInfo).toString()
                    val icon = packageManager.getApplicationIcon(appInfo)

                    AppUsageInfo(packageName, appName, icon, rx, tx)
                } catch (e: PackageManager.NameNotFoundException) {
                    // System UIDs or uninstalled apps might fail here
                    null 
                }
            }.sortedByDescending { it.totalBytes }
        }
    }

    private fun queryNetworkStats(networkType: Int, startTime: Long, endTime: Long, usageMap: MutableMap<Int, Pair<Long, Long>>) {
        // null subscriberId for current active network (mostly works for Wifi, for Mobile commonly requires permission or valid subId)
        // For querying summary per app, subscriberId is usually needed for Mobile. 
        // Passing null often queries "all interfaces" of that type or default.
        // If this fails on mobile, we might need to fetch subscriberId via TelephonyManager (requires READ_PHONE_STATE).
        
        try {
           val bucket = NetworkStats.Bucket()
           val stats = networkStatsManager.querySummary(networkType, null, startTime, endTime)
           
           while (stats.hasNextBucket()) {
               stats.getNextBucket(bucket)
               val uid = bucket.uid
               // Skip system/special UIDs if desired, or keep them. 
               // keeping them for 'OS Services' etc.
               
               val current = usageMap.getOrDefault(uid, Pair(0L, 0L))
               usageMap[uid] = Pair(current.first + bucket.rxBytes, current.second + bucket.txBytes)
           }
           stats.close()
        } catch(e: Exception) {
            // Check specific logic needed for mobile data subscriberId if null fails
        }
    }
}
