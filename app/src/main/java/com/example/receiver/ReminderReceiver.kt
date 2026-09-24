package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class ReminderReceiver : BroadcastReceiver() {
    companion object {
        const val CHANNEL_ID = "bill_subscription_reminders"
        const val CHANNEL_NAME = "Due Date Reminders"
        
        const val EXTRA_TITLE = "extra_title"
        const val EXTRA_MESSAGE = "extra_message"
        const val EXTRA_ID = "extra_id"
        const val EXTRA_ITEM_TYPE = "extra_item_type" // "BILL" or "SUBSCRIPTION"
        const val EXTRA_ITEM_ID = "extra_item_id"
        const val EXTRA_TARGET_MONTH_YEAR = "extra_target_month_year"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val pendingResult = goAsync()

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val itemType = intent.getStringExtra(EXTRA_ITEM_TYPE)
                val itemId = intent.getIntExtra(EXTRA_ITEM_ID, intent.getIntExtra(EXTRA_ID, -1))
                val targetMonthYear = intent.getStringExtra(EXTRA_TARGET_MONTH_YEAR)
                    ?: SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())

                val db = AppDatabase.getDatabase(context)
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager

                // CRITICAL CHECK: Verify thoroughly whether the bill or subscription is already paid
                if (itemType == "BILL" && itemId != -1) {
                    val bill = db.billDao().getBillById(itemId)
                    if (bill == null || !bill.isActive) {
                        notificationManager?.cancel(intent.getIntExtra(EXTRA_ID, itemId * 2))
                        notificationManager?.cancel(10000 + itemId)
                        return@launch
                    }

                    // Check paid status in bill entity
                    val isPaidForTarget = bill.isPaidForMonthYear(targetMonthYear)
                    val isPaidForCurrent = bill.isPaidForMonthYear(currentMonthYear)
                    val isPaidThisMonth = bill.isPaidThisMonth()
                    val isSkipped = bill.isSkippedForMonthYear(targetMonthYear) || bill.isSkippedForMonthYear(currentMonthYear)

                    // Check payments table for any matching record or recent payment (within 28 days)
                    val billPayments = db.billPaymentDao().getPaymentsForBill(itemId)
                    val hasMatchingPayment = billPayments.any { payment ->
                        payment.monthYear == targetMonthYear ||
                        payment.monthYear == currentMonthYear ||
                        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(payment.paymentDate)) == currentMonthYear ||
                        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(payment.paymentDate)) == targetMonthYear ||
                        (System.currentTimeMillis() - payment.paymentDate) < (28L * 24 * 60 * 60 * 1000)
                    }

                    if (isPaidForTarget || isPaidForCurrent || isPaidThisMonth || isSkipped || hasMatchingPayment) {
                        // Already paid or skipped: Cancel notifications and DO NOT alert
                        notificationManager?.cancel(intent.getIntExtra(EXTRA_ID, itemId * 2))
                        notificationManager?.cancel(10000 + itemId)
                        return@launch
                    }
                } else if (itemType == "SUBSCRIPTION" && itemId != -1) {
                    val sub = db.subscriptionDao().getSubscriptionById(itemId)
                    if (sub == null || !sub.isActive || !sub.isAutoNotify) {
                        notificationManager?.cancel(intent.getIntExtra(EXTRA_ID, itemId * 2 + 1))
                        notificationManager?.cancel(20000 + itemId)
                        return@launch
                    }

                    val isPaidForTarget = sub.isPaidForMonthYear(targetMonthYear)
                    val isPaidForCurrent = sub.isPaidForMonthYear(currentMonthYear)
                    val subPayments = db.subscriptionPaymentDao().getAllPaymentsList().filter { it.subscriptionId == itemId }
                    val hasMatchingPayment = subPayments.any { payment ->
                        val paymentMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(payment.paymentDate))
                        paymentMonthYear == targetMonthYear ||
                        paymentMonthYear == currentMonthYear ||
                        (System.currentTimeMillis() - payment.paymentDate) < (28L * 24 * 60 * 60 * 1000)
                    }
                    
                    // If renewal date is pushed well into future (beyond today + customReminderDaysBefore)
                    val isRenewalFarInFuture = sub.renewalDate > (System.currentTimeMillis() + 86400000L)

                    if (isPaidForTarget || isPaidForCurrent || hasMatchingPayment) {
                        // Already paid/renewed: Cancel notifications and DO NOT alert
                        notificationManager?.cancel(intent.getIntExtra(EXTRA_ID, itemId * 2 + 1))
                        notificationManager?.cancel(20000 + itemId)
                        return@launch
                    }
                }

                val title = intent.getStringExtra(EXTRA_TITLE) ?: "Payment Reminder"
                val message = intent.getStringExtra(EXTRA_MESSAGE) ?: "You have an upcoming payment due soon!"
                val notificationId = intent.getIntExtra(EXTRA_ID, if (itemId != -1) (if (itemType == "BILL") itemId * 2 else itemId * 2 + 1) else System.currentTimeMillis().toInt())

                if (notificationManager == null) return@launch

                // Create Channel for Android 8.0+
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    val channel = NotificationChannel(
                        CHANNEL_ID,
                        CHANNEL_NAME,
                        NotificationManager.IMPORTANCE_HIGH
                    ).apply {
                        description = "Reminders for monthly bills and automated subscription renewals"
                        enableVibration(true)
                    }
                    notificationManager.createNotificationChannel(channel)
                }

                // Action when notification clicked - open MainActivity
                val clickIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                }
                val pendingIntent = PendingIntent.getActivity(
                    context,
                    notificationId,
                    clickIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                )

                // Build Notification
                val notification = NotificationCompat.Builder(context, CHANNEL_ID)
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setContentTitle(title)
                    .setContentText(message)
                    .setStyle(NotificationCompat.BigTextStyle().bigText(message))
                    .setPriority(NotificationCompat.PRIORITY_HIGH)
                    .setContentIntent(pendingIntent)
                    .setAutoCancel(true)
                    .build()

                notificationManager.notify(notificationId, notification)
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                pendingResult.finish()
            }
        }
    }
}
