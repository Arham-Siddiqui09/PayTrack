package com.paytrack.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.ArrowCircleUp
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.DirectionsBus
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.LocalCafe
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PersonOutline
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.ShoppingCart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.paytrack.ui.theme.AppBlue
import com.paytrack.ui.theme.AppBorder
import com.paytrack.ui.theme.AppCoral
import com.paytrack.ui.theme.AppGrayChip
import com.paytrack.ui.theme.AppPink
import com.paytrack.ui.theme.AppPrimary
import com.paytrack.ui.theme.AppProgressTrack
import com.paytrack.ui.theme.AppSurfaceMuted
import com.paytrack.ui.theme.PayTrackTheme
import com.paytrack.viewmodel.BudgetCategoryUiState
import com.paytrack.viewmodel.FolderTransactionChartUiState
import com.paytrack.viewmodel.HighlightCardUiState
import com.paytrack.viewmodel.HomeUiState
import com.paytrack.viewmodel.InsightCardUiState
import com.paytrack.viewmodel.RecentTransactionUiState
import java.text.NumberFormat
import java.util.Locale

private val inrFormatter = NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN"))

@Composable
fun HomeRoute(
    uiState: HomeUiState,
    onAddCategory: () -> Unit,
    onEditCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onOpenQr: () -> Unit,
    onOpenInsights: () -> Unit,
    modifier: Modifier = Modifier
) {
    HomeScreen(
        uiState = uiState,
        onAddCategory = onAddCategory,
        onEditCategory = onEditCategory,
        onDeleteCategory = onDeleteCategory,
        onOpenQr = onOpenQr,
        onOpenInsights = onOpenInsights,
        modifier = modifier
    )
}

@Composable
fun HomeScreen(
    uiState: HomeUiState,
    onAddCategory: () -> Unit,
    onEditCategory: (String) -> Unit,
    onDeleteCategory: (String) -> Unit,
    onOpenQr: () -> Unit,
    onOpenInsights: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center),
                    color = MaterialTheme.colorScheme.primary
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        HeaderRow(appName = uiState.appName)
                    }

                    item {
                        BudgetOverviewCard(
                            uiState = uiState,
                            onOpenQr = onOpenQr,
                            onOpenInsights = onOpenInsights
                        )
                    }

                    item {
                        SectionHeader(
                            title = "Monthly Insights",
                            trailingText = uiState.monthLabel
                        )
                    }

                    item {
                        FolderSpendBarChart(
                            entries = uiState.folderSpendChart,
                            isEmpty = uiState.isFolderSpendChartEmpty
                        )
                    }

                    item {
                        GrowthForecastCard(highlightCard = uiState.highlightCard)
                    }

                    item {
                        SectionHeader(title = "Folders")
                    }

                    items(uiState.folders) { category ->
                        CategoryCard(
                            category = category,
                            onEdit = { onEditCategory(category.id) },
                            onDelete = { onDeleteCategory(category.id) }
                        )
                    }

                    item {
                        AddCategoryCard(onClick = onAddCategory)
                    }

                    item {
                        RecentTransactionsCard(transactions = uiState.recentTransactions)
                    }

                    item {
                        Spacer(modifier = Modifier.height(12.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun FolderSpendBarChart(
    entries: List<FolderTransactionChartUiState>,
    isEmpty: Boolean
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "Money transacted per folder",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )

            if (isEmpty || entries.isEmpty()) {
                Text(
                    text = "No folder transactions recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                val visibleEntries = entries.take(6)
                val maxAmount = visibleEntries.maxOfOrNull(FolderTransactionChartUiState::amount)?.takeIf { it > 0.0 } ?: 1.0

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(220.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Bottom
                ) {
                    visibleEntries.forEach { entry ->
                        val fraction = (entry.amount / maxAmount).toFloat().coerceIn(0f, 1f)
                        val accentColor = Color(entry.accentColor)

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Bottom
                        ) {
                            Text(
                                text = inrFormatter.format(entry.amount),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 18.dp)
                                    .fillMaxHeight(0.75f * fraction)
                                    .clip(RoundedCornerShape(topStart = 18.dp, topEnd = 18.dp, bottomStart = 6.dp, bottomEnd = 6.dp))
                                    .background(
                                        brush = Brush.verticalGradient(
                                            listOf(
                                                accentColor,
                                                accentColor.copy(alpha = 0.45f)
                                            )
                                        )
                                    )
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = entry.name.take(10),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeaderRow(appName: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFD9E6EC)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.PersonOutline,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(16.dp)
                )
            }
            Text(
                text = appName,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
        }

        Icon(
            imageVector = Icons.Outlined.Notifications,
            contentDescription = "Notifications",
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun BudgetOverviewCard(
    uiState: HomeUiState,
    onOpenQr: () -> Unit,
    onOpenInsights: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(28.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    brush = Brush.linearGradient(
                        listOf(
                            Color(0xFFF8FBFB),
                            Color(0xFFF7FAFD)
                        )
                    )
                )
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text = "Available Budget",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Live balance across all folders",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Text(
                text = uiState.availableBudget,
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Start
            ) {
                BudgetAmountBlock(
                    label = "TRACKED SPEND",
                    value = uiState.spentAmount,
                    valueColor = Color(0xFF23A98B)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = uiState.budgetProgressLabel,
                    style = MaterialTheme.typography.labelSmall,
                    color = Color(0xFF168F81),
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFDDF7EF))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                )

                ProgressTrack(
                    progress = uiState.budgetProgress,
                    progressColor = AppPrimary
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ActionPill(
                    label = "QR Pay",
                    backgroundColor = AppGrayChip,
                    contentColor = Color.White,
                    icon = Icons.Outlined.QrCodeScanner,
                    onClick = onOpenQr
                )
                ActionPill(
                    label = "Insights",
                    backgroundColor = AppBlue,
                    contentColor = Color(0xFF357693),
                    icon = Icons.Outlined.Analytics,
                    onClick = onOpenInsights
                )
            }
        }
    }
}

@Composable
private fun BudgetAmountBlock(
    label: String,
    value: String,
    valueColor: Color
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = valueColor
        )
    }
}

@Composable
private fun ActionPill(
    label: String,
    backgroundColor: Color,
    contentColor: Color,
    icon: ImageVector,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .clickable(onClick = onClick)
            .clip(RoundedCornerShape(20.dp))
            .background(backgroundColor)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(16.dp)
        )
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SectionHeader(
    title: String,
    trailingText: String? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )

        if (trailingText != null) {
            Text(
                text = trailingText,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InsightCard(insight: InsightCardUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(26.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFE5F7FC)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Home,
                            contentDescription = null,
                            tint = Color(0xFF3C88A4),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    Text(
                        text = insight.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Text(
                    text = insight.status,
                    style = MaterialTheme.typography.labelMedium,
                    color = Color(0xFF4A9AC0),
                    fontWeight = FontWeight.SemiBold
                )
            }

            Text(
                text = insight.description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = insight.progressLabel,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = insight.progressValue,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            ProgressTrack(
                progress = insight.progress,
                progressColor = AppBlue
            )
        }
    }
}

@Composable
private fun GrowthForecastCard(highlightCard: HighlightCardUiState) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppPink),
        shape = RoundedCornerShape(28.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(AppCoral),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.TrendingUp,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(18.dp)
                )
            }

            Text(
                text = highlightCard.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = highlightCard.subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun CategoryCard(
    category: BudgetCategoryUiState,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    val accentColor = Color(category.accentColor)

    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        shape = RoundedCornerShape(18.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF2F2F0)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = categoryIcon(category.name),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(18.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = category.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = category.amount,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FolderActionButton(
                        label = "Edit",
                        icon = Icons.Outlined.Edit,
                        onClick = onEdit,
                        containerColor = accentColor.copy(alpha = 0.14f),
                        contentColor = accentColor
                    )
                    FolderActionButton(
                        label = "Delete",
                        icon = Icons.Outlined.DeleteOutline,
                        onClick = onDelete,
                        containerColor = Color(0xFFFFF1F0),
                        contentColor = AppCoral
                    )
                }

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(999.dp))
                        .background(accentColor.copy(alpha = 0.18f))
                )
            }
        }
    }
}

@Composable
private fun FolderActionButton(
    label: String,
    icon: ImageVector,
    onClick: () -> Unit,
    containerColor: Color,
    contentColor: Color
) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = contentColor,
            modifier = Modifier.size(15.dp)
        )
        Text(
            text = label,
            color = contentColor,
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun AddCategoryCard(onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .border(
                width = 1.dp,
                color = AppBorder,
                shape = RoundedCornerShape(28.dp)
            ),
        shape = RoundedCornerShape(28.dp),
        color = Color.Transparent
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF4F4F1)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "Add New Folder",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.SemiBold
            )
        }
    }
}

@Composable
private fun RecentTransactionsCard(transactions: List<RecentTransactionUiState>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AppSurfaceMuted),
        shape = RoundedCornerShape(22.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Recent Transactions",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            if (transactions.isEmpty()) {
                Text(
                    text = "No QR payments recorded yet.",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                transactions.forEachIndexed { index, transaction ->
                    TransactionRow(transaction = transaction)
                    if (index != transactions.lastIndex) {
                        HorizontalDivider(color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(transaction: RecentTransactionUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.9f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalCafe,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(14.dp)
                )
            }

            Column {
                Text(
                    text = transaction.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = transaction.subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = transaction.time,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = transaction.amount,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Bold,
            color = if (transaction.isExpense) AppCoral else Color(0xFF23A98B)
        )
    }
}

@Composable
private fun ProgressTrack(
    progress: Float,
    progressColor: Color
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .background(AppProgressTrack)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(progress.coerceIn(0f, 1f))
                .height(6.dp)
                .clip(RoundedCornerShape(50))
                .background(progressColor)
        )
    }
}

private fun categoryIcon(name: String): ImageVector {
    return when {
        "Dining" in name -> Icons.Outlined.Restaurant
        "Grocery" in name -> Icons.Outlined.ShoppingCart
        else -> Icons.Outlined.DirectionsBus
    }
}

@Preview(
    name = "Home - Filled",
    showBackground = true,
    showSystemUi = true,
    heightDp = 920,
    widthDp = 412
)
@Composable
private fun HomeScreenFilledPreview() {
    PayTrackTheme {
        HomeScreen(
            uiState = HomeUiState(
                appName = "PayTrack",
                monthLabel = "April 2026",
                availableBudget = "\u20b92,200.00",
                spentAmount = "\u20b9335.00",
                budgetProgress = 0.4f,
                budgetProgressLabel = "5 payments tracked from folders",
                insights = listOf(
                    InsightCardUiState(
                        title = "Folder Availability",
                        status = "Live",
                        description = "Current total balance across all folders after recorded QR payments.",
                        progressLabel = "BALANCE",
                        progressValue = "\u20b92,200.00",
                        progress = 1f
                    )
                ),
                folderSpendChart = listOf(
                    FolderTransactionChartUiState(
                        name = "Dining",
                        amount = 820.0,
                        accentColor = 0xFF5CC9C0
                    ),
                    FolderTransactionChartUiState(
                        name = "Grocery",
                        amount = 460.0,
                        accentColor = 0xFFFF8D8D
                    ),
                    FolderTransactionChartUiState(
                        name = "Travel",
                        amount = 290.0,
                        accentColor = 0xFF7DA6FF
                    )
                ),
                isFolderSpendChartEmpty = false,
                highlightCard = HighlightCardUiState(
                    title = "UPI payments are being tracked",
                    subtitle = "Cafe payment updated your budgets."
                ),
                folders = listOf(
                    BudgetCategoryUiState(
                        id = "1",
                        name = "Dining & Drinks",
                        amount = "\u20b91,200.00",
                        accentColor = 0xFF5CC9C0
                    ),
                    BudgetCategoryUiState(
                        id = "2",
                        name = "Grocery",
                        amount = "\u20b9600.00",
                        accentColor = 0xFFFF8D8D
                    )
                ),
                recentTransactions = listOf(
                    RecentTransactionUiState(
                        title = "Artisan Brews",
                        subtitle = "Dining & Drinks via GPay",
                        time = "02 Apr, 09:42 AM",
                        amount = "-\u20b914.50",
                        isExpense = true
                    )
                ),
                isLoading = false
            ),
            onAddCategory = {},
            onEditCategory = {},
            onDeleteCategory = {},
            onOpenQr = {},
            onOpenInsights = {}
        )
    }
}

@Preview(
    name = "Home - Empty",
    showBackground = true,
    showSystemUi = true,
    heightDp = 920,
    widthDp = 412
)
@Composable
private fun HomeScreenEmptyPreview() {
    PayTrackTheme {
        HomeScreen(
            uiState = HomeUiState(
                appName = "PayTrack",
                monthLabel = "April 2026",
                availableBudget = "\u20b90.00",
                spentAmount = "\u20b90.00",
                budgetProgress = 0f,
                budgetProgressLabel = "Start tracking payments from your folders",
                folderSpendChart = emptyList(),
                isFolderSpendChartEmpty = true,
                highlightCard = HighlightCardUiState(
                    title = "Ready for your first QR payment",
                    subtitle = "Scan a merchant QR to deduct directly from a folder."
                ),
                folders = emptyList(),
                recentTransactions = emptyList(),
                isLoading = false
            ),
            onAddCategory = {},
            onEditCategory = {},
            onDeleteCategory = {},
            onOpenQr = {},
            onOpenInsights = {}
        )
    }
}
