package com.plantia.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.plantia.api.PlantIAClient
import com.plantia.api.models.PlantaResponse
import com.plantia.reminders.WaterReminderStore
import com.plantia.ui.components.ConfianzaBadge
import com.plantia.ui.components.EmptyState
import com.plantia.ui.components.ErrorBanner
import com.plantia.ui.components.PlantSnackbarHost
import com.plantia.ui.components.PlantTopBar
import com.plantia.ui.components.WaterTodayCard
import com.plantia.ui.components.rememberSnackbarHostState
import com.plantia.util.imageUrlFromPath
import com.plantia.util.markPlantWatered
import com.plantia.util.userMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

private data class SortOption(val value: String, val label: String)

private val sortOptions = listOf(
    SortOption("recientes", "Más recientes"),
    SortOption("antiguas", "Más antiguas"),
    SortOption("nombre", "Nombre (A-Z)"),
    SortOption("confianza", "Confianza"),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PlantsScreen(
    onOpenCamera: () -> Unit,
    onOpenDetail: (Int) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val gridState = rememberLazyGridState()
    val baseUrl = PlantIAClient.baseUrl(context)
    val reminderStore = remember { WaterReminderStore(context) }
    val snackbar = rememberSnackbarHostState()
    var dueToday by remember { mutableStateOf(reminderStore.dueToday()) }

    var q by remember { mutableStateOf("") }
    var orden by remember { mutableStateOf("recientes") }
    var sortExpanded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var loadingMore by remember { mutableStateOf(false) }
    var refreshing by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var items by remember { mutableStateOf<List<PlantaResponse>>(emptyList()) }
    var page by remember { mutableIntStateOf(1) }
    var total by remember { mutableIntStateOf(0) }
    var totalPages by remember { mutableIntStateOf(1) }

    val hasMore = page < totalPages

    suspend fun fetchPage(pageNum: Int, replace: Boolean) {
        val resp = PlantIAClient.api(context).listar(
            q = q.trim(),
            page = pageNum,
            pageSize = PAGE_SIZE,
            orden = orden,
        )
        items = if (replace) resp.items else items + resp.items
        page = resp.page
        total = resp.total
        totalPages = resp.total_pages
    }

    suspend fun loadInitial() {
        loading = true
        error = null
        try {
            fetchPage(1, replace = true)
        } catch (e: Exception) {
            error = e.userMessage("Error al cargar")
        } finally {
            loading = false
            refreshing = false
        }
    }

    suspend fun loadMore() {
        if (loadingMore || !hasMore) return
        loadingMore = true
        try {
            fetchPage(page + 1, replace = false)
        } catch (e: Exception) {
            error = e.userMessage("Error al cargar más")
        } finally {
            loadingMore = false
        }
    }

    LaunchedEffect(Unit) { loadInitial() }

    LaunchedEffect(q, orden) {
        delay(350)
        loadInitial()
    }

    LaunchedEffect(gridState, items.size, hasMore) {
        snapshotFlow {
            gridState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
        }
            .distinctUntilChanged()
            .collect { lastVisible ->
                if (lastVisible >= items.size - 4 && hasMore && !loading && !loadingMore) {
                    loadMore()
                }
            }
    }

    Scaffold(
        topBar = { PlantTopBar(title = "Mis plantas") },
        snackbarHost = { PlantSnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(onClick = onOpenCamera) {
                Icon(Icons.Default.CameraAlt, contentDescription = "Identificar planta")
            }
        },
    ) { padding ->
        PullToRefreshBox(
            isRefreshing = refreshing,
            onRefresh = {
                scope.launch {
                    refreshing = true
                    loadInitial()
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    value = q,
                    onValueChange = { q = it },
                    label = { Text("Buscar por nombre") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                    singleLine = true,
                )

                ExposedDropdownMenuBox(
                    expanded = sortExpanded,
                    onExpandedChange = { sortExpanded = it },
                ) {
                    OutlinedTextField(
                        value = sortOptions.first { it.value == orden }.label,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Ordenar por") },
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = sortExpanded) },
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth(),
                    )
                    ExposedDropdownMenu(
                        expanded = sortExpanded,
                        onDismissRequest = { sortExpanded = false },
                    ) {
                        sortOptions.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    orden = option.value
                                    sortExpanded = false
                                },
                            )
                        }
                    }
                }

                when {
                    loading -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    error != null && items.isEmpty() -> {
                        ErrorBanner(message = error!!, onRetry = { scope.launch { loadInitial() } })
                    }
                    items.isEmpty() -> {
                        EmptyState(
                            emoji = "🪴",
                            title = if (q.isBlank()) "Aún no tienes plantas" else "Sin resultados",
                            subtitle = if (q.isBlank()) {
                                "Identifica tu primera planta con la cámara."
                            } else {
                                "Prueba con otro nombre o término de búsqueda."
                            },
                            actionLabel = if (q.isBlank()) "Identificar planta" else null,
                            onAction = if (q.isBlank()) onOpenCamera else null,
                        )
                    }
                    else -> {
                        if (error != null) {
                            ErrorBanner(message = error!!)
                        }
                        Text(
                            "$total planta${if (total == 1) "" else "s"}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Text(
                            "Desliza una tarjeta a la derecha para marcar «Regada hoy»",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            state = gridState,
                            contentPadding = PaddingValues(bottom = 88.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (dueToday.isNotEmpty()) {
                                item(span = { GridItemSpan(2) }) {
                                    WaterTodayCard(duePlants = dueToday)
                                }
                            }
                            items(items, key = { it.id }) { planta ->
                                SwipeablePlantCard(
                                    planta = planta,
                                    baseUrl = baseUrl,
                                    onClick = { onOpenDetail(planta.id) },
                                    onWater = {
                                        scope.launch {
                                            try {
                                                val updated = markPlantWatered(context, planta)
                                                items = items.map { if (it.id == updated.id) updated else it }
                                                dueToday = reminderStore.dueToday()
                                                snackbar.showSnackbar(
                                                    "${planta.nombre_comun} regada",
                                                    duration = SnackbarDuration.Short,
                                                )
                                            } catch (e: Exception) {
                                                snackbar.showSnackbar(
                                                    e.userMessage("No se pudo guardar"),
                                                    duration = SnackbarDuration.Short,
                                                )
                                            }
                                        }
                                    },
                                )
                            }
                            if (loadingMore) {
                                item(span = { GridItemSpan(2) }) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(16.dp),
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        CircularProgressIndicator()
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SwipeablePlantCard(
    planta: PlantaResponse,
    baseUrl: String,
    onClick: () -> Unit,
    onWater: () -> Unit,
) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = { value ->
            if (value == SwipeToDismissBoxValue.StartToEnd) {
                onWater()
            }
            false
        },
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = true,
        enableDismissFromEndToStart = false,
        backgroundContent = {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFF22C55E)),
                contentAlignment = Alignment.CenterStart,
            ) {
                Text(
                    "💧 Regada hoy",
                    modifier = Modifier.padding(horizontal = 16.dp),
                    color = Color.White,
                    style = MaterialTheme.typography.labelLarge,
                )
            }
        },
        content = {
            PlantGridCard(planta = planta, baseUrl = baseUrl, onClick = onClick)
        },
    )
}

@Composable
private fun PlantGridCard(planta: PlantaResponse, baseUrl: String, onClick: () -> Unit) {
    val imageUrl = imageUrlFromPath(planta.image_path, baseUrl)
    val firstNote = planta.notas_usuario.trim().split("\n").firstOrNull().orEmpty()

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column {
            AsyncImage(
                model = imageUrl,
                contentDescription = planta.nombre_comun,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)),
            )
            Column(
                modifier = Modifier.padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text(
                    planta.nombre_comun,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (planta.nombre_cientifico.isNotBlank()) {
                    Text(
                        planta.nombre_cientifico,
                        style = MaterialTheme.typography.bodySmall,
                        fontStyle = FontStyle.Italic,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                ConfianzaBadge(confianza = planta.confianza)
                if (firstNote.isNotBlank()) {
                    Text(
                        "📝 $firstNote",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
