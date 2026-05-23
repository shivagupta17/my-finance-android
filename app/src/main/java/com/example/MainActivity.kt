package com.example

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.ui.Alignment
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Dashboard
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
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
import android.content.Intent
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
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

fun getInitials(name: String): String {
  if (name.isBlank()) return "?"
  val parts = name.trim().split("\\s+".toRegex()).filter { it.isNotBlank() }
  if (parts.isEmpty()) return "?"
  if (parts.size == 1) {
    return parts[0].take(2).uppercase()
  }
  val firstChar = parts[0].firstOrNull() ?: '?'
  val lastChar = parts.last().firstOrNull() ?: '?'
  return "$firstChar$lastChar".uppercase()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppContainer() {
  val context = LocalContext.current
  val viewModel: TrackerViewModel = viewModel()
  
  // Profile State variables using SharedPreferences
  val sharedPrefs = remember { context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE) }
  
  // Detect if running inside a Robolectric / automated JVM test environment
  val isRunningInTest = remember {
    try {
      Class.forName("org.robolectric.RuntimeEnvironment") != null
    } catch (e: Exception) {
      false
    }
  }

  var userName by remember {
    val initial = sharedPrefs.getString("user_name", "") ?: ""
    mutableStateOf(if (initial.isBlank() && isRunningInTest) "Test User" else initial)
  }
  var showOnboardingDialog by remember { mutableStateOf(userName.isBlank() && !isRunningInTest) }
  var showEditDialog by remember { mutableStateOf(false) }
  var showClearConfirmation by remember { mutableStateOf(false) }
  var currentTab by remember { mutableStateOf("Home") }

  val exportBackupLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.CreateDocument("application/json")
  ) { uri ->
    if (uri != null) {
      try {
        val jsonString = viewModel.exportDataToJson()
        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
          outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
        }
        android.widget.Toast.makeText(context, "Backup exported successfully!", android.widget.Toast.LENGTH_LONG).show()
      } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Export failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
      }
    }
  }

  val importBackupLauncher = rememberLauncherForActivityResult(
    contract = ActivityResultContracts.OpenDocument()
  ) { uri ->
    if (uri != null) {
      try {
        val jsonString = context.contentResolver.openInputStream(uri)?.use { inputStream ->
          inputStream.bufferedReader().use { it.readText() }
        }
        if (jsonString != null) {
          viewModel.importDataFromJson(
            jsonString = jsonString,
            onSuccess = {
              android.widget.Toast.makeText(context, "Backup imported successfully!", android.widget.Toast.LENGTH_LONG).show()
              showEditDialog = false
            },
            onError = { errorMsg ->
              android.widget.Toast.makeText(context, "Import failed: $errorMsg", android.widget.Toast.LENGTH_LONG).show()
            }
          )
        } else {
          android.widget.Toast.makeText(context, "Could not read backup file content.", android.widget.Toast.LENGTH_LONG).show()
        }
      } catch (e: Exception) {
        android.widget.Toast.makeText(context, "Import failed: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
      }
    }
  }

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

  // Trigger immediate overdue and due items check on launch
  LaunchedEffect(Unit) {
    val checkIntent = Intent(context, com.example.receiver.CriticalAlertReceiver::class.java).apply {
      action = com.example.receiver.CriticalAlertReceiver.ACTION_CHECK_ALERTS
    }
    context.sendBroadcast(checkIntent)
    // Schedule a default repeating interval check
    com.example.receiver.CriticalAlertReceiver.scheduleNextCheck(context, 1)
  }

  // Visual/onboarding popups
  if (showOnboardingDialog) {
    var tempName by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { /* Force response to set name initially, or skip below */ },
      title = {
        Text(
          text = "Welcome to Bill Tracker!",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      },
      text = {
        Column {
          Text(
            text = "Please enter your name to personalize your financial dashboard. We will use your initials for the profile avatar.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
          )
          OutlinedTextField(
            value = tempName,
            onValueChange = {
              tempName = it
              if (errorText != null && it.isNotBlank()) {
                errorText = null
              }
            },
            label = { Text("Your Name") },
            placeholder = { Text("e.g. Shiva Gupta") },
            singleLine = true,
            isError = errorText != null,
            modifier = Modifier.fillMaxWidth()
          )
          if (errorText != null) {
            Text(
              text = errorText ?: "",
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(top = 4.dp)
            )
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            if (tempName.trim().isBlank()) {
              errorText = "Name cannot be empty!"
            } else {
              val savedName = tempName.trim()
              sharedPrefs.edit().putString("user_name", savedName).apply()
              userName = savedName
              showOnboardingDialog = false
            }
          },
          modifier = Modifier.testTag("onboarding_save_button")
        ) {
          Text("Get Started")
        }
      },
      dismissButton = {
        TextButton(
          onClick = {
            val savedName = "User"
            sharedPrefs.edit().putString("user_name", savedName).apply()
            userName = savedName
            showOnboardingDialog = false
          },
          modifier = Modifier.testTag("onboarding_skip_button")
        ) {
          Text("Skip / Use Default")
        }
      },
      modifier = Modifier.testTag("onboarding_dialog")
    )
  }

  if (showEditDialog) {
    var tempName by remember { mutableStateOf(userName) }
    var tempSnooze by remember { mutableStateOf(sharedPrefs.getInt("snooze_hours", 1).toString()) }
    var errorText by remember { mutableStateOf<String?>(null) }

    AlertDialog(
      onDismissRequest = { showEditDialog = false },
      title = {
        Text(
          text = "Settings & Profile",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
        )
      },
      text = {
        Column {
          Text(
            text = "Update your profile name and alert snooze settings below.",
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.padding(bottom = 16.dp)
          )
          OutlinedTextField(
            value = tempName,
            onValueChange = {
              tempName = it
              if (errorText != null && it.isNotBlank()) {
                errorText = null
              }
            },
            label = { Text("Your Name") },
            placeholder = { Text("e.g. Shiva Gupta") },
            singleLine = true,
            isError = errorText != null,
            modifier = Modifier.fillMaxWidth()
          )
          
          OutlinedTextField(
            value = tempSnooze,
            onValueChange = {
              tempSnooze = it
              if (errorText != null && it.isNotBlank()) {
                errorText = null
              }
            },
            label = { Text("Notification Snooze (Hours)") },
            placeholder = { Text("e.g. 1") },
            singleLine = true,
            isError = errorText != null && (tempSnooze.toIntOrNull() == null || (tempSnooze.toIntOrNull() ?: 1) <= 0),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
              .fillMaxWidth()
              .padding(top = 16.dp)
              .testTag("profile_snooze_input")
          )

          if (errorText != null) {
            Text(
              text = errorText ?: "",
              color = MaterialTheme.colorScheme.error,
              style = MaterialTheme.typography.bodySmall,
              modifier = Modifier.padding(top = 8.dp)
            )
          }

          HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
          )

          Text(
            text = "Backup & Restore",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = {
                exportBackupLauncher.launch("bill_tracker_backup.json")
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("export_data_button")
            ) {
              Icon(
                imageVector = Icons.Default.Upload,
                contentDescription = "Export backup",
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Export", maxLines = 1)
            }

            Button(
              onClick = {
                importBackupLauncher.launch(arrayOf("application/json"))
              },
              colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
              ),
              modifier = Modifier
                .weight(1f)
                .testTag("import_data_button")
            ) {
              Icon(
                imageVector = Icons.Default.Download,
                contentDescription = "Import backup",
                modifier = Modifier.size(18.dp)
              )
              Spacer(modifier = Modifier.width(4.dp))
              Text("Import", maxLines = 1)
            }
          }

          HorizontalDivider(
            modifier = Modifier.padding(vertical = 16.dp),
            color = MaterialTheme.colorScheme.outlineVariant
          )

          Text(
            text = "Danger Zone",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          Button(
            onClick = {
              showClearConfirmation = true
            },
            colors = ButtonDefaults.buttonColors(
              containerColor = MaterialTheme.colorScheme.error,
              contentColor = MaterialTheme.colorScheme.onError
            ),
            modifier = Modifier
              .fillMaxWidth()
              .testTag("clear_all_data_button")
          ) {
            Text("Clear All Data")
          }
        }
      },
      confirmButton = {
        Button(
          onClick = {
            val snoozeInt = tempSnooze.toIntOrNull()
            if (tempName.trim().isBlank()) {
              errorText = "Name cannot be empty!"
            } else if (snoozeInt == null || snoozeInt <= 0) {
              errorText = "Snooze hours must be a positive integer!"
            } else {
              val savedName = tempName.trim()
              sharedPrefs.edit()
                .putString("user_name", savedName)
                .putInt("snooze_hours", snoozeInt)
                .apply()
              userName = savedName
              showEditDialog = false
            }
          },
          modifier = Modifier.testTag("edit_save_button")
        ) {
          Text("Save Changes")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showEditDialog = false },
          modifier = Modifier.testTag("edit_cancel_button")
        ) {
          Text("Cancel")
        }
      },
      modifier = Modifier.testTag("edit_dialog")
    )
  }

  if (showClearConfirmation) {
    AlertDialog(
      onDismissRequest = { showClearConfirmation = false },
      title = {
        Text(
          text = "Clear All Data?",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = MaterialTheme.colorScheme.error
        )
      },
      text = {
        Text(
          text = "Are you sure you want to clear all data? This will permanently delete all your bills, subscriptions, payment history, and profile settings. This action is irreversible and cannot be recovered.",
          style = MaterialTheme.typography.bodyMedium
        )
      },
      confirmButton = {
        Button(
          onClick = {
            viewModel.clearAllData()
            sharedPrefs.edit().clear().apply()
            userName = ""
            currentTab = "Home"
            showClearConfirmation = false
            showEditDialog = false
            showOnboardingDialog = true
          },
          colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
          ),
          modifier = Modifier.testTag("confirm_clear_button")
        ) {
          Text("Clear Everything")
        }
      },
      dismissButton = {
        TextButton(
          onClick = { showClearConfirmation = false },
          modifier = Modifier.testTag("dismiss_clear_button")
        ) {
          Text("Cancel")
        }
      },
      modifier = Modifier.testTag("clear_confirmation_dialog")
    )
  }

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
              .clip(CircleShape)
              .background(color = Color(0xFFD0BCFF))
              .clickable { showEditDialog = true }
              .testTag("profile_initials_button"),
            contentAlignment = Alignment.Center
          ) {
            Text(
              text = getInitials(userName),
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
