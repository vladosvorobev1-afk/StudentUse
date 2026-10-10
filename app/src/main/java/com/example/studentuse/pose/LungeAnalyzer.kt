package com.example.studentuse.pose

import com.example.studentuse.util.AngleMath
import kotlin.math.abs

/**
 * Анализатор выпадов.
 *
 * ВАЖНО: камера должна стоять СБОКУ, тело в профиль.
 *
 * Счёт по фазам с ГИСТЕРЕЗИСОМ:
 *   - Вход в DOWN:        угол < 150°
 *   - Возврат в STANDING: угол > 155°  (понижено с 165° — для плавного возврата)
 *   - BOTTOM:             угол < 100°
 *
 * Зазор 5° между DOWN (150°) и STANDING (155°).
 * Фаза меняется только после 2 кадров подряд — шум не переключит.
 *
 * Повтор: DOWN → BOTTOM → UP → STANDING.
 *   - Быстрый возврат (поднял ногу) → STANDING → +1.
 *   - Плавный возврат (волочение) → тоже STANDING (155°) → +1.
 *   - Ложный счёт невозможен: чтобы попасть в STANDING,
 *     нужно сначала пройти DOWN → BOTTOM (угол < 100°).
 */
class LungeAnalyzer {

    companion object {
        // --- Гистерезис фаз ---
        const val STANDING_ANGLE = 155f       // возврат в STANDING (было 165)
        const val DOWN_ANGLE = 150f           // вход в DOWN
        const val BOTTOM_ANGLE = 100f
        const val PARALLEL_ANGLE = 90f

        // --- Порог «настоящего» выпада ---
        const val REAL_LUNGE_THRESHOLD = 130f

        // --- Защита от скачков ---
        const val MAX_JUMP = 60f

        // --- Техника: контроль спины ---
        const val BACK_LEAN_MAX_DEG = 50f

        // --- Подтверждение кадрами ---
        const val CONFIRM_FRAMES = 2

        // --- Индексы BlazePose ---
        const val L_SHOULDER = 11
        const val R_SHOULDER = 12
        const val L_HIP = 23
        const val R_HIP = 24
        const val L_KNEE = 25
        const val R_KNEE = 26
        const val L_ANKLE = 27
        const val R_ANKLE = 28
    }

    private var repCount = 0
    private var phase: Phase = Phase.STANDING
    private var lastKneeAngle: Float? = null
    private var bottomReached = false

    private var pendingPhase: Phase = Phase.STANDING
    private var pendingCount = 0

    private var bottomCandidateCount = 0

    private var sawGood = false
    private var sawPartial = false
    private var sawBackWarning = false

    enum class Phase { STANDING, DOWN, BOTTOM, UP }
    enum class Depth { NONE, PARTIAL, PARALLEL, DEEP }
    enum class FrontLeg { LEFT, RIGHT, UNKNOWN }

    data class Feedback(
        val repCount: Int,
        val phase: Phase,
        val depth: Depth,
        val hint: String,
        val frontLeg: FrontLeg = FrontLeg.UNKNOWN,
        val backWarning: Boolean = false
    )

    fun reset() {
        repCount = 0
        phase = Phase.STANDING
        lastKneeAngle = null
        bottomReached = false
        pendingPhase = Phase.STANDING
        pendingCount = 0
        bottomCandidateCount = 0
        sawGood = false
        sawPartial = false
        sawBackWarning = false
    }

    fun getRepCount(): Int = repCount

    fun analyze(points: List<PosePoint>?): Feedback? {
        if (points == null || points.size < 33) return null

        val lShoulder = points.getOrNull(L_SHOULDER)
        val rShoulder = points.getOrNull(R_SHOULDER)
        val lHip = points.getOrNull(L_HIP)
        val rHip = points.getOrNull(R_HIP)
        val lKnee = points.getOrNull(L_KNEE)
        val rKnee = points.getOrNull(R_KNEE)
        val lAnkle = points.getOrNull(L_ANKLE)
        val rAnkle = points.getOrNull(R_ANKLE)

        if (lShoulder == null || rShoulder == null ||
            lHip == null || rHip == null ||
            lKnee == null || rKnee == null ||
            lAnkle == null || rAnkle == null
        ) return null

        // === Угол в колене для каждой ноги ===
        val leftKneeAngle = AngleMath.angle(lHip, lKnee, lAnkle)
        val rightKneeAngle = AngleMath.angle(rHip, rKnee, rAnkle)

        if (leftKneeAngle == null || rightKneeAngle == null) return null

        // === Определяем переднюю ногу ===
        val frontLeg: FrontLeg
        val frontKneeAngle: Float

        if (leftKneeAngle < rightKneeAngle) {
            frontLeg = FrontLeg.LEFT
            frontKneeAngle = leftKneeAngle
        } else {
            frontLeg = FrontLeg.RIGHT
            frontKneeAngle = rightKneeAngle
        }

        // === Наклон корпуса ===
        val shoulder = if (frontLeg == FrontLeg.LEFT) lShoulder else rShoulder
        val hip = if (frontLeg == FrontLeg.LEFT) lHip else rHip
        val lean = leanDegrees(shoulder, hip) ?: 0f

        // === Защита от скачков ===
        val last = lastKneeAngle
        if (last != null && abs(frontKneeAngle - last) > MAX_JUMP) {
            return Feedback(repCount, phase, Depth.NONE, "", frontLeg, false)
        }
        lastKneeAngle = frontKneeAngle

        // === Желаемая фаза с гистерезисом ===
        val desiredPhase: Phase = when (phase) {
            Phase.STANDING -> if (frontKneeAngle < DOWN_ANGLE) Phase.DOWN else Phase.STANDING
            Phase.DOWN -> when {
                frontKneeAngle <= BOTTOM_ANGLE -> Phase.BOTTOM
                frontKneeAngle > STANDING_ANGLE -> Phase.STANDING
                else -> Phase.DOWN
            }
            Phase.BOTTOM -> if (frontKneeAngle > BOTTOM_ANGLE + 10f) Phase.UP else Phase.BOTTOM
            Phase.UP -> when {
                frontKneeAngle > STANDING_ANGLE -> Phase.STANDING
                frontKneeAngle <= BOTTOM_ANGLE -> Phase.BOTTOM
                else -> Phase.UP
            }
        }

        // === Подтверждение фазы кадрами ===
        var hintToSay = ""

        if (desiredPhase != phase) {
            if (desiredPhase == pendingPhase) {
                pendingCount++
            } else {
                pendingPhase = desiredPhase
                pendingCount = 1
            }

            if (pendingCount >= CONFIRM_FRAMES) {
                val prevPhase = phase
                phase = desiredPhase
                pendingCount = 0

                // Переход UP → STANDING = конец повтора
                if (prevPhase == Phase.UP && phase == Phase.STANDING) {
                    if (bottomReached) {
                        repCount++

                        hintToSay = when {
                            sawBackWarning -> "Держи спину"
                            sawGood -> "Хорошо"
                            sawPartial -> "Ниже"
                            else -> ""
                        }

                        bottomReached = false
                        sawGood = false
                        sawPartial = false
                        sawBackWarning = false
                        bottomCandidateCount = 0
                    }
                }
            }
        } else {
            pendingCount = 0
        }

        // === Глубина ===
        if (phase != Phase.STANDING) {
            if (frontKneeAngle <= PARALLEL_ANGLE) sawGood = true
            else if (frontKneeAngle <= BOTTOM_ANGLE) sawPartial = true
        }

        // === Контроль спины ===
        if (phase == Phase.DOWN || phase == Phase.BOTTOM) {
            if (lean > BACK_LEAN_MAX_DEG) {
                sawBackWarning = true
            }
        }

        // === bottomReached — 2 кадра < 130° ===
        if (frontKneeAngle <= REAL_LUNGE_THRESHOLD) {
            bottomCandidateCount++
            if (bottomCandidateCount >= 2) {
                bottomReached = true
            }
        } else {
            bottomCandidateCount = 0
        }

        // === Глубина для HUD ===
        val depth = when {
            phase == Phase.STANDING -> Depth.NONE
            frontKneeAngle <= PARALLEL_ANGLE -> Depth.PARALLEL
            frontKneeAngle <= BOTTOM_ANGLE -> Depth.PARTIAL
            else -> Depth.PARTIAL
        }

        return Feedback(
            repCount = repCount,
            phase = phase,
            depth = depth,
            hint = hintToSay,
            frontLeg = frontLeg,
            backWarning = sawBackWarning
        )
    }

    private fun leanDegrees(shoulder: PosePoint?, hip: PosePoint?): Float? {
        if (shoulder == null || hip == null) return null
        val vx = shoulder.px - hip.px
        val vy = shoulder.py - hip.py
        val len = kotlin.math.sqrt(vx * vx + vy * vy)
        if (len < 1e-6f) return null

        val cosUp = (-vy / len).coerceIn(-1f, 1f)
        val angleRad = kotlin.math.acos(cosUp)
        return Math.toDegrees(angleRad.toDouble()).toFloat()
    }
}