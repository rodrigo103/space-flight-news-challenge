package com.example.myandroidapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.example.myandroidapp.data.connectivity.ConnectivityObserver
import com.example.myandroidapp.theme.SpaceFlightNewsTheme
import com.example.myandroidapp.ui.navigation.ResponsiveApp
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val connectivityObserver: ConnectivityObserver by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        enableEdgeToEdge()
        setContent {
            SpaceFlightNewsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ResponsiveApp(
                        connectivityStatus = connectivityObserver.status,
                    )
                }
            }
        }
    }
}
