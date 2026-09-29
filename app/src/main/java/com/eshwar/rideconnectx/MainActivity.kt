package com.eshwar.rideconnectx

import com.eshwar.rideconnectx.presentation.theme.LocalGlassIntensity
import android.os.Build
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
import androidx.lifecycle.lifecycleScope
import com.eshwar.rideconnectx.data.repository.NotificationRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import dagger.hilt.android.EntryPointAccessors

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    /** Resolved on demand — see [ServiceEntryPoint] for why not `@Inject`. */
    private val bleRepository: BleRepository
        get() = EntryPointAccessors
            .fromApplication(applicationContext, ServiceEntryPoint::class.java)
            .bleRepository()

    /** The screen a tapped phone notification asked for (N13); taken once. */
    private val openKind = androidx.compose.runtime.mutableStateOf<String?>(null)

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        openKind.value = intent.getStringExtra(NotificationRepository.EXTRA_KIND)
    }

    override fun onResume() {
        super.onResume()
        // A reminder that could not reach the shade (permission off) is
        // retried as soon as the rider is back (N13).
        lifecycleScope.launch {
            EntryPointAccessors.fromApplication(applicationContext, ServiceEntryPoint::class.java)
                .serviceReminder().checkOnce()
        }
    }

    /**
     * Ask for the display's fastest mode at the current resolution (90/120 Hz
     * where the phone has it). Many phones keep apps at 60 Hz unless asked, and
     * every Compose animation is drawn once per refresh, so this is what makes
     * motion as smooth as the phone allows. The system can still lower it for
     * battery saver or heat.
     */
    private fun preferHighestRefreshRate() {
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) display else null
        display ?: return
        val current = display.mode
        val best = display.supportedModes
            .filter { it.physicalWidth == current.physicalWidth && it.physicalHeight == current.physicalHeight }
            .maxByOrNull { it.refreshRate } ?: return
        window.attributes = window.attributes.apply { preferredDisplayModeId = best.modeId }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        // Must run before super.onCreate() — swaps the stock system launch icon
        // for the RCX splash and hands off cleanly to the Compose splash.
        installSplashScreen()

        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        preferHighestRefreshRate()

        // Cold start with a vehicle already paired should just reconnect —
        // the rider should not have to walk back into Pair Vehicle every time.
        // No-ops when nothing was paired, or when the permission is missing.
        if (savedInstanceState == null) bleRepository.reconnectLastDevice()
        if (savedInstanceState == null) openKind.value = intent.getStringExtra(NotificationRepository.EXTRA_KIND)

        setContent {
            // Appearance is applied here so a theme change takes effect
            // instantly and survives process death, without a restart.
            val appearance: AppearanceViewModel = hiltViewModel()
            val themeMode by appearance.themeMode.collectAsStateWithLifecycle()
            val fontSize by appearance.fontSize.collectAsStateWithLifecycle()
            val accent by appearance.accentColor.collectAsStateWithLifecycle()
            val surfaceStyle by appearance.surfaceStyle.collectAsStateWithLifecycle()
            val glassIntensity by appearance.glassIntensity.collectAsStateWithLifecycle()


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
                        if (surfaceStyle == SurfaceStyle.GLASS) StyleMode.GLASS else StyleMode.FLAT,
                    LocalGlassIntensity provides glassIntensity,
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

                    NavGraph(
                        navController = navController,
                        openKind = openKind.value,
                        onKindOpened = { openKind.value = null },
                    )
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
