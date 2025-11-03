package com.example.olhaagua

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel

// Imports do Compose (Layout)
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
import androidx.compose.foundation.lazy.itemsIndexed // <-- MUDANÇA: de 'items' para 'itemsIndexed'

// Imports do Compose (UI)
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider // <-- NOVO IMPORT
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api

// Imports do Compose (Runtime e Animação)
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Import do nosso Tema (com acento!)
import com.example.olhaagua.ui.theme.OlhaAÁguaTheme

// Imports do Kotlin Coroutines e Datas
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
    // ... (Todo o código do ViewModel que já tínhamos)
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
}

// Classe de dados para representar o "Estado" da UI
// (Sem mudanças aqui)
data class TelaPrincipalUiState(
    val metaDiaria: Int = 0,
    val totalBebidoHoje: Int = 0,
    val historicoDeHoje: List<WaterLog> = emptyList()
)

// --- A TELA EM SI (COMPOSABLE - ATUALIZADA!) ---
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

            // --- CÍRCULO DE PROGRESSO ---
            Spacer(modifier = Modifier.height(32.dp))
            // ... (O código do Círculo de Progresso fica aqui, sem mudanças)
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

            // --- BOTÕES DE AÇÃO ---
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

            // --- ATUALIZAÇÃO: LISTA DE HISTÓRICO COM DIVISOR ---
            Text(
                text = "Histórico de hoje:",
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            LazyColumn(modifier = Modifier.fillMaxWidth()) {
                // MUDANÇA: Usamos 'itemsIndexed' para saber qual é o último item
                itemsIndexed(uiState.historicoDeHoje) { index, log ->
                    Column { // Um Column para conter a Linha E o Divisor
                        // Desenha um item da lista
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "+ ${log.amount} ml",
                                style = MaterialTheme.typography.bodyLarge
                            )
                            Text(
                                text = timeFormatter.format(log.timestamp),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        // --- SUA SUGESTÃO (A LINHA SUTIL) ---
                        // Adiciona o divisor se NÃO for o último item da lista
                        if (index < uiState.historicoDeHoje.lastIndex) {
                            Divider(
                                color = MaterialTheme.colorScheme.outlineVariant, // Cinza sutil
                                modifier = Modifier.padding(horizontal = 16.dp) // Não toca as bordas
                            )
                        }
                        // --- FIM DA SUGESTÃO ---
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