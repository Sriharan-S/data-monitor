package com.sriharan.datamonitor.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.graphics.drawable.toBitmap
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sriharan.datamonitor.data.model.AppUsageInfo
import com.sriharan.datamonitor.ui.home.HomeUiState
import com.sriharan.datamonitor.ui.home.HomeViewModel
import com.sriharan.datamonitor.ui.home.UsagePeriod
import com.sriharan.datamonitor.util.formatDataSize

@Composable
fun MainScreen(
    viewModel: HomeViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.checkPermissionAndLoad()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    if (!uiState.hasPermission) {
        PermissionScreen { viewModel.openUsageSettings() }
    } else {
        DashboardScreen(uiState) { viewModel.onPeriodSelected(it) }
    }
}

@Composable
fun PermissionScreen(onGrantClick: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Usage Permission Needed",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "To track data usage, this app requires access to usage stats. Please enable it in Settings.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = onGrantClick) {
            Text("Grant Permission")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(state: HomeUiState, onPeriodSelected: (UsagePeriod) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Data Monitor", fontWeight = FontWeight.SemiBold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding)) {
            // Period Selector
            Row(
                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                UsagePeriod.values().forEach { period ->
                    FilterChip(
                        selected = state.selectedPeriod == period,
                        onClick = { onPeriodSelected(period) },
                        label = { Text(period.name.replace("_", " ")) }
                    )
                }
            }

            // Total Usage Card
            Card(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Total Usage",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    Text(
                        text = formatDataSize(state.totalUsageBytes),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))

            // App List
            if (state.isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(state.usageList) { app ->
                        AppUsageItem(app)
                    }
                }
            }
        }
    }
}

@Composable
fun AppUsageItem(app: AppUsageInfo) {
    ListItem(
        headlineContent = { Text(app.appName, fontWeight = FontWeight.Medium) },
        supportingContent = { Text(app.packageName, style = MaterialTheme.typography.bodySmall) },
        trailingContent = {
            Text(
                formatDataSize(app.totalBytes),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        },
        leadingContent = {
             if (app.icon != null) {
                 Image(
                     bitmap = app.icon.toBitmap().asImageBitmap(), // Simple conversion, inefficient for lists but works for MVP
                     contentDescription = null,
                     modifier = Modifier.size(40.dp)
                 )
             } else {
                 // Placeholder
                 Box(modifier = Modifier.size(40.dp))
             }
        }
    )
}
