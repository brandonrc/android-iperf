package com.iperf3.android.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.rememberNavController
import com.iperf3.android.domain.repository.PreferencesRepository
import com.iperf3.android.presentation.ui.navigation.IPerf3BottomBar
import com.iperf3.android.presentation.ui.navigation.IPerf3NavHost
import com.iperf3.android.presentation.ui.theme.IPerf3Theme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferencesRepository: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val darkModePref by preferencesRepository.getDarkModeEnabled()
                .collectAsState(initial = false)
            val systemDark = isSystemInDarkTheme()
            // Use the user preference if explicitly enabled, otherwise follow system
            val useDarkTheme = darkModePref || systemDark

            IPerf3Theme(darkTheme = useDarkTheme) {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = { IPerf3BottomBar(navController) }
                ) { innerPadding ->
                    IPerf3NavHost(
                        navController = navController,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}
