package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.data.db.AppDatabase
import com.example.ui.QuickAddExpenseActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class AddExpenseWidgetProvider : AppWidgetProvider() {

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
        if (intent.action == ACTION_UPDATE_EXPENSE_WIDGET || intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val componentName = ComponentName(context, AddExpenseWidgetProvider::class.java)
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
                val allExpenses = db.expenseDao().getAllExpensesList()

                // Calculate today's start and end timestamps
                val cal = Calendar.getInstance().apply {
                    set(Calendar.HOUR_OF_DAY, 0)
                    set(Calendar.MINUTE, 0)
                    set(Calendar.SECOND, 0)
                    set(Calendar.MILLISECOND, 0)
                }
                val startOfToday = cal.timeInMillis

                val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
                val currentMonthShort = SimpleDateFormat("MMM", Locale.getDefault()).format(Date())

                val todayExpenses = allExpenses.filter { it.date >= startOfToday }
                val todayTotal = todayExpenses.sumOf { it.amount }
                val monthExpenses = allExpenses.filter { it.monthYear == currentMonthYear }
                val monthTotal = monthExpenses.sumOf { it.amount }

                val mostRecentExpense = allExpenses.maxByOrNull { it.date }

                val todayStatText = if (todayExpenses.isEmpty()) {
                    "Today: ₹0"
                } else {
                    "Today: ₹${String.format(Locale.getDefault(), "%,.0f", todayTotal)}"
                }

                val monthStatText = "$currentMonthShort: ₹${String.format(Locale.getDefault(), "%,.0f", monthTotal)}"

                val recentText = if (mostRecentExpense != null) {
                    "Last: ${mostRecentExpense.title} • ₹${String.format(Locale.getDefault(), "%,.2f", mostRecentExpense.amount)} (${mostRecentExpense.category})"
                } else {
                    "Tap + to log your daily spending"
                }

                for (widgetId in appWidgetIds) {
                    val views = RemoteViews(context.packageName, R.layout.widget_add_expense)

                    views.setTextViewText(R.id.tv_widget_today_stat, todayStatText)
                    views.setTextViewText(R.id.tv_widget_month_stat, monthStatText)
                    views.setTextViewText(R.id.tv_widget_recent_expense, recentText)

                    // 1. Main "+ Add Expense" Hero Button -> Launch QuickAddExpenseActivity
                    val addIntent = Intent(context, QuickAddExpenseActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    }
                    val addPendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId * 10 + 1,
                        addIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_widget_add_expense, addPendingIntent)

                    // 2. Groceries Shortcut Button -> QuickAddExpenseActivity with Category=Groceries
                    val grocIntent = Intent(context, QuickAddExpenseActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(QuickAddExpenseActivity.EXTRA_CATEGORY, "Groceries")
                    }
                    val grocPendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId * 10 + 2,
                        grocIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_widget_cat_groceries, grocPendingIntent)

                    // 3. Dining Shortcut Button -> QuickAddExpenseActivity with Category="Dining & Food"
                    val diningIntent = Intent(context, QuickAddExpenseActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(QuickAddExpenseActivity.EXTRA_CATEGORY, "Dining & Food")
                    }
                    val diningPendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId * 10 + 3,
                        diningIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_widget_cat_dining, diningPendingIntent)

                    // 4. Shopping Shortcut Button -> QuickAddExpenseActivity with Category=Shopping
                    val shopIntent = Intent(context, QuickAddExpenseActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(QuickAddExpenseActivity.EXTRA_CATEGORY, "Shopping")
                    }
                    val shopPendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId * 10 + 4,
                        shopIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_widget_cat_shopping, shopPendingIntent)

                    // 5. Transport Shortcut Button -> QuickAddExpenseActivity with Category=Transport
                    val transportIntent = Intent(context, QuickAddExpenseActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(QuickAddExpenseActivity.EXTRA_CATEGORY, "Transport")
                    }
                    val transportPendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId * 10 + 5,
                        transportIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.btn_widget_cat_transport, transportPendingIntent)

                    // 6. Entire Background / Header Click -> Open MainActivity directly to Expenses
                    val mainAppIntent = Intent(context, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                        putExtra(MainActivity.EXTRA_TARGET_TAB, "Expenses")
                    }
                    val mainAppPendingIntent = PendingIntent.getActivity(
                        context,
                        widgetId * 10 + 6,
                        mainAppIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
                    )
                    views.setOnClickPendingIntent(R.id.widget_expense_root, mainAppPendingIntent)

                    appWidgetManager.updateAppWidget(widgetId, views)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    companion object {
        const val ACTION_UPDATE_EXPENSE_WIDGET = "com.example.action.UPDATE_EXPENSE_WIDGET"

        fun triggerUpdate(context: Context) {
            val intent = Intent(context, AddExpenseWidgetProvider::class.java).apply {
                action = ACTION_UPDATE_EXPENSE_WIDGET
            }
            context.sendBroadcast(intent)
        }
    }
}
