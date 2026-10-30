package com.example.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.data.model.PlatformItem
import com.example.util.toCoilModel
import com.example.ui.components.SearchBarView
import com.example.ui.theme.AmberAccent
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.IndigoAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.AdminViewModel

@Composable
fun AdminDashboardScreen(
    viewModel: AdminViewModel,
    onBackToUserApp: () -> Unit
) {
    val context = LocalContext.current
    val platforms by viewModel.platforms.collectAsState()
    val stats by viewModel.adminStats.collectAsState()
    val adminEmail by viewModel.adminEmail.collectAsState()
    val isProcessing by viewModel.isProcessing.collectAsState()

    var editingPlatform by remember { mutableStateOf<PlatformItem?>(null) }
    var isCreatingPlatform by remember { mutableStateOf(false) }
    var platformToDelete by remember { mutableStateOf<PlatformItem?>(null) }
    var adminSearchQuery by remember { mutableStateOf("") }

    var selectedSection by remember { mutableIntStateOf(0) } // 0 = Platforms, 1 = Banners
    val banners by viewModel.banners.collectAsState()
    val bannerStats by viewModel.bannerStats.collectAsState()
    var editingBanner by remember { mutableStateOf<BannerItem?>(null) }
    var isCreatingBanner by remember { mutableStateOf(false) }
    var bannerToDelete by remember { mutableStateOf<BannerItem?>(null) }
    var bannerSearchQuery by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        viewModel.messageEvents.collect { msg ->
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val filteredPlatforms = remember(platforms, adminSearchQuery) {
        if (adminSearchQuery.isBlank()) platforms
        else platforms.filter {
            it.name.contains(adminSearchQuery, ignoreCase = true) ||
                    it.category.contains(adminSearchQuery, ignoreCase = true) ||
                    it.websiteUrl.contains(adminSearchQuery, ignoreCase = true)
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("admin_dashboard_scaffold"),
        containerColor = DarkBg,
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (selectedSection == 0) {
                        isCreatingPlatform = true
                    } else {
                        isCreatingBanner = true
                    }
                },
                containerColor = if (selectedSection == 0) CyanAccent else Color(0xFF229ED9),
                contentColor = if (selectedSection == 0) DarkBg else Color.White,
                shape = CircleShape,
                modifier = Modifier.testTag("admin_add_platform_fab")
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (selectedSection == 0) "Add Platform" else "Add Banner"
                )
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
        ) {
            // Admin Header
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = onBackToUserApp,
                            modifier = Modifier.testTag("admin_back_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.ArrowBack,
                                contentDescription = "Back to App",
                                tint = TextPrimary
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Column {
                            Text(
                                text = "Admin Dashboard",
                                style = MaterialTheme.typography.titleLarge.copy(
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 20.sp,
                                    color = TextPrimary
                                )
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.VerifiedUser,
                                    contentDescription = null,
                                    tint = CyanAccent,
                                    modifier = Modifier.size(12.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = adminEmail ?: "Super Administrator",
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        color = TextMuted,
                                        fontSize = 11.sp
                                    )
                                )
                            }
                        }
                    }

                    IconButton(
                        onClick = {
                            viewModel.logout()
                            onBackToUserApp()
                        },
                        modifier = Modifier.testTag("admin_logout_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Logout,
                            contentDescription = "Logout",
                            tint = RoseAccent
                        )
                    }
                }
            }

            // Section Selector: Platforms vs Banner Management
            item {
                Spacer(modifier = Modifier.height(14.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(DarkSurfaceVariant)
                        .padding(4.dp)
                        .testTag("admin_section_tabs")
                ) {
                    val isPlatforms = selectedSection == 0
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isPlatforms) CyanAccent else Color.Transparent)
                            .clickable { selectedSection = 0 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Apps,
                                contentDescription = null,
                                tint = if (isPlatforms) DarkBg else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Platforms (${platforms.size})",
                                color = if (isPlatforms) DarkBg else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }

                    val isBanners = selectedSection == 1
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isBanners) Color(0xFF229ED9) else Color.Transparent)
                            .clickable { selectedSection = 1 }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Campaign,
                                contentDescription = null,
                                tint = if (isBanners) Color.White else TextSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Banners (${banners.size})",
                                color = if (isBanners) Color.White else TextSecondary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }

            if (selectedSection == 0) {
                // Stats Cards Grid
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "CATALOG OVERVIEW",
                        color = CyanAccent,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminStatCard(
                            title = "Total",
                            count = stats.total,
                            icon = Icons.Default.Apps,
                            accentColor = CyanAccent,
                            modifier = Modifier.weight(1f)
                        )
                        AdminStatCard(
                            title = "Active",
                            count = stats.active,
                            icon = Icons.Default.VerifiedUser,
                            accentColor = EmeraldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        AdminStatCard(
                            title = "Upcoming",
                            count = stats.upcoming,
                            icon = Icons.Default.Schedule,
                            accentColor = AmberAccent,
                            modifier = Modifier.weight(1f)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminStatCard(
                            title = "Web Portals",
                            count = stats.withWebsite,
                            icon = Icons.Default.Language,
                            accentColor = IndigoAccent,
                            modifier = Modifier.weight(1f)
                        )
                        AdminStatCard(
                            title = "With APKs",
                            count = stats.withApk,
                            icon = Icons.Default.Download,
                            accentColor = Color(0xFFA855F7),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Initial Seed Action Banner
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Seed 4 Initial Platforms",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "PW Study, PW Marco, AS Multiverse & Parcham",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = { viewModel.seedInitialPlatforms() },
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = CyanAccent,
                                    contentColor = DarkBg
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("admin_seed_platforms_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Seed Data", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Platform List Section Title & Search
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "MANAGE PLATFORMS (${filteredPlatforms.size})",
                            color = CyanAccent,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SearchBarView(
                        query = adminSearchQuery,
                        onQueryChange = { adminSearchQuery = it },
                        placeholder = "Search platforms to edit or configure..."
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Platform Manage Items
                if (filteredPlatforms.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No platforms found. Tap '+' to create one or 'Seed Data'.",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(filteredPlatforms, key = { "admin_${it.id}" }) { platform ->
                        AdminPlatformCard(
                            platform = platform,
                            onEdit = { editingPlatform = platform },
                            onDelete = { platformToDelete = platform },
                            onToggleActive = { viewModel.togglePlatformActive(platform) },
                            onToggleUpcoming = { viewModel.togglePlatformUpcoming(platform) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            } else {
                // Banner Management Section
                item {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "BANNER OVERVIEW",
                        color = Color(0xFF229ED9),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AdminStatCard(
                            title = "Total",
                            count = bannerStats.total,
                            icon = Icons.Default.Campaign,
                            accentColor = Color(0xFF229ED9),
                            modifier = Modifier.weight(1f)
                        )
                        AdminStatCard(
                            title = "Active",
                            count = bannerStats.enabled,
                            icon = Icons.Default.VerifiedUser,
                            accentColor = EmeraldAccent,
                            modifier = Modifier.weight(1f)
                        )
                        AdminStatCard(
                            title = "Telegram",
                            count = bannerStats.telegram,
                            icon = Icons.AutoMirrored.Filled.Send,
                            accentColor = Color(0xFF229ED9),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Sample Seed Banners Action Banner
                item {
                    Spacer(modifier = Modifier.height(14.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
                            .padding(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Seed Sample Banners",
                                    color = TextPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                                Text(
                                    text = "Initial Telegram Community & Test Series banners",
                                    color = TextMuted,
                                    fontSize = 11.sp
                                )
                            }

                            Button(
                                onClick = { viewModel.seedInitialBanners() },
                                enabled = !isProcessing,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFF229ED9),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.testTag("admin_seed_banners_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.CloudDownload,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Seed Banners", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                // Banner Management Header with "+ Add Banner" Button
                item {
                    Spacer(modifier = Modifier.height(18.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = "BANNER MANAGEMENT (${banners.size})",
                            color = Color(0xFF229ED9),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )

                        Button(
                            onClick = { isCreatingBanner = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF229ED9),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(8.dp),
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                            modifier = Modifier.testTag("admin_add_banner_btn")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(15.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add Banner", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    SearchBarView(
                        query = bannerSearchQuery,
                        onQueryChange = { bannerSearchQuery = it },
                        placeholder = "Search banners by title or link..."
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                }

                // Banner List
                val filteredBanners = if (bannerSearchQuery.isBlank()) banners
                else banners.filter {
                    it.title.contains(bannerSearchQuery, ignoreCase = true) ||
                            it.subtitle.contains(bannerSearchQuery, ignoreCase = true) ||
                            it.buttonUrl.contains(bannerSearchQuery, ignoreCase = true)
                }

                if (filteredBanners.isEmpty()) {
                    item {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "No banners configured. Tap '+ Add Banner' or 'Seed Banners'.",
                                color = TextMuted,
                                fontSize = 13.sp
                            )
                        }
                    }
                } else {
                    items(filteredBanners, key = { "banner_${it.id}" }) { banner ->
                        AdminBannerCard(
                            banner = banner,
                            onEdit = { editingBanner = banner },
                            onDelete = { bannerToDelete = banner },
                            onToggleEnabled = { viewModel.toggleBannerEnabled(banner) },
                            onMoveUp = { viewModel.updateBannerOrder(banner, (banner.order - 1).coerceAtLeast(0)) },
                            onMoveDown = { viewModel.updateBannerOrder(banner, banner.order + 1) }
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                    }
                }
            }

            item {
                Spacer(modifier = Modifier.height(80.dp))
            }
        }
    }

    // Add Platform Dialog
    if (isCreatingPlatform) {
        PlatformEditDialog(
            initialPlatform = null,
            isProcessing = isProcessing,
            onDismiss = { isCreatingPlatform = false },
            onSave = { newPlatform ->
                viewModel.savePlatform(newPlatform) {
                    isCreatingPlatform = false
                }
            }
        )
    }

    // Edit Platform Dialog
    if (editingPlatform != null) {
        PlatformEditDialog(
            initialPlatform = editingPlatform,
            isProcessing = isProcessing,
            onDismiss = { editingPlatform = null },
            onSave = { updatedPlatform ->
                viewModel.savePlatform(updatedPlatform) {
                    editingPlatform = null
                }
            }
        )
    }

    // Delete Confirmation Dialog
    if (platformToDelete != null) {
        val platform = platformToDelete!!
        AlertDialog(
            onDismissRequest = { platformToDelete = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Delete Platform?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove '${platform.name}' from GyanSarthi? This will immediately remove it from all users' apps.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePlatform(platform)
                        platformToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent, contentColor = Color.White),
                    modifier = Modifier.testTag("admin_confirm_delete_btn")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { platformToDelete = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }

    // Add Banner Dialog
    if (isCreatingBanner) {
        BannerEditDialog(
            initialBanner = null,
            isProcessing = isProcessing,
            onDismiss = { isCreatingBanner = false },
            onSave = { newBanner ->
                viewModel.saveBanner(newBanner) {
                    isCreatingBanner = false
                }
            }
        )
    }

    // Edit Banner Dialog
    if (editingBanner != null) {
        BannerEditDialog(
            initialBanner = editingBanner,
            isProcessing = isProcessing,
            onDismiss = { editingBanner = null },
            onSave = { updatedBanner ->
                viewModel.saveBanner(updatedBanner) {
                    editingBanner = null
                }
            }
        )
    }

    // Delete Banner Confirmation Dialog
    if (bannerToDelete != null) {
        val banner = bannerToDelete!!
        AlertDialog(
            onDismissRequest = { bannerToDelete = null },
            containerColor = DarkSurface,
            title = {
                Text(
                    text = "Delete Banner?",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to delete '${banner.title}'? It will be removed immediately from the Home screen.",
                    color = TextSecondary,
                    fontSize = 13.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBanner(banner)
                        bannerToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseAccent, contentColor = Color.White),
                    modifier = Modifier.testTag("admin_confirm_delete_banner_btn")
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { bannerToDelete = null }) {
                    Text("Cancel", color = TextMuted)
                }
            }
        )
    }
}

@Composable
fun AdminStatCard(
    title: String,
    count: Int,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(DarkSurface)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
            .padding(10.dp)
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = title,
                    color = TextMuted,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accentColor,
                    modifier = Modifier.size(16.dp)
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                color = TextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
        }
    }
}

@Composable
fun AdminPlatformCard(
    platform: PlatformItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleActive: () -> Unit,
    onToggleUpcoming: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(1.dp, DarkCardBorder, RoundedCornerShape(14.dp))
            .padding(14.dp)
            .testTag("admin_platform_row_${platform.id}")
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(DarkSurfaceVariant)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (platform.logoUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(platform.logoUrl.toCoilModel())
                                    .crossfade(true)
                                    .build(),
                                contentDescription = platform.name,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(8.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Text(
                                text = platform.sortOrder.toString(),
                                color = CyanAccent,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(10.dp))

                    Column {
                        Text(
                            text = platform.name,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = platform.category,
                                color = CyanAccent,
                                fontSize = 11.sp
                            )
                            if (platform.apkUrl.isNotBlank()) {
                                Text(
                                    text = "• Has APK",
                                    color = EmeraldAccent,
                                    fontSize = 11.sp
                                )
                            }
                        }
                    }
                }

                // Edit & Delete actions
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onEdit,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("admin_edit_btn_${platform.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit Platform",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("admin_delete_btn_${platform.id}")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Platform",
                            tint = RoseAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // URL preview
            Text(
                text = "Web: ${platform.websiteUrl.ifBlank { "None" }}",
                color = TextMuted,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Toggles Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Active Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (platform.isActive) "Active" else "Inactive",
                        color = if (platform.isActive) EmeraldAccent else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = platform.isActive,
                        onCheckedChange = { onToggleActive() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBg,
                            checkedTrackColor = EmeraldAccent
                        )
                    )
                }

                // Upcoming Switch
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (platform.isUpcoming) "Upcoming" else "Live",
                        color = if (platform.isUpcoming) AmberAccent else TextMuted,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Switch(
                        checked = platform.isUpcoming,
                        onCheckedChange = { onToggleUpcoming() },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBg,
                            checkedTrackColor = AmberAccent
                        )
                    )
                }
            }
        }
    }
}

@Composable
fun AdminBannerCard(
    banner: BannerItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleEnabled: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    val isTelegram = banner.type.equals("telegram", ignoreCase = true)
    val accentColor = if (isTelegram) Color(0xFF229ED9) else CyanAccent

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(DarkSurface)
            .border(
                1.dp,
                if (banner.enabled) DarkCardBorder else DarkCardBorder.copy(alpha = 0.5f),
                RoundedCornerShape(14.dp)
            )
            .padding(14.dp)
            .testTag("admin_banner_card_${banner.id}")
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    verticalAlignment = Alignment.Top
                ) {
                    // Thumbnail or placeholder
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(DarkBg)
                            .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (banner.imageUrl.isNotBlank()) {
                            AsyncImage(
                                model = ImageRequest.Builder(LocalContext.current)
                                    .data(banner.imageUrl.toCoilModel())
                                    .crossfade(true)
                                    .build(),
                                contentDescription = banner.title,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(RoundedCornerShape(10.dp)),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Icon(
                                imageVector = if (isTelegram) Icons.AutoMirrored.Filled.Send else Icons.Default.Image,
                                contentDescription = null,
                                tint = accentColor,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(accentColor.copy(alpha = 0.15f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isTelegram) "TELEGRAM" else "IMAGE BANNER",
                                    color = accentColor,
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Order: #${banner.order}",
                                color = TextMuted,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = banner.title,
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )

                        if (banner.subtitle.isNotBlank()) {
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = banner.subtitle,
                                color = TextSecondary,
                                fontSize = 12.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        if (banner.buttonUrl.isNotBlank()) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.OpenInNew,
                                    contentDescription = null,
                                    tint = accentColor,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = banner.buttonUrl,
                                    color = accentColor,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }

                // Enabled switch
                Switch(
                    checked = banner.enabled,
                    onCheckedChange = { onToggleEnabled() },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = DarkBg,
                        checkedTrackColor = CyanAccent,
                        uncheckedThumbColor = TextMuted,
                        uncheckedTrackColor = DarkCardBorder
                    ),
                    modifier = Modifier.testTag("admin_banner_switch_${banner.id}")
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Action row: Reordering arrows, Edit, Delete
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(DarkBg)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Order controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Sort:",
                        color = TextMuted,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    IconButton(
                        onClick = onMoveUp,
                        modifier = Modifier.size(28.dp),
                        enabled = banner.order > 0
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Move Up",
                            tint = if (banner.order > 0) CyanAccent else TextMuted.copy(alpha = 0.3f),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    IconButton(
                        onClick = onMoveDown,
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Move Down",
                            tint = CyanAccent,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Edit & Delete Buttons
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onEdit,
                        shape = RoundedCornerShape(6.dp),
                        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        modifier = Modifier.height(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit", color = CyanAccent, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    IconButton(
                        onClick = onDelete,
                        modifier = Modifier.size(30.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Banner",
                            tint = RoseAccent,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
