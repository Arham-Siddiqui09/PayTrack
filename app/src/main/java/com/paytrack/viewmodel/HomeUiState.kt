package com.paytrack.viewmodel

data class HomeUiState(
    val appName: String = "",
    val monthLabel: String = "",
    val availableBudget: String = "",
    val spentAmount: String = "",
    val budgetProgress: Float = 0f,
    val budgetProgressLabel: String = "",
    val insights: List<InsightCardUiState> = emptyList(),
    val folderSpendChart: List<FolderTransactionChartUiState> = emptyList(),
    val isFolderSpendChartEmpty: Boolean = true,
    val highlightCard: HighlightCardUiState = HighlightCardUiState(),
    val folders: List<BudgetCategoryUiState> = emptyList(),
    val recentTransactions: List<RecentTransactionUiState> = emptyList(),
    val isLoading: Boolean = true
)

data class InsightCardUiState(
    val title: String = "",
    val status: String = "",
    val description: String = "",
    val progressLabel: String = "",
    val progressValue: String = "",
    val progress: Float = 0f
)

data class HighlightCardUiState(
    val title: String = "",
    val subtitle: String = ""
)

data class BudgetCategoryUiState(
    val id: String = "",
    val name: String = "",
    val amount: String = "",
    val accentColor: Long = 0xFF5CC9C0
)

data class RecentTransactionUiState(
    val title: String = "",
    val subtitle: String = "",
    val time: String = "",
    val amount: String = "",
    val isExpense: Boolean = true
)

data class InsightsUiState(
    val totalSpent: String = "",
    val transactionCountLabel: String = "",
    val mostUsedFolder: String = "",
    val mostUsedFolderSpend: String = "",
    val selectedTransactionBarPeriod: TransactionBarPeriod = TransactionBarPeriod.WEEK,
    val weeklyDailyTotals: List<ChartPointUiState> = emptyList(),
    val currentMonthDailyTotals: List<ChartPointUiState> = emptyList(),
    val last12MonthTotals: List<ChartPointUiState> = emptyList(),
    val folderTransactionTotals: List<FolderTransactionChartUiState> = emptyList(),
    val recentMerchants: List<String> = emptyList(),
    val isTransactionChartEmpty: Boolean = true,
    val isMonthlyTrendEmpty: Boolean = true,
    val isFolderChartEmpty: Boolean = true,
    val isLoading: Boolean = true
)

enum class TransactionBarPeriod {
    WEEK,
    MONTH
}

data class ChartPointUiState(
    val label: String = "",
    val amount: Double = 0.0
)

data class FolderTransactionChartUiState(
    val name: String = "",
    val amount: Double = 0.0,
    val accentColor: Long = 0xFF5CC9C0
)

data class QrScanUiState(
    val folders: List<FolderPickerUiState> = emptyList(),
    val availableUpiApps: List<UpiAppUiState> = emptyList(),
    val scannedMerchantName: String = "",
    val scannedPayeeVpa: String = "",
    val scannedNote: String? = null,
    val scannedAmountText: String = "",
    val amountInput: String = "",
    val isAmountLocked: Boolean = false,
    val selectedFolderId: String? = null,
    val selectedFolderBalance: String = "",
    val projectedBalance: String = "",
    val pendingConfirmation: PendingConfirmationUiState? = null,
    val scanError: String? = null,
    val paymentError: String? = null,
    val hasCameraPermission: Boolean = false,
    val canLaunchPayment: Boolean = false
)

data class FolderPickerUiState(
    val id: String,
    val name: String,
    val balance: String
)

data class UpiAppUiState(
    val label: String,
    val packageName: String
)

data class PendingConfirmationUiState(
    val merchantName: String,
    val folderName: String,
    val amount: String,
    val appLabel: String
)
