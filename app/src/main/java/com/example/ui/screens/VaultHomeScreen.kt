package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
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
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.VaultCollection
import com.example.data.VaultItem
import com.example.data.VaultItemType
import com.example.ui.VaultViewModel
import com.example.ui.components.AddEditVaultItemSheet
import com.example.ui.components.BackupRestoreDialog
import com.example.ui.components.CloudSyncAuthDialog
import com.example.ui.components.CollectionDetailDialog
import com.example.ui.components.CollectionsManagerDialog
import com.example.ui.components.CompactVaultItemCard
import com.example.ui.components.CreateCollectionDialog
import com.example.ui.components.ItemDetailDialog
import com.example.ui.components.MoreAddMenuSheet
import com.example.ui.components.MultiFieldRecordSheet
import com.example.ui.components.PasswordGeneratorDialog
import com.example.ui.components.PowerfulSearchDialog
import com.example.ui.components.SecurityAuditDialog
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VaultHomeScreen(
    viewModel: VaultViewModel,
    onLockRequested: () -> Unit
) {
    val allItems by viewModel.items.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val clipboardSeconds by viewModel.clipboardClearSeconds.collectAsStateWithLifecycle()
    val statusMsg by viewModel.statusMessage.collectAsStateWithLifecycle()
    val audit by viewModel.audit.collectAsStateWithLifecycle()
    val genConfig by viewModel.genConfig.collectAsStateWithLifecycle()
    val generatedPassword by viewModel.generatedPassword.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isCloudSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }

    // 4 Bottom Navigation Tabs: 0 = Home, 1 = Items, 2 = Favorites, 3 = Settings
    var selectedTab by remember { mutableIntStateOf(0) }

    // Dialog & Sheet States
    var showAddSheet by remember { mutableStateOf(false) }
    var currentAddType by remember { mutableStateOf(VaultItemType.LINK) }
    var itemToEdit by remember { mutableStateOf<VaultItem?>(null) }
    var itemToInspect by remember { mutableStateOf<VaultItem?>(null) }
    var showMoreAddMenu by remember { mutableStateOf(false) }
    var showMultiFieldSheet by remember { mutableStateOf(false) }
    var showPowerfulSearch by remember { mutableStateOf(false) }
    var selectedCollectionForDetail by remember { mutableStateOf<VaultCollection?>(null) }
    var showCollectionsManager by remember { mutableStateOf(false) }
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var showGeneratorDialog by remember { mutableStateOf(false) }
    var showAuditDialog by remember { mutableStateOf(false) }
    var showBackupDialog by remember { mutableStateOf(false) }
    var showCloudDialog by remember { mutableStateOf(false) }

    LaunchedEffect(statusMsg) {
        statusMsg?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearStatusMessage()
        }
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .testTag("vault_home_screen"),
        containerColor = VaultBgDark,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar(
                containerColor = VaultSurfaceDark,
                tonalElevation = 8.dp,
                modifier = Modifier.testTag("vault_bottom_navigation")
            ) {
                NavigationBarItem(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                    label = { Text("Home", fontSize = 11.sp, fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        unselectedIconColor = VaultTextSecondary,
                        unselectedTextColor = VaultTextSecondary,
                        indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_home")
                )
                NavigationBarItem(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    icon = { Icon(Icons.Default.GridView, contentDescription = "Items") },
                    label = { Text("Items", fontSize = 11.sp, fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        unselectedIconColor = VaultTextSecondary,
                        unselectedTextColor = VaultTextSecondary,
                        indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_items")
                )
                NavigationBarItem(
                    selected = selectedTab == 2,
                    onClick = { selectedTab = 2 },
                    icon = { Icon(Icons.Default.Star, contentDescription = "Favorites") },
                    label = { Text("Favorites", fontSize = 11.sp, fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        unselectedIconColor = VaultTextSecondary,
                        unselectedTextColor = VaultTextSecondary,
                        indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_favorites")
                )
                NavigationBarItem(
                    selected = selectedTab == 3,
                    onClick = { selectedTab = 3 },
                    icon = { Icon(Icons.Default.Settings, contentDescription = "Settings") },
                    label = { Text("Settings", fontSize = 11.sp, fontWeight = if (selectedTab == 3) FontWeight.Bold else FontWeight.Normal) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = EmeraldPrimary,
                        selectedTextColor = EmeraldPrimary,
                        unselectedIconColor = VaultTextSecondary,
                        unselectedTextColor = VaultTextSecondary,
                        indicatorColor = EmeraldPrimary.copy(alpha = 0.15f)
                    ),
                    modifier = Modifier.testTag("nav_tab_settings")
                )
            }
        },
        floatingActionButton = {
            // Show FAB on Home and Items tabs
            if (selectedTab == 0 || selectedTab == 1) {
                FloatingActionButton(
                    onClick = {
                        itemToEdit = null
                        currentAddType = VaultItemType.LINK
                        showAddSheet = true
                    },
                    containerColor = EmeraldPrimary,
                    contentColor = Color(0xFF003822),
                    shape = CircleShape,
                    modifier = Modifier
                        .size(56.dp)
                        .testTag("add_item_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add new record", modifier = Modifier.size(28.dp))
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (selectedTab) {
                0 -> {
                    // TAB 0: Clean, simplified Home Screen
                    HomeScreenContent(
                        allItems = allItems,
                        collections = collections,
                        clipboardSeconds = clipboardSeconds,
                        onOpenSearch = { showPowerfulSearch = true },
                        onQuickAdd = { type ->
                            itemToEdit = null
                            currentAddType = type
                            showAddSheet = true
                        },
                        onOpenMoreAdd = { showMoreAddMenu = true },
                        onSeeAllCollections = { showCollectionsManager = true },
                        onSelectCollection = { collection -> selectedCollectionForDetail = collection },
                        onSeeAllRecent = { selectedTab = 1 },
                        onInspectItem = { item -> itemToInspect = item },
                        onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
                        onQuickCopy = { label, value -> viewModel.copyToClipboard(label, value) },
                        onOpenSettings = { selectedTab = 3 },
                        onLockVault = {
                            viewModel.lockVault()
                            onLockRequested()
                        }
                    )
                }
                1 -> {
                    // TAB 1: Main Browsing Screen (Items)
                    ItemsScreen(
                        viewModel = viewModel,
                        onItemClick = { item -> itemToInspect = item },
                        onEditItem = { item ->
                            itemToEdit = item
                            if (item.type == VaultItemType.RECORD) {
                                showMultiFieldSheet = true
                            } else {
                                currentAddType = item.type
                                showAddSheet = true
                            }
                        },
                        onAddItem = {
                            itemToEdit = null
                            currentAddType = VaultItemType.LINK
                            showAddSheet = true
                        }
                    )
                }
                2 -> {
                    // TAB 2: Clean Favorites Screen
                    FavoritesScreen(
                        viewModel = viewModel,
                        onItemClick = { item -> itemToInspect = item },
                        onEditItem = { item ->
                            itemToEdit = item
                            if (item.type == VaultItemType.RECORD) {
                                showMultiFieldSheet = true
                            } else {
                                currentAddType = item.type
                                showAddSheet = true
                            }
                        }
                    )
                }
                3 -> {
                    // TAB 3: Organized Settings Screen
                    SettingsScreen(
                        viewModel = viewModel,
                        onOpenBackupDialog = { showBackupDialog = true },
                        onOpenCloudDialog = { showCloudDialog = true },
                        onOpenSecurityAudit = { showAuditDialog = true },
                        onOpenCollections = { showCollectionsManager = true }
                    )
                }
            }
        }
    }

    // ================== DIALOGS & SHEETS ==================

    // Common Structure Add / Edit Sheet
    if (showAddSheet) {
        AddEditVaultItemSheet(
            initialItem = itemToEdit,
            defaultType = currentAddType,
            collections = collections,
            onSave = { savedItem ->
                viewModel.saveItem(savedItem)
                showAddSheet = false
                itemToEdit = null
            },
            onDismiss = {
                showAddSheet = false
                itemToEdit = null
            }
        )
    }

    // "More" Add Menu Sheet
    if (showMoreAddMenu) {
        MoreAddMenuSheet(
            onSelectType = { type ->
                showMoreAddMenu = false
                itemToEdit = null
                currentAddType = type
                showAddSheet = true
            },
            onSelectMultiFieldRecord = {
                showMoreAddMenu = false
                itemToEdit = null
                showMultiFieldSheet = true
            },
            onDismiss = { showMoreAddMenu = false }
        )
    }

    // Multi-Field Record Sheet
    if (showMultiFieldSheet) {
        MultiFieldRecordSheet(
            initialItem = itemToEdit,
            collections = collections,
            templates = viewModel.recordTemplates,
            onSave = { savedItem ->
                viewModel.saveItem(savedItem)
                showMultiFieldSheet = false
                itemToEdit = null
            },
            onDismiss = {
                showMultiFieldSheet = false
                itemToEdit = null
            }
        )
    }

    // Item Detail Dialog
    if (itemToInspect != null) {
        ItemDetailDialog(
            item = itemToInspect!!,
            onEdit = {
                itemToEdit = itemToInspect
                itemToInspect = null
                if (itemToEdit?.type == VaultItemType.RECORD) {
                    showMultiFieldSheet = true
                } else {
                    currentAddType = itemToEdit?.type ?: VaultItemType.LINK
                    showAddSheet = true
                }
            },
            onDelete = {
                viewModel.deleteItem(itemToInspect!!)
                itemToInspect = null
            },
            onToggleFavorite = {
                viewModel.toggleFavorite(itemToInspect!!)
                itemToInspect = itemToInspect!!.copy(favorite = !itemToInspect!!.favorite)
            },
            onTogglePin = {
                viewModel.togglePin(itemToInspect!!)
                itemToInspect = itemToInspect!!.copy(pinned = !itemToInspect!!.pinned)
            },
            onCopy = { label, value ->
                viewModel.copyToClipboard(label, value)
            },
            onVerifyRevealAuth = { pin ->
                viewModel.verifyPinForReveal(pin)
            },
            onDismiss = { itemToInspect = null }
        )
    }

    // Powerful Grouped Search Dialog
    if (showPowerfulSearch) {
        PowerfulSearchDialog(
            items = allItems,
            collections = collections,
            onSelectItem = { item ->
                showPowerfulSearch = false
                itemToInspect = item
            },
            onSelectCollection = { collection ->
                showPowerfulSearch = false
                selectedCollectionForDetail = collection
            },
            onDismiss = { showPowerfulSearch = false }
        )
    }

    // Collection Detail Dialog
    if (selectedCollectionForDetail != null) {
        CollectionDetailDialog(
            collection = selectedCollectionForDetail!!,
            items = allItems,
            onItemClick = { item -> itemToInspect = item },
            onToggleFavorite = { item -> viewModel.toggleFavorite(item) },
            onEditItem = { item ->
                itemToEdit = item
                selectedCollectionForDetail = null
                if (item.type == VaultItemType.RECORD) {
                    showMultiFieldSheet = true
                } else {
                    currentAddType = item.type
                    showAddSheet = true
                }
            },
            onDeleteItem = { item -> viewModel.deleteItem(item) },
            onCopy = { valToCopy -> viewModel.copyToClipboard("Item Value", valToCopy) },
            onAddItem = {
                itemToEdit = null
                currentAddType = VaultItemType.LINK
                selectedCollectionForDetail = null
                showAddSheet = true
            },
            onDismiss = { selectedCollectionForDetail = null }
        )
    }

    // Collections Manager Dialog
    if (showCollectionsManager) {
        CollectionsManagerDialog(
            collections = collections,
            items = allItems,
            onSelectCollection = { collection ->
                showCollectionsManager = false
                selectedCollectionForDetail = collection
            },
            onOpenCreateCollection = {
                showCollectionsManager = false
                showCreateCollectionDialog = true
            },
            onDismiss = { showCollectionsManager = false }
        )
    }

    // Create Collection Dialog
    if (showCreateCollectionDialog) {
        CreateCollectionDialog(
            onDismiss = { showCreateCollectionDialog = false },
            onCreate = { name, icon, colorHex, desc ->
                viewModel.createCollection(name, icon, colorHex, desc)
                showCreateCollectionDialog = false
            }
        )
    }

    // Password Generator Dialog
    if (showGeneratorDialog) {
        PasswordGeneratorDialog(
            generatedPassword = generatedPassword,
            config = genConfig,
            onConfigChange = { viewModel.updateGenConfig(it) },
            onRegenerate = { viewModel.regeneratePassword() },
            onCopy = { viewModel.copyToClipboard("Generated Password", it) },
            onDismiss = { showGeneratorDialog = false }
        )
    }

    // Security Audit Dialog
    if (showAuditDialog) {
        SecurityAuditDialog(
            audit = audit,
            onDismiss = { showAuditDialog = false }
        )
    }

    // Backup & Restore Dialog
    if (showBackupDialog) {
        BackupRestoreDialog(
            onExport = { passphrase, onComplete ->
                viewModel.exportEncryptedBackup(passphrase, onComplete)
            },
            onImport = { backupData, passphrase, onComplete ->
                viewModel.importEncryptedBackup(backupData, passphrase, onComplete)
            },
            onCopyBackup = { text ->
                viewModel.copyToClipboard("Backup Data", text)
            },
            onDismiss = { showBackupDialog = false }
        )
    }

    // Cloud Sync Auth Dialog
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
 * Clean, uncluttered Home Screen matching User Requirement #1
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeScreenContent(
    allItems: List<VaultItem>,
    collections: List<VaultCollection>,
    clipboardSeconds: Int?,
    onOpenSearch: () -> Unit,
    onQuickAdd: (VaultItemType) -> Unit,
    onOpenMoreAdd: () -> Unit,
    onSeeAllCollections: () -> Unit,
    onSelectCollection: (VaultCollection) -> Unit,
    onSeeAllRecent: () -> Unit,
    onInspectItem: (VaultItem) -> Unit,
    onToggleFavorite: (VaultItem) -> Unit,
    onQuickCopy: (label: String, value: String) -> Unit,
    onOpenSettings: () -> Unit,
    onLockVault: () -> Unit
) {
    val recentItems = remember(allItems) {
        allItems.sortedByDescending { it.updatedAt }.take(4)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 680.dp)
                .padding(bottom = 80.dp)
        ) {
            // Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.2f))
                    ) {
                        Icon(
                            imageVector = Icons.Default.Shield,
                            contentDescription = null,
                            tint = EmeraldPrimary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "MyVault",
                        color = VaultTextPrimary,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onLockVault,
                        modifier = Modifier.testTag("home_lock_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = "Lock Vault",
                            tint = RoseDanger,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("home_settings_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Settings",
                            tint = VaultTextSecondary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Clipboard Protection Banner (if active)
            AnimatedVisibility(
                visible = clipboardSeconds != null,
                enter = fadeIn(),
                exit = fadeOut()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(0xFF0F172A))
                        .border(1.dp, CyanAccent.copy(alpha = 0.4f), RoundedCornerShape(10.dp))
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.Security,
                            contentDescription = null,
                            tint = CyanAccent,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Clipboard clears in ${clipboardSeconds ?: 0}s",
                            color = CyanAccent,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Text(
                        text = "Clear Now",
                        color = EmeraldPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clickable { onQuickCopy("Clipboard", "") }
                            .padding(4.dp)
                    )
                }
            }

            // Search Bar Button (opens powerful search)
            Surface(
                onClick = onOpenSearch,
                shape = RoundedCornerShape(14.dp),
                color = VaultSurfaceDark,
                border = BorderStroke(1.dp, VaultBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("home_search_bar")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = VaultTextSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "Search anything...",
                        color = VaultTextSecondary,
                        fontSize = 15.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Quick Add Card
            Card(
                colors = CardDefaults.cardColors(containerColor = VaultSurfaceDark),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, VaultBorder),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .testTag("quick_add_card")
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Quick Add",
                        color = VaultTextSecondary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    // Row 1: Link, Phone, Email
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickAddActionItem(
                            icon = Icons.Default.Link,
                            iconColor = IndigoLink,
                            label = "Link",
                            modifier = Modifier.weight(1f),
                            onClick = { onQuickAdd(VaultItemType.LINK) }
                        )
                        QuickAddActionItem(
                            icon = Icons.Default.Phone,
                            iconColor = CyanAccent,
                            label = "Phone",
                            modifier = Modifier.weight(1f),
                            onClick = { onQuickAdd(VaultItemType.PHONE) }
                        )
                        QuickAddActionItem(
                            icon = Icons.Default.Email,
                            iconColor = CyanAccent,
                            label = "Email",
                            modifier = Modifier.weight(1f),
                            onClick = { onQuickAdd(VaultItemType.EMAIL) }
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Row 2: Text, Number, More
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        QuickAddActionItem(
                            icon = Icons.Default.Note,
                            iconColor = AmberWarning,
                            label = "Text",
                            modifier = Modifier.weight(1f),
                            onClick = { onQuickAdd(VaultItemType.TEXT) }
                        )
                        QuickAddActionItem(
                            icon = Icons.Default.Tag,
                            iconColor = PurpleDoc,
                            label = "Number",
                            modifier = Modifier.weight(1f),
                            onClick = { onQuickAdd(VaultItemType.NUMBER) }
                        )
                        QuickAddActionItem(
                            icon = Icons.Default.MoreHoriz,
                            iconColor = EmeraldPrimary,
                            label = "More",
                            modifier = Modifier.weight(1f),
                            onClick = onOpenMoreAdd
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Collections Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Collections",
                    color = VaultTextPrimary,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "See all",
                    color = EmeraldPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier
                        .clickable(onClick = onSeeAllCollections)
                        .padding(vertical = 4.dp, horizontal = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Collections Grid/Row
            val displayCollections = collections.take(4)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Two rows of 2 collections
                displayCollections.chunked(2).forEach { rowCollections ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        rowCollections.forEach { col ->
                            val itemCount = allItems.count { it.collectionId == col.id }
                            Surface(
                                onClick = { onSelectCollection(col) },
                                shape = RoundedCornerShape(12.dp),
                                color = VaultSurfaceDark,
                                border = BorderStroke(1.dp, VaultBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("collection_card_${col.id}")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = col.icon.ifBlank { "📁" },
                                        fontSize = 20.sp
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(
                                            text = col.name,
                                            color = VaultTextPrimary,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "$itemCount items",
                                            color = VaultTextSecondary,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                        if (rowCollections.size == 1) {
                            Spacer(modifier = Modifier.weight(1f))
                        }
                    }
                }
            }

            // Recent Section: ONLY show if there are recent items!
            if (recentItems.isNotEmpty()) {
                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 18.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Recent",
                        color = VaultTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "See all",
                        color = EmeraldPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier
                            .clickable(onClick = onSeeAllRecent)
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    recentItems.forEach { item ->
                        CompactVaultItemCard(
                            item = item,
                            onClick = { onInspectItem(item) },
                            onToggleFavorite = { onToggleFavorite(item) },
                            onEdit = { onInspectItem(item) },
                            onDelete = {},
                            onCopy = { valToCopy -> onQuickCopy(item.title, valToCopy) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun QuickAddActionItem(
    icon: ImageVector,
    iconColor: Color,
    label: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = VaultSurfaceElevated,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
            .padding(horizontal = 4.dp)
            .testTag("quick_add_${label.lowercase()}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp, horizontal = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                color = VaultTextPrimary,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}
