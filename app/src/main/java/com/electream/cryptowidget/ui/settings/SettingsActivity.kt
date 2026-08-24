package com.electream.cryptowidget.ui.settings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.electream.cryptowidget.ui.nav.Screen
import com.electream.cryptowidget.ui.screens.*
import com.electream.cryptowidget.ui.theme.CoinflowTheme
import com.electream.cryptowidget.ui.theme.toThemeColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    companion object {
        /** Intent extra key used by AlertNotifier to deep-link to a coin's detail screen. */
        const val EXTRA_COIN_ID = "extra_coin_id"
    }

    private val vm: SettingsViewModel by viewModels()

    private val notifPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or denied — no-op; user sees permission dialog */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request POST_NOTIFICATIONS on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED
            ) {
                notifPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        val deepLinkCoinId = intent.getStringExtra(EXTRA_COIN_ID)

        setContent {
            val state by vm.state.collectAsStateWithLifecycle()
            CoinflowTheme(themeColors = state.appTheme.toThemeColors(state.customAccentArgb, state.customSecondaryArgb)) {
                CoinflowNavHost(vm = vm, deepLinkCoinId = deepLinkCoinId)
            }
        }
    }
}

@Composable
private fun CoinflowNavHost(
    vm: SettingsViewModel,
    deepLinkCoinId: String? = null
) {
    val navController = rememberNavController()

    // If opened via notification deep link, navigate directly to the coin's detail screen
    LaunchedEffect(deepLinkCoinId) {
        if (deepLinkCoinId != null) {
            navController.navigate(Screen.coinDetail(deepLinkCoinId))
        }
    }

    NavHost(
        navController    = navController,
        startDestination = Screen.HOME
    ) {
        composable(Screen.HOME) {
            HomeScreen(vm = vm, navController = navController)
        }

        composable(Screen.PORTFOLIO) {
            PortfolioScreen(
                onBack      = { navController.popBackStack() },
                onCoinClick = { coinId -> navController.navigate(Screen.coinDetail(coinId)) }
            )
        }

        composable(
            route     = Screen.COIN_DETAIL,
            arguments = listOf(navArgument(Screen.ARG_COIN_ID) { type = NavType.StringType })
        ) { backStack ->
            val coinId = backStack.arguments?.getString(Screen.ARG_COIN_ID) ?: return@composable
            CoinDetailNavScreen(
                vm     = vm,
                coinId = coinId,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.APPEARANCE) {
            AppearanceScreen(
                vm     = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.WIDGET_SETTINGS) {
            WidgetSettingsScreen(
                vm     = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(Screen.ADD_COIN) {
            AddCustomCoinScreen(onBack = { navController.popBackStack() })
        }

        composable(Screen.DEBUG) {
            DebugScreen(vm = vm, onBack = { navController.popBackStack() })
        }
    }
}
