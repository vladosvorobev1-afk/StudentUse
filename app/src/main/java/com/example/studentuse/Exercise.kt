package com.example.studentuse

enum class Exercise(
    val title: String,
    val emoji: String,
    val available: Boolean
) {
    SQUAT("Приседания", "🦵", true),
    PUSHUP("Отжимания", "💪", true),
    PLANK("Планка", "🧘", true),
    LUNGE("Выпады", "🚶", true);
}