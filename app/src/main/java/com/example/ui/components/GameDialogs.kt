package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Typeface
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.res.ResourcesCompat
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.game.BoosterType
import com.example.ui.theme.AlertRed
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.GoldCoin
import com.example.ui.theme.GoldCoinDark
import com.example.ui.theme.HudBlue
import com.example.ui.theme.HudBlueDark
import com.example.ui.theme.LilitaOneFontFamily
import com.example.ui.theme.OutlineInk
import com.example.ui.theme.PlayGreen
import com.example.ui.theme.PlayGreenDark
import com.example.ui.theme.ShopPurple
import com.example.ui.theme.ShopPurpleDark
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun GlossyButton(
  text: String,
  topColor: Color,
  bottomColor: Color,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
  testTag: String? = null,
) {
  val interactionSource = remember { MutableInteractionSource() }
  val isPressed by interactionSource.collectIsPressedAsState()
  val scale by animateFloatAsState(if (isPressed) 0.92f else 1f, spring(), label = "btn_scale")

  Box(
    modifier =
      modifier
        .scale(scale)
        .then(if (testTag != null) Modifier.testTag(testTag) else Modifier)
        .clip(RoundedCornerShape(18.dp))
        .background(bottomColor)
        .padding(bottom = 5.dp)
        .clip(RoundedCornerShape(16.dp))
        .background(topColor)
        .border(2.5.dp, OutlineInk, RoundedCornerShape(16.dp))
        .clickable(interactionSource = interactionSource, indication = null, onClick = onClick)
        .padding(horizontal = 18.dp, vertical = 12.dp),
    contentAlignment = Alignment.Center,
  ) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
      if (icon != null) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(6.dp))
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge.copy(fontFamily = FredokaFontFamily, fontSize = 16.sp),
        color = Color.White,
        maxLines = 1,
      )
    }
  }
}

@Composable
fun CoinPill(
  coins: Int,
  onAddClick: () -> Unit,
  modifier: Modifier = Modifier,
  pillWidth: Dp = 132.dp,
) {
  val visualHeight = pillWidth * (108f / 363f)
  val touchHeight = maxOf(visualHeight, 48.dp)
  Box(
    modifier =
      modifier
        .width(pillWidth)
        .height(touchHeight)
        .clickable(onClick = onAddClick)
        .testTag("coin_pill"),
    contentAlignment = Alignment.Center,
  ) {
    Box(
      modifier = Modifier.width(pillWidth).height(visualHeight),
      contentAlignment = Alignment.Center,
    ) {
      Image(
        painter = painterResource(id = R.drawable.btn_hud_coin),
        contentDescription = "Coins: $coins",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
      )
      val baseSize =
        if (pillWidth < 120.dp) {
          if (coins >= 1000) 11.5.sp else 13.5.sp
        } else {
          if (coins >= 1000) 13.sp else 16.sp
        }
      Text(
        text = coins.toString(),
        style =
          MaterialTheme.typography.labelLarge.copy(
            fontFamily = FredokaFontFamily,
            fontSize = baseSize,
            fontWeight = FontWeight.Bold,
            shadow = Shadow(color = OutlineInk, offset = Offset(0f, 2f), blurRadius = 2f),
          ),
        color = Color.White,
        maxLines = 1,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(start = pillWidth * 0.27f, end = pillWidth * 0.25f),
      )
    }
  }
}

@Composable
fun CartoonPopupCard(
  title: String,
  headerColor: Color = HudBlue,
  onDismiss: () -> Unit,
  content: @Composable () -> Unit,
) {
  Dialog(onDismissRequest = onDismiss) {
    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .clip(RoundedCornerShape(26.dp))
          .background(Color.White)
          .border(3.dp, OutlineInk, RoundedCornerShape(26.dp)),
    ) {
      Box(
        modifier =
          Modifier
            .fillMaxWidth()
            .background(headerColor)
            .padding(vertical = 14.dp, horizontal = 18.dp),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge.copy(fontFamily = FredokaFontFamily),
          color = Color.White,
          textAlign = TextAlign.Center,
        )
      }
      Column(
        modifier = Modifier.fillMaxWidth().padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        content()
      }
    }
  }
}

@Composable
fun WinLevelDialog(
  levelNumber: Int,
  @Suppress("UNUSED_PARAMETER") stars: Int,
  onNextLevel: () -> Unit,
  onClaimBonusAndNext: () -> Unit = onNextLevel,
  @Suppress("UNUSED_PARAMETER") onHome: () -> Unit = {},
  modifier: Modifier = Modifier,
) {
  val backdropInteraction = remember { MutableInteractionSource() }
  val cardInteraction = remember { MutableInteractionSource() }
  val cardPopScale = remember { Animatable(0.76f) }

  LaunchedEffect(Unit) {
    cardPopScale.animateTo(
      targetValue = 1f,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMedium,
        ),
    )
  }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.85f))
        .clickable(
          interactionSource = backdropInteraction,
          indication = null,
          onClick = {},
        )
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.displayCutout)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 16.dp, vertical = 16.dp)
        .testTag("win_dim_backdrop"),
    contentAlignment = Alignment.Center,
  ) {
    val cardAspect = 408f / 605f
    val targetCardW = minOf(maxWidth * 0.88f, 365.dp, (maxHeight * 0.90f) * cardAspect)
    BoxWithConstraints(
      modifier =
        Modifier
          .width(targetCardW)
          .aspectRatio(cardAspect)
          .scale(cardPopScale.value)
          .clickable(
            interactionSource = cardInteraction,
            indication = null,
            onClick = {},
          )
          .testTag("win_level_dialog"),
    ) {
      val cardW = maxWidth
      val cardH = maxHeight

      Image(
        painter = painterResource(id = R.drawable.popup_level_win),
        contentDescription = "Level $levelNumber Complete",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
      )

      // Crisp "Perfect!" text extracted from reference card, centered between the two sparkles
      val perfectW = cardW * 0.465f
      val perfectH = perfectW * (228f / 798f)
      Image(
        painter = painterResource(id = R.drawable.txt_win_perfect),
        contentDescription = stringResource(R.string.win_perfect_title),
        contentScale = ContentScale.Fit,
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .offset(y = cardH * 0.258f - perfectH * 0.5f)
            .size(width = perfectW, height = perfectH)
            .testTag("win_perfect_logo"),
      )

      // Golden reward panel with "+20" gold coin pile centered below Perfect logo
      val rewardPanelW = cardW * 0.78f
      val rewardPanelH = rewardPanelW * (358f / 596f)
      val coinsH = rewardPanelH * 0.84f
      val coinsW = coinsH * (562f / 396f)
      Box(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .offset(y = cardH * 0.382f)
            .size(width = rewardPanelW, height = rewardPanelH)
            .testTag("win_reward_panel"),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          painter = painterResource(id = R.drawable.panel_win_reward),
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize(),
        )
        Image(
          painter = painterResource(id = R.drawable.img_win_coins_20),
          contentDescription = "+20 Coins Reward",
          contentScale = ContentScale.Fit,
          modifier =
            Modifier
              .offset(y = rewardPanelH * 0.015f)
              .size(width = coinsW, height = coinsH)
              .testTag("win_reward_coins"),
        )
      }

      // Red "NEXT" and blue "Getx20 400" buttons side-by-side below the reward panel like reference
      val btnW = cardW * 0.405f
      val btnGap = cardW * 0.028f
      val nextH = btnW * (180f / 425f)
      val getBonusH = btnW * (181f / 425f)
      val btnTouchH = maxOf(nextH, getBonusH, 48.dp)

      val nextInteraction = remember { MutableInteractionSource() }
      val nextPressed by nextInteraction.collectIsPressedAsState()
      val nextScale by animateFloatAsState(
        if (nextPressed) 0.92f else 1f,
        spring(),
        label = "win_next_scale",
      )

      val getBonusInteraction = remember { MutableInteractionSource() }
      val getBonusPressed by getBonusInteraction.collectIsPressedAsState()
      val getBonusScale by animateFloatAsState(
        if (getBonusPressed) 0.92f else 1f,
        spring(),
        label = "win_get_bonus_scale",
      )

      Row(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .offset(y = cardH * 0.822f - btnTouchH * 0.5f)
            .width(btnW * 2f + btnGap)
            .height(btnTouchH),
        horizontalArrangement = Arrangement.spacedBy(btnGap),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        Box(
          modifier =
            Modifier
              .size(width = btnW, height = btnTouchH)
              .scale(nextScale)
              .clickable(
                interactionSource = nextInteraction,
                indication = null,
                onClick = onNextLevel,
              )
              .testTag("win_next_button"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.btn_win_next),
            contentDescription = stringResource(R.string.win_next_button),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = btnW, height = nextH),
          )
        }

        Box(
          modifier =
            Modifier
              .size(width = btnW, height = btnTouchH)
              .scale(getBonusScale)
              .clickable(
                interactionSource = getBonusInteraction,
                indication = null,
                onClick = onClaimBonusAndNext,
              )
              .testTag("win_get_bonus_button"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.btn_win_get_bonus),
            contentDescription = stringResource(R.string.win_get_bonus_button),
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = btnW, height = getBonusH),
          )
        }
      }

      val context = LocalContext.current
      val lilitaTypeface = remember(context) {
        ResourcesCompat.getFont(context, R.font.lilita_one) ?: Typeface.DEFAULT_BOLD
      }

      Canvas(
        modifier =
          Modifier
            .fillMaxSize()
            .semantics { contentDescription = "LEVEL $levelNumber" }
            .testTag("win_level_ribbon_title"),
      ) {
        val w = size.width
        val h = size.height
        val label = "LEVEL $levelNumber"

        val basePaint =
          Paint(Paint.ANTI_ALIAS_FLAG).apply {
            typeface = lilitaTypeface
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.02f
          }

        // Fit cleanly between the two yellow stars & sparkles on the ribbon
        var textSizePx = w * 0.118f
        basePaint.textSize = textSizePx
        val maxRibbonTextW = w * 0.41f
        val measured = basePaint.measureText(label)
        if (measured > maxRibbonTextW && measured > 0f) {
          textSizePx *= (maxRibbonTextW / measured)
          basePaint.textSize = textSizePx
        }

        // Gentle convex upward arch following the blue ribbon contour
        val arcStartX = w * 0.25f
        val arcEndX = w * 0.75f
        val endBaselineY = h * 0.104f
        val midBaselineY = h * 0.097f
        val ctrlY = 2f * midBaselineY - endBaselineY
        val ribbonArcPath =
          AndroidPath().apply {
            moveTo(arcStartX, endBaselineY)
            quadTo(w * 0.50f, ctrlY, arcEndX, endBaselineY)
          }

        val shadowPaint =
          Paint(basePaint).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = textSizePx * 0.20f
            color = Color(0xFF031845).toArgb()
          }
        val outlinePaint =
          Paint(basePaint).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = textSizePx * 0.18f
            color = Color(0xFF062666).toArgb()
          }
        val fillPaint =
          Paint(basePaint).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.WHITE
          }

        drawIntoCanvas { canvas ->
          val native = canvas.nativeCanvas
          native.drawTextOnPath(label, ribbonArcPath, 0f, h * 0.0045f, shadowPaint)
          native.drawTextOnPath(label, ribbonArcPath, 0f, 0f, outlinePaint)
          native.drawTextOnPath(label, ribbonArcPath, 0f, 0f, fillPaint)
        }
      }
    }
  }
}

@Composable
fun LoseLevelDialog(
  coins: Int,
  notice: String?,
  onRetry: () -> Unit,
  onRevive: () -> Unit,
  onHome: () -> Unit,
) {
  CartoonPopupCard(title = "LEVEL FAILED", headerColor = AlertRed, onDismiss = {}) {
    Text(
      text = "All waiting slots are full and no parked bus matches the next passenger!",
      style = MaterialTheme.typography.bodyLarge,
      color = OutlineInk,
      textAlign = TextAlign.Center,
    )
    if (notice != null) {
      Spacer(Modifier.height(8.dp))
      Text(
        text = notice,
        style = MaterialTheme.typography.labelLarge,
        color = AlertRed,
      )
    }
    Spacer(Modifier.height(16.dp))
    GlossyButton(
      text = "Revive (+1 Slot) • 200",
      topColor = GoldCoin,
      bottomColor = GoldCoinDark,
      onClick = onRevive,
      modifier = Modifier.fillMaxWidth(),
      testTag = "revive_button",
    )
    Spacer(Modifier.height(10.dp))
    GlossyButton(
      text = "Retry Level",
      topColor = HudBlue,
      bottomColor = HudBlueDark,
      onClick = onRetry,
      modifier = Modifier.fillMaxWidth(),
      testTag = "retry_button",
    )
    Spacer(Modifier.height(10.dp))
    GlossyButton(
      text = "Home",
      topColor = Color(0xFF7B8BA3),
      bottomColor = Color(0xFF566376),
      onClick = onHome,
      modifier = Modifier.fillMaxWidth(),
      testTag = "lose_home_button",
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = "Your Coins: $coins",
      style = MaterialTheme.typography.labelSmall,
      color = Color(0xFF5C6C84),
    )
  }
}

@Composable
fun PauseDialog(
  soundEnabled: Boolean,
  vibrationEnabled: Boolean,
  cloudSaveEnabled: Boolean,
  languageLabel: String,
  onToggleSound: (Boolean) -> Unit,
  onToggleVibration: (Boolean) -> Unit,
  onToggleCloudSave: (Boolean) -> Unit,
  onCycleLanguage: () -> Unit,
  onOpenSupport: () -> Unit,
  onResume: () -> Unit,
  onRestart: () -> Unit,
  onHome: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingsPopupOverlay(
    soundEnabled = soundEnabled,
    vibrationEnabled = vibrationEnabled,
    cloudSaveEnabled = cloudSaveEnabled,
    languageLabel = languageLabel,
    onToggleSound = onToggleSound,
    onToggleVibration = onToggleVibration,
    onToggleCloudSave = onToggleCloudSave,
    onCycleLanguage = onCycleLanguage,
    onOpenSupport = onOpenSupport,
    onDismiss = onResume,
    onRestart = onRestart,
    onHome = onHome,
    modifier = modifier,
  )
}

@Composable
fun SettingsPopupOverlay(
  soundEnabled: Boolean,
  vibrationEnabled: Boolean,
  cloudSaveEnabled: Boolean,
  languageLabel: String,
  onToggleSound: (Boolean) -> Unit,
  onToggleVibration: (Boolean) -> Unit,
  onToggleCloudSave: (Boolean) -> Unit,
  onCycleLanguage: () -> Unit,
  onOpenSupport: () -> Unit,
  onDismiss: () -> Unit,
  onRestart: () -> Unit,
  onHome: (() -> Unit)? = null,
  modifier: Modifier = Modifier,
) {
  val backdropInteraction = remember { MutableInteractionSource() }
  val cardInteraction = remember { MutableInteractionSource() }
  val cardPopScale = remember { Animatable(0.76f) }
  var showLevelsPanel by remember { mutableStateOf(false) }

  BackHandler(enabled = showLevelsPanel) {
    showLevelsPanel = false
  }

  LaunchedEffect(Unit) {
    cardPopScale.animateTo(
      targetValue = 1f,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMedium,
        ),
    )
  }

  val isHomeSettings = onHome == null
  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = if (isHomeSettings) 0.78f else 0.87f))
        .clickable(
          interactionSource = backdropInteraction,
          indication = null,
          onClick = onDismiss,
        )
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.displayCutout)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 8.dp, vertical = 10.dp)
        .testTag("settings_dim_backdrop"),
    contentAlignment = Alignment.Center,
  ) {
    val cardAspect = if (isHomeSettings) 950f / 1050f else 1058f / 1442f
    val targetCardW =
      if (isHomeSettings) {
        minOf(maxWidth * 0.85f, 390.dp, (maxHeight * 0.56f) * cardAspect)
      } else {
        minOf(maxWidth * 0.94f, 420.dp, (maxHeight * 0.90f) * cardAspect)
      }
    BoxWithConstraints(
      modifier =
        Modifier
          .width(targetCardW)
          .aspectRatio(cardAspect)
          .scale(cardPopScale.value)
          .clickable(
            interactionSource = cardInteraction,
            indication = null,
            onClick = {},
          )
          .testTag("settings_pause_dialog"),
    ) {
      val cardW = maxWidth
      val cardH = maxHeight

      Image(
        painter =
          painterResource(
            id = if (isHomeSettings) R.drawable.popup_settings_home else R.drawable.popup_settings,
          ),
        contentDescription = "Settings",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
      )

      // Top-right glossy red 'X' close button protruding cleanly outward on the ribbon corner
      val closeTouchSize = maxOf(48.dp, cardW * 0.165f)
      val closeVisualSize = cardW * 0.142f
      val closeInteraction = remember { MutableInteractionSource() }
      val closePressed by closeInteraction.collectIsPressedAsState()
      val closeScale by animateFloatAsState(if (closePressed) 0.90f else 1f, spring(), label = "settings_close_scale")

      Box(
        modifier =
          Modifier
            .offset(
              x = cardW * (if (isHomeSettings) 0.956f else 0.946f) - closeTouchSize / 2f,
              y = cardH * (if (isHomeSettings) 0.169f else 0.114f) - closeTouchSize / 2f,
            )
            .size(closeTouchSize)
            .scale(closeScale)
            .clickable(
              interactionSource = closeInteraction,
              indication = null,
              onClick = onDismiss,
            )
            .semantics { contentDescription = "Close Settings" }
            .testTag("settings_close_button"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier =
            Modifier
              .size(closeVisualSize + 3.dp)
              .clip(CircleShape)
              .background(Color(0xFF131D3A)),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.btn_settings_close),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(closeVisualSize),
          )
        }
      }

      // Off-white/cream rounded tray behind the 3 top grouped toggle icons (neatly inset inside blue frame)
      val trayW = cardW * (if (isHomeSettings) 0.790f else 0.800f)
      val trayH = cardH * (if (isHomeSettings) 0.176f else 0.156f)
      val trayTopY = cardH * (if (isHomeSettings) 0.262f else 0.180f)
      val trayX = (cardW - trayW) * 0.5f
      Canvas(
        modifier =
          Modifier
            .offset(x = trayX, y = trayTopY)
            .size(width = trayW, height = trayH)
            .testTag("settings_top_cream_tray"),
      ) {
        val w = size.width
        val h = size.height
        val corner = CornerRadius(h * 0.17f, h * 0.17f)
        val shadowDrop = 3.5.dp.toPx()
        val bevelPx = 2.5.dp.toPx()
        val insetPx = 1.5.dp.toPx()

        // Subtle drop shadow under tray
        drawRoundRect(
          color = Color(0x4702245C),
          topLeft = Offset(0f, shadowDrop),
          size = Size(w, h),
          cornerRadius = corner,
        )
        // Soft warm tan-cream bottom bevel
        drawRoundRect(
          color = Color(0xFFD6AB7B),
          topLeft = Offset.Zero,
          size = Size(w, h),
          cornerRadius = corner,
        )
        // Crisp bright off-white top highlight rim
        drawRoundRect(
          color = Color(0xFFFFFAF2),
          topLeft = Offset.Zero,
          size = Size(w, h - bevelPx),
          cornerRadius = corner,
        )
        // Soft off-white cream interior matching reference
        drawRoundRect(
          brush =
            Brush.verticalGradient(
              colors = listOf(Color(0xFFFCF2E3), Color(0xFFF6E3CA)),
            ),
          topLeft = Offset(insetPx, insetPx),
          size = Size(w - insetPx * 2f, h - insetPx - bevelPx),
          cornerRadius = CornerRadius(maxOf(4f, corner.x - insetPx), maxOf(4f, corner.y - insetPx)),
        )

        // Faint decorative stars inside the off-white cream tray matching reference
        val starSpots =
          listOf(
            Triple(w * 0.08f, h * 0.24f, h * 0.085f),
            Triple(w * 0.35f, h * 0.18f, h * 0.062f),
            Triple(w * 0.63f, h * 0.80f, h * 0.068f),
            Triple(w * 0.91f, h * 0.26f, h * 0.082f),
          )
        for ((sx, sy, sr) in starSpots) {
          val starPath =
            Path().apply {
              for (p in 0 until 10) {
                val r = if (p % 2 == 0) sr else sr * 0.46f
                val angle = -PI / 2.0 + p * (PI / 5.0)
                val px = sx + (r * cos(angle)).toFloat()
                val py = sy + (r * sin(angle)).toFloat()
                if (p == 0) moveTo(px, py) else lineTo(px, py)
              }
              close()
            }
          drawPath(starPath, color = Color(0xFFEBD0AC).copy(alpha = 0.50f))
        }
      }

      // Top row: Slot 1 = Volume, Slot 2 = Vibrate, Slot 3 = Guide (evenly centered inside cream tray)
      val iconBoxW = cardW * (if (isHomeSettings) 0.185f else 0.170f)
      val iconBoxH = maxOf(48.dp, if (isHomeSettings) trayH * 0.72f else iconBoxW * 0.94f)
      val iconTopY = trayTopY + (trayH - iconBoxH) * 0.5f
      val iconGap = (trayW - iconBoxW * 3f) / 4f
      val slot1X = trayX + iconGap
      val slot2X = slot1X + iconBoxW + iconGap
      val slot3X = slot2X + iconBoxW + iconGap

      // 1. Volume (ON / OFF muted speaker)
      SettingsSpriteButton(
        drawableRes = if (soundEnabled) R.drawable.btn_settings_sound_on else R.drawable.btn_settings_sound_off,
        contentDescription = if (soundEnabled) "Sound On" else "Sound Off",
        onClick = { onToggleSound(!soundEnabled) },
        testTag = "settings_sound_toggle",
        modifier =
          Modifier
            .offset(x = slot1X, y = iconTopY)
            .size(width = iconBoxW, height = iconBoxH),
      )

      // 2. Vibrate (diagonal red slash overlay when toggled OFF)
      Box(
        modifier =
          Modifier
            .offset(x = slot2X, y = iconTopY)
            .size(width = iconBoxW, height = iconBoxH),
        contentAlignment = Alignment.Center,
      ) {
        SettingsSpriteButton(
          drawableRes = R.drawable.btn_settings_vibrate,
          contentDescription = if (vibrationEnabled) "Vibration On" else "Vibration Off",
          onClick = { onToggleVibration(!vibrationEnabled) },
          testTag = "settings_vibration_toggle",
          modifier = Modifier.fillMaxSize(),
        )
        if (!vibrationEnabled) {
          androidx.compose.foundation.Canvas(modifier = Modifier.fillMaxSize(0.72f)) {
            drawLine(
              color = AlertRed,
              start = Offset(size.width * 0.14f, size.height * 0.86f),
              end = Offset(size.width * 0.86f, size.height * 0.14f),
              strokeWidth = size.minDimension * 0.12f,
              cap = StrokeCap.Round,
            )
          }
        }
      }

      // 3. Guide / Tropical Themes Panel icon (opens sliding inner panel)
      SettingsSpriteButton(
        drawableRes = R.drawable.btn_settings_guide,
        contentDescription = "Themes & Stages",
        onClick = { showLevelsPanel = true },
        testTag = "settings_guide_button",
        modifier =
          Modifier
            .offset(x = slot3X, y = iconTopY)
            .size(width = iconBoxW, height = iconBoxH),
      )

      // Middle vertical stack: Home + Replay + Support + Language in-game, Support + Language on Home
      val stackBtnW = cardW * (if (isHomeSettings) 0.44f else 0.395f)
      val stackBtnH = maxOf(48.dp, stackBtnW * (146f / 394f))
      val stackX = (cardW - stackBtnW) * 0.5f

      if (onHome != null) {
        SettingsSpriteButton(
          drawableRes = R.drawable.btn_settings_home,
          contentDescription = "Home",
          onClick = onHome,
          testTag = "pause_home_button",
          modifier =
            Modifier
              .offset(x = stackX, y = cardH * 0.352f)
              .size(width = stackBtnW, height = stackBtnH),
        )

        SettingsSpriteButton(
          drawableRes = R.drawable.btn_settings_replay,
          contentDescription = "Replay Level",
          onClick = onRestart,
          testTag = "restart_button",
          modifier =
            Modifier
              .offset(x = stackX, y = cardH * 0.471f)
              .size(width = stackBtnW, height = stackBtnH),
        )
      }

      val supportY = if (isHomeSettings) cardH * 0.466f else cardH * 0.590f
      val langY = if (isHomeSettings) cardH * 0.632f else cardH * 0.709f

      SettingsSpriteButton(
        drawableRes = R.drawable.btn_settings_support,
        contentDescription = "Support and Help",
        onClick = onOpenSupport,
        testTag = "settings_support_button",
        modifier =
          Modifier
            .offset(x = stackX, y = supportY)
            .size(width = stackBtnW, height = stackBtnH),
      )

      Box(
        modifier =
          Modifier
            .offset(x = stackX, y = langY)
            .size(width = stackBtnW, height = stackBtnH),
        contentAlignment = Alignment.Center,
      ) {
        SettingsSpriteButton(
          drawableRes = R.drawable.btn_settings_language,
          contentDescription = "Language: $languageLabel",
          onClick = onCycleLanguage,
          testTag = "settings_language_button",
          modifier = Modifier.fillMaxSize(),
        )
        if (languageLabel != "English") {
          Box(
            modifier =
              Modifier
                .width(stackBtnW * 0.80f)
                .height(stackBtnH * 0.68f)
                .clip(RoundedCornerShape(14.dp))
                .background(ShopPurple),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = languageLabel,
              style =
                MaterialTheme.typography.titleMedium.copy(
                  fontFamily = FredokaFontFamily,
                  fontWeight = FontWeight.ExtraBold,
                  shadow = Shadow(color = OutlineInk, offset = Offset(0f, 2f), blurRadius = 2f),
                ),
              color = Color.White,
              maxLines = 1,
            )
          }
        }
      }

      // Bottom row: Cloud Save label + ON / OFF toggle button positioned cleanly above bottom rim
      val cloudBtnW = cardW * 0.205f
      val cloudBtnVisualH = cloudBtnW * (131f / 194f)
      val cloudRowH = maxOf(48.dp, cloudBtnVisualH)
      val cloudCenterY = cardH * (if (isHomeSettings) 0.868f else 0.884f)
      Row(
        modifier =
          Modifier
            .offset(x = cardW * 0.130f, y = cloudCenterY - cloudRowH * 0.5f)
            .width(cardW * 0.740f)
            .height(cloudRowH),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Image(
          painter = painterResource(id = R.drawable.lbl_settings_cloud_save),
          contentDescription = "Cloud Save",
          contentScale = ContentScale.Fit,
          modifier = Modifier.weight(0.62f).height(cloudRowH * 0.64f),
        )
        Spacer(Modifier.width(cardW * 0.03f))
        SettingsSpriteButton(
          drawableRes = if (cloudSaveEnabled) R.drawable.btn_settings_cloud_on else R.drawable.btn_settings_cloud_off,
          contentDescription = if (cloudSaveEnabled) "Cloud Save On" else "Cloud Save Off",
          onClick = { onToggleCloudSave(!cloudSaveEnabled) },
          testTag = "settings_cloud_save_toggle",
          modifier = Modifier.width(cloudBtnW).height(cloudRowH),
        )
      }

      // Inner expanding/sliding Level/Theme Selection Panel triggered by top-right Volume/Vibrate companion button
      Box(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .offset(y = cardH * (if (isHomeSettings) 0.165f else 0.135f))
            .size(width = cardW * 0.88f, height = cardH * (if (isHomeSettings) 0.780f else 0.720f))
            .clip(RoundedCornerShape(cardW * 0.04f)),
        contentAlignment = Alignment.BottomCenter,
      ) {
        AnimatedVisibility(
          visible = showLevelsPanel,
          enter =
            slideInVertically(
              animationSpec =
                spring(
                  dampingRatio = 0.82f,
                  stiffness = Spring.StiffnessMediumLow,
                ),
              initialOffsetY = { (it * 0.88f).roundToInt() },
            ) +
              scaleIn(
                initialScale = 0.82f,
                transformOrigin = TransformOrigin(0.5f, 0.88f),
                animationSpec = tween(260, easing = FastOutSlowInEasing),
              ) +
              fadeIn(animationSpec = tween(180)),
          exit =
            slideOutVertically(
              animationSpec = tween(240, easing = FastOutSlowInEasing),
              targetOffsetY = { (it * 0.88f).roundToInt() },
            ) +
              scaleOut(
                targetScale = 0.82f,
                transformOrigin = TransformOrigin(0.5f, 0.88f),
                animationSpec = tween(220, easing = FastOutSlowInEasing),
              ) +
              fadeOut(animationSpec = tween(170)),
        ) {
          val panelH = cardH * (if (isHomeSettings) 0.750f else 0.690f)
          val panelW = panelH * (935f / 1253f)
          val panelCloseSize = panelW * 0.148f
          val panelCloseTouchSize = maxOf(48.dp, panelCloseSize)
          val panelInteraction = remember { MutableInteractionSource() }
          val panelCloseInteraction = remember { MutableInteractionSource() }
          val panelClosePressed by panelCloseInteraction.collectIsPressedAsState()
          val panelCloseScale by animateFloatAsState(
            targetValue = if (panelClosePressed) 0.90f else 1f,
            animationSpec = spring(),
            label = "levels_panel_close_scale",
          )

          Box(
            modifier =
              Modifier
                .size(width = panelW, height = panelH)
                .clickable(
                  interactionSource = panelInteraction,
                  indication = null,
                  onClick = {},
                )
                .testTag("settings_levels_panel"),
          ) {
            Image(
              painter = painterResource(id = R.drawable.panel_settings_levels),
              contentDescription = "Theme Stages",
              contentScale = ContentScale.Fit,
              modifier = Modifier.fillMaxSize(),
            )

            // Red glossy 'X' button from sprite sheet positioned at upper-right corner matching video
            Box(
              modifier =
                Modifier
                  .align(Alignment.TopStart)
                  .offset(
                    x = panelW * 0.962f - panelCloseTouchSize * 0.5f,
                    y = panelH * 0.044f - panelCloseTouchSize * 0.5f,
                  )
                  .size(panelCloseTouchSize)
                  .scale(panelCloseScale)
                  .clickable(
                    interactionSource = panelCloseInteraction,
                    indication = null,
                    onClick = { showLevelsPanel = false },
                  )
                  .semantics { contentDescription = "Close Stages Panel" }
                  .testTag("settings_levels_close_button"),
              contentAlignment = Alignment.Center,
            ) {
              Image(
                painter = painterResource(id = R.drawable.btn_panel_close_red),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(panelCloseSize),
              )
            }
          }
        }
      }
    }
  }
}

@Composable
private fun SettingsSpriteButton(
  drawableRes: Int,
  contentDescription: String,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by animateFloatAsState(if (pressed) 0.92f else 1f, spring(), label = "${testTag}_scale")

  Box(
    modifier =
      modifier
        .scale(scale)
        .clickable(
          interactionSource = interaction,
          indication = null,
          onClick = onClick,
        )
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

@Composable
fun SettingToggleRow(
  label: String,
  checked: Boolean,
  onCheckedChange: (Boolean) -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0xFFF1F6FC))
        .padding(horizontal = 14.dp, vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Text(text = label, style = MaterialTheme.typography.bodyLarge, color = OutlineInk)
    Switch(checked = checked, onCheckedChange = onCheckedChange)
  }
}

@Composable
fun HelpDialog(onDismiss: () -> Unit) {
  CartoonPopupCard(title = "HOW TO PLAY", headerColor = HudBlue, onDismiss = onDismiss) {
    HelpStepRow(
      step = "1",
      title = "Tap Clear Buses",
      desc = "Vehicles drive in the direction of their roof arrow. Tap unblocked vehicles to send them to the waiting bay.",
    )
    Spacer(Modifier.height(10.dp))
    HelpStepRow(
      step = "2",
      title = "Board Matching Passengers",
      desc = "Stickmen ride the top conveyor belt and jump into waiting buses of the same color.",
    )
    Spacer(Modifier.height(10.dp))
    HelpStepRow(
      step = "3",
      title = "Watch Slots & Tunnels",
      desc = "Don't fill all waiting slots with mismatched buses! Tunnels spawn extra vehicles once space opens.",
    )
    Spacer(Modifier.height(18.dp))
    GlossyButton(
      text = "Got It!",
      topColor = PlayGreen,
      bottomColor = PlayGreenDark,
      onClick = onDismiss,
      modifier = Modifier.fillMaxWidth(),
      testTag = "help_close_button",
    )
  }
}

@Composable
private fun HelpStepRow(step: String, title: String, desc: String) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0xFFF3F8FF))
        .padding(10.dp),
    verticalAlignment = Alignment.Top,
  ) {
    Box(
      modifier = Modifier.size(26.dp).clip(CircleShape).background(HudBlue),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = step,
        style = MaterialTheme.typography.labelLarge.copy(fontFamily = FredokaFontFamily),
        color = Color.White,
      )
    }
    Spacer(Modifier.width(10.dp))
    Column {
      Text(text = title, style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp), color = OutlineInk)
      Text(text = desc, style = MaterialTheme.typography.bodyMedium, color = Color(0xFF4A5870))
    }
  }
}

@Composable
fun BoosterPurchaseDialog(
  booster: BoosterType,
  coins: Int,
  notice: String?,
  onGetFree: () -> Unit,
  onBuy: () -> Unit,
  onDismiss: () -> Unit,
) {
  val drawableRes =
    when (booster) {
      BoosterType.BOARD -> R.drawable.popup_booster_board
      BoosterType.REFRESH -> R.drawable.popup_booster_refresh
      BoosterType.VIP_SLOT -> R.drawable.popup_booster_vip
    }
  val aspectRatio =
    when (booster) {
      BoosterType.BOARD -> 894f / 1124f
      BoosterType.REFRESH -> 890f / 1122f
      BoosterType.VIP_SLOT -> 890f / 1143f
    }
  val closeCenterY = if (booster == BoosterType.VIP_SLOT) 0.161f else 0.148f
  val buttonsTopY = if (booster == BoosterType.VIP_SLOT) 0.775f else 0.790f

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    Column(
      modifier = Modifier.widthIn(max = 350.dp).fillMaxWidth(0.86f),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      BoxWithConstraints(
        modifier =
          Modifier
            .fillMaxWidth()
            .aspectRatio(aspectRatio)
            .testTag("booster_popup_card"),
      ) {
        val cardW = maxWidth
        val cardH = maxHeight

        Image(
          painter = painterResource(id = drawableRes),
          contentDescription = booster.title,
          contentScale = ContentScale.Fit,
          modifier = Modifier.fillMaxSize(),
        )

        // Top-right red 'X' close button
        val closeSize = maxOf(48.dp, cardW * 0.145f)
        Box(
          modifier =
            Modifier
              .offset(
                x = cardW * 0.932f - closeSize / 2f,
                y = cardH * closeCenterY - closeSize / 2f,
              )
              .size(closeSize)
              .clip(CircleShape)
              .clickable(onClick = onDismiss)
              .semantics { contentDescription = "Close ${booster.title} popup" }
              .testTag("booster_popup_close"),
        )

        // Bottom-left 'Get' free booster button
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.105f, y = cardH * buttonsTopY)
              .size(width = cardW * 0.375f, height = maxOf(48.dp, cardH * 0.155f))
              .clip(RoundedCornerShape(16.dp))
              .clickable(onClick = onGetFree)
              .semantics { contentDescription = "Get ${booster.title}" }
              .testTag("booster_popup_get"),
        )

        // Bottom-right '200' coin purchase button
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.505f, y = cardH * buttonsTopY)
              .size(width = cardW * 0.385f, height = maxOf(48.dp, cardH * 0.155f))
              .clip(RoundedCornerShape(16.dp))
              .clickable(onClick = onBuy)
              .semantics { contentDescription = "Buy ${booster.title} for ${booster.costCoins} coins" }
              .testTag("buy_booster_confirm"),
        )
      }

      if (notice != null) {
        Spacer(Modifier.height(10.dp))
        Box(
          modifier =
            Modifier
              .clip(RoundedCornerShape(14.dp))
              .background(Color.White.copy(alpha = 0.96f))
              .border(2.dp, AlertRed, RoundedCornerShape(14.dp))
              .padding(horizontal = 14.dp, vertical = 6.dp),
        ) {
          Text(
            text = "$notice (Coins: $coins)",
            style = MaterialTheme.typography.labelLarge.copy(fontFamily = FredokaFontFamily),
            color = AlertRed,
            textAlign = TextAlign.Center,
          )
        }
      }
    }
  }
}

@Composable
fun UnlockSlotDialog(
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  val cardPopScale = remember { Animatable(0.80f) }
  LaunchedEffect(Unit) {
    cardPopScale.animateTo(
      targetValue = 1f,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMedium,
        ),
    )
  }

  Dialog(
    onDismissRequest = onDismiss,
    properties = DialogProperties(usePlatformDefaultWidth = false),
  ) {
    val cardAspect = 406f / 508f
    BoxWithConstraints(
      modifier =
        Modifier
          .widthIn(max = 360.dp)
          .fillMaxWidth(0.86f)
          .aspectRatio(cardAspect)
          .scale(cardPopScale.value)
          .testTag("unlock_slot_popup_card"),
    ) {
      val cardW = maxWidth
      val cardH = maxHeight

      // 1. Red "Parking Slot — Add one Parking Slot" panel artwork
      Image(
        painter = painterResource(id = R.drawable.popup_parking_slot),
        contentDescription = "Add one Parking Slot",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
      )

      // 2. Top-right red close (X) button aligned on upper-right ribbon tip like reference image
      val closeBtnSize = cardW * 0.112f
      val closeTouchSize = maxOf(closeBtnSize, 48.dp)
      val closeInteraction = remember { MutableInteractionSource() }
      val closePressed by closeInteraction.collectIsPressedAsState()
      val closeScale by animateFloatAsState(
        if (closePressed) 0.90f else 1f,
        spring(),
        label = "unlock_slot_close_scale",
      )
      Box(
        modifier =
          Modifier
            .align(Alignment.TopStart)
            .offset(
              x = cardW * 0.946f - closeTouchSize * 0.5f,
              y = cardH * 0.108f - closeTouchSize * 0.5f,
            )
            .size(closeTouchSize)
            .scale(closeScale)
            .clickable(
              interactionSource = closeInteraction,
              indication = null,
              onClick = onDismiss,
            )
            .testTag("unlock_slot_close_button"),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          painter = painterResource(id = R.drawable.btn_panel_close_red),
          contentDescription = "Close Parking Slot popup",
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(closeBtnSize),
        )
      }

      // 3. Green glossy "Get" button from sprite sheet centered in lower panel like reference image
      val getW = cardW * 0.435f
      val getH = getW * (250f / 656f)
      val getTouchH = maxOf(getH, 48.dp)
      val getInteraction = remember { MutableInteractionSource() }
      val getPressed by getInteraction.collectIsPressedAsState()
      val getScale by animateFloatAsState(
        if (getPressed) 0.92f else 1f,
        spring(),
        label = "unlock_slot_get_scale",
      )
      Box(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .offset(y = cardH * 0.874f - getTouchH * 0.5f)
            .size(width = getW, height = getTouchH)
            .scale(getScale)
            .clickable(
              interactionSource = getInteraction,
              indication = null,
              onClick = onConfirm,
            )
            .testTag("confirm_unlock_slot"),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          painter = painterResource(id = R.drawable.btn_parking_slot_get),
          contentDescription = "Get extra Parking Slot",
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(width = getW, height = getH),
        )
      }
    }
  }
}

@Composable
fun ShopDialog(
  @Suppress("UNUSED_PARAMETER") coins: Int,
  @Suppress("UNUSED_PARAMETER") boardCount: Int,
  @Suppress("UNUSED_PARAMETER") refreshCount: Int,
  @Suppress("UNUSED_PARAMETER") vipCount: Int,
  @Suppress("UNUSED_PARAMETER") notice: String?,
  @Suppress("UNUSED_PARAMETER") onBuyBooster: (BoosterType) -> Unit,
  onClaimFreeCoins: () -> Unit,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  val backdropInteraction = remember { MutableInteractionSource() }

  Column(
    modifier =
      Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0xFF682BD0), Color(0xFF5620B8), Color(0xFF47189E)),
          ),
        )
        .clickable(
          interactionSource = backdropInteraction,
          indication = null,
          onClick = onDismiss,
        )
        .testTag("shop_screen_overlay"),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    BoxWithConstraints(
      modifier =
        Modifier
          .windowInsetsPadding(WindowInsets.statusBars)
          .fillMaxWidth()
          .aspectRatio(856f / 262f),
    ) {
      val bannerW = maxWidth
      val bannerH = maxHeight

      Image(
        painter = painterResource(id = R.drawable.bg_shop_top_banner),
        contentDescription = stringResource(R.string.shop_top_banner_desc),
        contentScale = ContentScale.FillBounds,
        modifier =
          Modifier
            .fillMaxSize()
            .testTag("shop_top_banner"),
      )

      val closeInteraction = remember { MutableInteractionSource() }
      val isClosePressed by closeInteraction.collectIsPressedAsState()
      val closeScale by animateFloatAsState(
        targetValue = if (isClosePressed) 0.90f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "shopCloseScale",
      )
      val closeIconSize = (bannerW * 0.105f).coerceIn(36.dp, 48.dp)

      Box(
        modifier =
          Modifier
            .align(Alignment.TopStart)
            .windowInsetsPadding(WindowInsets.displayCutout)
            .padding(
              start = (bannerW * 0.022f).coerceAtLeast(6.dp),
              top = (bannerH * 0.10f).coerceAtLeast(6.dp),
            )
            .size(48.dp)
            .scale(closeScale)
            .clip(CircleShape)
            .clickable(
              interactionSource = closeInteraction,
              indication = null,
              onClick = onDismiss,
            )
            .testTag("shop_close_button"),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          painter = painterResource(id = R.drawable.btn_shop_close_red),
          contentDescription = stringResource(R.string.shop_close_desc),
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(closeIconSize),
        )
      }
    }

    Column(
      modifier =
        Modifier
          .fillMaxWidth()
          .weight(1f)
          .verticalScroll(rememberScrollState())
          .windowInsetsPadding(WindowInsets.navigationBars)
          .padding(top = 30.dp, bottom = 24.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      val starterPanelInteraction = remember { MutableInteractionSource() }
      BoxWithConstraints(
        modifier =
          Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .aspectRatio(663f / 317f)
            .clickable(
              interactionSource = starterPanelInteraction,
              indication = null,
              onClick = {},
            )
            .testTag("shop_starter_orange_panel"),
      ) {
        val cardW = maxWidth
        val cardH = maxHeight

        Image(
          painter = painterResource(id = R.drawable.panel_shop_starter_orange),
          contentDescription = stringResource(R.string.shop_starter_panel_desc),
          contentScale = ContentScale.FillBounds,
          modifier = Modifier.fillMaxSize(),
        )

        val chestW = cardW * 0.445f
        val chestH = chestW * (369f / 654f)
        val chestCenterX = cardW * 0.324f
        val chestTop = cardH * 0.068f
        val chestCenterY = chestTop + chestH * 0.50f

        val starterLeftHoardSize = cardW * 0.210f
        val starterRightHoardSize = cardW * 0.185f

        val starterChestAnim = rememberInfiniteTransition(label = "shopStarterChestAnim")
        val chestRaySpin by starterChestAnim.animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 9000, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "chestRaySpin",
        )
        val chestGlowPulse by starterChestAnim.animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 2400, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "chestGlowPulse",
        )
        val chestScale by starterChestAnim.animateFloat(
          initialValue = 0.95f,
          targetValue = 1.05f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "chestScale",
        )
        val chestTilt by starterChestAnim.animateFloat(
          initialValue = -3.4f,
          targetValue = 3.4f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1150, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "chestTilt",
        )
        val chestBobDp by starterChestAnim.animateFloat(
          initialValue = -1.8f,
          targetValue = 1.8f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 980, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "chestBob",
        )
        val leftCoinBobDp by starterChestAnim.animateFloat(
          initialValue = 2.0f,
          targetValue = -2.0f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 820, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "leftCoinBob",
        )
        val leftCoinTilt by starterChestAnim.animateFloat(
          initialValue = -5.5f,
          targetValue = 4.5f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1040, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "leftCoinTilt",
        )
        val rightCoinBobDp by starterChestAnim.animateFloat(
          initialValue = -2.0f,
          targetValue = 2.0f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 880, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "rightCoinBob",
        )
        val rightCoinTilt by starterChestAnim.animateFloat(
          initialValue = 5.5f,
          targetValue = -4.5f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "rightCoinTilt",
        )
        val chestSparklePhase by starterChestAnim.animateFloat(
          initialValue = 0f,
          targetValue = (2.0 * PI).toFloat(),
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1800, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "chestSparkle",
        )

        // Rotating white sunbeam rays & warm-white core halo behind the treasure chest and coins
        Canvas(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(cardH * 0.64f)
              .padding(horizontal = cardW * 0.024f, vertical = cardH * 0.055f)
              .clip(RoundedCornerShape(cardH * 0.11f))
              .testTag("shop_starter_chest_rays"),
        ) {
          val insetX = (cardW * 0.024f).toPx()
          val insetY = (cardH * 0.055f).toPx()
          val cx = chestCenterX.toPx() - insetX
          val cy = chestCenterY.toPx() - insetY
          val maxR = (cardW * 0.250f).toPx()
          val haloAlpha = 0.72f + 0.24f * sin(chestGlowPulse * 2f * PI.toFloat())

          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    Color(0xF0FFFBE8).copy(alpha = haloAlpha),
                    Color(0x99FFFFFF).copy(alpha = haloAlpha * 0.75f),
                    Color.Transparent,
                  ),
                center = Offset(cx, cy),
                radius = maxR * 0.85f,
              ),
            radius = maxR * 0.85f,
            center = Offset(cx, cy),
          )

          val rayBrush =
            Brush.radialGradient(
              colors =
                listOf(
                  Color(0xE8FFFFFF),
                  Color(0x88FFFDF4),
                  Color(0x22FFFFFF),
                  Color.Transparent,
                ),
              center = Offset(cx, cy),
              radius = maxR,
            )
          rotate(degrees = chestRaySpin, pivot = Offset(cx, cy)) {
            val rayCount = 12
            for (i in 0 until rayCount) {
              val a0 = (i * (360.0 / rayCount)) * (PI / 180.0)
              val halfSpread = 0.12
              val a1 = a0 + halfSpread
              val a2 = a0 - halfSpread
              val rayPath =
                Path().apply {
                  moveTo(cx, cy)
                  lineTo(cx + cos(a1).toFloat() * maxR, cy + sin(a1).toFloat() * maxR)
                  lineTo(cx + cos(a2).toFloat() * maxR, cy + sin(a2).toFloat() * maxR)
                  close()
                }
              drawPath(rayPath, brush = rayBrush)
            }
          }
        }

        // Left flanking coin sack & gold coin hoard behind the chest (flipped so sack sits beside chest)
        Image(
          painter = painterResource(id = R.drawable.ic_shop_special_coin_hoard),
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier =
            Modifier
              .offset(x = chestCenterX - chestW * 0.54f, y = cardH * 0.145f)
              .size(starterLeftHoardSize)
              .graphicsLayer {
                scaleX = -1f * (0.97f + 0.03f * chestScale)
                scaleY = 0.97f + 0.03f * chestScale
                rotationZ = leftCoinTilt * 0.6f
                translationY = leftCoinBobDp.dp.toPx()
              },
        )

        // Right flanking coin sack & gold coin hoard behind the chest
        Image(
          painter = painterResource(id = R.drawable.ic_shop_special_coin_hoard),
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier =
            Modifier
              .offset(x = chestCenterX + chestW * 0.19f, y = cardH * 0.188f)
              .size(starterRightHoardSize)
              .graphicsLayer {
                scaleX = 0.97f + 0.03f * chestScale
                scaleY = 0.97f + 0.03f * chestScale
                rotationZ = rightCoinTilt * 0.6f
                translationY = rightCoinBobDp.dp.toPx()
              },
        )

        // Center red-and-blue treasure chest overflowing with gold coins + twinkling stars
        Box(
          modifier =
            Modifier
              .offset(x = chestCenterX - chestW * 0.50f, y = chestTop)
              .size(width = chestW, height = chestH)
              .testTag("shop_starter_animated_chest"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_shop_large_pack_chest),
            contentDescription = stringResource(R.string.shop_starter_chest_desc),
            contentScale = ContentScale.Fit,
            modifier =
              Modifier
                .fillMaxSize()
                .graphicsLayer {
                  scaleX = 0.98f + 0.02f * chestScale
                  scaleY = 0.98f + 0.02f * chestScale
                  rotationZ = chestTilt * 0.6f
                  translationY = chestBobDp.dp.toPx()
                },
          )
          Canvas(modifier = Modifier.fillMaxSize()) {
            fun drawFourPointStar(cx: Float, cy: Float, radius: Float, alpha: Float) {
              if (alpha <= 0.05f || radius <= 0.5f) return
              val inner = radius * 0.24f
              val starPath =
                Path().apply {
                  moveTo(cx, cy - radius)
                  lineTo(cx + inner, cy - inner)
                  lineTo(cx + radius, cy)
                  lineTo(cx + inner, cy + inner)
                  lineTo(cx, cy + radius)
                  lineTo(cx - inner, cy + inner)
                  lineTo(cx - radius, cy)
                  lineTo(cx - inner, cy - inner)
                  close()
                }
              drawPath(path = starPath, color = Color.White.copy(alpha = alpha))
            }

            val t1 = ((sin(chestSparklePhase.toDouble()) + 1.0) * 0.5).toFloat()
            val t2 = ((sin((chestSparklePhase + 2.1f).toDouble()) + 1.0) * 0.5).toFloat()
            val t3 = ((sin((chestSparklePhase + 4.2f).toDouble()) + 1.0) * 0.5).toFloat()

            drawFourPointStar(
              cx = size.width * 0.12f,
              cy = size.height * 0.22f,
              radius = size.minDimension * (0.06f + 0.05f * t1),
              alpha = 0.25f + 0.75f * t1,
            )
            drawFourPointStar(
              cx = size.width * 0.88f,
              cy = size.height * 0.20f,
              radius = size.minDimension * (0.06f + 0.05f * t2),
              alpha = 0.25f + 0.75f * t2,
            )
            drawFourPointStar(
              cx = size.width * 0.78f,
              cy = size.height * 0.06f,
              radius = size.minDimension * (0.05f + 0.05f * t3),
              alpha = 0.25f + 0.75f * t3,
            )
          }
        }

        // Bold white "1000" label with crisp black outline right below the chest coins
        val coinsLabel = stringResource(R.string.shop_starter_coins_amount)
        val coinsLabelStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (cardH.value * 0.118f).coerceIn(15f, 24f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.3.sp,
          )
        val coinsBoxW = cardW * 0.24f
        Box(
          modifier =
            Modifier
              .offset(x = chestCenterX - coinsBoxW * 0.50f, y = cardH * 0.485f)
              .width(coinsBoxW)
              .testTag("shop_starter_coins_label"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = coinsLabel,
            style =
              coinsLabelStyle.copy(
                drawStyle = Stroke(width = 8.5f, join = StrokeJoin.Round),
              ),
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.offset(y = 1.2.dp),
          )
          Text(
            text = coinsLabel,
            style =
              coinsLabelStyle.copy(
                drawStyle = Stroke(width = 7.0f, join = StrokeJoin.Round),
              ),
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
          )
          Text(
            text = coinsLabel,
            style = coinsLabelStyle,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
          )
        }

        val trayW = cardW * 0.312f
        val trayH = cardH * 0.387f
        val qtyLabel = stringResource(R.string.shop_booster_qty_two)
        val qtyStyle =
          MaterialTheme.typography.labelLarge.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (trayH.value * 0.23f).coerceIn(11f, 17f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.617f, y = cardH * 0.175f)
              .size(width = trayW, height = trayH)
              .testTag("shop_starter_booster_tray"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.panel_shop_starter_booster_tray),
            contentDescription = stringResource(R.string.shop_starter_booster_tray_desc),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
          )
          Row(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(horizontal = trayW * 0.055f, vertical = trayH * 0.07f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val starterBoosters =
              listOf(
                Pair(R.drawable.ic_shop_booster_vip, R.string.shop_booster_vip_desc),
                Pair(R.drawable.ic_shop_booster_bus, R.string.shop_booster_bus_desc),
                Pair(R.drawable.ic_shop_booster_refresh, R.string.shop_booster_refresh_desc),
              )
            starterBoosters.forEach { (iconRes, descRes) ->
              Box(
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
              ) {
                Image(
                  painter = painterResource(id = iconRes),
                  contentDescription = stringResource(descRes),
                  contentScale = ContentScale.Fit,
                  modifier =
                    Modifier
                      .fillMaxWidth(0.92f)
                      .height(trayH * 0.64f)
                      .padding(top = trayH * 0.02f),
                )
                Box(
                  modifier =
                    Modifier
                      .align(Alignment.BottomCenter)
                      .padding(bottom = trayH * 0.01f),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = qtyLabel,
                    style =
                      qtyStyle.copy(
                        drawStyle = Stroke(width = 5.5f, join = StrokeJoin.Round),
                      ),
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(y = 0.8.dp),
                  )
                  Text(
                    text = qtyLabel,
                    style =
                      qtyStyle.copy(
                        drawStyle = Stroke(width = 4.5f, join = StrokeJoin.Round),
                      ),
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false,
                  )
                  Text(
                    text = qtyLabel,
                    style = qtyStyle,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
          }
        }

        val ribbonW = cardW * 0.628f
        val ribbonH = ribbonW * (180f / 857f)
        val ribbonTitle = stringResource(R.string.shop_starter_pack_title)
        val ribbonTextStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (ribbonH.value * 0.48f).coerceIn(17f, 28f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.3.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.016f, y = cardH * 0.662f)
              .size(width = ribbonW, height = ribbonH)
              .testTag("shop_starter_pack_ribbon"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.ribbon_shop_starter_orange),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(horizontal = ribbonW * 0.12f)
                .padding(bottom = ribbonH * 0.14f),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = ribbonTitle,
              style =
                ribbonTextStyle.copy(
                  drawStyle = Stroke(width = 9f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF4A1000),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.offset(y = 1.6.dp),
            )
            Text(
              text = ribbonTitle,
              style =
                ribbonTextStyle.copy(
                  drawStyle = Stroke(width = 7.5f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF6A1E00),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
            Text(
              text = ribbonTitle,
              style =
                ribbonTextStyle.copy(
                  shadow = Shadow(color = Color(0x994A1000), offset = Offset(0f, 1.5f), blurRadius = 1.5f),
                ),
              color = Color.White,
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
          }
        }

        val priceBtnW = cardW * 0.271f
        val priceBtnH = cardH * 0.229f
        val priceTouchH = maxOf(priceBtnH, 48.dp)
        val priceBtnCenterY = cardH * 0.692f + priceBtnH * 0.50f
        val priceBtnInteraction = remember { MutableInteractionSource() }
        val isPricePressed by priceBtnInteraction.collectIsPressedAsState()
        val priceBtnScale by animateFloatAsState(
          targetValue = if (isPricePressed) 0.93f else 1f,
          animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
          label = "starterPriceBtnScale",
        )
        val starterPriceText = stringResource(R.string.shop_starter_pack_price)
        val starterPriceStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (priceBtnH.value * 0.43f).coerceIn(13f, 20f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.690f, y = priceBtnCenterY - priceTouchH * 0.50f)
              .size(width = priceBtnW, height = priceTouchH)
              .scale(priceBtnScale)
              .clickable(
                interactionSource = priceBtnInteraction,
                indication = null,
                onClick = onClaimFreeCoins,
              )
              .testTag("shop_starter_price_button"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier = Modifier.size(width = priceBtnW, height = priceBtnH),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              painter = painterResource(id = R.drawable.btn_shop_price_green),
              contentDescription = "$ribbonTitle $starterPriceText",
              contentScale = ContentScale.FillBounds,
              modifier = Modifier.fillMaxSize(),
            )
            Box(
              modifier =
                Modifier
                  .fillMaxSize()
                  .padding(horizontal = priceBtnW * 0.06f)
                  .padding(bottom = priceBtnH * 0.06f),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = starterPriceText,
                style =
                  starterPriceStyle.copy(
                    drawStyle = Stroke(width = 7.5f, join = StrokeJoin.Round),
                  ),
                color = Color(0xFF063810),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.offset(y = 1.2.dp),
              )
              Text(
                text = starterPriceText,
                style =
                  starterPriceStyle.copy(
                    drawStyle = Stroke(width = 6.0f, join = StrokeJoin.Round),
                  ),
                color = Color(0xFF0A4D18),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
              )
              Text(
                text = starterPriceText,
                style =
                  starterPriceStyle.copy(
                    shadow = Shadow(color = Color(0x88063810), offset = Offset(0f, 1.2f), blurRadius = 1.2f),
                  ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
              )
            }
          }
        }
      }

      Spacer(Modifier.height(20.dp))

      val noAdsPanelInteraction = remember { MutableInteractionSource() }
      BoxWithConstraints(
        modifier =
          Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .aspectRatio(642f / 196f)
            .clickable(
              interactionSource = noAdsPanelInteraction,
              indication = null,
              onClick = {},
            )
            .testTag("shop_no_ads_purple_panel"),
      ) {
        val cardW = maxWidth
        val cardH = maxHeight

        Image(
          painter = painterResource(id = R.drawable.panel_shop_no_ads_purple),
          contentDescription = stringResource(R.string.shop_no_ads_panel_desc),
          contentScale = ContentScale.FillBounds,
          modifier = Modifier.fillMaxSize(),
        )

        val badgeW = cardW * 0.163f
        val badgeH = badgeW * (127f / 131f)
        val badgeLeft = cardW * 0.085f
        val badgeTop = cardH * 0.119f
        val badgeAnim = rememberInfiniteTransition(label = "shopNoAdsBadgeAnim")
        val raySpin by badgeAnim.animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 9000, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "shopNoAdsRays",
        )
        val pulsePhase by badgeAnim.animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 2400, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "shopNoAdsGlowPulse",
        )
        val badgePulse by badgeAnim.animateFloat(
          initialValue = 0.94f,
          targetValue = 1.06f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "badgePulse",
        )
        val badgeTilt by badgeAnim.animateFloat(
          initialValue = -5.0f,
          targetValue = 5.0f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "badgeTilt",
        )
        val badgeBobDp by badgeAnim.animateFloat(
          initialValue = -1.5f,
          targetValue = 1.5f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "badgeBob",
        )
        val sparklePhase by badgeAnim.animateFloat(
          initialValue = 0f,
          targetValue = (2.0 * PI).toFloat(),
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1800, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "badgeSparkle",
        )

        // Rotating white sunbeam rays + radiant core glow strictly behind the purple ADS badge
        Canvas(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(horizontal = cardW * 0.024f, vertical = cardH * 0.075f)
              .clip(RoundedCornerShape(cardH * 0.12f))
              .testTag("shop_no_ads_rotating_rays"),
        ) {
          val insetX = (cardW * 0.024f).toPx()
          val insetY = (cardH * 0.075f).toPx()
          val cx = (badgeLeft + badgeW * 0.5f).toPx() - insetX
          val cy = (badgeTop + badgeH * 0.5f).toPx() - insetY
          val maxR = (badgeW * 0.98f).toPx()
          val haloAlpha = 0.72f + 0.24f * sin(pulsePhase * 2f * PI.toFloat())

          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    Color(0xF0FFF8E6).copy(alpha = haloAlpha),
                    Color(0x99FFFFFF).copy(alpha = haloAlpha * 0.75f),
                    Color.Transparent,
                  ),
                center = Offset(cx, cy),
                radius = maxR * 0.82f,
              ),
            radius = maxR * 0.82f,
            center = Offset(cx, cy),
          )

          val rayBrush =
            Brush.radialGradient(
              colors =
                listOf(
                  Color(0xE8FFFFFF),
                  Color(0x88FFF4FF),
                  Color(0x22FFFFFF),
                  Color.Transparent,
                ),
              center = Offset(cx, cy),
              radius = maxR,
            )
          rotate(degrees = raySpin, pivot = Offset(cx, cy)) {
            val rayCount = 12
            for (i in 0 until rayCount) {
              val a0 = (i * (360.0 / rayCount)) * (PI / 180.0)
              val halfSpread = 0.12
              val a1 = a0 + halfSpread
              val a2 = a0 - halfSpread
              val rayPath =
                Path().apply {
                  moveTo(cx, cy)
                  lineTo(cx + cos(a1).toFloat() * maxR, cy + sin(a1).toFloat() * maxR)
                  lineTo(cx + cos(a2).toFloat() * maxR, cy + sin(a2).toFloat() * maxR)
                  close()
                }
              drawPath(rayPath, brush = rayBrush)
            }
          }
        }

        Box(
          modifier =
            Modifier
              .offset(x = badgeLeft, y = badgeTop)
              .size(width = badgeW, height = badgeH)
              .testTag("shop_no_ads_animated_badge"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_shop_no_ads_purple),
            contentDescription = stringResource(R.string.shop_no_ads_icon_desc),
            contentScale = ContentScale.Fit,
            modifier =
              Modifier
                .fillMaxSize()
                .graphicsLayer {
                  scaleX = badgePulse
                  scaleY = badgePulse
                  rotationZ = badgeTilt
                  translationY = badgeBobDp.dp.toPx()
                },
          )
          Canvas(modifier = Modifier.fillMaxSize()) {
            fun drawFourPointStar(cx: Float, cy: Float, radius: Float, alpha: Float) {
              if (alpha <= 0.05f || radius <= 0.5f) return
              val inner = radius * 0.24f
              val starPath =
                Path().apply {
                  moveTo(cx, cy - radius)
                  lineTo(cx + inner, cy - inner)
                  lineTo(cx + radius, cy)
                  lineTo(cx + inner, cy + inner)
                  lineTo(cx, cy + radius)
                  lineTo(cx - inner, cy + inner)
                  lineTo(cx - radius, cy)
                  lineTo(cx - inner, cy - inner)
                  close()
                }
              drawPath(path = starPath, color = Color.White.copy(alpha = alpha))
            }

            val t1 = ((sin(sparklePhase.toDouble()) + 1.0) * 0.5).toFloat()
            val t2 = ((sin((sparklePhase + 2.1f).toDouble()) + 1.0) * 0.5).toFloat()
            val t3 = ((sin((sparklePhase + 4.2f).toDouble()) + 1.0) * 0.5).toFloat()

            drawFourPointStar(
              cx = size.width * 0.04f,
              cy = size.height * 0.18f,
              radius = size.minDimension * (0.08f + 0.07f * t1),
              alpha = 0.25f + 0.75f * t1,
            )
            drawFourPointStar(
              cx = size.width * 0.96f,
              cy = size.height * 0.14f,
              radius = size.minDimension * (0.07f + 0.06f * t2),
              alpha = 0.25f + 0.75f * t2,
            )
            drawFourPointStar(
              cx = size.width * 0.95f,
              cy = size.height * 0.88f,
              radius = size.minDimension * (0.08f + 0.07f * t3),
              alpha = 0.25f + 0.75f * t3,
            )
          }
        }

        val ribbonW = cardW * 0.368f
        val ribbonH = ribbonW * (274f / 907f)
        val noAdsTitle = stringResource(R.string.shop_no_ads_title)
        val noAdsFontSize = (ribbonH.value * 0.57f).coerceIn(17f, 28f).sp
        val noAdsTextStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = noAdsFontSize,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.4.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.286f, y = cardH * 0.196f)
              .size(width = ribbonW, height = ribbonH)
              .testTag("shop_no_ads_ribbon"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.ribbon_shop_no_ads_purple),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(horizontal = ribbonW * 0.16f)
                .padding(bottom = ribbonH * 0.15f),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = noAdsTitle,
              style =
                noAdsTextStyle.copy(
                  drawStyle = Stroke(width = 9f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF47117A),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.offset(y = 1.6.dp),
            )
            Text(
              text = noAdsTitle,
              style =
                noAdsTextStyle.copy(
                  drawStyle = Stroke(width = 7.5f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF531888),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
            Text(
              text = noAdsTitle,
              style =
                noAdsTextStyle.copy(
                  shadow = Shadow(color = Color(0x993E0A72), offset = Offset(0f, 1.5f), blurRadius = 1.5f),
                ),
              color = Color.White,
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
          }
        }

        val noAdsPriceBtnW = cardW * 0.271f
        val noAdsPriceBtnH = cardH * 0.379f
        val noAdsPriceTouchH = maxOf(noAdsPriceBtnH, 48.dp)
        val noAdsPriceBtnCenterY = cardH * 0.221f + noAdsPriceBtnH * 0.50f
        val noAdsPriceInteraction = remember { MutableInteractionSource() }
        val isNoAdsPricePressed by noAdsPriceInteraction.collectIsPressedAsState()
        val noAdsPriceScale by animateFloatAsState(
          targetValue = if (isNoAdsPricePressed) 0.93f else 1f,
          animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
          label = "noAdsPriceBtnScale",
        )
        val noAdsPriceText = stringResource(R.string.shop_no_ads_price)
        val noAdsPriceStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (noAdsPriceBtnH.value * 0.345f).coerceIn(11.0f, 17.5f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.690f, y = noAdsPriceBtnCenterY - noAdsPriceTouchH * 0.50f)
              .size(width = noAdsPriceBtnW, height = noAdsPriceTouchH)
              .scale(noAdsPriceScale)
              .clickable(
                interactionSource = noAdsPriceInteraction,
                indication = null,
                onClick = {},
              )
              .testTag("shop_no_ads_price_button"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier = Modifier.size(width = noAdsPriceBtnW, height = noAdsPriceBtnH),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              painter = painterResource(id = R.drawable.btn_shop_price_green),
              contentDescription = "$noAdsTitle $noAdsPriceText",
              contentScale = ContentScale.FillBounds,
              modifier = Modifier.fillMaxSize(),
            )
            Box(
              modifier =
                Modifier
                  .fillMaxSize()
                  .padding(horizontal = noAdsPriceBtnW * 0.05f)
                  .padding(bottom = noAdsPriceBtnH * 0.06f),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = noAdsPriceText,
                style =
                  noAdsPriceStyle.copy(
                    drawStyle = Stroke(width = 7.0f, join = StrokeJoin.Round),
                  ),
                color = Color(0xFF063810),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.offset(y = 1.2.dp),
              )
              Text(
                text = noAdsPriceText,
                style =
                  noAdsPriceStyle.copy(
                    drawStyle = Stroke(width = 5.5f, join = StrokeJoin.Round),
                  ),
                color = Color(0xFF0A4D18),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
              )
              Text(
                text = noAdsPriceText,
                style =
                  noAdsPriceStyle.copy(
                    shadow = Shadow(color = Color(0x88063810), offset = Offset(0f, 1.2f), blurRadius = 1.2f),
                  ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
              )
            }
          }
        }

        Text(
          text = stringResource(R.string.shop_rewarded_ads_note),
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.SansSerif,
              fontWeight = FontWeight.Black,
              fontSize = (cardW.value * 0.0285f).coerceIn(8.5f, 11.5f).sp,
              letterSpacing = 0.2.sp,
              shadow = Shadow(color = Color(0x99320F6B), offset = Offset(0f, 1.5f), blurRadius = 1.5f),
            ),
          color = Color.White,
          maxLines = 1,
          softWrap = false,
          modifier =
            Modifier
              .offset(x = cardW * 0.052f, y = cardH * 0.765f)
              .testTag("shop_no_ads_rewarded_note"),
        )
      }

      Spacer(Modifier.height(20.dp))

      val noAdsPackPanelInteraction = remember { MutableInteractionSource() }
      BoxWithConstraints(
        modifier =
          Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .aspectRatio(642f / 318f)
            .clickable(
              interactionSource = noAdsPackPanelInteraction,
              indication = null,
              onClick = {},
            )
            .testTag("shop_no_ads_blue_panel"),
      ) {
        val cardW = maxWidth
        val cardH = maxHeight

        Image(
          painter = painterResource(id = R.drawable.panel_shop_no_ads_blue),
          contentDescription = stringResource(R.string.shop_no_ads_pack_panel_desc),
          contentScale = ContentScale.FillBounds,
          modifier = Modifier.fillMaxSize(),
        )

        val badgeW = cardW * 0.215f
        val badgeH = badgeW * (123f / 124f)
        val badgeLeft = cardW * 0.120f
        val badgeTop = cardH * 0.084f
        val packBadgeAnim = rememberInfiniteTransition(label = "shopNoAdsPackBadgeAnim")
        val raySpin by packBadgeAnim.animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 9000, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "shopNoAdsPackRays",
        )
        val pulsePhase by packBadgeAnim.animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 2400, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "shopNoAdsPackGlowPulse",
        )
        val badgePulse by packBadgeAnim.animateFloat(
          initialValue = 0.94f,
          targetValue = 1.06f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 850, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "packBadgePulse",
        )
        val badgeTilt by packBadgeAnim.animateFloat(
          initialValue = -5.0f,
          targetValue = 5.0f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1300, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "packBadgeTilt",
        )
        val badgeBobDp by packBadgeAnim.animateFloat(
          initialValue = -1.5f,
          targetValue = 1.5f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1050, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "packBadgeBob",
        )
        val sparklePhase by packBadgeAnim.animateFloat(
          initialValue = 0f,
          targetValue = (2.0 * PI).toFloat(),
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1800, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "packBadgeSparkle",
        )

        // Rotating white sunbeam rays + radiant halo strictly behind the sky-blue ADS badge
        Canvas(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(horizontal = cardW * 0.024f, vertical = cardH * 0.046f)
              .clip(RoundedCornerShape(cardH * 0.075f))
              .testTag("shop_no_ads_pack_rotating_rays"),
        ) {
          val insetX = (cardW * 0.024f).toPx()
          val insetY = (cardH * 0.046f).toPx()
          val cx = (badgeLeft + badgeW * 0.5f).toPx() - insetX
          val cy = (badgeTop + badgeH * 0.5f).toPx() - insetY
          val maxR = (badgeW * 0.98f).toPx()
          val haloAlpha = 0.72f + 0.24f * sin(pulsePhase * 2f * PI.toFloat())

          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    Color(0xF0FFF8E6).copy(alpha = haloAlpha),
                    Color(0x99FFFFFF).copy(alpha = haloAlpha * 0.75f),
                    Color.Transparent,
                  ),
                center = Offset(cx, cy),
                radius = maxR * 0.82f,
              ),
            radius = maxR * 0.82f,
            center = Offset(cx, cy),
          )

          val rayBrush =
            Brush.radialGradient(
              colors =
                listOf(
                  Color(0xE8FFFFFF),
                  Color(0x88EBF8FF),
                  Color(0x22FFFFFF),
                  Color.Transparent,
                ),
              center = Offset(cx, cy),
              radius = maxR,
            )
          rotate(degrees = raySpin, pivot = Offset(cx, cy)) {
            val rayCount = 12
            for (i in 0 until rayCount) {
              val a0 = (i * (360.0 / rayCount)) * (PI / 180.0)
              val halfSpread = 0.12
              val a1 = a0 + halfSpread
              val a2 = a0 - halfSpread
              val rayPath =
                Path().apply {
                  moveTo(cx, cy)
                  lineTo(cx + cos(a1).toFloat() * maxR, cy + sin(a1).toFloat() * maxR)
                  lineTo(cx + cos(a2).toFloat() * maxR, cy + sin(a2).toFloat() * maxR)
                  close()
                }
              drawPath(rayPath, brush = rayBrush)
            }
          }
        }

        Box(
          modifier =
            Modifier
              .offset(x = badgeLeft, y = badgeTop)
              .size(width = badgeW, height = badgeH)
              .testTag("shop_no_ads_pack_animated_badge"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_shop_no_ads_blue),
            contentDescription = stringResource(R.string.shop_no_ads_icon_desc),
            contentScale = ContentScale.Fit,
            modifier =
              Modifier
                .fillMaxSize()
                .graphicsLayer {
                  scaleX = badgePulse
                  scaleY = badgePulse
                  rotationZ = badgeTilt
                  translationY = badgeBobDp.dp.toPx()
                },
          )
          Canvas(modifier = Modifier.fillMaxSize()) {
            fun drawFourPointStar(cx: Float, cy: Float, radius: Float, alpha: Float) {
              if (alpha <= 0.05f || radius <= 0.5f) return
              val inner = radius * 0.24f
              val starPath =
                Path().apply {
                  moveTo(cx, cy - radius)
                  lineTo(cx + inner, cy - inner)
                  lineTo(cx + radius, cy)
                  lineTo(cx + inner, cy + inner)
                  lineTo(cx, cy + radius)
                  lineTo(cx - inner, cy + inner)
                  lineTo(cx - radius, cy)
                  lineTo(cx - inner, cy - inner)
                  close()
                }
              drawPath(path = starPath, color = Color.White.copy(alpha = alpha))
            }

            val t1 = ((sin(sparklePhase.toDouble()) + 1.0) * 0.5).toFloat()
            val t2 = ((sin((sparklePhase + 2.1f).toDouble()) + 1.0) * 0.5).toFloat()
            val t3 = ((sin((sparklePhase + 4.2f).toDouble()) + 1.0) * 0.5).toFloat()

            drawFourPointStar(
              cx = size.width * 0.02f,
              cy = size.height * 0.16f,
              radius = size.minDimension * (0.08f + 0.07f * t1),
              alpha = 0.25f + 0.75f * t1,
            )
            drawFourPointStar(
              cx = size.width * 0.96f,
              cy = size.height * 0.14f,
              radius = size.minDimension * (0.07f + 0.06f * t2),
              alpha = 0.25f + 0.75f * t2,
            )
            drawFourPointStar(
              cx = size.width * 0.96f,
              cy = size.height * 0.87f,
              radius = size.minDimension * (0.08f + 0.07f * t3),
              alpha = 0.25f + 0.75f * t3,
            )
          }
        }

        val packTrayW = cardW * 0.417f
        val packTrayH = cardH * 0.383f
        val packQtyOne = stringResource(R.string.shop_booster_qty_one)
        val packCoinsAmt = stringResource(R.string.shop_starter_coins_amount)
        val packQtyStyle =
          MaterialTheme.typography.labelLarge.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (packTrayH.value * 0.225f).coerceIn(10.5f, 16f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp,
          )
        val packCoinsLabelStyle =
          MaterialTheme.typography.labelLarge.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (packTrayH.value * 0.195f).coerceIn(9.5f, 14f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.477f, y = cardH * 0.092f)
              .size(width = packTrayW, height = packTrayH)
              .testTag("shop_no_ads_pack_tray"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.panel_shop_no_ads_pack_tray),
            contentDescription = stringResource(R.string.shop_no_ads_pack_tray_desc),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
          )
          Row(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(horizontal = packTrayW * 0.042f, vertical = packTrayH * 0.07f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val packItems =
              listOf(
                Triple(R.drawable.ic_shop_pack_vip, R.string.shop_booster_vip_desc, packQtyOne),
                Triple(R.drawable.ic_shop_pack_bus, R.string.shop_booster_bus_desc, packQtyOne),
                Triple(R.drawable.ic_shop_pack_refresh, R.string.shop_booster_refresh_desc, packQtyOne),
                Triple(R.drawable.ic_shop_pack_coins, R.string.shop_pack_coins_desc, packCoinsAmt),
              )
            packItems.forEachIndexed { index, (iconRes, descRes, label) ->
              val isCoinsItem = index == 3
              val itemLabelStyle = if (isCoinsItem) packCoinsLabelStyle else packQtyStyle
              Box(
                modifier = Modifier.weight(if (isCoinsItem) 1.18f else 1f).fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
              ) {
                Image(
                  painter = painterResource(id = iconRes),
                  contentDescription = stringResource(descRes),
                  contentScale = ContentScale.Fit,
                  modifier =
                    Modifier
                      .fillMaxWidth(0.92f)
                      .height(packTrayH * 0.64f)
                      .padding(top = packTrayH * 0.02f),
                )
                Box(
                  modifier =
                    Modifier
                      .align(Alignment.BottomCenter)
                      .padding(bottom = packTrayH * 0.01f),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = label,
                    style =
                      itemLabelStyle.copy(
                        drawStyle = Stroke(width = 5.5f, join = StrokeJoin.Round),
                      ),
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(y = 0.8.dp),
                  )
                  Text(
                    text = label,
                    style =
                      itemLabelStyle.copy(
                        drawStyle = Stroke(width = 4.5f, join = StrokeJoin.Round),
                      ),
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false,
                  )
                  Text(
                    text = label,
                    style = itemLabelStyle,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
          }
        }

        val packRibbonW = cardW * 0.548f
        val packRibbonH = cardH * 0.244f
        val packRibbonTitle = stringResource(R.string.shop_no_ads_pack_title)
        val packRibbonTextStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (packRibbonH.value * 0.47f).coerceIn(13.5f, 22f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.25.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.045f, y = cardH * 0.572f)
              .size(width = packRibbonW, height = packRibbonH)
              .testTag("shop_no_ads_pack_ribbon"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.ribbon_shop_no_ads_blue),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(horizontal = packRibbonW * 0.14f)
                .padding(bottom = packRibbonH * 0.14f),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = packRibbonTitle,
              style =
                packRibbonTextStyle.copy(
                  drawStyle = Stroke(width = 9f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF002B7A),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.offset(y = 1.8.dp),
            )
            Text(
              text = packRibbonTitle,
              style =
                packRibbonTextStyle.copy(
                  drawStyle = Stroke(width = 7.5f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF003B9E),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
            Text(
              text = packRibbonTitle,
              style =
                packRibbonTextStyle.copy(
                  shadow = Shadow(color = Color(0x9900256E), offset = Offset(0f, 1.5f), blurRadius = 1.5f),
                ),
              color = Color.White,
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
          }
        }

        val packPriceBtnW = cardW * 0.273f
        val packPriceBtnH = cardH * 0.220f
        val packPriceTouchH = maxOf(packPriceBtnH, 48.dp)
        val packPriceBtnCenterY = cardH * 0.583f + packPriceBtnH * 0.50f
        val packPriceInteraction = remember { MutableInteractionSource() }
        val isPackPricePressed by packPriceInteraction.collectIsPressedAsState()
        val packPriceScale by animateFloatAsState(
          targetValue = if (isPackPricePressed) 0.93f else 1f,
          animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
          label = "noAdsPackPriceBtnScale",
        )
        val packPriceText = stringResource(R.string.shop_no_ads_pack_price)
        val packPriceStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (packPriceBtnH.value * 0.345f).coerceIn(11.0f, 17.5f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.690f, y = packPriceBtnCenterY - packPriceTouchH * 0.50f)
              .size(width = packPriceBtnW, height = packPriceTouchH)
              .scale(packPriceScale)
              .clickable(
                interactionSource = packPriceInteraction,
                indication = null,
                onClick = {},
              )
              .testTag("shop_no_ads_pack_price_button"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier = Modifier.size(width = packPriceBtnW, height = packPriceBtnH),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              painter = painterResource(id = R.drawable.btn_shop_price_green),
              contentDescription = "$packRibbonTitle $packPriceText",
              contentScale = ContentScale.FillBounds,
              modifier = Modifier.fillMaxSize(),
            )
            Box(
              modifier =
                Modifier
                  .fillMaxSize()
                  .padding(horizontal = packPriceBtnW * 0.05f)
                  .padding(bottom = packPriceBtnH * 0.06f),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = packPriceText,
                style =
                  packPriceStyle.copy(
                    drawStyle = Stroke(width = 7.0f, join = StrokeJoin.Round),
                  ),
                color = Color(0xFF063810),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.offset(y = 1.2.dp),
              )
              Text(
                text = packPriceText,
                style =
                  packPriceStyle.copy(
                    drawStyle = Stroke(width = 5.5f, join = StrokeJoin.Round),
                  ),
                color = Color(0xFF0A4D18),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
              )
              Text(
                text = packPriceText,
                style =
                  packPriceStyle.copy(
                    shadow = Shadow(color = Color(0x88063810), offset = Offset(0f, 1.2f), blurRadius = 1.2f),
                  ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
              )
            }
          }
        }

        Text(
          text = stringResource(R.string.shop_rewarded_ads_note),
          style =
            MaterialTheme.typography.labelSmall.copy(
              fontFamily = FontFamily.SansSerif,
              fontWeight = FontWeight.Black,
              fontSize = (cardW.value * 0.0285f).coerceIn(8.5f, 11.5f).sp,
              letterSpacing = 0.2.sp,
              shadow = Shadow(color = Color(0x9900256E), offset = Offset(0f, 1.5f), blurRadius = 1.5f),
            ),
          color = Color.White,
          textAlign = TextAlign.Center,
          maxLines = 1,
          softWrap = false,
          modifier =
            Modifier
              .align(Alignment.BottomCenter)
              .padding(bottom = cardH * 0.048f)
              .testTag("shop_no_ads_pack_rewarded_note"),
        )
      }

      Spacer(Modifier.height(20.dp))

      val smallPackPanelInteraction = remember { MutableInteractionSource() }
      BoxWithConstraints(
        modifier =
          Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .aspectRatio(642f / 313f)
            .clickable(
              interactionSource = smallPackPanelInteraction,
              indication = null,
              onClick = {},
            )
            .testTag("shop_small_pack_blue_panel"),
      ) {
        val cardW = maxWidth
        val cardH = maxHeight

        Image(
          painter = painterResource(id = R.drawable.panel_shop_small_pack_blue),
          contentDescription = stringResource(R.string.shop_small_pack_panel_desc),
          contentScale = ContentScale.FillBounds,
          modifier = Modifier.fillMaxSize(),
        )

        val smallChestW = cardW * 0.455f
        val smallChestH = smallChestW * (361f / 670f)
        val smallChestCenterX = cardW * 0.326f
        val smallChestTop = cardH * 0.072f
        val smallChestCenterY = smallChestTop + smallChestH * 0.50f
        val smallLeftHoardSize = cardW * 0.215f
        val smallRightHoardSize = cardW * 0.190f
        val smallChestAnim = rememberInfiniteTransition(label = "smallPackChestAnim")
        val smallRaySpin by smallChestAnim.animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 9000, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "smallChestRaySpin",
        )
        val smallGlowPulse by smallChestAnim.animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 2400, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "smallChestGlowPulse",
        )
        val smallChestScale by smallChestAnim.animateFloat(
          initialValue = 0.95f,
          targetValue = 1.05f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "smallChestScale",
        )
        val smallChestTilt by smallChestAnim.animateFloat(
          initialValue = -3.0f,
          targetValue = 3.0f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1150, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "smallChestTilt",
        )
        val smallChestBobDp by smallChestAnim.animateFloat(
          initialValue = -1.8f,
          targetValue = 1.8f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 980, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "smallChestBob",
        )
        val smallLeftCoinBobDp by smallChestAnim.animateFloat(
          initialValue = 2.0f,
          targetValue = -2.0f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 820, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "smallLeftCoinBob",
        )
        val smallLeftCoinTilt by smallChestAnim.animateFloat(
          initialValue = -5.5f,
          targetValue = 4.5f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1040, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "smallLeftCoinTilt",
        )
        val smallRightCoinBobDp by smallChestAnim.animateFloat(
          initialValue = -2.0f,
          targetValue = 2.0f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 880, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "smallRightCoinBob",
        )
        val smallRightCoinTilt by smallChestAnim.animateFloat(
          initialValue = 5.5f,
          targetValue = -4.5f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "smallRightCoinTilt",
        )
        val smallSparklePhase by smallChestAnim.animateFloat(
          initialValue = 0f,
          targetValue = (2.0 * PI).toFloat(),
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1800, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "smallChestSparkle",
        )

        // Rotating white sunbeam rays & radiant white core halo behind the blue 5000 chest and flanking coins
        Canvas(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(cardH * 0.65f)
              .padding(horizontal = cardW * 0.024f, vertical = cardH * 0.055f)
              .clip(RoundedCornerShape(cardH * 0.11f))
              .testTag("shop_small_pack_chest_rays"),
        ) {
          val insetX = (cardW * 0.024f).toPx()
          val insetY = (cardH * 0.055f).toPx()
          val cx = smallChestCenterX.toPx() - insetX
          val cy = smallChestCenterY.toPx() - insetY
          val maxR = (cardW * 0.250f).toPx()
          val haloAlpha = 0.72f + 0.24f * sin(smallGlowPulse * 2f * PI.toFloat())

          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    Color(0xF0FFFBE8).copy(alpha = haloAlpha),
                    Color(0x99FFFFFF).copy(alpha = haloAlpha * 0.75f),
                    Color.Transparent,
                  ),
                center = Offset(cx, cy),
                radius = maxR * 0.85f,
              ),
            radius = maxR * 0.85f,
            center = Offset(cx, cy),
          )

          val rayBrush =
            Brush.radialGradient(
              colors =
                listOf(
                  Color(0xE8FFFFFF),
                  Color(0x88EBF8FF),
                  Color(0x22FFFFFF),
                  Color.Transparent,
                ),
              center = Offset(cx, cy),
              radius = maxR,
            )
          rotate(degrees = smallRaySpin, pivot = Offset(cx, cy)) {
            val rayCount = 12
            for (i in 0 until rayCount) {
              val a0 = (i * (360.0 / rayCount)) * (PI / 180.0)
              val halfSpread = 0.12
              val a1 = a0 + halfSpread
              val a2 = a0 - halfSpread
              val rayPath =
                Path().apply {
                  moveTo(cx, cy)
                  lineTo(cx + cos(a1).toFloat() * maxR, cy + sin(a1).toFloat() * maxR)
                  lineTo(cx + cos(a2).toFloat() * maxR, cy + sin(a2).toFloat() * maxR)
                  close()
                }
              drawPath(rayPath, brush = rayBrush)
            }
          }
        }

        // Left flanking coin sack & gold coin hoard behind the chest (flipped so sack sits beside chest)
        Image(
          painter = painterResource(id = R.drawable.ic_shop_special_coin_hoard),
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier =
            Modifier
              .offset(x = smallChestCenterX - smallChestW * 0.54f, y = cardH * 0.145f)
              .size(smallLeftHoardSize)
              .graphicsLayer {
                scaleX = -1f * (0.97f + 0.03f * smallChestScale)
                scaleY = 0.97f + 0.03f * smallChestScale
                rotationZ = smallLeftCoinTilt * 0.6f
                translationY = smallLeftCoinBobDp.dp.toPx()
              },
        )

        // Right flanking coin sack & gold coin hoard behind the chest
        Image(
          painter = painterResource(id = R.drawable.ic_shop_special_coin_hoard),
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier =
            Modifier
              .offset(x = smallChestCenterX + smallChestW * 0.19f, y = cardH * 0.188f)
              .size(smallRightHoardSize)
              .graphicsLayer {
                scaleX = 0.97f + 0.03f * smallChestScale
                scaleY = 0.97f + 0.03f * smallChestScale
                rotationZ = smallRightCoinTilt * 0.6f
                translationY = smallRightCoinBobDp.dp.toPx()
              },
        )

        // Center glossy blue treasure chest overflowing with gold coins + twinkling stars
        Box(
          modifier =
            Modifier
              .offset(x = smallChestCenterX - smallChestW * 0.50f, y = smallChestTop)
              .size(width = smallChestW, height = smallChestH)
              .testTag("shop_small_pack_animated_chest"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_shop_small_pack_chest),
            contentDescription = stringResource(R.string.shop_small_pack_chest_desc),
            contentScale = ContentScale.Fit,
            modifier =
              Modifier
                .fillMaxSize()
                .graphicsLayer {
                  scaleX = 0.98f + 0.02f * smallChestScale
                  scaleY = 0.98f + 0.02f * smallChestScale
                  rotationZ = smallChestTilt * 0.6f
                  translationY = smallChestBobDp.dp.toPx()
                },
          )
          Canvas(modifier = Modifier.fillMaxSize()) {
            fun drawFourPointStar(cx: Float, cy: Float, radius: Float, alpha: Float) {
              if (alpha <= 0.05f || radius <= 0.5f) return
              val inner = radius * 0.24f
              val starPath =
                Path().apply {
                  moveTo(cx, cy - radius)
                  lineTo(cx + inner, cy - inner)
                  lineTo(cx + radius, cy)
                  lineTo(cx + inner, cy + inner)
                  lineTo(cx, cy + radius)
                  lineTo(cx - inner, cy + inner)
                  lineTo(cx - radius, cy)
                  lineTo(cx - inner, cy - inner)
                  close()
                }
              drawPath(path = starPath, color = Color.White.copy(alpha = alpha))
            }

            val t1 = ((sin(smallSparklePhase.toDouble()) + 1.0) * 0.5).toFloat()
            val t2 = ((sin((smallSparklePhase + 2.1f).toDouble()) + 1.0) * 0.5).toFloat()
            val t3 = ((sin((smallSparklePhase + 4.2f).toDouble()) + 1.0) * 0.5).toFloat()

            drawFourPointStar(
              cx = size.width * 0.12f,
              cy = size.height * 0.22f,
              radius = size.minDimension * (0.06f + 0.05f * t1),
              alpha = 0.25f + 0.75f * t1,
            )
            drawFourPointStar(
              cx = size.width * 0.88f,
              cy = size.height * 0.20f,
              radius = size.minDimension * (0.06f + 0.05f * t2),
              alpha = 0.25f + 0.75f * t2,
            )
            drawFourPointStar(
              cx = size.width * 0.78f,
              cy = size.height * 0.06f,
              radius = size.minDimension * (0.05f + 0.05f * t3),
              alpha = 0.25f + 0.75f * t3,
            )
          }
        }

        // Bold crisp white "5000" label with sharp black outline right below the chest coins
        val smallCoinsLabel = stringResource(R.string.shop_small_pack_coins_amount)
        val smallCoinsLabelStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (cardH.value * 0.122f).coerceIn(15f, 24f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.3.sp,
          )
        val smallCoinsBoxW = cardW * 0.25f
        Box(
          modifier =
            Modifier
              .offset(x = smallChestCenterX - smallCoinsBoxW * 0.50f, y = cardH * 0.482f)
              .width(smallCoinsBoxW)
              .testTag("shop_small_pack_coins_label"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = smallCoinsLabel,
            style =
              smallCoinsLabelStyle.copy(
                drawStyle = Stroke(width = 8.5f, join = StrokeJoin.Round),
              ),
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.offset(y = 1.2.dp),
          )
          Text(
            text = smallCoinsLabel,
            style =
              smallCoinsLabelStyle.copy(
                drawStyle = Stroke(width = 7.0f, join = StrokeJoin.Round),
              ),
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
          )
          Text(
            text = smallCoinsLabel,
            style = smallCoinsLabelStyle,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
          )
        }

        val smallRibbonW = cardW * 0.530f
        val smallRibbonH = cardH * 0.240f
        val smallRibbonTitle = stringResource(R.string.shop_small_pack_title)
        val smallRibbonTextStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (smallRibbonH.value * 0.48f).coerceIn(13.5f, 22f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.25.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.045f, y = cardH * 0.688f)
              .size(width = smallRibbonW, height = smallRibbonH)
              .testTag("shop_small_pack_ribbon"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.ribbon_shop_no_ads_blue),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(horizontal = smallRibbonW * 0.14f)
                .padding(bottom = smallRibbonH * 0.14f),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = smallRibbonTitle,
              style =
                smallRibbonTextStyle.copy(
                  drawStyle = Stroke(width = 9f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF002B7A),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.offset(y = 1.8.dp),
            )
            Text(
              text = smallRibbonTitle,
              style =
                smallRibbonTextStyle.copy(
                  drawStyle = Stroke(width = 7.5f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF003B9E),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
            Text(
              text = smallRibbonTitle,
              style =
                smallRibbonTextStyle.copy(
                  shadow = Shadow(color = Color(0x9900256E), offset = Offset(0f, 1.5f), blurRadius = 1.5f),
                ),
              color = Color.White,
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
          }
        }

        val smallPriceBtnW = cardW * 0.273f
        val smallPriceBtnH = cardH * 0.224f
        val smallPriceTouchH = maxOf(smallPriceBtnH, 48.dp)
        val smallPriceBtnCenterY = cardH * 0.693f + smallPriceBtnH * 0.50f
        val smallPriceInteraction = remember { MutableInteractionSource() }
        val isSmallPricePressed by smallPriceInteraction.collectIsPressedAsState()
        val smallPriceScale by animateFloatAsState(
          targetValue = if (isSmallPricePressed) 0.93f else 1f,
          animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
          label = "smallPackPriceBtnScale",
        )
        val smallPriceText = stringResource(R.string.shop_small_pack_price)
        val smallPriceStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (smallPriceBtnH.value * 0.345f).coerceIn(11.0f, 17.5f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.690f, y = smallPriceBtnCenterY - smallPriceTouchH * 0.50f)
              .size(width = smallPriceBtnW, height = smallPriceTouchH)
              .scale(smallPriceScale)
              .clickable(
                interactionSource = smallPriceInteraction,
                indication = null,
                onClick = {},
              )
              .testTag("shop_small_pack_price_button"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier = Modifier.size(width = smallPriceBtnW, height = smallPriceBtnH),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              painter = painterResource(id = R.drawable.btn_shop_price_green),
              contentDescription = "$smallRibbonTitle $smallPriceText",
              contentScale = ContentScale.FillBounds,
              modifier = Modifier.fillMaxSize(),
            )
            Box(
              modifier =
                Modifier
                  .fillMaxSize()
                  .padding(horizontal = smallPriceBtnW * 0.05f)
                  .padding(bottom = smallPriceBtnH * 0.06f),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = smallPriceText,
                style =
                  smallPriceStyle.copy(
                    drawStyle = Stroke(width = 7.0f, join = StrokeJoin.Round),
                  ),
                color = Color(0xFF063810),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.offset(y = 1.2.dp),
              )
              Text(
                text = smallPriceText,
                style =
                  smallPriceStyle.copy(
                    drawStyle = Stroke(width = 5.5f, join = StrokeJoin.Round),
                  ),
                color = Color(0xFF0A4D18),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
              )
              Text(
                text = smallPriceText,
                style =
                  smallPriceStyle.copy(
                    shadow = Shadow(color = Color(0x88063810), offset = Offset(0f, 1.2f), blurRadius = 1.2f),
                  ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
              )
            }
          }
        }

        val smallTrayW = cardW * 0.288f
        val smallTrayH = cardH * 0.370f
        val smallQtyOne = stringResource(R.string.shop_booster_qty_one)
        val smallQtyStyle =
          MaterialTheme.typography.labelLarge.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (smallTrayH.value * 0.225f).coerceIn(10.5f, 16.5f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.628f, y = cardH * 0.186f)
              .size(width = smallTrayW, height = smallTrayH)
              .testTag("shop_small_pack_booster_tray"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.panel_starter_pack_blue_tray),
            contentDescription = stringResource(R.string.shop_small_pack_tray_desc),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
          )
          Row(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(horizontal = smallTrayW * 0.065f, vertical = smallTrayH * 0.085f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val smallBoosters =
              listOf(
                Pair(R.drawable.ic_shop_booster_vip, R.string.shop_booster_vip_desc),
                Pair(R.drawable.ic_shop_booster_bus, R.string.shop_booster_bus_desc),
                Pair(R.drawable.ic_shop_booster_refresh, R.string.shop_booster_refresh_desc),
              )
            smallBoosters.forEach { (iconRes, descRes) ->
              Box(
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
              ) {
                Image(
                  painter = painterResource(id = iconRes),
                  contentDescription = stringResource(descRes),
                  contentScale = ContentScale.Fit,
                  modifier =
                    Modifier
                      .fillMaxWidth(0.92f)
                      .height(smallTrayH * 0.62f)
                      .padding(top = smallTrayH * 0.02f),
                )
                Box(
                  modifier =
                    Modifier
                      .align(Alignment.BottomCenter)
                      .padding(bottom = smallTrayH * 0.01f),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = smallQtyOne,
                    style =
                      smallQtyStyle.copy(
                        drawStyle = Stroke(width = 5.5f, join = StrokeJoin.Round),
                      ),
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(y = 0.8.dp),
                  )
                  Text(
                    text = smallQtyOne,
                    style =
                      smallQtyStyle.copy(
                        drawStyle = Stroke(width = 4.5f, join = StrokeJoin.Round),
                      ),
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false,
                  )
                  Text(
                    text = smallQtyOne,
                    style = smallQtyStyle,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
          }
        }
      }

      Spacer(Modifier.height(20.dp))

      val specialPackPanelInteraction = remember { MutableInteractionSource() }
      BoxWithConstraints(
        modifier =
          Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .aspectRatio(688f / 335f)
            .clickable(
              interactionSource = specialPackPanelInteraction,
              indication = null,
              onClick = {},
            )
            .testTag("shop_special_pack_purple_panel"),
      ) {
        val cardW = maxWidth
        val cardH = maxHeight

        Image(
          painter = painterResource(id = R.drawable.panel_shop_special_purple),
          contentDescription = stringResource(R.string.shop_special_pack_panel_desc),
          contentScale = ContentScale.FillBounds,
          modifier = Modifier.fillMaxSize(),
        )

        val specialChestW = cardW * 0.455f
        val specialChestH = specialChestW * (360f / 669f)
        val specialChestCenterX = cardW * 0.330f
        val specialChestTop = cardH * 0.072f
        val specialChestCenterY = specialChestTop + specialChestH * 0.50f
        val specialChestAnim = rememberInfiniteTransition(label = "specialPackChestAnim")
        val specialRaySpin by specialChestAnim.animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 9000, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "specialChestRaySpin",
        )
        val specialGlowPulse by specialChestAnim.animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 2400, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "specialChestGlowPulse",
        )
        val specialChestScale by specialChestAnim.animateFloat(
          initialValue = 0.96f,
          targetValue = 1.04f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "specialChestScale",
        )
        val specialChestTilt by specialChestAnim.animateFloat(
          initialValue = -2.5f,
          targetValue = 2.5f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1150, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "specialChestTilt",
        )
        val specialChestBobDp by specialChestAnim.animateFloat(
          initialValue = -1.6f,
          targetValue = 1.6f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 980, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "specialChestBob",
        )
        val specialLeftCoinBobDp by specialChestAnim.animateFloat(
          initialValue = 1.8f,
          targetValue = -1.8f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 820, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "specialLeftCoinBob",
        )
        val specialLeftCoinTilt by specialChestAnim.animateFloat(
          initialValue = -3.5f,
          targetValue = 3.0f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1040, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "specialLeftCoinTilt",
        )
        val specialRightCoinBobDp by specialChestAnim.animateFloat(
          initialValue = -1.8f,
          targetValue = 1.8f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 880, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "specialRightCoinBob",
        )
        val specialRightCoinTilt by specialChestAnim.animateFloat(
          initialValue = 3.5f,
          targetValue = -3.0f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "specialRightCoinTilt",
        )
        val specialSparklePhase by specialChestAnim.animateFloat(
          initialValue = 0f,
          targetValue = (2.0 * PI).toFloat(),
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1800, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "specialChestSparkle",
        )

        // Rotating white sunbeam rays & radiant halo strictly behind the Special Pack chest
        Canvas(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(cardH * 0.65f)
              .padding(horizontal = cardW * 0.024f, vertical = cardH * 0.055f)
              .clip(RoundedCornerShape(cardH * 0.11f))
              .testTag("shop_special_pack_chest_rays"),
        ) {
          val insetX = (cardW * 0.024f).toPx()
          val insetY = (cardH * 0.055f).toPx()
          val cx = specialChestCenterX.toPx() - insetX
          val cy = specialChestCenterY.toPx() - insetY
          val maxR = (cardW * 0.255f).toPx()
          val haloAlpha = 0.72f + 0.24f * sin(specialGlowPulse * 2f * PI.toFloat())

          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    Color(0xF0FFFBE8).copy(alpha = haloAlpha),
                    Color(0x99FFFFFF).copy(alpha = haloAlpha * 0.75f),
                    Color.Transparent,
                  ),
                center = Offset(cx, cy),
                radius = maxR * 0.85f,
              ),
            radius = maxR * 0.85f,
            center = Offset(cx, cy),
          )

          val rayBrush =
            Brush.radialGradient(
              colors =
                listOf(
                  Color(0xE8FFFFFF),
                  Color(0x88F7EBFF),
                  Color(0x22FFFFFF),
                  Color.Transparent,
                ),
              center = Offset(cx, cy),
              radius = maxR,
            )
          rotate(degrees = specialRaySpin, pivot = Offset(cx, cy)) {
            val rayCount = 12
            for (i in 0 until rayCount) {
              val a0 = (i * (360.0 / rayCount)) * (PI / 180.0)
              val halfSpread = 0.12
              val a1 = a0 + halfSpread
              val a2 = a0 - halfSpread
              val rayPath =
                Path().apply {
                  moveTo(cx, cy)
                  lineTo(cx + cos(a1).toFloat() * maxR, cy + sin(a1).toFloat() * maxR)
                  lineTo(cx + cos(a2).toFloat() * maxR, cy + sin(a2).toFloat() * maxR)
                  close()
                }
              drawPath(rayPath, brush = rayBrush)
            }
          }
        }

        // Left flanking coin sack & gold coin hoard (flipped horizontally so sack sits beside chest and coins spill left)
        val specialLeftHoardSize = cardW * 0.215f
        Image(
          painter = painterResource(id = R.drawable.ic_shop_special_coin_hoard),
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier =
            Modifier
              .offset(x = specialChestCenterX - specialChestW * 0.54f, y = cardH * 0.145f)
              .size(specialLeftHoardSize)
              .graphicsLayer {
                scaleX = -1f * (0.97f + 0.03f * specialChestScale)
                scaleY = 0.97f + 0.03f * specialChestScale
                rotationZ = specialLeftCoinTilt * 0.6f
                translationY = specialLeftCoinBobDp.dp.toPx()
              },
        )

        // Right flanking coin sack & gold coin hoard (sack beside chest, coins spill right)
        val specialRightHoardSize = cardW * 0.190f
        Image(
          painter = painterResource(id = R.drawable.ic_shop_special_coin_hoard),
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier =
            Modifier
              .offset(x = specialChestCenterX + specialChestW * 0.19f, y = cardH * 0.185f)
              .size(specialRightHoardSize)
              .graphicsLayer {
                scaleX = 0.97f + 0.03f * specialChestScale
                scaleY = 0.97f + 0.03f * specialChestScale
                rotationZ = specialRightCoinTilt * 0.6f
                translationY = specialRightCoinBobDp.dp.toPx()
              },
        )

        // Left-facing violet treasure chest overflowing with gold coins + twinkling stars
        Box(
          modifier =
            Modifier
              .offset(x = specialChestCenterX - specialChestW * 0.50f, y = specialChestTop)
              .size(width = specialChestW, height = specialChestH)
              .testTag("shop_special_pack_animated_chest"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_shop_special_pack_chest),
            contentDescription = stringResource(R.string.shop_special_pack_chest_desc),
            contentScale = ContentScale.Fit,
            modifier =
              Modifier
                .fillMaxSize()
                .graphicsLayer {
                  scaleX = 0.98f + 0.02f * specialChestScale
                  scaleY = 0.98f + 0.02f * specialChestScale
                  rotationZ = specialChestTilt * 0.6f
                  translationY = specialChestBobDp.dp.toPx()
                },
          )
          Canvas(modifier = Modifier.fillMaxSize()) {
            fun drawFourPointStar(cx: Float, cy: Float, radius: Float, alpha: Float) {
              if (alpha <= 0.05f || radius <= 0.5f) return
              val inner = radius * 0.24f
              val starPath =
                Path().apply {
                  moveTo(cx, cy - radius)
                  lineTo(cx + inner, cy - inner)
                  lineTo(cx + radius, cy)
                  lineTo(cx + inner, cy + inner)
                  lineTo(cx, cy + radius)
                  lineTo(cx - inner, cy + inner)
                  lineTo(cx - radius, cy)
                  lineTo(cx - inner, cy - inner)
                  close()
                }
              drawPath(path = starPath, color = Color.White.copy(alpha = alpha))
            }

            val t1 = ((sin(specialSparklePhase.toDouble()) + 1.0) * 0.5).toFloat()
            val t2 = ((sin((specialSparklePhase + 2.1f).toDouble()) + 1.0) * 0.5).toFloat()
            val t3 = ((sin((specialSparklePhase + 4.2f).toDouble()) + 1.0) * 0.5).toFloat()

            drawFourPointStar(
              cx = size.width * 0.12f,
              cy = size.height * 0.22f,
              radius = size.minDimension * (0.06f + 0.05f * t1),
              alpha = 0.25f + 0.75f * t1,
            )
            drawFourPointStar(
              cx = size.width * 0.88f,
              cy = size.height * 0.20f,
              radius = size.minDimension * (0.06f + 0.05f * t2),
              alpha = 0.25f + 0.75f * t2,
            )
            drawFourPointStar(
              cx = size.width * 0.78f,
              cy = size.height * 0.06f,
              radius = size.minDimension * (0.05f + 0.05f * t3),
              alpha = 0.25f + 0.75f * t3,
            )
          }
        }

        // Bold crisp white "10000" label with sharp black outline right below the Special Pack chest
        val specialCoinsLabel = stringResource(R.string.shop_special_pack_coins_amount)
        val specialCoinsLabelStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (cardH.value * 0.118f).coerceIn(15f, 24f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.3.sp,
          )
        val specialCoinsBoxW = cardW * 0.28f
        Box(
          modifier =
            Modifier
              .offset(x = specialChestCenterX - specialCoinsBoxW * 0.50f, y = cardH * 0.482f)
              .width(specialCoinsBoxW)
              .testTag("shop_special_pack_coins_label"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = specialCoinsLabel,
            style =
              specialCoinsLabelStyle.copy(
                drawStyle = Stroke(width = 8.5f, join = StrokeJoin.Round),
              ),
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.offset(y = 1.2.dp),
          )
          Text(
            text = specialCoinsLabel,
            style =
              specialCoinsLabelStyle.copy(
                drawStyle = Stroke(width = 7.0f, join = StrokeJoin.Round),
              ),
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
          )
          Text(
            text = specialCoinsLabel,
            style = specialCoinsLabelStyle,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
          )
        }

        val specialRibbonW = cardW * 0.525f
        val specialRibbonH = cardH * 0.238f
        val specialRibbonTitle = stringResource(R.string.shop_special_pack_title)
        val specialRibbonTextStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (specialRibbonH.value * 0.48f).coerceIn(13.5f, 22f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.25.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.045f, y = cardH * 0.696f)
              .size(width = specialRibbonW, height = specialRibbonH)
              .testTag("shop_special_pack_ribbon"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.ribbon_shop_no_ads_purple),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(horizontal = specialRibbonW * 0.14f)
                .padding(bottom = specialRibbonH * 0.14f),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = specialRibbonTitle,
              style =
                specialRibbonTextStyle.copy(
                  drawStyle = Stroke(width = 7.5f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF47117A),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.offset(y = 1.5.dp),
            )
            Text(
              text = specialRibbonTitle,
              style =
                specialRibbonTextStyle.copy(
                  drawStyle = Stroke(width = 6.0f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF531888),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
            Text(
              text = specialRibbonTitle,
              style =
                specialRibbonTextStyle.copy(
                  shadow = Shadow(color = Color(0x993E0A72), offset = Offset(0f, 1.5f), blurRadius = 1.5f),
                ),
              color = Color.White,
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
          }
        }

        val specialPriceBtnW = cardW * 0.272f
        val specialPriceBtnH = cardH * 0.200f
        val specialPriceTouchH = maxOf(specialPriceBtnH, 48.dp)
        val specialPriceBtnCenterY = cardH * 0.718f + specialPriceBtnH * 0.50f
        val specialPriceInteraction = remember { MutableInteractionSource() }
        val isSpecialPricePressed by specialPriceInteraction.collectIsPressedAsState()
        val specialPriceScale by animateFloatAsState(
          targetValue = if (isSpecialPricePressed) 0.93f else 1f,
          animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
          label = "specialPackPriceBtnScale",
        )
        val specialPriceText = stringResource(R.string.shop_special_pack_price)
        val specialPriceStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (specialPriceBtnH.value * 0.345f).coerceIn(11.0f, 17.5f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.690f, y = specialPriceBtnCenterY - specialPriceTouchH * 0.50f)
              .size(width = specialPriceBtnW, height = specialPriceTouchH)
              .scale(specialPriceScale)
              .clickable(
                interactionSource = specialPriceInteraction,
                indication = null,
                onClick = {},
              )
              .testTag("shop_special_pack_price_button"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier = Modifier.size(width = specialPriceBtnW, height = specialPriceBtnH),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              painter = painterResource(id = R.drawable.btn_shop_price_green),
              contentDescription = "$specialRibbonTitle $specialPriceText",
              contentScale = ContentScale.FillBounds,
              modifier = Modifier.fillMaxSize(),
            )
            Box(
              modifier =
                Modifier
                  .fillMaxSize()
                  .padding(horizontal = specialPriceBtnW * 0.05f)
                  .padding(bottom = specialPriceBtnH * 0.06f),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = specialPriceText,
                style =
                  specialPriceStyle.copy(
                    drawStyle = Stroke(width = 7.0f, join = StrokeJoin.Round),
                  ),
                color = Color(0xFF063810),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.offset(y = 1.2.dp),
              )
              Text(
                text = specialPriceText,
                style =
                  specialPriceStyle.copy(
                    drawStyle = Stroke(width = 5.5f, join = StrokeJoin.Round),
                  ),
                color = Color(0xFF0A4D18),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
              )
              Text(
                text = specialPriceText,
                style =
                  specialPriceStyle.copy(
                    shadow = Shadow(color = Color(0x88063810), offset = Offset(0f, 1.2f), blurRadius = 1.2f),
                  ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
              )
            }
          }
        }

        val specialTrayW = cardW * 0.288f
        val specialTrayH = cardH * 0.370f
        val specialQtyThree = stringResource(R.string.shop_booster_qty_three)
        val specialQtyStyle =
          MaterialTheme.typography.labelLarge.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (specialTrayH.value * 0.225f).coerceIn(10.5f, 16.5f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.628f, y = cardH * 0.191f)
              .size(width = specialTrayW, height = specialTrayH)
              .testTag("shop_special_pack_booster_tray"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.panel_shop_special_pack_tray),
            contentDescription = stringResource(R.string.shop_special_pack_tray_desc),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
          )
          Row(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(horizontal = specialTrayW * 0.065f, vertical = specialTrayH * 0.085f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val specialBoosters =
              listOf(
                Pair(R.drawable.ic_shop_booster_vip, R.string.shop_booster_vip_desc),
                Pair(R.drawable.ic_shop_booster_bus, R.string.shop_booster_bus_desc),
                Pair(R.drawable.ic_shop_booster_refresh, R.string.shop_booster_refresh_desc),
              )
            specialBoosters.forEach { (iconRes, descRes) ->
              Box(
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
              ) {
                Image(
                  painter = painterResource(id = iconRes),
                  contentDescription = stringResource(descRes),
                  contentScale = ContentScale.Fit,
                  modifier =
                    Modifier
                      .fillMaxWidth(0.92f)
                      .height(specialTrayH * 0.62f)
                      .padding(top = specialTrayH * 0.02f),
                )
                Box(
                  modifier =
                    Modifier
                      .align(Alignment.BottomCenter)
                      .padding(bottom = specialTrayH * 0.01f),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = specialQtyThree,
                    style =
                      specialQtyStyle.copy(
                        drawStyle = Stroke(width = 5.5f, join = StrokeJoin.Round),
                      ),
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(y = 0.8.dp),
                  )
                  Text(
                    text = specialQtyThree,
                    style =
                      specialQtyStyle.copy(
                        drawStyle = Stroke(width = 4.5f, join = StrokeJoin.Round),
                      ),
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false,
                  )
                  Text(
                    text = specialQtyThree,
                    style = specialQtyStyle,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
          }
        }
      }

      Spacer(Modifier.height(20.dp))

      val largePackPanelInteraction = remember { MutableInteractionSource() }
      BoxWithConstraints(
        modifier =
          Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
            .aspectRatio(689f / 351f)
            .clickable(
              interactionSource = largePackPanelInteraction,
              indication = null,
              onClick = {},
            )
            .testTag("shop_large_pack_orange_red_panel"),
      ) {
        val cardW = maxWidth
        val cardH = maxHeight

        Image(
          painter = painterResource(id = R.drawable.panel_shop_large_orange_red),
          contentDescription = stringResource(R.string.shop_large_pack_panel_desc),
          contentScale = ContentScale.FillBounds,
          modifier = Modifier.fillMaxSize(),
        )

        val largeChestW = cardW * 0.455f
        val largeChestH = largeChestW * (369f / 654f)
        val largeChestCenterX = cardW * 0.330f
        val largeChestTop = cardH * 0.068f
        val largeChestCenterY = largeChestTop + largeChestH * 0.50f
        val largeChestAnim = rememberInfiniteTransition(label = "largePackChestAnim")
        val largeRaySpin by largeChestAnim.animateFloat(
          initialValue = 0f,
          targetValue = 360f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 9000, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "largeChestRaySpin",
        )
        val largeGlowPulse by largeChestAnim.animateFloat(
          initialValue = 0f,
          targetValue = 1f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 2400, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "largeChestGlowPulse",
        )
        val largeChestScale by largeChestAnim.animateFloat(
          initialValue = 0.96f,
          targetValue = 1.04f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 900, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "largeChestScale",
        )
        val largeChestTilt by largeChestAnim.animateFloat(
          initialValue = -2.0f,
          targetValue = 2.0f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1150, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "largeChestTilt",
        )
        val largeChestBobDp by largeChestAnim.animateFloat(
          initialValue = -1.5f,
          targetValue = 1.5f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 980, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "largeChestBob",
        )
        val largeLeftCoinBobDp by largeChestAnim.animateFloat(
          initialValue = 1.8f,
          targetValue = -1.8f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 820, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "largeLeftCoinBob",
        )
        val largeLeftCoinTilt by largeChestAnim.animateFloat(
          initialValue = -3.0f,
          targetValue = 2.8f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1040, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "largeLeftCoinTilt",
        )
        val largeRightCoinBobDp by largeChestAnim.animateFloat(
          initialValue = -1.8f,
          targetValue = 1.8f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 880, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "largeRightCoinBob",
        )
        val largeRightCoinTilt by largeChestAnim.animateFloat(
          initialValue = 3.0f,
          targetValue = -2.8f,
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1100, easing = FastOutSlowInEasing),
              repeatMode = RepeatMode.Reverse,
            ),
          label = "largeRightCoinTilt",
        )
        val largeSparklePhase by largeChestAnim.animateFloat(
          initialValue = 0f,
          targetValue = (2.0 * PI).toFloat(),
          animationSpec =
            infiniteRepeatable(
              animation = tween(durationMillis = 1800, easing = LinearEasing),
              repeatMode = RepeatMode.Restart,
            ),
          label = "largeChestSparkle",
        )

        // Rotating white sunbeam rays & radiant halo strictly behind the Large Pack chest
        Canvas(
          modifier =
            Modifier
              .fillMaxWidth()
              .height(cardH * 0.63f)
              .padding(horizontal = cardW * 0.024f, vertical = cardH * 0.052f)
              .clip(RoundedCornerShape(cardH * 0.11f))
              .testTag("shop_large_pack_chest_rays"),
        ) {
          val insetX = (cardW * 0.024f).toPx()
          val insetY = (cardH * 0.052f).toPx()
          val cx = largeChestCenterX.toPx() - insetX
          val cy = largeChestCenterY.toPx() - insetY
          val maxR = (cardW * 0.255f).toPx()
          val haloAlpha = 0.72f + 0.24f * sin(largeGlowPulse * 2f * PI.toFloat())

          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    Color(0xF0FFFBE8).copy(alpha = haloAlpha),
                    Color(0x99FFFFFF).copy(alpha = haloAlpha * 0.75f),
                    Color.Transparent,
                  ),
                center = Offset(cx, cy),
                radius = maxR * 0.85f,
              ),
            radius = maxR * 0.85f,
            center = Offset(cx, cy),
          )

          val rayBrush =
            Brush.radialGradient(
              colors =
                listOf(
                  Color(0xE8FFFFFF),
                  Color(0x88FFF0DE),
                  Color(0x22FFFFFF),
                  Color.Transparent,
                ),
              center = Offset(cx, cy),
              radius = maxR,
            )
          rotate(degrees = largeRaySpin, pivot = Offset(cx, cy)) {
            val rayCount = 12
            for (i in 0 until rayCount) {
              val a0 = (i * (360.0 / rayCount)) * (PI / 180.0)
              val halfSpread = 0.12
              val a1 = a0 + halfSpread
              val a2 = a0 - halfSpread
              val rayPath =
                Path().apply {
                  moveTo(cx, cy)
                  lineTo(cx + cos(a1).toFloat() * maxR, cy + sin(a1).toFloat() * maxR)
                  lineTo(cx + cos(a2).toFloat() * maxR, cy + sin(a2).toFloat() * maxR)
                  close()
                }
              drawPath(rayPath, brush = rayBrush)
            }
          }
        }

        // Left flanking coin sack & gold coin hoard behind the chest (flipped so sack sits beside chest)
        val largeLeftHoardSize = cardW * 0.215f
        Image(
          painter = painterResource(id = R.drawable.ic_shop_special_coin_hoard),
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier =
            Modifier
              .offset(x = largeChestCenterX - largeChestW * 0.54f, y = cardH * 0.145f)
              .size(largeLeftHoardSize)
              .graphicsLayer {
                scaleX = -1f * (0.97f + 0.03f * largeChestScale)
                scaleY = 0.97f + 0.03f * largeChestScale
                rotationZ = largeLeftCoinTilt
                translationY = largeLeftCoinBobDp.dp.toPx()
              },
        )

        // Right flanking coin sack & gold coin hoard behind the chest
        val largeRightHoardSize = cardW * 0.190f
        Image(
          painter = painterResource(id = R.drawable.ic_shop_special_coin_hoard),
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier =
            Modifier
              .offset(x = largeChestCenterX + largeChestW * 0.19f, y = cardH * 0.185f)
              .size(largeRightHoardSize)
              .graphicsLayer {
                scaleX = 0.97f + 0.03f * largeChestScale
                scaleY = 0.97f + 0.03f * largeChestScale
                rotationZ = largeRightCoinTilt
                translationY = largeRightCoinBobDp.dp.toPx()
              },
        )

        // Center red-and-blue treasure chest overflowing with gold coins + twinkling stars
        Box(
          modifier =
            Modifier
              .offset(x = largeChestCenterX - largeChestW * 0.50f, y = largeChestTop)
              .size(width = largeChestW, height = largeChestH)
              .testTag("shop_large_pack_animated_chest"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.ic_shop_large_pack_chest),
            contentDescription = stringResource(R.string.shop_large_pack_chest_desc),
            contentScale = ContentScale.Fit,
            modifier =
              Modifier
                .fillMaxSize()
                .graphicsLayer {
                  scaleX = largeChestScale
                  scaleY = largeChestScale
                  rotationZ = largeChestTilt
                  translationY = largeChestBobDp.dp.toPx()
                },
          )
          Canvas(modifier = Modifier.fillMaxSize()) {
            fun drawFourPointStar(cx: Float, cy: Float, radius: Float, alpha: Float) {
              if (alpha <= 0.05f || radius <= 0.5f) return
              val inner = radius * 0.24f
              val starPath =
                Path().apply {
                  moveTo(cx, cy - radius)
                  lineTo(cx + inner, cy - inner)
                  lineTo(cx + radius, cy)
                  lineTo(cx + inner, cy + inner)
                  lineTo(cx, cy + radius)
                  lineTo(cx - inner, cy + inner)
                  lineTo(cx - radius, cy)
                  lineTo(cx - inner, cy - inner)
                  close()
                }
              drawPath(path = starPath, color = Color.White.copy(alpha = alpha))
            }

            val t1 = ((sin(largeSparklePhase.toDouble()) + 1.0) * 0.5).toFloat()
            val t2 = ((sin((largeSparklePhase + 2.1f).toDouble()) + 1.0) * 0.5).toFloat()
            val t3 = ((sin((largeSparklePhase + 4.2f).toDouble()) + 1.0) * 0.5).toFloat()

            drawFourPointStar(
              cx = size.width * 0.12f,
              cy = size.height * 0.22f,
              radius = size.minDimension * (0.06f + 0.05f * t1),
              alpha = 0.25f + 0.75f * t1,
            )
            drawFourPointStar(
              cx = size.width * 0.88f,
              cy = size.height * 0.20f,
              radius = size.minDimension * (0.06f + 0.05f * t2),
              alpha = 0.25f + 0.75f * t2,
            )
            drawFourPointStar(
              cx = size.width * 0.78f,
              cy = size.height * 0.06f,
              radius = size.minDimension * (0.05f + 0.05f * t3),
              alpha = 0.25f + 0.75f * t3,
            )
          }
        }

        // Bold crisp white "25000" label with sharp black outline right below the Large Pack chest
        val largeCoinsLabel = stringResource(R.string.shop_large_pack_coins_amount)
        val largeCoinsLabelStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (cardH.value * 0.118f).coerceIn(15f, 24f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.3.sp,
          )
        val largeCoinsBoxW = cardW * 0.28f
        Box(
          modifier =
            Modifier
              .offset(x = largeChestCenterX - largeCoinsBoxW * 0.50f, y = cardH * 0.475f)
              .width(largeCoinsBoxW)
              .testTag("shop_large_pack_coins_label"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = largeCoinsLabel,
            style =
              largeCoinsLabelStyle.copy(
                drawStyle = Stroke(width = 8.5f, join = StrokeJoin.Round),
              ),
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.offset(y = 1.2.dp),
          )
          Text(
            text = largeCoinsLabel,
            style =
              largeCoinsLabelStyle.copy(
                drawStyle = Stroke(width = 7.0f, join = StrokeJoin.Round),
              ),
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
          )
          Text(
            text = largeCoinsLabel,
            style = largeCoinsLabelStyle,
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
          )
        }

        val largeRibbonW = cardW * 0.606f
        val largeRibbonH = largeRibbonW * (172f / 856f)
        val largeRibbonTitle = stringResource(R.string.shop_large_pack_title)
        val largeRibbonTextStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (largeRibbonH.value * 0.48f).coerceIn(13.5f, 22f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.25.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.032f, y = cardH * 0.672f)
              .size(width = largeRibbonW, height = largeRibbonH)
              .testTag("shop_large_pack_ribbon"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.ribbon_shop_large_red),
            contentDescription = null,
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(horizontal = largeRibbonW * 0.18f)
                .padding(bottom = largeRibbonH * 0.12f),
            contentAlignment = Alignment.Center,
          ) {
            Text(
              text = largeRibbonTitle,
              style =
                largeRibbonTextStyle.copy(
                  drawStyle = Stroke(width = 7.5f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF5A0606),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.offset(y = 1.5.dp),
            )
            Text(
              text = largeRibbonTitle,
              style =
                largeRibbonTextStyle.copy(
                  drawStyle = Stroke(width = 6.0f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF700B0B),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
            Text(
              text = largeRibbonTitle,
              style =
                largeRibbonTextStyle.copy(
                  shadow = Shadow(color = Color(0x994A0000), offset = Offset(0f, 1.5f), blurRadius = 1.5f),
                ),
              color = Color.White,
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
          }
        }

        val largeTrayW = cardW * 0.288f
        val largeTrayH = cardH * 0.355f
        val largeQtyFour = stringResource(R.string.shop_booster_qty_four)
        val largeQtyStyle =
          MaterialTheme.typography.labelLarge.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (largeTrayH.value * 0.225f).coerceIn(10.5f, 16.5f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.2.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.628f, y = cardH * 0.169f)
              .size(width = largeTrayW, height = largeTrayH)
              .testTag("shop_large_pack_booster_tray"),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.panel_shop_large_pack_tray),
            contentDescription = stringResource(R.string.shop_large_pack_tray_desc),
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
          )
          Row(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(horizontal = largeTrayW * 0.065f, vertical = largeTrayH * 0.085f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
          ) {
            val largeBoosters =
              listOf(
                Pair(R.drawable.ic_shop_booster_vip, R.string.shop_booster_vip_desc),
                Pair(R.drawable.ic_shop_booster_bus, R.string.shop_booster_bus_desc),
                Pair(R.drawable.ic_shop_booster_refresh, R.string.shop_booster_refresh_desc),
              )
            largeBoosters.forEach { (iconRes, descRes) ->
              Box(
                modifier = Modifier.weight(1f).fillMaxSize(),
                contentAlignment = Alignment.TopCenter,
              ) {
                Image(
                  painter = painterResource(id = iconRes),
                  contentDescription = stringResource(descRes),
                  contentScale = ContentScale.Fit,
                  modifier =
                    Modifier
                      .fillMaxWidth(0.92f)
                      .height(largeTrayH * 0.62f)
                      .padding(top = largeTrayH * 0.02f),
                )
                Box(
                  modifier =
                    Modifier
                      .align(Alignment.BottomCenter)
                      .padding(bottom = largeTrayH * 0.01f),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = largeQtyFour,
                    style =
                      largeQtyStyle.copy(
                        drawStyle = Stroke(width = 5.5f, join = StrokeJoin.Round),
                      ),
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(y = 0.8.dp),
                  )
                  Text(
                    text = largeQtyFour,
                    style =
                      largeQtyStyle.copy(
                        drawStyle = Stroke(width = 4.5f, join = StrokeJoin.Round),
                      ),
                    color = Color.Black,
                    maxLines = 1,
                    softWrap = false,
                  )
                  Text(
                    text = largeQtyFour,
                    style = largeQtyStyle,
                    color = Color.White,
                    maxLines = 1,
                    softWrap = false,
                  )
                }
              }
            }
          }
        }

        val largePriceBtnW = cardW * 0.272f
        val largePriceBtnH = cardH * 0.198f
        val largePriceTouchH = maxOf(largePriceBtnH, 48.dp)
        val largePriceBtnCenterY = cardH * 0.692f + largePriceBtnH * 0.50f
        val largePriceInteraction = remember { MutableInteractionSource() }
        val isLargePricePressed by largePriceInteraction.collectIsPressedAsState()
        val largePriceScale by animateFloatAsState(
          targetValue = if (isLargePricePressed) 0.93f else 1f,
          animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
          label = "largePackPriceBtnScale",
        )
        val largePriceText = stringResource(R.string.shop_large_pack_price)
        val largePriceStyle =
          MaterialTheme.typography.titleMedium.copy(
            fontFamily = LilitaOneFontFamily,
            fontSize = (largePriceBtnH.value * 0.345f).coerceIn(11.0f, 17.5f).sp,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
          )
        Box(
          modifier =
            Modifier
              .offset(x = cardW * 0.690f, y = largePriceBtnCenterY - largePriceTouchH * 0.50f)
              .size(width = largePriceBtnW, height = largePriceTouchH)
              .scale(largePriceScale)
              .clickable(
                interactionSource = largePriceInteraction,
                indication = null,
                onClick = {},
              )
              .testTag("shop_large_pack_price_button"),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier = Modifier.size(width = largePriceBtnW, height = largePriceBtnH),
            contentAlignment = Alignment.Center,
          ) {
            Image(
              painter = painterResource(id = R.drawable.btn_shop_price_green),
              contentDescription = "$largeRibbonTitle $largePriceText",
              contentScale = ContentScale.FillBounds,
              modifier = Modifier.fillMaxSize(),
            )
            Box(
              modifier =
                Modifier
                  .fillMaxSize()
                  .padding(horizontal = largePriceBtnW * 0.05f)
                  .padding(bottom = largePriceBtnH * 0.06f),
              contentAlignment = Alignment.Center,
            ) {
              Text(
                text = largePriceText,
                style =
                  largePriceStyle.copy(
                    drawStyle = Stroke(width = 7.0f, join = StrokeJoin.Round),
                  ),
                color = Color(0xFF063810),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                modifier = Modifier.offset(y = 1.2.dp),
              )
              Text(
                text = largePriceText,
                style =
                  largePriceStyle.copy(
                    drawStyle = Stroke(width = 5.5f, join = StrokeJoin.Round),
                  ),
                color = Color(0xFF0A4D18),
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
              )
              Text(
                text = largePriceText,
                style =
                  largePriceStyle.copy(
                    shadow = Shadow(color = Color(0x88063810), offset = Offset(0f, 1.2f), blurRadius = 1.2f),
                  ),
                color = Color.White,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
              )
            }
          }
        }
      }

      Spacer(Modifier.height(20.dp))

      data class ShopCoinTileConfig(
        val drawableRes: Int,
        val descRes: Int,
        val amountRes: Int,
        val priceRes: Int,
        val tag: String,
      )
      val tileRows =
        listOf(
          listOf(
            ShopCoinTileConfig(
              R.drawable.tile_shop_coin_1000,
              R.string.shop_coin_tile_1000_desc,
              R.string.shop_coin_tile_1000_amount,
              R.string.shop_coin_tile_1000_price,
              "shop_coin_tile_1000",
            ),
            ShopCoinTileConfig(
              R.drawable.tile_shop_coin_5000,
              R.string.shop_coin_tile_5000_desc,
              R.string.shop_coin_tile_5000_amount,
              R.string.shop_coin_tile_5000_price,
              "shop_coin_tile_5000",
            ),
            ShopCoinTileConfig(
              R.drawable.tile_shop_coin_10000,
              R.string.shop_coin_tile_10000_desc,
              R.string.shop_coin_tile_10000_amount,
              R.string.shop_coin_tile_10000_price,
              "shop_coin_tile_10000",
            ),
          ),
          listOf(
            ShopCoinTileConfig(
              R.drawable.tile_shop_coin_25000,
              R.string.shop_coin_tile_25000_desc,
              R.string.shop_coin_tile_25000_amount,
              R.string.shop_coin_tile_25000_price,
              "shop_coin_tile_25000",
            ),
            ShopCoinTileConfig(
              R.drawable.tile_shop_coin_50000,
              R.string.shop_coin_tile_50000_desc,
              R.string.shop_coin_tile_50000_amount,
              R.string.shop_coin_tile_50000_price,
              "shop_coin_tile_50000",
            ),
            ShopCoinTileConfig(
              R.drawable.tile_shop_coin_100000,
              R.string.shop_coin_tile_100000_desc,
              R.string.shop_coin_tile_100000_amount,
              R.string.shop_coin_tile_100000_price,
              "shop_coin_tile_100000",
            ),
          ),
        )
      Column(
        modifier =
          Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        tileRows.forEach { rowTiles ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            rowTiles.forEach { tile ->
              val tileInteraction = remember { MutableInteractionSource() }
              val tileBtnInteraction = remember { MutableInteractionSource() }
              val isTileBtnPressed by tileBtnInteraction.collectIsPressedAsState()
              val tileBtnScale by animateFloatAsState(
                targetValue = if (isTileBtnPressed) 0.93f else 1f,
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                label = "${tile.tag}_btn_scale",
              )
              val tileAmountText = stringResource(tile.amountRes)
              val tilePriceText = stringResource(tile.priceRes)
              BoxWithConstraints(
                modifier =
                  Modifier
                    .weight(1f)
                    .aspectRatio(206f / 254f)
                    .clickable(
                      interactionSource = tileInteraction,
                      indication = null,
                      onClick = {},
                    )
                    .testTag(tile.tag),
              ) {
                val tileW = maxWidth
                val tileH = maxHeight

                Image(
                  painter = painterResource(id = tile.drawableRes),
                  contentDescription = stringResource(tile.descRes),
                  contentScale = ContentScale.FillBounds,
                  modifier = Modifier.fillMaxSize(),
                )

                val coinW = tileW * 0.56f
                val coinH = coinW * (188f / 204f)
                Image(
                  painter = painterResource(id = R.drawable.ic_shop_coin_tile_cluster),
                  contentDescription = null,
                  contentScale = ContentScale.Fit,
                  modifier =
                    Modifier
                      .offset(x = (tileW - coinW) * 0.50f, y = tileH * 0.130f)
                      .size(width = coinW, height = coinH),
                )

                val tileAmountStyle =
                  MaterialTheme.typography.titleMedium.copy(
                    fontFamily = LilitaOneFontFamily,
                    fontSize = (tileW.value * 0.165f).coerceIn(13.5f, 22.0f).sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.2.sp,
                  )
                Box(
                  modifier =
                    Modifier
                      .offset(x = 0.dp, y = tileH * 0.555f)
                      .size(width = tileW, height = tileH * 0.160f)
                      .padding(horizontal = tileW * 0.08f)
                      .testTag("${tile.tag}_amount_label"),
                  contentAlignment = Alignment.Center,
                ) {
                  Text(
                    text = tileAmountText,
                    style =
                      tileAmountStyle.copy(
                        drawStyle = Stroke(width = 6.5f, join = StrokeJoin.Round),
                      ),
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                    modifier = Modifier.offset(y = 1.0.dp),
                  )
                  Text(
                    text = tileAmountText,
                    style =
                      tileAmountStyle.copy(
                        drawStyle = Stroke(width = 5.2f, join = StrokeJoin.Round),
                      ),
                    color = Color.Black,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                  )
                  Text(
                    text = tileAmountText,
                    style = tileAmountStyle,
                    color = Color.White,
                    textAlign = TextAlign.Center,
                    maxLines = 1,
                    softWrap = false,
                  )
                }

                val tileBtnW = tileW * 0.785f
                val tileBtnH = tileH * 0.210f
                val tileBtnTouchH = maxOf(tileBtnH, 48.dp)
                val tileBtnBottom = tileH * 0.068f
                val tileBtnCenterY = tileH - tileBtnBottom - tileBtnH * 0.50f
                val tilePriceStyle =
                  MaterialTheme.typography.labelLarge.copy(
                    fontFamily = LilitaOneFontFamily,
                    fontSize = (tileBtnW.value * 0.136f).coerceIn(9.5f, 14.0f).sp,
                    fontWeight = FontWeight.Normal,
                    letterSpacing = 0.sp,
                  )
                Box(
                  modifier =
                    Modifier
                      .offset(
                        x = (tileW - tileBtnW) * 0.50f,
                        y = tileBtnCenterY - tileBtnTouchH * 0.50f,
                      )
                      .size(width = tileBtnW, height = tileBtnTouchH)
                      .scale(tileBtnScale)
                      .clickable(
                        interactionSource = tileBtnInteraction,
                        indication = null,
                        onClick = {},
                      )
                      .testTag("${tile.tag}_price_button"),
                  contentAlignment = Alignment.Center,
                ) {
                  Box(
                    modifier = Modifier.size(width = tileBtnW, height = tileBtnH),
                    contentAlignment = Alignment.Center,
                  ) {
                    Image(
                      painter = painterResource(id = R.drawable.btn_shop_price_green),
                      contentDescription = "${stringResource(tile.descRes)} $tilePriceText",
                      contentScale = ContentScale.FillBounds,
                      modifier = Modifier.fillMaxSize(),
                    )
                    Box(
                      modifier =
                        Modifier
                          .fillMaxSize()
                          .padding(horizontal = tileBtnW * 0.05f)
                          .padding(bottom = tileBtnH * 0.06f),
                      contentAlignment = Alignment.Center,
                    ) {
                      Text(
                        text = tilePriceText,
                        style =
                          tilePriceStyle.copy(
                            drawStyle = Stroke(width = 5.0f, join = StrokeJoin.Round),
                          ),
                        color = Color(0xFF063810),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                        modifier = Modifier.offset(y = 1.0.dp),
                      )
                      Text(
                        text = tilePriceText,
                        style =
                          tilePriceStyle.copy(
                            drawStyle = Stroke(width = 3.8f, join = StrokeJoin.Round),
                          ),
                        color = Color(0xFF0A4D18),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                      )
                      Text(
                        text = tilePriceText,
                        style =
                          tilePriceStyle.copy(
                            shadow =
                              Shadow(
                                color = Color(0x88063810),
                                offset = Offset(0f, 1.0f),
                                blurRadius = 1.0f,
                              ),
                          ),
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        softWrap = false,
                      )
                    }
                  }
                }
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun SettingsDialog(
  soundEnabled: Boolean,
  vibrationEnabled: Boolean,
  cloudSaveEnabled: Boolean,
  languageLabel: String,
  onToggleSound: (Boolean) -> Unit,
  onToggleVibration: (Boolean) -> Unit,
  onToggleCloudSave: (Boolean) -> Unit,
  onCycleLanguage: () -> Unit,
  onOpenSupport: () -> Unit,
  onResetToLevel1: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  SettingsPopupOverlay(
    soundEnabled = soundEnabled,
    vibrationEnabled = vibrationEnabled,
    cloudSaveEnabled = cloudSaveEnabled,
    languageLabel = languageLabel,
    onToggleSound = onToggleSound,
    onToggleVibration = onToggleVibration,
    onToggleCloudSave = onToggleCloudSave,
    onCycleLanguage = onCycleLanguage,
    onOpenSupport = onOpenSupport,
    onDismiss = onDismiss,
    onRestart = onResetToLevel1,
    onHome = null,
    modifier = modifier,
  )
}

@Composable
fun StarterPackDialog(
  alreadyClaimed: Boolean,
  onActivate: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val backdropInteraction = remember { MutableInteractionSource() }
  val cardInteraction = remember { MutableInteractionSource() }
  val cardPopScale = remember { Animatable(0.76f) }

  val infiniteTransition = rememberInfiniteTransition(label = "starter_pack_dialog_anim")
  val raySpin by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
      label = "starter_pack_dialog_rays",
    )
  val pulsePhase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
      label = "starter_pack_dialog_pulse",
    )

  LaunchedEffect(Unit) {
    cardPopScale.animateTo(
      targetValue = 1f,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMedium,
        ),
    )
  }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.74f))
        .clickable(
          interactionSource = backdropInteraction,
          indication = null,
          onClick = onDismiss,
        )
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.displayCutout)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 6.dp, vertical = 10.dp)
        .testTag("starter_pack_dim_backdrop"),
    contentAlignment = Alignment.Center,
  ) {
    val cardAspect = 982f / 1336f
    val targetCardW = minOf(maxWidth * 0.95f, 470.dp, (maxHeight * 0.94f) * cardAspect)
    BoxWithConstraints(
      modifier =
        Modifier
          .width(targetCardW)
          .aspectRatio(cardAspect)
          .scale(cardPopScale.value)
          .clickable(
            interactionSource = cardInteraction,
            indication = null,
            onClick = {},
          )
          .testTag("starter_pack_popup_card"),
    ) {
      val cardW = maxWidth
      val cardH = maxHeight

      // 1. New clean Sky-Blue Starter Pack base card
      Image(
        painter = painterResource(id = R.drawable.popup_starter_pack),
        contentDescription = "Starter Pack",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
      )

      // 2. Behind-the-chest animated glow halo, rotating sunbeam rays & twinkling sparkles
      val chestW = cardW * 0.76f
      val chestH = chestW * (369f / 654f)
      val chestCenterY = cardH * 0.382f
      val breathScale = 1f + 0.025f * sin(pulsePhase * 2f * PI.toFloat())

      // Clipped strictly inside the inner sky-blue upper panel so rays stay behind chest & inside frame
      Box(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .offset(y = cardH * 0.154f)
            .size(width = cardW * 0.85f, height = cardH * 0.44f)
            .clip(RoundedCornerShape(cardW * 0.04f)),
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val cx = size.width * 0.5f
          // Align glow/ray center with the chest center (0.382 - 0.154) / 0.44 ≈ 0.518
          val cy = size.height * 0.518f
          val maxR = size.maxDimension * 0.62f
          val haloAlpha = 0.74f + 0.22f * sin(pulsePhase * 2f * PI.toFloat())

          // Soft radiant golden-white halo behind the treasure chest
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    Color(0xF0FFF8D6).copy(alpha = haloAlpha),
                    Color(0x99FFE082).copy(alpha = haloAlpha * 0.72f),
                    Color(0x33FFFFFF).copy(alpha = haloAlpha * 0.45f),
                    Color.Transparent,
                  ),
                center = Offset(cx, cy),
                radius = maxR * 0.78f,
              ),
            radius = maxR * 0.78f,
            center = Offset(cx, cy),
          )

          // Rotating radial sunbeams emerging from behind the chest
          val rayBrush =
            Brush.radialGradient(
              colors =
                listOf(
                  Color(0xC8FFFFFF),
                  Color(0x66FFF4CC),
                  Color(0x18FFFFFF),
                  Color.Transparent,
                ),
              center = Offset(cx, cy),
              radius = maxR,
            )
          rotate(degrees = raySpin, pivot = Offset(cx, cy)) {
            val rayCount = 12
            for (i in 0 until rayCount) {
              val a0 = (i * (360.0 / rayCount)) * (PI / 180.0)
              val halfSpread = 0.115
              val a1 = a0 + halfSpread
              val a2 = a0 - halfSpread
              val rayPath =
                Path().apply {
                  moveTo(cx, cy)
                  lineTo(cx + cos(a1).toFloat() * maxR, cy + sin(a1).toFloat() * maxR)
                  lineTo(cx + cos(a2).toFloat() * maxR, cy + sin(a2).toFloat() * maxR)
                  close()
                }
              drawPath(rayPath, brush = rayBrush)
            }
          }

          // Twinkling 4-point sparkles around the chest matching reference positions
          val sparkleDefs =
            listOf(
              Triple(0.08f, 0.34f, 0.0f),
              Triple(0.89f, 0.16f, 0.33f),
              Triple(0.92f, 0.62f, 0.67f),
            )
          for ((sxFrac, syFrac, phaseOffset) in sparkleDefs) {
            val twinkle = 0.45f + 0.55f * (0.5f + 0.5f * sin((pulsePhase + phaseOffset) * 2f * PI.toFloat()))
            val sx = size.width * sxFrac
            val sy = size.height * syFrac
            val sr = size.minDimension * 0.045f * (0.75f + 0.35f * twinkle)
            val starPath =
              Path().apply {
                moveTo(sx, sy - sr)
                quadraticTo(sx, sy, sx + sr, sy)
                quadraticTo(sx, sy, sx, sy + sr)
                quadraticTo(sx, sy, sx - sr, sy)
                quadraticTo(sx, sy, sx, sy - sr)
                close()
              }
            drawPath(starPath, color = Color.White.copy(alpha = 0.88f * twinkle))
          }
        }
      }

      // 3. New glossy red treasure chest overflowing with gold coins + bold "x1000" text on chest front
      val starterChestLabel = stringResource(R.string.starter_pack_coins_x1000)
      val starterChestLabelStyle =
        MaterialTheme.typography.headlineSmall.copy(
          fontFamily = LilitaOneFontFamily,
          fontSize = (cardW.value * 0.095f).coerceIn(22f, 38f).sp,
          fontWeight = FontWeight.Normal,
          letterSpacing = 0.4.sp,
        )
      Box(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .offset(y = chestCenterY - chestH * 0.5f)
            .size(width = chestW, height = chestH)
            .scale(breathScale)
            .testTag("starter_pack_chest"),
      ) {
        Image(
          painter = painterResource(id = R.drawable.img_starter_pack_chest_red),
          contentDescription = "1000 Coins Chest",
          contentScale = ContentScale.FillBounds,
          modifier = Modifier.fillMaxSize(),
        )
        val x1000BoxW = chestW * 0.46f
        Box(
          modifier =
            Modifier
              .align(Alignment.TopStart)
              .offset(x = chestW * 0.455f - x1000BoxW * 0.5f, y = chestH * 0.72f)
              .width(x1000BoxW)
              .testTag("starter_pack_coins_label"),
          contentAlignment = Alignment.Center,
        ) {
          Text(
            text = starterChestLabel,
            style =
              starterChestLabelStyle.copy(
                drawStyle = Stroke(width = 11.5f, join = StrokeJoin.Round),
              ),
            color = Color(0xFF1A0E06),
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            modifier = Modifier.offset(y = 1.6.dp),
          )
          Text(
            text = starterChestLabel,
            style =
              starterChestLabelStyle.copy(
                drawStyle = Stroke(width = 9.5f, join = StrokeJoin.Round),
              ),
            color = Color.Black,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
          )
          Text(
            text = starterChestLabel,
            style =
              starterChestLabelStyle.copy(
                shadow = Shadow(color = Color(0x99000000), offset = Offset(0f, 1.5f), blurRadius = 1.5f),
              ),
            color = Color.White,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
          )
        }
      }

      // 4. Top-right close (X) button positioned on upper-right ribbon corner
      val closeBtnSize = cardW * 0.112f
      val closeTouchSize = maxOf(closeBtnSize, 48.dp)
      val closeInteraction = remember { MutableInteractionSource() }
      val closePressed by closeInteraction.collectIsPressedAsState()
      val closeScale by animateFloatAsState(if (closePressed) 0.90f else 1f, spring(), label = "starter_pack_close_scale")
      Box(
        modifier =
          Modifier
            .align(Alignment.TopStart)
            .offset(
              x = cardW * 0.916f - closeTouchSize * 0.5f,
              y = cardH * 0.132f - closeTouchSize * 0.5f,
            )
            .size(closeTouchSize)
            .scale(closeScale)
            .clickable(
              interactionSource = closeInteraction,
              indication = null,
              onClick = onDismiss,
            )
            .testTag("starter_pack_close_button"),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          painter = painterResource(id = R.drawable.btn_settings_close),
          contentDescription = "Close",
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(closeBtnSize),
        )
      }

      // 5. Middle blue rewards box with VIP x2, Bus x2, Refresh x2 arranged in order
      val rewardW = cardW * 0.54f
      val rewardH = cardH * 0.158f
      val qtyTwoLabel = stringResource(R.string.shop_booster_qty_two)
      val starterQtyStyle =
        MaterialTheme.typography.labelLarge.copy(
          fontFamily = LilitaOneFontFamily,
          fontSize = (rewardH.value * 0.25f).coerceIn(13f, 20f).sp,
          fontWeight = FontWeight.Normal,
          letterSpacing = 0.2.sp,
        )
      Box(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .offset(y = cardH * 0.592f)
            .size(width = rewardW, height = rewardH)
            .testTag("starter_pack_rewards_panel"),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          painter = painterResource(id = R.drawable.panel_starter_pack_blue_tray),
          contentDescription = "2 VIP, 2 Bus Clear, 2 Refresh",
          contentScale = ContentScale.FillBounds,
          modifier = Modifier.fillMaxSize(),
        )
        Row(
          modifier =
            Modifier
              .fillMaxSize()
              .padding(horizontal = rewardW * 0.065f, vertical = rewardH * 0.075f),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          val starterDialogBoosters =
            listOf(
              Pair(R.drawable.ic_shop_booster_vip, R.string.shop_booster_vip_desc),
              Pair(R.drawable.ic_shop_booster_bus, R.string.shop_booster_bus_desc),
              Pair(R.drawable.ic_shop_booster_refresh, R.string.shop_booster_refresh_desc),
            )
          starterDialogBoosters.forEach { (iconRes, descRes) ->
            Box(
              modifier = Modifier.weight(1f).fillMaxSize(),
              contentAlignment = Alignment.TopCenter,
            ) {
              Image(
                painter = painterResource(id = iconRes),
                contentDescription = stringResource(descRes),
                contentScale = ContentScale.Fit,
                modifier =
                  Modifier
                    .fillMaxWidth(0.90f)
                    .height(rewardH * 0.62f)
                    .padding(top = rewardH * 0.02f),
              )
              Box(
                modifier =
                  Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = rewardH * 0.01f),
                contentAlignment = Alignment.Center,
              ) {
                Text(
                  text = qtyTwoLabel,
                  style =
                    starterQtyStyle.copy(
                      drawStyle = Stroke(width = 6.0f, join = StrokeJoin.Round),
                    ),
                  color = Color.Black,
                  maxLines = 1,
                  softWrap = false,
                  modifier = Modifier.offset(y = 0.9.dp),
                )
                Text(
                  text = qtyTwoLabel,
                  style =
                    starterQtyStyle.copy(
                      drawStyle = Stroke(width = 4.8f, join = StrokeJoin.Round),
                    ),
                  color = Color.Black,
                  maxLines = 1,
                  softWrap = false,
                )
                Text(
                  text = qtyTwoLabel,
                  style = starterQtyStyle,
                  color = Color.White,
                  maxLines = 1,
                  softWrap = false,
                )
              }
            }
          }
        }
      }

      // 6. Bottom glossy green price button with bold outlined "Rs 550.00" text & red "50% OFF" ribbon
      val priceW = cardW * 0.510f
      val priceH = priceW * (191f / 594f)
      val priceInteraction = remember { MutableInteractionSource() }
      val pricePressed by priceInteraction.collectIsPressedAsState()
      val priceScale by animateFloatAsState(if (pricePressed) 0.93f else 1f, spring(), label = "starter_pack_price_scale")
      val priceLabel =
        stringResource(
          id = if (alreadyClaimed) R.string.shop_claimed_label else R.string.shop_starter_pack_price,
        )
      val priceTextStyle =
        MaterialTheme.typography.titleLarge.copy(
          fontFamily = LilitaOneFontFamily,
          fontSize = (priceH.value * 0.44f).coerceIn(17f, 25f).sp,
          fontWeight = FontWeight.Normal,
          letterSpacing = 0.3.sp,
        )

      Box(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .offset(y = cardH * 0.798f)
            .size(width = priceW, height = maxOf(priceH, 48.dp))
            .scale(priceScale)
            .clickable(
              interactionSource = priceInteraction,
              indication = null,
              onClick = {
                if (!alreadyClaimed) {
                  onActivate()
                } else {
                  onDismiss()
                }
              },
            )
            .testTag("starter_pack_buy_button"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier = Modifier.size(width = priceW, height = priceH),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.btn_starter_pack_price_green),
            contentDescription = if (alreadyClaimed) "Starter Pack Claimed" else "Buy Starter Pack for Rs 550.00",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(horizontal = priceW * 0.06f)
                .padding(bottom = priceH * 0.06f),
            contentAlignment = Alignment.Center,
          ) {
            // Subtle dark-green drop shadow
            Text(
              text = priceLabel,
              style =
                priceTextStyle.copy(
                  drawStyle = Stroke(width = 8.0f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF063810),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.offset(y = 1.4.dp),
            )
            // Crisp dark-green outline
            Text(
              text = priceLabel,
              style =
                priceTextStyle.copy(
                  drawStyle = Stroke(width = 6.5f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF0A4D18),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
            // Bright white foreground text
            Text(
              text = priceLabel,
              style =
                priceTextStyle.copy(
                  shadow = Shadow(color = Color(0x88063810), offset = Offset(0f, 1.2f), blurRadius = 1.2f),
                ),
              color = Color.White,
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
          }
          if (!alreadyClaimed) {
            val badgeW = priceW * 0.56f
            val badgeH = badgeW * (137f / 404f)
            Image(
              painter = painterResource(id = R.drawable.badge_shop_50_off),
              contentDescription = stringResource(R.string.shop_starter_pack_discount_badge),
              contentScale = ContentScale.Fit,
              modifier =
                Modifier
                  .align(Alignment.TopEnd)
                  .offset(x = priceW * 0.035f, y = -badgeH * 0.55f)
                  .size(width = badgeW, height = badgeH)
                  .testTag("starter_pack_discount_badge"),
            )
          }
        }
      }
    }
  }
}

@Composable
fun NoAdsPackDialog(
  alreadyUnlocked: Boolean,
  onActivate: () -> Unit,
  onDismiss: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val backdropInteraction = remember { MutableInteractionSource() }
  val cardInteraction = remember { MutableInteractionSource() }
  val cardPopScale = remember { Animatable(0.76f) }

  val infiniteTransition = rememberInfiniteTransition(label = "no_ads_dialog_anim")
  val raySpin by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 360f,
      animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Restart),
      label = "no_ads_dialog_rays",
    )
  val pulsePhase by
    infiniteTransition.animateFloat(
      initialValue = 0f,
      targetValue = 1f,
      animationSpec = infiniteRepeatable(tween(2400, easing = LinearEasing), RepeatMode.Restart),
      label = "no_ads_dialog_pulse",
    )

  LaunchedEffect(Unit) {
    cardPopScale.animateTo(
      targetValue = 1f,
      animationSpec =
        spring(
          dampingRatio = Spring.DampingRatioMediumBouncy,
          stiffness = Spring.StiffnessMedium,
        ),
    )
  }

  BoxWithConstraints(
    modifier =
      modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.74f))
        .clickable(
          interactionSource = backdropInteraction,
          indication = null,
          onClick = onDismiss,
        )
        .windowInsetsPadding(WindowInsets.statusBars)
        .windowInsetsPadding(WindowInsets.displayCutout)
        .windowInsetsPadding(WindowInsets.navigationBars)
        .padding(horizontal = 6.dp, vertical = 10.dp)
        .testTag("no_ads_dim_backdrop"),
    contentAlignment = Alignment.Center,
  ) {
    val cardAspect = 1024f / 1536f
    val targetCardW = minOf(maxWidth * 0.98f, 480.dp, (maxHeight * 0.96f) * cardAspect)
    BoxWithConstraints(
      modifier =
        Modifier
          .width(targetCardW)
          .aspectRatio(cardAspect)
          .scale(cardPopScale.value)
          .clickable(
            interactionSource = cardInteraction,
            indication = null,
            onClick = {},
          )
          .testTag("no_ads_popup_card"),
    ) {
      val cardW = maxWidth
      val cardH = maxHeight

      // 1. Base green No Ads Pack card with ribbon
      Image(
        painter = painterResource(id = R.drawable.popup_no_ads_pack),
        contentDescription = "No Ads Pack",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
      )

      // 2. Top-right red close (X) button from sprite sheet positioned identically to reference image
      val closeBtnSize = cardW * 0.106f
      val closeTouchSize = maxOf(closeBtnSize, 48.dp)
      val closeInteraction = remember { MutableInteractionSource() }
      val closePressed by closeInteraction.collectIsPressedAsState()
      val closeScale by animateFloatAsState(if (closePressed) 0.90f else 1f, spring(), label = "no_ads_close_scale")
      Box(
        modifier =
          Modifier
            .align(Alignment.TopStart)
            .offset(
              x = cardW * 0.935f - closeTouchSize * 0.5f,
              y = cardH * 0.098f - closeTouchSize * 0.5f,
            )
            .size(closeTouchSize)
            .scale(closeScale)
            .clickable(
              interactionSource = closeInteraction,
              indication = null,
              onClick = onDismiss,
            )
            .testTag("no_ads_close_button"),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          painter = painterResource(id = R.drawable.btn_panel_close_red),
          contentDescription = "Close",
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(closeBtnSize),
        )
      }

      // 3. Upper-center Orange ADS badge with animated rotating sunbeams & glow BEHIND it
      val badgeW = cardW * 0.305f
      val badgeH = badgeW * (340f / 354f)
      val glowBoxSize = cardW * 0.64f
      val badgeCenterY = cardH * 0.324f
      val breathScale = 1f + 0.032f * sin(pulsePhase * 2f * PI.toFloat())

      Box(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .offset(y = badgeCenterY - glowBoxSize * 0.5f)
            .size(glowBoxSize),
        contentAlignment = Alignment.Center,
      ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
          val cx = size.width * 0.5f
          val cy = size.height * 0.5f
          val maxR = size.minDimension * 0.5f
          val haloAlpha = 0.72f + 0.24f * sin(pulsePhase * 2f * PI.toFloat())

          // Soft warm radiant core glow behind the ADS emblem
          drawCircle(
            brush =
              Brush.radialGradient(
                colors =
                  listOf(
                    Color(0xF0FFF8E6).copy(alpha = haloAlpha),
                    Color(0x99FFF5B8).copy(alpha = haloAlpha * 0.75f),
                    Color.Transparent,
                  ),
                center = Offset(cx, cy),
                radius = maxR * 0.85f,
              ),
            radius = maxR * 0.85f,
            center = Offset(cx, cy),
          )

          // Rotating radial sunbeams emerging from behind the ADS emblem
          val rayBrush =
            Brush.radialGradient(
              colors =
                listOf(
                  Color(0xD8FFFFFF),
                  Color(0x70FFF4FF),
                  Color(0x18FFFFFF),
                  Color.Transparent,
                ),
              center = Offset(cx, cy),
              radius = maxR,
            )
          rotate(degrees = raySpin, pivot = Offset(cx, cy)) {
            val rayCount = 12
            for (i in 0 until rayCount) {
              val a0 = (i * (360.0 / rayCount)) * (PI / 180.0)
              val halfSpread = 0.12
              val a1 = a0 + halfSpread
              val a2 = a0 - halfSpread
              val rayPath =
                Path().apply {
                  moveTo(cx, cy)
                  lineTo(cx + cos(a1).toFloat() * maxR, cy + sin(a1).toFloat() * maxR)
                  lineTo(cx + cos(a2).toFloat() * maxR, cy + sin(a2).toFloat() * maxR)
                  close()
                }
              drawPath(rayPath, brush = rayBrush)
            }
          }
        }

        Image(
          painter = painterResource(id = R.drawable.badge_no_ads_orange),
          contentDescription = "No Ads",
          contentScale = ContentScale.Fit,
          modifier =
            Modifier
              .size(width = badgeW, height = badgeH)
              .scale(breathScale),
        )
      }

      // 4. Green VIP reward bar centered between ADS badge and "Remove ads permanently"
      val rewardW = cardW * 0.72f
      val rewardH = rewardW * (533f / 1823f)
      Image(
        painter = painterResource(id = R.drawable.panel_no_ads_rewards),
        contentDescription = "1 VIP, 1 Bus Clear, 1 Refresh, 1000 Coins",
        contentScale = ContentScale.Fit,
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .offset(y = cardH * 0.490f)
            .size(width = rewardW, height = rewardH)
            .testTag("no_ads_rewards_panel"),
      )

      // 4b. Outlined "Remove ads permanently" subtitle centered between rewards panel & price button
      val subtitleText = stringResource(id = R.string.no_ads_remove_permanently)
      val subtitleSize = (cardW.value * 0.049f).coerceIn(14f, 21f).sp
      val subtitleStyle =
        MaterialTheme.typography.titleMedium.copy(
          fontFamily = FredokaFontFamily,
          fontSize = subtitleSize,
          fontWeight = FontWeight.ExtraBold,
          letterSpacing = 0.2.sp,
        )
      Box(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .offset(y = cardH * 0.720f)
            .fillMaxWidth(0.86f),
        contentAlignment = Alignment.Center,
      ) {
        Text(
          text = subtitleText,
          style =
            subtitleStyle.copy(
              drawStyle = Stroke(width = 8f, join = StrokeJoin.Round),
            ),
          color = Color(0xFF0A4D18),
          textAlign = TextAlign.Center,
          maxLines = 1,
        )
        Text(
          text = subtitleText,
          style = subtitleStyle,
          color = Color.White,
          textAlign = TextAlign.Center,
          maxLines = 1,
        )
      }

      // 5. Glossy green button with bold dark-green outlined "Rs 3,350.00" text
      val priceW = cardW * 0.510f
      val priceH = priceW * (191f / 594f)
      val priceInteraction = remember { MutableInteractionSource() }
      val pricePressed by priceInteraction.collectIsPressedAsState()
      val priceScale by animateFloatAsState(if (pricePressed) 0.93f else 1f, spring(), label = "no_ads_price_scale")
      val priceLabel =
        stringResource(
          id = if (alreadyUnlocked) R.string.no_ads_active_label else R.string.no_ads_price_label,
        )
      val priceTextStyle =
        MaterialTheme.typography.titleLarge.copy(
          fontFamily = LilitaOneFontFamily,
          fontSize = (priceH.value * 0.44f).coerceIn(17f, 25f).sp,
          fontWeight = FontWeight.Normal,
          letterSpacing = 0.3.sp,
        )

      Box(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .offset(y = cardH * 0.818f)
            .size(width = priceW, height = maxOf(priceH, 48.dp))
            .scale(priceScale)
            .clickable(
              interactionSource = priceInteraction,
              indication = null,
              onClick = {
                if (!alreadyUnlocked) {
                  onActivate()
                } else {
                  onDismiss()
                }
              },
            )
            .testTag("no_ads_buy_button"),
        contentAlignment = Alignment.Center,
      ) {
        Box(
          modifier = Modifier.size(width = priceW, height = priceH),
          contentAlignment = Alignment.Center,
        ) {
          Image(
            painter = painterResource(id = R.drawable.btn_no_ads_price_green),
            contentDescription = if (alreadyUnlocked) "No Ads Pack Active" else "Buy No Ads Pack for Rs 3,350.00",
            contentScale = ContentScale.FillBounds,
            modifier = Modifier.fillMaxSize(),
          )
          Box(
            modifier =
              Modifier
                .fillMaxSize()
                .padding(horizontal = priceW * 0.06f)
                .padding(bottom = priceH * 0.06f),
            contentAlignment = Alignment.Center,
          ) {
            // Subtle dark green drop shadow
            Text(
              text = priceLabel,
              style =
                priceTextStyle.copy(
                  drawStyle = Stroke(width = 8.0f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF063810),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
              modifier = Modifier.offset(y = 1.4.dp),
            )
            // Crisp dark-green outline
            Text(
              text = priceLabel,
              style =
                priceTextStyle.copy(
                  drawStyle = Stroke(width = 6.5f, join = StrokeJoin.Round),
                ),
              color = Color(0xFF0A4D18),
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
            // Bright white foreground text
            Text(
              text = priceLabel,
              style =
                priceTextStyle.copy(
                  shadow = Shadow(color = Color(0x88063810), offset = Offset(0f, 1.2f), blurRadius = 1.2f),
                ),
              color = Color.White,
              textAlign = TextAlign.Center,
              maxLines = 1,
              softWrap = false,
            )
          }
        }
      }
    }
  }
}
