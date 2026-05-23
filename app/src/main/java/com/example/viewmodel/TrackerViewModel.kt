package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.db.AppDatabase
import com.example.data.model.Bill
import com.example.data.model.Subscription
import com.example.data.model.SubscriptionPayment
import com.example.data.model.BillPayment
import com.example.data.repository.TrackerRepository
import com.example.receiver.ReminderScheduler
import kotlinx.coroutines.flow.MutableStateFlow
import org.json.JSONObject
import org.json.JSONArray
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// Custom combined item for chronological upcoming payments list
data class UpcomingPaymentItem(
    val id: Int,
    val name: String,
    val amount: Double,
    val daysRemaining: Int,
    val isOverdue: Boolean,
    val itemType: String, // "BILL" or "SUBSCRIPTION"
    val extraInfo: String, // Bill category or Subscription payment source
    val parentItem: Any // Reference to original object
)

data class MonthlyScheduleItem(
    val id: Int,
    val name: String,
    val amount: Double,
    val isPaid: Boolean,
    val dueText: String, // e.g., "Due in 3 days", "Paid on 15 May"
    val itemType: String, // "BILL" or "SUBSCRIPTION"
    val category: String,
    val extraInfo: String,
    val parentItem: Any
)

class TrackerViewModel(application: Application) : AndroidViewModel(application) {
    private val repository: TrackerRepository
    val context = application.applicationContext

    private val _selectedMonthYear = MutableStateFlow(
        SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
    )
    val selectedMonthYear = _selectedMonthYear.asStateFlow()

    fun getMonthYearOptions(): List<Pair<String, String>> {
        val list = mutableListOf<Pair<String, String>>()
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -6)
        val valueSdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        val displaySdf = SimpleDateFormat("MMMM yyyy", Locale.getDefault())
        for (i in 0..12) {
            list.add(Pair(valueSdf.format(cal.time), displaySdf.format(cal.time)))
            cal.add(Calendar.MONTH, 1)
        }
        return list
    }

    fun selectMonthYear(monthYear: String) {
        _selectedMonthYear.value = monthYear
    }

    init {
        val database = AppDatabase.getDatabase(application)
        repository = TrackerRepository(
            database.billDao(),
            database.subscriptionDao(),
            database.subscriptionPaymentDao(),
            database.billPaymentDao()
        )
    }

    // Raw database flows
    val bills: StateFlow<List<Bill>> = repository.allBills
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val subscriptions: StateFlow<List<Subscription>> = repository.allSubscriptions
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val payments: StateFlow<List<SubscriptionPayment>> = repository.allPayments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val billPayments: StateFlow<List<BillPayment>> = repository.allBillPayments
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // Combined Flow for upcoming payments dashboard lists, filtered by selectedMonthYear, chronologically sorted
    val upcomingPayments: StateFlow<List<UpcomingPaymentItem>> = combine(
        bills, 
        subscriptions,
        payments,
        selectedMonthYear
    ) { billList, subList, paymentList, monthYear ->
        val items = mutableListOf<UpcomingPaymentItem>()
        val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
        
        // Map Bills (only unpaid and due in the selected month)
        billList.forEach { bill ->
            if (bill.isDueInMonthYear(monthYear) && !bill.isPaidForMonthYear(monthYear)) {
                val days = bill.daysRemaining()
                items.add(
                    UpcomingPaymentItem(
                        id = bill.id,
                        name = bill.name,
                        amount = bill.amount,
                        daysRemaining = days,
                        isOverdue = days < 0 && monthYear == currentMonthYear,
                        itemType = "BILL",
                        extraInfo = bill.category,
                        parentItem = bill
                    )
                )
            }
        }
        
        // Map Subscriptions (only those that are due/renewing in the selected month, and not yet paid for this cycle)
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        subList.forEach { sub ->
            if (sub.isActive && sub.isDueInMonthYear(monthYear)) {
                val isPaid = paymentList.any { pay ->
                    pay.subscriptionId == sub.id && sdf.format(Date(pay.paymentDate)) == monthYear
                }
                if (!isPaid) {
                    val days = sub.daysRemaining()
                    val isOverdue = System.currentTimeMillis() > sub.renewalDate && monthYear == currentMonthYear
                    items.add(
                        UpcomingPaymentItem(
                            id = sub.id,
                            name = sub.name,
                            amount = sub.amount,
                            daysRemaining = days,
                            isOverdue = isOverdue,
                            itemType = "SUBS_RENEWAL",
                            extraInfo = sub.paymentSource,
                            parentItem = sub
                        )
                    )
                }
            }
        }
        
        // Sort: overdue first, then soonest remaining due days
        items.sortedWith(compareBy<UpcomingPaymentItem> { if (it.isOverdue) -1000 + it.daysRemaining else it.daysRemaining })
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Combined Flow for completed payments in selectedMonthYear
    val completedPayments: StateFlow<List<UpcomingPaymentItem>> = combine(
        bills, 
        subscriptions,
        payments,
        selectedMonthYear
    ) { billList, subList, paymentList, monthYear ->
        val items = mutableListOf<UpcomingPaymentItem>()
        
        // Map Paid Bills
        billList.forEach { bill ->
            if (bill.isDueInMonthYear(monthYear) && bill.isPaidForMonthYear(monthYear)) {
                items.add(
                    UpcomingPaymentItem(
                        id = bill.id,
                        name = bill.name,
                        amount = bill.amount,
                        daysRemaining = 0,
                        isOverdue = false,
                        itemType = "BILL",
                        extraInfo = bill.category,
                        parentItem = bill
                    )
                )
            }
        }
        
        // Map Renewed Subscriptions
        val sdf = SimpleDateFormat("yyyy-MM", Locale.getDefault())
        subList.forEach { sub ->
            if (sub.isDueInMonthYear(monthYear)) {
                val isPaid = paymentList.any { pay ->
                    pay.subscriptionId == sub.id && sdf.format(Date(pay.paymentDate)) == monthYear
                }
                if (isPaid) {
                    items.add(
                        UpcomingPaymentItem(
                            id = sub.id,
                            name = sub.name,
                            amount = sub.amount,
                            daysRemaining = 0,
                            isOverdue = false,
                            itemType = "SUBS_RENEWAL",
                            extraInfo = sub.paymentSource,
                            parentItem = sub
                        )
                    )
                }
            }
        }
        
        items.sortedBy { it.name }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Stats flows
    val monthlyBillsTotal: StateFlow<Double> = combine(bills, selectedMonthYear) { billList, monthYear ->
        billList.filter { it.isDueInMonthYear(monthYear) }.map { it.amount }.sum()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val monthlySubscriptionsTotal: StateFlow<Double> = subscriptions.map { subList ->
        subList.filter { it.isActive }.map {
            when (it.billingCycle) {
                "Yearly" -> it.amount / 12.0
                "Quarterly" -> it.amount / 3.0
                else -> it.amount
            }
        }.sum()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    val outstandingBillsTotal: StateFlow<Double> = combine(bills, selectedMonthYear) { billList, monthYear ->
        billList.filter { it.isDueInMonthYear(monthYear) && !it.isPaidForMonthYear(monthYear) }.map { it.amount }.sum()
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    // Grouping stats
    val subscriptionsBySource: StateFlow<Map<String, Double>> = subscriptions.map { subList ->
        subList.filter { it.isActive }
            .groupBy { it.paymentSource.ifBlank { "Unspecified Source" } }
            .mapValues { entry ->
                entry.value.map {
                    when (it.billingCycle) {
                        "Yearly" -> it.amount / 12.0
                        "Quarterly" -> it.amount / 3.0
                        else -> it.amount
                    }
                }.sum()
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val subscriptionsByPlatform: StateFlow<Map<String, Double>> = subscriptions.map { subList ->
        subList.filter { it.isActive }
            .groupBy { it.platform.ifBlank { "Direct" } }
            .mapValues { entry ->
                entry.value.map {
                    when (it.billingCycle) {
                        "Yearly" -> it.amount / 12.0
                        "Quarterly" -> it.amount / 3.0
                        else -> it.amount
                    }
                }.sum()
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())

    val billsByMockCategory: StateFlow<Map<String, Double>> = bills.map { billList ->
        billList.groupBy { it.category }
            .mapValues { entry ->
                entry.value.map { it.amount }.sum()
            }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())


    // BILL EVENTS
    fun addBill(name: String, amount: Double, category: String, dueDay: Int, reminderDays: Int, notes: String, billingCycle: String = "Monthly", startMonthYear: String = "2026-05", isVariable: Boolean = false) {
        viewModelScope.launch {
            val bill = Bill(
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
            val id = repository.insertBill(bill)
            // Schedule the alarm
            val createdBill = bill.copy(id = id.toInt())
            ReminderScheduler.scheduleBillReminder(context, createdBill)
        }
    }

    fun updateBill(bill: Bill) {
        viewModelScope.launch {
            repository.updateBill(bill)
            // Reschedule alarm
            ReminderScheduler.cancelBillReminder(context, bill)
            ReminderScheduler.scheduleBillReminder(context, bill)
        }
    }

    fun deleteBill(bill: Bill) {
        viewModelScope.launch {
            repository.deleteBill(bill)
            ReminderScheduler.cancelBillReminder(context, bill)
        }
    }

    fun toggleBillPaid(bill: Bill, monthYear: String, customAmount: Double? = null) {
        viewModelScope.launch {
            val isCurrentlyPaid = bill.isPaidForMonthYear(monthYear)
            
            val updatedPaidMonths = if (isCurrentlyPaid) {
                // Mark unpaid by removing current month
                val list = bill.paidMonths.split(",").toMutableList()
                list.remove(monthYear)
                list.filter { it.isNotEmpty() }.joinToString(",")
            } else {
                // Mark paid by adding current month
                if (bill.paidMonths.isEmpty()) monthYear else "${bill.paidMonths},$monthYear"
            }
            
            val updatedBill = bill.copy(paidMonths = updatedPaidMonths)
            repository.updateBill(updatedBill)
            
            if (isCurrentlyPaid) {
                repository.deleteBillPaymentByBillIdAndMonth(bill.id, monthYear)
            } else {
                val finalAmount = customAmount ?: bill.amount
                val payment = BillPayment(
                    billId = bill.id,
                    billName = bill.name,
                    amount = finalAmount,
                    paymentDate = System.currentTimeMillis(),
                    monthYear = monthYear,
                    category = bill.category
                )
                repository.insertBillPayment(payment)
            }
            
            // Reschedule reminders for upcoming cycle if paid
            ReminderScheduler.cancelBillReminder(context, updatedBill)
            ReminderScheduler.scheduleBillReminder(context, updatedBill)
        }
    }


    // SUBSCRIPTION EVENTS
    fun addSubscription(
        name: String,
        amount: Double,
        billingCycle: String,
        source: String,
        platform: String,
        category: String,
        renewalDate: Long,
        isAutoNotify: Boolean,
        reminderDays: Int,
        notes: String
    ) {
        viewModelScope.launch {
            val subscription = Subscription(
                name = name,
                amount = amount,
                billingCycle = billingCycle,
                paymentSource = source,
                platform = platform,
                category = category,
                renewalDate = renewalDate,
                isAutoNotify = isAutoNotify,
                customReminderDaysBefore = reminderDays,
                notes = notes
            )
            val id = repository.insertSubscription(subscription)
            val createdSub = subscription.copy(id = id.toInt())
            if (createdSub.isActive && createdSub.isAutoNotify) {
                ReminderScheduler.scheduleSubscriptionReminder(context, createdSub)
            }
        }
    }

    fun updateSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.updateSubscription(subscription)
            ReminderScheduler.cancelSubscriptionReminder(context, subscription)
            if (subscription.isActive && subscription.isAutoNotify) {
                ReminderScheduler.scheduleSubscriptionReminder(context, subscription)
            }
        }
    }

    fun deleteSubscription(subscription: Subscription) {
        viewModelScope.launch {
            repository.deleteSubscription(subscription)
            ReminderScheduler.cancelSubscriptionReminder(context, subscription)
        }
    }

    fun renewSubscription(subscription: Subscription, selectedMonthYear: String) {
        viewModelScope.launch {
            val currentMonthYear = SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date())
            val paymentTimeMillis = if (selectedMonthYear == currentMonthYear) {
                System.currentTimeMillis()
            } else {
                val cal = Calendar.getInstance()
                val parts = selectedMonthYear.split("-")
                if (parts.size == 2) {
                    val year = parts[0].toIntOrNull() ?: cal.get(Calendar.YEAR)
                    val month = (parts[1].toIntOrNull() ?: (cal.get(Calendar.MONTH) + 1)) - 1
                    cal.set(Calendar.YEAR, year)
                    cal.set(Calendar.MONTH, month)
                    val subCal = Calendar.getInstance().apply { timeInMillis = subscription.renewalDate }
                    cal.set(Calendar.DAY_OF_MONTH, subCal.get(Calendar.DAY_OF_MONTH))
                }
                cal.timeInMillis
            }

            val nextRenewal = subscription.getNextRenewalDate()
            val updatedSub = subscription.copy(renewalDate = nextRenewal)
            repository.updateSubscription(updatedSub)
            
            // Record this payment in the history!
            val payment = SubscriptionPayment(
                subscriptionId = subscription.id,
                subscriptionName = subscription.name,
                amount = subscription.amount,
                paymentDate = paymentTimeMillis,
                billingCycle = subscription.billingCycle,
                paymentSource = subscription.paymentSource,
                platform = subscription.platform,
                category = subscription.category
            )
            repository.insertPayment(payment)
            
            // Cancel old reminder, schedule for the next interval
            ReminderScheduler.cancelSubscriptionReminder(context, updatedSub)
            if (updatedSub.isActive && updatedSub.isAutoNotify) {
                ReminderScheduler.scheduleSubscriptionReminder(context, updatedSub)
            }
        }
    }

    fun toggleSubscriptionActive(subscription: Subscription) {
        viewModelScope.launch {
            val updatedSub = subscription.copy(isActive = !subscription.isActive)
            repository.updateSubscription(updatedSub)
            
            if (updatedSub.isActive) {
                if (updatedSub.isAutoNotify) {
                    ReminderScheduler.scheduleSubscriptionReminder(context, updatedSub)
                }
            } else {
                ReminderScheduler.cancelSubscriptionReminder(context, subscription)
            }
        }
    }

    fun deletePayment(payment: SubscriptionPayment) {
        viewModelScope.launch {
            repository.deletePaymentById(payment.id)
        }
    }

    fun deleteBillPayment(payment: BillPayment) {
        viewModelScope.launch {
            repository.deleteBillPaymentById(payment.id)
            // Unmark it in parent Bill
            val bill = repository.getBillById(payment.billId)
            if (bill != null && bill.isPaidForMonthYear(payment.monthYear)) {
                val list = bill.paidMonths.split(",").toMutableList()
                list.remove(payment.monthYear)
                val updatedMonths = list.filter { it.isNotEmpty() }.joinToString(",")
                repository.updateBill(bill.copy(paidMonths = updatedMonths))
            }
        }
    }

    fun addHistoricalBillPayment(bill: Bill, monthYear: String, amount: Double, paymentDate: Long) {
        viewModelScope.launch {
            val isCurrentlyPaid = bill.isPaidForMonthYear(monthYear)
            if (!isCurrentlyPaid) {
                val updatedPaidMonths = if (bill.paidMonths.isEmpty()) monthYear else "${bill.paidMonths},$monthYear"
                repository.updateBill(bill.copy(paidMonths = updatedPaidMonths))
            }
            val payment = BillPayment(
                billId = bill.id,
                billName = bill.name,
                amount = amount,
                paymentDate = paymentDate,
                monthYear = monthYear,
                category = bill.category
            )
            repository.insertBillPayment(payment)
        }
    }

    fun addHistoricalSubscriptionPayment(subscription: Subscription, amount: Double, paymentDate: Long) {
        viewModelScope.launch {
            val payment = SubscriptionPayment(
                subscriptionId = subscription.id,
                subscriptionName = subscription.name,
                amount = amount,
                paymentDate = paymentDate,
                billingCycle = subscription.billingCycle,
                paymentSource = subscription.paymentSource,
                platform = subscription.platform,
                category = subscription.category
            )
            repository.insertPayment(payment)
        }
    }

    fun exportDataToJson(): String {
        val backupObj = JSONObject()
        backupObj.put("version", 1)
        
        // Bills
        val billsArray = JSONArray()
        bills.value.forEach { bill ->
            val obj = JSONObject().apply {
                put("id", bill.id)
                put("name", bill.name)
                put("amount", bill.amount)
                put("category", bill.category)
                put("dueDay", bill.dueDay)
                put("customReminderDaysBefore", bill.customReminderDaysBefore)
                put("paidMonths", bill.paidMonths)
                put("notes", bill.notes)
                put("billingCycle", bill.billingCycle)
                put("startMonthYear", bill.startMonthYear)
                put("isVariable", bill.isVariable)
            }
            billsArray.put(obj)
        }
        backupObj.put("bills", billsArray)
        
        // Subscriptions
        val subsArray = JSONArray()
        subscriptions.value.forEach { sub ->
            val obj = JSONObject().apply {
                put("id", sub.id)
                put("name", sub.name)
                put("amount", sub.amount)
                put("billingCycle", sub.billingCycle)
                put("paymentSource", sub.paymentSource)
                put("platform", sub.platform)
                put("category", sub.category)
                put("renewalDate", sub.renewalDate)
                put("isAutoNotify", sub.isAutoNotify)
                put("customReminderDaysBefore", sub.customReminderDaysBefore)
                put("isActive", sub.isActive)
                put("notes", sub.notes)
            }
            subsArray.put(obj)
        }
        backupObj.put("subscriptions", subsArray)
        
        // Bill Payments
        val billPaymentsArray = JSONArray()
        billPayments.value.forEach { bp ->
            val obj = JSONObject().apply {
                put("id", bp.id)
                put("billId", bp.billId)
                put("billName", bp.billName)
                put("amount", bp.amount)
                put("paymentDate", bp.paymentDate)
                put("monthYear", bp.monthYear)
                put("category", bp.category)
            }
            billPaymentsArray.put(obj)
        }
        backupObj.put("billPayments", billPaymentsArray)
        
        // Subscription Payments
        val subPaymentsArray = JSONArray()
        payments.value.forEach { sp ->
            val obj = JSONObject().apply {
                put("id", sp.id)
                put("subscriptionId", sp.subscriptionId)
                put("subscriptionName", sp.subscriptionName)
                put("amount", sp.amount)
                put("paymentDate", sp.paymentDate)
                put("billingCycle", sp.billingCycle)
                put("paymentSource", sp.paymentSource)
                put("platform", sp.platform)
                put("category", sp.category)
            }
            subPaymentsArray.put(obj)
        }
        backupObj.put("subscriptionPayments", subPaymentsArray)
        
        return backupObj.toString(4)
    }

    fun importDataFromJson(jsonString: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            try {
                val backupObj = JSONObject(jsonString)
                if (!backupObj.has("version")) {
                    onError("Invalid backup file: Missing version indicator.")
                    return@launch
                }
                
                // First cancel all existing notifications
                bills.value.forEach { bill ->
                    ReminderScheduler.cancelBillReminder(context, bill)
                }
                subscriptions.value.forEach { sub ->
                    ReminderScheduler.cancelSubscriptionReminder(context, sub)
                }
                
                // Clear existing databases
                repository.clearAllData()
                
                // Parse and insert Bills
                val billsArray = backupObj.optJSONArray("bills")
                if (billsArray != null) {
                    for (i in 0 until billsArray.length()) {
                        val obj = billsArray.getJSONObject(i)
                        val bill = Bill(
                            id = obj.getInt("id"),
                            name = obj.getString("name"),
                            amount = obj.getDouble("amount"),
                            category = obj.getString("category"),
                            dueDay = obj.getInt("dueDay"),
                            customReminderDaysBefore = obj.optInt("customReminderDaysBefore", 1),
                            paidMonths = obj.optString("paidMonths", ""),
                            notes = obj.optString("notes", ""),
                            billingCycle = obj.optString("billingCycle", "Monthly"),
                            startMonthYear = if (obj.has("startMonthYear")) obj.getString("startMonthYear") else SimpleDateFormat("yyyy-MM", Locale.getDefault()).format(Date()),
                            isVariable = obj.optBoolean("isVariable", false)
                        )
                        repository.insertBill(bill)
                        ReminderScheduler.scheduleBillReminder(context, bill)
                    }
                }
                
                // Parse and insert Subscriptions
                val subsArray = backupObj.optJSONArray("subscriptions")
                if (subsArray != null) {
                    for (i in 0 until subsArray.length()) {
                        val obj = subsArray.getJSONObject(i)
                        val sub = Subscription(
                            id = obj.getInt("id"),
                            name = obj.getString("name"),
                            amount = obj.getDouble("amount"),
                            billingCycle = obj.getString("billingCycle"),
                            paymentSource = obj.getString("paymentSource"),
                            platform = obj.getString("platform"),
                            category = obj.getString("category"),
                            renewalDate = obj.getLong("renewalDate"),
                            isAutoNotify = obj.optBoolean("isAutoNotify", true),
                            customReminderDaysBefore = obj.optInt("customReminderDaysBefore", 2),
                            isActive = obj.optBoolean("isActive", true),
                            notes = obj.optString("notes", "")
                        )
                        repository.insertSubscription(sub)
                        if (sub.isActive && sub.isAutoNotify) {
                            ReminderScheduler.scheduleSubscriptionReminder(context, sub)
                        }
                    }
                }
                
                // Parse and insert Bill Payments
                val billPaymentsArray = backupObj.optJSONArray("billPayments")
                if (billPaymentsArray != null) {
                    for (i in 0 until billPaymentsArray.length()) {
                        val obj = billPaymentsArray.getJSONObject(i)
                        val bp = BillPayment(
                            id = obj.getInt("id"),
                            billId = obj.getInt("billId"),
                            billName = obj.getString("billName"),
                            amount = obj.getDouble("amount"),
                            paymentDate = obj.getLong("paymentDate"),
                            monthYear = obj.getString("monthYear"),
                            category = obj.getString("category")
                        )
                        repository.insertBillPayment(bp)
                    }
                }
                
                // Parse and insert Subscription Payments
                val subPaymentsArray = backupObj.optJSONArray("subscriptionPayments")
                if (subPaymentsArray != null) {
                    for (i in 0 until subPaymentsArray.length()) {
                        val obj = subPaymentsArray.getJSONObject(i)
                        val sp = SubscriptionPayment(
                            id = obj.getInt("id"),
                            subscriptionId = obj.getInt("subscriptionId"),
                            subscriptionName = obj.getString("subscriptionName"),
                            amount = obj.getDouble("amount"),
                            paymentDate = obj.getLong("paymentDate"),
                            billingCycle = obj.getString("billingCycle"),
                            paymentSource = obj.getString("paymentSource"),
                            platform = obj.getString("platform"),
                            category = obj.getString("category")
                        )
                        repository.insertPayment(sp)
                    }
                }
                
                onSuccess()
            } catch (e: Exception) {
                onError(e.localizedMessage ?: "Failed to parse JSON backup.")
            }
        }
    }

    fun clearAllData() {
        viewModelScope.launch {
            // Cancel reminders for current bills
            bills.value.forEach { bill ->
                ReminderScheduler.cancelBillReminder(context, bill)
            }
            // Cancel reminders for current subscriptions
            subscriptions.value.forEach { sub ->
                ReminderScheduler.cancelSubscriptionReminder(context, sub)
            }
            // Delete all tables from database
            repository.clearAllData()
        }
    }
}
