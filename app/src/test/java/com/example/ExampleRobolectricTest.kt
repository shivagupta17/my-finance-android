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
}

