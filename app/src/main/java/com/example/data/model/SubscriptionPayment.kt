package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "subscription_payments")
data class SubscriptionPayment(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val subscriptionId: Int,
    val subscriptionName: String,
    val amount: Double,
    val paymentDate: Long, // timestamp when user marked paid
    val billingCycle: String,
    val paymentSource: String,
    val platform: String,
    val category: String
)
