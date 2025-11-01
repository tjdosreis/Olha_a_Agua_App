package com.example.olhaagua

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow
import java.util.Date

// @Dao diz ao Room que esta é uma interface de acesso
@Dao
interface WaterLogDao {

    // @Insert ensina o Room a inserir um novo registro
    @Insert
    suspend fun insert(log: WaterLog)

    // @Query permite escrever SQL para buscar dados
    // Esta função nos dará um Flow (lista) de todos os registros
    // entre duas datas (ex: o dia de hoje)
    @Query("SELECT * FROM water_log WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getLogsForPeriod(startTime: Date, endTime: Date): Flow<List<WaterLog>>

    // Esta função soma a coluna 'amount' para um período
    // Retorna Int? (pode ser nulo se não houver registros)
    @Query("SELECT SUM(amount) FROM water_log WHERE timestamp BETWEEN :startTime AND :endTime")
    fun getTotalAmountForPeriod(startTime: Date, endTime: Date): Flow<Int?>
}