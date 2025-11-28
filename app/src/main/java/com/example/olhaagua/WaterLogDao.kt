package com.example.olhaagua

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Delete // <-- NOVO IMPORT
import kotlinx.coroutines.flow.Flow
import java.util.Date

// @Dao diz ao Room que esta é uma interface de acesso
@Dao
interface WaterLogDao {

    // @Insert ensina o Room a inserir um novo registro
    @Insert
    suspend fun insert(log: WaterLog)

    // --- NOSSA NOVA MUDANÇA ---
    // @Delete ensina o Room a excluir um item.
    // Ele identifica o item pela sua PrimaryKey (o 'id')
    @Delete
    suspend fun delete(log: WaterLog)
    // --- FIM DA MUDANÇA ---

    // @Query permite escrever SQL para buscar dados
    // Esta função nos dará um Flow (lista) de todos os registros
    // entre duas datas (ex: o dia de hoje)
    @Query("SELECT * FROM water_log WHERE timestamp BETWEEN :startTime AND :endTime ORDER BY timestamp DESC")
    fun getLogsForPeriod(startTime: Date, endTime: Date): Flow<List<WaterLog>>

    // Busca TODOS os registros, do mais recente para o mais antigo
    @Query("SELECT * FROM water_log ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<WaterLog>>

    // Esta função soma a coluna 'amount' para um período
    // Retorna Int? (pode ser nulo se não houver registros)
    @Query("SELECT SUM(amount) FROM water_log WHERE timestamp BETWEEN :startTime AND :endTime")
    fun getTotalAmountForPeriod(startTime: Date, endTime: Date): Flow<Int?>
}