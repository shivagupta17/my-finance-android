package com.example.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Expense
import com.example.data.repository.TrackerRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
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
class ExpenseRepositoryTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repository: TrackerRepository

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        AppDatabase.setTestDatabase(db)
        repository = TrackerRepository(
            billDao = db.billDao(),
            subscriptionDao = db.subscriptionDao(),
            subscriptionPaymentDao = db.subscriptionPaymentDao(),
            billPaymentDao = db.billPaymentDao(),
            expenseDao = db.expenseDao()
        )
    }

    @After
    fun tearDown() {
        AppDatabase.setTestDatabase(null)
        db.close()
    }

    @Test
    fun testExpenseInsertUpdateDeleteAndMonthQuery() = runBlocking {
        val now = System.currentTimeMillis()
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val monthYear = sdf.format(Date(now))

        val expense = Expense(
            id = 1,
            title = "Weekend Shopping",
            amount = 2500.0,
            date = now,
            monthYear = monthYear,
            category = "Shopping",
            notes = "Clothes and shoes"
        )

        // Insert
        repository.insertExpense(expense)

        val allExpenses = repository.allExpenses.first()
        assertEquals(1, allExpenses.size)
        assertEquals("Weekend Shopping", allExpenses[0].title)
        assertEquals(2500.0, allExpenses[0].amount, 0.01)

        // Query by month
        val monthExpenses = repository.getExpensesByMonth(monthYear).first()
        assertEquals(1, monthExpenses.size)
        assertEquals(1, monthExpenses[0].id)

        // Update
        val updatedExpense = expense.copy(amount = 2800.0, notes = "Updated notes")
        repository.updateExpense(updatedExpense)

        val updatedList = repository.allExpenses.first()
        assertEquals(2800.0, updatedList[0].amount, 0.01)
        assertEquals("Updated notes", updatedList[0].notes)

        // Delete
        repository.deleteExpense(updatedList[0])
        val emptyList = repository.allExpenses.first()
        assertTrue(emptyList.isEmpty())
    }
}
