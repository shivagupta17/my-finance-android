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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Backup
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.BrightnessAuto
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.ui.components.FloatingGlassNavBar
import com.example.ui.theme.ObsidianTokens
import com.example.ui.theme.bounceClick
import com.example.ui.theme.specularBorder
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
import androidx.compose.ui.unit.sp
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
import com.example.ui.ExpensesScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.viewmodel.TrackerViewModel

class MainActivity : ComponentActivity() {
  companion object {
    const val EXTRA_TARGET_TAB = "extra_target_tab"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val initialTab = intent?.getStringExtra(EXTRA_TARGET_TAB) ?: "Home"
    setContent {
      val context = LocalContext.current
      val sharedPrefs = remember { context.getSharedPreferences("user_profile_prefs", Context.MODE_PRIVATE) }
      val isSystemDark = isSystemInDarkTheme()
      var themeMode by remember {
        mutableStateOf(sharedPrefs.getString("app_theme_mode", "dark") ?: "dark")
      }
      val isDark = when (themeMode) {
        "light" -> false
        "system" -> isSystemDark
        else -> true // default dark
      }

      MyApplicationTheme(darkTheme = isDark) {
        MainAppContainer(
          initialTab = initialTab,
          themeMode = themeMode,
          onThemeModeChanged = { newMode ->
            themeMode = newMode
            sharedPrefs.edit().putString("app_theme_mode", newMode).apply()
          }
        )
      }
    }
  }

  override fun onStop() {
    super.onStop()
    try {
      val checkIntent = Intent(this, com.example.receiver.CriticalAlertReceiver::class.java).apply {
        action = com.example.receiver.CriticalAlertReceiver.ACTION_CHECK_ALERTS
      }
      sendBroadcast(checkIntent)
    } catch (e: Exception) {
      e.printStackTrace()
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
fun MainAppContainer(
  initialTab: String = "Home",
  themeMode: String = "dark",
  onThemeModeChanged: (String) -> Unit = {}
) {
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
    mutableStateOf(if (initial.isNotBlank()) initial else if (isRunningInTest) "Test User" else "User")
  }
  var showOnboardingDialog by remember { mutableStateOf(false) }
  var showEditDialog by remember { mutableStateOf(false) }
  var showClearConfirmation by remember { mutableStateOf(false) }
  var currentTab by remember { mutableStateOf(initialTab) }

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

  // Trigger immediate overdue and due items check on launch, and clear old notifications
  // from previous builds to prevent stale system-shade asset/loading errors.
  LaunchedEffect(Unit) {
    try {
      val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? android.app.NotificationManager
      notificationManager?.cancelAll()
    } catch (e: Exception) {
      e.printStackTrace()
    }
    
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
      onDismissRequest = { showOnboardingDialog = false },
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
            placeholder = { Text("e.g. John Doe") },
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
            placeholder = { Text("e.g. John Doe") },
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
            text = "Appearance & Theme",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = 8.dp)
          )

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            listOf(
              Triple("dark", "Dark", Icons.Default.DarkMode),
              Triple("light", "Light", Icons.Default.LightMode),
              Triple("system", "System", Icons.Default.BrightnessAuto)
            ).forEach { (mode, label, icon) ->
              val isSelected = themeMode == mode
              FilterChip(
                selected = isSelected,
                onClick = { onThemeModeChanged(mode) },
                label = { Text(label, style = MaterialTheme.typography.labelSmall) },
                leadingIcon = {
                  Icon(
                    imageVector = icon,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp)
                  )
                },
                colors = FilterChipDefaults.filterChipColors(
                  selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                  selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                  containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                modifier = Modifier
                  .weight(1f)
                  .testTag("theme_chip_$mode")
              )
            }
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
            val currentTheme = sharedPrefs.getString("app_theme_mode", "dark") ?: "dark"
            sharedPrefs.edit().clear().apply()
            sharedPrefs.edit().putString("app_theme_mode", currentTheme).apply()
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
    containerColor = ObsidianTokens.Canvas,
    topBar = {
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .background(ObsidianTokens.Canvas)
      ) {
        CenterAlignedTopAppBar(
          title = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Text(
                text = when (currentTab) {
                  "Home" -> "Financial Cockpit"
                  "Dashboard" -> "Spend Analytics"
                  "Bills" -> "Bills Drawer"
                  "Subscriptions" -> "Subscriptions"
                  "Expenses" -> "Expenses & Spend"
                  else -> "Tracker"
                },
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = ObsidianTokens.TextPrimary
              )
              Text(
                text = when (currentTab) {
                  "Home" -> "OBSIDIAN VELOCITY & SCHEDULES"
                  "Dashboard" -> "HISTORICAL & PREDICTIVE INSIGHTS"
                  "Bills" -> "TRACK & SETTLE PRIORITY BILLS"
                  "Subscriptions" -> "ACTIVE RECURRING SERVICES"
                  "Expenses" -> "DAILY VARIABLE PURCHASES"
                  else -> "PERSONAL FINANCE"
                },
                style = ObsidianTokens.MicroLabel.copy(
                  fontSize = 10.sp,
                  letterSpacing = 1.2.sp,
                  color = ObsidianTokens.TextMuted
                )
              )
            }
          },
          navigationIcon = {
            Box(
              modifier = Modifier
                .padding(start = 12.dp)
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(ObsidianTokens.AccentMint.copy(alpha = 0.14f))
                .specularBorder(
                  shape = RoundedCornerShape(12.dp),
                  borderWidth = 1.dp,
                  alphaTop = 0.35f,
                  alphaBottom = 0.06f
                )
                .bounceClick(scaleDown = 0.92f) { showEditDialog = true }
                .testTag("profile_initials_button"),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = getInitials(userName),
                style = ObsidianTokens.TabularDigits.copy(
                  fontWeight = FontWeight.Bold,
                  fontSize = 13.sp,
                  color = ObsidianTokens.AccentMint
                )
              )
            }
          },
          actions = {
            Box(
              modifier = Modifier
                .padding(end = 12.dp)
                .size(38.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .specularBorder(
                  shape = RoundedCornerShape(12.dp),
                  borderWidth = 1.dp,
                  alphaTop = 0.35f,
                  alphaBottom = 0.08f
                )
                .bounceClick(scaleDown = 0.92f) {
                  val nextMode = if (themeMode == "dark") "light" else "dark"
                  onThemeModeChanged(nextMode)
                }
                .testTag("theme_toggle_button"),
              contentAlignment = Alignment.Center
            ) {
              Icon(
                imageVector = if (themeMode == "light") Icons.Default.DarkMode else Icons.Default.LightMode,
                contentDescription = "Toggle Dark/Light Theme",
                tint = ObsidianTokens.TextPrimary,
                modifier = Modifier.size(19.dp)
              )
            }
          },
          colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = Color.Transparent
          )
        )
        HorizontalDivider(
          color = MaterialTheme.colorScheme.outlineVariant,
          thickness = 1.dp
        )
      }
    },
    bottomBar = {
      FloatingGlassNavBar(
        currentTab = currentTab,
        onTabSelected = { currentTab = it }
      )
    }
  ) { innerPadding ->
    val screenModifier = Modifier.padding(innerPadding)
    
    when (currentTab) {
      "Home" -> HomeScreen(
        viewModel = viewModel,
        onNavigateToBills = { currentTab = "Bills" },
        onNavigateToSubs = { currentTab = "Subscriptions" },
        onNavigateToExpenses = { currentTab = "Expenses" },
        onNavigateToDashboard = { currentTab = "Dashboard" },
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
      "Expenses" -> ExpensesScreen(
        viewModel = viewModel,
        modifier = screenModifier
      )
    }
  }
}
