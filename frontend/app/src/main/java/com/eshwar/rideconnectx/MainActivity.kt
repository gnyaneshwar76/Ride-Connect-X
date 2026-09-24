package com.eshwar.rideconnectx

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.eshwar.rideconnectx.domain.model.ThemeMode
import com.eshwar.rideconnectx.presentation.navigation.NavGraph
import androidx.compose.runtime.CompositionLocalProvider
import com.eshwar.rideconnectx.domain.model.SurfaceStyle
import com.eshwar.rideconnectx.presentation.theme.LocalStyleMode
import com.eshwar.rideconnectx.presentation.theme.StyleMode
import com.eshwar.rideconnectx.presentation.theme.Rcx
import com.eshwar.rideconnectx.presentation.theme.RcxTheme
import com.eshwar.rideconnectx.presentation.viewmodel.AppearanceViewModel
import com.eshwar.rideconnectx.core.di.ServiceEntryPoint
import com.eshwar.rideconnectx.domain.repository.BleRepository
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.EntryPointAccessors

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Resolved on demand — see [ServiceEntryPoint] for why not `@Inject`. */
    private val bleRepository: BleRepository
        get() = EntryPointAccessors
            .fromApplication(applicationContext, ServiceEntryPoint::class.java)
            .bleRepository()

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate() — swaps the stock system launch icon
        // for the RCX splash and hands off cleanly to the Compose splash.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Cold start with a vehicle already paired should just reconnect —
        // the rider should not have to walk back into Pair Vehicle every time.
        // No-ops when nothing was paired, or when the permission is missing.
        if (savedInstanceState == null) bleRepository.reconnectLastDevice()

        setContent {
            // Appearance is applied here so a theme change takes effect
            // instantly and survives process death, without a restart.
            val appearance: AppearanceViewModel = hiltViewModel()
            val themeMode by appearance.themeMode.collectAsStateWithLifecycle()
            val fontSize by appearance.fontSize.collectAsStateWithLifecycle()
            val accent by appearance.accentColor.collectAsStateWithLifecycle()
            val surfaceStyle by appearance.surfaceStyle.collectAsStateWithLifecycle()


            val dark = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.DARK -> true
                ThemeMode.LIGHT -> false
            }

            RcxTheme(darkTheme = dark, fontScale = fontSize.scale, accent = accent) {
                // Published here so every screen can ask for the glass style
                // without threading it through a dozen composables.
                CompositionLocalProvider(
                    LocalStyleMode provides
                        if (surfaceStyle == SurfaceStyle.GLASS) StyleMode.GLASS else StyleMode.FLAT
                ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Rcx.colors.bg,
                ) {
                    val navController = rememberNavController()

                    // Back at the root of the app must leave the app.
                    //
                    // Navigation Compose only finishes the Activity when the
                    // *start* destination is popped, and the start destination
                    // here is the splash — which every route pops off itself.
                    // So backing out of Welcome, or out of the Dashboard on a
                    // returning launch, emptied the back stack and left the
                    // NavHost with nothing to draw: a blank screen that only a
                    // swipe from Recents recovered from. This catches the case
                    // wherever it happens rather than screen by screen.
                    BackHandler(enabled = true) {
                        if (navController.previousBackStackEntry != null) {
                            navController.popBackStack()
                        } else {
                            finish()
                        }
                    }

                    NavGraph(navController = navController)
                }
                }
            }
        }
    }

    /**
     * Leaving the app for good — back out of the last screen, or the task being
     * finished — drops the vehicle link too. Recents-swipe is handled separately
     * in [com.eshwar.rideconnectx.data.ble.BleForegroundService.onTaskRemoved],
     * because that path does not always reach an Activity callback.
     */
    override fun onDestroy() {
        if (isFinishing) bleRepository.shutdown()
        super.onDestroy()
    }
}
