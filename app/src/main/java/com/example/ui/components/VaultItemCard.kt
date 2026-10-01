package com.example.ui.components

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Badge
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultItem
import com.example.data.VaultItemType
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IndigoLink
import com.example.ui.theme.PurpleDoc
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurfaceDark
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun VaultItemCard(
    item: VaultItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onQuickCopy: (label: String, value: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val (typeIcon, typeColor) = when (item.type) {
        VaultItemType.PASSWORD -> Pair(Icons.Default.Key, EmeraldPrimary)
        VaultItemType.LINK -> Pair(Icons.Default.Link, IndigoLink)
        VaultItemType.DOCUMENT_ID -> Pair(Icons.Default.Badge, PurpleDoc)
        VaultItemType.MAIL -> Pair(Icons.Default.Email, CyanAccent)
        VaultItemType.NOTE -> Pair(Icons.Default.Note, AmberWarning)
        VaultItemType.PHONE -> Pair(Icons.Default.Phone, CyanAccent)
        VaultItemType.EMAIL -> Pair(Icons.Default.Email, CyanAccent)
        VaultItemType.TEXT -> Pair(Icons.Default.Note, AmberWarning)
        VaultItemType.NUMBER -> Pair(Icons.Default.Badge, PurpleDoc)
        VaultItemType.CUSTOM -> Pair(Icons.Default.Shield, EmeraldPrimary)
        VaultItemType.RECORD -> Pair(Icons.Default.Note, IndigoLink)
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 5.dp)
            .border(1.dp, VaultBorder, RoundedCornerShape(14.dp))
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .testTag("vault_card_${item.id}"),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceDark)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Category Icon Badge
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(typeColor.copy(alpha = 0.15f))
                    .border(1.dp, typeColor.copy(alpha = 0.35f), CircleShape)
            ) {
                Icon(
                    imageVector = typeIcon,
                    contentDescription = item.type.displayName,
                    tint = typeColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Info Column
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = item.title,
                        color = VaultTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )

                    // Small category tag chip
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(VaultSurfaceElevated)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = item.categoryTag,
                            color = VaultTextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                Spacer(modifier = Modifier.height(3.dp))

                // Type-specific primary preview
                when (item.type) {
                    VaultItemType.PASSWORD -> {
                        Text(
                            text = if (item.username.isNotBlank()) item.username else "••••••••",
                            color = VaultTextSecondary,
                            fontSize = 13.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    VaultItemType.LINK -> {
                        Text(
                            text = item.url.replace("https://", "").replace("http://", ""),
                            color = IndigoLink,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    VaultItemType.DOCUMENT_ID -> {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "${item.documentType}: ",
                                color = VaultTextSecondary,
                                fontSize = 12.sp
                            )
                            Text(
                                text = if (item.documentNumber.length > 4) "•••• ${item.documentNumber.takeLast(4)}" else item.documentNumber,
                                color = VaultTextPrimary,
                                fontSize = 12.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }

                    VaultItemType.MAIL -> {
                        Text(
                            text = item.emailAddress,
                            color = VaultTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    VaultItemType.NOTE -> {
                        Text(
                            text = item.notes.replace("\n", " "),
                            color = VaultTextSecondary,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    else -> {
                        Text(
                            text = item.effectiveValue,
                            color = VaultTextSecondary,
                            fontSize = 12.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Quick Actions
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Link direct launcher
                if (item.type == VaultItemType.LINK && item.url.isNotBlank()) {
                    IconButton(
                        onClick = {
                            try {
                                val safe = if (item.url.startsWith("http")) item.url else "https://${item.url}"
                                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(safe))
                                context.startActivity(intent)
                            } catch (ignored: Exception) {
                            }
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Default.OpenInBrowser,
                            contentDescription = "Open Link",
                            tint = IndigoLink,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Quick Copy primary secret
                val copyPayload = when (item.type) {
                    VaultItemType.PASSWORD -> Pair("Password", item.password)
                    VaultItemType.LINK -> Pair("URL", item.url)
                    VaultItemType.DOCUMENT_ID -> Pair("Doc Number", item.documentNumber)
                    VaultItemType.MAIL -> Pair("Email", item.emailAddress)
                    VaultItemType.NOTE -> Pair("Note", item.notes)
                    else -> Pair(item.type.displayName, item.effectiveValue)
                }

                if (copyPayload.second.isNotBlank()) {
                    IconButton(
                        onClick = { onQuickCopy(copyPayload.first, copyPayload.second) },
                        modifier = Modifier
                            .size(36.dp)
                            .testTag("quick_copy_${item.id}")
                    ) {
                        Icon(
                            Icons.Default.ContentCopy,
                            contentDescription = "Copy ${copyPayload.first}",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Favorite star
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        imageVector = if (item.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = "Favorite",
                        tint = if (item.isFavorite) CyanAccent else VaultTextSecondary.copy(alpha = 0.5f),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
