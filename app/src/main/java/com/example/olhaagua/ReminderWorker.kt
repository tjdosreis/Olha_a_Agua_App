package com.example.olhaagua

import android.R // <-- IMPORTANTE: Precisamos do 'R' do Android
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.flow.first
import java.util.Calendar
import java.util.Date

// Este é o nosso "Trabalhador" (VERSÃO DE PRODUÇÃO com ÍCONE PLACEHOLDER "i")
class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "lembrete_agua_channel"
        const val NOTIFICATION_ID = 1
    }

    override suspend fun doWork(): Result {
        // ... (A lógica do doWork está 100% correta)
        val settingsRepo = SettingsRepository(context)
        val waterLogDao = AppDatabase.getDatabase(context).waterLogDao()
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val hojeInicio = calendar.time
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val hojeFim = calendar.time
        val metaDiaria = settingsRepo.metaDiariaFlow.first()
        val totalBebidoHoje = waterLogDao.getTotalAmountForPeriod(hojeInicio, hojeFim).first() ?: 0
        if (totalBebidoHoje < metaDiaria) {
            enviarNotificacao(totalBebidoHoje, metaDiaria)
        }
        return Result.success()
    }

    private fun enviarNotificacao(total: Int, meta: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // ... (Criação do Canal está correta)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Lembretes de Água", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificações para lembrar de beber água"
            }
            notificationManager.createNotificationChannel(channel)
        }

        // --- 1. CRIA A AÇÃO DO BOTÃO (Correto) ---
        val add250Intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_ADD_250
        }
        val add250PendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context, 0, add250Intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // --- 2. PREPARA O ÍCONE GRANDE (Correto) ---
        val largeIcon = BitmapFactory.decodeResource(
            context.resources,
            com.example.olhaagua.R.mipmap.ic_launcher_round // Usa o R do NOSSO app
        )

        // --- 3. CONSTRÓI A NOTIFICAÇÃO (Usando o ícone "i" que sabemos que funciona) ---
        val notificacao = NotificationCompat.Builder(context, CHANNEL_ID)
            // --- O CONSERTO (PLACEHOLDER) ---
            .setSmallIcon(android.R.drawable.ic_dialog_info) // <-- O ícone "i"

            .setLargeIcon(largeIcon) // O ícone colorido (está correto)
            .setContentTitle("Hora de se hidratar!")
            .setContentText("Sua meta é $meta ml. Você já bebeu $total ml hoje.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(
                // Usa o ícone "+" embutido do Android
                android.R.drawable.ic_input_add,
                "+250 ml",
                add250PendingIntent
            )
            .build()

        notificationManager.notify(NOTIFICATION_ID, notificacao)
    }
}