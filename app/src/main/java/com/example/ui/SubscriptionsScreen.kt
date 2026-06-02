package com.example.ui

import android.app.DatePickerDialog
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import com.example.data.model.SubscriptionPayment
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
    val payments by viewModel.payments.collectAsState()
    
    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedSubForEdit by remember { mutableStateOf<Subscription?>(null) }
    var showAddHistoricalPaymentDialog by remember { mutableStateOf(false) }
    
    var activeSubTab by remember { mutableStateOf("My Subscriptions") } // "My Subscriptions", "Payment History"

    var showDeleteSubConfirmDialog by remember { mutableStateOf(false) }
    var subToDelete by remember { mutableStateOf<Subscription?>(null) }

    var showDeletePaymentConfirmDialog by remember { mutableStateOf(false) }
    var paymentToDelete by remember { mutableStateOf<SubscriptionPayment?>(null) }

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
                        text = "Subscriptions Hub",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Manage recurring plans, sources, and platforms",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
            }

            // Main Tab Switcher
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(4.dp)
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
                // Subscription List
                if (subscriptions.isEmpty()) {
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
                        items(items = subscriptions, key = { it.id }) { sub ->
                            SubscriptionRowItem(
                                subscription = sub,
                                onStatusChange = { newStatus -> viewModel.updateSubscriptionStatus(sub, newStatus) },
                                onEditClick = {
                                    selectedSubForEdit = sub
                                    showAddEditDialog = true
                                },
                                onDeleteClick = {
                                    subToDelete = sub
                                    showDeleteSubConfirmDialog = true
                                }
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
                    val groupedSubPayments = remember(payments) {
                        payments.sortedByDescending { it.paymentDate }
                            .groupBy { payment ->
                                val sdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
                                sdf.format(Date(payment.paymentDate))
                            }
                    }

                    LazyColumn(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(bottom = 80.dp)
                    ) {
                        groupedSubPayments.forEach { (monthName, paymentsInMonth) ->
                            item(key = "header_$monthName") {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 8.dp, horizontal = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = monthName,
                                        style = MaterialTheme.typography.titleSmall.copy(
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.primary
                                        ),
                                        modifier = Modifier.padding(end = 8.dp)
                                    )
                                    HorizontalDivider(
                                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                        thickness = 1.dp,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }

                            items(items = paymentsInMonth, key = { "payment_${it.id}" }) { payment ->
                                SubscriptionPaymentRowItem(
                                    payment = payment,
                                    onDeleteClick = {
                                        paymentToDelete = payment
                                        showDeletePaymentConfirmDialog = true
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        // FAB
        if (activeSubTab == "My Subscriptions") {
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
        } else {
            FloatingActionButton(
                onClick = {
                    showAddHistoricalPaymentDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_historical_subscription_payment_fab"),
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Historical Subscription Payment")
            }
        }

        // Add/Edit Dialog sheet
        if (showAddEditDialog) {
            AddEditSubscriptionDialog(
                subscription = selectedSubForEdit,
                onDismiss = { showAddEditDialog = false },
                onSave = { name, amount, cycle, source, platform, category, date, autoNotify, reminderDays, notes, status ->
                    if (selectedSubForEdit == null) {
                        viewModel.addSubscription(
                            name, amount, cycle, source, platform, category, date, autoNotify, reminderDays, notes, status
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
                            notes = notes,
                            status = status,
                            isActive = (status == "Active")
                        )
                        viewModel.updateSubscription(updated)
                    }
                    showAddEditDialog = false
                }
            )
        }

        if (showAddHistoricalPaymentDialog) {
            AddHistoricalSubscriptionPaymentDialog(
                subscriptions = subscriptions,
                monthOptions = viewModel.getMonthYearOptions(),
                payments = payments,
                onDismiss = { showAddHistoricalPaymentDialog = false },
                onConfirm = { subscription, monthYear, amount, paymentDate, overwrite ->
                    viewModel.addHistoricalSubscriptionPayment(subscription, monthYear, amount, paymentDate, overwrite)
                    showAddHistoricalPaymentDialog = false
                }
            )
        }

        if (showDeleteSubConfirmDialog && subToDelete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteSubConfirmDialog = false
                    subToDelete = null
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Delete Subscription?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete the subscription '${subToDelete?.name ?: ""}'? This will also permanently delete all associated payment history logs for this subscription. This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            subToDelete?.let { viewModel.deleteSubscription(it) }
                            showDeleteSubConfirmDialog = false
                            subToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.testTag("confirm_delete_subscription_button")
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteSubConfirmDialog = false
                            subToDelete = null
                        },
                        modifier = Modifier.testTag("dismiss_delete_subscription_button")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeletePaymentConfirmDialog && paymentToDelete != null) {
            val payment = paymentToDelete!!
            val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
            val formattedDate = remember(payment.paymentDate) { sdf.format(Date(payment.paymentDate)) }
            AlertDialog(
                onDismissRequest = { 
                    showDeletePaymentConfirmDialog = false
                    paymentToDelete = null
                },
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "Warning",
                            tint = MaterialTheme.colorScheme.error
                        )
                        Text(
                            text = "Delete Payment Log?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete the payment entry of ${payment.amount} for month ${payment.monthYear} paid on $formattedDate? This status of the subscription will revert to unpaid/unskipped for that period.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deletePayment(payment)
                            showDeletePaymentConfirmDialog = false
                            paymentToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.testTag("confirm_delete_subscription_payment_button")
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeletePaymentConfirmDialog = false
                            paymentToDelete = null
                        },
                        modifier = Modifier.testTag("dismiss_delete_subscription_payment_button")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun SubscriptionRowItem(
    subscription: Subscription,
    onStatusChange: (String) -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var statusMenuExpanded by remember { mutableStateOf(false) }

    val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
    val formattedRenewal = sdf.format(Date(subscription.renewalDate))
    val status = subscription.status

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("sub_card_${subscription.id}"),
        colors = CardDefaults.cardColors(
            containerColor = when (status) {
                "Paused" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                "Cancelled" -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = when (status) {
                "Paused" -> MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                "Cancelled" -> MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                else -> MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Upper details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Styled platform indicator
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            when (status) {
                                "Paused" -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
                                "Cancelled" -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.03f)
                                else -> MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            }
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (subscription.platform) {
                            "Apple Subscription" -> Icons.Default.PhoneIphone
                            "Google Subscription" -> Icons.Default.PlayArrow
                            else -> Icons.Default.Autorenew
                        },
                        contentDescription = null,
                        tint = when (status) {
                            "Paused" -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            "Cancelled" -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                            else -> MaterialTheme.colorScheme.primary
                        },
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            text = subscription.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (status == "Cancelled") MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f) else MaterialTheme.colorScheme.onSurface
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        // Tag cycle
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = if (status == "Active") 0.8f else 0.3f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = subscription.billingCycle,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = if (status == "Active") 1.0f else 0.6f)
                            )
                        }
                    }

                    Text(
                        text = "Source: ${subscription.paymentSource}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = if (status == "Active") 0.6f else 0.4f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Cost & Status dropdown selector
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = "₹${String.format("%.2f", subscription.amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (status == "Cancelled") MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                        )
                    )

                    // Compact Interactive Status Badge (opens dropdown to transition status)
                    Box {
                        Row(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(
                                    when (status) {
                                        "Active" -> Color(0xFFE8F5E9)
                                        "Paused" -> Color(0xFFFFF3E0)
                                        else -> Color(0xFFECEFF1)
                                    }
                                )
                                .clickable { statusMenuExpanded = true }
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = status,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 9.sp,
                                    color = when (status) {
                                        "Active" -> Color(0xFF2E7D32)
                                        "Paused" -> Color(0xFFE65100)
                                        else -> Color(0xFF455A64)
                                    }
                                )
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = "Change Status",
                                tint = when (status) {
                                    "Active" -> Color(0xFF2E7D32)
                                    "Paused" -> Color(0xFFE65100)
                                    else -> Color(0xFF455A64)
                                },
                                modifier = Modifier.size(12.dp)
                              )
                        }

                        DropdownMenu(
                            expanded = statusMenuExpanded,
                            onDismissRequest = { statusMenuExpanded = false }
                        ) {
                            DropdownMenuItem(
                                text = { Text("Active (Resume)", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    onStatusChange("Active")
                                    statusMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, null, tint = Color(0xFF2E7D32)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Pause Plan", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    onStatusChange("Paused")
                                    statusMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.Pause, null, tint = Color(0xFFE65100)) }
                            )
                            DropdownMenuItem(
                                text = { Text("Cancel Plan", style = MaterialTheme.typography.bodyMedium) },
                                onClick = {
                                    onStatusChange("Cancelled")
                                    statusMenuExpanded = false
                                },
                                leadingIcon = { Icon(Icons.Default.Cancel, null, tint = Color(0xFF455A64)) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )

            // Bottom action row: Renewal Day vs configurations edit/delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Part: Date or Current State Information
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Renewal Date",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Next Renewal: $formattedRenewal",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                // Right Part: Compact configurations edit/delete
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Compact edit shortcut icon
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("edit_sub_${subscription.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Subscription",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    // Compact delete shortcut icon
                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_sub_${subscription.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Subscription",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f)
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
        category: String, nextDate: Long, autoNotify: Boolean, reminderDays: Int, notes: String,
        status: String
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
    var status by remember { mutableStateOf(subscription?.status ?: "Active") }
    var autoNotify by remember { mutableStateOf(subscription?.isAutoNotify ?: true) }
    var customReminderDaysStr by remember { mutableStateOf(subscription?.customReminderDaysBefore?.toString() ?: "2") }
    var notes by remember { mutableStateOf(subscription?.notes ?: "") }

    var cycleExpanded by remember { mutableStateOf(false) }
    var platformExpanded by remember { mutableStateOf(false) }
    var categoryExpanded by remember { mutableStateOf(false) }
    var statusExpanded by remember { mutableStateOf(false) }

    // Dropdown choices
    val cycles = listOf("Monthly", "Quarterly", "Yearly")
    val platforms = listOf("Direct", "Apple Subscription", "Google Subscription", "Amazon Prime Channels", "Roku", "Other")
    val categories = listOf("Entertainment", "Gaming", "Music", "Productivity", "Utility", "Fitness", "Other")
    val statuses = listOf("Active", "Paused", "Cancelled")

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
                            notes,
                            status
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
                    .verticalScroll(rememberScrollState())
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

                // Plan Status
                ExposedDropdownMenuBox(
                    expanded = statusExpanded,
                    onExpandedChange = { statusExpanded = !statusExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        readOnly = true,
                        value = status,
                        onValueChange = {},
                        label = { Text("Plan Status") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = statusExpanded) },
                        colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor()
                            .testTag("input_sub_status")
                    )
                    ExposedDropdownMenu(
                        expanded = statusExpanded,
                        onDismissRequest = { statusExpanded = false }
                    ) {
                        statuses.forEach { selection ->
                            DropdownMenuItem(
                                text = { Text(selection) },
                                onClick = {
                                    status = selection
                                    statusExpanded = false
                                }
                            )
                        }
                    }
                }

                // Cost (Full Width)
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
                        .fillMaxWidth()
                        .testTag("input_sub_amount"),
                    supportingText = {
                        if (amountError) {
                            Text("Cost > 0 required", color = MaterialTheme.colorScheme.error)
                        }
                    }
                )

                // Cycle (Full Width Dropdown)
                ExposedDropdownMenuBox(
                    expanded = cycleExpanded,
                    onExpandedChange = { cycleExpanded = !cycleExpanded },
                    modifier = Modifier.fillMaxWidth()
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

                // Payment Source
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

                // Via Platform (Full Width Dropdown)
                ExposedDropdownMenuBox(
                    expanded = platformExpanded,
                    onExpandedChange = { platformExpanded = !platformExpanded },
                    modifier = Modifier.fillMaxWidth()
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

                // Category (Full Width Dropdown)
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
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

                // Custom Notification section
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { autoNotify = !autoNotify }
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = autoNotify,
                            onCheckedChange = { autoNotify = it },
                            modifier = Modifier.testTag("checkbox_sub_notify")
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Notify before due date?",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    if (autoNotify) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 4.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Remind me days before due:",
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                listOf(1, 2, 3, 5, 7).forEach { days ->
                                    val daysStr = days.toString()
                                    val isSelected = customReminderDaysStr == daysStr
                                    
                                    Surface(
                                        selected = isSelected,
                                        onClick = { customReminderDaysStr = daysStr },
                                        shape = RoundedCornerShape(8.dp),
                                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                                        border = BorderStroke(
                                            width = 1.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
                                        ),
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(36.dp)
                                            .testTag("sub_reminder_chip_$days")
                                    ) {
                                        Box(
                                            modifier = Modifier.fillMaxSize(),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = "${days}d",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                                            )
                                        }
                                    }
                                }
                            }
                            
                            OutlinedTextField(
                                value = customReminderDaysStr,
                                onValueChange = { 
                                    if (it.all { char -> char.isDigit() }) {
                                        customReminderDaysStr = it
                                    }
                                },
                                label = { Text("Custom days before due") },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("input_sub_reminder_days"),
                                shape = RoundedCornerShape(12.dp)
                            )
                        }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHistoricalSubscriptionPaymentDialog(
    subscriptions: List<Subscription>,
    monthOptions: List<Pair<String, String>>,
    payments: List<SubscriptionPayment>,
    onDismiss: () -> Unit,
    onConfirm: (subscription: Subscription, monthYear: String, amount: Double, paymentDate: Long, overwrite: Boolean) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedSub by remember { mutableStateOf<Subscription?>(subscriptions.firstOrNull()) }
    var selectedMonthOption by remember { mutableStateOf(monthOptions.find { it.first == SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) } ?: monthOptions.firstOrNull()) }
    var amountStr by remember { mutableStateOf("") }
    var paymentDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    var subDropdownExpanded by remember { mutableStateOf(false) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    LaunchedEffect(selectedSub) {
        if (selectedSub != null) {
            amountStr = selectedSub!!.amount.toString()
        }
    }

    val isAlreadyPaid = selectedSub != null && selectedMonthOption != null && payments.any { pay ->
        pay.subscriptionId == selectedSub!!.id && pay.monthYear == selectedMonthOption!!.first
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Historical Subscription Payment",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .testTag("add_historical_sub_payment_dialog"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (subscriptions.isEmpty()) {
                    Text(
                        text = "Please add at least one subscription first in the 'My Subscriptions' tab.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // Subscription Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = subDropdownExpanded,
                        onExpandedChange = { subDropdownExpanded = !subDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedSub?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Subscription") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = subDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("historical_sub_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = subDropdownExpanded,
                            onDismissRequest = { subDropdownExpanded = false }
                        ) {
                            subscriptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.name) },
                                    onClick = {
                                        selectedSub = option
                                        subDropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("historical_sub_option_${option.name}")
                                )
                            }
                        }
                    }

                    // Month Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = monthDropdownExpanded,
                        onExpandedChange = { monthDropdownExpanded = !monthDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedMonthOption?.second ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Billing Month") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = monthDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("historical_sub_month_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = monthDropdownExpanded,
                            onDismissRequest = { monthDropdownExpanded = false }
                        ) {
                            monthOptions.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.second) },
                                    onClick = {
                                        selectedMonthOption = option
                                        monthDropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("historical_sub_month_option_${option.first}")
                                )
                            }
                        }
                    }

                    // Duplicate Warning Banner
                    if (isAlreadyPaid) {
                        val monthLabel = selectedMonthOption?.second ?: ""
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Warning,
                                    contentDescription = "Warning",
                                    tint = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "Payment already marked for $monthLabel. Saving will replace it.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                    }

                    // Amount Text Field
                    OutlinedTextField(
                        value = amountStr,
                        onValueChange = {
                            amountStr = it
                            val parsed = it.toDoubleOrNull()
                            amountError = parsed == null || parsed < 0.0
                        },
                        label = { Text("Amount Paid (₹)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        isError = amountError,
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth().testTag("historical_sub_amount_input"),
                        shape = RoundedCornerShape(12.dp)
                    )

                    // Date Selection Input
                    val dateLabel = SimpleDateFormat("dd MMM yyyy", Locale.getDefault()).format(Date(paymentDateMillis))
                    OutlinedTextField(
                        value = dateLabel,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Payment Date") },
                        trailingIcon = {
                            Icon(
                                imageVector = Icons.Default.CalendarToday,
                                contentDescription = "Pick Date",
                                modifier = Modifier.clickable {
                                    showDatePicker(context, paymentDateMillis) { newMillis ->
                                        paymentDateMillis = newMillis
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDatePicker(context, paymentDateMillis) { newMillis ->
                                    paymentDateMillis = newMillis
                                }
                            }
                            .testTag("historical_sub_date_picker_trigger"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (subscriptions.isNotEmpty()) {
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull()
                        val sub = selectedSub
                        val monthYear = selectedMonthOption?.first
                        if (amount != null && amount >= 0.0 && sub != null && monthYear != null) {
                            onConfirm(sub, monthYear, amount, paymentDateMillis, isAlreadyPaid)
                        } else {
                            if (amount == null || amount < 0.0) amountError = true
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_historical_sub_payment")
                ) {
                    Text(if (isAlreadyPaid) "Overwrite" else "Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_add_historical_sub_payment")) {
                Text("Cancel")
            }
        }
    )
}
