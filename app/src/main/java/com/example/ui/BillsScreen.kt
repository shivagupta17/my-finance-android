package com.example.ui

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

    var showAddEditDialog by remember { mutableStateOf(false) }
    var selectedBillForEdit by remember { mutableStateOf<Bill?>(null) }
    var showAddHistoricalPaymentDialog by remember { mutableStateOf(false) }

    // Screen Main Tabs: "My Bills", "Payment History"
    var activeBillTab by remember { mutableStateOf("My Bills") } // "My Bills", "Payment History"

    var showDeleteBillConfirmDialog by remember { mutableStateOf(false) }
    var billToDelete by remember { mutableStateOf<Bill?>(null) }

    var showDeleteBillPaymentConfirmDialog by remember { mutableStateOf(false) }
    var billPaymentToDelete by remember { mutableStateOf<BillPayment?>(null) }

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
                        text = "Bills Configuration",
                        style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Add, modify, or remove recurring bill schedules",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
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
                // Bills Configuration List
                if (bills.isEmpty()) {
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
                                text = "No bills configured",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            )
                            Text(
                                text = "Tap the '+' button below to define your first recurring bill item.",
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
                        items(items = bills, key = { it.id }) { bill ->
                            BillRowItem(
                                bill = bill,
                                onEditClick = {
                                    selectedBillForEdit = bill
                                    showAddEditDialog = true
                                },
                                onDeleteClick = {
                                    billToDelete = bill
                                    showDeleteBillConfirmDialog = true
                                }
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
                    val groupedBillPayments = remember(billPayments) {
                        billPayments.sortedByDescending { it.paymentDate }
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
                        groupedBillPayments.forEach { (monthName, paymentsInMonth) ->
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
                                BillPaymentRowItem(
                                    payment = payment,
                                    onDeleteClick = {
                                        billPaymentToDelete = payment
                                        showDeleteBillPaymentConfirmDialog = true
                                    }
                                )
                            }
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

        // Add/Edit Dialog sheet
        if (showAddEditDialog) {
            AddEditBillDialog(
                bill = selectedBillForEdit,
                defaultMonthYear = selectedMonthYear,
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
                onConfirm = { bill, monthYear, amount, paymentDate, overwrite ->
                    viewModel.addHistoricalBillPayment(bill, monthYear, amount, paymentDate, overwrite)
                    showAddHistoricalPaymentDialog = false
                }
            )
        }

        if (showDeleteBillConfirmDialog && billToDelete != null) {
            AlertDialog(
                onDismissRequest = { 
                    showDeleteBillConfirmDialog = false
                    billToDelete = null
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
                            text = "Delete Bill?",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                },
                text = {
                    Text(
                        text = "Are you sure you want to delete '${billToDelete?.name ?: ""}'? This will also permanently delete all associated payment history logs for this bill. This action cannot be undone.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            billToDelete?.let { viewModel.deleteBill(it) }
                            showDeleteBillConfirmDialog = false
                            billToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.testTag("confirm_delete_bill_button")
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteBillConfirmDialog = false
                            billToDelete = null
                        },
                        modifier = Modifier.testTag("dismiss_delete_bill_button")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }

        if (showDeleteBillPaymentConfirmDialog && billPaymentToDelete != null) {
            val payment = billPaymentToDelete!!
            val sdf = remember { SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()) }
            val formattedDate = remember(payment.paymentDate) { sdf.format(Date(payment.paymentDate)) }
            AlertDialog(
                onDismissRequest = { 
                    showDeleteBillPaymentConfirmDialog = false
                    billPaymentToDelete = null
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
                        text = "Are you sure you want to delete the payment entry of ${payment.amount} for month ${payment.monthYear} paid on $formattedDate? This status of the bill will revert to unpaid/unskipped for that period.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.deleteBillPayment(payment)
                            showDeleteBillPaymentConfirmDialog = false
                            billPaymentToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        ),
                        modifier = Modifier.testTag("confirm_delete_bill_payment_button")
                    ) {
                        Text("Delete")
                    }
                },
                dismissButton = {
                    TextButton(
                        onClick = {
                            showDeleteBillPaymentConfirmDialog = false
                            billPaymentToDelete = null
                        },
                        modifier = Modifier.testTag("dismiss_delete_bill_payment_button")
                    ) {
                        Text("Cancel")
                    }
                }
            )
        }
    }
}

@Composable
fun BillRowItem(
    bill: Bill,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bill_card_${bill.id}"),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            width = 1.dp,
            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Upper details row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Large Platform Indicator Block
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = when (bill.category) {
                            "Rent", "Rent & Housing" -> Icons.Default.Home
                            "Electricity" -> Icons.Default.Bolt
                            "Water" -> Icons.Default.Opacity
                            "Gas & Fuel" -> Icons.Default.LocalGasStation
                            "Internet", "Internet & Wi-Fi" -> Icons.Default.Wifi
                            "Mobile & Phone" -> Icons.Default.PhoneAndroid
                            "DTH & Cable TV" -> Icons.Default.Tv
                            "Groceries & Milk" -> Icons.Default.ShoppingCart
                            "Credit Card" -> Icons.Default.CreditCard
                            "Loan & EMI" -> Icons.Default.AccountBalance
                            "Insurance" -> Icons.Default.Shield
                            "Maid & Services" -> Icons.Default.Person
                            "Health & Gym" -> Icons.Default.Favorite
                            "Education & School" -> Icons.Default.School
                            else -> Icons.Default.Receipt
                        },
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
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
                            text = bill.name,
                            style = MaterialTheme.typography.titleMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.8f))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = bill.category,
                                style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSecondaryContainer
                            )
                        }
                        
                        if (bill.isVariable) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.8f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "Variable",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onErrorContainer
                                )
                            }
                        }
                        
                        if (bill.billingCycle == "One-time" || bill.billingCycle == "One-Time") {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.8f))
                                    .padding(horizontal = 4.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "One-time",
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(2.dp))

                    Text(
                        text = "Due Day: ${bill.dueDay}",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                    )
                }

                // Cost display
                Text(
                    text = if (bill.isVariable) {
                        if (bill.amount <= 0.0) "Variable" else "Variable (~₹${String.format("%.2f", bill.amount)})"
                    } else {
                        "₹${String.format("%.2f", bill.amount)}"
                    },
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            if (bill.notes.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = bill.notes,
                    style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 8.dp),
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f)
            )

            // Bottom Actions Row (Edit / Delete icon shortcuts)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cycle and reminders
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = "Billing Cycle",
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                    Text(
                        text = "Cycle: ${bill.billingCycle} • Remind: ${bill.customReminderDaysBefore} day(s) before",
                        style = MaterialTheme.typography.bodySmall.copy(fontSize = 11.sp),
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("edit_bill_${bill.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Bill",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }

                    IconButton(
                        onClick = onDeleteClick,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_bill_${bill.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Bill",
                            modifier = Modifier.size(14.dp),
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
    defaultMonthYear: String,
    onDismiss: () -> Unit,
    onSave: (String, Double, String, Int, Int, String, String, String, Boolean) -> Unit
) {
    var name by remember { mutableStateOf(bill?.name ?: "") }
    var amountStr by remember { mutableStateOf(bill?.amount?.let { if (it == 0.0) "" else it.toString() } ?: "") }
    var category by remember { mutableStateOf(bill?.category ?: "Electricity") }
    var dueDay by remember { mutableStateOf(bill?.dueDay ?: 1) }
    var reminderDays by remember { mutableStateOf(bill?.customReminderDaysBefore ?: 1) }
    var notes by remember { mutableStateOf(bill?.notes ?: "") }
    var billingCycle by remember { mutableStateOf(bill?.billingCycle ?: "Monthly") }
    var startMonthYear by remember { mutableStateOf(bill?.startMonthYear?.ifBlank { defaultMonthYear } ?: defaultMonthYear) }
    var isVariable by remember { mutableStateOf(bill?.isVariable ?: false) }

    var nameError by remember { mutableStateOf(false) }
    var amountError by remember { mutableStateOf(false) }

    val categories = listOf(
        "Rent & Housing",
        "Electricity",
        "Water",
        "Gas & Fuel",
        "Internet & Wi-Fi",
        "Mobile & Phone",
        "DTH & Cable TV",
        "Groceries & Milk",
        "Credit Card",
        "Loan & EMI",
        "Insurance",
        "Maid & Services",
        "Health & Gym",
        "Education & School",
        "Utilities",
        "Other"
    )
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
                    .verticalScroll(rememberScrollState())
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
                            CompactSelectableChip(
                                selected = isSelected,
                                onClick = { isVariable = isVar },
                                text = if (isVar) "Variable" else "Fixed",
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
                    Spacer(modifier = Modifier.height(4.dp))
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Monthly", "Quarterly").forEach { cycle ->
                                val isSelected = billingCycle == cycle
                                CompactSelectableChip(
                                    selected = isSelected,
                                    onClick = { billingCycle = cycle },
                                    text = cycle,
                                    modifier = Modifier.weight(1f).testTag("bill_cycle_chip_$cycle")
                                )
                            }
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            listOf("Yearly", "One-time").forEach { cycle ->
                                val isSelected = billingCycle == cycle
                                CompactSelectableChip(
                                    selected = isSelected,
                                    onClick = { billingCycle = cycle },
                                    text = cycle,
                                    modifier = Modifier.weight(1f).testTag("bill_cycle_chip_$cycle")
                                )
                            }
                        }
                    }
                }

                // Bill Starting Month
                if (true) {
                    var startMonthExpanded by remember { mutableStateOf(false) }
                    val currentLabel = monthOptions.find { it.first == startMonthYear }?.second ?: startMonthYear
                    val labelText = when (billingCycle) {
                        "One-time", "One-Time" -> "Payment Month"
                        "Monthly" -> "Starting Month"
                        else -> "Start Cycle Month"
                    }
                    
                    ExposedDropdownMenuBox(
                        expanded = startMonthExpanded,
                        onExpandedChange = { startMonthExpanded = !startMonthExpanded },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedTextField(
                            value = currentLabel,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(labelText) },
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
                            CompactSelectableChip(
                                selected = isSelected,
                                onClick = { reminderDays = days },
                                text = "${days}d",
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
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
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
    onConfirm: (bill: Bill, monthYear: String, amount: Double, paymentDate: Long, overwrite: Boolean) -> Unit
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

    val isAlreadyPaid = selectedBill?.isPaidForMonthYear(selectedMonthOption?.first ?: "") == true

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
                    .verticalScroll(rememberScrollState())
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
                            onConfirm(bill, monthYear, amount, paymentDateMillis, isAlreadyPaid)
                        } else {
                            if (amount == null || amount < 0.0) amountError = true
                        }
                    },
                    modifier = Modifier.testTag("confirm_add_historical_payment")
                ) {
                    Text(if (isAlreadyPaid) "Overwrite" else "Save")
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

@Composable
fun CompactSelectableChip(
    selected: Boolean,
    onClick: () -> Unit,
    text: String,
    modifier: Modifier = Modifier
) {
    Surface(
        selected = selected,
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
        border = BorderStroke(
            width = 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
        ),
        modifier = modifier.height(36.dp)
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 4.dp)
            )
        }
    }
}

