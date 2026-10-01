package com.example.ui.screens

import android.app.Activity
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Folder
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pin
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.VaultViewModel
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.VaultBgDark
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurfaceDark
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: VaultViewModel,
    onOpenBackupDialog: () -> Unit,
    onOpenCloudDialog: () -> Unit,
    onOpenSecurityAudit: () -> Unit,
    onOpenCollections: () -> Unit
) {
    val appLock by viewModel.appLockEnabled.collectAsStateWithLifecycle()
    val biometric by viewModel.biometricEnabled.collectAsStateWithLifecycle()
    val autoLockTimeout by viewModel.autoLockTimeout.collectAsStateWithLifecycle()
    val clipboardTimeout by viewModel.clipboardTimeout.collectAsStateWithLifecycle()
    val sensitiveMasked by viewModel.sensitiveDataMasked.collectAsStateWithLifecycle()
    val revealAuth by viewModel.revealAuthRequired.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()
    val isSyncing by viewModel.isCloudSyncing.collectAsStateWithLifecycle()
    val autoCloudSync by viewModel.autoCloudSyncEnabled.collectAsStateWithLifecycle()
    val cloudSyncStatus by viewModel.cloudSyncStatus.collectAsStateWithLifecycle()

    var showChangePinDialog by remember { mutableStateOf(false) }
    var newPinInput by remember { mutableStateOf("") }
    var confirmNewPinInput by remember { mutableStateOf("") }
    var pinChangeError by remember { mutableStateOf<String?>(null) }
    var showAboutDialog by remember { mutableStateOf(false) }

    var autoLockMenuExpanded by remember { mutableStateOf(false) }
    var clipboardMenuExpanded by remember { mutableStateOf(false) }

    if (showChangePinDialog) {
        AlertDialog(
            onDismissRequest = {
                showChangePinDialog = false
                newPinInput = ""
                confirmNewPinInput = ""
                pinChangeError = null
            },
            containerColor = VaultSurfaceDark,
            title = { Text("Change Master PIN", color = VaultTextPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newPinInput,
                        onValueChange = {
                            newPinInput = it
                            pinChangeError = null
                        },
                        label = { Text("New PIN (4–6 digits)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = VaultBorder,
                            focusedTextColor = VaultTextPrimary,
                            unfocusedTextColor = VaultTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = confirmNewPinInput,
                        onValueChange = {
                            confirmNewPinInput = it
                            pinChangeError = null
                        },
                        label = { Text("Confirm New PIN") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = VaultBorder,
                            focusedTextColor = VaultTextPrimary,
                            unfocusedTextColor = VaultTextPrimary
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    pinChangeError?.let {
                        Text(it, color = RoseDanger, fontSize = 12.sp)
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newPinInput.length < 4) {
                            pinChangeError = "PIN must be at least 4 digits"
                            return@Button
                        }
                        if (newPinInput != confirmNewPinInput) {
                            pinChangeError = "PINs do not match"
                            return@Button
                        }
                        viewModel.setupMasterPin(newPinInput)
                        showChangePinDialog = false
                        newPinInput = ""
                        confirmNewPinInput = ""
                        pinChangeError = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Update PIN", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showChangePinDialog = false }) {
                    Text("Cancel", color = VaultTextSecondary)
                }
            }
        )
    }

    if (showAboutDialog) {
        AlertDialog(
            onDismissRequest = { showAboutDialog = false },
            containerColor = VaultSurfaceDark,
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Shield, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(24.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("About MyVault", color = VaultTextPrimary, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("MyVault Version 1.0", color = EmeraldPrimary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "MyVault is a private, zero-knowledge vault designed to protect credentials, secure notes, contact records, and structured cards.",
                        color = VaultTextSecondary,
                        fontSize = 13.sp
                    )
                    Text(
                        "• All information is encrypted client-side using device protection.\n• Cloud sync uploads strictly encrypted payloads without plaintext data.\n• Offline-first with local backup and restore.",
                        color = VaultTextSecondary,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showAboutDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("OK", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VaultBgDark)
            .padding(horizontal = 16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("settings_screen"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Title Header
        Text(
            text = "Settings",
            color = VaultTextPrimary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 8.dp)
        )

        // SECTION 1: SECURITY
        SettingsSectionCard(title = "SECURITY") {
            SettingsToggleRow(
                icon = Icons.Default.Lock,
                title = "App Lock",
                subtitle = "Require PIN or biometric verification to open vault",
                checked = appLock,
                onCheckedChange = { viewModel.setAppLock(it) }
            )

            HorizontalDivider(color = VaultBorder)

            SettingsToggleRow(
                icon = Icons.Default.Fingerprint,
                title = "Biometric Unlock",
                subtitle = "Use fingerprint or face recognition for fast unlock",
                checked = biometric,
                onCheckedChange = { viewModel.setBiometric(it) }
            )

            HorizontalDivider(color = VaultBorder)

            SettingsActionRow(
                icon = Icons.Default.Pin,
                title = "Change Master PIN",
                subtitle = "Update your 4–6 digit master vault security code",
                onClick = { showChangePinDialog = true }
            )

            HorizontalDivider(color = VaultBorder)

            // Auto Lock Timeout Dropdown
            val timeoutLabels = mapOf(
                0 to "Immediately upon exit",
                30 to "After 30 seconds",
                60 to "After 1 minute",
                300 to "After 5 minutes",
                -1 to "Never"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Lock, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Auto Lock", color = VaultTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(timeoutLabels[autoLockTimeout] ?: "After 30 seconds", color = VaultTextSecondary, fontSize = 12.sp)
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = autoLockMenuExpanded,
                    onExpandedChange = { autoLockMenuExpanded = it }
                ) {
                    TextButton(
                        onClick = { autoLockMenuExpanded = true },
                        modifier = Modifier.menuAnchor()
                    ) {
                        Text("Change", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                    }
                    ExposedDropdownMenu(
                        expanded = autoLockMenuExpanded,
                        onDismissRequest = { autoLockMenuExpanded = false },
                        modifier = Modifier.background(VaultSurfaceElevated)
                    ) {
                        timeoutLabels.forEach { (sec, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = VaultTextPrimary) },
                                onClick = {
                                    viewModel.setAutoLockTimeout(sec)
                                    autoLockMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // SECTION 2: PRIVACY
        SettingsSectionCard(title = "PRIVACY") {
            SettingsToggleRow(
                icon = Icons.Default.Shield,
                title = "Sensitive Data Masking",
                subtitle = "Mask confidential numbers and passwords by default",
                checked = sensitiveMasked,
                onCheckedChange = { viewModel.setSensitiveDataMasked(it) }
            )

            HorizontalDivider(color = VaultBorder)

            SettingsToggleRow(
                icon = Icons.Default.Visibility,
                title = "Reveal Authentication",
                subtitle = "Prompt Master PIN or biometric before unmasking secrets",
                checked = revealAuth,
                onCheckedChange = { viewModel.setRevealAuthRequired(it) }
            )

            HorizontalDivider(color = VaultBorder)

            // Clipboard Protection Dropdown
            val clipLabels = mapOf(
                15 to "Auto-clear in 15 seconds",
                30 to "Auto-clear in 30 seconds",
                60 to "Auto-clear in 1 minute",
                0 to "Never auto-clear"
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Default.Security, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Clipboard Protection", color = VaultTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(clipLabels[clipboardTimeout] ?: "Auto-clear in 30 seconds", color = VaultTextSecondary, fontSize = 12.sp)
                    }
                }

                ExposedDropdownMenuBox(
                    expanded = clipboardMenuExpanded,
                    onExpandedChange = { clipboardMenuExpanded = it }
                ) {
                    TextButton(
                        onClick = { clipboardMenuExpanded = true },
                        modifier = Modifier.menuAnchor()
                    ) {
                        Text("Change", color = EmeraldPrimary, fontWeight = FontWeight.Bold)
                    }
                    ExposedDropdownMenu(
                        expanded = clipboardMenuExpanded,
                        onDismissRequest = { clipboardMenuExpanded = false },
                        modifier = Modifier.background(VaultSurfaceElevated)
                    ) {
                        clipLabels.forEach { (sec, label) ->
                            DropdownMenuItem(
                                text = { Text(label, color = VaultTextPrimary) },
                                onClick = {
                                    viewModel.setClipboardTimeout(sec)
                                    clipboardMenuExpanded = false
                                }
                            )
                        }
                    }
                }
            }
        }

        // SECTION 3: DATA MANAGEMENT
        SettingsSectionCard(title = "DATA MANAGEMENT") {
            // Cloud Sync Row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(
                        imageVector = if (currentUser != null) Icons.Default.CloudDone else Icons.Default.CloudSync,
                        contentDescription = null,
                        tint = if (currentUser != null) EmeraldPrimary else CyanAccent,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Cloud Sync", color = VaultTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(
                            text = if (currentUser != null) {
                                if (autoCloudSync) "☁ Auto-Sync Active • ${currentUser?.email ?: "Connected"}"
                                else "☁ Connected • Auto-Sync Paused"
                            } else "Offline • Tap to connect",
                            color = if (currentUser != null) EmeraldPrimary else VaultTextSecondary,
                            fontSize = 12.sp
                        )
                        if (cloudSyncStatus != null && currentUser != null) {
                            Text(
                                text = cloudSyncStatus!!,
                                color = CyanAccent,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                Button(
                    onClick = onOpenCloudDialog,
                    colors = ButtonDefaults.buttonColors(containerColor = VaultSurfaceElevated),
                    border = BorderStroke(1.dp, VaultBorder),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(if (currentUser != null) "Manage" else "Connect", color = VaultTextPrimary, fontSize = 12.sp)
                }
            }

            if (currentUser != null) {
                HorizontalDivider(color = VaultBorder)

                SettingsToggleRow(
                    icon = Icons.Default.Sync,
                    title = "Automatic Cloud Sync",
                    subtitle = "Automatically encrypts & syncs additions, edits, and favorites to Cloud Firestore in real time",
                    checked = autoCloudSync,
                    onCheckedChange = { viewModel.setAutoCloudSync(it) }
                )

                HorizontalDivider(color = VaultBorder)

                SettingsActionRow(
                    icon = Icons.Default.CloudSync,
                    title = "Sync Vault Now",
                    subtitle = if (isSyncing) "Synchronizing Zero-Knowledge items..." else "Perform immediate bidirectional cloud synchronization",
                    onClick = { viewModel.triggerAutoSync(isUserInitiated = true) }
                )
            }

            HorizontalDivider(color = VaultBorder)

            // Local Backup & Restore
            SettingsActionRow(
                icon = Icons.Default.Save,
                title = "Export Local Backup",
                subtitle = "Create a password-encrypted backup file on your device",
                onClick = onOpenBackupDialog
            )

            HorizontalDivider(color = VaultBorder)

            SettingsActionRow(
                icon = Icons.Default.Restore,
                title = "Restore from Backup",
                subtitle = "Import your encrypted backup file to recover items",
                onClick = onOpenBackupDialog
            )
        }

        // SECTION 4: ORGANIZATION
        SettingsSectionCard(title = "ORGANIZATION") {
            SettingsActionRow(
                icon = Icons.Default.Folder,
                title = "Collections Manager",
                subtitle = "Personal, College, Work, Projects & custom groups",
                onClick = onOpenCollections
            )

            HorizontalDivider(color = VaultBorder)

            SettingsActionRow(
                icon = Icons.Default.Security,
                title = "Security Health Score",
                subtitle = "Audit weak, reused, or expired credentials",
                onClick = onOpenSecurityAudit
            )
        }

        // SECTION 5: APP & ABOUT
        SettingsSectionCard(title = "APP") {
            SettingsActionRow(
                icon = Icons.Default.Info,
                title = "About MyVault",
                subtitle = "Version 1.0 • Zero-Knowledge Encryption Shield",
                onClick = { showAboutDialog = true }
            )
        }

        Spacer(modifier = Modifier.height(80.dp))
    }
}

@Composable
private fun SettingsSectionCard(
    title: String,
    content: @Composable () -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(
            text = title,
            color = EmeraldPrimary,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp)
        )
        Card(
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = VaultSurfaceDark),
            border = BorderStroke(1.dp, VaultBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            ) {
                content()
            }
        }
    }
}

@Composable
private fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = VaultTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = VaultTextSecondary, fontSize = 12.sp)
            }
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.Black,
                checkedTrackColor = EmeraldPrimary,
                uncheckedTrackColor = VaultSurfaceElevated
            )
        )
    }
}

@Composable
private fun SettingsActionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = CyanAccent, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, color = VaultTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = VaultTextSecondary, fontSize = 12.sp)
            }
        }

        Icon(Icons.AutoMirrored.Filled.ArrowForward, contentDescription = null, tint = VaultTextSecondary.copy(alpha = 0.5f), modifier = Modifier.size(16.dp))
    }
}
