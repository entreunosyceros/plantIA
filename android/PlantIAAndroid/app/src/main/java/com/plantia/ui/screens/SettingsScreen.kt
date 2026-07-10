package com.plantia.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.plantia.api.PlantIAClient
import com.plantia.data.ServerPreferences
import com.plantia.data.ThemeMode
import com.plantia.ui.components.ErrorBanner
import com.plantia.ui.components.PlantTopBar
import com.plantia.util.userMessage
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    onThemeModeChange: (ThemeMode) -> Unit = {},
    onOpenAbout: () -> Unit,
) {
    val context = LocalContext.current
    val prefs = remember { ServerPreferences(context) }
    val scope = rememberCoroutineScope()

    var serverUrl by remember { mutableStateOf(prefs.baseUrl) }
    var testing by remember { mutableStateOf(false) }
    var saving by remember { mutableStateOf(false) }
    var result by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = { PlantTopBar(title = "Ajustes") },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("Conexión LAN", style = MaterialTheme.typography.titleLarge)
            Text(
                "Introduce la URL de tu servidor PlantIA (IP y puerto en la red local).",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            OutlinedTextField(
                value = serverUrl,
                onValueChange = { serverUrl = it },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("URL del servidor") },
                placeholder = { Text("http://192.168.1.96:8000/") },
                singleLine = true,
                supportingText = {
                    Text("Ejemplo: http://192.168.1.96:8000/", fontFamily = FontFamily.Monospace)
                },
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                Button(
                    onClick = {
                        saving = true
                        error = null
                        result = null
                        scope.launch {
                            try {
                                val normalized = ServerPreferences.normalizeBaseUrl(serverUrl)
                                prefs.baseUrl = normalized
                                serverUrl = normalized
                                PlantIAClient.invalidate()
                                result = "Servidor guardado"
                            } catch (e: Exception) {
                                error = e.userMessage("No se pudo guardar")
                            } finally {
                                saving = false
                            }
                        }
                    },
                    enabled = !saving && !testing,
                    modifier = Modifier.weight(1f),
                ) {
                    Text(if (saving) "Guardando…" else "Guardar URL")
                }

                Button(
                    onClick = {
                        testing = true
                        error = null
                        result = null
                        scope.launch {
                            try {
                                val normalized = ServerPreferences.normalizeBaseUrl(serverUrl)
                                prefs.baseUrl = normalized
                                PlantIAClient.invalidate()
                                serverUrl = normalized
                                val resp = PlantIAClient.api(context).listar(page = 1, pageSize = 1)
                                result = "Conectado · ${resp.total} planta(s)"
                            } catch (e: Exception) {
                                error = e.userMessage("No se pudo conectar")
                            } finally {
                                testing = false
                            }
                        }
                    },
                    enabled = !saving && !testing,
                    modifier = Modifier.weight(1f),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (testing) {
                            CircularProgressIndicator(
                                strokeWidth = 2.dp,
                                modifier = Modifier
                                    .padding(end = 8.dp)
                                    .size(18.dp),
                            )
                        }
                        Text(if (testing) "…" else "Probar")
                    }
                }
            }

            if (result != null) {
                Text(result!!, color = MaterialTheme.colorScheme.primary)
            }
            if (error != null) {
                ErrorBanner(message = error!!)
            }

            Text("Apariencia", style = MaterialTheme.typography.titleLarge)
            Text(
                "Elige el tema de la app. «Sistema» sigue la configuración del dispositivo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectableGroup()
                        .padding(8.dp),
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val label = when (mode) {
                            ThemeMode.SYSTEM -> "Sistema"
                            ThemeMode.LIGHT -> "Claro"
                            ThemeMode.DARK -> "Oscuro"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .selectable(
                                    selected = themeMode == mode,
                                    onClick = { onThemeModeChange(mode) },
                                    role = Role.RadioButton,
                                )
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = themeMode == mode,
                                onClick = null,
                            )
                            Text(label, modifier = Modifier.padding(start = 8.dp))
                        }
                    }
                }
            }

            OutlinedCard(
                onClick = onOpenAbout,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        androidx.compose.material3.Icon(Icons.Default.Info, contentDescription = null)
                        Column {
                            Text("Acerca de PlantIA", style = MaterialTheme.typography.titleMedium)
                            Text(
                                "Información y código fuente",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                    androidx.compose.material3.Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null)
                }
            }
        }
    }
}
