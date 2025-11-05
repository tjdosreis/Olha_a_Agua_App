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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Import do TEMA (com acento!)
import com.example.olhaagua.ui.theme.OlhaAÁguaTheme

// Imports para Coroutines (rodar em segundo plano)
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

// Import do nosso Repositório (o "Gerente do Cofre")
import com.example.olhaagua.SettingsRepository

// Imports do WorkManager (o "Trabalhador")
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequest
import androidx.work.WorkManager
import java.util.concurrent.TimeUnit

// --- NOSSOS NOVOS IMPORTS DE NAVEGAÇÃO ---
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Define as "rotas" (endereços) do nosso app (COM A NOVA ROTA)
object Routes {
    const val ONBOARDING = "onboarding"
    const val PRIMER = "permission_primer" // <-- A NOVA TELA
    const val PRINCIPAL = "principal"
    const val CONFIGURACOES = "configuracoes"
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val context = LocalContext.current
            val repository = remember(context) { SettingsRepository(context) }

            // Lê o tema salvo (ex: "Sistema", "Claro", "Escuro")
            val temaEscolhido by repository.appThemeFlow.collectAsState(initial = null)

            // --- A CORREÇÃO ESTÁ AQUI ---

            // 1. Coletamos o state
            val onboardingConcluido by repository.onboardingConcluidoFlow.collectAsState(initial = null)

            // 2. SÓ renderizamos quando os dois valores (tema E onboarding)
            //    forem carregados pela primeira vez.
            if (temaEscolhido != null && onboardingConcluido != null) {

                // 3. Nós "lembramos" o startDestination INICIAL.
                //    Como 'remember' não tem chaves (keys), ele só roda UMA VEZ.
                //    Esta variável 'startDest' NUNCA mais vai mudar,
                //    mesmo se 'onboardingConcluido' mudar para 'true'
                //    no meio do fluxo de onboarding.
                val startDest = remember {
                    if (onboardingConcluido == true) Routes.PRINCIPAL else Routes.ONBOARDING
                }

                val navController = rememberNavController()

                OlhaAÁguaTheme(temaEscolhido = temaEscolhido!!) {
                    // 4. Usamos a variável "lembrada" (travada)
                    NavHost(
                        navController = navController,
                        startDestination = startDest
                    ) {

                        // Rota 1: Onboarding
                        composable(Routes.ONBOARDING) {
                            OnboardingTela1(
                                navController = navController
                            )
                        }

                        // Rota 2: Tela de "Aquecimento"
                        composable(Routes.PRIMER) {
                            PermissionPrimerScreen(
                                onNavigateToPrincipal = {
                                    navController.navigate(Routes.PRINCIPAL) {
                                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                                    }
                                }
                            )
                        }

                        // Rota 3: Tela Principal
                        composable(Routes.PRINCIPAL) {
                            TelaPrincipal(
                                onConfigClick = {
                                    navController.navigate(Routes.CONFIGURACOES)
                                }
                            )
                        }

                        // Rota 4: Tela de Configurações
                        composable(Routes.CONFIGURACOES) {
                            TelaConfiguracoes(
                                onVoltarClick = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            } else {
                // Tela de "Carregando" inicial (enquanto o tema e o onboarding são lidos)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            }
            // --- FIM DA CORREÇÃO ---
        }
    }
}

// --- TELA DE ONBOARDING (Sem mudanças) ---
@Composable
fun OnboardingTela1(
    modifier: Modifier = Modifier,
    navController: NavHostController
) {
    var modoSelecionado by remember { mutableStateOf("inicial") }
    var pesoTexto by remember { mutableStateOf("") }
    var metaManualTexto by remember { mutableStateOf("2000") }

    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { SettingsRepository(context) }

    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Qual sua meta diária de água?", style = MaterialTheme.typography.headlineMedium, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "Não se preocupe se não souber, podemos sugerir uma.", style = MaterialTheme.typography.bodyLarge, textAlign = TextAlign.Center)
        Spacer(modifier = Modifier.height(32.dp))
        Button(onClick = { modoSelecionado = "sugestao" }, modifier = Modifier.fillMaxWidth()) {
            Text("Sugerir para mim (Recomendado)")
        }
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedButton(onClick = { modoSelecionado = "manual" }, modifier = Modifier.fillMaxWidth()) {
            Text("Prefiro inserir manualmente")
        }
        Spacer(modifier = Modifier.height(32.dp))
        when (modoSelecionado) {
            "sugestao" -> {
                OutlinedTextField(value = pesoTexto, onValueChange = { novoTexto -> pesoTexto = novoTexto }, label = { Text("Seu peso (em kg)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
            "manual" -> {
                OutlinedTextField(value = metaManualTexto, onValueChange = { novoTexto -> metaManualTexto = novoTexto }, label = { Text("Sua meta diária (em ml)") }, singleLine = true, keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), modifier = Modifier.fillMaxWidth())
            }
            else -> {}
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            enabled = modoSelecionado != "inicial",
            onClick = {
                // A lógica de salvar (em background) continua a mesma
                scope.launch {
                    val frequenciaSalva = repository.frequenciaLembreteFlow.first()
                    val agendarTrabalho = {
                        val periodicRequest = PeriodicWorkRequest.Builder(
                            ReminderWorker::class.java, frequenciaSalva.toLong(), TimeUnit.MINUTES
                        ).build()
                        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                            "lembrete-agua-periodico", ExistingPeriodicWorkPolicy.KEEP, periodicRequest
                        )
                    }
                    if (modoSelecionado == "sugestao") {
                        val peso = pesoTexto.toIntOrNull() ?: 0
                        if (peso > 0) {
                            val metaCalculada = peso * 35
                            repository.salvarMetaDiaria(metaCalculada)
                            repository.marcarOnboardingConcluido() // <-- Isso ainda acontece
                            agendarTrabalho()
                        }
                    }
                    if (modoSelecionado == "manual") {
                        val metaManual = metaManualTexto.toIntOrNull() ?: 0
                        if (metaManual > 0) {
                            repository.salvarMetaDiaria(metaManual)
                            repository.marcarOnboardingConcluido() // <-- Isso ainda acontece
                            agendarTrabalho()
                        }
                    }
                }

                // A lógica de navegação (imediata) continua a mesma
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    navController.navigate(Routes.PRIMER)
                } else {
                    navController.navigate(Routes.PRINCIPAL) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Próximo")
        }
    }
}

// Preview (Atualizado para funcionar)
@Preview(showBackground = true)
@Composable
fun OnboardingTela1Preview() {
    OlhaAÁguaTheme {
        val navController = rememberNavController()
        OnboardingTela1(navController = navController)
    }
}