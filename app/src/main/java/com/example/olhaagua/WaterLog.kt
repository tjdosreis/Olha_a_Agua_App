package com.example.olhaagua

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

// @Entity diz ao Room para criar uma tabela chamada "water_log"
@Entity(tableName = "water_log")
data class WaterLog(
    // @PrimaryKey define a "chave" (ID) única de cada registro
    // autoGenerate = true faz o Room criar o ID para nós (1, 2, 3...)
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    // A quantidade de água em mililitros
    val amount: Int,

    // A data/hora que o registro foi feito
    val timestamp: Date
)