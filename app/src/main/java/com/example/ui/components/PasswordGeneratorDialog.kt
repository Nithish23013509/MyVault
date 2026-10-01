package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.PasswordStrengthCalculator
import com.example.ui.PasswordGenConfig
import com.example.ui.theme.AmberWarning
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
fun PasswordGeneratorDialog(
    generatedPassword: String,
    config: PasswordGenConfig,
    onConfigChange: (PasswordGenConfig) -> Unit,
    onRegenerate: () -> Unit,
    onCopy: (String) -> Unit,
    onSelectPassword: ((String) -> Unit)? = null,
    onDismiss: () -> Unit
) {
    val strength = remember(generatedPassword) {
        PasswordStrengthCalculator.evaluate(generatedPassword)
    }

    val strengthColor = when (strength.level) {
        PasswordStrengthCalculator.StrengthLevel.VERY_WEAK -> RoseDanger
        PasswordStrengthCalculator.StrengthLevel.WEAK -> RoseDanger
        PasswordStrengthCalculator.StrengthLevel.FAIR -> AmberWarning
        PasswordStrengthCalculator.StrengthLevel.STRONG -> CyanAccent
        PasswordStrengthCalculator.StrengthLevel.VERY_STRONG -> EmeraldPrimary
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Password Generator",
                    color = VaultTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                // Generated Password Box
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VaultBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = VaultBgDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = generatedPassword,
                                color = VaultTextPrimary,
                                fontSize = 16.sp,
                                fontFamily = FontFamily.Monospace,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(end = 8.dp)
                            )
                            Row {
                                IconButton(
                                    onClick = onRegenerate,
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("regenerate_password_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Refresh,
                                        contentDescription = "Regenerate",
                                        tint = CyanAccent
                                    )
                                }
                                IconButton(
                                    onClick = { onCopy(generatedPassword) },
                                    modifier = Modifier
                                        .size(36.dp)
                                        .testTag("copy_generated_password_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Copy password",
                                        tint = EmeraldPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Strength meter
                        LinearProgressIndicator(
                            progress = { (strength.score + 1) / 5f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = strengthColor,
                            trackColor = VaultSurfaceElevated
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${strength.level.name.replace("_", " ")}",
                                color = strengthColor,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "%.1f bits entropy".format(strength.entropyBits),
                                color = VaultTextSecondary,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Length slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Length: ${config.length}",
                        color = VaultTextPrimary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Slider(
                    value = config.length.toFloat(),
                    onValueChange = { onConfigChange(config.copy(length = it.toInt())) },
                    valueRange = 8f..36f,
                    steps = 27,
                    colors = SliderDefaults.colors(
                        thumbColor = EmeraldPrimary,
                        activeTrackColor = EmeraldPrimary,
                        inactiveTrackColor = VaultSurfaceElevated
                    ),
                    modifier = Modifier.testTag("password_length_slider")
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Options toggles
                GeneratorToggleRow("Uppercase (A-Z)", config.useUpper) {
                    onConfigChange(config.copy(useUpper = it))
                }
                GeneratorToggleRow("Lowercase (a-z)", config.useLower) {
                    onConfigChange(config.copy(useLower = it))
                }
                GeneratorToggleRow("Numbers (0-9)", config.useDigits) {
                    onConfigChange(config.copy(useDigits = it))
                }
                GeneratorToggleRow("Special Symbols (!@#$)", config.useSymbols) {
                    onConfigChange(config.copy(useSymbols = it))
                }
                GeneratorToggleRow("Avoid Ambiguous (0, O, 1, l)", config.avoidAmbiguous) {
                    onConfigChange(config.copy(avoidAmbiguous = it))
                }
            }
        },
        confirmButton = {
            if (onSelectPassword != null) {
                Button(
                    onClick = {
                        onSelectPassword(generatedPassword)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                    modifier = Modifier.testTag("use_password_button")
                ) {
                    Text("Use Password", color = Color(0xFF003822), fontWeight = FontWeight.Bold)
                }
            } else {
                Button(
                    onClick = {
                        onCopy(generatedPassword)
                        onDismiss()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
                ) {
                    Text("Copy & Close", color = Color(0xFF003822), fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = VaultTextSecondary)
            }
        }
    )
}

@Composable
private fun GeneratorToggleRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = title, color = VaultTextSecondary, fontSize = 13.sp)
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = EmeraldPrimary,
                checkedTrackColor = Color(0xFF064E3B),
                uncheckedThumbColor = VaultTextSecondary,
                uncheckedTrackColor = VaultSurfaceElevated
            ),
            modifier = Modifier.size(width = 44.dp, height = 24.dp)
        )
    }
}
