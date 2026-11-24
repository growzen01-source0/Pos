package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.MenuItemEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderWithItems
import com.example.data.model.CartItem
import com.example.data.model.CustomerOrderValidator
import com.example.data.model.DefaultMenuSeed
import com.example.data.model.PaymentMethod
import com.example.data.model.PosSettings
import com.example.data.model.UdharPaymentValidator
import com.example.data.model.formatPkr
import com.example.data.repository.PosRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class PosTab(val label: String) {
    POS("POS"),
    UDHAR("Udhar"),
    EDIT_MENU("Menu"),
    SALES("Sales")
}

enum class SalesFilter(val label: String) {
    ALL("All"),
    FULL_PAID("Full Paid"),
    UDHAR("Udhar Due")
}

private data class LocalPosInteractionState(
    val isInitialized: Boolean = false,
    val isUnlocked: Boolean = false,
    val pinInput: String = "",
    val pinError: String? = null,
    val selectedTab: PosTab = PosTab.POS,
    val selectedPosCategory: String = "All",
    val posSearchQuery: String = "",
    val selectedEditorCategory: String = "All",
    val cartQuantities: Map<Long, Int> = emptyMap(),
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val amountPaidInput: String = "",
    val isAmountCustomEdited: Boolean = false,
    val transactionIdInput: String = "",
    val customerNameInput: String = "",
    val customerPhoneInput: String = "",
    val checkoutError: String? = null,
    val salesFilter: SalesFilter = SalesFilter.ALL,
    val udharSearchQuery: String = "",
    val activeReceipt: OrderWithItems? = null,
    val udharPaymentTarget: OrderEntity? = null,
    val statusMessage: String? = null
)

data class PosUiState(
    val isInitialized: Boolean = false,
    val isUnlocked: Boolean = false,
    val pinInput: String = "",
    val pinError: String? = null,
    val settings: PosSettings = PosSettings(),
    val selectedTab: PosTab = PosTab.POS,
    val menuItems: List<MenuItemEntity> = emptyList(),
    val selectedPosCategory: String = "All",
    val posSearchQuery: String = "",
    val selectedEditorCategory: String = "All",
    val cartItems: List<CartItem> = emptyList(),
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val amountPaidInput: String = "",
    val transactionIdInput: String = "",
    val customerNameInput: String = "",
    val customerPhoneInput: String = "",
    val checkoutError: String? = null,
    val salesFilter: SalesFilter = SalesFilter.ALL,
    val udharSearchQuery: String = "",
    val orders: List<OrderWithItems> = emptyList(),
    val activeReceipt: OrderWithItems? = null,
    val udharPaymentTarget: OrderEntity? = null,
    val statusMessage: String? = null
) {
    val allCategories: List<String>
        get() {
            val dynamicCats = menuItems.map { it.category }.distinct()
            val ordered = DefaultMenuSeed.categories.filter { it in dynamicCats }
            val extra = dynamicCats.filter { it !in DefaultMenuSeed.categories }.sorted()
            return listOf("All") + ordered + extra
        }

    val filteredPosMenu: List<MenuItemEntity>
        get() = menuItems.filter { item ->
            val matchesCat = selectedPosCategory == "All" || item.category == selectedPosCategory
            val matchesQuery = posSearchQuery.isBlank() ||
                item.name.contains(posSearchQuery, ignoreCase = true) ||
                item.category.contains(posSearchQuery, ignoreCase = true)
            matchesCat && matchesQuery
        }

    val filteredEditorMenu: List<MenuItemEntity>
        get() = menuItems.filter { item ->
            selectedEditorCategory == "All" || item.category == selectedEditorCategory
        }

    val udharOrders: List<OrderWithItems>
        get() = orders.filter { orderWithItems ->
            if (!orderWithItems.order.isUdharPending) return@filter false
            if (udharSearchQuery.isBlank()) return@filter true
            val name = orderWithItems.order.customerName.orEmpty()
            val phone = orderWithItems.order.customerPhone.orEmpty()
            name.contains(udharSearchQuery, ignoreCase = true) ||
                phone.contains(udharSearchQuery, ignoreCase = true)
        }

    val filteredOrders: List<OrderWithItems>
        get() = when (salesFilter) {
            SalesFilter.ALL -> orders
            SalesFilter.FULL_PAID -> orders.filter { !it.order.isUdharPending }
            SalesFilter.UDHAR -> orders.filter { it.order.isUdharPending }
        }

    val cartTotalPkr: Int
        get() = cartItems.sumOf { it.lineTotalPkr }

    val cartItemCount: Int
        get() = cartItems.sumOf { it.quantity }

    val parsedAmountEntered: Int?
        get() = amountPaidInput.toIntOrNull()

    val isAutoUdhar: Boolean
        get() {
            if (cartTotalPkr <= 0) return false
            val entered = parsedAmountEntered ?: return false
            return entered < cartTotalPkr
        }

    val udharRemainingPreviewPkr: Int
        get() {
            if (cartTotalPkr <= 0) return 0
            val entered = parsedAmountEntered ?: return 0
            return (cartTotalPkr - entered).coerceAtLeast(0)
        }

    val cashChangePreviewPkr: Int?
        get() {
            if (selectedPaymentMethod != PaymentMethod.CASH || cartTotalPkr <= 0) return null
            val entered = parsedAmountEntered ?: return null
            return if (entered > cartTotalPkr) entered - cartTotalPkr else null
        }

    // Sales & Balance Breakdown
    val totalBilledSalesPkr: Int
        get() = orders.sumOf { it.order.totalPkr }

    val totalCurrentBalancePkr: Int
        get() = orders.sumOf { it.order.paidPkr }

    val totalUdharPendingPkr: Int
        get() = orders.sumOf { it.order.udharPkr }

    val totalOriginalUdharIssuedPkr: Int
        get() = orders.filter { it.order.paymentMode.startsWith("Udhar") }
            .sumOf { orderWithItems ->
                val initialPaid = orderWithItems.payments.firstOrNull()?.amountPkr ?: orderWithItems.order.paidPkr
                (orderWithItems.order.totalPkr - initialPaid).coerceAtLeast(orderWithItems.order.udharPkr)
            }

    val totalUdharRecoveredPkr: Int
        get() = (totalOriginalUdharIssuedPkr - totalUdharPendingPkr).coerceAtLeast(0)

    val udharOrderCount: Int
        get() = orders.count { it.order.isUdharPending }

    val udharCustomerCount: Int
        get() = orders
            .filter { it.order.isUdharPending }
            .map { orderWithItems ->
                val phoneKey = orderWithItems.order.customerPhone?.filter { it.isDigit() }.orEmpty()
                if (phoneKey.isNotEmpty()) phoneKey
                else orderWithItems.order.customerName?.trim()?.lowercase() ?: orderWithItems.order.id.toString()
            }
            .distinct()
            .size

    val cashRevenuePkr: Int
        get() = orders.sumOf { orderWithItems ->
            if (orderWithItems.payments.isNotEmpty()) {
                orderWithItems.payments
                    .filter { it.paymentMethod == PaymentMethod.CASH.label }
                    .sumOf { it.amountPkr }
            } else if (orderWithItems.order.paymentMethod == PaymentMethod.CASH.label) {
                orderWithItems.order.paidPkr
            } else 0
        }

    val onlineRevenuePkr: Int
        get() = orders.sumOf { orderWithItems ->
            if (orderWithItems.payments.isNotEmpty()) {
                orderWithItems.payments
                    .filter { it.paymentMethod == PaymentMethod.ONLINE.label }
                    .sumOf { it.amountPkr }
            } else if (orderWithItems.order.paymentMethod == PaymentMethod.ONLINE.label) {
                orderWithItems.order.paidPkr
            } else 0
        }
}

class PosViewModel(private val repository: PosRepository) : ViewModel() {

    private val localState = MutableStateFlow(LocalPosInteractionState())

    val uiState: StateFlow<PosUiState> = combine(
        localState,
        repository.menuItemsFlow,
        repository.ordersFlow,
        repository.settingsFlow
    ) { local, menu, orders, settings ->
        val menuById = menu.associateBy { it.id }
        val resolvedCart = local.cartQuantities.mapNotNull { (itemId, qty) ->
            val item = menuById[itemId]
            if (item != null && item.isAvailable && qty > 0) {
                CartItem(menuItem = item, quantity = qty)
            } else {
                null
            }
        }
        val cartTotal = resolvedCart.sumOf { it.lineTotalPkr }
        val resolvedAmountInput = if (cartTotal <= 0) {
            ""
        } else if (!local.isAmountCustomEdited) {
            cartTotal.toString()
        } else {
            local.amountPaidInput
        }

        val refreshedReceipt = local.activeReceipt?.let { current ->
            orders.find { it.order.id == current.order.id } ?: current
        }

        PosUiState(
            isInitialized = local.isInitialized,
            isUnlocked = local.isUnlocked,
            pinInput = local.pinInput,
            pinError = local.pinError,
            settings = settings,
            selectedTab = local.selectedTab,
            menuItems = menu,
            selectedPosCategory = local.selectedPosCategory,
            posSearchQuery = local.posSearchQuery,
            selectedEditorCategory = local.selectedEditorCategory,
            cartItems = resolvedCart,
            selectedPaymentMethod = local.selectedPaymentMethod,
            amountPaidInput = resolvedAmountInput,
            transactionIdInput = local.transactionIdInput,
            customerNameInput = local.customerNameInput,
            customerPhoneInput = local.customerPhoneInput,
            checkoutError = local.checkoutError,
            salesFilter = local.salesFilter,
            udharSearchQuery = local.udharSearchQuery,
            orders = orders,
            activeReceipt = refreshedReceipt,
            udharPaymentTarget = local.udharPaymentTarget,
            statusMessage = local.statusMessage
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = PosUiState()
    )

    init {
        viewModelScope.launch {
            repository.initializeIfNeeded()
            localState.update { it.copy(isInitialized = true) }
        }
    }

    fun onPinDigit(digit: Char) {
        val current = localState.value
        if (current.pinInput.length >= 6) return
        val updated = current.pinInput + digit
        localState.update { it.copy(pinInput = updated, pinError = null) }
        val expectedLength = uiState.value.settings.pinLength
        if (updated.length == expectedLength) {
            submitPin(updated)
        }
    }

    fun onPinBackspace() {
        localState.update {
            if (it.pinInput.isNotEmpty()) {
                it.copy(pinInput = it.pinInput.dropLast(1), pinError = null)
            } else {
                it.copy(pinError = null)
            }
        }
    }

    fun onPinClear() {
        localState.update { it.copy(pinInput = "", pinError = null) }
    }

    fun submitPin(candidate: String = localState.value.pinInput) {
        if (candidate.length < 4) {
            localState.update { it.copy(pinError = "Enter at least 4 digits") }
            return
        }
        viewModelScope.launch {
            val valid = repository.verifyPin(candidate)
            if (valid) {
                localState.update {
                    it.copy(
                        isUnlocked = true,
                        pinInput = "",
                        pinError = null
                    )
                }
            } else {
                localState.update {
                    it.copy(
                        pinInput = "",
                        pinError = "Incorrect PIN. Try again."
                    )
                }
            }
        }
    }

    fun lockRegister() {
        localState.update {
            it.copy(
                isUnlocked = false,
                pinInput = "",
                pinError = null,
                activeReceipt = null,
                udharPaymentTarget = null
            )
        }
    }

    fun selectTab(tab: PosTab) {
        localState.update { it.copy(selectedTab = tab) }
    }

    fun selectPosCategory(category: String) {
        localState.update { it.copy(selectedPosCategory = category) }
    }

    fun updatePosSearch(query: String) {
        localState.update { it.copy(posSearchQuery = query) }
    }

    fun selectEditorCategory(category: String) {
        localState.update { it.copy(selectedEditorCategory = category) }
    }

    fun selectSalesFilter(filter: SalesFilter) {
        localState.update { it.copy(salesFilter = filter) }
    }

    fun updateUdharSearch(query: String) {
        localState.update { it.copy(udharSearchQuery = query) }
    }

    fun addToCart(item: MenuItemEntity) {
        if (!item.isAvailable) {
            localState.update { it.copy(statusMessage = "${item.name} is marked Sold Out") }
            return
        }
        localState.update { state ->
            val currentQty = state.cartQuantities[item.id] ?: 0
            val updated = state.cartQuantities.toMutableMap()
            updated[item.id] = currentQty + 1
            state.copy(cartQuantities = updated, checkoutError = null)
        }
    }

    fun decrementCartItem(itemId: Long) {
        localState.update { state ->
            val currentQty = state.cartQuantities[itemId] ?: return@update state
            val updated = state.cartQuantities.toMutableMap()
            if (currentQty <= 1) {
                updated.remove(itemId)
            } else {
                updated[itemId] = currentQty - 1
            }
            state.copy(
                cartQuantities = updated,
                isAmountCustomEdited = if (updated.isEmpty()) false else state.isAmountCustomEdited,
                checkoutError = null
            )
        }
    }

    fun clearCart() {
        localState.update {
            it.copy(
                cartQuantities = emptyMap(),
                amountPaidInput = "",
                isAmountCustomEdited = false,
                transactionIdInput = "",
                customerNameInput = "",
                customerPhoneInput = "",
                selectedPaymentMethod = PaymentMethod.CASH,
                checkoutError = null
            )
        }
    }

    fun selectPaymentMethod(method: PaymentMethod) {
        localState.update {
            it.copy(
                selectedPaymentMethod = method,
                transactionIdInput = if (method == PaymentMethod.ONLINE) it.transactionIdInput else "",
                checkoutError = null
            )
        }
    }

    fun updateAmountPaid(input: String) {
        val digitsOnly = input.filter { it.isDigit() }.take(7)
        localState.update {
            it.copy(
                amountPaidInput = digitsOnly,
                isAmountCustomEdited = true,
                checkoutError = null
            )
        }
    }

    fun setFullAmountShortcut() {
        localState.update {
            it.copy(
                isAmountCustomEdited = false,
                amountPaidInput = uiState.value.cartTotalPkr.toString(),
                checkoutError = null
            )
        }
    }

    fun setZeroAmountShortcut() {
        localState.update {
            it.copy(
                isAmountCustomEdited = true,
                amountPaidInput = "0",
                checkoutError = null
            )
        }
    }

    fun updateTransactionId(input: String) {
        localState.update { it.copy(transactionIdInput = input, checkoutError = null) }
    }

    fun updateCustomerName(input: String) {
        localState.update { it.copy(customerNameInput = input, checkoutError = null) }
    }

    fun updateCustomerPhone(input: String) {
        val filtered = input.filter { it.isDigit() || it == '+' || it == '-' || it == ' ' }.take(18)
        localState.update { it.copy(customerPhoneInput = filtered, checkoutError = null) }
    }

    fun completeOrder(): Boolean {
        val currentUi = uiState.value
        val validation = CustomerOrderValidator.validate(
            totalPkr = currentUi.cartTotalPkr,
            paymentMethod = currentUi.selectedPaymentMethod,
            amountPaidInput = currentUi.amountPaidInput,
            transactionIdInput = currentUi.transactionIdInput,
            customerName = currentUi.customerNameInput,
            customerPhone = currentUi.customerPhoneInput
        )

        when (validation) {
            is CustomerOrderValidator.ValidationResult.Invalid -> {
                localState.update { it.copy(checkoutError = validation.message) }
                return false
            }
            is CustomerOrderValidator.ValidationResult.Valid -> {
                viewModelScope.launch {
                    val savedOrder = repository.completeOrder(
                        cartItems = currentUi.cartItems,
                        paymentMethod = currentUi.selectedPaymentMethod,
                        isUdhar = validation.isUdhar,
                        paidPkr = validation.paidPkr,
                        udharPkr = validation.udharPkr,
                        transactionId = validation.transactionId,
                        customerName = validation.customerName,
                        customerPhone = validation.customerPhone,
                        cashReceivedPkr = validation.cashReceivedPkr
                    )
                    localState.update {
                        it.copy(
                            cartQuantities = emptyMap(),
                            amountPaidInput = "",
                            isAmountCustomEdited = false,
                            transactionIdInput = "",
                            customerNameInput = "",
                            customerPhoneInput = "",
                            selectedPaymentMethod = PaymentMethod.CASH,
                            checkoutError = null,
                            activeReceipt = savedOrder
                        )
                    }
                }
                return true
            }
        }
    }

    fun openUdharPaymentDialog(order: OrderEntity) {
        localState.update { it.copy(udharPaymentTarget = order) }
    }

    fun dismissUdharPaymentDialog() {
        localState.update { it.copy(udharPaymentTarget = null) }
    }

    fun submitUdharPayment(
        order: OrderEntity,
        amountInput: String,
        paymentMethod: PaymentMethod,
        transactionIdInput: String,
        onError: (String) -> Unit
    ) {
        val validation = UdharPaymentValidator.validate(
            remainingUdharPkr = order.udharPkr,
            amountInput = amountInput,
            paymentMethod = paymentMethod,
            transactionIdInput = transactionIdInput
        )
        validation.onFailure { err ->
            onError(err.message ?: "Invalid payment amount.")
            return
        }
        val installment = validation.getOrNull() ?: return
        viewModelScope.launch {
            repository.receiveUdharPayment(
                order = order,
                amountPkr = installment.amountPkr,
                paymentMethod = installment.paymentMethod,
                transactionId = installment.transactionId
            )
            val customerLabel = order.customerName ?: "Order #%04d".format(order.orderNumber)
            val message = if (installment.remainingAfterPkr == 0) {
                "$customerLabel's Udhar cleared (${formatPkr(installment.amountPkr)} received)"
            } else {
                "Received ${formatPkr(installment.amountPkr)} from $customerLabel • Remaining: ${formatPkr(installment.remainingAfterPkr)}"
            }
            localState.update {
                it.copy(
                    udharPaymentTarget = null,
                    statusMessage = message
                )
            }
        }
    }

    fun viewReceipt(order: OrderWithItems) {
        localState.update { it.copy(activeReceipt = order) }
    }

    fun dismissReceipt() {
        localState.update { it.copy(activeReceipt = null) }
    }

    fun addMenuItem(name: String, category: String, pricePkr: Int, description: String) {
        viewModelScope.launch {
            repository.addMenuItem(name, category, pricePkr, description)
            localState.update { it.copy(statusMessage = "Added \"$name\" to menu") }
        }
    }

    fun updateMenuItem(item: MenuItemEntity) {
        viewModelScope.launch {
            repository.updateMenuItem(item)
            localState.update { it.copy(statusMessage = "Updated \"${item.name}\"") }
        }
    }

    fun toggleItemAvailability(item: MenuItemEntity) {
        viewModelScope.launch {
            repository.updateMenuItem(item.copy(isAvailable = !item.isAvailable))
        }
    }

    fun deleteMenuItem(item: MenuItemEntity) {
        viewModelScope.launch {
            repository.deleteMenuItem(item)
            localState.update { state ->
                val updatedCart = state.cartQuantities.toMutableMap()
                updatedCart.remove(item.id)
                state.copy(
                    cartQuantities = updatedCart,
                    statusMessage = "Removed \"${item.name}\""
                )
            }
        }
    }

    fun restoreDefaultMenu() {
        viewModelScope.launch {
            repository.restoreDefaultMenu()
            localState.update {
                it.copy(
                    cartQuantities = emptyMap(),
                    isAmountCustomEdited = false,
                    selectedPosCategory = "All",
                    selectedEditorCategory = "All",
                    statusMessage = "Default fast-food menu restored"
                )
            }
        }
    }

    fun clearDailySales() {
        viewModelScope.launch {
            repository.clearSalesHistory()
            localState.update { it.copy(statusMessage = "Daily sales log cleared") }
        }
    }

    fun changePin(
        currentPin: String,
        newPin: String,
        confirmPin: String,
        onResult: (Result<Unit>) -> Unit
    ) {
        if (newPin != confirmPin) {
            onResult(Result.failure(IllegalArgumentException("New PINs do not match.")))
            return
        }
        viewModelScope.launch {
            val result = repository.changePin(currentPin, newPin)
            if (result.isSuccess) {
                localState.update { it.copy(statusMessage = "Staff PIN updated") }
            }
            onResult(result)
        }
    }

    fun updateShopSettings(shopName: String, receiptFooter: String) {
        viewModelScope.launch {
            repository.updateShopSettings(shopName, receiptFooter)
            localState.update { it.copy(statusMessage = "Receipt settings saved") }
        }
    }

    fun consumeStatusMessage() {
        localState.update { it.copy(statusMessage = null) }
    }
}

class PosViewModelFactory(private val repository: PosRepository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(PosViewModel::class.java)) {
            return PosViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
