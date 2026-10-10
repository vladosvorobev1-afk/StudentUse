package com.example.studentuse

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Calendar

class WorkoutRepository(context: Context) {

    private val appContext = context.applicationContext
    private val dao = WorkoutDatabase.get(appContext).workoutDao()
    private val settings = AppSettings(appContext)

    // === Сохранение ===

    suspend fun saveSquat(reps: Int) = save(Exercise.SQUAT, reps, 0)
    suspend fun savePushup(reps: Int) = save(Exercise.PUSHUP, reps, 0)
    suspend fun saveLunge(reps: Int) = save(Exercise.LUNGE, reps, 0)
    suspend fun savePlank(seconds: Int) = save(Exercise.PLANK, 0, seconds)

    private suspend fun save(exercise: Exercise, reps: Int, seconds: Int) {
        withContext(Dispatchers.IO) {
            dao.insert(
                WorkoutRecord(
                    timestamp = System.currentTimeMillis(),
                    exercise = exercise.name,
                    reps = reps,
                    seconds = seconds
                )
            )

            updateStreak()

            if (reps > 0) {
                val key = "pb_${exercise.name}_reps"
                val current = getRecordValue(key)
                if (reps > current) {
                    setRecordValue(key, reps)
                }
            }
            if (seconds > 0) {
                val key = "pb_${exercise.name}_seconds"
                val current = getRecordValue(key)
                if (seconds > current) {
                    setRecordValue(key, seconds)
                }
            }
        }
    }

    // === Удаление ===

    suspend fun deleteById(id: Long) = withContext(Dispatchers.IO) {
        dao.deleteById(id)
    }

    // === Streak ===

    private fun updateStreak() {
        val today = startOfDay(System.currentTimeMillis())
        val lastDay = settings.lastWorkoutDay

        if (lastDay == today) return

        val newStreak = if (lastDay == 0L) {
            1
        } else {
            val yesterday = today - 24L * 60 * 60 * 1000
            if (lastDay == yesterday) {
                settings.currentStreak + 1
            } else {
                1
            }
        }

        settings.currentStreak = newStreak
        settings.lastWorkoutDay = today
    }

    fun getStreak(): Int {
        val lastDay = settings.lastWorkoutDay
        if (lastDay == 0L) return 0

        val today = startOfDay(System.currentTimeMillis())
        val yesterday = today - 24L * 60 * 60 * 1000

        return if (lastDay == today || lastDay == yesterday) {
            settings.currentStreak
        } else {
            0
        }
    }

    private fun startOfDay(timestampMs: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = timestampMs
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    // === Рекорды ===

    fun getRecord(exercise: Exercise, isSeconds: Boolean): Int {
        val key = if (isSeconds) {
            "pb_${exercise.name}_seconds"
        } else {
            "pb_${exercise.name}_reps"
        }
        return getRecordValue(key)
    }

    private fun getRecordValue(key: String): Int {
        return appContext.getSharedPreferences(
            "student_use_records",
            Context.MODE_PRIVATE
        ).getInt(key, 0)
    }

    private fun setRecordValue(key: String, value: Int) {
        appContext.getSharedPreferences(
            "student_use_records",
            Context.MODE_PRIVATE
        ).edit().putInt(key, value).apply()
    }

    // === Чтение ===

    suspend fun getAll(): List<WorkoutRecord> = withContext(Dispatchers.IO) {
        dao.getAll()
    }

    suspend fun getLast7Days(): List<WorkoutRecord> = withContext(Dispatchers.IO) {
        val weekAgo = System.currentTimeMillis() - 7L * 24 * 60 * 60 * 1000
        dao.getSince(weekAgo)
    }

    suspend fun clearAll() = withContext(Dispatchers.IO) {
        dao.clearAll()
    }

    suspend fun count(): Int = withContext(Dispatchers.IO) {
        dao.count()
    }
}