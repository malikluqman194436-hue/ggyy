package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
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
import com.example.game.DEPART_CLOSE_FRACTION
import com.example.game.Direction
import com.example.game.FloatingText
import com.example.game.IsoProjection
import com.example.game.JumpingPassenger
import com.example.game.LaneGeometry
import com.example.game.LaneSample
import com.example.game.Particle
import com.example.game.SlotKind
import com.example.game.TrackLoopShape
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
  0.1280f, // Slot 0 (VIP)
  0.2370f, // Slot 1
  0.3360f, // Slot 2
  0.4350f, // Slot 3
  0.5340f, // Slot 4
  0.6340f, // Slot 5 (+ locked)
  0.7360f, // Slot 6 (+ locked)
  0.8400f, // Slot 7 (+ locked)
)
private const val SLOT_CENTER_Y_NORM = 0.3545f
private const val SLOT_CARD_CENTER_Y_NORM = 0.3633f
private const val SLOT_CARD_TILT_DEG = -16.7f
private const val SLOT_TILT_DEG = -14.5f

private fun slotCenterXNorm(index: Int): Float =
  if (index in SLOT_CENTERS_X_NORM.indices) SLOT_CENTERS_X_NORM[index]
  else 0.1313f + index * 0.104f

private data class ParkedSlotBusLayout(
  val centerX: Float,
  val centerY: Float,
  val drawW: Float,
  val drawH: Float,
  val tiltDeg: Float,
)

private fun computeParkedSlotBusLayout(
  canvasW: Float,
  canvasH: Float,
  densityPxPerDp: Float,
  slotIndex: Int,
  capacity: Int,
  arriveScale: Float = 1f,
): ParkedSlotBusLayout {
  val cardW = canvasW * 0.0760f
  val cardH = canvasH * 0.0725f
  val borderInset = maxOf(cardW * 0.056f, 1.8f * densityPxPerDp)
  val innerW = (cardW - borderInset * 2f).coerceAtLeast(1f)
  val innerH = (cardH - borderInset * 2f).coerceAtLeast(1f)

  val targetHeightFrac =
    when {
      capacity <= 16 -> 0.78f // Small (16): matches yellow small bus in reference stand
      capacity <= 24 -> 0.89f // Medium (24): sits proportionally between small (16) and big (40)
      else -> 1.00f // Big (40): matches orange big bus filling full stand height
    }
  val drawW = (innerW * 0.92f * arriveScale).coerceAtLeast(1f)
  val drawH = (innerH * targetHeightFrac * arriveScale).coerceAtLeast(1f)

  val topMargin =
    when {
      capacity <= 16 -> innerH * 0.015f
      capacity <= 24 -> innerH * 0.008f
      else -> 0f
    }
  val targetLocalY = -innerH * 0.5f + topMargin + drawH * 0.5f
  val tiltRad = (SLOT_CARD_TILT_DEG * PI / 180.0).toFloat()
  val slotCx = slotCenterXNorm(slotIndex) * canvasW
  val slotCardCy = SLOT_CARD_CENTER_Y_NORM * canvasH

  return ParkedSlotBusLayout(
    centerX = slotCx - targetLocalY * sin(tiltRad),
    centerY = slotCardCy + targetLocalY * cos(tiltRad),
    drawW = drawW,
    drawH = drawH,
    tiltDeg = SLOT_CARD_TILT_DEG,
  )
}

private data class LotMetrics(
  val cellPx: Float,
  val stride: Float,
  val gapPx: Float,
  val cellBoxPx: Float,
  val centerX: Float,
  val centerY: Float,
  val gridCols: Int,
  val gridRows: Int,
  val isOrthographic: Boolean = false,
) {
  fun isoToScreenX(gx: Float, gy: Float): Float {
    val du = gx - gridCols * 0.5f
    val dv = gy - gridRows * 0.5f
    return if (isOrthographic) {
      centerX + du * cellPx
    } else {
      centerX + (du * IsoProjection.UX + dv * IsoProjection.VX) * cellPx
    }
  }

  fun isoToScreenY(gx: Float, gy: Float): Float {
    val du = gx - gridCols * 0.5f
    val dv = gy - gridRows * 0.5f
    return if (isOrthographic) {
      centerY + dv * cellPx
    } else {
      centerY + (du * IsoProjection.UY + dv * IsoProjection.VY) * cellPx
    }
  }

  fun isoToScreen(gx: Float, gy: Float): Offset =
    Offset(isoToScreenX(gx, gy), isoToScreenY(gx, gy))

  fun screenToIso(sx: Float, sy: Float): Pair<Float, Float> {
    val rx = (sx - centerX) / cellPx.coerceAtLeast(1f)
    val ry = (sy - centerY) / cellPx.coerceAtLeast(1f)
    if (isOrthographic) {
      return (rx + gridCols * 0.5f) to (ry + gridRows * 0.5f)
    }
    val det = (IsoProjection.UX * IsoProjection.VY - IsoProjection.UY * IsoProjection.VX).takeIf { abs(it) > 1e-4f } ?: 1f
    val du = (rx * IsoProjection.VY - ry * IsoProjection.VX) / det
    val dv = (IsoProjection.UX * ry - IsoProjection.UY * rx) / det
    return (du + gridCols * 0.5f) to (dv + gridRows * 0.5f)
  }
}

private fun isoScreenDirection(dir: Direction): Pair<Float, Float> =
  when (dir) {
    Direction.RIGHT, Direction.UP_RIGHT -> IsoProjection.UX to IsoProjection.UY
    Direction.DOWN, Direction.DOWN_RIGHT -> IsoProjection.VX to IsoProjection.VY
    Direction.LEFT, Direction.DOWN_LEFT -> -IsoProjection.UX to -IsoProjection.UY
    Direction.UP, Direction.UP_LEFT -> -IsoProjection.VX to -IsoProjection.VY
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
  isOrthographic: Boolean = false,
): LotMetrics {
  if (isOrthographic) {
    val spanX = gridCols.toFloat().coerceAtLeast(4f)
    val spanY = gridRows.toFloat().coerceAtLeast(4f)
    val cellByW = (widthPx * 0.80f) / spanX
    val cellByH = (heightPx * 0.84f) / spanY
    val cellPx = min(cellByW, cellByH).coerceIn(14f * density, 44f * density)
    val gapPx = (cellPx * 0.14f).coerceIn(2.5f * density, 5.5f * density)
    return LotMetrics(
      cellPx = cellPx,
      stride = cellPx,
      gapPx = gapPx,
      cellBoxPx = cellPx - gapPx,
      centerX = widthPx * 0.495f,
      centerY = heightPx * 0.515f,
      gridCols = gridCols,
      gridRows = gridRows,
      isOrthographic = true,
    )
  }
  val spanX = (gridCols * IsoProjection.UX + gridRows * IsoProjection.VX).coerceAtLeast(3.5f)
  val spanY = (gridCols * abs(IsoProjection.UY) + gridRows * IsoProjection.VY).coerceAtLeast(3.5f)
  val cellByW = (widthPx * 0.88f) / spanX
  val cellByH = (heightPx * 0.90f) / spanY
  val cellPx = min(cellByW, cellByH).coerceIn(18f * density, 44f * density)
  val gapPx = (cellPx * 0.14f).coerceIn(3.5f * density, 6f * density)
  return LotMetrics(
    cellPx = cellPx,
    stride = cellPx,
    gapPx = gapPx,
    cellBoxPx = cellPx - gapPx,
    centerX = widthPx * 0.49f,
    centerY = heightPx * 0.49f,
    gridCols = gridCols,
    gridRows = gridRows,
    isOrthographic = false,
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
    frames = intArrayOf(9, 11, 10, 11),
    flipPerFrame = booleanArrayOf(false, false, false, false),
    idleCol = 11,
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
    frames = intArrayOf(9, 11, 10, 11),
    flipPerFrame = booleanArrayOf(true, true, true, true),
    idleCol = 11,
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
    frames = intArrayOf(6, 5, 6, 5),
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

private val POSE_LEVEL2_FRONT =
  SheetDirectionPose(
    row = 0,
    frames = intArrayOf(0, 1, 0, 1),
    flipPerFrame = booleanArrayOf(false, false, false, true),
    idleCol = 0,
    idleFlip = false,
  )

private val POSE_LEVEL2_FRONT_RIGHT =
  SheetDirectionPose(
    row = 0,
    frames = intArrayOf(2, 0, 2, 0),
    flipPerFrame = booleanArrayOf(false, false, false, false),
    idleCol = 2,
    idleFlip = false,
  )

private val POSE_LEVEL2_FRONT_LEFT =
  SheetDirectionPose(
    row = 0,
    frames = intArrayOf(2, 0, 2, 0),
    flipPerFrame = booleanArrayOf(true, true, true, true),
    idleCol = 2,
    idleFlip = true,
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
  headScale: Float = 1f,
  widthScale: Float = 1f,
  heightScale: Float = 1f,
  compactSphericalChibi: Boolean = false,
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
  val drawW = laneColumnWidthPx * uniformScale * widthScale
  val drawH = laneColumnWidthPx * uniformScale * (cellH.toFloat() / cellW.toFloat()) * 0.90f * heightScale
  val walkBob: Float
  val animScaleX: Float
  val animBounceY: Float
  val walkWaddleDeg: Float

  if (isWalking) {
    val frameCount = pose.frames.size.coerceAtLeast(1)
    // 1.0 beatPhase = one full step (Contact -> Pass -> Next Contact), 2.0 beatPhase = full 4-frame cycle
    val cyclePos = beatPhase * 2f
    val idx = (((cyclePos + 0.5f) % frameCount + frameCount) % frameCount).toInt().coerceIn(0, frameCount - 1)
    frameCol = pose.frames[idx].coerceIn(0, cols - 1)
    flipHorizontal = pose.flipPerFrame[idx]

    // Gentle vertical step lift without any squash/stretch so all 4 passengers stay strictly identical size
    val passLift = (0.5f * (1f - cos(cyclePos * PI))).toFloat()
    walkBob = -passLift * (if (compactSphericalChibi) drawW * 0.020f else drawH * 0.025f)
    animScaleX = 1f
    animBounceY = bounceScaleY
    walkWaddleDeg = 0f
  } else {
    // Waiting queue: keep BOTH feet planted and all 4 passengers strictly uniform size
    frameCol = pose.idleCol.coerceIn(0, cols - 1)
    flipHorizontal = pose.idleFlip
    walkBob = 0f
    animScaleX = 1f
    animBounceY = bounceScaleY
    walkWaddleDeg = 0f
  }

  if (compactSphericalChibi) {
    val fullH = laneColumnWidthPx * uniformScale * (cellH.toFloat() / cellW.toFloat()) * 0.85f * heightScale

    // Ground shadow tucked cleanly beneath twin feet so it never touches curbs
    drawOval(
      color = Color.Black.copy(alpha = 0.22f * alpha),
      topLeft = Offset(center.x - drawW * 0.24f, center.y + fullH * 0.35f),
      size = Size(drawW * 0.48f, fullH * 0.065f),
    )

    val totalLeanDeg = leanDeg + walkWaddleDeg
    withTransform({
      translate(center.x, center.y + walkBob)
      if (totalLeanDeg != 0f) rotate(degrees = totalLeanDeg, pivot = Offset.Zero)
      scale(
        scaleX = (if (flipHorizontal) -1f else 1f) * animScaleX,
        scaleY = animBounceY,
        pivot = Offset.Zero,
      )
    }) {
      // Subtle ambient-occlusion contour behind full figure so stacked 4x4 rows stay distinct
      drawOval(
        color = Color.Black.copy(alpha = 0.16f * alpha),
        topLeft = Offset(-drawW * 0.28f, -fullH * 0.42f),
        size = Size(drawW * 0.56f, fullH * 0.52f),
      )

      if (kotlin.math.abs(headScale - 1f) < 0.005f) {
        drawImage(
          image = sprite,
          srcOffset = IntOffset(frameCol * cellW, safeRow * cellH),
          srcSize = IntSize(cellW, cellH),
          dstOffset = IntOffset((-drawW * 0.5f).roundToInt(), (-fullH * 0.495f).roundToInt()),
          dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), fullH.roundToInt().coerceAtLeast(1)),
          alpha = alpha,
        )
      } else {
        val splitSrcY = (cellH * 0.50f).roundToInt().coerceIn(1, cellH - 1)
        val bodyDstH = (fullH * 0.50f).roundToInt().coerceAtLeast(1)
        val neckY = -fullH * 0.005f
        drawImage(
          image = sprite,
          srcOffset = IntOffset(frameCol * cellW, safeRow * cellH + splitSrcY),
          srcSize = IntSize(cellW, cellH - splitSrcY),
          dstOffset = IntOffset((-drawW * 0.5f).roundToInt(), neckY.roundToInt()),
          dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), bodyDstH),
          alpha = alpha,
        )
        val headW = drawW * headScale
        val headH = fullH * 0.50f * headScale
        drawImage(
          image = sprite,
          srcOffset = IntOffset(frameCol * cellW, safeRow * cellH),
          srcSize = IntSize(cellW, splitSrcY),
          dstOffset = IntOffset((-headW * 0.5f).roundToInt(), (neckY - headH).roundToInt()),
          dstSize = IntSize(headW.roundToInt().coerceAtLeast(1), (headH + 1.5f).roundToInt().coerceAtLeast(1)),
          alpha = alpha,
        )
      }
    }
    return
  }

  // Soft ground shadow directly beneath feet (foot-anchored so sprite spans -0.80*drawH .. +0.20*drawH)
  drawOval(
    color = Color.Black.copy(alpha = 0.24f * alpha),
    topLeft = Offset(center.x - drawW * 0.23f, center.y + drawH * 0.11f),
    size = Size(drawW * 0.46f, drawH * 0.08f),
  )

  val totalLeanDeg = leanDeg + walkWaddleDeg
  withTransform({
    translate(center.x, center.y + walkBob)
    if (totalLeanDeg != 0f) rotate(degrees = totalLeanDeg, pivot = Offset(0f, drawH * 0.16f))
    scale(
      scaleX = (if (flipHorizontal) -1f else 1f) * animScaleX,
      scaleY = animBounceY,
      pivot = Offset(0f, drawH * 0.16f),
    )
  }) {
    if (kotlin.math.abs(headScale - 1f) < 0.005f) {
      drawImage(
        image = sprite,
        srcOffset = IntOffset(frameCol * cellW, safeRow * cellH),
        srcSize = IntSize(cellW, cellH),
        dstOffset = IntOffset((-drawW * 0.5f).roundToInt(), (-drawH * 0.80f).roundToInt()),
        dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), drawH.roundToInt().coerceAtLeast(1)),
        alpha = alpha,
      )
    } else {
      val splitSrcY = (cellH * 0.50f).roundToInt().coerceIn(1, cellH - 1)
      val bodyDstH = (drawH * 0.50f).roundToInt().coerceAtLeast(1)
      val neckY = -drawH * 0.30f
      drawImage(
        image = sprite,
        srcOffset = IntOffset(frameCol * cellW, safeRow * cellH + splitSrcY),
        srcSize = IntSize(cellW, cellH - splitSrcY),
        dstOffset = IntOffset((-drawW * 0.5f).roundToInt(), neckY.roundToInt()),
        dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), bodyDstH),
        alpha = alpha,
      )
      val headW = drawW * headScale
      val headH = drawH * 0.50f * headScale
      drawImage(
        image = sprite,
        srcOffset = IntOffset(frameCol * cellW, safeRow * cellH),
        srcSize = IntSize(cellW, splitSrcY),
        dstOffset = IntOffset((-headW * 0.5f).roundToInt(), (neckY - headH).roundToInt()),
        dstSize = IntSize(headW.roundToInt().coerceAtLeast(1), (headH + 1.5f).roundToInt().coerceAtLeast(1)),
        alpha = alpha,
      )
    }
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
  drawRoadDashes: Boolean = false,
  passengerSizeScale: Float = 1.0f,
  passengerHeadScale: Float = 1.0f,
  passengerWidthScale: Float = 1.0f,
  passengerHeightScale: Float = 1.0f,
  trackShape: TrackLoopShape = TrackLoopShape.LEVEL1_OVAL,
) {
  val assets = rememberAssetManager()
  val textMeasurer = rememberTextMeasurer()

  BoxWithConstraints(modifier = modifier.fillMaxSize().clipToBounds()) {
    val boxW = maxWidth
    val boxH = maxHeight
    val laneGeom = androidx.compose.runtime.remember(boxW.value, boxH.value, passengerSizeScale, trackShape) {
      LaneGeometry(boxW.value, boxH.value, passengerSizeScale, trackShape)
    }

    androidx.compose.runtime.LaunchedEffect(boxW.value, boxH.value) {
      onViewportSized(boxW.value, boxH.value)
    }

    Canvas(modifier = Modifier.fillMaxSize()) {
      // Rotating conveyor road belt: animated white dashed centerlines along the oval ring & arms
      val ringLenPx = laneGeom.ringLenDp.dp.toPx()
      if (drawRoadDashes && ringLenPx > 1f) {
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
        val minVisibleYDp = -(laneGeom.passengerSpriteHeightDp * passengerHeightScale * 0.85f)
        val halfSpriteWDp = laneGeom.passengerSpriteWidthDp * passengerWidthScale * 0.65f
        val minVisibleXDp = -halfSpriteWDp
        val maxVisibleXDp = boxW.value + halfSpriteWDp
        val spriteBaseWidthPx = laneGeom.passengerSpriteWidthDp.dp.toPx()
        val offscreenArmCullDist = -(laneGeom.rowSpacingDp * 2.5f + laneGeom.passengerSpriteHeightDp)

        // Sort strictly by screen Y ascending (painter's order) so closer rows naturally overlap rows behind
        data class RenderItem(
          val passenger: ConveyorPassenger,
          val sample: LaneSample,
          val xDp: Float,
          val yDp: Float,
          val scale: Float,
        )

        val isLevel2Oval = trackShape == TrackLoopShape.LEVEL2_ROUNDED_OVAL
        val renderList = ArrayList<RenderItem>(passengers.size)
        for (p in passengers) {
          if (p.segment != ConveyorSegment.RING && p.distDp < offscreenArmCullDist) continue
          val (sample, posDp) =
            laneGeom.passengerCoords(
              segment = p.segment,
              distDp = p.distDp,
              colInRow = p.colInRow,
              widthScale = passengerWidthScale,
              heightScale = passengerHeightScale,
            )
          if (isLevel2Oval && p.segment != ConveyorSegment.RING && sample.y < boxH.value * 0.145f) continue
          if (posDp.second < minVisibleYDp || posDp.second > maxVisibleYDp) continue
          if (posDp.first < minVisibleXDp || posDp.first > maxVisibleXDp) continue
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
          val pose =
            if (isLevel2Oval) {
              when (p.segment) {
                ConveyorSegment.LEFT_ARM -> POSE_LEVEL2_FRONT_RIGHT
                ConveyorSegment.RIGHT_ARM -> POSE_LEVEL2_FRONT_LEFT
                ConveyorSegment.RING -> POSE_LEVEL2_FRONT
              }
            } else {
              directionPoseForTangent(item.sample.tx, item.sample.ty)
            }
          val beat = (conveyorOffsetPx * 0.068f) + (p.rowGroupId % 2) * 0.5f

          drawPassengerAsset(
            sprite = sprite,
            center = Offset(item.xDp.dp.toPx(), item.yDp.dp.toPx()),
            laneColumnWidthPx = spriteBaseWidthPx,
            color = p.color,
            textMeasurer = textMeasurer,
            uniformScale = item.scale,
            pose = pose,
            beatPhase = beat,
            sheetCols = sCols,
            sheetRows = sRows,
            isWalking = p.isWalking,
            headScale = passengerHeadScale,
            widthScale = passengerWidthScale,
            heightScale = passengerHeightScale,
            compactSphericalChibi = isLevel2Oval,
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
    val bayWidth = (w * 0.095f).coerceAtLeast(34.dp)
    val bayHeight = (h * 0.090f).coerceAtLeast(64.dp)
    val closeFraction = DEPART_CLOSE_FRACTION

    // Cover baked '+' stands on unlocked locked-slots with exact replicas of stands 1..4
    if (slots.any { it.kind == SlotKind.LOCKED && it.unlocked }) {
      Canvas(modifier = Modifier.fillMaxSize()) {
        val cw = size.width
        val ch = size.height
        val cardW = cw * 0.0760f
        val cardH = ch * 0.0725f
        val borderInset = (cardW * 0.056f).coerceAtLeast(1.8.dp.toPx())
        val innerW = (cardW - borderInset * 2f).coerceAtLeast(1f)
        val innerH = (cardH - borderInset * 2f).coerceAtLeast(1f)

        // Pass 1: Completely erase the baked '+' card + dashed border & shadow in native FillBounds space (941x1672)
        // Strictly clip below y=539.5 (the yellow strip ends at y=536, baked '+' cards start at y=544)
        val coverW941 = 941f * 0.0995f
        val coverH1672 = 1672f * 0.0795f
        val bakedCy1672 = 1672f * 0.3622f
        withTransform({
          scale(scaleX = cw / 941f, scaleY = ch / 1672f, pivot = Offset.Zero)
        }) {
          clipRect(left = 0f, top = 539.5f, right = 941f, bottom = 700f) {
            for (slot in slots) {
              if (slot.kind != SlotKind.LOCKED || !slot.unlocked) continue
              val bakedCx941 =
                when (slot.index) {
                  5 -> 941f * 0.6350f
                  6 -> 941f * 0.7391f
                  7 -> 941f * 0.8438f
                  else -> continue
                }

              withTransform({
                translate(bakedCx941, bakedCy1672)
                rotate(-17.2f, pivot = Offset.Zero)
              }) {
                drawRoundRect(
                  brush =
                    Brush.verticalGradient(
                      colors = listOf(Color(0xFF8A9BBD), Color(0xFF8B9CBD), Color(0xFF8D9DBF)),
                      startY = -coverH1672 * 0.5f,
                      endY = coverH1672 * 0.5f,
                    ),
                  topLeft = Offset(-coverW941 * 0.5f, -coverH1672 * 0.5f),
                  size = Size(coverW941, coverH1672),
                  cornerRadius = CornerRadius(coverW941 * 0.18f),
                )
              }
            }
          }
        }

        // Pass 2: Draw crisp white-bordered navy stand replicas identical to stands 1..4
        for (slot in slots) {
          if (slot.kind != SlotKind.LOCKED || !slot.unlocked) continue
          val scx = slotCenterXNorm(slot.index) * cw
          val scy = SLOT_CARD_CENTER_Y_NORM * ch

          withTransform({
            translate(scx, scy)
            rotate(SLOT_CARD_TILT_DEG, pivot = Offset.Zero)
          }) {
            // Subtle outer shadow rim matching stands 1..4
            drawRoundRect(
              color = Color(0xFF566B9B),
              topLeft = Offset(-cardW * 0.5f - 0.6.dp.toPx(), -cardH * 0.5f - 0.6.dp.toPx()),
              size = Size(cardW + 1.4.dp.toPx(), cardH + 2.0.dp.toPx()),
              cornerRadius = CornerRadius(cardW * 0.24f),
            )

            // Crisp white outer frame identical to stands 1..4
            drawRoundRect(
              color = Color(0xFFF6FAFF),
              topLeft = Offset(-cardW * 0.5f, -cardH * 0.5f),
              size = Size(cardW, cardH),
              cornerRadius = CornerRadius(cardW * 0.23f),
            )

            // Deep royal-navy interior identical to stands 1..4 (#152B8A)
            drawRoundRect(
              brush =
                Brush.verticalGradient(
                  colors = listOf(Color(0xFF152C8E), Color(0xFF152B8A), Color(0xFF142784)),
                  startY = -innerH * 0.5f,
                  endY = innerH * 0.5f,
                ),
              topLeft = Offset(-innerW * 0.5f, -innerH * 0.5f),
              size = Size(innerW, innerH),
              cornerRadius = CornerRadius(innerW * 0.19f),
            )
          }
        }
      }
    }

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
        if (slot.kind == SlotKind.LOCKED && !slot.unlocked && slot.index in 1..4) {
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
          val topUpSprite = assets.getWaitingBayBusSprite(occupant.color, occupant.capacity)
          val passengerSprite = assets.getPassengerSprite(occupant.color)
          val inFlightCount = jumpingPassengers.count { it.targetVehicleId == occupant.id }
          val densityPx = LocalDensity.current.density

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
              val layout =
                computeParkedSlotBusLayout(
                  canvasW = w.toPx(),
                  canvasH = h.toPx(),
                  densityPxPerDp = densityPx,
                  slotIndex = slot.index,
                  capacity = occupant.capacity,
                  arriveScale = arriveScale,
                )
              val dx = layout.centerX - cx.toPx()
              val dy = layout.centerY - cy.toPx()

              withTransform({
                translate(center.x + dx, center.y + dy)
                rotate(layout.tiltDeg, pivot = Offset.Zero)
              }) {
                drawOpenTopBusBody(
                  topSprite = topUpSprite,
                  passengerSprite = passengerSprite,
                  vehicle = occupant,
                  drawW = layout.drawW,
                  drawH = layout.drawH,
                  inFlightTowardRoof = inFlightCount,
                  sunOpenWFrac = 204f / 256f,
                  sunOpenHFrac = (topUpSprite.height - 110f) / topUpSprite.height.toFloat(),
                  sunOpenCenterYFrac = -23f / topUpSprite.height.toFloat(),
                )
              }
            }
          }
        }
      }

      if (showOccupant && occupant != null) {
        val remaining = (occupant.capacity - occupant.boardedCount).coerceAtLeast(0)
        BusSeatCounterPill(
          countText = remaining.toString(),
          busColor = occupant.color,
          modifier =
            Modifier
              .offset(x = cx - bayWidth * 0.50f, y = cy + bayHeight * 0.48f)
              .width(bayWidth),
        )
      }
    }
  }
}

@Composable
private fun BusSeatCounterPill(
  countText: String,
  busColor: VehicleColor,
  modifier: Modifier = Modifier,
) {
  val density = LocalDensity.current.density
  val topColor =
    if (busColor == VehicleColor.WHITE) Color(0xFFF7FAFF)
    else lerp(busColor.highlight, busColor.fill, 0.32f)
  val mainColor =
    if (busColor == VehicleColor.WHITE) Color(0xFFD7E1F0)
    else busColor.fill
  val shadowColor =
    if (busColor == VehicleColor.WHITE) Color(0xFF7D8BA3)
    else busColor.shade
  val strokeWidthPx = (2.6f * density).coerceAtLeast(4.5f)
  val numberBaseStyle =
    MaterialTheme.typography.labelSmall.copy(
      fontSize = 12.sp,
      fontFamily = FredokaFontFamily,
      fontWeight = FontWeight.ExtraBold,
      letterSpacing = 0.2.sp,
    )

  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    // 3D colored shadow pill matching the bus color
    Box(
      modifier =
        Modifier
          .clip(RoundedCornerShape(6.dp))
          .background(shadowColor)
          .padding(bottom = 2.dp)
          .clip(RoundedCornerShape(5.5.dp))
          .background(Brush.verticalGradient(colors = listOf(topColor, mainColor)))
          .border(1.1.dp, shadowColor.copy(alpha = 0.95f), RoundedCornerShape(5.5.dp))
          .padding(horizontal = 8.dp, vertical = 1.dp),
      contentAlignment = Alignment.Center,
    ) {
      Box(contentAlignment = Alignment.Center) {
        Text(
          text = countText,
          style =
            numberBaseStyle.copy(
              drawStyle = Stroke(width = strokeWidthPx * 1.15f, join = StrokeJoin.Round),
            ),
          color = Color(0xCC0C1426),
          maxLines = 1,
          modifier = Modifier.offset(y = 0.8.dp),
        )
        Text(
          text = countText,
          style =
            numberBaseStyle.copy(
              drawStyle = Stroke(width = strokeWidthPx, join = StrokeJoin.Round),
            ),
          color = Color(0xFF141C2E),
          maxLines = 1,
        )
        Text(
          text = countText,
          style = numberBaseStyle,
          color = Color.White,
          maxLines = 1,
        )
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
  isOrthographicLot: Boolean = false,
) {
  val assets = rememberAssetManager()
  val textMeasurer = rememberTextMeasurer()
  val tutorialHandBitmap = ImageBitmap.imageResource(id = R.drawable.ic_tutorial_hand)
  val infiniteTransition = rememberInfiniteTransition(label = "tutorial_pulse")
  val handBob by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(560, easing = FastOutSlowInEasing), RepeatMode.Reverse),
      label = "hand_bob",
    )

  BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
    Canvas(
      modifier =
        Modifier
          .fillMaxSize()
          .testTag("parking_lot_canvas")
          .pointerInput(gridCols, gridRows, isOrthographicLot) {
            detectTapGestures { offset ->
              val m =
                computeLotMetrics(
                  size.width.toFloat(),
                  size.height.toFloat(),
                  gridCols,
                  gridRows,
                  density,
                  isOrthographic = isOrthographicLot,
                )
              val (gx, gy) = m.screenToIso(offset.x, offset.y)
              onLotTap(gx, gy)
            }
          },
    ) {
      val m =
        computeLotMetrics(
          size.width,
          size.height,
          gridCols,
          gridRows,
          density,
          isOrthographic = isOrthographicLot,
        )
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
            if (isOrthographicLot) {
              compareBy<VehicleEntity> { it.y }
                .thenBy { it.x }
                .thenBy { it.id }
            } else {
              compareBy<VehicleEntity> { IsoProjection.UY * it.x + IsoProjection.VY * it.y }
                .thenBy { IsoProjection.UX * it.x + IsoProjection.VX * it.y }
                .thenBy { it.id }
            }
          )

      for (v in lotVehicles) {
        val shakeJitter =
          if (v.shakePhase > 0f) sin(v.shakePhase * 28.0).toFloat() * 3.dp.toPx() * v.shakePhase else 0f
        val (sdx, sdy) =
          if (isOrthographicLot) v.direction.dx to v.direction.dy
          else isoScreenDirection(v.direction)
        val bx = v.x + v.direction.dx * v.bumpOffset
        val by = v.y + v.direction.dy * v.bumpOffset
        val basePos = m.isoToScreen(bx, by)
        val cx = basePos.x - sdy * shakeJitter
        val cy = basePos.y + sdx * shakeJitter

        if (isOrthographicLot) {
          drawOrthoLotBusSprite(
            assets = assets,
            textMeasurer = textMeasurer,
            v = v,
            center = Offset(cx, cy),
            cellPx = cellPx,
          )
        } else {
          drawIsoLotBusSprite(
            assets = assets,
            textMeasurer = textMeasurer,
            v = v,
            center = Offset(cx, cy),
            cellPx = cellPx,
          )
        }

        if (tutorialVehicleId == v.id && (v.stage == VehicleStage.IN_LOT || v.stage == VehicleStage.INTRO_SLIDING)) {
          drawCircle(
            color = GoldCoin.copy(alpha = 0.78f - 0.42f * handBob),
            radius = cellPx * (0.95f + 0.16f * handBob),
            center = Offset(cx, cy),
            style = Stroke(width = 3.5.dp.toPx()),
          )
          drawTutorialHand(
            handBitmap = tutorialHandBitmap,
            targetCenter = Offset(cx, cy),
            cellPx = cellPx,
            tapProgress = handBob,
          )
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

  // Exact uniform cell-space scale matching IsoProjection.PX_PER_CELL inside SPRITE_SIZE_PX
  val drawSize = cellPx * (IsoProjection.SPRITE_SIZE_PX / IsoProjection.PX_PER_CELL) * v.squashFactor

  drawImage(
    image = sprite,
    dstOffset = IntOffset((center.x - drawSize * 0.5f).roundToInt(), (center.y - drawSize * 0.5f).roundToInt()),
    dstSize = IntSize(drawSize.roundToInt().coerceAtLeast(1), drawSize.roundToInt().coerceAtLeast(1)),
  )

  if (v.flashRedTimer > 0f) {
    drawOval(
      color = AlertRed.copy(alpha = 0.42f),
      topLeft = Offset(center.x - cellPx * 0.72f, center.y - cellPx * 0.52f),
      size = Size(cellPx * 1.44f, cellPx * 1.04f),
    )
    drawOval(
      color = Color.White.copy(alpha = 0.88f),
      topLeft = Offset(center.x - cellPx * 0.72f, center.y - cellPx * 0.52f),
      size = Size(cellPx * 1.44f, cellPx * 1.04f),
      style = Stroke(width = 2.dp.toPx()),
    )
  }
}

private fun DrawScope.drawOrthoLotBusSprite(
  assets: AssetManager,
  @Suppress("UNUSED_PARAMETER") textMeasurer: TextMeasurer,
  v: VehicleEntity,
  center: Offset,
  cellPx: Float,
  angleOverrideDeg: Float? = null,
) {
  val renderAngle = angleOverrideDeg ?: v.direction.angleDeg
  val sprite =
    assets.getOrthoBusSprite(
      color = v.color,
      angleDeg = renderAngle,
      lengthInCells = v.lengthInCells,
      widthInCells = v.widthInCells,
    )
  val drawSize =
    cellPx * (com.example.game.OrthoProjection.SPRITE_SIZE_PX / com.example.game.OrthoProjection.PX_PER_CELL) *
      v.squashFactor

  drawImage(
    image = sprite,
    dstOffset = IntOffset((center.x - drawSize * 0.5f).roundToInt(), (center.y - drawSize * 0.5f).roundToInt()),
    dstSize = IntSize(drawSize.roundToInt().coerceAtLeast(1), drawSize.roundToInt().coerceAtLeast(1)),
  )

  if (v.flashRedTimer > 0f) {
    val fw = v.lengthInCells * cellPx * 0.94f
    val fh = v.widthInCells * cellPx * 0.94f
    withTransform({
      translate(center.x, center.y)
      rotate(renderAngle, pivot = Offset.Zero)
    }) {
      drawRoundRect(
        color = AlertRed.copy(alpha = 0.45f),
        topLeft = Offset(-fw * 0.5f, -fh * 0.5f),
        size = Size(fw, fh),
        cornerRadius = CornerRadius(fh * 0.25f),
      )
      drawRoundRect(
        color = Color.White.copy(alpha = 0.88f),
        topLeft = Offset(-fw * 0.5f, -fh * 0.5f),
        size = Size(fw, fh),
        cornerRadius = CornerRadius(fh * 0.25f),
        style = Stroke(width = 2.dp.toPx()),
      )
    }
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
  sunOpenWFrac: Float = 0.797f,
  sunOpenHFrac: Float = 0.771f,
  sunOpenCenterYFrac: Float = -0.048f,
) {
  drawImage(
    image = topSprite,
    dstOffset = IntOffset((-drawW * 0.5f).roundToInt(), (-drawH * 0.5f).roundToInt()),
    dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), drawH.roundToInt().coerceAtLeast(1)),
  )

  val open = vehicle.roofOpenProgress.coerceIn(0f, 1f)
  if (open <= 0.05f) return

  val remainingToLand = (vehicle.capacity - vehicle.boardedCount).coerceAtLeast(0)
  val effectiveRemaining = (vehicle.capacity - vehicle.reservedCount).coerceAtLeast(0)
  val visibleHeads =
    if (inFlightTowardRoof > 0) {
      (vehicle.capacity - effectiveRemaining - inFlightTowardRoof).coerceIn(0, vehicle.capacity)
    } else {
      (vehicle.capacity - remainingToLand).coerceIn(0, vehicle.capacity)
    }

  val fullOpenW = drawW * sunOpenWFrac
  val fullOpenH = drawH * sunOpenHFrac
  val openCenterY = drawH * sunOpenCenterYFrac
  val fullTop = openCenterY - fullOpenH * 0.5f
  val fullLeft = -fullOpenW * 0.5f

  val curW = (fullOpenW * open).coerceAtLeast(2f)
  val curH = (fullOpenH * open).coerceAtLeast(2f)
  val curLeft = -curW * 0.5f
  val curTop = openCenterY - curH * 0.5f
  val cornerRadius = CornerRadius(min(curW, fullOpenW) * 0.14f)

  // Clean matching-color recessed sunroof when empty; richer shaded bus-color cabin floor when passengers are seated
  val sunTopColor =
    if (visibleHeads > 0) lerp(vehicle.color.roofDark, Color(0xFF0B1220), 0.32f)
    else lerp(vehicle.color.roofMain, vehicle.color.roofDark, 0.16f)
  val sunMidColor =
    if (visibleHeads > 0) lerp(vehicle.color.roofDark, vehicle.color.roofMain, 0.12f)
    else vehicle.color.roofMain
  val sunBotColor =
    if (visibleHeads > 0) lerp(vehicle.color.roofDark, vehicle.color.roofMain, 0.25f)
    else lerp(vehicle.color.roofMain, vehicle.color.roofLight, 0.24f)

  drawRoundRect(
    brush =
      Brush.verticalGradient(
        colors = listOf(sunTopColor, sunMidColor, sunBotColor),
        startY = curTop,
        endY = curTop + curH,
      ),
    topLeft = Offset(curLeft, curTop),
    size = Size(curW, curH),
    cornerRadius = cornerRadius,
  )

  val rows = ((vehicle.capacity + SEAT_COLS - 1) / SEAT_COLS).coerceAtLeast(1)
  val padX = fullOpenW * 0.03f
  val padY = fullOpenH * 0.035f
  val usableW = (fullOpenW - padX * 2f).coerceAtLeast(4f)
  val usableH = (fullOpenH - padY * 2f).coerceAtLeast(4f)
  val colPitch = usableW / SEAT_COLS.toFloat()
  val rowPitch = min(usableH / rows.toFloat(), colPitch * 1.14f)
  val unit = min(colPitch, rowPitch)

  if (visibleHeads > 0 && open > 0.20f) {
    // Big chubby 3D seated passenger heads packed 4-per-row edge-to-edge across the cabin
    val headR = (unit * 0.485f).coerceAtLeast(2.3.dp.toPx())
    val contourColor = lerp(vehicle.color.roofDark, Color(0xFF080D18), 0.52f)
    val highlightPeak = lerp(vehicle.color.roofLight, Color.White, 0.45f)
    val midBodyColor = lerp(vehicle.color.roofLight, vehicle.color.roofMain, 0.34f)
    val edgeShadeColor = lerp(vehicle.color.roofMain, vehicle.color.roofDark, 0.40f)
    val outlineStrokeW = (headR * 0.14f).coerceAtLeast(0.65.dp.toPx())
    val shineR = (headR * 0.26f).coerceAtLeast(0.9f)

    clipRect(
      left = curLeft + 0.8f,
      top = curTop + 0.8f,
      right = curLeft + curW - 0.8f,
      bottom = curTop + curH - 0.8f,
    ) {
      val gridStartX = fullLeft + padX + colPitch * 0.5f
      val gridStartY = fullTop + padY + rowPitch * 0.5f
      var seatIdx = 0
      for (r in 0 until rows) {
        val sy = gridStartY + r * rowPitch
        for (c in 0 until SEAT_COLS) {
          if (seatIdx >= visibleHeads) break
          val sx = gridStartX + c * colPitch

          // 1. Chubby seated shoulders tucked below/behind the big head
          val shW = headR * 1.98f
          val shH = headR * 1.06f
          val shTop = sy + headR * 0.12f
          drawOval(
            color = contourColor,
            topLeft = Offset(sx - shW * 0.5f - outlineStrokeW * 0.6f, shTop - outlineStrokeW * 0.4f),
            size = Size(shW + outlineStrokeW * 1.2f, shH + outlineStrokeW),
          )
          drawOval(
            color = midBodyColor,
            topLeft = Offset(sx - shW * 0.5f, shTop),
            size = Size(shW, shH),
          )

          // 2. Dark shadow/contour rim around the big head so all 4 heads in a row stay crisp
          val headCenter = Offset(sx, sy - headR * 0.06f)
          drawCircle(
            color = contourColor,
            radius = headR + outlineStrokeW * 0.9f,
            center = Offset(headCenter.x, headCenter.y + headR * 0.12f),
          )

          // 3. Large chubby 3D glossy head
          drawCircle(
            brush =
              Brush.radialGradient(
                colors = listOf(highlightPeak, vehicle.color.roofLight, midBodyColor, edgeShadeColor),
                center = Offset(headCenter.x - headR * 0.28f, headCenter.y - headR * 0.30f),
                radius = (headR * 1.38f).coerceAtLeast(1f),
              ),
            radius = headR,
            center = headCenter,
          )
          drawCircle(
            color = contourColor.copy(alpha = 0.72f),
            radius = headR,
            center = headCenter,
            style = Stroke(width = outlineStrokeW),
          )

          // 4. Glossy specular crown shine
          drawCircle(
            color = Color.White.copy(alpha = 0.88f),
            radius = shineR,
            center = Offset(headCenter.x - headR * 0.30f, headCenter.y - headR * 0.32f),
          )
          seatIdx++
        }
      }
    }
  }

  // Soft matching sunroof bezel rim
  drawRoundRect(
    color = vehicle.color.roofLight.copy(alpha = 0.62f),
    topLeft = Offset(curLeft, curTop),
    size = Size(curW, curH),
    cornerRadius = cornerRadius,
    style = Stroke(width = (unit * 0.14f).coerceAtLeast(1.0.dp.toPx())),
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
  val busLongPx = (v.lengthInCells * stridePx - gapPx * 0.35f).coerceAtLeast(stridePx * 0.90f)
  val busShortPx = (v.widthInCells * stridePx - gapPx * 0.20f).coerceAtLeast(stridePx * 0.60f)

  val isStaticHorizontal =
    angleOverrideDeg == null && (v.direction == Direction.LEFT || v.direction == Direction.RIGHT)
  if (isStaticHorizontal) {
    val horizSprite = assets.getBusSprite(v.color, BusView.HORIZ)
    if (horizSprite != null) {
      val drawW = busLongPx * v.squashFactor
      val drawH = busShortPx * v.squashFactor
      val flipRight = v.direction == Direction.RIGHT

      withTransform({
        translate(center.x, center.y)
        if (flipRight) scale(scaleX = -1f, scaleY = 1f, pivot = Offset.Zero)
      }) {
        drawOval(
          color = Color.Black.copy(alpha = 0.24f),
          topLeft = Offset(-drawW * 0.46f, -drawH * 0.34f + 2.5.dp.toPx()),
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
      normAngle in 20f..160f
    } else {
      v.direction == Direction.DOWN ||
        v.direction == Direction.DOWN_LEFT ||
        v.direction == Direction.DOWN_RIGHT
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
  val drawW = busShortPx * v.squashFactor
  val drawH = busLongPx * v.squashFactor
  val flashRed = v.flashRedTimer > 0f
  val passengerSprite = assets.getPassengerSprite(v.color)

  withTransform({
    translate(center.x, center.y)
    rotate(rotationDeg, pivot = Offset.Zero)
  }) {
    drawOval(
      color = Color.Black.copy(alpha = 0.22f),
      topLeft = Offset(-drawW * 0.42f, -drawH * 0.42f + 2.5.dp.toPx()),
      size = Size(drawW * 0.88f, drawH * 0.88f),
    )
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

private fun DrawScope.drawTutorialHand(
  handBitmap: ImageBitmap,
  targetCenter: Offset,
  cellPx: Float,
  tapProgress: Float,
) {
  val handW = maxOf(54.dp.toPx(), cellPx * 1.12f)
  val handH = handW * (handBitmap.height.toFloat() / handBitmap.width.toFloat())
  val hoverOffsetX = (12f - 10f * tapProgress).dp.toPx()
  val hoverOffsetY = (-8f + 10f * tapProgress).dp.toPx()
  val pressScale = 1.06f - 0.14f * tapProgress
  val pressTilt = 4f - 7f * tapProgress
  val tipX = targetCenter.x + hoverOffsetX
  val tipY = targetCenter.y + hoverOffsetY
  val dstLeft = -(handW * 0.10f)
  val dstTop = -(handH * 0.10f)

  withTransform({
    translate(left = tipX, top = tipY)
    rotate(degrees = pressTilt, pivot = Offset.Zero)
    scale(scaleX = pressScale, scaleY = pressScale, pivot = Offset.Zero)
  }) {
    drawImage(
      image = handBitmap,
      srcOffset = IntOffset.Zero,
      srcSize = IntSize(handBitmap.width, handBitmap.height),
      dstOffset = IntOffset(dstLeft.roundToInt(), dstTop.roundToInt()),
      dstSize = IntSize(handW.roundToInt().coerceAtLeast(1), handH.roundToInt().coerceAtLeast(1)),
    )
  }
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
  isOrthographicLot: Boolean = false,
) {
  val assets = rememberAssetManager()
  val textMeasurer = rememberTextMeasurer()

  Canvas(modifier = modifier.fillMaxSize()) {
    val w = size.width
    val h = size.height

    val lotTop = h * 0.452f
    val lotH = h * 0.455f
    val m =
      computeLotMetrics(
        w,
        lotH,
        gridCols,
        gridRows,
        density,
        isOrthographic = isOrthographicLot,
      )
    val cellPx = m.cellPx
    val gapPx = m.gapPx

    val entryLaneY = h * 0.448f
    val rightGapY = h * 0.437f
    val leftRoadX = w * 0.088f
    val rightRoadX = w * 0.912f
    val botRoadY = h * 0.895f
    val cornerRadiusPx = (18f / 360f) * w

    val bayW = maxOf(w * 0.095f, 34.dp.toPx())
    val bayH = maxOf(h * 0.090f, 64.dp.toPx())
    val closeFraction = DEPART_CLOSE_FRACTION

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
          val parkedSprite = assets.getParkedBusSprite(v.color, v.capacity)
          val slotLayout =
            computeParkedSlotBusLayout(
              canvasW = w,
              canvasH = h,
              densityPxPerDp = 1.dp.toPx(),
              slotIndex = v.slotIndex,
              capacity = v.capacity,
            )
          val pullOutX = slotLayout.centerX + (bayW * 0.72f) * (localT * localT)
          val pullOutY = slotLayout.centerY + (exitLaneY - slotLayout.centerY) * (localT * localT * (3f - 2f * localT))
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
            val rotDeg = slotLayout.tiltDeg * (1f - localT)
            val drawW = slotLayout.drawW.roundToInt().coerceAtLeast(1)
            val drawH = slotLayout.drawH.roundToInt().coerceAtLeast(1)
            withTransform({
              translate(pullOutX, pullOutY)
              rotate(rotDeg, pivot = Offset.Zero)
            }) {
              drawImage(
                image = parkedSprite,
                dstOffset = IntOffset(-drawW / 2, -drawH / 2),
                dstSize = IntSize(drawW, drawH),
              )
            }
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

      // Drive cleanly straight out along the lot exit vector onto the outer perimeter road, then up to slot
      val (sdx, sdy) =
        if (isOrthographicLot) v.direction.dx to v.direction.dy
        else isoScreenDirection(v.direction)
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

      val bayEntryOffsetX = -sin(SLOT_TILT_DEG * PI / 180.0).toFloat() * (entryLaneY - slotCy) * 0.42f
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
            add(Offset(slotCx + bayEntryOffsetX, entryLaneY))
            add(Offset(slotCx, slotCy))
          }
        )

      val curvedPath = smoothCornerPolyline(rawWaypoints, radiusPx = cornerRadiusPx, arcSteps = 8)
      val rawP = v.animProgress.coerceIn(0f, 1f)
      val easedT = rawP * rawP * (3f - 2f * rawP)
      val sampled = samplePolyline(curvedPath, easedT)

      val trailWindowPx = (240f / 720f) * w
      drawTimedDotTrail(
        points = curvedPath,
        currentDistPx = sampled.distPx,
        windowPx = trailWindowPx,
        dotSpacingPx = 7.5.dp.toPx(),
        dotRadiusPx = 2.4.dp.toPx(),
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

      if (!isOrthographicLot && approachT < 0.20f && absDx > 0.86f && abs(tangDy) < 0.28f && sideSprite != null) {
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
      } else if (approachT < 0.20f) {
        if (isOrthographicLot) {
          drawOrthoLotBusSprite(
            assets = assets,
            textMeasurer = textMeasurer,
            v = v,
            center = sampled.pos,
            cellPx = curStride,
            angleOverrideDeg = blendedAngle,
          )
        } else {
          val driveIsoView =
            when {
              tangDx >= 0f && tangDy < 0f -> BusView.ISO_1
              tangDx >= 0f && tangDy >= 0f -> BusView.ISO_2
              tangDx < 0f && tangDy >= 0f -> BusView.ISO_3
              else -> BusView.ISO_4
            }
          val isoSprite = assets.getBestIsoBusSprite(v.color, driveIsoView)
          if (isoSprite != null) {
            val driveDrawSize = curStride * (IsoProjection.SPRITE_SIZE_PX / IsoProjection.PX_PER_CELL)
            drawSpriteUniform(
              sprite = isoSprite,
              missingLabel = "Missing bus_${v.color.assetKey}_iso",
              textMeasurer = textMeasurer,
              center = sampled.pos,
              maxW = driveDrawSize,
              maxH = driveDrawSize,
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
        }
      } else {
        // Pull cleanly into the waiting slot with the exact slot-aligned parked bus sprite (zero intermediate sprite pop)
        val parkedSprite = assets.getParkedBusSprite(v.color, v.capacity)
        val targetLayout =
          computeParkedSlotBusLayout(
            canvasW = w,
            canvasH = h,
            densityPxPerDp = 1.dp.toPx(),
            slotIndex = v.slotIndex,
            capacity = v.capacity,
          )
        val settleT = ((approachT - 0.20f) / 0.80f).coerceIn(0f, 1f)
        val startScale = 0.88f
        val curScale = startScale + (1f - startScale) * settleT
        val drawW = (targetLayout.drawW * curScale).roundToInt().coerceAtLeast(1)
        val drawH = (targetLayout.drawH * curScale).roundToInt().coerceAtLeast(1)
        val drawX = sampled.pos.x + (targetLayout.centerX - sampled.pos.x) * settleT
        val drawY = sampled.pos.y + (targetLayout.centerY - sampled.pos.y) * settleT
        val rotDeg = targetLayout.tiltDeg * settleT + (blendedAngle + 90f) * (1f - settleT)
        withTransform({
          translate(drawX, drawY)
          rotate(rotDeg, pivot = Offset.Zero)
        }) {
          drawImage(
            image = parkedSprite,
            dstOffset = IntOffset(-drawW / 2, -drawH / 2),
            dstSize = IntSize(drawW, drawH),
          )
        }
      }
    }

    // 2. Jumping passengers (arcing fast and smoothly from track into parked bus open roof)
    val jumpColWidth = w * 0.0405f
    for (jump in jumpingPassengers) {
      val sprite = assets.getPassengerSprite(jump.color) ?: continue
      val (sCols, sRows) = assets.getPassengerGrid(jump.color)
      val p = jump.progress.coerceIn(0f, 1f)
      val sx = jump.startNormX * w
      val sy = jump.startNormY * h
      val ex = slotCenterXNorm(jump.targetSlotIndex) * w
      val ey = SLOT_CENTER_Y_NORM * h - bayH * 0.06f

      val arcY = sin(p * PI).toFloat() * h * 0.065f
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

    // 3. Confetti, collision smoke puffs, impact sparks & dust particles
    val smokeSheet = assets.getExhaustPuffSheet()
    for (pt in particles) {
      val alpha = (pt.life / pt.maxLife).coerceIn(0f, 1f)
      val px = pt.x * w
      val py = pt.y * h
      when {
        pt.isConfetti -> {
          rotate(degrees = pt.life * 360f, pivot = Offset(px, py)) {
            drawRect(
              color = pt.color.copy(alpha = alpha),
              topLeft = Offset(px - pt.sizeDp.dp.toPx() * 0.5f, py - pt.sizeDp.dp.toPx() * 0.35f),
              size = Size(pt.sizeDp.dp.toPx(), pt.sizeDp.dp.toPx() * 0.65f),
            )
          }
        }
        pt.isSmokePuff -> {
          val ageProgress = (1f - alpha).coerceIn(0f, 1f)
          val puffSize = pt.sizeDp.dp.toPx() * (0.65f + ageProgress * 0.72f)
          val puffAlpha = (alpha * 1.15f).coerceIn(0f, 0.94f)
          if (smokeSheet != null) {
            val frameIdx = (ageProgress * 8f).roundToInt().coerceIn(0, 8)
            drawStripFrame(
              sheet = smokeSheet,
              frameCount = 9,
              frameIndex = frameIdx,
              center = Offset(px, py),
              maxW = puffSize,
              maxH = puffSize,
              alpha = puffAlpha,
            )
          } else {
            drawCircle(
              color = pt.color.copy(alpha = puffAlpha * 0.85f),
              radius = puffSize * 0.45f,
              center = Offset(px, py),
            )
          }
        }
        pt.isSpark -> {
          val len = pt.sizeDp.dp.toPx() * (0.8f + alpha * 0.9f)
          val speed = hypot(pt.vx, pt.vy).coerceAtLeast(0.001f)
          val dx = (pt.vx / speed) * len
          val dy = (pt.vy / speed) * len
          drawLine(
            color = pt.color.copy(alpha = alpha),
            start = Offset(px - dx * 0.4f, py - dy * 0.4f),
            end = Offset(px + dx, py + dy),
            strokeWidth = (2.4.dp.toPx() * alpha).coerceAtLeast(1.dp.toPx()),
            cap = StrokeCap.Round,
          )
          drawCircle(
            color = Color.White.copy(alpha = alpha * 0.9f),
            radius = (1.6.dp.toPx() * alpha).coerceAtLeast(0.6f),
            center = Offset(px, py),
          )
        }
        else -> {
          drawCircle(
            color = pt.color.copy(alpha = alpha * 0.85f),
            radius = pt.sizeDp.dp.toPx() * 0.5f,
            center = Offset(px, py),
          )
        }
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
