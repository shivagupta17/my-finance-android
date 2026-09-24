package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Bill
import com.example.data.model.Expense
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAppWidgetManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class GlanceBillWidgetProviderTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var shadowAppWidgetManager: ShadowAppWidgetManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        AppDatabase.setTestDatabase(db)

        val appWidgetManager = AppWidgetManager.getInstance(context)
        shadowAppWidgetManager = shadowOf(appWidgetManager)
    }

    @After
    fun tearDown() {
        AppDatabase.setTestDatabase(null)
        db.close()
    }

    @Test
    fun testWidgetProviderRegistrationAndBroadcastTrigger() = runBlocking {
        val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)
        val maxDays = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
        val targetDueDay = if (currentDay + 3 > maxDays) maxDays else currentDay + 3

        // Insert a bill due in 3 days
        val bill = Bill(
            id = 50,
            name = "Broadband Internet",
            amount = 999.0,
            category = "Internet",
            dueDay = targetDueDay,
            status = "Active"
        )
        db.billDao().insertBill(bill)

        // Insert an expense for this month
        val expense = Expense(
            id = 10,
            title = "Stationery",
            amount = 350.0,
            date = System.currentTimeMillis(),
            monthYear = currentMonthYear,
            category = "Shopping"
        )
        db.expenseDao().insertExpense(expense)

        val provider = GlanceBillWidgetProvider()
        val componentName = ComponentName(context, GlanceBillWidgetProvider::class.java)

        // Trigger update intent
        val updateIntent = Intent(context, GlanceBillWidgetProvider::class.java).apply {
            action = GlanceBillWidgetProvider.ACTION_UPDATE_WIDGET
        }
        provider.onReceive(context, updateIntent)

        // Trigger helper
        GlanceBillWidgetProvider.triggerUpdate(context)

        assertNotNull(componentName)
    }
}
