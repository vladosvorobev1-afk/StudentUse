package com.example.studentuse.pose

import com.example.studentuse.util.AngleMath
import kotlin.math.abs
import kotlin.math.max

/**
 * Анализатор отжиманий.
 *
 * ВАЖНО: камера должна стоять ВПОЛОБОРОТА (~60°) или СПЕРЕДИ.
 * Строго сбоку — не работает (одна рука перекрывает другую).
 *
 * Ключевое отличие от первой версии:
 *   Угол локтя берём МАКСИМАЛЬНЫЙ из двух, а не средний.
 *   Потому что один локоть ближе к камере и виден лучше.
 *   Средний угол искажается шумом дальнего локтя (22° вместо 76°).
 *
 * Счёт по фазам с гистерезисом:
 *   - Вход в DOWN:   угол < 150°
 *   - Возврат в TOP: угол > 165°
 *   - BOTTOM:        угол < 100°
 *
 * Защиты от предметов:
 *   1. Симметрия плеч: |L_SH.y − R_SH.y| ≤ 0.12
 *   2. Баланс ПРАВОЙ руки: |dist(SH,ELB) − dist(ELB,WRI)| ≤ 0.08
 *
 * Данные полевого теста (вполоборота):
 *   Ты TOP:      угол ~170°, |SH.y|=0.009, R_bal=0.009
 *   Ты BOTTOM:   угол ~72°,  |SH.y|=0.073, R_bal=0.053
 *   Одежда:      угол ~161°, |SH.y|=0.171, R_bal=0.101
 */
class PushupAnalyzer {

    companion object {
        // --- Гистерезис фаз ---
        const val TOP_ANGLE = 165f
        const val DOWN_ANGLE = 150f
        const val BOTTOM_ANGLE = 100f
        const val PARALLEL_ANGLE = 90f

        // --- Порог «настоящего» отжимания ---
        const val REAL_PUSHUP_THRESHOLD = 130f

        // --- Защита от скачков ---
        const val MAX_JUMP = 60f

        // --- Линия тела ---
        const val BODY_LINE_MAX_DEVIATION = 0.18f

        // === Защита 1: симметрия плеч ===
        // Ты: 0.009, 0.073, 0.076. Одежда: 0.171.
        const val SHOULDER_Y_SYMMETRY_MAX = 0.12f

        // === Защита 2: баланс ПРАВОЙ руки ===
        // Ты: 0.009, 0.040, 0.053. Одежда: 0.101.
        const val ARM_BALANCE_MAX = 0.08f

        // --- Подтверждение кадрами ---
        const val CONFIRM_FRAMES = 2

        // --- Индексы BlazePose ---
        const val L_SHOULDER = 11
        const val R_SHOULDER = 12
        const val L_ELBOW = 13
        const val R_ELBOW = 14
        const val L_WRIST = 15
        const val R_WRIST = 16
        const val L_HIP = 23
        const val R_HIP = 24
        const val L_KNEE = 25
        const val R_KNEE = 26
        const val L_ANKLE = 27
        const val R_ANKLE = 28
    }

    private var repCount = 0
    private var phase: Phase = Phase.TOP
    private var lastElbowAngle: Float? = null
    private var bottomReached = false

    private var pendingPhase: Phase = Phase.TOP
    private var pendingCount = 0

    private var bottomCandidateCount = 0

    private var sawGood = false
    private var sawPartial = false
    private var sawBodyLineWarning = false

    enum class Phase { TOP, DOWN, BOTTOM, UP }
    enum class Depth { NONE, PARTIAL, GOOD }

    data class Feedback(
        val repCount: Int,
        val phase: Phase,
        val depth: Depth,
        val hint: String,
        val bodyLineWarning: Boolean = false
    )

    fun reset() {
        repCount = 0
        phase = Phase.TOP
        lastElbowAngle = null
        bottomReached = false
        pendingPhase = Phase.TOP
        pendingCount = 0
        bottomCandidateCount = 0
        sawGood = false
        sawPartial = false
        sawBodyLineWarning = false
    }

    fun getRepCount(): Int = repCount

    fun analyze(points: List<PosePoint>?): Feedback? {
        if (points == null || points.size < 33) return null

        val lShoulder = points.getOrNull(L_SHOULDER)
        val rShoulder = points.getOrNull(R_SHOULDER)
        val lElbow = points.getOrNull(L_ELBOW)
        val rElbow = points.getOrNull(R_ELBOW)
        val lWrist = points.getOrNull(L_WRIST)
        val rWrist = points.getOrNull(R_WRIST)
        val lHip = points.getOrNull(L_HIP)
        val rHip = points.getOrNull(R_HIP)
        val lKnee = points.getOrNull(L_KNEE)
        val rKnee = points.getOrNull(R_KNEE)
        val lAnkle = points.getOrNull(L_ANKLE)
        val rAnkle = points.getOrNull(R_ANKLE)

        if (lShoulder == null || rShoulder == null ||
            lElbow == null || rElbow == null ||
            lWrist == null || rWrist == null ||
            lHip == null || rHip == null ||
            lKnee == null || rKnee == null ||
            lAnkle == null || rAnkle == null
        ) return null

        // === Защита 1: симметрия плеч ===
        if (abs(lShoulder.py - rShoulder.py) > SHOULDER_Y_SYMMETRY_MAX) return null

        // === Защита 2: баланс ПРАВОЙ руки ===
        // (левый локоть может быть ближе к камере и шумит)
        val rightUpperArm = AngleMath.dist2D(rShoulder, rElbow)
        val rightForearm = AngleMath.dist2D(rElbow, rWrist)
        val rightBalance = abs(rightUpperArm - rightForearm)
        if (rightBalance > ARM_BALANCE_MAX) return null

        // === Угол в локте — МАКСИМУМ из двух ===
        val leftElbowAngle = AngleMath.angle(lShoulder, lElbow, lWrist)
        val rightElbowAngle = AngleMath.angle(rShoulder, rElbow, rWrist)
        val elbowAngleList = listOfNotNull(leftElbowAngle, rightElbowAngle)
        if (elbowAngleList.isEmpty()) return null
        val elbowAngle = elbowAngleList.max()

        // === Защита от скачков ===
        val last = lastElbowAngle
        if (last != null && abs(elbowAngle - last) > MAX_JUMP) {
            return Feedback(repCount, phase, Depth.NONE, "", false)
        }
        lastElbowAngle = elbowAngle

        // === Желаемая фаза с гистерезисом ===
        val desiredPhase: Phase = when (phase) {
            Phase.TOP -> if (elbowAngle < DOWN_ANGLE) Phase.DOWN else Phase.TOP
            Phase.DOWN -> when {
                elbowAngle <= BOTTOM_ANGLE -> Phase.BOTTOM
                elbowAngle > TOP_ANGLE -> Phase.TOP
                else -> Phase.DOWN
            }
            Phase.BOTTOM -> if (elbowAngle > BOTTOM_ANGLE + 10f) Phase.UP else Phase.BOTTOM
            Phase.UP -> when {
                elbowAngle > TOP_ANGLE -> Phase.TOP
                elbowAngle <= BOTTOM_ANGLE -> Phase.BOTTOM
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

                // Переход UP → TOP = конец повтора
                if (prevPhase == Phase.UP && phase == Phase.TOP) {
                    if (bottomReached) {
                        repCount++

                        hintToSay = when {
                            sawBodyLineWarning -> "Держи тело ровно"
                            sawGood -> "Хорошо"
                            sawPartial -> "Ниже"
                            else -> ""
                        }

                        bottomReached = false
                        sawGood = false
                        sawPartial = false
                        sawBodyLineWarning = false
                        bottomCandidateCount = 0
                    }
                }
            }
        } else {
            pendingCount = 0
        }

        // === Глубина ===
        if (phase == Phase.DOWN || phase == Phase.BOTTOM) {
            if (elbowAngle <= PARALLEL_ANGLE) sawGood = true
            else if (elbowAngle <= BOTTOM_ANGLE) sawPartial = true
        }

        // === Линия тела ===
        if (phase == Phase.DOWN || phase == Phase.BOTTOM) {
            val leftDeviation = bodyLineDeviation(lShoulder, lHip, lKnee, lAnkle)
            val rightDeviation = bodyLineDeviation(rShoulder, rHip, rKnee, rAnkle)
            val maxDeviation = maxOf(leftDeviation ?: 0f, rightDeviation ?: 0f)
            if (maxDeviation > BODY_LINE_MAX_DEVIATION) {
                sawBodyLineWarning = true
            }
        }

        // === bottomReached — 2 кадра < 130° ===
        if (elbowAngle <= REAL_PUSHUP_THRESHOLD) {
            bottomCandidateCount++
            if (bottomCandidateCount >= 2) {
                bottomReached = true
            }
        } else {
            bottomCandidateCount = 0
        }

        // === Глубина для HUD ===
        val depth = when {
            phase == Phase.TOP -> Depth.NONE
            elbowAngle <= PARALLEL_ANGLE -> Depth.GOOD
            else -> Depth.PARTIAL
        }

        return Feedback(
            repCount = repCount,
            phase = phase,
            depth = depth,
            hint = hintToSay,
            bodyLineWarning = sawBodyLineWarning
        )
    }

    private fun bodyLineDeviation(
        shoulder: PosePoint?,
        hip: PosePoint?,
        knee: PosePoint?,
        ankle: PosePoint?
    ): Float? {
        if (shoulder == null || hip == null || knee == null || ankle == null) return null

        val dx = ankle.px - shoulder.px
        val dy = ankle.py - shoulder.py
        val lineLen = kotlin.math.sqrt(dx * dx + dy * dy)
        if (lineLen < 1e-6f) return null

        val hipDev = perpendicularDistance(shoulder, ankle, hip, lineLen)
        val kneeDev = perpendicularDistance(shoulder, ankle, knee, lineLen)

        return maxOf(hipDev, kneeDev)
    }

    private fun perpendicularDistance(
        a: PosePoint,
        b: PosePoint,
        p: PosePoint,
        lineLen: Float
    ): Float {
        val cross = abs(
            (b.px - a.px) * (p.py - a.py) -
                    (b.py - a.py) * (p.px - a.px)
        )
        return cross / lineLen
    }
}