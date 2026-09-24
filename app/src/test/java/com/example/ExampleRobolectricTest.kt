package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import com.example.viewmodel.TrackerViewModel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Bill & Subscription Tracker", appName)
  }

  @Test
  fun `launch main activity`() {
    val controller = Robolectric.buildActivity(MainActivity::class.java).setup()
    val activity = controller.get()
    assertNotNull(activity)
  }

  @Test
  fun `clear all data purges database`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = TrackerViewModel(application)
    
    viewModel.addBill(
      name = "Gym",
      amount = 50.0,
      category = "Fitness",
      dueDay = 15,
      reminderDays = 1,
      notes = ""
    )
    
    delay(500)
    
    viewModel.clearAllData()
    delay(500)
    
    assertTrue(viewModel.bills.value.isEmpty())
  }

  @Test
  fun `test billing computations on different periods`() = runBlocking {
    val application = ApplicationProvider.getApplicationContext<android.app.Application>()
    val viewModel = TrackerViewModel(application)
    
    viewModel.addBill(
      name = "Old Monthly Bill",
      amount = 100.0,
      category = "Utilities",
      dueDay = 5,
      reminderDays = 2,
      notes = "",
      billingCycle = "Monthly",
      startMonthYear = "2026-01"
    )

    viewModel.addBill(
      name = "One Time Bill",
      amount = 500.0,
      category = "Rent",
      dueDay = 10,
      reminderDays = 1,
      notes = "",
      billingCycle = "One-time",
      startMonthYear = "2026-05"
    )

    delay(1000)

    val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    viewModel.selectMonthYear(currentMonthYear)
    delay(500)
    
    assertNotNull(viewModel.bills.value)
    assertNotNull(viewModel.monthlyBillsTotal.value)
    assertNotNull(viewModel.outstandingBillsTotal.value)
  }
}

