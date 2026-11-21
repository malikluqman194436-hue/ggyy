package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.GameOutcome
import com.example.game.GameUiState
import com.example.game.GameViewModel
import com.example.ui.components.BoosterPurchaseDialog
import com.example.ui.components.CoinPill
import com.example.ui.components.ConveyorTrackView
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

  // Smooth 60fps simulation ticker
  LaunchedEffect(uiState.outcome, uiState.levelNumber) {
    if (uiState.outcome != GameOutcome.PLAYING) return@LaunchedEffect
    var lastNanos = 0L
    while (true) {
      withFrameNanos { frameNanos ->
        if (lastNanos != 0L) {
          val dt = (frameNanos - lastNanos) / 1_000_000_000f
          viewModel.onFrame(dt)
        }
        lastNanos = frameNanos
      }
    }
  }

  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .focusRequester(focusRequester)
        .focusable()
        .onPreviewKeyEvent { event ->
          if (event.type == KeyEventType.KeyDown && event.key == Key.D) {
            viewModel.toggleLaneDebug()
            true
          } else {
            false
          }
        }
  ) {
    val screenH = maxHeight

    // 1. Exact full-bleed gameplay background (glossy racetrack + waiting bays + yellow curbs + corner park bushes)
    Image(
      painter = painterResource(id = R.drawable.game_play_bg),
      contentDescription = null,
      contentScale = ContentScale.FillBounds,
      modifier = Modifier.fillMaxSize(),
    )

    // 2. ZONE 1: Passengers circulating along the background's oval road loop & left/right arms
    ConveyorTrackView(
      passengers = uiState.passengers,
      conveyorOffsetPx = uiState.conveyorOffsetPx,
      remainingCount = uiState.passengers.size,
      showLaneDebug = uiState.showLaneDebug,
      onViewportSized = { w, h -> viewModel.updateLaneViewport(w, h) },
      modifier = Modifier.fillMaxSize(),
    )

    // 3. WAITING BAYS: 8 tilted bays aligned 1-to-1 with the background stand slots
    WaitingSlotsRow(
      slots = uiState.slots,
      vehicles = uiState.vehicles,
      jumpingPassengers = uiState.jumpingPassengers,
      onSlotTap = { viewModel.onSlotTapped(it) },
      modifier = Modifier.fillMaxSize(),
    )

    // 4. ZONE 2: Middle Parking Lot + Tunnels + Vehicles positioned cleanly inside the grey asphalt area
    Box(
      modifier =
        Modifier
          .offset(y = screenH * 0.452f)
          .fillMaxWidth()
          .height(screenH * 0.455f)
    ) {
      ParkingLotCanvas(
        gridCols = uiState.gridCols,
        gridRows = uiState.gridRows,
        vehicles = uiState.vehicles,
        tunnels = uiState.tunnels,
        tutorialVehicleId = uiState.tutorialVehicleId,
        onLotTap = { gx, gy -> viewModel.onLotTap(gx, gy) },
        modifier = Modifier.fillMaxSize(),
      )
    }

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

    // 7. Full-bleed Playfield Effects Overlay (Driving buses + dotted trail, jumping stickmen, confetti, "+1")
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
      coins = uiState.coins,
      notice = uiState.shopNotice,
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
