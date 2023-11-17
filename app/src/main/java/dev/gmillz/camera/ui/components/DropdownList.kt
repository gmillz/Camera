package dev.gmillz.camera.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DropdownList(
    expandedO: Boolean = false,
    list: List<String>,
    onDismissRequest: () -> Unit,
    onSelect: (String) -> Unit
) {
    var selectedText by remember { mutableStateOf("") }
    var expanded by remember {
        mutableStateOf(expandedO)
    }

    TextField(
        readOnly = true,
        value = selectedText,
        onValueChange = {},
        label = { },
        trailingIcon = {
            ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded)
        },
        modifier = Modifier.clickable { expanded = true }
    )

    DropdownMenu(
        expanded = expanded,
        onDismissRequest = onDismissRequest
    ) {
        list.forEach { item ->
            DropdownMenuItem(
                text = { Text(text = item) },
                onClick = {
                    onDismissRequest()
                    onSelect(item)
                    selectedText = item
                }
            )
        }
    }
}