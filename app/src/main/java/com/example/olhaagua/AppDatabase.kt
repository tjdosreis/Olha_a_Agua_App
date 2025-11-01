package com.example.olhaagua

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverter // Importe este
import androidx.room.TypeConverters
import java.util.Date

// @Database define o banco, listando as 'entities' (tabelas)
@Database(entities = [WaterLog::class], version = 1)
// @TypeConverters ensina o Room a salvar tipos complexos (como 'Date')
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    // Expõe o DAO para o resto do app
    abstract fun waterLogDao(): WaterLogDao

    companion object {
        // 'Volatile' garante que esta variável seja sempre a mais atual
        @Volatile
        private var INSTANCE: AppDatabase? = null

        // Esta função 'get' é o "portão de entrada" para o banco de dados
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "water_app_database" // O nome do arquivo do banco
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}

// O Room não sabe salvar 'Date'. Este conversor ensina-o a
// transformar 'Date' em 'Long' (um número) e vice-versa.
class Converters {
    @TypeConverter // <-- Note que é o @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter // <-- Note que é o @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }
}