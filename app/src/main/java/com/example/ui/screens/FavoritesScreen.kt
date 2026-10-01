package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.VaultItem
import com.example.data.VaultItemType
import com.example.ui.VaultViewModel
import com.example.ui.components.CompactVaultItemCard
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.VaultBgDark
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurfaceDark
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun FavoritesScreen(
    viewModel: VaultViewModel,
    onItemClick: (VaultItem) -> Unit,
    onEditItem: (VaultItem) -> Unit
) {
    val favoriteItems by viewModel.favoriteItems.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf("All") }

    val filteredList = remember(favoriteItems, selectedTab) {
        when (selectedTab) {
            "Links" -> favoriteItems.filter { it.type == VaultItemType.LINK }
            "Contacts" -> favoriteItems.filter { it.type == VaultItemType.PHONE || it.type == VaultItemType.EMAIL }
            "Records" -> favoriteItems.filter { it.type == VaultItemType.RECORD || it.type == VaultItemType.DOCUMENT_ID }
            else -> favoriteItems
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBgDark)
            .padding(horizontal = 16.dp)
            .testTag("favorites_screen"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
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
                text = "Favorites",
                color = VaultTextPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${filteredList.size} items",
                color = VaultTextSecondary,
                fontSize = 12.sp
            )
        }

        // Filter Pills: All, Links, Contacts, Records
        val tabs = listOf("All", "Links", "Contacts", "Records")
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(tabs) { tab ->
                val isSelected = selectedTab == tab
                FilterChip(
                    selected = isSelected,
                    onClick = { selectedTab = tab },
                    label = { Text(tab, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal) },
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

        if (filteredList.isEmpty()) {
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
                    Text("★", fontSize = 48.sp, color = VaultTextSecondary.copy(alpha = 0.4f))
                    Text(
                        text = "No favorites yet",
                        color = VaultTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Mark important items as favorites to access them quickly.",
                        color = VaultTextSecondary,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(filteredList, key = { it.id }) { item ->
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
