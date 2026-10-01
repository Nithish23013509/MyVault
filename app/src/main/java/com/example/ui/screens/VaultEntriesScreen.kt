package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.crypto.PasswordGenerator
import com.example.data.VaultEntry
import com.example.ui.VaultViewModel
import com.example.ui.components.CloudSyncAuthDialog
import com.example.ui.theme.AmberWarning
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.IndigoLink
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.VaultBgDark
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurfaceDark
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.VaultSurfaceHighlight
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import com.example.ui.theme.VaultTextTertiary
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Jetpack Compose screen that lists encrypted vault entries and includes
 * a Floating Action Button (FAB) to trigger adding new secure entries.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultEntriesScreen(
    viewModel: VaultViewModel,
    modifier: Modifier = Modifier,
    onBackOrLock: (() -> Unit)? = null
) {
    val entries by viewModel.vaultEntries.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var searchQuery by remember { mutableStateOf("") }
    var selectedCategoryFilter by remember { mutableStateOf<String?>(null) }
    var showAddDialog by remember { mutableStateOf(false) }
    var showCloudDialog by remember { mutableStateOf(false) }
    var activeInspectEntry by remember { mutableStateOf<VaultEntry?>(null) }
    var decryptedContentMap by remember { mutableStateOf<Map<Long, String>>(emptyMap()) }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    val filteredEntries = remember(entries, searchQuery, selectedCategoryFilter) {
        val trimmedQuery = searchQuery.trim()
        entries.filter { entry ->
            val matchesCategory = selectedCategoryFilter == null ||
                    entry.category.equals(selectedCategoryFilter, ignoreCase = true)
            val matchesSearch = trimmedQuery.isBlank() ||
                    entry.title.contains(trimmedQuery, ignoreCase = true) ||
                    entry.category.contains(trimmedQuery, ignoreCase = true)
            matchesCategory && matchesSearch
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .testTag("vault_entries_screen"),
        containerColor = VaultBgDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(EmeraldPrimary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Column {
                            Text(
                                text = "MyVault Items",
                                color = VaultTextPrimary,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${entries.size} items protected • Shield active",
                                color = EmeraldPrimary,
                                fontSize = 12.sp
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { showCloudDialog = true },
                        modifier = Modifier.testTag("entries_cloud_sync_button")
                    ) {
                        Icon(
                            imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.CloudSync,
                            contentDescription = "Cloud Protection & Sync",
                            tint = if (currentUser != null) EmeraldPrimary else CyanAccent
                        )
                    }

                    if (onBackOrLock != null) {
                        IconButton(
                            onClick = onBackOrLock,
                            modifier = Modifier.testTag("vault_lock_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = "Lock Vault",
                                tint = EmeraldPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = VaultSurfaceDark
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = EmeraldPrimary,
                contentColor = Color.Black,
                shape = CircleShape,
                modifier = Modifier.testTag("add_vault_entry_fab")
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add New Vault Entry",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search Input Field at the top of the vault entries list
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("vault_entries_search_bar")
            ) {
                val focusManager = LocalFocusManager.current
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = {
                        Text("Search by title or category...", color = VaultTextTertiary, fontSize = 14.sp)
                    },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search icon",
                            tint = if (searchQuery.isNotEmpty()) EmeraldPrimary else VaultTextSecondary
                        )
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = { searchQuery = "" },
                                modifier = Modifier.testTag("clear_search_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = VaultTextSecondary
                                )
                            }
                        }
                    },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VaultSurfaceElevated,
                        unfocusedContainerColor = VaultSurfaceDark,
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = VaultBorder,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entries_search_field")
                )
            }

            // Real-time filter feedback badge if query is active
            if (searchQuery.isNotBlank() || selectedCategoryFilter != null) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filtered: ${filteredEntries.size} of ${entries.size} entries",
                        color = CyanAccent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        text = "Reset filters",
                        color = AmberWarning,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable {
                                searchQuery = ""
                                selectedCategoryFilter = null
                            }
                            .padding(4.dp)
                    )
                }
            }

            // Category Filter Chips
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item {
                    FilterChip(
                        selected = selectedCategoryFilter == null,
                        onClick = { selectedCategoryFilter = null },
                        label = { Text("All (${entries.size})") },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = VaultSurfaceDark,
                            labelColor = VaultTextSecondary
                        )
                    )
                }
                item {
                    val count = entries.count { it.category.equals(VaultEntry.CATEGORY_PASSWORD, true) }
                    FilterChip(
                        selected = selectedCategoryFilter == VaultEntry.CATEGORY_PASSWORD,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == VaultEntry.CATEGORY_PASSWORD) null else VaultEntry.CATEGORY_PASSWORD
                        },
                        label = { Text("Passwords ($count)") },
                        leadingIcon = {
                            Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = EmeraldPrimary,
                            selectedLabelColor = Color.Black,
                            containerColor = VaultSurfaceDark,
                            labelColor = VaultTextSecondary
                        )
                    )
                }
                item {
                    val count = entries.count { it.category.equals(VaultEntry.CATEGORY_LINK, true) }
                    FilterChip(
                        selected = selectedCategoryFilter == VaultEntry.CATEGORY_LINK,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == VaultEntry.CATEGORY_LINK) null else VaultEntry.CATEGORY_LINK
                        },
                        label = { Text("Links ($count)") },
                        leadingIcon = {
                            Icon(Icons.Default.Link, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = IndigoLink,
                            selectedLabelColor = Color.White,
                            containerColor = VaultSurfaceDark,
                            labelColor = VaultTextSecondary
                        )
                    )
                }
                item {
                    val count = entries.count { it.category.equals(VaultEntry.CATEGORY_NOTE, true) }
                    FilterChip(
                        selected = selectedCategoryFilter == VaultEntry.CATEGORY_NOTE,
                        onClick = {
                            selectedCategoryFilter = if (selectedCategoryFilter == VaultEntry.CATEGORY_NOTE) null else VaultEntry.CATEGORY_NOTE
                        },
                        label = { Text("Notes ($count)") },
                        leadingIcon = {
                            Icon(Icons.Default.Note, contentDescription = null, modifier = Modifier.size(16.dp))
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = AmberWarning,
                            selectedLabelColor = Color.Black,
                            containerColor = VaultSurfaceDark,
                            labelColor = VaultTextSecondary
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Vault Entries List or Empty State
            if (filteredEntries.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(VaultSurfaceDark),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Shield,
                                contentDescription = null,
                                tint = EmeraldPrimary.copy(alpha = 0.6f),
                                modifier = Modifier.size(36.dp)
                            )
                        }
                        Text(
                            text = if (searchQuery.isNotEmpty()) "No entries match \"$searchQuery\"" else "No protected items stored yet",
                            color = VaultTextPrimary,
                            fontSize = 17.sp,
                            fontWeight = FontWeight.SemiBold,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = if (searchQuery.isNotEmpty()) "Try clearing your search query or filters." else "Tap the '+' button below to protect your first password, link, or note.",
                            color = VaultTextSecondary,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.widthIn(max = 280.dp)
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("vault_entries_lazy_column"),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 88.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(filteredEntries, key = { it.id }) { entry ->
                        val decrypted = decryptedContentMap[entry.id]
                        VaultEntryCard(
                            entry = entry,
                            decryptedContent = decrypted,
                            onToggleReveal = {
                                if (decrypted != null) {
                                    decryptedContentMap = decryptedContentMap - entry.id
                                } else {
                                    scope.launch {
                                        val plain = viewModel.decryptEntryContent(entry)
                                        decryptedContentMap = decryptedContentMap + (entry.id to plain)
                                    }
                                }
                            },
                            onInspect = { activeInspectEntry = entry },
                            onCopy = {
                                scope.launch {
                                    val plain = decrypted ?: viewModel.decryptEntryContent(entry)
                                    viewModel.copyToClipboard(entry.title, plain)
                                }
                            },
                            onDelete = {
                                viewModel.deleteVaultEntry(entry)
                            }
                        )
                    }
                }
            }
        }
    }

    // Dialog for adding a new entry
    if (showAddDialog) {
        AddVaultEntryDialog(
            onDismiss = { showAddDialog = false },
            onSave = { title, content, category ->
                viewModel.saveNewEntry(
                    title = title,
                    rawContent = content,
                    category = category,
                    onSuccess = { showAddDialog = false }
                )
            }
        )
    }

    // Dialog for inspecting entry details
    activeInspectEntry?.let { entry ->
        InspectVaultEntryDialog(
            entry = entry,
            viewModel = viewModel,
            onDismiss = { activeInspectEntry = null }
        )
    }

    // Cloud Sync & Firebase Auth Dialog
    if (showCloudDialog) {
        CloudSyncAuthDialog(
            currentUser = currentUser,
            isSyncing = isCloudSyncing,
            syncStatusMessage = cloudSyncStatus,
            viewModel = viewModel,
            onDismiss = { showCloudDialog = false }
        )
    }
}

/**
 * Card representation of an individual [VaultEntry].
 */
@Composable
fun VaultEntryCard(
    entry: VaultEntry,
    decryptedContent: String?,
    onToggleReveal: () -> Unit,
    onInspect: () -> Unit,
    onCopy: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    val categoryConfig = getCategoryUiConfig(entry.category)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, VaultBorder, RoundedCornerShape(14.dp))
            .clickable { onInspect() }
            .testTag("vault_entry_card_${entry.id}"),
        colors = CardDefaults.cardColors(containerColor = VaultSurfaceDark)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(categoryConfig.accentColor.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = categoryConfig.icon,
                            contentDescription = null,
                            tint = categoryConfig.accentColor,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column {
                        Text(
                            text = entry.title,
                            color = VaultTextPrimary,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Text(
                                text = categoryConfig.label,
                                color = categoryConfig.accentColor,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "•",
                                color = VaultTextTertiary,
                                fontSize = 11.sp
                            )
                            Text(
                                text = formatTimestamp(entry.timestamp),
                                color = VaultTextSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // Security badge
                Surface(
                    color = EmeraldPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(6.dp)
                ) {
                    Text(
                        text = "Protected",
                        color = EmeraldPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Content preview container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(VaultSurfaceElevated)
                    .padding(horizontal = 10.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = decryptedContent ?: "••••••••••••••••",
                        color = if (decryptedContent != null) VaultTextPrimary else VaultTextTertiary,
                        fontSize = 13.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        IconButton(
                            onClick = onToggleReveal,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = if (decryptedContent != null) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (decryptedContent != null) "Hide" else "Reveal",
                                tint = VaultTextSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onCopy,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy Content",
                                tint = EmeraldPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        IconButton(
                            onClick = onDelete,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete Entry",
                                tint = RoseDanger.copy(alpha = 0.8f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dialog for creating and encrypting a new [VaultEntry].
 */
@Composable
fun AddVaultEntryDialog(
    onDismiss: () -> Unit,
    onSave: (title: String, content: String, category: String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf(VaultEntry.CATEGORY_PASSWORD) }
    var isPasswordVisible by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurfaceDark,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = EmeraldPrimary)
                Text(
                    text = "New Vault Entry",
                    color = VaultTextPrimary,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Items are secured with hardware-level shield protection before saving.",
                    color = VaultTextSecondary,
                    fontSize = 12.sp
                )

                // Category Selection
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val categories = listOf(
                        VaultEntry.CATEGORY_PASSWORD to "Password",
                        VaultEntry.CATEGORY_LINK to "Link",
                        VaultEntry.CATEGORY_NOTE to "Note"
                    )
                    categories.forEach { (catId, label) ->
                        val isSelected = selectedCategory == catId
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .background(if (isSelected) EmeraldPrimary else VaultSurfaceElevated)
                                .clickable { selectedCategory = catId }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                color = if (isSelected) Color.Black else VaultTextSecondary,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }

                // Title Input
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title / Label") },
                    placeholder = { Text("e.g. GitHub, Router Key") },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = VaultBorder,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary,
                        focusedContainerColor = VaultSurfaceElevated,
                        unfocusedContainerColor = VaultSurfaceElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_entry_title_input")
                )

                // Sensitive Content Input
                OutlinedTextField(
                    value = content,
                    onValueChange = { content = it },
                    label = { Text(if (selectedCategory == VaultEntry.CATEGORY_PASSWORD) "Secret Password" else if (selectedCategory == VaultEntry.CATEGORY_LINK) "Secret URL" else "Secure Content") },
                    placeholder = { Text("Sensitive value to encrypt") },
                    visualTransformation = if (isPasswordVisible || selectedCategory != VaultEntry.CATEGORY_PASSWORD) VisualTransformation.None else PasswordVisualTransformation(),
                    trailingIcon = {
                        IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                            Icon(
                                imageVector = if (isPasswordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (isPasswordVisible) "Hide" else "Show",
                                tint = VaultTextSecondary
                            )
                        }
                    },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = EmeraldPrimary,
                        unfocusedBorderColor = VaultBorder,
                        focusedTextColor = VaultTextPrimary,
                        unfocusedTextColor = VaultTextPrimary,
                        focusedContainerColor = VaultSurfaceElevated,
                        unfocusedContainerColor = VaultSurfaceElevated
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_entry_content_input")
                )

                // Quick Password Generator Button (if password category)
                if (selectedCategory == VaultEntry.CATEGORY_PASSWORD) {
                    TextButton(
                        onClick = {
                            content = PasswordGenerator.generate(18, true, true, true, true, true)
                            isPasswordVisible = true
                        },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Generate Strong Password", color = CyanAccent, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && content.isNotBlank()) {
                        onSave(title, content, selectedCategory)
                    }
                },
                enabled = title.isNotBlank() && content.isNotBlank(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("save_vault_entry_button")
            ) {
                Text("Save & Protect", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = VaultTextSecondary)
            }
        }
    )
}

/**
 * Inspection modal for viewing and copying the full decrypted entry.
 */
@Composable
fun InspectVaultEntryDialog(
    entry: VaultEntry,
    viewModel: VaultViewModel,
    onDismiss: () -> Unit
) {
    var decryptedContent by remember { mutableStateOf<String?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    LaunchedEffect(entry) {
        decryptedContent = viewModel.decryptEntryContent(entry)
        isLoading = false
    }

    val categoryConfig = getCategoryUiConfig(entry.category)

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurfaceDark,
        shape = RoundedCornerShape(16.dp),
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(categoryConfig.accentColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = categoryConfig.icon,
                        contentDescription = null,
                        tint = categoryConfig.accentColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Column {
                    Text(
                        text = entry.title,
                        color = VaultTextPrimary,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Category: ${categoryConfig.label}",
                        color = categoryConfig.accentColor,
                        fontSize = 12.sp
                    )
                }
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Decrypted Content:",
                    color = VaultTextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(VaultSurfaceElevated)
                        .padding(12.dp)
                ) {
                    Text(
                        text = if (isLoading) "Decrypting..." else (decryptedContent ?: "Unable to decrypt"),
                        color = VaultTextPrimary,
                        fontSize = 14.sp,
                        fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                    )
                }

                Text(
                    text = "Created: ${formatTimestamp(entry.timestamp)}",
                    color = VaultTextTertiary,
                    fontSize = 11.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    decryptedContent?.let {
                        viewModel.copyToClipboard(entry.title, it)
                    }
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = EmeraldPrimary,
                    contentColor = Color.Black
                ),
                shape = RoundedCornerShape(8.dp)
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Copy & Secure", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = VaultTextSecondary)
            }
        }
    )
}

private data class CategoryUiConfig(
    val label: String,
    val icon: ImageVector,
    val accentColor: Color
)

private fun getCategoryUiConfig(category: String): CategoryUiConfig {
    return when (category.lowercase(Locale.ROOT)) {
        VaultEntry.CATEGORY_PASSWORD -> CategoryUiConfig("Password", Icons.Default.Key, EmeraldPrimary)
        VaultEntry.CATEGORY_LINK -> CategoryUiConfig("Link", Icons.Default.Link, IndigoLink)
        VaultEntry.CATEGORY_NOTE -> CategoryUiConfig("Note", Icons.Default.Note, AmberWarning)
        else -> CategoryUiConfig(category.replaceFirstChar { it.uppercase() }, Icons.Default.Shield, CyanAccent)
    }
}

private fun formatTimestamp(timestamp: Long): String {
    val sdf = SimpleDateFormat("MMM dd, yyyy • HH:mm", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
