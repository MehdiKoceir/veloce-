package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.VeloceRepository
import com.example.data.database.VeloceDatabase
import com.example.ui.VeloceViewModel
import com.example.ui.VeloceViewModel.AppTab
import com.example.ui.VeloceViewModelFactory
import com.example.ui.screens.*
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.VeloceDarkBackground

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Supports modern edge-to-edge full bleed rendering
        enableEdgeToEdge()

        setContent {
            val context = LocalContext.current
            
            // 1. Core Database and Repository initialization (Offline-first)
            val database = remember { VeloceDatabase.getDatabase(context) }
            val repository = remember { VeloceRepository(database.veloceDao()) }
            
            // 2. ViewModel instantiation using Factory
            val viewModel: VeloceViewModel by viewModels {
                VeloceViewModelFactory(repository, context.applicationContext)
            }

            val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
            val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
            val isDarkTheme by viewModel.isDarkTheme.collectAsStateWithLifecycle()

            MyApplicationTheme(darkTheme = isDarkTheme) {
                Crossfade(
                    targetState = isDarkTheme,
                    animationSpec = tween(durationMillis = 600),
                    label = "theme_fade"
                ) { targetDark ->
                    MyApplicationTheme(darkTheme = targetDark) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            if (userProfile == null) {
                        // Loading State while fetching Database
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = androidx.compose.ui.Alignment.Center
                        ) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else if (userProfile?.name == "Athlète Veloce") {
                        // Onboarding Flow on fresh launch to customize MET variables
                        OnboardingScreen(
                            currentProfile = userProfile,
                            onComplete = { name, weight, height, age, gender, activityLevel, metric ->
                                viewModel.updateProfile(name, weight, height, age, gender, activityLevel, metric)
                            }
                        )
                    } else {
                        // Core Application Navigation HUD
                        Scaffold(
                            modifier = Modifier
                                .fillMaxSize()
                                .windowInsetsPadding(WindowInsets.safeDrawing),
                            bottomBar = {
                                NavigationBar(
                                    modifier = Modifier
                                        .navigationBarsPadding()
                                        .testTag("bottom_nav_bar"),
                                    containerColor = MaterialTheme.colorScheme.surface,
                                    tonalElevation = 8.dp
                                ) {
                                    // Feed Tab
                                    NavigationBarItem(
                                        selected = currentTab == AppTab.FEED,
                                        onClick = { viewModel.selectTab(AppTab.FEED) },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentTab == AppTab.FEED) Icons.Filled.People else Icons.Outlined.People,
                                                contentDescription = "Feed"
                                            )
                                        },
                                        label = { Text("Feed", fontWeight = FontWeight.Bold) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("nav_tab_feed")
                                    )

                                    // Live Tracking Tab
                                    NavigationBarItem(
                                        selected = currentTab == AppTab.TRACKING,
                                        onClick = { viewModel.selectTab(AppTab.TRACKING) },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentTab == AppTab.TRACKING) Icons.Filled.DirectionsRun else Icons.Outlined.DirectionsRun,
                                                contentDescription = "Tracking"
                                            )
                                        },
                                        label = { Text("Tracé", fontWeight = FontWeight.Bold) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("nav_tab_tracking")
                                    )

                                    // History & Stats Tab
                                    NavigationBarItem(
                                        selected = currentTab == AppTab.HISTORY,
                                        onClick = { viewModel.selectTab(AppTab.HISTORY) },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentTab == AppTab.HISTORY) Icons.Filled.BarChart else Icons.Outlined.BarChart,
                                                contentDescription = "Stats"
                                            )
                                        },
                                        label = { Text("Stats", fontWeight = FontWeight.Bold) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("nav_tab_history")
                                    )

                                    // Profile & Settings Tab
                                    NavigationBarItem(
                                        selected = currentTab == AppTab.PROFILE,
                                        onClick = { viewModel.selectTab(AppTab.PROFILE) },
                                        icon = {
                                            Icon(
                                                imageVector = if (currentTab == AppTab.PROFILE) Icons.Filled.Person else Icons.Outlined.Person,
                                                contentDescription = "Profil"
                                            )
                                        },
                                        label = { Text("Profil", fontWeight = FontWeight.Bold) },
                                        colors = NavigationBarItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.primary,
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer
                                        ),
                                        modifier = Modifier.testTag("nav_tab_profile")
                                    )
                                }
                            }
                        ) { innerPadding ->
                            // Custom Animated tab switcher
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(innerPadding)
                            ) {
                                when (currentTab) {
                                    AppTab.FEED -> {
                                        FeedScreen(
                                            viewModel = viewModel,
                                            profile = userProfile!!
                                        )
                                    }
                                    AppTab.TRACKING -> {
                                        TrackingScreen(
                                            viewModel = viewModel,
                                            profile = userProfile!!
                                        )
                                    }
                                    AppTab.HISTORY -> {
                                        HistoryScreen(
                                            viewModel = viewModel,
                                            profile = userProfile!!
                                        )
                                    }
                                    AppTab.PROFILE -> {
                                        ProfileScreen(
                                            viewModel = viewModel,
                                            profile = userProfile!!
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
}
}
