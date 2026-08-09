package com.example.receiver

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.Bill
import com.example.data.model.Subscription
import java.util.Calendar

object ReminderScheduler {

    fun scheduleBillReminder(context: Context, bill: Bill) {
        if (!bill.isActive) {
            cancelBillReminder(context, bill)
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        // Calculate reminder time
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val targetDay = if (bill.dueDay > maxDays) maxDays else bill.dueDay
        
        calendar.set(Calendar.DAY_OF_MONTH, targetDay)
        calendar.add(Calendar.DAY_OF_YEAR, -bill.customReminderDaysBefore)
        calendar.set(Calendar.HOUR_OF_DAY, 9) // Notify at 9:00 AM
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        
        // If the notification time has already passed for this month's cycle, schedule for next month
        val now = System.currentTimeMillis()
        if (calendar.timeInMillis <= now) {
            calendar.add(Calendar.MONTH, 1)
            val nextMaxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
            val nextTargetDay = if (bill.dueDay > nextMaxDays) nextMaxDays else bill.dueDay
            calendar.set(Calendar.DAY_OF_MONTH, nextTargetDay)
            calendar.add(Calendar.DAY_OF_YEAR, -bill.customReminderDaysBefore)
        }
        
        val triggerTime = calendar.timeInMillis
        val title = "Upcoming Monthly Bill: ${bill.name}"
        val message = "Your ${bill.category} payment of ₹${String.format("%.2f", bill.amount)} is due in ${bill.customReminderDaysBefore} day(s) on day ${bill.dueDay} of this month."

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
            putExtra(ReminderReceiver.EXTRA_ID, bill.id)
        }
        
        // RequestCode is unique per bill: use id * 10 (or matching unique offset) to avoid overlaps
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
        if (!subscription.isAutoNotify || !subscription.isActive) {
            cancelSubscriptionReminder(context, subscription)
            return
        }
        
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        
        // Calculate reminder time: renewalDate minus X days
        val triggerTime = subscription.renewalDate - (subscription.customReminderDaysBefore * 24L * 60 * 60 * 1000)
        
        // Ensure reminder is only scheduled in the future
        if (triggerTime <= System.currentTimeMillis()) {
            return
        }
        
        val title = "Subscription Renewal: ${subscription.name}"
        val message = "Your ${subscription.name} subscription (₹${String.format("%.2f", subscription.amount)}) is renewing via ${subscription.paymentSource} in ${subscription.customReminderDaysBefore} day(s)."

        val intent = Intent(context, ReminderReceiver::class.java).apply {
            putExtra(ReminderReceiver.EXTRA_TITLE, title)
            putExtra(ReminderReceiver.EXTRA_MESSAGE, message)
            putExtra(ReminderReceiver.EXTRA_ID, subscription.id)
        }
        
        // RequestCode is unique per subscription: use id * 10 + 1 to avoid overlaps with bills
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
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val requestCode = bill.id * 2
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }

    fun cancelSubscriptionReminder(context: Context, subscription: Subscription) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReminderReceiver::class.java)
        val requestCode = subscription.id * 2 + 1
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
        }
    }
}
