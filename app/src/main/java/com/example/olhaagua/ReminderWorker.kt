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

        // --- NOSSA NOVA MUDANÇA (Início) ---

        // 1. Pega os limites salvos (em minutos)
        val inicioMinutos = settingsRepo.periodoAtivoInicioFlow.first()
        val fimMinutos = settingsRepo.periodoAtivoFimFlow.first()

        // 2. Pega a hora atual (em minutos)
        // Precisamos pegar uma nova instância para ter a hora/minuto corretos de AGORA
        val calendarAgora = Calendar.getInstance()
        val agoraMinutos = calendarAgora.get(Calendar.HOUR_OF_DAY) * 60 + calendarAgora.get(Calendar.MINUTE)

        // 3. Verifica se estamos DENTRO do período ativo
        // (ex: 480 (8h) e 1320 (22h). Se 'agoraMinutos' for 500 (8h20), 500 in 480..1320 = true)
        val isPeriodoAtivo = agoraMinutos in inicioMinutos..fimMinutos

        // --- FIM DA MUDANÇA ---

        val metaDiaria = settingsRepo.metaDiariaFlow.first()
        val totalBebidoHoje = waterLogDao.getTotalAmountForPeriod(hojeInicio, hojeFim).first() ?: 0

        // --- CONDIÇÃO ATUALIZADA ---
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

        // --- 1. CRIA AÇÃO DO BOTÃO ---
        val add250Intent = Intent(context, NotificationActionReceiver::class.java).apply {
            action = NotificationActionReceiver.ACTION_ADD_250
        }

        val add250PendingIntent: PendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            add250Intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        // --- 2. PREPARA OS ÍCONES ---
        val largeIcon = BitmapFactory.decodeResource(
            context.resources,
            R.mipmap.ic_launcher_round // Pega o ícone redondo do app
        )

        // --- 3. CONSTRÓI A NOTIFICAÇÃO ---
        val notificacao = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification_copo)
            .setLargeIcon(largeIcon) // <-- Usa o ícone colorido do app
            .setContentTitle("Hora de se hidratar!")
            .setContentText("Sua meta é $meta ml. Você já bebeu $total ml hoje.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
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