package com.paytrack.ui.insights

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.paytrack.viewmodel.ChartPointUiState
import com.paytrack.viewmodel.FolderTransactionChartUiState
import com.paytrack.viewmodel.InsightsUiState
import com.paytrack.viewmodel.TransactionBarPeriod
import kotlin.math.max

@Composable
fun InsightsRoute(
    uiState: InsightsUiState,
    onTransactionBarPeriodChange: (TransactionBarPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    InsightsScreen(
        uiState = uiState,
        onTransactionBarPeriodChange = onTransactionBarPeriodChange,
        modifier = modifier
    )
}

@Composable
fun InsightsScreen(
    uiState: InsightsUiState,
    onTransactionBarPeriodChange: (TransactionBarPeriod) -> Unit,
    modifier: Modifier = Modifier
) {
    if (uiState.isLoading) {
        Column(
            modifier = modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            CircularProgressIndicator()
        }
        return
    }

    val transactionPoints = when (uiState.selectedTransactionBarPeriod) {
        TransactionBarPeriod.WEEK -> uiState.weeklyDailyTotals
        TransactionBarPeriod.MONTH -> uiState.currentMonthDailyTotals
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item { Spacer(modifier = Modifier.height(12.dp)) }

        item {
            InsightSummaryCard(
                title = "Tracked spend",
                value = uiState.totalSpent,
                subtitle = uiState.transactionCountLabel
            )
        }

        item {
            InsightSummaryCard(
                title = "Most used folder",
                value = uiState.mostUsedFolder,
                subtitle = uiState.mostUsedFolderSpend
            )
        }

        item {
            DailyTransactionChartCard(
                selectedPeriod = uiState.selectedTransactionBarPeriod,
                points = transactionPoints,
                isEmpty = uiState.isTransactionChartEmpty,
                onTransactionBarPeriodChange = onTransactionBarPeriodChange
            )
        }

        item {
            MonthlyTrendChartCard(
                points = uiState.last12MonthTotals,
                isEmpty = uiState.isMonthlyTrendEmpty
            )
        }

        item {
            FolderBarChartCard(
                points = uiState.folderTransactionTotals,
                isEmpty = uiState.isFolderChartEmpty
            )
        }

        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Recent merchants",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (uiState.recentMerchants.isEmpty()) {
                        Text(
                            text = "Merchant names will appear here after your first confirmed QR payment.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        uiState.recentMerchants.forEach { merchant ->
                            Text(
                                text = merchant,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(20.dp)) }
    }
}

@Composable
private fun DailyTransactionChartCard(
    selectedPeriod: TransactionBarPeriod,
    points: List<ChartPointUiState>,
    isEmpty: Boolean,
    onTransactionBarPeriodChange: (TransactionBarPeriod) -> Unit
) {
    ChartCard(
        title = "Transactions",
        subtitle = if (selectedPeriod == TransactionBarPeriod.WEEK) {
            "Daily spend in the current week"
        } else {
            "Daily spend in the current month"
        }
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            TransactionBarPeriod.entries.forEach { period ->
                FilterChip(
                    selected = selectedPeriod == period,
                    onClick = { onTransactionBarPeriodChange(period) },
                    label = {
                        Text(
                            text = if (period == TransactionBarPeriod.WEEK) "Week" else "Month"
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        if (isEmpty) {
            EmptyChartMessage("No transactions recorded for this period yet.")
        } else {
            val barWidth = if (selectedPeriod == TransactionBarPeriod.WEEK) 32.dp else 18.dp
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                points.forEach { point ->
                    DailyBar(
                        point = point,
                        maxAmount = points.maxOfOrNull(ChartPointUiState::amount) ?: 0.0,
                        barWidth = barWidth
                    )
                }
            }
        }
    }
}

@Composable
private fun MonthlyTrendChartCard(
    points: List<ChartPointUiState>,
    isEmpty: Boolean
) {
    ChartCard(
        title = "Monthly trend",
        subtitle = "Last 12 months of transaction totals"
    ) {
        if (isEmpty) {
            EmptyChartMessage("Monthly trends will appear after transactions are recorded.")
        } else {
            LineChart(points = points)
        }
    }
}

@Composable
private fun FolderBarChartCard(
    points: List<FolderTransactionChartUiState>,
    isEmpty: Boolean
) {
    ChartCard(
        title = "Folder spend",
        subtitle = "Transactions grouped by folder"
    ) {
        if (isEmpty) {
            EmptyChartMessage("Folder spend will appear after your first tracked payment.")
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                points.forEach { point ->
                    FolderBar(point = point, maxAmount = points.maxOfOrNull(FolderTransactionChartUiState::amount) ?: 0.0)
                }
            }
        }
    }
}

@Composable
private fun ChartCard(
    title: String,
    subtitle: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            content()
        }
    }
}

@Composable
private fun DailyBar(
    point: ChartPointUiState,
    maxAmount: Double,
    barWidth: androidx.compose.ui.unit.Dp
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = point.label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Box(
            modifier = Modifier
                .height(148.dp)
                .width(barWidth),
                contentAlignment = Alignment.BottomCenter
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                        shape = RoundedCornerShape(16.dp)
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(((point.amount / max(maxAmount, 1.0)) * 148).dp)
                    .background(
                        color = MaterialTheme.colorScheme.primary,
                        shape = RoundedCornerShape(16.dp)
                    )
            )
        }
        Text(
            text = point.amount.toCompactCurrency(),
            style = MaterialTheme.typography.labelSmall,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun LineChart(points: List<ChartPointUiState>) {
    val maxAmount = points.maxOfOrNull(ChartPointUiState::amount) ?: 0.0
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(180.dp)
        ) {
            val chartWidth = size.width
            val chartHeight = size.height

            repeat(4) { index ->
                val y = chartHeight * index / 3f
                drawLine(
                    color = gridColor,
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 2f
                )
            }

            val stepX = if (points.size > 1) chartWidth / (points.size - 1) else 0f
            val pointOffsets = points.mapIndexed { index, point ->
                val normalized = if (maxAmount <= 0.0) 0f else (point.amount / maxAmount).toFloat()
                Offset(
                    x = stepX * index,
                    y = chartHeight - (normalized * (chartHeight - 12.dp.toPx())) - 6.dp.toPx()
                )
            }

            val path = Path()
            pointOffsets.forEachIndexed { index, offset ->
                if (index == 0) {
                    path.moveTo(offset.x, offset.y)
                } else {
                    path.lineTo(offset.x, offset.y)
                }
            }

            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 6f, cap = StrokeCap.Round)
            )

            pointOffsets.forEach { offset ->
                drawCircle(
                    color = lineColor,
                    radius = 8f,
                    center = offset
                )
                drawCircle(
                    color = Color.White,
                    radius = 4f,
                    center = offset
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            points.forEach { point ->
                Text(
                    text = point.label,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun FolderBar(
    point: FolderTransactionChartUiState,
    maxAmount: Double
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = point.name,
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = point.amount.toCurrencyLabel(),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(14.dp)
                .background(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                    shape = RoundedCornerShape(999.dp)
                )
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth((point.amount / max(maxAmount, 1.0)).toFloat())
                    .height(14.dp)
            ) {
                drawRoundRect(
                    color = Color(point.accentColor),
                    cornerRadius = CornerRadius(999f, 999f)
                )
            }
        }
    }
}

@Composable
private fun EmptyChartMessage(message: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .width(44.dp)
                .height(44.dp)
                .background(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = CircleShape
                )
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun InsightSummaryCard(
    title: String,
    value: String,
    subtitle: String
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

private fun Double.toCompactCurrency(): String {
    return when {
        this >= 1000 -> "\u20b9${(this / 1000).toInt()}k"
        this > 0 -> "\u20b9${toInt()}"
        else -> "\u20b90"
    }
}

private fun Double.toCurrencyLabel(): String {
    return "\u20b9${"%,.0f".format(this)}"
}
