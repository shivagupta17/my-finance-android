package com.example.ui

import android.content.Context
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Expense
import com.example.viewmodel.TrackerViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

val EXPENSE_CATEGORIES = listOf(
    "Groceries",
    "Dining & Food",
    "Shopping",
    "Transport",
    "Entertainment",
    "Health",
    "Electronics",
    "Travel",
    "Personal Care",
    "Other"
)

fun getExpenseCategoryIcon(category: String): ImageVector {
    return when (category.lowercase(Locale.ROOT)) {
        "groceries" -> Icons.Default.LocalGroceryStore
        "dining & food", "dining", "food" -> Icons.Default.Restaurant
        "shopping" -> Icons.Default.ShoppingBag
        "transport", "fuel", "travel" -> Icons.Default.DirectionsCar
        "entertainment" -> Icons.Default.Movie
        "health", "medical" -> Icons.Default.LocalHospital
        "electronics" -> Icons.Default.Devices
        "personal care" -> Icons.Default.Spa
        else -> Icons.Default.Payments
    }
}

fun getExpenseCategoryColor(category: String): Color {
    return when (category.lowercase(Locale.ROOT)) {
        "groceries" -> Color(0xFF4CAF50)
        "dining & food", "dining", "food" -> Color(0xFFFF9800)
        "shopping" -> Color(0xFFE91E63)
        "transport", "fuel", "travel" -> Color(0xFF2196F3)
        "entertainment" -> Color(0xFF9C27B0)
        "health", "medical" -> Color(0xFFF44336)
        "electronics" -> Color(0xFF00BCD4)
        "personal care" -> Color(0xFFFF4081)
        else -> Color(0xFF607D8B)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExpensesScreen(
    viewModel: TrackerViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val allExpenses by viewModel.expenses.collectAsState()
    val selectedMonthYear by viewModel.selectedMonthYear.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf("All") }

    var showAddEditDialog by remember { mutableStateOf(false) }
    var expenseToEdit by remember { mutableStateOf<Expense?>(null) }
    var expenseToDelete by remember { mutableStateOf<Expense?>(null) }

    // Filter expenses by selectedMonthYear, category, and search
    val monthExpenses = remember(allExpenses, selectedMonthYear) {
        allExpenses.filter { it.monthYear == selectedMonthYear }
    }

    val filteredExpenses = remember(monthExpenses, selectedCategoryFilter, searchQuery) {
        monthExpenses.filter { expense ->
            val matchesCategory = selectedCategoryFilter == "All" || expense.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesSearch = searchQuery.isBlank() ||
                expense.title.contains(searchQuery, ignoreCase = true) ||
                expense.notes.contains(searchQuery, ignoreCase = true) ||
                expense.category.contains(searchQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }.sortedByDescending { it.date }
    }

    val totalMonthSpent = remember(monthExpenses) {
        monthExpenses.sumOf { it.amount }
    }

    val totalFilteredSpent = remember(filteredExpenses) {
        filteredExpenses.sumOf { it.amount }
    }

    // Top Category in selected month
    val topCategory = remember(monthExpenses) {
        monthExpenses.groupBy { it.category }
            .maxByOrNull { entry -> entry.value.sumOf { it.amount } }
            ?.key
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    expenseToEdit = null
                    showAddEditDialog = true
                },
                icon = { Icon(Icons.Default.Add, contentDescription = "Add Expense") },
                text = { Text("Add Expense") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("add_expense_fab")
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .testTag("expenses_screen"),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header Row with Title and Month Selector
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "One-Off Expenses",
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                letterSpacing = (-0.5).sp
                            ),
                            color = MaterialTheme.colorScheme.onBackground
                        )
                        Text(
                            text = "Track non-recurring purchases & shopping",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
                        )
                    }

                    MonthSelector(viewModel = viewModel)
                }
            }

            // Monthly Summary Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expenses_summary_card"),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Text(
                            text = "Purchases in Selected Month",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "₹${String.format("%,.2f", totalMonthSpent)}",
                            style = MaterialTheme.typography.displaySmall.copy(
                                fontWeight = FontWeight.ExtraBold
                            ),
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(14.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "Transactions",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                                Text(
                                    text = "${monthExpenses.size}",
                                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }

                            if (topCategory != null) {
                                Column(horizontalAlignment = Alignment.End) {
                                    Text(
                                        text = "Top Category",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                    )
                                    Text(
                                        text = topCategory,
                                        style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Search Bar
            item {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_search_field"),
                    placeholder = { Text("Search by title, category, or note...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon"
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear search")
                            }
                        }
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp)
                )
            }

            // Category Filter Chips Row
            item {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    item {
                        FilterChip(
                            selected = selectedCategoryFilter == "All",
                            onClick = { selectedCategoryFilter = "All" },
                            label = { Text("All (${monthExpenses.size})") },
                            modifier = Modifier.testTag("expense_filter_All")
                        )
                    }
                    items(EXPENSE_CATEGORIES) { cat ->
                        val count = monthExpenses.count { it.category.equals(cat, ignoreCase = true) }
                        if (count > 0 || selectedCategoryFilter == cat) {
                            FilterChip(
                                selected = selectedCategoryFilter == cat,
                                onClick = { selectedCategoryFilter = cat },
                                label = { Text("$cat ($count)") },
                                modifier = Modifier.testTag("expense_filter_$cat")
                            )
                        }
                    }
                }
            }

            // Filtered Subtotal if filter or search active
            if (selectedCategoryFilter != "All" || searchQuery.isNotEmpty()) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Filtered Total (${filteredExpenses.size} items):",
                            style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
                        )
                        Text(
                            text = "₹${String.format("%,.2f", totalFilteredSpent)}",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Empty State
            if (filteredExpenses.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp)
                            .testTag("expense_empty_card"),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty() || selectedCategoryFilter != "All") {
                                    "No matching expenses found"
                                } else {
                                    "No one-off expenses logged"
                                },
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = if (searchQuery.isNotEmpty() || selectedCategoryFilter != "All") {
                                    "Try clearing filters or search to view more items."
                                } else {
                                    "Tap '+ Add Expense' to record groceries, shopping, dining, or other purchases."
                                },
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            if (searchQuery.isEmpty() && selectedCategoryFilter == "All") {
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(
                                    onClick = {
                                        expenseToEdit = null
                                        showAddEditDialog = true
                                    },
                                    modifier = Modifier.testTag("empty_add_expense_button")
                                ) {
                                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Log First Expense")
                                }
                            }
                        }
                    }
                }
            } else {
                // List of Expenses
                items(filteredExpenses, key = { it.id }) { expense ->
                    ExpenseCardItem(
                        expense = expense,
                        onEdit = {
                            expenseToEdit = expense
                            showAddEditDialog = true
                        },
                        onDelete = {
                            expenseToDelete = expense
                        }
                    )
                }
            }

            // Bottom spacer so FAB doesn't obscure the last card
            item {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Add / Edit Expense Dialog
    if (showAddEditDialog) {
        AddEditExpenseDialog(
            existingExpense = expenseToEdit,
            onDismiss = { showAddEditDialog = false },
            onSave = { title, amount, dateMillis, category, notes ->
                if (expenseToEdit == null) {
                    viewModel.addExpense(
                        title = title,
                        amount = amount,
                        date = dateMillis,
                        category = category,
                        notes = notes
                    )
                } else {
                    val updated = expenseToEdit!!.copy(
                        title = title,
                        amount = amount,
                        date = dateMillis,
                        category = category,
                        notes = notes
                    )
                    viewModel.updateExpense(updated)
                }
                showAddEditDialog = false
            }
        )
    }

    // Delete Confirmation Dialog
    if (expenseToDelete != null) {
        AlertDialog(
            onDismissRequest = { expenseToDelete = null },
            title = {
                Text("Delete Expense?", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
            },
            text = {
                Text("Are you sure you want to delete '${expenseToDelete?.title}' (₹${String.format("%,.2f", expenseToDelete?.amount ?: 0.0)})?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        expenseToDelete?.let { viewModel.deleteExpense(it) }
                        expenseToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_delete_expense_button")
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { expenseToDelete = null },
                    modifier = Modifier.testTag("cancel_delete_expense_button")
                ) {
                    Text("Cancel")
                }
            },
            modifier = Modifier.testTag("delete_expense_dialog")
        )
    }
}

@Composable
fun ExpenseCardItem(
    expense: Expense,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val dateStr = remember(expense.date) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        sdf.format(Date(expense.date))
    }

    val catColor = getExpenseCategoryColor(expense.category)
    val catIcon = getExpenseCategoryIcon(expense.category)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onEdit() }
            .testTag("expense_card_${expense.id}"),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(catColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = catIcon,
                    contentDescription = expense.category,
                    tint = catColor,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Details Column
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = expense.title,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(2.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    // Category Badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(catColor.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = expense.category,
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                            color = catColor
                        )
                    }

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )

                    Text(
                        text = dateStr,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (expense.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = expense.notes,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            // Amount and actions
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "₹${String.format("%,.2f", expense.amount)}",
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("edit_expense_${expense.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(28.dp)
                            .testTag("delete_expense_${expense.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditExpenseDialog(
    existingExpense: Expense?,
    onDismiss: () -> Unit,
    onSave: (title: String, amount: Double, dateMillis: Long, category: String, notes: String) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(existingExpense?.title ?: "") }
    var amountText by remember { mutableStateOf(existingExpense?.amount?.let { if (it % 1.0 == 0.0) it.toLong().toString() else it.toString() } ?: "") }
    var selectedCategory by remember { mutableStateOf(existingExpense?.category ?: EXPENSE_CATEGORIES[0]) }
    var dateMillis by remember { mutableStateOf(existingExpense?.date ?: System.currentTimeMillis()) }
    var notes by remember { mutableStateOf(existingExpense?.notes ?: "") }

    var titleError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    val formattedDate = remember(dateMillis) {
        val sdf = SimpleDateFormat("dd MMMM yyyy", Locale.getDefault())
        sdf.format(Date(dateMillis))
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingExpense == null) "Add Expense" else "Edit Expense",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Title Field
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        if (titleError != null) titleError = null
                    },
                    label = { Text("Item / Description *") },
                    placeholder = { Text("e.g. Weekly Groceries, Fuel, Dinner") },
                    singleLine = true,
                    isError = titleError != null,
                    supportingText = titleError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_title_input"),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences)
                )

                // Amount Field
                OutlinedTextField(
                    value = amountText,
                    onValueChange = {
                        amountText = it
                        if (amountError != null) amountError = null
                    },
                    label = { Text("Amount (₹) *") },
                    placeholder = { Text("0.00") },
                    singleLine = true,
                    leadingIcon = { Text("₹", fontWeight = FontWeight.Bold) },
                    isError = amountError != null,
                    supportingText = amountError?.let { { Text(it, color = MaterialTheme.colorScheme.error) } },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_amount_input")
                )

                // Category Selection
                Column {
                    Text(
                        text = "Category",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(EXPENSE_CATEGORIES) { cat ->
                            val isSelected = selectedCategory == cat
                            FilterChip(
                                selected = isSelected,
                                onClick = { selectedCategory = cat },
                                label = { Text(cat) },
                                leadingIcon = {
                                    Icon(
                                        imageVector = getExpenseCategoryIcon(cat),
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            )
                        }
                    }
                }

                // Date Picker Row
                Column {
                    Text(
                        text = "Date of Purchase",
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                showExpenseDatePicker(context, dateMillis) { selectedMillis ->
                                    dateMillis = selectedMillis
                                }
                            },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("expense_date_button"),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(formattedDate, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        TextButton(
                            onClick = { dateMillis = System.currentTimeMillis() },
                            modifier = Modifier.testTag("expense_date_today_button")
                        ) {
                            Text("Today")
                        }
                    }
                }

                // Notes Field
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes (Optional)") },
                    placeholder = { Text("e.g. Store name, receipt details") },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("expense_notes_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    var hasError = false
                    if (title.isBlank()) {
                        titleError = "Title is required"
                        hasError = true
                    }
                    val amount = amountText.toDoubleOrNull()
                    if (amount == null || amount <= 0) {
                        amountError = "Enter a valid amount (> 0)"
                        hasError = true
                    }

                    if (!hasError && amount != null) {
                        onSave(title.trim(), amount, dateMillis, selectedCategory, notes.trim())
                    }
                },
                modifier = Modifier.testTag("save_expense_button")
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("dismiss_expense_button")
            ) {
                Text("Cancel")
            }
        },
        modifier = Modifier.testTag("add_edit_expense_dialog")
    )
}

private fun showExpenseDatePicker(
    context: Context,
    initialDateMillis: Long,
    onDateSelected: (Long) -> Unit
) {
    val cal = Calendar.getInstance().apply { timeInMillis = initialDateMillis }
    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val resultCal = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onDateSelected(resultCal.timeInMillis)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}
