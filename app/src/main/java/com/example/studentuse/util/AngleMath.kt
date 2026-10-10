package com.example.studentuse.util

import com.example.studentuse.pose.PosePoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt

/**
 * Геометрические утилиты для анализа позы.
 *
 * Все углы считаются в 2D (только px, py), потому что координата z
 * в MediaPipe Pose Landmarker шумная и для анализа техники в проекции
 * камеры не нужна.
 */
object AngleMath {

    /**
     * Угол в точке [vertex] между лучами vertex->[a] и vertex->[b].
     * Возвращает значение в градусах, 0..180.
     *
     * Возвращает null, если какая-то из точек отсутствует.
     */
    fun angle(a: PosePoint?, vertex: PosePoint?, b: PosePoint?): Float? {
        if (a == null || vertex == null || b == null) return null

        val v1x = a.px - vertex.px
        val v1y = a.py - vertex.py
        val v2x = b.px - vertex.px
        val v2y = b.py - vertex.py

        val dot = v1x * v2x + v1y * v2y
        val len1 = sqrt(v1x * v1x + v1y * v1y)
        val len2 = sqrt(v2x * v2x + v2y * v2y)

        if (len1 < 1e-6f || len2 < 1e-6f) return null

        val cos = (dot / (len1 * len2)).coerceIn(-1f, 1f)
        return Math.toDegrees(atan2(sqrt(1f - cos * cos).toDouble(), cos.toDouble())).toFloat()
    }

    /**
     * Евклидово расстояние между двумя точками в 2D.
     */
    fun dist2D(a: PosePoint?, b: PosePoint?): Float {
        if (a == null || b == null) return 0f
        val dx = a.px - b.px
        val dy = a.py - b.py
        return sqrt(dx * dx + dy * dy)
    }

    /**
     * Разница двух углов по модулю.
     */
    fun angleDiff(a: Float?, b: Float?): Float? {
        if (a == null || b == null) return null
        return abs(a - b)
    }
}