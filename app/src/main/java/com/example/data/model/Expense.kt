package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "expenses")
data class Expense(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val amount: Double,
    val date: Long, // timestamp of the purchase date (epoch millis)
    val monthYear: String, // "yyyy-MM" for easy monthly grouping and analytics
    val category: String, // e.g. "Groceries", "Dining", "Shopping", "Transport", "Entertainment", "Health", "Bills", "Other"
    val notes: String = ""
)
