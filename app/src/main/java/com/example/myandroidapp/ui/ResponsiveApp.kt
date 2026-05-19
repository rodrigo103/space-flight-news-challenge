package com.example.myandroidapp.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myandroidapp.MainNavigation
import com.example.myandroidapp.data.connectivity.ConnectivityStatus
import com.example.myandroidapp.ui.components.OfflineBanner
import kotlinx.coroutines.flow.Flow

private val DUAL_PANE_BREAKPOINT = 840.dp

@Composable
fun ResponsiveApp(
    connectivityStatus: Flow<ConnectivityStatus>,
    modifier: Modifier = Modifier,
) {
    var selectedArticleId by rememberSaveable { mutableStateOf<Int?>(null) }
    val status by connectivityStatus.collectAsState(initial = ConnectivityStatus.Available)

    Box(modifier = modifier.fillMaxSize()) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            if (maxWidth < DUAL_PANE_BREAKPOINT) {
                MainNavigation(
                    selectedArticleId = selectedArticleId,
                    onArticleSelected = { selectedArticleId = it },
                )
            } else {
                DualPaneScreen(
                    selectedArticleId = selectedArticleId,
                    onArticleSelected = { selectedArticleId = it },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        OfflineBanner(
            connectivityStatus = status,
            modifier = Modifier.fillMaxWidth().align(Alignment.TopCenter),
        )
    }
}

