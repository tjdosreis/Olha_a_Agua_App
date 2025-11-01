package com.example.olhaagua

// Imports do ViewModel, Application, etc.
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

// Imports do Compose (UI)
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text

// Imports do Compose (Runtime e Animação)
import androidx.compose.animation.animateColorAsState // <-- NOVO IMPORT
import androidx.compose.animation.core.tween // <-- NOVO IMPORT
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color // <-- NOVO IMPORT
import androidx.compose.ui.graphics.StrokeCap // <-- NOVO IMPORT
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
import java.util.Calendar
import java.util.Date

// --- O CÉREBRO DA TELA (VIEWMODEL) ---
// (Sem mudanças aqui, está 100% correto)
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
        waterLogDao.getTotalAmountForPeriod(hojeInicio, hojeFim)
    ) { meta, total ->
        TelaPrincipalUiState(
            metaDiaria = meta,
            totalBebidoHoje = total ?: 0
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
    val totalBebidoHoje: Int = 0
)

// --- A TELA EM SI (COMPOSABLE - ATUALIZADA!) ---
@Composable
fun TelaPrincipal(
    modifier: Modifier = Modifier,
    viewModel: TelaPrincipalViewModel = viewModel(
        factory = TelaPrincipalViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val uiState by viewModel.uiState.collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {

        // --- NOSSO NOVO CÍRCULO DE PROGRESSO POLIDO ---

        // 1. Calcula o progresso (um valor 'float' entre 0.0 e 1.0)
        val progresso = if (uiState.metaDiaria > 0) {
            uiState.totalBebidoHoje.toFloat() / uiState.metaDiaria.toFloat()
        } else {
            0.0f
        }

        // 2. Define as cores para a animação
        val corPrimaria = MaterialTheme.colorScheme.primary // Azul
        val corSucesso = Color(0xFF4CAF50) // Verde Sucesso (pode usar Color.Green)

        // 3. Determina a cor alvo
        val corAlvo = if (progresso >= 1.0f) corSucesso else corPrimaria

        // 4. Anima a mudança de cor
        val corAnimada = animateColorAsState(
            targetValue = corAlvo,
            label = "ProgressColorAnimation",
            animationSpec = tween(durationMillis = 500) // Animação de 0.5s
        )

        // 5. A "Caixa" para empilhar os componentes
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.size(200.dp)
        ) {

            // O Círculo de "fundo" (a trilha cinza)
            CircularProgressIndicator(
                progress = { 1.0f },
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 16.dp,
            )

            // O Círculo de "progresso" (o que se move)
            CircularProgressIndicator(
                progress = { progresso.coerceIn(0.0f, 1.0f) }, // Garante que não passe de 1.0
                modifier = Modifier.fillMaxSize(),
                color = corAnimada.value, // <-- USA A COR ANIMADA!
                strokeWidth = 16.dp,
                strokeCap = StrokeCap.Round
            )

            // O Texto (no centro da caixa)
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = "${uiState.totalBebidoHoje} ml",
                    style = MaterialTheme.typography.headlineLarge
                )
                Text(
                    text = "de ${uiState.metaDiaria} ml",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }

        // --- FIM DO NOVO CÍRCULO ---

        Spacer(modifier = Modifier.height(32.dp))

        // --- BOTÕES DE AÇÃO (Sem mudanças) ---
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
    }
}

// --- A "FÁBRICA" QUE CRIA O VIEWMODEL ---
// (Sem mudanças aqui, está 100% correto)
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

// --- PREVIEW (Sem mudanças) ---
@Preview(showBackground = true)
@Composable
fun TelaPrincipalPreview() {
    OlhaAÁguaTheme {
        // ... (o preview continua o mesmo)
        Column(modifier = Modifier.fillMaxSize().padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            Text(text = "250 ml / 2500 ml", style = MaterialTheme.typography.headlineLarge)
        }
    }
}