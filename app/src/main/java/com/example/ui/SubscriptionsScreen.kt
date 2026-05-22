package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Subscription
import com.example.viewmodel.TrackerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionsScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val subscriptions by viewModel.subscriptions.collectAsState()
    val totalSubsAmount by viewModel.monthlySubscriptionsTotal.collectAsState()
    val payments by viewModel.payments.collectAsState()
    
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedSubForEdit by remember { mutableStateOf<Subscription?>(null) }
    
    var activeSubTab by remember { mutableStateOf("My Subscriptions") } // "My Subscriptions", "Payment History"
    
    // Status Filter
    var statusFilter by remember { mutableStateOf("Active") } // "Active", "Inactive", "All"

    val filteredSubs = remember(subscriptions, statusFilter) {
        when (statusFilter) {
            "Active" -> subscriptions.filter { it.isActive }
            "Inactive" -> subscriptions.filter { !it.isActive }
            else -> subscriptions
        }
    }

    val selectedMonthYear by viewModel.selectedMonthYear.collectAsState()

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Subscription Hub",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Review recurring plans, sources, and platforms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // Overview budget card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Monthly Sub Budget",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "₹${String.format("%,.2f", totalSubsAmount)}/mo",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = "Based on ${subscriptions.filter { it.isActive }.size} active plans",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                        )
                    }

                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.1f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            // Main Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("My Subscriptions", "Payment History").forEach { tab ->
                    val isSelected = activeSubTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { activeSubTab = tab }
                            .padding(vertical = 10.dp)
                            .testTag("tab_sub_main_$tab"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = tab,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            if (activeSubTab == "My Subscriptions") {
                // Filtering selector
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    listOf("Active", "Inactive", "All").forEach { filter ->
                        val isSelected = statusFilter == filter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.secondary
                                    else Color.Transparent
                                )
                                .clickable { statusFilter = filter }
                                .padding(vertical = 10.dp)
                                .testTag("filter_subs_$filter"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onSecondary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Subscription List
                if (filteredSubs.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CardMembership,
                                contentDescription = "Empty Subs",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "No subscription plans",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "Tap the '+' button below to start tracking your automated subscriptions like Netflix, PlayStation etc.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(items = filteredSubs, key = { it.id }) { sub ->
                            SubscriptionRowItem(
                                subscription = sub,
                                onToggleActive = { viewModel.toggleSubscriptionActive(sub) },
                                onRenewClick = { viewModel.renewSubscription(sub, selectedMonthYear) },
                                onEditClick = {
                                    selectedSubForEdit = sub
                                    showAddEditDialog = true
                                },
                                onDeleteClick = { viewModel.deleteSubscription(sub) }
                            )
                        }
                    }
                }
            } else {
                // Payment History Section
                if (payments.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ReceiptLong,
                                contentDescription = "Empty Payments",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "No subscription payments logged",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "When you click 'Mark Paid' on any card in the Subscriptions or Dashboard screeen, the transaction is automatically recorded here.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 32.dp)
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        items(items = payments, key = { it.id }) { payment ->
                            SubscriptionPaymentRowItem(
                                payment = payment,
                                onDeleteClick = { viewModel.deletePayment(payment) }
                            )
                        }
                    }
                }
            }
        }

        // FAB
        FloatingActionButton(
            onClick = {
                selectedSubForEdit = null
                showAddEditDialog = true
            },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .testTag("add_subscription_fab"),
            containerColor = MaterialTheme.colorScheme.secondary,
            contentColor = MaterialTheme.colorScheme.onSecondary
        ) {
            Icon(imageVector = Icons.Default.Add, contentDescription = "Add Subscription")
        }

        // Add/Edit Dialog sheet
        if (showAddEditDialog) {
            AddEditSubscriptionDialog(
                subscription = selectedSubForEdit,
                onDismiss = { showAddEditDialog = false },
                onSave = { name, amount, cycle, source, platform, category, date, autoNotify, reminderDays, notes ->
                    if (selectedSubForEdit == null) {
                        viewModel.addSubscription(
                            name, amount, cycle, source, platform, category, date, autoNotify, reminderDays, notes
                        )
                    } else {
                        val updated = selectedSubForEdit!!.copy(
                            name = name,
                            amount = amount,
                            billingCycle = cycle,
                            paymentSource = source,
                            platform = platform,
                            category = category,
                            renewalDate = date,
                            isAutoNotify = autoNotify,
                            customReminderDaysBefore = reminderDays,
                            notes = notes
                        )
                        viewModel.updateSubscription(updated)
                    }
                    showAddEditDialog = false
                }
            )
        }
    }
}

@Composable
fun SubscriptionRowItem(
    subscription: Subscription,
    onToggleActive: () -> Unit,
    onRenewClick: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val formattedRenewal = sdf.format(Date(subscription.renewalDate))
    val isSoon = subscription.isRenewingSoon()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sub_card_${subscription.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (!subscription.isActive) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else if (isSoon) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.15f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isSoon && subscription.isActive) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Upper details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Platform Indicator Design
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (subscription.platform) {
                            "Apple Subscription" -> Icons.Default.PhoneIphone
                            "Google Subscription" -> Icons.Default.PlayArrow
                            else -> Icons.Default.Autorenew
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = subscription.name,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        // Tag cycle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = subscription.billingCycle,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                    }

                    Text(
                        text = "Source: ${subscription.paymentSource}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Cost display
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "₹${String.format("%.2f", subscription.amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    
                    // Switch state
                    Switch(
                        checked = subscription.isActive,
                        onCheckedChange = { onToggleActive() },
                        modifier = Modifier
                            .scale(0.7f)
                            .testTag("toggle_sub_state_${subscription.id}")
                    )
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            // Bottom stats & controls row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Renewal tracker
                Column {
                    Text(
                        text = "Next Due Date",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isSoon) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.secondary
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formattedRenewal,
                            style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSoon) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }

                // Control Buttons
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (subscription.isActive) {
                        Button(
                            onClick = onRenewClick,
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("renew_sub_btn_${subscription.id}"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.secondary
                            ),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp)
                        ) {
                            Text(
                                "Mark Paid",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }

                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_sub_${subscription.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Subscription",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.secondary
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_sub_${subscription.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Subscription",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditSubscriptionDialog(
    subscription: Subscription?,
    onDismiss: () -> Unit,
    onSave: (
        name: String, amount: Double, cycle: String, source: String, platform: String,
        category: String, nextDate: Long, autoNotify: Boolean, reminderDays: Int, notes: String
    ) -> Unit
) {
    val context = LocalContext.current
    val calendar = Calendar.getInstance()

    // Setup initial Date State
    var selectedDateMillis by remember {
        mutableStateOf(subscription?.renewalDate ?: calendar.timeInMillis)
    }

    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val displayedDateStr = remember(selectedDateMillis) {
        sdf.format(Date(selectedDateMillis))
    }

    var name by remember { mutableStateOf(subscription?.name ?: "") }
    var amountStr by remember { mutableStateOf(subscription?.amount?.toString() ?: "") }
    var cycle by remember { mutableStateOf(subscription?.billingCycle ?: "Monthly") }
    var source by remember { mutableStateOf(subscription?.paymentSource ?: "ICICI Credit Card") }
    var platform by remember { mutableStateOf(subscription?.platform ?: "Direct") }
    var category by remember { mutableStateOf(subscription?.category ?: "Entertainment") }
    var autoNotify by remember { mutableStateOf(subscription?.isAutoNotify ?: true) }
    var customReminderDaysStr by remember { mutableStateOf(subscription?.customReminderDaysBefore?.toString() ?: "2") }
    var notes by remember { mutableStateOf(subscription?.notes ?: "") }

    var cycleExpanded by remember { mutableStateOf(false) }
    var platformExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }

    // Dropdown choices
    val cycles = listOf("Monthly", "Quarterly", "Yearly")
    val platforms = listOf("Direct", "Apple Subscription", "Google Subscription", "Amazon Prime Channels", "Roku", "Other")
    val categories = listOf("Entertainment", "Gaming", "Music", "Productivity", "Utility", "Fitness", "Other")

    // Input validations
    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }
    var sourceError by remember { mutableStateOf(false) }

    // Dynamic Datepicker helper used on demand

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (subscription == null) "Add Automated Plan" else "Edit Subscription",
                fontWeight = FontWeight.Bold
            )
        },
        confirmButton = {
            Button(
                onClick = {
                    val isValidName = name.isNotBlank()
                    val amountVal = amountStr.toDoubleOrNull()
                    val isValidAmount = amountVal != null && amountVal > 0
                    val isValidSource = source.isNotBlank()

                    nameError = !isValidName
                    amountError = !isValidAmount
                    sourceError = !isValidSource

                    if (isValidName && isValidAmount && isValidSource) {
                        onSave(
                            name,
                            amountVal!!,
                            cycle,
                            source,
                            platform,
                            category,
                            selectedDateMillis,
                            autoNotify,
                            customReminderDaysStr.toIntOrNull() ?: 2,
                            notes
                        )
                    }
                },
                modifier = Modifier.testTag("dialog_confirm")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dialog_dismiss")
            ) {
                Text("Cancel")
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_edit_sub_dialog"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = false
                    },
                    label = { Text("Service Name (e.g., Netflix)") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_sub_name"),
                    supportingText = {
                        if (nameError) {
                            Text("Service name is required", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                // Cost and Cycle row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            amountStr = it
                            amountError = false
                        },
                        label = { Text("Cost (₹)") },
                        isError = amountError,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier
                            .weight(1.1f)
                            .testTag("input_sub_amount"),
                        supportingText = {
                            if (amountError) {
                                Text("Cost > 0 required", color = MaterialTheme.colorScheme.error)
                            }
                        }
                    )

                    ExposedDropdownMenuBox(
                        expanded = cycleExpanded,
                        onExpandedChange = { cycleExpanded = !cycleExpanded },
                        modifier = Modifier.weight(0.9f)
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = cycle,
                            onValueChange = {},
                            label = { Text("Cycle") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cycleExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = cycleExpanded,
                            onDismissRequest = { cycleExpanded = false }
                        ) {
                            cycles.forEach { selection ->
                                DropdownMenuItem(
                                    text = { Text(selection) },
                                    onClick = {
                                        cycle = selection
                                        cycleExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Payment Source & Platform row
                OutlinedTextField(
                    value = source,
                    onValueChange = {
                        source = it
                        sourceError = false
                    },
                    label = { Text("Payment Source (e.g. ICICI Credit Card)") },
                    isError = sourceError,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_sub_source"),
                    supportingText = {
                        if (sourceError) {
                            Text("Payment Source cannot be empty", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    ExposedDropdownMenuBox(
                        expanded = platformExpanded,
                        onExpandedChange = { platformExpanded = !platformExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = platform,
                            onValueChange = {},
                            label = { Text("Via Platform") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = platformExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = platformExpanded,
                            onDismissRequest = { platformExpanded = false }
                        ) {
                            platforms.forEach { selection ->
                                DropdownMenuItem(
                                    text = { Text(selection) },
                                    onClick = {
                                        platform = selection
                                        platformExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    ExposedDropdownMenuBox(
                        expanded = categoryExpanded,
                        onExpandedChange = { categoryExpanded = !categoryExpanded },
                        modifier = Modifier.weight(1f)
                    ) {
                        OutlinedTextField(
                            readOnly = true,
                            value = category,
                            onValueChange = {},
                            label = { Text("Category") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                            colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .menuAnchor()
                        )
                        ExposedDropdownMenu(
                            expanded = categoryExpanded,
                            onDismissRequest = { categoryExpanded = false }
                        ) {
                            categories.forEach { selection ->
                                DropdownMenuItem(
                                    text = { Text(selection) },
                                    onClick = {
                                        category = selection
                                        categoryExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // renewal Date Picker trigger row
                OutlinedTextField(
                    readOnly = true,
                    value = displayedDateStr,
                    onValueChange = {},
                    label = { Text("Next Due Date") },
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.CalendarToday,
                            contentDescription = "Pick Date",
                            modifier = Modifier.clickable {
                                showDatePicker(context, selectedDateMillis) { newMillis ->
                                    selectedDateMillis = newMillis
                                }
                            }
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            showDatePicker(context, selectedDateMillis) { newMillis ->
                                selectedDateMillis = newMillis
                            }
                        }
                        .testTag("input_sub_date")
                )

                // Custom Notification row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = autoNotify,
                            onCheckedChange = { autoNotify = it },
                            modifier = Modifier.testTag("checkbox_sub_notify")
                        )
                        Text(
                            text = "Notify before due date?",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }

                    if (autoNotify) {
                        OutlinedTextField(
                            value = customReminderDaysStr,
                            onValueChange = { customReminderDaysStr = it },
                            label = { Text("Days before") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier
                                .width(90.dp)
                                .testTag("input_sub_reminder_days")
                        )
                    }
                }

                // Notes Description
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    maxLines = 2,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_sub_notes")
                )
            }
        }
    )
}

@Composable
fun SubscriptionPaymentRowItem(
    payment: com.example.data.model.SubscriptionPayment,
    onDeleteClick: () -> Unit
) {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val formattedDate = sdf.format(Date(payment.paymentDate))
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("payment_history_item_${payment.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (payment.platform) {
                        "Apple Subscription" -> Icons.Default.PhoneAndroid
                        "Google Subscription" -> Icons.Default.PlayArrow
                        else -> Icons.Default.CreditCard
                    },
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(20.dp)
                )
            }
            
            // Details
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = payment.subscriptionName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        text = "₹${String.format("%.2f", payment.amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${payment.billingCycle} • ${payment.paymentSource}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = formattedDate,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                )
            }
            
            // Delete button
            IconButton(
                onClick = onDeleteClick,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete payment log",
                    tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

private fun showDatePicker(
    context: android.content.Context,
    currentMillis: Long,
    onDateSelected: (Long) -> Unit
) {
    val initialCal = Calendar.getInstance().apply { timeInMillis = currentMillis }
    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onDateSelected(selectedCal.timeInMillis)
        },
        initialCal.get(Calendar.YEAR),
        initialCal.get(Calendar.MONTH),
        initialCal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
