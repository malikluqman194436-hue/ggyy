package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
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

  BackHandler(enabled = showSettings || showHelp || showShop || showNoAds || showStarterPack) {
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
              .clickable(interactionSource = levelBtnInteraction, indication = null) {
                onPlayLevel(progress.currentLevel)
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
        onToggleSound = onToggleSound,
        onToggleVibration = onToggleVibration,
        onToggleCloudSave = onToggleCloudSave,
        onCycleLanguage = onCycleLanguage,
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
