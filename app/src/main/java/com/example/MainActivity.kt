package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.PosDatabase
import com.example.data.local.SecurityPreferences
import com.example.data.repository.PosRepository
import com.example.ui.PosApp
import com.example.ui.PosViewModel
import com.example.ui.PosViewModelFactory
import com.example.ui.theme.FlamePosTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val database = PosDatabase.getInstance(applicationContext)
        val securityPrefs = SecurityPreferences(applicationContext)
        val repository = PosRepository(database.posDao(), securityPrefs)

        setContent {
            val posViewModel: PosViewModel = viewModel(
                factory = PosViewModelFactory(repository)
            )
            FlamePosTheme {
                PosApp(viewModel = posViewModel)
            }
        }
    }
}
