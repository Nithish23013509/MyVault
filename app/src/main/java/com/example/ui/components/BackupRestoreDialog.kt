package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.VaultBgDark
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurfaceDark
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary

@Composable
fun BackupRestoreDialog(
    onExport: (passphrase: String, onComplete: (Boolean, String) -> Unit) -> Unit,
    onImport: (backupData: String, passphrase: String, onComplete: (Boolean, String) -> Unit) -> Unit,
    onCopyBackup: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) } // 0: Export, 1: Import
    var passphrase by remember { mutableStateOf("") }
    var importData by remember { mutableStateOf("") }
    var exportedResult by remember { mutableStateOf<String?>(null) }
    var statusText by remember { mutableStateOf<String?>(null) }
    var isError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurfaceDark,
        title = {
            Text(
                text = "Encrypted Vault Backup",
                color = VaultTextPrimary,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                TabRow(
                    selectedTabIndex = selectedTab,
                    containerColor = VaultSurfaceDark,
                    contentColor = EmeraldPrimary,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                            color = EmeraldPrimary
                        )
                    }
                ) {
                    Tab(
                        selected = selectedTab == 0,
                        onClick = {
                            selectedTab = 0
                            statusText = null
                        },
                        text = { Text("Export Vault") },
                        icon = { Icon(Icons.Default.Upload, contentDescription = null) }
                    )
                    Tab(
                        selected = selectedTab == 1,
                        onClick = {
                            selectedTab = 1
                            statusText = null
                        },
                        text = { Text("Restore / Import") },
                        icon = { Icon(Icons.Default.Download, contentDescription = null) }
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                if (selectedTab == 0) {
                    // Export flow
                    Text(
                        text = "Create an offline shielded backup file protected by your custom passphrase.",
                        color = VaultTextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Backup Protection Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = VaultBorder,
                            focusedTextColor = VaultTextPrimary,
                            unfocusedTextColor = VaultTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("backup_passphrase_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (passphrase.length < 6) {
                                isError = true
                                statusText = "Passphrase must be at least 6 characters"
                                return@Button
                            }
                            onExport(passphrase) { success, result ->
                                if (success) {
                                    exportedResult = result
                                    statusText = "Backup encrypted and generated!"
                                    isError = false
                                } else {
                                    isError = true
                                    statusText = result
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("generate_backup_button")
                    ) {
                        Icon(Icons.Default.VpnKey, contentDescription = null, tint = Color(0xFF003822))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Generate Protected Backup", color = Color(0xFF003822), fontWeight = FontWeight.Bold)
                    }

                    if (exportedResult != null) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .border(1.dp, VaultBorder, RoundedCornerShape(8.dp)),
                            colors = CardDefaults.cardColors(containerColor = VaultBgDark)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Encrypted Backup Payload",
                                        color = CyanAccent,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    IconButton(
                                        onClick = { onCopyBackup(exportedResult!!) },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            Icons.Default.ContentCopy,
                                            contentDescription = "Copy payload",
                                            tint = EmeraldPrimary
                                        )
                                    }
                                }
                                Text(
                                    text = exportedResult!!.take(200) + "... [Full length: ${exportedResult!!.length} chars]",
                                    color = VaultTextSecondary,
                                    fontSize = 11.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }
                        }
                    }
                } else {
                    // Import flow
                    Text(
                        text = "Paste your encrypted backup payload and enter the passphrase used during export.",
                        color = VaultTextSecondary,
                        fontSize = 13.sp
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedTextField(
                        value = importData,
                        onValueChange = { importData = it },
                        label = { Text("Encrypted Backup String / JSON") },
                        maxLines = 4,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = VaultBorder,
                            focusedTextColor = VaultTextPrimary,
                            unfocusedTextColor = VaultTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_backup_data_input")
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = passphrase,
                        onValueChange = { passphrase = it },
                        label = { Text("Backup Passphrase") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = EmeraldPrimary,
                            unfocusedBorderColor = VaultBorder,
                            focusedTextColor = VaultTextPrimary,
                            unfocusedTextColor = VaultTextPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("import_passphrase_input")
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = {
                            if (importData.isBlank() || passphrase.isBlank()) {
                                isError = true
                                statusText = "Please fill in both payload and passphrase"
                                return@Button
                            }
                            onImport(importData, passphrase) { success, msg ->
                                isError = !success
                                statusText = msg
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("restore_backup_button")
                    ) {
                        Text("Decrypt & Restore Items", color = Color(0xFF00363D), fontWeight = FontWeight.Bold)
                    }
                }

                if (statusText != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = statusText!!,
                        color = if (isError) RoseDanger else EmeraldPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = VaultTextSecondary)
            }
        }
    )
}
