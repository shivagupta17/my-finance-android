package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class GlanceBillWidgetProvider : AppWidgetProvider() {

    private val providerScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        super.onUpdate(context, appWidgetManager, appWidgetIds)
        updateAllWidgets(context, appWidgetManager, appWidgetIds)
    }

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == ACTION_UPDATE_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, GlanceBillWidgetProvider::class.java)
            val appWidgetIds = appWidgetManager.getAppWidgetIds(componentName)
            if (appWidgetIds != null && appWidgetIds.isNotEmpty()) {
                updateAllWidgets(context, appWidgetManager, appWidgetIds)
            }
        }
    }

    private fun updateAllWidgets(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        providerScope.launch {
            try {
                val db = AppDatabase.getDatabase(context)
                val sdfMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault())
                val currentMonthYear = sdfMonthYear.format(Date())
                val currentMonthName = SimpleDateFormat("MMMM", Locale.getDefault()).format(Date()).uppercase()

                // 1. Calculate Total Spent This Month
                val billPayments = db.billPaymentDao().getAllBillPaymentsList()
                val subPayments = db.subscriptionPaymentDao().getAllPaymentsList()
                val expenses = db.expenseDao().getAllExpensesList()

                val billSpent = billPayments.filter { it.monthYear == currentMonthYear }.sumOf { it.amount }
                val subSpent = subPayments.filter { 
                    try {
                        sdfMonthYear.format(Date(it.paymentDate)) == currentMonthYear
                    } catch (e: Exception) {
                        false
                    }
                }.sumOf { it.amount }
                val expenseSpent = expenses.filter { it.monthYear == currentMonthYear }.sumOf { it.amount }
                val totalSpent = billSpent + subSpent + expenseSpent
                val formattedSpent = "₹${String.format(Locale.getDefault(), "%,.2f", totalSpent)}"

                // 2. Calculate Bills Due in Next 3 to 7 Days (including upcoming and overdue)
                val allBills = db.billDao().getAllBillsList()
                val dueBills = allBills.filter { bill ->
                    bill.isActive &&
                    bill.isDueInMonthYear(currentMonthYear) &&
                    !bill.isPaidForMonthYear(currentMonthYear) &&
                    !bill.isSkippedForMonthYear(currentMonthYear) &&
                    bill.daysRemainingForMonthYear(currentMonthYear) <= 7
                }.sortedBy { it.daysRemainingForMonthYear(currentMonthYear) }

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_glance_bills)

                    // Month label
                    views.setTextViewText(R.id.tv_widget_current_month, currentMonthName)

                    // Spent so far
                    views.setTextViewText(R.id.tv_widget_spent_amount, formattedSpent)
                    views.setTextViewText(R.id.tv_widget_spent_subtext, "Bills • Subs • Expenses")

                    // Bills due in 3 to 7 days
                    if (dueBills.isNotEmpty()) {
                        val topBill = dueBills.first()
                        val days = topBill.daysRemainingForMonthYear(currentMonthYear)
                        val statusText = when {
                            days < 0 -> "Overdue by ${-days} day(s)!"
                            days == 0 -> "Due today!"
                            days == 1 -> "Due tomorrow"
                            else -> "Due in $days days"
                        }

                        views.setTextViewText(R.id.tv_widget_due_badge, "${dueBills.size} DUE")
                        views.setInt(R.id.tv_widget_due_badge, "setBackgroundResource", R.drawable.widget_badge_orange)
                        views.setTextColor(R.id.tv_widget_due_badge, 0xFFFFB870.toInt())

                        views.setTextViewText(R.id.tv_widget_bill_title, topBill.name)
                        views.setTextViewText(
                            R.id.tv_widget_bill_amount,
                            "₹${String.format(Locale.getDefault(), "%,.2f", topBill.amount)}"
                        )
                        views.setTextViewText(R.id.tv_widget_bill_status, statusText)

                        if (dueBills.size > 1) {
                            views.setViewVisibility(R.id.tv_widget_more_bills, View.VISIBLE)
                            views.setTextViewText(
                                R.id.tv_widget_more_bills,
                                "+${dueBills.size - 1} more"
                            )
                        } else {
                            views.setViewVisibility(R.id.tv_widget_more_bills, View.GONE)
                        }
                    } else {
                        // All clear
                        views.setTextViewText(R.id.tv_widget_due_badge, "ALL CLEAR")
                        views.setInt(R.id.tv_widget_due_badge, "setBackgroundResource", R.drawable.widget_badge_green)
                        views.setTextColor(R.id.tv_widget_due_badge, 0xFF6DDB8A.toInt())

                        views.setTextViewText(R.id.tv_widget_bill_title, "No bills due soon")
                        views.setTextViewText(R.id.tv_widget_bill_amount, "")
                        views.setTextViewText(
                            R.id.tv_widget_bill_status,
                            "Relax! No bills due in the next 7 days 🎉"
                        )
                        views.setViewVisibility(R.id.tv_widget_more_bills, View.GONE)
                    }

                    // Tapping launches MainActivity
                    val launchIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val pendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId,
                        launchIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_root_container, pendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_WIDGET = "com.example.action.UPDATE_GLANCE_WIDGET"

        fun triggerUpdate(context: Context) {
            val intent = Intent(context, GlanceBillWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }
}
