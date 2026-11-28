package com.example.game

import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PointF
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.toArgb
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Shared isometric diamond basis vectors matching the game parking lot camera (~8° clockwise tilt):
 * - +u (RIGHT / UP_RIGHT) points up-right (~31°)
 * - +v (DOWN / DOWN_RIGHT) points down-right (~47°)
 */
object IsoProjection {
  const val UX = 0.84f
  const val UY = -0.50f
  const val VX = 0.64f
  const val VY = 0.69f
  const val SPRITE_SIZE_PX = 512
  const val PX_PER_CELL = 172f
}

object OrthoProjection {
  const val SPRITE_SIZE_PX = 384
  const val PX_PER_CELL = 152f
}

/**
 * Generates crisp, high-resolution, 3D-shaded isometric miniature bus sprites (`ISO_1..4`)
 * with rounded lofted corners, glossy roof bevels, recessed sapphire-tinted windows,
 * elliptical 3D tires/hubcaps, headlights/taillights, mirrors, and isometric white roof arrows.
 */
object IsoBusSpriteFactory {

  private data class Vec2(val l: Float, val w: Float)

  private data class Palette(
    val roofBright: Int,
    val roofMain: Int,
    val sideSun: Int,
    val sideShade: Int,
    val deepContour: Int,
    val taillightGlow: Int,
  )

  fun parkedBusTierForCapacity(capacity: Int): Int =
    when {
      capacity <= 16 -> 0
      capacity <= 24 -> 1
      else -> 2
    }

  fun parkedBusHeightForCapacity(capacity: Int): Int =
    when (parkedBusTierForCapacity(capacity)) {
      0 -> 490
      1 -> 565
      else -> 630
    }

  fun renderParkedBus(busColor: VehicleColor, capacity: Int = 16): ImageBitmap {
    val bmpW = 256
    val bmpH = parkedBusHeightForCapacity(capacity)
    val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val pal = paletteFor(busColor)
    val bodyLeft = 10f
    val bodyTop = 8f
    val bodyRight = bmpW - 10f
    val bodyBottom = bmpH - 8f
    val bodyW = bodyRight - bodyLeft
    val bodyH = bodyBottom - bodyTop
    val bodyCorner = bodyW * 0.22f

    // 1. Soft contact shadow underneath the parked bus
    val shadowPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(95, 8, 14, 28)
        maskFilter = BlurMaskFilter(9f, BlurMaskFilter.Blur.NORMAL)
      }
    canvas.drawRoundRect(
      bodyLeft + 3f,
      bodyTop + 6f,
      bodyRight + 3f,
      bodyBottom + 7f,
      bodyCorner,
      bodyCorner,
      shadowPaint,
    )

    // 2. Wheels tucked cleanly inside lower body wheel arches
    val wheelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(22, 26, 36) }
    val wheelHighlight = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(64, 74, 94) }
    val wheelH = bodyH * 0.14f
    val wheelW = 7f
    for (wy in floatArrayOf(bodyTop + bodyH * 0.17f, bodyTop + bodyH * 0.69f)) {
      canvas.drawRoundRect(bodyLeft - 1.5f, wy, bodyLeft + wheelW, wy + wheelH, 4f, 4f, wheelPaint)
      canvas.drawRoundRect(bodyLeft - 0.5f, wy + 4f, bodyLeft + 2.5f, wy + wheelH - 4f, 2f, 2f, wheelHighlight)
      canvas.drawRoundRect(bodyRight - wheelW, wy, bodyRight + 1.5f, wy + wheelH, 4f, 4f, wheelPaint)
      canvas.drawRoundRect(bodyRight - 2.5f, wy + 4f, bodyRight + 0.5f, wy + wheelH - 4f, 2f, 2f, wheelHighlight)
    }

    // 3. Tiny side mirrors near front/lower body corners
    val mirrorPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.sideShade }
    val mirrorRim =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.5f
        color = pal.deepContour
      }
    val mirrorY = bodyBottom - bodyH * 0.15f
    canvas.drawRoundRect(bodyLeft - 7f, mirrorY, bodyLeft + 5f, mirrorY + 18f, 6f, 6f, mirrorPaint)
    canvas.drawRoundRect(bodyLeft - 7f, mirrorY, bodyLeft + 5f, mirrorY + 18f, 6f, 6f, mirrorRim)
    canvas.drawRoundRect(bodyRight - 5f, mirrorY, bodyRight + 7f, mirrorY + 18f, 6f, 6f, mirrorPaint)
    canvas.drawRoundRect(bodyRight - 5f, mirrorY, bodyRight + 7f, mirrorY + 18f, 6f, 6f, mirrorRim)

    // 4. Outer 3D bus chassis shell
    val chassisPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            bodyLeft,
            bodyTop,
            bodyRight,
            bodyBottom,
            intArrayOf(pal.roofBright, pal.sideSun, pal.sideShade),
            floatArrayOf(0f, 0.50f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRoundRect(bodyLeft, bodyTop, bodyRight, bodyBottom, bodyCorner, bodyCorner, chassisPaint)

    val outlinePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4f
        color = pal.deepContour
      }
    canvas.drawRoundRect(bodyLeft, bodyTop, bodyRight, bodyBottom, bodyCorner, bodyCorner, outlinePaint)

    // 5. Recessed front/bottom windshield cap + sleek side window strips
    val sideWinPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(22, 38, 70) }
    val winTopStart = bodyTop + 34f
    val winSegH = (bodyH - 120f) / 3.4f
    for (k in 0 until 3) {
      val wt = winTopStart + k * (winSegH + 10f)
      canvas.drawRoundRect(bodyLeft + 2f, wt, bodyLeft + 9f, wt + winSegH, 4f, 4f, sideWinPaint)
      canvas.drawRoundRect(bodyRight - 9f, wt, bodyRight - 2f, wt + winSegH, 4f, 4f, sideWinPaint)
    }

    val glassPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            bodyLeft,
            bodyBottom - 56f,
            bodyRight,
            bodyBottom - 16f,
            AndroidColor.rgb(22, 42, 78),
            AndroidColor.rgb(52, 108, 178),
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRoundRect(
      bodyLeft + 18f,
      bodyBottom - 44f,
      bodyRight - 18f,
      bodyBottom - 14f,
      14f,
      14f,
      glassPaint,
    )
    canvas.drawLine(
      bodyLeft + 34f,
      bodyBottom - 26f,
      bodyLeft + 76f,
      bodyBottom - 31f,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(115, 195, 236, 255)
        strokeWidth = 4.5f
        strokeCap = Paint.Cap.ROUND
      },
    )

    // Bright glossy nose/bumper specular strip along bottom front cap
    val lightPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.taillightGlow }
    canvas.drawRoundRect(bodyLeft + 20f, bodyBottom - 10f, bodyLeft + 48f, bodyBottom - 2f, 4f, 4f, lightPaint)
    canvas.drawRoundRect(bodyRight - 48f, bodyBottom - 10f, bodyRight - 20f, bodyBottom - 2f, 4f, 4f, lightPaint)

    // 6. Clean glossy top roof deck (wide deck for prominent 4-column seated cabin)
    val roofLeft = bodyLeft + 8f
    val roofTop = bodyTop + 14f
    val roofRight = bodyRight - 8f
    val roofBottom = bodyBottom - 54f
    val roofCorner = (roofRight - roofLeft) * 0.18f

    val roofPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            roofLeft,
            roofTop,
            roofRight,
            roofBottom,
            intArrayOf(pal.roofBright, pal.roofMain, lerpColor(pal.roofMain, pal.sideSun, 0.25f)),
            floatArrayOf(0f, 0.55f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRoundRect(roofLeft, roofTop, roofRight, roofBottom, roofCorner, roofCorner, roofPaint)

    // Crisp specular highlight rim on the roof
    val roofRimPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 3.5f
        color = lerpColor(pal.roofBright, AndroidColor.WHITE, 0.45f)
      }
    canvas.drawRoundRect(roofLeft, roofTop, roofRight, roofBottom, roofCorner, roofCorner, roofRimPaint)

    // 7. Wide matching-color recessed sunroof panel (clean when empty; reveals 4-across chubby seats when boarding)
    val sunLeft = roofLeft + 8f
    val sunTop = roofTop + 10f
    val sunRight = roofRight - 8f
    val sunBottom = roofBottom - 16f
    val sunCorner = (sunRight - sunLeft) * 0.14f

    val sunroofFill =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            sunLeft,
            sunTop,
            sunRight,
            sunBottom,
            intArrayOf(
              lerpColor(pal.roofMain, pal.sideSun, 0.30f),
              pal.roofMain,
              lerpColor(pal.roofMain, pal.roofBright, 0.38f),
            ),
            floatArrayOf(0f, 0.5f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawRoundRect(sunLeft, sunTop, sunRight, sunBottom, sunCorner, sunCorner, sunroofFill)

    val sunroofInnerRim =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.6f
        color = lerpColor(pal.sideShade, pal.roofMain, 0.35f)
      }
    canvas.drawRoundRect(sunLeft, sunTop, sunRight, sunBottom, sunCorner, sunCorner, sunroofInnerRim)

    return bmp.asImageBitmap()
  }

  fun renderIsoBus(busColor: VehicleColor, view: BusView): ImageBitmap {
    val size = IsoProjection.SPRITE_SIZE_PX
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val cx = size * 0.5f
    val cy = size * 0.5f
    val pxPerCell = IsoProjection.PX_PER_CELL

    val (fx, fy, rx, ry) =
      when (view) {
        BusView.ISO_1 -> floatArrayOf(IsoProjection.UX, IsoProjection.UY, IsoProjection.VX, IsoProjection.VY) // ↗
        BusView.ISO_2 -> floatArrayOf(IsoProjection.VX, IsoProjection.VY, -IsoProjection.UX, -IsoProjection.UY) // ↘
        BusView.ISO_3 -> floatArrayOf(-IsoProjection.UX, -IsoProjection.UY, -IsoProjection.VX, -IsoProjection.VY) // ↙
        else -> floatArrayOf(-IsoProjection.VX, -IsoProjection.VY, IsoProjection.UX, IsoProjection.UY) // ↖ (ISO_4)
      }

    fun proj(l: Float, w: Float, z: Float): PointF {
      val sx = cx + (l * fx + w * rx) * pxPerCell
      val sy = cy + (l * fy + w * ry - (z - 0.16f) * 0.90f) * pxPerCell
      return PointF(sx, sy)
    }

    val pal = paletteFor(busColor)
    val halfL = 0.87f
    val halfW = 0.42f
    val cornerR = 0.145f
    val zBase = 0.085f
    val zMid = 0.25f
    val zShoulder = 0.55f
    val zRoof = 0.675f

    val perimeter = buildRoundedRectLoop(halfL, halfW, cornerR, cornerSteps = 7)
    val n = perimeter.size

    // 1. Soft isometric contact shadow underneath the bus
    run {
      val shadowPath = Path()
      perimeter.forEachIndexed { idx, p ->
        val sp = proj(p.l * 1.06f + 0.02f, p.w * 1.08f + 0.02f, 0.0f)
        if (idx == 0) shadowPath.moveTo(sp.x, sp.y + 4f) else shadowPath.lineTo(sp.x, sp.y + 4f)
      }
      shadowPath.close()
      val shadowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          this.color = AndroidColor.argb(72, 10, 18, 34)
          maskFilter = BlurMaskFilter(11f, BlurMaskFilter.Blur.NORMAL)
        }
      canvas.drawPath(shadowPath, shadowPaint)
    }

    // 2. Dark underbody shadow plate (makes chassis sit cleanly on tires)
    run {
      val basePath = Path()
      perimeter.forEachIndexed { idx, p ->
        val pt = proj(p.l * 0.94f, p.w * 0.94f, zBase - 0.01f)
        if (idx == 0) basePath.moveTo(pt.x, pt.y) else basePath.lineTo(pt.x, pt.y)
      }
      basePath.close()
      val basePaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          this.color = AndroidColor.rgb(20, 26, 40)
        }
      canvas.drawPath(basePath, basePaint)
    }

    // 3. Draw back wheels slightly peeking? (Mostly hidden on iso side away from camera)
    // Draw main extruded lower-to-shoulder body walls for all camera-facing segments
    val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val seamPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
      }

    // Determine camera-facing side sign (+1 -> +w faces camera, -1 -> -w faces camera)
    val sideFacesCam = ry > 0f
    val sideSign = if (sideFacesCam) 1f else -1f
    val frontFacesCam = fy > 0f // true for ISO_2 (↘) and ISO_3 (↙)

    // Precompute screen polygons for lofted wall segments
    for (i in 0 until n) {
      val a = perimeter[i]
      val b = perimeter[(i + 1) % n]
      val midL = (a.l + b.l) * 0.5f
      val midW = (a.w + b.w) * 0.5f

      // Approximate outward direction in screen space
      val outL = b.w - a.w
      val outW = -(b.l - a.l)
      val outLen = hypot(outL.toDouble(), outW.toDouble()).toFloat().coerceAtLeast(1e-4f)
      val nl = outL / outLen
      val nw = outW / outLen
      val screenNx = nl * fx + nw * rx
      val screenNy = nl * fy + nw * ry

      // Only skip segments strictly facing straight away from camera
      if (screenNy < -0.26f) continue

      // Lighting factor: sunlit from upper-left (-x, -y) -> left-facing planes are brighter than right-facing planes
      val sunDot = (-screenNx * 0.80f + screenNy * 0.15f).coerceIn(-1f, 1f)
      val wallColor =
        lerpColor(
          pal.sideShade,
          pal.sideSun,
          ((sunDot + 0.7f) / 1.4f).coerceIn(0f, 1f),
        )
      val lowerColor = lerpColor(wallColor, pal.deepContour, 0.24f)

      val p0Bot = proj(a.l * 0.97f, a.w * 0.97f, zBase)
      val p1Bot = proj(b.l * 0.97f, b.w * 0.97f, zBase)
      val p0Mid = proj(a.l * 1.005f, a.w * 1.005f, zMid)
      val p1Mid = proj(b.l * 1.005f, b.w * 1.005f, zMid)
      val p0Top = proj(a.l * 0.975f, a.w * 0.975f, zShoulder)
      val p1Top = proj(b.l * 0.975f, b.w * 0.975f, zShoulder)
      val p0Roof = proj(a.l * 0.86f, a.w * 0.84f, zRoof)
      val p1Roof = proj(b.l * 0.86f, b.w * 0.84f, zRoof)

      // Lower skirt quad (zBase -> zMid)
      val lowerPath =
        Path().apply {
          moveTo(p0Bot.x, p0Bot.y)
          lineTo(p1Bot.x, p1Bot.y)
          lineTo(p1Mid.x, p1Mid.y)
          lineTo(p0Mid.x, p0Mid.y)
          close()
        }
      wallPaint.shader =
        LinearGradient(
          p0Mid.x,
          p0Mid.y,
          p0Bot.x,
          p0Bot.y,
          wallColor,
          lowerColor,
          Shader.TileMode.CLAMP,
        )
      canvas.drawPath(lowerPath, wallPaint)
      seamPaint.color = lowerColor
      canvas.drawPath(lowerPath, seamPaint)

      // Upper body wall quad (zMid -> zShoulder)
      val upperPath =
        Path().apply {
          moveTo(p0Mid.x, p0Mid.y)
          lineTo(p1Mid.x, p1Mid.y)
          lineTo(p1Top.x, p1Top.y)
          lineTo(p0Top.x, p0Top.y)
          close()
        }
      wallPaint.shader =
        LinearGradient(
          p0Top.x,
          p0Top.y,
          p0Mid.x,
          p0Mid.y,
          lerpColor(wallColor, pal.roofMain, 0.35f),
          wallColor,
          Shader.TileMode.CLAMP,
        )
      canvas.drawPath(upperPath, wallPaint)
      seamPaint.color = wallColor
      canvas.drawPath(upperPath, seamPaint)

      // Roof bevel shoulder quad (zShoulder -> zRoof)
      val bevelPath =
        Path().apply {
          moveTo(p0Top.x, p0Top.y)
          lineTo(p1Top.x, p1Top.y)
          lineTo(p1Roof.x, p1Roof.y)
          lineTo(p0Roof.x, p0Roof.y)
          close()
        }
      val bevelTopCol =
        if (screenNx < 0f || midL < 0f && !frontFacesCam) pal.roofBright else lerpColor(pal.roofMain, pal.roofBright, 0.35f)
      wallPaint.shader =
        LinearGradient(
          p0Roof.x,
          p0Roof.y,
          p0Top.x,
          p0Top.y,
          bevelTopCol,
          lerpColor(wallColor, pal.roofMain, 0.55f),
          Shader.TileMode.CLAMP,
        )
      canvas.drawPath(bevelPath, wallPaint)
      seamPaint.color = lerpColor(wallColor, pal.roofMain, 0.50f)
      canvas.drawPath(bevelPath, seamPaint)

      // Bottom outline stroke along the camera-facing silhouette edge
      if (screenNy > 0.10f) {
        val edgePaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = pal.deepContour
            style = Paint.Style.STROKE
            strokeWidth = 2.6f
            strokeCap = Paint.Cap.ROUND
          }
        canvas.drawLine(p0Bot.x, p0Bot.y, p1Bot.x, p1Bot.y, edgePaint)
      }
      // Subtle lower rocker highlight line
      if (abs(midW) > halfW * 0.72f || abs(midL) > halfL * 0.75f) {
        val trimPaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = AndroidColor.argb(65, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.6f
          }
        canvas.drawLine(p0Mid.x, p0Mid.y, p1Mid.x, p1Mid.y, trimPaint)
      }
    }
    wallPaint.shader = null

    // 4. Camera-facing Side Wall Windows & Wheels
    val sideWindowLStarts = floatArrayOf(-0.62f, -0.28f, 0.06f, 0.40f)
    val winLen = 0.25f
    val winZBot = 0.295f
    val winZTop = 0.505f
    for (l0 in sideWindowLStarts) {
      drawSideGlassWindow(
        canvas = canvas,
        lStart = l0,
        lEnd = l0 + winLen,
        wPos = sideSign * (halfW - 0.002f),
        zBot = winZBot,
        zTop = winZTop,
        proj = ::proj,
      )
    }

    // Two 3D wheels along the camera-facing side
    val wheelCentersL = floatArrayOf(-0.50f, 0.50f)
    for (wl in wheelCentersL) {
      drawIsoSideWheel(
        canvas = canvas,
        wheelL = wl,
        wheelW = sideSign * (halfW + 0.006f),
        fwdX = fx,
        fwdY = fy,
        pxPerCell = pxPerCell,
        proj = ::proj,
      )
    }

    // 5. Front Face (Windshield + Headlights + Bumper + Mirrors) OR Rear Face (Rear Window + Taillights)
    if (frontFacesCam) {
      drawFrontWindshieldAndLights(
        canvas = canvas,
        halfL = halfL,
        halfW = halfW,
        pal = pal,
        proj = ::proj,
      )
    } else {
      drawRearWindowAndTaillights(
        canvas = canvas,
        halfL = halfL,
        halfW = halfW,
        pal = pal,
        proj = ::proj,
      )
    }

    // 6. Glossy Top Roof Deck
    val roofPath = Path()
    val roofPts = ArrayList<PointF>(n)
    for (i in 0 until n) {
      val p = perimeter[i]
      val rp = proj(p.l * 0.865f, p.w * 0.845f, zRoof)
      roofPts.add(rp)
      if (i == 0) roofPath.moveTo(rp.x, rp.y) else roofPath.lineTo(rp.x, rp.y)
    }
    roofPath.close()

    val rMinX = roofPts.minOf { it.x }
    val rMaxX = roofPts.maxOf { it.x }
    val rMinY = roofPts.minOf { it.y }
    val rMaxY = roofPts.maxOf { it.y }

    val roofPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            rMinX,
            rMinY,
            rMaxX,
            rMaxY,
            intArrayOf(pal.roofBright, pal.roofMain, lerpColor(pal.roofMain, pal.sideSun, 0.35f)),
            floatArrayOf(0f, 0.52f, 1f),
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawPath(roofPath, roofPaint)

    // Soft specular gloss rim on top roof edge
    val roofRimPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.4f
        color = lerpColor(pal.roofBright, AndroidColor.WHITE, 0.42f)
      }
    canvas.drawPath(roofPath, roofRimPaint)

    // Subtle outer contour around roof top to keep bevel crisp
    val roofInnerSeam =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f
        color = AndroidColor.argb(40, 18, 24, 42)
      }
    canvas.drawPath(roofPath, roofInnerSeam)

    // 7. Crisp Isometric White Direction Arrow on Roof
    drawIsometricRoofArrow(canvas = canvas, zRoof = zRoof + 0.006f, proj = ::proj)

    // 8. Front/Side tiny A-pillar mirrors (drawn after roof bevel so far-side mirror peeks neatly)
    if (frontFacesCam) {
      drawMirror(canvas, l = 0.65f, w = sideSign * (halfW + 0.048f), z = 0.41f, pal = pal, proj = ::proj)
    }

    return bmp.asImageBitmap()
  }

  /**
   * Renders a 2.5D top-down + south-tilted miniature bus matching the orthographic parking lot:
   * - Clean 3D extruded body with glossy beveled roof and vibrant colors
   * - Wheels tucked smoothly INSIDE recessed wheel arches along camera-facing sides (never sticking out)
   * - Proportional 2/3/4 side windows, sloped front windshield + lights, and crisp white roof arrow
   */
  fun renderOrthoBus(
    busColor: VehicleColor,
    angleDeg: Float,
    lengthInCells: Float,
    widthInCells: Float = 0.84f,
  ): ImageBitmap {
    val size = OrthoProjection.SPRITE_SIZE_PX
    val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bmp)

    val cx = size * 0.5f
    val cy = size * 0.5f
    val pxPerCell = OrthoProjection.PX_PER_CELL

    val rad = angleDeg * (PI / 180.0)
    val fx = cos(rad).toFloat()
    val fy = sin(rad).toFloat()
    val rx = -fy
    val ry = fx

    fun proj(l: Float, w: Float, z: Float): PointF {
      val sx = cx + (l * fx + w * rx) * pxPerCell
      val sy = cy + (l * fy + w * ry) * 0.94f * pxPerCell - (z - 0.22f) * 0.45f * pxPerCell
      return PointF(sx, sy)
    }

    val pal = orthoPaletteFor(busColor)
    val halfL = (lengthInCells * 0.470f).coerceIn(0.45f, 0.90f)
    val halfW = (widthInCells * 0.460f).coerceIn(0.32f, 0.44f)
    val cornerR = (halfW * 0.34f).coerceIn(0.11f, 0.16f)
    val zBase = 0.055f
    val zMid = 0.19f
    val zShoulder = 0.47f
    val zRoof = 0.58f

    val perimeter = buildRoundedRectLoop(halfL, halfW, cornerR, cornerSteps = 7)
    val n = perimeter.size

    // 1. Soft contact shadow underneath the bus
    run {
      val shadowPath = Path()
      perimeter.forEachIndexed { idx, p ->
        val sp = proj(p.l * 1.04f, p.w * 1.04f, 0.0f)
        if (idx == 0) shadowPath.moveTo(sp.x, sp.y + 4f) else shadowPath.lineTo(sp.x, sp.y + 4f)
      }
      shadowPath.close()
      val shadowPaint =
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          this.color = AndroidColor.argb(82, 10, 16, 30)
          maskFilter = BlurMaskFilter(7.5f, BlurMaskFilter.Blur.NORMAL)
        }
      canvas.drawPath(shadowPath, shadowPaint)
    }

    // 2. Dark underbody shadow plate
    run {
      val basePath = Path()
      perimeter.forEachIndexed { idx, p ->
        val pt = proj(p.l * 0.94f, p.w * 0.94f, zBase - 0.008f)
        if (idx == 0) basePath.moveTo(pt.x, pt.y) else basePath.lineTo(pt.x, pt.y)
      }
      basePath.close()
      canvas.drawPath(
        basePath,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = AndroidColor.rgb(20, 25, 38) },
      )
    }

    val wallPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { style = Paint.Style.FILL }
    val seamPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.4f
      }

    val sideFacesCam = ry >= 0f
    val sideSign = if (sideFacesCam) 1f else -1f
    val frontFacesCam = fy > 0.18f
    val rearFacesCam = fy < -0.18f

    // 3. Lofted 3D walls for downward-facing surfaces
    for (i in 0 until n) {
      val a = perimeter[i]
      val b = perimeter[(i + 1) % n]
      val midL = (a.l + b.l) * 0.5f
      val midW = (a.w + b.w) * 0.5f

      val outL = b.w - a.w
      val outW = -(b.l - a.l)
      val outLen = hypot(outL.toDouble(), outW.toDouble()).toFloat().coerceAtLeast(1e-4f)
      val nl = outL / outLen
      val nw = outW / outLen
      val screenNx = nl * fx + nw * rx
      val screenNy = nl * fy + nw * ry

      if (screenNy < -0.22f) continue

      val sunDot = (-screenNx * 0.45f + screenNy * 0.25f).coerceIn(-1f, 1f)
      val wallColor =
        lerpColor(
          pal.sideShade,
          pal.sideSun,
          ((sunDot + 0.65f) / 1.35f).coerceIn(0f, 1f),
        )
      val lowerColor = lerpColor(wallColor, pal.deepContour, 0.22f)

      val p0Bot = proj(a.l * 0.96f, a.w * 0.96f, zBase)
      val p1Bot = proj(b.l * 0.96f, b.w * 0.96f, zBase)
      val p0Mid = proj(a.l * 1.00f, a.w * 1.00f, zMid)
      val p1Mid = proj(b.l * 1.00f, b.w * 1.00f, zMid)
      val p0Top = proj(a.l * 0.975f, a.w * 0.975f, zShoulder)
      val p1Top = proj(b.l * 0.975f, b.w * 0.975f, zShoulder)
      val p0Roof = proj(a.l * 0.875f, a.w * 0.855f, zRoof)
      val p1Roof = proj(b.l * 0.875f, b.w * 0.855f, zRoof)

      val lowerPath =
        Path().apply {
          moveTo(p0Bot.x, p0Bot.y)
          lineTo(p1Bot.x, p1Bot.y)
          lineTo(p1Mid.x, p1Mid.y)
          lineTo(p0Mid.x, p0Mid.y)
          close()
        }
      wallPaint.shader =
        LinearGradient(
          p0Mid.x,
          p0Mid.y,
          p0Bot.x,
          p0Bot.y,
          wallColor,
          lowerColor,
          Shader.TileMode.CLAMP,
        )
      canvas.drawPath(lowerPath, wallPaint)
      seamPaint.color = lowerColor
      canvas.drawPath(lowerPath, seamPaint)

      val upperPath =
        Path().apply {
          moveTo(p0Mid.x, p0Mid.y)
          lineTo(p1Mid.x, p1Mid.y)
          lineTo(p1Top.x, p1Top.y)
          lineTo(p0Top.x, p0Top.y)
          close()
        }
      wallPaint.shader =
        LinearGradient(
          p0Top.x,
          p0Top.y,
          p0Mid.x,
          p0Mid.y,
          lerpColor(wallColor, pal.roofMain, 0.35f),
          wallColor,
          Shader.TileMode.CLAMP,
        )
      canvas.drawPath(upperPath, wallPaint)
      seamPaint.color = wallColor
      canvas.drawPath(upperPath, seamPaint)

      val bevelPath =
        Path().apply {
          moveTo(p0Top.x, p0Top.y)
          lineTo(p1Top.x, p1Top.y)
          lineTo(p1Roof.x, p1Roof.y)
          lineTo(p0Roof.x, p0Roof.y)
          close()
        }
      val bevelTopCol =
        if (screenNx < 0f || (midL < 0f && !frontFacesCam)) pal.roofBright
        else lerpColor(pal.roofMain, pal.roofBright, 0.32f)
      wallPaint.shader =
        LinearGradient(
          p0Roof.x,
          p0Roof.y,
          p0Top.x,
          p0Top.y,
          bevelTopCol,
          lerpColor(wallColor, pal.roofMain, 0.55f),
          Shader.TileMode.CLAMP,
        )
      canvas.drawPath(bevelPath, wallPaint)
      seamPaint.color = lerpColor(wallColor, pal.roofMain, 0.50f)
      canvas.drawPath(bevelPath, seamPaint)

      if (screenNy > 0.08f) {
        canvas.drawLine(
          p0Bot.x,
          p0Bot.y,
          p1Bot.x,
          p1Bot.y,
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = pal.deepContour
            style = Paint.Style.STROKE
            strokeWidth = 2.0f
            strokeCap = Paint.Cap.ROUND
          },
        )
      }
      if (abs(midW) > halfW * 0.72f || abs(midL) > halfL * 0.75f) {
        canvas.drawLine(
          p0Mid.x,
          p0Mid.y,
          p1Mid.x,
          p1Mid.y,
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = AndroidColor.argb(55, 255, 255, 255)
            style = Paint.Style.STROKE
            strokeWidth = 1.3f
          },
        )
      }
    }
    wallPaint.shader = null

    // 4. Side Windows & Tucked Wheels (only when side wall faces camera, never sticking out on vertical UP/DOWN)
    if (abs(ry) > 0.20f) {
      val winCount =
        when {
          lengthInCells >= 1.65f -> 4
          lengthInCells >= 1.40f -> 3
          else -> 2
        }
      val winSpanTotal = (halfL * 2f - 0.34f).coerceAtLeast(0.50f)
      val winGap = 0.075f
      val winLen = ((winSpanTotal - winGap * (winCount - 1)) / winCount).coerceIn(0.16f, 0.26f)
      val startL = -((winCount * winLen + (winCount - 1) * winGap) * 0.5f)
      for (k in 0 until winCount) {
        val l0 = startL + k * (winLen + winGap)
        drawSideGlassWindow(
          canvas = canvas,
          lStart = l0,
          lEnd = l0 + winLen,
          wPos = sideSign * (halfW - 0.002f),
          zBot = 0.255f,
          zTop = 0.435f,
          proj = ::proj,
        )
      }

      val wheelOffsetL = (halfL * 0.56f).coerceIn(0.28f, 0.50f)
      for (wl in floatArrayOf(-wheelOffsetL, wheelOffsetL)) {
        drawOrthoTuckedWheel(
          canvas = canvas,
          wheelL = wl,
          wheelW = sideSign * (halfW - 0.014f),
          fwdX = fx,
          fwdY = fy,
          pxPerCell = pxPerCell,
          proj = ::proj,
        )
      }
    }

    // 5. Front/Rear Windows & Lights
    if (frontFacesCam) {
      drawOrthoFrontWindshieldAndLights(
        canvas = canvas,
        halfL = halfL,
        halfW = halfW,
        pal = pal,
        proj = ::proj,
      )
    } else if (rearFacesCam) {
      drawOrthoRearWindowAndTaillights(
        canvas = canvas,
        halfL = halfL,
        halfW = halfW,
        pal = pal,
        proj = ::proj,
      )
    }

    // 6. Glossy Top Roof Deck
    val roofPath = Path()
    val roofPts = ArrayList<PointF>(n)
    for (i in 0 until n) {
      val p = perimeter[i]
      val rp = proj(p.l * 0.875f, p.w * 0.855f, zRoof)
      roofPts.add(rp)
      if (i == 0) roofPath.moveTo(rp.x, rp.y) else roofPath.lineTo(rp.x, rp.y)
    }
    roofPath.close()

    val rMinX = roofPts.minOf { it.x }
    val rMaxX = roofPts.maxOf { it.x }
    val rMinY = roofPts.minOf { it.y }
    val rMaxY = roofPts.maxOf { it.y }

    canvas.drawPath(
      roofPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            rMinX,
            rMinY,
            rMaxX,
            rMaxY,
            intArrayOf(pal.roofBright, pal.roofMain, lerpColor(pal.roofMain, pal.sideSun, 0.30f)),
            floatArrayOf(0f, 0.54f, 1f),
            Shader.TileMode.CLAMP,
          )
      },
    )

    // Crisp front windshield brow hint on top nose when bus points UP/AWAY so direction reads cleanly
    if (!frontFacesCam) {
      val browHalfW = halfW * 0.62f
      val browPath =
        Path().apply {
          val p0 = proj(halfL * 0.875f, -browHalfW, zRoof)
          val p1 = proj(halfL * 0.875f, browHalfW, zRoof)
          val p2 = proj(halfL * 0.965f, browHalfW * 0.88f, zShoulder + 0.03f)
          val p3 = proj(halfL * 0.965f, -browHalfW * 0.88f, zShoulder + 0.03f)
          moveTo(p0.x, p0.y)
          lineTo(p1.x, p1.y)
          lineTo(p2.x, p2.y)
          lineTo(p3.x, p3.y)
          close()
        }
      canvas.drawPath(
        browPath,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(24, 42, 74) },
      )
    }

    canvas.drawPath(
      roofPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        color = lerpColor(pal.roofBright, AndroidColor.WHITE, 0.40f)
      },
    )
    canvas.drawPath(
      roofPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.2f
        color = AndroidColor.argb(38, 16, 22, 38)
      },
    )

    // 7. Crisp White Roof Arrow scaled neatly for short/medium/long buses
    drawOrthoRoofArrow(
      canvas = canvas,
      zRoof = zRoof + 0.005f,
      halfL = halfL,
      halfW = halfW,
      proj = ::proj,
    )

    return bmp.asImageBitmap()
  }

  private fun drawSideGlassWindow(
    canvas: Canvas,
    lStart: Float,
    lEnd: Float,
    wPos: Float,
    zBot: Float,
    zTop: Float,
    proj: (Float, Float, Float) -> PointF,
  ) {
    val lMid0 = lStart + (lEnd - lStart) * 0.14f
    val lMid1 = lEnd - (lEnd - lStart) * 0.14f
    val zMidB = zBot + (zTop - zBot) * 0.18f
    val zMidT = zTop - (zTop - zBot) * 0.18f
    val wTop = wPos * 0.978f
    val wBot = wPos * 0.996f

    val path =
      Path().apply {
        val p0 = proj(lMid0, wBot, zBot)
        val p1 = proj(lMid1, wBot, zBot)
        val p2 = proj(lEnd, wBot, zMidB)
        val p3 = proj(lEnd, wTop, zMidT)
        val p4 = proj(lMid1, wTop, zTop)
        val p5 = proj(lMid0, wTop, zTop)
        val p6 = proj(lStart, wTop, zMidT)
        val p7 = proj(lStart, wBot, zMidB)
        moveTo(p0.x, p0.y)
        lineTo(p1.x, p1.y)
        quadTo(proj(lEnd, wBot, zBot).x, proj(lEnd, wBot, zBot).y, p2.x, p2.y)
        lineTo(p3.x, p3.y)
        quadTo(proj(lEnd, wTop, zTop).x, proj(lEnd, wTop, zTop).y, p4.x, p4.y)
        lineTo(p5.x, p5.y)
        quadTo(proj(lStart, wTop, zTop).x, proj(lStart, wTop, zTop).y, p6.x, p6.y)
        lineTo(p7.x, p7.y)
        quadTo(proj(lStart, wBot, zBot).x, proj(lStart, wBot, zBot).y, p0.x, p0.y)
        close()
      }

    val topP = proj((lStart + lEnd) * 0.5f, wTop, zTop)
    val botP = proj((lStart + lEnd) * 0.5f, wBot, zBot)

    val glassPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            topP.x,
            topP.y,
            botP.x,
            botP.y,
            AndroidColor.rgb(24, 40, 72),
            AndroidColor.rgb(38, 76, 128),
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawPath(path, glassPaint)

    // Crisp glossy sky reflection streak inside window
    val glA = proj(lStart + 0.04f, wTop * 0.999f, zTop - 0.04f)
    val glB = proj(lStart + 0.14f, wTop * 0.999f, zTop - 0.04f)
    val glC = proj(lStart + 0.09f, wBot * 0.999f, zBot + 0.05f)
    val glPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(85, 165, 220, 255)
        style = Paint.Style.STROKE
        strokeWidth = 3.2f
        strokeCap = Paint.Cap.ROUND
      }
    canvas.drawLine((glA.x + glB.x) * 0.5f, glA.y, glC.x, glC.y, glPaint)

    val framePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        color = AndroidColor.rgb(14, 22, 38)
      }
    canvas.drawPath(path, framePaint)
  }

  private fun drawIsoSideWheel(
    canvas: Canvas,
    wheelL: Float,
    wheelW: Float,
    fwdX: Float,
    fwdY: Float,
    pxPerCell: Float,
    proj: (Float, Float, Float) -> PointF,
  ) {
    // Dark recessed wheel arch
    val archPath = Path()
    val archSteps = 12
    val archRadiusL = 0.155f
    val archCenterZ = 0.105f
    for (i in 0..archSteps) {
      val theta = PI * i / archSteps
      val dl = -archRadiusL * cos(theta).toFloat()
      val dz = 0.135f * sin(theta).toFloat()
      val pt = proj(wheelL + dl, wheelW * 0.995f, archCenterZ + dz)
      if (i == 0) archPath.moveTo(pt.x, pt.y) else archPath.lineTo(pt.x, pt.y)
    }
    archPath.close()
    val archPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(14, 18, 28) }
    canvas.drawPath(archPath, archPaint)

    // 3D Elliptical rubber tire
    val tirePath = Path()
    val tireSteps = 24
    val tireR = 0.125f
    for (i in 0 until tireSteps) {
      val theta = 2.0 * PI * i / tireSteps
      val dl = tireR * cos(theta).toFloat()
      val dz = tireR * sin(theta).toFloat()
      val pt = proj(wheelL + dl, wheelW + 0.012f * sign(wheelW), 0.115f + dz)
      if (i == 0) tirePath.moveTo(pt.x, pt.y) else tirePath.lineTo(pt.x, pt.y)
    }
    tirePath.close()

    val wc = proj(wheelL, wheelW + 0.012f * sign(wheelW), 0.115f)
    val tirePaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          RadialGradient(
            wc.x - 3f,
            wc.y - 4f,
            tireR * pxPerCell * 1.1f,
            AndroidColor.rgb(54, 62, 82),
            AndroidColor.rgb(20, 24, 34),
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawPath(tirePath, tirePaint)

    val tireRim =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        color = AndroidColor.rgb(12, 16, 24)
      }
    canvas.drawPath(tirePath, tireRim)

    // Silver-gray hubcap center
    val hubPath = Path()
    val hubR = 0.064f
    for (i in 0 until 18) {
      val theta = 2.0 * PI * i / 18
      val dl = hubR * cos(theta).toFloat()
      val dz = hubR * sin(theta).toFloat()
      val pt = proj(wheelL + dl, wheelW + 0.016f * sign(wheelW), 0.115f + dz)
      if (i == 0) hubPath.moveTo(pt.x, pt.y) else hubPath.lineTo(pt.x, pt.y)
    }
    hubPath.close()
    val hubPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            wc.x - fwdX * 8f,
            wc.y - fwdY * 8f - 8f,
            wc.x + fwdX * 8f,
            wc.y + fwdY * 8f + 8f,
            AndroidColor.rgb(218, 228, 245),
            AndroidColor.rgb(118, 134, 162),
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawPath(hubPath, hubPaint)

    val centerDot =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.rgb(56, 68, 92)
      }
    canvas.drawCircle(wc.x, wc.y, 2.8f, centerDot)
  }

  private fun drawFrontWindshieldAndLights(
    canvas: Canvas,
    halfL: Float,
    halfW: Float,
    pal: Palette,
    proj: (Float, Float, Float) -> PointF,
  ) {
    // Wide curved front windshield
    val wHalf = halfW * 0.78f
    val lTop = halfL * 0.942f
    val lBot = halfL * 0.988f
    val zBot = 0.29f
    val zTop = 0.545f

    val wsPath =
      Path().apply {
        val bl = proj(lBot, -wHalf * 0.80f, zBot)
        val br = proj(lBot, wHalf * 0.80f, zBot)
        val mr = proj(lBot, wHalf, zBot + 0.06f)
        val tr = proj(lTop, wHalf * 0.96f, zTop - 0.05f)
        val tcr = proj(lTop, wHalf * 0.76f, zTop)
        val tcl = proj(lTop, -wHalf * 0.76f, zTop)
        val tl = proj(lTop, -wHalf * 0.96f, zTop - 0.05f)
        val ml = proj(lBot, -wHalf, zBot + 0.06f)
        moveTo(bl.x, bl.y)
        lineTo(br.x, br.y)
        quadTo(proj(lBot, wHalf, zBot).x, proj(lBot, wHalf, zBot).y, mr.x, mr.y)
        lineTo(tr.x, tr.y)
        quadTo(proj(lTop, wHalf * 0.96f, zTop).x, proj(lTop, wHalf * 0.96f, zTop).y, tcr.x, tcr.y)
        lineTo(tcl.x, tcl.y)
        quadTo(proj(lTop, -wHalf * 0.96f, zTop).x, proj(lTop, -wHalf * 0.96f, zTop).y, tl.x, tl.y)
        lineTo(ml.x, ml.y)
        quadTo(proj(lBot, -wHalf, zBot).x, proj(lBot, -wHalf, zBot).y, bl.x, bl.y)
        close()
      }

    val topCenter = proj(lTop, 0f, zTop)
    val botCenter = proj(lBot, 0f, zBot)
    val wsPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            topCenter.x - 18f,
            topCenter.y,
            botCenter.x + 18f,
            botCenter.y,
            AndroidColor.rgb(22, 38, 68),
            AndroidColor.rgb(44, 88, 148),
            Shader.TileMode.CLAMP,
          )
      }
    canvas.drawPath(wsPath, wsPaint)

    // Bright glass reflection stripe across front windshield
    val r1 = proj(lTop + 0.01f, -wHalf * 0.45f, zTop - 0.04f)
    val r2 = proj(lBot - 0.01f, -wHalf * 0.12f, zBot + 0.04f)
    val reflPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = AndroidColor.argb(95, 185, 232, 255)
        strokeWidth = 6.5f
        strokeCap = Paint.Cap.ROUND
      }
    canvas.drawLine(r1.x, r1.y, r2.x, r2.y, reflPaint)

    val wsStroke =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.4f
        color = AndroidColor.rgb(14, 22, 38)
      }
    canvas.drawPath(wsPath, wsStroke)

    // Front grille slot between headlights
    val grillePath =
      Path().apply {
        val g0 = proj(halfL * 1.004f, -0.13f, 0.145f)
        val g1 = proj(halfL * 1.004f, 0.13f, 0.145f)
        val g2 = proj(halfL * 1.004f, 0.12f, 0.215f)
        val g3 = proj(halfL * 1.004f, -0.12f, 0.215f)
        moveTo(g0.x, g0.y)
        lineTo(g1.x, g1.y)
        lineTo(g2.x, g2.y)
        lineTo(g3.x, g3.y)
        close()
      }
    canvas.drawPath(
      grillePath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply { color = lerpColor(pal.deepContour, AndroidColor.rgb(28, 36, 52), 0.72f) },
    )

    // Two bright 3D headlights
    for (signW in floatArrayOf(-1f, 1f)) {
      val hw = signW * halfW * 0.64f
      val hlCenter = proj(halfL * 0.996f, hw, 0.185f)
      val hlPath = Path()
      for (i in 0 until 14) {
        val t = 2.0 * PI * i / 14
        val dw = 0.068f * cos(t).toFloat()
        val dz = 0.044f * sin(t).toFloat()
        val pt = proj(halfL * 0.996f, hw + dw, 0.185f + dz)
        if (i == 0) hlPath.moveTo(pt.x, pt.y) else hlPath.lineTo(pt.x, pt.y)
      }
      hlPath.close()
      canvas.drawPath(
        hlPath,
        Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(255, 244, 184) },
      )
      canvas.drawPath(
        hlPath,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 2.0f
          color = pal.deepContour
        },
      )
      canvas.drawCircle(hlCenter.x - 1.5f, hlCenter.y - 1.5f, 2.5f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.WHITE })
    }
  }

  private fun drawRearWindowAndTaillights(
    canvas: Canvas,
    halfL: Float,
    halfW: Float,
    pal: Palette,
    proj: (Float, Float, Float) -> PointF,
  ) {
    val wHalf = halfW * 0.65f
    val lFace = -halfL * 0.968f
    val rwPath =
      Path().apply {
        val p0 = proj(lFace, -wHalf, 0.34f)
        val p1 = proj(lFace, wHalf, 0.34f)
        val p2 = proj(-halfL * 0.935f, wHalf * 0.94f, 0.515f)
        val p3 = proj(-halfL * 0.935f, -wHalf * 0.94f, 0.515f)
        moveTo(p0.x, p0.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        close()
      }
    canvas.drawPath(
      rwPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(24, 40, 70) },
    )
    canvas.drawPath(
      rwPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        color = AndroidColor.rgb(14, 22, 38)
      },
    )

    // Rear glowing taillights
    for (signW in floatArrayOf(-1f, 1f)) {
      val tw = signW * halfW * 0.66f
      val tlPath =
        Path().apply {
          val p0 = proj(-halfL * 0.996f, tw - 0.065f, 0.155f)
          val p1 = proj(-halfL * 0.996f, tw + 0.065f, 0.155f)
          val p2 = proj(-halfL * 0.996f, tw + 0.058f, 0.222f)
          val p3 = proj(-halfL * 0.996f, tw - 0.058f, 0.222f)
          moveTo(p0.x, p0.y)
          lineTo(p1.x, p1.y)
          lineTo(p2.x, p2.y)
          lineTo(p3.x, p3.y)
          close()
        }
      canvas.drawPath(tlPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.taillightGlow })
      canvas.drawPath(
        tlPath,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 1.8f
          color = pal.deepContour
        },
      )
    }
  }

  private fun drawMirror(
    canvas: Canvas,
    l: Float,
    w: Float,
    z: Float,
    pal: Palette,
    proj: (Float, Float, Float) -> PointF,
  ) {
    val armA = proj(l - 0.03f, w * 0.88f, z - 0.02f)
    val armB = proj(l, w, z)
    val armPaint =
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = pal.deepContour
        strokeWidth = 3.5f
        strokeCap = Paint.Cap.ROUND
      }
    canvas.drawLine(armA.x, armA.y, armB.x, armB.y, armPaint)

    val capPath =
      Path().apply {
        val p0 = proj(l - 0.04f, w - 0.026f, z - 0.05f)
        val p1 = proj(l + 0.04f, w + 0.026f, z - 0.05f)
        val p2 = proj(l + 0.04f, w + 0.026f, z + 0.045f)
        val p3 = proj(l - 0.04f, w - 0.026f, z + 0.045f)
        moveTo(p0.x, p0.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        close()
      }
    canvas.drawPath(capPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.roofMain })
    canvas.drawPath(
      capPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        color = pal.deepContour
      },
    )
  }

  private fun drawIsometricRoofArrow(
    canvas: Canvas,
    zRoof: Float,
    proj: (Float, Float, Float) -> PointF,
  ) {
    val pts =
      listOf(
        Vec2(-0.42f, -0.092f),
        Vec2(-0.42f, 0.092f),
        Vec2(0.05f, 0.092f),
        Vec2(0.05f, 0.235f),
        Vec2(0.45f, 0.0f),
        Vec2(0.05f, -0.235f),
        Vec2(0.05f, -0.092f),
      )

    val arrowPath = Path()
    pts.forEachIndexed { idx, v ->
      val pt = proj(v.l, v.w, zRoof)
      if (idx == 0) arrowPath.moveTo(pt.x, pt.y) else arrowPath.lineTo(pt.x, pt.y)
    }
    arrowPath.close()

    // Soft dark drop shadow under arrow for crisp separation on Yellow & White roofs
    val shadowPath = Path()
    pts.forEachIndexed { idx, v ->
      val pt = proj(v.l, v.w, zRoof)
      if (idx == 0) shadowPath.moveTo(pt.x + 1.5f, pt.y + 3.0f) else shadowPath.lineTo(pt.x + 1.5f, pt.y + 3.0f)
    }
    shadowPath.close()
    canvas.drawPath(
      shadowPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.argb(72, 16, 22, 38) },
    )

    // Crisp dark outline + pure white fill
    canvas.drawPath(
      arrowPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 6.0f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = AndroidColor.rgb(24, 32, 52)
      },
    )
    canvas.drawPath(
      arrowPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = AndroidColor.WHITE
      },
    )
  }

  private fun buildRoundedRectLoop(
    halfL: Float,
    halfW: Float,
    radius: Float,
    cornerSteps: Int,
  ): List<Vec2> {
    val r = min(radius, min(halfL * 0.45f, halfW * 0.45f))
    val innerL = halfL - r
    val innerW = halfW - r
    val out = ArrayList<Vec2>(cornerSteps * 4)

    // Walk counter-clockwise in (l, w):
    // Corner 0: (+l, +w) angles 0..PI/2
    // Corner 1: (-l, +w) angles PI/2..PI
    // Corner 2: (-l, -w) angles PI..3PI/2
    // Corner 3: (+l, -w) angles 3PI/2..2PI
    val centers =
      arrayOf(
        Vec2(innerL, innerW),
        Vec2(-innerL, innerW),
        Vec2(-innerL, -innerW),
        Vec2(innerL, -innerW),
      )
    val startAngles = doubleArrayOf(0.0, PI * 0.5, PI, PI * 1.5)

    for (c in 0 until 4) {
      val center = centers[c]
      val a0 = startAngles[c]
      for (s in 0 until cornerSteps) {
        val ang = a0 + (PI * 0.5) * (s.toDouble() / cornerSteps)
        out.add(Vec2(center.l + r * cos(ang).toFloat(), center.w + r * sin(ang).toFloat()))
      }
    }
    return out
  }

  private fun drawOrthoTuckedWheel(
    canvas: Canvas,
    wheelL: Float,
    wheelW: Float,
    fwdX: Float,
    fwdY: Float,
    pxPerCell: Float,
    proj: (Float, Float, Float) -> PointF,
  ) {
    val archPath = Path()
    val archSteps = 12
    val archRadiusL = 0.132f
    val archCenterZ = 0.075f
    for (i in 0..archSteps) {
      val theta = PI * i / archSteps
      val dl = -archRadiusL * cos(theta).toFloat()
      val dz = 0.118f * sin(theta).toFloat()
      val pt = proj(wheelL + dl, wheelW * 0.985f, archCenterZ + dz)
      if (i == 0) archPath.moveTo(pt.x, pt.y) else archPath.lineTo(pt.x, pt.y)
    }
    archPath.close()
    canvas.drawPath(archPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(14, 18, 28) })

    val tirePath = Path()
    val tireSteps = 22
    val tireR = 0.106f
    for (i in 0 until tireSteps) {
      val theta = 2.0 * PI * i / tireSteps
      val dl = tireR * cos(theta).toFloat()
      val dz = tireR * sin(theta).toFloat()
      val pt = proj(wheelL + dl, wheelW + 0.004f * sign(wheelW), 0.088f + dz)
      if (i == 0) tirePath.moveTo(pt.x, pt.y) else tirePath.lineTo(pt.x, pt.y)
    }
    tirePath.close()

    val wc = proj(wheelL, wheelW + 0.004f * sign(wheelW), 0.088f)
    canvas.drawPath(
      tirePath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          RadialGradient(
            wc.x - 2f,
            wc.y - 3f,
            tireR * pxPerCell * 1.1f,
            AndroidColor.rgb(52, 60, 78),
            AndroidColor.rgb(18, 22, 32),
            Shader.TileMode.CLAMP,
          )
      },
    )
    canvas.drawPath(
      tirePath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        color = AndroidColor.rgb(12, 16, 24)
      },
    )

    val hubPath = Path()
    val hubR = 0.054f
    for (i in 0 until 16) {
      val theta = 2.0 * PI * i / 16
      val dl = hubR * cos(theta).toFloat()
      val dz = hubR * sin(theta).toFloat()
      val pt = proj(wheelL + dl, wheelW + 0.006f * sign(wheelW), 0.088f + dz)
      if (i == 0) hubPath.moveTo(pt.x, pt.y) else hubPath.lineTo(pt.x, pt.y)
    }
    hubPath.close()
    canvas.drawPath(
      hubPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            wc.x - fwdX * 6f,
            wc.y - fwdY * 6f - 6f,
            wc.x + fwdX * 6f,
            wc.y + fwdY * 6f + 6f,
            AndroidColor.rgb(214, 224, 242),
            AndroidColor.rgb(112, 128, 156),
            Shader.TileMode.CLAMP,
          )
      },
    )
    canvas.drawCircle(wc.x, wc.y, 2.0f, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(52, 64, 88) })
  }

  private fun drawOrthoFrontWindshieldAndLights(
    canvas: Canvas,
    halfL: Float,
    halfW: Float,
    pal: Palette,
    proj: (Float, Float, Float) -> PointF,
  ) {
    val wHalf = halfW * 0.76f
    val lTop = halfL * 0.90f
    val lBot = halfL * 0.985f
    val zBot = 0.25f
    val zTop = 0.49f

    val wsPath =
      Path().apply {
        val bl = proj(lBot, -wHalf * 0.80f, zBot)
        val br = proj(lBot, wHalf * 0.80f, zBot)
        val mr = proj(lBot, wHalf, zBot + 0.05f)
        val tr = proj(lTop, wHalf * 0.94f, zTop - 0.04f)
        val tcr = proj(lTop, wHalf * 0.74f, zTop)
        val tcl = proj(lTop, -wHalf * 0.74f, zTop)
        val tl = proj(lTop, -wHalf * 0.94f, zTop - 0.04f)
        val ml = proj(lBot, -wHalf, zBot + 0.05f)
        moveTo(bl.x, bl.y)
        lineTo(br.x, br.y)
        quadTo(proj(lBot, wHalf, zBot).x, proj(lBot, wHalf, zBot).y, mr.x, mr.y)
        lineTo(tr.x, tr.y)
        quadTo(proj(lTop, wHalf * 0.94f, zTop).x, proj(lTop, wHalf * 0.94f, zTop).y, tcr.x, tcr.y)
        lineTo(tcl.x, tcl.y)
        quadTo(proj(lTop, -wHalf * 0.94f, zTop).x, proj(lTop, -wHalf * 0.94f, zTop).y, tl.x, tl.y)
        lineTo(ml.x, ml.y)
        quadTo(proj(lBot, -wHalf, zBot).x, proj(lBot, -wHalf, zBot).y, bl.x, bl.y)
        close()
      }

    val topCenter = proj(lTop, 0f, zTop)
    val botCenter = proj(lBot, 0f, zBot)
    canvas.drawPath(
      wsPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        shader =
          LinearGradient(
            topCenter.x - 12f,
            topCenter.y,
            botCenter.x + 12f,
            botCenter.y,
            AndroidColor.rgb(22, 38, 68),
            AndroidColor.rgb(42, 84, 144),
            Shader.TileMode.CLAMP,
          )
      },
    )
    canvas.drawPath(
      wsPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 2.0f
        color = AndroidColor.rgb(14, 22, 38)
      },
    )

    for (signW in floatArrayOf(-1f, 1f)) {
      val hw = signW * halfW * 0.62f
      val hlPath = Path()
      for (i in 0 until 12) {
        val t = 2.0 * PI * i / 12
        val dw = 0.056f * cos(t).toFloat()
        val dz = 0.036f * sin(t).toFloat()
        val pt = proj(halfL * 0.992f, hw + dw, 0.155f + dz)
        if (i == 0) hlPath.moveTo(pt.x, pt.y) else hlPath.lineTo(pt.x, pt.y)
      }
      hlPath.close()
      canvas.drawPath(hlPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(255, 244, 184) })
      canvas.drawPath(
        hlPath,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 1.6f
          color = pal.deepContour
        },
      )
    }
  }

  private fun drawOrthoRearWindowAndTaillights(
    canvas: Canvas,
    halfL: Float,
    halfW: Float,
    pal: Palette,
    proj: (Float, Float, Float) -> PointF,
  ) {
    val wHalf = halfW * 0.62f
    val rwPath =
      Path().apply {
        val p0 = proj(-halfL * 0.965f, -wHalf, 0.29f)
        val p1 = proj(-halfL * 0.965f, wHalf, 0.29f)
        val p2 = proj(-halfL * 0.905f, wHalf * 0.92f, 0.46f)
        val p3 = proj(-halfL * 0.905f, -wHalf * 0.92f, 0.46f)
        moveTo(p0.x, p0.y)
        lineTo(p1.x, p1.y)
        lineTo(p2.x, p2.y)
        lineTo(p3.x, p3.y)
        close()
      }
    canvas.drawPath(rwPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.rgb(24, 40, 70) })
    canvas.drawPath(
      rwPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 1.6f
        color = AndroidColor.rgb(14, 22, 38)
      },
    )

    for (signW in floatArrayOf(-1f, 1f)) {
      val tw = signW * halfW * 0.64f
      val tlPath =
        Path().apply {
          val p0 = proj(-halfL * 0.992f, tw - 0.056f, 0.135f)
          val p1 = proj(-halfL * 0.992f, tw + 0.056f, 0.135f)
          val p2 = proj(-halfL * 0.992f, tw + 0.050f, 0.195f)
          val p3 = proj(-halfL * 0.992f, tw - 0.050f, 0.195f)
          moveTo(p0.x, p0.y)
          lineTo(p1.x, p1.y)
          lineTo(p2.x, p2.y)
          lineTo(p3.x, p3.y)
          close()
        }
      canvas.drawPath(tlPath, Paint(Paint.ANTI_ALIAS_FLAG).apply { color = pal.taillightGlow })
      canvas.drawPath(
        tlPath,
        Paint(Paint.ANTI_ALIAS_FLAG).apply {
          style = Paint.Style.STROKE
          strokeWidth = 1.5f
          color = pal.deepContour
        },
      )
    }
  }

  private fun drawOrthoRoofArrow(
    canvas: Canvas,
    zRoof: Float,
    halfL: Float,
    halfW: Float,
    proj: (Float, Float, Float) -> PointF,
  ) {
    val scaleL = (halfL * 0.54f).coerceIn(0.28f, 0.44f)
    val scaleW = (halfW * 0.56f).coerceIn(0.18f, 0.24f)
    val pts =
      listOf(
        Vec2(-0.88f * scaleL, -0.34f * scaleW),
        Vec2(-0.88f * scaleL, 0.34f * scaleW),
        Vec2(0.12f * scaleL, 0.34f * scaleW),
        Vec2(0.12f * scaleL, 0.92f * scaleW),
        Vec2(0.95f * scaleL, 0.0f),
        Vec2(0.12f * scaleL, -0.92f * scaleW),
        Vec2(0.12f * scaleL, -0.34f * scaleW),
      )

    val arrowPath = Path()
    val shadowPath = Path()
    pts.forEachIndexed { idx, v ->
      val pt = proj(v.l, v.w, zRoof)
      if (idx == 0) {
        arrowPath.moveTo(pt.x, pt.y)
        shadowPath.moveTo(pt.x + 1.0f, pt.y + 2.0f)
      } else {
        arrowPath.lineTo(pt.x, pt.y)
        shadowPath.lineTo(pt.x + 1.0f, pt.y + 2.0f)
      }
    }
    arrowPath.close()
    shadowPath.close()

    canvas.drawPath(
      shadowPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply { color = AndroidColor.argb(65, 16, 22, 38) },
    )
    canvas.drawPath(
      arrowPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeWidth = 4.6f
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
        color = AndroidColor.rgb(24, 32, 52)
      },
    )
    canvas.drawPath(
      arrowPath,
      Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
        color = AndroidColor.WHITE
      },
    )
  }

  private fun orthoPaletteFor(color: VehicleColor): Palette {
    return when (color) {
      VehicleColor.GREEN ->
        Palette(
          roofBright = AndroidColor.rgb(164, 255, 68),
          roofMain = AndroidColor.rgb(112, 232, 28),
          sideSun = AndroidColor.rgb(86, 198, 18),
          sideShade = AndroidColor.rgb(58, 154, 10),
          deepContour = AndroidColor.rgb(30, 94, 4),
          taillightGlow = AndroidColor.rgb(255, 86, 78),
        )
      VehicleColor.PURPLE ->
        Palette(
          roofBright = AndroidColor.rgb(198, 122, 255),
          roofMain = AndroidColor.rgb(148, 58, 240),
          sideSun = AndroidColor.rgb(122, 42, 206),
          sideShade = AndroidColor.rgb(92, 24, 164),
          deepContour = AndroidColor.rgb(54, 10, 102),
          taillightGlow = AndroidColor.rgb(255, 86, 98),
        )
      VehicleColor.BROWN ->
        Palette(
          roofBright = AndroidColor.rgb(184, 114, 98),
          roofMain = AndroidColor.rgb(138, 74, 62),
          sideSun = AndroidColor.rgb(112, 56, 46),
          sideShade = AndroidColor.rgb(86, 38, 30),
          deepContour = AndroidColor.rgb(48, 20, 16),
          taillightGlow = AndroidColor.rgb(255, 92, 84),
        )
      VehicleColor.WHITE ->
        Palette(
          roofBright = AndroidColor.rgb(255, 255, 255),
          roofMain = AndroidColor.rgb(238, 242, 248),
          sideSun = AndroidColor.rgb(206, 214, 228),
          sideShade = AndroidColor.rgb(164, 176, 196),
          deepContour = AndroidColor.rgb(88, 98, 118),
          taillightGlow = AndroidColor.rgb(255, 76, 86),
        )
      else -> paletteFor(color)
    }
  }

  private fun paletteFor(color: VehicleColor): Palette {
    return when (color) {
      VehicleColor.RED ->
        Palette(
          roofBright = AndroidColor.rgb(255, 118, 150),
          roofMain = AndroidColor.rgb(237, 29, 82),
          sideSun = AndroidColor.rgb(212, 20, 68),
          sideShade = AndroidColor.rgb(164, 12, 50),
          deepContour = AndroidColor.rgb(104, 6, 30),
          taillightGlow = AndroidColor.rgb(255, 196, 48),
        )
      VehicleColor.BLUE ->
        Palette(
          roofBright = AndroidColor.rgb(112, 206, 255),
          roofMain = AndroidColor.rgb(26, 156, 245),
          sideSun = AndroidColor.rgb(18, 126, 214),
          sideShade = AndroidColor.rgb(12, 92, 168),
          deepContour = AndroidColor.rgb(6, 54, 108),
          taillightGlow = AndroidColor.rgb(255, 76, 98),
        )
      VehicleColor.YELLOW ->
        Palette(
          roofBright = AndroidColor.rgb(255, 246, 120),
          roofMain = AndroidColor.rgb(248, 218, 22),
          sideSun = AndroidColor.rgb(226, 182, 8),
          sideShade = AndroidColor.rgb(186, 138, 2),
          deepContour = AndroidColor.rgb(120, 84, 0),
          taillightGlow = AndroidColor.rgb(255, 68, 86),
        )
      else -> {
        val main = color.fill.toArgb()
        val hi = color.highlight.toArgb()
        val sh = color.shade.toArgb()
        Palette(
          roofBright = hi,
          roofMain = main,
          sideSun = lerpColor(main, sh, 0.28f),
          sideShade = sh,
          deepContour = lerpColor(sh, AndroidColor.rgb(16, 20, 32), 0.45f),
          taillightGlow = AndroidColor.rgb(255, 92, 84),
        )
      }
    }
  }

  private fun lerpColor(a: Int, b: Int, tRaw: Float): Int {
    val t = tRaw.coerceIn(0f, 1f)
    val ar = AndroidColor.red(a)
    val ag = AndroidColor.green(a)
    val ab = AndroidColor.blue(a)
    val br = AndroidColor.red(b)
    val bg = AndroidColor.green(b)
    val bb = AndroidColor.blue(b)
    return AndroidColor.rgb(
      (ar + (br - ar) * t).roundToInt(),
      (ag + (bg - ag) * t).roundToInt(),
      (ab + (bb - ab) * t).roundToInt(),
    )
  }

  private fun sign(v: Float): Float = if (v >= 0f) 1f else -1f
}
