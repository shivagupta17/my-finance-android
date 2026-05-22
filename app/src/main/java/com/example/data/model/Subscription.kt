package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Calendar
import java.util.Date

@Entity(tableName = "subscriptions")
data class Subscription(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String, // Netflix, PlayStation, Apple Music, Spotify, Google One, etc.
    val amount: Double,
    val billingCycle: String, // Monthly, Yearly
    val paymentSource: String, // ICICI credit card, SBI saving account, Apple Pay, etc.
    val platform: String, // Direct, Apple subscription, Google subscription, etc.
    val category: String, // Entertainment, Gaming, Music, Productivity, Utility, Fitness, Other
    val renewalDate: Long, // Next renewal timestamp (millis)
    val isAutoNotify: Boolean = true,
    val customReminderDaysBefore: Int = 2, // Default 2 days before
    val isActive: Boolean = true,
    val notes: String = ""
) {
    // Check if the subscription is renewing soon (e.g., within 7 days)
    fun isRenewingSoon(): Boolean {
        if (!isActive) return false
        val diff = renewalDate - System.currentTimeMillis()
        val sevenDaysMillis = 7L * 24 * 60 * 60 * 1000
        return diff in 0..sevenDaysMillis
    }

    // Days remaining until renewal date
    fun daysRemaining(): Int {
        if (!isActive) return 0
        val diff = renewalDate - System.currentTimeMillis()
        val days = (diff / (1000 * 60 * 60 * 24)).toInt()
        return if (days < 0) 0 else days
    }

    // Generate the next renewal date after renewal is paid/handled
    fun getNextRenewalDate(): Long {
        val calendar = Calendar.getInstance()
        calendar.timeInMillis = renewalDate
        
        when (billingCycle) {
            "Yearly" -> calendar.add(Calendar.YEAR, 1)
            "Quarterly" -> calendar.add(Calendar.MONTH, 3)
            else -> calendar.add(Calendar.MONTH, 1) // Monthly
        }
        return calendar.timeInMillis
    }
}
