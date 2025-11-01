package com.example.olhaagua

// Imports básicos da Activity
import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts

// Imports do Compose para UI (Layout, Botões, Texto, etc.)
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions // <-- NOVO IMPORT
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType // <-- NOVO IMPORT
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Import do TEMA (com acento!)
import com.example.olhaagua.ui.theme.OlhaAÁguaTheme

// Imports para Coroutines (rodar em segundo plano)
import kotlinx.coroutines.launch

// Import do nosso Repositório (o "Gerente do Cofre")
import com.example.olhaagua.SettingsRepository

// Imports do WorkManager (o "Trabalhador")
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            // 1. Chamamos nosso Tema
            OlhaAÁguaTheme {

                // --- NOSSO ROTEADOR PRINCIPAL (CORRIGIDO) ---

                val context = LocalContext.current
                val repository = remember(context) { SettingsRepository(context) }

                val onboardingConcluido by repository.onboardingConcluidoFlow.collectAsState(initial = null)
                var onboardingAcabouDeTerminar by remember { mutableStateOf(false) }
                val statusFinal = if (onboardingAcabouDeTerminar) true else onboardingConcluido

                when (statusFinal) {
                    null -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    false -> {
                        // Caso 2: Onboarding NÃO foi feito (VERSÃO CORRIGIDA)

                        val permissionLauncher = rememberLauncherForActivityResult(
                            contract = ActivityResultContracts.RequestPermission()
                        ) { isGranted ->
                            // CORREÇÃO: NÃO FAÇA NADA AQUI.
                        }

                        LaunchedEffect(Unit) {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                            // CORREÇÃO: O 'ELSE' FOI REMOVIDO.
                        }

                        // Mostra a tela de Onboarding
                        OnboardingTela1(onOnboardingCompleto = {
                            onboardingAcabouDeTerminar = true
                        })
                    }
                    true -> {
                        // Caso 3: Onboarding JÁ foi feito
                        TelaPrincipal()
                    }
                }
                // --- FIM DO ROTEADOR ---
            }
        }
    }
}

// Esta é a nossa nova tela de Onboarding
@Composable
fun OnboardingTela1(
    modifier: Modifier = Modifier,
    onOnboardingCompleto: () -> Unit // O "interfone" para avisar que terminou
) {

    // --- NOSSAS VARIÁVEIS DE ESTADO (COM A CORREÇÃO) ---
    var modoSelecionado by remember { mutableStateOf("inicial") }
    var pesoTexto by remember { mutableStateOf("") }
    var metaManualTexto by remember { mutableStateOf("2000") } // <-- NOVO ESTADO

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { SettingsRepository(context) }
    // --- FIM DAS VARIÁVEIS ---

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // ... (Textos de Título e Subtítulo) ...
        Text(text = "Qual sua meta diária de água?", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Não se preocupe se não souber, podemos sugerir uma.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))

        // ... (Botões de Sugestão e Manual) ...
        Button(onClick = { modoSelecionado = "sugestao" }, modifier = Modifier.fillMaxWidth()) {
            Text("Sugerir para mim (Recomendado)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = { modoSelecionado = "manual" }, modifier = Modifier.fillMaxWidth()) {
            Text("Prefiro inserir manualmente")
        }

        Spacer(modifier = Modifier.height(32.dp))

        // 5. O Bloco 'WHEN' (COM A CORREÇÃO)
        when (modoSelecionado) {
            "sugestao" -> {
                OutlinedTextField(
                    value = pesoTexto,
                    onValueChange = { novoTexto -> pesoTexto = novoTexto },
                    label = { Text("Seu peso (em kg)") },
                    singleLine = true,
                    // Pede um teclado numérico
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            "manual" -> {
                // --- CÓDIGO CORRIGIDO ---
                OutlinedTextField(
                    value = metaManualTexto,
                    onValueChange = { novoTexto -> metaManualTexto = novoTexto },
                    label = { Text("Sua meta diária (em ml)") },
                    singleLine = true,
                    // Pede um teclado numérico
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth()
                )
                // --- FIM DA CORREÇÃO ---
            }
            // Adiciona um 'else' para o modo 'inicial' (não mostrar nada)
            else -> {}
        }

        // 6. O Botão "PRÓXIMO" (COM A CORREÇÃO)
        Spacer(modifier = Modifier.height(32.dp))

        Button(
            // Habilita o botão apenas se um modo foi selecionado
            enabled = modoSelecionado != "inicial",
            onClick = {
                // Agendador comum para ambos os casos
                val agendarTrabalho = {
                    val periodicRequest = PeriodicWorkRequest.Builder(
                        ReminderWorker::class.java, 90, TimeUnit.MINUTES
                    ).build()
                    WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                        "lembrete-agua-periodico", ExistingPeriodicWorkPolicy.KEEP, periodicRequest
                    )
                }

                if (modoSelecionado == "sugestao") {
                    val peso = pesoTexto.toIntOrNull() ?: 0
                    if (peso > 0) {
                        val metaCalculada = peso * 35
                        scope.launch {
                            repository.salvarMetaDiaria(metaCalculada)
                            repository.marcarOnboardingConcluido()
                            agendarTrabalho()
                            onOnboardingCompleto()
                        }
                    }
                }

                if (modoSelecionado == "manual") {
                    // --- CÓDIGO CORRIGIDO ---
                    val metaManual = metaManualTexto.toIntOrNull() ?: 0
                    if (metaManual > 0) {
                        scope.launch {
                            repository.salvarMetaDiaria(metaManual) // <-- Salva a meta manual
                            repository.marcarOnboardingConcluido()
                            agendarTrabalho() // <-- Agenda o trabalho aqui também
                            onOnboardingCompleto()
                        }
                    }
                    // --- FIM DA CORREÇÃO ---
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Próximo")
        }
    }
}

// Preview
@Preview(showBackground = true)
@Composable
fun OnboardingTela1Preview() {
    OlhaAÁguaTheme {
        OnboardingTela1(onOnboardingCompleto = {})
    }
}