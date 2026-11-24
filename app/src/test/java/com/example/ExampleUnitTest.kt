package com.example

import com.example.data.model.CartItem
import com.example.data.model.CustomerOrderValidator
import com.example.data.model.DefaultMenuSeed
import com.example.data.model.PaymentMethod
import com.example.data.model.UdharPaymentValidator
import com.example.data.model.formatPkr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun seedMenuContainsAllRequiredCategoriesInPkr() {
        val items = DefaultMenuSeed.createSeedItems()
        val categories = items.map { it.category }.toSet()

        assertTrue(
            categories.containsAll(
                listOf("Pizza", "Shawarma", "Finger Chips", "Zinger Shawarma", "Zinger Burger")
            )
        )
        assertTrue(items.all { it.pricePkr > 0 })
    }

    @Test
    fun onlyCashAndOnlinePaymentMethodsExist() {
        assertEquals(listOf(PaymentMethod.CASH, PaymentMethod.ONLINE), PaymentMethod.entries)
    }

    @Test
    fun cartCalculationsAndPkrFormattingAreAccurate() {
        val items = DefaultMenuSeed.createSeedItems()
        val pizza = items.first { it.category == "Pizza" }
        val burger = items.first { it.category == "Zinger Burger" }

        val cart = listOf(
            CartItem(menuItem = pizza, quantity = 2),
            CartItem(menuItem = burger, quantity = 3)
        )
        val total = cart.sumOf { it.lineTotalPkr }
        assertEquals(pizza.pricePkr * 2 + burger.pricePkr * 3, total)
        assertEquals("Rs. 1,350", formatPkr(1350))
    }

    @Test
    fun smartCashFullPaymentKeepsCustomerOptionalAndAutoSwitchesToUdharWhenLess() {
        val fullCash = CustomerOrderValidator.validate(
            totalPkr = 2000,
            paymentMethod = PaymentMethod.CASH,
            amountPaidInput = "2000",
            transactionIdInput = "",
            customerName = "",
            customerPhone = ""
        )
        assertTrue(fullCash is CustomerOrderValidator.ValidationResult.Valid)
        val fullValid = fullCash as CustomerOrderValidator.ValidationResult.Valid
        assertFalse(fullValid.isUdhar)
        assertEquals(2000, fullValid.paidPkr)
        assertEquals(0, fullValid.udharPkr)
        assertNull(fullValid.customerName)
        assertNull(fullValid.customerPhone)

        val partialCashMissingCustomer = CustomerOrderValidator.validate(
            totalPkr = 2000,
            paymentMethod = PaymentMethod.CASH,
            amountPaidInput = "1000",
            transactionIdInput = "",
            customerName = "",
            customerPhone = ""
        )
        assertTrue(partialCashMissingCustomer is CustomerOrderValidator.ValidationResult.Invalid)

        val partialCashValid = CustomerOrderValidator.validate(
            totalPkr = 2000,
            paymentMethod = PaymentMethod.CASH,
            amountPaidInput = "1000",
            transactionIdInput = "",
            customerName = "Ali Raza",
            customerPhone = "0300-1234567"
        )
        assertTrue(partialCashValid is CustomerOrderValidator.ValidationResult.Valid)
        val udharValid = partialCashValid as CustomerOrderValidator.ValidationResult.Valid
        assertTrue(udharValid.isUdhar)
        assertEquals(1000, udharValid.paidPkr)
        assertEquals(1000, udharValid.udharPkr)
        assertEquals("Ali Raza", udharValid.customerName)
    }

    @Test
    fun smartOnlineRequiresPsidTransactionIdAndAutoUdharWhenLess() {
        val missingTxId = CustomerOrderValidator.validate(
            totalPkr = 1500,
            paymentMethod = PaymentMethod.ONLINE,
            amountPaidInput = "1500",
            transactionIdInput = "",
            customerName = "",
            customerPhone = ""
        )
        assertTrue(missingTxId is CustomerOrderValidator.ValidationResult.Invalid)

        val partialOnlineValid = CustomerOrderValidator.validate(
            totalPkr = 1500,
            paymentMethod = PaymentMethod.ONLINE,
            amountPaidInput = "600",
            transactionIdInput = "TXN-8821",
            customerName = "Sara Khan",
            customerPhone = "0321-7654321"
        )
        assertTrue(partialOnlineValid is CustomerOrderValidator.ValidationResult.Valid)
        val udharOnline = partialOnlineValid as CustomerOrderValidator.ValidationResult.Valid
        assertTrue(udharOnline.isUdhar)
        assertEquals(600, udharOnline.paidPkr)
        assertEquals(900, udharOnline.udharPkr)
        assertEquals("TXN-8821", udharOnline.transactionId)
    }

    @Test
    fun udharInstallmentRecoveryAllowsPartialPaymentsUntilCleared() {
        // Bill 2000, initial paid 1000 -> remaining 1000. Customer pays 500 next day.
        val firstRecovery = UdharPaymentValidator.validate(
            remainingUdharPkr = 1000,
            amountInput = "500",
            paymentMethod = PaymentMethod.CASH,
            transactionIdInput = ""
        )
        assertTrue(firstRecovery.isSuccess)
        val first = firstRecovery.getOrThrow()
        assertEquals(500, first.amountPkr)
        assertEquals(500, first.remainingAfterPkr)

        // Cannot overpay remaining 500
        val overpay = UdharPaymentValidator.validate(
            remainingUdharPkr = 500,
            amountInput = "600",
            paymentMethod = PaymentMethod.CASH,
            transactionIdInput = ""
        )
        assertTrue(overpay.isFailure)

        // Final 500 payment clears Udhar to 0
        val finalRecovery = UdharPaymentValidator.validate(
            remainingUdharPkr = 500,
            amountInput = "500",
            paymentMethod = PaymentMethod.CASH,
            transactionIdInput = ""
        )
        assertTrue(finalRecovery.isSuccess)
        val cleared = finalRecovery.getOrThrow()
        assertEquals(500, cleared.amountPkr)
        assertEquals(0, cleared.remainingAfterPkr)
    }
}
