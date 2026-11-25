package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin
import com.example.R
import com.example.game.GameOutcome
import com.example.game.GameUiState
import com.example.game.GameViewModel
import com.example.ui.components.BoosterPurchaseDialog
import com.example.ui.components.CoinPill
import com.example.ui.components.HelpDialog
import com.example.ui.components.LoseLevelDialog
import com.example.ui.components.ParkingLotCanvas
import com.example.ui.components.PauseDialog
import com.example.ui.components.PlayfieldEffectsOverlay
import com.example.ui.components.ShopDialog
import com.example.ui.components.UnlockSlotDialog
import com.example.ui.components.WaitingSlotsRow
import com.example.ui.components.WinLevelDialog
import com.example.ui.theme.FredokaFontFamily

@Composable
fun GameScreen(
  uiState: GameUiState,
  viewModel: GameViewModel,
  onNavigateHome: () -> Unit,
  modifier: Modifier = Modifier,
) {
  BackHandler {
    when {
      uiState.showHelpDialog -> viewModel.toggleHelp(false)
      uiState.showMainShopDialog -> viewModel.toggleMainShop(false)
      uiState.activeBoosterShop != null -> viewModel.dismissBoosterShop()
      uiState.activeSlotUnlockIndex != null -> viewModel.dismissSlotUnlock()
      uiState.outcome == GameOutcome.PAUSED -> viewModel.resumeGame()
      uiState.outcome == GameOutcome.PLAYING -> viewModel.pauseGame()
      else -> onNavigateHome()
    }
  }

  var isScreenHeld by remember { mutableStateOf(false) }
  val canBoostSpeed =
    uiState.outcome == GameOutcome.PLAYING &&
      !uiState.showHelpDialog &&
      !uiState.showMainShopDialog &&
      uiState.activeBoosterShop == null &&
      uiState.activeSlotUnlockIndex == null
  val isSpeedBoosted = isScreenHeld && canBoostSpeed

  LaunchedEffect(isSpeedBoosted) {
    viewModel.setSpeedBoosted(isSpeedBoosted)
  }

  LaunchedEffect(uiState.outcome) {
    if (uiState.outcome != GameOutcome.PLAYING) return@LaunchedEffect
    var lastFrameNanos = withFrameNanos { it }
    while (true) {
      val nowNanos = withFrameNanos { it }
      val dt = ((nowNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0.001f, 0.05f)
      lastFrameNanos = nowNanos
      viewModel.onFrame(dt)
    }
  }

  DisposableEffect(Unit) {
    onDispose {
      viewModel.setSpeedBoosted(false)
    }
  }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color(0xFF00B8FF))
        .pointerInput(Unit) {
          awaitPointerEventScope {
            while (true) {
              val event = awaitPointerEvent(PointerEventPass.Initial)
              isScreenHeld = event.changes.any { it.pressed }
            }
          }
        }
  ) {
    val screenH = maxHeight

    // 1. Clean full-bleed gameplay background (sky + clouds, green park strip, waiting bays, curbs & corner bushes)
    Image(
      painter = painterResource(id = R.drawable.game_play_bg_clean),
      contentDescription = null,
      contentScale = ContentScale.FillBounds,
      modifier = Modifier.fillMaxSize(),
    )

    // 2. Glossy navy oval road loop & entry arms with smooth animated conveyor dashes
    Box(
      modifier =
        Modifier
          .align(Alignment.TopCenter)
          .fillMaxWidth()
          .height(screenH * 0.319f)
          .clipToBounds(),
    ) {
      Image(
        painter = painterResource(id = R.drawable.game_oval_road_loop_v3),
        contentDescription = null,
        contentScale = ContentScale.FillBounds,
        modifier = Modifier.fillMaxSize(),
      )
      AnimatedRoadBeltDashes(
        isPlaying = uiState.outcome == GameOutcome.PLAYING,
        isSpeedBoosted = isSpeedBoosted,
        modifier = Modifier.fillMaxSize(),
      )
    }

    // 3. Waiting bays + Middle Parking Lot (3D isometric buses + tutorial hand) + driving/particles overlay
    WaitingSlotsRow(
      slots = uiState.slots,
      vehicles = uiState.vehicles,
      jumpingPassengers = uiState.jumpingPassengers,
      onSlotTap = { slotIdx -> viewModel.onSlotTapped(slotIdx) },
      modifier = Modifier.fillMaxSize(),
    )

    ParkingLotCanvas(
      gridCols = uiState.gridCols,
      gridRows = uiState.gridRows,
      vehicles = uiState.vehicles,
      tunnels = uiState.tunnels,
      tutorialVehicleId = uiState.tutorialVehicleId,
      onLotTap = { gx, gy -> viewModel.onLotTap(gx, gy) },
      modifier =
        Modifier
          .align(Alignment.TopCenter)
          .padding(top = screenH * 0.452f)
          .fillMaxWidth()
          .height(screenH * 0.455f),
    )

    PlayfieldEffectsOverlay(
      vehicles = uiState.vehicles,
      gridCols = uiState.gridCols,
      gridRows = uiState.gridRows,
      jumpingPassengers = uiState.jumpingPassengers,
      particles = uiState.particles,
      floatingTexts = uiState.floatingTexts,
      totalSlots = uiState.slots.size,
      modifier = Modifier.fillMaxSize(),
    )

    val screenW = maxWidth
    val hudW = minOf(screenW, 560.dp)
    val safeTop =
      maxOf(
        WindowInsets.statusBars.asPaddingValues().calculateTopPadding(),
        WindowInsets.displayCutout.asPaddingValues().calculateTopPadding(),
      )
    val levelTop = maxOf(screenH * 0.049f, safeTop + 6.dp)
    val sideRowTop = levelTop + screenH * 0.030f
    val sideMargin = hudW * 0.018f
    val levelW = hudW * 0.292f
    val levelH = levelW / 3.00f
    val coinW = hudW * 0.314f
    val coinVisualH = coinW * (108f / 363f)
    val coinTouchH = maxOf(coinVisualH, 48.dp)
    val pauseVisualW = hudW * 0.092f
    val pauseVisualH = pauseVisualW / 1.116f

    // 5. TOP HUD: Level N Banner (top-center) + Coins (left, lower) + Cyan Pause (right, lower)
    Box(
      modifier =
        Modifier
          .align(Alignment.TopCenter)
          .width(hudW)
          .padding(horizontal = sideMargin),
    ) {
      CoinPill(
        coins = uiState.coins,
        onAddClick = { viewModel.toggleMainShop(true) },
        pillWidth = coinW,
        modifier =
          Modifier
            .align(Alignment.TopStart)
            .padding(top = (sideRowTop - (coinTouchH - coinVisualH) * 0.5f).coerceAtLeast(0.dp)),
      )

      Box(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .padding(top = levelTop)
            .width(levelW)
            .height(levelH)
            .testTag("level_banner"),
        contentAlignment = Alignment.Center,
      ) {
        if (uiState.levelNumber == 1) {
          Image(
            painter = painterResource(id = R.drawable.btn_hud_level),
            contentDescription = "Level 1",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Image(
            painter = painterResource(id = R.drawable.btn_hud_level_blank),
            contentDescription = "Level ${uiState.levelNumber}",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
          val levelText = "Level ${uiState.levelNumber}"
          val levelTextStyle =
            MaterialTheme.typography.titleMedium.copy(
              fontFamily = FredokaFontFamily,
              fontSize = 14.5.sp,
              fontWeight = FontWeight.ExtraBold,
            )
          Text(
            text = levelText,
            style = levelTextStyle.copy(drawStyle = Stroke(width = 5.5f, join = StrokeJoin.Round)),
            color = Color(0xFF04145C),
            maxLines = 1,
          )
          Text(
            text = levelText,
            style = levelTextStyle,
            color = Color.White,
            maxLines = 1,
          )
        }
      }

      Box(
        modifier =
          Modifier
            .align(Alignment.TopEnd)
            .padding(top = (sideRowTop - (48.dp - pauseVisualH) * 0.5f).coerceAtLeast(0.dp))
            .size(48.dp)
            .clickable { viewModel.pauseGame() }
            .testTag("pause_button"),
        contentAlignment = Alignment.CenterEnd,
      ) {
        Image(
          painter = painterResource(id = R.drawable.btn_hud_pause),
          contentDescription = "Pause",
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(width = pauseVisualW, height = pauseVisualH),
        )
      }
    }

    // 6. ZONE 3: Bottom Booster Bar with the 3 locked level buttons (Lv.2, Lv.3, Lv.5)
    Row(
      modifier =
        Modifier
          .align(Alignment.BottomCenter)
          .windowInsetsPadding(WindowInsets.navigationBars)
          .widthIn(max = 460.dp)
          .fillMaxWidth()
          .padding(horizontal = 24.dp, vertical = 10.dp),
      horizontalArrangement = Arrangement.SpaceEvenly,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      LockedBoosterButton(
        drawableRes = R.drawable.btn_booster_lock_lv2,
        contentDescription = "Locked until Level 2",
        testTag = "booster_board_locked",
      )

      LockedBoosterButton(
        drawableRes = R.drawable.btn_booster_lock_lv3,
        contentDescription = "Locked until Level 3",
        testTag = "booster_refresh_locked",
      )

      LockedBoosterButton(
        drawableRes = R.drawable.btn_booster_lock_lv5,
        contentDescription = "Locked until Level 5",
        testTag = "booster_vip_locked",
      )
    }

    // 8. Pause / Settings full-screen dark dim overlay + glossy Settings card
    AnimatedVisibility(
      visible = uiState.outcome == GameOutcome.PAUSED,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      PauseDialog(
        soundEnabled = uiState.soundEnabled,
        vibrationEnabled = uiState.vibrationEnabled,
        cloudSaveEnabled = uiState.cloudSaveEnabled,
        languageLabel = uiState.languageLabel,
        onToggleSound = { viewModel.toggleSound(it) },
        onToggleVibration = { viewModel.toggleVibration(it) },
        onToggleCloudSave = { viewModel.toggleCloudSave(it) },
        onCycleLanguage = { viewModel.cycleLanguage() },
        onOpenSupport = { viewModel.toggleHelp(true) },
        onResume = { viewModel.resumeGame() },
        onRestart = { viewModel.restartLevel() },
        onHome = onNavigateHome,
      )
    }

    // 9. Win / Level Complete full-screen dark dim overlay + glossy reward panel
    AnimatedVisibility(
      visible = uiState.outcome == GameOutcome.WON,
      enter = fadeIn(),
      exit = fadeOut(),
      modifier = Modifier.fillMaxSize(),
    ) {
      WinLevelDialog(
        levelNumber = uiState.levelNumber,
        stars = uiState.starsEarned,
        onNextLevel = { viewModel.nextLevel() },
        onClaimBonusAndNext = { viewModel.claimWinBonusAndNext(400) },
        onHome = onNavigateHome,
      )
    }
  }

  // Modal Dialogs
  if (uiState.outcome == GameOutcome.LOST) {
    LoseLevelDialog(
      coins = uiState.coins,
      notice = uiState.shopNotice,
      onRetry = { viewModel.restartLevel() },
      onRevive = { viewModel.reviveOnLose() },
      onHome = onNavigateHome,
    )
  }

  if (uiState.showHelpDialog) {
    HelpDialog(onDismiss = { viewModel.toggleHelp(false) })
  }

  val activeBooster = uiState.activeBoosterShop
  if (activeBooster != null) {
    BoosterPurchaseDialog(
      booster = activeBooster,
      coins = uiState.coins,
      notice = uiState.shopNotice,
      onGetFree = { viewModel.claimFreeBooster(activeBooster) },
      onBuy = { viewModel.buyAndUseBooster(activeBooster) },
      onDismiss = { viewModel.dismissBoosterShop() },
    )
  }

  val unlockSlotIdx = uiState.activeSlotUnlockIndex
  if (unlockSlotIdx != null) {
    UnlockSlotDialog(
      onConfirm = { viewModel.confirmUnlockSlot(unlockSlotIdx) },
      onDismiss = { viewModel.dismissSlotUnlock() },
    )
  }

  if (uiState.showMainShopDialog) {
    ShopDialog(
      coins = uiState.coins,
      boardCount = uiState.boardBoosters,
      refreshCount = uiState.refreshBoosters,
      vipCount = uiState.vipBoosters,
      notice = uiState.shopNotice,
      onBuyBooster = { viewModel.buyBoosterFromShop(it) },
      onClaimFreeCoins = { viewModel.addBonusCoins(100) },
      onDismiss = { viewModel.toggleMainShop(false) },
    )
  }
}

@Composable
private fun LockedBoosterButton(
  drawableRes: Int,
  contentDescription: String,
  testTag: String,
) {
  Box(
    modifier =
      Modifier
        .size(76.dp)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(id = drawableRes),
      contentDescription = contentDescription,
      contentScale = ContentScale.Fit,
      modifier = Modifier.fillMaxSize(),
    )
  }
}

@Suppress("unused")
@Composable
private fun ImageBoosterButton(
  drawableRes: Int,
  contentDescription: String,
  ownedCount: Int,
  badgeCenterXFrac: Float,
  badgeCenterYFrac: Float,
  badgeDiamFrac: Float,
  drawableAspectRatio: Float,
  testTag: String,
  onClick: () -> Unit,
) {
  BoxWithConstraints(
    modifier =
      Modifier
        .size(76.dp)
        .clickable(onClick = onClick)
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(id = drawableRes),
      contentDescription = contentDescription,
      contentScale = ContentScale.Fit,
      modifier = Modifier.fillMaxSize(),
    )

    // When the player owns >= 1 of this booster, replace the red '+' badge seamlessly with the glossy 3D green '1'/count badge
    if (ownedCount > 0) {
      val drawW = maxWidth
      val drawH = drawW / drawableAspectRatio
      val topOffset = (maxHeight - drawH) * 0.5f
      val badgeSize = drawW * badgeDiamFrac
      val badgeX = drawW * badgeCenterXFrac - badgeSize * 0.5f
      val badgeY = topOffset + drawH * badgeCenterYFrac - badgeSize * 0.5f

      Box(
        modifier = Modifier.align(Alignment.TopStart).offset(x = badgeX, y = badgeY).size(badgeSize),
        contentAlignment = Alignment.Center,
      ) {
        if (ownedCount == 1) {
          Image(
            painter = painterResource(id = R.drawable.badge_booster_green_1),
            contentDescription = "1 available",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
        } else {
          Image(
            painter = painterResource(id = R.drawable.badge_booster_green_empty),
            contentDescription = "$ownedCount available",
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
          )
          val countStyle =
            MaterialTheme.typography.labelLarge.copy(
              fontFamily = FredokaFontFamily,
              fontSize = if (ownedCount >= 10) 13.sp else 16.sp,
              fontWeight = FontWeight.ExtraBold,
            )
          Text(
            text = ownedCount.toString(),
            style = countStyle.copy(drawStyle = Stroke(width = 6f, join = StrokeJoin.Round)),
            color = Color(0xFF0D3E00),
            maxLines = 1,
          )
          Text(
            text = ownedCount.toString(),
            style = countStyle,
            color = Color.White,
            maxLines = 1,
          )
        }
      }
    }
  }
}

@Composable
private fun AnimatedRoadBeltDashes(
  isPlaying: Boolean,
  isSpeedBoosted: Boolean,
  modifier: Modifier = Modifier,
) {
  var beltOffsetDp by remember { mutableFloatStateOf(0f) }
  var currentSpeedDp by remember { mutableFloatStateOf(42f) }
  val latestSpeedBoosted by rememberUpdatedState(isSpeedBoosted)

  LaunchedEffect(isPlaying) {
    if (!isPlaying) return@LaunchedEffect
    var lastFrameNanos = withFrameNanos { it }
    while (true) {
      val nowNanos = withFrameNanos { it }
      val dt = ((nowNanos - lastFrameNanos) / 1_000_000_000f).coerceIn(0f, 0.05f)
      lastFrameNanos = nowNanos
      val targetSpeed = if (latestSpeedBoosted) 126f else 42f
      currentSpeedDp += (targetSpeed - currentSpeedDp) * (dt * 14f).coerceAtMost(1f)
      beltOffsetDp = (beltOffsetDp + dt * currentSpeedDp) % 10_000f
    }
  }

  val leftCtrlNorm = remember {
    listOf(
      -0.0450f to -0.1201f,
      -0.0350f to 0.0901f,
      -0.0120f to 0.2853f,
      0.0240f to 0.4324f,
      0.0820f to 0.5345f,
      0.1500f to 0.5766f,
      0.2180f to 0.6006f,
    )
  }
  val rightCtrlNorm = remember(leftCtrlNorm) {
    leftCtrlNorm.map { (nx, ny) -> (1f - nx) to ny }
  }

  val pathCache = remember { RoadDashPathCache() }

  Canvas(modifier = modifier) {
    val w = size.width
    val h = size.height
    if (w <= 1f || h <= 1f) return@Canvas

    if (pathCache.width != w || pathCache.height != h) {
      val cx = 0.500f * w
      val cy = 0.6404f * h
      val rx = 0.2330f * w
      val ry = 0.2418f * h

      val ring =
        Path().apply {
          val steps = 144
          for (i in 0 until steps) {
            val theta = (2.0 * PI * i) / steps
            val px = cx + rx * cos(theta).toFloat()
            val py = cy + ry * sin(theta).toFloat()
            if (i == 0) moveTo(px, py) else lineTo(px, py)
          }
          close()
        }
      val ringMeasure = PathMeasure().apply { setPath(ring, true) }
      pathCache.width = w
      pathCache.height = h
      pathCache.ringPath = ring
      pathCache.ringLenPx = ringMeasure.length
      pathCache.leftArmPath = buildRoadArmDashPath(leftCtrlNorm, w, h)
      pathCache.rightArmPath = buildRoadArmDashPath(rightCtrlNorm, w, h)
    }

    val ringLenPx = pathCache.ringLenPx
    if (ringLenPx <= 1f) return@Canvas

    val targetPeriodPx = 22.dp.toPx()
    val periodCount = (ringLenPx / targetPeriodPx).roundToInt().coerceAtLeast(16)
    val exactPeriodPx = ringLenPx / periodCount
    val dashPx = exactPeriodPx * 0.44f
    val gapPx = (exactPeriodPx - dashPx).coerceAtLeast(1f)
    val phasePx = -(beltOffsetDp.dp.toPx() % exactPeriodPx)

    val strokeWidthPx = 2.8.dp.toPx()
    val dashIntervals = floatArrayOf(dashPx, gapPx)
    val ringDashStroke =
      Stroke(
        width = strokeWidthPx,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
        pathEffect = PathEffect.dashPathEffect(dashIntervals, phasePx),
      )
    val staticArmDashStroke =
      Stroke(
        width = strokeWidthPx,
        cap = StrokeCap.Round,
        join = StrokeJoin.Round,
        pathEffect = PathEffect.dashPathEffect(dashIntervals, 0f),
      )
    val dashColor = Color(0xFF7D85B2).copy(alpha = 0.92f)

    // Clockwise rotating center oval dashes only
    drawPath(path = pathCache.ringPath, color = dashColor, style = ringDashStroke)
    // Left and right feeder arm dashes remain stationary
    drawPath(path = pathCache.leftArmPath, color = dashColor, style = staticArmDashStroke)
    drawPath(path = pathCache.rightArmPath, color = dashColor, style = staticArmDashStroke)
  }
}

private class RoadDashPathCache {
  var width: Float = -1f
  var height: Float = -1f
  var ringLenPx: Float = 0f
  var ringPath: Path = Path()
  var leftArmPath: Path = Path()
  var rightArmPath: Path = Path()
}

private fun buildRoadArmDashPath(
  ctrlNorm: List<Pair<Float, Float>>,
  widthPx: Float,
  heightPx: Float,
): Path {
  val n = ctrlNorm.size
  val stepsPerSeg = 24
  val raw = ArrayList<Pair<Float, Float>>((n - 1) * stepsPerSeg + 1)
  for (i in 0 until n - 1) {
    val p0 = if (i == 0) ctrlNorm[0] else ctrlNorm[i - 1]
    val p1 = ctrlNorm[i]
    val p2 = ctrlNorm[i + 1]
    val p3 = if (i + 2 < n) ctrlNorm[i + 2] else ctrlNorm[n - 1]
    for (s in 0 until stepsPerSeg) {
      val t = s.toFloat() / stepsPerSeg
      val t2 = t * t
      val t3 = t2 * t
      val nx =
        0.5f *
          (
            (2f * p1.first) +
              (-p0.first + p2.first) * t +
              (2f * p0.first - 5f * p1.first + 4f * p2.first - p3.first) * t2 +
              (-p0.first + 3f * p1.first - 3f * p2.first + p3.first) * t3
          )
      val ny =
        0.5f *
          (
            (2f * p1.second) +
              (-p0.second + p2.second) * t +
              (2f * p0.second - 5f * p1.second + 4f * p2.second - p3.second) * t2 +
              (-p0.second + 3f * p1.second - 3f * p2.second + p3.second) * t3
          )
      raw.add((nx * widthPx) to (ny * heightPx))
    }
  }
  raw.add((ctrlNorm.last().first * widthPx) to (ctrlNorm.last().second * heightPx))

  val stopIdx = (raw.size * 0.89f).roundToInt().coerceIn(2, raw.size)
  return Path().apply {
    for (i in 0 until stopIdx) {
      val (px, py) = raw[i]
      if (i == 0) moveTo(px, py) else lineTo(px, py)
    }
  }
}

