package com.sriharan.datamonitor.ui.home

import android.app.Application
import android.content.Intent
import android.provider.Settings
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sriharan.datamonitor.data.UsageRepository
import com.sriharan.datamonitor.data.model.AppUsageInfo
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class UsagePeriod {
    PAST_HOUR, TODAY, YESTERDAY
}

data class HomeUiState(
    val isLoading: Boolean = false,
    val usageList: List<AppUsageInfo> = emptyList(),
    val totalUsageBytes: Long = 0,
    val hasPermission: Boolean = false,
    val selectedPeriod: UsagePeriod = UsagePeriod.TODAY
)

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val repository: UsageRepository,
    private val app: Application // Needed for starting intent if we put logic here, but UI should handle Intent usually.
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    init {
        checkPermissionAndLoad()
    }

    fun checkPermissionAndLoad() {
        val hasPerm = repository.hasPermission()
       
        _uiState.update { it.copy(hasPermission = hasPerm) }

        if (hasPerm) {
            loadUsage(_uiState.value.selectedPeriod)
        }
    }

    fun onPeriodSelected(period: UsagePeriod) {
        _uiState.update { it.copy(selectedPeriod = period) }
        loadUsage(period)
    }

    private fun loadUsage(period: UsagePeriod) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val list = when (period) {
                UsagePeriod.PAST_HOUR -> repository.getPastHourUsage()
                UsagePeriod.TODAY -> repository.getTodayUsage()
                UsagePeriod.YESTERDAY -> {
                    // Logic for yesterday
                    val now = System.currentTimeMillis()
                    // Simplification: 24h to 48h ago roughly, or precise calendar
                     // TODO: Implement Yesterday logic in repo or here
                    emptyList() 
                }
            }
            val total = list.sumOf { it.totalBytes }
            _uiState.update { it.copy(isLoading = false, usageList = list, totalUsageBytes = total) }
        }
    }

    fun openUsageSettings() {
        val intent = Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        app.startActivity(intent)
    }
}
