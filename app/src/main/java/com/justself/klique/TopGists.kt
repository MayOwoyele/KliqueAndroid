package com.justself.klique

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.justself.klique.gists.data.models.GistModel
import com.justself.klique.gists.ui.GistListCaller
import com.justself.klique.gists.ui.GistPreview
import com.justself.klique.gists.ui.viewModel.SharedCliqueViewModel

@Composable
fun TopGistsScreen(viewModel: SharedCliqueViewModel, navController: NavController) {
    val topGists by viewModel.topGists.collectAsState()
    var selectedGist by remember { mutableStateOf<GistModel?>(null) }
    val customerId by SessionManager.customerId.collectAsState()
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        viewModel.getTopGists()
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .padding(horizontal = 16.dp)) {
        GistListCaller(
            modifier = Modifier.fillMaxSize(),
            gists = topGists,
            customerId = customerId,
            onTap = { gist -> selectedGist = gist },
            listState = listState,
            defaultMessage = "No top gists yet 👀"
        )
        selectedGist?.let { gist ->
            val onGistStart = {
                viewModel.enterGist(gist.gistId)
                Screen.Home.navigate(navController)
            }
            GistPreview(
                selectedGist = gist,
                onDismiss = { selectedGist = null },
                onGistStart = onGistStart
            )
        }
        Box(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .align(Alignment.TopCenter)
                .background(
                    MaterialTheme.colorScheme.background.copy(alpha = 0.5f)
                )
        ) {
            Text(
                "Top Gists",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.padding(10.dp)
            )
        }
    }
}