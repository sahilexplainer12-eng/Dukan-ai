package com.dukaanai.app

import com.dukaanai.app.data.local.entities.Customer
import com.dukaanai.app.data.local.models.CustomerWithBalance
import org.junit.Assert.assertEquals
import org.junit.Test

class CustomerBalanceTest {

    @Test
    fun testCustomerOutstandingCreditBalance() {
        val customer = Customer(id = 1, name = "Ramesh Kumar")
        val customerWithBalance = CustomerWithBalance(
            customer = customer,
            totalCredit = 700.0,
            totalPayment = 300.0
        )
        // Customer owes 400
        assertEquals(400.0, customerWithBalance.balance, 0.001)
    }

    @Test
    fun testCustomerAdvancePaymentBalance() {
        val customer = Customer(id = 2, name = "Sunil Bhaiya")
        val customerWithBalance = CustomerWithBalance(
            customer = customer,
            totalCredit = 200.0,
            totalPayment = 500.0
        )
        // Customer has 300 advance (negative balance)
        assertEquals(-300.0, customerWithBalance.balance, 0.001)
    }

    @Test
    fun testCustomerSettledBalance() {
        val customer = Customer(id = 3, name = "Pooja Sharma")
        val customerWithBalance = CustomerWithBalance(
            customer = customer,
            totalCredit = 450.0,
            totalPayment = 450.0
        )
        // Account settled
        assertEquals(0.0, customerWithBalance.balance, 0.001)
    }
}
