package com.justself.klique

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.justself.klique.MyKliqueApp.Companion.appContext

@Composable
fun UpdateRequiredScreen(navController: NavController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        val currentVersion = BuildConfig.VERSION_CODE
        val requiredVersion = AppUpdateManager.getRequiredVersion(appContext)
        val expirationDate = AppUpdateManager.getExpirationDate(appContext)
        val currentTime = System.currentTimeMillis()
        val twoWeeksMillis = 14 * 24 * 60 * 60 * 1000L
        val timeDifferenceMillis = expirationDate - currentTime
        val daysLeft = (timeDifferenceMillis / (1000 * 60 * 60 * 24)).toInt()
        val notYetExpired = currentVersion < requiredVersion && (expirationDate - currentTime <= twoWeeksMillis) && expirationDate > currentTime
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Update Required",
                style = MaterialTheme.typography.displayMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(16.dp))
            if (notYetExpired) {
                val dayString = when (daysLeft) {
                    0 -> "today"
                    1 -> "a day"
                    else -> "$daysLeft days"
                }
                Text(
                    text = "You'd need to update klique in at most, $dayString",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center
                )
            } else {
                Text(
                    text = "To continue using klique, please update to the latest version.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onPrimary,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = {  },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Update Now",
                    style = MaterialTheme.typography.bodyLarge
                )
            }
            if (notYetExpired) {
                Button(onClick = { AppUpdateManager.dismissUpdateRequirement() }, colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )) {
                    Text(text = "Continue to app", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}