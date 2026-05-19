package com.example.myandroidapp.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.myandroidapp.MainNavigation

private val DUAL_PANE_BREAKPOINT = 840.dp

@Composable
fun ResponsiveApp(modifier: Modifier = Modifier) {
    var selectedArticleId by rememberSaveable { mutableStateOf<Int?>(null) }

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
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
}

