package com.example.data.local

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "orders")
data class OrderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderNumber: Int,
    val totalPkr: Int,
    val itemCount: Int,
    val paymentMethod: String,
    val cashReceivedPkr: Int? = null,
    val transactionId: String? = null,
    val paymentMode: String = "Paid in Full",
    val paidPkr: Int = totalPkr,
    val udharPkr: Int = 0,
    val customerName: String? = null,
    val customerPhone: String? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isUdharPending: Boolean
        get() = udharPkr > 0
}

@Entity(
    tableName = "order_items",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"])]
)
data class OrderItemEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val itemName: String,
    val category: String,
    val unitPricePkr: Int,
    val quantity: Int
) {
    val lineTotalPkr: Int
        get() = unitPricePkr * quantity
}

@Entity(
    tableName = "payment_transactions",
    foreignKeys = [
        ForeignKey(
            entity = OrderEntity::class,
            parentColumns = ["id"],
            childColumns = ["orderId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["orderId"])]
)
data class PaymentTransactionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val orderId: Long,
    val amountPkr: Int,
    val paymentMethod: String,
    val transactionId: String? = null,
    val note: String = "Order Payment",
    val paidAt: Long = System.currentTimeMillis()
)

data class OrderWithItems(
    @Embedded val order: OrderEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val items: List<OrderItemEntity>,
    @Relation(
        parentColumn = "id",
        entityColumn = "orderId"
    )
    val payments: List<PaymentTransactionEntity> = emptyList()
)
