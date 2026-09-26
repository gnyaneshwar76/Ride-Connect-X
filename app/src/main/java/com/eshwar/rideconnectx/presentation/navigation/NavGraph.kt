package com.eshwar.rideconnectx.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope
import com.eshwar.rideconnectx.core.util.rememberPermissionsController
import androidx.compose.runtime.getValue
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.eshwar.rideconnectx.data.local.db.NotificationKind
import com.eshwar.rideconnectx.domain.model.AuthState
import com.eshwar.rideconnectx.domain.model.LegalDoc
import com.eshwar.rideconnectx.presentation.screens.AboutScreen
import com.eshwar.rideconnectx.presentation.screens.AppearanceScreen
import com.eshwar.rideconnectx.presentation.screens.BlePairingScreen
import com.eshwar.rideconnectx.presentation.screens.CreateProfileScreen
import com.eshwar.rideconnectx.presentation.screens.DashboardScreen
import com.eshwar.rideconnectx.presentation.screens.GuestProfileScreen
import com.eshwar.rideconnectx.presentation.screens.LegalScreen
import com.eshwar.rideconnectx.presentation.screens.NavigationScreen
import com.eshwar.rideconnectx.presentation.screens.NotificationsScreen
import com.eshwar.rideconnectx.presentation.screens.PermissionDetailsScreen
import com.eshwar.rideconnectx.presentation.screens.PermissionsScreen
import com.eshwar.rideconnectx.presentation.screens.ProfileFoundScreen
import com.eshwar.rideconnectx.presentation.screens.ProfileScreen
import com.eshwar.rideconnectx.presentation.screens.SafetyScreen
import com.eshwar.rideconnectx.presentation.screens.ServiceScreen
import com.eshwar.rideconnectx.presentation.screens.SettingsScreen
import com.eshwar.rideconnectx.presentation.screens.SignInScreen
import com.eshwar.rideconnectx.presentation.screens.SplashScreen
import com.eshwar.rideconnectx.presentation.screens.StatisticsScreen
import com.eshwar.rideconnectx.presentation.screens.VehicleGalleryScreen
import com.eshwar.rideconnectx.presentation.screens.IntroScreen
import com.eshwar.rideconnectx.presentation.viewmodel.AuthViewModel
import com.eshwar.rideconnectx.presentation.viewmodel.ProfileFormMode

/** Route names mirror the `ScreenId` union in the design. */
object Routes {
    const val SPLASH = "splash"
    /** Welcome plus the three onboarding pages — one swipeable pager. */
    const val INTRO = "intro"
    const val SIGN_IN = "signin"
    const val GUEST_PROFILE = "guestCreate"
    const val TERMS = "terms"
    const val PRIVACY = "privacy"

    /** Terms and Privacy on a single page — what the profile step links to. */
    const val LEGAL = "legal"
    /** "Add your guest data to this account?" — a guest signing into an account with a profile. */
    const val PROFILE_FOUND = "profile_found"
    const val PERMISSIONS = "perms"
    const val VEHICLE = "vehicle"

    /** The same form as [VEHICLE], but only the vehicle part of it. */
    const val CHANGE_VEHICLE = "vehicle_change"
    const val DASHBOARD = "dash"
    const val BLE = "ble"
    const val NAVIGATION = "nav"
    const val STATS = "stats"
    const val NOTIFICATIONS = "notifs"
    const val SERVICE = "service"
    const val SAFETY = "safety"
    const val SETTINGS = "settings"
    const val PROFILE = "profile"
    const val APPEARANCE = "appearance"
    const val ABOUT = "about"

    /**
     * The reference page, reached from Settings. Distinct from [PERMISSIONS],
     * which is the one-time setup step that fires the dialogs automatically.
     */
    const val PERMISSION_DETAILS = "permission_details"
}

@Composable
fun NavGraph(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.SPLASH) {

        composable(Routes.SPLASH) {
            val authVm: AuthViewModel = hiltViewModel()
            val authState by authVm.authState.collectAsStateWithLifecycle()

            val scope = rememberCoroutineScope()

            SplashScreen(
                onFinished = {
                    // Auto-login: an already-authenticated user never sees the
                    // sign-in flow. Loading resolves before the animation ends,
                    // so there is no extra wait. Unfinished setup resumes.
                    scope.launch {
                        val destination = SetupGate.launchRoute(
                            signedIn = authState is AuthState.Authenticated,
                            profileDone = authVm.isProfileCompleted(),
                            guestMergePending = authVm.isGuestMergePending(),
                        )
                        navController.navigate(destination) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                }
            )
        }

        composable(Routes.INTRO) {
            IntroScreen(
                onFinished = { navController.navigate(Routes.SIGN_IN) },
                onSkip = { navController.navigate(Routes.SIGN_IN) },
            )
        }

        composable(Routes.SIGN_IN) {
            val authVm: AuthViewModel = hiltViewModel()
            val authState by authVm.authState.collectAsStateWithLifecycle()

            // Sign-in success is observed rather than pushed, so Google, email
            // and guest all converge on one navigation path.
            LaunchedEffect(authState) {
                if (authState is AuthState.Authenticated) {
                    navController.navigate(Routes.PERMISSIONS) {
                        // The whole intro, not just this screen. Popping only
                        // Sign In left Welcome and the three onboarding pages
                        // underneath, so Back from Permissions walked a
                        // signed-in rider straight back into the tour.
                        popUpTo(0) { inclusive = true }
                    }
                }
            }

            SignInScreen(
                onGuest = { navController.navigate(Routes.GUEST_PROFILE) },
                onTerms = { navController.navigate(Routes.TERMS) },
                onPrivacy = { navController.navigate(Routes.PRIVACY) },
                vm = authVm,
            )
        }

        composable(Routes.GUEST_PROFILE) {
            GuestProfileScreen(
                onBack = { navController.popBackStack() },
                onContinue = {
                    navController.navigate(Routes.PERMISSIONS) {
                        // Sign-in is done; don't let Back return into it, or
                        // into the onboarding tour sitting underneath it.
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        composable(Routes.TERMS) {
            LegalScreen(doc = LegalDoc.TERMS, onBack = { navController.popBackStack() })
        }

        composable(Routes.PRIVACY) {
            LegalScreen(doc = LegalDoc.PRIVACY, onBack = { navController.popBackStack() })
        }

        composable(Routes.LEGAL) {
            LegalScreen(doc = LegalDoc.BOTH, onBack = { navController.popBackStack() })
        }

        composable(Routes.PERMISSIONS) {
            val authVm: AuthViewModel = hiltViewModel()
            val profileDone by authVm.profileCompleted.collectAsStateWithLifecycle()
            val perms = rememberPermissionsController()

            val scope = rememberCoroutineScope()
            // A returning rider goes straight to the dashboard. A guest who
            // signed into an account that already has a profile is asked first
            // whether to add their guest data to it (rider, 26 Sep).
            suspend fun next(done: Boolean) {
                when (val route = SetupGate.afterPermissions(done, authVm.isGuestMergePending())) {
                    Routes.VEHICLE -> navController.navigate(route)
                    else -> navController.navigate(route) { popUpTo(0) { inclusive = true } }
                }
            }
            // Nothing to ask for: don't show the setup screen at all.
            LaunchedEffect(Unit) {
                if (perms.allRequiredGranted) next(authVm.isProfileCompleted())
            }

            PermissionsScreen(onContinue = { scope.launch { next(profileDone) } })
        }

        composable(Routes.PROFILE_FOUND) {
            ProfileFoundScreen(
                onAdded = { profileDone ->
                    // Setup resumes if the account's own profile is unfinished.
                    navController.navigate(SetupGate.launchRoute(signedIn = true, profileDone = profileDone)) {
                        popUpTo(0) { inclusive = true }
                    }
                },
                onChooseAnother = {
                    navController.navigate(Routes.SIGN_IN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }

        // Create Profile — name, city, vehicle, paint and consent. Runs once,
        // ahead of the dashboard, because the dashboard renders the chosen
        // vehicle and greets the rider by name.
        composable(Routes.VEHICLE) {
            // Back here asks before leaving; see CreateProfileScreen.
            CreateProfileScreen(
                onContinue = {
                    navController.navigate(Routes.DASHBOARD) {
                        // The whole setup run, not just this step. Popping only
                        // this screen left Permissions underneath, so Back from
                        // the Dashboard dropped the rider into a setup screen
                        // they had already finished.
                        popUpTo(0) { inclusive = true }
                    }
                },
                onLegal = { navController.navigate(Routes.LEGAL) },
            )
        }

        // Changing the vehicle later reuses the same form with everything the
        // rider already settled — name, city, photo, consent — left out.
        composable(Routes.CHANGE_VEHICLE) {
            CreateProfileScreen(
                mode = ProfileFormMode.CHANGE_VEHICLE,
                onBack = { navController.popBackStack() },
                onContinue = { navController.popBackStack() },
            )
        }

        composable(Routes.DASHBOARD) {
            DashboardScreen(
                onNavigate = { navController.navigate(Routes.NAVIGATION) },
                onPairVehicle = { navController.navigate(Routes.BLE) },
                onStatistics = { navController.navigate(Routes.STATS) },
                onNotifications = { navController.navigate(Routes.NOTIFICATIONS) },
                onService = { navController.navigate(Routes.SERVICE) },
                onSafety = { navController.navigate(Routes.SAFETY) },
                onSettings = { navController.navigate(Routes.SETTINGS) },
                onProfile = { navController.navigate(Routes.PROFILE) },
            )
        }

        composable(Routes.BLE) {
            BlePairingScreen(
                onBack = { navController.popBackStack() },
                onPaired = {
                    navController.navigate(Routes.DASHBOARD) {
                        popUpTo(Routes.DASHBOARD) { inclusive = true }
                    }
                },
                onSkip = { navController.popBackStack() },
            )
        }

        composable(Routes.NAVIGATION) {
            NavigationScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.STATS) {
            StatisticsScreen(onBack = { navController.popBackStack() })
        }
        composable(Routes.NOTIFICATIONS) {
            NotificationsScreen(
                onBack = { navController.popBackStack() },
                // The screen stays destination-agnostic; routing lives here.
                onOpen = { kind ->
                    val route = when (kind) {
                        NotificationKind.RIDE -> Routes.STATS
                        NotificationKind.VEHICLE -> Routes.BLE
                        NotificationKind.SERVICE -> Routes.SERVICE
                        NotificationKind.SAFETY -> Routes.SAFETY
                        // System notices refer to the app itself, so there is
                        // nowhere to push; they stay on the list.
                        NotificationKind.SYSTEM -> null
                    }
                    route?.let { navController.navigate(it) }
                },
            )
        }

        composable(Routes.SERVICE) {
            ServiceScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SAFETY) {
            SafetyScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onAppearance = { navController.navigate(Routes.APPEARANCE) },
                onAbout = { navController.navigate(Routes.ABOUT) },
                onPermissions = { navController.navigate(Routes.PERMISSION_DETAILS) },
                onPrivacy = { navController.navigate(Routes.PRIVACY) },
                onTerms = { navController.navigate(Routes.TERMS) },
                onPairVehicle = { navController.navigate(Routes.BLE) },
                onProfile = { navController.navigate(Routes.PROFILE) },
            )
        }

        composable(Routes.PERMISSION_DETAILS) {
            PermissionDetailsScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.APPEARANCE) {
            AppearanceScreen(onBack = { navController.popBackStack() })
        }

        composable(Routes.ABOUT) {
            AboutScreen(
                onBack = { navController.popBackStack() },
                onPrivacy = { navController.navigate(Routes.PRIVACY) },
                onTerms = { navController.navigate(Routes.TERMS) },
            )
        }

        composable(Routes.PROFILE) {
            ProfileScreen(
                onBack = { navController.popBackStack() },
                onChangeVehicle = { navController.navigate(Routes.CHANGE_VEHICLE) },
                onSignedOut = {
                    // Everything behind the account goes with it, so the whole
                    // back stack is cleared rather than left to be popped into.
                    navController.navigate(Routes.SIGN_IN) {
                        popUpTo(0) { inclusive = true }
                    }
                },
            )
        }
    }
}
