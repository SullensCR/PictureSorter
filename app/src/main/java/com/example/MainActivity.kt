package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.PhotoDatabase
import com.example.data.PhotoSorterRepository
import com.example.data.PhotoSorterViewModel
import com.example.data.PhotoSorterViewModelFactory
import com.example.ui.screens.PhotoSorterApp
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                val context = this
                val db = remember { PhotoDatabase.getDatabase(context) }
                val repo = remember { PhotoSorterRepository(db.dao()) }
                val mainViewModel: PhotoSorterViewModel = viewModel(
                    factory = PhotoSorterViewModelFactory(repo, context.applicationContext)
                )

                PhotoSorterApp(viewModel = mainViewModel)
            }
        }
    }
}
