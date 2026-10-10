package com.example.studentuse

import android.content.Context
import android.content.SharedPreferences

class AppSettings(context: Context) {

    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    // === Цель за подход (повторы — для приседаний и отжиманий) ===
    var targetReps: Int
        get() = prefs.getInt(KEY_TARGET_REPS, DEFAULT_TARGET_REPS)
        set(value) = prefs.edit().putInt(KEY_TARGET_REPS, value).apply()

    // === Цель за подход в СЕКУНДАХ (для планки) ===
    var targetSeconds: Int
        get() = prefs.getInt(KEY_TARGET_SECONDS, DEFAULT_TARGET_SECONDS)
        set(value) = prefs.edit().putInt(KEY_TARGET_SECONDS, value).apply()

    // === Общая фраза «Подход завершён» ===
    var saySetComplete: Boolean
        get() = prefs.getBoolean(KEY_SAY_SET_COMPLETE, DEFAULT_SAY_SET_COMPLETE)
        set(value) = prefs.edit().putBoolean(KEY_SAY_SET_COMPLETE, value).apply()

    // === Вибрация при завершении подхода ===
    var vibrateOnSetComplete: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE_ON_SET_COMPLETE, DEFAULT_VIBRATE_ON_SET_COMPLETE)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE_ON_SET_COMPLETE, value).apply()

    // === Отдельные подсказки ===
    var sayBack: Boolean
        get() = prefs.getBoolean(KEY_SAY_BACK, true)
        set(value) = prefs.edit().putBoolean(KEY_SAY_BACK, value).apply()

    var sayHeels: Boolean
        get() = prefs.getBoolean(KEY_SAY_HEELS, true)
        set(value) = prefs.edit().putBoolean(KEY_SAY_HEELS, value).apply()

    var sayDeeper: Boolean
        get() = prefs.getBoolean(KEY_SAY_DEEPER, true)
        set(value) = prefs.edit().putBoolean(KEY_SAY_DEEPER, value).apply()

    var sayGood: Boolean
        get() = prefs.getBoolean(KEY_SAY_GOOD, true)
        set(value) = prefs.edit().putBoolean(KEY_SAY_GOOD, value).apply()

    // === Для планки ===
    var sayBodyLine: Boolean
        get() = prefs.getBoolean(KEY_SAY_BODY_LINE, true)
        set(value) = prefs.edit().putBoolean(KEY_SAY_BODY_LINE, value).apply()

    var saySeconds: Boolean
        get() = prefs.getBoolean(KEY_SAY_SECONDS, false)
        set(value) = prefs.edit().putBoolean(KEY_SAY_SECONDS, value).apply()

    // === STREAK ===
    var lastWorkoutDay: Long
        get() = prefs.getLong(KEY_LAST_WORKOUT_DAY, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_WORKOUT_DAY, value).apply()

    var currentStreak: Int
        get() = prefs.getInt(KEY_CURRENT_STREAK, 0)
        set(value) = prefs.edit().putInt(KEY_CURRENT_STREAK, value).apply()

    // === Тема ===
    var themeMode: Int
        get() = prefs.getInt(KEY_THEME_MODE, THEME_SYSTEM)
        set(value) = prefs.edit().putInt(KEY_THEME_MODE, value).apply()

    // === Вибрация при счёте (для зала) ===
    /**
     * Если true — при каждом засчитанном повторе короткая вибрация.
     * Помогает в зале, когда шумно и не слышно голос.
     */
    var vibrateOnCount: Boolean
        get() = prefs.getBoolean(KEY_VIBRATE_ON_COUNT, false)
        set(value) = prefs.edit().putBoolean(KEY_VIBRATE_ON_COUNT, value).apply()

    companion object {
        private const val PREFS_NAME = "student_use_settings"
        private const val KEY_TARGET_REPS = "target_reps"
        private const val KEY_TARGET_SECONDS = "target_seconds"
        private const val KEY_SAY_SET_COMPLETE = "say_set_complete"
        private const val KEY_VIBRATE_ON_SET_COMPLETE = "vibrate_on_set_complete"
        private const val KEY_SAY_BACK = "say_back"
        private const val KEY_SAY_HEELS = "say_heels"
        private const val KEY_SAY_DEEPER = "say_deeper"
        private const val KEY_SAY_GOOD = "say_good"
        private const val KEY_SAY_BODY_LINE = "say_body_line"
        private const val KEY_SAY_SECONDS = "say_seconds"
        private const val KEY_LAST_WORKOUT_DAY = "last_workout_day"
        private const val KEY_CURRENT_STREAK = "current_streak"
        private const val KEY_THEME_MODE = "theme_mode"
        private const val KEY_VIBRATE_ON_COUNT = "vibrate_on_count"

        const val DEFAULT_TARGET_REPS = 10
        const val DEFAULT_TARGET_SECONDS = 60
        const val DEFAULT_SAY_SET_COMPLETE = true
        const val DEFAULT_VIBRATE_ON_SET_COMPLETE = true

        const val TARGET_REPS_MIN = 5
        const val TARGET_REPS_MAX = 500
        const val TARGET_REPS_STEP = 5
        const val TARGET_REPS_NONE = 0

        const val TARGET_SECONDS_MIN = 5
        const val TARGET_SECONDS_MAX = 300
        const val TARGET_SECONDS_STEP = 5
        const val TARGET_SECONDS_NONE = 0

        const val THEME_SYSTEM = 0
        const val THEME_DARK = 1
        const val THEME_LIGHT = 2
    }
}