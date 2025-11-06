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
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material.icons.filled.Check
// --- NOVOS IMPORTS PARA O DROPDOWN ---
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.TextField
// --- FIM DOS NOVOS IMPORTS ---
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
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit
import com.example.olhaagua.ReminderWorker

// --- O CÉREBRO DA TELA (VIEWMODEL) ---
// (Sem mudanças aqui, ele já está pronto)
class TelaConfiguracoesViewModel(
    private val settingsRepo: SettingsRepository
) : ViewModel() {

    val metaDiariaState: StateFlow<Int> = settingsRepo.metaDiariaFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val frequenciaState: StateFlow<Int> = settingsRepo.frequenciaLembreteFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 90)

    val inicioAtivoState: StateFlow<Int> = settingsRepo.periodoAtivoInicioFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 8 * 60) // 8h

    val fimAtivoState: StateFlow<Int> = settingsRepo.periodoAtivoFimFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 22 * 60) // 22h

    val appThemeState: StateFlow<String> = settingsRepo.appThemeFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), "Sistema")

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

    fun salvarPeriodoAtivo(inicioMinutos: Int, fimMinutos: Int) {
        viewModelScope.launch {
            settingsRepo.salvarPeriodoAtivo(inicioMinutos, fimMinutos)
        }
    }

    fun salvarTema(tema: String) {
        viewModelScope.launch {
            settingsRepo.salvarTema(tema)
        }
    }
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
    val inicioAtivoAtual = viewModel.inicioAtivoState.collectAsState()
    val fimAtivoAtual = viewModel.fimAtivoState.collectAsState()
    val temaAtual = viewModel.appThemeState.collectAsState()

    var temaSelecionado by remember(temaAtual.value) { mutableStateOf(temaAtual.value) }
    val opcoesTema = listOf("Sistema", "Claro", "Escuro")

    var metaTexto by remember(metaDiariaAtual.value) {
        mutableStateOf(metaDiariaAtual.value.toString())
    }
    var frequenciaTexto by remember(frequenciaAtual.value) {
        mutableStateOf(frequenciaAtual.value.toString())
    }

    // --- NOSSAS NOVAS MUDANÇAS (Início) ---
    // Os estados de texto (inicioTexto/fimTexto) são mantidos,
    // pois eles guardarão o valor selecionado (ex: "8")
    var inicioTexto by remember(inicioAtivoAtual.value) {
        mutableStateOf((inicioAtivoAtual.value / 60).toString())
    }
    var fimTexto by remember(fimAtivoAtual.value) {
        mutableStateOf((fimAtivoAtual.value / 60).toString())
    }

    // Estados para controlar se os menus estão abertos
    var isInicioExpanded by remember { mutableStateOf(false) }
    var isFimExpanded by remember { mutableStateOf(false) }

    // A lista de opções (0-23)
    val horas = (0..23).map { it.toString() }
    // --- FIM DAS MUDANÇAS ---

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

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

            // --- Bloco 1: Meta Diária ---
            OutlinedTextField(
                value = metaTexto,
                onValueChange = { metaTexto = it },
                label = { Text("Meta Diária (em ml)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth()
            )

            // --- Bloco 2: Frequência ---
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

            // --- Bloco 3: Período Ativo (COM DROPDOWNS) ---
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
                // --- DROPDOWN 1 (INÍCIO) ---
                ExposedDropdownMenuBox(
                    expanded = isInicioExpanded,
                    onExpandedChange = { isInicioExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = "$inicioTexto:00", // Mostra "8:00"
                        onValueChange = {}, // Vazio, pois é readOnly
                        readOnly = true,
                        label = { Text("Início (HH)") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isInicioExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier
                            .menuAnchor() // Conecta o campo ao menu
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isInicioExpanded,
                        onDismissRequest = { isInicioExpanded = false }
                    ) {
                        horas.forEach { hora ->
                            DropdownMenuItem(
                                text = { Text("$hora:00") },
                                onClick = {
                                    inicioTexto = hora // Atualiza o estado
                                    isInicioExpanded = false // Fecha o menu
                                }
                            )
                        }
                    }
                }

                Text(text = "até")

                // --- DROPDOWN 2 (FIM) ---
                ExposedDropdownMenuBox(
                    expanded = isFimExpanded,
                    onExpandedChange = { isFimExpanded = it },
                    modifier = Modifier.weight(1f)
                ) {
                    TextField(
                        value = "$fimTexto:00", // Mostra "22:00"
                        onValueChange = {}, // Vazio, pois é readOnly
                        readOnly = true,
                        label = { Text("Fim (HH)") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = isFimExpanded)
                        },
                        colors = ExposedDropdownMenuDefaults.textFieldColors(),
                        modifier = Modifier
                            .menuAnchor()
                            .fillMaxWidth()
                    )
                    ExposedDropdownMenu(
                        expanded = isFimExpanded,
                        onDismissRequest = { isFimExpanded = false }
                    ) {
                        horas.forEach { hora ->
                            DropdownMenuItem(
                                text = { Text("$hora:00") },
                                onClick = {
                                    fimTexto = hora // Atualiza o estado
                                    isFimExpanded = false // Fecha o menu
                                }
                            )
                        }
                    }
                }
            }
            Text(
                text = "Selecione a hora de início e fim do período ativo.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            // --- FIM DA MUDANÇA ---

            // --- Bloco 4: Aparência ---
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Aparência",
                style = MaterialTheme.typography.titleMedium
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                opcoesTema.forEach { tema ->
                    val isSelected = tema == temaSelecionado
                    FilterChip(
                        selected = isSelected,
                        onClick = { temaSelecionado = tema },
                        label = { Text(tema) },
                        enabled = true,
                        leadingIcon = if (isSelected) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selecionado",
                                    modifier = Modifier.padding(start = 4.dp)
                                )
                            }
                        } else {
                            null
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botão Salvar
            Button(
                onClick = {
                    viewModel.salvarTema(temaSelecionado)

                    val novaMeta = metaTexto.toIntOrNull() ?: metaDiariaAtual.value
                    var novaFrequencia = frequenciaTexto.toIntOrNull() ?: frequenciaAtual.value

                    if (novaFrequencia < 15) {
                        novaFrequencia = 15
                    }
                    viewModel.salvarMetaDiaria(novaMeta)
                    viewModel.salvarFrequencia(novaFrequencia)

                    // A LÓGICA DE SALVAR NÃO MUDA
                    // Ela já lê 'inicioTexto' e 'fimTexto'
                    // e os converte para Int
                    val horaInicio = inicioTexto.toIntOrNull()?.coerceIn(0, 23) ?: (inicioAtivoAtual.value / 60)
                    val horaFim = fimTexto.toIntOrNull()?.coerceIn(0, 23) ?: (fimAtivoAtual.value / 60)
                    val inicioMinutos = horaInicio * 60
                    val fimMinutos = horaFim * 60

                    viewModel.salvarPeriodoAtivo(inicioMinutos, fimMinutos)

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