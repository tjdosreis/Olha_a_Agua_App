package com.example.olhaagua

// Importe o R do NOSSO app
import com.example.olhaagua.R

// Imports do Android (Note que 'android.R' NÃO está aqui)
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

// Este é o nosso "Trabalhador" (VERSÃO ATUALIZADA COM PERÍODO ATIVO)
class ReminderWorker(
    private val context: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(context, workerParams) {

    companion object {
        const val CHANNEL_ID = "lembrete_agua_channel"
        const val NOTIFICATION_ID = 1
    }

    override suspend fun doWork(): Result {

        val settingsRepo = SettingsRepository(context)
        val waterLogDao = AppDatabase.getDatabase(context).waterLogDao()

        // Pega o calendário para definir início e fim do dia
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0); calendar.set(Calendar.MINUTE, 0); calendar.set(Calendar.SECOND, 0); calendar.set(Calendar.MILLISECOND, 0)
        val hojeInicio = calendar.time
        calendar.set(Calendar.HOUR_OF_DAY, 23); calendar.set(Calendar.MINUTE, 59); calendar.set(Calendar.SECOND, 59); calendar.set(Calendar.MILLISECOND, 999)
        val hojeFim = calendar.time

        // 1. Pega os limites salvos (em minutos)
        val inicioMinutos = settingsRepo.periodoAtivoInicioFlow.first()
        val fimMinutos = settingsRepo.periodoAtivoFimFlow.first()

        // 2. Pega a hora atual (em minutos)
        val calendarAgora = Calendar.getInstance()
        val agoraMinutos = calendarAgora.get(Calendar.HOUR_OF_DAY) * 60 + calendarAgora.get(Calendar.MINUTE)

        // 3. Verifica se estamos DENTRO do período ativo
        val isPeriodoAtivo = agoraMinutos in inicioMinutos..fimMinutos

        val metaDiaria = settingsRepo.metaDiariaFlow.first()
        val totalBebidoHoje = waterLogDao.getTotalAmountForPeriod(hojeInicio, hojeFim).first() ?: 0

        // Só envia notificação se a meta não foi batida E estamos no período ativo
        if (totalBebidoHoje < metaDiaria && isPeriodoAtivo) {
            enviarNotificacao(totalBebidoHoje, metaDiaria)
        }

        return Result.success()
    }

    private fun enviarNotificacao(total: Int, meta: Int) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        // Cria o Canal (sem mudanças)
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

        // --- 1. CRIA A AÇÃO DO BOTÃO (+250ml) ---
        val add250Intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_ADD_250
        }

        val add250PendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            0, // Request code 0
            add250Intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // --- 2. CRIA A AÇÃO DE CLIQUE (Abrir o App) ---
        // (Esta é a nossa correção)
        val openAppIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val openAppPendingIntent: PendingIntent = PendingIntent.getActivity(
            context,
            1, // Request code 1 (diferente do broadcast)
            openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        // --- FIM DA CORREÇÃO ---


        // --- 3. PREPARA OS ÍCONES ---
        val largeIcon = BitmapFactory.decodeResource(
            context.resources,
            R.mipmap.ic_launcher_round // Pega o ícone redondo do app
        )

        // --- 4. CONSTRÓI A NOTIFICAÇÃO (COM A CORREÇÃO) ---
        val notificacao = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_copo)
            .setLargeIcon(largeIcon)
            .setContentTitle("Hora de se hidratar!")
            .setContentText("Sua meta é $meta ml. Você já bebeu $total ml hoje.")

            // --- AQUI ESTÁ A CORREÇÃO ---
            // Define o que acontece ao clicar na notificação
            .setContentIntent(openAppPendingIntent)
            // --- FIM DA CORREÇÃO ---

            .setPriority(NotificationCompat.PRIORITY_HIGH)
            // .setAutoCancel(true) faz a notificação sumir quando clicada
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_notification_copo,
                "+250 ml",
                add250PendingIntent
            )
            .build()

        // Envia a notificação
        notificationManager.notify(NOTIFICATION_ID, notificacao)
    }
}