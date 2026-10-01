package com.example.ui.components

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.VaultCollection
import com.example.data.VaultItem
import com.example.data.VaultItemType
import com.example.ui.RecordTemplate
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.EmeraldPrimary
import com.example.ui.theme.PurpleDoc
import com.example.ui.theme.RoseDanger
import com.example.ui.theme.VaultBorder
import com.example.ui.theme.VaultSurfaceDark
import com.example.ui.theme.VaultSurfaceElevated
import com.example.ui.theme.VaultTextPrimary
import com.example.ui.theme.VaultTextSecondary
import org.json.JSONArray
import org.json.JSONObject

data class EditableField(
    var id: Long = System.currentTimeMillis() + (0..1000).random(),
    var name: String = "",
    var type: String = "text",
    var value: String = "",
    var sensitive: Boolean = false
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun MultiFieldRecordSheet(
    initialItem: VaultItem? = null,
    collections: List<VaultCollection>,
    templates: List<RecordTemplate>,
    onSave: (VaultItem) -> Unit,
    onDismiss: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var recordName by remember { mutableStateOf(initialItem?.title ?: "") }
    var selectedCollection by remember {
        mutableStateOf(initialItem?.collectionId ?: collections.firstOrNull()?.id ?: "personal")
    }

    var collectionDropdownExpanded by remember { mutableStateOf(false) }

    val fieldsList = remember {
        mutableStateListOf<EditableField>().apply {
            if (initialItem != null && initialItem.value.startsWith("[")) {
                try {
                    val array = JSONArray(initialItem.value)
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        add(
                            EditableField(
                                name = obj.optString("name", "Field"),
                                type = obj.optString("type", "text"),
                                value = obj.optString("value", ""),
                                sensitive = obj.optBoolean("sensitive", false)
                            )
                        )
                    }
                } catch (ignored: Exception) {
                    add(EditableField(name = "Detail", value = initialItem.effectiveValue))
                }
            } else {
                add(EditableField(name = "Field 1", value = ""))
            }
        }
    }

    var isFavorite by remember { mutableStateOf(initialItem?.effectiveFavorite ?: false) }
    var isSensitive by remember { mutableStateOf(initialItem?.sensitive ?: true) }
    var validationError by remember { mutableStateOf<String?>(null) }

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
                    text = if (initialItem == null) "New Record" else "Edit Record",
                    color = VaultTextPrimary,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = VaultTextSecondary)
                }
            }

            // Templates (Only on Create mode)
            if (initialItem == null) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Choose a template", color = VaultTextSecondary, fontSize = 12.sp)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        templates.forEach { tmpl ->
                            Card(
                                shape = RoundedCornerShape(10.dp),
                                colors = CardDefaults.cardColors(containerColor = VaultSurfaceElevated),
                                border = BorderStroke(1.dp, VaultBorder),
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        recordName = tmpl.name
                                        fieldsList.clear()
                                        tmpl.fields.forEach { (fname, ftype) ->
                                            fieldsList.add(
                                                EditableField(
                                                    name = fname,
                                                    type = ftype,
                                                    value = "",
                                                    sensitive = fname.contains("Number", ignoreCase = true) || fname.contains("ID", ignoreCase = true)
                                                )
                                            )
                                        }
                                    }
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Text("${tmpl.icon} ${tmpl.name}", color = VaultTextPrimary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                                    Text("${tmpl.fieldCount} fields", color = VaultTextSecondary, fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }

            // Record Name
            OutlinedTextField(
                value = recordName,
                onValueChange = {
                    recordName = it
                    validationError = null
                },
                label = { Text("Record Name") },
                placeholder = { Text("e.g. Driving Licence, Vehicle Docs, College Profile") },
                singleLine = true,
                colors = outlinedFieldColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("record_name_input")
            )

            // Collection Dropdown
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

            // Fields Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Fields", color = VaultTextPrimary, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    text = "+ Add Field",
                    color = EmeraldPrimary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier
                        .clickable {
                            fieldsList.add(EditableField(name = "Field ${fieldsList.size + 1}", value = ""))
                        }
                        .padding(4.dp)
                )
            }

            // List of structured fields
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                fieldsList.forEachIndexed { index, field ->
                    Card(
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = VaultSurfaceElevated),
                        border = BorderStroke(1.dp, VaultBorder),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = field.name,
                                    onValueChange = { newName ->
                                        fieldsList[index] = field.copy(name = newName)
                                    },
                                    label = { Text("Field Name", fontSize = 11.sp) },
                                    singleLine = true,
                                    colors = outlinedFieldColors(),
                                    modifier = Modifier.weight(1f)
                                )

                                Spacer(modifier = Modifier.width(8.dp))

                                // Sensitive toggle
                                IconButton(
                                    onClick = {
                                        fieldsList[index] = field.copy(sensitive = !field.sensitive)
                                    }
                                ) {
                                    Icon(
                                        imageVector = if (field.sensitive) Icons.Default.Lock else Icons.Default.Visibility,
                                        contentDescription = "Toggle sensitive",
                                        tint = if (field.sensitive) EmeraldPrimary else VaultTextSecondary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                if (fieldsList.size > 1) {
                                    IconButton(
                                        onClick = { fieldsList.removeAt(index) }
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete field", tint = RoseDanger, modifier = Modifier.size(20.dp))
                                    }
                                }
                            }

                            var showVal by remember { mutableStateOf(!field.sensitive) }
                            OutlinedTextField(
                                value = field.value,
                                onValueChange = { newVal ->
                                    fieldsList[index] = field.copy(value = newVal)
                                },
                                label = { Text("Value", fontSize = 11.sp) },
                                singleLine = true,
                                visualTransformation = if (field.sensitive && !showVal) PasswordVisualTransformation() else VisualTransformation.None,
                                trailingIcon = {
                                    if (field.sensitive) {
                                        IconButton(onClick = { showVal = !showVal }) {
                                            Icon(
                                                imageVector = if (showVal) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                                contentDescription = null,
                                                tint = VaultTextSecondary
                                            )
                                        }
                                    }
                                },
                                colors = outlinedFieldColors(),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Tags
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

            // Checkboxes: Favorite, Sensitive
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { isFavorite = !isFavorite }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isFavorite,
                        onCheckedChange = { isFavorite = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Favorite", color = VaultTextPrimary, fontSize = 14.sp)
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clickable { isSensitive = !isSensitive }
                        .padding(vertical = 4.dp)
                ) {
                    Checkbox(
                        checked = isSensitive,
                        onCheckedChange = { isSensitive = it },
                        colors = CheckboxDefaults.colors(checkedColor = EmeraldPrimary)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Sensitive Record", color = VaultTextPrimary, fontSize = 14.sp)
                }
            }

            validationError?.let { err ->
                Text(err, color = RoseDanger, fontSize = 12.sp)
            }

            // Save Record Button
            Button(
                onClick = {
                    if (recordName.isBlank()) {
                        validationError = "Please enter a record name"
                        return@Button
                    }
                    val jsonArray = JSONArray()
                    for (f in fieldsList) {
                        val obj = JSONObject().apply {
                            put("name", f.name.trim())
                            put("type", f.type)
                            put("value", f.value.trim())
                            put("sensitive", f.sensitive)
                        }
                        jsonArray.put(obj)
                    }

                    val firstVal = fieldsList.firstOrNull { it.value.isNotBlank() }?.value ?: ""
                    val itemToSave = VaultItem(
                        id = initialItem?.id ?: 0L,
                        type = VaultItemType.RECORD,
                        title = recordName.trim(),
                        value = jsonArray.toString(),
                        collectionId = selectedCollection,
                        categoryTag = selectedCollection.replaceFirstChar { it.uppercase() },
                        tags = tagsList.joinToString(", "),
                        notes = "${fieldsList.size} fields recorded",
                        favorite = isFavorite,
                        isFavorite = isFavorite,
                        pinned = initialItem?.pinned ?: false,
                        sensitive = isSensitive,
                        documentNumber = firstVal,
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
                    .testTag("save_record_button")
            ) {
                Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Black)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Save Record", color = Color.Black, fontSize = 16.sp, fontWeight = FontWeight.Bold)
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
