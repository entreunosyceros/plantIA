package com.plantia

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.plantia.data.ThemeMode
import com.plantia.data.ThemePreferences
import com.plantia.ui.PlantIAApp
import com.plantia.ui.SplashScreen
import com.plantia.ui.theme.PlantIATheme

class MainActivity : ComponentActivity() {
    private val notificationPermission = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { /* opcional */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermission.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val openPlantId = intent.getIntExtra(EXTRA_OPEN_PLANT_ID, -1).takeIf { it > 0 }

        setContent {
            val context = LocalContext.current
            val themePrefs = remember { ThemePreferences(context) }
            var themeMode by remember { mutableStateOf(themePrefs.mode) }
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            var showSplash by remember { mutableStateOf(true) }

            PlantIATheme(darkTheme = darkTheme) {
                if (showSplash) {
                    SplashScreen(
                        modifier = Modifier.fillMaxSize(),
                        onFinished = { showSplash = false },
                    )
                } else {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = MaterialTheme.colorScheme.background,
                    ) {
                        PlantIAApp(
                            initialPlantId = openPlantId,
                            themeMode = themeMode,
                            onThemeModeChange = { mode ->
                                themePrefs.mode = mode
                                themeMode = mode
                            },
                        )
                    }
                }
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_PLANT_ID = "open_plant_id"
    }
}
