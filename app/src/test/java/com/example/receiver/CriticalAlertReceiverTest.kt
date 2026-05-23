package com.example.receiver

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.db.AppDatabase
import com.example.data.model.Bill
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowNotificationManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class CriticalAlertReceiverTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var notificationManager: NotificationManager
    private lateinit var shadowNotificationManager: ShadowNotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Build in-memory database to prevent disk locks and allow main thread queries in tests
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        AppDatabase.setTestDatabase(db)
        
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        shadowNotificationManager = shadowOf(notificationManager)
    }

    @After
    fun tearDown() {
        AppDatabase.setTestDatabase(null)
        db.close()
    }

    @Test
    fun testCriticalNotificationTriggersForOverdueBill() = runBlocking {
        val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        val calendar = Calendar.getInstance()
        val currentDay = calendar.get(Calendar.DAY_OF_MONTH)

        val bill = Bill(
            id = 101,
            name = "Test Phone Bill",
            amount = 750.0,
            category = "Internet",
            dueDay = currentDay,
            paidMonths = "", // unpaid
            billingCycle = "Monthly",
            startMonthYear = currentMonthYear,
            isVariable = false
        )
        db.billDao().insertBill(bill)

        val insertedBills = db.billDao().getAllBillsList()
        println("TRACKER_TEST: Inserted bills count: ${insertedBills.size}")
        insertedBills.forEach { b ->
            println("TRACKER_TEST: Bill Name=${b.name}, DueDay=${b.dueDay}, StartMonthYear=${b.startMonthYear}")
            println("TRACKER_TEST: isDue=${b.isDueInMonthYear(currentMonthYear)}, isPaid=${b.isPaidForMonthYear(currentMonthYear)}, isDateReached=${currentDay >= b.dueDay}")
        }

        // Directly call checkAndNotify synchronously
        val receiver = CriticalAlertReceiver()
        receiver.checkAndNotify(context, db)

        // Validate posted notifications
        val notifications = shadowNotificationManager.allNotifications
        assertTrue("A critical bill notification should have been shown", notifications.isNotEmpty())
        
        val notification = shadowNotificationManager.getNotification(10101)
        assertNotNull("Notification for bill ID 101 should exist", notification)
        
        val titleText = notification.extras?.getCharSequence(android.app.Notification.EXTRA_TITLE)?.toString()
        val textContent = notification.extras?.getCharSequence(android.app.Notification.EXTRA_TEXT)?.toString()
        println("TRACKER_TEST: Notification title=$titleText, text=$textContent")
        
        assertTrue("Notification title should contain the bill name", titleText?.contains("Test Phone Bill") == true)
        assertTrue("Notification message should contain the amount", textContent?.contains("750.00") == true)
    }

    @Test
    fun testSnoozeSchedulesAdditionalCheckDirectly() {
        // Test high-level alarm scheduling helper completely synchronously
        val snoozeHours = 3
        CriticalAlertReceiver.scheduleNextCheck(context, snoozeHours)

        // Verify alarm was scheduled (handled by AlarmManager shadow)
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as android.app.AlarmManager
        val shadowAlarmManager = shadowOf(alarmManager)
        val nextAlarm = shadowAlarmManager.nextScheduledAlarm
        assertNotNull("scheduleNextCheck should schedule a future check alarm", nextAlarm)
    }
}
