package com.justself.klique.gists.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.justself.klique.GlobalEventBus
import com.justself.klique.gists.data.models.GistModel
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel


enum class CurrentTab {
    TRENDING, INTERACTIONS
}
@Composable
fun GistScreen(customerId: Int, viewModel: SharedCliqueViewModel, navController: NavController) {
    LaunchedEffect(key1 = Unit) {
        viewModel.fetchTrendingGists(customerId)
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        var currentTab by remember { mutableStateOf(CurrentTab.TRENDING) }

        val uiState by viewModel.uiState.collectAsState()
        Row(modifier = Modifier) {
            Button(
                onClick = { currentTab = CurrentTab.TRENDING; viewModel.fetchTrendingGists(customerId) },
                colors = ButtonDefaults.buttonColors()
                    .copy(containerColor = if (currentTab == CurrentTab.TRENDING) MaterialTheme.colorScheme.primary else Color.Gray),
                modifier = Modifier
                    .padding(16.dp)
                    .width(146.dp)
            ) {
                Text(text = "Trending", color = MaterialTheme.colorScheme.onPrimary)
            }
            Button(
                onClick = { currentTab = CurrentTab.INTERACTIONS; viewModel.fetchInteractions(customerId) }, modifier = Modifier
                    .padding(16.dp)
                    .width(146.dp),
                colors = ButtonDefaults.buttonColors()
                    .copy(containerColor = if (currentTab == CurrentTab.INTERACTIONS) MaterialTheme.colorScheme.primary else Color.Gray)
            ) {
                Text(text = "Interactions", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        when (currentTab) {
            CurrentTab.TRENDING -> {
                val message = "Probably loading"
                GistListCaller(uiState.trendingGists, customerId,
                    { viewModel.enterGist(it) }, defaultMessage = message)
            }
            CurrentTab.INTERACTIONS -> {
                val message = "You don't yet have any interactions with any gists"
                GistListCaller(uiState.interactions, customerId,
                    { viewModel.enterGist(it) }, defaultMessage = message)
            }
        }
    }
}