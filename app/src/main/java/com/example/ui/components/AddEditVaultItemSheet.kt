package com.example.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Link
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Note
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultCollection
import com.example.data.VaultItem
import com.example.data.VaultItemType
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

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AddEditVaultItemSheet(
    initialItem: VaultItem? = null,
    defaultType: VaultItemType = VaultItemType.LINK,
    collections: List<VaultCollection>,
    onSave: (VaultItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var name by remember { mutableStateOf(initialItem?.title ?: "") }
    var type by remember { mutableStateOf(initialItem?.type ?: defaultType) }
    var value by remember { mutableStateOf(initialItem?.effectiveValue ?: "") }
    var customFieldName by remember { mutableStateOf(initialItem?.username.orEmpty()) }
    var selectedCollection by remember {
        mutableStateOf(initialItem?.collectionId ?: collections.firstOrNull()?.id ?: "personal")
    }
    var notes by remember { mutableStateOf(initialItem?.notes ?: "") }

    var isFavorite by remember { mutableStateOf(initialItem?.effectiveFavorite ?: false) }
    var isPinned by remember { mutableStateOf(initialItem?.pinned ?: false) }
    var isSensitive by remember { mutableStateOf(initialItem?.sensitive ?: (type == VaultItemType.PASSWORD || type == VaultItemType.CUSTOM)) }

    var passwordVisible by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    var typeDropdownExpanded by remember { mutableStateOf(false) }
    var collectionDropdownExpanded by remember { mutableStateOf(false) }

    var tagInput by remember { mutableStateOf("") }
    val tagsList = remember {
        mutableStateListOf<String>().apply {
            if (!initialItem?.tags.isNullOrBlank()) {
                addAll(initialItem!!.tags.split(",").map { it.trim() }.filter { it.isNotEmpty() })
            }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = VaultSurfaceDark,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 32.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (initialItem == null) "Add Item" else "Edit Item",
                    color = VaultTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = VaultTextSecondary)
                }
            }

            // 1. Name Field
            OutlinedTextField(
                value = name,
                onValueChange = {
                    name = it
                    validationError = null
                },
                label = { Text("Name") },
                placeholder = { Text("e.g. GitHub, College Portal, Mom") },
                singleLine = true,
                colors = outlinedFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("add_item_name_input")
            )

            // 2. Type Dropdown
            ExposedDropdownMenuBox(
                expanded = typeDropdownExpanded,
                onExpandedChange = { typeDropdownExpanded = it }
            ) {
                OutlinedTextField(
                    value = type.displayName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = typeDropdownExpanded) },
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("add_item_type_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = typeDropdownExpanded,
                    onDismissRequest = { typeDropdownExpanded = false },
                    modifier = Modifier.background(VaultSurfaceElevated)
                ) {
                    val selectableTypes = listOf(
                        VaultItemType.LINK,
                        VaultItemType.PHONE,
                        VaultItemType.EMAIL,
                        VaultItemType.TEXT,
                        VaultItemType.NUMBER,
                        VaultItemType.CUSTOM,
                        VaultItemType.PASSWORD
                    )
                    selectableTypes.forEach { itemType ->
                        DropdownMenuItem(
                            text = { Text(itemType.displayName, color = VaultTextPrimary) },
                            onClick = {
                                type = itemType
                                typeDropdownExpanded = false
                                if (itemType == VaultItemType.PASSWORD || itemType == VaultItemType.CUSTOM) {
                                    isSensitive = true
                                }
                            }
                        )
                    }
                }
            }

            // 3. Dynamic Value Field based on Type
            when (type) {
                VaultItemType.LINK -> {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Value") },
                        placeholder = { Text("https://example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_item_value_input")
                    )
                }
                VaultItemType.PHONE -> {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+1 (555) 012-3456") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_item_value_input")
                    )
                }
                VaultItemType.EMAIL -> {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Email Address") },
                        placeholder = { Text("user@example.com") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_item_value_input")
                    )
                }
                VaultItemType.NUMBER -> {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Number") },
                        placeholder = { Text("12345678") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_item_value_input")
                    )
                }
                VaultItemType.CUSTOM -> {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = customFieldName,
                            onValueChange = { customFieldName = it },
                            label = { Text("Field Type / Label") },
                            placeholder = { Text("e.g. Aadhaar Number, License Key") },
                            singleLine = true,
                            colors = outlinedFieldColors(),
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = value,
                            onValueChange = { value = it },
                            label = { Text("Value") },
                            placeholder = { Text("Secret value") },
                            singleLine = true,
                            visualTransformation = if (isSensitive && !passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                            trailingIcon = {
                                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                    Icon(
                                        if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                        contentDescription = null,
                                        tint = VaultTextSecondary
                                    )
                                }
                            },
                            colors = outlinedFieldColors(),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("add_item_value_input")
                        )
                    }
                }
                VaultItemType.PASSWORD -> {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Password") },
                        singleLine = true,
                        visualTransformation = if (!passwordVisible) PasswordVisualTransformation() else VisualTransformation.None,
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = null,
                                    tint = VaultTextSecondary
                                )
                            }
                        },
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_item_value_input")
                    )
                }
                else -> {
                    OutlinedTextField(
                        value = value,
                        onValueChange = { value = it },
                        label = { Text("Value / Content") },
                        singleLine = false,
                        maxLines = 3,
                        colors = outlinedFieldColors(),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_item_value_input")
                    )
                }
            }

            // 4. Collection Dropdown
            ExposedDropdownMenuBox(
                expanded = collectionDropdownExpanded,
                onExpandedChange = { collectionDropdownExpanded = it }
            ) {
                val currentCollectionName = collections.find { it.id == selectedCollection }?.name
                    ?: selectedCollection.replaceFirstChar { it.uppercase() }
                OutlinedTextField(
                    value = currentCollectionName,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Collection") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = collectionDropdownExpanded) },
                    colors = outlinedFieldColors(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor()
                        .testTag("add_item_collection_dropdown")
                )
                ExposedDropdownMenu(
                    expanded = collectionDropdownExpanded,
                    onDismissRequest = { collectionDropdownExpanded = false },
                    modifier = Modifier.background(VaultSurfaceElevated)
                ) {
                    collections.forEach { col ->
                        DropdownMenuItem(
                            text = { Text("${col.icon}  ${col.name}", color = VaultTextPrimary) },
                            onClick = {
                                selectedCollection = col.id
                                collectionDropdownExpanded = false
                            }
                        )
                    }
                }
            }

            // 5. Tags
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Tags", color = VaultTextSecondary, fontSize = 12.sp)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    tagsList.forEach { tag ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(VaultSurfaceElevated)
                                .border(1.dp, VaultBorder, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(tag, color = VaultTextPrimary, fontSize = 12.sp)
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Close,
                                contentDescription = "Remove tag",
                                tint = VaultTextSecondary,
                                modifier = Modifier
                                    .size(14.dp)
                                    .clickable { tagsList.remove(tag) }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = tagInput,
                        onValueChange = { tagInput = it },
                        placeholder = { Text("Add tag...") },
                        singleLine = true,
                        colors = outlinedFieldColors(),
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(
                        onClick = {
                            if (tagInput.isNotBlank() && !tagsList.contains(tagInput.trim())) {
                                tagsList.add(tagInput.trim())
                                tagInput = ""
                            }
                        },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(EmeraldPrimary.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Add tag", tint = EmeraldPrimary)
                    }
                }
            }

            // 6. Notes
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text("Notes") },
                placeholder = { Text("Additional confidential details...") },
                minLines = 2,
                maxLines = 4,
                colors = outlinedFieldColors(),
                modifier = Modifier.fillMaxWidth()
            )

            // 7. Checkboxes: Favorite, Pin, Sensitive
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isFavorite = !isFavorite }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Favorite", color = VaultTextPrimary, fontSize = 14.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isPinned = !isPinned }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isPinned,
                        onCheckedChange = { isPinned = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Pin to Top", color = VaultTextPrimary, fontSize = 14.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { isSensitive = !isSensitive }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isSensitive,
                        onCheckedChange = { isSensitive = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Sensitive (Masked by default)", color = VaultTextPrimary, fontSize = 14.sp)
                }
            }

            validationError?.let { err ->
                Text(err, color = RoseDanger, fontSize = 12.sp)
            }

            // Save Button
            Button(
                onClick = {
                    if (name.isBlank()) {
                        validationError = "Please enter a name"
                        return@Button
                    }
                    val itemToSave = VaultItem(
                        id = initialItem?.id ?: 0L,
                        type = type,
                        title = name.trim(),
                        value = value.trim(),
                        collectionId = selectedCollection,
                        categoryTag = selectedCollection.replaceFirstChar { it.uppercase() },
                        tags = tagsList.joinToString(", "),
                        notes = notes.trim(),
                        favorite = isFavorite,
                        isFavorite = isFavorite,
                        pinned = isPinned,
                        sensitive = isSensitive,
                        username = customFieldName.ifBlank { if (type == VaultItemType.PASSWORD) name.trim() else "" },
                        password = if (type == VaultItemType.PASSWORD) value else "",
                        url = if (type == VaultItemType.LINK) value else "",
                        createdAt = initialItem?.createdAt ?: System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    onSave(itemToSave)
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = EmeraldPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("save_vault_item_button")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
private fun outlinedFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedBorderColor = EmeraldPrimary,
    unfocusedBorderColor = VaultBorder,
    focusedTextColor = VaultTextPrimary,
    unfocusedTextColor = VaultTextPrimary,
    focusedLabelColor = EmeraldPrimary,
    unfocusedLabelColor = VaultTextSecondary,
    cursorColor = EmeraldPrimary
)
