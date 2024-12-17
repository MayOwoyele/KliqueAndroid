package com.justself.klique

import SettingsViewModel
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController

@Composable
fun SettingsScreen(navController: NavController) {
    val settingsViewModel: SettingsViewModel = viewModel()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .background(MaterialTheme.colorScheme.background)
    ) {
        Text(
            text = "Settings",
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // Delete My Account Setting Item
        Text(
            text = "Delete My Account",
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    settingsViewModel.showDeleteAccountDialog.value = true
                }
                .padding(8.dp)
        )

        if (settingsViewModel.showDeleteAccountDialog.value) {
            DeleteAccountDialog(
                onDismiss = { settingsViewModel.dismissDeleteDialog() },
                onDelete = {
                    settingsViewModel.deleteAccount(
                        onFailure = { error ->
                            println("Failed to delete account: $error")
                        }
                    )
                }
            )
        }
    }
}

@Composable
fun DeleteAccountDialog(
    onDismiss: () -> Unit,
    onDelete: (String) -> Unit
) {
    var reason by remember { mutableStateOf("") }
    val isReasonValid = reason.isNotBlank() && reason.length <= 1000

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete My Account", style = MaterialTheme.typography.displayLarge) },
        text = {
            Column {
                Text("Are you sure you want to delete your account?")
                Spacer(modifier = Modifier.height(8.dp))
                Text("Please provide a reason or say \"none\" (max 1000 characters):")
                TextField(
                    value = reason,
                    onValueChange = {
                        if (it.length <= 1000) {
                            reason = it
                        }
                    },
                    placeholder = { Text("Type your reason here...") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${reason.length}/1000 characters",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onDelete(reason) },
                enabled = isReasonValid
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            Button(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}