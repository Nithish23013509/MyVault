package com.example.ui.components

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultItem
import com.example.data.VaultItemType
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IndigoLink
import com.example.ui.theme.PurpleDoc
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.VaultBgDark
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurfaceDark
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import kotlinx.coroutines.delay
import org.json.JSONArray
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemDetailDialog(
    item: VaultItem,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleFavorite: () -> Unit,
    onTogglePin: () -> Unit,
    onCopy: (label: String, value: String) -> Unit,
    onVerifyRevealAuth: ((String) -> Boolean)? = null,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var isRevealed by remember { mutableStateOf(!item.sensitive && item.type != VaultItemType.PASSWORD) }
    var revealSecondsLeft by remember { mutableIntStateOf(0) }
    var showAuthPrompt by remember { mutableStateOf(false) }
    var authPinInput by remember { mutableStateOf("") }
    var authError by remember { mutableStateOf<String?>(null) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showOverflowMenu by remember { mutableStateOf(false) }

    // Auto-mask sensitive values after 30 seconds
    LaunchedEffect(isRevealed) {
        if (isRevealed && (item.sensitive || item.type == VaultItemType.PASSWORD)) {
            revealSecondsLeft = 30
            while (revealSecondsLeft > 0) {
                delay(1000)
                revealSecondsLeft--
            }
            isRevealed = false
        }
    }

    val createdDateStr = remember(item.createdAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        sdf.format(Date(item.createdAt))
    }

    val updatedDateStr = remember(item.updatedAt) {
        val sdf = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        sdf.format(Date(item.updatedAt))
    }

    // Masked value string
    val rawValue = item.effectiveValue
    val maskedValue = remember(rawValue) {
        if (rawValue.length > 4) "••••••••" + rawValue.takeLast(4) else "••••••••"
    }

    val collectionTagLine = remember(item) {
        val col = item.collectionId?.replaceFirstChar { it.uppercase() } ?: item.categoryTag
        val tags = item.tags.ifBlank { "General" }
        "$col · $tags"
    }

    // Auth prompt dialog for revealing sensitive data
    if (showAuthPrompt) {
        AlertDialog(
            onDismissRequest = {
                showAuthPrompt = false
                authPinInput = ""
                authError = null
            },
            containerColor = VaultSurfaceDark,
            title = { Text("Authenticate to Reveal", color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Enter your Master PIN to view protected value.", color = VaultTextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = authPinInput,
                        onValueChange = {
                            authPinInput = it
                            authError = null
                        },
                        label = { Text("Master PIN") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = VaultBorder,
                            focusedTextColor = VaultTextPrimary,
                            unfocusedTextColor = VaultTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    authError?.let {
                        Text(it, color = RoseDanger, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val verified = onVerifyRevealAuth?.invoke(authPinInput) ?: (authPinInput.length >= 4)
                        if (verified) {
                            isRevealed = true
                            showAuthPrompt = false
                            authPinInput = ""
                            authError = null
                        } else {
                            authError = "Incorrect PIN. Please try again."
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Verify & Reveal", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAuthPrompt = false }) {
                    Text("Cancel", color = VaultTextSecondary)
                }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            containerColor = VaultSurfaceDark,
            title = { Text("Delete Record?", color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
            text = { Text("Are you sure you want to permanently delete \"${item.title}\"?", color = VaultTextSecondary) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteConfirm = false
                        onDelete()
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = RoseDanger)
                ) {
                    Text("Delete", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("Cancel", color = VaultTextSecondary)
                }
            }
        )
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("item_detail_screen"),
        color = VaultBgDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Top Navigation & Kebab Menu
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VaultTextPrimary)
                    }
                    Text(
                        text = "Item Details",
                        color = VaultTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Box {
                    IconButton(onClick = { showOverflowMenu = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More actions", tint = VaultTextSecondary)
                    }
                    DropdownMenu(
                        expanded = showOverflowMenu,
                        onDismissRequest = { showOverflowMenu = false },
                        modifier = Modifier.background(VaultSurfaceElevated)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Edit Item", color = VaultTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = EmeraldPrimary) },
                            onClick = {
                                showOverflowMenu = false
                                onEdit()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text(if (item.pinned) "Unpin" else "Pin to Top", color = VaultTextPrimary) },
                            leadingIcon = { Icon(Icons.Default.PushPin, contentDescription = null, tint = CyanAccent) },
                            onClick = {
                                showOverflowMenu = false
                                onTogglePin()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = RoseDanger) },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RoseDanger) },
                            onClick = {
                                showOverflowMenu = false
                                showDeleteConfirm = true
                            }
                        )
                    }
                }
            }

            // Hero Emblem & Title
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(EmeraldPrimary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = when (item.type) {
                            VaultItemType.LINK -> "🔗"
                            VaultItemType.PHONE -> "☎"
                            VaultItemType.EMAIL, VaultItemType.MAIL -> "✉"
                            VaultItemType.TEXT -> "📝"
                            VaultItemType.NUMBER -> "#"
                            VaultItemType.RECORD -> "📋"
                            VaultItemType.PASSWORD -> "🔐"
                            else -> "✦"
                        },
                        fontSize = 26.sp
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = item.title,
                    color = VaultTextPrimary,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = collectionTagLine,
                    color = VaultTextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            HorizontalDivider(color = VaultBorder)

            // Value Section with Masking & Copy
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Value", color = VaultTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    if (isRevealed && revealSecondsLeft > 0) {
                        Text("Auto-masking in ${revealSecondsLeft}s", color = CyanAccent, fontSize = 11.sp)
                    }
                }

                Card(
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = VaultSurfaceElevated),
                    border = BorderStroke(1.dp, VaultBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = if (isRevealed) rawValue else maskedValue,
                            color = VaultTextPrimary,
                            fontSize = 15.sp,
                            fontFamily = if (isRevealed) FontFamily.Default else FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (item.sensitive || item.type == VaultItemType.PASSWORD) {
                                IconButton(
                                    onClick = {
                                        if (isRevealed) {
                                            isRevealed = false
                                        } else {
                                            showAuthPrompt = true
                                        }
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (isRevealed) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = "Reveal",
                                        tint = CyanAccent
                                    )
                                }
                            }

                            IconButton(
                                onClick = { onCopy(item.title, rawValue) }
                            ) {
                                Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = EmeraldPrimary)
                            }

                            if (item.type == VaultItemType.LINK && rawValue.isNotBlank()) {
                                IconButton(
                                    onClick = {
                                        try {
                                            val safe = if (rawValue.startsWith("http")) rawValue else "https://$rawValue"
                                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safe))
                                            context.startActivity(intent)
                                        } catch (ignored: Exception) {}
                                    }
                                ) {
                                    Icon(Icons.Default.OpenInBrowser, contentDescription = "Open Link", tint = IndigoLink)
                                }
                            }
                        }
                    }
                }
            }

            // Multi-Field Details (If Record)
            data class RecordFieldInfo(
                val name: String,
                val type: String,
                val value: String,
                val sensitive: Boolean
            )

            val parsedRecordFields = remember(item.value) {
                if (item.type == VaultItemType.RECORD && item.value.startsWith("[")) {
                    try {
                        val arr = JSONArray(item.value)
                        val list = mutableListOf<RecordFieldInfo>()
                        for (i in 0 until arr.length()) {
                            val fobj = arr.getJSONObject(i)
                            list.add(
                                RecordFieldInfo(
                                    name = fobj.optString("name", "Field"),
                                    type = fobj.optString("type", "text"),
                                    value = fobj.optString("value", ""),
                                    sensitive = fobj.optBoolean("sensitive", false)
                                )
                            )
                        }
                        list
                    } catch (e: Exception) {
                        emptyList()
                    }
                } else {
                    emptyList()
                }
            }

            if (parsedRecordFields.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Record Fields", color = VaultTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    parsedRecordFields.forEach { rField ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .background(VaultSurfaceElevated)
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                val typeIcon = when (rField.type) {
                                    "link" -> "🔗 "
                                    "phone" -> "☎ "
                                    "email" -> "✉ "
                                    "password" -> "🔐 "
                                    "number" -> "# "
                                    else -> ""
                                }
                                Text("$typeIcon${rField.name}", color = VaultTextSecondary, fontSize = 11.sp)
                                Text(
                                    text = if (rField.sensitive && !isRevealed) "••••••••" else rField.value,
                                    color = VaultTextPrimary,
                                    fontSize = 14.sp,
                                    fontFamily = if (rField.type == "password" || rField.sensitive) FontFamily.Monospace else FontFamily.Default
                                )
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Action button based on field type
                                if (rField.value.isNotBlank()) {
                                    when (rField.type) {
                                        "link" -> {
                                            IconButton(
                                                onClick = {
                                                    val urlStr = if (!rField.value.startsWith("http://") && !rField.value.startsWith("https://")) {
                                                        "https://${rField.value}"
                                                    } else rField.value
                                                    try {
                                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(urlStr)))
                                                    } catch (e: Exception) {
                                                        onCopy(rField.name, rField.value)
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.OpenInBrowser, contentDescription = "Open link", tint = IndigoLink, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        "phone" -> {
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${rField.value}")))
                                                    } catch (e: Exception) {
                                                        onCopy(rField.name, rField.value)
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Phone, contentDescription = "Call phone", tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                        "email" -> {
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        context.startActivity(Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:${rField.value}")))
                                                    } catch (e: Exception) {
                                                        onCopy(rField.name, rField.value)
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(Icons.Default.Email, contentDescription = "Send email", tint = CyanAccent, modifier = Modifier.size(16.dp))
                                            }
                                        }
                                    }
                                }

                                IconButton(
                                    onClick = { onCopy(rField.name, rField.value) },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(Icons.Default.ContentCopy, contentDescription = "Copy", tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Notes
            if (item.notes.isNotBlank()) {
                HorizontalDivider(color = VaultBorder)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Notes", color = VaultTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Text(item.notes, color = VaultTextPrimary, fontSize = 14.sp)
                }
            }

            // Tags
            if (item.tags.isNotBlank()) {
                HorizontalDivider(color = VaultBorder)
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Tags", color = VaultTextSecondary, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        item.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { tag ->
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(VaultSurfaceElevated)
                                    .border(1.dp, VaultBorder, RoundedCornerShape(6.dp))
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text(tag, color = VaultTextPrimary, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = VaultBorder)

            // Timestamps
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Created", color = VaultTextSecondary, fontSize = 11.sp)
                    Text(createdDateStr, color = VaultTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("Updated", color = VaultTextSecondary, fontSize = 11.sp)
                    Text(updatedDateStr, color = VaultTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                }
            }

            HorizontalDivider(color = VaultBorder)

            // Star Favorite & Pin toggles
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onToggleFavorite,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = if (item.effectiveFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = if (item.effectiveFavorite) AmberWarning else VaultTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (item.effectiveFavorite) "Favorited" else "Favorite", color = VaultTextPrimary, fontSize = 13.sp)
                }

                OutlinedButton(
                    onClick = onTogglePin,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Default.PushPin,
                        contentDescription = null,
                        tint = if (item.pinned) EmeraldPrimary else VaultTextSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (item.pinned) "Pinned" else "Pin", color = VaultTextPrimary, fontSize = 13.sp)
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom Actions: Edit, Share, Delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = onEdit,
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Edit", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                OutlinedButton(
                    onClick = {
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_SUBJECT, item.title)
                                putExtra(Intent.EXTRA_TEXT, "${item.title}\n${item.effectiveValue}")
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share with"))
                        } catch (ignored: Exception) {}
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share", color = CyanAccent, fontWeight = FontWeight.SemiBold)
                }

                OutlinedButton(
                    onClick = { showDeleteConfirm = true },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(0.9f)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = RoseDanger, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Delete", color = RoseDanger, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}
