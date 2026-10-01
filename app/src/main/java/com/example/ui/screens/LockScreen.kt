package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Backspace
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
fun LockScreen(
    isSetupMode: Boolean,
    isBiometricAvailable: Boolean,
    configuredPinLength: Int = 4,
    onPinEntered: (String) -> Boolean,
    onSetupPin: (String) -> Unit,
    onTriggerBiometric: () -> Unit,
    onQuickUnlock: (() -> Unit)? = null,
    errorMessage: String? = null
) {
    var enteredPin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var isConfirmStep by remember { mutableStateOf(false) }
    var localError by remember { mutableStateOf<String?>(null) }
    var setupLength by remember { mutableIntStateOf(if (configuredPinLength in listOf(4, 6)) configuredPinLength else 4) }

    val focusRequester = remember { FocusRequester() }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    val currentTargetLength = when {
        isSetupMode && !isConfirmStep -> setupLength
        isSetupMode && isConfirmStep -> enteredPin.length
        else -> configuredPinLength
    }

    val currentPin = if (isSetupMode && isConfirmStep) confirmPin else enteredPin

    fun handleDigitPress(digit: String) {
        localError = null
        if (isSetupMode) {
            if (!isConfirmStep) {
                if (enteredPin.length < setupLength) {
                    val next = enteredPin + digit
                    enteredPin = next
                    if (next.length == setupLength) {
                        isConfirmStep = true
                    }
                }
            } else {
                if (confirmPin.length < enteredPin.length) {
                    val next = confirmPin + digit
                    confirmPin = next
                    if (next.length == enteredPin.length) {
                        if (next == enteredPin) {
                            onSetupPin(enteredPin)
                        } else {
                            localError = "PINs do not match. Please re-enter."
                            confirmPin = ""
                        }
                    }
                }
            }
        } else {
            if (enteredPin.length < currentTargetLength) {
                val next = enteredPin + digit
                enteredPin = next
                if (next.length == currentTargetLength) {
                    val success = onPinEntered(next)
                    if (!success) {
                        localError = "Incorrect PIN. Please try again."
                        enteredPin = ""
                    }
                }
            }
        }
    }

    fun handleDeletePress() {
        localError = null
        if (isSetupMode && isConfirmStep) {
            if (confirmPin.isNotEmpty()) {
                confirmPin = confirmPin.dropLast(1)
            }
        } else {
            if (enteredPin.isNotEmpty()) {
                enteredPin = enteredPin.dropLast(1)
            }
        }
    }

    fun handleClearPress() {
        localError = null
        if (isSetupMode && isConfirmStep) {
            confirmPin = ""
        } else {
            enteredPin = ""
        }
    }

    fun handleExplicitUnlock() {
        if (!isSetupMode && enteredPin.length >= 4) {
            val success = onPinEntered(enteredPin)
            if (!success) {
                localError = "Incorrect PIN. Please try again."
                enteredPin = ""
            }
        }
    }

    Surface(
        modifier = Modifier
            .fillMaxSize()
            .focusRequester(focusRequester)
            .focusable()
            .onKeyEvent { event ->
                if (event.type == KeyEventType.KeyUp) {
                    when (event.key) {
                        Key.Zero, Key.NumPad0 -> { handleDigitPress("0"); true }
                        Key.One, Key.NumPad1 -> { handleDigitPress("1"); true }
                        Key.Two, Key.NumPad2 -> { handleDigitPress("2"); true }
                        Key.Three, Key.NumPad3 -> { handleDigitPress("3"); true }
                        Key.Four, Key.NumPad4 -> { handleDigitPress("4"); true }
                        Key.Five, Key.NumPad5 -> { handleDigitPress("5"); true }
                        Key.Six, Key.NumPad6 -> { handleDigitPress("6"); true }
                        Key.Seven, Key.NumPad7 -> { handleDigitPress("7"); true }
                        Key.Eight, Key.NumPad8 -> { handleDigitPress("8"); true }
                        Key.Nine, Key.NumPad9 -> { handleDigitPress("9"); true }
                        Key.Backspace -> { handleDeletePress(); true }
                        Key.Enter, Key.NumPadEnter -> { handleExplicitUnlock(); true }
                        else -> false
                    }
                } else false
            }
            .testTag("lock_screen"),
        color = VaultBgDark
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 480.dp)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterVertically)
        ) {
            // Header Shield Emblem
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(top = 6.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(72.dp)
                        .scale(pulseScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    EmeraldPrimary.copy(alpha = 0.25f),
                                    Color.Transparent
                                )
                            )
                        )
                        .border(1.5.dp, Brush.linearGradient(listOf(EmeraldPrimary, CyanAccent)), CircleShape)
                ) {
                    Icon(
                        imageVector = if (isSetupMode) Icons.Default.VpnKey else Icons.Default.Lock,
                        contentDescription = "Security Vault Emblem",
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(36.dp)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text(
                    text = "MyVault",
                    color = VaultTextPrimary,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )

                Text(
                    text = when {
                        isSetupMode && !isConfirmStep -> "Set Master PIN ($setupLength digits) to protect vault"
                        isSetupMode && isConfirmStep -> "Re-enter PIN to confirm"
                        else -> "Protection Active • Enter Master PIN"
                    },
                    color = VaultTextSecondary,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )

                // Length toggle chips in Setup Mode (Step 1)
                if (isSetupMode && !isConfirmStep) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.padding(top = 10.dp)
                    ) {
                        PinLengthChip(
                            label = "4-Digit PIN",
                            selected = setupLength == 4,
                            onClick = {
                                setupLength = 4
                                enteredPin = ""
                            }
                        )
                        PinLengthChip(
                            label = "6-Digit PIN",
                            selected = setupLength == 6,
                            onClick = {
                                setupLength = 6
                                enteredPin = ""
                            }
                        )
                    }
                }

                // Protection status badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .padding(top = 8.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(VaultSurfaceElevated)
                        .border(1.dp, VaultBorder, RoundedCornerShape(20.dp))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = EmeraldPrimary,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "PROTECTION ACTIVE • ZERO KNOWLEDGE",
                        color = EmeraldPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                }
            }

            // PIN Indicator Dots
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.padding(vertical = 4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    for (i in 0 until currentTargetLength) {
                        val isFilled = i < currentPin.length
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isFilled) EmeraldPrimary else VaultSurfaceElevated
                                )
                                .border(
                                    width = 1.5.dp,
                                    color = if (isFilled) EmeraldPrimary else VaultBorder,
                                    shape = CircleShape
                                )
                        )
                    }
                }

                val activeError = localError ?: errorMessage
                AnimatedVisibility(visible = activeError != null) {
                    Text(
                        text = activeError ?: "",
                        color = RoseDanger,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier.padding(top = 8.dp, start = 12.dp, end = 12.dp)
                    )
                }
            }

            // Numeric Keypad
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val rows = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("LEFT_ACTION", "0", "DEL")
                )

                for (row in rows) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        for (key in row) {
                            when (key) {
                                "LEFT_ACTION" -> {
                                    if (!isSetupMode && isBiometricAvailable) {
                                        KeypadButton(
                                            content = {
                                                Icon(
                                                    imageVector = Icons.Default.Fingerprint,
                                                    contentDescription = "Unlock with Biometrics",
                                                    tint = CyanAccent,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            },
                                            onClick = onTriggerBiometric,
                                            testTag = "biometric_unlock_button"
                                        )
                                    } else {
                                        // Clear button for easy re-entry
                                        KeypadButton(
                                            content = {
                                                Text(
                                                    text = "C",
                                                    color = VaultTextSecondary,
                                                    fontSize = 18.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            },
                                            onClick = { handleClearPress() },
                                            testTag = "pin_clear_button"
                                        )
                                    }
                                }
                                "DEL" -> {
                                    KeypadButton(
                                        content = {
                                            Icon(
                                                imageVector = Icons.Default.Backspace,
                                                contentDescription = "Delete digit",
                                                tint = VaultTextSecondary,
                                                modifier = Modifier.size(22.dp)
                                            )
                                        },
                                        onClick = { handleDeletePress() },
                                        testTag = "pin_backspace_button"
                                    )
                                }
                                else -> {
                                    KeypadButton(
                                        content = {
                                            Text(
                                                text = key,
                                                color = VaultTextPrimary,
                                                fontSize = 22.sp,
                                                fontWeight = FontWeight.Bold,
                                                fontFamily = FontFamily.Monospace
                                            )
                                        },
                                        onClick = { handleDigitPress(key) },
                                        testTag = "pin_key_$key"
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Quick actions & confirmation buttons
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // If in setup mode and target length entered, allow advancing
                if (isSetupMode && !isConfirmStep && enteredPin.length == setupLength) {
                    Button(
                        onClick = { isConfirmStep = true },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .testTag("continue_pin_setup_button")
                    ) {
                        Text("Confirm $setupLength-digit PIN →", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                // If in setup mode and in confirm step, allow going back to edit
                if (isSetupMode && isConfirmStep) {
                    TextButton(
                        onClick = {
                            isConfirmStep = false
                            confirmPin = ""
                            localError = null
                        },
                        modifier = Modifier.testTag("back_to_enter_pin_button")
                    ) {
                        Text("← Re-enter first PIN", color = CyanAccent, fontSize = 12.sp)
                    }
                }

                // If in unlock mode and 4+ digits entered, offer an explicit unlock button
                if (!isSetupMode && enteredPin.length >= 4) {
                    Button(
                        onClick = { handleExplicitUnlock() },
                        colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .testTag("unlock_vault_button")
                    ) {
                        Icon(Icons.Default.Check, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Unlock Vault", color = Color.Black, fontWeight = FontWeight.Bold)
                    }
                }

                // Quick Unlock button for emulator testing & fast onboarding
                onQuickUnlock?.let { quickUnlockAction ->
                    OutlinedButton(
                        onClick = quickUnlockAction,
                        border = BorderStroke(1.dp, EmeraldPrimary.copy(alpha = 0.6f)),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.85f)
                            .testTag("quick_unlock_button")
                    ) {
                        Icon(Icons.Default.Bolt, contentDescription = null, tint = EmeraldPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isSetupMode) "Quick Setup (Default PIN: 1234)" else "Quick Unlock (PIN: 1234)",
                            color = EmeraldPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PinLengthChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(if (selected) EmeraldPrimary.copy(alpha = 0.2f) else VaultSurfaceElevated)
            .border(1.dp, if (selected) EmeraldPrimary else VaultBorder, RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = if (selected) EmeraldPrimary else VaultTextSecondary,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
        )
    }
}

@Composable
private fun KeypadButton(
    content: @Composable () -> Unit,
    onClick: () -> Unit,
    testTag: String
) {
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = VaultSurfaceDark,
        border = BorderStroke(1.dp, VaultBorder),
        shadowElevation = 2.dp,
        modifier = Modifier
            .size(64.dp)
            .testTag(testTag)
    ) {
        Box(contentAlignment = Alignment.Center) {
            content()
        }
    }
}
