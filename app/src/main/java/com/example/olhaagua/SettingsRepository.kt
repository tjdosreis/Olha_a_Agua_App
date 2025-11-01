package com.example.olhaagua

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

// ESTA É A NOVA VERSÃO USANDO SHARERDPREFERENCES

class SettingsRepository(private val context: Context) {

    // companion object é onde definimos coisas "estáticas"
    companion object {
        // Nome do nosso arquivo de preferências
        private const val PREFERENCES_NAME = "olha_agua_settings"

        // Nossas Chaves para o "Cofre"
        // (Note: são apenas strings, sem 'intKey')
        private const val KEY_META_DIARIA = "meta_diaria"
        private const val KEY_ONBOARDING_CONCLUIDO = "onboarding_concluido"
    }

    // 1. Pega (ou cria) o "cofre" do SharedPreferences
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)


    // --- Funções de Leitura (para LER do cofre) ---

    /**
     * Expõe um Fluxo (Flow) que emite a meta diária.
     * Nós usamos 'flow { ... }' para imitar o comportamento do DataStore
     * e manter nossa arquitetura.
     */
    val metaDiariaFlow: Flow<Int> = flow {
        // Lê o valor da chave, se não encontrar, retorna 0
        emit(preferences.getInt(KEY_META_DIARIA, 0))
    }

    /**
     * Expõe um Fluxo (Flow) que emite 'true' se o onboarding foi concluído,
     * ou 'false' caso contrário.
     */
    val onboardingConcluidoFlow: Flow<Boolean> = flow {
        // Lê o valor da chave, se não encontrar, retorna false
        emit(preferences.getBoolean(KEY_ONBOARDING_CONCLUIDO, false))
    }


    // --- Funções de Escrita (para SALVAR no cofre) ---

    /**
     * Salva um novo valor para a meta diária.
     * 'suspend' está aqui para manter a compatibilidade com a nossa MainActivity.
     * .apply() salva os dados em segundo plano.
     */
    suspend fun salvarMetaDiaria(meta: Int) {
        preferences.edit().putInt(KEY_META_DIARIA, meta).apply()
    }

    /**
     * Marca o onboarding como concluído (salvando 'true').
     */
    suspend fun marcarOnboardingConcluido() {
        preferences.edit().putBoolean(KEY_ONBOARDING_CONCLUIDO, true).apply()
    }
}