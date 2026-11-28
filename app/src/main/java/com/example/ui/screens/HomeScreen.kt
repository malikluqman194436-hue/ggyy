package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale as drawScale
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.data.PlayerProgress
import com.example.game.BoosterType
import com.example.game.VehicleColor
import com.example.game.rememberAssetManager
import com.example.ui.components.CoinPill
import com.example.ui.components.HelpDialog
import com.example.ui.components.NoAdsPackDialog
import com.example.ui.components.SettingsDialog
import com.example.ui.components.ShopDialog
import com.example.ui.components.StarterPackDialog
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.LilitaOneFontFamily
import com.example.ui.theme.OutlineInk
import com.example.ui.theme.PlayGreen
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun HomeScreen(
  progress: PlayerProgress,
  shopNotice: String?,
  onPlayLevel: (Int) -> Unit,
  onToggleSound: (Boolean) -> Unit,
  onToggleVibration: (Boolean) -> Unit,
  onToggleCloudSave: (Boolean) -> Unit = {},
  onCycleLanguage: () -> Unit = {},
  onChangePassengerSize: (Float) -> Unit = {},
  onBuyBooster: (BoosterType) -> Unit,
  onClaimFreeCoins: () -> Unit,
  onClaimStarterPack: () -> Unit,
  onUnlockNoAds: () -> Unit,
  onResetToLevel1: () -> Unit,
  onButtonClick: () -> Unit = {},
  onSettingsClick: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  var showSettings by remember { mutableStateOf(false) }
  var showHelp by remember { mutableStateOf(false) }
  var showShop by remember { mutableStateOf(false) }
  var showNoAds by remember { mutableStateOf(false) }
  var showStarterPack by remember { mutableStateOf(false) }
  var isBoardingStart by remember { mutableStateOf(false) }
  val boardingProgress = remember { Animatable(0f) }

  LaunchedEffect(isBoardingStart) {
    if (!isBoardingStart) return@LaunchedEffect
    boardingProgress.snapTo(0f)
    boardingProgress.animateTo(
      targetValue = 1f,
      animationSpec = tween(durationMillis = 2600, easing = LinearEasing),
    )
    onPlayLevel(progress.currentLevel)
  }

  BackHandler(enabled = !isBoardingStart && (showSettings || showHelp || showShop || showNoAds || showStarterPack)) {
    when {
      showHelp -> showHelp = false
      showShop -> showShop = false
      showNoAds -> showNoAds = false
      showStarterPack -> showStarterPack = false
      showSettings -> showSettings = false
    }
  }

  BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val screenW = maxWidth
    val screenH = maxHeight
    val containerW = minOf(screenW, 560.dp)
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val btnHeight = minOf(containerW * 0.182f, screenH * 0.086f)
    val settingsW = btnHeight * (524f / 446f)
    val levelW = btnHeight * (733f / 315f)
    val shopW = btnHeight * (518f / 446f)
    val sideEdgePad = containerW * 0.032f
    val bottomRowPad = maxOf(screenH * 0.036f, navBottom + 8.dp)

    Image(
      painter = painterResource(id = R.drawable.home_play_fg),
      contentDescription = null,
      contentScale = ContentScale.Crop,
      alignment = Alignment.Center,
      modifier = Modifier.fillMaxSize(),
    )

    // Exact ContentScale.Crop coordinate mapping from home_play_fg (1024x1535) so the bus
    // always stands squarely on the asphalt road lane regardless of device aspect ratio.
    val density = LocalDensity.current
    val screenWPx = constraints.maxWidth.toFloat()
    val screenHPx = constraints.maxHeight.toFloat()
    val fgBgW = 1024f
    val fgBgH = 1535f
    val fgCropScale = maxOf(screenWPx / fgBgW, screenHPx / fgBgH)
    val renderedBgW = fgBgW * fgCropScale
    val renderedBgH = fgBgH * fgCropScale
    val bgOffsetX = (screenWPx - renderedBgW) * 0.5f
    val bgOffsetY = (screenHPx - renderedBgH) * 0.5f

    val busAspect = 1688f / 932f
    val busWidthPx = renderedBgW * 0.43f
    val busHeightPx = busWidthPx / busAspect
    val busBaseCenterX = bgOffsetX + renderedBgW * 0.545f
    // Road asphalt spans ny = 0.752..0.870 with center line at 0.805; tires rest cleanly at 0.842
    val tireBottomY = bgOffsetY + renderedBgH * 0.842f
    val busBaseLeftPx = busBaseCenterX - busWidthPx * 0.5f
    val busTopPx = tireBottomY - busHeightPx * 0.962f

    // 4 matching pink 3D passengers standing in a neat ordered single-file line at the bus stop
    val pinkWaveSheet = ImageBitmap.imageResource(id = R.drawable.home_pink_passenger_sheet)
    val pinkBoardingSheet = ImageBitmap.imageResource(id = R.drawable.home_pink_boarding_sheet)
    val waitingGuests =
      remember {
        listOf(
          HomeWaitingGuest(
            homeNx = 0.318f,
            homeNy = 0.746f,
            boardOrder = 0,
            seatWindowCenterX = 0.841f,
            phaseOffset = 0.00f,
          ),
          HomeWaitingGuest(
            homeNx = 0.275f,
            homeNy = 0.746f,
            boardOrder = 1,
            seatWindowCenterX = 0.690f,
            phaseOffset = 0.25f,
          ),
          HomeWaitingGuest(
            homeNx = 0.232f,
            homeNy = 0.746f,
            boardOrder = 2,
            seatWindowCenterX = 0.535f,
            phaseOffset = 0.50f,
          ),
          HomeWaitingGuest(
            homeNx = 0.189f,
            homeNy = 0.746f,
            boardOrder = 3,
            seatWindowCenterX = 0.376f,
            phaseOffset = 0.75f,
          ),
        )
      }

    // Standing guitarist mascot on the sidewalk at the marked right-side spot behind the bus
    val musicTransition = rememberInfiniteTransition(label = "home_guitarist_music")
    val guitarGroovePhase by musicTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(1500, easing = LinearEasing), RepeatMode.Restart),
      label = "guitar_groove_phase",
    )
    val musicNotePhase by musicTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(2900, easing = LinearEasing), RepeatMode.Restart),
      label = "music_note_phase",
    )

    val guitaristAspect = 703f / 999f
    val guitaristHeightPx = renderedBgH * 0.092f
    val guitaristWidthPx = guitaristHeightPx * guitaristAspect
    val guitaristCenterX = bgOffsetX + renderedBgW * 0.795f
    // Sidewalk spans ny = 0.715..0.750; feet stand squarely at 0.740 right in front of the flowerpots
    val guitaristFeetY = bgOffsetY + renderedBgH * 0.740f
    val guitaristLeftPx = guitaristCenterX - guitaristWidthPx * 0.5f
    val guitaristTopPx = guitaristFeetY - guitaristHeightPx * 0.987f

    val grooveWave = sin(guitarGroovePhase * 2f * PI.toFloat())
    val strumBouncePx = abs(grooveWave) * (guitaristHeightPx * 0.018f)
    val grooveAngleDeg = grooveWave * 2.4f

    // Sidewalk contact shadow directly under the guitarist's feet
    Canvas(modifier = Modifier.fillMaxSize()) {
      val feetShadowCenterX = guitaristLeftPx + guitaristWidthPx * 0.44f
      val shadowR = guitaristWidthPx * 0.46f
      drawScale(scaleX = 1f, scaleY = 0.24f, pivot = Offset(feetShadowCenterX, guitaristFeetY)) {
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(Color(0x6E18202E), Color(0x3018202E), Color.Transparent),
              center = Offset(feetShadowCenterX, guitaristFeetY),
              radius = shadowR,
            ),
          radius = shadowR,
          center = Offset(feetShadowCenterX, guitaristFeetY),
        )
      }
    }

    Image(
      painter = painterResource(id = R.drawable.home_standing_guitarist),
      contentDescription = "Green passenger standing on sidewalk playing music",
      contentScale = ContentScale.FillBounds,
      modifier =
        Modifier
          .offset { IntOffset(guitaristLeftPx.roundToInt(), (guitaristTopPx - strumBouncePx).roundToInt()) }
          .size(
            width = with(density) { guitaristWidthPx.toDp() },
            height = with(density) { guitaristHeightPx.toDp() },
          )
          .graphicsLayer {
            rotationZ = grooveAngleDeg
            transformOrigin = TransformOrigin(0.44f, 0.98f)
          }
          .testTag("home_standing_guitarist"),
    )

    // Floating vector musical notes rising from the guitar strings
    Canvas(modifier = Modifier.fillMaxSize()) {
      val noteOriginX = guitaristLeftPx + guitaristWidthPx * 0.72f
      val noteOriginY = guitaristTopPx + guitaristHeightPx * 0.56f
      val noteRiseMax = guitaristHeightPx * 0.78f
      val noteRadius = (guitaristWidthPx * 0.068f).coerceAtLeast(3.5f)
      val noteColors = arrayOf(Color(0xFFFFE44D), Color(0xFF4BE3FF), Color(0xFFFF8CE8))

      for (i in 0 until 3) {
        val localP = (musicNotePhase + i * 0.333f) % 1f
        val fadeInAlpha = (localP / 0.18f).coerceIn(0f, 1f)
        val fadeOutAlpha = ((1f - localP) / 0.28f).coerceIn(0f, 1f)
        val noteAlpha = fadeInAlpha * fadeOutAlpha
        if (noteAlpha <= 0.02f) continue

        val driftX = sin(localP * 2f * PI.toFloat() + i * 1.4f) * guitaristWidthPx * 0.14f + i * guitaristWidthPx * 0.06f
        val nx = noteOriginX + driftX
        val ny = noteOriginY - localP * noteRiseMax
        val noteScale = 0.78f + 0.32f * sin(localP * PI.toFloat())
        val r = noteRadius * noteScale
        val fillColor = noteColors[i].copy(alpha = noteAlpha)
        val strokeColor = Color(0xFF0E1B3A).copy(alpha = noteAlpha * 0.85f)
        val stemW = (r * 0.34f).coerceAtLeast(1.5f)

        rotate(degrees = sin(localP * 2f * PI.toFloat() + i) * 10f, pivot = Offset(nx, ny)) {
          if (i % 2 == 0) {
            val headCenter = Offset(nx, ny)
            drawCircle(color = strokeColor, radius = r * 1.25f, center = headCenter)
            drawCircle(color = fillColor, radius = r, center = headCenter)
            val stemX = nx + r * 0.78f
            val stemTopY = ny - r * 3.1f
            drawLine(strokeColor, Offset(stemX, ny), Offset(stemX, stemTopY), strokeWidth = stemW * 1.6f, cap = StrokeCap.Round)
            drawLine(fillColor, Offset(stemX, ny), Offset(stemX, stemTopY), strokeWidth = stemW, cap = StrokeCap.Round)
            val flagPath =
              Path().apply {
                moveTo(stemX, stemTopY)
                quadraticTo(stemX + r * 1.6f, stemTopY + r * 0.7f, stemX + r * 1.15f, stemTopY + r * 1.9f)
              }
            drawPath(flagPath, strokeColor, style = Stroke(width = stemW * 1.6f, cap = StrokeCap.Round))
            drawPath(flagPath, fillColor, style = Stroke(width = stemW, cap = StrokeCap.Round))
          } else {
            val h1 = Offset(nx - r * 0.9f, ny + r * 0.25f)
            val h2 = Offset(nx + r * 0.9f, ny - r * 0.20f)
            val s1Top = Offset(h1.x + r * 0.72f, h1.y - r * 2.75f)
            val s2Top = Offset(h2.x + r * 0.72f, h2.y - r * 2.75f)
            for (hc in arrayOf(h1, h2)) {
              drawCircle(color = strokeColor, radius = r * 1.15f, center = hc)
              drawCircle(color = fillColor, radius = r * 0.9f, center = hc)
            }
            drawLine(strokeColor, Offset(s1Top.x, h1.y), s1Top, strokeWidth = stemW * 1.55f, cap = StrokeCap.Round)
            drawLine(strokeColor, Offset(s2Top.x, h2.y), s2Top, strokeWidth = stemW * 1.55f, cap = StrokeCap.Round)
            drawLine(strokeColor, s1Top, s2Top, strokeWidth = stemW * 2.0f, cap = StrokeCap.Round)
            drawLine(fillColor, Offset(s1Top.x, h1.y), s1Top, strokeWidth = stemW, cap = StrokeCap.Round)
            drawLine(fillColor, Offset(s2Top.x, h2.y), s2Top, strokeWidth = stemW, cap = StrokeCap.Round)
            drawLine(fillColor, s1Top, s2Top, strokeWidth = stemW * 1.35f, cap = StrokeCap.Round)
          }
        }
      }
    }

    val boardT = if (isBoardingStart) boardingProgress.value else 0f
    val driveP = if (!isBoardingStart) 0f else ((boardT - 0.74f) / 0.26f).coerceIn(0f, 1f)
    val driveEase = driveP * driveP * (1.4f - 0.4f * driveP)
    val busDriveOffsetX = -driveEase * (busBaseLeftPx + busWidthPx * 1.15f)
    val busCenterX = busBaseCenterX + busDriveOffsetX
    val busLeftPx = busBaseLeftPx + busDriveOffsetX
    val busDriveRumblePx =
      if (driveP > 0f && driveP < 1f) {
        sin(driveP * 26f) * (busHeightPx * 0.0065f) * (1f - driveP * 0.35f)
      } else {
        0f
      }

    val guestHeightPx = renderedBgH * 0.076f
    val queueFrontX = bgOffsetX + renderedBgW * 0.318f
    val queueStepX = renderedBgW * 0.043f
    val queueBaseY = bgOffsetY + renderedBgH * 0.746f
    val busDoorX = busBaseLeftPx + busWidthPx * 0.225f
    val busDoorY = tireBottomY - busHeightPx * 0.025f
    val busCabinEntryX = busBaseLeftPx + busWidthPx * 0.285f
    val busCabinEntryY = busTopPx + busHeightPx * 0.58f
    val winTop = busTopPx + busHeightPx * 0.314f
    val winBottom = busTopPx + busHeightPx * 0.544f
    val winHeightPx = winBottom - winTop
    // Seated frame height inside 96x136 cell is ~112/136; size so head + shoulders sit cleanly in the bus window
    val seatedScale = (winHeightPx * 1.18f) / (guestHeightPx * (112f / 136f))

    data class ComputedGuestRender(
      val guest: HomeWaitingGuest,
      val curX: Float,
      val curY: Float,
      val scale: Float,
      val localP: Float,
      val frameIdx: Int,
      val isInsideBusWindow: Boolean,
      val drawInFrontOfBus: Boolean,
    )

    val staggerStep = 0.140f
    val activeSpan = 0.300f
    val computedGuests =
      waitingGuests.map { guest ->
        val startX = bgOffsetX + renderedBgW * guest.homeNx
        val startY = bgOffsetY + renderedBgH * guest.homeNy
        val staggerStart = guest.boardOrder * staggerStep
        val localP = if (!isBoardingStart) 0f else ((boardT - staggerStart) / activeSpan).coerceIn(0f, 1f)
        val winX = busLeftPx + busWidthPx * guest.seatWindowCenterX
        val windowSeatBottomY = winBottom + winHeightPx * 0.14f

        when {
          !isBoardingStart -> {
            ComputedGuestRender(
              guest = guest,
              curX = startX,
              curY = startY,
              scale = 1f,
              localP = 0f,
              frameIdx = -1,
              isInsideBusWindow = false,
              drawInFrontOfBus = true,
            )
          }
          boardT < staggerStart -> {
            // Queue stepping smoothly forward 1 spot as each passenger in front walks to board
            var totalAdvance = 0f
            var steppingFrac = -1f
            for (k in 0 until guest.boardOrder) {
              val stepP = ((boardT - k * staggerStep) / (staggerStep * 0.84f)).coerceIn(0f, 1f)
              val smooth = stepP * stepP * (3f - 2f * stepP)
              totalAdvance += smooth
              if (stepP in 0.02f..0.98f) steppingFrac = stepP
            }
            val currentSlotPos = (guest.boardOrder - totalAdvance).coerceAtLeast(0f)
            val queueX = queueFrontX - currentSlotPos * queueStepX
            val stepBounce =
              if (steppingFrac >= 0f) abs(sin(steppingFrac * 2f * PI.toFloat())) * guestHeightPx * 0.020f
              else 0f
            val stepFrame = if (steppingFrac >= 0f) ((steppingFrac * 8f).toInt()) % 7 else -1
            ComputedGuestRender(
              guest = guest,
              curX = queueX,
              curY = queueBaseY - stepBounce,
              scale = 1f,
              localP = 0f,
              frameIdx = stepFrame,
              isInsideBusWindow = false,
              drawInFrontOfBus = true,
            )
          }
          localP <= 0.54f -> {
            // 1. Walk from the front of the queue straight to the bus door (frames 0..6)
            val walkP = (localP / 0.54f).coerceIn(0f, 1f)
            val strideBounce = abs(sin(walkP * 4f * PI.toFloat())) * guestHeightPx * 0.025f
            val walkFrame = ((walkP * 14f).toInt()) % 7
            ComputedGuestRender(
              guest = guest,
              curX = queueFrontX + (busDoorX - queueFrontX) * walkP,
              curY = queueBaseY + (busDoorY - queueBaseY) * walkP - strideBounce,
              scale = 1f - 0.08f * walkP,
              localP = localP,
              frameIdx = walkFrame,
              isInsideBusWindow = false,
              drawInFrontOfBus = true,
            )
          }
          localP <= 0.70f -> {
            // 2. Step & turn smoothly up into the bus doorway (frames 7..9)
            val turnP = ((localP - 0.54f) / 0.16f).coerceIn(0f, 1f)
            val turnFrame = 7 + (turnP * 2.99f).toInt().coerceIn(0, 2)
            ComputedGuestRender(
              guest = guest,
              curX = busDoorX + (busCabinEntryX - busDoorX) * turnP,
              curY = busDoorY + (busCabinEntryY - busDoorY) * turnP,
              scale = 0.92f - (0.92f - seatedScale * 1.08f) * turnP,
              localP = localP,
              frameIdx = turnFrame,
              isInsideBusWindow = false,
              drawInFrontOfBus = turnP < 0.55f,
            )
          }
          localP <= 0.88f -> {
            // 3. Move inside the bus cabin to assigned window seat and sit down (frames 10..12)
            val sitP = ((localP - 0.70f) / 0.18f).coerceIn(0f, 1f)
            val easedSit = sitP * sitP * (3f - 2f * sitP)
            val entryWindowX = busLeftPx + busWidthPx * 0.305f
            val sitFrame = 10 + (sitP * 2.99f).toInt().coerceIn(0, 2)
            ComputedGuestRender(
              guest = guest,
              curX = entryWindowX + (winX - entryWindowX) * easedSit,
              curY = windowSeatBottomY + (1f - easedSit) * (winHeightPx * 0.10f),
              scale = seatedScale * (1.08f - 0.08f * easedSit),
              localP = localP,
              frameIdx = sitFrame,
              isInsideBusWindow = true,
              drawInFrontOfBus = false,
            )
          }
          else -> {
            // 4. Settled seated inside their bus window seat, riding along as the bus departs (frames 13..15)
            val settleP = ((localP - 0.88f) / 0.12f).coerceIn(0f, 1f)
            val seatedFrame = 13 + (settleP * 2.99f).toInt().coerceIn(0, 2)
            ComputedGuestRender(
              guest = guest,
              curX = winX,
              curY = windowSeatBottomY,
              scale = seatedScale,
              localP = localP,
              frameIdx = seatedFrame,
              isInsideBusWindow = true,
              drawInFrontOfBus = false,
            )
          }
        }
      }

    // Subtle bus suspension bounce whenever a passenger settles into a seat
    val busSeatBouncePx =
      if (!isBoardingStart) 0f
      else {
        computedGuests.sumOf { cg ->
          if (cg.localP in 0.84f..0.98f) {
            val sp = (cg.localP - 0.84f) / 0.14f
            (sin(sp * PI) * (busHeightPx * 0.015f)).toDouble()
          } else {
            0.0
          }
        }.toFloat()
      }
    val busBounceTotalPx = busSeatBouncePx + busDriveRumblePx

    // Doorway-entering guests briefly tuck behind bus frame right before appearing in window
    Canvas(modifier = Modifier.fillMaxSize()) {
      val behindBusGuests =
        computedGuests
          .filter { !it.isInsideBusWindow && !it.drawInFrontOfBus }
          .sortedBy { it.curY }
      for (cg in behindBusGuests) {
        drawHomePinkGuest(
          guest = cg.guest,
          feetX = cg.curX,
          feetY = cg.curY,
          guestHeightPx = guestHeightPx,
          scale = cg.scale,
          alpha = 1f,
          idleWavePhase = musicNotePhase,
          boardingFrameIdx = cg.frameIdx,
          pinkWaveSheet = pinkWaveSheet,
          pinkBoardingSheet = pinkBoardingSheet,
          drawContactShadow = false,
        )
      }
    }

    // Grounded asphalt contact shadows directly under the bus chassis and tires
    Canvas(modifier = Modifier.fillMaxSize()) {
      val chassisShadowY = tireBottomY - busHeightPx * 0.015f
      val chassisShadowR = busWidthPx * 0.44f
      drawScale(scaleX = 1f, scaleY = 0.14f, pivot = Offset(busCenterX, chassisShadowY)) {
        drawCircle(
          brush =
            Brush.radialGradient(
              colors = listOf(Color(0x700B101B), Color(0x360B101B), Color.Transparent),
              center = Offset(busCenterX, chassisShadowY),
              radius = chassisShadowR,
            ),
          radius = chassisShadowR,
          center = Offset(busCenterX, chassisShadowY),
        )
      }

      val frontTireX = busLeftPx + busWidthPx * 0.206f
      val rearTireX = busLeftPx + busWidthPx * 0.768f
      val tireShadowR = busWidthPx * 0.105f
      for (tireX in floatArrayOf(frontTireX, rearTireX)) {
        drawScale(scaleX = 1f, scaleY = 0.22f, pivot = Offset(tireX, tireBottomY)) {
          drawCircle(
            brush =
              Brush.radialGradient(
                colors = listOf(Color(0x9E080C14), Color(0x4A080C14), Color.Transparent),
                center = Offset(tireX, tireBottomY),
                radius = tireShadowR,
              ),
            radius = tireShadowR,
            center = Offset(tireX, tireBottomY),
          )
        }
      }

      // Animated exhaust puffs behind the rear right bumper as the bus drives forward
      if (driveP > 0.01f && driveP < 0.96f) {
        val tailpipeX = busLeftPx + busWidthPx * 0.99f
        val tailpipeY = tireBottomY - busHeightPx * 0.14f
        for (i in 0 until 4) {
          val puffP = ((driveP * 2.4f) + i * 0.25f) % 1f
          val puffAlpha = (1f - puffP) * 0.55f * (1f - driveP * 0.3f)
          val px = tailpipeX + puffP * busWidthPx * 0.18f
          val py = tailpipeY - puffP * busHeightPx * 0.16f + sin(puffP * 4f + i) * 3f
          val pr = (busHeightPx * 0.032f) * (0.65f + puffP * 1.15f)
          drawCircle(
            color = Color(0xFFEBF2FA).copy(alpha = puffAlpha.coerceIn(0f, 1f)),
            radius = pr,
            center = Offset(px, py),
          )
        }
      }
    }

    Image(
      painter = painterResource(id = R.drawable.home_standing_blue_bus),
      contentDescription = "Blue bus parked on road",
      contentScale = ContentScale.FillBounds,
      modifier =
        Modifier
          .offset { IntOffset(busLeftPx.roundToInt(), (busTopPx + busBounceTotalPx).roundToInt()) }
          .size(
            width = with(density) { busWidthPx.toDp() },
            height = with(density) { busHeightPx.toDp() },
          )
          .testTag("home_standing_bus"),
    )

    // Boarding & seated pink passengers clipped cleanly inside the 4 blue bus side windows
    Canvas(modifier = Modifier.fillMaxSize()) {
      val insideBusGuests = computedGuests.filter { it.isInsideBusWindow }
      if (insideBusGuests.isNotEmpty()) {
        val clippedWinTop = busTopPx + busBounceTotalPx + busHeightPx * 0.314f
        val clippedWinBottom = busTopPx + busBounceTotalPx + busHeightPx * 0.544f
        val winLeft = busLeftPx + busWidthPx * 0.305f
        val winRight = busLeftPx + busWidthPx * 0.912f
        clipRect(left = winLeft, top = clippedWinTop, right = winRight, bottom = clippedWinBottom) {
          for (cg in insideBusGuests) {
            drawHomePinkGuest(
              guest = cg.guest,
              feetX = cg.curX,
              feetY = cg.curY + busBounceTotalPx,
              guestHeightPx = guestHeightPx,
              scale = cg.scale,
              alpha = 1f,
              idleWavePhase = musicNotePhase,
              boardingFrameIdx = cg.frameIdx,
              pinkWaveSheet = pinkWaveSheet,
              pinkBoardingSheet = pinkBoardingSheet,
              drawContactShadow = false,
            )
          }
          // Translucent blue window-glass tint so passengers look clearly seated behind glass
          drawRect(
            color = Color(0x381056B8),
            topLeft = Offset(winLeft, clippedWinTop),
            size = Size(winRight - winLeft, clippedWinBottom - clippedWinTop),
          )
        }
      }
    }

    // Foreground waiting line & walking guests outside the bus (drawn over sidewalk/road in front of bus)
    Canvas(modifier = Modifier.fillMaxSize()) {
      val frontGuests =
        computedGuests
          .filter { !it.isInsideBusWindow && it.drawInFrontOfBus }
          .sortedBy { it.curY }
      for (cg in frontGuests) {
        drawHomePinkGuest(
          guest = cg.guest,
          feetX = cg.curX,
          feetY = cg.curY,
          guestHeightPx = guestHeightPx,
          scale = cg.scale,
          alpha = 1f,
          idleWavePhase = musicNotePhase,
          boardingFrameIdx = cg.frameIdx,
          pinkWaveSheet = pinkWaveSheet,
          pinkBoardingSheet = pinkBoardingSheet,
          drawContactShadow = cg.localP <= 0.60f,
        )
      }
    }

    Column(
      modifier =
        Modifier
          .fillMaxSize()
          .widthIn(max = 560.dp)
          .align(Alignment.Center)
          .windowInsetsPadding(WindowInsets.statusBars)
          .windowInsetsPadding(WindowInsets.displayCutout),
      verticalArrangement = Arrangement.SpaceBetween,
    ) {
      // Top HUD: Coin Pill
      Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        CoinPill(
          coins = progress.coins,
          onAddClick = {
            onButtonClick()
            showShop = true
          },
        )
      }

      // Left-side floating promo buttons tucked closely together near the left edge
      Column(
        modifier = Modifier.padding(start = 8.dp, top = 6.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        AnimatedNoAdsButton(
          adFreeActive = progress.noAdsUnlocked,
          onClick = {
            onButtonClick()
            showNoAds = true
          },
        )
        AnimatedStarterPackButton(
          claimed = progress.starterPackClaimed,
          onClick = {
            onButtonClick()
            showStarterPack = true
          },
        )
      }

      Spacer(modifier = Modifier.weight(1f))

      // Bottom Control Bar placed strictly below the white sidewalk curb over the green grass
      Row(
        modifier =
          Modifier
            .fillMaxWidth()
            .padding(start = sideEdgePad, end = sideEdgePad, bottom = bottomRowPad),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        HomeBarButton(
          drawableRes = R.drawable.btn_home_settings,
          contentDescription = "Settings",
          buttonWidth = settingsW,
          buttonHeight = btnHeight,
          onClick = {
            onSettingsClick()
            showSettings = true
          },
          testTag = "home_settings_button",
        )

        val levelBtnInteraction = remember { MutableInteractionSource() }
        val levelBtnPressed by levelBtnInteraction.collectIsPressedAsState()
        val levelBtnScale by animateFloatAsState(if (levelBtnPressed) 0.92f else 1f, spring(), label = "stage_btn_scale")

        Box(
          modifier =
            Modifier
              .size(width = levelW, height = btnHeight)
              .scale(levelBtnScale)
              .clickable(
                enabled = !isBoardingStart,
                interactionSource = levelBtnInteraction,
                indication = null,
              ) {
                if (!isBoardingStart) {
                  onButtonClick()
                  isBoardingStart = true
                }
              }
              .testTag("play_level_button"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.btn_home_level_blank),
            contentDescription = "LEVEL ${progress.currentLevel}",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
          val levelText = "LEVEL ${progress.currentLevel}"
          val levelTextStyle =
            MaterialTheme.typography.titleLarge.copy(
              fontFamily = LilitaOneFontFamily,
              fontSize = (btnHeight.value * 0.43f).coerceIn(25f, 33f).sp,
              fontWeight = FontWeight.Normal,
              letterSpacing = 0.4.sp,
            )
          // Subtle bottom 3D drop shadow matching reference LEVEL 1 text
          Text(
            text = levelText,
            style =
              levelTextStyle.copy(
                drawStyle = Stroke(width = 11.5f, join = StrokeJoin.Round),
              ),
            color = Color(0xFF07112B),
            maxLines = 1,
            modifier = Modifier.offset(y = 2.dp),
          )
          // Crisp dark-navy cartoon outline around thick white letters
          Text(
            text = levelText,
            style =
              levelTextStyle.copy(
                drawStyle = Stroke(width = 10.5f, join = StrokeJoin.Round),
              ),
            color = Color(0xFF0B1A3E),
            maxLines = 1,
          )
          // Bright white foreground fill
          Text(
            text = levelText,
            style = levelTextStyle,
            color = Color.White,
            maxLines = 1,
          )
        }

        HomeBarButton(
          drawableRes = R.drawable.btn_home_shop,
          contentDescription = "Shop",
          buttonWidth = shopW,
          buttonHeight = btnHeight,
          onClick = {
            onButtonClick()
            showShop = true
          },
          testTag = "home_shop_button",
        )
      }
    }

    AnimatedVisibility(
      visible = showSettings,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      SettingsDialog(
        soundEnabled = progress.soundEnabled,
        vibrationEnabled = progress.vibrationEnabled,
        cloudSaveEnabled = progress.cloudSaveEnabled,
        languageLabel = progress.languageLabel,
        passengerSizeScale = progress.passengerSizeScale,
        onToggleSound = onToggleSound,
        onToggleVibration = onToggleVibration,
        onToggleCloudSave = onToggleCloudSave,
        onCycleLanguage = onCycleLanguage,
        onChangePassengerSize = onChangePassengerSize,
        onOpenSupport = {
          onButtonClick()
          showHelp = true
        },
        onResetToLevel1 = {
          onResetToLevel1()
          showSettings = false
        },
        onDismiss = {
          onButtonClick()
          showSettings = false
        },
      )
    }

    AnimatedVisibility(
      visible = showNoAds,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      NoAdsPackDialog(
        alreadyUnlocked = progress.noAdsUnlocked,
        onActivate = {
          onUnlockNoAds()
          showNoAds = false
        },
        onDismiss = {
          onButtonClick()
          showNoAds = false
        },
      )
    }

    AnimatedVisibility(
      visible = showStarterPack,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      StarterPackDialog(
        alreadyClaimed = progress.starterPackClaimed,
        onActivate = {
          onClaimStarterPack()
          showStarterPack = false
        },
        onDismiss = {
          onButtonClick()
          showStarterPack = false
        },
      )
    }
  }

  if (showHelp) {
    HelpDialog(
      onDismiss = {
        onButtonClick()
        showHelp = false
      },
    )
  }

  if (showShop) {
    ShopDialog(
      coins = progress.coins,
      boardCount = progress.boardBoosters,
      refreshCount = progress.refreshBoosters,
      vipCount = progress.vipBoosters,
      notice = shopNotice,
      onBuyBooster = onBuyBooster,
      onClaimFreeCoins = onClaimFreeCoins,
      onDismiss = {
        onButtonClick()
        showShop = false
      },
    )
  }
}

@Composable
private fun SmoothCircularLightGlow(
  raySpinDegrees: Float,
  glowPhase: Float,
  centerYFraction: Float,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val cx = size.width * 0.5f
    val cy = size.height * centerYFraction
    // Extend smoothly past the badge edges (unclipped) so rotating rays clearly radiate behind it like the video
    val haloR = size.minDimension * 0.68f
    val breath = 0.88f + 0.12f * sin(glowPhase * 2f * PI.toFloat())

    // Soft warm-white / pale-gold core halo
    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              Color(0xD8FFFBE0).copy(alpha = 0.85f * breath),
              Color(0x88FFE97A).copy(alpha = 0.60f * breath),
              Color(0x2EFFD21F),
              Color.Transparent,
            ),
          center = Offset(cx, cy),
          radius = haloR * 0.92f,
        ),
      radius = haloR * 0.92f,
      center = Offset(cx, cy),
    )

    // Smoothly feathered rotating circular sunbeam rays behind the stationary button
    val rayBrush =
      Brush.radialGradient(
        colors =
          listOf(
            Color(0xE6FFFDF0),
            Color(0x9EFFEB78),
            Color(0x3DFFD84A),
            Color.Transparent,
          ),
        center = Offset(cx, cy),
        radius = haloR,
      )

    rotate(degrees = raySpinDegrees, pivot = Offset(cx, cy)) {
      val rayCount = 9
      for (i in 0 until rayCount) {
        val a0 = (i * (360.0 / rayCount)) * (PI / 180.0)
        val halfSpread = 0.165
        val a1 = a0 + halfSpread
        val a2 = a0 - halfSpread
        val path =
          Path().apply {
            moveTo(cx, cy)
            lineTo(cx + cos(a1).toFloat() * haloR, cy + sin(a1).toFloat() * haloR)
            lineTo(cx + cos(a2).toFloat() * haloR, cy + sin(a2).toFloat() * haloR)
            close()
          }
        drawPath(path, brush = rayBrush)
      }
    }
  }
}

@Composable
private fun SoftInnerShimmerAndSparkles(
  glowPhase: Float,
  centerYFraction: Float,
  modifier: Modifier = Modifier,
) {
  Canvas(modifier = modifier) {
    val cx = size.width * 0.5f
    val cy = size.height * centerYFraction
    val innerR = size.minDimension * 0.34f

    // Gentle luminous breathing glow inside the badge/chest (completely smooth sine wave, never abrupt)
    val pulse = 0.5f - 0.5f * cos(glowPhase * 2f * PI.toFloat())
    drawCircle(
      brush =
        Brush.radialGradient(
          colors =
            listOf(
              Color.White.copy(alpha = 0.08f + 0.16f * pulse),
              Color(0xFFFFF3B0).copy(alpha = 0.03f + 0.08f * pulse),
              Color.Transparent,
            ),
          center = Offset(cx, cy),
          radius = innerR,
        ),
      radius = innerR,
      center = Offset(cx, cy),
    )

    // 3 tiny soft twinkling 4-point sparkles gently fading in & out around the icon rim
    val sparkleOffsets =
      arrayOf(
        Triple(-0.34f, -0.30f, 0.00f),
        Triple(0.35f, -0.24f, 0.33f),
        Triple(0.32f, 0.16f, 0.67f),
      )
    for ((ox, oy, phaseShift) in sparkleOffsets) {
      val localPhase = (glowPhase + phaseShift) % 1f
      val twinkle = sin(localPhase * PI.toFloat()).coerceAtLeast(0f).pow(2.2f)
      if (twinkle > 0.04f) {
        val sx = cx + size.width * ox
        val sy = cy + size.height * oy
        val arm = size.minDimension * (0.045f + 0.055f * twinkle)
        val core = arm * 0.24f
        val starPath =
          Path().apply {
            moveTo(sx, sy - arm)
            lineTo(sx + core, sy - core)
            lineTo(sx + arm, sy)
            lineTo(sx + core, sy + core)
            lineTo(sx, sy + arm)
            lineTo(sx - core, sy + core)
            lineTo(sx - arm, sy)
            lineTo(sx - core, sy - core)
            close()
          }
        drawPath(starPath, color = Color.White.copy(alpha = 0.88f * twinkle))
      }
    }
  }
}

@Composable
private fun AnimatedNoAdsButton(
  adFreeActive: Boolean,
  onClick: () -> Unit,
) {
  val assets = rememberAssetManager()
  val cleanSheet = assets.getNoAdsAnimSheet()
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val pressScale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(), label = "noads_press")

  val infiniteTransition = rememberInfiniteTransition(label = "no_ads_glow")
  val raySpin by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
    label = "no_ads_rays",
  )
  val glowPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(2600, easing = LinearEasing), RepeatMode.Restart),
    label = "no_ads_shimmer",
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      Modifier
        .clickable(
          interactionSource = interaction,
          indication = null,
          role = Role.Button,
          onClick = onClick,
        )
        .semantics { contentDescription = if (adFreeActive) "No Ads Active" else "No Ads" }
        .testTag("no_ads_button"),
  ) {
    Box(
      modifier = Modifier.size(84.dp).scale(pressScale),
      contentAlignment = Alignment.Center,
    ) {
      SmoothCircularLightGlow(
        raySpinDegrees = raySpin,
        glowPhase = glowPhase,
        centerYFraction = 0.45f,
        modifier = Modifier.fillMaxSize(),
      )

      // Keep stationary Frame 0 (clean circle edge without frozen rays) so spinning background light shines cleanly
      if (cleanSheet != null) {
        Canvas(modifier = Modifier.fillMaxSize(0.94f)) {
          val cellW = cleanSheet.width / 6
          val cellH = cleanSheet.height
          val drawSize = size.minDimension
          val drawH = drawSize * (cellH.toFloat() / cellW.toFloat())
          drawImage(
            image = cleanSheet,
            srcOffset = IntOffset(0, 0),
            srcSize = IntSize(cellW, cellH),
            dstOffset =
              IntOffset(
                ((size.width - drawSize) * 0.5f).roundToInt(),
                ((size.height - drawH) * 0.5f).roundToInt(),
              ),
            dstSize = IntSize(drawSize.roundToInt().coerceAtLeast(1), drawH.roundToInt().coerceAtLeast(1)),
          )
        }
      } else {
        Image(
          painter = painterResource(id = R.drawable.btn_no_ads_badge),
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize(0.94f),
        )
      }

      SoftInnerShimmerAndSparkles(
        glowPhase = glowPhase,
        centerYFraction = 0.43f,
        modifier = Modifier.fillMaxSize(),
      )
    }

    if (adFreeActive) {
      Box(
        modifier =
          Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PlayGreen)
            .border(1.5.dp, OutlineInk, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
      ) {
        Text(
          text = "ACTIVE",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FredokaFontFamily),
          color = Color.White,
        )
      }
    }
  }
}

@Composable
private fun AnimatedStarterPackButton(
  claimed: Boolean,
  onClick: () -> Unit,
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val pressScale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(), label = "starter_press")

  val infiniteTransition = rememberInfiniteTransition(label = "starter_pack_glow")
  val raySpin by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(tween(9500, easing = LinearEasing), RepeatMode.Restart),
    label = "starter_pack_rays",
  )
  val glowPhase by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(tween(2800, easing = LinearEasing), RepeatMode.Restart),
    label = "starter_pack_shimmer",
  )

  Column(
    horizontalAlignment = Alignment.CenterHorizontally,
    modifier =
      Modifier
        .clickable(
          interactionSource = interaction,
          indication = null,
          role = Role.Button,
          onClick = onClick,
        )
        .semantics { contentDescription = if (claimed) "Starter Pack Claimed" else "Starter Pack" }
        .testTag("starter_pack_button"),
  ) {
    Box(
      modifier = Modifier.size(width = 86.dp, height = 90.dp).scale(pressScale),
      contentAlignment = Alignment.Center,
    ) {
      SmoothCircularLightGlow(
        raySpinDegrees = raySpin,
        glowPhase = glowPhase,
        centerYFraction = 0.45f,
        modifier = Modifier.fillMaxSize(),
      )

      Image(
        painter = painterResource(id = R.drawable.btn_starter_pack_badge),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(0.95f),
      )

      SoftInnerShimmerAndSparkles(
        glowPhase = (glowPhase + 0.45f) % 1f,
        centerYFraction = 0.46f,
        modifier = Modifier.fillMaxSize(),
      )
    }

    if (claimed) {
      Box(
        modifier =
          Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(PlayGreen)
            .border(1.5.dp, OutlineInk, RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 1.dp),
      ) {
        Text(
          text = "CLAIMED",
          style = MaterialTheme.typography.labelSmall.copy(fontSize = 8.sp, fontFamily = FredokaFontFamily),
          color = Color.White,
        )
      }
    }
  }
}

@Composable
private fun HomeBarButton(
  drawableRes: Int,
  contentDescription: String,
  buttonWidth: androidx.compose.ui.unit.Dp,
  buttonHeight: androidx.compose.ui.unit.Dp,
  onClick: () -> Unit,
  testTag: String,
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(), label = "${testTag}_scale")

  Image(
    painter = painterResource(id = drawableRes),
    contentDescription = contentDescription,
    contentScale = ContentScale.Fit,
    modifier =
      Modifier
        .size(width = buttonWidth, height = buttonHeight)
        .scale(scale)
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
        .testTag(testTag),
  )
}

private data class HomeWaitingGuest(
  val homeNx: Float,
  val homeNy: Float,
  val boardOrder: Int,
  val seatWindowCenterX: Float,
  val phaseOffset: Float,
)

private fun DrawScope.drawHomePinkGuest(
  guest: HomeWaitingGuest,
  feetX: Float,
  feetY: Float,
  guestHeightPx: Float,
  scale: Float,
  alpha: Float,
  idleWavePhase: Float,
  boardingFrameIdx: Int,
  pinkWaveSheet: ImageBitmap,
  pinkBoardingSheet: ImageBitmap,
  drawContactShadow: Boolean,
) {
  val drawH = guestHeightPx * scale

  if (drawContactShadow && scale > 0.72f) {
    val shadowR = drawH * 0.28f
    drawScale(scaleX = 1f, scaleY = 0.24f, pivot = Offset(feetX, feetY)) {
      drawCircle(
        brush =
          Brush.radialGradient(
            colors = listOf(Color(0x66121A28).copy(alpha = 0.42f * alpha), Color.Transparent),
            center = Offset(feetX, feetY),
            radius = shadowR,
          ),
        radius = shadowR,
        center = Offset(feetX, feetY),
      )
    }
  }

  if (boardingFrameIdx < 0) {
    // Idle waiting pose at the bus stop using the crisp 2-frame pink passenger sheet
    val cellW = (pinkWaveSheet.width / 2).coerceAtLeast(1)
    val cellH = pinkWaveSheet.height.coerceAtLeast(1)
    val drawW = drawH * (cellW.toFloat() / cellH.toFloat())
    val waveCycle = (idleWavePhase * 3.0f + guest.phaseOffset) % 1f
    val frameIdx = if (waveCycle < 0.56f) 1 else 0
    val swayDeg = if (frameIdx == 1) sin(waveCycle * 4f * PI.toFloat()) * 4.2f else 0f
    val bobY = if (frameIdx == 1) -abs(sin(waveCycle * 2f * PI.toFloat())) * drawH * 0.025f else 0f

    withTransform({
      translate(feetX, feetY + bobY)
      if (swayDeg != 0f) rotate(degrees = swayDeg, pivot = Offset(0f, -drawH * 0.08f))
    }) {
      drawImage(
        image = pinkWaveSheet,
        srcOffset = IntOffset(frameIdx * cellW, 0),
        srcSize = IntSize(cellW, cellH),
        dstOffset = IntOffset((-drawW * 0.5f).roundToInt(), (-drawH * 0.985f).roundToInt()),
        dstSize = IntSize(drawW.roundToInt().coerceAtLeast(1), drawH.roundToInt().coerceAtLeast(1)),
        alpha = alpha,
        filterQuality = FilterQuality.High,
      )
    }
    return
  }

  // Walking (0..6), Turning (7..9), Boarding/Sitting (10..12), Seated (13..15)
  val totalCols = 16
  val cellW = (pinkBoardingSheet.width / totalCols).coerceAtLeast(1)
  val cellH = pinkBoardingSheet.height.coerceAtLeast(1)
  // Scale by 1.05x so the character body height inside 96x136 matches the standing idle pose seamlessly
  val calibratedH = drawH * 1.05f
  val calibratedW = calibratedH * (cellW.toFloat() / cellH.toFloat())
  val clampedFrame = boardingFrameIdx.coerceIn(0, totalCols - 1)
  val feetBaselineFraction = 132f / 136f

  withTransform({
    translate(feetX, feetY)
  }) {
    drawImage(
      image = pinkBoardingSheet,
      srcOffset = IntOffset(clampedFrame * cellW, 0),
      srcSize = IntSize(cellW, cellH),
      dstOffset =
        IntOffset(
          (-calibratedW * 0.5f).roundToInt(),
          (-calibratedH * feetBaselineFraction).roundToInt(),
        ),
      dstSize = IntSize(calibratedW.roundToInt().coerceAtLeast(1), calibratedH.roundToInt().coerceAtLeast(1)),
      alpha = alpha,
      filterQuality = FilterQuality.High,
    )
  }
}

