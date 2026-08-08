package com.example.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.util.Calendar
import java.util.Locale

@Composable
fun MonthYearPickerDialog(
    initialMonthYear: String? = null, // "yyyy-MM" format
    onDismissRequest: () -> Unit,
    onMonthYearSelected: (monthYear: String, label: String) -> Unit
) {
    val currentCal = remember { Calendar.getInstance() }
    val defaultYear = currentCal.get(Calendar.YEAR)
    val defaultMonth = currentCal.get(Calendar.MONTH) // 0-based

    // Parse initialMonthYear if valid
    val (startYear, startMonth) = remember(initialMonthYear) {
        if (!initialMonthYear.isNullOrBlank() && initialMonthYear.contains("-")) {
            val parts = initialMonthYear.split("-")
            val y = parts.getOrNull(0)?.toIntOrNull() ?: defaultYear
            val m = (parts.getOrNull(1)?.toIntOrNull() ?: (defaultMonth + 1)) - 1
            Pair(y, m.coerceIn(0, 11))
        } else {
            Pair(defaultYear, defaultMonth)
        }
    }

    var selectedYear by remember { mutableStateOf(startYear) }
    var selectedMonth by remember { mutableStateOf(startMonth) }

    val monthNames = remember {
        listOf(
            "Jan", "Feb", "Mar", "Apr",
            "May", "Jun", "Jul", "Aug",
            "Sep", "Oct", "Nov", "Dec"
        )
    }

    val monthFullNames = remember {
        listOf(
            "January", "February", "March", "April",
            "May", "June", "July", "August",
            "September", "October", "November", "December"
        )
    }

    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Select Month & Year",
                    style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold)
                )
                TextButton(
                    onClick = {
                        selectedYear = defaultYear
                        selectedMonth = defaultMonth
                    },
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Today,
                        contentDescription = "Current Month",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Today", style = MaterialTheme.typography.labelMedium)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Year Header Control
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = { selectedYear -= 1 },
                            modifier = Modifier.testTag("month_picker_prev_year")
                        ) {
                            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous Year")
                        }

                        Text(
                            text = "$selectedYear",
                            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )

                        IconButton(
                            onClick = { selectedYear += 1 },
                            modifier = Modifier.testTag("month_picker_next_year")
                        ) {
                            Icon(Icons.Default.ChevronRight, contentDescription = "Next Year")
                        }
                    }
                }

                // Month Grid (3 columns, 4 rows)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(3),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    itemsIndexed(monthNames) { index, shortName ->
                        val isSelected = (index == selectedMonth)
                        val isCurrentMonthAndYear = (index == defaultMonth && selectedYear == defaultYear)

                        Card(
                            onClick = { selectedMonth = index },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) {
                                    MaterialTheme.colorScheme.primary
                                } else if (isCurrentMonthAndYear) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surface
                                },
                                contentColor = if (isSelected) {
                                    MaterialTheme.colorScheme.onPrimary
                                } else if (isCurrentMonthAndYear) {
                                    MaterialTheme.colorScheme.onPrimaryContainer
                                } else {
                                    MaterialTheme.colorScheme.onSurface
                                }
                            ),
                            border = if (!isSelected && isCurrentMonthAndYear) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                            } else if (!isSelected) {
                                BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                            } else null,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .aspectRatio(1.5f)
                                .testTag("month_grid_item_$index")
                        ) {
                            Box(
                                contentAlignment = Alignment.Center,
                                modifier = Modifier.fillMaxSize()
                            ) {
                                Text(
                                    text = shortName,
                                    style = MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = if (isSelected || isCurrentMonthAndYear) FontWeight.Bold else FontWeight.Normal
                                    ),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val formattedMonthYear = String.format(Locale.US, "%04d-%02d", selectedYear, selectedMonth + 1)
                    val formattedLabel = "${monthFullNames[selectedMonth]} $selectedYear"
                    onMonthYearSelected(formattedMonthYear, formattedLabel)
                    onDismissRequest()
                },
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.testTag("month_picker_confirm_btn")
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismissRequest,
                modifier = Modifier.testTag("month_picker_cancel_btn")
            ) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(20.dp)
    )
}
