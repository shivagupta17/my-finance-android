package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.Bill
import com.example.data.model.Subscription
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object ReminderScheduler {

    fun scheduleBillReminder(context: Context, bill: Bill) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (!bill.isActive) {
            cancelBillReminder(context, bill)
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonthYear = sdf.format(Date())

        // If this bill is already paid or skipped for the current month:
        if (bill.isPaidForMonthYear(currentMonthYear) || bill.isSkippedForMonthYear(currentMonthYear) || bill.isPaidThisMonth()) {
            // Dismiss any active notification immediately
            notificationManager?.cancel(bill.id * 2)
            notificationManager?.cancel(10000 + bill.id)

            // If it's a one-time bill, cancel reminder completely
            if (bill.billingCycle.equals("One-time", ignoreCase = true) || bill.billingCycle.equals("One-Time", ignoreCase = true)) {
                cancelBillReminder(context, bill)
                return
            }
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Determine which month cycle this reminder belongs to
        val now = System.currentTimeMillis()
        val calendar = Calendar.getInstance()
        
        // Target calculation
        val currentMaxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val initialTargetDay = if (bill.dueDay > currentMaxDays) currentMaxDays else bill.dueDay
        
        calendar.set(Calendar.DAY_OF_MONTH, initialTargetDay)
        calendar.set(Calendar.HOUR_OF_DAY, 9)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        calendar.add(Calendar.DAY_OF_YEAR, -bill.customReminderDaysBefore)

        var targetMonthYear = currentMonthYear

        // If this cycle's reminder is already in the past, OR if it's already paid/skipped for this month:
        if (calendar.timeInMillis <= now || bill.isPaidForMonthYear(currentMonthYear) || bill.isSkippedForMonthYear(currentMonthYear) || bill.isPaidThisMonth()) {
            val nextCal = Calendar.getInstance()
            when (bill.billingCycle) {
                "Quarterly" -> nextCal.add(Calendar.MONTH, 3)
                "Yearly" -> nextCal.add(Calendar.YEAR, 1)
                else -> nextCal.add(Calendar.MONTH, 1)
            }
            val nextMaxDays = nextCal.getActualMaximum(Calendar.DAY_OF_MONTH)
            val nextTargetDay = if (bill.dueDay > nextMaxDays) nextMaxDays else bill.dueDay
            
            nextCal.set(Calendar.DAY_OF_MONTH, nextTargetDay)
            nextCal.set(Calendar.HOUR_OF_DAY, 9)
            nextCal.set(Calendar.MINUTE, 0)
            nextCal.set(Calendar.SECOND, 0)
            nextCal.set(Calendar.MILLISECOND, 0)
            nextCal.add(Calendar.DAY_OF_YEAR, -bill.customReminderDaysBefore)

            calendar.timeInMillis = nextCal.timeInMillis
            targetMonthYear = sdf.format(nextCal.time)
        }

        val triggerTime = calendar.timeInMillis
        // Only schedule if trigger time is strictly in the future
        if (triggerTime <= now) return

        val title = "Upcoming Monthly Bill: ${bill.name}"
        val message = "Your ${bill.category} payment of ₹${String.format(Locale.getDefault(), "%.2f", bill.amount)} is due in ${bill.customReminderDaysBefore} day(s) on day ${bill.dueDay}."

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
            putExtra(ReminderReceiver.EXTRA_ID, bill.id * 2)
            putExtra(ReminderReceiver.EXTRA_ITEM_TYPE, "BILL")
            putExtra(ReminderReceiver.EXTRA_ITEM_ID, bill.id)
            putExtra(ReminderReceiver.EXTRA_TARGET_MONTH_YEAR, targetMonthYear)
        }

        val requestCode = bill.id * 2
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun scheduleSubscriptionReminder(context: Context, subscription: Subscription) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

        if (!subscription.isAutoNotify || !subscription.isActive) {
            cancelSubscriptionReminder(context, subscription)
            return
        }

        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val currentMonthYear = sdf.format(Date())

        if (subscription.isPaidForMonthYear(currentMonthYear)) {
            // Dismiss any active notification
            notificationManager?.cancel(subscription.id * 2 + 1)
            notificationManager?.cancel(20000 + subscription.id)
            cancelSubscriptionReminder(context, subscription)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Calculate reminder time: renewalDate minus customReminderDaysBefore at 9:00 AM
        val cal = Calendar.getInstance().apply {
            timeInMillis = subscription.renewalDate
            add(Calendar.DAY_OF_YEAR, -subscription.customReminderDaysBefore)
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        val now = System.currentTimeMillis()
        val triggerTime = cal.timeInMillis

        // If reminder time has passed, or if the subscription renewal has already passed
        if (triggerTime <= now) {
            return
        }

        val targetMonthYear = sdf.format(Date(subscription.renewalDate))
        val title = "Subscription Renewal: ${subscription.name}"
        val message = "Your ${subscription.name} subscription (₹${String.format(Locale.getDefault(), "%.2f", subscription.amount)}) is renewing via ${subscription.paymentSource} in ${subscription.customReminderDaysBefore} day(s)."

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
            putExtra(ReminderReceiver.EXTRA_ID, subscription.id * 2 + 1)
            putExtra(ReminderReceiver.EXTRA_ITEM_TYPE, "SUBSCRIPTION")
            putExtra(ReminderReceiver.EXTRA_ITEM_ID, subscription.id)
            putExtra(ReminderReceiver.EXTRA_TARGET_MONTH_YEAR, targetMonthYear)
        }

        val requestCode = subscription.id * 2 + 1
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && alarmManager.canScheduleExactAlarms()) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            } else {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun cancelBillReminder(context: Context, bill: Bill) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val requestCode = bill.id * 2
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        // Dismiss any currently visible status bar notifications
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(bill.id * 2)
        notificationManager?.cancel(10000 + bill.id)
    }

    fun cancelSubscriptionReminder(context: Context, subscription: Subscription) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager
        val intent = Intent(context, ReminderReceiver::class.java)
        val requestCode = subscription.id * 2 + 1
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null && alarmManager != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }

        // Dismiss any currently visible status bar notifications
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
        notificationManager?.cancel(subscription.id * 2 + 1)
        notificationManager?.cancel(20000 + subscription.id)
    }
}
