package com.paytrack.viewmodel

import android.content.Context
import android.content.Intent
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.CreationExtras
import com.paytrack.data.Category
import com.paytrack.data.FinanceRepository
import com.paytrack.data.PaymentTransaction
import com.paytrack.payment.PendingPayment
import com.paytrack.payment.ParsedUpiQr
import com.paytrack.payment.UpiAppResolver
import com.paytrack.payment.UpiQrParser
import java.util.Calendar
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PENDING_PAYMENT_KEY = "pending_payment_json"
private const val SCANNED_QR_KEY = "scanned_qr_value"
private const val AMOUNT_INPUT_KEY = "amount_input"
private const val SELECTED_FOLDER_KEY = "selected_folder_id"

class HomeViewModel(
    private val repository: FinanceRepository,
    private val appContext: Context,
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _insightsUiState = MutableStateFlow(InsightsUiState())
    val insightsUiState: StateFlow<InsightsUiState> = _insightsUiState.asStateFlow()

    private val _qrUiState = MutableStateFlow(QrScanUiState())
    val qrUiState: StateFlow<QrScanUiState> = _qrUiState.asStateFlow()

    private val currencyFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))
    private val monthFormatter = SimpleDateFormat("MMMM yyyy", Locale.ENGLISH)
    private val timeFormatter = SimpleDateFormat("dd MMM, hh:mm a", Locale.ENGLISH)

    init {
        loadInitialData()
    }

    private fun loadInitialData() {
        viewModelScope.launch {
            repository.ensureDefaultCategories()
            combine(
                repository.getCategories(),
                repository.getTransactions()
            ) { categories, transactions ->
                categories to transactions
            }.collect { (categories, transactions) ->
                updateHomeState(categories, transactions)
                updateInsightsState(categories, transactions)
                syncQrState(categories)
            }
        }
    }

    fun updateTransactionBarPeriod(period: TransactionBarPeriod) {
        _insightsUiState.update { currentState ->
            currentState.copy(selectedTransactionBarPeriod = period)
        }
    }

    fun createCategory(name: String, amount: Double) {
        viewModelScope.launch {
            repository.createCategory(
                name = name.trim(),
                amount = amount
            )
        }
    }

    fun updateCategory(id: String, name: String, amount: Double) {
        viewModelScope.launch {
            repository.updateCategory(
                id = id,
                name = name.trim(),
                amount = amount
            )
        }
    }

    fun deleteCategory(id: String) {
        viewModelScope.launch {
            repository.deleteCategory(id)
        }
    }

    fun getCategory(categoryId: String?): BudgetCategoryUiState? {
        if (categoryId.isNullOrBlank()) return null
        return _uiState.value.folders.firstOrNull { it.id == categoryId }
    }

    fun setCameraPermission(granted: Boolean) {
        _qrUiState.update { it.copy(hasCameraPermission = granted) }
    }

    fun onQrScanned(rawValue: String) {
        val parsed = UpiQrParser.parse(rawValue)
        if (parsed == null) {
            _qrUiState.update {
                it.copy(
                    scanError = "This QR is not a supported UPI payment code.",
                    paymentError = null
                )
            }
            return
        }

        savedStateHandle[SCANNED_QR_KEY] = rawValue
        if (parsed.amount != null && savedStateHandle.get<String>(AMOUNT_INPUT_KEY).isNullOrBlank()) {
            savedStateHandle[AMOUNT_INPUT_KEY] = parsed.amount.toString()
        }

        _qrUiState.update { currentState ->
            val amountText = savedStateHandle.get<String>(AMOUNT_INPUT_KEY)?.takeUnless { it.isBlank() }
                ?: parsed.amount?.toString().orEmpty()
            currentState.copy(
                scannedMerchantName = parsed.payeeName,
                scannedPayeeVpa = parsed.payeeVpa,
                scannedNote = parsed.note,
                scannedAmountText = parsed.amount?.let(currencyFormatter::format).orEmpty(),
                amountInput = amountText,
                isAmountLocked = parsed.hasEmbeddedAmount,
                scanError = null,
                paymentError = null
            )
        }
        refreshQrDerivedState()
    }

    fun clearScannedQr() {
        savedStateHandle[SCANNED_QR_KEY] = null
        savedStateHandle[AMOUNT_INPUT_KEY] = null
        savedStateHandle[PENDING_PAYMENT_KEY] = null
        _qrUiState.update {
            it.copy(
                scannedMerchantName = "",
                scannedPayeeVpa = "",
                scannedNote = null,
                scannedAmountText = "",
                amountInput = "",
                isAmountLocked = false,
                pendingConfirmation = null,
                scanError = null,
                paymentError = null,
                canLaunchPayment = false
            )
        }
        refreshQrDerivedState()
    }

    fun updateSelectedFolder(folderId: String) {
        savedStateHandle[SELECTED_FOLDER_KEY] = folderId
        refreshQrDerivedState()
    }

    fun updateAmountInput(value: String) {
        if (_qrUiState.value.isAmountLocked) return
        savedStateHandle[AMOUNT_INPUT_KEY] = value
        _qrUiState.update { it.copy(amountInput = value, paymentError = null) }
        refreshQrDerivedState()
    }

    fun refreshInstalledUpiApps() {
        _qrUiState.update {
            it.copy(
                availableUpiApps = UpiAppResolver.resolve(appContext).map(::upiAppToUiState),
                paymentError = null
            )
        }
        refreshQrDerivedState()
    }

    fun buildUpiLaunchIntent(packageName: String): Intent? {
        currentParsedPayload() ?: return markPaymentError("Scan a valid UPI QR code first.")
        selectedFolder() ?: return markPaymentError("Choose a folder before opening a UPI app.")
        enteredAmount() ?: return markPaymentError("Enter a valid amount greater than zero.")
        val selectedApp = _qrUiState.value.availableUpiApps.firstOrNull { it.packageName == packageName }
            ?: return markPaymentError("The selected UPI app is no longer available.")
        return UpiAppResolver.launchIntent(appContext, selectedApp.packageName)
            ?: markPaymentError("Unable to open the selected UPI app on this device.")
    }

    fun recordPaymentWhenUpiAppOpened(packageName: String) {
        val payload = currentParsedPayload() ?: return
        val selectedFolder = selectedFolder() ?: return
        val amount = enteredAmount() ?: return
        val selectedApp = _qrUiState.value.availableUpiApps.firstOrNull { it.packageName == packageName } ?: return

        viewModelScope.launch {
            repository.recordSuccessfulPayment(
                folderId = selectedFolder.id,
                merchantName = payload.payeeName,
                amount = amount,
                upiAppPackage = selectedApp.packageName,
                upiAppLabel = "${selectedApp.label} (Rescan)",
                payeeVpa = payload.payeeVpa,
                note = payload.note
            )
            savedStateHandle[PENDING_PAYMENT_KEY] = null
            savedStateHandle[SCANNED_QR_KEY] = null
            savedStateHandle[AMOUNT_INPUT_KEY] = null
            _qrUiState.update {
                it.copy(
                    scannedMerchantName = "",
                    scannedPayeeVpa = "",
                    scannedNote = null,
                    scannedAmountText = "",
                    amountInput = "",
                    isAmountLocked = false,
                    pendingConfirmation = null,
                    paymentError = null,
                    scanError = null
                )
            }
            refreshQrDerivedState()
        }
    }

    fun startManualPaymentConfirmation() {
        val payload = currentParsedPayload() ?: run {
            markPaymentError("Scan a valid UPI QR code first.")
            return
        }
        val selectedFolder = selectedFolder() ?: run {
            markPaymentError("Choose a folder before recording the payment.")
            return
        }
        val amount = enteredAmount() ?: run {
            markPaymentError("Enter a valid amount greater than zero.")
            return
        }

        val pendingPayment = PendingPayment(
            folderId = selectedFolder.id,
            folderName = selectedFolder.name,
            merchantName = payload.payeeName,
            amount = amount,
            upiAppPackage = "manual.external",
            upiAppLabel = "Manual UPI Payment",
            payeeVpa = payload.payeeVpa,
            note = payload.note,
            rawQrValue = payload.rawValue
        )
        savedStateHandle[PENDING_PAYMENT_KEY] = pendingPayment.toJson()

        _qrUiState.update {
            it.copy(
                pendingConfirmation = PendingConfirmationUiState(
                    merchantName = payload.payeeName,
                    folderName = selectedFolder.name,
                    amount = currencyFormatter.format(amount),
                    appLabel = "Manual UPI Payment"
                ),
                paymentError = null
            )
        }
    }

    fun onUpiLaunchFailed() {
        _qrUiState.update {
            it.copy(paymentError = "Unable to open the selected UPI app on this device.")
        }
    }

    fun confirmPaymentResult(success: Boolean) {
        val pendingPayment = PendingPayment.fromJson(savedStateHandle[PENDING_PAYMENT_KEY])
        if (pendingPayment == null) {
            _qrUiState.update {
                it.copy(paymentError = "No pending payment was found to confirm.")
            }
            return
        }

        if (!success) {
            savedStateHandle[PENDING_PAYMENT_KEY] = null
            _qrUiState.update {
                it.copy(
                    pendingConfirmation = null,
                    paymentError = "Payment was not recorded. Your folder balance is unchanged."
                )
            }
            refreshQrDerivedState()
            return
        }

        viewModelScope.launch {
            repository.recordSuccessfulPayment(
                folderId = pendingPayment.folderId,
                merchantName = pendingPayment.merchantName,
                amount = pendingPayment.amount,
                upiAppPackage = pendingPayment.upiAppPackage,
                upiAppLabel = pendingPayment.upiAppLabel,
                payeeVpa = pendingPayment.payeeVpa,
                note = pendingPayment.note
            )
            savedStateHandle[PENDING_PAYMENT_KEY] = null
            savedStateHandle[SCANNED_QR_KEY] = null
            savedStateHandle[AMOUNT_INPUT_KEY] = null
            _qrUiState.update {
                it.copy(
                    scannedMerchantName = "",
                    scannedPayeeVpa = "",
                    scannedNote = null,
                    scannedAmountText = "",
                    amountInput = "",
                    isAmountLocked = false,
                    pendingConfirmation = null,
                    paymentError = null,
                    scanError = null
                )
            }
            refreshQrDerivedState()
        }
    }

    private fun currentParsedPayload(): ParsedUpiQr? {
        val rawValue: String? = savedStateHandle[SCANNED_QR_KEY]
        return rawValue?.let(UpiQrParser::parse)
    }

    private fun selectedFolder(): FolderPickerUiState? {
        val selectedId = savedStateHandle.get<String>(SELECTED_FOLDER_KEY)
        return _qrUiState.value.folders.firstOrNull { it.id == selectedId }
    }

    private fun enteredAmount(): Double? {
        return savedStateHandle.get<String>(AMOUNT_INPUT_KEY)?.toDoubleOrNull()?.takeIf { it > 0.0 }
    }

    private fun refreshQrDerivedState() {
        val selectedFolder = selectedFolder()
        val amount = enteredAmount()
        val canLaunch = currentParsedPayload() != null &&
            selectedFolder != null &&
            amount != null &&
            _qrUiState.value.availableUpiApps.isNotEmpty()

        _qrUiState.update {
            it.copy(
                amountInput = savedStateHandle.get<String>(AMOUNT_INPUT_KEY).orEmpty(),
                selectedFolderId = selectedFolder?.id,
                selectedFolderBalance = selectedFolder?.balance.orEmpty(),
                projectedBalance = if (selectedFolder != null && amount != null) {
                    currencyFormatter.format(selectedFolder.balance.toCurrencyValue() - amount)
                } else {
                    ""
                },
                canLaunchPayment = canLaunch
            )
        }
    }

    private fun updateHomeState(
        categories: List<Category>,
        transactions: List<PaymentTransaction>
    ) {
        val totalFolderAmount = categories.sumOf(Category::amount)
        val totalSpent = transactions.sumOf(PaymentTransaction::amount)
        val recentTransactions = transactions.take(5).map(::transactionToUiState)

        _uiState.update {
            it.copy(
                appName = "PayTrack",
                monthLabel = monthFormatter.format(Date()),
                availableBudget = currencyFormatter.format(totalFolderAmount),
                spentAmount = currencyFormatter.format(totalSpent),
                budgetProgress = if (categories.isEmpty()) 0f else {
                    (transactions.size.toFloat() / (transactions.size + categories.size).coerceAtLeast(1)).coerceIn(0f, 1f)
                },
                budgetProgressLabel = if (transactions.isEmpty()) {
                    "Start tracking payments from your folders"
                } else {
                    "${transactions.size} payments tracked from folders"
                },
                insights = buildHomeInsights(categories, transactions),
                folderSpendChart = buildFolderSpendChart(categories, transactions),
                isFolderSpendChartEmpty = transactions.none { it.amount > 0.0 },
                highlightCard = HighlightCardUiState(
                    title = if (transactions.isEmpty()) "Ready for your first QR payment" else "UPI payments are being tracked",
                    subtitle = if (transactions.isEmpty()) {
                        "Scan a merchant QR to deduct directly from a folder."
                    } else {
                        "${recentTransactions.firstOrNull()?.title ?: "Recent"} updated your budgets."
                    }
                ),
                folders = categories.map(::categoryToUiState),
                recentTransactions = recentTransactions,
                isLoading = false
            )
        }
    }

    private fun updateInsightsState(
        categories: List<Category>,
        transactions: List<PaymentTransaction>
    ) {
        val groupedByFolder = transactions.groupBy(PaymentTransaction::folderName)
        val topFolderEntry = groupedByFolder.maxByOrNull { entry -> entry.value.sumOf(PaymentTransaction::amount) }
        val chartData = buildInsightsChartData(categories, transactions, System.currentTimeMillis())
        _insightsUiState.update {
            it.copy(
                totalSpent = currencyFormatter.format(transactions.sumOf(PaymentTransaction::amount)),
                transactionCountLabel = "${transactions.size} successful UPI payments",
                mostUsedFolder = topFolderEntry?.key ?: "No folder usage yet",
                mostUsedFolderSpend = topFolderEntry?.value?.sumOf(PaymentTransaction::amount)?.let(currencyFormatter::format)
                    ?: currencyFormatter.format(0),
                weeklyDailyTotals = chartData.weeklyDailyTotals,
                currentMonthDailyTotals = chartData.currentMonthDailyTotals,
                last12MonthTotals = chartData.last12MonthTotals,
                folderTransactionTotals = chartData.folderTransactionTotals,
                recentMerchants = transactions.map(PaymentTransaction::merchantName).distinct().take(5),
                isTransactionChartEmpty = chartData.weeklyDailyTotals.none { point -> point.amount > 0.0 } &&
                    chartData.currentMonthDailyTotals.none { point -> point.amount > 0.0 },
                isMonthlyTrendEmpty = chartData.last12MonthTotals.none { point -> point.amount > 0.0 },
                isFolderChartEmpty = chartData.folderTransactionTotals.none { point -> point.amount > 0.0 },
                isLoading = false
            )
        }
    }

    private fun syncQrState(categories: List<Category>) {
        val folderUi = categories.map {
            FolderPickerUiState(
                id = it.id,
                name = it.name,
                balance = currencyFormatter.format(it.amount)
            )
        }
        val selectedFolderId = savedStateHandle.get<String>(SELECTED_FOLDER_KEY)
        val resolvedSelectedFolderId = selectedFolderId?.takeIf { id -> categories.any { it.id == id } }
            ?: categories.firstOrNull()?.id

        if (resolvedSelectedFolderId != null) {
            savedStateHandle[SELECTED_FOLDER_KEY] = resolvedSelectedFolderId
        }

        val payload = currentParsedPayload()
        val pendingPayment = PendingPayment.fromJson(savedStateHandle[PENDING_PAYMENT_KEY])

        _qrUiState.update {
            it.copy(
                folders = folderUi,
                availableUpiApps = UpiAppResolver.resolve(appContext).map(::upiAppToUiState),
                scannedMerchantName = payload?.payeeName.orEmpty(),
                scannedPayeeVpa = payload?.payeeVpa.orEmpty(),
                scannedNote = payload?.note,
                scannedAmountText = payload?.amount?.let(currencyFormatter::format).orEmpty(),
                amountInput = savedStateHandle.get<String>(AMOUNT_INPUT_KEY)
                    ?: payload?.amount?.toString().orEmpty(),
                isAmountLocked = payload?.hasEmbeddedAmount == true,
                pendingConfirmation = pendingPayment?.let { payment ->
                    PendingConfirmationUiState(
                        merchantName = payment.merchantName,
                        folderName = payment.folderName,
                        amount = currencyFormatter.format(payment.amount),
                        appLabel = payment.upiAppLabel
                    )
                },
                hasCameraPermission = it.hasCameraPermission
            )
        }
        refreshQrDerivedState()
    }

    private fun buildHomeInsights(
        categories: List<Category>,
        transactions: List<PaymentTransaction>
    ): List<InsightCardUiState> {
        val totalFolderAmount = categories.sumOf(Category::amount)
        val spent = transactions.sumOf(PaymentTransaction::amount)
        val topFolder = categories.maxByOrNull(Category::amount)

        return listOf(
            InsightCardUiState(
                title = "Folder Availability",
                status = if (totalFolderAmount >= 0) "Live" else "Negative",
                description = "Current total balance across all folders after recorded QR payments.",
                progressLabel = "BALANCE",
                progressValue = currencyFormatter.format(totalFolderAmount),
                progress = if (totalFolderAmount <= 0.0) 0f else 1f
            ),
            InsightCardUiState(
                title = "QR Spend Tracker",
                status = if (transactions.isEmpty()) "Waiting" else "Active",
                description = "Recorded spend from confirmed UPI payments launched through PayTrack.",
                progressLabel = "SPENT",
                progressValue = currencyFormatter.format(spent),
                progress = if (spent <= 0.0 || totalFolderAmount <= 0.0) 0f else {
                    (spent / (spent + totalFolderAmount)).toFloat().coerceIn(0f, 1f)
                }
            ),
            InsightCardUiState(
                title = "Largest Folder",
                status = "Top",
                description = "Folder with the highest currently available balance.",
                progressLabel = "FOLDER",
                progressValue = topFolder?.name ?: "No folders",
                progress = 1f
            )
        )
    }

    private fun buildFolderSpendChart(
        categories: List<Category>,
        transactions: List<PaymentTransaction>
    ): List<FolderTransactionChartUiState> {
        val folderLookup = categories.associateBy(Category::id)
        return transactions
            .groupBy(PaymentTransaction::folderId)
            .mapNotNull { (folderId, folderTransactions) ->
                val folder = folderLookup[folderId] ?: return@mapNotNull null
                FolderTransactionChartUiState(
                    name = folder.name,
                    amount = folderTransactions.sumOf(PaymentTransaction::amount),
                    accentColor = folder.accentColor
                )
            }
            .sortedByDescending(FolderTransactionChartUiState::amount)
    }

    private fun categoryToUiState(category: Category): BudgetCategoryUiState {
        return BudgetCategoryUiState(
            id = category.id,
            name = category.name,
            amount = currencyFormatter.format(category.amount),
            accentColor = category.accentColor
        )
    }

    private fun transactionToUiState(transaction: PaymentTransaction): RecentTransactionUiState {
        return RecentTransactionUiState(
            title = transaction.merchantName,
            subtitle = "${transaction.folderName} via ${transaction.upiAppLabel}",
            time = timeFormatter.format(Date(transaction.createdAtMillis)),
            amount = "-${currencyFormatter.format(transaction.amount)}",
            isExpense = true
        )
    }

    private fun upiAppToUiState(app: com.paytrack.data.UpiAppInfo): UpiAppUiState {
        return UpiAppUiState(
            label = app.label,
            packageName = app.packageName
        )
    }

    private fun markPaymentError(message: String): Intent? {
        _qrUiState.update { it.copy(paymentError = message) }
        return null
    }
}

class HomeViewModelFactory(
    private val repository: FinanceRepository,
    private val appContext: Context
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(
        modelClass: Class<T>,
        extras: CreationExtras
    ): T {
        if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
            return HomeViewModel(
                repository = repository,
                appContext = appContext,
                savedStateHandle = extras.createSavedStateHandle()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}

private fun String.toCurrencyValue(): Double {
    return replace("$", "")
        .replace("\u20b9", "")
        .replace(",", "")
        .trim()
        .toDoubleOrNull() ?: 0.0
}

internal data class InsightsChartData(
    val weeklyDailyTotals: List<ChartPointUiState>,
    val currentMonthDailyTotals: List<ChartPointUiState>,
    val last12MonthTotals: List<ChartPointUiState>,
    val folderTransactionTotals: List<FolderTransactionChartUiState>
)

internal fun buildInsightsChartData(
    categories: List<Category>,
    transactions: List<PaymentTransaction>,
    nowMillis: Long
): InsightsChartData {
    val nowCalendar = Calendar.getInstance().apply { timeInMillis = nowMillis }
    val currentYear = nowCalendar.get(Calendar.YEAR)
    val currentMonth = nowCalendar.get(Calendar.MONTH)

    val weekStart = (nowCalendar.clone() as Calendar).apply {
        firstDayOfWeek = Calendar.MONDAY
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        val difference = (get(Calendar.DAY_OF_WEEK) - firstDayOfWeek + 7) % 7
        add(Calendar.DAY_OF_MONTH, -difference)
    }

    val weeklyTotals = MutableList(7) { 0.0 }
    val daysInMonth = nowCalendar.getActualMaximum(Calendar.DAY_OF_MONTH)
    val currentMonthTotals = MutableList(daysInMonth) { 0.0 }
    val monthTotals = linkedMapOf<Pair<Int, Int>, Double>()

    val rollingMonthCalendar = (nowCalendar.clone() as Calendar).apply {
        set(Calendar.DAY_OF_MONTH, 1)
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
        add(Calendar.MONTH, -11)
    }
    repeat(12) {
        monthTotals[rollingMonthCalendar.get(Calendar.YEAR) to rollingMonthCalendar.get(Calendar.MONTH)] = 0.0
        rollingMonthCalendar.add(Calendar.MONTH, 1)
    }

    val folderColorLookup = categories.associateBy(Category::id)
    val folderTotals = linkedMapOf<String, FolderTransactionChartUiState>()

    transactions.forEach { transaction ->
        val transactionCalendar = Calendar.getInstance().apply { timeInMillis = transaction.createdAtMillis }
        val transactionYear = transactionCalendar.get(Calendar.YEAR)
        val transactionMonth = transactionCalendar.get(Calendar.MONTH)

        val startOfTransactionDay = (transactionCalendar.clone() as Calendar).apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val daysFromWeekStart = ((startOfTransactionDay.timeInMillis - weekStart.timeInMillis) / MILLIS_PER_DAY).toInt()
        if (daysFromWeekStart in 0..6) {
            weeklyTotals[daysFromWeekStart] += transaction.amount
        }

        if (transactionYear == currentYear && transactionMonth == currentMonth) {
            val dayIndex = transactionCalendar.get(Calendar.DAY_OF_MONTH) - 1
            if (dayIndex in currentMonthTotals.indices) {
                currentMonthTotals[dayIndex] += transaction.amount
            }
        }

        val monthKey = transactionYear to transactionMonth
        if (monthTotals.containsKey(monthKey)) {
            monthTotals[monthKey] = monthTotals.getValue(monthKey) + transaction.amount
        }

        val existingFolderTotal = folderTotals[transaction.folderId]
        val folderAccentColor = folderColorLookup[transaction.folderId]?.accentColor ?: 0xFF5CC9C0
        folderTotals[transaction.folderId] = FolderTransactionChartUiState(
            name = transaction.folderName,
            amount = (existingFolderTotal?.amount ?: 0.0) + transaction.amount,
            accentColor = folderAccentColor
        )
    }

    val weekLabels = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val monthLabelFormatter = SimpleDateFormat("MMM", Locale.ENGLISH)

    return InsightsChartData(
        weeklyDailyTotals = weekLabels.mapIndexed { index, label ->
            ChartPointUiState(label = label, amount = weeklyTotals[index])
        },
        currentMonthDailyTotals = currentMonthTotals.mapIndexed { index, amount ->
            ChartPointUiState(label = (index + 1).toString(), amount = amount)
        },
        last12MonthTotals = monthTotals.map { (yearMonth, amount) ->
            val labelCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, yearMonth.first)
                set(Calendar.MONTH, yearMonth.second)
                set(Calendar.DAY_OF_MONTH, 1)
            }
            ChartPointUiState(label = monthLabelFormatter.format(labelCalendar.time), amount = amount)
        },
        folderTransactionTotals = folderTotals.values.sortedByDescending(FolderTransactionChartUiState::amount)
    )
}

private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L
