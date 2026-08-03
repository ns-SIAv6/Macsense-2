package com.macsense.ai.ui.screens

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import com.macsense.ai.ui.viewmodel.FlowCaptureViewModel

@Composable
fun FlowCaptureScreenWithAudio() {
    val context = LocalContext.current
    val vm: FlowCaptureViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T =
            FlowCaptureViewModel(context) as T
    })
    FlowCaptureScreen(viewModel = vm)
}
