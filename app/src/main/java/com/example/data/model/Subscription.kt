package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    val notes: String = "",
    val status: String = "Active" // "Active", "Paused", "Cancelled"
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

    // Check if subscription has renewal due or occurs in a given monthYear ("yyyy-MM")
    fun isDueInMonthYear(monthYear: String): Boolean {
        if (!isActive) return false
        
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val targetDate = try { sdf.parse(monthYear) } catch (e: Exception) { null } ?: return false
        val targetCal = Calendar.getInstance().apply { time = targetDate }
        
        val renewalCal = Calendar.getInstance().apply { timeInMillis = renewalDate }
        
        val targetMonthCount = targetCal.get(Calendar.YEAR) * 12 + targetCal.get(Calendar.MONTH)
        val renewalMonthCount = renewalCal.get(Calendar.YEAR) * 12 + renewalCal.get(Calendar.MONTH)
        
        // If the target month is prior to current renewal month, then it is not yet due
        if (targetMonthCount < renewalMonthCount) {
            return false
        }
        
        val diffMonths = targetMonthCount - renewalMonthCount
        return when (billingCycle) {
            "Yearly" -> diffMonths % 12 == 0
            "Quarterly" -> diffMonths % 3 == 0
            else -> true // Monthly
        }
    }

    // Days remaining in selected month-year
    fun daysRemainingForMonthYear(monthYear: String): Int {
        if (!isActive) return 0
        
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val todayCal = Calendar.getInstance()
        
        val renewalCal = Calendar.getInstance().apply { timeInMillis = renewalDate }
        val renewalDay = renewalCal.get(Calendar.DAY_OF_MONTH)
        
        val targetMonthCal = Calendar.getInstance()
        try {
            val date = sdf.parse(monthYear)
            if (date != null) {
                targetMonthCal.time = date
            }
        } catch (e: Exception) {
            return daysRemaining()
        }
        
        val maxDays = targetMonthCal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val targetDay = if (renewalDay > maxDays) maxDays else renewalDay
        
        targetMonthCal.set(Calendar.DAY_OF_MONTH, targetDay)
        targetMonthCal.set(Calendar.HOUR_OF_DAY, 9)
        targetMonthCal.set(Calendar.MINUTE, 0)
        targetMonthCal.set(Calendar.SECOND, 0)
        targetMonthCal.set(Calendar.MILLISECOND, 0)
        
        todayCal.set(Calendar.HOUR_OF_DAY, 0)
        todayCal.set(Calendar.MINUTE, 0)
        todayCal.set(Calendar.SECOND, 0)
        todayCal.set(Calendar.MILLISECOND, 0)
        
        val diff = targetMonthCal.timeInMillis - todayCal.timeInMillis
        return (diff / (1000L * 60 * 60 * 24)).toInt()
    }
}
