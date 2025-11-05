package com.example.olhaagua

import android.content.Context
import android.content.SharedPreferences // <-- NOVO IMPORT
import kotlinx.coroutines.flow.Flow
// --- MUDANÇAS NOS IMPORTS ---
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.channels.awaitClose
// --- FIM DAS MUDANÇAS ---

class SettingsRepository(private val context: Context) {

    companion object {
        private const val PREFERENCES_NAME = "olha_agua_settings"
        private const val KEY_META_DIARIA = "meta_diaria"
        private const val KEY_ONBOARDING_CONCLUIDO = "onboarding_concluido"
        private const val KEY_FREQUENCIA_LEMBRETE = "frequencia_lembrete"
        private const val KEY_PERIODO_ATIVO_INICIO = "periodo_ativo_inicio"
        private const val KEY_PERIODO_ATIVO_FIM = "periodo_ativo_fim"
        private const val KEY_APP_THEME = "app_theme"
    }

    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    // --- MUDANÇA GERAL: Todos os flows foram convertidos para callbackFlow ---

    val metaDiariaFlow: Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_META_DIARIA) {
                trySend(prefs.getInt(KEY_META_DIARIA, 0))
            }
        }
        trySend(preferences.getInt(KEY_META_DIARIA, 0)) // Emite o valor inicial
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val onboardingConcluidoFlow: Flow<Boolean> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_ONBOARDING_CONCLUIDO) {
                trySend(prefs.getBoolean(KEY_ONBOARDING_CONCLUIDO, false))
            }
        }
        trySend(preferences.getBoolean(KEY_ONBOARDING_CONCLUIDO, false))
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }


    val frequenciaLembreteFlow: Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_FREQUENCIA_LEMBRETE) {
                trySend(prefs.getInt(KEY_FREQUENCIA_LEMBRETE, 90))
            }
        }
        trySend(preferences.getInt(KEY_FREQUENCIA_LEMBRETE, 90))
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val periodoAtivoInicioFlow: Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_PERIODO_ATIVO_INICIO) {
                trySend(prefs.getInt(KEY_PERIODO_ATIVO_INICIO, 8 * 60))
            }
        }
        trySend(preferences.getInt(KEY_PERIODO_ATIVO_INICIO, 8 * 60))
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    val periodoAtivoFimFlow: Flow<Int> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_PERIODO_ATIVO_FIM) {
                trySend(prefs.getInt(KEY_PERIODO_ATIVO_FIM, 22 * 60))
            }
        }
        trySend(preferences.getInt(KEY_PERIODO_ATIVO_FIM, 22 * 60))
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }

    /**
     * Lê a preferência de tema do app.
     * Padrão: "Sistema"
     */
    val appThemeFlow: Flow<String> = callbackFlow {
        val listener = SharedPreferences.OnSharedPreferenceChangeListener { prefs, key ->
            if (key == KEY_APP_THEME) {
                trySend(prefs.getString(KEY_APP_THEME, "Sistema") ?: "Sistema")
            }
        }
        trySend(preferences.getString(KEY_APP_THEME, "Sistema") ?: "Sistema")
        preferences.registerOnSharedPreferenceChangeListener(listener)
        awaitClose { preferences.unregisterOnSharedPreferenceChangeListener(listener) }
    }


    // --- Funções de Escrita (SALVAR) ---
    // (Estas funções permanecem exatamente iguais)

    suspend fun salvarMetaDiaria(meta: Int) {
        preferences.edit().putInt(KEY_META_DIARIA, meta).apply()
    }

    suspend fun marcarOnboardingConcluido() {
        preferences.edit().putBoolean(KEY_ONBOARDING_CONCLUIDO, true).apply()
    }

    suspend fun salvarFrequencia(minutos: Int) {
        preferences.edit().putInt(KEY_FREQUENCIA_LEMBRETE, minutos).apply()
    }

    suspend fun salvarPeriodoAtivo(inicioMinutos: Int, fimMinutos: Int) {
        preferences.edit()
            .putInt(KEY_PERIODO_ATIVO_INICIO, inicioMinutos)
            .putInt(KEY_PERIODO_ATIVO_FIM, fimMinutos)
            .apply()
    }

    suspend fun salvarTema(tema: String) {
        preferences.edit().putString(KEY_APP_THEME, tema).apply()
    }
}