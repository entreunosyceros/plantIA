package com.plantia.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.plantia.api.PlantIAClient
import com.plantia.api.models.PlantaResponse
import com.plantia.api.models.PlantaUpdateRequest
import com.plantia.domain.JsonHelpers
import com.plantia.reminders.WaterReminder
import com.plantia.reminders.WaterReminderStore
import com.plantia.ui.components.CandidateCard
import com.plantia.ui.components.CareGrid
import com.plantia.ui.components.ConfianzaBadge
import com.plantia.ui.components.DeletePlantDialog
import com.plantia.ui.components.ErrorBanner
import com.plantia.ui.components.ImageLightbox
import com.plantia.ui.components.DetailInfoSection
import com.plantia.ui.components.JournalTimeline
import com.plantia.ui.components.MoonCard
import com.plantia.ui.components.PlantSnackbarHost
import com.plantia.ui.components.PlantTopBar
import com.plantia.ui.components.SectionCard
import com.plantia.ui.components.rememberSnackbarHostState
import com.plantia.util.MoonCalculator
import com.plantia.util.appendJournalLine
import com.plantia.util.formatJournalDate
import com.plantia.util.imageUrlFromPath
import com.plantia.util.userMessage
import kotlinx.coroutines.launch

private data class JournalChip(val label: String, val template: String)

private val journalChips = listOf(
    JournalChip("🧪 Abonada", "Abonada el {fecha}"),
    JournalChip("🪴 Trasplantada", "Trasplantada el {fecha}"),
    JournalChip("✂️ Podada", "Podada el {fecha}"),
    JournalChip("🌸 Floreció", "Floreció el {fecha}"),
    JournalChip("🌱 Nuevo sustrato", "Cambié el sustrato el {fecha}"),
)

private val reminderIntervals = listOf(1, 2, 3, 5, 7, 14)
private val reminderHours = (6..21).toList()

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DetailScreen(
    plantaId: Int,
    onBack: () -> Unit,
    onReIdentify: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val scroll = rememberScrollState()
    val uriHandler = LocalUriHandler.current
    val snackbar = rememberSnackbarHostState()
    val reminderStore = remember { WaterReminderStore(context) }
    val baseUrl = PlantIAClient.baseUrl(context)
    val moon = remember { MoonCalculator.moonInfo() }

    var loading by remember { mutableStateOf(true) }
    var saving by remember { mutableStateOf(false) }
    var deleting by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showLightbox by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var planta by remember { mutableStateOf<PlantaResponse?>(null) }
    var notas by remember { mutableStateOf("") }
    var reminderEnabled by remember { mutableStateOf(false) }
    var reminderDays by remember { mutableStateOf(3) }
    var reminderHour by remember { mutableStateOf(9) }
    var reminderMenuExpanded by remember { mutableStateOf(false) }
    var reminderHourMenuExpanded by remember { mutableStateOf(false) }

    fun loadReminderState() {
        val r = reminderStore.get(plantaId)
        if (r != null) {
            reminderEnabled = r.enabled
            reminderDays = r.intervalDays
            reminderHour = r.hourOfDay
        } else {
            reminderEnabled = false
            reminderDays = 3
            reminderHour = 9
        }
    }

    fun persistReminder(name: String) {
        if (reminderEnabled) {
            val existing = reminderStore.get(plantaId)
            reminderStore.save(
                WaterReminder(
                    plantId = plantaId,
                    plantName = name,
                    intervalDays = reminderDays,
                    lastWateredMillis = existing?.lastWateredMillis ?: System.currentTimeMillis(),
                    enabled = true,
                    hourOfDay = reminderHour,
                ),
            )
        } else {
            reminderStore.delete(plantaId)
        }
    }

    suspend fun saveNotas(showSnack: Boolean = true) {
        val p = planta ?: return
        saving = true
        try {
            val request = PlantaUpdateRequest(notas_usuario = notas)
            val updated = try {
                PlantIAClient.api(context).guardarNotas(p.id, request)
            } catch (_: Exception) {
                PlantIAClient.api(context).actualizar(p.id, request)
            }
            planta = updated
            notas = updated.notas_usuario
            error = null
            if (showSnack) {
                snackbar.showSnackbar("Cuaderno guardado", duration = SnackbarDuration.Short)
            }
        } catch (e: Exception) {
            error = e.userMessage("No se pudo guardar")
        } finally {
            saving = false
        }
    }

    suspend fun load() {
        loading = true
        error = null
        try {
            val p = PlantIAClient.api(context).detalle(plantaId)
            planta = p
            notas = p.notas_usuario
            loadReminderState()
        } catch (e: Exception) {
            error = e.userMessage("Error al cargar la ficha")
        } finally {
            loading = false
        }
    }

    LaunchedEffect(plantaId) { load() }

    if (showDeleteDialog) {
        val name = planta?.nombre_comun ?: "esta planta"
        DeletePlantDialog(
            plantName = name,
            onDismiss = { showDeleteDialog = false },
            onConfirm = {
                showDeleteDialog = false
                if (deleting) return@DeletePlantDialog
                deleting = true
                scope.launch {
                    try {
                        PlantIAClient.api(context).eliminar(plantaId)
                        reminderStore.delete(plantaId)
                        onBack()
                    } catch (e: Exception) {
                        error = e.userMessage("No se pudo borrar")
                        deleting = false
                    }
                }
            },
        )
    }

    Scaffold(
        topBar = {
            PlantTopBar(
                title = planta?.nombre_comun ?: "Ficha",
                onBack = onBack,
            )
        },
        snackbarHost = { PlantSnackbarHost(snackbar) },
    ) { padding ->
        when {
            loading -> {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    CircularProgressIndicator()
                }
            }
            error != null && planta == null -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .padding(16.dp),
                ) {
                    ErrorBanner(message = error!!, onRetry = { scope.launch { load() } })
                }
            }
            else -> {
                val p = planta ?: return@Scaffold
                val imageUrl = imageUrlFromPath(p.image_path, baseUrl)

                if (showLightbox) {
                    ImageLightbox(
                        imageUrl = imageUrl,
                        contentDescription = p.nombre_comun,
                        onDismiss = { showLightbox = false },
                    )
                }

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                        .verticalScroll(scroll)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    AsyncImage(
                        model = imageUrl,
                        contentDescription = p.nombre_comun,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { showLightbox = true },
                    )

                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(p.nombre_comun, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                        if (p.nombre_cientifico.isNotBlank()) {
                            Text(p.nombre_cientifico, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        ConfianzaBadge(confianza = p.confianza)
                        OutlinedButton(
                            onClick = onReIdentify,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("Re-identificar con nueva foto")
                        }
                    }

                    MoonCard(moon = moon)

                    if (error != null) {
                        ErrorBanner(message = error!!)
                    }

                    if (p.descripcion.isNotBlank()) {
                        SectionCard(title = "Descripción") { Text(p.descripcion) }
                    }

                    val careItems = listOf(
                        "💧" to p.riego,
                        "☀️" to p.luz,
                        "🌡️" to p.temperatura,
                        "💨" to p.humedad,
                        "🪴" to p.suelo,
                        "🧪" to p.fertilizacion,
                    ).filter { it.second.isNotBlank() }

                    if (careItems.isNotEmpty()) {
                        SectionCard(title = "Cuidados") {
                            CareGrid(items = careItems)
                        }
                    }

                    DetailInfoSection(
                        title = "Características",
                        items = listOf(
                            "Dificultad" to p.dificultad,
                            "Tamaño adulto" to p.tamano_adulto,
                            "Crecimiento" to p.crecimiento,
                            "Floración" to p.floracion,
                        ),
                    )

                    DetailInfoSection(
                        title = "Mantenimiento",
                        items = listOf(
                            "Época de poda" to p.epoca_poda,
                            "Época de trasplante" to p.epoca_trasplante,
                            "Señales de trasplante" to p.senales_trasplante,
                            "Maceta y sustrato" to p.maceta_y_sustrato,
                        ),
                    )

                    DetailInfoSection(
                        title = "Seguridad y problemas",
                        items = listOf(
                            "Toxicidad perros" to p.toxicidad_perros,
                            "Toxicidad gatos" to p.toxicidad_gatos,
                            "Problemas comunes" to p.problemas_comunes,
                            "Plagas habituales" to p.plagas_habituales,
                        ),
                    )

                    SectionCard(
                        title = "Recordatorio de riego",
                        subtitle = "Notificación local cuando toque regar.",
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text("Activar recordatorio")
                            Switch(
                                checked = reminderEnabled,
                                onCheckedChange = {
                                    reminderEnabled = it
                                    persistReminder(p.nombre_comun)
                                },
                            )
                        }
                        if (reminderEnabled) {
                            ExposedDropdownMenuBox(
                                expanded = reminderMenuExpanded,
                                onExpandedChange = { reminderMenuExpanded = it },
                            ) {
                                OutlinedTextField(
                                    value = "Cada $reminderDays día${if (reminderDays == 1) "" else "s"}",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Frecuencia") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderMenuExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                )
                                ExposedDropdownMenu(
                                    expanded = reminderMenuExpanded,
                                    onDismissRequest = { reminderMenuExpanded = false },
                                ) {
                                    reminderIntervals.forEach { days ->
                                        DropdownMenuItem(
                                            text = { Text("Cada $days día${if (days == 1) "" else "s"}") },
                                            onClick = {
                                                reminderDays = days
                                                reminderMenuExpanded = false
                                                persistReminder(p.nombre_comun)
                                            },
                                        )
                                    }
                                }
                            }
                            ExposedDropdownMenuBox(
                                expanded = reminderHourMenuExpanded,
                                onExpandedChange = { reminderHourMenuExpanded = it },
                            ) {
                                OutlinedTextField(
                                    value = "Aviso a las ${"%02d".format(reminderHour)}:00",
                                    onValueChange = {},
                                    readOnly = true,
                                    label = { Text("Hora del aviso") },
                                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = reminderHourMenuExpanded) },
                                    modifier = Modifier
                                        .menuAnchor()
                                        .fillMaxWidth(),
                                )
                                ExposedDropdownMenu(
                                    expanded = reminderHourMenuExpanded,
                                    onDismissRequest = { reminderHourMenuExpanded = false },
                                ) {
                                    reminderHours.forEach { hour ->
                                        DropdownMenuItem(
                                            text = { Text("${"%02d".format(hour)}:00") },
                                            onClick = {
                                                reminderHour = hour
                                                reminderHourMenuExpanded = false
                                                persistReminder(p.nombre_comun)
                                            },
                                        )
                                    }
                                }
                            }
                        }
                    }

                    val rasgos = JsonHelpers.parseStringList(p.rasgos_observados_json)
                    if (rasgos.isNotEmpty()) {
                        SectionCard(title = "Rasgos observados") {
                            rasgos.forEach { Text("• $it") }
                        }
                    }

                    val preguntas = JsonHelpers.parseStringList(p.preguntas_para_mejorar_json)
                    if (preguntas.isNotEmpty()) {
                        SectionCard(
                            title = "Preguntas para afinar",
                            subtitle = "Responde en tu cuaderno o con nuevas fotos.",
                        ) {
                            preguntas.forEachIndexed { i, q -> Text("${i + 1}. $q") }
                        }
                    }

                    val guia = JsonHelpers.parseGuia(p.guia_inicio_json)
                    if (guia.pasos.isNotEmpty() || guia.errores_comunes.isNotEmpty()) {
                        SectionCard(title = "Empezar hoy") {
                            guia.pasos.forEachIndexed { i, paso -> Text("${i + 1}. $paso") }
                            if (guia.errores_comunes.isNotEmpty()) {
                                Text("Errores comunes:", fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = 8.dp))
                                guia.errores_comunes.forEach { Text("⚠ $it") }
                            }
                        }
                    }

                    val candidatos = JsonHelpers.parseCandidatos(p.candidatos_json)
                    if (candidatos.isNotEmpty()) {
                        SectionCard(title = "Otras posibilidades") {
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                candidatos.forEach { c ->
                                    CandidateCard(
                                        candidato = c,
                                        onOpenWiki = { url -> uriHandler.openUri(url) },
                                    )
                                }
                            }
                        }
                    }

                    SectionCard(
                        title = "Cuaderno de la planta",
                        subtitle = "Registra riegos, trasplantes y observaciones.",
                    ) {
                        Button(
                            onClick = {
                                val line = "Regada el ${formatJournalDate()}"
                                notas = appendJournalLine(notas, line)
                                reminderStore.markWatered(plantaId, p.nombre_comun)
                                scope.launch { saveNotas(showSnack = true) }
                            },
                            enabled = !saving && !deleting,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text("💧 Regada hoy")
                        }

                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            journalChips.forEach { chip ->
                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                    modifier = Modifier.clickable {
                                        val line = chip.template.replace("{fecha}", formatJournalDate())
                                        notas = appendJournalLine(notas, line)
                                    },
                                ) {
                                    Text(
                                        chip.label,
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        style = MaterialTheme.typography.labelMedium,
                                    )
                                }
                            }
                        }

                        Text("Historial", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                        JournalTimeline(notas = notas)

                        OutlinedTextField(
                            value = notas,
                            onValueChange = { notas = it },
                            modifier = Modifier.fillMaxWidth(),
                            minLines = 4,
                            label = { Text("Editar observaciones") },
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            Button(
                                onClick = {
                                    if (saving) return@Button
                                    scope.launch { saveNotas(showSnack = true) }
                                },
                                enabled = !saving && !deleting,
                                modifier = Modifier.weight(1f),
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    if (saving) {
                                        CircularProgressIndicator(
                                            strokeWidth = 2.dp,
                                            modifier = Modifier
                                                .padding(end = 8.dp)
                                                .size(18.dp),
                                        )
                                    }
                                    Text(if (saving) "Guardando…" else "Guardar")
                                }
                            }

                            OutlinedButton(
                                onClick = { showDeleteDialog = true },
                                enabled = !saving && !deleting,
                            ) {
                                Text(if (deleting) "Borrando…" else "Eliminar")
                            }
                        }
                    }
                }
            }
        }
    }
}
