package com.example.olhaagua

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// ESTA É A NOVA VERSÃO ATUALIZADA

class SettingsRepository(private val context: Context) {

    // companion object é onde definimos coisas "estáticas"
    companion object {
        // Nome do nosso arquivo de preferências
        private const val PREFERENCES_NAME = "olha_agua_settings"

        // Nossas Chaves para o "Cofre"
        private const val KEY_META_DIARIA = "meta_diaria"
        private const val KEY_ONBOARDING_CONCLUIDO = "onboarding_concluido"

        // --- NOSSAS NOVAS CHAVES ---
        private const val KEY_FREQUENCIA_LEMBRETE = "frequencia_lembrete"
        private const val KEY_PERIODO_ATIVO_INICIO = "periodo_ativo_inicio"
        private const val KEY_PERIODO_ATIVO_FIM = "periodo_ativo_fim"
    }

    // 1. Pega (ou cria) o "cofre" do SharedPreferences
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)


    // --- Funções de Leitura (para LER do cofre) ---

    val metaDiariaFlow: Flow<Int> = flow {
        emit(preferences.getInt(KEY_META_DIARIA, 0))
    }

    val onboardingConcluidoFlow: Flow<Boolean> = flow {
        emit(preferences.getBoolean(KEY_ONBOARDING_CONCLUIDO, false))
    }

    // --- NOSSAS NOVAS FUNÇÕES DE LEITURA ---

    /**
     * Lê a frequência do lembrete (em minutos).
     * O valor padrão é 90 minutos (o que "chumbamos" no código).
     */
    val frequenciaLembreteFlow: Flow<Int> = flow {
        emit(preferences.getInt(KEY_FREQUENCIA_LEMBRETE, 90))
    }

    /**
     * Lê a hora de INÍCIO do período ativo (em minutos desde a meia-noite).
     * Padrão: 8h00 (8 * 60 = 480 minutos).
     */
    val periodoAtivoInicioFlow: Flow<Int> = flow {
        emit(preferences.getInt(KEY_PERIODO_ATIVO_INICIO, 8 * 60))
    }

    /**
     * Lê a hora de FIM do período ativo (em minutos desde a meia-noite).
     * Padrão: 22h00 (22 * 60 = 1320 minutos).
     */
    val periodoAtivoFimFlow: Flow<Int> = flow {
        emit(preferences.getInt(KEY_PERIODO_ATIVO_FIM, 22 * 60))
    }


    // --- Funções de Escrita (para SALVAR no cofre) ---

    suspend fun salvarMetaDiaria(meta: Int) {
        preferences.edit().putInt(KEY_META_DIARIA, meta).apply()
    }

    suspend fun marcarOnboardingConcluido() {
        preferences.edit().putBoolean(KEY_ONBOARDING_CONCLUIDO, true).apply()
    }

    // --- NOSSAS NOVAS FUNÇÕES DE ESCRITA ---

    /**
     * Salva a frequência do lembrete (em minutos).
     */
    suspend fun salvarFrequencia(minutos: Int) {
        preferences.edit().putInt(KEY_FREQUENCIA_LEMBRETE, minutos).apply()
    }

    /**
     * Salva o período ativo (início e fim, em minutos desde a meia-noite).
     */
    suspend fun salvarPeriodoAtivo(inicioMinutos: Int, fimMinutos: Int) {
        preferences.edit()
            .putInt(KEY_PERIODO_ATIVO_INICIO, inicioMinutos)
            .putInt(KEY_PERIODO_ATIVO_FIM, fimMinutos)
            .apply()
    }
}