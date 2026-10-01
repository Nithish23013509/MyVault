package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultCollection
import com.example.data.VaultItem
import com.example.data.VaultItemType
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.VaultBgDark
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurfaceDark
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun CollectionDetailDialog(
    collection: VaultCollection,
    items: List<VaultItem>,
    onItemClick: (VaultItem) -> Unit,
    onToggleFavorite: (VaultItem) -> Unit,
    onEditItem: (VaultItem) -> Unit,
    onDeleteItem: (VaultItem) -> Unit,
    onCopy: (String) -> Unit,
    onAddItem: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val collectionItems = items.filter {
        it.collectionId == collection.id || it.categoryTag.equals(collection.name, ignoreCase = true)
    }

    val linkCount = collectionItems.count { it.type == VaultItemType.LINK }
    val emailCount = collectionItems.count { it.type == VaultItemType.EMAIL || it.type == VaultItemType.MAIL }
    val phoneCount = collectionItems.count { it.type == VaultItemType.PHONE }
    val numberCount = collectionItems.count { it.type == VaultItemType.NUMBER }
    val recordCount = collectionItems.count { it.type == VaultItemType.RECORD || it.type == VaultItemType.DOCUMENT_ID }
    val customCount = collectionItems.count { it.type == VaultItemType.CUSTOM || it.type == VaultItemType.TEXT || it.type == VaultItemType.PASSWORD }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VaultBgDark,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = VaultTextPrimary)
                }

                Spacer(modifier = Modifier.width(8.dp))

                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(collection.colorHex)).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(collection.icon, fontSize = 20.sp)
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = collection.name,
                        color = VaultTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${collectionItems.size} Items",
                        color = VaultTextSecondary,
                        fontSize = 12.sp
                    )
                }

                IconButton(
                    onClick = {
                        onDismiss()
                        onAddItem()
                    }
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Item", tint = EmeraldPrimary)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Breakdown Badges
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                if (linkCount > 0) BreakdownBadge(label = "Links", count = linkCount)
                if (emailCount > 0) BreakdownBadge(label = "Emails", count = emailCount)
                if (phoneCount > 0) BreakdownBadge(label = "Phone", count = phoneCount)
                if (numberCount > 0) BreakdownBadge(label = "Numbers", count = numberCount)
                if (recordCount > 0) BreakdownBadge(label = "Records", count = recordCount)
                if (customCount > 0) BreakdownBadge(label = "Custom", count = customCount)
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (collectionItems.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(collection.icon, fontSize = 42.sp)
                        Text("No items in ${collection.name}", color = VaultTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Add items to organize your ${collection.name.lowercase()} information.", color = VaultTextSecondary, fontSize = 12.sp)
                        Button(
                            onClick = {
                                onDismiss()
                                onAddItem()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Text("+ Add Item", color = Color.Black, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(collectionItems, key = { it.id }) { item ->
                        CompactVaultItemCard(
                            item = item,
                            onClick = { onItemClick(item) },
                            onToggleFavorite = { onToggleFavorite(item) },
                            onEdit = { onEditItem(item) },
                            onDelete = { onDeleteItem(item) },
                            onCopy = onCopy
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BreakdownBadge(label: String, count: Int) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(VaultSurfaceElevated)
            .border(1.dp, VaultBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(label, color = VaultTextSecondary, fontSize = 12.sp)
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = "$count",
            color = VaultTextPrimary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
