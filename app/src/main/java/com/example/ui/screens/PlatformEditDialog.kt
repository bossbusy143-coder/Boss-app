package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddBusiness
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.PlatformItem
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.DarkBg
import com.example.ui.theme.DarkCardBorder
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldAccent
import com.example.ui.theme.RoseAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.util.ImageUtils
import com.example.util.toCoilModel

@Composable
fun PlatformEditDialog(
    initialPlatform: PlatformItem? = null,
    isProcessing: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (PlatformItem) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = initialPlatform != null

    var name by remember { mutableStateOf(initialPlatform?.name ?: "") }
    var description by remember { mutableStateOf(initialPlatform?.description ?: "") }
    var category by remember { mutableStateOf(initialPlatform?.category ?: "Education") }
    var logoUrl by remember { mutableStateOf(initialPlatform?.logoUrl ?: "") }
    var bannerUrl by remember { mutableStateOf(initialPlatform?.bannerUrl ?: "") }
    var websiteUrl by remember { mutableStateOf(initialPlatform?.websiteUrl ?: "") }
    var apkUrl by remember { mutableStateOf(initialPlatform?.apkUrl ?: "") }
    var isActive by remember { mutableStateOf(initialPlatform?.isActive ?: true) }
    var isUpcoming by remember { mutableStateOf(initialPlatform?.isUpcoming ?: false) }
    var sortOrder by remember { mutableStateOf((initialPlatform?.sortOrder ?: 0).toString()) }

    var validationError by remember { mutableStateOf<String?>(null) }

    // Direct Gallery Photo Picker for Community Logo
    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64 = ImageUtils.uriToBase64(context, uri, maxDimension = 320, quality = 85)
            if (base64 != null) {
                logoUrl = base64
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        containerColor = DarkSurface,
        modifier = Modifier
            .border(1.dp, DarkCardBorder, RoundedCornerShape(20.dp))
            .testTag("platform_edit_dialog"),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(CyanAccent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.AddBusiness,
                        contentDescription = null,
                        tint = CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Text(
                    text = if (isEditMode) "Edit Platform" else "Add New Platform",
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = RoseAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                // Platform Name
                AdminFormField(
                    value = name,
                    onValueChange = { name = it; validationError = null },
                    label = "Platform Name *",
                    placeholder = "e.g. PW Study, PW Marco",
                    testTag = "edit_platform_name"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Description
                AdminFormField(
                    value = description,
                    onValueChange = { description = it },
                    label = "Description",
                    placeholder = "Brief explanation of courses, batches & features",
                    singleLine = false,
                    maxLines = 3,
                    testTag = "edit_platform_desc"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Category
                AdminFormField(
                    value = category,
                    onValueChange = { category = it },
                    label = "Category *",
                    placeholder = "Education, Study, Courses, Tools, etc.",
                    testTag = "edit_platform_category"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Website URL
                AdminFormField(
                    value = websiteUrl,
                    onValueChange = { websiteUrl = it; validationError = null },
                    label = "Website URL",
                    placeholder = "https://your-platform-domain.com",
                    keyboardType = KeyboardType.Uri,
                    testTag = "edit_platform_website"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // APK URL
                AdminFormField(
                    value = apkUrl,
                    onValueChange = { apkUrl = it },
                    label = "APK Download URL (Optional)",
                    placeholder = "https://example.com/app.apk",
                    keyboardType = KeyboardType.Uri,
                    testTag = "edit_platform_apk"
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Community Logo (Selected directly from device gallery)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Community Logo (Optional)",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (logoUrl.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBg)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                                .padding(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(DarkSurfaceVariant)
                                    .border(1.dp, DarkCardBorder, RoundedCornerShape(10.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(logoUrl.toCoilModel())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Logo Preview",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .clip(RoundedCornerShape(10.dp)),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.width(12.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Logo Attached",
                                        color = EmeraldAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = "Ready to save with platform",
                                    color = TextMuted,
                                    fontSize = 10.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            logoPickerLauncher.launch(
                                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                            )
                                        },
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp),
                                        shape = RoundedCornerShape(6.dp)
                                    ) {
                                        Text("Change", fontSize = 11.sp, color = CyanAccent)
                                    }

                                    TextButton(
                                        onClick = { logoUrl = "" },
                                        modifier = Modifier.height(28.dp),
                                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                                    ) {
                                        Text("Remove", fontSize = 11.sp, color = RoseAccent)
                                    }
                                }
                            }
                        }
                    } else {
                        OutlinedButton(
                            onClick = {
                                logoPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("select_logo_gallery_btn"),
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = CyanAccent),
                            border = ButtonDefaults.outlinedButtonBorder.copy(
                                brush = SolidColor(DarkCardBorder)
                            )
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddPhotoAlternate,
                                contentDescription = null,
                                tint = CyanAccent,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Select Logo from Gallery",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanAccent
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Sort Order
                AdminFormField(
                    value = sortOrder,
                    onValueChange = { sortOrder = it },
                    label = "Display Order (Numeric)",
                    placeholder = "e.g. 1, 2, 3",
                    keyboardType = KeyboardType.Number,
                    testTag = "edit_platform_sort"
                )

                Spacer(modifier = Modifier.height(14.dp))

                // Active toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Active Status",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Platform visible and openable by users",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isActive,
                        onCheckedChange = { isActive = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBg,
                            checkedTrackColor = CyanAccent
                        ),
                        modifier = Modifier.testTag("edit_platform_active_toggle")
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Upcoming toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Upcoming Status",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.sp
                        )
                        Text(
                            text = "Mark as coming soon in Upcoming tab",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isUpcoming,
                        onCheckedChange = { isUpcoming = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBg,
                            checkedTrackColor = CyanAccent
                        ),
                        modifier = Modifier.testTag("edit_platform_upcoming_toggle")
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank()) {
                        validationError = "Platform name cannot be empty."
                        return@Button
                    }
                    if (websiteUrl.isBlank() && apkUrl.isBlank()) {
                        validationError = "Please enter either a Website URL or APK URL."
                        return@Button
                    }

                    val orderNum = sortOrder.toIntOrNull() ?: 0
                    val updated = (initialPlatform ?: PlatformItem()).copy(
                        name = name.trim(),
                        description = description.trim(),
                        category = category.trim().ifBlank { "Education" },
                        websiteUrl = websiteUrl.trim(),
                        apkUrl = apkUrl.trim(),
                        logoUrl = logoUrl.trim(),
                        bannerUrl = bannerUrl.trim(),
                        isActive = isActive,
                        isUpcoming = isUpcoming,
                        sortOrder = orderNum
                    )
                    onSave(updated)
                },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = DarkBg),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.testTag("edit_platform_save_btn")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = DarkBg
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text("Save Platform", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text("Cancel", color = TextMuted)
            }
        }
    )
}

@Composable
fun AdminFormField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    placeholder: String = "",
    singleLine: Boolean = true,
    maxLines: Int = 1,
    keyboardType: KeyboardType = KeyboardType.Text,
    testTag: String = ""
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, fontSize = 12.sp) },
        placeholder = { Text(placeholder, fontSize = 12.sp, color = TextMuted) },
        singleLine = singleLine,
        maxLines = maxLines,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = CyanAccent,
            unfocusedBorderColor = DarkCardBorder,
            focusedTextColor = TextPrimary,
            unfocusedTextColor = TextPrimary,
            cursorColor = CyanAccent
        ),
        modifier = Modifier
            .fillMaxWidth()
            .then(if (testTag.isNotBlank()) Modifier.testTag(testTag) else Modifier)
    )
}
