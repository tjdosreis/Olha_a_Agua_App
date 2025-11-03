package com.example.olhaagua

// Imports do Android
import android.Manifest
import android.os.Build

// Imports do Compose
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

// Import do nosso Tema (com acento!)
import com.example.olhaagua.ui.theme.OlhaAÁguaTheme

@Composable
fun PermissionPrimerScreen(
    // "Interfone" para avisar o NavHost que terminamos
    // e podemos ir para a Tela Principal
    onNavigateToPrincipal: () -> Unit
) {
    // 1. Criamos o "Lançador" da permissão
    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // 3. Quando o usuário responde (Permitir ou Negar),
        //    nós o navegamos para a Tela Principal de qualquer jeito.
        onNavigateToPrincipal()
    }

    // A UI (Interface) da tela
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp), // Mais padding para centralizar
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Ícone (um sino de notificação)
        Icon(
            imageVector = Icons.Default.Notifications,
            contentDescription = "Ícone de Notificação",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Título
        Text(
            text = "Habilite os lembretes",
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Texto explicativo (O "Primer")
        Text(
            text = "Para que o \"Olha a Água!\" possa lhe enviar lembretes na hora certa, precisamos da sua permissão para enviar notificações.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Botão de Ação
        Button(
            onClick = {
                // 2. Pede a permissão (o 'launcher' vai navegar)
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                } else {
                    // Se for Android 12- (não deveria chegar aqui, mas por segurança),
                    // apenas navega direto.
                    onNavigateToPrincipal()
                }
            }
        ) {
            Text("Habilitar Notificações")
        }
    }
}

// Preview para vermos o design
@Preview(showBackground = true)
@Composable
fun PermissionPrimerPreview() {
    OlhaAÁguaTheme {
        PermissionPrimerScreen(onNavigateToPrincipal = {})
    }
}