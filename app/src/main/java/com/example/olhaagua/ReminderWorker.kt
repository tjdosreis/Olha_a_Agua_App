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

// Este é o nosso "Trabalhador" (VERSÃO FINAL COM O ÍCONE CORRETO)
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

        // ... (lógica para pegar hojeInicio e hojeFim) ...
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

        // --- 1. CRIA A AÇÃO DO BOTÃO ---
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

        // Pega nosso ícone colorido (mipmap) e transforma em Bitmap
        val largeIcon = BitmapFactory.decodeResource(
            context.resources,
            R.mipmap.ic_launcher_round // Pega o ícone redondo do app
        )

        // --- 3. CONSTRÓI A NOTIFICAÇÃO (COM O ÍCONE CORRETO) ---

        val notificacao = NotificationCompat.Builder(context, CHANNEL_ID)
            // --- O CONSERTO (Ícone Pequeno) ---
            // Usa a silhueta do copo que criamos manualmente
            .setSmallIcon(R.drawable.ic_notification_copo)

            // --- O POLIMENTO (Ícone Grande) ---
            .setLargeIcon(largeIcon) // <-- Usa o ícone colorido do app

            .setContentTitle("Hora de se hidratar!")
            .setContentText("Sua meta é $meta ml. Você já bebeu $total ml hoje.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(
                // Reusa a silhueta do copo no botão
                R.drawable.ic_notification_copo,
                "+250 ml",
                add250PendingIntent
            )
            .build()

        // Envia a notificação
        notificationManager.notify(NOTIFICATION_ID, notificacao)
    }
}