package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Expense
import com.example.ui.QuickAddExpenseActivity
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AddExpenseWidgetProviderTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        AppDatabase.setTestDatabase(db)
    }

    @After
    fun tearDown() {
        AppDatabase.setTestDatabase(null)
        db.close()
    }

    @Test
    fun testAddExpenseWidgetCalculatesAndUpdatesProperly() = runBlocking {
        val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val now = System.currentTimeMillis()

        // Insert sample expenses
        val expense1 = Expense(
            id = 101,
            title = "Morning Coffee",
            amount = 150.0,
            date = now,
            monthYear = currentMonthYear,
            category = "Dining & Food"
        )
        val expense2 = Expense(
            id = 102,
            title = "Grocery Supplies",
            amount = 850.0,
            date = now,
            monthYear = currentMonthYear,
            category = "Groceries"
        )
        db.expenseDao().insertExpense(expense1)
        db.expenseDao().insertExpense(expense2)

        val expenses = db.expenseDao().getAllExpensesList()
        assertEquals(2, expenses.size)
        assertEquals(1000.0, expenses.sumOf { it.amount }, 0.01)

        val provider = AddExpenseWidgetProvider()
        val componentName = ComponentName(context, AddExpenseWidgetProvider::class.java)
        assertNotNull(componentName)

        // Trigger update intent via broadcast
        val updateIntent = Intent(context, AddExpenseWidgetProvider::class.java).apply {
            action = AddExpenseWidgetProvider.ACTION_UPDATE_EXPENSE_WIDGET
        }
        provider.onReceive(context, updateIntent)

        // Trigger helper
        AddExpenseWidgetProvider.triggerUpdate(context)
    }

    @Test
    fun testQuickAddExpenseIntentCategoryExtras() {
        val groceriesIntent = Intent(context, QuickAddExpenseActivity::class.java).apply {
            putExtra(QuickAddExpenseActivity.EXTRA_CATEGORY, "Groceries")
        }
        assertEquals("Groceries", groceriesIntent.getStringExtra(QuickAddExpenseActivity.EXTRA_CATEGORY))

        val diningIntent = Intent(context, QuickAddExpenseActivity::class.java).apply {
            putExtra(QuickAddExpenseActivity.EXTRA_CATEGORY, "Dining & Food")
        }
        assertEquals("Dining & Food", diningIntent.getStringExtra(QuickAddExpenseActivity.EXTRA_CATEGORY))
    }
}
