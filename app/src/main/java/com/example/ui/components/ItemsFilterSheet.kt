package com.example.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultCollection
import com.example.data.VaultItemType
import com.example.ui.ItemsFilter
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.VaultBgDark
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurfaceDark
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ItemsFilterSheet(
    currentFilter: ItemsFilter,
    collections: List<VaultCollection>,
    onApply: (ItemsFilter) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var selectedType by remember { mutableStateOf(currentFilter.type) }
    var selectedCollection by remember { mutableStateOf(currentFilter.collectionId) }
    var favoritesOnly by remember { mutableStateOf(currentFilter.favoritesOnly) }
    var pinnedOnly by remember { mutableStateOf(currentFilter.pinnedOnly) }
    var sensitiveOnly by remember { mutableStateOf(currentFilter.sensitiveOnly) }
    var archived by remember { mutableStateOf(currentFilter.archived) }

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
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Filter Items",
                    color = VaultTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = VaultTextSecondary)
                }
            }

            // 1. Filter by Type
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Type", color = VaultTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val types = listOf(
                        null to "All",
                        VaultItemType.LINK to "Links",
                        VaultItemType.PHONE to "Phone",
                        VaultItemType.EMAIL to "Email",
                        VaultItemType.TEXT to "Text",
                        VaultItemType.NUMBER to "Number",
                        VaultItemType.CUSTOM to "Custom",
                        VaultItemType.RECORD to "Records",
                        VaultItemType.PASSWORD to "Passwords"
                    )
                    types.forEach { (typeVal, label) ->
                        FilterChip(
                            selected = selectedType == typeVal,
                            onClick = { selectedType = typeVal },
                            label = { Text(label, fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = EmeraldPrimary
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = VaultBorder)

            // 2. Filter by Collection
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Collection", color = VaultTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = selectedCollection == null,
                        onClick = { selectedCollection = null },
                        label = { Text("All Collections", fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                            selectedLabelColor = EmeraldPrimary
                        )
                    )
                    collections.forEach { col ->
                        FilterChip(
                            selected = selectedCollection == col.id,
                            onClick = { selectedCollection = col.id },
                            label = { Text("${col.icon} ${col.name}", fontSize = 12.sp) },
                            colors = FilterChipDefaults.filterChipColors(
                                selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                                selectedLabelColor = EmeraldPrimary
                            )
                        )
                    }
                }
            }

            HorizontalDivider(color = VaultBorder)

            // 3. Flags: Favorites, Pinned, Sensitive, Archived
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Attributes", color = VaultTextSecondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { favoritesOnly = !favoritesOnly }
                ) {
                    Checkbox(
                        checked = favoritesOnly,
                        onCheckedChange = { favoritesOnly = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Favorites Only (★)", color = VaultTextPrimary, fontSize = 14.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { pinnedOnly = !pinnedOnly }
                ) {
                    Checkbox(
                        checked = pinnedOnly,
                        onCheckedChange = { pinnedOnly = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Pinned to Top", color = VaultTextPrimary, fontSize = 14.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { sensitiveOnly = !sensitiveOnly }
                ) {
                    Checkbox(
                        checked = sensitiveOnly,
                        onCheckedChange = { sensitiveOnly = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sensitive Only", color = VaultTextPrimary, fontSize = 14.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { archived = !archived }
                ) {
                    Checkbox(
                        checked = archived,
                        onCheckedChange = { archived = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Show Archived Records", color = VaultTextPrimary, fontSize = 14.sp)
                }
            }

            // Buttons: Reset & Apply
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = {
                        onReset()
                        onDismiss()
                    },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset", color = VaultTextSecondary)
                }

                Button(
                    onClick = {
                        val filter = ItemsFilter(
                            type = selectedType,
                            collectionId = selectedCollection,
                            favoritesOnly = favoritesOnly,
                            pinnedOnly = pinnedOnly,
                            sensitiveOnly = sensitiveOnly,
                            archived = archived
                        )
                        onApply(filter)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Apply Filter", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
