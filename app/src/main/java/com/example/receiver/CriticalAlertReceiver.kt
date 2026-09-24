package com.example.receiver

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.RemoteInput
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.model.BillPayment
import com.example.data.model.SubscriptionPayment
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CriticalAlertReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "critical_payment_alerts"
        const val CHANNEL_NAME = "Payment Due Alerts"

        const val ACTION_CHECK_ALERTS = "com.example.action.CHECK_ALERTS"
        const val ACTION_MARK_PAID = "com.example.action.MARK_PAID"
        const val ACTION_SNOOZE_ALERT = "com.example.action.SNOOZE_ALERT"

        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_ITEM_TYPE = "extra_item_type" // "BILL" or "SUB"
        const val KEY_AMOUNT = "key_amount"

        fun scheduleNextCheck(context: Context, hours: Int) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, CriticalAlertReceiver::class.java).apply {
                action = ACTION_CHECK_ALERTS
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                99999, // General check request code
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerTime = System.currentTimeMillis() + (hours * 60L * 60 * 1000)
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
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = AppDatabase.getDatabase(context)
                when (action) {
                    ACTION_CHECK_ALERTS -> {
                        checkAndNotify(context, db)
                        // Reschedule the next check so that it runs periodically even when app is closed/in background
                        scheduleNextCheck(context, 1)
                    }
                    Intent.ACTION_BOOT_COMPLETED -> {
                        // Restart periodic check
                        scheduleNextCheck(context, 1)
                        
                        // Reschedule all active bill reminders
                        val bills = db.billDao().getAllBillsList()
                        bills.forEach { bill ->
                            ReminderScheduler.scheduleBillReminder(context, bill)
                        }
                        
                        // Reschedule all active subscription reminders
                        val subscriptions = db.subscriptionDao().getAllSubscriptionsList()
                        subscriptions.forEach { sub ->
                            ReminderScheduler.scheduleSubscriptionReminder(context, sub)
                        }
                    }
                    ACTION_MARK_PAID -> {
                        val itemId = intent.getIntExtra(EXTRA_ITEM_ID, -1)
                        val itemType = intent.getStringExtra(EXTRA_ITEM_TYPE)
                        if (itemId != -1 && itemType != null) {
                            handleMarkPaid(context, db, itemId, itemType, intent)
                        }
                    }
                    ACTION_SNOOZE_ALERT -> {
                        // User cleared/dismissed notification without paying
                        val sharedPrefs = context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE)
                        val snoozeHours = sharedPrefs.getInt("snooze_hours", 1)
                        scheduleNextCheck(context, snoozeHours)
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }

    internal suspend fun checkAndNotify(context: Context, db: AppDatabase) {
        val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val currentTime = System.currentTimeMillis()

        val bills = db.billDao().getAllBillsList()
        val allBillPayments = db.billPaymentDao().getAllBillPaymentsList()
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        // Create high importance alerts channel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Reminder channel for bill and subscription deadlines"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        bills.forEach { bill ->
            val isDue = bill.isDueInMonthYear(currentMonthYear)
            val isPaidInBill = bill.isPaidForMonthYear(currentMonthYear) || bill.isPaidThisMonth()
            val isSkipped = bill.isSkippedForMonthYear(currentMonthYear)
            
            // Check if there is an explicit payment record in bill payments table
            val hasPaymentRecord = allBillPayments.any { payment ->
                payment.billId == bill.id && (
                    payment.monthYear == currentMonthYear ||
                    SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(payment.paymentDate)) == currentMonthYear ||
                    (currentTime - payment.paymentDate) < (28L * 24 * 60 * 60 * 1000)
                )
            }

            val isEffectivelyPaid = isPaidInBill || hasPaymentRecord
            val isDateReached = currentDay >= bill.dueDay

            if (bill.isActive && isDue && !isEffectivelyPaid && !isSkipped && isDateReached) {
                val isOverdue = currentDay > bill.dueDay
                showCriticalNotification(context, notificationManager, bill.id, "BILL", bill.name, bill.amount, bill.isVariable, "Day ${bill.dueDay} of this month", isOverdue)
            } else if (isEffectivelyPaid || isSkipped || !bill.isActive) {
                // Bill is paid, skipped, or inactive: dismiss any active notification
                notificationManager.cancel(10000 + bill.id)
                notificationManager.cancel(bill.id * 2)
            }
        }

        // Check subscriptions
        val subscriptions = db.subscriptionDao().getAllSubscriptionsList()
        val allSubPayments = db.subscriptionPaymentDao().getAllPaymentsList()
        subscriptions.forEach { sub ->
            val isPaidInSub = sub.isPaidForMonthYear(currentMonthYear)
            val hasPaymentRecord = allSubPayments.any { payment ->
                payment.subscriptionId == sub.id && (
                    SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(payment.paymentDate)) == currentMonthYear ||
                    (currentTime - payment.paymentDate) < (28L * 24 * 60 * 60 * 1000)
                )
            }
            val isEffectivelyPaid = isPaidInSub || hasPaymentRecord

            if (sub.isActive && sub.isAutoNotify && !isEffectivelyPaid && currentTime >= sub.renewalDate) {
                val formattedDate = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(sub.renewalDate))
                val isOverdue = currentTime > sub.renewalDate
                showCriticalNotification(context, notificationManager, sub.id, "SUB", sub.name, sub.amount, false, formattedDate, isOverdue)
            } else if (isEffectivelyPaid || !sub.isActive || !sub.isAutoNotify) {
                // Subscription is paid, renewed, or inactive: dismiss any active notification
                notificationManager.cancel(20000 + sub.id)
                notificationManager.cancel(sub.id * 2 + 1)
            }
        }
    }

    private fun showCriticalNotification(
        context: Context,
        notificationManager: NotificationManager,
        itemId: Int,
        itemType: String,
        name: String,
        amount: Double,
        isVariable: Boolean,
        dueDateStr: String,
        isOverdue: Boolean
    ) {
        val notificationId = if (itemType == "BILL") 10000 + itemId else 20000 + itemId

        // Open app on click
        val clickIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            clickIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Delete intent (when notification is dismissed/cleared)
        val deleteIntent = Intent(context, CriticalAlertReceiver::class.java).apply {
            action = ACTION_SNOOZE_ALERT
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_ITEM_TYPE, itemType)
        }
        val deletePendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 40000,
            deleteIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Title and body message
        val title = if (isOverdue) "Overdue: $name" else "Due: $name"
        val itemLabel = if (itemType == "BILL") "bill" else "subscription"
        val message = "Your $itemLabel of ₹${String.format(Locale.getDefault(), "%.2f", amount)} is unpaid (Due date: $dueDateStr). Tap to manage."

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(pendingIntent)
            .setDeleteIntent(deletePendingIntent)
            .setAutoCancel(true)

        // Set action button for "Mark Paid"
        val payIntent = Intent(context, CriticalAlertReceiver::class.java).apply {
            action = ACTION_MARK_PAID
            putExtra(EXTRA_ITEM_ID, itemId)
            putExtra(EXTRA_ITEM_TYPE, itemType)
        }
        val payPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId + 30000,
            payIntent,
            if (itemType == "BILL" && isVariable) {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            } else {
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            }
        )

        if (itemType == "BILL" && isVariable) {
            // Include RemoteInput for custom amount
            val remoteInput = RemoteInput.Builder(KEY_AMOUNT)
                .setLabel("Enter amount in ₹")
                .build()

            val action = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_save,
                "Pay Custom Amount",
                payPendingIntent
            ).addRemoteInput(remoteInput).build()

            builder.addAction(action)
        } else {
            val action = NotificationCompat.Action.Builder(
                android.R.drawable.ic_menu_save,
                "Mark Paid",
                payPendingIntent
            ).build()

            builder.addAction(action)
        }

        notificationManager.notify(notificationId, builder.build())
    }

    private suspend fun handleMarkPaid(
        context: Context,
        db: AppDatabase,
        itemId: Int,
        itemType: String,
        intent: Intent
    ) {
        val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return
        val notificationId = if (itemType == "BILL") 10000 + itemId else 20000 + itemId

        if (itemType == "BILL") {
            val bill = db.billDao().getBillById(itemId)
            if (bill != null) {
                // Read optional amount from RemoteInput
                val results = RemoteInput.getResultsFromIntent(intent)
                val amountText = results?.getCharSequence(KEY_AMOUNT)?.toString()
                val finalAmount = amountText?.toDoubleOrNull() ?: bill.amount

                // Check if already paid to avoid double operations
                val isCurrentlyPaid = bill.isPaidForMonthYear(currentMonthYear)
                if (!isCurrentlyPaid) {
                    val updatedPaidMonths = if (bill.paidMonths.isEmpty()) currentMonthYear else "${bill.paidMonths},$currentMonthYear"
                    val updatedBill = bill.copy(paidMonths = updatedPaidMonths)
                    db.billDao().updateBill(updatedBill)

                    val payment = BillPayment(
                        billId = bill.id,
                        billName = bill.name,
                        amount = finalAmount,
                        paymentDate = System.currentTimeMillis(),
                        monthYear = currentMonthYear,
                        category = bill.category
                    )
                    db.billPaymentDao().insertBillPayment(payment)
                    
                    // Reschedule regular alarm
                    ReminderScheduler.cancelBillReminder(context, updatedBill)
                    ReminderScheduler.scheduleBillReminder(context, updatedBill)
                }
            }
        } else {
            val subscription = db.subscriptionDao().getSubscriptionById(itemId)
            if (subscription != null) {
                val nextRenewal = subscription.getNextRenewalDate()
                val updatedSub = subscription.copy(renewalDate = nextRenewal)
                db.subscriptionDao().updateSubscription(updatedSub)

                val payment = SubscriptionPayment(
                    subscriptionId = subscription.id,
                    subscriptionName = subscription.name,
                    amount = subscription.amount,
                    paymentDate = System.currentTimeMillis(),
                    billingCycle = subscription.billingCycle,
                    paymentSource = subscription.paymentSource,
                    platform = subscription.platform,
                    category = subscription.category
                )
                db.subscriptionPaymentDao().insertPayment(payment)

                // Reschedule regular reminder
                ReminderScheduler.cancelSubscriptionReminder(context, updatedSub)
                if (updatedSub.isActive && updatedSub.isAutoNotify) {
                    ReminderScheduler.scheduleSubscriptionReminder(context, updatedSub)
                }
            }
        }

        // Dismiss notice and regular reminder immediately
        notificationManager.cancel(notificationId)
        if (itemType == "BILL") {
            notificationManager.cancel(itemId * 2)
        } else {
            notificationManager.cancel(itemId * 2 + 1)
        }
    }
}
