package com.example.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.viewmodel.TrackerViewModel
import com.example.viewmodel.UpcomingPaymentItem
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: TrackerViewModel,
    onNavigateToBills: () -> Unit,
    onNavigateToSubs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bills by viewModel.bills.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val upcomingPayments by viewModel.upcomingPayments.collectAsState()
    val completedPayments by viewModel.completedPayments.collectAsState()
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsState()
    
    val totalBillsAmount by viewModel.monthlyBillsTotal.collectAsState()
    val outstandingBills by viewModel.outstandingBillsTotal.collectAsState()
    val totalSubsAmount by viewModel.monthlySubscriptionsTotal.collectAsState()

    val totalMonthExpense = totalBillsAmount + totalSubsAmount
    val totalPaidBills = totalBillsAmount - outstandingBills

    var dashboardListTab by remember { mutableStateOf("Pending") } // "Pending", "Paid"
    var billToPayInput by remember { mutableStateOf<com.example.data.model.Bill?>(null) }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .testTag("dashboard_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
        // Welcome and Header
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Your Financial Cockpit",
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.Bold,
                            letterSpacing = (-0.5).sp
                        ),
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = SimpleDateFormat("EEEE, d MMMM yyyy", Locale.getDefault()).format(Date()),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                    )
                }
                
                MonthSelector(viewModel = viewModel)
            }
        }

        // Custom Visual Dashboard Widget (Expenses Gauge Ring)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("summary_gauge_card"),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Custom Drawing Graphic (Donut Ring Chart)
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .padding(4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        val progressRatio = if (totalBillsAmount > 0) {
                            val ratio = (totalPaidBills / totalBillsAmount).toFloat()
                            if (ratio.isNaN()) 1.0f else ratio.coerceIn(0f, 1f)
                        } else 1.0f

                        val animatedProgress by animateFloatAsState(
                            targetValue = progressRatio,
                            animationSpec = tween(durationMillis = 1000)
                        )

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            // Background Ring
                            drawCircle(
                                color = Color.Gray.copy(alpha = 0.2f),
                                radius = size.minDimension / 2,
                                style = Stroke(width = 10.dp.toPx(), cap = StrokeCap.Round)
                            )
                            // Foreground Active Flow
                            drawArc(
                                color = Color(0xFF4CAF50), // Green for paid
                                startAngle = -90f,
                                sweepAngle = 360f * animatedProgress,
                                useCenter = false,
                                style = Stroke(width = 12.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                        
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "${(progressRatio * 100).toInt()}%",
                                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Bills Paid",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Monthly Commitment",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "₹${String.format("%,.2f", totalMonthExpense)}",
                            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Paid Bills",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "₹${String.format("%,.0f", totalPaidBills)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = Color(0xFF4CAF50)
                                )
                            }
                            Column {
                                Text(
                                    text = "Sub-plans",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = "₹${String.format("%,.0f", totalSubsAmount)}",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Quick Navigation Grid
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToBills() }
                        .testTag("nav_bills"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.ReceiptLong,
                            contentDescription = "Bills",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Manage Bills",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${bills.size} registered items",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }

                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onNavigateToSubs() }
                        .testTag("nav_subscriptions"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.Autorenew,
                            contentDescription = "Subscriptions",
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Subscriptions",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = "${subscriptions.filter { it.isActive }.size} active plans",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }

        // Upcoming Payments Section Title and Tab Filter
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf("Pending", "Paid").forEach { tab ->
                        val isSelected = dashboardListTab == tab
                        val count = if (tab == "Pending") upcomingPayments.size else completedPayments.size
                        ElevatedFilterChip(
                            selected = isSelected,
                            onClick = { dashboardListTab = tab },
                            label = { Text("$tab ($count)") },
                            modifier = Modifier.testTag("dashboard_tab_$tab")
                        )
                    }
                }

                // Small calendar/clock tag
                Text(
                    text = "Schedules",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }

        // List representation of filter
        val selectedList = if (dashboardListTab == "Pending") upcomingPayments else completedPayments

        if (selectedList.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = if (dashboardListTab == "Pending") Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = "No payments",
                            tint = if (dashboardListTab == "Pending") Color(0xFF4CAF50) else MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(56.dp)
                        )
                        Text(
                            text = if (dashboardListTab == "Pending") "You are all caught up!" else "No completed payments",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Text(
                            text = if (dashboardListTab == "Pending") {
                                "No pending bills or subscription payments in this cycle."
                            } else {
                                "No registered items have been marked paid for this month."
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }
        } else {
            items(
                items = selectedList,
                key = { "${dashboardListTab}_${it.itemType}_${it.id}" }
            ) { scheduleItem ->
                UpcomingPaymentRow(
                    item = scheduleItem,
                    isPaidMode = dashboardListTab == "Paid",
                    onPayToggle = {
                        val parent = scheduleItem.parentItem
                        if (scheduleItem.itemType == "BILL" && parent is com.example.data.model.Bill) {
                            if (dashboardListTab == "Pending" && parent.isVariable) {
                                billToPayInput = parent
                            } else {
                                viewModel.toggleBillPaid(parent, selectedMonthYear)
                            }
                        } else if (parent is com.example.data.model.Subscription) {
                            if (dashboardListTab == "Pending") {
                                viewModel.renewSubscription(parent, selectedMonthYear)
                            }
                        }
                    }
                )
            }
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
}
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MonthSelector(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsState()
    val options = viewModel.getMonthYearOptions()
    var expanded by remember { mutableStateOf(false) }

    val currentLabel = options.find { it.first == selectedMonthYear }?.second ?: selectedMonthYear

    Box(modifier = modifier) {
        Card(
            onClick = { expanded = true },
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
            ),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
        ) {
            Row(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.CalendarMonth,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = currentLabel,
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.testTag("month_selector_dropdown")
        ) {
            options.forEach { (value, label) ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = label,
                            fontWeight = if (value == selectedMonthYear) FontWeight.Bold else FontWeight.Normal,
                            color = if (value == selectedMonthYear) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    },
                    onClick = {
                        viewModel.selectMonthYear(value)
                        expanded = false
                    },
                    modifier = Modifier.testTag("month_option_$value")
                )
            }
        }
    }
}

@Composable
fun UpcomingPaymentRow(
    item: UpcomingPaymentItem,
    isPaidMode: Boolean,
    onPayToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isBill = item.itemType == "BILL"
    val isOverdue = item.isOverdue

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("upcoming_payment_row_${item.itemType}_${item.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isPaidMode) {
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)
            } else if (isOverdue) {
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f)
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = BorderStroke(
            width = 1.dp,
            color = if (isPaidMode) {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
            } else if (isOverdue) {
                MaterialTheme.colorScheme.error.copy(alpha = 0.4f)
            } else {
                MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            }
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Priority Indicator / Color Theme Block
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (isPaidMode) Color(0xFFE8F5E9)
                        else if (isOverdue) MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
                        else if (isBill) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                        else MaterialTheme.colorScheme.secondary.copy(alpha = 0.15f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isPaidMode) Icons.Default.CheckCircle
                    else if (isOverdue) Icons.Default.Warning
                    else if (isBill) Icons.Default.Receipt
                    else Icons.Default.CreditCard,
                    contentDescription = item.itemType,
                    tint = if (isPaidMode) Color(0xFF4CAF50)
                    else if (isOverdue) MaterialTheme.colorScheme.error
                    else if (isBill) MaterialTheme.colorScheme.primary
                    else MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            // Text Details
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.width(6.dp))

                    // Small Tag representation of Bill vs Sub
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(
                                (if (isBill) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer).copy(alpha = 0.6f)
                            )
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = if (isBill) "Bill" else "Sub",
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 9.sp),
                            color = if (isBill) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }

                Text(
                    text = item.extraInfo,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                // Days Counter Text Badge
                Text(
                    text = when {
                        isPaidMode -> "Payment Recorded"
                        isOverdue -> {
                            val parent = item.parentItem
                            if (isBill && parent is com.example.data.model.Bill) {
                                "Overdue (Due on Day ${parent.dueDay})"
                            } else {
                                "Overdue"
                            }
                        }
                        item.daysRemaining == 0 -> "Due today!"
                        item.daysRemaining == 1 -> "Due tomorrow"
                        isBill && item.parentItem is com.example.data.model.Bill -> {
                            val parent = item.parentItem as com.example.data.model.Bill
                            "Due in ${item.daysRemaining} days (Day ${parent.dueDay})"
                        }
                        else -> "Due in ${item.daysRemaining} days"
                    },
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isPaidMode) Color(0xFF2E7D32) else if (isOverdue) MaterialTheme.colorScheme.error else if (item.daysRemaining <= 2) Color(0xFFFF9800) else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Amount and Action
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "₹${String.format("%.2f", item.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(4.dp))

                if (!isPaidMode) {
                    TextButton(
                        onClick = onPayToggle,
                        modifier = Modifier
                            .height(32.dp)
                            .testTag("action_button_${item.itemType}_${item.id}"),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = if (isBill) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
                        ),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isBill) "Pay" else "Mark Paid",
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold)
                        )
                    }
                } else {
                    if (isBill) {
                        // For bills, they can unmark it paid too
                        TextButton(
                            onClick = onPayToggle,
                            modifier = Modifier
                                .height(32.dp)
                                .testTag("unpay_button_${item.itemType}_${item.id}"),
                            colors = ButtonDefaults.textButtonColors(
                                contentColor = MaterialTheme.colorScheme.error
                            ),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                        ) {
                            Text(
                                text = "Unpay",
                                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold)
                            )
                        }
                    } else {
                        // For subscriptions, display checked sign
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Paid",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val bills by viewModel.bills.collectAsState()
    val subscriptions by viewModel.subscriptions.collectAsState()
    val billPayments by viewModel.billPayments.collectAsState()
    val subPayments by viewModel.payments.collectAsState()
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsState()

    var activePeriod by remember { mutableStateOf("Monthly") } // "Monthly", "Quarterly", "Yearly", "Lifetime", "Forecast"

    // Parse current selection
    val sdfMY = SimpleDateFormat("yyyy-MM", Locale.getDefault())
    val selectedDate = try { sdfMY.parse(selectedMonthYear) } catch (e: Exception) { Date() }
    val cal = Calendar.getInstance().apply { time = selectedDate }
    val selectedYear = cal.get(Calendar.YEAR)
    val selectedMonthRaw = cal.get(Calendar.MONTH) + 1 // 1-12

    val selectedQuarter = when (selectedMonthRaw) {
        in 1..3 -> 1
        in 4..6 -> 2
        in 7..9 -> 3
        else -> 4
    }

    var dashboardYear by remember { mutableStateOf(selectedYear) }
    var dashboardQuarter by remember { mutableStateOf(selectedQuarter) }

    LaunchedEffect(selectedMonthYear) {
        dashboardYear = selectedYear
        dashboardQuarter = selectedQuarter
    }

    // Filters for payments
    val targetMonthsForPeriod = remember(selectedMonthYear, activePeriod, dashboardYear, dashboardQuarter) {
        when (activePeriod) {
            "Monthly" -> listOf(selectedMonthYear)
            "Quarterly" -> {
                val qMonths = when (dashboardQuarter) {
                    1 -> listOf("01", "02", "03")
                    2 -> listOf("04", "05", "06")
                    3 -> listOf("07", "08", "09")
                    else -> listOf("10", "11", "12")
                }
                qMonths.map { "$dashboardYear-$it" }
            }
            "Yearly" -> {
                (1..12).map { 
                    val mStr = if (it < 10) "0$it" else "$it"
                    "$dashboardYear-$mStr"
                }
            }
            else -> emptyList()
        }
    }

    // Helper to format timestamps to yyyy-MM
    fun getMonthYearOfTimestamp(timestamp: Long): String {
        return sdfMY.format(Date(timestamp))
    }

    // Filter payments in selected period
    val periodBillPayments = remember(billPayments, targetMonthsForPeriod, activePeriod) {
        if (activePeriod == "Lifetime") {
            billPayments
        } else {
            billPayments.filter { targetMonthsForPeriod.contains(it.monthYear) }
        }
    }

    val periodSubPayments = remember(subPayments, targetMonthsForPeriod, activePeriod) {
        if (activePeriod == "Lifetime") {
            subPayments
        } else {
            subPayments.filter { 
                val pMY = getMonthYearOfTimestamp(it.paymentDate)
                targetMonthsForPeriod.contains(pMY)
            }
        }
    }

    val totalSpentBill = periodBillPayments.sumOf { it.amount }
    val totalSpentSub = periodSubPayments.sumOf { it.amount }
    val totalSpentOverall = totalSpentBill + totalSpentSub

    // Group expenses by category
    val expensesByCategory = remember(periodBillPayments, periodSubPayments) {
        val groups = mutableMapOf<String, Double>()
        periodBillPayments.forEach { groups[it.category] = (groups[it.category] ?: 0.0) + it.amount }
        periodSubPayments.forEach { groups[it.category] = (groups[it.category] ?: 0.0) + it.amount }
        groups.toList().sortedByDescending { it.second }
    }

    // Combine item descriptions for detailed maximum expense list
    val topExpensesList = remember(periodBillPayments, periodSubPayments) {
        val list = mutableListOf<Pair<String, Double>>()
        periodBillPayments.forEach { list.add(Pair(it.billName + " (Bill)", it.amount)) }
        periodSubPayments.forEach { list.add(Pair(it.subscriptionName + " (Sub)", it.amount)) }
        list.sortedByDescending { it.second }
    }

    // Average monthly spending for historical comparison
    val allPreviousMonthsSpending = remember(billPayments, subPayments, selectedMonthYear) {
        val monthlyTotals = mutableMapOf<String, Double>()
        billPayments.filter { it.monthYear != selectedMonthYear }.forEach {
            monthlyTotals[it.monthYear] = (monthlyTotals[it.monthYear] ?: 0.0) + it.amount
        }
        subPayments.forEach {
            val pMY = getMonthYearOfTimestamp(it.paymentDate)
            if (pMY != selectedMonthYear) {
                monthlyTotals[pMY] = (monthlyTotals[pMY] ?: 0.0) + it.amount
            }
        }
        if (monthlyTotals.isEmpty()) 0.0 else monthlyTotals.values.average()
    }

    // --- NEXT MONTH FORECAST CRADLE ---
    val nextMonthYearStr = remember(selectedMonthYear) {
        val nextCal = Calendar.getInstance().apply { time = selectedDate }
        nextCal.add(Calendar.MONTH, 1)
        sdfMY.format(nextCal.time)
    }

    val forecastedItems = remember(bills, subscriptions, billPayments, nextMonthYearStr) {
        val list = mutableListOf<Pair<String, Double>>()
        
        // Active Subscriptions
        subscriptions.filter { it.isActive }.forEach {
            list.add(Pair(it.name + " (Sub)", it.amount))
        }

        // Bills due next month
        bills.forEach { b ->
            if (b.isDueInMonthYear(nextMonthYearStr)) {
                val predictedAmount = if (b.isVariable) {
                    // Average of previous occurrences
                    val pastPays = billPayments.filter { it.billId == b.id }
                    if (pastPays.isNotEmpty()) {
                        pastPays.map { it.amount }.average()
                    } else {
                        b.amount // default estimate
                    }
                } else {
                    b.amount
                }
                list.add(Pair(b.name + " (Bill)", predictedAmount))
            }
        }
        list.sortedByDescending { it.second }
    }

    val forecastedTotal = forecastedItems.sumOf { it.second }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("dashboard_analytics_screen"),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Dashboard Header
        item {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = "Analytics & Insights",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = "Gain visibility into your spending trends",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                )
            }
        }

        // Active Period Selector Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                        RoundedCornerShape(12.dp)
                    )
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf("Monthly", "Quarterly", "Yearly", "Lifetime", "Forecast").forEach { period ->
                    val isSelected = activePeriod == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent)
                            .clickable { activePeriod = period }
                            .padding(vertical = 8.dp)
                            .testTag("analytics_period_$period"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (period == "Forecast") "Forecast" else period,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Sub-selector row depending on the chosen period style
        if (activePeriod != "Forecast") {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        when (activePeriod) {
                            "Monthly" -> {
                                Text(
                                    text = "Viewing Month Analytics:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                MonthSelector(viewModel = viewModel)
                            }
                            "Quarterly" -> {
                                Text(
                                    text = "Viewing Quarter:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                
                                // Quarter Selection Dropdown
                                var qExpanded by remember { mutableStateOf(false) }
                                Box(modifier = Modifier.padding(end = 8.dp)) {
                                    Card(
                                        onClick = { qExpanded = true },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "Q$dashboardQuarter",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onSecondaryContainer
                                            )
                                        }
                                    }
                                    DropdownMenu(expanded = qExpanded, onDismissRequest = { qExpanded = false }) {
                                        (1..4).forEach { q ->
                                            DropdownMenuItem(
                                                text = { Text("Quarter $q (Q$q)", style = MaterialTheme.typography.bodyMedium) },
                                                onClick = {
                                                    dashboardQuarter = q
                                                    qExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }

                                // Year Selection Dropdown
                                var yExpanded by remember { mutableStateOf(false) }
                                Box {
                                    Card(
                                        onClick = { yExpanded = true },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "$dashboardYear",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    DropdownMenu(expanded = yExpanded, onDismissRequest = { yExpanded = false }) {
                                        listOf(2024, 2025, 2026, 2027, 2028).forEach { yr ->
                                            DropdownMenuItem(
                                                text = { Text("$yr", style = MaterialTheme.typography.bodyMedium) },
                                                onClick = {
                                                    dashboardYear = yr
                                                    yExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            "Yearly" -> {
                                Text(
                                    text = "Viewing Year:",
                                    style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                    color = MaterialTheme.colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )

                                // Year Selection Dropdown
                                var yExpanded by remember { mutableStateOf(false) }
                                Box {
                                    Card(
                                        onClick = { yExpanded = true },
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "$dashboardYear",
                                                style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                                                color = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                            Icon(
                                                imageVector = Icons.Default.ArrowDropDown,
                                                contentDescription = null,
                                                modifier = Modifier.size(16.dp),
                                                tint = MaterialTheme.colorScheme.onPrimaryContainer
                                            )
                                        }
                                    }
                                    DropdownMenu(expanded = yExpanded, onDismissRequest = { yExpanded = false }) {
                                        listOf(2024, 2025, 2026, 2027, 2028).forEach { yr ->
                                            DropdownMenuItem(
                                                text = { Text("$yr", style = MaterialTheme.typography.bodyMedium) },
                                                onClick = {
                                                    dashboardYear = yr
                                                    yExpanded = false
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                            "Lifetime" -> {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Timeline,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Text(
                                        text = "Entire Lifetime: Complete historical payments",
                                        style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        if (activePeriod != "Forecast") {
            // SPENT SUMMARY CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = when (activePeriod) {
                                "Monthly" -> {
                                    val options = viewModel.getMonthYearOptions()
                                    val label = options.find { it.first == selectedMonthYear }?.second ?: selectedMonthYear
                                    "Total Spend (Actual) • $label"
                                }
                                "Quarterly" -> "Q$dashboardQuarter Spend (Actual) • $dashboardYear"
                                "Yearly" -> "Yearly Spend (Actual) • $dashboardYear"
                                else -> "Lifetime Spend (Actual)"
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format("%,.2f", totalSpentOverall)}",
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Bills Paid", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                                Text("₹${String.format("%,.2f", totalSpentBill)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                            Column {
                                Text("Subscriptions", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f))
                                Text("₹${String.format("%,.2f", totalSpentSub)}", style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                    }
                }
            }

            // MONTHLY COMPARISON CARD
            if (activePeriod == "Monthly" && allPreviousMonthsSpending > 0) {
                item {
                    val diff = totalSpentOverall - allPreviousMonthsSpending
                    val percent = (diff / allPreviousMonthsSpending) * 100
                    val isOverspent = diff > 0

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (isOverspent) {
                                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                            } else {
                                Color(0xFFE8F5E9)
                            }
                        ),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, if (isOverspent) MaterialTheme.colorScheme.error.copy(alpha = 0.2f) else Color(0xFFC8E6C9))
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = if (isOverspent) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                                contentDescription = null,
                                tint = if (isOverspent) MaterialTheme.colorScheme.error else Color(0xFF388E3C),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column {
                                Text(
                                    text = if (isOverspent) "Spending is up!" else "Spending is down!",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = if (isOverspent) MaterialTheme.colorScheme.onErrorContainer else Color(0xFF1B5E20)
                                )
                                Text(
                                    text = "You spent ₹${String.format("%,.2f", Math.abs(diff))} (${String.format("%.1f", Math.abs(percent))}%) ${if (isOverspent) "more" else "less"} than your historical monthly average (₹${String.format("%,.0f", allPreviousMonthsSpending)}).",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // CATEGORY DISTRIBUTION CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Spend by Category",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(16.dp))

                        if (expensesByCategory.isEmpty()) {
                            Text(
                                text = "No payments registered for this period. Mark items as paid in Home/Bills tabs to populate analytics.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                expensesByCategory.forEach { (cat, amount) ->
                                    val percent = if (totalSpentOverall > 0) (amount / totalSpentOverall).toFloat() else 0f
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Icon(
                                                    imageVector = when (cat) {
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
                                                    modifier = Modifier.size(16.dp)
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(cat, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
                                            }
                                            Text(
                                                text = "₹${String.format("%,.2f", amount)} (${String.format("%.1f", percent * 100)}%)",
                                                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.ExtraBold)
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(6.dp))
                                        LinearProgressIndicator(
                                            progress = { percent },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(6.dp)
                                                .clip(CircleShape),
                                            color = MaterialTheme.colorScheme.primary,
                                            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // TOP EXPENDITURE ITEMS CARD
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Highest Expense Items",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))

                        if (topExpensesList.isEmpty()) {
                            Text(
                                "None recorded",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                topExpensesList.take(5).forEachIndexed { index, (name, amt) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Box(
                                                modifier = Modifier
                                                    .size(24.dp)
                                                    .clip(CircleShape)
                                                    .background(MaterialTheme.colorScheme.secondaryContainer),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = "${index + 1}",
                                                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                                                    color = MaterialTheme.colorScheme.onSecondaryContainer
                                                )
                                            }
                                            Spacer(modifier = Modifier.width(10.dp))
                                            Text(name, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text(
                                            text = "₹${String.format("%,.2f", amt)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                    if (index < topExpensesList.size - 1 && index < 4) {
                                        HorizontalDivider(color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.05f))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // --- FORECAST INSIGHTS SPLIT ---
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Timeline,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = "Next-Month Projection",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Estimated spend for $nextMonthYearStr is calculated from active subscriptions and averages of previous bills (including variable inputs like electricity).",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "₹${String.format("%,.2f", forecastedTotal)}",
                            style = MaterialTheme.typography.displaySmall.copy(fontWeight = FontWeight.ExtraBold),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text(
                            text = "Breakdown of Planned Obligations",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        if (forecastedItems.isEmpty()) {
                            Text(
                                text = "No active monthly obligations declared. Create bills or subscriptions to trigger future-looking forecasting models.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                            )
                        } else {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                forecastedItems.forEach { (name, amt) ->
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                imageVector = if (name.contains("Bill")) Icons.Default.ReceiptLong else Icons.Default.Autorenew,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(name, style = MaterialTheme.typography.bodyMedium)
                                        }
                                        Text(
                                            text = "₹${String.format("%,.2f", amt)}",
                                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
