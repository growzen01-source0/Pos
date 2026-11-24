package com.example.data.model

import com.example.data.local.MenuItemEntity
import java.text.NumberFormat
import java.util.Locale

enum class PaymentMethod(val label: String) {
    CASH("Cash"),
    ONLINE("Online")
}

data class CartItem(
    val menuItem: MenuItemEntity,
    val quantity: Int
) {
    val lineTotalPkr: Int
        get() = menuItem.pricePkr * quantity
}

data class PosSettings(
    val isDefaultPin: Boolean = true,
    val pinLength: Int = 4,
    val shopName: String = "FlamePOS Fast Food",
    val receiptFooter: String = "Thank you for dining with us!"
)

fun formatPkr(amount: Int): String {
    val formatter = NumberFormat.getNumberInstance(Locale.US)
    return "Rs. ${formatter.format(amount)}"
}

object CustomerOrderValidator {
    fun validate(
        totalPkr: Int,
        paymentMethod: PaymentMethod,
        amountPaidInput: String,
        transactionIdInput: String,
        customerName: String,
        customerPhone: String
    ): ValidationResult {
        if (totalPkr <= 0) {
            return ValidationResult.Invalid("Add at least one item to complete the order.")
        }

        val amountFieldLabel = if (paymentMethod == PaymentMethod.CASH) "Amount Received" else "Amount Paid"
        if (amountPaidInput.isBlank()) {
            return ValidationResult.Invalid("Please enter $amountFieldLabel.")
        }

        val amountEntered = amountPaidInput.toIntOrNull()
        if (amountEntered == null || amountEntered < 0) {
            return ValidationResult.Invalid("Enter a valid $amountFieldLabel in PKR.")
        }

        val cleanTxId = transactionIdInput.trim()
        if (paymentMethod == PaymentMethod.ONLINE && cleanTxId.isEmpty()) {
            return ValidationResult.Invalid("PSID / Transaction ID is mandatory for Online payments.")
        }

        val cleanName = customerName.trim()
        val cleanPhone = customerPhone.trim()
        val isUdhar = amountEntered < totalPkr

        if (isUdhar) {
            if (cleanName.isEmpty()) {
                return ValidationResult.Invalid("Customer Name is mandatory for Udhar (Credit) orders.")
            }
            val phoneDigits = cleanPhone.filter { it.isDigit() }
            if (phoneDigits.length < 7) {
                return ValidationResult.Invalid("Valid Phone Number is mandatory for Udhar (Credit) orders.")
            }
        }

        val paidTowardsBill = amountEntered.coerceAtMost(totalPkr)
        val udharDue = (totalPkr - paidTowardsBill).coerceAtLeast(0)
        val cashTendered = if (paymentMethod == PaymentMethod.CASH && !isUdhar) amountEntered else null

        return ValidationResult.Valid(
            isUdhar = isUdhar,
            paidPkr = paidTowardsBill,
            udharPkr = udharDue,
            cashReceivedPkr = cashTendered,
            transactionId = if (paymentMethod == PaymentMethod.ONLINE) cleanTxId else null,
            customerName = cleanName.ifEmpty { null },
            customerPhone = cleanPhone.ifEmpty { null }
        )
    }

    sealed class ValidationResult {
        data class Valid(
            val isUdhar: Boolean,
            val paidPkr: Int,
            val udharPkr: Int,
            val cashReceivedPkr: Int?,
            val transactionId: String?,
            val customerName: String?,
            val customerPhone: String?
        ) : ValidationResult()

        data class Invalid(val message: String) : ValidationResult()
    }
}

object UdharPaymentValidator {
    fun validate(
        remainingUdharPkr: Int,
        amountInput: String,
        paymentMethod: PaymentMethod,
        transactionIdInput: String
    ): Result<ValidatedInstallment> {
        if (remainingUdharPkr <= 0) {
            return Result.failure(IllegalArgumentException("This Udhar account is already cleared."))
        }
        val amount = amountInput.toIntOrNull()
        if (amount == null || amount <= 0) {
            return Result.failure(IllegalArgumentException("Enter a valid payment amount greater than Rs. 0."))
        }
        if (amount > remainingUdharPkr) {
            return Result.failure(
                IllegalArgumentException("Amount cannot exceed remaining Udhar (${formatPkr(remainingUdharPkr)}).")
            )
        }
        val cleanTxId = transactionIdInput.trim()
        if (paymentMethod == PaymentMethod.ONLINE && cleanTxId.isEmpty()) {
            return Result.failure(IllegalArgumentException("Enter PSID / Transaction ID for Online recovery."))
        }
        return Result.success(
            ValidatedInstallment(
                amountPkr = amount,
                remainingAfterPkr = remainingUdharPkr - amount,
                paymentMethod = paymentMethod,
                transactionId = if (paymentMethod == PaymentMethod.ONLINE) cleanTxId else null
            )
        )
    }

    data class ValidatedInstallment(
        val amountPkr: Int,
        val remainingAfterPkr: Int,
        val paymentMethod: PaymentMethod,
        val transactionId: String?
    )
}

object DefaultMenuSeed {
    val categories = listOf(
        "Pizza",
        "Shawarma",
        "Finger Chips",
        "Zinger Shawarma",
        "Zinger Burger"
    )

    fun createSeedItems(): List<MenuItemEntity> = listOf(
        MenuItemEntity(
            name = "Chicken Tikka Pizza (S)",
            category = "Pizza",
            pricePkr = 750,
            description = "Smoky tikka chunks, mozzarella & onions",
            sortOrder = 1
        ),
        MenuItemEntity(
            name = "Chicken Tikka Pizza (M)",
            category = "Pizza",
            pricePkr = 1350,
            description = "10-inch hand-tossed crust with tikka & cheese",
            sortOrder = 2
        ),
        MenuItemEntity(
            name = "Chicken Fajita Pizza (L)",
            category = "Pizza",
            pricePkr = 1850,
            description = "13-inch spiced fajita chicken, peppers & olives",
            sortOrder = 3
        ),
        MenuItemEntity(
            name = "Crown Crust Pizza (L)",
            category = "Pizza",
            pricePkr = 2100,
            description = "Cheese-stuffed crown edges with creamy sauce",
            sortOrder = 4
        ),
        MenuItemEntity(
            name = "Classic Chicken Shawarma",
            category = "Shawarma",
            pricePkr = 320,
            description = "Char-grilled chicken, pickles & garlic mayo",
            sortOrder = 10
        ),
        MenuItemEntity(
            name = "Arabic Garlic Shawarma",
            category = "Shawarma",
            pricePkr = 380,
            description = "Toasted saj wrap with extra toum & fries",
            sortOrder = 11
        ),
        MenuItemEntity(
            name = "Chicken Cheese Shawarma",
            category = "Shawarma",
            pricePkr = 420,
            description = "Melted cheddar & garlic sauce pita wrap",
            sortOrder = 12
        ),
        MenuItemEntity(
            name = "Regular Finger Chips",
            category = "Finger Chips",
            pricePkr = 200,
            description = "Crispy golden salted crinkle fries",
            sortOrder = 20
        ),
        MenuItemEntity(
            name = "Large Masala Fries",
            category = "Finger Chips",
            pricePkr = 350,
            description = "Chaat masala dusted fries with garlic dip",
            sortOrder = 21
        ),
        MenuItemEntity(
            name = "Loaded Cheese Fries",
            category = "Finger Chips",
            pricePkr = 520,
            description = "Topped with cheddar, jalapeños & spicy sauce",
            sortOrder = 22
        ),
        MenuItemEntity(
            name = "Crispy Zinger Shawarma",
            category = "Zinger Shawarma",
            pricePkr = 450,
            description = "Crunchy fried zinger strips in warm pita",
            sortOrder = 30
        ),
        MenuItemEntity(
            name = "Zinger Cheese Shawarma",
            category = "Zinger Shawarma",
            pricePkr = 520,
            description = "Crispy zinger fillet, cheese slice & chipotle",
            sortOrder = 31
        ),
        MenuItemEntity(
            name = "Jumbo Zinger Platter Shawarma",
            category = "Zinger Shawarma",
            pricePkr = 680,
            description = "Double zinger wrap served with fries & dip",
            sortOrder = 32
        ),
        MenuItemEntity(
            name = "Classic Zinger Burger",
            category = "Zinger Burger",
            pricePkr = 480,
            description = "Crunchy chicken thigh fillet, lettuce & mayo",
            sortOrder = 40
        ),
        MenuItemEntity(
            name = "Double Decker Zinger",
            category = "Zinger Burger",
            pricePkr = 750,
            description = "Twin crispy fillets, double cheese & dynamite",
            sortOrder = 41
        ),
        MenuItemEntity(
            name = "Zinger Cheese Meal",
            category = "Zinger Burger",
            pricePkr = 690,
            description = "Zinger cheese burger with regular finger chips",
            sortOrder = 42
        )
    )
}
