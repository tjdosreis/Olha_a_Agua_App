package com.example.olhaagua

import android.app.Application
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Imports do WorkManager (que vamos precisar)
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// --- A CORREÇÃO ESTÁ AQUI ---
import com.example.olhaagua.ReminderWorker // <-- O import que faltava

// --- O CÉREBRO DA TELA (VIEWMODEL) ---
class TelaConfiguracoesViewModel(
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    val metaDiariaState: StateFlow<Int> = settingsRepo.metaDiariaFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val frequenciaState: StateFlow<Int> = settingsRepo.frequenciaLembreteFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 90)

    // --- NOSSA NOVA MUDANÇA (Início) ---
    val inicioAtivoState: StateFlow<Int> = settingsRepo.periodoAtivoInicioFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8 * 60) // 8h

    val fimAtivoState: StateFlow<Int> = settingsRepo.periodoAtivoFimFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 22 * 60) // 22h
    // --- FIM DA MUDANÇA ---

    fun salvarMetaDiaria(meta: Int) {
        viewModelScope.launch {
            settingsRepo.salvarMetaDiaria(meta)
        }
    }

    fun salvarFrequencia(minutos: Int) {
        viewModelScope.launch {
            settingsRepo.salvarFrequencia(minutos)
        }
    }

    // --- NOSSA NOVA MUDANÇA (Início) ---
    fun salvarPeriodoAtivo(inicioMinutos: Int, fimMinutos: Int) {
        viewModelScope.launch {
            settingsRepo.salvarPeriodoAtivo(inicioMinutos, fimMinutos)
        }
    }
    // --- FIM DA MUDANÇA ---
}

// --- A TELA EM SI (COMPOSABLE - VERSÃO FINAL) ---
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaConfiguracoes(
    onVoltarClick: () -> Unit,
    viewModel: TelaConfiguracoesViewModel = viewModel(
        factory = TelaConfiguracoesViewModelFactory(
            LocalContext.current.applicationContext as Application
        )
    )
) {
    val metaDiariaAtual = viewModel.metaDiariaState.collectAsState()
    val frequenciaAtual = viewModel.frequenciaState.collectAsState()
    val inicioAtivoAtual = viewModel.inicioAtivoState.collectAsState() // Em minutos (ex: 480)
    val fimAtivoAtual = viewModel.fimAtivoState.collectAsState()     // Em minutos (ex: 1320)

    // Converte minutos para horas (String) para os TextFields
    var metaTexto by remember(metaDiariaAtual.value) {
        mutableStateOf(metaDiariaAtual.value.toString())
    }
    var frequenciaTexto by remember(frequenciaAtual.value) {
        mutableStateOf(frequenciaAtual.value.toString())
    }
    var inicioTexto by remember(inicioAtivoAtual.value) {
        mutableStateOf((inicioAtivoAtual.value / 60).toString()) // ex: 480 / 60 = "8"
    }
    var fimTexto by remember(fimAtivoAtual.value) { // <-- Corrigido da última vez
        mutableStateOf((fimAtivoAtual.value / 60).toString()) // ex: 1320 / 60 = "22"
    }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current // <-- Pega o Contexto

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = { Text("Configurações") },
                navigationIcon = {
                    IconButton(onClick = onVoltarClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Voltar"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Campo para Meta Diária
            OutlinedTextField(
                value = metaTexto,
                onValueChange = { metaTexto = it },
                label = { Text("Meta Diária (em ml)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // Campo para Frequência
            OutlinedTextField(
                value = frequenciaTexto,
                onValueChange = { frequenciaTexto = it },
                label = { Text("Frequência do Lembrete (em minutos)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            Text(
                text = "O Android exige um mínimo de 15 minutos para lembretes periódicos.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // --- NOSSA NOVA MUDANÇA (Início) ---
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Período Ativo (Não incomodar)",
                style = MaterialTheme.typography.titleMedium
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = inicioTexto,
                    onValueChange = { inicioTexto = it },
                    label = { Text("Início (HH)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
                Text(text = "até")
                OutlinedTextField(
                    value = fimTexto,
                    onValueChange = { fimTexto = it },
                    label = { Text("Fim (HH)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.weight(1f)
                )
            }
            Text(
                text = "Insira as horas no formato 24h (ex: 8 para 8h, 22 para 22h).",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // --- FIM DA MUDANÇA ---


            Spacer(modifier = Modifier.height(16.dp))

            // Botão Salvar (COM O CÓDIGO DE PRODUÇÃO)
            Button(
                onClick = {
                    val novaMeta = metaTexto.toIntOrNull() ?: metaDiariaAtual.value
                    var novaFrequencia = frequenciaTexto.toIntOrNull() ?: frequenciaAtual.value

                    if (novaFrequencia < 15) {
                        novaFrequencia = 15 // Força o mínimo
                    }
                    viewModel.salvarMetaDiaria(novaMeta)
                    viewModel.salvarFrequencia(novaFrequencia)

                    val horaInicio = inicioTexto.toIntOrNull()?.coerceIn(0, 23) ?: (inicioAtivoAtual.value / 60)
                    val horaFim = fimTexto.toIntOrNull()?.coerceIn(0, 23) ?: (fimAtivoAtual.value / 60)
                    val inicioMinutos = horaInicio * 60
                    val fimMinutos = horaFim * 60

                    viewModel.salvarPeriodoAtivo(inicioMinutos, fimMinutos)

                    // --- ATUALIZA O WORKMANAGER (CÓDIGO DE PRODUÇÃO) ---
                    val periodicRequest = PeriodicWorkRequest.Builder(
                        ReminderWorker::class.java,
                        novaFrequencia.toLong(),
                        TimeUnit.MINUTES
                    )
                        .build()

                    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                        "lembrete-agua-periodico",
                        ExistingPeriodicWorkPolicy.UPDATE,
                        periodicRequest
                    )
                    // --- FIM DA ATUALIZAÇÃO ---

                    onVoltarClick()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Salvar")
            }
        }
    }
}

// --- FÁBRICA E VIEWMODELFACTORY (Sem mudanças) ---
class TelaConfiguracoesViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(TelaConfiguracoesViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return TelaConfiguracoesViewModel(
                settingsRepo = SettingsRepository(application)
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}