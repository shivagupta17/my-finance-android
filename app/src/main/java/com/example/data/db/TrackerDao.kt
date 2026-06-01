package com.example.data.db

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.Bill
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionPayment
import com.example.data.model.BillPayment
import kotlinx.coroutines.flow.Flow

@Dao
interface BillDao {
    @Query("SELECT * FROM bills ORDER BY dueDay ASC")
    fun getAllBills(): Flow<List<Bill>>

    @Query("SELECT * FROM bills WHERE id = :id")
    suspend fun getBillById(id: Int): Bill?

    @Query("SELECT * FROM bills")
    suspend fun getAllBillsList(): List<Bill>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBill(bill: Bill): Long

    @Update
    suspend fun updateBill(bill: Bill)

    @Delete
    suspend fun deleteBill(bill: Bill)

    @Query("DELETE FROM bills")
    suspend fun deleteAllBills()
}

@Dao
interface SubscriptionDao {
    @Query("SELECT * FROM subscriptions WHERE isActive = 1 ORDER BY renewalDate ASC")
    fun getActiveSubscriptions(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions ORDER BY renewalDate ASC")
    fun getAllSubscriptions(): Flow<List<Subscription>>

    @Query("SELECT * FROM subscriptions WHERE id = :id")
    suspend fun getSubscriptionById(id: Int): Subscription?

    @Query("SELECT * FROM subscriptions")
    suspend fun getAllSubscriptionsList(): List<Subscription>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSubscription(subscription: Subscription): Long

    @Update
    suspend fun updateSubscription(subscription: Subscription)

    @Delete
    suspend fun deleteSubscription(subscription: Subscription)

    @Query("DELETE FROM subscriptions")
    suspend fun deleteAllSubscriptions()
}

@Dao
interface SubscriptionPaymentDao {
    @Query("SELECT * FROM subscription_payments ORDER BY paymentDate DESC")
    fun getAllPayments(): Flow<List<SubscriptionPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: SubscriptionPayment): Long

    @Query("DELETE FROM subscription_payments WHERE id = :id")
    suspend fun deletePaymentById(id: Int)

    @Query("DELETE FROM subscription_payments WHERE subscriptionId = :subId")
    suspend fun deletePaymentsBySubscriptionId(subId: Int)

    @Query("DELETE FROM subscription_payments WHERE subscriptionId = :subId AND monthYear = :monthYear")
    suspend fun deletePaymentsBySubIdAndMonth(subId: Int, monthYear: String)

    @Query("DELETE FROM subscription_payments")
    suspend fun deleteAllPayments()
}

@Dao
interface BillPaymentDao {
    @Query("SELECT * FROM bill_payments ORDER BY paymentDate DESC")
    fun getAllBillPayments(): Flow<List<BillPayment>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertBillPayment(payment: BillPayment): Long

    @Query("DELETE FROM bill_payments WHERE id = :id")
    suspend fun deleteBillPaymentById(id: Int)

    @Query("DELETE FROM bill_payments WHERE billId = :billId")
    suspend fun deletePaymentsByBillId(billId: Int)

    @Query("DELETE FROM bill_payments WHERE billId = :billId AND monthYear = :monthYear")
    suspend fun deletePaymentsByBillIdAndMonth(billId: Int, monthYear: String)

    @Query("DELETE FROM bill_payments")
    suspend fun deleteAllBillPayments()
}

