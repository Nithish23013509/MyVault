package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.VaultItem
import com.example.data.VaultItemType
import com.example.ui.ItemsFilter
import com.example.ui.VaultViewModel
import com.example.ui.components.CompactVaultItemCard
import com.example.ui.components.ItemsFilterSheet
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.VaultBgDark
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurfaceDark
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun ItemsScreen(
    viewModel: VaultViewModel,
    onItemClick: (VaultItem) -> Unit,
    onEditItem: (VaultItem) -> Unit,
    onAddItem: () -> Unit
) {
    val items by viewModel.filteredItems.collectAsStateWithLifecycle()
    val allItems by viewModel.items.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val activeFilter by viewModel.itemsFilter.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()

    var showFilterSheet by remember { mutableStateOf(false) }

    if (showFilterSheet) {
        ItemsFilterSheet(
            currentFilter = activeFilter,
            collections = collections,
            onApply = { newFilter ->
                viewModel.updateItemsFilter(newFilter)
            },
            onReset = {
                viewModel.clearItemsFilter()
            },
            onDismiss = { showFilterSheet = false }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBgDark)
            .padding(horizontal = 16.dp)
            .testTag("items_screen"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Title Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Items",
                color = VaultTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${items.size} of ${allItems.size}",
                color = VaultTextSecondary,
                fontSize = 12.sp
            )
        }

        // Search bar with Filter button
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { viewModel.setSearchQuery(it) },
                placeholder = { Text("Search items...", color = VaultTextSecondary, fontSize = 14.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setSearchQuery("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear", tint = VaultTextSecondary)
                        }
                    }
                },
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = EmeraldPrimary,
                    unfocusedBorderColor = VaultBorder,
                    focusedTextColor = VaultTextPrimary,
                    unfocusedTextColor = VaultTextPrimary,
                    focusedContainerColor = VaultSurfaceDark,
                    unfocusedContainerColor = VaultSurfaceDark
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .weight(1f)
                    .testTag("items_search_input")
            )

            // Filter button
            IconButton(
                onClick = { showFilterSheet = true },
                modifier = Modifier
                    .size(48.dp)
                    .background(
                        if (activeFilter != ItemsFilter()) EmeraldPrimary.copy(alpha = 0.2f) else VaultSurfaceDark,
                        RoundedCornerShape(12.dp)
                    )
                    .testTag("items_filter_button")
            ) {
                Icon(
                    imageVector = Icons.Default.FilterList,
                    contentDescription = "Filter",
                    tint = if (activeFilter != ItemsFilter()) EmeraldPrimary else VaultTextSecondary
                )
            }
        }

        // Category Pills (All, Links, Phone, Email, Text, Number, Custom, Records)
        val typeTabs = listOf(
            null to "All",
            VaultItemType.LINK to "Links",
            VaultItemType.PHONE to "Phone",
            VaultItemType.EMAIL to "Email",
            VaultItemType.TEXT to "Text",
            VaultItemType.NUMBER to "Number",
            VaultItemType.CUSTOM to "Custom",
            VaultItemType.RECORD to "Records"
        )

        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(typeTabs) { (tabType, label) ->
                val isSelected = activeFilter.type == tabType
                FilterChip(
                    selected = isSelected,
                    onClick = {
                        viewModel.selectCategory(tabType)
                    },
                    label = { Text(label, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = EmeraldPrimary.copy(alpha = 0.2f),
                        selectedLabelColor = EmeraldPrimary,
                        containerColor = VaultSurfaceDark,
                        labelColor = VaultTextSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        enabled = true,
                        selected = isSelected,
                        borderColor = if (isSelected) EmeraldPrimary else VaultBorder
                    )
                )
            }
        }

        // Active Filter indicator badge
        if (activeFilter.collectionId != null || activeFilter.favoritesOnly || activeFilter.pinnedOnly || activeFilter.sensitiveOnly) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = buildString {
                        append("Filter active: ")
                        if (activeFilter.collectionId != null) append("${activeFilter.collectionId} • ")
                        if (activeFilter.favoritesOnly) append("Favorites • ")
                        if (activeFilter.pinnedOnly) append("Pinned • ")
                    }.trimEnd(' ', '•'),
                    color = EmeraldPrimary,
                    fontSize = 11.sp
                )
                Text(
                    text = "Clear filter",
                    color = VaultTextSecondary,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable { viewModel.clearItemsFilter() }
                )
            }
        }

        // List or Empty State
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(bottom = 60.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text("📦", fontSize = 48.sp)
                    Text(
                        text = if (searchQuery.isNotEmpty() || activeFilter != ItemsFilter()) "No matching items" else "Your vault is empty",
                        color = VaultTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = if (searchQuery.isNotEmpty() || activeFilter != ItemsFilter())
                            "Try adjusting your search query or filter."
                        else
                            "Add your first item to start organizing your information.",
                        color = VaultTextSecondary,
                        fontSize = 13.sp
                    )
                    Button(
                        onClick = onAddItem,
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.padding(top = 8.dp)
                    ) {
                        Text("+ Add Item", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(items, key = { it.id }) { item ->
                    CompactVaultItemCard(
                        item = item,
                        onClick = { onItemClick(item) },
                        onToggleFavorite = { viewModel.toggleFavorite(item) },
                        onEdit = { onEditItem(item) },
                        onDelete = { viewModel.deleteItem(item) },
                        onCopy = { valText ->
                            viewModel.copyToClipboard(item.title, valText)
                        }
                    )
                }
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }
    }
}
