package com.example.olhaagua

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.util.Calendar // <-- NOVO IMPORT
import java.util.Date // <-- NOVO IMPORT

// Este é o nosso "Trabalhador"
class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "lembrete_agua_channel"
        const val NOTIFICATION_ID = 1
    }

    // Este é o trabalho que roda em segundo plano
    override suspend fun doWork(): Result {

        // 1. Instancia nossos "gerentes" (ambos!)
        val settingsRepo = SettingsRepository(context)
        // Precisamos do DAO para ler o total bebido
        val waterLogDao = AppDatabase.getDatabase(context).waterLogDao()

        // 2. Pega o início e o fim do dia de "hoje"
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val hojeInicio = calendar.time
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val hojeFim = calendar.time

        // 3. Lê os dados dos "cofres"
        // .first() pega o valor mais recente dos nossos Flows
        val metaDiaria = settingsRepo.metaDiariaFlow.first()
        val totalBebidoHoje = waterLogDao.getTotalAmountForPeriod(hojeInicio, hojeFim).first() ?: 0 // <-- CORRIGIDO!

        // 4. Verifica se deve notificar
        if (totalBebidoHoje < metaDiaria) {
            // Se ainda NÃO bateu a meta, envia a notificação
            enviarNotificacao(totalBebidoHoje, metaDiaria)
        }

        // 5. Avisa ao sistema que o trabalho foi um sucesso
        // (Se a meta foi batida, ele simplesmente termina sem notificar)
        return Result.success()
    }

    // Função para construir e mostrar a notificação
    // (Sem mudanças aqui)
    private fun enviarNotificacao(total: Int, meta: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Lembretes de Água",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações para lembrar de beber água"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notificacao = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground) // TODO: Trocar por um ícone de gota
            .setContentTitle("Hora de se hidratar!")
            .setContentText("Sua meta é $meta ml. Você já bebeu $total ml hoje.") // <-- Texto atualizado
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notificacao)
    }
}