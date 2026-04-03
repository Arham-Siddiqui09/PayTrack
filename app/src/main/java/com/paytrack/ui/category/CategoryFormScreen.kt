package com.paytrack.ui.category

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.paytrack.viewmodel.BudgetCategoryUiState
import java.text.NumberFormat
import java.util.Locale

@Composable
fun CategoryFormRoute(
    category: BudgetCategoryUiState?,
    onNavigateBack: () -> Unit,
    onSaveNewCategory: (String, Double) -> Unit,
    onSaveEditedCategory: (String, String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    CategoryFormScreen(
        category = category,
        onNavigateBack = onNavigateBack,
        onSaveNewCategory = onSaveNewCategory,
        onSaveEditedCategory = onSaveEditedCategory,
        modifier = modifier
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryFormScreen(
    category: BudgetCategoryUiState?,
    onNavigateBack: () -> Unit,
    onSaveNewCategory: (String, Double) -> Unit,
    onSaveEditedCategory: (String, String, Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val isEditMode = category != null
    val currentAmount = remember(category?.amount) { category?.amount?.toCurrencyValue() ?: 0.0 }
    val currencyFormatter = remember { NumberFormat.getCurrencyInstance(Locale.forLanguageTag("en-IN")) }

    var name by rememberSaveable(category?.id) { mutableStateOf(category?.name.orEmpty()) }
    var amount by rememberSaveable(category?.id) { mutableStateOf("") }
    var addAmount by rememberSaveable(category?.id) { mutableStateOf("") }
    var removeAmount by rememberSaveable(category?.id) { mutableStateOf("") }
    var nameError by remember { mutableStateOf<String?>(null) }
    var amountError by remember { mutableStateOf<String?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (isEditMode) "Edit Folder" else "Add Folder") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isEditMode) "Update folder details" else "Create a new folder",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = name,
                        onValueChange = {
                            name = it
                            nameError = null
                        },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Folder name") },
                        singleLine = true,
                        isError = nameError != null,
                        supportingText = {
                            nameError?.let { Text(it) }
                        }
                    )

                    if (isEditMode) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    color = MaterialTheme.colorScheme.surfaceVariant,
                                    shape = RoundedCornerShape(20.dp)
                                )
                                .padding(16.dp)
                        ) {
                            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                                Text(
                                    text = "Current amount",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = currencyFormatter.format(currentAmount),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        OutlinedTextField(
                            value = addAmount,
                            onValueChange = {
                                addAmount = it
                                amountError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Add money") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                        )

                        OutlinedTextField(
                            value = removeAmount,
                            onValueChange = {
                                removeAmount = it
                                amountError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Remove money") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = amountError != null,
                            supportingText = {
                                amountError?.let { Text(it) }
                            }
                        )
                    } else {
                        OutlinedTextField(
                            value = amount,
                            onValueChange = {
                                amount = it
                                amountError = null
                            },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Folder amount") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            isError = amountError != null,
                            supportingText = {
                                amountError?.let { Text(it) }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Button(
                        onClick = {
                            val trimmedName = name.trim()
                            if (trimmedName.isEmpty()) {
                                nameError = "Please enter a folder name."
                                return@Button
                            }

                            if (category != null) {
                                val addValue = addAmount.toDoubleOrNull()
                                val removeValue = removeAmount.toDoubleOrNull()
                                if ((addAmount.isNotBlank() && addValue == null) ||
                                    (removeAmount.isNotBlank() && removeValue == null)
                                ) {
                                    amountError = "Enter valid numbers for add or remove."
                                    return@Button
                                }

                                val updatedAmount = currentAmount + (addValue ?: 0.0) - (removeValue ?: 0.0)
                                onSaveEditedCategory(category.id, trimmedName, updatedAmount)
                            } else {
                                val parsedAmount = amount.toDoubleOrNull()
                                if (parsedAmount == null) {
                                    amountError = "Please enter a valid amount."
                                    return@Button
                                }

                                onSaveNewCategory(trimmedName, parsedAmount)
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(if (isEditMode) "Save Changes" else "Save Folder")
                    }
                }
            }
        }
    }
}

private fun String.toCurrencyValue(): Double {
    return replace("$", "")
        .replace("\u20b9", "")
        .replace(",", "")
        .trim()
        .toDoubleOrNull() ?: 0.0
}
