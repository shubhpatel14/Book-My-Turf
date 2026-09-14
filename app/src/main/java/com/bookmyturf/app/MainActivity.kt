package com.bookmyturf.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

private val mainTabs = listOf(Routes.USER, Routes.EXPLORE, Routes.BOOKINGS, Routes.PROFILE)
private fun tabDirection(from: String?, to: String?): Int {
    val fromIndex = mainTabs.indexOf(from)
    val toIndex = mainTabs.indexOf(to)
    return if (fromIndex >= 0 && toIndex >= 0 && toIndex < fromIndex) -1 else 1
}

@Composable
fun BookMyTurfRoot() {
    val context = LocalContext.current
    val app = context.applicationContext as BookMyTurfApplication
    val factory = remember(app) { AppViewModelFactory(app.repository) }
    val vm: AppViewModel = viewModel(factory = factory)
    val nav = rememberNavController()
    val currentRoute = nav.currentBackStackEntryAsState().value?.destination?.route
    val snackbar = remember { SnackbarHostState() }
    val message by vm.snackbar
    LaunchedEffect(message) {
        if (message != null) { snackbar.showSnackbar(message!!); vm.clearSnackbar() }
    }
    Scaffold(snackbarHost = { SnackbarHost(snackbar) }) { _ ->
        Box(Modifier.fillMaxSize()) {
            NavHost(
                navController = nav,
                startDestination = Routes.SPLASH,
            enterTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                if (from in mainTabs && to in mainTabs) {
                    fadeIn(tween(180, easing = FastOutSlowInEasing))
                } else {
                    val direction = tabDirection(from, to)
                    fadeIn(tween(220)) + slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { direction * it / 5 }
                }
            },
            exitTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                if (from in mainTabs && to in mainTabs) {
                    fadeOut(tween(130, easing = FastOutSlowInEasing))
                } else {
                    val direction = tabDirection(from, to)
                    fadeOut(tween(190)) + slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { -direction * it / 8 }
                }
            },
            popEnterTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                if (from in mainTabs && to in mainTabs) {
                    fadeIn(tween(180, easing = FastOutSlowInEasing))
                } else {
                    val direction = tabDirection(from, to)
                    fadeIn(tween(220)) + slideInHorizontally(tween(300, easing = FastOutSlowInEasing)) { direction * it / 5 }
                }
            },
            popExitTransition = {
                val from = initialState.destination.route
                val to = targetState.destination.route
                if (from in mainTabs && to in mainTabs) {
                    fadeOut(tween(130, easing = FastOutSlowInEasing))
                } else {
                    val direction = tabDirection(from, to)
                    fadeOut(tween(190)) + slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { -direction * it / 8 }
                }
            }
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
            if (currentRoute in mainTabs) {
                UserBottomBar(currentRoute!!, nav, Modifier.align(Alignment.BottomCenter))
            }
        }
    }
}
