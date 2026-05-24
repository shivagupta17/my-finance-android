package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@Entity(tableName = "bills")
data class Bill(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val amount: Double,
    val category: String, // Rent, Electricity, Water, Credit Card, Internet, Insurance, Other
    val dueDay: Int, // Day of month (1-31)
    val customReminderDaysBefore: Int = 1, // Number of days before due date to notify
    val paidMonths: String = "", // Comma-separated string of "yyyy-MM" (e.g., "2026-04,2026-05")
    val notes: String = "",
    val billingCycle: String = "Monthly", // Monthly, Quarterly, Yearly, One-time
    val startMonthYear: String = "", // yyyy-MM
    val isVariable: Boolean = false
) {
    // Helper to check if this bill is due in a selected month-year
    fun isDueInMonthYear(monthYear: String): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val startVal = (startMonthYear as? String) ?: ""
        val cycle = (billingCycle as? String) ?: "Monthly"
        if (startVal.isBlank()) {
            if (cycle == "One-time" || cycle == "One-Time") {
                return monthYear == sdf.format(Date())
            }
            return true
        }
        val effectiveStart = startVal
        val startDate = try { sdf.parse(effectiveStart) } catch (e: Exception) { null } ?: return true
        val selectedDate = try { sdf.parse(monthYear) } catch (e: Exception) { null } ?: return true
        
        val startCal = Calendar.getInstance().apply { time = startDate }
        val selectedCal = Calendar.getInstance().apply { time = selectedDate }
        
        val startMonthCount = startCal.get(Calendar.YEAR) * 12 + startCal.get(Calendar.MONTH)
        val selectedMonthCount = selectedCal.get(Calendar.YEAR) * 12 + selectedCal.get(Calendar.MONTH)
        
        val diffMonths = selectedMonthCount - startMonthCount
        if (diffMonths < 0) return false // Due starting from startMonthYear

        if (cycle == "One-time" || cycle == "One-Time") {
            return monthYear == effectiveStart
        }
        if (cycle == "Monthly") return true
        
        return when (cycle) {
            "Quarterly" -> diffMonths % 3 == 0
            "Yearly" -> diffMonths % 12 == 0
            else -> true
        }
    }
    // Helper to check if this bill is paid in the current month
    fun isPaidForMonthYear(monthYear: String): Boolean {
        val paid = (paidMonths as? String) ?: ""
        if (paid.isEmpty()) return false
        val paidList = paid.split(",")
        return paidList.contains(monthYear)
    }

    // Helper to check if this bill is paid for the current month
    fun isPaidThisMonth(): Boolean {
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonthYear = sdf.format(Date())
        return isPaidForMonthYear(currentMonthYear)
    }

    // Get due date for the current (or next) month
    fun getDueDateForCurrentMonth(): Date {
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        // If the due day of this month has passed and it is already paid for this month, or we just want to show next month's due date
        // However, for visualization, we show the due date of the current month.
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val targetDay = if (dueDay > maxDays) maxDays else dueDay
        
        calendar.set(Calendar.DAY_OF_MONTH, targetDay)
        calendar.set(Calendar.HOUR_OF_DAY, 9) // Default 9:00 AM
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // If target day has passed and it's already paid this month, we can represent next month's due date
        if (currentDay > targetDay && isPaidThisMonth()) {
            calendar.add(Calendar.MONTH, 1)
            val nextMaxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            calendar.set(Calendar.DAY_OF_MONTH, if (dueDay > nextMaxDays) nextMaxDays else dueDay)
        }
        
        return calendar.time
    }

    // Days remaining until next due
    fun daysRemaining(): Int {
        if (isPaidThisMonth()) {
            val nextDue = getDueDateForCurrentMonth()
            val diff = nextDue.time - System.currentTimeMillis()
            return if (diff < 0) 0 else (diff / (1000 * 60 * 60 * 24)).toInt()
        }
        
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val targetDay = if (dueDay > maxDays) maxDays else dueDay
        
        if (currentDay <= targetDay) {
            return targetDay - currentDay
        } else {
            // Due date has passed and is unpaid (overdue)
            return targetDay - currentDay // Negative number represents overdue days
        }
    }
}
