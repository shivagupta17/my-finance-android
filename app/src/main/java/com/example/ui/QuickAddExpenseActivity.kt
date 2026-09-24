package com.example.ui

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Launch
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.MainActivity
import com.example.data.db.AppDatabase
import com.example.data.model.Expense
import com.example.ui.theme.MyApplicationTheme
import com.example.widget.AddExpenseWidgetProvider
import com.example.widget.GlanceBillWidgetProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class QuickAddExpenseActivity : ComponentActivity() {

    companion object {
        const val EXTRA_CATEGORY = "extra_preselected_category"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val preselectedCategory = intent.getStringExtra(EXTRA_CATEGORY) ?: "Groceries"

        setContent {
            MyApplicationTheme {
                QuickAddExpenseDialog(
                    initialCategory = preselectedCategory,
                    onDismiss = { finish() },
                    onSaveExpense = { title, amount, category, date, notes ->
                        lifecycleScope.launch(Dispatchers.IO) {
                            val db = AppDatabase.getDatabase(applicationContext)
                            val monthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date(date))
                            val expense = Expense(
                                title = title.ifBlank { category },
                                amount = amount,
                                date = date,
                                monthYear = monthYear,
                                category = category,
                                notes = notes
                            )
                            db.expenseDao().insertExpense(expense)

                            // Notify widgets immediately
                            withContext(Dispatchers.Main) {
                                AddExpenseWidgetProvider.triggerUpdate(applicationContext)
                                GlanceBillWidgetProvider.triggerUpdate(applicationContext)
                                Toast.makeText(
                                    applicationContext,
                                    "Added ₹${String.format(Locale.getDefault(), "%,.2f", amount)} for $category",
                                    Toast.LENGTH_SHORT
                                ).show()
                                finish()
                            }
                        }
                    },
                    onOpenFullApp = {
                        val mainIntent = Intent(this, MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra(MainActivity.EXTRA_TARGET_TAB, "Expenses")
                        }
                        startActivity(mainIntent)
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuickAddExpenseDialog(
    initialCategory: String,
    onDismiss: () -> Unit,
    onSaveExpense: (title: String, amount: Double, category: String, date: Long, notes: String) -> Unit,
    onOpenFullApp: () -> Unit
) {
    var amountText by remember { mutableStateOf("") }
    var titleText by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(initialCategory) }
    var selectedPaymentMethod by remember { mutableStateOf("UPI") }
    var notesText by remember { mutableStateOf("") }
    var showNotesField by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val amountFocusRequester = remember { FocusRequester() }

    val quickSuggestions = listOf("Coffee", "Lunch", "Dinner", "Groceries", "Fuel", "Snacks", "Medicine", "Milk")
    val paymentMethods = listOf("UPI", "Cash", "Credit Card", "Debit Card")

    LaunchedEffect(Unit) {
        // Automatically request focus for amount field
        try {
            amountFocusRequester.requestFocus()
        } catch (e: Exception) {
            // Ignore if layout not ready
        }
    }

    // Outer dim overlay container
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.55f))
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onDismiss
            ),
        contentAlignment = Alignment.BottomCenter
    ) {
        // Floating Sheet Card
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .imePadding()
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = { /* Prevent clicks inside card from dismissing */ }
                ),
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 18.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                // Drag handle bar
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f))
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Header Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Quick Add Expense",
                                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = SimpleDateFormat("EEEE, dd MMM yyyy", Locale.getDefault()).format(Date()),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Big Amount Input Card
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "AMOUNT SPENT",
                            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold, letterSpacing = 1.sp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Text(
                                text = "₹",
                                style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(end = 4.dp)
                            )
                            TextField(
                                value = amountText,
                                onValueChange = { input ->
                                    // Allow numbers with max 2 decimals
                                    if (input.isEmpty() || input.matches(Regex("""^\d*\.?\d{0,2}$"""))) {
                                        amountText = input
                                        if (errorMessage != null) errorMessage = null
                                    }
                                },
                                placeholder = {
                                    Text(
                                        "0.00",
                                        style = MaterialTheme.typography.headlineLarge.copy(fontWeight = FontWeight.Bold),
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                                    )
                                },
                                textStyle = MaterialTheme.typography.headlineLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(
                                    keyboardType = KeyboardType.Decimal,
                                    imeAction = ImeAction.Next
                                ),
                                singleLine = true,
                                colors = TextFieldDefaults.colors(
                                    focusedContainerColor = Color.Transparent,
                                    unfocusedContainerColor = Color.Transparent,
                                    focusedIndicatorColor = Color.Transparent,
                                    unfocusedIndicatorColor = Color.Transparent
                                ),
                                modifier = Modifier
                                    .width(IntrinsicSize.Min)
                                    .focusRequester(amountFocusRequester)
                                    .testTag("input_quick_amount")
                            )
                        }
                    }
                }

                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 6.dp)
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Title / Description input
                OutlinedTextField(
                    value = titleText,
                    onValueChange = { titleText = it },
                    label = { Text("What did you buy? (Optional)") },
                    placeholder = { Text("e.g. Milk & eggs, Lunch, Coffee") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("input_quick_title"),
                    shape = RoundedCornerShape(14.dp)
                )

                // Quick suggestions chips
                Spacer(modifier = Modifier.height(8.dp))
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(quickSuggestions) { suggestion ->
                        SuggestionChip(
                            onClick = {
                                titleText = suggestion
                                // Auto-assign category for convenience
                                when (suggestion) {
                                    "Coffee", "Lunch", "Dinner", "Snacks" -> selectedCategory = "Dining & Food"
                                    "Groceries", "Milk" -> selectedCategory = "Groceries"
                                    "Fuel" -> selectedCategory = "Transport"
                                    "Medicine" -> selectedCategory = "Health"
                                }
                            },
                            label = { Text(suggestion, style = MaterialTheme.typography.labelSmall) },
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Category Selection
                Text(
                    text = "Category",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(EXPENSE_CATEGORIES) { cat ->
                        val isSelected = selectedCategory == cat
                        val catColor = getExpenseCategoryColor(cat)
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedCategory = cat },
                            leadingIcon = {
                                Icon(
                                    imageVector = getExpenseCategoryIcon(cat),
                                    contentDescription = null,
                                    tint = if (isSelected) MaterialTheme.colorScheme.onPrimary else catColor,
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            label = { Text(cat, style = MaterialTheme.typography.labelMedium) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = MaterialTheme.colorScheme.primary,
                                selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(10.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Payment Method Selection
                Text(
                    text = "Payment Method",
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    paymentMethods.forEach { method ->
                        val isSelected = selectedPaymentMethod == method
                        FilterChip(
                            selected = isSelected,
                            onClick = { selectedPaymentMethod = method },
                            label = { Text(method, style = MaterialTheme.typography.labelSmall) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp)
                        )
                    }
                }

                // Optional Notes Toggle
                if (!showNotesField) {
                    TextButton(
                        onClick = { showNotesField = true },
                        modifier = Modifier.align(Alignment.Start),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Add Notes / Location", style = MaterialTheme.typography.labelSmall)
                    }
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = notesText,
                        onValueChange = { notesText = it },
                        label = { Text("Notes (optional)") },
                        placeholder = { Text("e.g. Paid via PhonePe, Starbuck's downtown") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Save Expense Button
                Button(
                    onClick = {
                        val amount = amountText.toDoubleOrNull()
                        if (amount == null || amount <= 0) {
                            errorMessage = "Please enter a valid expense amount"
                            return@Button
                        }
                        val fullNotes = if (notesText.isNotBlank()) {
                            "Paid via $selectedPaymentMethod • $notesText"
                        } else {
                            "Paid via $selectedPaymentMethod"
                        }
                        onSaveExpense(
                            titleText.trim(),
                            amount,
                            selectedCategory,
                            System.currentTimeMillis(),
                            fullNotes
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("btn_save_quick_expense"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Icon(imageVector = Icons.Default.Check, contentDescription = null, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Save Expense",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Open full app button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    TextButton(onClick = onOpenFullApp) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Launch,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Open Expense Manager & Analytics",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
