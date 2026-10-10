package com.example.studentuse

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase

/**
 * Запись о завершённом подходе.
 *
 * Одна строка = один подход.
 *   - reps > 0, seconds = 0 — приседания / отжимания / выпады
 *   - reps = 0, seconds > 0 — планка
 */
@Entity(tableName = "workout_records")
data class WorkoutRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    /** Время завершения подхода (millis). */
    val timestamp: Long,

    /** Название упражнения: SQUAT / PUSHUP / PLANK / LUNGE. */
    val exercise: String,

    /** Количество повторов (для силовых). */
    val reps: Int,

    /** Количество секунд (для планки). */
    val seconds: Int
)

@Dao
interface WorkoutDao {

    @Insert
    suspend fun insert(record: WorkoutRecord)

    @Query("SELECT * FROM workout_records ORDER BY timestamp DESC")
    suspend fun getAll(): List<WorkoutRecord>

    @Query("SELECT * FROM workout_records WHERE timestamp >= :sinceMs ORDER BY timestamp DESC")
    suspend fun getSince(sinceMs: Long): List<WorkoutRecord>

    @Query("SELECT COUNT(*) FROM workout_records")
    suspend fun count(): Int

    @Query("DELETE FROM workout_records")
    suspend fun clearAll()

    @Query("DELETE FROM workout_records WHERE id = :id")
    suspend fun deleteById(id: Long)
}

@Database(
    entities = [WorkoutRecord::class],
    version = 1,
    exportSchema = false
)
abstract class WorkoutDatabase : RoomDatabase() {

    abstract fun workoutDao(): WorkoutDao

    companion object {
        @Volatile
        private var INSTANCE: WorkoutDatabase? = null

        fun get(context: Context): WorkoutDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    WorkoutDatabase::class.java,
                    "student_use_workouts.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}