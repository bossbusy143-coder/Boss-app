package com.example.ui.components

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.BannerItem
import com.example.util.toCoilModel
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

private val TelegramBlue = Color(0xFF229ED9)

@Composable
fun DynamicBannerCarousel(
    banners: List<BannerItem>,
    modifier: Modifier = Modifier,
    onOpenUrl: ((String) -> Unit)? = null
) {
    val context = LocalContext.current

    val openLink: (String) -> Unit = { url ->
        if (onOpenUrl != null) {
            onOpenUrl(url)
        } else if (url.isNotBlank()) {
            try {
                val formattedUrl = if (!url.startsWith("http://") && !url.startsWith("https://") && !url.startsWith("tg://")) {
                    "https://$url"
                } else {
                    url
                }
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(formattedUrl)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(context, "Could not open link: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    if (banners.isEmpty()) {
        // Fallback placeholder when no banners in Firestore
        FallbackLearningGatewayBanner(modifier = modifier)
        return
    }

    val pagerState = rememberPagerState(initialPage = 0) { banners.size }

    // Auto-scroll effect if there are multiple banners
    LaunchedEffect(banners.size, pagerState.currentPage) {
        if (banners.size > 1) {
            while (true) {
                delay(5000)
                if (!pagerState.isScrollInProgress) {
                    val next = (pagerState.currentPage + 1) % banners.size
                    pagerState.animateScrollToPage(next)
                }
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("dynamic_banner_carousel_container")
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("home_banner_pager")
        ) { page ->
            val banner = banners[page]
            BannerCard(
                banner = banner,
                onCardClick = {
                    if (banner.buttonUrl.isNotBlank()) {
                        openLink(banner.buttonUrl)
                    }
                },
                onActionClick = {
                    if (banner.buttonUrl.isNotBlank()) {
                        openLink(banner.buttonUrl)
                    }
                }
            )
        }

        // Only show indicator dots if there are multiple banners
        if (banners.size > 1) {
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("banner_carousel_indicators"),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                banners.indices.forEach { index ->
                    val isSelected = pagerState.currentPage == index
                    val width by animateDpAsState(
                        targetValue = if (isSelected) 18.dp else 6.dp,
                        label = "indicator_width"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(5.dp)
                            .width(width)
                            .clip(CircleShape)
                            .background(
                                if (isSelected) CyanAccent else Color.White.copy(alpha = 0.25f)
                            )
                    )
                }
            }
        }
    }
}

@Composable
fun BannerCard(
    banner: BannerItem,
    onCardClick: () -> Unit,
    onActionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isTelegram = banner.type.equals("telegram", ignoreCase = true)
    val accentColor = if (isTelegram) TelegramBlue else CyanAccent
    val hasButton = banner.buttonText.isNotBlank() || banner.buttonUrl.isNotBlank()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .height(175.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(
                        Color(0xFF1E293B),
                        Color(0xFF0F172A)
                    )
                )
            )
            .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
            .clickable(enabled = banner.buttonUrl.isNotBlank(), onClick = onCardClick)
            .testTag("banner_card_${banner.id}")
    ) {
        // Background banner image with dark gradient tint
        if (banner.imageUrl.isNotBlank()) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(banner.imageUrl.toCoilModel())
                    .crossfade(true)
                    .build(),
                contentDescription = banner.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(RoundedCornerShape(20.dp)),
                contentScale = ContentScale.Crop,
                alpha = 0.38f
            )
        }

        // Gradient overlay to guarantee high-contrast text readability
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color(0x660B1120),
                            Color(0xD90B1120),
                            Color(0xF20B1120)
                        )
                    )
                )
        )

        // Banner Content
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Header Tag Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(accentColor.copy(alpha = 0.18f))
                        .border(1.dp, accentColor.copy(alpha = 0.35f), RoundedCornerShape(6.dp))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (isTelegram) Icons.AutoMirrored.Filled.Send else Icons.Default.Campaign,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(12.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isTelegram) "TELEGRAM COMMUNITY" else "FEATURED UPDATE",
                            color = accentColor,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Title & Subtitle
            Column {
                if (banner.title.isNotBlank()) {
                    Text(
                        text = banner.title,
                        style = MaterialTheme.typography.titleMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            color = TextPrimary,
                            fontSize = 17.sp,
                            lineHeight = 22.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (banner.subtitle.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = banner.subtitle,
                        style = MaterialTheme.typography.bodySmall.copy(
                            color = TextSecondary,
                            fontSize = 12.sp,
                            lineHeight = 16.sp
                        ),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Action Button Row
            if (hasButton) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val defaultText = if (isTelegram) "Join Telegram" else "Learn More"
                    val buttonLabel = banner.buttonText.ifBlank { defaultText }

                    Button(
                        onClick = onActionClick,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentColor,
                            contentColor = if (isTelegram) Color.White else Color(0xFF0F172A)
                        ),
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = 14.dp,
                            vertical = 6.dp
                        ),
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("banner_action_btn_${banner.id}")
                    ) {
                        Icon(
                            imageVector = if (isTelegram) Icons.AutoMirrored.Filled.Send else Icons.Default.OpenInNew,
                            contentDescription = null,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = buttonLabel,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun FallbackLearningGatewayBanner(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .height(160.dp)
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(
                    listOf(Color(0xFF1E293B), Color(0xFF0F172A))
                )
            )
            .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(18.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(CyanAccent.copy(alpha = 0.2f))
                    .padding(horizontal = 8.dp, vertical = 3.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "LIVE CLOUD CATALOG",
                        color = CyanAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Column {
                Text(
                    text = "Unified Learning Gateway",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary,
                        fontSize = 19.sp
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Access premier study portals, live batches and interactive test rooms in one place.",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )
                )
            }
        }
    }
}
