package com.example.studentuse.pose

import com.example.studentuse.util.AngleMath
import kotlin.math.abs

/**
 * Анализатор приседаний.
 *
 * Проверки техники (в порядке приоритета подсказок):
 *   1. Спина   — наклон корпуса от вертикали > 50°  → «Держи спину»
 *   2. Пятки   — стопа стала круче эталона на 20°+  → «Пятки на полу»
 *   3. Глубина — дошёл до параллели / глубже       → «Хорошо»
 *               не дошёл до 100°                   → «Глубже»
 */
class SquatAnalyzer {

    companion object {
        const val STANDING_ANGLE = 160f
        const val BOTTOM_ANGLE = 100f
        const val PARALLEL_ANGLE = 90f
        const val DEEP_ANGLE = 70f
        const val REAL_SQUAT_THRESHOLD = 120f
        const val MAX_JUMP = 60f
        const val BACK_LEAN_MAX_DEG = 50f
        const val HEEL_LIFT_DELTA_DEG = 20f
        const val FOOT_VISIBLE_MIN_DIST = 0.05f

        const val NOSE = 0
        const val L_SHOULDER = 11
        const val R_SHOULDER = 12
        const val L_HIP = 23
        const val R_HIP = 24
        const val L_KNEE = 25
        const val R_KNEE = 26
        const val L_ANKLE = 27
        const val R_ANKLE = 28
        const val L_FOOT_INDEX = 31
        const val R_FOOT_INDEX = 32
    }

    private var repCount = 0
    private var phase: Phase = Phase.STANDING
    private var lastKneeAngle: Float? = null
    private var bottomReached = false

    private var sawDeep = false
    private var sawParallel = false
    private var sawPartial = false
    private var sawBackWarning = false
    private var sawHeelLift = false

    private var baselineFootAngle: Float? = null
    private var baselineSamples = 0

    enum class Phase { STANDING, DOWN, BOTTOM, UP }
    enum class Depth { NONE, PARTIAL, PARALLEL, DEEP }

    data class Feedback(
        val repCount: Int,
        val phase: Phase,
        val depth: Depth,
        val hint: String,
        val backWarning: Boolean = false,
        val heelWarning: Boolean = false
    )

    fun reset() {
        repCount = 0
        phase = Phase.STANDING
        lastKneeAngle = null
        bottomReached = false
        sawDeep = false
        sawParallel = false
        sawPartial = false
        sawBackWarning = false
        sawHeelLift = false
        baselineFootAngle = null
        baselineSamples = 0
    }

    fun getRepCount(): Int = repCount

    fun analyze(points: List<PosePoint>?): Feedback? {
        if (points == null || points.size < 33) return null

        val nose = points.getOrNull(NOSE)
        val lShoulder = points.getOrNull(L_SHOULDER)
        val rShoulder = points.getOrNull(R_SHOULDER)
        val lHip = points.getOrNull(L_HIP)
        val rHip = points.getOrNull(R_HIP)
        val lKnee = points.getOrNull(L_KNEE)
        val rKnee = points.getOrNull(R_KNEE)
        val lAnkle = points.getOrNull(L_ANKLE)
        val rAnkle = points.getOrNull(R_ANKLE)
        val lFoot = points.getOrNull(L_FOOT_INDEX)
        val rFoot = points.getOrNull(R_FOOT_INDEX)

        if (nose == null || lHip == null || rHip == null ||
            lKnee == null || rKnee == null || lAnkle == null || rAnkle == null
        ) return null

        val verticalChain =
            nose.py < lHip.py - 0.05f &&
                    nose.py < rHip.py - 0.05f &&
                    lKnee.py < lAnkle.py - 0.03f &&
                    rKnee.py < rAnkle.py - 0.03f
        if (!verticalChain) return null

        val leftLegLength = AngleMath.dist2D(lHip, lKnee) + AngleMath.dist2D(lKnee, lAnkle)
        val rightLegLength = AngleMath.dist2D(rHip, rKnee) + AngleMath.dist2D(rKnee, rAnkle)
        if (leftLegLength < 0.15f || rightLegLength < 0.15f) return null

        val kneeDistance = abs(lKnee.px - rKnee.px)
        if (kneeDistance < 0.03f) return null

        val leftKneeAngle = AngleMath.angle(lHip, lKnee, lAnkle)
        val rightKneeAngle = AngleMath.angle(rHip, rKnee, rAnkle)
        val kneeAngle = listOfNotNull(leftKneeAngle, rightKneeAngle)
            .average()
            .toFloat()

        val last = lastKneeAngle
        if (last != null && abs(kneeAngle - last) > MAX_JUMP) {
            return Feedback(repCount, phase, Depth.NONE, "", false, false)
        }
        lastKneeAngle = kneeAngle

        val prevPhase = phase
        phase = when {
            kneeAngle >= STANDING_ANGLE -> Phase.STANDING
            kneeAngle <= BOTTOM_ANGLE -> Phase.BOTTOM
            prevPhase == Phase.STANDING && kneeAngle < STANDING_ANGLE -> Phase.DOWN
            prevPhase == Phase.BOTTOM && kneeAngle > BOTTOM_ANGLE -> Phase.UP
            prevPhase == Phase.DOWN -> Phase.DOWN
            prevPhase == Phase.UP -> Phase.UP
            else -> Phase.DOWN
        }

        if (phase != Phase.STANDING) {
            if (kneeAngle <= DEEP_ANGLE) sawDeep = true
            else if (kneeAngle <= PARALLEL_ANGLE) sawParallel = true
            else if (kneeAngle <= BOTTOM_ANGLE) sawPartial = true
        }

        if (phase == Phase.DOWN || phase == Phase.BOTTOM) {
            val leftLean = leanDegrees(lShoulder, lHip)
            val rightLean = leanDegrees(rShoulder, rHip)
            val lean = listOfNotNull(leftLean, rightLean).average().toFloat()
            if (lean > BACK_LEAN_MAX_DEG) {
                sawBackWarning = true
            }
        }

        val currentFootAngle = footAngle(lAnkle, lFoot, rAnkle, rFoot)

        if (phase == Phase.STANDING) {
            if (currentFootAngle != null) {
                val base = baselineFootAngle
                if (base == null) {
                    baselineFootAngle = currentFootAngle
                    baselineSamples = 1
                } else {
                    baselineFootAngle = (base * baselineSamples + currentFootAngle) /
                            (baselineSamples + 1)
                    baselineSamples++
                }
            }
        } else if (phase == Phase.DOWN || phase == Phase.BOTTOM) {
            val base = baselineFootAngle
            if (base != null && currentFootAngle != null) {
                if (currentFootAngle - base > HEEL_LIFT_DELTA_DEG) {
                    sawHeelLift = true
                }
            }
        }

        if (kneeAngle <= REAL_SQUAT_THRESHOLD && !bottomReached) {
            bottomReached = true
        }

        var hintToSay = ""

        if (bottomReached && prevPhase == Phase.UP && phase == Phase.STANDING) {
            repCount++
            bottomReached = false

            hintToSay = when {
                sawBackWarning -> "Держи спину"
                sawHeelLift -> "Пятки на полу"
                sawDeep || sawParallel -> "Хорошо"
                sawPartial -> "Глубже"
                else -> ""
            }

            sawDeep = false
            sawParallel = false
            sawPartial = false
            sawBackWarning = false
            sawHeelLift = false
        }

        val depth = when {
            phase == Phase.STANDING -> Depth.NONE
            kneeAngle <= DEEP_ANGLE -> Depth.DEEP
            kneeAngle <= PARALLEL_ANGLE -> Depth.PARALLEL
            kneeAngle <= BOTTOM_ANGLE -> Depth.PARTIAL
            else -> Depth.PARTIAL
        }

        return Feedback(
            repCount = repCount,
            phase = phase,
            depth = depth,
            hint = hintToSay,
            backWarning = sawBackWarning,
            heelWarning = sawHeelLift
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

    private fun footAngle(
        lAnkle: PosePoint?, lFoot: PosePoint?,
        rAnkle: PosePoint?, rFoot: PosePoint?
    ): Float? {
        val angles = mutableListOf<Float>()

        if (lAnkle != null && lFoot != null &&
            AngleMath.dist2D(lAnkle, lFoot) > FOOT_VISIBLE_MIN_DIST
        ) {
            footAngleSide(lAnkle, lFoot)?.let { angles.add(it) }
        }

        if (rAnkle != null && rFoot != null &&
            AngleMath.dist2D(rAnkle, rFoot) > FOOT_VISIBLE_MIN_DIST
        ) {
            footAngleSide(rAnkle, rFoot)?.let { angles.add(it) }
        }

        if (angles.isEmpty()) return null
        return angles.average().toFloat()
    }

    private fun footAngleSide(ankle: PosePoint, foot: PosePoint): Float? {
        val dx = foot.px - ankle.px
        val dy = foot.py - ankle.py
        if (abs(dx) < 1e-6f && abs(dy) < 1e-6f) return null
        val angle = Math.toDegrees(
            kotlin.math.atan2(abs(dy).toDouble(), abs(dx).toDouble())
        ).toFloat()
        return angle
    }
}