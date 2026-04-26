package com.cleanerapp.cleaner

import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
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
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val permManager = remember { viewModel.permManager }

    var clearCache by remember { mutableStateOf(true) }
    var clearTemp by remember { mutableStateOf(true) }
    var clearBrowser by remember { mutableStateOf(true) }
    var clearApk by remember { mutableStateOf(true) }
    var clearThumbnails by remember { mutableStateOf(true) }
    var clearWhatsApp by remember { mutableStateOf(true) }
    var clearTelegram by remember { mutableStateOf(true) }
    var hasWhatsAppAccess by remember { mutableStateOf(permManager.hasWhatsAppAccess()) }
    var hasTelegramAccess by remember { mutableStateOf(permManager.hasTelegramAccess()) }

    val whatsAppLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            permManager.saveWhatsAppUri(it)
            hasWhatsAppAccess = true
        }
    }

    val telegramLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        uri?.let {
            context.contentResolver.takePersistableUriPermission(
                it,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )
            permManager.saveTelegramUri(it)
            hasTelegramAccess = true
        }
    }

    fun buildParams() = CleanerViewModel.CleanParams(
        clearCache = clearCache,
        clearTemp = clearTemp,
        clearBrowser = clearBrowser,
        clearApk = clearApk,
        clearThumbnails = clearThumbnails,
        clearWhatsApp = clearWhatsApp && hasWhatsAppAccess,
        clearTelegram = clearTelegram && hasTelegramAccess,
        whatsAppDocFile = if (clearWhatsApp && hasWhatsAppAccess)
            permManager.getWhatsAppDocumentFile() else null,
        telegramDocFile = if (clearTelegram && hasTelegramAccess)
            permManager.getTelegramDocumentFile() else null
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Cleaner", style = MaterialTheme.typography.headlineLarge)
        Spacer(modifier = Modifier.height(4.dp))

        when (val s = state) {

            // --- IDLE : sélection des options ---
            is CleanerViewModel.CleanState.Idle -> {
                SectionHeader("Général")
                CleanOptionRow("Cache des apps", clearCache) { clearCache = it }
                CleanOptionRow("Fichiers temporaires", clearTemp) { clearTemp = it }
                CleanOptionRow("Données navigateur", clearBrowser) { clearBrowser = it }
                CleanOptionRow("APK téléchargés", clearApk) { clearApk = it }
                CleanOptionRow("Miniatures médias", clearThumbnails) { clearThumbnails = it }

                SectionHeader("Messagerie")
                MessagingRow(
                    label = "WhatsApp",
                    hasAccess = hasWhatsAppAccess,
                    checked = clearWhatsApp,
                    onAuthorise = { whatsAppLauncher.launch(null) },
                    onCheck = { if (hasWhatsAppAccess) clearWhatsApp = it }
                )
                MessagingRow(
                    label = "Telegram",
                    hasAccess = hasTelegramAccess,
                    checked = clearTelegram,
                    onAuthorise = { telegramLauncher.launch(null) },
                    onCheck = { if (hasTelegramAccess) clearTelegram = it }
                )

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.runAnalysis(buildParams()) },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Analyser")
                }
            }

            // --- ANALYSE EN COURS ---
            is CleanerViewModel.CleanState.Analysing -> {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Analyse en cours...", style = MaterialTheme.typography.bodyLarge)
            }

            // --- RÉSULTAT ANALYSE ---
            is CleanerViewModel.CleanState.AnalysisDone -> {
                Text(
                    "Rapport d'analyse",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(4.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.result.categories.forEach { cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    formatSize(cat.sizeBytes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cat.sizeBytes > 0)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (cat.filesCount > 0) {
                                Text(
                                    "${cat.filesCount} fichier(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider()
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total", fontWeight = FontWeight.Bold)
                            Text(
                                formatSize(s.result.totalBytes),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.runCleaner() },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Nettoyer maintenant")
                }
                OutlinedButton(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Modifier la sélection")
                }
            }

            // --- NETTOYAGE EN COURS ---
            is CleanerViewModel.CleanState.Running -> {
                Spacer(modifier = Modifier.height(32.dp))
                CircularProgressIndicator()
                Spacer(modifier = Modifier.height(16.dp))
                Text("Nettoyage en cours...", style = MaterialTheme.typography.bodyLarge)
            }

            // --- COMPTE RENDU FINAL ---
            is CleanerViewModel.CleanState.Done -> {
                Text(
                    "Nettoyage terminé !",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.height(4.dp))

                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        s.details.forEach { cat ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(cat.name, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    formatSize(cat.sizeBytes),
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (cat.sizeBytes > 0)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (cat.filesCount > 0) {
                                Text(
                                    "${cat.filesCount} fichier(s) supprimé(s)",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            HorizontalDivider()
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Total libéré", fontWeight = FontWeight.Bold)
                            Text(
                                formatSize(s.totalBytes),
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        if (s.errors.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                "${s.errors.size} erreur(s) mineures",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = { viewModel.reset() },
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Text("Recommencer")
                }
            }

            // --- ERREUR ---
            is CleanerViewModel.CleanState.Error -> {
                Text(
                    "Erreur : ${s.message}",
                    color = MaterialTheme.colorScheme.error
                )
                OutlinedButton(onClick = { viewModel.reset() }) {
                    Text("Réessayer")
                }
            }
        }
    }
}

@Composable
fun MessagingRow(
    label: String,
    hasAccess: Boolean,
    checked: Boolean,
    onAuthorise: () -> Unit,
    onCheck: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = if (hasAccess) "Accès autorisé ✓" else "Accès requis",
                style = MaterialTheme.typography.bodySmall,
                color = if (hasAccess) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.error
            )
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            if (!hasAccess) {
                TextButton(onClick = onAuthorise) { Text("Autoriser") }
            }
            Switch(
                checked = checked && hasAccess,
                onCheckedChange = onCheck,
                enabled = hasAccess
            )
        }
    }
}

@Composable
fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleSmall,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
    )
    HorizontalDivider()
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

fun formatSize(bytes: Long): String {
    return when {
        bytes >= 1_048_576 -> "%.1f Mo".format(bytes / 1_048_576.0)
        bytes >= 1_024     -> "%.1f Ko".format(bytes / 1_024.0)
        else               -> "$bytes octets"
    }
}