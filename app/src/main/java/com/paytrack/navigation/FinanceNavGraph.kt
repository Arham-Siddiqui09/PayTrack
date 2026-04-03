package com.paytrack.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavType
import androidx.navigation.navArgument
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.rememberNavController
import com.paytrack.ui.category.CategoryFormRoute
import com.paytrack.ui.home.HomeRoute
import com.paytrack.ui.insights.InsightsRoute
import com.paytrack.ui.qr.QrScanRoute
import com.paytrack.viewmodel.HomeUiState
import com.paytrack.viewmodel.HomeViewModel

private const val HOME_ROUTE = "home"
private const val QR_SCAN_ROUTE = "qr_scan"
private const val INSIGHTS_ROUTE = "insights"
private const val CATEGORY_FORM_ROUTE = "category_form"
private const val CATEGORY_ID_ARG = "categoryId"

@Composable
fun FinanceNavGraph(
    uiState: HomeUiState,
    viewModel: HomeViewModel,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val qrUiState by viewModel.qrUiState.collectAsStateWithLifecycle()
    val insightsUiState by viewModel.insightsUiState.collectAsStateWithLifecycle()
    val bottomBarItems = listOf(
        BottomBarDestination(HOME_ROUTE, "Home", Icons.Outlined.Home),
        BottomBarDestination(QR_SCAN_ROUTE, "QR Scan", Icons.Outlined.QrCodeScanner),
        BottomBarDestination(INSIGHTS_ROUTE, "Insights", Icons.Outlined.Analytics)
    )

    Scaffold(
        modifier = modifier,
        bottomBar = {
            if (currentDestination?.route != CATEGORY_FORM_ROUTE &&
                currentDestination?.route != "$CATEGORY_FORM_ROUTE?$CATEGORY_ID_ARG={$CATEGORY_ID_ARG}"
            ) {
                NavigationBar {
                    bottomBarItems.forEach { destination ->
                        NavigationBarItem(
                            selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true,
                            onClick = {
                                navController.navigate(destination.route) {
                                    popUpTo(HOME_ROUTE) { saveState = true }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = destination.label
                                )
                            },
                            label = null
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = HOME_ROUTE,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(HOME_ROUTE) {
                HomeRoute(
                    uiState = uiState,
                    onAddCategory = {
                        navController.navigate(CATEGORY_FORM_ROUTE)
                    },
                    onEditCategory = { categoryId ->
                        navController.navigate("$CATEGORY_FORM_ROUTE?$CATEGORY_ID_ARG=$categoryId")
                    },
                    onDeleteCategory = viewModel::deleteCategory,
                    onOpenQr = {
                        navController.navigate(QR_SCAN_ROUTE) {
                            launchSingleTop = true
                        }
                    },
                    onOpenInsights = {
                        navController.navigate(INSIGHTS_ROUTE) {
                            launchSingleTop = true
                        }
                    }
                )
            }

            composable(QR_SCAN_ROUTE) {
                QrScanRoute(
                    uiState = qrUiState,
                    onPermissionResult = viewModel::setCameraPermission,
                    onQrScanned = viewModel::onQrScanned,
                    onAmountChanged = viewModel::updateAmountInput,
                    onFolderSelected = viewModel::updateSelectedFolder,
                    onStartManualConfirmation = viewModel::startManualPaymentConfirmation,
                    onRefreshApps = viewModel::refreshInstalledUpiApps,
                    onLaunchPayment = viewModel::buildUpiLaunchIntent,
                    onPaymentAppOpened = viewModel::recordPaymentWhenUpiAppOpened,
                    onLaunchFailed = viewModel::onUpiLaunchFailed,
                    onConfirmResult = viewModel::confirmPaymentResult,
                    onScanAgain = viewModel::clearScannedQr
                )
            }

            composable(INSIGHTS_ROUTE) {
                InsightsRoute(
                    uiState = insightsUiState,
                    onTransactionBarPeriodChange = viewModel::updateTransactionBarPeriod
                )
            }

            composable(
                route = "$CATEGORY_FORM_ROUTE?$CATEGORY_ID_ARG={$CATEGORY_ID_ARG}",
                arguments = listOf(
                    navArgument(CATEGORY_ID_ARG) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString(CATEGORY_ID_ARG)
                CategoryFormRoute(
                    category = viewModel.getCategory(categoryId),
                    onNavigateBack = { navController.popBackStack() },
                    onSaveNewCategory = { name, amount ->
                        viewModel.createCategory(name, amount)
                        navController.popBackStack()
                    },
                    onSaveEditedCategory = { id, name, amount ->
                        viewModel.updateCategory(id, name, amount)
                        navController.popBackStack()
                    }
                )
            }
        }
    }
}

private data class BottomBarDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
