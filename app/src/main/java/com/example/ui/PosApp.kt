package com.example.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccountBalanceWallet
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material.icons.outlined.Storefront
import androidx.compose.material.icons.rounded.AccountBalanceWallet
import androidx.compose.material.icons.rounded.BarChart
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.Storefront
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.formatPkr
import com.example.ui.components.MenuEditorScreen
import com.example.ui.components.PinLockScreen
import com.example.ui.components.PosBillingScreen
import com.example.ui.components.ReceiptDialog
import com.example.ui.components.ReceiveUdharPaymentDialog
import com.example.ui.components.SalesHistoryScreen
import com.example.ui.components.SettingsDialog
import com.example.ui.components.UdharLedgerScreen
import com.example.ui.theme.JetBrainsMonoFontFamily

@Composable
fun PosApp(viewModel: PosViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Crossfade(
        targetState = uiState.isUnlocked,
        label = "auth_gate_transition"
    ) { unlocked ->
        if (!unlocked) {
            PinLockScreen(
                shopName = uiState.settings.shopName,
                pinInput = uiState.pinInput,
                expectedPinLength = uiState.settings.pinLength,
                isDefaultPin = uiState.settings.isDefaultPin,
                pinError = uiState.pinError,
                onDigitClick = viewModel::onPinDigit,
                onBackspaceClick = viewModel::onPinBackspace,
                onClearClick = viewModel::onPinClear,
                onSubmitClick = { viewModel.submitPin() }
            )
        } else {
            PosMainWorkspace(
                uiState = uiState,
                viewModel = viewModel
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PosMainWorkspace(
    uiState: PosUiState,
    viewModel: PosViewModel
) {
    var showSettingsDialog by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(uiState.statusMessage) {
        val msg = uiState.statusMessage
        if (msg != null) {
            snackbarHostState.showSnackbar(msg)
            viewModel.consumeStatusMessage()
        }
    }

    if (uiState.selectedTab != PosTab.POS) {
        BackHandler {
            viewModel.selectTab(PosTab.POS)
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 760.dp

        Scaffold(
            snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            topBar = {
                CenterAlignedTopAppBar(
                    colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    ),
                    navigationIcon = {
                        IconButton(
                            onClick = { viewModel.lockRegister() },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("lock_register_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Lock,
                                contentDescription = "Lock Register",
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    title = {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = uiState.settings.shopName,
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Surface(
                                shape = RoundedCornerShape(999.dp),
                                color = MaterialTheme.colorScheme.tertiaryContainer
                            ) {
                                val badgeText = if (uiState.totalUdharPendingPkr > 0) {
                                    "Bal: ${formatPkr(uiState.totalCurrentBalancePkr)} • Udhar: ${formatPkr(uiState.totalUdharPendingPkr)}"
                                } else {
                                    "Balance: ${formatPkr(uiState.totalCurrentBalancePkr)}"
                                }
                                Text(
                                    text = badgeText,
                                    fontFamily = JetBrainsMonoFontFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 2.dp)
                                )
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showSettingsDialog = true },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("open_settings_button")
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Settings,
                                contentDescription = "POS Settings",
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                )
            },
            bottomBar = {
                if (!useRail) {
                    NavigationBar(
                        windowInsets = WindowInsets.navigationBars,
                        containerColor = MaterialTheme.colorScheme.surface,
                        tonalElevation = 3.dp
                    ) {
                        PosTab.entries.forEach { tab ->
                            val selected = uiState.selectedTab == tab
                            val (selectedIcon, unselectedIcon) = tabIcons(tab)
                            NavigationBarItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    if (tab == PosTab.UDHAR && uiState.udharCustomerCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(uiState.udharCustomerCount.toString())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) selectedIcon else unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) selectedIcon else unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = {
                                    Text(
                                        text = tab.label,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                },
                                modifier = Modifier.testTag("nav_tab_${tab.name.lowercase()}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (useRail) {
                    NavigationRail(
                        containerColor = MaterialTheme.colorScheme.surface,
                        modifier = Modifier.fillMaxHeight()
                    ) {
                        Spacer(modifier = Modifier.weight(0.15f))
                        PosTab.entries.forEach { tab ->
                            val selected = uiState.selectedTab == tab
                            val (selectedIcon, unselectedIcon) = tabIcons(tab)
                            NavigationRailItem(
                                selected = selected,
                                onClick = { viewModel.selectTab(tab) },
                                icon = {
                                    if (tab == PosTab.UDHAR && uiState.udharCustomerCount > 0) {
                                        BadgedBox(
                                            badge = {
                                                Badge {
                                                    Text(uiState.udharCustomerCount.toString())
                                                }
                                            }
                                        ) {
                                            Icon(
                                                imageVector = if (selected) selectedIcon else unselectedIcon,
                                                contentDescription = tab.label
                                            )
                                        }
                                    } else {
                                        Icon(
                                            imageVector = if (selected) selectedIcon else unselectedIcon,
                                            contentDescription = tab.label
                                        )
                                    }
                                },
                                label = { Text(tab.label) },
                                modifier = Modifier.testTag("rail_tab_${tab.name.lowercase()}")
                            )
                        }
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                Box(modifier = Modifier.weight(1f)) {
                    when (uiState.selectedTab) {
                        PosTab.POS -> PosBillingScreen(
                            categories = uiState.allCategories,
                            selectedCategory = uiState.selectedPosCategory,
                            searchQuery = uiState.posSearchQuery,
                            filteredMenuItems = uiState.filteredPosMenu,
                            cartItems = uiState.cartItems,
                            cartTotalPkr = uiState.cartTotalPkr,
                            cartItemCount = uiState.cartItemCount,
                            selectedPaymentMethod = uiState.selectedPaymentMethod,
                            amountPaidInput = uiState.amountPaidInput,
                            isAutoUdhar = uiState.isAutoUdhar,
                            udharRemainingPkr = uiState.udharRemainingPreviewPkr,
                            cashChangePkr = uiState.cashChangePreviewPkr,
                            transactionIdInput = uiState.transactionIdInput,
                            customerNameInput = uiState.customerNameInput,
                            customerPhoneInput = uiState.customerPhoneInput,
                            checkoutError = uiState.checkoutError,
                            onSelectCategory = viewModel::selectPosCategory,
                            onSearchChange = viewModel::updatePosSearch,
                            onAddToCart = viewModel::addToCart,
                            onDecrementCartItem = viewModel::decrementCartItem,
                            onClearCart = viewModel::clearCart,
                            onSelectPaymentMethod = viewModel::selectPaymentMethod,
                            onAmountPaidChange = viewModel::updateAmountPaid,
                            onFullAmountShortcut = viewModel::setFullAmountShortcut,
                            onZeroAmountShortcut = viewModel::setZeroAmountShortcut,
                            onTransactionIdChange = viewModel::updateTransactionId,
                            onCustomerNameChange = viewModel::updateCustomerName,
                            onCustomerPhoneChange = viewModel::updateCustomerPhone,
                            onCompleteOrder = viewModel::completeOrder
                        )

                        PosTab.UDHAR -> UdharLedgerScreen(
                            udharOrders = uiState.udharOrders,
                            totalUdharPendingPkr = uiState.totalUdharPendingPkr,
                            distinctCustomersCount = uiState.udharCustomerCount,
                            pendingBillsCount = uiState.udharOrderCount,
                            searchQuery = uiState.udharSearchQuery,
                            onSearchChange = viewModel::updateUdharSearch,
                            onOrderClick = viewModel::viewReceipt,
                            onOpenReceivePayment = viewModel::openUdharPaymentDialog
                        )

                        PosTab.EDIT_MENU -> MenuEditorScreen(
                            categories = uiState.allCategories,
                            selectedCategory = uiState.selectedEditorCategory,
                            menuItems = uiState.filteredEditorMenu,
                            onSelectCategory = viewModel::selectEditorCategory,
                            onAddItem = viewModel::addMenuItem,
                            onUpdateItem = viewModel::updateMenuItem,
                            onToggleAvailability = viewModel::toggleItemAvailability,
                            onDeleteItem = viewModel::deleteMenuItem
                        )

                        PosTab.SALES -> SalesHistoryScreen(
                            orders = uiState.filteredOrders,
                            totalOrdersCount = uiState.orders.size,
                            totalBilledSalesPkr = uiState.totalBilledSalesPkr,
                            totalCurrentBalancePkr = uiState.totalCurrentBalancePkr,
                            totalUdharPendingPkr = uiState.totalUdharPendingPkr,
                            udharCustomerCount = uiState.udharCustomerCount,
                            udharOrderCount = uiState.udharOrderCount,
                            cashRevenuePkr = uiState.cashRevenuePkr,
                            onlineRevenuePkr = uiState.onlineRevenuePkr,
                            selectedFilter = uiState.salesFilter,
                            onSelectFilter = viewModel::selectSalesFilter,
                            onOrderClick = viewModel::viewReceipt,
                            onReceiveUdharPayment = viewModel::openUdharPaymentDialog,
                            onClearDailySales = viewModel::clearDailySales
                        )
                    }
                }
            }
        }
    }

    uiState.activeReceipt?.let { receiptOrder ->
        ReceiptDialog(
            orderWithItems = receiptOrder,
            shopName = uiState.settings.shopName,
            receiptFooter = uiState.settings.receiptFooter,
            onReceiveUdharPayment = viewModel::openUdharPaymentDialog,
            onDismiss = viewModel::dismissReceipt
        )
    }

    uiState.udharPaymentTarget?.let { targetOrder ->
        ReceiveUdharPaymentDialog(
            order = targetOrder,
            onDismiss = viewModel::dismissUdharPaymentDialog,
            onConfirmPayment = { amountInput, method, txId, onError ->
                viewModel.submitUdharPayment(
                    order = targetOrder,
                    amountInput = amountInput,
                    paymentMethod = method,
                    transactionIdInput = txId,
                    onError = onError
                )
            }
        )
    }

    if (showSettingsDialog) {
        SettingsDialog(
            settings = uiState.settings,
            onChangePin = viewModel::changePin,
            onSaveShopSettings = viewModel::updateShopSettings,
            onRestoreDefaultMenu = viewModel::restoreDefaultMenu,
            onDismiss = { showSettingsDialog = false }
        )
    }
}

private fun tabIcons(tab: PosTab) = when (tab) {
    PosTab.POS -> Icons.Rounded.Storefront to Icons.Outlined.Storefront
    PosTab.UDHAR -> Icons.Rounded.AccountBalanceWallet to Icons.Outlined.AccountBalanceWallet
    PosTab.EDIT_MENU -> Icons.Rounded.EditNote to Icons.Outlined.EditNote
    PosTab.SALES -> Icons.Rounded.BarChart to Icons.Outlined.BarChart
}
