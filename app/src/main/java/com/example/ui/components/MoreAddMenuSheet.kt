package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultItemType
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IndigoLink
import com.example.ui.theme.PurpleDoc
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurfaceDark
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

import androidx.compose.foundation.layout.navigationBarsPadding

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MoreAddMenuSheet(
    onSelectType: (VaultItemType) -> Unit,
    onSelectMultiFieldRecord: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VaultSurfaceDark,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .navigationBarsPadding()
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Add to Vault",
                    color = VaultTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = VaultTextSecondary)
                }
            }

            AddMenuItemRow(
                icon = Icons.Default.Link,
                iconColor = IndigoLink,
                title = "Link",
                subtitle = "Websites, bookmarks & portal URLs",
                onClick = {
                    onSelectType(VaultItemType.LINK)
                    onDismiss()
                }
            )

            AddMenuItemRow(
                icon = Icons.Default.Phone,
                iconColor = EmeraldPrimary,
                title = "Phone",
                subtitle = "Contact numbers & hotlines",
                onClick = {
                    onSelectType(VaultItemType.PHONE)
                    onDismiss()
                }
            )

            AddMenuItemRow(
                icon = Icons.Default.Email,
                iconColor = CyanAccent,
                title = "Email",
                subtitle = "Email accounts & addresses",
                onClick = {
                    onSelectType(VaultItemType.EMAIL)
                    onDismiss()
                }
            )

            AddMenuItemRow(
                icon = Icons.Default.Note,
                iconColor = Color(0xFFE2E8F0),
                title = "Text",
                subtitle = "Private notes, codes & recovery phrases",
                onClick = {
                    onSelectType(VaultItemType.TEXT)
                    onDismiss()
                }
            )

            AddMenuItemRow(
                icon = Icons.Default.Tag,
                iconColor = Color(0xFFF59E0B),
                title = "Number",
                subtitle = "Account numbers, serials & PIN codes",
                onClick = {
                    onSelectType(VaultItemType.NUMBER)
                    onDismiss()
                }
            )

            AddMenuItemRow(
                icon = Icons.Default.Key,
                iconColor = Color(0xFFEC4899),
                title = "Custom Information",
                subtitle = "ID cards, secret attributes & credentials",
                onClick = {
                    onSelectType(VaultItemType.CUSTOM)
                    onDismiss()
                }
            )

            AddMenuItemRow(
                icon = Icons.Default.Lock,
                iconColor = EmeraldPrimary,
                title = "Password",
                subtitle = "Service credentials & passphrases",
                onClick = {
                    onSelectType(VaultItemType.PASSWORD)
                    onDismiss()
                }
            )

            HorizontalDivider(
                color = VaultBorder,
                modifier = Modifier.padding(vertical = 6.dp)
            )

            AddMenuItemRow(
                icon = Icons.Default.Key,
                iconColor = PurpleDoc,
                title = "Multi-Field Record",
                subtitle = "Store multiple links, phones, emails & custom fields in one item",
                onClick = {
                    onSelectMultiFieldRecord()
                    onDismiss()
                }
            )
        }
    }
}

@Composable
private fun AddMenuItemRow(
    icon: ImageVector,
    iconColor: Color,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(iconColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(title, color = VaultTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = VaultTextSecondary, fontSize = 12.sp)
        }

        Icon(Icons.Default.ChevronRight, contentDescription = null, tint = VaultTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(20.dp))
    }
}
