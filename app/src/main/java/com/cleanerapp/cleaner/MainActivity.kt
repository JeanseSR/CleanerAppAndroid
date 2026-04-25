package com.cleanerapp.cleaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.cleanerapp.cleaner.ui.theme.CleanerAppTheme

class MainActivity : ComponentActivity() {

    private val viewModel: CleanerViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            CleanerAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CleanerScreen(viewModel)
                }
            }
        }
    }
}

@Composable
fun CleanerScreen(viewModel: CleanerViewModel) {
    val state by viewModel.state.collectAsState()

    var clearCache by remember { mutableStateOf(true) }
    var clearTemp by remember { mutableStateOf(true) }
    var clearBrowser by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "Cleaner",
            style = MaterialTheme.typography.headlineLarge
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Toggles
        CleanOptionRow("Cache des apps", clearCache) { clearCache = it }
        CleanOptionRow("Fichiers temporaires", clearTemp) { clearTemp = it }
        CleanOptionRow("Données navigateur", clearBrowser) { clearBrowser = it }

        Spacer(modifier = Modifier.height(16.dp))

        // Bouton principal
        Button(
            onClick = {
                if (state is CleanerViewModel.CleanState.Idle ||
                    state is CleanerViewModel.CleanState.Done ||
                    state is CleanerViewModel.CleanState.Error) {
                    viewModel.runCleaner(clearCache, clearTemp, clearBrowser)
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            enabled = state !is CleanerViewModel.CleanState.Running
        ) {
            Text(
                text = when (state) {
                    is CleanerViewModel.CleanState.Running -> "Nettoyage en cours..."
                    else -> "Nettoyer"
                }
            )
        }

        // Résultat
        when (val s = state) {
            is CleanerViewModel.CleanState.Running -> {
                CircularProgressIndicator()
            }
            is CleanerViewModel.CleanState.Done -> {
                ResultCard(
                    bytes = s.totalBytes,
                    filesCount = s.filesCount,
                    errors = s.errors,
                    onReset = { viewModel.reset() }
                )
            }
            is CleanerViewModel.CleanState.Error -> {
                Text(
                    text = "Erreur : ${s.message}",
                    color = MaterialTheme.colorScheme.error
                )
            }
            else -> {}
        }
    }
}

@Composable
fun CleanOptionRow(label: String, checked: Boolean, onCheck: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheck)
    }
}

@Composable
fun ResultCard(
    bytes: Long,
    filesCount: Int,
    errors: List<String>,
    onReset: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Nettoyage terminé !",
                style = MaterialTheme.typography.titleMedium
            )
            Text(text = "Espace libéré : ${formatSize(bytes)}")
            if (filesCount > 0)
                Text(text = "Fichiers supprimés : $filesCount")
            if (errors.isNotEmpty())
                Text(
                    text = "${errors.size} erreur(s) mineures",
                    color = MaterialTheme.colorScheme.error
                )
            Spacer(modifier = Modifier.height(4.dp))
            TextButton(onClick = onReset) {
                Text("Recommencer")
            }
        }
    }
}

fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "%.1f Mo".format(bytes / 1_048_576.0)
        bytes >= 1_024     -> "%.1f Ko".format(bytes / 1_024.0)
        else               -> "$bytes octets"
    }
}