package com.example.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.game.BoosterType
import com.example.ui.theme.FredokaFontFamily
import com.example.ui.theme.LilitaOneFontFamily

@Composable
fun ShopDialog(
  coins: Int,
  boardCount: Int,
  refreshCount: Int,
  vipCount: Int,
  starterPackClaimed: Boolean = false,
  noAdsUnlocked: Boolean = false,
  notice: String?,
  onBuyBooster: (BoosterType) -> Unit,
  onClaimFreeCoins: () -> Unit,
  onClaimStarterPack: () -> Unit = {},
  onUnlockNoAds: () -> Unit = {},
  onClaimNoAdsPack: () -> Unit = onUnlockNoAds,
  onClaimSmallPack: () -> Unit = onClaimFreeCoins,
  onDismiss: () -> Unit,
) {
  BackHandler(onBack = onDismiss)
  val blockerInteraction = remember { MutableInteractionSource() }

  Box(
    modifier =
      Modifier
        .fillMaxSize()
        .background(
          Brush.verticalGradient(
            colors = listOf(Color(0xFF682BD0), Color(0xFF5620B8), Color(0xFF421495)),
          ),
        )
        .clickable(
          interactionSource = blockerInteraction,
          indication = null,
          onClick = {},
        )
        .testTag("shop_screen_overlay"),
  ) {
    Column(modifier = Modifier.fillMaxSize()) {
      // Top Awning Header with Red Close Button (left) and Coin Pill (right)
      ShopTopHeader(
        coins = coins,
        onDismiss = onDismiss,
      )

      if (notice != null) {
        Box(
          modifier =
            Modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp, vertical = 4.dp),
          contentAlignment = Alignment.Center,
        ) {
          Box(
            modifier =
              Modifier
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xD91A0940))
                .border(1.5.dp, Color(0xFFFFD85A), RoundedCornerShape(14.dp))
                .padding(horizontal = 14.dp, vertical = 6.dp),
          ) {
            Text(
              text = notice,
              style = MaterialTheme.typography.labelLarge.copy(fontFamily = FredokaFontFamily),
              color = Color(0xFFFFF6C7),
              textAlign = TextAlign.Center,
            )
          }
        }
      }

      // Scrollable Glossy Shop Cards matching reference layout
      Column(
        modifier =
          Modifier
            .fillMaxWidth()
            .weight(1f)
            .verticalScroll(rememberScrollState())
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(top = 14.dp, bottom = 26.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp),
      ) {
        Column(
          modifier =
            Modifier
              .fillMaxWidth()
              .widthIn(max = 520.dp)
              .padding(horizontal = 14.dp),
          verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
          // 1. Starter Pack Card (Orange-Gold with overlapping 50% OFF badge)
          ShopStarterPackCard(
            claimed = starterPackClaimed,
            onClaim = onClaimStarterPack,
          )

          // 2. No Ads Card (Purple)
          ShopNoAdsCard(
            active = noAdsUnlocked,
            onUnlock = onUnlockNoAds,
          )

          // 3. No Ads Pack Card (Sky Blue with 4-item bundle)
          ShopNoAdsPackCard(
            active = noAdsUnlocked,
            onClaim = onClaimNoAdsPack,
          )

          // 4. Small Pack Card (Royal Blue with 5000 coins + x1 boosters)
          ShopSmallPackCard(
            onClaim = onClaimSmallPack,
          )

          // 5. Coin Chest & Booster Refills (spend coins / claim free +100)
          ShopBoostersAndFreeCoinsCard(
            boardCount = boardCount,
            refreshCount = refreshCount,
            vipCount = vipCount,
            onBuyBooster = onBuyBooster,
            onClaimFreeCoins = onClaimFreeCoins,
          )
        }
      }
    }
  }
}

@Composable
private fun ShopTopHeader(
  coins: Int,
  onDismiss: () -> Unit,
) {
  val closeInteraction = remember { MutableInteractionSource() }
  val closePressed by closeInteraction.collectIsPressedAsState()
  val closeScale by animateFloatAsState(
    if (closePressed) 0.90f else 1f,
    spring(),
    label = "shop_close_scale",
  )

  Box(
    modifier =
      Modifier
        .fillMaxWidth()
        .windowInsetsPadding(WindowInsets.statusBars),
  ) {
    Image(
      painter = painterResource(id = R.drawable.bg_shop_top_banner),
      contentDescription = stringResource(R.string.shop_top_banner_desc),
      contentScale = ContentScale.FillWidth,
      modifier =
        Modifier
          .align(Alignment.TopCenter)
          .fillMaxWidth()
          .aspectRatio(856f / 131f)
          .testTag("shop_top_banner"),
    )

    Row(
      modifier =
        Modifier
          .align(Alignment.Center)
          .fillMaxWidth()
          .windowInsetsPadding(WindowInsets.displayCutout)
          .padding(horizontal = 12.dp),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Box(
        modifier =
          Modifier
            .size(48.dp)
            .scale(closeScale)
            .clickable(
              interactionSource = closeInteraction,
              indication = null,
              onClick = onDismiss,
            )
            .testTag("shop_close_button"),
        contentAlignment = Alignment.Center,
      ) {
        Image(
          painter = painterResource(id = R.drawable.btn_settings_close),
          contentDescription = stringResource(R.string.shop_close_desc),
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(40.dp),
        )
      }

      // Top-right Coin Pill matching reference
      Box(
        modifier = Modifier.testTag("shop_coin_pill"),
        contentAlignment = Alignment.CenterStart,
      ) {
        Box(
          modifier =
            Modifier
              .padding(start = 18.dp)
              .clip(RoundedCornerShape(50))
              .background(Color(0xBD180E36))
              .border(1.5.dp, Color(0x55FFFFFF), RoundedCornerShape(50))
              .padding(start = 26.dp, end = 16.dp, top = 4.dp, bottom = 4.dp),
          contentAlignment = Alignment.Center,
        ) {
          ShopOutlinedText(
            text = coins.toString(),
            fontSize = 18.sp,
            outlineColor = Color(0xFF0A1433),
            strokeWidth = 5f,
          )
        }
        Image(
          painter = painterResource(id = R.drawable.ic_coin_star),
          contentDescription = null,
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(34.dp),
        )
      }
    }
  }
}

@Composable
private fun ShopStarterPackCard(
  claimed: Boolean,
  onClaim: () -> Unit,
) {
  BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
    val badgeW = minOf(maxWidth * 0.34f, 128.dp)
    val badgeH = badgeW * (137f / 404f)

    ShopGlossyCardFrame(
      gradientColors = listOf(Color(0xFFFFC135), Color(0xFFFCA718), Color(0xFFF08705)),
      innerBorderColor = Color(0xFFFFE58D),
      modifier = Modifier.fillMaxWidth(),
    ) {
      Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp),
      ) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          Box(
            modifier = Modifier.weight(0.47f),
            contentAlignment = Alignment.BottomCenter,
          ) {
            Image(
              painter = painterResource(id = R.drawable.img_starter_pack_chest),
              contentDescription = "1000 Coins Chest",
              contentScale = ContentScale.Fit,
              modifier = Modifier.height(92.dp),
            )
            ShopOutlinedText(
              text = "1000",
              fontSize = 20.sp,
              outlineColor = Color(0xFF1F1004),
              strokeWidth = 6f,
              modifier = Modifier.offset(y = 2.dp),
            )
          }
          Spacer(Modifier.width(8.dp))
          Image(
            painter = painterResource(id = R.drawable.panel_starter_pack_rewards),
            contentDescription = "VIP x2, Bus x2, Refresh x2",
            contentScale = ContentScale.Fit,
            modifier = Modifier.weight(0.53f).height(68.dp),
          )
        }

        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically,
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          ShopRibbonTag(
            title = stringResource(R.string.shop_starter_pack_title),
            topColor = Color(0xFFFF880A),
            bottomColor = Color(0xFFD95600),
            borderColor = Color(0xFF8C3100),
            modifier = Modifier.weight(0.58f),
          )
          Spacer(Modifier.width(10.dp))
          ShopGreenPriceButton(
            label =
              if (claimed) stringResource(R.string.shop_claimed_label)
              else stringResource(R.string.shop_starter_pack_price),
            enabled = !claimed,
            onClick = onClaim,
            testTag = "shop_starter_pack_button",
            modifier = Modifier.weight(0.42f),
          )
        }
      }
    }

    Image(
      painter = painterResource(id = R.drawable.badge_shop_50_off),
      contentDescription = "50% OFF",
      contentScale = ContentScale.Fit,
      modifier =
        Modifier
          .align(Alignment.TopEnd)
          .offset(x = (-2).dp, y = (-12).dp)
          .size(width = badgeW, height = badgeH),
    )
  }
}

@Composable
private fun ShopNoAdsCard(
  active: Boolean,
  onUnlock: () -> Unit,
) {
  ShopGlossyCardFrame(
    gradientColors = listOf(Color(0xFF9668F7), Color(0xFF7D4CE9), Color(0xFF6030CC)),
    innerBorderColor = Color(0xFFCBB1FF),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Image(
          painter = painterResource(id = R.drawable.badge_no_ads_orange),
          contentDescription = "No Ads",
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(60.dp),
        )
        Spacer(Modifier.width(8.dp))
        ShopRibbonTag(
          title = stringResource(R.string.shop_no_ads_title),
          topColor = Color(0xFFB36CFF),
          bottomColor = Color(0xFF7B31D6),
          borderColor = Color(0xFF471696),
          modifier = Modifier.weight(0.56f),
        )
        Spacer(Modifier.width(8.dp))
        ShopGreenPriceButton(
          label =
            if (active) stringResource(R.string.no_ads_active_label)
            else stringResource(R.string.shop_no_ads_price),
          enabled = !active,
          onClick = onUnlock,
          testTag = "shop_no_ads_button",
          modifier = Modifier.weight(0.44f),
        )
      }
      Text(
        text = stringResource(R.string.shop_rewarded_ads_note),
        style =
          MaterialTheme.typography.labelSmall.copy(
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.4.sp,
          ),
        color = Color(0xFFF3EBFF),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun ShopNoAdsPackCard(
  active: Boolean,
  onClaim: () -> Unit,
) {
  ShopGlossyCardFrame(
    gradientColors = listOf(Color(0xFF56AEFF), Color(0xFF3992F5), Color(0xFF1F74DD)),
    innerBorderColor = Color(0xFFA7D7FF),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Image(
          painter = painterResource(id = R.drawable.badge_no_ads_orange),
          contentDescription = "No Ads Pack",
          contentScale = ContentScale.Fit,
          modifier = Modifier.size(70.dp),
        )
        Spacer(Modifier.width(8.dp))
        Image(
          painter = painterResource(id = R.drawable.panel_no_ads_rewards),
          contentDescription = "VIP x1, Bus x1, Refresh x1, Coins x1000",
          contentScale = ContentScale.Fit,
          modifier = Modifier.weight(1f).height(62.dp),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        ShopRibbonTag(
          title = stringResource(R.string.shop_no_ads_pack_title),
          topColor = Color(0xFF289AFF),
          bottomColor = Color(0xFF0A66D6),
          borderColor = Color(0xFF063A8A),
          modifier = Modifier.weight(0.58f),
        )
        Spacer(Modifier.width(10.dp))
        ShopGreenPriceButton(
          label =
            if (active) stringResource(R.string.no_ads_active_label)
            else stringResource(R.string.shop_no_ads_pack_price),
          enabled = !active,
          onClick = onClaim,
          testTag = "shop_no_ads_pack_button",
          modifier = Modifier.weight(0.42f),
        )
      }

      Text(
        text = stringResource(R.string.shop_rewarded_ads_note),
        style =
          MaterialTheme.typography.labelSmall.copy(
            fontFamily = FredokaFontFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            letterSpacing = 0.4.sp,
          ),
        color = Color(0xFFEAF5FF),
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth(),
      )
    }
  }
}

@Composable
private fun ShopSmallPackCard(
  onClaim: () -> Unit,
) {
  ShopGlossyCardFrame(
    gradientColors = listOf(Color(0xFF4398FC), Color(0xFF2B7CEB), Color(0xFF195FD0)),
    innerBorderColor = Color(0xFF9ACBFF),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        Box(
          modifier = Modifier.weight(0.46f),
          contentAlignment = Alignment.BottomCenter,
        ) {
          Image(
            painter = painterResource(id = R.drawable.img_win_coins_20),
            contentDescription = "5000 Coins",
            contentScale = ContentScale.Fit,
            modifier = Modifier.height(78.dp),
          )
          ShopOutlinedText(
            text = "5000",
            fontSize = 20.sp,
            outlineColor = Color(0xFF1F1004),
            strokeWidth = 6f,
          )
        }
        Spacer(Modifier.width(8.dp))
        Row(
          modifier =
            Modifier
              .weight(0.54f)
              .clip(RoundedCornerShape(14.dp))
              .background(Color(0x4D0D357D))
              .border(1.5.dp, Color(0xFF9FD0FF), RoundedCornerShape(14.dp))
              .padding(horizontal = 8.dp, vertical = 6.dp),
          horizontalArrangement = Arrangement.SpaceEvenly,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          SmallPackBoosterIcon(R.drawable.btn_booster_vip, "VIP x1")
          SmallPackBoosterIcon(R.drawable.btn_booster_board, "Bus x1")
          SmallPackBoosterIcon(R.drawable.btn_booster_refresh, "Refresh x1")
        }
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        ShopRibbonTag(
          title = stringResource(R.string.shop_small_pack_title),
          topColor = Color(0xFF289AFF),
          bottomColor = Color(0xFF0A66D6),
          borderColor = Color(0xFF063A8A),
          modifier = Modifier.weight(0.58f),
        )
        Spacer(Modifier.width(10.dp))
        ShopGreenPriceButton(
          label = stringResource(R.string.shop_small_pack_price),
          enabled = true,
          onClick = onClaim,
          testTag = "shop_small_pack_button",
          modifier = Modifier.weight(0.42f),
        )
      }
    }
  }
}

@Composable
private fun SmallPackBoosterIcon(drawableRes: Int, label: String) {
  Column(horizontalAlignment = Alignment.CenterHorizontally) {
    Image(
      painter = painterResource(id = drawableRes),
      contentDescription = label,
      contentScale = ContentScale.Fit,
      modifier = Modifier.size(38.dp),
    )
    ShopOutlinedText(
      text = "x1",
      fontSize = 13.sp,
      outlineColor = Color(0xFF062666),
      strokeWidth = 4f,
    )
  }
}

@Composable
private fun ShopBoostersAndFreeCoinsCard(
  boardCount: Int,
  refreshCount: Int,
  vipCount: Int,
  onBuyBooster: (BoosterType) -> Unit,
  onClaimFreeCoins: () -> Unit,
) {
  ShopGlossyCardFrame(
    gradientColors = listOf(Color(0xFF2BA8EA), Color(0xFF1E84D4), Color(0xFF1564B5)),
    innerBorderColor = Color(0xFF8CE0FF),
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(
      modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(10.dp),
    ) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        ShopBoosterTile(
          drawableRes = R.drawable.btn_booster_board,
          title = "Board",
          owned = boardCount,
          costCoins = BoosterType.BOARD.costCoins,
          onClick = { onBuyBooster(BoosterType.BOARD) },
          modifier = Modifier.weight(1f),
        )
        ShopBoosterTile(
          drawableRes = R.drawable.btn_booster_refresh,
          title = "Refresh",
          owned = refreshCount,
          costCoins = BoosterType.REFRESH.costCoins,
          onClick = { onBuyBooster(BoosterType.REFRESH) },
          modifier = Modifier.weight(1f),
        )
        ShopBoosterTile(
          drawableRes = R.drawable.btn_booster_vip,
          title = "VIP",
          owned = vipCount,
          costCoins = BoosterType.VIP_SLOT.costCoins,
          onClick = { onBuyBooster(BoosterType.VIP_SLOT) },
          modifier = Modifier.weight(1f),
        )
      }

      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
      ) {
        ShopRibbonTag(
          title = stringResource(R.string.shop_free_coins_title),
          topColor = Color(0xFFFFB21E),
          bottomColor = Color(0xFFE07D00),
          borderColor = Color(0xFF854400),
          modifier = Modifier.weight(0.58f),
        )
        Spacer(Modifier.width(10.dp))
        ShopGreenPriceButton(
          label = stringResource(R.string.shop_free_coins_btn),
          enabled = true,
          onClick = onClaimFreeCoins,
          testTag = "claim_free_coins",
          modifier = Modifier.weight(0.42f),
        )
      }
    }
  }
}

@Composable
private fun ShopBoosterTile(
  drawableRes: Int,
  title: String,
  owned: Int,
  costCoins: Int,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by animateFloatAsState(if (pressed) 0.93f else 1f, spring(), label = "booster_tile_$title")

  Column(
    modifier =
      modifier
        .scale(scale)
        .clip(RoundedCornerShape(14.dp))
        .background(Color(0x470B2A68))
        .border(1.5.dp, Color(0x88FFFFFF), RoundedCornerShape(14.dp))
        .clickable(interactionSource = interaction, indication = null, onClick = onClick)
        .padding(vertical = 8.dp, horizontal = 4.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    Image(
      painter = painterResource(id = drawableRes),
      contentDescription = "$title ($owned owned)",
      contentScale = ContentScale.Fit,
      modifier = Modifier.size(42.dp),
    )
    Text(
      text = "$title ($owned)",
      style = MaterialTheme.typography.labelSmall.copy(fontFamily = FredokaFontFamily, fontWeight = FontWeight.Bold),
      color = Color.White,
      maxLines = 1,
    )
    Row(verticalAlignment = Alignment.CenterVertically) {
      Image(
        painter = painterResource(id = R.drawable.ic_coin_star),
        contentDescription = null,
        modifier = Modifier.size(14.dp),
      )
      Spacer(Modifier.width(3.dp))
      Text(
        text = "$costCoins",
        style = MaterialTheme.typography.labelSmall.copy(fontFamily = LilitaOneFontFamily),
        color = Color(0xFFFFE570),
      )
    }
  }
}

@Composable
private fun ShopGlossyCardFrame(
  gradientColors: List<Color>,
  innerBorderColor: Color,
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.() -> Unit,
) {
  Box(
    modifier =
      modifier
        .clip(RoundedCornerShape(24.dp))
        .background(Color(0xFFE7C992))
        .padding(bottom = 3.dp)
        .clip(RoundedCornerShape(23.dp))
        .background(Color(0xFFFFF5DE))
        .padding(3.5.dp)
        .clip(RoundedCornerShape(20.dp))
        .background(Brush.verticalGradient(gradientColors))
        .border(2.dp, innerBorderColor.copy(alpha = 0.72f), RoundedCornerShape(20.dp)),
  ) {
    Canvas(modifier = Modifier.matchParentSize()) {
      val stripeW = size.width * 0.16f
      val slant = size.height * 0.45f
      for (i in -1..5 step 2) {
        val sx = i * stripeW
        val path =
          Path().apply {
            moveTo(sx, 0f)
            lineTo(sx + stripeW * 0.75f, 0f)
            lineTo(sx + stripeW * 0.75f - slant, size.height)
            lineTo(sx - slant, size.height)
            close()
          }
        drawPath(path, Color.White.copy(alpha = 0.08f))
      }
      // Subtle top gloss sheen
      drawCircle(
        color = Color.White.copy(alpha = 0.16f),
        radius = size.width * 0.42f,
        center = Offset(size.width * 0.32f, -size.height * 0.10f),
      )
    }
    content()
  }
}

@Composable
private fun ShopRibbonTag(
  title: String,
  topColor: Color,
  bottomColor: Color,
  borderColor: Color,
  modifier: Modifier = Modifier,
) {
  Box(
    modifier =
      modifier
        .height(40.dp)
        .clip(RoundedCornerShape(12.dp))
        .background(borderColor)
        .padding(bottom = 3.dp)
        .clip(RoundedCornerShape(11.dp))
        .background(Brush.verticalGradient(listOf(topColor, bottomColor)))
        .border(1.5.dp, Color(0x88FFFFFF), RoundedCornerShape(11.dp))
        .padding(horizontal = 8.dp),
    contentAlignment = Alignment.Center,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.SpaceBetween,
    ) {
      Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = null,
        tint = Color(0xFFFFD84D).copy(alpha = 0.55f),
        modifier = Modifier.size(14.dp),
      )
      ShopOutlinedText(
        text = title,
        fontSize = 15.sp,
        outlineColor = borderColor,
        strokeWidth = 4.5f,
      )
      Icon(
        imageVector = Icons.Filled.Star,
        contentDescription = null,
        tint = Color(0xFFFFD84D).copy(alpha = 0.55f),
        modifier = Modifier.size(14.dp),
      )
    }
  }
}

@Composable
private fun ShopGreenPriceButton(
  label: String,
  enabled: Boolean,
  onClick: () -> Unit,
  testTag: String,
  modifier: Modifier = Modifier,
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val scale by animateFloatAsState(
    if (pressed && enabled) 0.92f else 1f,
    spring(),
    label = "${testTag}_scale",
  )

  Box(
    modifier =
      modifier
        .height(48.dp)
        .scale(scale)
        .clickable(
          interactionSource = interaction,
          indication = null,
          enabled = enabled,
          onClick = onClick,
        )
        .testTag(testTag),
    contentAlignment = Alignment.Center,
  ) {
    Image(
      painter = painterResource(id = R.drawable.btn_home_level_blank),
      contentDescription = label,
      contentScale = ContentScale.FillBounds,
      modifier = Modifier.fillMaxSize(),
    )
    ShopOutlinedText(
      text = label,
      fontSize = 16.sp,
      outlineColor = Color(0xFF0A4705),
      strokeWidth = 5f,
    )
  }
}

@Composable
private fun ShopOutlinedText(
  text: String,
  fontSize: TextUnit,
  outlineColor: Color,
  strokeWidth: Float,
  modifier: Modifier = Modifier,
) {
  val baseStyle =
    MaterialTheme.typography.titleMedium.copy(
      fontFamily = LilitaOneFontFamily,
      fontSize = fontSize,
      fontWeight = FontWeight.Normal,
    )
  Box(modifier = modifier, contentAlignment = Alignment.Center) {
    Text(
      text = text,
      style =
        baseStyle.copy(
          drawStyle = Stroke(width = strokeWidth, join = StrokeJoin.Round),
        ),
      color = outlineColor,
      maxLines = 1,
    )
    Text(
      text = text,
      style =
        baseStyle.copy(
          shadow = Shadow(color = outlineColor.copy(alpha = 0.6f), offset = Offset(0f, 2f), blurRadius = 2f),
        ),
      color = Color.White,
      maxLines = 1,
    )
  }
}
