package com.example.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.AssetManager
import com.example.game.BusView
import com.example.game.ConveyorPassenger
import com.example.game.ConveyorSegment
import com.example.game.Direction
import com.example.game.FloatingText
import com.example.game.JumpingPassenger
import com.example.game.LaneGeometry
import com.example.game.LaneSample
import com.example.game.Particle
import com.example.game.SlotKind
import com.example.game.TunnelSide
import com.example.game.TunnelState
import com.example.game.VehicleColor
import com.example.game.VehicleEntity
import com.example.game.VehicleStage
import com.example.game.WaitingSlotState
import com.example.game.rememberAssetManager
import com.example.ui.theme.AlertRed
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.OutlineInk
import com.example.ui.theme.PlayGreen
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val OVAL_CENTER_X_NORM = 0.500f
private const val OVAL_CENTER_Y_NORM = 0.220f
private val SLOT_CENTERS_X_NORM = floatArrayOf(
  0.1444f, // Slot 0 (VIP)
  0.2403f, // Slot 1
  0.3375f, // Slot 2
  0.4347f, // Slot 3
  0.5324f, // Slot 4
  0.6338f, // Slot 5 (+ locked)
  0.7324f, // Slot 6 (+ locked)
  0.8306f, // Slot 7 (+ locked)
)
private const val SLOT_CENTER_Y_NORM = 0.3828f
private const val SLOT_TILT_DEG = -17.5f

private fun slotCenterXNorm(index: Int): Float =
  if (index in SLOT_CENTERS_X_NORM.indices) SLOT_CENTERS_X_NORM[index]
  else 0.1313f + index * 0.104f

private const val ISO_COS = 0.78f
private const val ISO_SIN = 0.625f

private data class LotMetrics(
  val cellPx: Float,
  val stride: Float,
  val gapPx: Float,
  val cellBoxPx: Float,
  val centerX: Float,
  val centerY: Float,
  val gridCols: Int,
  val gridRows: Int,
) {
  fun isoToScreenX(gx: Float, gy: Float): Float {
    val du = gx - gridCols * 0.5f
    val dv = gy - gridRows * 0.5f
    return centerX + (du + dv) * cellPx * ISO_COS
  }

  fun isoToScreenY(gx: Float, gy: Float): Float {
    val du = gx - gridCols * 0.5f
    val dv = gy - gridRows * 0.5f
    return centerY + (-du + dv) * cellPx * ISO_SIN
  }

  fun isoToScreen(gx: Float, gy: Float): Offset =
    Offset(isoToScreenX(gx, gy), isoToScreenY(gx, gy))

  fun screenToIso(sx: Float, sy: Float): Pair<Float, Float> {
    val rx = (sx - centerX) / (cellPx * ISO_COS).coerceAtLeast(1f)
    val ry = (sy - centerY) / (cellPx * ISO_SIN).coerceAtLeast(1f)
    val du = (rx - ry) * 0.5f
    val dv = (rx + ry) * 0.5f
    return (du + gridCols * 0.5f) to (dv + gridRows * 0.5f)
  }
}

private fun isoScreenDirection(dir: Direction): Pair<Float, Float> =
  when (dir) {
    Direction.RIGHT, Direction.UP_RIGHT -> ISO_COS to -ISO_SIN
    Direction.DOWN, Direction.DOWN_RIGHT -> ISO_COS to ISO_SIN
    Direction.LEFT, Direction.DOWN_LEFT -> -ISO_COS to ISO_SIN
    Direction.UP, Direction.UP_LEFT -> -ISO_COS to -ISO_SIN
  }

private fun isoViewForDirection(dir: Direction): BusView =
  when (dir) {
    Direction.RIGHT, Direction.UP_RIGHT -> BusView.ISO_1
    Direction.DOWN, Direction.DOWN_RIGHT -> BusView.ISO_2
    Direction.LEFT, Direction.DOWN_LEFT -> BusView.ISO_3
    Direction.UP, Direction.UP_LEFT -> BusView.ISO_4
  }

private fun computeLotMetrics(
  widthPx: Float,
  heightPx: Float,
  gridCols: Int,
  gridRows: Int,
  density: Float = 3f,
): LotMetrics {
  val span = (gridCols + gridRows).coerceAtLeast(4).toFloat()
  val cellByW = (widthPx * 0.84f) / (span * ISO_COS)
  val cellByH = (heightPx * 0.90f) / (span * ISO_SIN)
  val cellPx = min(cellByW, cellByH).coerceIn(18f * density, 40f * density)
  val gapPx = (cellPx * 0.14f).coerceIn(3.5f * density, 6f * density)
  return LotMetrics(
    cellPx = cellPx,
    stride = cellPx,
    gapPx = gapPx,
    cellBoxPx = cellPx - gapPx,
    centerX = widthPx * 0.5f,
    centerY = heightPx * 0.50f,
    gridCols = gridCols,
    gridRows = gridRows,
  )
}

private fun DrawScope.drawMissingAssetError(
  textMeasurer: TextMeasurer,
  message: String,
  center: Offset,
  fontSizeSp: Float = 8f,
) {
  val layout =
    textMeasurer.measure(
      text = message,
      style =
        TextStyle(
          color = Color.Red,
          fontSize = fontSizeSp.sp,
          fontWeight = FontWeight.Bold,
        ),
    )
  drawText(
    textLayoutResult = layout,
    topLeft = Offset(center.x - layout.size.width * 0.5f, center.y - layout.size.height * 0.5f),
  )
}

private const val SHEET_COLS = 12
private const val SHEET_ROWS = 1

private data class SheetDirectionPose(
  val row: Int,
  val frames: IntArray,
  val flipPerFrame: BooleanArray,
  val idleCol: Int = 8,
  val idleFlip: Boolean = false,
)

private val POSE_RIGHT =
  SheetDirectionPose(
    row = 0,
    frames = intArrayOf(2, 3, 4, 3),
    flipPerFrame = booleanArrayOf(false, false, false, false),
    idleCol = 3,
    idleFlip = false,
  )
private val POSE_FRONT_RIGHT =
  SheetDirectionPose(
    row = 0,
    frames = intArrayOf(9, 11, 10, 11),
    flipPerFrame = booleanArrayOf(false, false, false, false),
    idleCol = 11,
    idleFlip = false,
  )
private val POSE_FRONT =
  SheetDirectionPose(
    row = 0,
    frames = intArrayOf(1, 0, 1, 0),
    flipPerFrame = booleanArrayOf(false, false, true, false),
    idleCol = 0,
    idleFlip = false,
  )
private val POSE_FRONT_LEFT =
  SheetDirectionPose(
    row = 0,
    frames = intArrayOf(9, 11, 10, 11),
    flipPerFrame = booleanArrayOf(true, true, true, true),
    idleCol = 11,
    idleFlip = true,
  )
private val POSE_LEFT =
  SheetDirectionPose(
    row = 0,
    frames = intArrayOf(2, 3, 4, 3),
    flipPerFrame = booleanArrayOf(true, true, true, true),
    idleCol = 3,
    idleFlip = true,
  )
private val POSE_BACK_LEFT =
  SheetDirectionPose(
    row = 0,
    frames = intArrayOf(6, 5, 8, 5),
    flipPerFrame = booleanArrayOf(true, true, true, true),
    idleCol = 5,
    idleFlip = true,
  )
private val POSE_BACK =
  SheetDirectionPose(
    row = 0,
    frames = intArrayOf(7, 5, 7, 5),
    flipPerFrame = booleanArrayOf(false, false, true, false),
    idleCol = 5,
    idleFlip = false,
  )
private val POSE_BACK_RIGHT =
  SheetDirectionPose(
    row = 0,
    frames = intArrayOf(6, 5, 8, 5),
    flipPerFrame = booleanArrayOf(false, false, false, false),
    idleCol = 5,
    idleFlip = false,
  )

private fun directionPoseForTangent(tx: Float, ty: Float): SheetDirectionPose {
  val angleDeg = (atan2(ty.toDouble(), tx.toDouble()) * 180.0 / PI).toFloat()
  return when {
    angleDeg in -24f..24f -> POSE_RIGHT
    angleDeg in 24f..68f -> POSE_FRONT_RIGHT
    angleDeg in 68f..112f -> POSE_FRONT
    angleDeg in 112f..156f -> POSE_FRONT_LEFT
    angleDeg >= 156f || angleDeg <= -156f -> POSE_LEFT
    angleDeg in -156f..-112f -> POSE_BACK_LEFT
    angleDeg in -112f..-68f -> POSE_BACK
    else -> POSE_BACK_RIGHT
  }
}

/**
 * Draws a crisp, limp-free animated frame from the 12x1 transparent chibi sprite strip (`180x216` cells).
 * - Walking passengers use symmetric 4-frame ContactA -> Pass -> ContactB -> Pass gait phase-locked to footfalls.
 * - Idle waiting passengers lock to a planted-feet standing frame (`pose.idleCol`) and only sway/bob gently in place.
 */
private fun DrawScope.drawPassengerAsset(
  sprite: ImageBitmap?,
  center: Offset,
  laneColumnWidthPx: Float,
  @Suppress("UNUSED_PARAMETER") color: VehicleColor,
  @Suppress("UNUSED_PARAMETER") textMeasurer: TextMeasurer? = null,
  alpha: Float = 1f,
  bounceScaleY: Float = 1f,
  uniformScale: Float = 1f,
  leanDeg: Float = 0f,
  pose: SheetDirectionPose = POSE_FRONT,
  beatPhase: Float = 0f,
  sheetCols: Int = SHEET_COLS,
  sheetRows: Int = SHEET_ROWS,
  isWalking: Boolean = true,
) {
  if (sprite == null) {
    return
  }

  val cols = sheetCols.coerceAtLeast(1)
  val rows = sheetRows.coerceAtLeast(1)
  val cellW = sprite.width / cols
  val cellH = sprite.height / rows
  val safeRow = pose.row.coerceIn(0, rows - 1)

  val frameCol: Int
  val flipHorizontal: Boolean
  val drawW = laneColumnWidthPx * 2.38f * uniformScale
  val drawH = drawW * (cellH.toFloat() / cellW.toFloat())
  val walkBob: Float
  val animScaleX: Float
  val animBounceY: Float

  if (isWalking) {
    val frameCount = pose.frames.size.coerceAtLeast(1)
    // 1.0 beatPhase = one full step (Contact -> Pass -> Next Contact), 2.0 beatPhase = full 4-frame cycle
    val cyclePos = beatPhase * 2f
    val idx = (((cyclePos + 0.5f) % frameCount + frameCount) % frameCount).toInt().coerceIn(0, frameCount - 1)
    frameCol = pose.frames[idx].coerceIn(0, cols - 1)
    flipHorizontal = pose.flipPerFrame[idx]

    // Lift gently during Pass frames (cyclePos = 1, 3) and land softly on Contact frames (cyclePos = 0, 2)
    val passLift = (0.5f * (1f - cos(cyclePos * PI))).toFloat()
    walkBob = -passLift * drawH * 0.026f
    animScaleX = 1f
    animBounceY = bounceScaleY * (0.988f + 0.022f * passLift)
  } else {
    // Waiting queue: keep BOTH feet completely planted & stationary, only gentle torso/head breathing bounce
    frameCol = pose.idleCol.coerceIn(0, cols - 1)
    flipHorizontal = pose.idleFlip
    val idleWave = (0.5f + 0.5f * sin(beatPhase * PI * 1.15)).toFloat()
    walkBob = 0f
    animScaleX = 1.008f - 0.016f * idleWave
    animBounceY = bounceScaleY * (0.984f + 0.032f * idleWave)
  }

  // Soft ground shadow directly beneath feet
  drawOval(
    color = Color.Black.copy(alpha = 0.22f * alpha),
    topLeft = Offset(center.x - drawW * 0.24f, center.y + drawH * 0.16f),
    size = Size(drawW * 0.48f, drawH * 0.09f),
  )

  withTransform({
    translate(center.x, center.y + walkBob)
    if (leanDeg != 0f) rotate(degrees = leanDeg, pivot = Offset(0f, drawH * 0.248f))
    scale(
      scaleX = (if (flipHorizontal) -1f else 1f) * animScaleX,
      scaleY = animBounceY,
      pivot = Offset(0f, drawH * 0.248f),
    )
  }) {
    drawImage(
      image = sprite,
      srcOffset = IntOffset(frameCol * cellW, safeRow * cellH),
      srcSize = IntSize(cellW, cellH),
      dstOffset = IntOffset((-drawW * 0.5f).roundToInt(), (-drawH * 0.72f).roundToInt()),
      dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), drawH.roundToInt().coerceAtLeast(1)),
      alpha = alpha,
    )
  }
}

/**
 * Top Zone Overlay: Draws tight 4-column rows of glossy chibi passengers along the
 * Catmull-Rom U-shaped road & oval buffer loop in Y-sorted painter's order.
 */
@Composable
fun ConveyorTrackView(
  passengers: List<ConveyorPassenger>,
  conveyorOffsetPx: Float,
  @Suppress("UNUSED_PARAMETER") remainingCount: Int,
  showLaneDebug: Boolean,
  onViewportSized: (Float, Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val assets = rememberAssetManager()
  val textMeasurer = rememberTextMeasurer()

  BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
    val boxW = maxWidth
    val boxH = maxHeight
    val laneGeom = androidx.compose.runtime.remember(boxW.value, boxH.value) {
      LaneGeometry(boxW.value, boxH.value)
    }

    androidx.compose.runtime.LaunchedEffect(boxW.value, boxH.value) {
      onViewportSized(boxW.value, boxH.value)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
      // Rotating conveyor road belt: animated white dashed centerlines along the oval ring & arms
      val ringLenPx = laneGeom.ringLenDp.dp.toPx()
      if (ringLenPx > 1f) {
        val targetPeriodPx = 22.dp.toPx()
        val periodCount = (ringLenPx / targetPeriodPx).roundToInt().coerceAtLeast(12)
        val exactPeriodPx = ringLenPx / periodCount
        val dashPx = exactPeriodPx * 0.44f
        val gapPx = (exactPeriodPx - dashPx).coerceAtLeast(1f)
        val phasePx = -((conveyorOffsetPx.dp.toPx()) % exactPeriodPx)
        val beltEffect = PathEffect.dashPathEffect(floatArrayOf(dashPx, gapPx), phasePx)
        val beltStroke =
          Stroke(
            width = 3.2.dp.toPx(),
            cap = StrokeCap.Round,
            join = StrokeJoin.Round,
            pathEffect = beltEffect,
          )
        val beltColor = Color(0xFFE4ECF7).copy(alpha = 0.82f)

        val dpToPx = 1.dp.toPx()
        val ringPts = laneGeom.roadCenterlinePoints(ConveyorSegment.RING, steps = 144)
        val ringPath = Path().apply {
          ringPts.forEachIndexed { idx, pair ->
            val px = pair.first * dpToPx
            val py = pair.second * dpToPx
            if (idx == 0) moveTo(px, py) else lineTo(px, py)
          }
          close()
        }
        drawPath(path = ringPath, color = beltColor, style = beltStroke)

        for (armSeg in arrayOf(ConveyorSegment.LEFT_ARM, ConveyorSegment.RIGHT_ARM)) {
          val armPts = laneGeom.roadCenterlinePoints(armSeg, steps = 42, endFraction = 0.82f)
          val armPath = Path().apply {
            armPts.forEachIndexed { idx, pair ->
              val px = pair.first * dpToPx
              val py = pair.second * dpToPx
              if (idx == 0) moveTo(px, py) else lineTo(px, py)
            }
          }
          drawPath(path = armPath, color = beltColor, style = beltStroke)
        }
      }

      if (passengers.isNotEmpty()) {
        val maxVisibleYDp = boxH.value + 32f
        val minVisibleYDp = -32f
        val colWidthPx = laneGeom.colStepDp.dp.toPx()

        // Sort strictly by screen Y ascending (painter's order) so closer rows naturally overlap rows behind
        data class RenderItem(
          val passenger: ConveyorPassenger,
          val sample: LaneSample,
          val xDp: Float,
          val yDp: Float,
          val scale: Float,
        )

        val renderList = ArrayList<RenderItem>(passengers.size)
        for (p in passengers) {
          val (sample, posDp) = laneGeom.passengerCoords(p.segment, p.distDp, p.colInRow)
          if (posDp.second < minVisibleYDp || posDp.second > maxVisibleYDp) continue
          val scale = laneGeom.passengerScale(p.segment, p.distDp)
          renderList.add(RenderItem(p, sample, posDp.first, posDp.second, scale))
        }
        renderList.sortWith(
          compareBy<RenderItem> { it.yDp }
            .thenBy { it.passenger.colInRow }
            .thenBy { it.passenger.id }
        )

        for (item in renderList) {
          val p = item.passenger
          val sprite = assets.getPassengerSprite(p.color) ?: continue
          val (sCols, sRows) = assets.getPassengerGrid(p.color)
          val pose = directionPoseForTangent(item.sample.tx, item.sample.ty)
          val beat = (conveyorOffsetPx * 0.08f) + (p.rowGroupId % 2) * 0.5f + (p.colInRow % 2) * 0.22f

          drawPassengerAsset(
            sprite = sprite,
            center = Offset(item.xDp.dp.toPx(), item.yDp.dp.toPx()),
            laneColumnWidthPx = colWidthPx,
            color = p.color,
            textMeasurer = textMeasurer,
            uniformScale = item.scale,
            pose = pose,
            beatPhase = beat,
            sheetCols = sCols,
            sheetRows = sRows,
            isWalking = p.isWalking,
          )
        }
      }

      if (showLaneDebug) {
        val debugPaths = laneGeom.debugPaths()
        val columnDash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()), 0f)
        debugPaths.forEachIndexed { idx, pts ->
          val kind = idx % 7
          val (strokeColor, strokeW, effect) = when (kind) {
            0, 6 -> Triple(Color.Cyan.copy(alpha = 0.92f), 2.dp.toPx(), null)
            3 -> Triple(Color.Yellow, 2.dp.toPx(), null)
            else -> Triple(Color(0xFF39FF14).copy(alpha = 0.85f), 1.2.dp.toPx(), columnDash)
          }
          val path = Path().apply {
            pts.forEachIndexed { pIdx, pair ->
              val px = pair.first.dp.toPx()
              val py = pair.second.dp.toPx()
              if (pIdx == 0) moveTo(px, py) else lineTo(px, py)
            }
          }
          drawPath(path = path, color = strokeColor, style = Stroke(width = strokeW, pathEffect = effect))
        }
      }
    }
  }
}

@Composable
fun SideBushesOverlay(modifier: Modifier = Modifier) {
  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    val bushRadiusBase = (w * 0.036f).coerceIn(10.dp.toPx(), 18.dp.toPx())

    val leftClusters = listOf(0.365f, 0.485f, 0.530f, 0.575f, 0.620f, 0.665f, 0.710f, 0.755f, 0.800f)
    for ((idx, yf) in leftClusters.withIndex()) {
      val r = bushRadiusBase * (if (idx % 2 == 0) 1.0f else 0.88f)
      drawBushCluster(center = Offset(w * 0.012f, h * yf), radius = r)
    }

    val rightClusters = listOf(0.365f, 0.515f, 0.560f, 0.605f, 0.650f, 0.695f, 0.740f, 0.785f)
    for ((idx, yf) in rightClusters.withIndex()) {
      val r = bushRadiusBase * (if (idx % 2 == 0) 0.96f else 1.04f)
      drawBushCluster(center = Offset(w * 0.988f, h * yf), radius = r)
    }
  }
}

private fun DrawScope.drawBushCluster(center: Offset, radius: Float) {
  val shadeGreen = Color(0xFF2B7A14)
  val midGreen = Color(0xFF4EC722)
  val highlightGreen = Color(0xFF8EF047)

  drawCircle(color = Color.Black.copy(alpha = 0.18f), radius = radius * 1.05f, center = Offset(center.x, center.y + radius * 0.22f))
  drawCircle(color = shadeGreen, radius = radius, center = center)
  drawCircle(color = midGreen, radius = radius * 0.86f, center = Offset(center.x, center.y - radius * 0.06f))
  drawCircle(color = highlightGreen, radius = radius * 0.52f, center = Offset(center.x - radius * 0.16f, center.y - radius * 0.24f))
}

/**
 * Aligned Waiting Bays Overlay: 8 evenly spaced slots (~40x76 px proportions).
 * Parked buses use the real top_front sprite ONLY, centered straight UP (rotation = 0),
 * ~90% of slot height, completely inside the slot with a teal/blue countdown pill below.
 */
@Composable
fun WaitingSlotsRow(
  slots: List<WaitingSlotState>,
  vehicles: List<VehicleEntity>,
  jumpingPassengers: List<JumpingPassenger> = emptyList(),
  onSlotTap: (Int) -> Unit,
  modifier: Modifier = Modifier,
) {
  val assets = rememberAssetManager()
  val textMeasurer = rememberTextMeasurer()

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val w = maxWidth
    val h = maxHeight
    val bayWidth = (w * 0.086f).coerceAtLeast(32.dp)
    val bayHeight = (h * 0.066f).coerceAtLeast(52.dp)
    val closeFraction = 0.20f / 0.90f

    for (slot in slots) {
      val occupant = slot.occupantVehicleId?.let { id -> vehicles.firstOrNull { it.id == id } }
      val cx = w * slotCenterXNorm(slot.index)
      val cy = h * SLOT_CENTER_Y_NORM
      val showOccupant =
        occupant != null &&
          (occupant.stage == VehicleStage.PARKED ||
            (occupant.stage == VehicleStage.DEPARTING_FULL && occupant.animProgress < closeFraction))

      Box(
        modifier =
          Modifier
            .offset(x = cx - bayWidth * 0.5f, y = cy - bayHeight * 0.5f)
            .width(bayWidth)
            .height(bayHeight)
            .clickable(enabled = !slot.unlocked) { onSlotTap(slot.index) }
            .testTag("waiting_slot_${slot.index}"),
        contentAlignment = Alignment.Center,
      ) {
        if (slot.kind == SlotKind.LOCKED && slot.unlocked) {
          Image(
            painter = painterResource(id = R.drawable.img_slot_normal),
            contentDescription = "Unlocked Waiting Slot",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
        } else if (slot.kind == SlotKind.LOCKED && !slot.unlocked && slot.index in 1..4) {
          // Visual lock + badge when higher levels start with fewer free bays
          Canvas(modifier = Modifier.fillMaxSize()) {
            rotate(SLOT_TILT_DEG, pivot = Offset(size.width * 0.5f, size.height * 0.5f)) {
              drawRoundRect(
                color = Color(0xCC242D46),
                topLeft = Offset(size.width * 0.08f, size.height * 0.08f),
                size = Size(size.width * 0.84f, size.height * 0.84f),
                cornerRadius = CornerRadius(6.dp.toPx()),
              )
              val plusR = min(size.width, size.height) * 0.19f
              val c = Offset(size.width * 0.5f, size.height * 0.5f)
              drawLine(
                color = Color(0xFF4ADE80),
                start = Offset(c.x - plusR, c.y),
                end = Offset(c.x + plusR, c.y),
                strokeWidth = 3.2.dp.toPx(),
              )
              drawLine(
                color = Color(0xFF4ADE80),
                start = Offset(c.x, c.y - plusR),
                end = Offset(c.x, c.y + plusR),
                strokeWidth = 3.2.dp.toPx(),
              )
            }
          }
        } else if (slot.kind == SlotKind.VIP && slot.unlocked && occupant == null) {
          Box(
            modifier =
              Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(PlayGreen)
                .border(1.2.dp, Color.White, RoundedCornerShape(6.dp))
                .padding(horizontal = 4.dp, vertical = 1.dp)
          ) {
            Text(
              text = "OPEN",
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 7.sp, fontFamily = FredokaFontFamily),
              color = Color.White,
            )
          }
        }

        if (showOccupant && occupant != null) {
          val arriveScale = occupant.squashFactor
          val topUpSprite = assets.getWaitingBayBusSprite(occupant.color)
          val passengerSprite = assets.getPassengerSprite(occupant.color)
          val inFlightCount = jumpingPassengers.count { it.targetVehicleId == occupant.id }

          Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width * 0.5f, size.height * 0.5f)
            if (topUpSprite == null) {
              drawMissingAssetError(
                textMeasurer = textMeasurer,
                message = "Missing\n${occupant.color.assetKey}",
                center = center,
                fontSizeSp = 7f,
              )
            } else {
              val maxW = (size.width * 0.90f * arriveScale).coerceAtLeast(1f)
              val maxH = (size.height * 0.92f * arriveScale).coerceAtLeast(1f)
              val scale = min(maxW / topUpSprite.width.toFloat(), maxH / topUpSprite.height.toFloat())
              val drawW = (topUpSprite.width * scale).coerceAtLeast(1f)
              val drawH = (topUpSprite.height * scale).coerceAtLeast(1f)

              withTransform({
                translate(center.x, center.y)
                rotate(SLOT_TILT_DEG, pivot = Offset.Zero)
              }) {
                drawOpenTopBusBody(
                  topSprite = topUpSprite,
                  passengerSprite = passengerSprite,
                  vehicle = occupant,
                  drawW = drawW,
                  drawH = drawH,
                  inFlightTowardRoof = inFlightCount,
                )
              }
            }
          }
        }
      }

      if (showOccupant && occupant != null) {
        val remaining = (occupant.capacity - occupant.boardedCount).coerceAtLeast(0)
        val progress = (occupant.boardedCount.toFloat() / occupant.capacity.coerceAtLeast(1)).coerceIn(0f, 1f)

        Column(
          modifier =
            Modifier
              .offset(x = cx - bayWidth * 0.5f, y = cy + bayHeight * 0.48f)
              .width(bayWidth),
          horizontalAlignment = Alignment.CenterHorizontally,
        ) {
          Box(
            modifier =
              Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(
                  Brush.verticalGradient(
                    colors = listOf(Color(0xFF1EC4D6), Color(0xFF0B829D)),
                  )
                )
                .border(1.4.dp, Color(0xFF064859), RoundedCornerShape(8.dp))
                .padding(horizontal = 7.dp, vertical = 1.5.dp),
          ) {
            Text(
              text = remaining.toString(),
              style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp, fontFamily = FredokaFontFamily),
              color = Color.White,
              maxLines = 1,
            )
          }
          Spacer(Modifier.height(2.dp))
          Box(
            modifier =
              Modifier
                .fillMaxWidth(0.76f)
                .height(3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(Color(0xFF283454))
          ) {
            Box(
              modifier =
                Modifier
                  .fillMaxWidth(progress)
                  .fillMaxHeight()
                  .background(PlayGreen)
            )
          }
        }
      }
    }
  }
}

/**
 * Middle Zone: Isometric diamond parking lot (~37 deg), sorted in back-to-front painter's order,
 * drawing crisp un-rotated upright clay ISO_1..4 bus sprites with smooth tap/shake/tutorial feedback.
 */
@Composable
fun ParkingLotCanvas(
  gridCols: Int,
  gridRows: Int,
  vehicles: List<VehicleEntity>,
  tunnels: List<TunnelState>,
  tutorialVehicleId: Int?,
  onLotTap: (Float, Float) -> Unit,
  modifier: Modifier = Modifier,
) {
  val assets = rememberAssetManager()
  val textMeasurer = rememberTextMeasurer()
  val infiniteTransition = rememberInfiniteTransition(label = "tutorial_pulse")
  val handBob by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(650, easing = LinearEasing), RepeatMode.Reverse),
      label = "hand_bob",
    )

  BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    Canvas(
      modifier =
        Modifier
          .fillMaxSize()
          .testTag("parking_lot_canvas")
          .pointerInput(gridCols, gridRows) {
            detectTapGestures { offset ->
              val m = computeLotMetrics(size.width.toFloat(), size.height.toFloat(), gridCols, gridRows, density)
              val (gx, gy) = m.screenToIso(offset.x, offset.y)
              onLotTap(gx, gy)
            }
          },
    ) {
      val m = computeLotMetrics(size.width, size.height, gridCols, gridRows, density)
      val cellPx = m.cellPx

      for (tunnel in tunnels) {
        val tGx = if (tunnel.side == TunnelSide.LEFT) -0.45f else gridCols + 0.45f
        val tGy = tunnel.row + 0.5f
        val tPos = m.isoToScreen(tGx, tGy)
        drawIsoTunnelPortal(
          tunnel = tunnel,
          center = tPos,
          cellSize = cellPx,
          textMeasurer = textMeasurer,
        )
      }

      val lotVehicles =
        vehicles
          .filter {
            it.stage == VehicleStage.INTRO_SLIDING ||
              it.stage == VehicleStage.IN_LOT ||
              it.stage == VehicleStage.SPAWNING_FROM_TUNNEL
          }
          .sortedWith(
            compareBy<VehicleEntity> { -it.x + it.y }
              .thenBy { it.x + it.y }
              .thenBy { it.id }
          )

      for (v in lotVehicles) {
        val shakeJitter =
          if (v.shakePhase > 0f) sin(v.shakePhase * 28.0).toFloat() * 3.dp.toPx() * v.shakePhase else 0f
        val (sdx, sdy) = isoScreenDirection(v.direction)
        val bx = v.x + v.direction.dx * v.bumpOffset
        val by = v.y + v.direction.dy * v.bumpOffset
        val basePos = m.isoToScreen(bx, by)
        val cx = basePos.x - sdy * shakeJitter
        val cy = basePos.y + sdx * shakeJitter

        drawIsoLotBusSprite(
          assets = assets,
          textMeasurer = textMeasurer,
          v = v,
          center = Offset(cx, cy),
          cellPx = cellPx,
        )

        if (tutorialVehicleId == v.id && (v.stage == VehicleStage.IN_LOT || v.stage == VehicleStage.INTRO_SLIDING)) {
          drawCircle(
            color = GoldCoin.copy(alpha = 0.78f - 0.42f * handBob),
            radius = cellPx * (0.95f + 0.16f * handBob),
            center = Offset(cx, cy),
            style = Stroke(width = 3.5.dp.toPx()),
          )
          drawTutorialHand(center = Offset(cx + 10.dp.toPx(), cy + 12.dp.toPx() - 7.dp.toPx() * handBob))
        }
      }
    }
  }
}

private fun DrawScope.drawIsoTunnelPortal(
  tunnel: TunnelState,
  center: Offset,
  cellSize: Float,
  textMeasurer: TextMeasurer,
) {
  val portalW = 24.dp.toPx()
  val portalH = cellSize * 0.92f
  val px = center.x - portalW * 0.5f
  val py = center.y - portalH * 0.5f
  val closed = tunnel.queue.isEmpty()

  drawRoundRect(
    color = if (closed) Color(0xFF7D8899) else Color(0xFFB35334),
    topLeft = Offset(px, py),
    size = Size(portalW, portalH),
    cornerRadius = CornerRadius(8.dp.toPx()),
  )
  drawRoundRect(
    color = OutlineInk,
    topLeft = Offset(px, py),
    size = Size(portalW, portalH),
    cornerRadius = CornerRadius(8.dp.toPx()),
    style = Stroke(width = 2.2.dp.toPx()),
  )
  drawRoundRect(
    color = if (closed) Color(0xFF5D6675) else Color(0xFF1A202C),
    topLeft = Offset(px + 4.dp.toPx(), py + 5.dp.toPx()),
    size = Size(portalW - 8.dp.toPx(), portalH - 10.dp.toPx()),
    cornerRadius = CornerRadius(5.dp.toPx()),
  )

  val badgeCenter = Offset(px + portalW * 0.5f, py - 2.dp.toPx())
  val badgeColor = if (closed) Color(0xFF9AA5B4) else AlertRed
  drawCircle(color = badgeColor, radius = 10.dp.toPx(), center = badgeCenter)
  drawCircle(color = Color.White, radius = 10.dp.toPx(), center = badgeCenter, style = Stroke(width = 2.dp.toPx()))
  val countLayout =
    textMeasurer.measure(
      text = tunnel.queue.size.toString(),
      style =
        TextStyle(
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          color = Color.White,
        ),
    )
  drawText(
    textLayoutResult = countLayout,
    topLeft = Offset(badgeCenter.x - countLayout.size.width / 2f, badgeCenter.y - countLayout.size.height / 2f),
  )
}

private fun DrawScope.drawIsoLotBusSprite(
  assets: AssetManager,
  textMeasurer: TextMeasurer,
  v: VehicleEntity,
  center: Offset,
  cellPx: Float,
) {
  val view = isoViewForDirection(v.direction)
  val sprite = assets.getBestIsoBusSprite(v.color, view)
  if (sprite == null) {
    drawMissingAssetError(textMeasurer, "Missing bus_${v.color.assetKey}", center, fontSizeSp = 8f)
    return
  }

  val maxW = cellPx * 1.82f * v.squashFactor
  val maxH = cellPx * 1.62f * v.squashFactor
  val scaleFactor = min(maxW / sprite.width.toFloat(), maxH / sprite.height.toFloat())
  val drawW = sprite.width * scaleFactor
  val drawH = sprite.height * scaleFactor

  // Soft isometric ground shadow beneath bus wheels
  drawOval(
    color = Color.Black.copy(alpha = 0.22f),
    topLeft = Offset(center.x - drawW * 0.44f, center.y - drawH * 0.16f + drawH * 0.18f),
    size = Size(drawW * 0.88f, drawH * 0.52f),
  )

  drawImage(
    image = sprite,
    dstOffset = IntOffset((center.x - drawW * 0.5f).roundToInt(), (center.y - drawH * 0.5f).roundToInt()),
    dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), drawH.roundToInt().coerceAtLeast(1)),
  )

  if (v.flashRedTimer > 0f) {
    drawOval(
      color = AlertRed.copy(alpha = 0.44f),
      topLeft = Offset(center.x - drawW * 0.42f, center.y - drawH * 0.36f),
      size = Size(drawW * 0.84f, drawH * 0.72f),
    )
    drawOval(
      color = Color.White.copy(alpha = 0.90f),
      topLeft = Offset(center.x - drawW * 0.42f, center.y - drawH * 0.36f),
      size = Size(drawW * 0.84f, drawH * 0.72f),
      style = Stroke(width = 2.dp.toPx()),
    )
  }
}

private fun DrawScope.drawTunnelPortal(
  tunnel: TunnelState,
  originX: Float,
  originY: Float,
  boardW: Float,
  cellSize: Float,
  textMeasurer: TextMeasurer,
) {
  val isLeft = tunnel.side == TunnelSide.LEFT
  val portalW = 24.dp.toPx()
  val portalH = cellSize * 0.86f
  val px = if (isLeft) originX - portalW + 4.dp.toPx() else originX + boardW - 4.dp.toPx()
  val py = originY + (tunnel.row + 0.5f) * cellSize - portalH * 0.5f
  val closed = tunnel.queue.isEmpty()

  drawRoundRect(
    color = if (closed) Color(0xFF7D8899) else Color(0xFFB35334),
    topLeft = Offset(px, py),
    size = Size(portalW, portalH),
    cornerRadius = CornerRadius(8.dp.toPx()),
  )
  drawRoundRect(
    color = OutlineInk,
    topLeft = Offset(px, py),
    size = Size(portalW, portalH),
    cornerRadius = CornerRadius(8.dp.toPx()),
    style = Stroke(width = 2.2.dp.toPx()),
  )
  drawRoundRect(
    color = if (closed) Color(0xFF5D6675) else Color(0xFF1A202C),
    topLeft = Offset(px + 4.dp.toPx(), py + 5.dp.toPx()),
    size = Size(portalW - 8.dp.toPx(), portalH - 10.dp.toPx()),
    cornerRadius = CornerRadius(5.dp.toPx()),
  )

  val badgeCenter = Offset(px + portalW * 0.5f, py - 2.dp.toPx())
  val badgeColor = if (closed) Color(0xFF9AA5B4) else AlertRed
  drawCircle(color = badgeColor, radius = 10.dp.toPx(), center = badgeCenter)
  drawCircle(color = Color.White, radius = 10.dp.toPx(), center = badgeCenter, style = Stroke(width = 2.dp.toPx()))
  val countLayout =
    textMeasurer.measure(
      text = tunnel.queue.size.toString(),
      style =
        TextStyle(
          fontFamily = FredokaFontFamily,
          fontWeight = FontWeight.Bold,
          fontSize = 11.sp,
          color = Color.White,
        ),
    )
  drawText(
    textLayoutResult = countLayout,
    topLeft = Offset(badgeCenter.x - countLayout.size.width / 2f, badgeCenter.y - countLayout.size.height / 2f),
  )
}

private const val SEAT_COLS = 4
private const val SEAT_ROWS = 10

/**
 * Draws the open-top bus body centered at (0,0) inside the current Canvas transform:
 * - Opening grows from center in 0.25s up to 80% of bus width x 72% of roof length (leaving front windshield crisp).
 * - Rim in the bus's LIGHT shade, interior filled with the bus's MAIN color (never black/navy).
 * - 10 rows x 4 seats = 40 exact seats in the bus's DARK shade at 45% opacity.
 * - Boarded passenger heads fill row-by-row, left-to-right with matching 3D glossy chibi heads.
 */
private fun DrawScope.drawOpenTopBusBody(
  topSprite: ImageBitmap,
  @Suppress("UNUSED_PARAMETER") passengerSprite: ImageBitmap?,
  vehicle: VehicleEntity,
  drawW: Float,
  drawH: Float,
  inFlightTowardRoof: Int = 0,
) {
  drawOval(
    color = Color.Black.copy(alpha = 0.26f),
    topLeft = Offset(-drawW * 0.46f + 1.5.dp.toPx(), -drawH * 0.44f + 2.5.dp.toPx()),
    size = Size(drawW * 0.92f, drawH * 0.92f),
  )
  drawImage(
    image = topSprite,
    dstOffset = IntOffset((-drawW * 0.5f).roundToInt(), (-drawH * 0.5f).roundToInt()),
    dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), drawH.roundToInt().coerceAtLeast(1)),
  )

  val open = vehicle.roofOpenProgress.coerceIn(0f, 1f)
  if (open <= 0.01f) return

  val fullOpenW = drawW * 0.70f
  val fullOpenH = drawH * 0.72f
  val openCenterY = drawH * 0.02f
  val fullTop = openCenterY - fullOpenH * 0.5f
  val fullLeft = -fullOpenW * 0.5f

  val curW = (fullOpenW * open).coerceAtLeast(2f)
  val curH = (fullOpenH * open).coerceAtLeast(2f)
  val curLeft = -curW * 0.5f
  val curTop = openCenterY - curH * 0.5f
  val cornerRadius = CornerRadius(min(curW, fullOpenW) * 0.14f)

  // Dark charcoal cabin interior matching the open-roof bus design
  drawRoundRect(
    brush =
      Brush.verticalGradient(
        colors = listOf(Color(0xFF1D1E22), Color(0xFF28292E), Color(0xFF1F2024)),
        startY = curTop,
        endY = curTop + curH,
      ),
    topLeft = Offset(curLeft, curTop),
    size = Size(curW, curH),
    cornerRadius = cornerRadius,
  )

  val colPitch = fullOpenW / (SEAT_COLS + 0.65f)
  val rowPitch = fullOpenH / (SEAT_ROWS + 0.7f)
  val unit = min(colPitch, rowPitch)

  if (open > 0.12f) {
    val seatBackColor = Color(0xFF56595F)
    val seatCushionColor = Color(0xFF7B7E85)
    val seatW = colPitch * 0.72f
    val seatH = rowPitch * 0.66f
    val seatCorner = CornerRadius(seatW * 0.32f)
    val headR = (min(colPitch, rowPitch) * 0.42f).coerceAtLeast(1.4.dp.toPx())
    val outlineStrokeW = (unit * 0.14f).coerceAtLeast(0.6.dp.toPx())
    val shineR = (headR * 0.32f).coerceAtLeast(0.75f)

    val remainingToLand = (vehicle.capacity - vehicle.boardedCount).coerceAtLeast(0)
    val effectiveRemaining = (vehicle.capacity - vehicle.reservedCount).coerceAtLeast(0)
    val visibleHeads =
      if (inFlightTowardRoof > 0) {
        (vehicle.capacity - effectiveRemaining - inFlightTowardRoof).coerceIn(0, vehicle.capacity)
      } else {
        (vehicle.capacity - remainingToLand).coerceIn(0, vehicle.capacity)
      }

    clipRect(
      left = curLeft + 1f,
      top = curTop + 1f,
      right = curLeft + curW - 1f,
      bottom = curTop + curH - 1f,
    ) {
      val gridStartX = fullLeft + (fullOpenW - (SEAT_COLS - 1) * colPitch) * 0.5f
      val gridStartY = fullTop + (fullOpenH - (SEAT_ROWS - 1) * rowPitch) * 0.5f
      var seatIdx = 0
      for (r in 0 until SEAT_ROWS) {
        val sy = gridStartY + r * rowPitch
        for (c in 0 until SEAT_COLS) {
          val sx = gridStartX + c * colPitch
          drawRoundRect(
            color = seatBackColor,
            topLeft = Offset(sx - seatW * 0.5f, sy - seatH * 0.5f),
            size = Size(seatW, seatH),
            cornerRadius = seatCorner,
          )
          drawRoundRect(
            color = seatCushionColor,
            topLeft = Offset(sx - seatW * 0.42f, sy - seatH * 0.18f),
            size = Size(seatW * 0.84f, seatH * 0.56f),
            cornerRadius = CornerRadius(seatW * 0.24f),
          )
          if (seatIdx < visibleHeads) {
            drawCircle(
              color = vehicle.color.roofDark,
              radius = headR + outlineStrokeW,
              center = Offset(sx, sy + headR * 0.14f),
            )
            drawCircle(
              color = vehicle.color.roofMain,
              radius = headR,
              center = Offset(sx, sy + headR * 0.04f),
            )
            drawCircle(
              color = vehicle.color.roofLight,
              radius = headR * 0.80f,
              center = Offset(sx - headR * 0.10f, sy - headR * 0.08f),
            )
            drawCircle(
              color = Color.White.copy(alpha = 0.85f),
              radius = shineR,
              center = Offset(sx - headR * 0.28f, sy - headR * 0.28f),
            )
          }
          seatIdx++
        }
      }
    }
  }

  // Molded interior bezel matching the bus outer color & dark inner edge
  drawRoundRect(
    color = Color(0xFF15161A),
    topLeft = Offset(curLeft, curTop),
    size = Size(curW, curH),
    cornerRadius = cornerRadius,
    style = Stroke(width = (unit * 0.22f).coerceAtLeast(1.1.dp.toPx())),
  )
  drawRoundRect(
    color = vehicle.color.roofMain,
    topLeft = Offset(curLeft - 0.8.dp.toPx(), curTop - 0.8.dp.toPx()),
    size = Size(curW + 1.6.dp.toPx(), curH + 1.6.dp.toPx()),
    cornerRadius = cornerRadius,
    style = Stroke(width = (unit * 0.26f).coerceAtLeast(1.3.dp.toPx())),
  )
}

/**
 * Draws a 1x2 (vertical) or 2x1 (horizontal) straight lattice bus with 4-6px neighbor gaps.
 */
private fun DrawScope.drawLotVehicleSprite(
  assets: AssetManager,
  textMeasurer: TextMeasurer,
  v: VehicleEntity,
  center: Offset,
  stridePx: Float,
  gapPx: Float,
  angleOverrideDeg: Float? = null,
) {
  val isStaticHorizontal =
    angleOverrideDeg == null && (v.direction == Direction.LEFT || v.direction == Direction.RIGHT)
  if (isStaticHorizontal) {
    val horizSprite = assets.getBusSprite(v.color, BusView.HORIZ)
    if (horizSprite != null) {
      val maxW = (stridePx * 2f - gapPx).coerceAtLeast(1f)
      val maxH = (stridePx - gapPx).coerceAtLeast(1f)
      val baseScale = min(maxW / horizSprite.width.toFloat(), maxH / horizSprite.height.toFloat())
      val uniformScale = baseScale * v.squashFactor
      val drawW = horizSprite.width * uniformScale
      val drawH = horizSprite.height * uniformScale
      val flipRight = v.direction == Direction.RIGHT

      withTransform({
        translate(center.x, center.y)
        if (flipRight) scale(scaleX = -1f, scaleY = 1f, pivot = Offset.Zero)
      }) {
        drawOval(
          color = Color.Black.copy(alpha = 0.22f),
          topLeft = Offset(-drawW * 0.46f, -drawH * 0.36f + 2.dp.toPx()),
          size = Size(drawW * 0.92f, drawH * 0.84f),
        )
        drawImage(
          image = horizSprite,
          dstOffset = IntOffset((-drawW * 0.5f).roundToInt(), (-drawH * 0.5f).roundToInt()),
          dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), drawH.roundToInt().coerceAtLeast(1)),
        )
        if (v.flashRedTimer > 0f) {
          drawRoundRect(
            color = AlertRed.copy(alpha = 0.50f),
            topLeft = Offset(-drawW * 0.48f, -drawH * 0.46f),
            size = Size(drawW * 0.96f, drawH * 0.92f),
            cornerRadius = CornerRadius(drawH * 0.25f),
          )
        }
      }
      return
    }
  }

  val isMovingDown =
    if (angleOverrideDeg != null) {
      val normAngle = ((angleOverrideDeg % 360f) + 360f) % 360f
      normAngle in 45f..135f
    } else {
      v.direction == Direction.DOWN
    }
  val topView = if (isMovingDown) BusView.TOP_FRONT else BusView.TOP_REAR
  val topSprite = assets.getBusSprite(v.color, topView) ?: assets.getBusSprite(v.color, BusView.TOP_FRONT)
  if (topSprite == null) {
    drawMissingAssetError(
      textMeasurer = textMeasurer,
      message = "Missing\nbus_${v.color.assetKey}",
      center = center,
      fontSizeSp = 8f,
    )
    return
  }

  val baseAngleDeg = angleOverrideDeg ?: v.direction.angleDeg
  val rotationDeg =
    if (angleOverrideDeg == null && (v.direction == Direction.UP || v.direction == Direction.DOWN)) {
      0f
    } else if (isMovingDown) {
      baseAngleDeg - 90f
    } else {
      baseAngleDeg + 90f
    }
  val maxShortPx = (stridePx - gapPx).coerceAtLeast(1f)
  val maxLongPx = (stridePx * 2f - gapPx).coerceAtLeast(1f)
  val drawW = maxShortPx * 0.96f * v.squashFactor
  val drawH = min(maxLongPx * 0.94f, maxShortPx * 1.78f) * v.squashFactor
  val flashRed = v.flashRedTimer > 0f
  val passengerSprite = assets.getPassengerSprite(v.color)

  withTransform({
    translate(center.x, center.y)
    rotate(rotationDeg, pivot = Offset.Zero)
  }) {
    drawOpenTopBusBody(
      topSprite = topSprite,
      passengerSprite = passengerSprite,
      vehicle = v,
      drawW = drawW,
      drawH = drawH,
    )
    if (flashRed) {
      drawRoundRect(
        color = AlertRed.copy(alpha = 0.50f),
        topLeft = Offset(-drawW * 0.48f, -drawH * 0.48f),
        size = Size(drawW * 0.96f, drawH * 0.96f),
        cornerRadius = CornerRadius(drawW * 0.22f),
      )
      drawRoundRect(
        color = Color.White,
        topLeft = Offset(-drawW * 0.48f, -drawH * 0.48f),
        size = Size(drawW * 0.96f, drawH * 0.96f),
        cornerRadius = CornerRadius(drawW * 0.22f),
        style = Stroke(width = 2.dp.toPx()),
      )
    }
  }
}

/**
 * Draws an ImageBitmap inside [maxW] x [maxH] with a SINGLE uniform scale factor (never stretched).
 * Shows red error text if sprite is null (never draws rectangle placeholders).
 */
private fun DrawScope.drawSpriteUniform(
  sprite: ImageBitmap?,
  missingLabel: String,
  textMeasurer: TextMeasurer,
  center: Offset,
  maxW: Float,
  maxH: Float,
  flipHorizontal: Boolean = false,
  alpha: Float = 1f,
) {
  if (sprite == null) {
    drawMissingAssetError(textMeasurer, missingLabel, center, fontSizeSp = 8f)
    return
  }

  val scaleFactor = min(maxW / sprite.width.toFloat(), maxH / sprite.height.toFloat())
  val drawW = sprite.width * scaleFactor
  val drawH = sprite.height * scaleFactor

  withTransform({
    translate(center.x, center.y)
    if (flipHorizontal) {
      scale(scaleX = -1f, scaleY = 1f, pivot = Offset.Zero)
    }
  }) {
    drawImage(
      image = sprite,
      dstOffset = IntOffset((-drawW * 0.5f).roundToInt(), (-drawH * 0.5f).roundToInt()),
      dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), drawH.roundToInt().coerceAtLeast(1)),
      alpha = alpha,
    )
  }
}

private fun DrawScope.drawStripFrame(
  sheet: ImageBitmap,
  frameCount: Int,
  frameIndex: Int,
  center: Offset,
  maxW: Float,
  maxH: Float,
  flipHorizontal: Boolean = false,
  alpha: Float = 1f,
) {
  val safeCount = frameCount.coerceAtLeast(1)
  val frameW = (sheet.width / safeCount).coerceAtLeast(1)
  val frameH = sheet.height.coerceAtLeast(1)
  val clampedIdx = frameIndex.coerceIn(0, safeCount - 1)
  val scaleFactor = min(maxW / frameW.toFloat(), maxH / frameH.toFloat())
  val drawW = (frameW * scaleFactor).roundToInt().coerceAtLeast(1)
  val drawH = (frameH * scaleFactor).roundToInt().coerceAtLeast(1)

  withTransform({
    translate(center.x, center.y)
    if (flipHorizontal) {
      scale(scaleX = -1f, scaleY = 1f, pivot = Offset.Zero)
    }
  }) {
    drawImage(
      image = sheet,
      srcOffset = IntOffset(clampedIdx * frameW, 0),
      srcSize = IntSize(frameW, frameH),
      dstOffset = IntOffset(-drawW / 2, -drawH / 2),
      dstSize = IntSize(drawW, drawH),
      alpha = alpha,
    )
  }
}

private fun DrawScope.drawTutorialHand(center: Offset) {
  drawCircle(color = OutlineInk, radius = 13.dp.toPx(), center = center)
  drawCircle(color = Color.White, radius = 10.5.dp.toPx(), center = center)
  drawRoundRect(
    color = OutlineInk,
    topLeft = Offset(center.x - 5.dp.toPx(), center.y - 22.dp.toPx()),
    size = Size(10.dp.toPx(), 18.dp.toPx()),
    cornerRadius = CornerRadius(5.dp.toPx()),
  )
  drawRoundRect(
    color = Color.White,
    topLeft = Offset(center.x - 3.dp.toPx(), center.y - 20.dp.toPx()),
    size = Size(6.dp.toPx(), 15.dp.toPx()),
    cornerRadius = CornerRadius(3.dp.toPx()),
  )
}

/**
 * Full-screen playfield overlay:
 * - DRIVING_TO_SLOT: ONE continuous smooth path of horizontal/vertical segments + rounded corners (~36px),
 *   arc-length ease-in-out motion (~380 px/s peak), top_front sprite ONLY rotated by path tangent,
 *   and 0.4s fading white circular dots along the traveled trail.
 * - DEPARTING_FULL: straight DOWN out of slot -> rounded right turn -> off right side through open border gap
 *   with horizontally flipped bus_<color>_side.png (speed lines trailing on the left).
 * - Jumping passengers (man_<color>.png uniform scale) arcing into bus roof in 0.30s.
 */
@Composable
fun PlayfieldEffectsOverlay(
  vehicles: List<VehicleEntity>,
  gridCols: Int,
  gridRows: Int,
  jumpingPassengers: List<JumpingPassenger>,
  particles: List<Particle>,
  floatingTexts: List<FloatingText>,
  totalSlots: Int,
  modifier: Modifier = Modifier,
) {
  val assets = rememberAssetManager()
  val textMeasurer = rememberTextMeasurer()

  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    val lotTop = h * 0.452f
    val lotH = h * 0.455f
    val m = computeLotMetrics(w, lotH, gridCols, gridRows, density)
    val cellPx = m.cellPx
    val gapPx = m.gapPx

    val entryLaneY = h * 0.448f
    val rightGapY = h * 0.437f
    val leftRoadX = w * 0.088f
    val rightRoadX = w * 0.912f
    val botRoadY = h * 0.895f
    val cornerRadiusPx = (18f / 360f) * w

    val bayW = maxOf(w * 0.086f, 32.dp.toPx())
    val bayH = maxOf(h * 0.066f, 52.dp.toPx())
    val closeFraction = 0.20f / 0.90f

    for (v in vehicles) {
      if (v.stage == VehicleStage.DEPARTING_FULL && v.slotIndex >= 0) {
        val rawDepartP = v.animProgress.coerceIn(0f, 1f)
        if (rawDepartP < closeFraction) continue
        val p = ((rawDepartP - closeFraction) / (1f - closeFraction)).coerceIn(0f, 1f)
        val slotCx = slotCenterXNorm(v.slotIndex) * w
        val slotCy = SLOT_CENTER_Y_NORM * h
        val exitLaneY = rightGapY

        if (p < 0.28f) {
          val localT = (p / 0.28f).coerceIn(0f, 1f)
          val pullOutX = slotCx + (bayW * 0.72f) * (localT * localT)
          val pullOutY = slotCy + (exitLaneY - slotCy) * (localT * localT * (3f - 2f * localT))
          val isoSprite = assets.getBusSprite(v.color, BusView.ISO_2)
          if (localT > 0.25f && isoSprite != null) {
            drawOval(
              color = Color.Black.copy(alpha = 0.24f),
              topLeft = Offset(pullOutX - bayW * 0.48f, pullOutY - bayH * 0.18f + 3.dp.toPx()),
              size = Size(bayW * 0.96f, bayH * 0.42f),
            )
            drawSpriteUniform(
              sprite = isoSprite,
              missingLabel = "Missing bus_${v.color.assetKey}_iso_2",
              textMeasurer = textMeasurer,
              center = Offset(pullOutX, pullOutY),
              maxW = bayH * 0.98f,
              maxH = bayH * 0.96f,
              flipHorizontal = false,
            )
          } else {
            val angle = (-90f + SLOT_TILT_DEG) * (1f - localT)
            val slotStride = (bayH * 0.96f + gapPx) * 0.5f
            drawLotVehicleSprite(
              assets = assets,
              textMeasurer = textMeasurer,
              v = v,
              center = Offset(pullOutX, pullOutY),
              stridePx = slotStride,
              gapPx = gapPx,
              angleOverrideDeg = angle,
            )
          }
        } else {
          val blastT = ((p - 0.28f) / 0.72f).coerceIn(0f, 1f)
          val accel = blastT * blastT
          val startX = slotCx + bayW * 0.72f
          val drawX = startX + (w * 1.45f - startX) * accel
          val drawY = exitLaneY
          val alpha = (1f - (blastT - 0.78f).coerceAtLeast(0f) / 0.22f).coerceIn(0f, 1f)
          val departingLen = bayH * 0.96f
          val departingThick = bayW * 0.96f

          val puffSheet = assets.getExhaustPuffSheet()
          for (sIdx in 0..3) {
            val trailLag = sIdx * 11.dp.toPx() + blastT * 16.dp.toPx()
            val puffAlpha = ((1f - sIdx * 0.20f) * (1f - blastT * 0.58f) * 0.85f).coerceIn(0f, 1f)
            val puffX = drawX - departingLen * 0.56f - trailLag
            val puffY = drawY + ((sIdx % 2) * 4 - 2).dp.toPx() - blastT * (2f + sIdx).dp.toPx()
            if (puffSheet != null) {
              val puffFrame = (sIdx * 2 + (blastT * 5f).roundToInt()).coerceIn(0, 8)
              val puffSize = (13.dp.toPx() + sIdx * 4.dp.toPx()) * (0.78f + 0.38f * blastT)
              drawStripFrame(
                sheet = puffSheet,
                frameCount = 9,
                frameIndex = puffFrame,
                center = Offset(puffX, puffY),
                maxW = puffSize,
                maxH = puffSize,
                alpha = puffAlpha,
              )
            } else {
              val puffR = (5.5.dp.toPx() + sIdx * 1.8.dp.toPx()) * (0.7f + 0.4f * blastT)
              drawCircle(
                color = Color(0xFFF2F6FB).copy(alpha = puffAlpha),
                radius = puffR,
                center = Offset(puffX, puffY),
              )
            }
          }

          val sideSprite = assets.getBusSprite(v.color, BusView.SIDE) ?: assets.getBusSprite(v.color, BusView.HORIZ)
          drawSpriteUniform(
            sprite = sideSprite,
            missingLabel = "Missing bus_${v.color.assetKey}_side",
            textMeasurer = textMeasurer,
            center = Offset(drawX, drawY),
            maxW = departingLen * 1.24f,
            maxH = departingThick * 1.12f,
            flipHorizontal = true,
            alpha = alpha,
          )
        }
        continue
      }

      if (v.stage != VehicleStage.DRIVING_TO_SLOT || v.slotIndex < 0) continue

      val startPt = Offset(m.isoToScreenX(v.startX, v.startY), lotTop + m.isoToScreenY(v.startX, v.startY))
      val slotCx = slotCenterXNorm(v.slotIndex) * w
      val slotCy = SLOT_CENTER_Y_NORM * h

      // Drive cleanly straight out along the isometric diagonal onto the outer perimeter road, then up to slot
      val (sdx, sdy) = isoScreenDirection(v.direction)
      val tX =
        when {
          sdx < -0.01f -> (leftRoadX - startPt.x) / sdx
          sdx > 0.01f -> (rightRoadX - startPt.x) / sdx
          else -> Float.MAX_VALUE
        }
      val tY =
        when {
          sdy < -0.01f -> (entryLaneY - startPt.y) / sdy
          sdy > 0.01f -> (botRoadY - startPt.y) / sdy
          else -> Float.MAX_VALUE
        }
      val hitT = min(tX, tY).coerceAtLeast(cellPx * 0.5f)
      val exitPt = Offset(startPt.x + sdx * hitT, startPt.y + sdy * hitT)

      val rawWaypoints =
        cleanRectilinearWaypoints(
          buildList {
            add(startPt)
            add(exitPt)
            if (exitPt.y > entryLaneY + 3f) {
              val sideX = if (sdx < 0f) leftRoadX else rightRoadX
              if (abs(exitPt.x - sideX) > 3f) {
                add(Offset(sideX, exitPt.y))
              }
              add(Offset(sideX, entryLaneY))
            } else if (abs(exitPt.y - entryLaneY) > 3f) {
              val topCornerX = exitPt.x.coerceIn(leftRoadX, rightRoadX)
              add(Offset(topCornerX, entryLaneY))
            }
            add(Offset(slotCx, entryLaneY))
            add(Offset(slotCx, slotCy))
          }
        )

      val curvedPath = smoothCornerPolyline(rawWaypoints, radiusPx = cornerRadiusPx, arcSteps = 8)
      val rawP = v.animProgress.coerceIn(0f, 1f)
      val easedT = rawP * rawP * (3f - 2f * rawP)
      val sampled = samplePolyline(curvedPath, easedT)

      val trailWindowPx = (152f / 720f) * w
      drawTimedDotTrail(
        points = curvedPath,
        currentDistPx = sampled.distPx,
        windowPx = trailWindowPx,
        dotSpacingPx = 8.dp.toPx(),
        dotRadiusPx = 2.2.dp.toPx(),
      )

      val slotTargetStride = bayH * 0.520f + gapPx
      val approachT =
        if (sampled.pos.y < entryLaneY - 1.5.dp.toPx()) {
          ((entryLaneY - sampled.pos.y) / (entryLaneY - slotCy).coerceAtLeast(1f)).coerceIn(0f, 1f)
        } else {
          0f
        }
      val curStride = cellPx + (slotTargetStride - cellPx) * approachT
      val curLongBox = curStride * 2f - gapPx
      val targetTiltAngle = -90f + SLOT_TILT_DEG
      val blendedAngle =
        if (approachT > 0f) {
          val delta = ((targetTiltAngle - sampled.angleDeg + 540f) % 360f) - 180f
          sampled.angleDeg + delta * approachT
        } else {
          sampled.angleDeg
        }

      val tangRad = sampled.angleDeg * (PI / 180.0)
      val tangDx = cos(tangRad).toFloat()
      val tangDy = sin(tangRad).toFloat()
      val absDx = abs(tangDx)
      val sideSprite = assets.getBusSprite(v.color, BusView.SIDE) ?: assets.getBusSprite(v.color, BusView.HORIZ)

      if (approachT < 0.14f && absDx > 0.86f && abs(tangDy) < 0.28f && sideSprite != null) {
        drawOval(
          color = Color.Black.copy(alpha = 0.22f),
          topLeft = Offset(sampled.pos.x - curLongBox * 0.44f, sampled.pos.y - curLongBox * 0.18f + 2.5.dp.toPx()),
          size = Size(curLongBox * 0.88f, curLongBox * 0.38f),
        )
        drawSpriteUniform(
          sprite = sideSprite,
          missingLabel = "Missing bus_${v.color.assetKey}_side",
          textMeasurer = textMeasurer,
          center = sampled.pos,
          maxW = curLongBox * 1.22f,
          maxH = (curStride - gapPx) * 1.12f,
          flipHorizontal = tangDx > 0f,
        )
      } else if (approachT < 0.14f) {
        val driveIsoView =
          when {
            tangDx >= 0f && tangDy < 0f -> BusView.ISO_1
            tangDx >= 0f && tangDy >= 0f -> BusView.ISO_2
            tangDx < 0f && tangDy >= 0f -> BusView.ISO_3
            else -> BusView.ISO_4
          }
        val isoSprite = assets.getBestIsoBusSprite(v.color, driveIsoView)
        if (isoSprite != null) {
          drawOval(
            color = Color.Black.copy(alpha = 0.22f),
            topLeft = Offset(sampled.pos.x - curStride * 0.92f, sampled.pos.y + curStride * 0.12f),
            size = Size(curStride * 1.84f, curStride * 0.92f),
          )
          drawSpriteUniform(
            sprite = isoSprite,
            missingLabel = "Missing bus_${v.color.assetKey}_iso",
            textMeasurer = textMeasurer,
            center = sampled.pos,
            maxW = curStride * 1.82f,
            maxH = curStride * 1.62f,
          )
        } else {
          drawLotVehicleSprite(
            assets = assets,
            textMeasurer = textMeasurer,
            v = v,
            center = sampled.pos,
            stridePx = curStride,
            gapPx = gapPx,
            angleOverrideDeg = blendedAngle,
          )
        }
      } else {
        drawLotVehicleSprite(
          assets = assets,
          textMeasurer = textMeasurer,
          v = v,
          center = sampled.pos,
          stridePx = curStride,
          gapPx = gapPx,
          angleOverrideDeg = blendedAngle,
        )
      }
    }

    // 2. Jumping passengers (arcing smoothly from boarding gate into parked bus open roof)
    val jumpColWidth = w * 0.052f
    for (jump in jumpingPassengers) {
      val sprite = assets.getPassengerSprite(jump.color) ?: continue
      val (sCols, sRows) = assets.getPassengerGrid(jump.color)
      val p = jump.progress.coerceIn(0f, 1f)
      val sx = jump.startNormX * w
      val sy = jump.startNormY * h
      val ex = slotCenterXNorm(jump.targetSlotIndex) * w
      val ey = SLOT_CENTER_Y_NORM * h - bayH * 0.06f

      val arcY = sin(p * PI).toFloat() * h * 0.085f
      val curX = sx + (ex - sx) * p
      val curY = sy + (ey - sy) * p - arcY
      val scaleIn = (1.0f - p * 0.42f).coerceIn(0.45f, 1.1f)
      val pose = directionPoseForTangent(ex - sx, ey - sy)

      drawPassengerAsset(
        sprite = sprite,
        center = Offset(curX, curY),
        laneColumnWidthPx = jumpColWidth,
        color = jump.color,
        alpha = (1f - (p - 0.86f).coerceAtLeast(0f) / 0.14f).coerceIn(0f, 1f),
        uniformScale = scaleIn,
        pose = pose,
        beatPhase = p * 2f,
        sheetCols = sCols,
        sheetRows = sRows,
        isWalking = true,
      )
    }

    // 3. Confetti & dust particles
    for (pt in particles) {
      val alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f)
      val px = pt.x * w
      val py = pt.y * h
      if (pt.isConfetti) {
        rotate(degrees = pt.life * 360f, pivot = Offset(px, py)) {
          drawRect(
            color = pt.color.copy(alpha = alpha),
            topLeft = Offset(px - pt.sizeDp.dp.toPx() * 0.5f, py - pt.sizeDp.dp.toPx() * 0.35f),
            size = Size(pt.sizeDp.dp.toPx(), pt.sizeDp.dp.toPx() * 0.65f),
          )
        }
      } else {
        drawCircle(
          color = pt.color.copy(alpha = alpha * 0.85f),
          radius = pt.sizeDp.dp.toPx() * 0.5f,
          center = Offset(px, py),
        )
      }
    }

    // 4. Floating pop texts
    for (ft in floatingTexts) {
      val alpha = (ft.life / ft.maxLife).coerceIn(0f, 1f)
      val layout =
        textMeasurer.measure(
          text = ft.text,
          style =
            TextStyle(
              fontFamily = FredokaFontFamily,
              fontWeight = FontWeight.Bold,
              fontSize = 15.sp,
              color = ft.color.copy(alpha = alpha),
            ),
        )
      drawText(
        textLayoutResult = layout,
        topLeft = Offset(ft.x * w - layout.size.width / 2f, ft.y * h - layout.size.height / 2f),
      )
    }
  }
}

private data class PolylineSample(
  val pos: Offset,
  val angleDeg: Float,
  val distPx: Float,
  val totalLenPx: Float,
)

private fun cleanRectilinearWaypoints(points: List<Offset>): List<Offset> {
  val dedup = mutableListOf<Offset>()
  for (pt in points) {
    if (dedup.isEmpty() || hypot(pt.x - dedup.last().x, pt.y - dedup.last().y) > 2f) {
      dedup.add(pt)
    }
  }
  if (dedup.size <= 2) return dedup

  val simplified = mutableListOf(dedup.first())
  for (i in 1 until dedup.size - 1) {
    val a = simplified.last()
    val b = dedup[i]
    val c = dedup[i + 1]
    val cross = abs((b.x - a.x) * (c.y - a.y) - (b.y - a.y) * (c.x - a.x))
    val lenProd = hypot(b.x - a.x, b.y - a.y) * hypot(c.x - b.x, c.y - b.y)
    if (lenProd > 0.01f && cross / lenProd > 0.02f) {
      simplified.add(b)
    }
  }
  simplified.add(dedup.last())
  return simplified
}

private fun DrawScope.drawTimedDotTrail(
  points: List<Offset>,
  currentDistPx: Float,
  windowPx: Float,
  dotSpacingPx: Float,
  dotRadiusPx: Float,
) {
  if (points.size < 2 || currentDistPx < dotSpacingPx) return
  val minKeepDist = (currentDistPx - windowPx).coerceAtLeast(0f)

  var walked = 0f
  var nextSample = dotSpacingPx * 0.5f
  for (i in 0 until points.size - 1) {
    val a = points[i]
    val b = points[i + 1]
    val seg = hypot(b.x - a.x, b.y - a.y)
    if (seg <= 0.01f) continue
    while (nextSample <= walked + seg && nextSample <= currentDistPx) {
      if (nextSample >= minKeepDist) {
        val localT = ((nextSample - walked) / seg).coerceIn(0f, 1f)
        val px = a.x + (b.x - a.x) * localT
        val py = a.y + (b.y - a.y) * localT
        val freshness = ((nextSample - minKeepDist) / windowPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
        val alpha = (freshness * freshness * 0.85f).coerceIn(0f, 0.85f)
        val rad = dotRadiusPx * (0.5f + 0.5f * freshness)
        drawCircle(color = Color.White.copy(alpha = alpha), radius = rad, center = Offset(px, py))
      }
      nextSample += dotSpacingPx
    }
    walked += seg
    if (walked > currentDistPx) break
  }
}

private fun smoothCornerPolyline(
  points: List<Offset>,
  radiusPx: Float,
  arcSteps: Int = 6,
): List<Offset> {
  if (points.size <= 2) return points
  val out = mutableListOf(points.first())
  for (i in 1 until points.size - 1) {
    val prev = points[i - 1]
    val curr = points[i]
    val next = points[i + 1]
    val d1 = hypot(curr.x - prev.x, curr.y - prev.y)
    val d2 = hypot(next.x - curr.x, next.y - curr.y)
    if (d1 < 2f || d2 < 2f) {
      out.add(curr)
      continue
    }
    val cut = minOf(radiusPx, d1 * 0.45f, d2 * 0.45f)
    val pIn = Offset(curr.x - ((curr.x - prev.x) / d1) * cut, curr.y - ((curr.y - prev.y) / d1) * cut)
    val pOut = Offset(curr.x + ((next.x - curr.x) / d2) * cut, curr.y + ((next.y - curr.y) / d2) * cut)
    for (s in 0..arcSteps) {
      val t = s.toFloat() / arcSteps
      val inv = 1f - t
      val qx = inv * inv * pIn.x + 2f * inv * t * curr.x + t * t * pOut.x
      val qy = inv * inv * pIn.y + 2f * inv * t * curr.y + t * t * pOut.y
      out.add(Offset(qx, qy))
    }
  }
  out.add(points.last())
  return out
}

private fun samplePolyline(
  points: List<Offset>,
  fraction: Float,
): PolylineSample {
  if (points.size <= 1) {
    return PolylineSample(points.firstOrNull() ?: Offset.Zero, -90f, 0f, 0f)
  }
  val segLens = FloatArray(points.size - 1)
  var totalLen = 0f
  for (i in 0 until points.size - 1) {
    val d = hypot(points[i + 1].x - points[i].x, points[i + 1].y - points[i].y)
    segLens[i] = d
    totalLen += d
  }
  if (totalLen <= 0.001f) {
    return PolylineSample(points.last(), -90f, 0f, 0f)
  }

  val targetDist = fraction.coerceIn(0f, 1f) * totalLen
  var acc = 0f
  for (i in 0 until points.size - 1) {
    val seg = segLens[i]
    if (acc + seg >= targetDist || i == points.size - 2) {
      val localT = if (seg > 0.001f) ((targetDist - acc) / seg).coerceIn(0f, 1f) else 1f
      val pos =
        Offset(
          points[i].x + (points[i + 1].x - points[i].x) * localT,
          points[i].y + (points[i + 1].y - points[i].y) * localT,
        )
      val angle =
        (atan2((points[i + 1].y - points[i].y).toDouble(), (points[i + 1].x - points[i].x).toDouble()) * 180.0 / PI)
          .toFloat()
      return PolylineSample(pos, angle, targetDist, totalLen)
    }
    acc += seg
  }
  return PolylineSample(points.last(), -90f, totalLen, totalLen)
}
