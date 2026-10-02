package com.myai.assistant.ui.models

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.myai.assistant.inference.ModelInfo

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModelManagerScreen(onBack: () -> Unit, viewModel: ModelManagerViewModel = viewModel()) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    // Show download/load failures as a dismissible snackbar instead of
    // silently doing nothing, which was the original bug.
    LaunchedEffect(state.error) {
        val message = state.error ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(message)
        viewModel.dismissError()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Models") },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                }
            )
        },
        snackbarHost = {
            SnackbarHost(snackbarHostState) { data ->
                Snackbar(
                    action = {
                        TextButton(onClick = { data.dismiss() }) { Text("Dismiss") }
                    }
                ) { Text(data.visuals.message) }
            }
        }
    ) { padding ->
        LazyColumn(modifier = Modifier.padding(padding)) {
            items(state.models) { model ->
                ModelRow(
                    model = model,
                    isDownloaded = model.id in state.downloadedIds,
                    isActive = model.id == state.activeModelId,
                    isLoadingThis = state.isLoadingModel && model.id == state.activeModelId,
                    isLoadingAny = state.isLoadingModel,
                    progress = state.progressById[model.id],
                    onDownload = { viewModel.download(model) },
                    onDelete = { viewModel.delete(model) },
                    onUse = { viewModel.setActive(model) }
                )
                Divider()
            }
        }
    }
}

@Composable
private fun ModelRow(
    model: ModelInfo,
    isDownloaded: Boolean,
    isActive: Boolean,
    isLoadingThis: Boolean,
    isLoadingAny: Boolean,
    progress: Float?,
    onDownload: () -> Unit,
    onDelete: () -> Unit,
    onUse: () -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text(model.displayName, style = MaterialTheme.typography.titleMedium)
        Text(
            "~${model.approxSizeGb} GB \u00b7 ${model.contextLength} ctx \u00b7 needs ~${model.minRamGb} GB RAM",
            style = MaterialTheme.typography.bodySmall
        )
        Text(model.licenseNote, style = MaterialTheme.typography.labelSmall)
        Spacer(modifier = Modifier.height(8.dp))
        when {
            progress != null -> {
                LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth())
                Text("${(progress * 100).toInt()}%", style = MaterialTheme.typography.labelSmall)
            }
            isLoadingThis -> {
                Row {
                    CircularProgressIndicator(modifier = Modifier.height(20.dp).width(20.dp), strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Loading into memory\u2026", style = MaterialTheme.typography.bodySmall)
                }
            }
            else -> {
                Row {
                    if (isDownloaded) {
                        Button(onClick = onUse, enabled = !isActive && !isLoadingAny) {
                            Text(if (isActive) "Active" else "Use")
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedButton(onClick = onDelete, enabled = !isActive) { Text("Delete") }
                    } else {
                        Button(onClick = onDownload) { Text("Download") }
                    }
                }
            }
        }
    }
}
