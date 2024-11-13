package com.justself.klique.gists.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel


enum class CurrentTab {
    TRENDING, MY_GISTS
}
@Composable
fun GistScreen(modifier: Modifier, customerId: Int, viewModel: SharedCliqueViewModel, navController: NavController) {
    LaunchedEffect(key1 = Unit) {
        viewModel.fetchTrendingGists(customerId)
    }
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
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
                onClick = { currentTab = CurrentTab.MY_GISTS; viewModel.fetchMyGists(customerId) }, modifier = Modifier
                    .padding(16.dp)
                    .width(146.dp),
                colors = ButtonDefaults.buttonColors()
                    .copy(containerColor = if (currentTab == CurrentTab.MY_GISTS) MaterialTheme.colorScheme.primary else Color.Gray)
            ) {
                Text(text = "My Gists", color = MaterialTheme.colorScheme.onPrimary)
            }
        }
        when (currentTab) {
            CurrentTab.TRENDING -> {
                TrendingGists(uiState.trendingGists, customerId = customerId, viewModel)
            }

            CurrentTab.MY_GISTS -> {
                MyGists(uiState.myGists, customerId = customerId, viewModel)
            }
        }
    }
}