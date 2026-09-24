package com.example.data.repository

import com.example.data.db.BillDao
import com.example.data.db.SubscriptionDao
import com.example.data.db.SubscriptionPaymentDao
import com.example.data.db.BillPaymentDao
import com.example.data.db.ExpenseDao
import com.example.data.model.Bill
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionPayment
import com.example.data.model.BillPayment
import com.example.data.model.Expense
import kotlinx.coroutines.flow.Flow

class TrackerRepository(
    private val billDao: BillDao,
    private val subscriptionDao: SubscriptionDao,
    private val subscriptionPaymentDao: SubscriptionPaymentDao,
    private val billPaymentDao: BillPaymentDao,
    private val expenseDao: ExpenseDao
) {
    // Bills API
    val allBills: Flow<List<Bill>> = billDao.getAllBills()

    suspend fun getBillById(id: Int): Bill? = billDao.getBillById(id)

    suspend fun insertBill(bill: Bill): Long = billDao.insertBill(bill)

    suspend fun updateBill(bill: Bill) = billDao.updateBill(bill)

    suspend fun deleteBill(bill: Bill) {
        billDao.deleteBill(bill)
        // Clean up from bill payment history too
        billPaymentDao.deletePaymentsByBillId(bill.id)
    }

    // Subscriptions API
    val allSubscriptions: Flow<List<Subscription>> = subscriptionDao.getAllSubscriptions()
    val activeSubscriptions: Flow<List<Subscription>> = subscriptionDao.getActiveSubscriptions()

    suspend fun getSubscriptionById(id: Int): Subscription? = subscriptionDao.getSubscriptionById(id)

    suspend fun insertSubscription(subscription: Subscription): Long = subscriptionDao.insertSubscription(subscription)

    suspend fun updateSubscription(subscription: Subscription) = subscriptionDao.updateSubscription(subscription)

    suspend fun deleteSubscription(subscription: Subscription) {
        subscriptionDao.deleteSubscription(subscription)
        // Clean up from payment history too
        subscriptionPaymentDao.deletePaymentsBySubscriptionId(subscription.id)
    }

    // Payments API
    val allPayments: Flow<List<SubscriptionPayment>> = subscriptionPaymentDao.getAllPayments()

    suspend fun insertPayment(payment: SubscriptionPayment): Long = subscriptionPaymentDao.insertPayment(payment)

    suspend fun deletePaymentById(id: Int) = subscriptionPaymentDao.deletePaymentById(id)

    // Bill Payments API
    val allBillPayments: Flow<List<BillPayment>> = billPaymentDao.getAllBillPayments()

    suspend fun insertBillPayment(payment: BillPayment): Long = billPaymentDao.insertBillPayment(payment)

    suspend fun deleteBillPaymentById(id: Int) = billPaymentDao.deleteBillPaymentById(id)

    suspend fun deleteSubscriptionPaymentBySubIdAndMonth(subId: Int, monthYear: String) =
        subscriptionPaymentDao.deletePaymentsBySubIdAndMonth(subId, monthYear)

    suspend fun deleteBillPaymentByBillIdAndMonth(billId: Int, monthYear: String) =
        billPaymentDao.deletePaymentsByBillIdAndMonth(billId, monthYear)

    // Expenses API
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()

    fun getExpensesByMonth(monthYear: String): Flow<List<Expense>> = expenseDao.getExpensesByMonth(monthYear)

    suspend fun getExpenseById(id: Int): Expense? = expenseDao.getExpenseById(id)

    suspend fun insertExpense(expense: Expense): Long = expenseDao.insertExpense(expense)

    suspend fun updateExpense(expense: Expense) = expenseDao.updateExpense(expense)

    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun deleteExpenseById(id: Int) = expenseDao.deleteExpenseById(id)

    suspend fun clearAllData() {
        billDao.deleteAllBills()
        subscriptionDao.deleteAllSubscriptions()
        subscriptionPaymentDao.deleteAllPayments()
        billPaymentDao.deleteAllBillPayments()
        expenseDao.deleteAllExpenses()
    }
}
