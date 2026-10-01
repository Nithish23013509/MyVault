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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.SecurityAudit
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
fun SecurityAuditDialog(
    audit: SecurityAudit,
    onDismiss: () -> Unit
) {
    val scoreColor = when {
        audit.healthScore >= 80 -> EmeraldPrimary
        audit.healthScore >= 50 -> AmberWarning
        else -> RoseDanger
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = VaultSurfaceDark,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Security,
                    contentDescription = null,
                    tint = scoreColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Vault Security Health",
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
                    .verticalScroll(rememberScrollState())
            ) {
                // Score Banner
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, VaultBorder, RoundedCornerShape(12.dp)),
                    colors = CardDefaults.cardColors(containerColor = VaultBgDark),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${audit.healthScore}%",
                            color = scoreColor,
                            fontSize = 38.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Text(
                            text = when {
                                audit.healthScore >= 80 -> "Security Status: Robust"
                                audit.healthScore >= 50 -> "Security Status: Needs Attention"
                                else -> "Security Status: Vulnerable"
                            },
                            color = scoreColor,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { audit.healthScore / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = scoreColor,
                            trackColor = VaultSurfaceElevated
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Stats Grid
                Text(
                    text = "AUDIT BREAKDOWN",
                    color = VaultTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                AuditStatRow(
                    label = "Total Vault Records",
                    value = "${audit.totalItems}",
                    statusColor = CyanAccent
                )
                AuditStatRow(
                    label = "Weak Passwords",
                    value = "${audit.weakPasswordsCount}",
                    statusColor = if (audit.weakPasswordsCount == 0) EmeraldPrimary else RoseDanger
                )
                AuditStatRow(
                    label = "Reused Passwords",
                    value = "${audit.reusedPasswordsCount}",
                    statusColor = if (audit.reusedPasswordsCount == 0) EmeraldPrimary else AmberWarning
                )
                AuditStatRow(
                    label = "Expiring Document IDs",
                    value = "${audit.expiringDocsCount}",
                    statusColor = if (audit.expiringDocsCount == 0) EmeraldPrimary else AmberWarning
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Recommendations
                Text(
                    text = "RECOMMENDATIONS",
                    color = VaultTextSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Spacer(modifier = Modifier.height(8.dp))

                for (rec in audit.recommendations) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = if (audit.healthScore >= 80) Icons.Default.CheckCircle else Icons.Default.Info,
                            contentDescription = null,
                            tint = if (audit.healthScore >= 80) EmeraldPrimary else AmberWarning,
                            modifier = Modifier
                                .size(16.dp)
                                .padding(top = 2.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = rec,
                            color = VaultTextPrimary,
                            fontSize = 13.sp,
                            lineHeight = 18.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary)
            ) {
                Text("Done", color = Color(0xFF003822), fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
private fun AuditStatRow(label: String, value: String, statusColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(VaultSurfaceElevated)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, color = VaultTextPrimary, fontSize = 13.sp)
        Text(
            text = value,
            color = statusColor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
