package com.paytrack

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.paytrack.data.FinanceRepository
import com.paytrack.navigation.FinanceNavGraph
import com.paytrack.ui.theme.PayTrackTheme
import com.paytrack.viewmodel.HomeViewModel
import com.paytrack.viewmodel.HomeViewModelFactory

class MainActivity : ComponentActivity() {

    private val viewModel: HomeViewModel by viewModels {
        HomeViewModelFactory(
            repository = FinanceRepository(applicationContext),
            appContext = applicationContext
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            val uiState by viewModel.uiState.collectAsStateWithLifecycle()

            PayTrackTheme {
                FinanceNavGraph(
                    uiState = uiState,
                    viewModel = viewModel
                )
            }
        }
    }
}
