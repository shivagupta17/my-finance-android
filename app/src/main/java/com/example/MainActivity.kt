package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.graphics.Color
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.BillsScreen
import com.example.ui.HomeScreen
import com.example.ui.DashboardScreen
import com.example.ui.SubscriptionsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TrackerViewModel

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      MyApplicationTheme {
        MainAppContainer()
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer() {
  val context = LocalContext.current
  val viewModel: TrackerViewModel = viewModel()
  
  // Permission Handling for Android 13+ system notifications
  val permissionLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.RequestPermission()
  ) { isGranted ->
    // Permission handled: True if user allowed, False if denied.
  }
  
  if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
    LaunchedEffect(Unit) {
      val isGranted = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.POST_NOTIFICATIONS
      ) == PackageManager.PERMISSION_GRANTED
      
      if (!isGranted) {
        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
      }
    }
  }

  // Navigation Screen States
  var currentTab by remember { mutableStateOf("Home") }

  Scaffold(
    modifier = Modifier.fillMaxSize(),
    topBar = {
      CenterAlignedTopAppBar(
        title = {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
              text = when (currentTab) {
                "Home" -> "Home Tracker"
                "Dashboard" -> "Spend Analytics"
                "Bills" -> "Bills Drawer"
                else -> "Subscriptions"
              },
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = MaterialTheme.colorScheme.onBackground
            )
            Text(
              text = when (currentTab) {
                "Home" -> "Financial Checklist & Actions"
                "Dashboard" -> "Historical & Predictive Insights"
                "Bills" -> "Track & Pay Priority Bills"
                else -> "Manage Active Subscriptions"
              },
              style = MaterialTheme.typography.labelSmall,
              color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
            )
          }
        },
        navigationIcon = {
          Box(
            modifier = Modifier
              .padding(start = 12.dp)
              .size(36.dp)
              .background(color = Color(0xFFD0BCFF), shape = CircleShape),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = "SG",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = Color(0xFF21005D)
            )
          }
        },
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
          containerColor = MaterialTheme.colorScheme.background
        )
      )
    },
    bottomBar = {
      NavigationBar(
        modifier = Modifier.testTag("bottom_nav_bar")
      ) {
        NavigationBarItem(
          selected = currentTab == "Home",
          onClick = { currentTab = "Home" },
          label = { Text("Home") },
          icon = {
            Icon(
              imageVector = Icons.Default.Home,
              contentDescription = "Home Screen Trigger"
            )
          },
          modifier = Modifier.testTag("tab_home")
        )

        NavigationBarItem(
          selected = currentTab == "Dashboard",
          onClick = { currentTab = "Dashboard" },
          label = { Text("Dashboard") },
          icon = {
            Icon(
              imageVector = Icons.Default.Dashboard,
              contentDescription = "Dashboard Screen Trigger"
            )
          },
          modifier = Modifier.testTag("tab_dashboard")
        )

        NavigationBarItem(
          selected = currentTab == "Bills",
          onClick = { currentTab = "Bills" },
          label = { Text("Bills") },
          icon = {
            Icon(
              imageVector = Icons.Default.ReceiptLong,
              contentDescription = "Bills Screen Trigger"
            )
          },
          modifier = Modifier.testTag("tab_bills")
        )

        NavigationBarItem(
          selected = currentTab == "Subscriptions",
          onClick = { currentTab = "Subscriptions" },
          label = { Text("Subscriptions") },
          icon = {
            Icon(
              imageVector = Icons.Default.Autorenew,
              contentDescription = "Subscriptions Screen Trigger"
            )
          },
          modifier = Modifier.testTag("tab_subscriptions")
        )
      }
    }
  ) { innerPadding ->
    val screenModifier = Modifier.padding(innerPadding)
    
    when (currentTab) {
      "Home" -> HomeScreen(
        viewModel = viewModel,
        onNavigateToBills = { currentTab = "Bills" },
        onNavigateToSubs = { currentTab = "Subscriptions" },
        modifier = screenModifier
      )
      "Dashboard" -> DashboardScreen(
        viewModel = viewModel,
        modifier = screenModifier
      )
      "Bills" -> BillsScreen(
        viewModel = viewModel,
        modifier = screenModifier
      )
      "Subscriptions" -> SubscriptionsScreen(
        viewModel = viewModel,
        modifier = screenModifier
      )
    }
  }
}
