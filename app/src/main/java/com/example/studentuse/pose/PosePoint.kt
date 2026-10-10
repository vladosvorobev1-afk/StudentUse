package com.example.studentuse.pose

/**
 * Точка скелета в нормализованных координатах кадра.
 *
 * px, py — координаты 0..1 (левый верх — 0,0; правый низ — 1,1).
 * conf    — уверенность. Сейчас всегда 1.0f (см. MainActivity).
 *
 * ВАЖНО: поля названы px, py, conf, а НЕ x, y, visibility,
 * чтобы не конфликтовать с именами методов MediaPipe.
 */
data class PosePoint(
    val px: Float,
    val py: Float,
    val conf: Float
)