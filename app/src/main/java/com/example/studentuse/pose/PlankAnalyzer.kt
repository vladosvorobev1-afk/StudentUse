package com.example.studentuse.pose

import com.example.studentuse.util.AngleMath
import kotlin.math.abs

/**
 * Анализатор планки.
 *
 * ВАЖНО: камера должна стоять СБОКУ, тело в профиль.
 *
 * Логика:
 *   1. Проверяем линию тела (плечо → бедро → колено → стопа).
 *   2. Если линия ровная — таймер идёт.
 *   3. Если линия сломалась — таймер на паузу + подсказка «Держи тело ровно».
 *   4. Если ушёл из кадра > INVISIBLE_RESET_SEC — сброс таймера.
 *
 * Состояния:
 *   IDLE      — таймер не запущен (или сброшен).
 *   HOLDING   — планка держится, время идёт.
 *   BROKEN    — линия сломалась, таймер на паузе.
 *   INVISIBLE — человека нет в кадре.
 *
 * Защита от предметов — 2 проверки, ОСЛАБЛЕННЫЕ для планки:
 *   1. Симметрия плеч:  |L_SH.y − R_SH.y| ≤ 0.12
 *   2. Баланс руки:     |dist(SH,ELB) − dist(ELB,WRI)| ≤ 0.12
 *
 * В планке ты статичен, и MediaPipe шумит — поэтому пороги мягче, чем в отжиманиях.
 * Защита 3 (длина тела) УБРАНА — она резала тебя в планке.
 */
class PlankAnalyzer {

    companion object {
        // --- Линия тела (лояльный порог) ---
        const val BODY_LINE_MAX_DEVIATION = 0.18f

        // --- Защита 1: симметрия плеч (ослаблена для планки) ---
        const val SHOULDER_Y_SYMMETRY_MAX = 0.12f

        // --- Защита 2: баланс руки (ослаблена для планки) ---
        const val ARM_BALANCE_MAX = 0.12f

        // --- Тайминг ---
        const val INVISIBLE_RESET_SEC = 2f

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

    private var secondsHeld: Float = 0f
    private var state: State = State.IDLE

    private var lastTimestampMs: Long = 0L
    private var invisibleMs: Long = 0L

    private var sawBodyLineWarning = false

    enum class State { IDLE, HOLDING, BROKEN, INVISIBLE }

    data class Feedback(
        val secondsHeld: Int,
        val state: State,
        val hint: String,
        val bodyLineWarning: Boolean = false
    )

    fun reset() {
        secondsHeld = 0f
        state = State.IDLE
        lastTimestampMs = 0L
        invisibleMs = 0L
        sawBodyLineWarning = false
    }

    fun getSecondsHeld(): Int = secondsHeld.toInt()

    fun analyze(points: List<PosePoint>?, timestampMs: Long): Feedback? {
        val deltaMs: Long = if (lastTimestampMs == 0L) {
            0L
        } else {
            (timestampMs - lastTimestampMs).coerceIn(0L, 200L)
        }
        lastTimestampMs = timestampMs
        val deltaSec = deltaMs / 1000f

        if (points == null || points.size < 33) {
            invisibleMs += deltaMs
            if (invisibleMs >= INVISIBLE_RESET_SEC * 1000f) {
                reset()
                return Feedback(0, State.INVISIBLE, "", false)
            }
            state = State.INVISIBLE
            return Feedback(secondsHeld.toInt(), State.INVISIBLE, "", false)
        }

        invisibleMs = 0L

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
        ) {
            state = State.INVISIBLE
            return Feedback(secondsHeld.toInt(), State.INVISIBLE, "", false)
        }

        // === Защита 1: симметрия плеч (ослаблена для планки) ===
        if (abs(lShoulder.py - rShoulder.py) > SHOULDER_Y_SYMMETRY_MAX) {
            state = State.INVISIBLE
            return Feedback(secondsHeld.toInt(), State.INVISIBLE, "", false)
        }

        // === Защита 2: баланс руки (ослаблена для планки) ===
        val leftUpperArm = AngleMath.dist2D(lShoulder, lElbow)
        val leftForearm = AngleMath.dist2D(lElbow, lWrist)
        val rightUpperArm = AngleMath.dist2D(rShoulder, rElbow)
        val rightForearm = AngleMath.dist2D(rElbow, rWrist)
        val leftBalance = abs(leftUpperArm - leftForearm)
        val rightBalance = abs(rightUpperArm - rightForearm)
        if (leftBalance > ARM_BALANCE_MAX && rightBalance > ARM_BALANCE_MAX) {
            state = State.INVISIBLE
            return Feedback(secondsHeld.toInt(), State.INVISIBLE, "", false)
        }

        // === Проверка линии тела ===
        val leftDeviation = bodyLineDeviation(lShoulder, lHip, lKnee, lAnkle)
        val rightDeviation = bodyLineDeviation(rShoulder, rHip, rKnee, rAnkle)
        val maxDeviation = maxOf(leftDeviation ?: 0f, rightDeviation ?: 0f)

        val isBodyStraight = maxDeviation <= BODY_LINE_MAX_DEVIATION

        var hintToSay = ""

        if (isBodyStraight) {
            state = State.HOLDING
            secondsHeld += deltaSec
        } else {
            sawBodyLineWarning = true
            state = State.BROKEN
            hintToSay = "Держи тело ровно"
        }

        return Feedback(
            secondsHeld = secondsHeld.toInt(),
            state = state,
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