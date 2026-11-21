package com.example.game

import kotlin.math.abs
import kotlin.math.hypot

enum class ConveyorSegment {
  LEFT_ARM,
  RIGHT_ARM,
  RING,
}

data class LaneSample(
  val x: Float,
  val y: Float,
  val tx: Float,
  val ty: Float,
  val nx: Float,
  val ny: Float,
)

private data class ArcPoint(
  val dist: Float,
  val x: Float,
  val y: Float,
)

/**
 * Continuous Catmull-Rom arc-length lane geometry matching the top U-shaped road & oval loop:
 * - Left arm enters from top-left down into the oval ring at left junction (ringDist = 0)
 * - Right arm enters from top-right down into the oval ring at right junction (ringDist = rightEntryRingDistDp)
 * - Closed oval ring circulates smoothly around bottom boarding zone -> right -> top -> left
 */
class LaneGeometry(
  val widthDp: Float,
  val heightDp: Float,
) {
  // Sized so 4 plump chibi passengers sit comfortably across the wider 135px HD oval loop & arms
  val safeHalfRoadWidthDp: Float = minOf(widthDp * 0.0660f, heightDp * 0.0280f).coerceAtLeast(22.5f)
  private val outerRimMarginDp: Float = (0.85f / 360f) * widthDp
  val colStepDp: Float = (safeHalfRoadWidthDp - outerRimMarginDp) / 1.94f
  val passengerSlotWidthDp: Float = colStepDp
  val passengerWidthDp: Float = colStepDp * 1.16f
  val rowSpacingDp: Float = colStepDp * 0.96f
  val armHoldStopOffsetDp: Float = safeHalfRoadWidthDp * 1.40f + rowSpacingDp * 0.45f

  private val ringPoints: List<ArcPoint>
  private val leftArmPoints: List<ArcPoint>
  private val rightArmPoints: List<ArcPoint>

  val ringLenDp: Float
  val leftArmLenDp: Float
  val rightArmLenDp: Float
  val leftEntryRingDistDp: Float = 0f
  val rightEntryRingDistDp: Float
  val boardingGateCenterDp: Float
  val boardingGateHalfWindowDp: Float
  val boardingStartDp: Float
  val boardingEndDp: Float

  init {
    // Control points measured directly from the 135px-wide video roundabout centerlines (cx=0.500, cy=0.220, rx=0.252, ry=0.0765)
    // Ordered CLOCKWISE (9:30h -> 12h -> 2:30h -> 6h boarding gate -> 9:30h) matching the rotating belt
    val ringControl = listOf(
      pt(0.2500f, 0.2050f), // 0: left junction (9:30 o'clock)
      pt(0.2850f, 0.1715f), // 1: top-left curve (10:30 o'clock)
      pt(0.3760f, 0.1505f), // 2: top-left arch (11 o'clock)
      pt(0.5000f, 0.1432f), // 3: top-center (12 o'clock)
      pt(0.6240f, 0.1505f), // 4: top-right arch (1 o'clock)
      pt(0.7150f, 0.1715f), // 5: top-right curve (1:30 o'clock)
      pt(0.7500f, 0.2050f), // 6: right junction (2:30 o'clock)
      pt(0.7300f, 0.2530f), // 7: bottom-right curve (4 o'clock)
      pt(0.6240f, 0.2875f), // 8: bottom-right exit arch (5 o'clock)
      pt(0.5000f, 0.2962f), // 9: bottom-center boarding gate (6 o'clock)
      pt(0.3760f, 0.2875f), // 10: bottom-left approach arch (7 o'clock)
      pt(0.2700f, 0.2530f), // 11: bottom-left curve (8 o'clock)
    )
    ringPoints = buildClosedCatmullRom(ringControl, stepsPerSeg = 22)
    ringLenDp = ringPoints.last().dist

    // Find arc distances at key ring landmarks
    rightEntryRingDistDp = ringPoints[6 * 22].dist
    boardingGateCenterDp = ringPoints[9 * 22].dist
    // Boarding gate covers a clean row-capture window at bottom center so full 4-wide rows board together
    boardingGateHalfWindowDp = rowSpacingDp * 0.80f
    boardingStartDp = wrapRing(boardingGateCenterDp - boardingGateHalfWindowDp)
    boardingEndDp = wrapRing(boardingGateCenterDp + boardingGateHalfWindowDp)

    val leftArmControl = listOf(
      pt(-0.0220f, -0.0200f),
      pt(-0.0050f, 0.0450f),
      pt(0.0200f, 0.1050f),
      pt(0.0550f, 0.1480f),
      pt(0.1100f, 0.1760f),
      pt(0.1750f, 0.1920f),
      pt(0.2500f, 0.2050f),
    )
    leftArmPoints = buildOpenCatmullRom(leftArmControl, stepsPerSeg = 20)
    leftArmLenDp = leftArmPoints.last().dist

    val rightArmControl = listOf(
      pt(1.0220f, -0.0200f),
      pt(1.0050f, 0.0450f),
      pt(0.9800f, 0.1050f),
      pt(0.9450f, 0.1480f),
      pt(0.8900f, 0.1760f),
      pt(0.8250f, 0.1920f),
      pt(0.7500f, 0.2050f),
    )
    rightArmPoints = buildOpenCatmullRom(rightArmControl, stepsPerSeg = 20)
    rightArmLenDp = rightArmPoints.last().dist
  }

  private fun pt(nx: Float, ny: Float): Pair<Float, Float> = (nx * widthDp) to (ny * heightDp)

  /**
   * Returns signed offset along the ring relative to [boardingGateCenterDp] if inside the short
   * [+- boardingGateHalfWindowDp] gate window; otherwise null.
   */
  fun signedGateOffsetDp(segment: ConveyorSegment, distDp: Float): Float? {
    if (segment != ConveyorSegment.RING || ringLenDp <= 0.001f) return null
    val wrapped = wrapRing(distDp)
    var delta = wrapped - boardingGateCenterDp
    val halfRing = ringLenDp * 0.5f
    if (delta > halfRing) delta -= ringLenDp
    if (delta < -halfRing) delta += ringLenDp
    return if (abs(delta) <= boardingGateHalfWindowDp) delta else null
  }

  fun isInBoardingZone(segment: ConveyorSegment, distDp: Float): Boolean {
    return signedGateOffsetDp(segment, distDp) != null
  }

  fun wrapRing(distDp: Float): Float {
    if (ringLenDp <= 0.001f) return 0f
    var m = distDp % ringLenDp
    if (m < 0f) m += ringLenDp
    return m
  }

  fun forwardRingDistance(fromDp: Float, toDp: Float): Float {
    val a = wrapRing(fromDp)
    val b = wrapRing(toDp)
    val d = b - a
    return if (d >= 0f) d else d + ringLenDp
  }

  /**
   * Perspective scale matching the oval loop & feeder arms of the 1080x2402 HD game_play_bg:
   * - Top arch: 0.98x so 4 plump chibis stand cleanly along the upper arch
   * - Bottom arch: 1.06x along the foreground boarding curve
   * - Feeder arms: 0.90x near upper corners growing smoothly to 1.04x into the ring junction
   * - Wide side curves (9 & 3 o'clock): smoothly grows to 1.16x across the wide horizontal curb
   */
  fun passengerScale(segment: ConveyorSegment, distDp: Float): Float {
    val s = sample(segment, distDp)
    val nx = (s.x / widthDp).coerceIn(0f, 1f)
    val ny = (s.y / heightDp).coerceIn(0f, 0.40f)
    if (segment != ConveyorSegment.RING) {
      val armBlend = ((ny - 0.03f) / 0.18f).coerceIn(0f, 1f)
      return 0.92f + 0.12f * armBlend
    }
    val sideFactor = (abs(nx - 0.500f) / 0.2500f).coerceIn(0f, 1f)
    val archFactor = if (ny > 0.2200f) 1.08f else 1.00f
    return archFactor + (1.16f - archFactor) * (sideFactor * sideFactor)
  }

  private fun columnSpreadScale(segment: ConveyorSegment, distDp: Float): Float {
    val s = sample(segment, distDp)
    val nx = (s.x / widthDp).coerceIn(0f, 1f)
    val ny = (s.y / heightDp).coerceIn(0f, 0.40f)
    if (segment != ConveyorSegment.RING) {
      val armBlend = ((ny - 0.03f) / 0.18f).coerceIn(0f, 1f)
      return 0.80f + 0.12f * armBlend
    }
    val sideFactor = (abs(nx - 0.500f) / 0.2500f).coerceIn(0f, 1f)
    val archFactor = if (ny > 0.2200f) 0.96f else 0.90f
    return archFactor + (1.04f - archFactor) * (sideFactor * sideFactor)
  }

  fun localSafeHalfRoadWidthDp(segment: ConveyorSegment, distDp: Float): Float {
    val scale = passengerScale(segment, distDp)
    return safeHalfRoadWidthDp * scale.coerceAtLeast(1.0f)
  }

  fun sample(segment: ConveyorSegment, distDp: Float): LaneSample {
    return when (segment) {
      ConveyorSegment.RING -> samplePolyline(ringPoints, wrapRing(distDp), closed = true)
      ConveyorSegment.LEFT_ARM -> sampleOpenArm(leftArmPoints, distDp)
      ConveyorSegment.RIGHT_ARM -> sampleOpenArm(rightArmPoints, distDp)
    }
  }

  fun passengerCoords(segment: ConveyorSegment, distDp: Float, colInRow: Int): Pair<LaneSample, Pair<Float, Float>> {
    val s = sample(segment, distDp)
    val scale = passengerScale(segment, distDp)
    val spreadScale = columnSpreadScale(segment, distDp)
    val localHalf = localSafeHalfRoadWidthDp(segment, distDp)
    val maxOffset = (localHalf - outerRimMarginDp - passengerWidthDp * scale * 0.38f).coerceAtLeast(1f)
    val offset = ((colInRow - 1.5f) * colStepDp * spreadScale).coerceIn(-maxOffset, maxOffset)
    return s to ((s.x + s.nx * offset) to (s.y + s.ny * offset))
  }

  fun roadCenterlinePoints(
    segment: ConveyorSegment,
    steps: Int = 128,
    endFraction: Float = 1f,
  ): List<Pair<Float, Float>> {
    val maxDist = when (segment) {
      ConveyorSegment.RING -> ringLenDp
      ConveyorSegment.LEFT_ARM -> leftArmLenDp
      ConveyorSegment.RIGHT_ARM -> rightArmLenDp
    } * endFraction.coerceIn(0.1f, 1f)
    val count = steps.coerceAtLeast(8)
    val pts = ArrayList<Pair<Float, Float>>(count + 1)
    for (i in 0..count) {
      val d = (i.toFloat() / count) * maxDist
      val s = sample(segment, d)
      pts.add(s.x to s.y)
    }
    return pts
  }

  fun debugPaths(): List<List<Pair<Float, Float>>> {
    val result = mutableListOf<List<Pair<Float, Float>>>()
    val offsets = listOf(
      -safeHalfRoadWidthDp,
      -1.5f * colStepDp,
      -0.5f * colStepDp,
      0f,
      0.5f * colStepDp,
      1.5f * colStepDp,
      safeHalfRoadWidthDp,
    )
    for (seg in listOf(ConveyorSegment.LEFT_ARM, ConveyorSegment.RING, ConveyorSegment.RIGHT_ARM)) {
      val maxDist = when (seg) {
        ConveyorSegment.LEFT_ARM -> leftArmLenDp
        ConveyorSegment.RIGHT_ARM -> rightArmLenDp
        ConveyorSegment.RING -> ringLenDp
      }
      val steps = if (seg == ConveyorSegment.RING) 96 else 36
      for (off in offsets) {
        val pts = ArrayList<Pair<Float, Float>>(steps + 1)
        for (i in 0..steps) {
          val d = (i.toFloat() / steps) * maxDist
          val s = sample(seg, d)
          pts.add((s.x + s.nx * off) to (s.y + s.ny * off))
        }
        result.add(pts)
      }
    }
    return result
  }

  private fun sampleOpenArm(pts: List<ArcPoint>, distDp: Float): LaneSample {
    if (distDp <= 0f) {
      // Extrapolate smoothly upward above top edge of screen along initial entry tangent
      val s0 = samplePolyline(pts, 0f, closed = false)
      return LaneSample(
        x = s0.x + s0.tx * distDp,
        y = s0.y + s0.ty * distDp,
        tx = s0.tx,
        ty = s0.ty,
        nx = s0.nx,
        ny = s0.ny,
      )
    }
    return samplePolyline(pts, distDp.coerceAtMost(pts.last().dist), closed = false)
  }

  private fun samplePolyline(pts: List<ArcPoint>, targetDist: Float, closed: Boolean): LaneSample {
    val n = pts.size
    if (n < 2) return LaneSample(0f, 0f, 1f, 0f, 0f, 1f)

    var lo = 0
    var hi = n - 2
    while (lo < hi) {
      val mid = (lo + hi + 1) / 2
      if (pts[mid].dist <= targetDist) lo = mid else hi = mid - 1
    }

    val p0 = pts[lo]
    val p1 = pts[(lo + 1).coerceAtMost(n - 1)]
    val segLen = (p1.dist - p0.dist).coerceAtLeast(0.0001f)
    val frac = ((targetDist - p0.dist) / segLen).coerceIn(0f, 1f)
    val x = p0.x + (p1.x - p0.x) * frac
    val y = p0.y + (p1.y - p0.y) * frac

    // Smooth tangent across neighboring samples
    val prev = pts[if (lo > 0) lo - 1 else if (closed) n - 2 else 0]
    val next = pts[if (lo + 2 < n) lo + 2 else if (closed) 1 else n - 1]
    val dx = next.x - prev.x
    val dy = next.y - prev.y
    val len = hypot(dx, dy).coerceAtLeast(0.0001f)
    val tx = dx / len
    val ty = dy / len
    return LaneSample(x = x, y = y, tx = tx, ty = ty, nx = -ty, ny = tx)
  }

  private fun buildClosedCatmullRom(control: List<Pair<Float, Float>>, stepsPerSeg: Int): List<ArcPoint> {
    val n = control.size
    val raw = ArrayList<Pair<Float, Float>>(n * stepsPerSeg + 1)
    for (i in 0 until n) {
      val p0 = control[(i - 1 + n) % n]
      val p1 = control[i]
      val p2 = control[(i + 1) % n]
      val p3 = control[(i + 2) % n]
      for (s in 0 until stepsPerSeg) {
        raw.add(catmullRom(p0, p1, p2, p3, s.toFloat() / stepsPerSeg))
      }
    }
    raw.add(raw.first())
    return toArcLength(raw)
  }

  private fun buildOpenCatmullRom(control: List<Pair<Float, Float>>, stepsPerSeg: Int): List<ArcPoint> {
    val n = control.size
    val raw = ArrayList<Pair<Float, Float>>((n - 1) * stepsPerSeg + 1)
    for (i in 0 until n - 1) {
      val p0 = if (i == 0) control[0] else control[i - 1]
      val p1 = control[i]
      val p2 = control[i + 1]
      val p3 = if (i + 2 < n) control[i + 2] else control[n - 1]
      for (s in 0 until stepsPerSeg) {
        raw.add(catmullRom(p0, p1, p2, p3, s.toFloat() / stepsPerSeg))
      }
    }
    raw.add(control.last())
    return toArcLength(raw)
  }

  private fun catmullRom(
    p0: Pair<Float, Float>,
    p1: Pair<Float, Float>,
    p2: Pair<Float, Float>,
    p3: Pair<Float, Float>,
    t: Float,
  ): Pair<Float, Float> {
    val t2 = t * t
    val t3 = t2 * t
    val x = 0.5f * (
      (2f * p1.first) +
        (-p0.first + p2.first) * t +
        (2f * p0.first - 5f * p1.first + 4f * p2.first - p3.first) * t2 +
        (-p0.first + 3f * p1.first - 3f * p2.first + p3.first) * t3
      )
    val y = 0.5f * (
      (2f * p1.second) +
        (-p0.second + p2.second) * t +
        (2f * p0.second - 5f * p1.second + 4f * p2.second - p3.second) * t2 +
        (-p0.second + 3f * p1.second - 3f * p2.second + p3.second) * t3
      )
    return x to y
  }

  private fun toArcLength(raw: List<Pair<Float, Float>>): List<ArcPoint> {
    val out = ArrayList<ArcPoint>(raw.size)
    var acc = 0f
    out.add(ArcPoint(0f, raw[0].first, raw[0].second))
    for (i in 1 until raw.size) {
      val dx = raw[i].first - raw[i - 1].first
      val dy = raw[i].second - raw[i - 1].second
      acc += hypot(dx, dy)
      out.add(ArcPoint(acc, raw[i].first, raw[i].second))
    }
    return out
  }
}
