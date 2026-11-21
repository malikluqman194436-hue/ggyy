package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import android.graphics.Paint
import android.graphics.Path as AndroidPath
import android.graphics.Typeface
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import com.example.ui.theme.OutlineInk
import com.example.ui.theme.PlayGreen
import com.example.ui.theme.PlayGreenDark
import com.example.ui.theme.ShopPurple
import com.example.ui.theme.ShopPurpleDark
import kotlin.math.PI
import kotlin.math.cos
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

  Box(
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
    val cardAspect = 991f / 1243f
    BoxWithConstraints(
      modifier =
        Modifier
          .widthIn(max = 365.dp)
          .fillMaxWidth(0.88f)
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
        contentDescription = "Level $levelNumber Perfect",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
      )

      val context = LocalContext.current
      val fredokaTypeface = remember(context) {
        val base = ResourcesCompat.getFont(context, R.font.fredoka)
        if (base != null) Typeface.create(base, Typeface.BOLD) else Typeface.DEFAULT_BOLD
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
            typeface = fredokaTypeface
            isFakeBoldText = true
            textAlign = Paint.Align.CENTER
            letterSpacing = 0.03f
          }

        // Auto-fit text size so longer numbers like "LEVEL 100" still sit cleanly inside the sparkles
        var textSizePx = w * 0.094f
        basePaint.textSize = textSizePx
        val maxRibbonTextW = w * 0.355f
        val measured = basePaint.measureText(label)
        if (measured > maxRibbonTextW && measured > 0f) {
          textSizePx *= (maxRibbonTextW / measured)
          basePaint.textSize = textSizePx
        }

        // Curved path matching the convex upward arch of the blue ribbon
        val arcStartX = w * 0.25f
        val arcEndX = w * 0.75f
        val endBaselineY = h * 0.0985f
        val midBaselineY = h * 0.0865f
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
            strokeWidth = textSizePx * 0.21f
            color = Color(0xFF04225E).toArgb()
          }
        val outlinePaint =
          Paint(basePaint).apply {
            style = Paint.Style.STROKE
            strokeJoin = Paint.Join.ROUND
            strokeCap = Paint.Cap.ROUND
            strokeWidth = textSizePx * 0.185f
            color = Color(0xFF062C78).toArgb()
          }
        val fillPaint =
          Paint(basePaint).apply {
            style = Paint.Style.FILL
            color = android.graphics.Color.WHITE
          }

        drawIntoCanvas { canvas ->
          val native = canvas.nativeCanvas
          // Subtle bottom shadow depth matching original ribbon lettering
          native.drawTextOnPath(label, ribbonArcPath, 0f, h * 0.0028f, shadowPaint)
          native.drawTextOnPath(label, ribbonArcPath, 0f, 0f, outlinePaint)
          native.drawTextOnPath(label, ribbonArcPath, 0f, 0f, fillPaint)
        }
      }

      // Red 'NEXT' button (bottom-left) -> starts next level
      val nextInteraction = remember { MutableInteractionSource() }
      Box(
        modifier =
          Modifier
            .offset(x = cardW * 0.080f, y = cardH * 0.786f)
            .size(width = cardW * 0.410f, height = maxOf(48.dp, cardH * 0.159f))
            .clip(RoundedCornerShape(22.dp))
            .clickable(
              interactionSource = nextInteraction,
              indication = null,
              onClick = onNextLevel,
            )
            .semantics { contentDescription = "Next Level" }
            .testTag("next_level_button"),
      )

      // Green 'Getx20 400' button (bottom-right) -> grants 400 bonus coins & starts next level
      val bonusInteraction = remember { MutableInteractionSource() }
      Box(
        modifier =
          Modifier
            .offset(x = cardW * 0.515f, y = cardH * 0.786f)
            .size(width = cardW * 0.405f, height = maxOf(48.dp, cardH * 0.159f))
            .clip(RoundedCornerShape(22.dp))
            .clickable(
              interactionSource = bonusInteraction,
              indication = null,
              onClick = onClaimBonusAndNext,
            )
            .semantics { contentDescription = "Get 400 Bonus Coins and Next Level" }
            .testTag("win_bonus_button"),
      )
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
        .background(Color.Black.copy(alpha = 0.87f))
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
    val cardAspect = 1079f / 1457f
    val targetCardW = minOf(maxWidth * 0.96f, 440.dp, (maxHeight * 0.95f) * cardAspect)
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
        painter = painterResource(id = R.drawable.popup_settings),
        contentDescription = "Settings",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
      )

      // Top-right glossy red 'X' close button protruding cleanly outward on the ribbon corner
      val closeTouchSize = maxOf(48.dp, cardW * 0.172f)
      val closeVisualSize = cardW * 0.148f
      val closeInteraction = remember { MutableInteractionSource() }
      val closePressed by closeInteraction.collectIsPressedAsState()
      val closeScale by animateFloatAsState(if (closePressed) 0.90f else 1f, spring(), label = "settings_close_scale")

      Box(
        modifier =
          Modifier
            .offset(
              x = cardW * 0.962f - closeTouchSize / 2f,
              y = cardH * 0.126f - closeTouchSize / 2f,
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

      // Top off-white/cream box: Slot 1 = Volume, Slot 2 = Vibrate, Slot 3 = Guide
      val iconBoxW = cardW * 0.180f
      val iconBoxH = maxOf(48.dp, cardH * 0.114f)
      val iconTopY = cardH * 0.172f

      // 1. Volume (ON / OFF muted speaker)
      SettingsSpriteButton(
        drawableRes = if (soundEnabled) R.drawable.btn_settings_sound_on else R.drawable.btn_settings_sound_off,
        contentDescription = if (soundEnabled) "Sound On" else "Sound Off",
        onClick = { onToggleSound(!soundEnabled) },
        testTag = "settings_sound_toggle",
        modifier =
          Modifier
            .offset(x = cardW * 0.165f, y = iconTopY)
            .size(width = iconBoxW, height = iconBoxH),
      )

      // 2. Vibrate (diagonal red slash overlay when toggled OFF)
      Box(
        modifier =
          Modifier
            .offset(x = cardW * 0.410f, y = iconTopY)
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

      // 3. Guide / How to Play icon
      SettingsSpriteButton(
        drawableRes = R.drawable.btn_settings_guide,
        contentDescription = "How to Play",
        onClick = onOpenSupport,
        testTag = "settings_guide_button",
        modifier =
          Modifier
            .offset(x = cardW * 0.655f, y = iconTopY)
            .size(width = iconBoxW, height = iconBoxH),
      )

      // Middle vertical stack: Home (optional on Home screen), Replay, Support, Language
      val stackBtnW = cardW * 0.435f
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
              .offset(x = stackX, y = cardH * 0.320f)
              .size(width = stackBtnW, height = stackBtnH),
        )
      }

      val replayY = if (onHome != null) cardH * 0.443f else cardH * 0.352f
      val supportY = if (onHome != null) cardH * 0.566f else cardH * 0.506f
      val langY = if (onHome != null) cardH * 0.689f else cardH * 0.660f

      SettingsSpriteButton(
        drawableRes = R.drawable.btn_settings_replay,
        contentDescription = if (onHome != null) "Replay Level" else "Replay From Level 1",
        onClick = onRestart,
        testTag = "restart_button",
        modifier =
          Modifier
            .offset(x = stackX, y = replayY)
            .size(width = stackBtnW, height = stackBtnH),
      )

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
      val cloudBtnW = cardW * 0.195f
      val cloudBtnVisualH = cloudBtnW * (131f / 194f)
      val cloudRowH = maxOf(48.dp, cloudBtnVisualH)
      val cloudCenterY = cardH * 0.868f
      Row(
        modifier =
          Modifier
            .offset(x = cardW * 0.135f, y = cloudCenterY - cloudRowH * 0.5f)
            .width(cardW * 0.730f)
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
  coins: Int,
  notice: String?,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit,
) {
  CartoonPopupCard(title = "UNLOCK EXTRA SLOT", headerColor = PlayGreen, onDismiss = onDismiss) {
    Text(
      text = "Open an extra waiting slot permanently for this level to hold more buses at once!",
      style = MaterialTheme.typography.bodyLarge,
      color = OutlineInk,
      textAlign = TextAlign.Center,
    )
    if (notice != null) {
      Spacer(Modifier.height(8.dp))
      Text(text = notice, style = MaterialTheme.typography.labelLarge, color = AlertRed)
    }
    Spacer(Modifier.height(16.dp))
    GlossyButton(
      text = "Unlock • 300 Coins",
      topColor = GoldCoin,
      bottomColor = GoldCoinDark,
      onClick = onConfirm,
      modifier = Modifier.fillMaxWidth(),
      testTag = "confirm_unlock_slot",
    )
    Spacer(Modifier.height(8.dp))
    Text(text = "Your balance: $coins coins", style = MaterialTheme.typography.labelSmall, color = Color(0xFF5A6A80))
  }
}

@Composable
fun ShopDialog(
  coins: Int,
  boardCount: Int,
  refreshCount: Int,
  vipCount: Int,
  notice: String?,
  onBuyBooster: (BoosterType) -> Unit,
  onClaimFreeCoins: () -> Unit,
  onDismiss: () -> Unit,
) {
  CartoonPopupCard(title = "BUS JAM SHOP", headerColor = ShopPurple, onDismiss = onDismiss) {
    Text(
      text = "Coins: $coins",
      style = MaterialTheme.typography.titleMedium,
      color = GoldCoinDark,
    )
    if (notice != null) {
      Spacer(Modifier.height(6.dp))
      Text(text = notice, style = MaterialTheme.typography.labelLarge, color = PlayGreenDark)
    }
    Spacer(Modifier.height(10.dp))
    ShopItemRow(BoosterType.BOARD, boardCount, onBuy = { onBuyBooster(BoosterType.BOARD) })
    Spacer(Modifier.height(8.dp))
    ShopItemRow(BoosterType.REFRESH, refreshCount, onBuy = { onBuyBooster(BoosterType.REFRESH) })
    Spacer(Modifier.height(8.dp))
    ShopItemRow(BoosterType.VIP_SLOT, vipCount, onBuy = { onBuyBooster(BoosterType.VIP_SLOT) })
    Spacer(Modifier.height(14.dp))
    GlossyButton(
      text = "Free Coin Chest (+100)",
      topColor = GoldCoin,
      bottomColor = GoldCoinDark,
      onClick = onClaimFreeCoins,
      modifier = Modifier.fillMaxWidth(),
      testTag = "claim_free_coins",
    )
    Spacer(Modifier.height(8.dp))
    GlossyButton(
      text = "Close",
      topColor = HudBlue,
      bottomColor = HudBlueDark,
      onClick = onDismiss,
      modifier = Modifier.fillMaxWidth(),
    )
  }
}

@Composable
private fun ShopItemRow(
  type: BoosterType,
  owned: Int,
  onBuy: () -> Unit,
) {
  Row(
    modifier =
      Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0xFFF3F7FD))
        .border(1.5.dp, Color(0xFFD0DCEB), RoundedCornerShape(14.dp))
        .padding(10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.SpaceBetween,
  ) {
    Column(modifier = Modifier.weight(1f)) {
      Text(text = "${type.title} ($owned)", style = MaterialTheme.typography.titleMedium.copy(fontSize = 15.sp), color = OutlineInk)
      Text(text = "${type.costCoins} Coins", style = MaterialTheme.typography.labelSmall, color = GoldCoinDark)
    }
    Spacer(Modifier.width(8.dp))
    GlossyButton(
      text = "Buy",
      topColor = PlayGreen,
      bottomColor = PlayGreenDark,
      onClick = onBuy,
    )
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
fun PromoModalDialog(
  title: String,
  description: String,
  buttonText: String,
  alreadyClaimed: Boolean,
  onAction: () -> Unit,
  onDismiss: () -> Unit,
) {
  CartoonPopupCard(title = title, headerColor = ShopPurple, onDismiss = onDismiss) {
    Icon(
      imageVector = Icons.Filled.DirectionsBus,
      contentDescription = null,
      tint = GoldCoinDark,
      modifier = Modifier.size(54.dp),
    )
    Spacer(Modifier.height(10.dp))
    Text(
      text = description,
      style = MaterialTheme.typography.bodyLarge,
      color = OutlineInk,
      textAlign = TextAlign.Center,
    )
    Spacer(Modifier.height(16.dp))
    if (!alreadyClaimed) {
      GlossyButton(
        text = buttonText,
        topColor = PlayGreen,
        bottomColor = PlayGreenDark,
        onClick = onAction,
        modifier = Modifier.fillMaxWidth(),
      )
      Spacer(Modifier.height(10.dp))
    }
    GlossyButton(
      text = "Close",
      topColor = HudBlue,
      bottomColor = HudBlueDark,
      onClick = onDismiss,
      modifier = Modifier.fillMaxWidth(),
    )
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
    val cardAspect = 1046f / 1504f
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

      // 1. Base purple No Ads Pack card with ribbon & "Remove ads permanently"
      Image(
        painter = painterResource(id = R.drawable.popup_no_ads_pack),
        contentDescription = "No Ads Pack",
        contentScale = ContentScale.Fit,
        modifier = Modifier.fillMaxSize(),
      )

      // 2. Top-right green close (X) button positioned on outer right ribbon wing, clear of the star
      val closeBtnSize = cardW * 0.098f
      val closeTouchSize = maxOf(closeBtnSize, 48.dp)
      val closeInteraction = remember { MutableInteractionSource() }
      val closePressed by closeInteraction.collectIsPressedAsState()
      val closeScale by animateFloatAsState(if (closePressed) 0.90f else 1f, spring(), label = "no_ads_close_scale")
      Box(
        modifier =
          Modifier
            .align(Alignment.TopStart)
            .offset(
              x = cardW * 0.946f - closeTouchSize * 0.5f,
              y = cardH * 0.110f - closeTouchSize * 0.5f,
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
          painter = painterResource(id = R.drawable.btn_no_ads_close),
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
                    Color(0x99FFE0FA).copy(alpha = haloAlpha * 0.75f),
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

      // 4. Orange reward panel box with balanced spacing above "Remove ads permanently"
      val rewardW = cardW * 0.67f
      val rewardH = rewardW * (351f / 1017f)
      Image(
        painter = painterResource(id = R.drawable.panel_no_ads_rewards),
        contentDescription = "1 VIP, 1 Bus Clear, 1 Refresh, 1000 Coins",
        contentScale = ContentScale.Fit,
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .offset(y = cardH * 0.482f)
            .size(width = rewardW, height = rewardH)
            .testTag("no_ads_rewards_panel"),
      )

      // 5. Blue "Rs 3,000.00" price button with balanced spacing below "Remove ads permanently"
      val priceW = cardW * 0.49f
      val priceH = priceW * (299f / 1026f)
      val priceInteraction = remember { MutableInteractionSource() }
      val pricePressed by priceInteraction.collectIsPressedAsState()
      val priceScale by animateFloatAsState(if (pricePressed) 0.93f else 1f, spring(), label = "no_ads_price_scale")

      Box(
        modifier =
          Modifier
            .align(Alignment.TopCenter)
            .offset(y = cardH * 0.806f)
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
        Image(
          painter = painterResource(id = R.drawable.btn_no_ads_price),
          contentDescription = if (alreadyUnlocked) "No Ads Pack Active" else "Buy No Ads Pack for Rs 3,000.00",
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(width = priceW, height = priceH),
        )
      }
    }
  }
}
