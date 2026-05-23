package com.example.ui

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Bill
import com.example.data.model.BillPayment
import com.example.viewmodel.TrackerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BillsScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val bills by viewModel.bills.collectAsState()
    val billPayments by viewModel.billPayments.collectAsState()
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsState()
    val totalBillsAmount by viewModel.monthlyBillsTotal.collectAsState()
    val outstandingAmount by viewModel.outstandingBillsTotal.collectAsState()

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedBillForEdit by remember { mutableStateOf<Bill?>(null) }
    var billToPayInput by remember { mutableStateOf<Bill?>(null) }
    var showAddHistoricalPaymentDialog by remember { mutableStateOf(false) }

    // Screen Main Tabs: "My Bills", "Payment History"
    var activeBillTab by remember { mutableStateOf("My Bills") } // "My Bills", "Payment History"

    // Filter state for "My Bills" Tab
    var selectedFilter by remember { mutableStateOf("All") } // "All", "Paid", "Unpaid"

    val filteredBills = remember(bills, selectedFilter, selectedMonthYear) {
        val dueBills = bills.filter { it.isDueInMonthYear(selectedMonthYear) || it.isPaidForMonthYear(selectedMonthYear) }
        when (selectedFilter) {
            "Paid" -> dueBills.filter { it.isPaidForMonthYear(selectedMonthYear) }
            "Unpaid" -> dueBills.filter { !it.isPaidForMonthYear(selectedMonthYear) }
            else -> dueBills
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Screen Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Monthly Payments",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Track and authorize recurring service bills",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                
                // Month selector dynamically filters the outstanding list & stats
                MonthSelector(viewModel = viewModel)
            }

            // Centralized Navigation Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(4.dp)
            ) {
                listOf("My Bills", "Payment History").forEach { tab ->
                    val isSelected = activeBillTab == tab
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (isSelected) MaterialTheme.colorScheme.primary
                                else Color.Transparent
                            )
                            .clickable { activeBillTab = tab }
                            .padding(vertical = 10.dp)
                            .testTag("tab_bill_main_$tab"),
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

            if (activeBillTab == "My Bills") {
                // Summary/Progress Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Outstanding in ${selectedMonthYear}",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Icon(
                                imageVector = Icons.Default.Receipt,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }

                        Text(
                            text = "₹${String.format("%,.2f", outstandingAmount)}",
                            style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val progressRatio = if (totalBillsAmount > 0) {
                            val ratio = ((totalBillsAmount - outstandingAmount) / totalBillsAmount).toFloat()
                            if (ratio.isNaN()) 1f else ratio.coerceIn(0f, 1f)
                        } else 1f

                        LinearProgressIndicator(
                            progress = { progressRatio },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(CircleShape),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Paid: ₹${String.format("%,.0f", totalBillsAmount - outstandingAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Text(
                                text = "Total Commitment: ₹${String.format("%,.0f", totalBillsAmount)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                }

                // Filter Tabs (Segmented Control style)
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
                    listOf("All", "Unpaid", "Paid").forEach { filter ->
                        val isSelected = selectedFilter == filter
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(10.dp))
                                .background(
                                    if (isSelected) MaterialTheme.colorScheme.primary
                                    else Color.Transparent
                                )
                                .clickable { selectedFilter = filter }
                                .padding(vertical = 10.dp)
                                .testTag("filter_bills_$filter"),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = filter,
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Bills List
                if (filteredBills.isEmpty()) {
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
                                imageVector = Icons.Default.Inbox,
                                contentDescription = "Empty",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "No bills found",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "Tap the '+' button below to create your first monthly payment item.",
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
                        items(items = filteredBills, key = { it.id }) { bill ->
                            BillRowItem(
                                bill = bill,
                                selectedMonthYear = selectedMonthYear,
                                onTogglePaid = {
                                    if (!bill.isPaidForMonthYear(selectedMonthYear) && bill.isVariable) {
                                        billToPayInput = bill
                                    } else {
                                        viewModel.toggleBillPaid(bill, selectedMonthYear)
                                    }
                                },
                                onEditClick = {
                                    selectedBillForEdit = bill
                                    showAddEditDialog = true
                                },
                                onDeleteClick = { viewModel.deleteBill(bill) }
                            )
                        }
                    }
                }
            } else {
                // Payment History Tab
                if (billPayments.isEmpty()) {
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
                                imageVector = Icons.Default.History,
                                contentDescription = "History Empty",
                                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f),
                                modifier = Modifier.size(64.dp)
                            )
                            Text(
                                text = "No payment history recorded",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "Whenever you click 'Mark Paid' on any bill, the full details of that transaction will be stored in this logs page.",
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
                        items(items = billPayments, key = { it.id }) { payment ->
                            BillPaymentRowItem(
                                payment = payment,
                                onDeleteClick = { viewModel.deleteBillPayment(payment) }
                            )
                        }
                    }
                }
            }
        }

        // Floating Action Button
        if (activeBillTab == "My Bills") {
            FloatingActionButton(
                onClick = {
                    selectedBillForEdit = null
                    showAddEditDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_bill_fab"),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Bill")
            }
        } else {
            FloatingActionButton(
                onClick = {
                    showAddHistoricalPaymentDialog = true
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(24.dp)
                    .testTag("add_historical_bill_payment_fab"),
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = "Add Historical Bill Payment")
            }
        }

        if (billToPayInput != null) {
            RecordVariablePaymentDialog(
                bill = billToPayInput!!,
                onDismiss = { billToPayInput = null },
                onConfirm = { amt ->
                    viewModel.toggleBillPaid(billToPayInput!!, selectedMonthYear, amt)
                    billToPayInput = null
                }
            )
        }

        // Add/Edit Dialog sheet
        if (showAddEditDialog) {
            AddEditBillDialog(
                bill = selectedBillForEdit,
                onDismiss = { showAddEditDialog = false },
                onSave = { name, amount, category, dueDay, reminderDays, notes, billingCycle, startMonthYear, isVariable ->
                    if (selectedBillForEdit == null) {
                        viewModel.addBill(name, amount, category, dueDay, reminderDays, notes, billingCycle, startMonthYear, isVariable)
                    } else {
                        val updated = selectedBillForEdit!!.copy(
                            name = name,
                            amount = amount,
                            category = category,
                            dueDay = dueDay,
                            customReminderDaysBefore = reminderDays,
                            notes = notes,
                            billingCycle = billingCycle,
                            startMonthYear = startMonthYear,
                            isVariable = isVariable
                        )
                        viewModel.updateBill(updated)
                    }
                    showAddEditDialog = false
                }
            )
        }

        if (showAddHistoricalPaymentDialog) {
            AddHistoricalBillPaymentDialog(
                bills = bills,
                monthOptions = viewModel.getMonthYearOptions(),
                onDismiss = { showAddHistoricalPaymentDialog = false },
                onConfirm = { bill, monthYear, amount, paymentDate ->
                    viewModel.addHistoricalBillPayment(bill, monthYear, amount, paymentDate)
                    showAddHistoricalPaymentDialog = false
                }
            )
        }
    }
}

@Composable
fun BillRowItem(
    bill: Bill,
    selectedMonthYear: String,
    onTogglePaid: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPaid = bill.isPaidForMonthYear(selectedMonthYear)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bill_card_${bill.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaid) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Platform Indicator Block
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (bill.category) {
                            "Rent" -> Icons.Default.Home
                            "Electricity" -> Icons.Default.Bolt
                            "Water" -> Icons.Default.Opacity
                            "Credit Card" -> Icons.Default.CreditCard
                            "Internet" -> Icons.Default.Wifi
                            "Insurance" -> Icons.Default.Shield
                            else -> Icons.Default.Receipt
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = bill.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                textDecoration = if (isPaid) TextDecoration.LineThrough else TextDecoration.None
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = if (isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.width(6.dp))

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.6f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = bill.category,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        
                        if (bill.isVariable) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.6f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Variable",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        
                        if (bill.billingCycle == "One-time" || bill.billingCycle == "One-Time") {
                            Spacer(modifier = Modifier.width(4.dp))
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "One-time",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Due Day: ${bill.dueDay}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                        Text(
                            text = "•",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                        )
                        Text(
                            text = "🔔 Notify ${bill.customReminderDaysBefore} day(s) before",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                        )
                    }
                }

                // Cost display
                Text(
                    text = "₹${String.format("%.2f", bill.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.ExtraBold,
                        textDecoration = if (isPaid) TextDecoration.LineThrough else TextDecoration.None
                    ),
                    color = if (isPaid) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f) else MaterialTheme.colorScheme.onSurface
                )
            }

            if (bill.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = bill.notes,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 12.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
            )

            // Bottom Actions Row (Mark Paid Button / Edit / Delete)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Beautiful interactive button instead of raw checkbox!
                if (!isPaid) {
                    Button(
                        onClick = onTogglePaid,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("mark_paid_bill_${bill.id}"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(14.dp))
                            Text(
                                "Mark Paid",
                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    }
                } else {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Paid",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = "Paid",
                            style = MaterialTheme.typography.titleSmall.copy(
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF4CAF50)
                            )
                        )
                        
                        Spacer(modifier = Modifier.width(4.dp))
                        
                        // Let user click to un-pay quickly
                        TextButton(
                            onClick = onTogglePaid,
                            modifier = Modifier.height(32.dp),
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error),
                            contentPadding = PaddingValues(horizontal = 8.dp)
                        ) {
                            Text("Unpay", style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold))
                        }
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("edit_bill_${bill.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Bill",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.outline
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("delete_bill_${bill.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Bill",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BillPaymentRowItem(
    payment: BillPayment,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
    val formattedDate = sdf.format(Date(payment.paymentDate))

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE8F5E9)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = payment.billName,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(end = 8.dp)
                    )
                    Text(
                        text = "₹${String.format("%.2f", payment.amount)}",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                        color = Color(0xFF2E7D32)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "${payment.category} • Cycle: ${payment.monthYear}",
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditBillDialog(
    bill: Bill?,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, Int, Int, String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(bill?.name ?: "") }
    var amountStr by remember { mutableStateOf(bill?.amount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var category by remember { mutableStateOf(bill?.category ?: "Utilities") }
    var dueDay by remember { mutableStateOf(bill?.dueDay ?: 1) }
    var reminderDays by remember { mutableStateOf(bill?.customReminderDaysBefore ?: 1) }
    var notes by remember { mutableStateOf(bill?.notes ?: "") }
    var billingCycle by remember { mutableStateOf(bill?.billingCycle ?: "Monthly") }
    var startMonthYear by remember { mutableStateOf(bill?.startMonthYear ?: "2026-05") }
    var isVariable by remember { mutableStateOf(bill?.isVariable ?: false) }

    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    val categories = listOf("Rent", "Electricity", "Water", "Internet", "Credit Card", "Insurance", "Utilities", "Other")
    var categoryExpanded by remember { mutableStateOf(false) }

    val monthOptions = remember {
        val list = mutableListOf<Pair<String, String>>()
        val sdfValue = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val sdfLabel = SimpleDateFormat("MMM yyyy", Locale.getDefault())
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -3)
        repeat(12) {
            list.add(Pair(sdfValue.format(cal.time), sdfLabel.format(cal.time)))
            cal.add(Calendar.MONTH, 1)
        }
        list
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (bill == null) "Add New Bill" else "Edit Bill",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_edit_bill_dialog"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Name Field
                OutlinedTextField(
                    value = name,
                    onValueChange = {
                        name = it
                        nameError = it.isBlank()
                    },
                    label = { Text("Bill Name (e.g., Gas, Rent)") },
                    isError = nameError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bill_name_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Amount Type Selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Amount Type",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(false, true).forEach { isVar ->
                            val isSelected = isVariable == isVar
                            FilterChip(
                                selected = isSelected,
                                onClick = { isVariable = isVar },
                                label = { Text(if (isVar) "Variable (Ask on Pay)" else "Fixed Amount") },
                                modifier = Modifier.weight(1f).testTag("bill_variable_chip_$isVar")
                            )
                        }
                    }
                }

                // Amount Field
                OutlinedTextField(
                    value = amountStr,
                    onValueChange = {
                        amountStr = it
                        val dVal = it.toDoubleOrNull()
                        amountError = if (isVariable) {
                            it.isNotEmpty() && (dVal == null || dVal < 0.0)
                        } else {
                            dVal == null || dVal <= 0.0
                        }
                    },
                    label = { Text(if (isVariable) "Estimated Amount (₹, optional)" else "Amount (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amountError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("bill_amount_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                // Category dropdown
                ExposedDropdownMenuBox(
                    expanded = categoryExpanded,
                    onExpandedChange = { categoryExpanded = !categoryExpanded },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = category,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Category") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                            .testTag("bill_category_dropdown"),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        categories.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option) },
                                onClick = {
                                    category = option
                                    categoryExpanded = false
                                },
                                modifier = Modifier.testTag("category_option_$option")
                            )
                        }
                    }
                }

                // Billing Cycle frequency selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Billing Frequency",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Monthly", "Quarterly", "Yearly", "One-time").forEach { cycle ->
                            val isSelected = billingCycle == cycle
                            FilterChip(
                                selected = isSelected,
                                onClick = { billingCycle = cycle },
                                label = { Text(cycle) },
                                modifier = Modifier.weight(1f).testTag("bill_cycle_chip_$cycle")
                            )
                        }
                    }
                }

                // Bill Starting Month (Shown only for Quarterly or Yearly or One-time bills)
                if (billingCycle != "Monthly") {
                    var startMonthExpanded by remember { mutableStateOf(false) }
                    val currentLabel = monthOptions.find { it.first == startMonthYear }?.second ?: startMonthYear
                    
                    ExposedDropdownMenuBox(
                        expanded = startMonthExpanded,
                        onExpandedChange = { startMonthExpanded = !startMonthExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (billingCycle == "One-time") "Payment Month" else "Start cycle month") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = startMonthExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("bill_start_month_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = startMonthExpanded,
                            onDismissRequest = { startMonthExpanded = false }
                        ) {
                            monthOptions.forEach { opt ->
                                DropdownMenuItem(
                                    text = { Text(opt.second) },
                                    onClick = {
                                        startMonthYear = opt.first
                                        startMonthExpanded = false
                                    },
                                    modifier = Modifier.testTag("start_month_option_${opt.first}")
                                )
                            }
                        }
                    }
                }

                // Due Day input (1 - 31)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Due Day: Day $dueDay of the month",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Slider(
                        value = dueDay.toFloat(),
                        onValueChange = { dueDay = it.toInt() },
                        valueRange = 1f..31f,
                        steps = 29,
                        modifier = Modifier.fillMaxWidth().testTag("bill_due_day_slider")
                    )
                }

                // Reminder Days before (1, 2, 3, 5, 7)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Remind me $reminderDays day(s) before",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 2, 3, 5, 7).forEach { days ->
                            val isSelected = reminderDays == days
                            FilterChip(
                                selected = isSelected,
                                onClick = { reminderDays = days },
                                label = { Text("${days}d") },
                                modifier = Modifier.weight(1f).testTag("reminder_chip_$days")
                            )
                        }
                    }
                }

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (optional)") },
                    maxLines = 2,
                    modifier = Modifier.fillMaxWidth().testTag("bill_notes_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val isNameValid = name.isNotBlank()
                    val parsedAmount = amountStr.toDoubleOrNull()
                    val amountVal = parsedAmount ?: 0.0
                    val isAmountValid = if (isVariable) {
                        amountStr.isEmpty() || (parsedAmount != null && parsedAmount >= 0.0)
                    } else {
                        parsedAmount != null && parsedAmount > 0.0
                    }

                    nameError = !isNameValid
                    amountError = !isAmountValid

                    if (isNameValid && isAmountValid) {
                        onSave(name, amountVal, category, dueDay, reminderDays, notes, billingCycle, startMonthYear, isVariable)
                    }
                },
                modifier = Modifier.testTag("save_bill_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("cancel_bill_button")
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun RecordVariablePaymentDialog(
    bill: Bill,
    onDismiss: () -> Unit,
    onConfirm: (Double) -> Unit
) {
    var amountText by remember { mutableStateOf(if (bill.amount > 0.0) bill.amount.toString() else "") }
    var amountError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record ${bill.name} Payment",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Please enter the actual amount paid for this cycle of ${bill.name}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        val parsed = it.toDoubleOrNull()
                        amountError = parsed == null || parsed < 0.0
                    },
                    label = { Text("Amount Paid (₹)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = amountError,
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("variable_pay_input"),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val amount = amountText.toDoubleOrNull()
                    if (amount != null && amount >= 0.0) {
                        onConfirm(amount)
                    } else {
                        amountError = true
                    }
                },
                modifier = Modifier.testTag("confirm_variable_pay")
            ) {
                Text("Confirm & Pay")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHistoricalBillPaymentDialog(
    bills: List<Bill>,
    monthOptions: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onConfirm: (bill: Bill, monthYear: String, amount: Double, paymentDate: Long) -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    var selectedBill by remember { mutableStateOf<Bill?>(bills.firstOrNull()) }
    var selectedMonthOption by remember { mutableStateOf(monthOptions.find { it.first == SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()) } ?: monthOptions.firstOrNull()) }
    var amountStr by remember { mutableStateOf("") }
    var paymentDateMillis by remember { mutableStateOf(System.currentTimeMillis()) }

    var billDropdownExpanded by remember { mutableStateOf(false) }
    var monthDropdownExpanded by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    LaunchedEffect(selectedBill) {
        if (selectedBill != null) {
            amountStr = selectedBill!!.amount.toString()
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Record Historical Payment",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_historical_payment_dialog"),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (bills.isEmpty()) {
                    Text(
                        text = "Please add at least one bill first in the 'My Bills' tab.",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    // Bill Selector Dropdown
                    ExposedDropdownMenuBox(
                        expanded = billDropdownExpanded,
                        onExpandedChange = { billDropdownExpanded = !billDropdownExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = selectedBill?.name ?: "",
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Select Bill") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = billDropdownExpanded) },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                                .testTag("historical_bill_dropdown"),
                            shape = RoundedCornerShape(12.dp)
                        )
                        ExposedDropdownMenu(
                            expanded = billDropdownExpanded,
                            onDismissRequest = { billDropdownExpanded = false }
                        ) {
                            bills.forEach { option ->
                                DropdownMenuItem(
                                    text = { Text(option.name) },
                                    onClick = {
                                        selectedBill = option
                                        billDropdownExpanded = false
                                    },
                                    modifier = Modifier.testTag("historical_bill_option_${option.name}")
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
                                .testTag("historical_month_dropdown"),
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
                                    modifier = Modifier.testTag("historical_month_option_${option.first}")
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
                        modifier = Modifier.fillMaxWidth().testTag("historical_amount_input"),
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
                                    showDatePickerHelper(context, paymentDateMillis) { newMillis ->
                                        paymentDateMillis = newMillis
                                    }
                                }
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                showDatePickerHelper(context, paymentDateMillis) { newMillis ->
                                    paymentDateMillis = newMillis
                                }
                            }
                            .testTag("historical_date_picker_trigger"),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        },
        confirmButton = {
            if (bills.isNotEmpty()) {
                Button(
                    onClick = {
                        val amount = amountStr.toDoubleOrNull()
                        val bill = selectedBill
                        val monthYear = selectedMonthOption?.first
                        if (amount != null && amount >= 0.0 && bill != null && monthYear != null) {
                            onConfirm(bill, monthYear, amount, paymentDateMillis)
                        } else {
                            if (amount == null || amount < 0.0) amountError = true
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_historical_payment")
                ) {
                    Text("Save")
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, modifier = Modifier.testTag("cancel_add_historical_payment")) {
                Text("Cancel")
            }
        }
    )
}

private fun showDatePickerHelper(
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
            }
            onDateSelected(selectedCal.timeInMillis)
        },
        initialCal.get(Calendar.YEAR),
        initialCal.get(Calendar.MONTH),
        initialCal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

