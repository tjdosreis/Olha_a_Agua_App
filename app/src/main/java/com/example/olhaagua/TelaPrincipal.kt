package com.example.olhaagua

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel

// Imports do Compose
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState

// Imports UI Material 3
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Info // Ícone para o card
import androidx.compose.material3.*

// Imports Animação e Runtime
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.olhaagua.ui.theme.OlhaAÁguaTheme

// Imports Kotlin Coroutines
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class OpcaoFiltro(val label: String) {
    DIAS_7("7 dias"),
    DIAS_30("30 dias"),
    TUDO("Tudo")
}

// --- VIEWMODEL (SEM MUDANÇAS) ---
class TelaPrincipalViewModel(
    private val settingsRepo: SettingsRepository,
    private val waterLogDao: WaterLogDao
) : ViewModel() {

    private val hojeInicio: Date
    private val hojeFim: Date

    private val _filtroSelecionado = MutableStateFlow(OpcaoFiltro.DIAS_7)
    val filtroSelecionado: StateFlow<OpcaoFiltro> = _filtroSelecionado

    init {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        hojeInicio = calendar.time
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        hojeFim = calendar.time
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private val historicoGeralFiltrado = _filtroSelecionado.flatMapLatest { filtro ->
        val calendar = Calendar.getInstance()
        val fim = Date()
        when (filtro) {
            OpcaoFiltro.DIAS_7 -> {
                calendar.add(Calendar.DAY_OF_YEAR, -7)
                waterLogDao.getLogsForPeriod(calendar.time, fim)
            }
            OpcaoFiltro.DIAS_30 -> {
                calendar.add(Calendar.DAY_OF_YEAR, -30)
                waterLogDao.getLogsForPeriod(calendar.time, fim)
            }
            OpcaoFiltro.TUDO -> waterLogDao.getAllLogs()
        }
    }

    val uiState: StateFlow<TelaPrincipalUiState> = combine(
        settingsRepo.metaDiariaFlow,
        waterLogDao.getTotalAmountForPeriod(hojeInicio, hojeFim),
        waterLogDao.getLogsForPeriod(hojeInicio, hojeFim),
        historicoGeralFiltrado
    ) { meta, total, historicoHoje, historicoGeral ->
        TelaPrincipalUiState(
            metaDiaria = meta,
            totalBebidoHoje = total ?: 0,
            historicoDeHoje = historicoHoje,
            historicoGeral = historicoGeral
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TelaPrincipalUiState()
    )

    fun adicionarAgua(quantidade: Int) {
        viewModelScope.launch {
            waterLogDao.insert(WaterLog(amount = quantidade, timestamp = Date()))
        }
    }

    fun deletarRegistro(log: WaterLog) {
        viewModelScope.launch { waterLogDao.delete(log) }
    }

    fun atualizarFiltro(novoFiltro: OpcaoFiltro) {
        _filtroSelecionado.value = novoFiltro
    }
}

data class TelaPrincipalUiState(
    val metaDiaria: Int = 0,
    val totalBebidoHoje: Int = 0,
    val historicoDeHoje: List<WaterLog> = emptyList(),
    val historicoGeral: List<WaterLog> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPrincipal(
    modifier: Modifier = Modifier,
    onConfigClick: () -> Unit,
    viewModel: TelaPrincipalViewModel = viewModel(
        factory = TelaPrincipalViewModelFactory(LocalContext.current.applicationContext as Application)
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val filtroAtual by viewModel.filtroSelecionado.collectAsState()

    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    var tabIndex by remember { mutableStateOf(0) }
    val tabs = listOf("Hoje", "Geral")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Olha a Água!") },
                actions = {
                    IconButton(onClick = onConfigClick) {
                        Icon(Icons.Default.Settings, contentDescription = "Configurações")
                    }
                }
            )
        }
    ) { innerPadding ->

        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {

            // --- LÓGICA DA ABA "HOJE" (CÍRCULO) ---
            if (tabIndex == 0) {
                Spacer(modifier = Modifier.height(32.dp))
                val progresso = if (uiState.metaDiaria > 0) {
                    uiState.totalBebidoHoje.toFloat() / uiState.metaDiaria.toFloat()
                } else { 0.0f }

                val corAlvo = if (progresso >= 1.0f) Color(0xFF4CAF50) else MaterialTheme.colorScheme.primary
                val corAnimada = animateColorAsState(targetValue = corAlvo, label = "CorProgress", animationSpec = tween(500))

                Box(contentAlignment = Alignment.Center, modifier = Modifier.size(200.dp)) {
                    CircularProgressIndicator(progress = { 1.0f }, modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant, strokeWidth = 16.dp)
                    CircularProgressIndicator(progress = { progresso.coerceIn(0.0f, 1.0f) }, modifier = Modifier.fillMaxSize(), color = corAnimada.value, strokeWidth = 16.dp, strokeCap = StrokeCap.Round)
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = "${uiState.totalBebidoHoje} ml", style = MaterialTheme.typography.headlineLarge)
                        Text(text = "de ${uiState.metaDiaria} ml", style = MaterialTheme.typography.bodyMedium)
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))

                Text(text = "Adicionar consumo:", style = MaterialTheme.typography.bodyLarge)
                Spacer(modifier = Modifier.height(16.dp))
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                    Button(onClick = { viewModel.adicionarAgua(250) }) { Text("+250 ml") }
                    Spacer(modifier = Modifier.width(16.dp))
                    Button(onClick = { viewModel.adicionarAgua(500) }) { Text("+500 ml") }
                }
                Spacer(modifier = Modifier.height(32.dp))
            } else {
                Spacer(modifier = Modifier.height(16.dp))
            }

            // --- ABAS ---
            TabRow(selectedTabIndex = tabIndex) {
                tabs.forEachIndexed { index, title ->
                    Tab(
                        text = { Text(title) },
                        selected = tabIndex == index,
                        onClick = { tabIndex = index }
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            when (tabIndex) {
                0 -> {
                    // Aba HOJE
                    ListaHistorico(
                        lista = uiState.historicoDeHoje,
                        dateFormatter = timeFormatter,
                        onDelete = { viewModel.deletarRegistro(it) },
                        agruparPorData = false,
                        mostrarCardResumo = false // Sem resumo aqui
                    )
                }
                1 -> {
                    // Aba GERAL
                    Column {
                        // Filtros
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OpcaoFiltro.values().forEach { opcao ->
                                val isSelected = filtroAtual == opcao
                                FilterChip(
                                    selected = isSelected,
                                    onClick = { viewModel.atualizarFiltro(opcao) },
                                    label = { Text(opcao.label) },
                                    leadingIcon = if (isSelected) {
                                        { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
                                    } else null
                                )
                            }
                        }

                        // Lista GERAL (Com Resumo!)
                        ListaHistorico(
                            lista = uiState.historicoGeral,
                            dateFormatter = timeFormatter,
                            onDelete = { viewModel.deletarRegistro(it) },
                            agruparPorData = true,
                            mostrarCardResumo = true // Ativa o Card de Resumo
                        )
                    }
                }
            }
        }
    }
}

// --- NOVO COMPONENTE: CARD DE RESUMO ---
@Composable
fun CardResumo(lista: List<WaterLog>) {
    // Cálculos Matemáticos na UI (Simples e eficiente)
    val totalMl = lista.sumOf { it.amount }

    // Calcula dias únicos para a média
    val diasUnicos = lista.map {
        SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(it.timestamp)
    }.distinct().count()

    val mediaDiaria = if (diasUnicos > 0) totalMl / diasUnicos else 0

    // Formatação inteligente (Ml ou Litros)
    val totalTexto = if (totalMl >= 1000) String.format("%.1f L", totalMl / 1000f) else "$totalMl ml"
    val mediaTexto = if (mediaDiaria >= 1000) String.format("%.1f L", mediaDiaria / 1000f) else "$mediaDiaria ml"

    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceAround,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Total no Período", style = MaterialTheme.typography.labelMedium)
                Text(text = totalTexto, style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
            }
            Divider(modifier = Modifier.height(40.dp).width(1.dp), color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.2f))
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = "Média Diária", style = MaterialTheme.typography.labelMedium)
                Text(text = mediaTexto, style = MaterialTheme.typography.titleLarge)
            }
        }
    }
}

// --- LISTA INTELIGENTE (ATUALIZADA) ---
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ListaHistorico(
    lista: List<WaterLog>,
    dateFormatter: SimpleDateFormat,
    onDelete: (WaterLog) -> Unit,
    agruparPorData: Boolean,
    mostrarCardResumo: Boolean // <-- Novo Parâmetro
) {
    val listState = rememberLazyListState()
    var previousListSize by remember { mutableStateOf(lista.size) }

    LaunchedEffect(lista.size) {
        if (lista.size > previousListSize) {
            listState.animateScrollToItem(index = 0)
        }
        previousListSize = lista.size
    }

    val groupedMap = remember(lista, agruparPorData) {
        if (agruparPorData) {
            lista.groupBy { SimpleDateFormat("yyyyMMdd", Locale.getDefault()).format(it.timestamp) }
        } else {
            emptyMap()
        }
    }

    LazyColumn(modifier = Modifier.fillMaxWidth(), state = listState) {

        // --- ITEM 1: CARD DE RESUMO (Se ativado e se houver dados) ---
        if (mostrarCardResumo && lista.isNotEmpty()) {
            item {
                CardResumo(lista)
            }
        }

        if (agruparPorData) {
            groupedMap.forEach { (_, logsDoDia) ->
                // O Cabeçalho (Sticky) com TOTAL DO DIA
                stickyHeader {
                    val dataHeader = formatarDataAmigavel(logsDoDia.first().timestamp)

                    // Soma do dia específico
                    val totalDoDia = logsDoDia.sumOf { it.amount }
                    val totalDoDiaTexto = if (totalDoDia >= 1000) String.format("%.1f L", totalDoDia / 1000f) else "$totalDoDia ml"

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.colorScheme.surface)
                            .padding(vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = dataHeader,
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            // --- TOTAL DO DIA NO CABEÇALHO ---
                            Text(
                                text = totalDoDiaTexto,
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.secondary
                            )
                        }
                    }
                }

                itemsIndexed(items = logsDoDia, key = { _, log -> log.id }) { index, log ->
                    ItemDeHistorico(log, dateFormatter, onDelete, index < logsDoDia.lastIndex)
                }
            }
        } else {
            itemsIndexed(items = lista, key = { _, log -> log.id }) { index, log ->
                ItemDeHistorico(log, dateFormatter, onDelete, index < lista.lastIndex)
            }
        }
    }
}

// --- ITEM e UTILS (Sem mudanças) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ItemDeHistorico(log: WaterLog, dateFormatter: SimpleDateFormat, onDelete: (WaterLog) -> Unit, showDivider: Boolean) {
    val dismissState = rememberSwipeToDismissBoxState(
        confirmValueChange = {
            if (it == SwipeToDismissBoxValue.EndToStart) { onDelete(log); true } else false
        },
        positionalThreshold = { it * 0.25f }
    )

    SwipeToDismissBox(
        state = dismissState,
        enableDismissFromStartToEnd = false,
        enableDismissFromEndToStart = true,
        backgroundContent = {
            Box(
                modifier = Modifier.fillMaxSize().padding(vertical = 8.dp)
                    .let { if (showDivider) it.padding(bottom = 1.dp) else it }
                    .background(MaterialTheme.colorScheme.error),
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(Icons.Default.Delete, "Excluir", tint = MaterialTheme.colorScheme.onError, modifier = Modifier.padding(end = 24.dp))
            }
        }
    ) {
        Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
            Row(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "+ ${log.amount} ml", style = MaterialTheme.typography.bodyLarge)
                    Text(text = dateFormatter.format(log.timestamp), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { onDelete(log) }) {
                    Icon(Icons.Default.Close, "Remover", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            if (showDivider) Divider(color = MaterialTheme.colorScheme.outlineVariant, modifier = Modifier.padding(horizontal = 16.dp))
        }
    }
}

fun formatarDataAmigavel(data: Date): String {
    val calHoje = Calendar.getInstance()
    val calData = Calendar.getInstance().apply { time = data }

    return if (calHoje.get(Calendar.YEAR) == calData.get(Calendar.YEAR) &&
        calHoje.get(Calendar.DAY_OF_YEAR) == calData.get(Calendar.DAY_OF_YEAR)) {
        "Hoje"
    } else {
        calHoje.add(Calendar.DAY_OF_YEAR, -1)
        if (calHoje.get(Calendar.YEAR) == calData.get(Calendar.YEAR) &&
            calHoje.get(Calendar.DAY_OF_YEAR) == calData.get(Calendar.DAY_OF_YEAR)) {
            "Ontem"
        } else {
            SimpleDateFormat("dd 'de' MMMM", Locale("pt", "BR")).format(data)
        }
    }
}

class TelaPrincipalViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TelaPrincipalViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TelaPrincipalViewModel(
                settingsRepo = SettingsRepository(application),
                waterLogDao = AppDatabase.getDatabase(application).waterLogDao()
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}

@Preview(showBackground = true)
@Composable
fun TelaPrincipalPreview() {
    OlhaAÁguaTheme { TelaPrincipal(onConfigClick = {}) }
}