package com.example.data.repository

import com.example.data.local.MenuItemEntity
import com.example.data.local.OrderEntity
import com.example.data.local.OrderItemEntity
import com.example.data.local.OrderWithItems
import com.example.data.local.PaymentTransactionEntity
import com.example.data.local.PosDao
import com.example.data.local.SecurityPreferences
import com.example.data.model.CartItem
import com.example.data.model.DefaultMenuSeed
import com.example.data.model.PaymentMethod
import com.example.data.model.PosSettings
import kotlinx.coroutines.flow.Flow

class PosRepository(
    private val posDao: PosDao,
    private val securityPreferences: SecurityPreferences
) {
    val menuItemsFlow: Flow<List<MenuItemEntity>> = posDao.getAllMenuItems()
    val ordersFlow: Flow<List<OrderWithItems>> = posDao.getAllOrdersWithItems()
    val settingsFlow: Flow<PosSettings> = securityPreferences.settingsFlow

    suspend fun initializeIfNeeded() {
        securityPreferences.ensureInitialized()
        if (posDao.getMenuCount() == 0) {
            posDao.insertMenuItems(DefaultMenuSeed.createSeedItems())
        }
    }

    suspend fun verifyPin(pin: String): Boolean = securityPreferences.verifyPin(pin)

    suspend fun changePin(currentPin: String, newPin: String): Result<Unit> =
        securityPreferences.changePin(currentPin, newPin)

    suspend fun updateShopSettings(shopName: String, receiptFooter: String) =
        securityPreferences.updateShopSettings(shopName, receiptFooter)

    suspend fun addMenuItem(
        name: String,
        category: String,
        pricePkr: Int,
        description: String
    ) {
        posDao.insertMenuItem(
            MenuItemEntity(
                name = name.trim(),
                category = category.trim(),
                pricePkr = pricePkr,
                description = description.trim(),
                isAvailable = true
            )
        )
    }

    suspend fun updateMenuItem(item: MenuItemEntity) {
        posDao.updateMenuItem(item)
    }

    suspend fun deleteMenuItem(item: MenuItemEntity) {
        posDao.deleteMenuItem(item)
    }

    suspend fun restoreDefaultMenu() {
        posDao.deleteAllMenuItems()
        posDao.insertMenuItems(DefaultMenuSeed.createSeedItems())
    }

    suspend fun completeOrder(
        cartItems: List<CartItem>,
        paymentMethod: PaymentMethod,
        isUdhar: Boolean,
        paidPkr: Int,
        udharPkr: Int,
        transactionId: String?,
        customerName: String?,
        customerPhone: String?,
        cashReceivedPkr: Int?
    ): OrderWithItems {
        val nextNumber = posDao.getNextOrderNumber()
        val totalPkr = cartItems.sumOf { it.lineTotalPkr }
        val totalCount = cartItems.sumOf { it.quantity }
        val order = OrderEntity(
            orderNumber = nextNumber,
            totalPkr = totalPkr,
            itemCount = totalCount,
            paymentMethod = paymentMethod.label,
            cashReceivedPkr = cashReceivedPkr,
            transactionId = transactionId,
            paymentMode = if (isUdhar) "Udhar (Credit)" else "Paid in Full",
            paidPkr = paidPkr,
            udharPkr = udharPkr,
            customerName = customerName,
            customerPhone = customerPhone
        )
        val lineItems = cartItems.map { cartItem ->
            OrderItemEntity(
                orderId = 0L,
                itemName = cartItem.menuItem.name,
                category = cartItem.menuItem.category,
                unitPricePkr = cartItem.menuItem.pricePkr,
                quantity = cartItem.quantity
            )
        }
        val savedId = posDao.insertCompleteOrder(order, lineItems)
        return posDao.getOrderWithItemsById(savedId)
            ?: OrderWithItems(order.copy(id = savedId), lineItems)
    }

    suspend fun receiveUdharPayment(
        order: OrderEntity,
        amountPkr: Int,
        paymentMethod: PaymentMethod,
        transactionId: String?
    ): OrderWithItems? {
        val safeAmount = amountPkr.coerceIn(0, order.udharPkr)
        if (safeAmount <= 0) return posDao.getOrderWithItemsById(order.id)

        val newPaid = (order.paidPkr + safeAmount).coerceAtMost(order.totalPkr)
        val newUdhar = (order.totalPkr - newPaid).coerceAtLeast(0)
        val updatedOrder = order.copy(
            paidPkr = newPaid,
            udharPkr = newUdhar
        )
        val paymentNote = if (newUdhar == 0) "Final Udhar Cleared" else "Udhar Installment"
        val tx = PaymentTransactionEntity(
            orderId = order.id,
            amountPkr = safeAmount,
            paymentMethod = paymentMethod.label,
            transactionId = transactionId,
            note = paymentNote
        )
        posDao.recordUdharInstallment(updatedOrder, tx)
        return posDao.getOrderWithItemsById(order.id)
    }

    suspend fun clearSalesHistory() {
        posDao.clearAllOrders()
    }
}
