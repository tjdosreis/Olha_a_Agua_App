package com.example.olhaagua

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel

// Imports do Compose (Layout)
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
// --- NOVOS IMPORTS ---
import androidx.compose.foundation.lazy.rememberLazyListState
// --- FIM DOS NOVOS IMPORTS ---

// Imports do Compose (UI)
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.rememberSwipeToDismissBoxState


// Imports do Compose (Runtime e Animação)
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// --- NOVOS IMPORTS ---
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
// --- FIM DOS NOVOS IMPORTS ---
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.olhaagua.ui.theme.OlhaAÁguaTheme
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// --- O CÉREBRO DA TELA (VIEWMODEL) ---
// (Sem mudanças aqui)
class TelaPrincipalViewModel(
    private val settingsRepo: SettingsRepository,
    private val waterLogDao: WaterLogDao
) : ViewModel() {

    private val hojeInicio: Date
    private val hojeFim: Date

    init {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        hojeInicio = calendar.time
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        hojeFim = calendar.time
    }

    val uiState: StateFlow<TelaPrincipalUiState> = combine(
        settingsRepo.metaDiariaFlow,
        waterLogDao.getTotalAmountForPeriod(hojeInicio, hojeFim),
        waterLogDao.getLogsForPeriod(hojeInicio, hojeFim)
    ) { meta, total, historico ->
        TelaPrincipalUiState(
            metaDiaria = meta,
            totalBebidoHoje = total ?: 0,
            historicoDeHoje = historico
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = TelaPrincipalUiState()
    )

    fun adicionarAgua(quantidade: Int) {
        viewModelScope.launch {
            val log = WaterLog(
                amount = quantidade,
                timestamp = Date()
            )
            waterLogDao.insert(log)
        }
    }

    fun deletarRegistro(log: WaterLog) {
        viewModelScope.launch {
            waterLogDao.delete(log)
        }
    }
}

// Classe de dados para representar o "Estado" da UI
data class TelaPrincipalUiState(
    val metaDiaria: Int = 0,
    val totalBebidoHoje: Int = 0,
    val historicoDeHoje: List<WaterLog> = emptyList()
)

// --- A TELA EM SI (COMPOSABLE - VERSÃO FINAL) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaPrincipal(
    modifier: Modifier = Modifier,
    onConfigClick: () -> Unit,
    viewModel: TelaPrincipalViewModel = viewModel(
        factory = TelaPrincipalViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()
    val timeFormatter = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }

    // --- NOSSA NOVA MUDANÇA (Início) ---
    // 1. Criamos um estado para o LazyColumn
    val listState = rememberLazyListState()
    // 2. Criamos uma variável para lembrar o tamanho anterior da lista
    var previousListSize by remember { mutableStateOf(uiState.historicoDeHoje.size) }

    // 3. Este bloco é executado sempre que o tamanho da lista mudar
    LaunchedEffect(uiState.historicoDeHoje.size) {
        val currentListSize = uiState.historicoDeHoje.size
        // 4. Se o novo tamanho for MAIOR, significa que adicionamos um item
        if (currentListSize > previousListSize) {
            // 5. Rolamos a lista (com animação) para o item 0 (o topo)
            listState.animateScrollToItem(index = 0)
        }
        // 6. Atualizamos o "tamanho anterior" para a próxima comparação
        previousListSize = currentListSize
    }
    // --- FIM DA MUDANÇA ---


    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Olha a Água!") },
                actions = {
                    IconButton(onClick = onConfigClick) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Configurações"
                        )
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

            // --- CÍRCULO DE PROGRESSO (Sem mudanças) ---
            Spacer(modifier = Modifier.height(32.dp))
            val progresso = if (uiState.metaDiaria > 0) {
                uiState.totalBebidoHoje.toFloat() / uiState.metaDiaria.toFloat()
            } else { 0.0f }
            val corPrimaria = MaterialTheme.colorScheme.primary
            val corSucesso = Color(0xFF4CAF50)
            val corAlvo = if (progresso >= 1.0f) corSucesso else corPrimaria
            val corAnimada = animateColorAsState(
                targetValue = corAlvo, label = "ProgressColorAnimation", animationSpec = tween(durationMillis = 500)
            )
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(200.dp)
            ) {
                CircularProgressIndicator(progress = { 1.0f }, modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceVariant, strokeWidth = 16.dp)
                CircularProgressIndicator(
                    progress = { progresso.coerceIn(0.0f, 1.0f) },
                    modifier = Modifier.fillMaxSize(),
                    color = corAnimada.value,
                    strokeWidth = 16.dp,
                    strokeCap = StrokeCap.Round
                )
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${uiState.totalBebidoHoje} ml", style = MaterialTheme.typography.headlineLarge)
                    Text(text = "de ${uiState.metaDiaria} ml", style = MaterialTheme.typography.bodyMedium)
                }
            }

            // --- BOTÕES DE AÇÃO (Sem mudanças) ---
            // A lógica de rolagem agora é automática (no LaunchedEffect)
            // então não precisamos mudar os botões.
            Spacer(modifier = Modifier.height(32.dp))
            Text(
                text = "Adicionar consumo:",
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center
            ) {
                Button(onClick = { viewModel.adicionarAgua(250) }) {
                    Text("+250 ml")
                }
                Spacer(modifier = Modifier.width(16.dp))
                Button(onClick = { viewModel.adicionarAgua(500) }) {
                    Text("+500 ml")
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // --- LISTA DE HISTÓRICO (COM SWIPE E BOTÃO DELETAR) ---
            Text(
                text = "Histórico de hoje:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(
                modifier = Modifier.fillMaxWidth(),
                // --- NOSSA NOVA MUDANÇA ---
                state = listState // <-- Conectamos o estado à lista
                // --- FIM DA MUDANÇA ---
            ) {

                itemsIndexed(
                    items = uiState.historicoDeHoje,
                    key = { _, log -> log.id }
                ) { index, log ->

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart) {
                                viewModel.deletarRegistro(log)
                                true
                            } else {
                                false
                            }
                        },
                        positionalThreshold = { it * 0.25f }
                    )

                    SwipeToDismissBox(
                        state = dismissState,
                        enableDismissFromStartToEnd = false,
                        enableDismissFromEndToStart = true,

                        // O FUNDO VERMELHO (O que aparece por baixo)
                        backgroundContent = {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(vertical = 8.dp)
                                    .let {
                                        if (index < uiState.historicoDeHoje.lastIndex) {
                                            it.padding(bottom = 1.dp)
                                        } else {
                                            it
                                        }
                                    }
                                    .background(MaterialTheme.colorScheme.error),
                                contentAlignment = Alignment.CenterEnd,
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Excluir",
                                    tint = MaterialTheme.colorScheme.onError,
                                    modifier = Modifier.padding(end = 24.dp)
                                )
                            }
                        }
                    ) {
                        // O CONTEÚDO FRONTAL (O que o usuário vê)
                        Column(
                            modifier = Modifier.background(MaterialTheme.colorScheme.background)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Coluna para o texto
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = "+ ${log.amount} ml",
                                        style = MaterialTheme.typography.bodyLarge
                                    )
                                    Text(
                                        text = timeFormatter.format(log.timestamp),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // O ÍCONE 'X' (Dica Visual)
                                IconButton(onClick = { viewModel.deletarRegistro(log) }) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remover registro",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            // Divisor (sem mudança)
                            if (index < uiState.historicoDeHoje.lastIndex) {
                                Divider(
                                    color = MaterialTheme.colorScheme.outlineVariant,
                                    modifier = Modifier.padding(horizontal = 16.dp)
                                )
                            }
                        }
                    }
                }
            }
            // --- FIM DA LISTA DE HISTÓRICO ---
        }
    }
}

// --- FÁBRICA E PREVIEW (Sem mudanças) ---
class TelaPrincipalViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
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
    OlhaAÁguaTheme {
        TelaPrincipal(onConfigClick = {})
    }
}