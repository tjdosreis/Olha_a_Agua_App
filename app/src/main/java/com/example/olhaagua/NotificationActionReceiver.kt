package com.example.olhaagua

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Date

class NotificationActionReceiver : BroadcastReceiver() {

    // companion object para definir os "nomes" das nossas ações
    companion object {
        const val ACTION_ADD_250 = "com.example.olhaagua.ACTION_ADD_250"
        // TODO: Poderíamos adicionar "ACTION_SNOOZE" (Adiar) aqui no futuro
    }

    override fun onReceive(context: Context?, intent: Intent?) {
        // Garante que o contexto e a intenção não sejam nulos
        if (context == null || intent == null) return

        // Pega o "gerente" do nosso banco de dados Room
        val dao = AppDatabase.getDatabase(context).waterLogDao()

        // Pega o gerenciador de notificações do sistema
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // goAsync() nos dá "permissão" para rodar em segundo plano
        // por alguns segundos, o que é necessário para salvar no banco.
        val pendingResult = goAsync()

        // Lança uma Coroutine em uma thread de "IO" (Input/Output)
        // para fazer o trabalho de banco de dados (que não pode ser na thread principal)
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Verifica qual ação (botão) foi clicada
                when (intent.action) {
                    ACTION_ADD_250 -> {
                        // 1. Cria o registro de água
                        val log = WaterLog(
                            amount = 250,
                            timestamp = Date() // Salva com a data/hora de agora
                        )
                        // 2. Salva no banco de dados
                        dao.insert(log)

                        // 3. Fecha/Cancela a notificação que foi clicada
                        //    Usamos o ID que definimos no ReminderWorker
                        notificationManager.cancel(ReminderWorker.NOTIFICATION_ID)
                    }

                    // TODO: handle ACTION_SNOOZE
                }
            } finally {
                // 4. Avisa ao sistema que nosso trabalho terminou
                pendingResult.finish()
            }
        }
    }
}