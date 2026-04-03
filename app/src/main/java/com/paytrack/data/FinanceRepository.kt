package com.paytrack.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import org.json.JSONArray
import org.json.JSONObject

private const val DATASTORE_NAME = "finance_preferences"
private const val CATEGORIES_KEY = "categories_json"
private const val TRANSACTIONS_KEY = "transactions_json"
private const val SUCCESS_STATUS = "SUCCESS"

private val Context.financeDataStore: DataStore<Preferences> by preferencesDataStore(name = DATASTORE_NAME)

class FinanceRepository(
    private val context: Context
) {

    private val categoriesKey = stringPreferencesKey(CATEGORIES_KEY)
    private val transactionsKey = stringPreferencesKey(TRANSACTIONS_KEY)

    fun getCategories(): Flow<List<Category>> {
        return context.financeDataStore.data
            .catchDataStore()
            .map { preferences ->
                preferences[categoriesKey]
                    ?.takeIf(String::isNotBlank)
                    ?.let(::jsonToCategories)
                    .orEmpty()
            }
    }

    fun getTransactions(): Flow<List<PaymentTransaction>> {
        return context.financeDataStore.data
            .catchDataStore()
            .map { preferences ->
                preferences[transactionsKey]
                    ?.takeIf(String::isNotBlank)
                    ?.let(::jsonToTransactions)
                    .orEmpty()
                    .sortedByDescending(PaymentTransaction::createdAtMillis)
            }
    }

    suspend fun ensureDefaultCategories() {
        if (getCategories().first().isNotEmpty()) return
        saveCategories(defaultCategories())
    }

    suspend fun createCategory(name: String, amount: Double) {
        val existingCategories = getCategories().first()
        val updatedCategories = existingCategories + Category(
            id = UUID.randomUUID().toString(),
            name = name,
            amount = amount,
            accentColor = nextAccentColor(existingCategories.size)
        )
        saveCategories(updatedCategories)
    }

    suspend fun updateCategory(id: String, name: String, amount: Double) {
        val updatedCategories = getCategories().first().map { category ->
            if (category.id == id) {
                category.copy(
                    name = name,
                    amount = amount
                )
            } else {
                category
            }
        }
        saveCategories(updatedCategories)
    }

    suspend fun deleteCategory(id: String) {
        val updatedCategories = getCategories().first().filterNot { it.id == id }
        saveCategories(updatedCategories)
    }

    suspend fun getCategory(id: String): Category? {
        return getCategories().first().firstOrNull { it.id == id }
    }

    suspend fun recordSuccessfulPayment(
        folderId: String,
        merchantName: String,
        amount: Double,
        upiAppPackage: String,
        upiAppLabel: String,
        payeeVpa: String,
        note: String?
    ) {
        val categories = getCategories().first()
        val targetCategory = categories.firstOrNull { it.id == folderId } ?: return
        val updatedCategories = categories.map { category ->
            if (category.id == folderId) {
                category.copy(amount = category.amount - amount)
            } else {
                category
            }
        }

        val updatedTransactions = getTransactions().first() + PaymentTransaction(
            id = UUID.randomUUID().toString(),
            folderId = targetCategory.id,
            folderName = targetCategory.name,
            merchantName = merchantName,
            amount = amount,
            upiAppPackage = upiAppPackage,
            upiAppLabel = upiAppLabel,
            payeeVpa = payeeVpa,
            note = note,
            createdAtMillis = System.currentTimeMillis(),
            status = SUCCESS_STATUS
        )

        context.financeDataStore.edit { preferences ->
            preferences[categoriesKey] = categoriesToJson(updatedCategories)
            preferences[transactionsKey] = transactionsToJson(updatedTransactions)
        }
    }

    private suspend fun saveCategories(categories: List<Category>) {
        context.financeDataStore.edit { preferences ->
            preferences[categoriesKey] = categoriesToJson(categories)
        }
    }

    private fun defaultCategories(): List<Category> {
        return listOf(
            Category(
                id = UUID.randomUUID().toString(),
                name = "Grocery",
                amount = 0.0,
                accentColor = 0xFFFF8D8D
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Transportation",
                amount = 0.0,
                accentColor = 0xFFAEC7D6
            ),
            Category(
                id = UUID.randomUUID().toString(),
                name = "Others",
                amount = 0.0,
                accentColor = 0xFF5CC9C0
            )
        )
    }

    private fun nextAccentColor(index: Int): Long {
        val accentColors = listOf(
            0xFFFF8D8D,
            0xFFAEC7D6,
            0xFF5CC9C0,
            0xFFFFC27A,
            0xFFB8A1FF
        )
        return accentColors[index % accentColors.size]
    }

    private fun categoriesToJson(categories: List<Category>): String {
        val jsonArray = JSONArray()
        categories.forEach { category ->
            jsonArray.put(
                JSONObject()
                    .put("id", category.id)
                    .put("name", category.name)
                    .put("amount", category.amount)
                    .put("accentColor", category.accentColor)
            )
        }
        return jsonArray.toString()
    }

    private fun transactionsToJson(transactions: List<PaymentTransaction>): String {
        val jsonArray = JSONArray()
        transactions.forEach { transaction ->
            jsonArray.put(
                JSONObject()
                    .put("id", transaction.id)
                    .put("folderId", transaction.folderId)
                    .put("folderName", transaction.folderName)
                    .put("merchantName", transaction.merchantName)
                    .put("amount", transaction.amount)
                    .put("upiAppPackage", transaction.upiAppPackage)
                    .put("upiAppLabel", transaction.upiAppLabel)
                    .put("payeeVpa", transaction.payeeVpa)
                    .put("note", transaction.note)
                    .put("createdAtMillis", transaction.createdAtMillis)
                    .put("status", transaction.status)
            )
        }
        return jsonArray.toString()
    }

    private fun jsonToCategories(json: String): List<Category> {
        val jsonArray = JSONArray(json)
        return buildList {
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(index)
                add(
                    Category(
                        id = item.getString("id"),
                        name = item.getString("name"),
                        amount = item.getDouble("amount"),
                        accentColor = item.getLong("accentColor")
                    )
                )
            }
        }
    }

    private fun jsonToTransactions(json: String): List<PaymentTransaction> {
        val jsonArray = JSONArray(json)
        return buildList {
            for (index in 0 until jsonArray.length()) {
                val item = jsonArray.getJSONObject(index)
                add(
                    PaymentTransaction(
                        id = item.getString("id"),
                        folderId = item.getString("folderId"),
                        folderName = item.getString("folderName"),
                        merchantName = item.getString("merchantName"),
                        amount = item.getDouble("amount"),
                        upiAppPackage = item.getString("upiAppPackage"),
                        upiAppLabel = item.getString("upiAppLabel"),
                        payeeVpa = item.getString("payeeVpa"),
                        note = item.optString("note").takeIf(String::isNotBlank),
                        createdAtMillis = item.getLong("createdAtMillis"),
                        status = item.getString("status")
                    )
                )
            }
        }
    }

    private fun Flow<Preferences>.catchDataStore(): Flow<Preferences> {
        return catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
    }
}
