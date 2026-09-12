package com.bookmyturf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavType
import androidx.navigation.compose.*
import androidx.navigation.navArgument
import com.bookmyturf.app.ui.*
import com.bookmyturf.app.ui.screens.*
import com.bookmyturf.app.ui.theme.BookMyTurfTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent { BookMyTurfTheme { BookMyTurfRoot() } }
    }
}

object Routes {
    const val SPLASH = "splash"
    const val ONBOARDING = "onboarding"
    const val LOGIN = "login"
    const val REGISTER = "register"
    const val USER = "user"
    const val EXPLORE = "explore"
    const val TURF = "turf/{id}"
    const val SLOTS = "slots/{id}"
    const val SUMMARY = "summary"
    const val PAYMENT = "payment"
    const val CONFIRMATION = "confirmation"
    const val BOOKINGS = "bookings"
    const val FAVORITES = "favorites"
    const val NOTIFICATIONS = "notifications"
    const val PROFILE = "profile"
    const val ADMIN = "admin"
    fun turf(id: String) = "turf/$id"
    fun slots(id: String) = "slots/$id"
}

@Composable
fun BookMyTurfRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as BookMyTurfApplication
    val factory = remember(app) { AppViewModelFactory(app.repository) }
    val vm: AppViewModel = viewModel(factory = factory)
    val nav = rememberNavController()
    val snackbar = remember { SnackbarHostState() }
    val message by vm.snackbar
    LaunchedEffect(message) {
        if (message != null) { snackbar.showSnackbar(message!!); vm.clearSnackbar() }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { _ ->
        NavHost(
            navController = nav,
            startDestination = Routes.SPLASH,
            enterTransition = { fadeIn(tween(150)) + slideInHorizontally(tween(180)) { it / 18 } },
            exitTransition = { fadeOut(tween(110)) },
            popEnterTransition = { fadeIn(tween(140)) },
            popExitTransition = { slideOutHorizontally(tween(160)) { it / 18 } + fadeOut(tween(110)) }
        ) {
            composable(Routes.SPLASH) { SplashScreen { nav.navigate(Routes.ONBOARDING) { popUpTo(Routes.SPLASH) { inclusive = true } } } }
            composable(Routes.ONBOARDING) { OnboardingScreen(onDone = { nav.navigate(Routes.LOGIN) { popUpTo(Routes.ONBOARDING) { inclusive = true } } }) }
            composable(Routes.LOGIN) { LoginScreen(vm, onUser = { nav.navigate(Routes.USER) { popUpTo(Routes.LOGIN) { inclusive = true } } }, onAdmin = { nav.navigate(Routes.ADMIN) { popUpTo(Routes.LOGIN) { inclusive = true } } }, onRegister = { nav.navigate(Routes.REGISTER) }) }
            composable(Routes.REGISTER) { RegistrationScreen(vm, onBack = nav::popBackStack, onComplete = { nav.navigate(Routes.USER) { popUpTo(Routes.LOGIN) { inclusive = true } } }) }
            composable(Routes.USER) { UserHomeScreen(vm, nav) }
            composable(Routes.EXPLORE) { ExploreScreen(vm, nav) }
            composable(Routes.TURF, arguments = listOf(navArgument("id") { type = NavType.StringType })) { TurfDetailsScreen(vm, it.arguments?.getString("id").orEmpty(), nav) }
            composable(Routes.SLOTS, arguments = listOf(navArgument("id") { type = NavType.StringType })) { SlotBookingScreen(vm, it.arguments?.getString("id").orEmpty(), nav) }
            composable(Routes.SUMMARY) { BookingSummaryScreen(vm, nav) }
            composable(Routes.PAYMENT) { PaymentScreen(vm, nav) }
            composable(Routes.CONFIRMATION) { ConfirmationScreen(vm, nav) }
            composable(Routes.BOOKINGS) { MyBookingsScreen(vm, nav) }
            composable(Routes.FAVORITES) { FavoritesScreen(vm, nav) }
            composable(Routes.NOTIFICATIONS) { NotificationsScreen(vm, nav) }
            composable(Routes.PROFILE) { ProfileScreen(vm, nav, onLogout = { nav.navigate(Routes.LOGIN) { popUpTo(0) } }) }
            composable(Routes.ADMIN) { AdminDashboardScreen(vm, nav, onLogout = { nav.navigate(Routes.LOGIN) { popUpTo(0) } }) }
        }
    }
}
