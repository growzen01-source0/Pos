package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PosDao {
    @Query("SELECT * FROM menu_items ORDER BY sortOrder ASC, name ASC")
    fun getAllMenuItems(): Flow<List<MenuItemEntity>>

    @Query("SELECT COUNT(*) FROM menu_items")
    suspend fun getMenuCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItem(item: MenuItemEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMenuItems(items: List<MenuItemEntity>)

    @Update
    suspend fun updateMenuItem(item: MenuItemEntity)

    @Delete
    suspend fun deleteMenuItem(item: MenuItemEntity)

    @Query("DELETE FROM menu_items")
    suspend fun deleteAllMenuItems()

    @Transaction
    @Query("SELECT * FROM orders ORDER BY createdAt DESC, id DESC")
    fun getAllOrdersWithItems(): Flow<List<OrderWithItems>>

    @Transaction
    @Query("SELECT * FROM orders WHERE id = :orderId LIMIT 1")
    suspend fun getOrderWithItemsById(orderId: Long): OrderWithItems?

    @Query("SELECT COALESCE(MAX(orderNumber), 0) + 1 FROM orders")
    suspend fun getNextOrderNumber(): Int

    @Insert
    suspend fun insertOrder(order: OrderEntity): Long

    @Update
    suspend fun updateOrder(order: OrderEntity)

    @Insert
    suspend fun insertOrderItems(items: List<OrderItemEntity>)

    @Insert
    suspend fun insertPaymentTransaction(payment: PaymentTransactionEntity): Long

    @Transaction
    suspend fun insertCompleteOrder(
        order: OrderEntity,
        items: List<OrderItemEntity>
    ): Long {
        val orderId = insertOrder(order)
        val linkedItems = items.map { it.copy(orderId = orderId) }
        insertOrderItems(linkedItems)
        if (order.paidPkr > 0) {
            insertPaymentTransaction(
                PaymentTransactionEntity(
                    orderId = orderId,
                    amountPkr = order.paidPkr,
                    paymentMethod = order.paymentMethod,
                    transactionId = order.transactionId,
                    note = if (order.isUdharPending) "Initial Payment" else "Full Payment",
                    paidAt = order.createdAt
                )
            )
        }
        return orderId
    }

    @Transaction
    suspend fun recordUdharInstallment(
        updatedOrder: OrderEntity,
        payment: PaymentTransactionEntity
    ) {
        updateOrder(updatedOrder)
        insertPaymentTransaction(payment)
    }

    @Query("DELETE FROM orders")
    suspend fun clearAllOrders()
}
