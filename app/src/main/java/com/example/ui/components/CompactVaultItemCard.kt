package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
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
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurfaceDark
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun CompactVaultItemCard(
    item: VaultItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onCopy: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showMenu by remember { mutableStateOf(false) }

    val iconColor = when (item.type) {
        VaultItemType.LINK -> IndigoLink
        VaultItemType.PHONE -> EmeraldPrimary
        VaultItemType.EMAIL, VaultItemType.MAIL -> CyanAccent
        VaultItemType.NUMBER -> Color(0xFFF59E0B)
        VaultItemType.TEXT, VaultItemType.NOTE -> Color(0xFFE2E8F0)
        VaultItemType.RECORD, VaultItemType.DOCUMENT_ID -> PurpleDoc
        VaultItemType.PASSWORD -> EmeraldPrimary
        VaultItemType.CUSTOM -> Color(0xFFEC4899)
    }

    val iconVector: ImageVector = when (item.type) {
        VaultItemType.LINK -> Icons.Default.Link
        VaultItemType.PHONE -> Icons.Default.Phone
        VaultItemType.EMAIL, VaultItemType.MAIL -> Icons.Default.Email
        VaultItemType.NUMBER -> Icons.Default.Tag
        VaultItemType.TEXT, VaultItemType.NOTE -> Icons.Default.Note
        VaultItemType.RECORD, VaultItemType.DOCUMENT_ID -> Icons.Default.Key
        VaultItemType.PASSWORD -> Icons.Default.Lock
        VaultItemType.CUSTOM -> Icons.Default.Key
    }

    // Mask sensitive values by default: e.g. ••••••••1234
    val displayValue = remember(item) {
        val raw = item.effectiveValue
        if (item.sensitive || item.type == VaultItemType.PASSWORD) {
            if (raw.length > 4) "••••••••" + raw.takeLast(4) else "••••••••"
        } else {
            raw
        }
    }

    val tagsLine = remember(item) {
        val collection = item.collectionId?.replaceFirstChar { it.uppercase() } ?: item.categoryTag
        val tags = item.tags.takeIf { it.isNotBlank() } ?: "Protected"
        "$collection · $tags"
    }

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceDark),
        border = BorderStroke(1.dp, if (item.pinned) EmeraldPrimary.copy(alpha = 0.5f) else VaultBorder),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("item_card_${item.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left Type Icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = iconVector,
                    contentDescription = item.type.displayName,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Title, Value, Tags Column
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.title,
                        color = VaultTextPrimary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    if (item.pinned) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Pinned",
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                if (displayValue.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = displayValue,
                        color = VaultTextSecondary,
                        fontSize = 13.sp,
                        fontFamily = if (item.sensitive || item.type == VaultItemType.PASSWORD) FontFamily.Monospace else FontFamily.Default,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Spacer(modifier = Modifier.height(3.dp))
                Text(
                    text = tagsLine,
                    color = VaultTextSecondary.copy(alpha = 0.7f),
                    fontSize = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Star Favorite action
            IconButton(
                onClick = onToggleFavorite,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("favorite_item_${item.id}")
            ) {
                Icon(
                    imageVector = if (item.effectiveFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                    contentDescription = if (item.effectiveFavorite) "Remove from favorites" else "Add to favorites",
                    tint = if (item.effectiveFavorite) AmberWarning else VaultTextSecondary.copy(alpha = 0.5f),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Kebab Menu action
            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier
                        .size(36.dp)
                        .testTag("item_menu_${item.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.MoreVert,
                        contentDescription = "More actions",
                        tint = VaultTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false },
                    modifier = Modifier.background(VaultSurfaceElevated)
                ) {
                    DropdownMenuItem(
                        text = { Text("Copy", color = VaultTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.ContentCopy, contentDescription = null, tint = CyanAccent) },
                        onClick = {
                            showMenu = false
                            onCopy(item.effectiveValue)
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Edit", color = VaultTextPrimary) },
                        leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null, tint = EmeraldPrimary) },
                        onClick = {
                            showMenu = false
                            onEdit()
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete", color = RoseDanger) },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = RoseDanger) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
