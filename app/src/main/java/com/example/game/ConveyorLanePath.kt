package com.example.game

import kotlin.math.abs
import kotlin.math.hypot

enum class ConveyorSegment {
  LEFT_ARM,
  RIGHT_ARM,
  RING,
}

enum class TrackLoopShape {
  LEVEL1_OVAL,
  LEVEL2_ROUNDED_OVAL,
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
  val passengerSizeScale: Float = 1.0f,
  val trackShape: TrackLoopShape = TrackLoopShape.LEVEL1_OVAL,
) {
  private val sizeScale: Float = passengerSizeScale.coerceIn(0.65f, 1.50f)
  private val spriteScaleBase: Float = minOf(widthDp / 360f, heightDp / 800f).coerceAtLeast(0.75f)
  // Plump upright chibi proportions uniform across all 4 lanes
  val passengerSpriteWidthDp: Float =
    (if (trackShape == TrackLoopShape.LEVEL2_ROUNDED_OVAL) 14.0f else 14.2f) * spriteScaleBase * sizeScale
  val passengerSpriteHeightDp: Float =
    if (trackShape == TrackLoopShape.LEVEL2_ROUNDED_OVAL) {
      passengerSpriteWidthDp * (216f / 180f) * 0.85f
    } else {
      passengerSpriteWidthDp * (216f / 180f) * 0.90f
    }
  val safeHalfRoadWidthDp: Float =
    if (trackShape == TrackLoopShape.LEVEL2_ROUNDED_OVAL) {
      minOf(widthDp * 0.0560f, heightDp * 0.0184f).coerceAtLeast(14.5f)
    } else {
      minOf(widthDp * 0.044f, heightDp * 0.0195f).coerceAtLeast(15.2f)
    }
  val colStepDp: Float =
    (if (trackShape == TrackLoopShape.LEVEL2_ROUNDED_OVAL) 4.65f else 8.2f) *
      (0.45f + 0.55f * sizeScale) *
      spriteScaleBase
  val passengerSlotWidthDp: Float = colStepDp
  val passengerWidthDp: Float = passengerSpriteWidthDp * 0.78f
  val rowSpacingDp: Float
  val armHoldStopOffsetDp: Float =
    (if (trackShape == TrackLoopShape.LEVEL2_ROUNDED_OVAL) 26.0f else 25.0f) * (widthDp / 360f)

  private val ringPoints: List<ArcPoint>
  private val leftArmPoints: List<ArcPoint>
  private val rightArmPoints: List<ArcPoint>

  val ringLenDp: Float
  val leftArmLenDp: Float
  val rightArmLenDp: Float
  val leftEntryRingDistDp: Float
  val rightEntryRingDistDp: Float
  val boardingGateCenterDp: Float
  val boardingGateHalfWindowDp: Float
  val boardingStartDp: Float
  val boardingEndDp: Float

  init {
    if (trackShape == TrackLoopShape.LEVEL2_ROUNDED_OVAL) {
      // Exact rounded-oval center track extracted from game_rounded_road_loop_level2.png (CLOCKWISE)
      val ringControl = listOf(
        pt(0.2165f, 0.2350f), // 0: 9 o'clock left curve mid
        pt(0.2470f, 0.2042f), // 1: top-left corner
        pt(0.3100f, 0.1930f), // 2: top-left lead-in
        pt(0.4100f, 0.1876f), // 3: top straight left
        pt(0.5000f, 0.1868f), // 4: top center
        pt(0.5900f, 0.1876f), // 5: top straight right
        pt(0.6900f, 0.1930f), // 6: top-right lead-out
        pt(0.7530f, 0.2042f), // 7: top-right corner
        pt(0.7835f, 0.2350f), // 8: 3 o'clock right curve mid
        pt(0.7530f, 0.2645f), // 9: bottom-right corner
        pt(0.6900f, 0.2762f), // 10: bottom-right lead-in
        pt(0.5900f, 0.2816f), // 11: bottom straight right
        pt(0.5000f, 0.2822f), // 12: bottom center (boarding gate)
        pt(0.4100f, 0.2816f), // 13: bottom straight left
        pt(0.3100f, 0.2762f), // 14: bottom-left lead-out
        pt(0.2470f, 0.2645f), // 15: bottom-left corner
      )
      val stepsPerSeg = 16
      ringPoints = buildClosedCatmullRom(ringControl, stepsPerSeg = stepsPerSeg)
      ringLenDp = ringPoints.last().dist
      rowSpacingDp = maxOf(10.0f * spriteScaleBase, ringLenDp / 46f)

      val leftEntryIdx = 5
      val rightEntryIdx = 7 * stepsPerSeg + 11
      leftEntryRingDistDp = ringPoints[leftEntryIdx].dist
      rightEntryRingDistDp = ringPoints[rightEntryIdx].dist
      boardingGateCenterDp = ringPoints[12 * stepsPerSeg].dist
      boardingGateHalfWindowDp = maxOf(rowSpacingDp * 1.05f, 11.5f * spriteScaleBase)
      boardingStartDp = wrapRing(boardingGateCenterDp - boardingGateHalfWindowDp)
      boardingEndDp = wrapRing(boardingGateCenterDp + boardingGateHalfWindowDp)

      val leftJoinPt = ringPoints[leftEntryIdx]
      val leftJoinLookAhead = ringPoints[(leftEntryIdx + 6).coerceAtMost(ringPoints.lastIndex)]
      val leftArmControl = listOf(
        pt( 0.010f, 0.000f),
        pt(-0.005f, 0.065f),
        pt(-0.015f, 0.125f),
        pt( 0.008f, 0.166f),
        pt( 0.040f, 0.1825f),
        pt( 0.080f, 0.1950f),
        pt( 0.120f, 0.2050f),
        pt( 0.160f, 0.2140f),
        pt( 0.192f, 0.2200f),
        leftJoinPt.x to leftJoinPt.y,
      )
      leftArmPoints =
        buildOpenCatmullRom(
          control = leftArmControl,
          stepsPerSeg = 20,
          endLookAhead = leftJoinLookAhead.x to leftJoinLookAhead.y,
        )
      leftArmLenDp = leftArmPoints.last().dist

      val rightJoinPt = ringPoints[rightEntryIdx]
      val rightJoinLookAhead = ringPoints[(rightEntryIdx + 6).coerceAtMost(ringPoints.lastIndex)]
      val rightArmControl = listOf(
        pt(0.990f, 0.000f),
        pt(1.005f, 0.065f),
        pt(1.015f, 0.125f),
        pt(0.992f, 0.166f),
        pt(0.960f, 0.1825f),
        pt(0.920f, 0.1950f),
        pt(0.880f, 0.2050f),
        pt(0.840f, 0.2140f),
        pt(0.808f, 0.2200f),
        rightJoinPt.x to rightJoinPt.y,
      )
      rightArmPoints =
        buildOpenCatmullRom(
          control = rightArmControl,
          stepsPerSeg = 20,
          endLookAhead = rightJoinLookAhead.x to rightJoinLookAhead.y,
        )
      rightArmLenDp = rightArmPoints.last().dist
    } else {
      // Balanced ~45-row spacing around Level 1 oval ring
      rowSpacingDp = 10.2f * spriteScaleBase

      // Control points measured directly from game_oval_road_loop_v3.png asphalt centerlines
      // Ordered CLOCKWISE (9h -> 12h -> 3h -> 6h boarding gate -> 9h)
      val ringControl = listOf(
        pt(0.2680f, 0.2041f), // 0: left junction (9 o'clock)
        pt(0.3222f, 0.1520f), // 1: top-left curve (10 o'clock)
        pt(0.4153f, 0.1297f), // 2: top-left arch (11 o'clock)
        pt(0.5000f, 0.1243f), // 3: top-center (12 o'clock)
        pt(0.5847f, 0.1297f), // 4: top-right arch (1 o'clock)
        pt(0.6773f, 0.1520f), // 5: top-right curve (2 o'clock)
        pt(0.7320f, 0.2041f), // 6: right junction (3 o'clock)
        pt(0.6792f, 0.2564f), // 7: bottom-right curve (4 o'clock)
        pt(0.5838f, 0.2780f), // 8: bottom-right exit arch (5 o'clock)
        pt(0.5000f, 0.2831f), // 9: bottom-center boarding gate (6 o'clock)
        pt(0.4162f, 0.2780f), // 10: bottom-left approach arch (7 o'clock)
        pt(0.3204f, 0.2566f), // 11: bottom-left curve (8 o'clock)
      )
      ringPoints = buildClosedCatmullRom(ringControl, stepsPerSeg = 22)
      ringLenDp = ringPoints.last().dist

      // Find arc distances at key ring landmarks
      val entryArcOffsetSteps = 8
      leftEntryRingDistDp = ringPoints[entryArcOffsetSteps].dist
      rightEntryRingDistDp = ringPoints[6 * 22 + entryArcOffsetSteps].dist
      boardingGateCenterDp = ringPoints[9 * 22].dist
      // Boarding gate covers a clean row-capture window at bottom center so matching rows board crisply
      boardingGateHalfWindowDp = rowSpacingDp * 0.95f
      boardingStartDp = wrapRing(boardingGateCenterDp - boardingGateHalfWindowDp)
      boardingEndDp = wrapRing(boardingGateCenterDp + boardingGateHalfWindowDp)

      val leftJoinPt = ringPoints[entryArcOffsetSteps]
      val leftJoinLookAhead = ringPoints[(entryArcOffsetSteps + 6).coerceAtMost(ringPoints.lastIndex)]
      val leftArmControl = listOf(
        pt(-0.0400f, 0.0060f),
        pt(-0.0330f, 0.0380f),
        pt(-0.0170f, 0.0780f),
        pt( 0.0080f, 0.1160f),
        pt( 0.0460f, 0.1500f),
        pt( 0.0960f, 0.1730f),
        pt( 0.1540f, 0.1855f),
        pt( 0.2120f, 0.1920f),
        pt( 0.2540f, 0.1925f),
        leftJoinPt.x to leftJoinPt.y,
      )
      leftArmPoints =
        buildOpenCatmullRom(
          control = leftArmControl,
          stepsPerSeg = 20,
          endLookAhead = leftJoinLookAhead.x to leftJoinLookAhead.y,
        )
      leftArmLenDp = leftArmPoints.last().dist

      val rightJoinIndex = 6 * 22 + entryArcOffsetSteps
      val rightJoinPt = ringPoints[rightJoinIndex]
      val rightJoinLookAhead = ringPoints[(rightJoinIndex + 6).coerceAtMost(ringPoints.lastIndex)]
      val rightArmControl = listOf(
        pt(1.0400f, 0.0060f),
        pt(1.0330f, 0.0380f),
        pt(1.0170f, 0.0780f),
        pt(0.9920f, 0.1160f),
        pt(0.9540f, 0.1500f),
        pt(0.9040f, 0.1730f),
        pt(0.8460f, 0.1855f),
        pt(0.7880f, 0.1920f),
        pt(0.7460f, 0.1925f),
        rightJoinPt.x to rightJoinPt.y,
      )
      rightArmPoints =
        buildOpenCatmullRom(
          control = rightArmControl,
          stepsPerSeg = 20,
          endLookAhead = rightJoinLookAhead.x to rightJoinLookAhead.y,
        )
      rightArmLenDp = rightArmPoints.last().dist
    }
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
   * Strictly uniform 1:1 passenger scale across every row and column around the entire loop.
   */
  fun passengerScale(
    @Suppress("UNUSED_PARAMETER") segment: ConveyorSegment,
    @Suppress("UNUSED_PARAMETER") distDp: Float,
  ): Float = 1.0f

  private fun localAsphaltHalfWidthDp(segment: ConveyorSegment, s: LaneSample, distDp: Float = 0f): Float {
    if (trackShape == TrackLoopShape.LEVEL2_ROUNDED_OVAL) {
      val ringVerticalHalf = 0.0184f * heightDp
      val ringHorizontalHalf = 0.0560f * widthDp
      if (segment == ConveyorSegment.RING) {
        return s.nx * s.nx * ringHorizontalHalf + s.ny * s.ny * ringVerticalHalf
      }
      val armLen = if (segment == ConveyorSegment.LEFT_ARM) leftArmLenDp else rightArmLenDp
      val blendStart = (armLen - armHoldStopOffsetDp).coerceAtLeast(0f)
      val joinBlend =
        if (distDp > blendStart && armHoldStopOffsetDp > 0.01f) {
          ((distDp - blendStart) / armHoldStopOffsetDp).coerceIn(0f, 1f)
        } else {
          0f
        }
      val armVerticalHalf = 0.0176f * heightDp
      val armHorizontalHalf = 0.0500f * widthDp
      val verticalHalf = armVerticalHalf + (ringVerticalHalf - armVerticalHalf) * joinBlend
      val horizontalHalf = armHorizontalHalf + (ringHorizontalHalf - armHorizontalHalf) * joinBlend
      return s.nx * s.nx * horizontalHalf + s.ny * s.ny * verticalHalf
    }
    val ringVerticalHalf = if (s.y < 0.2041f * heightDp) 0.0196f * heightDp else 0.0190f * heightDp
    val ringHorizontalHalf = 0.0585f * widthDp
    if (segment == ConveyorSegment.RING) {
      return s.nx * s.nx * ringHorizontalHalf + s.ny * s.ny * ringVerticalHalf
    }
    val armLen = if (segment == ConveyorSegment.LEFT_ARM) leftArmLenDp else rightArmLenDp
    val blendStart = (armLen - armHoldStopOffsetDp).coerceAtLeast(0f)
    val joinBlend =
      if (distDp > blendStart && armHoldStopOffsetDp > 0.01f) {
        ((distDp - blendStart) / armHoldStopOffsetDp).coerceIn(0f, 1f)
      } else {
        0f
      }
    val armVerticalHalf = 0.0168f * heightDp
    val armHorizontalHalf = 0.0440f * widthDp
    val verticalHalf = armVerticalHalf + (ringVerticalHalf - armVerticalHalf) * joinBlend
    val horizontalHalf = armHorizontalHalf + (ringHorizontalHalf - armHorizontalHalf) * joinBlend
    return s.nx * s.nx * horizontalHalf + s.ny * s.ny * verticalHalf
  }

  fun localSafeHalfRoadWidthDp(segment: ConveyorSegment, distDp: Float): Float {
    return localAsphaltHalfWidthDp(segment, sample(segment, distDp), distDp)
  }

  fun sample(segment: ConveyorSegment, distDp: Float): LaneSample {
    return when (segment) {
      ConveyorSegment.RING -> samplePolyline(ringPoints, wrapRing(distDp), closed = true)
      ConveyorSegment.LEFT_ARM -> sampleArmWithJoinBlend(leftArmPoints, distDp, leftEntryRingDistDp)
      ConveyorSegment.RIGHT_ARM -> sampleArmWithJoinBlend(rightArmPoints, distDp, rightEntryRingDistDp)
    }
  }

  fun passengerCoords(
    segment: ConveyorSegment,
    distDp: Float,
    colInRow: Int,
    widthScale: Float = 1.0f,
    heightScale: Float = 1.0f,
  ): Pair<LaneSample, Pair<Float, Float>> {
    val s = sample(segment, distDp)
    val localHalf = localAsphaltHalfWidthDp(segment, s, distDp)
    val effectiveWidthScale = widthScale.coerceIn(0.65f, 1.65f)
    val effectiveHeightScale = heightScale.coerceIn(0.65f, 1.65f)

    if (trackShape == TrackLoopShape.LEVEL2_ROUNDED_OVAL) {
      // Visual-center anchored full-figure chibi kept strictly inside the dark asphalt away from white curbs
      val effectiveW = passengerSpriteWidthDp * effectiveWidthScale
      val effectiveH = passengerSpriteHeightDp * effectiveHeightScale
      val ellipseProj = hypot(effectiveW * 0.36f * abs(s.nx), effectiveH * 0.51f * abs(s.ny))
      val boxProj = effectiveW * 0.34f * abs(s.nx) + effectiveH * 0.49f * abs(s.ny)
      val bodyHalfAlongNormal = maxOf(ellipseProj, boxProj * 0.90f)
      val curbMargin = 0.9f * spriteScaleBase
      val minColsSpan = 3.2f * spriteScaleBase
      val maxOffset = (localHalf - curbMargin - bodyHalfAlongNormal).coerceAtLeast(minColsSpan)
      val localColStep = minOf(colStepDp, maxOffset / 1.5f)
      val offset = ((colInRow - 1.5f) * localColStep).coerceIn(-maxOffset, maxOffset)
      return s to ((s.x + s.nx * offset) to (s.y + s.ny * offset))
    }

    // Foot-anchored footprint: keep all 4 passengers strictly inside the asphalt curb borders on both arms & turns
    val footHalfW = passengerSpriteWidthDp * effectiveWidthScale * 0.44f
    val footHalfH = passengerSpriteHeightDp * effectiveHeightScale * 0.16f
    val footHalfAlongNormal = hypot(s.nx * footHalfW, s.ny * footHalfH)
    val curbMargin = (1.60f + 0.90f * (1f - abs(s.ny))) * spriteScaleBase
    val minColsSpan = 3.2f * spriteScaleBase
    val maxOffset = (localHalf - curbMargin - footHalfAlongNormal).coerceAtLeast(minColsSpan)
    val localColStep = minOf(colStepDp, maxOffset / 1.5f)
    val offset = ((colInRow - 1.5f) * localColStep).coerceIn(-maxOffset, maxOffset)
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

  private fun sampleArmWithJoinBlend(
    pts: List<ArcPoint>,
    distDp: Float,
    entryRingDistDp: Float,
  ): LaneSample {
    val raw = sampleOpenArm(pts, distDp)
    val armLen = pts.last().dist
    val blendSpan = (armHoldStopOffsetDp * 0.55f).coerceAtLeast(1f)
    val blendStart = (armLen - blendSpan).coerceAtLeast(0f)
    if (distDp <= blendStart) return raw
    val t = ((distDp - blendStart) / blendSpan).coerceIn(0f, 1f)
    val smooth = t * t * (3f - 2f * t)
    val ringEntry = samplePolyline(ringPoints, wrapRing(entryRingDistDp), closed = true)
    val bx = raw.tx + (ringEntry.tx - raw.tx) * smooth
    val by = raw.ty + (ringEntry.ty - raw.ty) * smooth
    val bLen = hypot(bx, by).coerceAtLeast(0.0001f)
    val tx = bx / bLen
    val ty = by / bLen
    return LaneSample(x = raw.x, y = raw.y, tx = tx, ty = ty, nx = -ty, ny = tx)
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

  private fun buildOpenCatmullRom(
    control: List<Pair<Float, Float>>,
    stepsPerSeg: Int,
    endLookAhead: Pair<Float, Float>? = null,
  ): List<ArcPoint> {
    val n = control.size
    val raw = ArrayList<Pair<Float, Float>>((n - 1) * stepsPerSeg + 1)
    for (i in 0 until n - 1) {
      val p0 = if (i == 0) control[0] else control[i - 1]
      val p1 = control[i]
      val p2 = control[i + 1]
      val p3 =
        when {
          i + 2 < n -> control[i + 2]
          endLookAhead != null -> endLookAhead
          else -> control[n - 1]
        }
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
