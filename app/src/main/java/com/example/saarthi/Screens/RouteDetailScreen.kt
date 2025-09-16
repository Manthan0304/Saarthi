package com.example.saarthi.Screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.saarthi.data.Route
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RouteDetailScreen(
    route: Route,
    onBack: () -> Unit,
    onContinueRecording: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(route.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = android.R.drawable.arrow_up_float),
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(text = "Distance: ${String.format("%.1f", route.totalDistance)}m", style = MaterialTheme.typography.titleMedium)
            Text(text = "Duration: ${route.duration}s", style = MaterialTheme.typography.bodyMedium)
            Text(text = "Started: ${route.startTime.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))}", style = MaterialTheme.typography.bodyMedium)
            route.endTime?.let {
                Text(text = "Ended: ${it.format(DateTimeFormatter.ofPattern("MMM dd, HH:mm"))}", style = MaterialTheme.typography.bodyMedium)
            }

            Spacer(modifier = Modifier.weight(1f))

            Button(
                onClick = onContinueRecording,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
            ) {
                Text(text = "Continue Recording")
            }
        }
    }
}


