package com.example.ui.components

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PowerfulSearchDialog(
    items: List<VaultItem>,
    collections: List<VaultCollection>,
    onSelectItem: (VaultItem) -> Unit,
    onSelectCollection: (VaultCollection) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val trimmed = query.trim()

    val matchingCollections = remember(trimmed, collections) {
        if (trimmed.isBlank()) emptyList()
        else collections.filter { it.name.contains(trimmed, ignoreCase = true) || it.description.contains(trimmed, ignoreCase = true) }
    }

    val matchingItems = remember(trimmed, items) {
        if (trimmed.isBlank()) emptyList()
        else items.filter {
            it.type != VaultItemType.RECORD &&
                    (it.title.contains(trimmed, ignoreCase = true) ||
                            it.effectiveValue.contains(trimmed, ignoreCase = true) ||
                            it.categoryTag.contains(trimmed, ignoreCase = true))
        }
    }

    val matchingRecords = remember(trimmed, items) {
        if (trimmed.isBlank()) emptyList()
        else items.filter {
            it.type == VaultItemType.RECORD &&
                    (it.title.contains(trimmed, ignoreCase = true) ||
                            it.notes.contains(trimmed, ignoreCase = true) ||
                            it.value.contains(trimmed, ignoreCase = true))
        }
    }

    val matchingFields = remember(trimmed, items) {
        if (trimmed.isBlank()) emptyList()
        else items.filter {
            it.tags.contains(trimmed, ignoreCase = true) ||
                    (it.username.contains(trimmed, ignoreCase = true) && !it.title.contains(trimmed, ignoreCase = true))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VaultBgDark,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
                .padding(bottom = 32.dp)
        ) {
            // Search Input Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close search", tint = VaultTextPrimary)
                }

                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search anything...", color = VaultTextSecondary) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = EmeraldPrimary) },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(Icons.Default.Close, contentDescription = "Clear", tint = VaultTextSecondary)
                            }
                        }
                    },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = VaultBorder,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .testTag("search_anything_input")
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (trimmed.isBlank()) {
                // Search suggestions / empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Search, contentDescription = null, tint = VaultTextSecondary.copy(alpha = 0.4f), modifier = Modifier.size(48.dp))
                        Text("Search your vault", color = VaultTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Quickly find credentials, links, contacts, records and fields.", color = VaultTextSecondary, fontSize = 12.sp)
                    }
                }
            } else if (matchingCollections.isEmpty() && matchingItems.isEmpty() && matchingRecords.isEmpty() && matchingFields.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 40.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text("🔍", fontSize = 42.sp)
                        Text("No results for \"$trimmed\"", color = VaultTextPrimary, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        Text("Try searching by title, username, tag, or collection name.", color = VaultTextSecondary, fontSize = 12.sp)
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // COLLECTIONS
                    if (matchingCollections.isNotEmpty()) {
                        item {
                            SectionHeader(title = "COLLECTIONS (${matchingCollections.size})")
                        }
                        items(matchingCollections) { col ->
                            SearchResultRow(
                                iconText = col.icon,
                                title = col.name,
                                subtitle = col.description.ifBlank { "Collection" },
                                onClick = {
                                    onDismiss()
                                    onSelectCollection(col)
                                }
                            )
                        }
                    }

                    // ITEMS
                    if (matchingItems.isNotEmpty()) {
                        item {
                            SectionHeader(title = "ITEMS (${matchingItems.size})")
                        }
                        items(matchingItems) { item ->
                            SearchResultRow(
                                iconText = when (item.type) {
                                    VaultItemType.LINK -> "🔗"
                                    VaultItemType.PHONE -> "☎"
                                    VaultItemType.EMAIL -> "✉"
                                    VaultItemType.NUMBER -> "#"
                                    VaultItemType.PASSWORD -> "🔐"
                                    else -> "✦"
                                },
                                title = item.title,
                                subtitle = "${item.type.displayName} • ${item.categoryTag}",
                                onClick = {
                                    onDismiss()
                                    onSelectItem(item)
                                }
                            )
                        }
                    }

                    // RECORDS
                    if (matchingRecords.isNotEmpty()) {
                        item {
                            SectionHeader(title = "RECORDS (${matchingRecords.size})")
                        }
                        items(matchingRecords) { record ->
                            SearchResultRow(
                                iconText = "📋",
                                title = record.title,
                                subtitle = "Multi-Field Record • ${record.categoryTag}",
                                onClick = {
                                    onDismiss()
                                    onSelectItem(record)
                                }
                            )
                        }
                    }

                    // FIELDS
                    if (matchingFields.isNotEmpty()) {
                        item {
                            SectionHeader(title = "FIELDS & TAGS (${matchingFields.size})")
                        }
                        items(matchingFields) { fieldItem ->
                            SearchResultRow(
                                iconText = "🏷",
                                title = fieldItem.title,
                                subtitle = "Tags: ${fieldItem.tags} • ${fieldItem.username}",
                                onClick = {
                                    onDismiss()
                                    onSelectItem(fieldItem)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        color = EmeraldPrimary,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(vertical = 4.dp)
    )
}

@Composable
private fun SearchResultRow(
    iconText: String,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(VaultSurfaceElevated)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(VaultSurfaceDark),
            contentAlignment = Alignment.Center
        ) {
            Text(iconText, fontSize = 18.sp)
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = VaultTextPrimary,
                fontSize = 14.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                color = VaultTextSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
