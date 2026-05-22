package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "bill_payments")
data class BillPayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val billId: Int,
    val billName: String,
    val amount: Double,
    val paymentDate: Long, // timestamp when user marked paid
    val monthYear: String, // "yyyy-MM" corresponding to the billed month
    val category: String
)
