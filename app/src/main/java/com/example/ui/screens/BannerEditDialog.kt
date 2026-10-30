package com.example.ui.screens

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Image
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
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
import com.example.data.model.BannerItem
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

private val TelegramBlue = Color(0xFF229ED9)

@Composable
fun BannerEditDialog(
    initialBanner: BannerItem? = null,
    isProcessing: Boolean = false,
    onDismiss: () -> Unit,
    onSave: (BannerItem) -> Unit
) {
    val context = LocalContext.current
    val isEditMode = initialBanner != null

    var type by remember { mutableStateOf(initialBanner?.type?.lowercase() ?: "image") }
    var title by remember { mutableStateOf(initialBanner?.title ?: "") }
    var subtitle by remember { mutableStateOf(initialBanner?.subtitle ?: "") }
    var imageUrl by remember { mutableStateOf(initialBanner?.imageUrl ?: "") }
    var buttonText by remember { mutableStateOf(initialBanner?.buttonText ?: "") }
    var buttonUrl by remember { mutableStateOf(initialBanner?.buttonUrl ?: "") }
    var enabled by remember { mutableStateOf(initialBanner?.enabled ?: true) }
    var orderText by remember { mutableStateOf((initialBanner?.order ?: 0).toString()) }

    var validationError by remember { mutableStateOf<String?>(null) }

    // Direct Gallery Photo Picker for Banner Image
    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri: Uri? ->
        if (uri != null) {
            val base64 = ImageUtils.uriToBase64(context, uri, maxDimension = 800, quality = 75)
            if (base64 != null) {
                imageUrl = base64
                validationError = null
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isProcessing) onDismiss() },
        modifier = Modifier
            .fillMaxWidth()
            .testTag("banner_edit_dialog"),
        containerColor = DarkSurface,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (isEditMode) Icons.Default.Edit else Icons.Default.AddCircle,
                    contentDescription = null,
                    tint = if (type == "telegram") TelegramBlue else CyanAccent,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEditMode) "Edit Banner" else "Add New Banner",
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
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Banner Type Selector
                Text(
                    text = "BANNER TYPE",
                    color = TextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Image Banner Type Option
                    val isImageSelected = type == "image"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isImageSelected) CyanAccent.copy(alpha = 0.15f) else DarkBg)
                            .border(
                                1.5.dp,
                                if (isImageSelected) CyanAccent else DarkCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                type = "image"
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Image,
                                contentDescription = null,
                                tint = if (isImageSelected) CyanAccent else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Image Banner",
                                color = if (isImageSelected) CyanAccent else TextSecondary,
                                fontWeight = if (isImageSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }

                    // Telegram Banner Type Option
                    val isTelegramSelected = type == "telegram"
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isTelegramSelected) TelegramBlue.copy(alpha = 0.15f) else DarkBg)
                            .border(
                                1.5.dp,
                                if (isTelegramSelected) TelegramBlue else DarkCardBorder,
                                RoundedCornerShape(10.dp)
                            )
                            .clickable {
                                type = "telegram"
                                if (buttonText.isBlank()) {
                                    buttonText = "Join Telegram"
                                }
                            }
                            .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = null,
                                tint = if (isTelegramSelected) TelegramBlue else TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Telegram",
                                color = if (isTelegramSelected) TelegramBlue else TextSecondary,
                                fontWeight = if (isTelegramSelected) FontWeight.Bold else FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = {
                        title = it
                        validationError = null
                    },
                    label = { Text("Banner Title *") },
                    placeholder = { Text(if (type == "telegram") "e.g. Join Official Telegram" else "e.g. New Batch Announcement") },
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_title_input")
                )

                // Subtitle
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text("Subtitle / Description") },
                    placeholder = { Text("Short description or call-to-action details") },
                    maxLines = 3,
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_subtitle_input")
                )

                // Banner Image (Selected directly from device gallery)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "Banner Image (Optional)",
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    if (imageUrl.isNotBlank()) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBg)
                                .border(1.dp, DarkCardBorder, RoundedCornerShape(12.dp))
                                .padding(10.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(110.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(DarkSurfaceVariant),
                                contentAlignment = Alignment.Center
                            ) {
                                AsyncImage(
                                    model = ImageRequest.Builder(context)
                                        .data(imageUrl.toCoilModel())
                                        .crossfade(true)
                                        .build(),
                                    contentDescription = "Banner Image Preview",
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = Icons.Default.CheckCircle,
                                        contentDescription = null,
                                        tint = EmeraldAccent,
                                        modifier = Modifier.size(13.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Image Selected",
                                        color = EmeraldAccent,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                    OutlinedButton(
                                        onClick = {
                                            bannerPickerLauncher.launch(
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
                                        onClick = { imageUrl = "" },
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
                                bannerPickerLauncher.launch(
                                    PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)
                                )
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(48.dp)
                                .testTag("banner_pick_image_btn"),
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
                                text = "Select Banner Image from Gallery",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = CyanAccent
                            )
                        }
                    }
                }

                // Button Text
                OutlinedTextField(
                    value = buttonText,
                    onValueChange = { buttonText = it },
                    label = { Text("Button Text (Optional)") },
                    placeholder = { Text(if (type == "telegram") "Join Telegram" else "Explore Now") },
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_button_text_input")
                )

                // Button / Target URL
                OutlinedTextField(
                    value = buttonUrl,
                    onValueChange = { buttonUrl = it },
                    label = { Text("Button URL / Target Link") },
                    placeholder = { Text(if (type == "telegram") "https://t.me/your_channel" else "https://example.com/target") },
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_button_url_input")
                )

                // Display Order
                OutlinedTextField(
                    value = orderText,
                    onValueChange = {
                        if (it.isEmpty() || it.all { char -> char.isDigit() }) {
                            orderText = it
                        }
                    },
                    label = { Text("Display Order") },
                    placeholder = { Text("0, 1, 2...") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("banner_order_input")
                )

                // Enabled Toggle Switch
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(DarkBg)
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text(
                            text = "Enable Banner",
                            color = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp
                        )
                        Text(
                            text = if (enabled) "Visible to all users on Home" else "Hidden from users",
                            color = TextMuted,
                            fontSize = 11.sp
                        )
                    }

                    Switch(
                        checked = enabled,
                        onCheckedChange = { enabled = it },
                        colors = SwitchDefaults.colors(
                            checkedThumbColor = DarkBg,
                            checkedTrackColor = CyanAccent,
                            uncheckedThumbColor = TextMuted,
                            uncheckedTrackColor = DarkCardBorder
                        ),
                        modifier = Modifier.testTag("banner_enabled_switch")
                    )
                }

                // Validation Error
                if (validationError != null) {
                    Text(
                        text = validationError ?: "",
                        color = RoseAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isBlank()) {
                        validationError = "Banner title cannot be empty."
                        return@Button
                    }
                    if (type == "telegram" && buttonUrl.isBlank()) {
                        validationError = "Please specify a Telegram URL (e.g., https://t.me/...)."
                        return@Button
                    }

                    val finalOrder = orderText.toIntOrNull() ?: 0
                    val finalButtonText = if (type == "telegram" && buttonText.isBlank()) {
                        "Join Telegram"
                    } else {
                        buttonText.trim()
                    }

                    val updatedBanner = BannerItem(
                        id = initialBanner?.id ?: "",
                        title = title.trim(),
                        subtitle = subtitle.trim(),
                        imageUrl = imageUrl.trim(),
                        buttonText = finalButtonText,
                        buttonUrl = buttonUrl.trim(),
                        type = type.lowercase(),
                        enabled = enabled,
                        order = finalOrder,
                        createdAt = initialBanner?.createdAt
                    )
                    onSave(updatedBanner)
                },
                enabled = !isProcessing,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (type == "telegram") TelegramBlue else CyanAccent,
                    contentColor = if (type == "telegram") Color.White else DarkBg
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("banner_save_button")
            ) {
                if (isProcessing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        strokeWidth = 2.dp,
                        color = DarkBg
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = if (isEditMode) "Save Changes" else "Create Banner",
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isProcessing
            ) {
                Text("Cancel", color = TextSecondary)
            }
        }
    )
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = CyanAccent,
    unfocusedBorderColor = DarkCardBorder,
    focusedLabelColor = CyanAccent,
    unfocusedLabelColor = TextSecondary,
    focusedTextColor = TextPrimary,
    unfocusedTextColor = TextPrimary,
    cursorColor = CyanAccent
)
