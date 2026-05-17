package com.example.myandroidapp.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.example.myandroidapp.MainNavigation
import com.example.myandroidapp.data.ArticlesRepository
import dagger.hilt.android.EntryPointAccessors

@Composable
fun ResponsiveApp(modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (maxWidth < 840.dp) {
            MainNavigation()
        } else {
            val context = LocalContext.current
            val repository = EntryPointAccessors.fromApplication(
                context.applicationContext,
                RepositoryEntryPoint::class.java,
            ).repository
            DualPaneScreen(
                repository = repository,
                modifier = Modifier.fillMaxSize(),
            )
        }
    }
}
